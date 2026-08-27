# Análisis — aportes duplicados generados por la carga Petro

> Escrito el 2026-08-26 a partir del código fuente y del historial de git. **No se ha ejecutado
> ninguna consulta contra la base todavía**: este documento dice qué buscar, por qué, y cómo
> decidir con los resultados en la mano. Las consultas están en
> `sql/61_ANALISIS_APORTES_DUPLICADOS_PETRO.sql` (solo lectura).
>
> Antecedentes inmediatos: `sql/58_APORTES_DUPLICADOS_MES.sql` (detección por fecha) y
> `sql/59_MESES_SIN_APORTE_DUPLICADOS.sql` (meses faltantes). Este análisis los supera: en vez
> de mirar sólo `APRT`, contrasta cada fila con el dinero que realmente entró.

---

## 1. La pregunta y por qué no se responde mirando sólo APRT

La pregunta es *qué filas de `CRD.APRT` sobran*. Una fila de aporte sobra cuando **no representa
dinero que haya entrado**, o cuando representa dinero **que ya está representado por otra fila**.
Eso no se ve en `APRT`: dos filas iguales pueden ser un duplicado o un partícipe que pagó dos
meses de golpe porque estaba en mora. La única forma de distinguirlas es seguir la plata.

La cadena de verdad, por partícipe y carga, es:

| Tabla | Qué dice | Enlace |
|---|---|---|
| `CXPG` (generación) | lo que se **pidió** cobrar, separado en tipo 9 y 11 | `GNAP` por periodo + filial → `PDGA` por entidad |
| `PXCA` (carga, producto `AH`) | lo que la empresa **descontó** — el dinero | `PXCA.PXCACDPT` = `ENTD.ENTDRLPC` (rol Petro) |
| `PGAP` | cada **aplicación** de ese dinero a un aporte | `PGAPCNCP` termina en `CargaArchivo: N` (no hay FK) |
| `APRT` | las filas que **quedaron** | `APRTIDAS` = id de carga (desde 2026-04-09); antes, sólo en la glosa |

La regla de oro del análisis: **para cada partícipe, `SUM(APRT.valor)` de las filas que creó la
carga debe ser igual a `SUM(PXCA.totalDescontado)` del producto `AH` en las cargas procesadas.**
Cualquier diferencia positiva es el exceso a retirar. La consulta A6 calcula exactamente eso.

---

## 2. Línea de tiempo — qué escribió cada versión del generador

Reconstruida con `git log -S` sobre `CargaArchivoPetroServiceImpl`. Importa porque las filas de
cada versión tienen **forma distinta** y hay que buscarlas de forma distinta.

| Versión | Vigencia (commits) | Cómo quedaba la fila en `APRT` |
|---|---|---|
| **V0** | 2025-11-24 → 2026-04-02 | La carga **no creaba aportes**. |
| **V1** | 2026-04-02 (`60b8258`) → 2026-04-09 | Una fila por tipo. `valor` = monto de `HistorialSueldo` (**no** el dinero descontado; si difería más de $1 sólo dejaba un log). `fechaTransaccion = now()` (fecha de proceso, no fin de mes). Glosa `Aporte jubilación - CargaArchivo: N`. **Sin `APRTIDAS`, sin usuario, sin `valorPagado`/`saldo`, sin `PGAP`.** |
| **V2** | 2026-04-09 (`07f2079`) → 2026-04-11 | Aparecen `SAA_AH`, `APRTIDAS`, fecha = fin de mes, FIFO sobre `valorPagado`/`saldo`, `PGAP`. El sobrante de un mes creaba una fila del mes siguiente con glosa `Abono al aporte …` (`valor` = esperado, `valorPagado` = sobrante). |
| **V3** | 2026-04-11 (`d30f70e`) → **hoy** | `procesarAporteUnicoTipo` / `procesarAportesAlternados`: FIFO sobre filas PARCIAL y luego un bucle que crea filas de `valor = esperado` hasta agotar el dinero. La última puede quedar PARCIAL. Glosa `Aporte X - Mes m/aaaa - CargaArchivo: N`. |
| **Modelo nuevo de saldo** | 2026-08-14 (`5525c1e`, `570cef2`) | `SaldoAporteService`: el saldo del partícipe es **`SUM(APRTVLRR)`**, sin ningún filtro. Pagos con aportes y devoluciones insertan filas **negativas**. `valorPagado`/`saldo` quedan como "mecánica del FIFO" (§7.2 de la especificación). |

### 2.1 Una discrepancia que hay que resolver antes de corregir nada

