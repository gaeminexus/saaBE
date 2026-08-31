# Corrección de aportes duplicados — cargas Petro desde junio 2025

**Carpeta creada:** 2026-08-31 · **Equipo:** `omen-saa-2` · **Árbitro:** `omen-saa-2-arb`
**Estado del frente:** diagnóstico. **Todavía no se corrigió ni un dato.**

> **Este archivo es el punto de entrada del frente.** Lo que estaba disperso entre un documento de
> análisis y once encabezados de `.sql` se resume acá, con lo que se verificó contra el código el
> 2026-08-31.

---

## 0. Cómo se numera acá, y por qué no seguimos `crd/sql/`

**Los scripts de esta carpeta llevan su propia serie: `01_`, `02_`, …**

En `docs/logica-negocio/crd/sql/` los números son un **recurso compartido entre equipos** y ya
chocaron dos veces (ver `REGISTRO-RESERVAS-EQUIPOS.md` §2b). El acuerdo de rangos que salió de ahí
—A: 96-149, B: 150-199— reparte esa carpeta, no ésta. Una carpeta propia con su propia serie
elimina el problema en vez de administrarlo: **nadie más escribe acá**, así que el número no
necesita reserva.

⚠️ **Eso no exime del orden global.** Los scripts de acá se intercalan con los de `crd/sql/`; el
orden real de ejecución está en §4 y hay que respetarlo.

---

## 1. Qué se está corrigiendo, en una frase

Desde junio de 2025 las cargas de descuentos de Petrocomercial dejaron en `CRD.APRT` filas que
**no representan dinero que haya entrado**, o que representan **dinero ya representado por otra
fila**. Como el saldo de aportes del partícipe es `SUM(APRTVLRR)` sin ningún filtro (modelo vigente
desde el 2026-08-14), cada una de esas filas **le infla el saldo a una persona real**.

### La cadena de verdad

| Tabla | Qué dice |
|---|---|
| `CRD.CXPG` | lo que se **pidió** cobrar |
| `CRD.PXCA` (producto `AH`) | lo que la empresa **descontó** — **el dinero** |
| `CRD.PGAP` | cada **aplicación** de ese dinero a un aporte |
| `CRD.APRT` | las filas que **quedaron** |

**La regla de oro:** por partícipe, `SUM(APRT.APRTVLRR)` de las filas de carga debe ser igual a
`SUM(PXCA.PXCADSDO)` del producto `AH` de las cargas procesadas. La diferencia es el exceso.

---

## 2. La historia, corta

| Cuándo | Qué pasó |
|---|---|
| 2025-06 | La **carga 352** descuenta **$162.004,30 a 2.021 partícipes**… y no crea **ni una** fila de aporte. Se procesó el 2026-03-30 y el generador de aportes nació el 2026-04-02: el código todavía no sabía crear aportes |
| 2026-04-02 → hoy | Cuatro versiones del generador (V1, V2, V3 y el modelo nuevo). **Cada una deja filas con forma distinta**, y por eso una sola consulta no las ve todas |
| 2026-08-24/26 | Primeros scripts de detección (58, 59) y el análisis (61) + `ANALISIS-APORTES-DUPLICADOS-PETRO.md` |
| 2026-08-27 | El **62** corre en producción con `SET APRTVLRR = NVL(APRTVLPG, 0)` y **pone en cero 2.635 filas de junio 2025, $160.350,81**. Un `NVL` sobre una columna nullable convirtió *"no sé"* en *"cero"* |
| 2026-08-27 | Se escribe el **74** para restaurar ese daño, y el **69** (V2 del análisis) con el universo corregido |

### El hallazgo que explica por qué costó tanto verlo

El 61 definía el universo con `APRTUSRG = 'SAA_AH'`. Las 2.635 filas de junio 2025 tienen
**usuario NULL**, glosa vieja (`Aporte jubilacion - CargaArchivo: 352`, **sin** ` - Mes `), **sin**
`APRTIDAS`, **sin** `APRTFCRG` y `APRTFCTR` a medianoche en vez de 23:59:59. El 61 nunca las vio. Y
después el 62 las puso en `valor = 0`, así que el filtro `APRTVLRR > 0` las excluyó **una segunda
vez, por una causa completamente distinta**.

> **La regla que quedó, y vale para cualquier consulta futura sobre esta tabla:** una fila es "de la
> carga" **por su FORMA** (`APRTIDAS`, o el patrón de la glosa), **nunca por el campo usuario** —
> ese campo puede faltar.

---

## 3. Lo verificado contra el código el 2026-08-31

El documento de análisis abre diciendo *«No se ha ejecutado ninguna consulta contra la base
todavía»* y lista ocho correcciones de código pendientes. **Cinco meses de eso ya no es cierto.**
Verificado archivo por archivo:

