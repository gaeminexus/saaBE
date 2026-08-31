# Algoritmo — reubicación de aportes duplicados a los meses huecos

**Fecha:** 2026-08-31 · **Pedido del usuario:** en vez de retirar los duplicados, **moverlos** a
los meses que quedaron sin aporte, dejando los huecos en la parte más reciente.

> **Veredicto de la validación: el pedido es correcto y es mejor que borrar — para el subconjunto
> de partícipes al que corresponde.** Pero tal como está enunciado tiene **dos defectos que lo
> volverían inefectivo o falsificarían datos**. Los dos están verificados contra el código y se
> corrigen abajo.

---

## 1. Defecto 1 — «actualizar la fecha» actualiza la columna equivocada

**`CRD.APRT` tiene dos fechas y significan cosas distintas** (`Aporte.java:104` y `:175-187`):

| Columna | Qué es | Se puede mover |
|---|---|---|
| `APRTFCTR` (`fechaTransaccion`) | **fecha de CAJA** — cuándo entró la plata. La lee contabilidad | ⛔ **NO.** Es un hecho: el dinero entró el día que Petro lo descontó |
| `APRTPRDV` (`periodoDevengo`) | **el mes al que pertenece el aporte** (siempre día 1) | ✅ **SÍ. Es exactamente el campo que hay que mover** |

El JavaDoc de `periodoDevengo` lo dice sin ambigüedad: *«NO es la fecha contable: esa sigue siendo
`fechaTransaccion` (APRTFCTR), la fecha de CAJA, que no cambia de significado con el devengo»*.

### Y hay algo peor que un matiz semántico

Todo el sistema decide a qué mes pertenece un aporte con esta expresión —
`PeriodoEfectivoAporteSql.PERIODO_EFECTIVO_SQL`, declarada **fuente única**:

```sql
CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
     WHEN a.APRTVLRR > 0         THEN TRUNC(a.APRTFCTR, 'MM')
     ELSE NULL END
```

**`APRTPRDV` gana siempre.** Entonces, en una fila que ya tiene devengo (todas las creadas desde el
2026-08-27, más las que tocó el backfill `63`):

> **Cambiar `APRTFCTR` no mueve el aporte de mes. Absolutamente nada.** El hueco sigue ahí para la
> generación, para la mora y para el cierre de cartera — y a cambio quedó **falseada la fecha de
> caja**, que es la única de las dos que contabilidad sí lee.

O sea: el cambio sería invisible donde importa y visible donde hace daño. Exactamente al revés.

Quién usa esa expresión, verificado:

- `AporteDaoServiceImpl.sumValorPorEntidadTipoYRangoDevengo` (`:340-348`) — la que alimenta el
  reparto por mes incompleto de la carga.
- `AportadoGeneracionDaoServiceImpl.sumAportadoPorEntidadPeriodoTipo` (`:38-45`) — **la que decide
  a quién se le cobra el faltante en la próxima generación**.

**Corrección: el «mover» es `UPDATE APRTPRDV`, y `APRTFCTR` no se toca nunca.** Como efecto
lateral bueno, para las filas de junio 2025 (que tienen `APRTPRDV` en NULL) esto es simplemente
*escribir* el devengo que nunca tuvieron.

---

## 2. Defecto 2 — mover no cambia el saldo, y por eso el orden importa

**`UPDATE` de una fecha no altera `SUM(APRTVLRR)`.** El saldo del partícipe queda idéntico. La
reubicación arregla la **cronología**; **no arregla el exceso**.

Y de ahí sale la única regla dura de todo este documento:

> ### ⛔ La reubicación es válida SOLO cuando el dinero cuadra
>
> Para cada partícipe: `SUM(valor de sus filas de carga)` vs `SUM(descontado real en PXCA)`.
>
> | Caso | Qué son los "duplicados" | Qué corresponde |
> |---|---|---|
> | **Diferencia ≈ 0** | **Mecanismo M6**: el partícipe estaba en mora, la generación le cobró varios meses de golpe y el generador estampó **todas** las filas con el mes de la carga. **Es plata real, con el mes mal puesto** | ✅ **Reubicar.** Es la corrección correcta, y borrar sería destruir el registro de una plata que sí entró |
> | **Diferencia > 0** | **M1/M3/M5**: plata contada dos veces, filas V1 reemplazadas, filas PARCIAL. **No hay dinero detrás** | ⛔ **NO reubicar.** Mover esas filas a meses huecos **convierte un saldo inflado en una historia falsificada**: meses que nadie pagó quedan como pagados, la generación no los vuelve a cobrar nunca, y el faltante desaparece del radar |

