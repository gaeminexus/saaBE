# PROCESO DIARIO DE INTERÉS DE MORA

**Implementado el 2026-08-14 · módulo CRD**

Calcula y **persiste** el interés de mora de todas las cuotas vencidas, todos los días a las
02:00. Hasta ahora la mora solo se calculaba al vuelo dentro de los reportes financieros; el
campo `DTPRMRAA` de la cuota nunca se escribía.

---

## 1. Qué existía antes

| Pregunta | Respuesta verificada en el código |
|---|---|
| ¿Se actualizaba `DTPRMRAA` (mora de la cuota)? | **No.** El único lugar que lo escribía era `PrestamoServiceImpl`, poniéndolo en `0.0` al generar la tabla de amortización. |
| ¿Y `DTPRMRCL` (moraCalculada) / `DTPRDSMR` (díasMora)? | **Tampoco.** Nunca se escribían. |
| ¿`DTPRTTLL` (total) sumaba la mora? | **No.** Era `cuota + desgravamen + seguro`. |
| ¿Dónde se calculaba la mora entonces? | Solo en memoria, dentro del G48 y el CCPM, vía `DetallePrestamoDaoService.calcularInteresMoraBatch`. El resultado se informaba en el reporte y se descartaba. |

---

## 2. Fórmula

Es **la misma** que ya usaban los reportes, sin ningún cambio:

```
mora_cuota = capital × (prestamo.interesNominal / 100 / 360) × díasDeMora
díasDeMora = días entre la fechaVencimiento de la cuota y la fecha de corte
```

- Si `interesNominal` es nulo o ≤ 0 se usa **9.0**, el mismo valor por defecto del G48.
- Base de días **360**, igual que el G48 y el CCPM.
- La diferencia con los reportes es la fecha: el proceso usa **hoy** (sysdate), no la fecha de
  corte del período reportado.

### Por qué la descomposición por cuota es equivalente

`calcularInteresMoraBatch` recorre las cuotas del préstamo desde la más antigua impaga y suma
`capital_k × tasa/36000 × días_k`. **Cada sumando es la mora de una cuota.** Por eso:

```sql
SUM(DTPRMRAA) de las cuotas vencidas de un préstamo  ==  el total que hoy reporta el G48
```

Es la misma sumatoria, escrita fila por fila en vez de agregada.

---

## 3. Universo: a qué cuotas aplica

| Condición | Detalle |
|---|---|
| Cuota pendiente | `DTPRESTD IS NULL OR DTPRESTD NOT IN (4 PAGADA, 7 CANCELADA_ANTICIPADA)` |
| Cuota vencida | `DTPRFCVN < fechaCorte` (inicio del día: **la cuota que vence hoy todavía no está en mora**) |
| Préstamo operable | **`PRSTIDST IN (2 VIGENTE, 11 EN_MORA)`** |

Los préstamos en estado terminal (3, 4, 5) quedan fuera y el proceso **nunca** les toca el estado.

### ⚠️ `DE_PLAZO_VENCIDO(8)` NO entra — corregido el 2026-08-24

**Este universo NO es el del Grupo 2 del G48**, aunque nació copiándolo. El G48 incluye el 8 y
está bien que lo haga: **el G48 solo LEE la mora**. Este proceso **ESCRIBE el estado del
préstamo**, y por eso no puede compartir universo con un reporte.

Mientras el 8 estuvo incluido (del 2026-08-14 al 2026-08-24), el proceso reclasificó a
`EN_MORA(11)` todos los préstamos que estaban en DE PLAZO VENCIDO. Ver §11.

La exclusión está en **dos niveles**, a propósito:

1. **En el universo del lote** — `DetallePrestamoDaoServiceImpl.selectPrestamosConCuotasVencidas`
   filtra `idEstado IN (2, 11)`.
2. **En una guarda dentro de `calcularMoraPrestamo`** — porque el endpoint
   `POST /prst/calcularMora/{idPrestamo}` entra directamente a ese método y **se saltea la
   consulta del universo**. Sin la guarda, un préstamo en 8 invocado a mano se seguiría
   rompiendo.

---

## 4. Qué escribe

### Por cada cuota vencida

