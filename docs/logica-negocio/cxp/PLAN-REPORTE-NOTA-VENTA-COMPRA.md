# Reporte de respaldo de la nota de venta de compra

**Equipo:** `omen-saa-2` · **Creado:** 2026-09-07 · **Estado:** diseño congelado, listo para implementar.
**Frente padre:** `PLAN-NOTA-VENTA-COMPRA-MANUAL.md`

---

## 0. El pedido y las decisiones del usuario

**Textual:** *«Se requiere un reporte similar al RIDE para estas notas de venta. Es verdad que no
existe uno oficial dado por el SRI, pero podemos hacer uno similar, que no sea igual al de facturas
pero que sí sea similar.»*

Y en el mismo mensaje cerró las dos preguntas que quedaban del frente padre:

| # | Decisión |
|---|---|
| **D5** | El tipo de comprobante **es `"02"`** — confirmado con contabilidad. La constante ya estaba bien, no se toca |
| **D6** | **Las notas de venta NO llevan IVA**: son de contribuyentes **RISE** |
| **D7** | El reporte es **similar al RIDE, no igual**. No existe RIDE oficial del SRI para este documento |

**No hay DDL en este frente ni en el padre.** Verificado: `docs/logica-negocio/cxp/sql/` no tiene
ningún script de nota de venta. Nada que correr en la base.

---

## 1. ⛔ La trampa que decide si esto se puede terminar o no

**El `.jasper` NO lo puede generar ningún agente. Lo tiene que generar el usuario con Jaspersoft
Studio 7.0.3.**

`CLAUDE.md` lo documenta con una tarde perdida detrás: en JasperReports **7.0.3 no hay compilación
en tiempo de ejecución que funcione** —`JRJaninoCompiler` no existe y `JRJdtCompiler` se movió a un
artefacto que no está en el `pom`—, y `ReporteServiceImpl:110` busca el `.jasper` y **sólo cae al
`.jrxml` si no lo encuentra… y ese respaldo está muerto.

> **Un reporte con sólo `.jrxml` compila en el IDE, pasa la revisión, entra en el commit y revienta
> la primera vez que un usuario lo ejecuta.** Ya pasó con los siete de `rhh`.

**Verificado el 2026-09-07:** los cinco RIDE que existen (`rep/cxc/RPRT_RIDE_FACTURA`,
`_LIQUIDACION`, `_NOTA_CREDITO`, `_NOTA_DEBITO`, `_RETENCION_V2`) **tienen los dos archivos**. El
patrón está establecido y hay que respetarlo.

**Consecuencia operativa: este frente se entrega en dos mitades.** El agente entrega el `.jrxml` y
la pantalla; **el usuario genera el `.jasper` y lo commitea**. Sin ese paso el botón de imprimir
falla en producción, no en la prueba.

---

## 2. Nombre y ubicación

| Qué | Valor | Por qué |
|---|---|---|
| Archivo | `src/main/resources/rep/cxp/RPRT_NOTA_VENTA_COMPRA.jrxml` (+ `.jasper`) | `ReporteServiceImpl` resuelve `/rep/{modulo}/{nombre}`. **La carpeta `rep/cxp/` no existe todavía: se crea** |
| Módulo | `cxp` | Es un documento de compra |
| Nombre | **`RPRT_NOTA_VENTA_COMPRA`**, NO `RPRT_RIDE_*` | **No es un RIDE.** Los cinco `RPRT_RIDE_*` son representaciones de comprobantes electrónicos que la empresa **emite** y que el SRI autorizó. Éste es un respaldo interno de un documento **recibido** en papel. Llamarlo RIDE sería mentir en el nombre, que es como empiezan los errores que este equipo viene registrando toda la semana |
| Parámetro | `P_ID_FACTURA` (Long) | Mismo nombre que el RIDE de factura: es el `id` de `PGS.FCTC`, y la nota de venta vive en esa misma tabla |

---

## 3. 🔴 Lo que el reporte NO debe parecer

**Un documento que se imprime y se archiva puede terminar en manos de un auditor.** Este no es un
comprobante autorizado por el SRI y no debe poder confundirse con uno.

**Obligatorio en el diseño:**

- **Una leyenda visible, no en letra chica**, del tipo: *«DOCUMENTO INTERNO DE RESPALDO — No es un
  comprobante autorizado por el SRI. El comprobante válido es la nota de venta física del
  proveedor.»*
- **Sin** clave de acceso, **sin** código de barras, **sin** «AUTORIZADO», **sin** ambiente ni fecha
  de autorización electrónica. La nota de venta no tiene ninguno de esos datos, y ponerlos vacíos
  con su etiqueta es peor que no ponerlos.
- El número de autorización de la preimpresa **sí** va, rotulado como lo que es
  («Autorización de la preimpresa»), no como una autorización electrónica.

> **El pedido del usuario ya venía con este instinto** —«que no sea igual al de facturas»— y vale la
> pena decir por qué tiene razón: **parecerse de más es el riesgo, no parecerse de menos.**

---

## 4. Contenido

**Cabecera**
- Datos de la empresa (los mismos que usa el RIDE de factura, para no inventar otra fuente)
- Título: **NOTA DE VENTA** · leyenda del §3
- Número: `EEE-PPP-SSSSSSSSS` · Fecha de emisión · Autorización de la preimpresa

