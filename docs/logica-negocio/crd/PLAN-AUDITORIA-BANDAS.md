# Pantalla de auditoría de distribución en bandas

**Fecha:** 2026-09-02 · **Equipo:** CRD / Equipo B · **Estado:** plan aprobado, pendiente de implementar

> **Pedido del usuario, 2026-09-02:** *«una pantalla ultramoderna e intuitiva que me permita ver el
> resumen de una carga y el detalle de cómo se distribuyeron los valores a cada banda contable. Es
> para que contabilidad pueda revisar por qué se mandan esos saldos a las cuentas contables […]
> También debe darme ese detalle por aportes. Debe ser ultradinámica: filtrar por tipo de préstamo,
> aportes, fechas, partícipes, bandas, cuentas contables, etc.»*
>
> **Ampliación, 2026-09-02:** *«la pantalla debe permitirme ver el detalle de cualquier distribución
> en bandas»* — no sólo la carga Petro.
>
> **Restricción de negocio, 2026-09-02:** *«en un momento vamos a desconectar contabilidad de crd
> para poder vender el sistema aparte»*.

---

## 1. La restricción de la venta separada decide el diseño, y no cuesta nada

**No hace falta una segunda versión del sistema.** Con una sola decisión de diseño, la pantalla
funciona igual con contabilidad conectada o desconectada. Y esa decisión hay que tomarla **ahora**,
porque tomarla después sí obliga a rehacer.

### ⛔ Dónde se clasifica hoy — y por qué es el problema

Hoy la clasificación en bandas ocurre **dentro de la contabilización**:
`CobroPetroContableServiceImpl:720` arma un `Map<String, LineaBandaAcumulada>` en memoria mientras
construye el asiento, y ese mapa **muere cuando el método termina**. Nadie lo persiste.

Eso tiene dos consecuencias, y la segunda es la que importa para la venta:

1. Contabilidad no puede auditar nada: el dato no existe después del asiento.
2. **Si `contabilidadActiva()` está en `false`, no se clasifica nada en absoluto.** El día que se
   desconecte contabilidad, una pantalla alimentada desde la contabilización queda **vacía**.

### ✅ La decisión: se persiste donde se APLICA el pago, no donde se arma el asiento

La banda **no es un dato contable**: es un dato de **cartera**. «Esta cuota vence en 45 días» o
«esta cuota está vencida hace 200 días» es verdad exista o no un asiento detrás. Contabilidad
**consume** esa clasificación para elegir una cuenta; no la produce.

Por eso la tabla nueva se escribe en el momento de aplicar el pago —donde ya se conoce la cuota, su
vencimiento y la fecha de pago— y guarda el hecho **en términos de CRD**:

| Se guarda (CRD) | NO se guarda (CNT) |
|---|---|
| producto, tipo de préstamo, tipo de aporte | número de cuenta contable |
| banda, etiqueta, tipo de cartera, días | plantilla, línea de plantilla |
| partícipe, préstamo, cuota, concepto, valor | — |
| origen del hecho y su id | — |

**El enganche con contabilidad es una sola columna anulable: `idAsiento`.** Con contabilidad
conectada se llena; desconectada queda en null y no pasa nada. La cuenta contable **no se copia**:
se resuelve al consultar, uniendo con la configuración de bandas (`BandaProducto` ya tiene
`idPlanCuenta` y `cuentaContable`). Sin CNT, la pantalla pierde **una columna**, no la función.

> Esa es la respuesta a la pregunta del usuario: **no hace falta armar otra versión.** La versión
> "sin contabilidad" es esta misma con una columna vacía.

---

## 2. Transversal desde el día uno, que sale al mismo precio

La clasificación por bandas no es exclusiva de Petro: la usan también el cobro individual (CBCRASN2)
y el abono a capital, todos vía `ClasificadorBandaService.clasificar` — que ya es el único punto por
donde deben pasar.

Si la tabla lleva **origen + id de origen**, la carga Petro es **un filtro más**, no la estructura de
la pantalla:

| Origen | Id |
|---|---|
| `CARGA_PETRO` | `CRD.CRAR` |
| `COBRO_INDIVIDUAL` | `CRD.CBCR` |
| `EVENTO_PRESTAMO` | `CRD.EVPR` (abono a capital, precancelación) |
| `PAGO_PENSION` | `CRD.PGPC` (cuando cierre ese frente) |

Colgarla de la carga cuesta lo mismo hoy y obliga a rehacer tabla y pantalla la primera vez que
contabilidad pregunte por un cobro individual.

---

## 3. ⛔ Agrupar por CONCEPTO, no por cuenta

