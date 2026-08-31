# Estado — EQUIPO SEGUROS (crd)

**Árbitro:** sesión de seguros · **Creado:** 2026-08-30
**Alcance:** seguros de desgravamen e incendio por pólizas anuales, en `crd`.
**Fuera de alcance:** jubilados, otorgamiento, reestructuración, cobros, cutover.
**Archivo ajeno (solo lectura):** `ejb/crd/serviceImpl/CalculadoraAmortizacionServiceImpl.java`
— es del equipo del ciclo del crédito y **ya calcula bien los importes de seguro por cuota**.

> Este documento es el estado de ESTE equipo. No lo escribe ningún otro árbitro.

---

## 1. Verificación del punto de partida (2026-08-30)

Todo lo de esta sección está **verificado contra el código**, no contra documentación.
Ninguna afirmación de acá viene de un `.md`.

### 1.1 Confirmado: el hecho administrativo no existe

| Pieza | Resultado de la búsqueda |
|---|---|
| Entidad JPA de póliza / aseguradora / inscripción | ❌ ninguna. `find src/main/java -iname "*poliza*" -o -iname "*seguro*" -o -iname "*aseguradora*"` devuelve **un solo archivo**, `com/saa/rubros/RhhCodigoSeguroSocialIess.java`, que es del IESS y no tiene nada que ver |
| DAO / Service de seguros | ❌ ninguno |
| Endpoint REST | ❌ ninguno. `grep -rn -i "poliza\|aseguradora" src/main/java/com/saa/ws/` → **cero líneas**. Ningún `@Path` de `ws/rest/crd` contiene `seg` ni `pol` |
| Códigos de tabla `PLZA`/`PLIZ`/`POLZ`/`INPL`/`ASEG`/`SGRO`/`SEGU` | libres en `model/` (falta el control contra `ALL_TABLES`) |

**El dato de partida del alcance es correcto: el sistema cobra un seguro que no tiene registrado.**

### 1.2 Dónde viven hoy los importes — y quién los escribe

`CRD.DTPR` (`DetallePrestamo`), por cuota:

| Campo Java | Columna | Quién lo escribe |
|---|---|---|
| `desgravamen` | `DTPRDSGR` | `PrestamoServiceImpl` (alta e importación Excel), `AbonoCapitalPrestamoServiceImpl`, `CalculadoraAmortizacionServiceImpl` |
| `desgravamenFirmado` | `DTPRDSFR` | `PrestamoServiceImpl:237,395`, `AbonoCapitalPrestamoServiceImpl:760` |
| `desgravamenDiferido` | `DTPRDSDF` | **siempre 0.0**, en los tres sitios que lo escriben |
| `desgravamenOriginal` | `DTPRDSOR` | copia de `desgravamen` al crear la cuota |
| `desgravamenPagado` | `DTPRDSPG` | `MotorPagoPrestamoServiceImpl:205`, `CargaArchivoPetroServiceImpl` (5 puntos) |
| `valorSeguroIncendio` | `DTPRVLSI` | `PrestamoServiceImpl:243,399`, `AbonoCapitalPrestamoServiceImpl:769` |
| `otrosSeguros` | `DTPROTSG` | **siempre 0.0** |
| `totalConSeguro` | `DTPRTTCS` | siempre igual a `DTPRTTLL` |

**Ninguna de esas ocho columnas tiene una FK, un código ni un texto que apunte a una póliza.**
Es literalmente la definición de "huérfanos" del alcance.

### 1.3 Hallazgo nuevo — `CRD.PRST` ya tiene cuatro columnas de seguro, y están MUERTAS

```
PRSTVLAS  valorAsegurado       Double
PRSTTSIN  tasaSeguroIncendio   Double
PRSTPRIN  primaSeguroIncendio  Double
PRSTTTSG  totalSeguros         Double
```

Están mapeadas en `model/crd/Prestamo.java:284-297` y `:133`, existen en la base (aparecen en
`sql/VERIFICACION-ENTIDADES-VS-ESQUEMA-CRD.sql`), están declaradas en el modelo del frontend
(`modules/crd/model/prestamo.ts:69-71`) — y **nadie las lee ni las escribe en ninguno de los dos
repositorios**. `grep` de los cuatro getters/setters fuera de la entidad: **cero resultados**.

Es el patrón "el dato no viene de donde parece": el esqueleto de un modelo por préstamo que se
mapeó y nunca se conectó. **Decidir explícitamente si el modelo nuevo las adopta o las declara
obsoletas** — dejarlas ahí, mapeadas y vacías, es lo que produjo esta confusión.

### 1.4 Hallazgo nuevo — la tasa de desgravamen es una CONSTANTE en Java

`CalculadoraAmortizacionServiceImpl:59`