| §8 del análisis | Realidad al 2026-08-31 |
|---|---|
| 1. La fase 3 no rechaza una carga ya procesada | ✅ **HECHO** — `CargaArchivoPetroServiceImpl.java:951-956` lanza `IncomeException` si `estado == 3` |
| 2. Migrar la carga al modelo `SUM(valor)` | ✅ **HECHO** — `crearNuevoAporte` (`:3748-3750`) escribe `valor = valorPagado = monto`, `saldo = 0`, estado `PAGADA`. **El FIFO ya no existe** |
| 3. `selectMinAporteConSaldo` | 🟡 parcial — existe, marcado `@deprecated` (`AporteDaoServiceImpl.java:291-295`), la carga ya no lo usa |
| 4. `DELETE /crar/{id}` sin guarda | ❌ **SIGUE IGUAL** — `CargaArchivoRest.java:122-133` borra sin mirar `APRT` ni `PGAP` (mecanismo M7, filas huérfanas) |
| 5. Actualizar `REGLAS-CARGA-PETRO.md` §3.6 | ✅ hecho — línea 322 ya dice que desapareció el FIFO |

### Consecuencia directa: **la decisión D1 ya está tomada por los hechos**

El análisis dejaba D1 abierta (*"¿qué hacer con las filas PARCIAL?"*) y recomendaba *"migrar la
carga y aplicar la opción (a)"*. **La carga ya está migrada.** Entonces:

- La fuente **dejó de ensuciarse cada mes**. Lo que hay es un pasivo acotado, no una gotera.
- La opción (a) —`valor = valorPagado`, `saldo = 0`, `estado = 4`— es la única coherente con el
  modelo vigente. **D1 no se decide: se da por decidida y se anota.**

---

## 4. Orden de ejecución

Los scripts de esta carpeta se intercalan con los de `crd/sql/`. **Este es el orden, no el número.**

| # | Script | Qué hace | Estado |
|---|---|---|---|
| 1 | **`01_DIAGNOSTICO_PRECONDICIONES.sql`** (acá) | Responde los dos bloqueantes y mide la exposición de los otros equipos. **Solo lectura** | ▶️ **listo para correr** |
| 2 | `crd/sql/74_RESTAURACION_VALOR_APORTES_ANULADOS.sql` | Devuelve su valor a las 2.635 filas que el 62 puso en cero | ❓ **no consta que haya corrido** |
| 3 | `crd/sql/69_ANALISIS_DUPLICADOS_APORTES_V2.sql` | El análisis completo. **Su bloque 0.0 se niega a servir si el 74 no corrió** | ❓ no consta |
| 4 | *(por escribir)* `02_CORRECCION_*.sql` | La corrección. **No se escribe hasta tener los resultados de 1 y 3 y cerradas D3 y D4** | ⬜ |

**Contexto de referencia, no se ejecutan de nuevo:** `61` (superado por el 69), `62` (versión
actual, ya corregida), `65`/`66` (reconstrucción de junio 2025 — el **66** es el de producción),
`63` (backfill de devengo), `67`/`68` (comparación de reportes G44), `75` (siete roles sin entidad
y el caso Fiallos Pacheco), `78` (backfill de `CRARCDGO`, `NO EJECUTADO`).

---

## 5. ⛔ Impacto en los otros dos equipos — verificado, no supuesto

**Este frente toca datos que otros equipos leen hoy.** El usuario coordina cualquier cambio de
código con el equipo A; los cambios de **datos** también les llegan.

### Al EQUIPO A, de lleno

El saldo de aportes es `SUM(APRTVLRR)`. Lo leen, entre otros:

| Archivo | Dueño (§4 del registro) |
|---|---|
| `AporteServiceImpl` | **EQUIPO A** |
| `DevolucionAporteServiceImpl` | **EQUIPO A** |
| `ProcesoPagoPrestamoServiceImpl` | **EQUIPO A** |
| `PagoPensionComplementariaServiceImpl` | **EQUIPO A** — jubilados, commiteado el 2026-08-31 |
| `CobroPetroContableServiceImpl`, `CierreCarteraServiceImpl` | equipo A (contabilidad) |

⚠️ **El riesgo con nombre propio: jubilados.** El proceso de jubilación traslada el remanente del
partícipe a pensión complementaria (tipo de aporte **23**) y lo paga. **Si se jubila a alguien con
el saldo inflado, se entrega dinero que esa persona no aportó** — y eso ya no lo arregla una
corrección de datos. Es la decisión **D4** del análisis, que hasta ahora era teórica y hoy tiene un
proceso vivo detrás.

✅ **Lo que juega a favor: el flag contable de CRD (rubro 237) está en 0.** No hay ni un asiento
emitido por esto. Corregir **ahora** es corregir datos; corregir **después** de encender el flag es
corregir datos *y* reversar asientos. **La ventana es ésta.**

### Al EQUIPO B, de refilón