**Aclaración del usuario, 2026-09-02:** la **mora se manda a la misma cuenta contable que el interés
ordinario**, y lo único que las distingue es la descripción de la línea.

Entonces: si la pantalla agrupa **por cuenta contable**, mora e interés ordinario **se fusionan en
una sola fila** y el desglose desaparece exactamente donde contabilidad lo necesita. El agrupador
primario es el **concepto**; la cuenta es un dato más de la fila.

Conceptos a distinguir, que son los que el asiento ya separa: capital (por banda), interés ordinario,
interés de mora, interés vencido, seguro de desgravamen, seguro de incendio, y aportes por tipo.

---

## 4. El cuadre va primero — decisión del usuario

El usuario ya eligió: **el cuadre se muestra antes que el detalle**. La pantalla abre respondiendo
«¿esto cuadra?» y sólo después deja explorar.

Encabezado, para el origen seleccionado:

| | |
|---|---|
| Recibido | lo que entró |
| Distribuido | suma de la tabla de bandas |
| **Diferencia** | **la cifra que motivó todo esto** |
| Asiento(s) | número y estado, o «contabilidad desconectada» |

Una diferencia distinta de cero se muestra **en rojo y arriba**, no escondida en un total al pie.
Es literalmente el problema que se pasó el día persiguiendo: $2.906,52 que nadie veía.

---

## 5. Qué construir

### 5.1 Backend

1. **Tabla nueva** en `CRD`, con su DDL en `sql/174` (control antes y después, reverso comentado).
2. **Escritura en el punto de aplicación**, no en la contabilización. Idempotente por
   (origen, idOrigen): reprocesar una carga **reemplaza** sus filas, no las duplica — la 449 se
   reprocesó tres veces en un día.
3. **Endpoints de consulta**: resumen de cuadre por origen, y detalle filtrable. Contrato en
   `API-AUDITORIA-BANDAS.md`.
4. La contabilización sigue armando el asiento como hoy. **No se le cambia la lógica**: sólo se le
   pasa el `idAsiento` a las filas ya escritas.

### 5.2 Frontend

Pantalla nueva bajo `crd/forms/`. Cuadre arriba, detalle abajo, filtros por origen, fechas, producto,
tipo de préstamo, tipo de aporte, partícipe, banda, concepto y cuenta. Exportable.

---

## 6. Qué NO se toca

- **`ClasificadorBandaService`** y la configuración de bandas: se leen, no se modifican.
- **La lógica de los asientos.** Esta pantalla audita lo que ya ocurre; no cambia ni un valor.
- **CNT.** Sólo se lee la cuenta al consultar, y de forma que su ausencia no rompa nada.
- **Los procesos de aplicación de pago**, salvo la línea que escribe la fila de auditoría.

---

## 7. Verificación

1. `mvn -q compile`.
2. Sobre la carga 449 reprocesada: `Distribuido` debe igualar al total de los pagos, y la diferencia
   contra lo recibido debe ser **la misma** que reporta `sql/171`. Si la pantalla dice otra cosa que
   el SQL, **la pantalla está mal** — el SQL ya está verificado.
3. Reprocesar dos veces la misma carga: la cantidad de filas **no cambia**.
4. Con `contabilidadActiva() = false`: la pantalla sigue mostrando la distribución completa, sin
   columna de cuenta y sin asiento. **Es la prueba de la venta separada** y no es opcional.

---

## 8. ⚠️ La transversalidad quedó a medias — cómo se cierra

**Estado real al 2026-09-02**, verificado contra el código: `DsbnOrigen` declara **cuatro** orígenes
y la pantalla los ofrece los cuatro, pero **el único que tiene alguien escribiendo filas es
`CARGA_PETRO`** (`CobroPetroContableServiceImpl`). Por los otros tres la pantalla devuelve vacío
siempre, hagan los procesos lo que hagan.

**Es un hueco del despacho, no del código.** El §2 pide transversalidad desde el día uno, y quedó
cumplida en la estructura de la tabla y en la pantalla — pero el despacho al agente puso todo el
énfasis en Petro y las escrituras de los otros tres nunca se pidieron. Lo detectó el usuario al
filtrar por «Cobro individual» y no ver nada.

### Orden de valor

| # | Origen | `idOrigen` | Dónde ya se clasifica |
|---|---|---|---|
| 1 | `COBRO_INDIVIDUAL` | `CRD.CBCR` | `CobroCreditoServiceImpl` — `haberDesdeEvento` / `haberDesdePagos` |
| 2 | `EVENTO_PRESTAMO` | `CRD.EVPR` | `lineasReclasificacionAbonoCapital` / `lineasBandaCapitalAbono` |
| 3 | `PAGO_PENSION` | `CRD.PGPC` | el pago mensual, cuando hay cruce |

