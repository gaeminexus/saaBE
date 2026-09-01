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

**~~Pendiente que apareció de paso: el backfill `78` no corrió.~~ FALSO — corregido el 2026-08-31.**
De 981.377 filas con `APRTIDAS`, solo 29.677 tienen `CRARCDGO`… **y ese es el resultado correcto.**

El `UPDATE` del `78` lleva `AND EXISTS (SELECT 1 FROM CRD.CRAR c WHERE c.CRARCDGO = a.APRTIDAS)`,
obligatorio porque `DDL-TRAZABILIDAD-CARGA-PETRO.sql:145` crea `FK_APRT_CRAR`: una fila cuyo
`APRTIDAS` no es una carga real **no puede** recibir el valor. Las 951.700 restantes son las de la
migración (§10.4). **Que sigan en NULL es el script funcionando, no faltando.**

La prueba está en el §10.3: los cuatro grupos con `APRTIDAS ∈ CRAR` tienen **todos** `CRARCDGO`
lleno, y suman **29.677 exacto** — el mismo número. Cero pendientes.

> **La lección, y es sobre mí:** conté filas que "deberían" tener un valor sin leer antes el `WHERE`
> del script que se lo pone. El número era correcto y la conclusión falsa. Alcanzó para que el
> árbitro del equipo A estuviera por desmarcar un ✅ correcto en `ESTADO-CRD.md`. **Un conteo no es
> una conclusión hasta que se sabe qué se esperaba contar.**

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

## 11. ▶ EL FRENTE, MEDIDO — `03` y `04` corridos el 2026-08-31

### 11.1 La cifra que cambia todo: 2.018 de 2.045 partícipes cuadran EXACTO

`03` §1, por **partícipe** (no por carga):

| Grupo | Partícipes | Filas | Registrado | Descontado | Diferencia |
|---|---|---|---|---|---|
| **Cuadra exacto** | **2.018** | 32.106 | 1.792.592,92 | 1.792.592,92 | **0,00** |
| Saldo **inflado** | **16** | 144 | 11.633,08 | 8.800,09 | **+2.832,99** |
| Registrado de **menos** | 11 | 64 | 4.447,72 | 12.739,95 | −8.292,23 |

> **El exceso real de todo el frente son 16 partícipes y $2.832,99.** No los $41.061,53 de la carga
> 354, ni nada parecido.

### 11.2 Por qué la carga 354 parecía tener $41.061,53 de más, y no los tiene

`04` A.1 y A.3 descartan los dos mecanismos que se sospechaban:

- **Una sola ejecución.** Todas las filas de la 354 se registraron en **una sola hora**:
  2026-04-09 21:03 → 21:12. **M1 (fase 3 corrida dos veces) queda descartado.**
- **Una sola versión.** Las 3.107 filas son **todas V3**, todas con devengo (`SIN_DEVENGO = 0`):
  1.053 de tipo 9 y 2.054 de tipo 11. **M3 (V1 conviviendo con V3) queda descartado.**

Lo que sí muestra: **2.054 filas de cesantía para 1.599 partícipes** — unos 455 con más de una fila.
Es la firma de **M6**: gente en mora a la que se le cobraron varios meses de golpe, y el generador
creó **una fila por mes de devengo**. Filas legítimas, dinero real.

> ### La comparación por carga del §9.3 era una comparación inválida
>
> Confronta **filas atribuidas a una carga por su glosa** contra **dinero descontado en esa carga**.
> Cuando una carga cobra atrasos, crea filas que llevan **su** id pero cubren **meses de otro
> período**. Los dos lados de la resta no hablan del mismo hecho.
>
> **La comparación válida es por partícipe** (§11.1), porque el saldo le pertenece a una persona, no
> a una carga. Y ahí el 98,7% cierra en cero.

**Detalle a mirar aparte:** la 354 (agosto) escribió sus aportes a las **21:03**, *después* de que
las cargas 355 a 366 (sept/2025 a mar/2026) se procesaran entre las 12:52 y las 16:18 **del mismo
día**. Se procesó fuera de orden.

### 11.3 La carga 448 tampoco perdió plata

`04` B.1: partícipes con descuento y **sin ninguna fila** de aporte en la 448: **dos**, por **$94,14**
en total. `04` B.3 confirma que `AH` es el único producto de aportes de esa carga ($120.657,06).