Se dijo que el modelo de aportes cambió en junio de 2026 y que la carga se adaptó en julio.
**El repositorio no muestra eso.** En `HEAD` (2026-08-26):

- El modelo `SUM(valor)` entró el **2026-08-14**, no en junio.
- `CargaArchivoPetroServiceImpl` **sigue escribiendo `valor = esperado`, `valorPagado`, `saldo`
  y usando el FIFO** (`crearNuevoAporte` :3639, `aplicarPagoAAporte` :3475,
  `selectMinAporteConSaldo`). No hay ningún commit de julio que la migre. Los commits de agosto
  (`61511e4`, `a890d81`) tocaron la mora del partícipe, no la escritura de aportes.

Esto no es un detalle: **mientras la carga siga con FIFO, cada mes va a dejar filas PARCIAL con
`valor > valorPagado`, y cada una de esas filas infla `SUM(valor)`.** La corrección de datos se
puede hacer igual, pero se vuelve a ensuciar en la siguiente carga. Ver §6, decisión D1.

Si el código desplegado no es el del repositorio, hay que decirlo antes de seguir.

---

## 3. Los mecanismos que pueden haber producido duplicados

Cada uno tiene una huella distinta en los datos y una consulta que la busca. **No sé cuál de
ellos ocurrió; las consultas lo dicen.**

### M1 — La fase 3 se ejecutó dos veces sobre la misma carga ← el sospechoso principal

`aplicarPagosArchivoPetro` (:906) **no verifica que la carga no esté ya en estado 3**. Su único
control de orden es `validarOrdenProcesamiento` (:2951), que exige que la carga sea el mes
siguiente a la última procesada… **y excluye explícitamente a la propia carga de esa comparación**
(:2958-2960: *"No validar contra sí misma"*). Consecuencia: la **última** carga procesada se puede
volver a procesar cuantas veces se quiera, y cada vez:

1. el FIFO encuentra las filas PARCIAL de la ejecución anterior y las paga (PGAP nuevo sobre fila
   vieja), y
2. con el resto crea filas nuevas, con el mismo `APRTIDAS`, la misma `fechaTransaccion` y usuario
   `SAA_AH`.

La huella: para esa carga, `SUM(PGAP)` ≈ 2 × `SUM(PXCA descontado)`, y las `fechaRegistro` de sus
filas se agrupan en dos bloques separados por horas o días. **Consulta A0** (`EJECUCIONES`,
`RATIO_PGAP_ARCHIVO`) y **A4**. La misma repetición también aplicó dos veces los pagos de
préstamos de esa carga; eso está fuera de este análisis pero hay que revisarlo (§7).

### M2 — Dos cargas distintas para el mismo periodo

El mismo archivo cargado dos veces (dos `CRAR` con igual mes/año/filial) y las dos procesadas.
El orden cronológico debería impedirlo (la segunda no es "mes siguiente"), salvo que la primera
haya quedado en otro estado. Huella: filas con `APRTIDAS` distinto para el mismo mes. **A1.**

### M3 — Filas V1 que conviven con filas V3 de la misma carga

Las cargas procesadas entre el 2 y el 9 de abril dejaron filas V1. Si después se volvieron a
procesar con V2/V3 (para tener `PGAP`, fecha de fin de mes, FIFO), las filas V1 **siguen ahí**:
no tienen `APRTIDAS` ni `SAA_AH`, su fecha es la de proceso (abril 2026), así que **ni el script
58 ni ningún filtro por fin de mes las ve, pero `SUM(valor)` las cuenta**. Y su `valor` es el
esperado de `HistorialSueldo`, no el dinero. **A2 y A2b.**

### M4 — Filas V2 de excedente

Dos días de vigencia. Las filas `Abono al aporte …` nacen con `valor = esperado` y
`valorPagado = sobrante`: son legítimas como registro de dinero, pero **sobrevaloradas** en
`valor − valorPagado` bajo el modelo nuevo, salvo que una carga posterior las haya completado por
FIFO. **A2** las cuenta; **A7** las clasifica fila a fila.

### M5 — Filas PARCIAL vigentes del FIFO (no es duplicado, pero infla igual)

Es el efecto de §2.1. Cada fila con `estado = 6` y `saldo > 0.01` aporta `saldo` de más a
`SUM(valor)`. No hay dinero duplicado; hay una fila "esperada" que el modelo nuevo lee como
"aportada". **A6** (`SALDO_FIFO`), **A7** (`PARCIAL CON SALDO`).

### M6 — Varias filas legítimas en el mismo mes (cobro de mora)

