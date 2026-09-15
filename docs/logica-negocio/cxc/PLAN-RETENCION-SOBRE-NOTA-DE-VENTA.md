# PLAN — Emitir una retención sobre una nota de venta

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-15 · **Estado:** diseño en disco, Fase 1 despachable, Fase 2 esperando decisión
**Contrato:** `cxc/API-RETENCION-SOBRE-NOTA-DE-VENTA.md` (espejado a `saaFE/docs/cxc/`)
**Script de medición:** `cxc/sql/e2-43-retencion-sobre-nota-de-venta.sql` (solo lectura)

---

## 0. El pedido, textual

> *«Al emitir retenciones desde cxc, también debe permitirme escoger notas de venta (las ingresadas
> manualmente), no solo facturas.»* — usuario, 2026-09-15

La nota de venta ingresada a mano es una `FacturaCompra` (`PGS.FCTC`) con `tipoComprobante = "02"`
(`FacturaCompraServiceImpl:49`, decisión D5 de `cxp/PLAN-NOTA-VENTA-COMPRA-MANUAL.md` §6). No tiene
clave de acceso ni XML; sí tiene el **número de autorización preimpreso**.

---

## 1. Medido contra el código (2026-09-15)

### 1.1 Por qué hoy no se puede elegir

`retencionesv2.component.ts:238-246`, `tipoCompraDelDocumento()` mapea los tipos a buscar: `01`, `03`,
`04` y `05`. **El `02` no está**: devuelve `null`, el botón de búsqueda no se habilita y la pantalla dice
*«Este tipo no se consulta en el sistema: ingrese los datos manualmente»* (`.html:112-113`).

El selector es `cxp/dialog/factura-compra-selector-dialog`, que ya sabe buscar `FACTURA`, `NOTA_CREDITO`,
`NOTA_DEBITO` y `LIQUIDACION` (`:31`, `:174-190`).

### 1.2 🔴 Lo que no se buscaba: hoy las notas de venta YA salen, mezcladas y declaradas como factura

El modo `FACTURA` del selector consulta `facturaService.selectByCriteria` **filtrando sólo por titular**
(`:129-133`, `:188`). No mira `tipoComprobante`. Así que las notas de venta del proveedor **ya aparecen
en la lista de «Facturas»**, sin nada que las distinga.

Si el usuario elige una desde ahí, el detalle se arma con `tipoDocReten: this.idDocumento.codigo`
(`retencionesv2.component.ts:431`), que vale `"01"`, y el XML declara
`codDocSustento = 01` (`RetencionV2ServiceImpl:392`). **Una nota de venta llega al SRI declarada como
factura.** No da error: sale mal.

**El filtro correcto ya existe en el repositorio**, en la pantalla de solicitud de pago:
`solicitud-pago.component.ts:313-317` — `FACTURA` → `tipoComprobante !== '02'`, `NOTA_VENTA` →
`tipoComprobante === '02'`. Se copia ese criterio, no se inventa otro.

El `e2-43` bloque 5 cuenta si alguna retención ya salió así.

### 1.3 ✅ El backend ya soporta el `02` sin cambios de lógica

| Paso | Dónde | ¿Sirve para nota de venta? |
|---|---|---|
| XML `codDocSustento` | `RetencionV2ServiceImpl:392` — sale de `DetalleRetencionV2.tipoDocReten` | ✅ escribe `02` |
| XML `numDocSustento` | `:393` — el número sin guiones | ✅ 15 dígitos, igual que una factura |
| XML `impuestosDocSustento` | `:402-423` — sin IVA escribe una línea IVA 0% con base = `totalSinImpuestos` | ✅ la nota de venta no lleva IVA (D6) |
| Validación previa (PASO 0.1) | `:1780-1812` → `AplicacionPagoCxpServiceImpl.resolverFacturaCompraPorNumero` (`:1169`) → `AplicacionPagoCxpDaoServiceImpl.selectFacturaByNumero` (`:162-170`): busca en `FacturaCompra` por número normalizado + titular + empresa, **sin filtrar tipo** | ✅ encuentra la `FCTC` tipo `02` |
| Cruce con el saldo | `aplicarRetencionEmitida` (`AplicacionPagoCxpServiceImpl:151-178`) crea la `AplicacionPagoCxp` sobre esa `FacturaCompra` | ✅ el saldo de la nota de venta baja, igual que con el pago del §41 |

**Por eso la Fase 1 no toca Java.** La nota de venta vive en la misma tabla que la factura, y todo el
camino de la retención ya pregunta por la tabla, no por el tipo — la misma razón que hizo gratis el §5
de `PLAN-NOTA-VENTA-COMPRA-MANUAL.md`.

### 1.4 🟠 El RIDE dice «Factura» siempre

