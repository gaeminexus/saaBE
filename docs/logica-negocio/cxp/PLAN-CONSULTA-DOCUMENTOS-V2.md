# PLAN — Consulta de Documentos de CXP: ver todo lo que hoy se averigua por SQL

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-21
**Pedido del usuario, textual:**

> *«Todas estas consultas que te estoy realizando indican que las pantallas que tiene el sistema
> actualmente no me están mostrando toda la información que necesito de los documentos. En una
> pantalla yo debería poder ver si estuvo marcada como intermediario, cuándo se generó, quién la
> generó, qué asiento contable tiene, etc. Revisa las pantallas de consulta de documentos de CXP y
> mejóralas… Debería poder verse todos los campos que hemos estado consultando últimamente, los que
> se usan en el ATS, si es intermediario, si se contabilizó, los asientos contables relacionados a
> la misma, e incluso debe permitirme revisar/descargar el XML del documento.»*

---

## 0. El hallazgo que define el tamaño del frente

**Casi todo lo que pide ya está en las entidades. Lo que falta es mostrarlo.**

Medido el 2026-09-21 contra `DocumentoCxp` y `FacturaCompra`:

| Lo que el usuario pide | Dónde está hoy | ¿Se ve? |
|---|---|---|
| Si es intermediario | `FCTC.FCTCESIN` (+ `FCTCPRIN`, el producto) | ❌ |
| Cuándo se generó / quién | `DCXP`: `fechaRegistroBD`/`usuarioRegistroBD`, `fechaCargaXml`/`usuarioCargaXml` | ❌ |
| Si se reversó / quién | `DCXP`: `fechaReversion`/`usuarioReversion` | ❌ |
| Qué asiento contable tiene | `FCTC.ASIENTO` → `Asiento` (`@ManyToOne`) | ❌ |
| Si se contabilizó | el mismo `ASIENTO` (nulo = no contabilizado) + `DCXP.estadoDocumento` | ❌ |
| Campos del ATS | `FCTCCSUS` (codSustento), `FCTCFCRG` (fecha de registro contable), autorización | ❌ |
| Bases por tarifa | `SUBTOTAL`, `SUBCERO`, `SUBNOOBJ`, `SUBEXENT`, `SUBTOTAL5`, `SUBTOTAL8` | ⚠️ parcial |
| Anulación | `FCTCMTAN`/`FCTCFCAN`/`FCTCUSAN` (motivo, fecha, usuario) | ❌ |
| Revisar/descargar el XML | ya existe (`26fb5793`) y `DCXP.pathXml` | ✅ |

> **O sea: esto NO es una pantalla nueva, es una ficha de documento que se quedó corta.** La pantalla
> ya trae el documento real al abrir el detalle (`verDetalle`, `case 'FACTURA_COMPRA'` → `getId`), así
> que el objeto con todos estos campos **ya está en el navegador** y no se pinta.

**Por eso se mejora la pantalla existente y NO se hace una v2 en paralelo.** Una segunda pantalla
dejaría dos lugares donde buscar lo mismo, y este equipo ya tiene un caso así (`titulares` y
`titulares-v2`, §43): el huérfano se pudre y nadie sabe cuál es el vigente.

## 1. Lo que hay que ver, agrupado como se lee

La ficha del documento pasa a tener secciones. El orden es el de las preguntas que el usuario viene
haciendo, no el de la tabla:

### 1.1 Identificación
Tipo, serie, RUC y razón social del emisor, fecha de emisión, autorización, clave de acceso,
ambiente.

### 1.2 Clasificación tributaria — «lo que el ATS va a declarar»
- **`codSustento`** (`FCTCCSUS`) con su descripción, no sólo el código.
- **Bases por tarifa, todas y rotuladas**: 0%, no objeto, exento, 5%, 8%, y **la gravada, que es
  una resta** (`SUBTOTAL − SUBCERO − SUBNOOBJ − SUBEXENT`) — mostrarla calculada y dejar dicho que
  es la que sale en `baseImpGrav`.
- **IVA**, ICE.
- **`esIntermediario`**, con el producto (`FCTCPRIN`) si lo tiene. ⚠️ Y que se lea qué significa:
  *«no se declara en el ATS ni en el cuadre 104»*, porque esa es la consecuencia real.
- **`fechaRegistroContable`** (`FCTCFCRG`), y avisar si está vacía: el ATS usa esa fecha con
  respaldo a la de emisión, y de ahí salen los avisos de *«sin fechaRegistro contable capturada»*.

### 1.3 Contabilidad
- ¿Tiene asiento? Número, fecha, estado y **el enlace para verlo**.
- Si no tiene: decir **«sin contabilizar»**, no dejar el campo vacío — vacío se lee como «no cargó».
- Anulación: motivo, fecha y usuario, si los hay.

### 1.4 Trazabilidad
Cargado (fecha + usuario), registrado (fecha + usuario), reversado (fecha + usuario), estado del
documento y la novedad si la tiene.

### 1.5 XML
Descargar (ya existe) y **ver** el contenido sin bajarlo. El `infoAdicional` de la planilla eléctrica
—bomberos, basura— sólo se puede revisar abriendo el archivo, y hoy eso obliga a bajarlo.

## 2. Lo que hay que medir antes de programar

1. **¿`FacturaCompra.asiento` viaja en el JSON de `GET /fctc/getId/{id}`?** Es un `@ManyToOne`
   (EAGER por defecto) y las entidades se serializan directo, sin DTO — así que probablemente sí,
   con el `Asiento` entero. Hay que confirmarlo y ver **qué tan pesado** es: si arrastra sus
   detalles, conviene exponer sólo el resumen.
2. **Los otros tres documentos** (`NTCC`, `NTDC`, `LQCC`): ¿tienen `asiento`, `esIntermediario` y
   auditoría equivalentes? Las secciones tienen que degradarse solas, mostrando lo que cada tipo
   tiene, sin inventar campos que no existen. **Un campo que no aplica se oculta; uno que aplica y
   está vacío se muestra vacío y se dice.** Son dos cosas distintas y la pantalla tiene que
   distinguirlas — es la misma lección del `subtotal5` en `null` contra `0`.
3. **La nota de venta manual** entra a esa lista como documento sintético (`TABLA_NOTA_VENTA`): no
   tiene XML ni clave de acceso. Esas secciones se ocultan, no se muestran vacías.

## 3. Criterio de aceptación

1. Abrir una planilla eléctrica de agosto y ver, **sin consultar la base**: su `codSustento`, sus
   bases repartidas (0%, no objeto), si es intermediario, su asiento, y quién y cuándo la cargó.
2. Abrir la factura 343 y ver `esIntermediario = Sí` con la leyenda de que no se declara.
3. Abrir un documento sin contabilizar y leer **«sin contabilizar»**, no un campo en blanco.
4. Ver el XML de una planilla eléctrica desde la pantalla y encontrar ahí el `CONTRIBUCION
   BOMBEROS` sin bajar el archivo.
5. Ningún tipo de documento muestra un campo que no le aplica.

## 4. Alcance explícito

**Se mejora `cxp/forms/procesos/consulta-documentos`.** No se crea una pantalla nueva, no se toca la
consulta de documentos electrónicos de `cxc` (esa es la de emisión, otro frente), y no se cambia
ningún endpoint salvo que la medición del §2.1 diga que hace falta un resumen del asiento.