**Consecuencia de proceso, y es lo que hay que respetar:**

```
1º  Depurar el exceso  (reglas R1-R6 del README)      ← lo que no es dinero, se retira o se ajusta
2º  Reubicar por meses (este algoritmo)               ← sobre lo que quedó, que ya es todo dinero real
```

**Invertir el orden propaga el error a meses limpios y lo vuelve indetectable.** Después del paso 1,
toda fila que queda está respaldada por dinero, y ahí la reubicación es segura por construcción.

---

## 3. Defecto 3 — un mes vacío no siempre es un hueco

Un partícipe que ingresó en octubre de 2025 **no debe nada** de junio a septiembre. Compactar sus
aportes hacia el piso le inventaría cuatro meses de historia y —peor— dejaría vacíos los meses
recientes, que **sí** debe: la próxima generación se los cobraría.

### ✅ Regla de negocio confirmada por el usuario (2026-08-31)

> *«Un mes nunca puede quedar hueco. Aunque un mes al partícipe no se le haya descontado, el
> siguiente mes que se le descuenta ese aporte **debe pertenecer al mes no descontado**, y solo a
> nivel contable registrarse en el mes en que efectivamente se le descontó.»*

Esto **confirma el diseño de §1** y le da nombre: la secuencia de devengo de un partícipe es
**contigua, sin huecos**, y la fecha de caja es otra cosa.

- **`APRTPRDV`** = el mes al que pertenece → **contiguo desde el primer mes esperado**.
- **`APRTFCTR`** = el mes en que Petro descontó → **es el que ve contabilidad, y no se toca**.

También confirma que **el código vigente ya hace esto hacia adelante**:
`distribuirAportePorDevengo` reparte lo recibido «entre los meses de devengo incompletos, **del más
antiguo al más nuevo**». El problema es solo **histórico**: las filas creadas antes de la fase de
devengo llevan como periodo el mes de la carga, no el mes adeudado más viejo.

Y por eso **la compactación es exactamente la regla del usuario aplicada al pasado.**

### El único matiz que sigue en pie

La regla dice «un mes nunca queda hueco» **dentro del período en que el partícipe aporta**. Quien
ingresó en octubre de 2025 no debe junio-septiembre: ahí no hay hueco que tapar, no hay secuencia
todavía. **El piso de cada partícipe es el mayor entre 2025-06 y el inicio de su vigencia.**

**Un mes es hueco solo si ese mes se esperaba un aporte.** La fuente es la vigencia del contrato
(`VigenciaContratoServiceImpl.esperadoPorEntidad:221-229`):

- Tiene que existir un **contrato ACTIVO** de la entidad (`CRD.CNTR`).
- Tiene que haber una **vigencia** (`CRD.VGCN`) de ese tipo de aporte que cubra el mes:
  `VGCNFCIN <= último día del mes AND (VGCNFCFN IS NULL OR VGCNFCFN >= último día del mes)`.
- Con importe esperado > 0. Si un tipo no tiene esperado (p. ej. cesantía en $0), **nunca** es hueco.

---

## 4. El algoritmo, ya corregido

Se ejecuta **por cada par (entidad, tipo de aporte)**, con los tipos **9 (jubilación)** y
**11 (cesantía)** por separado — un partícipe aporta a los dos cada mes, y un "duplicado" es
*misma entidad + mismo tipo + mismo mes*.

### 4.1 Rango

- **Piso:** `2025-06` — es `ALCANCE_MINIMO_DEVENGO` en `CargaArchivoPetroServiceImpl:3518`, y es
  obligatorio, no un rango elegido: por debajo de esa fecha el devengo es NULL **a propósito** y
  todos los meses se verían incompletos.