El mecanismo M1 (carga procesada dos veces) también **reaplicó pagos de préstamo**:
`CargaArchivoPetroServiceImpl:3204-3221` escribe en `CRD.PGPR`. Y quien reconstruye el saldo de la
cuota desde `PGPR` incluye a **`SimulacionPrestamoServiceImpl`, que es del equipo B**, además de
`MotorPagoPrestamoServiceImpl` y `AbonoCapitalPrestamoServiceImpl`.

### ⛔ Trampa verificada si se toca `PGPR`

Los formatos **no son iguales** y el análisis no lo dice:

| Tabla | Cómo termina el texto |
|---|---|
| `PGAP.PGAPCNCP` | `... - CargaArchivo: 352` |
| `PGPR.PGPROBSR` | `... [CargaArchivo: 352]` ← **con corchete de cierre** |

El script 69 usa `REGEXP_SUBSTR(..., 'CargaArchivo: ([0-9]+)\s*$', ...)`, **anclado al final**.
Sobre `PGAP` funciona. **Copiado tal cual a `PGPR` devuelve cero filas y no avisa.** Es el mismo
fallo silencioso que invalidó el 61 entero.

### Archivo sin dueño declarado

`CargaArchivoPetroServiceImpl` **no figura en el §4 del registro de reservas** y es donde caen las
correcciones de código pendientes. Lo tocaron los tres frentes en las últimas dos semanas.

---

## 6. Decisiones abiertas

| # | Decisión | Estado |
|---|---|---|
| **D1** | Qué hacer con las filas PARCIAL | ✅ **cerrada por los hechos** — la carga ya migró, va la opción (a). Ver §3 |
| **D2** | Retiro físico de la fila vs. contra-movimiento | 🟡 el análisis **recomienda retiro físico con respaldo**: estas filas nunca fueron un hecho económico, y un negativo se leería como una liquidación que no ocurrió (G43 liquida cesantes leyendo los negativos del mes). Falta confirmarlo |
| **D3** | Alcance temporal: ¿todas las cargas o solo desde junio 2025? | 🔴 **abierta.** Recomendado: ver el total una vez (el 69 §3 lo da sin filtro) y recién ahí acotar |
| **D4** | Partícipes que quedarían en saldo **negativo** al retirarles el exceso porque ya lo usaron | 🔴 **abierta, y ahora urgente por jubilados.** ¿Se les reconoce o se les cobra? Es decisión de negocio |
| **D5** | ¿Se toca `PGPR` (pagos de préstamo duplicados) en esta vuelta, o solo `APRT`? | 🔴 abierta. Solo `APRT` no toca nada del equipo B y es la mitad del trabajo; incluir `PGPR` cierra el problema completo |
| **D6** | Fiallos Pacheco (cédula 0603715772): $154,85 registrados en junio 2025 contra $49,16 descontados | 🟡 ¿ajuste deliberado o error? El `75` solo consulta |

---

## 7. Reglas de decisión fila por fila

Vigentes, del documento de análisis §5. Se repiten acá porque son lo que va a ejecutar la
corrección:

| Regla | Qué es | Qué hacer |
|---|---|---|
| **R1** | Fila creada por una **segunda corrida** de la fase 3 | Retirar la fila y sus `PGAP`; recalcular las filas de la primera corrida que recibieron `PGAP` de la segunda |
| **R2** | Fila **V1 reemplazada** por filas V3 de la misma carga | Retirar — el dinero está en las V3 |
| **R3** | Fila **V1 única** | **Conservar**, ajustando `valor` al descontado si está sobre/subvalorada |
| **R4** | Fila **PARCIAL con saldo** | Opción (a): `valor = valorPagado`, `saldo = 0`, `estado = 4` (D1 cerrada) |
| **R5** | Fila positiva **sin ningún `PGAP`** | Retirar |
| **R6** | Fila **pagada completa** | **No tocar**, aunque haya varias en el mismo mes (cobro de mora: es legítimo) |

> ⛔ **Nunca se tocan:** filas con `APRTVLRR < 0` (pagos con aportes, devoluciones, jubilación), ni
> glosas `REVERSO …` / `PAGO PRESTAMO …` / `DEVOLUCION …`, ni ninguna fila con usuario real (alta
> manual de pantalla).

---

## 8. Documentos relacionados

- `../ANALISIS-APORTES-DUPLICADOS-PETRO.md` — el marco: versiones del generador, mecanismos M1-M8,
  reglas R1-R6. **Vigente en lo conceptual; su §2.1 y su §8 quedaron viejos — ver §3 de acá.**
- `../../petro/REGLAS-CARGA-PETRO.md` §3.6 — cómo escribe aportes la carga **hoy**.
- `../PLAN-APORTES-DEVENGO-CONTRATOS.md` D11 — el alcance de datos "junio 2025 en adelante".
- `../../REGISTRO-RESERVAS-EQUIPOS.md` §4 — dueños de archivo.
- `../../ESTADO-EQUIPO-OMEN-2.md` — estado general de este equipo.