`rep/cxc/RPRT_RIDE_RETENCION_V2.jrxml:219-220`: el encabezado de cada documento sustento es un
`staticText` fijo `Factura - `. Ya imprime mal hoy para liquidación (`03`), nota de crédito (`04`) y
nota de débito (`05`); con la nota de venta sería un cuarto caso. El campo `TIPODOCRETEN` ya viene en
la consulta (`:49`, `:168`).

### 1.5 🔴 El ATS enlaza la retención con la compra por AUTORIZACIÓN — y la de una nota de venta no es única

`GeneradorAtsServiceImpl.cargarRetencionesCompra` (`:1075-1135`) agrupa las retenciones por
`DetalleRetencionV2.docResAutorizacion` y cada compra busca la suya con
`retenciones.get(c.autorizacion)` (`:697`).

- En un comprobante **electrónico** la autorización es la clave de acceso de 49 dígitos: **única por
  documento**. El enlace funciona.
- En una **nota de venta preimpresa** la autorización es la del **bloque de talonarios** que el SRI le
  autorizó al proveedor: **la comparten todas sus notas de venta**.

**Consecuencia:** si un proveedor tiene tres notas de venta en el mes con la misma autorización y se le
retiene una, el ATS le pega esa retención **a las tres**. Si se le retienen dos, suma las dos en una y
la repite en las tres. No da error: declara mal.

**Esto no lo crea este frente, lo activa.** Hoy casi no hay retenciones sobre notas de venta porque la
pantalla no deja elegirlas. El `e2-43` bloque 3 mide cuántas notas de venta comparten autorización.

**Arreglo propuesto (Fase 2):** enlazar por **autorización + número del documento sin guiones** en vez de
sólo autorización. Los dos datos ya están en las dos puntas (`DetalleRetencionV2.numDocReten` y
`LineaCompra` establecimiento/punto/secuencial), así que no hay DDL. Para los electrónicos no cambia
nada, porque la clave ya era única.

⛔ **`GeneradorAtsServiceImpl` es del módulo `sri`, que no está en el alcance declarado el 2026-09-15.**
Por eso la Fase 2 **no se despacha** hasta que el usuario diga si `sri` sigue siendo nuestro. Hay tiempo:
el ATS de septiembre se presenta en octubre.

### 1.6 🟡 Número repetido entre factura y nota de venta del mismo proveedor

`selectFacturaByNumero` no filtra tipo. Si un proveedor tuviera una factura y una nota de venta con el
mismo `EEE-PPP-SSSSSSSSS`, la validación previa rechaza con *«Existe más de una factura de compra con
el número…»*. Improbable pero posible. **No se arregla a ciegas:** el `e2-43` bloque 4 lo cuenta. Si da
filas, se agrega el tipo como filtro opcional en `resolverFacturaCompraPorNumero` (servicio compartido
con NC y ND, así que como sobrecarga, sin cambiar la firma existente).

---

## 2. 🟡 Pregunta tributaria que el sistema no decide

Las notas de venta las emiten contribuyentes **RISE / RIMPE negocio popular** (D6). **Si a ese tipo de
contribuyente corresponde retenerle, y qué códigos, lo confirma contabilidad** — no este diseño. El
sistema va a permitir elegir la nota de venta; no va a validar si la retención procede, igual que hoy no
lo valida para una factura.

---

## 3. Fases

### Fase 1 — despachable ya

| Ítem | Quién | Qué |
|---|---|---|
| **FE-1** | `omen-saa-2-fe` | Selector: nuevo tipo `NOTA_VENTA` (misma consulta que `FACTURA` + `tipoComprobante === '02'`). En `FACTURA`, excluir `'02'` **sólo cuando el llamador pasa `tipoDocumento` explícito** — la pantalla legado `pagos-transferencia` (`:343-350`) no lo pasa y queda exactamente como está |
| **FE-2** | `omen-saa-2-fe` | `retencionesv2`: `case '02': return 'NOTA_VENTA'` y los textos que enumeran los tipos |
| **BE-1** | `omen-saa-2-be` | RIDE: el encabezado del documento sustento sale de `TIPODOCRETEN`, no fijo. Compilar y pasar el fill de prueba |

### Fase 2 — espera decisión del usuario (alcance `sri`)

| Ítem | Quién | Qué |
|---|---|---|
| **BE-2** | `omen-saa-2-be` | ATS: enlace retención↔compra por autorización + número (§1.5) |
| **BE-3** | `omen-saa-2-be` | Sólo si el `e2-43` bloque 4 da filas: tipo opcional en `resolverFacturaCompraPorNumero` (§1.6) |

---

## 4. Despliegue

- **Sin DDL.** WAR y FE son independientes entre sí: el FE nuevo llama al mismo `POST /rtv2/procesarCompleta`
  con el mismo cuerpo; el WAR nuevo sólo cambia el `.jasper`.
