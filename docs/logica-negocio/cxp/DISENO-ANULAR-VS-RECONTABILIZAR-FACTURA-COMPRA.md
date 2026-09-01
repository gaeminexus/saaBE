# Anular vs. recontabilizar una factura de compra — diseño

**2026-08-31 · equipo `omen-saa-3`.** Nace de un `ORA-02292` al intentar revertir una factura de
compra que tenía un pago ya anulado. La investigación mostró que **el error era un síntoma: la
operación que se estaba usando no era la que correspondía a ninguno de los dos casos reales.**

**Estado: propuesta, pendiente de confirmación del usuario. Nada implementado.**

---

## 1. El error reportado

```
ORA-02292: restricción de integridad (PGS.FK_PGTR_FACTURA) violada - registro secundario encontrado
[delete from PGS.FCTC fc1_0 where fc1_0.ID=?]
   en ProcesoCargaDocumentosService.revertirDocumento
```

`revertirDocumento` → `revertirRegistrosBD` **borra físicamente** la factura y sus hijos
(`:3077-3086`). Antes de borrar comprueba que no haya `AplicacionPagoCxp`, pero **no mira
`PGS.PGTR`** (`PagoProgramado`), que tiene FK a la factura. Anular un pago lo deja en estado
`ANULADO(5)` — **no borra la fila**, así que la FK sigue apuntando y el `DELETE` falla.

### Barrido: qué más apunta a `FacturaCompra`

Se revisaron las siete entidades que la referencian, para no arreglar una FK y chocar con la
siguiente:

| Entidad | Tabla | ¿La cubre `revertirRegistrosBD`? |
|---|---|---|
| `DetalleFacturaCompra` | `PGS.DFCC` | ✅ borra |
| `FormaPagoFacturaCompra` | `PGS.FPFC` | ✅ borra |
| `PathFacturaCompra` | `PGS.PFCC` | ✅ borra |
| `ReembolsoFacturaCompra` | `PGS.RMBF` | ✅ borra |
| `AplicacionPagoCxp` | `PGS.APLP` | ✅ bloquea con mensaje |
| **`PagoProgramado`** | **`PGS.PGTR`** | ❌ **no lo mira — es el que falla hoy** |
| **`PagoNegociacion`** | **`PGS.PGNG`** | ❌ **no lo mira — fallaría a continuación** |

> **Faltaban dos, no una.** Arreglar sólo `PGTR` habría movido el error a `PGNG` en el siguiente
> intento.

---

## 2. Los dos casos reales (definidos por el usuario)

`revertirDocumento` **no es ninguno de los dos**. Es «deshacer la ingesta del XML», y por eso borra.

### Caso A — Anulación verdadera

La factura no debía existir, o se anula ante el SRI.

1. El pago se anula → el pago y **el asiento del pago** quedan `ANULADO`.
2. La factura se anula de verdad → **se anula su asiento** y **el documento queda anulado**.
3. **La factura NO se borra.** Queda como registro anulado, con su motivo y su auditoría.

### Caso B — Regenerar sólo la contabilidad

La factura está bien; lo que estaba mal era la **cuenta contable del grupo de producto**. Se corrige
el catálogo y se quiere rehacer el asiento.

1. El pago se anula igual que en el caso A.
2. **La factura NO se anula ni se borra.** El documento vuelve a *XML cargado*.
3. Se anula el asiento original.
4. Se vuelve a contabilizar → **asiento nuevo con las cuentas corregidas**.

---

## 3. ⚠️ La simplificación: el caso B no necesita reprocesar el XML

El pedido original decía «dejar la factura en estado de cargado el xml y **volver a procesar esa
factura**». **Se propone no hacerlo, y el motivo es verificable:**

- **`DetalleFacturaCompra` no guarda ninguna cuenta contable.** Se revisó campo por campo: no hay
  ni `cuenta` ni nada equivalente. La factura sólo guarda el `Asiento` que se le generó
  (`FacturaCompra:155`).
- **El asiento resuelve las cuentas contra el catálogo en el momento de generarse.**
  `generarAsientoFacturaCompra(idFactura, idEmpresa, tipo, fecha, obs, usuario)` recibe el **id de
  la factura**, no cuentas, y «agrupa el DEBE por cuenta contable» (`:1350`).

**Consecuencia:** reprocesar el XML volvería a crear una factura **idéntica** —el XML no cambió— y
lo único distinto sería el asiento. Borrar y recrear la factura para eso es un rodeo que:

1. **Reintroduce el `ORA-02292`**, porque vuelve a exigir borrar una fila que el pago referencia.
2. Cambia el `id` de la factura, rompiendo cualquier referencia externa que apunte al anterior.
3. Pierde lo que se haya capturado a mano sobre la factura (`parteRel`, `tipoProv`,
   `fechaRegistro`, sustento tributario).

> **Basta con anular el asiento y volver a generarlo sobre la misma factura.** Es menos trabajo,
> no toca ninguna FK y conserva el documento entero.

