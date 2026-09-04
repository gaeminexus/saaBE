# Nota de venta de compra — ingreso manual, sin XML

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-04 · **Estado:** diseño congelado, listo para implementar.

---

## 0. El pedido y las decisiones del usuario

**Textual:** *«Existen ciertas facturas de CxP de las que no llega el XML. Debe existir una pantalla
para poder ingresar manualmente esas facturas, no leyéndolas de un XML.»*

Al acotar el alcance el usuario precisó el punto clave:

> *«Solo **notas de venta**. Que es exactamente igual que una factura pero sin XML.»*

| # | Decisión |
|---|---|
| **D1** | Sólo **notas de venta**. No notas de crédito, ni de débito, ni liquidaciones de compra |
| **D2** | El detalle se captura **línea por línea con producto** (cantidad, precio unitario, descuento, IVA), igual que una factura electrónica |
| **D3** | **No hace falta una marca de origen**: la nota de venta **ya es otro tipo de documento**. El distintivo es el tipo, no una bandera |
| **D4** | Al registrar **genera el asiento contable en el acto**, con las mismas validaciones bloqueantes que la electrónica |

### D3 es la decisión que ahorra un DDL, y conviene entender por qué

Yo había propuesto una columna nueva en `PGS.FCTC` para distinguir «cargada a mano» de «llegada por
XML». **El usuario corrigió el encuadre y el problema desapareció:** no es una factura cargada a
mano, es **una nota de venta**, que es un tipo de comprobante distinto del SRI y ya tiene dónde
guardarse — el campo `TIPOCOMPROBANTE`, que hoy vale `"01"` para todas las facturas.

> **La marca que yo quería agregar era un dato derivado de otro que ya existía.** Preguntar «cómo
> distingo estos dos» dio una columna nueva; preguntar «qué son estos documentos» dio que ya estaban
> tipificados. **Modelar la categoría real es más barato que modelar la diferencia.**

---

## 1. Dónde vive: se reutiliza `PGS.FCTC`, no se crea tabla

**La nota de venta se graba como `FacturaCompra` con `tipoComprobante = "02"`.**

⚠️ **`"02"` HAY QUE CONFIRMARLO contra la tabla de tipos de comprobante del SRI antes de
implementar.** Es el código de nota de venta en el catálogo estándar, pero **no está escrito en
ningún lado de este repositorio** —`grep -rni "nota de venta" src/ docs/` no devuelve nada— así que
acá es una afirmación mía sin verificar contra la fuente. Es un cambio de un carácter si está mal,
pero queda mal grabado en producción si nadie lo mira. **Que lo confirme contabilidad.**

**Por qué reutilizar la tabla y no crear una nueva:** todo lo que ya está construido sobre
`FacturaCompra` aplica sin tocar una línea — consulta de documentos, pago, cruce de anticipos,
historial de abonos, anulación con cascada, estado de cuenta del proveedor, sustento tributario y
ATS. Una tabla nueva obligaría a reconstruir **todo** eso. El usuario lo dijo mejor que cualquier
argumento técnico: *«es exactamente igual que una factura»*.

### Lo que NO va a tener, y hay que verificar que nadie lo dé por sentado

| Campo | Por qué queda vacío |
|---|---|
| `CLAVE` (clave de acceso) | Sólo existe en comprobantes electrónicos |
| `AMBIENTE` | Ídem |
| `PATHGEN` / `PathFacturaCompra` | No hay archivo XML que guardar |
| `DocumentoCxp` (la bandeja) | No entra por la bandeja electrónica: es alta directa |
| `FECHAAUTORIZACION` | La nota de venta preimpresa tiene número de autorización pero no fecha de autorización electrónica |

⛔ **ÍTEM DE VERIFICACIÓN OBLIGATORIO para el agente:** buscar en `saaBE` y en `saaFE` todo lo que
lea esos campos de una `FacturaCompra` y comprobar que tolera `null`. Una pantalla que hoy hace
`factura.clave.substring(...)` o un «Ver XML» sin guarda **revienta la primera vez que alguien abre
una nota de venta**. Es exactamente la familia del §17: un campo ausente no da error, da
`undefined`, y el síntoma aparece lejos.