- **Techo:** el mes de la **última carga procesada** (`CRAR.CRARESTD = 3`), **no** `SYSDATE`. El mes
  en curso todavía no se cobró: no es un hueco.

### 4.2 Clasificación de las filas del par (entidad, tipo)

| Clase | Definición | Puede moverse |
|---|---|---|
| **FIJA** | Fila positiva **no** creada por la carga (alta manual, migrada) | ⛔ No. Y **su mes queda ocupado** |
| **MÓVIL** | Fila positiva creada por la carga — identificada **por FORMA**: `APRTIDAS IS NOT NULL` **OR** glosa `Aporte %CargaArchivo: %` **OR** glosa `Abono al aporte%` | ✅ Sí |
| **INTOCABLE** | `APRTVLRR < 0` (pagos con aportes, devoluciones, jubilación), glosas `REVERSO`/`PAGO PRESTAMO`/`DEVOLUCION`, y `tipoMovimiento = EXCEDENTE_PETRO` | ⛔ No, y **no ocupa mes**: nace con `periodoDevengo = NULL` a propósito porque no cubre el aporte esperado de ningún mes (`CargaArchivoPetroServiceImpl:3654`) |

> ⚠️ **Nunca clasificar por `APRTUSRG`.** Las 2.635 filas de junio 2025 tienen usuario NULL. Ese
> filtro es el que hizo que el análisis 61 no las viera nunca. Ver README §2.

### 4.3 Asignación — compactar hacia el piso

```
CUPOS   = meses del rango que se esperaban (§3),
          MENOS los meses ya ocupados por una fila FIJA,
          ordenados ascendente y numerados 1..N

FILAS   = filas MÓVILES del par, ordenadas por
            (periodo efectivo actual ASC, APRTCDGO ASC),
          numeradas 1..K

ASIGNAR   fila i  →  cupo i
SOBRANTE  filas con i > N  →  no se mueven, van a revisión (§4.5)
```

Si `cupo(i)` coincide con el mes que la fila ya tenía, **no se toca**: así el script es idempotente
y solo escribe lo que de verdad cambia.

### 4.4 Por qué esto es equivalente a tu ejemplo

Tu ejemplo: huecos en **sep, oct, nov/25**; duplicados en **dic/25** y **feb/26**. Supongamos filas
en jun, jul, ago, dic×2, ene, feb×2 (8 filas, 9 meses de jun/25 a feb/26).

| | jun | jul | ago | sep | oct | nov | dic | ene | feb |
|---|---|---|---|---|---|---|---|---|---|
| **Hoy** | 1 | 1 | 1 | — | — | — | **2** | 1 | **2** |
| **Tu procedimiento** | 1 | 1 | 1 | 1 (de dic) | 1 (de feb) | 1 (de dic) | 1 (de ene) | 1 (de feb) | — |
| **Compactación** | 1 | 1 | 1 | 1 (de dic) | 1 (de dic) | 1 (de ene) | 1 (de feb) | 1 (de feb) | — |

**La cobertura de meses es idéntica**, y en las dos el hueco queda arriba, en feb. Es lo esperable:
las dos conservan la cantidad de filas y producen un bloque contiguo desde el piso.

Lo único en que difieren es **qué fila física cae en qué mes**, o sea **qué importe** queda en cada
mes cuando los importes no son iguales entre sí. La compactación conserva el orden cronológico
relativo (estable); tu enunciado saca primero los excedentes y después cascadea.

> **Decisión pendiente tuya (§6, D8):** si los importes difieren entre filas, ¿importa cuál cae en
> qué mes? La compactación es la más simple de auditar y la más fácil de revertir. Si querés
> exactamente tu orden, se implementa igual — pero decidilo antes, no después de correr el UPDATE.

---

### 4.5 Qué NO resuelve, y hay que mirarlo aparte

- **`SOBRANTE`** — más filas móviles que cupos. Después del paso 1 (depuración del exceso) esto no
  debería pasar; si pasa, es un partícipe con más aportes que meses esperados y **va a revisión
  individual, no al lote**.
- **El importe del mes destino** no tiene por qué coincidir con lo esperado de ese mes: el sueldo
  pudo cambiar. Una fila movida a sep/25 con el importe de dic/25 deja ese mes **completo o
  incompleto por diferencia**, y la generación cobrará el faltante — que es el comportamiento
  correcto, pero conviene saberlo antes de que aparezca en el archivo del mes.