| Columna | Campo Java | Valor |
|---|---|---|
| `DTPRMRAA` | `mora` | La mora calculada |
| `DTPRMRCL` | `moraCalculada` | Igual que la anterior |
| `DTPRDSMR` | `diasMora` | Días transcurridos desde el vencimiento |
| `DTPRSLMR` | `saldoMora` | `max(0, mora − moraPagado)` |
| **`DTPRTTLL`** | **`total`** | **`totalBase + mora`** ← ver §5 |
| `DTPRTTCS` | `totalConSeguro` | Espejo de `DTPRTTLL` |
| `DTPRESTD` / `DTPRIDST` | `estado` / `idEstado` | **5 (EN_MORA)**, salvo que ya esté en 5 o en 6 (PARCIAL) |

> Una cuota **PARCIAL (6)** no se pisa: ese estado indica que ya recibió un pago y lo maneja el
> motor de pagos. Sí se le calcula la mora.

### Por cada préstamo

| `PRSTIDST` de entrada | Situación | Efecto |
|---|---|---|
| **8 DE_PLAZO_VENCIDO** | cualquiera | **No se toca NADA**: ni el estado del préstamo, ni el de sus cuotas, ni la mora. Sale antes de calcular |
| 2 VIGENTE | tiene cuotas vencidas | `PRSTIDST → 11 (EN_MORA)` + `PRSTFCMD = now` |
| 2 VIGENTE | sin cuotas vencidas | No se toca |
| 11 EN_MORA | tiene cuotas vencidas | Se recalcula la mora; el estado ya es 11 y no cambia |
| 11 EN_MORA | sin cuotas vencidas | `PRSTIDST → 2 (VIGENTE)` + `PRSTFCMD = now` |
| 3, 4, 5 (terminales) | cualquiera | **No se toca el estado** |

Nunca se escribe `ESPSCDGO` (es la FK al catálogo `CRD.ESPS`, no el estado operativo).

> ⚠️ **La regularización siempre manda a 2 VIGENTE, nunca a 8.** El proceso no guarda el
> estado anterior, así que no puede saber si un préstamo llegó a 11 porque de verdad se
> atrasó o porque lo reclasificó mal el defecto de la §11. Un préstamo que estaba en 8, fue
> mal reclasificado y después se regulariza, **termina en 2**. Restituirlos es corrección de
> datos; el proceso no lo puede deshacer solo.

---

## 5. `DTPRTTLL` ahora incluye la mora

**Decisión de negocio del 2026-08-14.** El total de la cuota pasa a ser *lo que hay que pagar
hoy*, mora incluida. El proceso diario es el **único dueño** de esa columna para las cuotas
vencidas.

### Idempotencia

El total se recompone, no se acumula:

```
totalBase  = DTPRTTLL_actual − DTPRMRAA_actual
DTPRTTLL   = totalBase + moraNueva
```

Correr el proceso N veces el mismo día da exactamente el mismo resultado. Además, al derivar la
base restando la mora anterior (en vez de recalcularla como `cuota + desgravamen + seguro`), se
respeta la base original de las tablas **cargadas desde Excel**, donde `DTPRTTLL` viene de la
columna "CUOTA A PAGAR" y puede no coincidir con la suma de los componentes.

### Consumidores que hubo que ajustar

Meter la mora en `DTPRTTLL` rompía a cuatro consumidores que leían esa columna esperando el total
**sin** mora. Los cuatro se ajustaron para restarla, de modo que **su comportamiento no cambia**:

| Clase | Qué hacía | Ajuste |
|---|---|---|
| `MotorPagoPrestamoServiceImpl` | `totalPendiente = total + mora + IV` | Ahora `total + IV`. Con la mora ya dentro del total, sumarla otra vez la cobraba **dos veces** |
| `CargaArchivoPetroServiceImpl` (fase 2) | Comparaba `total` contra el monto del archivo con tolerancia $1 | Usa el nuevo helper `totalBaseCuota(cuota)`. Sin esto, **toda cuota vencida** daría `MONTO_INCONSISTENTE (13)`, que bloquea la fase 3 completa |
| `CargaArchivoPetroServiceImpl` (fase 3) | `calcularSaldosRealesCuota` usaba `total` como pendiente | Usa `totalBaseCuota(cuota)`. Su prelación solo tiene 4 componentes y no puede imputar mora: toda cuota vencida habría quedado **PARCIAL en vez de PAGADA** |
| `GeneracionG48ServiceImpl` y `GeneracionCCPMServiceImpl` | `dividendo = cuota.getTotal()` | Usan `dividendoSinMora(cuota)`. Ambos reportes ya llevan la mora en su propia columna: sin el ajuste la reportarían **dos veces** |