```java
static final double FACTOR_DESGRAVAMEN_SOBRE_SALDO = 1.12 / 1000.0;
```

No sale del producto, ni de un rubro, ni de una póliza: está **quemada en el código**, y
`AbonoCapitalPrestamoServiceImpl:634` la importa para regenerar la tabla tras un abono a capital.
`model/crd/Producto.java` **no tiene ningún campo de seguro**.

Esto contesta por adelantado una de las "dudas abiertas" del alcance: hoy la tasa de desgravamen
**no sale del producto** — no sale de ningún dato, sale de una constante.

### 1.5 Hallazgo nuevo — la cartera real trae los seguros de la MIGRACIÓN

Dos caminos escriben `DTPR`, y hacen cosas distintas:

- `PrestamoServiceImpl.generarAmortizacion:196-197` — el alta normal — **fija
  `desgravamenPorCuota = 0.0` y `seguroIncendioPorCuota = 0.0`** de forma explícita, con el
  comentario *"El generador de tabla nueva no calcula desgravamen ni seguro de incendio por cuota
  (comportamiento preexistente)"*.
- `PrestamoServiceImpl:392-399` — la importación desde Excel — lee **columna 6 = desgravamen** y
  **columna 7 = seguro** de la hoja de migración.

**Los importes que hoy se cobran en producción son datos migrados de una hoja de cálculo.** No los
calculó el motor. Eso encaja con "cartera migrada, sin créditos nuevos", y cambia el planteo de la
inscripción retroactiva: no hay una tasa con la que reconstruirlos.

### 1.6 Hallazgo nuevo — ya hubo un parche manual, y dejó el problema escrito

`docs/logica-negocio/crd/sql/60_ACTUALIZA_SEGURO_INCENDIO_PRESTAMOS.sql` (commit `6a23e98`)
carga el seguro de incendio de **131 préstamos** identificados por `PRSTIDAS`, uno a uno con su
valor, y **solo sobre las cuotas de sep/oct/nov 2026**. Su propio encabezado lo dice:

> *"Cuotas fuera de la ventana sep-nov 2026: quedan con su seguro actual. Si el seguro debía
> aplicarse a toda la vida del préstamo, este script cubre solo tres meses y hay que decirlo."*
> *"PRST.PRSTPRIN (primaSeguroIncendio) y PRST.PRSTTSIN (tasa): no se pidieron."*

**Eso es la póliza de incendio, hecha a mano y por tres meses.** Los 131 préstamos de esa lista
son, con altísima probabilidad, la primera inscripción a migrar al modelo nuevo. **Falta confirmar
si el script llegó a ejecutarse en producción** — es una pregunta para el usuario, no algo que se
pueda deducir del repositorio.

### 1.7 Confirmado: la pantalla es un cascarón, y captura más de lo que el alcance supone

`saaFE/src/app/modules/crd/forms/asignacion-seguros/`

- `asignacion-seguros.component.ts:305` — el `TODO(pendiente-backend)`, exactamente en la línea
  que decía el alcance. En `:308` un `console.warn('… Asignación simulada …')`.
- El estado vive en un `signal` privado (`polizasAsignadas`) que **se pierde al recargar**; el
  comentario de `:117-120` lo dice.
- Ruta ✅ (`app.routes.ts:1172`) y entrada de menú ✅ (`menucreditos.component.ts:165`). La pantalla
  **sí está en el menú** — el problema no es visibilidad, es que no persiste.

**Y tres cosas que el alcance no menciona:**

1. **Hay un TERCER tipo de seguro en la pantalla: `PRENDARIO`.**
   `model/asignacion-seguro.ts:1` → `'INCENDIO' | 'DESGRAVAMEN' | 'PRENDARIO'`, con su regla de
   elegibilidad propia (`asignacion-seguros.component.ts:191`). Y la contabilidad lo respalda:
   `CobroPetroContableServiceImpl:103` tiene `AUX1_SEGURO_PRENDARIO = 43` y el plan de cuentas
   tiene `1.4.90.15.03` *Seguro préstamos prendarios*. **El alcance del equipo habla de dos
   seguros y el sistema ya reconoce tres.** Decisión del usuario, no del árbitro.
2. El diálogo ya captura **aseguradora, broker, número, vigencia, plazo en meses y un archivo
   adjunto** (`asignar-seguro-dialog.component.ts:57-63`) — o sea, buena parte del modelo de póliza
   ya está definida por la UI. **No captura tasa ni suma asegurada por préstamo.**
3. La asignación en la pantalla es **por tipo de seguro para toda la cartera elegible de golpe**,
   no préstamo por préstamo. Eso choca de frente con "se declara qué préstamos entran" de
   incendio: la pantalla actual no permite declarar un subconjunto.

### 1.8 Confirmado: el reembolso a la aseguradora ya tiene el insumo calculado