---

## 2. Qué tiene que replicar del camino electrónico

Medido leyendo `ProcesoCargaDocumentosServiceImpl.registrarFacturaCompra` (`:1298-1720`), que es el
molde. **El endpoint manual hace lo mismo salvo lo que dependa del XML.**

| Paso | Qué hace la electrónica | En la manual |
|---|---|---|
| Validar cuenta contable del proveedor | `verificarCuentaContableProveedor` → bloqueante `PROVEEDOR_SIN_CUENTA` | **igual** |
| Validar que existe el `TipoAsiento` de factura de compra | `existeTipoAsiento` → bloqueante | **igual** |
| Validar productos: ni `POR_CLASIFICAR` ni grupo sin cuenta | bloqueante por producto | **igual** |
| Crear la cabecera | `new FacturaCompra()` + `save` | **igual**, con `tipoComprobante="02"` |
| Crear los detalles | `DetalleFacturaCompra` por línea del XML | **igual**, por línea del formulario |
| Forma de pago | `FormaPagoFacturaCompra` | **igual** |
| Guardar el path del XML | `PathFacturaCompra` | **se omite** |
| Resolver el sustento tributario (`FCTCCSUS`) para el ATS | `SustentoTributarioService` | **igual** |
| Generar el asiento | `generarAsientoFacturaCompra` | **igual** |

⛔ **NO se usa el `POST /rest/fctc` genérico.** Ese es el CRUD de `EntityDaoImpl`: un `merge` pelado
que **no valida nada, no crea detalles, no resuelve sustento y no genera asiento**. Una nota de
venta creada por ahí queda invisible para la contabilidad y para el ATS, y no se nota hasta que no
cuadra el mes. **Va un endpoint de proceso propio.**

---

## 3. Lo que este diseño deja explícitamente abierto

### 3.1 🟡 El IVA de una nota de venta

Las notas de venta las emiten contribuyentes **RISE**, y en el régimen RISE **no se desglosa IVA**.
Este diseño **acepta los campos de IVA pero no los exige** (`vIVA`, `pIVA`, subtotales por tarifa
quedan opcionales y por defecto en cero).

**No lo cerré porque es una pregunta tributaria, no de diseño**, y equivocarse acá afecta el crédito
tributario del mes. **Que lo confirme contabilidad antes de que la pantalla se use en serio.** Si la
respuesta es «nunca lleva IVA», el formulario puede esconder esos campos, que es una mejora de
usabilidad y no un cambio de modelo.

### 3.2 🟡 La retención sobre una nota de venta

Comprar a un RISE tiene reglas de retención propias. **Este frente no genera retenciones**: registra
el documento. Emitir la retención sigue el camino que ya exista hoy. Si hace falta algo distinto, es
otro frente.

### 3.3 ⚪ Numeración y duplicados

`establecimiento-puntoEmisión-secuencial` de un mismo proveedor **no debería poder repetirse**. El
diseño incluye esa validación como bloqueante (`DOCUMENTO_DUPLICADO`). **Si la base ya tuviera un
índice único sobre eso, mejor**; no lo verifiqué contra el esquema y no depende de este frente.

---

## 4. Orden de ejecución

| Paso | Qué | Depende de |
|---|---|---|
| 1 | Confirmar con contabilidad el código `"02"` y la regla de IVA (§1 y §3.1) | — |
| 2 | BE: DTO + endpoint `POST /rest/fctc/manual` con las validaciones del §2 | 1 |
| 3 | BE: barrido de tolerancia a `null` de los campos del §1 | — |
| 4 | FE: pantalla de ingreso, y barrido del mismo tema en las pantallas existentes | 2 |
| 5 | Prueba manual de punta a punta | 2, 4 |

**No hay DDL en este frente.** No hace falta correr nada en la base antes del WAR.

---

## 5. Requisito del usuario, 2026-09-04 — tiene que vivir en todo `cxp` y `tsr`

