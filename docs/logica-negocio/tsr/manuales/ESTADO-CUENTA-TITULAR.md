# Estado de cuenta de titular — corrección

**Módulo:** Tesorería · **Fecha:** 2026-08-27 · **Tipo:** corrección de un proceso existente

---

## Qué estaba mal

En el estado de cuenta de un **cliente** no aparecía ninguna factura, aunque el cliente tuviera facturas emitidas y autorizadas. En el de **proveedor** sí aparecían los documentos, lo que hacía parecer que el problema era solo de clientes.

La pantalla descartaba como "anulado" todo documento cuyo estado no fuera `1`. Pero en CxC el estado de una factura es su ciclo de vida ante el SRI —
`1` ingresada, `3` firmada, `4` enviada, **`5` autorizada**, `6` no autorizada — y una factura anulada queda en `0`. Es decir: se ocultaban justamente las facturas válidas y se habrían mostrado las anuladas. En CxP los documentos se graban con estado `1` y nunca cambian, y de ahí venía la diferencia entre los dos estados de cuenta.

Como efecto colateral, tampoco se veían las notas de crédito y débito autorizadas, las retenciones emitidas ni los anticipos confirmados.

## Qué se corrigió

- Cada tipo de documento se evalúa con su propio catálogo de estados: en CxC se ocultan los anulados (`0`) y los no autorizados por el SRI (`6`); en CxP los anulados (`0`); en anticipos los anulados (`3`).
- Se agregó al estado de cuenta de **cliente** la sección de **retenciones recibidas**, que debe considerarse y no se estaba trayendo.
- Se agregó la columna **"Estado doc."**, para poder ver de un vistazo en qué situación está cada documento y por qué aparece o no.

## Cómo verificarlo

1. Entrar a **Tesorería → Procesos → Estado de Cuenta de Titular**.
2. Dejar seleccionada la pestaña **Cliente** (arriba a la derecha) y pulsar **Buscar cliente**.
3. Escribir parte del nombre o la identificación y pulsar **Seleccionar** en la fila del cliente.

El estado de cuenta muestra los totales, los documentos y sus saldos. En la columna **Estado doc.** las facturas válidas aparecen como *Autorizada*; las anuladas ya no se listan ni suman en los totales.

![Estado de cuenta de cliente con la factura autorizada](img/estado-cuenta-cliente-resultado.jpg)

En el ejemplo, el cliente tiene cinco facturas en el sistema: **una autorizada y cuatro anuladas**. La pantalla lista únicamente la autorizada (`001-001-000000795`, $460,00) y el total facturado refleja solo ese documento. Antes de la corrección esta pantalla salía vacía.

Para revisar al mismo titular como proveedor, se usa la pestaña **Proveedor** de la esquina superior derecha.

## Nota conocida

El aviso amarillo *"El estado de cuenta se muestra incompleto"* aparece cuando el titular **no tiene** notas de crédito, notas de débito o anticipos. No indica una falla: el sistema informa "sin registros" de una forma que la pantalla todavía no distingue de un error. Los documentos que sí existen se muestran correctamente. Corrección pendiente en el frontend.
