# Facturas de intermediario — diseño

**Equipo:** `lap-saa-1` · **2026-09-02** · Módulo `cxp` (toca `cnt`, compartido)
**Requerimiento urgente del usuario.**

---

## 1. Qué se pide

Hay facturas que **no son gasto real del fondo**: el fondo actúa de **intermediario** entre un
arrendatario y el gasto real. Contablemente no van como las demás.

1. Al presionar **Registrar**, se puede marcar la factura como **de intermediario** —igual que hoy
   se marcan las de reembolso de gastos—.
2. Al marcarla, el sistema **pide el producto** al que se quiere contabilizar.
3. El asiento de esa factura **ignora los detalles y los impuestos**: manda **el total** a la cuenta
   contable del **grupo de ese producto**.

---

## 2. ⚠️ El reembolso se marca en otro momento — no copiar el mecanismo

El usuario pidió «así como se marcan las de reembolso», y conviene precisar en qué se parecen y en
qué no, porque el mecanismo del reembolso **no sirve acá**:

| | Reembolso | Intermediario |
|---|---|---|
| Cuándo se marca | **al subir el XML** (paso 1) | **al presionar Registrar** (paso 2) |
| Dónde vive la marca | persistida en `DocumentoCxp` | **viaja en el request de `registrarBD`** |

`ProcesoCargaDocumentosServiceImpl:444-446` lo dice explícito: *«registrarBD lee `esReembolso` del
documento ya persistido, no del request»*. Como el intermediario se decide **en el momento de
registrar**, su marca tiene que llegar en el cuerpo de esa llamada.

**Se parecen en el resultado** —una bandera en `PGS.FCTC` y una rama propia en el asiento— y se
diferencian en el momento. Copiar el camino del reembolso llevaría a pedir la marca en la pantalla
equivocada.

---

## 3. Modelo

### 3.1 DDL — `PGS.FCTC`, dos columnas

| Columna | Tipo | Para qué |
|---|---|---|
| `FCTCESIN` | `NUMBER(1)` **nullable**, `DEFAULT 0` | 1 = factura de intermediario |
| `FCTCPRIN` | `NUMBER` nullable, FK a `PGS.PRDP(ID)` | producto al que se contabiliza |

⚠️ **`FCTCESIN` va NULLABLE a propósito, y no es descuido.** Es la lección de `CBR.ANTC.ANTCAPLC`
(2026-08-31): una columna `DEFAULT 0 NOT NULL` **rompe todo `INSERT`** de la entidad, porque
Hibernate **siempre nombra** la columna y **el `DEFAULT` de Oracle sólo actúa si el `INSERT` la
omite**. Con la columna nombrada y en `null`, salta `ORA-01400`. La integridad se consigue con el
**inicializador en Java** (`= 0L`), no aflojando ni endureciendo la base.

Las dos son `PGS` → `PGS`, así que la FK **no** necesita `GRANT REFERENCES`.

Script: `cxp/sql/lap1-08-factura-intermediario.sql`. **Va ANTES del WAR** — la entidad las mapea, y
una columna mapeada que no existe rompe **toda lectura** de `FacturaCompra` con `ORA-00904`.

### 3.2 Entidad

```java
@Basic @Column(name = "FCTCESIN")
private Long esIntermediario = 0L;          // inicializado, ver §3.1

@Basic @Column(name = "FCTCPRIN")
private Long idProductoIntermediario;
```

**`idProductoIntermediario` es un `Long`, no un `@ManyToOne`**, igual que
`DetalleFacturaCompra.producto` — que es el precedente del módulo y lo que el asiento ya resuelve
con `em.find(ProductoPago.class, id)`.

---

## 4. Contrato de API

### `POST /rest/carga-documentos/registrarBD/{idDocumentoCxp}`

El cuerpo ya es un `Map`, así que las claves nuevas son **aditivas**: un cliente que no las mande se
comporta exactamente como hoy.

```json
{
  "idEmpresa": 1,
  "idUsuario": 1,
  "esIntermediario": true,
  "idProductoIntermediario": 42
}
```

| Campo | Tipo | Obligatorio | Nota |
|---|---|---|---|
| `esIntermediario` | boolean | no | por defecto `false` |
| `idProductoIntermediario` | Long | **sí, si `esIntermediario` es `true`** | producto al que se contabiliza |

**Errores** (el endpoint ya responde **422** con `{error, tipo}` para los bloqueantes):

| Caso | `tipo` del bloqueante |
|---|---|
| `esIntermediario: true` sin `idProductoIntermediario` | `INTERMEDIARIO_SIN_PRODUCTO` |
| El producto no existe | `INTERMEDIARIO_PRODUCTO_INEXISTENTE` |
| El producto no tiene grupo | `INTERMEDIARIO_SIN_GRUPO` |
| El grupo no tiene cuenta contable | `INTERMEDIARIO_SIN_CUENTA_CONTABLE` |

Los cuatro responden **422** con la forma que **ya** devuelve este endpoint para los bloqueantes:
`{pendienteClasificacion: true, bloqueantes: [{tipo, detalle}], mensaje}`.