- ⚠️ **Precondición de datos:** el combo «Doc. que se retiene» sale de `CBR.TSRI` con `LSRI = '3'`
  (`retencionesv2.component.ts:34`, `:769`). **Si ahí no hay un `02` activo, el FE no puede ofrecerlo**
  aunque el código esté listo. Lo mide el `e2-43` bloque 1. Si falta, el alta del catálogo es decisión
  del usuario (catálogo compartido).
- **Mientras la Fase 2 no esté**, cada retención sobre una nota de venta es un renglón que el ATS puede
  declarar mal si esa autorización la comparten otras notas de venta del mismo proveedor en el mes.

## 4bis. Medido con el `e2-43` — 2026-09-15

| Bloque | Resultado | Qué decide |
|---|---|---|
| 1 | `02 NOTA DE VENTA` activo (`CBR.TSRI` id 7) | La precondición del §4 se cumple: no hace falta tocar el catálogo |
| 2 | `01`: 181 documentos, 177.565,27 · `02`: **2**, 117,50 · ningún tipo nulo | El volumen de notas de venta es mínimo hoy |
| 3 / 3b | vacíos | **Ninguna autorización compartida.** La trampa del ATS (§1.5) está latente, no activa. La Fase 2 sigue haciendo falta —la próxima nota de venta del mismo talonario la activa—, pero no es urgente |
| 4 | vacío | **BE-3 descartado**: no hay números repetidos entre factura y nota de venta |
| 5 | vacío | **Ninguna retención salió al SRI declarando `01` sobre una nota de venta.** El defecto del §1.2 no llegó a producir daño |

## 5. Criterio de aceptación — el número que tiene que dar

Con una nota de venta de prueba de total **T** y una retención de valor **R**:

1. El diálogo, en modo «Nota de venta», lista **sólo** filas con `tipoComprobante = '02'`; en modo
   «Factura», **ninguna** con `'02'`.
2. El XML generado trae `<codDocSustento>02</codDocSustento>` y el número de 15 dígitos.
3. El RIDE imprime `Nota de venta - EEE-PPP-SSSSSSSSS`.
4. Aparece una `PGS.APLP` tipo retención apuntando al `ID` de esa `FCTC`, y el saldo de la nota queda en
   **T − R**.

---

## 6. 🔴 Incidente en producción, 2026-09-15 11:11 — la primera retención volvió DEVUELTA

Retención 278 sobre la nota de venta `002-001-0000610` (Ayala Naranjo Juan Pablo, $37,50, código 332 al 0%).
Respuesta del SRI, del log del servidor:

> `cvc-pattern-valid: Value '0020010000610' is not facet-valid with respect to pattern '[0-9]{15}' for type 'numDocSustento'`

**Causa:** la nota de venta se registró a mano con secuencial de **7** dígitos. `POST /fctc/manual`
(`FacturaCompraServiceImpl:434-439`) graba lo que se tipea sin completar ceros, y el XML de la retención
(`RetencionV2ServiceImpl:393`) sólo quita guiones. Una factura electrónica nunca lo muestra porque su
número sale del XML del SRI, siempre 3-3-9. **El §1.3 de este plan decía «✅ 15 dígitos, igual que una
factura»: era cierto para la factura y no se verificó para el documento que se tipea a mano.**

El sistema borró la retención 278 (`eliminarRetencionV2NoEmitida`, nunca llegó al SRI): no queda residuo.

**Salida inmediata (dato):** `cxp/sql/e2-47` completa con ceros `NUMESTABLECIMIENTO`/`NUMPTOEMISION`/`SECUENCIAL`
y `NUMERO` de las notas de venta. Los pagos y cruces apuntan por `ID`, no por número.

**Salida de fondo (código), despachada:**

| Ítem | Quién | Qué |
|---|---|---|
| **BE-4** | `omen-saa-2-be` | `POST /fctc/manual`: sólo dígitos, largo máximo 3/3/9, y completar con ceros **antes** del control de duplicado y del grabado |
| **BE-5** | `omen-saa-2-be` | Retención: validar que cada `numDocSustento` sin guiones tenga 15 dígitos **antes de firmar** (en la validación previa), con mensaje que nombre el documento. Completar ceros si viene en forma `E-P-S` con segmentos cortos y sólo dígitos |
| **BE-6** | `omen-saa-2-be` | **Medir, no arreglar:** qué pasa en asiento y cruce con una retención de total `0,00` (código 332 al 0%, el caso real). `nuevaAplicacion` rechaza monto cero |
| **FE-3** | `omen-saa-2-fe` | Formulario de nota de venta manual: sólo dígitos, largo máximo, y completar con ceros al salir del campo |