Con el usuario **`SAA_JUL_FIN`** del §10.5 (84 filas, 51 partícipes, $36.036,31, todas del
2026-07-31), la explicación se sostiene: **el dinero se registró a mano, fuera del patrón de la
carga**. No falta; no lo ve ninguna consulta que filtre por carga.

### 11.4 Los huecos, y la duda que abren

`03` §5:

| Tipo | Meses esperados | Con aporte | **Huecos** | Partícipes |
|---|---|---|---|---|
| 9 (jubilación) | 10.466 | 10.349 | **117** | 748 |
| 11 (cesantía) | 19.842 | 19.308 | **534** | 1.418 |

**651 meses hueco.** Pero los partícipes con grilla de meses (748 y 1.418) son **muchos menos que
los 2.045 que tienen filas de carga**, y el bloque de SOBRANTES del `03` devolvió "muchísimos"
registros cuando debería ser una anomalía rara.

> ⚠️ **Eso apunta a que los CUPOS están subcontando**, no a que sobren aportes. La grilla sale de
> `CRD.CNTR` + `CRD.VGCN`, migrados desde `CRD.HSTR`: a quien le falte el contrato ACTIVO, la
> vigencia, o tenga `VGCNMNTO` en 0, le dan **cero cupos** y **todas** sus filas salen sobrantes.
>
> **Hasta resolverlo, la propuesta del `03` §2 no se puede aplicar:** estaría compactando contra una
> grilla incompleta y dejando partícipes fuera del alcance en silencio.
> Lo mide **`06_DIAGNOSTICO_CUPOS.sql`**, que devuelve pocas filas a propósito.

---

## 12. ▶ LOS CUPOS — `06` corrido el 2026-08-31. Era un problema de contratos

### 12.1 La sospecha del §11.4 era correcta

| | Partícipes |
|---|---|
| Con filas móviles | **2.044** |
| Con **contrato ACTIVO** | **1.640** |
| Con alguna vigencia | 1.640 |
| Con vigencia útil (`VGCNMNTO > 0`) | 1.640 |

**Los tres últimos números son idénticos: el dato de vigencias está impecable.** El bloque 5 lo
confirma — **todas** las vigencias caen en la categoría *"sirve para la grilla"*, ninguna en
"contrato no activo", "vigencia no activa" ni "monto en cero". Arrancan todas el **2025-06-01** y
están **abiertas**. Mi filtro `VGCNMNTO > 0` no excluye a nadie.

**El problema es uno solo y es anterior: 404 partícipes no tienen contrato ACTIVO en `CRD.CNTR`.**

### 12.2 Los "muchísimos sobrantes" eran eso, no aportes de más

| Situación | Pares (entidad,tipo) | Filas | Cupos | Sobrantes | Valor |
|---|---|---|---|---|---|
| **1. Sin ningún cupo** — falta contrato | **530** | 1.972 | 0 | 1.972 | **$132.782,01** |
| **2. Con cupos, pero cortos** — candidato real | **46** | 674 | 620 | **54** | $38.527,58 |
| 3. Alcanzan los cupos | 2.110 | 28.812 | 29.439 | 0 | $1.581.598,23 |

Y el sobrante de los candidatos reales es **de a una fila**: 39 pares con 1, 6 con 2, 1 con 3.
**43 partícipes en total.**

> **El frente de reubicación, medido de punta a punta:** ~300 pares con algo que mover, **651 meses
> hueco**, **54 filas sobrantes reales** y **16 partícipes con saldo inflado por $2.832,99**. Eso es
> todo. El resto de lo que parecía problema era la grilla faltante.

### 12.3 ⛔ Y esto es lo grave, y no es de este frente

Los 404 sin contrato **sí venían recibiendo sus aportes**: el bloque 4 los muestra con **14 y 15
filas**, cubriendo **06/2025 → 07/2026 completo**, sin un hueco. Son aportantes activos con el
registro de contrato faltante, no gente que dejó de aportar.

Ahora seguí el camino en el código, y ahí está el problema:

| Paso | Verificado |
|---|---|
| `esperadoPorEntidad` sin contrato ACTIVO | **devuelve `0.0`** — `VigenciaContratoServiceImpl:222-229` |
| `distribuirAportePorDevengo`, por cada mes y tipo | `if (esperado <= 0.0) continue;` — `:3600-3602` |
| Resultado con esperado siempre 0 | **no crea ni una fila**, `disponible` nunca baja, el bucle se va por el tope de 60 meses y sale por `System.err`: *"Disponible sin aplicar: $X"* — `:3620-3625` |

> **Cuando el devengo por `CRD.VGCN` corra en producción, esos 404 partícipes dejan de recibir
> aportes y su dinero se descarta con una advertencia en el log.** No falla la carga, no hay
> excepción, no hay fila: solo una línea en `stderr` que nadie mira.
>
> Sus filas actuales (06/2025→07/2026) las creó el camino anterior, que sacaba el esperado de
> `HistorialSueldo` y no de la vigencia. Por eso hasta hoy no se notó.

**Este árbitro no puede verificar si ese código ya está desplegado** — `ESTADO-CRD.md` daba el WAR
en despliegue el 2026-08-31. Si todavía no salió, **hay tiempo de crear los 404 contratos antes de
la próxima carga**. Si ya salió, **la carga de agosto ya descartó ese dinero**.

Avisado al árbitro del equipo A el 2026-08-31: `CRD.CNTR`, la carga Petro y el frente de
contratos-vigencias son suyos.

### 12.4 Qué significa para la reubicación

- **Aplicable ya:** los **1.640 partícipes con grilla**. Ahí los cupos alcanzan (28.812 filas contra
  29.439 cupos) y la compactación tiene contra qué compactar.
- **En espera:** los **404 sin contrato**, con **1.972 filas y $132.782,01**. No se les puede
  reubicar nada: sin grilla no hay mes destino. **Y no se les inventa una** — fabricar la grilla
  desde su propio historial de aportes sería deducir el contrato de los pagos, que es exactamente
  al revés de como debe leerse.

---

## 13. ▶ QUÉ TIENE `APRTPRDV` — `07` corrido el 2026-08-31

**Todo lo que hay en el campo lo escribió el backfill `63`.** Ninguna carga corrió con devengo
(confirmado por el usuario), así que no hay un solo valor puesto por la aplicación.

### 13.1 El reparto

| Desplazamiento (devengo − mes de caja) | Filas | Valor |
|---|---|---|
| **0** — regla 1, copia del mes de caja | **31.343** | 1.745.671,61 |
| **−1 a −15** — regla 2, movidas hacia atrás | **971** | ~63.163 |
| positivo | **0** | — |

Ningún desplazamiento positivo, como se esperaba: el `63` solo mira hacia atrás.

### 13.2 ⛔ 854 filas quedaron con devengo ANTERIOR al piso — y eso es un doble cobro pendiente

| | |
|---|---|
| Filas con `APRTPRDV < 2025-06-01` | **854** |
| Partícipes | **744** |
| Valor | **$55.765,33** |
| Rango de devengo asignado | **2024-11-01 → 2025-05-01** |

Y el cruce contra la vigencia (bloque 3) lo agrava, porque **todas las vigencias arrancan el
2025-06-01** (medido en el `06` §5):

| Situación | Filas | Partícipes | Valor |
|---|---|---|---|
| Devengo **fuera de toda vigencia** | **774** | 668 | **$50.238,32** |
| Sin contrato ACTIVO — no evaluable | 2.066 | 528 | 139.072,15 |
| Devengo dentro de la vigencia | 29.474 | — | 1.619.524,15 |

> **El `63` asignó pagos a meses en los que el partícipe no tenía ninguna obligación.** No es un
> matiz de fechas: `sumValorPorEntidadTipoYRangoDevengo` filtra
> `HAVING periodo BETWEEN 2025-06-01 AND mesCarga`, así que **esas 854 filas son invisibles para el
> cálculo del faltante**. El dinero está en el saldo (`SUM(APRTVLRR)` las cuenta), pero **no cubre
> ningún mes en alcance**.
>
> **Consecuencia: la primera carga con devengo les vuelve a cobrar esos meses a 744 partícipes.**

### 13.3 Los casos, con nombre

