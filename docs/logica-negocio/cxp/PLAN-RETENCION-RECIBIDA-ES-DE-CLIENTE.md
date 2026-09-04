# La retención que nos emiten es de un CLIENTE, no de un proveedor

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-04 · **Estado:** diseño congelado, listo para implementar.
**Origen:** el usuario lo levantó desde producción el 2026-09-04. Hallazgo completo en
`ESTADO-EQUIPO-OMEN-2.md` §21.

---

## 0. Decisiones del usuario — 2026-09-04

| # | Decisión |
|---|---|
| **D1** | El asiento de una retención recibida es **DEBE** crédito tributario / anticipo de IR retenido (cuenta por código SRI) y **HABER** la **CxC del cliente**. Baja lo que ese cliente nos debe |
| **D2** | **Los documentos siguen entrando por la carga automática de CxP.** No se mueven al módulo `cxc` |

**D2 acota el frente y lo mantiene dentro del alcance de este equipo.** Se corrige el rol y la cuenta
que se piden; **no** se toca el módulo `cxc` ni se cambia por dónde entra el documento.

---

## 1. ⚠️ CORRECCIÓN de lo que este árbitro reportó primero

**El 2026-09-04 dije que el asiento generado era «el espejo del correcto». Es falso, y la
diferencia cambia el tamaño del arreglo.**

Leyendo los **valores** en vez de los comentarios (`AsientoContableServiceImpl:3311-3345`):

```java
haberReten.setValorDebe(valor);      haberReten.setValorHaber(0.0);   // retención -> DEBE
debe.setValorDebe(0.0);              debe.setValorHaber(totalRetenido); // titular  -> HABER
```

**Los lados ya son los correctos** según D1: la retención al DEBE, el titular al HABER. **Lo único
equivocado es de qué rol se saca la cuenta del titular**: `obtenerCuentaProveedor` donde
corresponde `obtenerCuentaCliente`.

### Por qué me equivoqué, que es lo que hay que llevarse

**Las variables y los comentarios dicen lo contrario de lo que hace el código.** La variable que va
al HABER se llama `debe`; la que va al DEBE se llama `haberReten`; el bloque de la retención está
rotulado `── HABER: por código de retención ──` y las líneas van al debe; y el llamador
(`ProcesoCargaDocumentosServiceImpl:4345`) documenta *«DEBE: cuenta CxP del proveedor»*, que es
falso en los dos términos.

> **Leí los rótulos y no los valores, y el §13 ya avisaba de esto: deducir desde lo que se lee en
> vez de medir.** En un archivo donde los nombres están invertidos, **el comentario no es una pista
> débil: es una pista falsa**, y cuesta más que no tener ninguna. Es el §12 —«un comentario que
> describe la intención envejece peor que no tener comentario»— en su forma más cara: acá no
> describe ni la intención, describe lo contrario del comportamiento.

**Consecuencia práctica: el arreglo es chico.** No hay que rearmar el asiento; hay que cambiar de
qué rol sale una cuenta, y arreglar los nombres para que el próximo que lea no se equivoque igual.

---

## 2. Los cuatro cambios

Los métodos V1 y V2 son gemelos: **todo lo de abajo va por duplicado.**

### 2.1 `cnt` — la cuenta del titular sale del rol Cliente

`AsientoContableServiceImpl`, en `generarAsientoRetencionCompra` (`:3222`) y
`generarAsientoRetencionCompraV2` (`:3288`):

- `obtenerCuentaProveedor(rc.getProveedor().getCodigo(), idEmpresa)` → **`obtenerCuentaCliente(...)`**.
  Ya existe (`:1468`) y resuelve `obtenerCuentaPersona(..., tipoCuenta=1, RolPersona.CLIENTE)`.
- El mensaje de error pasa a nombrar **cliente** y **cuenta CxC**.
- La descripción de la línea deja de decir `"CxP Cliente retención V2: …"`.
- **Arreglar los nombres invertidos** (§1): la variable `debe` que va al haber, la `haberReten` que
  va al debe, y los dos rótulos de bloque.

### 2.2 `cxp` — la validación bloqueante pide el rol Cliente

`ProcesoCargaDocumentosServiceImpl`, validación **2a** de `registrarRetencionCompra` (`:2952`) y
`registrarRetencionCompraV2` (`:3117`):

- Deja de usar `verificarCuentaContableProveedor` y pasa a exigir
  `existeCuentaConRolEstricto(titular, idEmpresa, 1L, RolPersona.CLIENTE)`.
- El `tipo` del bloqueante pasa de `PROVEEDOR_SIN_CUENTA` a **`CLIENTE_SIN_CUENTA`**, y el texto
  nombra al **cliente** y a la **cuenta contable CxC**.
- ⛔ **Sólo en los dos métodos de retención.** `agregarBloqueantesComunesCompra` (`:2351`) y
  `registrarFacturaCompra` (`:1453`) **se quedan como están**: en facturas y notas de crédito el
  emisor **sí** es un proveedor.

### 2.3 `saaFE` — la etiqueta del bloqueante nuevo

`cxp/forms/procesos/gestion-documentos/gestion-documentos.component.ts:156` mapea los códigos a
etiquetas. Hay que **agregar** `CLIENTE_SIN_CUENTA` sin borrar `PROVEEDOR_SIN_CUENTA`, que sigue
vigente para facturas.

⛔ **Si el código llega y no está en el mapa, la pantalla no lo va a saber mostrar.** Este ítem no
es cosmético: es la mitad visible del cambio.

### 2.4 `cxp` — el rol que se auto-asigna al titular

`obtenerOAutoCrearProveedor` (`:~3715`) le asigna **rol Proveedor** al emisor, y lo auto-crea si no
existe. Para una retención el emisor es un **cliente**.

⛔ **NO cambiar el método:** lo comparten facturas, notas de crédito y liquidaciones, donde el rol
Proveedor es el correcto. Hay que **parametrizar el rol** (o hacer un gemelo para retenciones) y que
sólo los dos métodos de retención pidan Cliente.

**Y no debe QUITAR el rol Proveedor si el titular ya lo tiene:** un mismo titular puede ser cliente
y proveedor a la vez. El método suma el rol que falta, nunca reemplaza.

---

## 3. Lo que este diseño NO decide

- **Qué hacer con las retenciones ya cargadas** con la cuenta de proveedor en el haber. Hay que
  medirlo primero: `cxp/sql/e2-10-retenciones-cargadas-con-cuenta-de-proveedor.sql`. Si hay filas,
  el ajuste contable se diseña aparte y lo decide el usuario.
- **Renombrar `RetencionCompraV2.proveedor`** a algo honesto. Es el nombre correcto para el dato
  equivocado, pero tocar el campo de la entidad es un refactor aparte y no hace falta para esto.
- **Los titulares sin cuenta bajo rol Cliente.** `AsientoContableServiceImpl:117` tiene medido que
  **61 de 87 titulares con cuenta sólo la tienen bajo rol Proveedor**. Exigir el rol Cliente va a
  destapar casos. **Es correcto que los destape** —es el dato que falta de verdad— pero conviene
  medirlo antes de desplegar, con el mismo `e2-10`.
