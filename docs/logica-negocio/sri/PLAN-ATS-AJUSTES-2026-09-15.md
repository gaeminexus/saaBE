# PLAN — Tres ajustes al ATS (compras): retenciones por documento, liquidaciones emitidas e intermediario

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-15 · **Estado:** diseño en disco, despachado
**Alcance:** el usuario incorporó `sri` al equipo el 2026-09-15.
**Archivo principal:** `src/main/java/com/saa/ejb/sri/serviceImpl/GeneradorAtsServiceImpl.java`

Sin DDL. Sin cambio de contrato: `POST /rest/ats/generar` responde igual; cambia el contenido del XML y
aparecen avisos nuevos.

---

## 1. Retención ↔ compra por autorización **y número** (antes: sólo autorización)

**Origen:** `cxc/PLAN-RETENCION-SOBRE-NOTA-DE-VENTA.md` §1.5.

**Hoy:** `cargarRetencionesCompra` (`:1075-1135`) agrupa por `DetalleRetencionV2.docResAutorizacion`, y cada
compra busca con `retenciones.get(c.autorizacion)` (`:697`). En un electrónico la autorización es la clave de
49 dígitos, única. **En una nota de venta preimpresa es la del talonario y la comparten varias notas**: la
retención de una se pegaría a todas. Medido con el `e2-43` (bloque 3 vacío): hoy no pasa.

**Cambio:**
- Clave del mapa = `autorización + "|" + número de 15 dígitos`.
  - Del lado retención: `DetalleRetencionV2.numDocReten` normalizado.
  - Del lado compra: `establecimiento + puntoEmision + secuencial` de `LineaCompra`, normalizado igual.
- **La normalización es UNA sola función**, la misma que ya usa `RetencionV2ServiceImpl.normalizarNumDocSustento`
  (`1b44e51c`). Hoy es privada: se mueve a una clase utilitaria en `com.saa.ejb.cxc.util` (mismo patrón que
  `ProveedorSistemaSri`) y la usan los dos. Dos copias de la regla son el defecto del §24 esperando pasar.
- **Aviso nuevo, obligatorio:** toda retención autorizada del período que **no** quedó enlazada a ninguna
  compra se lista en `avisos`, con su número y el documento sustento. Antes una retención con la autorización
  correcta y el número mal tipeado se declaraba igual; con la clave nueva desaparecería **en silencio** del
  ATS. El aviso es lo que impide que el arreglo cree un defecto peor que el que corrige.

## 2. Liquidación de compra EMITIDA: la base gravada es `SUBTOTAL`, no `SUBTOTAL − SUBCERO`

**Origen:** avisos del ATS de agosto (liquidaciones 2 y 3) + `sri/sql/e2-46`.

**Hoy:** `comprasLiquidacion` (`:278-310`) llama `baseGravadaCompra(subtotal, subcero)` = `subtotal − subcero`.
El javadoc de `baseGravadaCompra` lo justifica verificando la **carga por XML**, donde
`SUBTOTAL = totalSinImpuestos`. Pero `PGS.LQCC` tiene un **segundo origen**:
`LiquidacionCompraServiceImpl.crearDocumentoCxp` (`:813`) copia desde `CBR.LQCS`, donde la pantalla graba
`subtotal = subtotalGravado` (`liquidaciones.component.ts:661`) y el XML emitido arma
`totalSinImpuestos = SUBTOTAL + SUBCERO` (`:1035`). Para esas, la gravada correcta es `SUBTOTAL`.

**Cómo distinguir el origen — por el dato, no por una marca nueva:** una `LQCC` es emitida por ASOPREP si
alguna `LiquidacionCompra` (`CBR.LQCS`) la referencia en `documentoCxp` (`LQCSLQCC`). Una sola consulta con
los ids de las `LQCC` del período → `Set<Long>` de emitidas.

**Cambio:** para las emitidas, gravada = `SUBTOTAL` (y el aviso de «SUBCERO mayor que SUBTOTAL» no aplica); para
el resto, lo de hoy. Javadoc de `baseGravadaCompra` corregido: hoy afirma una verificación que cubre sólo uno
de los dos orígenes.

## 3. Facturas de intermediario: fuera del ATS

**Pedido del usuario, textual (2026-09-15):** *«Las facturas que luego de cargarlas y al procesarlas se las
marca con el check de intermediario, esas no deben incluirse en el ATS.»*

**Dato:** `FacturaCompra.esIntermediario` (`PGS.FCTC.FCTCESIN`, `FacturaCompra.java:311`), 0/1, **columna
NULLABLE** (`lap1-08`): nulo = no es intermediario.

**Cambio:** en `comprasFacturaCompra` (`:241-276`) el JPQL agrega
`and (f.esIntermediario is null or f.esIntermediario <> 1)`. **No `nvl`/`coalesce` en JPQL**: ya reventó una vez
(§36 del estado).

**Aviso nuevo:** si una factura de intermediario excluida tiene una **retención emitida** enlazada, se avisa.
Esa retención quedaría sin compra donde declararse, y decidir qué hacer con ella es del contador.

**Fuera de alcance (pregunta abierta al usuario):** los cuadres 103/104 (`ReporteCuadreSriServiceImpl`) no
se tocan. Si también deben excluir las de intermediario, es otro ítem.

---

## 4. Criterio de aceptación

1. ATS de agosto regenerado: el mismo número de compras **menos** las facturas con `FCTCESIN = 1` del período.
2. Liquidaciones 2 y 3: desaparece el aviso de «SUBCERO mayor que SUBTOTAL»; su `baseImpGrav` sigue en 0.00
   (todo al 0%). Una liquidación emitida con parte gravada: `baseImpGrav = SUBTOTAL`.
3. Las retenciones de agosto (4.642,52) siguen sumando lo mismo en `<compras>`, **o** las que no se enlacen
   aparecen en los avisos con su número.