| Partícipe | Carga | Mes de caja | Devengo asignado | Desplazamiento |
|---|---|---|---|---|
| AGUILAR VALENCIA VICTOR HUGO | 427 | 06/2026 | **03/2025, 04/2025, 05/2025** | −15, −14, −13 |
| CAIZA GAVILANES BAYRUN MARCELO | 363 | 01/2026 | **11/2024, 12/2024, 01/2025, 02/2025** | −14 a −11 |
| TORRES ERAZO MAURICIO ALEXANDER | 360 | 11/2025 | 11/2024, 12/2024 | −12, −11 |

Leído en castellano: **CAIZA pagó cuatro meses atrasados en enero de 2026, y el backfill los fechó
entre noviembre de 2024 y febrero de 2025** — meses anteriores a que existiera la obligación. Los
meses que sí debía (2025-06 en adelante) siguen figurando impagos.

Todos los casos extremos son **tipo 11 (cesantía)**.

### 13.4 Qué cambia esto para el frente

**La reubicación deja de ser una mejora y pasa a ser la corrección de un defecto con monto.**

- **No es una primera pasada:** el `63` ya repartió, sin grilla y sin piso. Esta es la segunda, con
  las dos restricciones que le faltaban.
- **La compactación de este frente resuelve las 854**, porque solo asigna meses **dentro de la
  vigencia** y **desde 2025-06**. Es exactamente lo que el `63` no podía hacer.
- **Tiene fecha límite:** antes de la primera carga con devengo. Después, esa carga empieza a cobrar
  de nuevo los meses que las 854 filas dejaron descubiertos.

⚠️ **El `63` es del frente de devengo (equipo A).** Avisado su árbitro el 2026-08-31: el defecto es
de su script, la corrección cae en este frente, y conviene que la revisen antes de aplicarla.

---

## 14. Estado al cierre del 2026-08-31 — el `08` está listo y en espera

### 14.1 Confirmado: no hay nada que reconstruir

**Ninguna carga corrió con devengo.** Cuatro vías independientes lo dicen:

1. **Ni un `"Disponible sin aplicar"` en el log de WildFly** — verificado por el usuario del equipo A.
   Es la prueba directa.
2. El respaldo del propio `63` (`BKP_APRT_DEVENGO_20260827`) como corte temporal: ningún aporte
   posterior trae devengo.
3. **Cero desplazamientos positivos** en el `07` §1 — el proceso anticipa hacia adelante, el `63`
   solo mira hacia atrás.
4. El techo de cargas procesadas sigue en **2026-07**, 14 en estado 3.

**Consecuencia: los 404 y las 854 son problemas a prevenir, no daño a reparar.**

### 14.2 El orden acordado, y quién hace qué

| # | Qué | Quién |
|---|---|---|
| 1 | Crear los **404 contratos** faltantes en `CRD.CNTR` | **Equipo A** — es su tabla. Autorizado por su usuario |
| 2 | Correr el **`08`** sobre la grilla ya completa | **Este equipo**, con el visto bueno del usuario |

El `08` está **escrito, pusheado, revisado y aprobado por el árbitro del equipo A**, y **sin
ejecutar**. No se corre hasta que los contratos estén.

### 14.3 ⚠️ El cambio que viene, y su efecto lateral

El equipo A va a corregir el descarte silencioso: **la carga pasará a abortar** en vez de dejar una
línea en `stderr` cuando no pueda distribuir todo el dinero recibido. Es lo correcto —y resulta que
la regla ya estaba decidida en una sesión anterior; el código no la cumplía.

Pero conviene tenerlo escrito, porque cambia la naturaleza de la dependencia:

> **La grilla de contratos pasa de "conviene que esté completa" a "tiene que estarlo, todos los
> meses, o la carga del mes no corre."**

Y el modo de falla que hay que anticipar no son los 404 de hoy —esos se crean— sino **una vigencia
que se cierre**: `esperadoPorEntidad` devuelve 0 cuando no hay vigencia que cubra el mes, sin
distinguir *"falta el dato"* de *"esta persona ya no debe aportar"*. Las dos cosas dejan
`disponible > 0` y, con la regla nueva, **las dos abortan la carga entera**.

El caso concreto: **un partícipe que se jubila** y al que Petro le descuenta un mes más. Hoy es
inofensivo — medido en el `06` §5: **todas las vigencias están abiertas**, ninguna tiene fecha de
fin. El día que se cierre la primera, una sola persona detiene la carga de todos.