```java
// El patrón, idéntico en los tres archivos:
totalBase = nvl(total) − nvl(mora) − nvl(interesVencido)
```

### El archivo Petro sí cobra la mora (sin cambios de código)

`GeneracionArchivoPetroServiceImpl` **no** lee `DTPRTTLL`: arma el monto a cobrar en
`calcularSaldoCuota`, que ya sumaba los componentes uno a uno, mora incluida:

```java
capital + interes + mora + interesVencido + desgravamen   // menos lo ya pagado
```

Como `DTPRMRAA` valía siempre 0, ese término no aportaba nada. Desde que el proceso diario lo
alimenta, **el archivo empieza a cobrar la mora automáticamente**, sin tocar una línea de esa
clase. El seguro de incendio sigue viajando aparte en el producto `HS`, como siempre.

---

## 6. El interés vencido (`DTPRINVN`) sigue en 0

Este proceso calcula **solo mora**. No hay ninguna fórmula definida de interés vencido en el
sistema, así que `DTPRINVN` queda en 0 y el motor de pagos lo sigue sumando aparte (donde vale 0
y no aporta nada). La prelación del motor ya tiene el componente listo para cuando se defina.

---

## 7. Ejecución automática

`com.saa.ejb.crd.serviceImpl.ProcesoMoraPrestamoTimer` — es el **primer timer EJB del proyecto**.

```java
@Schedule(hour = "2", minute = "0", second = "0", persistent = false)
```

- Corre todos los días a las **02:00**, hora del servidor.
- **Cambiar el horario**: editar la anotación y redesplegar. El proyecto no tiene configuración
  externa para esto.
- **`persistent = false`**: el timer no se guarda en la base de timers de WildFly. Si el servidor
  está apagado a las 02:00 **la corrida se pierde** (se recupera con el endpoint manual), pero a
  cambio no quedan timers huérfanos tras un redespliegue ni hay que limpiar
  `standalone/data/timer-service-data`.
- El timer atrapa `Throwable`: un fallo no deja el timer en error ni dispara reintentos
  automáticos de WildFly.

### Transaccionalidad

Cada préstamo se procesa en **su propia transacción** (`REQUIRES_NEW`), invocada a través del
proxy EJB por auto-inyección:

```java
@EJB private ProcesoMoraPrestamoService self;   // el bucle llama self.calcularMoraPrestamo(...)
```

Un préstamo con datos malos se cuenta como error y **no aborta el lote**. El método orquestador
corre con `NOT_SUPPORTED` para no mantener una transacción abierta durante todo el recorrido.

---

## 8. Endpoints de recuperación manual

Los expone `PrestamoRest` con el mismo sobre de respuesta que el resto de los servicios de pago.

### Todo el sistema

```http
POST /SaaBE/rest/prst/calcularMora?fecha=2026-08-14&usuario=jperez
```

Ambos parámetros son opcionales: sin `fecha` usa hoy, sin `usuario` registra `SAA_MORA`.

```json
{
  "exito": true,
  "etapa": "APLICACION",
  "mensaje": "Mora calculada al 2026-08-14: 143 cuota(s) de 58 préstamo(s), total $1204.77",
  "resultado": {
    "fechaCorte": "2026-08-14",
    "fechaInicio": "2026-08-14T02:00:00", "fechaFin": "2026-08-14T02:00:11",
    "duracionMs": 11240,
    "prestamosEvaluados": 58, "prestamosProcesados": 58,
    "cuotasActualizadas": 143, "cuotasMarcadasEnMora": 12,
    "prestamosMarcadosEnMora": 4, "prestamosRegularizados": 1,
    "totalMoraCalculada": 1204.77,
    "prestamosConError": 0,
    "errores": []
  }
}
```

### Un préstamo

```http
POST /SaaBE/rest/prst/calcularMora/8523?fecha=2026-08-14&usuario=jperez
```

Mismo cuerpo de respuesta, con los conteos de ese préstamo.

### Errores