Desde `a890d81` la generación cobra de golpe todos los meses adeudados de un partícipe en mora.
La carga recibe N × esperado y el bucle de V3 crea N filas, **todas fechadas el último día del mes
de carga**, todas pagadas completas. Se ven como duplicados en el script 58 y **no lo son**.
Huella: N filas, una sola ejecución, `SUM(valorPagado)` = descontado. En **A5** salen como `OK`.

### M7 — Filas huérfanas

`CargaArchivoRest` tiene `DELETE /crar/{id}` y `APRTIDAS` no tiene FK. Si se borró una carga y se
volvió a cargar, las filas viejas apuntan a un id que ya no existe. **A3a.**

### M8 — Filas creadas y nunca pagadas

En V3 la fila nace y se paga en la misma transacción; una fila `SAA_AH` positiva sin ningún
`PGAP` no debería existir. Si existe, es un fantasma. **A3b.**

---

## 4. Cómo leer los resultados — orden sugerido

1. **A0** primero. Da la lista de cargas con cuándo se ejecutó cada una y cuántas veces. Si
   alguna tiene `EJECUCIONES > 1` o `RATIO_PGAP_ARCHIVO ≈ 2`, ya está identificado el mecanismo
   dominante (M1) y las cargas afectadas. `PRIMERA_EJECUCION < 2026-04-09` marca las cargas que
   pasaron por V1.
2. **A2 / A2b.** Cuántas filas hay de cada versión. Si `V1 ABRIL 2-9` tiene filas y A2b devuelve
   cargas, M3 está confirmado.
3. **A1, A3a, A3b.** Los tres deberían dar 0 filas. Lo que salga es caso aparte.
4. **A6.** La cifra: por partícipe, cuánto sobra en su saldo por culpa de la carga
   (`EXCESO_TOTAL`), descompuesto en `EXCESO_DINERO` (plata contada dos veces → M1/M2) y
   `SALDO_FIFO` (filas no completadas → M4/M5). Los partícipes con `EXCESO_TOTAL ≈ 0` no tienen
   nada que corregir aunque hayan salido en el script 58.
5. **A7.** Fila a fila, para los partícipes de A6, con la clasificación de §5. Es la lista de
   trabajo.
6. **A7b.** Los `PGAP` de una segunda ejecución que cayeron sobre filas de la primera. Son los
   que complican el retiro: al quitarlos hay que recalcular la fila que los recibió.
7. **A8.** Partícipes cuyo saldo quedaría **negativo** al retirar el exceso — usaron saldo inflado
   en un pago con aportes o una devolución desde el 2026-08-14. Van a revisión individual, no al
   lote.

Un detalle de las consultas: `PXCA` no separa jubilación de cesantía (el producto `AH` viene
sumado). Por eso A6 compara el **total** por partícipe, y el reparto por tipo sale de `PGAP`.

---

## 5. Reglas de decisión, fila por fila

Aplican sobre la columna `CLASIFICACION` de A7.

| Regla | Clasificación | Qué es | Qué hacer |
|---|---|---|---|
| **R1** | `EJECUCION REPETIDA` | Fila creada por una segunda corrida de la fase 3 (M1) | **Retirar la fila y sus `PGAP`.** Además, retirar los `PGAP` de esa ejecución que cayeron en filas de la primera (A7b) y recalcular en esas filas `valorPagado = SUM(PGAP restantes)`, `saldo = valor − valorPagado`, `estado` (4 si saldo ≤ 0.01, 6 si no). |
| **R2** | `V1 REEMPLAZADA` | Fila del generador de abril 2-9 cuya carga también tiene filas V3 (M3) | **Retirar.** El dinero está representado en las V3. |
| **R3** | `V1 UNICA` | Fila V1 sin reemplazo | **Conservar**, pero su `valor` es el esperado y no el descuento: si en A5 el partícipe/carga sale `SOBREVALORADO` o `SUBVALORADO`, **ajustar `valor` al descontado**. |
| **R4** | `PARCIAL CON SALDO` | Fila V2/V3 con `valor > valorPagado` (M4/M5). No es duplicado | Ver **decisión D1**. Opción (a): `valor = valorPagado`, `saldo = 0`, `estado = 4` — coherente con `SUM(valor)`. Opción (b): dejarla como deuda del FIFO. |
| **R5** | `SIN PAGO` | Fila positiva sin `PGAP` (M8) | **Retirar.** |
| **R6** | `PAGADA COMPLETA` | `valor = SUM(PGAP)` | **No tocar.** Aunque haya varias en el mismo mes (M6). |
| — | `REVISAR` | No encaja en ninguna | Caso a mano. |