**Sugerencia pasada al equipo A:** separar los dos casos antes de que la regla entre. *Falta el
dato* debe abortar; *el partícipe ya no tiene obligación* debería tener su propio camino, o el
proceso mensual queda rehén del mantenimiento del padrón.

### 14.3bis Resuelto — el diseño final ya distingue los cuatro casos

**Corrección a lo escrito arriba** (2026-08-31, informado por el árbitro del equipo A): el abort
**no** es indiscriminado. El cuadro que van a implementar:

| Situación | Tratamiento |
|---|---|
| Sin entidad en el padrón | **ABORTA** |
| Sin contrato ACTIVO | **ABORTA** |
| Sin `HistorialSueldo` / esperado $0 | NOVEDAD |
| **Con contrato, vigencia que no cubre el mes** | **ADVERTENCIA — la carga sigue** |

O sea: **el abort queda solo para cuando no se sabe quién es o si debe aportar**, que es
exactamente la separación que hacía falta. Un partícipe que se jubila y al que Petro le descuenta un
mes más **no detiene la carga**: sale como advertencia visible.

⚠️ **Y eso corrige una afirmación de este documento y de un mensaje entre árbitros:** se dijo que
los 2 partícipes con contrato y sin vigencia de jubilación *"detienen la carga todos los meses"*.
**No la detienen.** Su dinero sigue sin aplicarse —$2.291,04 en 9 meses— que es un problema real,
pero no de disponibilidad del proceso. La urgencia baja; el defecto no desaparece.

---

## 15. ▶ LA SECUENCIA DE EJECUCIÓN, ACORDADA CON EL EQUIPO A

Este es el orden final. **Ningún paso se saltea, y el `08` es el único que escribe.**

| # | Qué | Quién | Estado |
|---|---|---|---|
| 1 | `crd/sql/98_CONTRATOS_FALTANTES_404.sql` — 373 contratos, 486 vigencias | Equipo A | ✅ **corrido** |
| 2 | **`09_PREVUELO_DEL_08.sql`** — solo lectura | Este equipo | ⬜ pendiente del usuario |
| 3 | Si el **bloque B** trae partícipes → el equipo A les crea el contrato | Equipo A | condicional |
| 4 | Si el **bloque C** trae pares que aportan un tipo sin vigencia → el equipo A crea la vigencia | Equipo A | condicional |
| 5 | **`08_REUBICACION_DEVENGO.sql`** — el `UPDATE` | Este equipo | ⬜ bloqueado por 2-4 |
| 6 | Revisar controles 3.1, 3.2, 3.3, 3.3b, 3.5, 3.6 → `COMMIT` o `ROLLBACK` | Usuario | ⬜ |

### Por qué los pasos 3 y 4 son condicionales y no opcionales

Salieron de una aclaración que cambió la prioridad del equipo A: **el `08` usa la vigencia solo
para saber en qué MESES se esperaba aporte, no cuánto.** El único uso del monto es
`NVL(VGCNMNTO,0) > 0`. De ahí:

- un `HSTR` desactualizado **no afecta** la reubicación — monto equivocado pero positivo, misma grilla;
- un monto en **cero** sí saca el par de la grilla, y por eso **el pendiente abierto del equipo A
  (207 sin vigencia de jubilación, 53 sin cesantía) es un prerrequisito de este frente**, no un
  detalle suyo para después.

**Un partícipe sin grilla no se corrige y, con el abort nuevo, detiene la carga del mes.** Por eso
se cierra antes, no después.

### 15.1 El patrón que se repitió cinco veces hoy

Vale más que cualquiera de los hallazgos sueltos:

> **El nombre y el JavaDoc no son evidencia del contenido.**

| Caso | Decía | Era |
|---|---|---|
| `APRTIDAS` | trazabilidad de carga | eso **y** el id del sistema viejo — dos significados |
| `HSTRPRJB` | *"Porcentaje Jubilación"* | un **período** (`202606`) |
| `PGPR.PGPROBSR` | mismo formato que `PGAP` | termina en `]` — el regex anclado ve **cero** filas |
| Backfill `78` | ✅ corrido, en `ESTADO-CRD.md` | ✅ correcto — **la falsa era mi lectura del conteo** |
| `mvn` en `CLAUDE.md` | no está en el PATH | depende de la máquina |