---

## 5. Qué se escribe y qué no

| Campo | Acción | Por qué |
|---|---|---|
| `APRTPRDV` | ✅ **UPDATE** al primer día del mes destino | Es el campo que define a qué mes pertenece (§1) |
| `APRTFCTR` | ⛔ **no se toca** | Fecha de caja: cuándo entró la plata. Es un hecho |
| `APRTGLSA` | ✅ UPDATE — ver el formato exacto abajo | Trazabilidad del movimiento |
| `APRTIDAS` / `CRARCDGO` | ⛔ no se tocan | De qué carga vino el dinero sigue siendo verdad |
| `CRD.PGAP` | ⛔ **no se toca ninguna fila** | El pago ocurrió cuando ocurrió. Mover el aporte de mes de devengo no mueve el momento en que se cobró |
| `APRTVLRR` / `APRTVLPG` / `APRTSLDO` / `APRTIDST` | ⛔ no se tocan | La reubicación no es una corrección de importes |

### ⛔ El formato de la glosa — y una trampa que nos haríamos a nosotros mismos

La glosa actual es:

```
Aporte jubilación - Mes 12/2025 - CargaArchivo: 352
```

La tentación es **agregar la traza al final**. **No se puede.** El script `69` extrae el id de carga
con `REGEXP_SUBSTR(..., 'CargaArchivo: ([0-9]+)\s*$', ...)`, **anclado al final del texto**:
cualquier cosa después del número **rompe el análisis y no avisa** — la fila desaparece de todas
las consultas agrupadas por carga.

**Formato correcto — la traza va ANTES de `- CargaArchivo:`:**

```
Aporte jubilación - Mes 9/2025 (reubicado desde 12/2025) - CargaArchivo: 352
```

Así siguen funcionando **las dos** cosas: el `LIKE 'Aporte % - Mes %/% - CargaArchivo: %'` que
clasifica la fila como V3, y el regex anclado que extrae el `352`. Y de paso el marcador
`(reubicado desde` hace el UPDATE **idempotente**: una fila ya reubicada se reconoce y no se vuelve
a mover.

Para las filas de junio 2025, cuya glosa vieja no tiene ` - Mes `, se conserva tal cual y solo se
inserta la traza antes de `- CargaArchivo:`. **No se les cambia el patrón**: es la firma por la que
se las identifica.

---

## 6. Decisiones abiertas que agrega este algoritmo

| # | Decisión | Recomendación |
|---|---|---|
| **D7** | ¿Se reubica solo a los partícipes cuyo dinero cuadra, o a todos? | **Solo a los que cuadran** (§2). Los demás pasan primero por la depuración del exceso |
| **D8** | Si los importes difieren, ¿qué fila cae en qué mes? | Compactación estable (§4.4). Simple de auditar y de revertir |
| **D9** | ¿Se reubica también a partícipes **sin** duplicados pero **con** huecos? | **No.** No hay fila que mover: ese mes está genuinamente impago y lo cobra la generación |
| **D10** | El techo del rango: ¿última carga procesada o mes en curso? | **Última carga procesada.** El mes en curso todavía no se cobró |

---

## 7. Verificación antes de ejecutar

`03_PROPUESTA_REUBICACION_DRY_RUN.sql` (esta carpeta) calcula **la lista completa de movimientos
propuestos sin escribir nada**: por partícipe y tipo, qué fila se mueve, de qué mes a qué mes, y por
qué. Esa salida es lo que se revisa; recién con ella aprobada se escribe el `04_` con el `UPDATE`,
su tabla de respaldo, sus controles antes/después y su bloque de reverso comentado.

**Controles obligatorios del `04_`, cuando se escriba:**

1. `SUM(APRTVLRR)` por partícipe **antes = después**. La reubicación no puede mover ni un centavo.
2. Ningún mes del rango con más de una fila móvil por tipo, después.
3. Ninguna fila con `APRTFCTR` distinta de antes.
4. Cantidad de filas movidas = cantidad de movimientos propuestos por el dry run.