| HTTP | `error` | Cuándo |
|---|---|---|
| 400 | — | `fecha` con formato distinto de `yyyy-MM-dd`, o `idPrestamo` inválido |
| 404 | `PRESTAMO_NO_ENCONTRADO` | El préstamo no existe (solo en la variante por préstamo) |
| 422 | `FECHA_INVALIDA` | La fecha de corte es futura |
| 500 | — | Error inesperado |

Los préstamos que fallan **dentro** del lote no producen un error HTTP: la respuesta es 200 y
vienen contados en `prestamosConError` con el detalle en `errores` (hasta 50).

---

## 9. Controles SQL

```sql
-- Cuotas vencidas y su mora, para un préstamo
SELECT DTPRNMCT, DTPRFCVN, DTPRESTD, DTPRIDST, DTPRCPTL,
       DTPRMRAA, DTPRMRCL, DTPRDSMR, DTPRSLMR, DTPRTTLL, DTPRTTCS
FROM   CRD.DTPR
WHERE  PRSTCDGO = :id
ORDER BY DTPRNMCT;

-- Contraste con el G48: la suma por préstamo debe coincidir con el reporte a la misma fecha
SELECT d.PRSTCDGO, SUM(d.DTPRMRAA) AS MORA_TOTAL, COUNT(*) AS CUOTAS_VENCIDAS
FROM   CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
WHERE  p.PRSTIDST IN (2, 8, 11)
AND   (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
AND    d.DTPRFCVN < TRUNC(SYSDATE)
GROUP BY d.PRSTCDGO
ORDER BY MORA_TOTAL DESC;

-- Idempotencia: correr el proceso dos veces no debe cambiar nada.
-- Guardar esta foto, relanzar el endpoint y volver a compararla.
SELECT SUM(DTPRMRAA), SUM(DTPRTTLL) FROM CRD.DTPR;

-- Coherencia total/mora (debe devolver 0 filas):
-- ninguna cuota SIN mora puede tener el total inflado
SELECT DTPRCDGO, DTPRTTLL, DTPRCTAA, DTPRDSGR, DTPRVLSI, DTPRMRAA
FROM   CRD.DTPR
WHERE  NVL(DTPRMRAA,0) = 0
AND    ABS(NVL(DTPRTTLL,0) - (NVL(DTPRCTAA,0) + NVL(DTPRDSGR,0) + NVL(DTPRVLSI,0))) > 0.02
AND    DTPRTTLL IS NOT NULL;

-- Préstamos marcados EN_MORA por el proceso
SELECT PRSTCDGO, PRSTIDST, PRSTFCMD FROM CRD.PRST
WHERE  PRSTIDST = 11 ORDER BY PRSTFCMD DESC;

-- Espejo de estados intacto (debe devolver 0 filas)
SELECT DTPRCDGO FROM CRD.DTPR WHERE NVL(DTPRIDST,-1) <> NVL(DTPRESTD,-1);
```

---

## 10. Pruebas recomendadas

1. **Préstamo de control con cuotas vencidas**: correr `POST /prst/calcularMora/{id}` y verificar
   que cada cuota vencida tiene `DTPRMRAA` = `capital × tasa/36000 × días`, y que
   `DTPRTTLL = base + mora`.
2. **Contraste con el G48**: generar el G48 a la fecha de hoy y comparar su columna de mora contra
   `SUM(DTPRMRAA)` por préstamo. Deben coincidir.
3. **Idempotencia**: correr el endpoint dos veces seguidas y verificar que `SUM(DTPRTTLL)` no
   cambia.
4. **Regularización**: pagar todas las cuotas vencidas de un préstamo en 11, correr el proceso y
   verificar que vuelve a `PRSTIDST = 2`.
5. **Regresión del motor de pagos**: sobre una cuota con mora, `POST /prst/pagarCuota` por el
   total exacto debe dejarla PAGADA, con `PGPRMRPG` = la mora y **sin** cobrarla dos veces.
6. **Regresión Petro**: correr una carga completa con cuotas vencidas y verificar que **no**
   aparecen novedades `MONTO_INCONSISTENTE (13)` nuevas y que las cuotas quedan PAGADA, no
   PARCIAL.
7. **Timer**: confirmar en el log de WildFly la línea `TIMER MORA - Disparo automático` a las
   02:00 del día siguiente.