Cuatro de los cinco se detectaron **midiendo**, no leyendo. El de `HSTRPRJB` lo detectó Oracle al
rechazar el valor por precisión: **en una columna `NUMBER` a secas habría entrado sin un error.**

---

## 16. ✅ EL `08` CORRIÓ Y SE COMMITEÓ — 2026-09-01

Ejecutado por el usuario en producción, desde otra máquina, y **commiteado**.

### 16.1 Los controles

| Control | Resultado | Qué prueba |
|---|---|---|
| **`3.1`** | **0** | Ninguna fila con `APRTVLRR` o `APRTFCTR` distintos del respaldo. **No se movió ni un centavo ni una fecha de caja** — el invariante que le importaba al equipo A |
| **`3.2`** | **0** | Ningún partícipe con saldo distinto |
| **`3.5`** | **0** | Ningún mes con más de una fila móvil: la compactación no creó colisiones |
| **`3.3b`** | **8 filas / 6 partícipes** | Ver §16.2 |
| `3.3`, `3.4` | *pendientes de registrar* | Cuántas quedaron bajo el piso y cuántas se movieron |

### 16.2 Las 8 que quedaron, y por qué no eran motivo para revertir

`AGUILAR VALENCIA` (3 filas), `BRITO MALDONADO`, `MUÑOZ VILLALTA`, `SUAREZ BUSTOS`, `ERAZO ROMAN`
y `CAIZA GAVILANES`. Tienen contrato y vigencia útil, así que no caían en ninguna de las dos
exclusiones declaradas: **el control 3.3b hizo exactamente aquello para lo que se agregó.**

> **El dato que decidió: el `MERGE` no las tocó.** Estaban bajo el piso antes del `08` y siguen
> igual. **Commitear o revertir no tenía ningún efecto sobre ellas** — y revertir habría descartado
> además todo lo que sí se corrigió.

**Y son sobrantes, por construcción del propio `MERGE`.** El join es `c.RN = m.RN`: fila *i* al cupo
*i*. Con **N** filas y **M** cupos, si `N ≤ M` **todas** reciben cupo; una fila solo queda sin
colocar cuando `N > M`. Entonces que hayan quedado 8 sin mover **demuestra** que esos 6 tienen más
filas móviles que meses esperados — y que los M cupos se llenaron todos, o sea que **no les quedó
ningún mes en alcance sin cubrir**.

Su dinero es **pago de atrasos anteriores a 2025-06**, meses que el motor no cobra nunca
(`ALCANCE_MINIMO_DEVENGO`). **No hay doble cobro en ellas.** Lo confirma
`10_DIAGNOSTICO_LAS_8_DEL_3_3B.sql` §2, que debe devolver 0 filas.

### 16.3 Se llegó antes de la fecha límite

El plazo real de este frente nunca fue una fecha: era **la primera carga que se procese con
devengo**. Desde esa carga, `distribuirAportePorDevengo` empieza a consumir los huecos con plata
nueva y el reparto histórico pasa a ser un blanco móvil.

**Agosto no se había procesado todavía.** La reubicación entró antes. Las 854 filas dejaron de ser
un doble cobro pendiente sobre 744 partícipes.

⚠️ **NO BORRAR `CRD.BKP_APRT_DEVENGO_20260831`.** Es lo que permite revertir con el bloque 4 del
`08` después del `COMMIT`. Se elimina cuando el usuario dé la corrección por buena.

---

## 8. Documentos relacionados

- `../ANALISIS-APORTES-DUPLICADOS-PETRO.md` — el marco: versiones del generador, mecanismos M1-M8,
  reglas R1-R6. **Vigente en lo conceptual; su §2.1 y su §8 quedaron viejos — ver §3 de acá.**
- `../../petro/REGLAS-CARGA-PETRO.md` §3.6 — cómo escribe aportes la carga **hoy**.
- `../PLAN-APORTES-DEVENGO-CONTRATOS.md` D11 — el alcance de datos "junio 2025 en adelante".
- `../../REGISTRO-RESERVAS-EQUIPOS.md` §4 — dueños de archivo.
- `../../ESTADO-EQUIPO-OMEN-2.md` — estado general de este equipo.