El 1 va primero porque es el que el usuario ya intentó usar.

### ⛔ Reglas, y la tercera es la que se acaba de aprender a los golpes

- La fila se escribe **donde se aplica el pago**, nunca detrás del guardarraíl de `contabilidadActiva`.
- Idempotente por (origen, idOrigen): reprocesar **reemplaza**, no duplica.
- **No clasificar dos veces lo mismo en el mismo proceso.** En Petro esto ya pasó: la escritura
  duplicó las consultas de clasificación que el asiento ya hacía, dentro del proceso de 20+ minutos
  recién estabilizado. Se corrigió compartiendo el resultado (`1073d28`). **En los tres orígenes que
  faltan hay que compartir desde el principio**, no clasificar de nuevo.
- No cambia ni un valor de ningún asiento.

Si en alguno la clasificación **no** está disponible en el punto donde se aplica el pago —y por lo
tanto habría que clasificar de nuevo— **parar y rediseñar dónde se engancha**, en vez de agregar
consultas a un proceso.

### El cuadre de estos tres orígenes es `null`

Y está bien: `recibido` sólo existe hoy para Petro, que tiene las transferencias como fuente
independiente. Un cobro individual no tiene un «recibido» contra el cual contrastarse. Ver el tercer
estado del contrato — **no inventar un `$0,00`**.

---

## 9. Dónde engancha `COBRO_INDIVIDUAL` — y la convergencia que queda pendiente

**El agente BE paró antes de escribir, y bien.** Al investigar encontró que en el cobro individual la
clasificación **no está disponible en el punto donde se aplica el pago**:

- `procesarCobro` aplica vía `MotorPagoPrestamoServiceImpl`, y **el motor nunca llama al
  clasificador**.
- La clasificación ocurre sólo en `lineaBandaCapital` ← `haberDesdePagos` ← `haberDesdeEvento`, y esa
  cadena vive **detrás de `if (contabilidadActiva())`**.

O sea: **hoy, con contabilidad desconectada, el cobro individual no clasifica nada.** Escribir en el
punto de aplicación significa clasificar ahí por primera vez.

### Decisión, 2026-09-02: clasificar en `procesarCobro` y aceptar la duplicación, por ahora

Se clasifica justo después de cada llamada de aplicación —`ResultadoAplicacionPago.cuotasAfectadas`
ya trae `idPagoPrestamo` e `idCuota`, alcanza— y se escribe DSBN ahí. **El asiento sigue clasificando
por su lado**, y esa duplicación **se acepta a propósito**:

- Son operaciones **individuales**, de un puñado de cuotas. La regla de «no clasificar dos veces»
  nació por el lote de Petro, donde eran miles de filas y dos minutos de proceso. Aplicarla acá con
  el mismo rigor no compra rendimiento.
- Evitarla exige cambiar la firma de `haberDesdePagos`, que tiene **cuatro llamadores**
  (`AcuerdoCondonacionServiceImpl`, `CobroCreditoServiceImpl` y `ContabilidadPrestamoServiceImpl` en
  dos puntos, uno de ellos el pago de pensión recién construido). **Es un cambio ancho en un momento
  caliente**, y el beneficio no lo justifica.

⚠️ **Queda anotado como deuda, no como resuelto.** Si algún día el cobro individual pasa a procesarse
en lote, esta duplicación deja de ser barata.

### ⛔ La convergencia real, para cuando el módulo esté tranquilo

El lugar correcto para escribir la distribución **no es ninguno de estos servicios: es el motor**.
`MotorPagoPrestamoServiceImpl` es el punto por donde pasan **todas** las aplicaciones de pago —Petro,
cobro individual, abono a capital, precancelación, pago con aportes, pensión— y es literalmente «donde
se aplica el pago».

Y **puede saber de qué origen se trata sin que nadie se lo diga**: `ContextoPago` ya lleva
`idCargaArchivo`, `idEvento` e `idCobroCredito`.

Eso daría **un solo punto de escritura para los cuatro orígenes**, y de paso cerraría la salvedad
anotada al entregar Petro: hoy la escritura vive dentro de `CobroPetroContableServiceImpl`, así que el
día que se extraiga el módulo contable del despliegue, esa clase se iría llevándose la escritura.

**No se hace ahora**, y la razón es de riesgo, no de diseño: el motor es el código más crítico del
módulo, se migró hoy mismo, y el usuario está reprocesando en producción. Es exactamente el cambio
que no se toca en caliente.