**Nunca se tocan**: filas con `valor < 0` (pagos con aportes, devoluciones), glosas
`REVERSO …`, `PAGO PRESTAMO …`, `DEVOLUCION …`, ni ninguna fila con usuario real (aportes
manuales de pantalla). Las consultas ya las excluyen; la corrección debe excluirlas también.

---

## 6. Decisiones que hay que tomar antes de escribir la corrección

**D1 — Qué hacer con las PARCIAL (R4), y depende de la carga.**
Si la carga se migra al modelo nuevo (crear cada fila con `valor = dinero recibido`, `saldo = 0`,
`estado = 4`, sin FIFO), la opción (a) es la correcta y de una vez: el saldo queda exacto y no se
vuelve a ensuciar. Si la carga se queda con FIFO, la opción (a) cambia lo que hace la carga
siguiente (ya no encuentra deuda y crea filas nuevas por todo el descuento — que bajo
`SUM(valor)` también es correcto), pero cada mes va a dejar una PARCIAL nueva y habrá que repetir
el recorte. **Recomendación: migrar la carga y aplicar (a).** La migración es un cambio pequeño
en `crearNuevoAporte` + `aplicarPagoAAporte` + eliminar el FIFO; no está hecha.

**D2 — Retiro físico o contra-movimiento.**
La especificación de pagos (§ anulación, línea ~740) trata `APRT` como *append-only para los
reportes* e inserta contra-movimientos en vez de borrar. Aquí **recomiendo el retiro físico con
respaldo**, por dos razones:
- Estas filas **nunca fueron un hecho económico**. Un contra-movimiento negativo sí lo parece:
  G43 liquida cesantes *leyendo los negativos del mes*; una fila negativa fechada hoy (o en el mes
  afectado) se leería como una liquidación que no ocurrió.
- El padrón y la evaluación de mora cuentan filas con `valor > 0` por mes; un negativo no anula
  esa señal, un borrado sí deja el mes con la fila correcta.
El costo: los reportes ya emitidos de esos meses no van a coincidir con una regeneración. Eso ya es
así — se emitieron con datos inflados.

**D3 — Alcance temporal.** Todo lo anterior mira todas las cargas. Si sólo interesan las de mayo
de 2025 en adelante, se filtra por `CRAR.CRARANAF/CRARMSAF` en A0 y por `APRTFCTR` en el resto;
pero conviene ver el total una vez antes de acotar.

**D4 — Los partícipes de A8.** Si alguno usó saldo inflado en un pago con aportes o una
devolución, retirar el exceso lo deja en negativo. Hay que decidir si se le reconoce (se deja la
fila) o se le cobra (se retira igual y queda deuda). No es una decisión de datos.

---

## 7. Lo que este análisis no cubre y hay que mirar aparte

- **Préstamos en las cargas repetidas (M1).** La segunda corrida también volvió a aplicar los
  pagos de préstamos. `calcularSaldosRealesCuota` lee `PGPR`, así que una cuota ya PAGADA manda
  el dinero como excedente a la siguiente cuota: el partícipe aparece adelantado en su tabla de
  amortización sin haber pagado. Mismo diagnóstico, otra tabla (`PGPR` por `observación`
  `CargaArchivo: N`).
- **La mora del partícipe** (`ACTIVO_EN_MORA`) y **el cobro de meses adeudados en la generación**
  usan `APRT` con `valor > 0` por mes y la última fecha de aporte. Retirar filas puede cambiar la
  evaluación de un partícipe en la próxima generación. Efecto esperado y correcto, pero visible.
- **Contabilidad.** La carga no genera asientos por aportes (`PGAP.numeroAsiento` queda nulo en
  `crearRegistroPagoAporte`). El retiro no toca `CNT`.

---

## 8. Correcciones de código que este análisis deja pendientes

No se han hecho; se listan para que no se pierdan.

1. `aplicarPagosArchivoPetro` debe **rechazar una carga en estado 3** antes de hacer nada. Hoy
   la última carga procesada se puede reprocesar sin ningún aviso. Es la causa raíz de M1 si M1
   se confirma.
2. Migrar la escritura de aportes de la carga al modelo `SUM(valor)` (D1).
3. `selectMinAporteConSaldo` dice "PENDIENTE o PARCIAL" en el comentario pero sólo filtra
   `estado = 6`. Hoy no importa (las filas nacen y se pagan juntas); si se migra la carga, el
   método desaparece.
4. `APRTIDAS` sin FK a `CRAR` (M7). Al menos, el `DELETE /crar/{id}` debería negarse si hay
   filas de `APRT` o `PGAP` que la referencian.
5. Actualizar `REGLAS-CARGA-PETRO.md` §3.6 cuando se haga (1) y (2); hoy describe el FIFO como
   vigente, y lo es.