> *«Este documento debe entrar en todo el ambiente de CxP y TSR: debe incluirse en el estado de
> cuenta de titular y poder realizar pagos normales y con caja chica.»*

**Verificado camino por camino. Borde de la medición:** se leyeron los tres consumidores en
`saaBE/src` y `saaFE/src`; **no se ejecutó ninguno.**

### 5.1 ✅ Los tres caminos funcionan por construcción, y la razón importa

| Camino | Cómo busca | ¿Entra la nota de venta? |
|---|---|---|
| **Estado de cuenta de titular** (`tsr`) | `estado-cuenta-titular.service.ts:131` enumera **fuentes**: `{etiqueta:'Facturas de compra', url: ServiciosCxp.RS_FCTC, campoTitular:'titular'}` | ✅ **sí** — pega contra `RS_FCTC`, la misma tabla |
| **Pago normal** (proposición de pago) | `proposicion-pago:201` → `facturaS.selectByCriteria(criterioTitular)` | ✅ **sí** |
| **Pago con caja chica** | `documento-cruce-selector-dialog:105-113` → criterio único: `titular.codigo IGUAL` | ✅ **sí** |

**Ninguno de los tres filtra por `tipoComprobante`.** Los tres preguntan *«qué documentos tiene este
titular»*, y la nota de venta **es** un documento de ese titular en esa tabla.

> **Esto es lo que compró la decisión de reutilizar `PGS.FCTC` en vez de crear una tabla.** El
> requisito del usuario —«que entre en todo el ambiente»— no costó trabajo: ya estaba pagado al
> elegir el modelo. Con una tabla nueva, cada uno de estos tres consumidores habría necesitado un
> `forkJoin` más, y **cada uno que alguien olvidara habría sido un saldo mal calculado, en silencio**.

**Y hubo suerte, conviene decirlo:** el §5 del plan de caja chica avisaba de la trampa inversa —*«si
el estado de cuenta enumera los cinco tipos actuales, el sexto no aparecería»*—. Acá **la
enumeración es por endpoint, no por tipo**, así que un tipo nuevo sobre un endpoint existente pasa
solo. Si el estado de cuenta hubiera enumerado tipos de documento en vez de fuentes, este requisito
habría sido un frente entero. **La diferencia entre las dos formas de enumerar no se ve hasta que
agregás el elemento número seis.**

### 5.2 🟡 Lo que SÍ falta: las tres pantallas la van a llamar «Factura»

Funciona, pero miente en la etiqueta. Los tres consumidores rotulan por **origen del endpoint**, no
por el tipo de la fila:

| Dónde | Qué muestra hoy | Qué debe mostrar |
|---|---|---|
| Estado de cuenta de titular | la sección **«Facturas de compra»** | distinguir la nota de venta, o al menos mostrar el tipo por fila |
| Selector de caja chica | mapea todo lo de `facturaService` a `tipo: 'FACTURA'` → etiqueta **«Factura»** | **«Nota de venta»** cuando `tipoComprobante === '02'` |
| Proposición de pago | ídem | ídem |

**El saldo del titular sale bien igual** —por eso es 🟡 y no 🔴—, pero un usuario que ve «Factura
001-001-000000123» y busca el XML que no existe pierde el tiempo, y peor, puede creer que falta
cargar algo.

**Regla para los tres:** la etiqueta sale de `tipoComprobante` de la fila, no de qué servicio la
trajo. Un solo helper compartido y los tres lo usan.

### 5.3 ⚪ Verificado que NO hace falta tocar el backend para esto

Los tres caminos ya pasan por `selectByCriteria` de `FacturaCompra` o por el endpoint de estado de
cuenta, ninguno con filtro de tipo. **El requisito es enteramente de etiquetado en el frontend.**
Que la nota de venta se pueda **pagar** —normal o con caja chica— no necesita ni una línea nueva de
backend: `AplicacionPagoCxp` ya la referencia como `facturaCompra`, porque es una fila de `FCTC`.
