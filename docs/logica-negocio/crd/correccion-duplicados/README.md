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
| 4 | *(por escribir)* `0X_DEPURACION_EXCESO.sql` | Depuración del exceso (R1-R6). **Va ANTES de reubicar** — ver `02_ALGORITMO` §2 | ⬜ |
| 5 | **`03_PROPUESTA_REUBICACION_DRY_RUN.sql`** (acá) | Lista de movimientos propuestos, **sin escribir nada** | ▶️ listo para correr |
| 6 | *(por escribir)* `04_REUBICACION.sql` | El `UPDATE`, con respaldo, controles y reverso comentado | ⬜ |

**Reubicación de duplicados a los meses huecos** (pedido del usuario, 2026-08-31): el algoritmo y
su validación están en **`02_ALGORITMO_REUBICACION_MESES.md`**. Tres correcciones al enunciado
original que salieron de verificarlo contra el código: se mueve **`APRTPRDV`**, no `APRTFCTR`;
solo aplica a **partícipes cuyo dinero cuadra**; y un mes vacío **no es hueco** si no se esperaba
aporte. Agrega las decisiones **D7-D10** del §6.

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

## 9. ▶ RESULTADOS MEDIDOS — `01` corrido en producción el 2026-08-31

### 9.1 Los tres semáforos: verde

| Control | Resultado |
|---|---|
| `CRD.APRT.CRARCDGO` existe | ✅ sí — no hay riesgo de ORA-00904 |
| ¿Corrió el `74`? | ✅ **sí** — 0 filas pendientes de restaurar. **El `69` ya es válido** |
| Flag contable (rubro 237) | ✅ **apagado (0)** — corregir ahora es solo corregir datos |

**Pendiente que apareció de paso:** el backfill `78` **no corrió**. De 981.377 filas con `APRTIDAS`,
solo **29.677** tienen `CRARCDGO`.

### 9.2 ⛔ El hallazgo que invalida parte del `69`: `APRTIDAS` tiene DOS significados

El bloque 4 devolvió **393.869 filas** de tipos 9/11 con `APRTIDAS` lleno y fechas **desde
1990-01-28**. La primera carga Petro afecta a 2025-06: **esas filas no pueden venir de una carga.**

- **En el código:** el único punto que le escribe un valor es
  `CargaArchivoPetroServiceImpl.crearNuevoAporte:3751`. Todos los demás (`AporteServiceImpl`,
  `DevolucionAporteServiceImpl`, `ProcesoPagoPrestamoServiceImpl`,
  `PagoPensionComplementariaServiceImpl`) le ponen **NULL explícito**. Lo que hay de 1990 a 2025 no
  lo escribió esta aplicación: **lo puso la migración**, con el id del aporte en el sistema viejo.
  Después el código reusó la misma columna para el id de `CargaArchivo`.
- **En los datos:** filas con glosa V3 = **29.674**; filas con `CRARCDGO` lleno = **29.677**.
  Coinciden. Las 393.869 sobran por completo.

> **`APRTIDAS IS NOT NULL` no identifica una fila de carga.** El universo correcto es **la glosa**
> (los tres patrones) **o `CRARCDGO`**, que es la columna gobernada y solo la escribe la carga.
>
> **El `69` sigue usando `APRTIDAS` en su universo** (§3, «exceso por partícipe»). Esas cifras están
> infladas por 393.869 filas históricas. El `03` de esta carpeta ya está corregido; el `69`, no.

**Dos alarmas del `01` que este hallazgo desactiva:**

- **8.439 filas de carga en valor 0** (bloque 2c), fechas **1994-10-28 → 2025-06-09**: todas
  **anteriores a la primera carga**. Son filas históricas migradas, no daño de la carga.
- **El único jubilado con exceso** (bloque 5) tiene `DESCONTADO = 0` y 236 filas: es un falso
  positivo del mismo universo contaminado. De **188** jubilados complementarios, ninguno queda con
  exposición demostrada. **La urgencia de D4 baja** — pero se vuelve a medir con el universo bueno.

### 9.3 El frente es mucho más chico de lo que decía el análisis

Sumando las 14 cargas procesadas (bloque 4b):

| | |
|---|---|
| Descontado por Petro | **$1.819.785,81** |
| Registrado en `CRD.APRT` | **$1.808.673,72** |
| **Neto** | **−$11.112,09** |

**Globalmente no sobra plata: falta.** Y no es un goteo parejo — son **dos eventos localizados**:

| Carga | Periodo | Descontado | Registrado | Diferencia | Filas |
|---|---|---|---|---|---|
| **354** | 2025-08 | 143.084,04 | 184.145,57 | **+41.061,53** | **3.107** (las demás: 2.100-2.300) |
| **448** | 2026-07 | 120.657,06 | 86.299,57 | **−34.357,49** | 2.155 |
| otras 12 | — | — | — | entre −387 y −4.530 | normales |

- **354 es el único exceso real de todo el frente.** Se procesó el **2026-04-08 14:30**, el día
  *anterior* al cambio de generador del 2026-04-09: es el perfil exacto de **M3** (filas V1
  conviviendo con V3 de la misma carga) o **M1** (doble ejecución).
- **448 es un problema distinto y probablemente más urgente:** $34.357,49 descontados a la gente en
  julio de 2026 que **no figuran en ninguna cuenta**. No es duplicación, es dinero sin registrar.

**Consecuencia de alcance:** la depuración del exceso (R1-R6) se reduce prácticamente a **una carga**.
`04_ANALISIS_CARGA_354_Y_448.sql` disecciona las dos.

### 9.4 La trampa del regex, confirmada contra la base

Bloque 6b, sobre 25.242 filas de `CRD.PGPR` con texto de carga:

| Patrón | Filas que ve |
|---|---|
| El anclado que usa el `69` (`'CargaArchivo: ([0-9]+)\s*$'`) | **0** |
| Sin ancla | **25.242** |

No es un riesgo teórico: **el patrón del `69` no ve ni una sola fila de `PGPR`.**

**Dos limitaciones del bloque 6 que hay que saber al leerlo:** `CUOTAS_DISTINTAS` cuenta
`PGPRNMCT` (número de cuota, un entero chico), no cuotas distintas — la columna no sirve; y
`PGPRFCRG` viene **NULL** en estas filas, así que **no se puede detectar una doble ejecución en
`PGPR` por fecha de registro**, como sí se hace en `APRT`.

---

## 10. ▶ EL UNIVERSO, FIJADO — `05` corrido en producción el 2026-08-31

### 10.1 Ningún criterio suelto alcanza. Los tres fallan, y cada uno distinto

| Criterio | Qué se le escapa |
|---|---|
| **`APRTUSRG = 'SAA_AH'`** | **2.635 filas de junio 2025 con usuario NULL** ($160.350,81, 2.001 partícipes) **y 3 filas con usuario `SAA_AH_M`** — glosa impecable (`Aporte Cesantía - Mes 9/2025 - CargaArchivo: 355`), otro usuario |
| **`APRTIDAS` ∈ `CRD.CRAR`** | **las mismas 2.635 de junio 2025** (tienen `APRTIDAS` NULL) más 2 con `SAA_AH` |
| **glosa** | **1 fila de carga con glosa propia**: `Aportes igualados por transferencia de ene/2026 a Jul/2026` ($103,96, carga 360) |

**Los dos tenían razón, cada uno en la mitad.** Tu objeción a la glosa está probada por esa última
fila; mi objeción al usuario, por las 2.635 de junio. **Ningún criterio se descarta: se unen.**

### 10.2 El universo definitivo

```sql
APRTFCTR >= DATE '2025-06-01'
AND (   CRARCDGO IS NOT NULL
     OR APRTIDAS IN (SELECT CRARCDGO FROM CRD.CRAR)
     OR APRTGLSA LIKE 'Aporte %CargaArchivo: %'
     OR APRTGLSA LIKE 'Abono al aporte%')
```

⚠️ **El piso de fecha va ARRIBA de todo, no dentro del paréntesis.** La primera versión lo dejaba
fuera de la rama `CRARCDGO`, y eso colaba **2 filas del 1990-07-28** que tienen `CRARCDGO` lleno
—cosa que además hay que explicar, porque el backfill `78` no corrió—.

### 10.3 La comprobación que cierra el círculo

| | Filas | Valor |
|---|---|---|
| Carga vigente (`S S S S S`) | 29.671 | 1.647.880,51 |
| Junio 2025, solo la glosa las ve | 2.635 | 160.350,81 |
| `SAA_AH_M`, glosa buena | 3 | 155,94 |
| Junio 2025 con `SAA_AH` | 2 | 181,93 |
| Glosa propia, carga 360 | 1 | 103,96 |
| **Universo** | **32.312** | **1.808.673,15** |

El §9.3 midió, por otro camino (join a `CRD.CRAR`, carga por carga), **$1.808.673,72**. La
diferencia es **$0,57**, que es **exactamente** el valor de las 2 filas de 1990 que el piso ahora
excluye. **Las dos mediciones cierran al centavo.**

### 10.4 Lo que queda afuera, y está bien que quede

- **385.035 filas históricas** ($24.569.193,15, hasta 2025-05-31). Su `APRTIDAS` **no coincide con
  ningún código de carga**: el criterio `APRTIDAS ∈ CRAR` ya las rechaza solo. La contaminación del
  §9.2 era `APRTIDAS IS NOT NULL` a secas, no la idea de usar `APRTIDAS`.
- **395 filas manuales** ($62.051,16, 78 partícipes) posteriores al piso: usuarios `SAA_UC`,
  `SAA_UI`, `SAA_JUL_FIN`, `LCALDERON`, `GROBAYO`…, con `APRTIDAS = -99` y glosas tipo
  `CARGA APORTE MES JUN 2025`. **No son de la carga, pero SÍ ocupan su mes**: entran como `FIJA` y
  su mes deja de ser cupo. El algoritmo ya las trata así.

### 10.5 Una pista sobre el faltante de la carga 448

El bloque 3 muestra el usuario **`SAA_JUL_FIN`: 84 filas, 51 partícipes, $36.036,31, todas con
fecha 2026-07-31**. El faltante de la carga 448 (julio 2026) medido en §9.3 es **$34.357,49**.

Mismo mes, mismo orden de magnitud. **La hipótesis a probar es que ese dinero no falta: se
registró a mano, fuera del patrón de la carga**, y por eso ninguna consulta que filtre por carga lo
ve. Lo confirma o lo descarta el bloque B del `04`.

---

## 8. Documentos relacionados

- `../ANALISIS-APORTES-DUPLICADOS-PETRO.md` — el marco: versiones del generador, mecanismos M1-M8,
  reglas R1-R6. **Vigente en lo conceptual; su §2.1 y su §8 quedaron viejos — ver §3 de acá.**
- `../../petro/REGLAS-CARGA-PETRO.md` §3.6 — cómo escribe aportes la carga **hoy**.
- `../PLAN-APORTES-DEVENGO-CONTRATOS.md` D11 — el alcance de datos "junio 2025 en adelante".
- `../../REGISTRO-RESERVAS-EQUIPOS.md` §4 — dueños de archivo.
- `../../ESTADO-EQUIPO-OMEN-2.md` — estado general de este equipo.