8. **Préstamo en DE_PLAZO_VENCIDO(8)** (regresión del defecto de la §11): tomar un préstamo en
   8 **con cuotas vencidas** y correr `POST /prst/calcularMora/{id}`. Verificar que:
   - `PRSTIDST` sigue en **8** (no pasó a 11);
   - ninguna de sus cuotas cambió de estado ni tiene `DTPRMRAA` / `DTPRDSMR` nuevos;
   - `DTPRTTLL` de esas cuotas no cambió;
   - el log trae la línea `Préstamo N en estado 8 (DE PLAZO VENCIDO): fuera del proceso de mora`.

   Después correr el lote completo (`POST /prst/calcularMora`) y verificar lo mismo: el préstamo
   ni siquiera debe aparecer en el conteo de evaluados.

---

## 11. Historial de defectos

### 2026-08-24 — Los préstamos en DE PLAZO VENCIDO fueron reclasificados a EN MORA

| | |
|---|---|
| **Severidad** | Alta. Pérdida de un estado operativo en toda la cartera afectada |
| **Activo desde** | **2026-08-14**, la implementación original del proceso |
| **Detectado** | 2026-08-24, en producción |
| **Corregido** | 2026-08-24 |

#### Qué pasó

El universo del proceso se definió copiando el del **Grupo 2 del G48**, que incluye
`PRSTIDST IN (2, 8, 11)`. Para un reporte eso es correcto: el G48 informa la mora de los
préstamos de plazo vencido y **no escribe nada**.

Este proceso, en cambio, **escribe el estado del préstamo**: todo préstamo del universo con
cuotas vencidas y estado distinto de 11 pasa a `EN_MORA(11)`. Como los préstamos en
`DE_PLAZO_VENCIDO(8)` por definición tienen cuotas vencidas, **todos** entraron por esa rama.

Resultado observado en producción: **todos los préstamos que estaban en DE PLAZO VENCIDO
quedaron en EN MORA.** El estado 8 desapareció de la cartera.

Agravante: el proceso corre **todas las noches a las 02:00**, así que el daño se repetía en
cada corrida. Y como la regularización solo devuelve a `2 VIGENTE` (§4), los préstamos mal
reclasificados que se pusieran al día tampoco volvían a 8 por sí solos.

#### Por qué no se notó antes

El defecto no rompe ninguna cuenta: la mora calculada era correcta, `DTPRTTLL` cuadraba y el
contraste contra el G48 daba bien. Lo único que cambiaba era la **clasificación** del préstamo,
que ninguna de las pruebas de la §10 miraba. La prueba 8 se agregó justamente por eso.

#### Qué lo corrige

| Nivel | Archivo | Cambio |
|---|---|---|
| Universo del lote | `DetallePrestamoDaoServiceImpl.selectPrestamosConCuotasVencidas` | El JPQL pasa de `idEstado IN (2, 8, 11)` a `idEstado IN (2, 11)`. Se quitó el parámetro `:plazoVencido` |
| Guarda por préstamo | `ProcesoMoraPrestamoServiceImpl.calcularMoraPrestamo` | Sale temprano si `PRSTIDST = 8`, **sin calcular mora y sin tocar ningún estado**, con traza |
| Documentación | `DetallePrestamoDaoService` (JavaDoc de los dos métodos), `ProcesoMoraPrestamoService` (JavaDoc de clase y del método) | Dejan dicho que el 8 queda fuera y por qué |

**Los dos niveles son necesarios.** Sacar el 8 del universo cierra la puerta del lote, pero el
endpoint `POST /prst/calcularMora/{idPrestamo}` llama a `calcularMoraPrestamo` **salteándose la
consulta del universo**: desde ahí un préstamo en 8 se habría seguido reclasificando.

#### Corrección de datos

**Fuera del alcance del código.** La restitución de los préstamos afectados a su estado 8 la
resuelve el usuario contra su respaldo. El código corregido garantiza que, una vez restituidos,
el proceso no los vuelva a romper — ni en la corrida de las 02:00 ni desde el endpoint manual.

#### Lección

**Un universo compartido entre un reporte y un proceso que escribe no es reutilización, es un
acoplamiento peligroso.** Un reporte puede permitirse mirar de más; un proceso que persiste
estados, no. Si algún día un reporte necesita este método, que escriba su propia consulta.