⛔ **Ninguno cae a un comportamiento por defecto.** Contabilizar una factura de intermediario contra
la cuenta equivocada es peor que no registrarla.

### 4.1 ⛔ Bloqueante, NO `IncomeException` — corregido el 2026-09-02

**La primera versión de este diseño decía `IncomeException`, y estaba mal.** Lo detectó el agente de
backend al implementar, y no lo cambió por su cuenta: lo trajo con el mecanismo verificado.

`registrarDocumentoBD` envuelve todo en un `catch` que llama a `marcarError(...)` y deja el
`DocumentoCxp` en estado **ERROR**, en una transacción `REQUIRES_NEW` aparte. Y el guard de entrada
(`:811`) exige `ESTADO_XML_CARGADO`. O sea: **un documento que pasa a ERROR ya no se puede volver a
registrar desde la pantalla.** `IncomeException` extiende `RuntimeException`, así que las
validaciones habrían caído ahí.

**Consecuencia real:** un usuario que se olvida de elegir el producto no sólo recibe el error —
**deja el documento inutilizable**, y hay que sacarlo de ERROR por otra vía.

**El archivo ya distingue los dos casos, y el criterio es el correcto:**

| Naturaleza del fallo | Patrón | Estado del documento |
|---|---|---|
| El usuario eligió mal o falta un dato **que él puede corregir** | Map con `pendienteClasificacion` | se queda en `XML_CARGADO`, **reintentable** |
| Algo salió mal en el proceso | `IncomeException` → ERROR | no reintentable, y está bien que así sea |

Las cuatro validaciones de intermediario son del primer tipo: se arreglan eligiendo distinto y
volviendo a apretar Registrar. Son la tercera de la misma familia que `PRODUCTOS_SIN_CLASIFICAR` y
`GRUPOS_SIN_CUENTA_CONTABLE` — que tampoco por casualidad son «falta clasificar algo antes de
contabilizar».

> **La lección, y aplica más allá de esto:** al elegir cómo falla una validación, la pregunta no es
> «¿qué tan grave es?» sino **«¿puede el usuario arreglarlo y reintentar?»**. Un error que el usuario
> puede corregir no debe dejar rastro que se lo impida.

**El asiento sí conserva sus `IncomeException`** (§5): si se llega a generar el asiento con el
producto mal, ya no es un dato por elegir, es un estado inconsistente, y tiene que fallar ruidoso.

---

## 5. El asiento

Rama propia, **antes** de la de reembolso, en `generarAsientoFacturaCompra`:

```
si esIntermediario == 1:
    producto = em.find(ProductoPago, fc.getIdProductoIntermediario())
    cuenta   = producto.getGrupoProducto().getPlanCuenta()

    DEBE  = cuenta del grupo   por  fc.getTotal()
    HABER = CxP proveedor      por  fc.getTotal()
```

**Sin línea de IVA y sin recorrer los detalles.** Es lo pedido: el total completo —impuestos
incluidos— va a la cuenta de ese grupo, porque para el fondo no es gasto ni crédito tributario
propio, es un movimiento de paso.

### 5.1 Estas facturas no necesitan línea de diferencia por redondeo

Las dos líneas salen del **mismo número**, `fc.getTotal()`, así que el asiento cuadra por
construcción y no hay dos fuentes que puedan discrepar.

> **Y esa es exactamente la propiedad que hace correcta a la nota de débito de compra** (§6.2 de
> `DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md`): *derivar todo de una sola fuente*, en vez de hacer que
> dos cuadren por construcción sin compararlas. Acá se cumple sola.

⛔ **No llamar a `agregarDiferenciaRedondeoSri` en esta rama.** Siempre daría cero, y sugeriría que
hay algo que reconciliar donde no lo hay.

---

## 6. Frontend

`cxp/forms/procesos/gestion-documentos`, método `registrar(doc)` — hoy es un `confirm()` de una
línea (`:1013`).

Pasa a un diálogo con:
- casilla **«Factura de intermediario»**, apagada por defecto;
- al marcarla, **selector de producto obligatorio** (autocomplete), oculto mientras esté apagada;
- el botón de confirmar **deshabilitado** si está marcada y no hay producto elegido, para que el
  422 del servidor sea la red y no el camino normal.

**Texto que acompaña a la casilla**, para que quien registra entienda qué está eligiendo:
> *El total de la factura se contabiliza contra la cuenta del grupo del producto elegido. No se
> registran ni el detalle ni el IVA.*

---

## 7. Alcance y límites

| | |
|---|---|
| Entra | **Factura de compra** (`FCTC`) registrada desde la pantalla, una por una |
| ⏸️ Pendiente de decisión | **El registro por lote** (`/registrarLote`, fase 3) — hoy registra todo de corrido, sin preguntar. Una factura de intermediario **no se puede marcar ahí**; entra como factura normal |
| No entra | Liquidación de compra, notas de crédito y débito |

⚠️ **El lote es un hueco real, no un olvido.** Si una factura de intermediario se registra por lote,
se contabiliza como gasto normal y hay que anularla y volver a registrarla a mano. Conviene que el
usuario lo sepa antes de usar el lote con estas facturas.