`AbonoCapitalPrestamoServiceImpl:345-352, 504-515` calcula `seguroIncendioLiberado` — la suma del
seguro de incendio de las cuotas que desaparecen cuando un abono a capital acorta el plazo — y lo
expone en `SimulacionAbonoCapital:122` y `ResultadoAbonoCapital:306`. El comentario del código:

> *"Ese monto queda 'liberado' y es el insumo que un futuro proceso de reembolso a la aseguradora
> necesitaría — se deja calculado y expuesto en el resultado del abono."*

**Se calcula y se tira: nadie lo persiste.** Cada abono a capital que acorta plazo pierde el dato
en el momento en que se responde el request. El proceso de reembolso empieza, entonces, por
**guardarlo**, y queda un pendiente sobre los abonos ya procesados.

### 1.9 Estado de la contabilidad de seguros — parcial, y solo por Petro

| Pieza | Estado |
|---|---|
| Compra de la póliza (factura al activo) | ❌ **no existe en `crd`**. §3.2-⑤: entra por CxP, marcada No ATS / No IVA, contra `1.4.90.90.10` (desgravamen) o `1.4.90.15.02/.03/.06`. Plantilla **alterno 18** "CRD PAGO DE SEGUROS ANTICIPADOS" tiene las 4 cuentas al Debe |
| Cobro del seguro al partícipe — **vía Petro** | ✅ **en producción**. `CobroPetroContableServiceImpl:818-856`, plantilla 21: `AUX1_SEGURO_HIPOTECARIO=42`, `AUX1_SEGURO_PRENDARIO=43` (líneas RAW) y `CrdLineaAsiento.SEGURO_DESGRAVAMEN=60` |
| Cobro del seguro al partícipe — **por cualquier otra vía** | ❌ nada. Pagos manuales, cruce, abono y precancelación no dejan asiento (es el agujero del equipo de cobros) |
| Reembolso a la aseguradora | ❌ no existe cuenta ni plantilla |

**Consecuencia para este equipo:** la parte del asiento que nos toca **no es el cobro** (ya está
resuelta en Petro, y por las otras vías depende del cutover del otro equipo). Nos toca la **compra
y amortización de la póliza**: el activo que se debita al recibir la factura y contra el que se
acredita cada cobro. Hoy ese activo se debita en CxP y **nadie sabe contra qué póliza**, que es el
mismo agujero del punto 1.1, un piso más arriba.

### 1.10 Asimetría en el modelo de pagos que hay que respetar

`MotorPagoPrestamoServiceImpl:442`:

> *"El seguro de incendio no tiene campo 'pagado' en la cuota: su acumulado vive en PGPR."*

El desgravamen tiene `DTPRDSPG` en la cuota; el incendio **no tiene equivalente** y se reconstruye
sumando `PagoPrestamo.valorSeguroIncendio`. Cualquier consulta de "cuánto seguro se ha cobrado"
tiene que usar dos caminos distintos según el tipo. **Verificado, no asumido.**

La prelación oficial (`MotorPagoPrestamoServiceImpl:367-388`) cobra **primero incendio, después
desgravamen**, antes que mora, interés y capital.

---

## 2. Conflicto abierto — los rangos reservados cambiaron

El prompt de este equipo reserva **PRBR 260-269 · PDTR 1250-1299**.
`REGISTRO-RESERVAS-EQUIPOS.md`, reescrito por otro árbitro el mismo día, ya **no tiene un equipo
de seguros**: reparte en dos equipos y pone **seguros dentro del EQUIPO B "Ciclo del crédito y
seguros" (PRBR 270-289 · PDTR 1300-1399)**, mientras 260-269 / 1250-1299 pasó al EQUIPO A.

**No se reserva nada ni se pisa nada hasta que el usuario decida.** Es exactamente el error que ese
archivo existe para evitar.

---

## 3. Lo que NO se pudo verificar desde el repositorio

- Si `sql/60_ACTUALIZA_SEGURO_INCENDIO_PRESTAMOS.sql` se ejecutó, y en qué ambientes.
- Cuántos préstamos vigentes tienen hoy `DTPRVLSI > 0` y cuántos `DTPRDSGR > 0` (hace falta correr
  SQL; el árbitro escribe el script, el usuario lo corre).
- Si `PRSTVLAS`/`PRSTTSIN`/`PRSTPRIN` tienen datos en producción o están todas en NULL.
- El detalle del "Grupo Check" de seguros de la pizarra (IMG_0912) — decisión **D10** del
  levantamiento contable, marcada como ilegible y pendiente de pedir al usuario.

---

## 4. Bitácora

| Fecha | Qué |
|---|---|
| 2026-08-30 | Verificación del punto de partida contra el código. Sin plan todavía, sin prompts despachados, sin código ni SQL escrito |