**Y el estado ya existe con ese significado exacto:** cuando un reembolso no cuadra, el código deja
`doc.setEstadoDocumento(ESTADO_XML_CARGADO)` sin generar asiento (`:3957`, `:3973`). O sea que
**`XML_CARGADO(2)` ya se usa como "registrado en BD pero pendiente de contabilizar"** — no hay que
inventar nada.

---

## 4. Lo que hay que construir

### 4.1 Lo que ya existe y se reusa

| Pieza | Dónde | Para qué |
|---|---|---|
| `anularFacturaCompra(id, motivo, usuario, idUsuario, cascada)` | `FacturaCompraServiceImpl:111` | Caso A — ya anula la factura y reversa aplicaciones (frente R) |
| `anularAsientoDeDocumento(tipo, idDocBD)` | `ProcesoCargaDocumentosServiceImpl:3122` | Anula el asiento vigente del documento |
| `generarAsientoFacturaCompra(...)` | `AsientoContableService` | Genera el asiento nuevo leyendo el catálogo |
| `ESTADO_XML_CARGADO` | `EstadoDocumentoCxp` | El estado «registrado, sin contabilizar» |

### 4.2 Caso A — lo que falta

1. **Anular el `PagoProgramado` y su asiento** como parte de la anulación de la factura. Hoy
   `anularFacturaCompra` no toca `PGS.PGTR`.
2. **Marcar el `DocumentoCxp` como anulado.**
   ⚠️ **`EstadoDocumentoCxp` no tiene ANULADO.** Sus valores son `LEIDO(1)`, `XML_CARGADO(2)`,
   `REGISTRADO_BD(3)`, `ERROR(4)`, `NOVEDAD(5)`, `REVERTIDO(6)`. Hay que **agregar `ANULADO(7)`**
   al rubro **175** (`CXP_ESTADO_DOCUMENTO_CXP`), con su fila en `SCP.PDTR`.
   **El `PDTR` sale del bloque reservado de este equipo (1400-1499)** — se anota en
   `REGISTRO-RESERVAS-EQUIPOS.md` antes de escribir el script.
   *No se reusa `REVERTIDO(6)`: significa «los registros se borraron», y acá la factura sigue
   existiendo. Mezclarlos haría imposible distinguir los dos casos al consultar.*

### 4.3 Caso B — lo que falta

Un método nuevo, `recontabilizarDocumento(idDocumentoCxp, idUsuario)`:

1. Validar que el documento esté en `REGISTRADO_BD(3)`.
2. **Anular el pago programado y su asiento**, si hay (igual que el caso A).
3. `anularAsientoDeDocumento(tipo, idDocBD)` — anula el asiento vigente.
4. Dejar el `DocumentoCxp` en `XML_CARGADO(2)` con observación del motivo.
5. Un segundo paso —o el mismo, según se decida— que vuelva a llamar a
   `generarAsientoFacturaCompra(...)` y devuelva el documento a `REGISTRADO_BD(3)`.

**No borra nada.** Ni la factura, ni sus detalles, ni sus paths.

### 4.4 `revertirDocumento` — qué se hace con él

Sigue teniendo sentido para su propósito real: deshacer una ingesta equivocada del XML, cuando el
documento **no tuvo movimiento**. Se le agrega la guarda que le falta:

- Si hay `PagoProgramado` en cualquier estado → **bloquear con mensaje claro** diciendo que use
  anulación (caso A) o recontabilización (caso B), según lo que quiera hacer.
- Si hay `PagoNegociacion` → **desvincular** (`facturaCompra = null`). El modelo declara ese vínculo
  **opcional** (*«Factura de compra asociada a este pago (opcional)»*, `PagoNegociacion:77`), así que
  el pago vuelve a ser un anticipo sin factura, que es un estado válido para él.

> **Las dos FK reciben tratamiento distinto, y no es arbitrario:** `PagoProgramado` declara que su
> referencia es **excluyente** con egreso y anticipo (`PagoProgramado:101-103`), así que ponerla en
> `null` lo dejaría sin ninguna de las tres y rompería su invariante. `PagoNegociacion` declara la
> suya **opcional**. Cada uno se trata como su propio modelo dice.

---

## 5. Preguntas abiertas para el usuario

1. **Caso B, ¿un paso o dos?** ¿Un solo botón que anula y regenera el asiento de una, o dos pasos
   (anular contabilidad → revisar el catálogo → recontabilizar)? Dos pasos permiten corregir las
   cuentas *entre medio*, que es justamente el caso de uso.
2. **¿La anulación del pago la dispara este proceso, o el usuario ya la hizo aparte?** En el reporte
   original el pago ya estaba anulado a mano. Si el proceso lo hace solo, es un botón; si no, hay
   que validar que ya esté anulado y avisar si no lo está.
3. **Caso A: ¿el `DocumentoCxp` anulado debe poder volver a procesarse**, o queda cerrado para
   siempre? Cambia si `ANULADO(7)` es terminal.
