# Nota de venta manual: el combo de producto muestra la cuenta contable y se lee bien

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-13 por `omen-saa-2-arb`, antes de despachar.
**Pedido del usuario (2026-10-13):** al escoger el producto en el ingreso de notas de venta, que salga el
número de cuenta contable, y que el desplegable sea mucho más amigable (hoy es angosto y corta los nombres).

## Lo verificado en el código

- Pantalla: saaFE `modules/cxp/forms/procesos/nota-venta-compra-manual/`. El combo es un `mat-autocomplete`
  sin `panelWidth`, así que toma el ancho de la celda (`.col-producto { min-width: 220px }`), y cada
  opción muestra solo `nombre (codigo)`.
- La cuenta que usa el backend al registrar es **`producto.grupoProducto.planCuenta`**
  (`FacturaCompraServiceImpl.registrarManual`, §2c ~l.390): si el grupo es POR_CLASIFICAR o no tiene cuenta,
  la nota se rechaza con un bloqueante. Es la misma regla de `PagoProgramadoServiceImpl.cuentaDelProducto`.
- `ProductoPago` (`PGS.PRDP`) → `GrupoProductoPago` (`PGS.GRPP`, `PLNNCDGO`) → `PlanCuenta`
  (`cuentaContable`, `nombre`). Son `@ManyToOne` y la entidad se serializa entera, así que
  `/prdp/getAll` ya trae la cuenta. **No hace falta tocar el backend.**

## Lo que cambia (solo FRONTEND)

1. **Cada opción en dos líneas:** arriba el nombre del producto y su código; abajo, en gris,
   `Cta. {cuentaContable} — {nombre de la cuenta}`. Si el producto no tiene grupo, su grupo es
   POR_CLASIFICAR, o el grupo no tiene cuenta: abajo, en color de advertencia, «Sin cuenta contable: no se
   podrá registrar». No se oculta: la usuaria tiene que ver por qué no le sirve.
2. **Panel ancho y legible:** ancho de por lo menos 520 px (o el de la celda, el que sea mayor), opciones de
   altura automática con texto que hace salto de línea en vez de cortarse, y alto máximo del panel de
   unas 8 opciones con scroll.
3. **La búsqueda también encuentra por cuenta:** el texto tipeado se compara además con el número y el
   nombre de la cuenta contable (p. ej. «5.1.02» o «servicios básicos»).
4. **Después de escoger**, debajo del campo de la fila queda visible la cuenta elegida (`Cta. 5.1.02.03`),
   para que se vea en la grilla sin volver a abrir el combo.

No cambia: el contrato con el backend, el payload de `registrarManual` ni las validaciones.