**Proveedor**
- Nombre / razón social · RUC · dirección y teléfono si están en el titular

**Detalle** — una fila por `DetalleFacturaCompra`
- Descripción · Cantidad · Precio unitario · Descuento · **Total de línea**
- ⛔ **SIN columna de IVA** (D6). Una columna de ceros invita a preguntarse si falta algo

**Totales**
- Subtotal · Descuento · **TOTAL**
- ⛔ Sin línea de IVA, por lo mismo

**Pie**
- Observación
- **Usuario que la registró y fecha de registro.** Es un documento tipeado a mano: quién lo tipeó es
  parte del respaldo, y ningún RIDE lo lleva porque allá no hace falta

---

## 5. La consulta

⛔ **Columnas explícitas. Nada de `SELECT *`.** Es la trampa del §Reportes de `CLAUDE.md`: con
`SELECT *`, cuando dos tablas del `FROM` comparten un nombre de columna, Jaspersoft Studio renombra
la segunda a `COLUMN_n` **y ese campo se resuelve por posición**. Un `ALTER TABLE ... ADD` en
cualquiera de las tablas corre todo lo que viene después y el reporte imprime el dato de otra
columna, **sin error**. Pasó el 2026-08-27 con `RPRT_MVMN_APXT`.

**Verificado:** `RPRT_RIDE_FACTURA.jrxml` ya lista columnas explícitas. **Copiar esa disciplina.**

Y cuando dos tablas del `FROM` compartan nombre de columna, **alias explícito**
(`t.TTLRCDGO AS TTLR_TTLRCDGO`), nunca dejar que la herramienta invente el nombre.

**Filtro:** `FCTC.ID = $P{P_ID_FACTURA}`. **No** hace falta filtrar por `TIPOCOMPROBANTE = '02'`:
el id ya identifica la fila, y filtrar de más haría que el reporte devuelva vacío si alguna vez se
imprime desde otro lado.

---

## 6. Orden de ejecución

| Paso | Quién | Qué |
|---|---|---|
| 1 | agente BE | El `.jrxml` en `rep/cxp/`, con el layout del §3-§4 y la consulta del §5 |
| 2 | **el usuario** | ⛔ **Abrir el `.jrxml` en Jaspersoft Studio 7.0.3, generar el `.jasper` y commitear LOS DOS** |
| 3 | agente FE | Botón «Imprimir» en la pantalla, llamando a `jasperReportes.generar('cxp', 'RPRT_NOTA_VENTA_COMPRA', { P_ID_FACTURA: id }, 'PDF')` — mismo patrón que `facturas-ingreso.component.ts:793` |
| 4 | usuario | Probar de punta a punta |

**El paso 2 no se puede saltear ni delegar.** Es el único del frente que ningún agente puede hacer.

---

## 7. Entregado el `.jrxml` — `97694e0`, verificado por el árbitro

`src/main/resources/rep/cxp/RPRT_NOTA_VENTA_COMPRA.jrxml` (16,8 KB, carpeta `rep/cxp/` creada).

| Control | Resultado |
|---|---|
| `SELECT *` | **0 ocurrencias** — columnas explícitas y alias únicos en todos los campos |
| Parámetro | `P_ID_FACTURA`, único |
| Menciones de «IVA» | **0** — cumple D6 |
| Leyenda del §3 | presente: *«DOCUMENTO INTERNO DE RESPALDO — No es un comprobante autorizado por el SRI»* |

### Dos criterios del agente que conviene registrar

**1. Desvío justificado, y correcto:** decidió **no imprimir los badges regulatorios** que sí lleva
el RIDE (agente de retención, contribuyente especial, RIMPE, obligado a contabilidad). El plan no lo
prohibía explícitamente. **Son insignias de comprobante autorizado**, y ponerlas habría trabajado en
contra del §3. Lo marcó como desvío en vez de hacerlo callado, que es lo que permite revisarlo.

**2. Un hueco reportado en vez de inventado — y verificado acá:** el pie trae sólo
*«Registrado por: \<usuario\>»*, **sin fecha**, porque **`PGS.FCTC` no tiene columna de fecha de
creación**. Sus únicas fechas son `FECHA` (emisión) y `FECHAAUTORIZACION`.

⚠️ **Y de paso destapó una columna muerta.** `FCTCFCRG` (`fechaRegistroContable`) parecía la
candidata, pero **nadie la puebla**: verificado, los únicos `setFechaRegistroContable` del proyecto
son **los setters de las entidades**, en `FacturaCompra`, `LiquidacionCompraCompra`,
`NotaCreditoCompra` y `NotaDebitoCompra`. **Cuatro entidades declaran la columna y ninguna la
escribe.** Es el §2.6 otra vez —*«para saber si un ciclo existe hay que preguntar quién lo escribe,
no quién lo declara»*— y esta vez lo encontró un agente buscando dónde sacar una fecha.

**Pendiente derivado (no urgente):** el nombre sugiere que es para el **ATS**. Si algún día el ATS
la necesita, hoy iría en `NULL` para los cuatro documentos. **No verificado si el ATS la lee.**

**Si el usuario quiere la fecha real en el pie**, hace falta **una columna nueva** en `PGS.FCTC`.
Es DDL, y este frente se vendió como «sin DDL»: **queda como opción, no se hace por iniciativa
propia.**
