# Índice de los scripts `e2-*` — equipo `omen-saa-2`

**Por qué existe:** los scripts se numeran **en serie por equipo** (`e2-01`, `e2-02`, …) pero se
guardan **en la carpeta del módulo al que pertenecen**. O sea que **el número no dice dónde está el
archivo**, y buscar «e2-08» en `rhh/sql/` no lo encuentra porque vive en `tsr/sql/`.

Eso hizo perder tiempo el 2026-09-07. Este índice lo arregla: **una sola tabla con la ruta completa
de cada uno.**

> Última actualización: **2026-09-21** (e2-53). Al agregar un `e2-*` nuevo, agregar la fila acá **en el mismo
> commit**.

---

## Todos los scripts, con su ruta

| # | Ruta completa | Qué hace | Estado |
|---|---|---|---|
| **e2-01** | `rhh/sql/e2-01-verificacion-previa-beneficios.sql` | Verificación previa del frente de beneficios sociales. **Solo lectura** | ⚪ sin constancia de ejecución |
| **e2-02** | `rhh/sql/e2-02-verificacion-entidades-vs-esquema-rhh-cnt.sql` | Contrasta entidades JPA contra el esquema en `rhh` y `cnt`. **Solo lectura** | ⚪ sin constancia |
| **e2-03** | `rhh/sql/e2-03-orden-pago-beneficio-social.sql` | Crea `RHH.ODBS`, su secuencia, `RHH.LQBS.LQBSODBS` y el rubro 310 | ✅ **corrido**. Su FK falló en silencio; completada por el `e2-12` |
| **e2-04** | `rhh/sql/e2-04-corrige-indices-odbs-fuera-de-schema.sql` | Reubica al schema `RHH` dos índices que el `e2-03` creó sin prefijo | ✅ corrido |
| **e2-05** | `cxp/sql/e2-05-urgente-aplpfctc-debe-aceptar-null.sql` | `PGS.APLP.APLPFCTC` pasa a aceptar `NULL` (cruce contra liquidación) | ✅ **corrido y CONFIRMADO el 2026-09-07** — no por el DDL sino por el síntoma: **el cruce con liquidación funciona en producción** |
| **e2-06** | `rhh/sql/e2-06-cuenta-empleado-apunta-a-banco-externo.sql` | `RHH.CBEM`: de banco interno (`TSR.BNCO`) a banco externo (`TSR.BEXT`) | ✅ corrido. Su FK falló en silencio; completada por el `e2-11` |
| **e2-07** | `tsr/sql/e2-07-aplicacion-desde-caja-chica.sql` | `PGS.APLP.APLPMVCH`: un gasto de caja chica puede originar un pago | ✅ **corrido** (confirmado el 2026-09-04) |
| **e2-08** | **`tsr/sql/e2-08-diagnostico-comandos-busqueda.sql`** | Qué fila falta en el catálogo de comandos de búsqueda (`SCP.PDTR`, rubro alterno 71). Es la causa del `WFLYEJB0034` de `selectByCriteria`. **Solo lectura** | ✅ **corrido el 2026-09-07.** Faltan los alternos **12, 13 y 14**. *(La v1 no corría: cuatro columnas inventadas, §28)* |
| **e2-13** | **`tsr/sql/e2-13-inserta-los-tres-comandos-de-busqueda-faltantes.sql`** | ⚠️ **NO es lectura.** Inserta los tres detalles que faltan en `SCP.PDTR`. Valores sacados de `EntityDaoImpl`, no inventados | ✅ **CORRIDO el 2026-09-07.** Los 15 comandos dan OK |
| ~~e2-09~~ | — | *Borrado el 2026-09-04.* Diagnosticaba si `MVCHTPOO` estaba nulo; el DDL de `tsr/sql/02` ya lo garantiza con `NOT NULL` + `CHECK`. Un `.sql` que no hay que correr es ruido | ⛔ no existe |
| **e2-10** | **`cxp/sql/e2-10-retenciones-cargadas-con-cuenta-de-proveedor.sql`** | Retenciones ya cargadas con la cuenta de proveedor de un cliente (bloque A2) y titulares sin cuenta de cliente (B1). **Solo lectura** | ✅ **CERRADO por el usuario el 2026-09-07.** El B1 quedó descartado el 09-04: la cuenta de cliente se parametriza sobre la marcha |
| **e2-11** | `rhh/sql/e2-11-completa-la-fk-que-el-e2-06-no-pudo-crear.sql` | `GRANT` + `FK_CBEM_BEXT` + índice, que el `e2-06` no pudo crear | ✅ **corrido y cerrado** (`FK_CBEM_BEXT ENABLED`) |
| **e2-12** | `rhh/sql/e2-12-verifica-y-completa-fk-e-indices-de-odbs.sql` | Verifica y completa `FK_ODBS_PJRQ` y los índices de `ODBS`/`LQBS` | ✅ **corrido y cerrado** (`FK_ODBS_PJRQ ENABLED`) |
| ~~e2-14~~ | `tsr/sql/e2-14-codigo-institucion-banco-externo.sql` | Agregaba `TSR.BEXT.BEXTCDBC` para el código BCE | ⛔ **CANCELADO el 2026-09-07 por el `e2-17`: la columna NO hace falta.** El código ya estaba en `BEXTTRJT`, con otro nombre. **No correrlo.** Se conserva el archivo como registro de por qué se descartó |
| **e2-15** | `tsr/sql/e2-15-verifica-si-bextcdgo-ya-es-el-codigo-bce.sql` | ¿`TSR.BEXT.BEXTCDGO` ya es el código del BCE? **Solo lectura** | ✅ **CORRIDO el 2026-09-07. Respuesta: NO lo es.** Machala es 5 y Pacífico es 8; la PK es una secuencia corrida de 1 a 389 sin huecos. Y de paso destapó el `e2-16` |
| **e2-16** | **`tsr/sql/e2-16-sincroniza-la-secuencia-de-banco-externo.sql`** | ⚠️ **NO es lectura.** `SQ_BEXTCDGO` quedó en **95** con la tabla en **389**: dar de alta un banco externo desde la pantalla muere con **PK duplicada**. Reinicia la secuencia en 390 | ✅ **CORRIDO el 2026-09-07.** ⚠️ Y al arreglar el alta **quitó la barrera accidental** que impedía usar la pantalla de bancos: ver §33 del estado |
| **e2-18** | **`rhh/sql/e2-18-conceptos-decimo-acumulado-pagado.sql`** | ⚠️ **NO es lectura.** Crea los dos detalles del rubro **221** (alternos 32 y 33) y los dos conceptos **INFORMATIVOS** de `RHH.CPNM` con los que el décimo acumulado pagado vuelve al rol como novedad | ✅ **CORRIDO el 2026-09-21** según el usuario (*«ya corrí todos los sql en producción»*). ⚠️ La salida no llegó al árbitro |
| **e2-17** | **`tsr/sql/e2-17-es-bexttrjt-el-codigo-del-bce.sql`** | ¿`TSR.BEXT.BEXTTRJT` es el código del BCE? **Solo lectura** | ✅ **CORRIDO el 2026-09-07. Respuesta: SÍ.** 387 valores distintos de 389 filas (10 a 9997), las dos anclas OK (Machala 25, Pacífico 30) y `32` = `BANCO INTERNACIONAL`, que es lo que la especificación asume. **Cancela el `e2-14`.** El bloque 5 dio **cero daño**: nadie guardó nunca desde esa pantalla |
| **e2-37** | **`sri/sql/e2-37-por-que-el-ats-de-agosto-salio-sin-ventas.sql`** | Por qué el ATS de agosto salió con CERO ventas, qué son los 18 `<anulados>` (4 de las 7 tablas que consulta son de COMPRA), y las retenciones de agosto que el ATS no declara. **Solo lectura** | 🔴 **PENDIENTE — urgente, 2026-09-09.** Bloquea la declaración de agosto |
| **e2-38** | **`sri/sql/e2-38-las-retenciones-de-compra-estan-en-rcv2-o-en-rtcm.sql`** | El sistema tiene DOS juegos de tablas de retención de compra (`RTCM/DRCM` y `RCV2/DRC2`) y el ATS lee solo el segundo. Agosto: 71 compras y apenas 6 líneas de retención en `DRC2`. **Solo lectura** | 🔴 **PENDIENTE — urgente, 2026-09-09.** Decide si el ATS declara las retenciones reales o 761,58 |
| **e2-39** | **`cxc/sql/e2-39-tipo-identificacion-sujeto-retenido.sql`** | Por qué la retención 216 salió declarando CÉDULA con un RUC de 13 dígitos y el SRI la rechazó (identificador 69). Mide el catálogo del rubro 36, el titular rechazado y cuántos más quedarían mal. **Solo lectura** | 🔴 **PENDIENTE — urgente, 2026-09-09** |

---


---

## ⚠️ DEUDA DE ESTE ÍNDICE — 20 scripts sin fila (medido el 2026-09-14)

La convención #2 de más abajo dice *«agregar la fila a este índice en el MISMO commit que el
script»*, y **se rompió veinte veces seguidas**. La tabla de arriba documenta hasta el `e2-18`; en
disco hay **37 scripts**.

**Sin registrar:** `e2-18b` · `e2-18c` · `e2-19` a `e2-25` · `e2-26` a `e2-36`.

**Lo que hace cara esta deuda no es el desorden.** De los sin registrar, casi todos son de lectura
—se identifican por su cabecera `SOLO LECTURA`— pero **`e2-23` (`borrar-anticipo-huerfano`) es un
borrado y no hay rastro en ningún lado de si se corrió o no**. Un script que escribe y cuya
ejecución nadie anotó es exactamente el caso que este índice existe para evitar.

**Los que sí tienen constancia, por el documento de estado:** `e2-26` y `e2-29` (corridos, §36 y
§37), `e2-31` (corrido, §37), `e2-35` (corrido, §39).

⛔ **Al retomar este índice: no inventar el estado de ninguno.** Si no hay constancia escrita de que
se corrió, la fila dice *«sin constancia»*, no *«pendiente»* ni *«corrido»* — son tres cosas
distintas y confundirlas es peor que la deuda.

---

## Scripts del frente tributario (2026-09-09 → 09-14)

| # | Ruta completa | Qué hace | Estado |
|---|---|---|---|
| **e2-37** | `sri/sql/e2-37-por-que-el-ats-de-agosto-salio-sin-ventas.sql` | Por qué el ATS de agosto salió con CERO ventas, qué son los 18 `<anulados>` y las retenciones del mes. **Solo lectura** | ✅ **CORRIDO el 2026-09-09.** Contestó las tres: agosto tenía **20 facturas en estado 5** (autorizadas) por 24.423,19 y 13 anuladas; **julio no tiene ni una factura** en `CBR.FCTR` —el sistema empezó a emitir en agosto, y por eso el ATS de julio se armó por fuera—; y las retenciones de agosto en `PGS.DRC2` eran apenas 6 líneas por 761,58, que fue la pista del `e2-38` |
| **e2-38** | `sri/sql/e2-38-las-retenciones-de-compra-estan-en-rcv2-o-en-rtcm.sql` | ¿Las retenciones de compra están en `PGS.RTCM` o en `PGS.RCV2`? El ATS leía sólo la segunda. **Solo lectura** | ⛔ **NUNCA SE CORRIÓ, y la pregunta se contestó igual — por el camino caro.** El 2026-09-11 el contador reportó el talón con las retenciones en 0,00, y ahí se midió que **ninguna de las dos era**: las que ASOPREP emite viven en `CBR.RTV2`/`DRV2`. Corregido en `fd3265a8`. **Se conserva como registro de que un script escrito y no corrido no ahorra el problema, sólo lo demora** |
| **e2-39** | `cxc/sql/e2-39-tipo-identificacion-sujeto-retenido.sql` | Por qué la retención 216 declaró CÉDULA con un RUC de 13 dígitos, y cuántos titulares más tienen el tipo incoherente con la longitud. **Solo lectura** | ✅ **CORRIDO el 2026-09-09.** El catálogo del rubro 36 está **impecable** (01-04 con sus códigos SRI) y la titular estaba **bien clasificada**: lo que fallaba era que el código exigía el rubro **padre** —que vale 36 para todos y venía NULL— para leer el hijo. Y sólo **2 titulares** de 110 tienen el tipo incoherente, los dos marcadores de migración ya conocidos (`SUPER PACO`, `CLIENTES A JULIO 2026`) |

## Scripts de pagos a liquidaciones y notas de venta (2026-09-14 →)

| # | Ruta completa | Qué hace | Estado |
|---|---|---|---|
| **e2-40** | `cxp/sql/e2-40-liquidaciones-y-notas-de-venta-por-pagar.sql` | Antes de diseñar el pago de liquidaciones desde Solicitud de pago: forma de `PGS.PGTR` (¿existe `PGTRLQCC`?), liquidaciones emitidas con/sin documento CXP, `PGS.LQCC` por estado y con saldo, y las notas de venta (`FCTC` tipo `02`). **Solo lectura** | ⚪ opcional — el usuario confirmó que las columnas no existían y pidió avanzar; el `e2-41` repite los controles que importan |
| **e2-41** | `cxp/sql/e2-41-pago-programado-a-liquidacion-de-compra.sql` | ⚠️ **NO es lectura.** `PGS.PGTR.PGTRLQCC` + `FK_PGTR_LQCC` + índice: un pago programado puede pagar una liquidación de compra (`PGS.LQCC`). **Va ANTES del WAR** — si no, `ORA-00904` en todo el circuito de pagos | ✅ **CORRIDO el 2026-09-14** según el usuario, antes del WAR `cd800803`. La salida de los bloques 3.x no llegó al árbitro |
| **e2-42** | `tsr/sql/e2-42-identificacion-de-la-cuenta-bancaria-del-titular.sql` | ⚠️ **NO es lectura.** `TSR.CTBN.CTBNTPID` + `CTBNIDNT` + `CK_CTBN_IDENTIFICACION`: la identificación con la que se abrió la cuenta, para el archivo del banco. Bloque 4 = lectura de cuentas a revisar. **Va ANTES del WAR** — si no, `ORA-00904` en toda lectura de cuentas de titulares | ✅ **CORRIDO el 2026-09-21** según el usuario. El WAR de `main` ya se puede desplegar sin ORA-00904 en cuentas de titulares |

## Scripts de retenciones sobre notas de venta (2026-09-15 →)

| # | Ruta completa | Qué hace | Estado |
|---|---|---|---|
| **e2-43** | `cxc/sql/e2-43-retencion-sobre-nota-de-venta.sql` | Antes de emitir retenciones sobre notas de venta: si el combo tiene el tipo `02` (`CBR.TSRI`, LSRI 3), cuántas notas de venta comparten autorización (trampa del ATS), números repetidos entre factura y nota de venta, y retenciones **ya emitidas** sobre notas de venta — incluidas las que salieron declarando `01`. **Solo lectura** | ✅ **CORRIDO el 2026-09-15.** El `02` está activo en el combo (`TSRI` id 7). `FCTC`: 181 facturas por 177.565,27 y **2 notas de venta** por 117,50. Bloques 3, 3b, 4 y 5 **vacíos**: ninguna autorización compartida, ningún número repetido y **ninguna retención emitida todavía sobre una nota de venta**. La trampa del ATS está latente, no activa; el BE-3 del plan no hace falta |
| **e2-44** | `sri/sql/e2-44-tipocliente-pasaporte-c05580508.sql` | Por qué el `AT082026` (generado 14/9 17:27) sale sin `<tipoCliente>` para el pasaporte `C05580508`, si el código que lo escribe está desde `b170911e` (11/9). Titular(es) con esa identificación y sus facturas de agosto. **Solo lectura** | ✅ **CORRIDO el 2026-09-15. El dato está bien:** un solo titular (65, AGHAYAR SEYIDOV), tipo de identificación H=3 (pasaporte → `06`), **tipo de persona H=1 (NATURAL)**, `TTLRTPAT` nulo; una factura de agosto (147, `001-001-000000774`, 770,01, estado 5). ~~Con ese dato `resolverTipoClienteVenta` devuelve `01` por cualquier camino — el `AT082026` no lo generó el código vigente.~~ ⛔ **CONCLUSIÓN FALSA, corregida el mismo día.** Supuse que `PDTRVLRV` del rubro 35 era nulo porque así lo anotaba este índice el 11-09. Al regenerar, la pantalla avisó *«el catálogo del rubro 35 devolvió 'null'»*: **es el TEXTO `null`**, que el código rechaza con un `return null` **antes** de llegar al respaldo por alterno. El WAR sí es el vigente. Ver `e2-45` |
| **e2-45** | `sri/sql/e2-45-rubro-35-tipo-persona-valor-sri.sql` | ⚠️ **NO es solo lectura.** Control del texto `'null'` en el rubro 35, conteo de la familia en todo `SCP.PDTR`, y `UPDATE` de los dos detalles a `01` (NATURAL) / `02` (JURÍDICO), la Tabla 14 del ATS. `COMMIT` comentado, reverso comentado | ✅ **CORRIDO el 2026-09-15** — al regenerar el ATS el aviso del titular 65 desapareció |
| **e2-46** | `sri/sql/e2-46-liquidaciones-agosto-base-gravada-ats.sql` | `PGS.LQCC.SUBTOTAL` es «total sin impuestos» si vino del XML y «base gravada» si la emitió ASOPREP (`LiquidacionCompraServiceImpl:813`); el ATS resta `SUBCERO` en los dos casos. Mide las liquidaciones de agosto y cuánto gravado declararía de menos. **Solo lectura** | ✅ **CORRIDO el 2026-09-21** según el usuario. ⚠️ **Es de lectura y su salida no llegó al árbitro** — lo que midió se perdió |
| **e2-47** | `cxp/sql/e2-47-nota-venta-numero-a-15-digitos.sql` | ⚠️ **NO es solo lectura.** Completa con ceros el número de las notas de venta manuales (3-3-9). La retención 278 volvió DEVUELTA por `numDocSustento` de 13 dígitos. Bloque 0 guarda el original, bloque 1 controla choques, `COMMIT` comentado | ✅ **CORRIDO el 2026-09-21** según el usuario. Las notas de venta manuales ya tienen el número a 15 dígitos |
| **e2-48** | `cxp/sql/e2-48-observacion-adicional-documento-cxp.sql` | ⚠️ **DDL.** `PGS.DCXP.DCXPOBAD VARCHAR2(500 CHAR)`: la observación adicional que el usuario escribe al registrar un XML. **Va ANTES del WAR** — si no, `ORA-00904` en toda lectura de `DocumentoCxp` | ✅ **CORRIDO el 2026-09-15** según el usuario. Ya se puede desplegar el WAR de `main` |
| **e2-49** | `tsr/sql/e2-49-retenciones-autorizadas-sin-cruce.sql` | Retenciones V2 autorizadas con total > 0 que no rebajaron su documento (sin `PGS.APLP` activa): en el estado de cuenta restan y la factura no baja. Resumen y detalle con tipo de documento sustento. **Solo lectura** | ✅ **CORRIDO el 2026-09-21** según el usuario. ⚠️ **Es de lectura y su salida no llegó al árbitro** — lo que midió se perdió |
| **e2-50** | `tsr/sql/e2-50-nombre-titular-cuenta-bancaria.sql` | ⚠️ **DDL.** `TSR.CTBN.CTBNNMBR VARCHAR2(200 CHAR)`: el nombre de la persona a cuyo nombre está la cuenta, cuando no es el titular del pago. El bloque 3 lista las cuentas que ya tienen identificación propia distinta. **Va ANTES del WAR** | ✅ **CORRIDO el 2026-09-21** según el usuario. Con el `e2-42`, el WAR de `main` ya no rompe la lectura de cuentas |
| **e2-51** | `sri/sql/e2-51-ats-agosto-bases-y-retenciones.sql` | Por qué el ATS de agosto declara la base 0% en la columna de base gravada (cabecera `FCTC.SUBCERO` contra el detalle `DFCC.CODIGOIVASRI`), por qué las retenciones emitidas salen en 0 (enlace autorización + número) y si el detalle de las retenciones recibidas tiene datos en la base. **Solo lectura** | ✅ **CORRIDO el 2026-09-16.** Bloque 1: las 10 facturas del proveedor de la captura son **100% al 0%** en el detalle y la cabecera guarda 7,23 como base 0% — el dato de la carga está mal, no el ATS. Bloque 2 falló con ORA-22818 (subconsulta en el `GROUP BY`, error mío; rehecho en el `e2-52`). Bloque 3: **todas las retenciones de agosto enlazan**, salvo la 206, que es sobre una liquidación de compra (tipo `03`). Bloque 4: el detalle de las retenciones recibidas **sí tiene valores** en la base |
| **e2-52** | `sri/sql/e2-52-corrige-base-cero-compras-cargadas.sql` | ⚠️ **NO es lectura.** Pone la base 0% correcta en las compras ya cargadas cuyo detalle dice que son 100% al 0% (el ATS las declaraba como gravadas). Las mixtas se listan y no se tocan. `COMMIT` comentado | ✅ **CORRIDO el 2026-09-21** según el usuario. ⚠️ **Falta la salida de sus BLOQUES 0 a 2** — el bloque 2 lista las facturas MIXTAS que el script deliberadamente NO toca y hay que revisar a mano. **Y su criterio deja pasar el detalle con `CODIGOIVASRI` en NULL: ese hueco lo cubre el `e2-53`** |
| **e2-53** | `sri/sql/e2-53-nota-de-venta-base-cero.sql` | ⚠️ **NO es lectura.** La nota de venta se declara con base **gravada** y debe ir en base 0%: pone `SUBCERO = SUBTOTAL` en las notas de venta sin IVA y completa su detalle con `CODIGOIVASRI = 0`. Es el hueco que el `e2-52` deja abierto (su criterio clasifica el código en NULL como gravado). `COMMIT` comentado, reverso comentado | 🔴 **PENDIENTE — urgente, 2026-09-21.** Arregla el ATS **sin necesidad de WAR** |

### Consulta suelta que quedó sin script y vale anotarla

El **catálogo del rubro 35** (tipo de persona) se consultó el 2026-09-11 sin escribir un `e2-*`,
porque eran cuatro líneas. Resultado, y de ahí salió el mapeo del `tipoCliente` del ATS:

```sql
select d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV
  from SCP.PDTR d join SCP.PRBR r on r.PRBRCDGO = d.PRBRCDGO
 where r.PRBRALTR = 35;
--  1  NATURAL    (PDTRVLRV = NULL)
--  2  JURIDICO   (PDTRVLRV = NULL)
```

⛔ **Corregido el 2026-09-15:** ese «NULL» era **el texto `null`**, no un nulo — el cliente SQL los muestra
igual. El ATS lo delató al avisar *«devolvió 'null'»*. Ver `e2-45`.

**Las dos filas con el `valorAlfanumerico` vacío**, que es por lo que el ATS deriva el `01`/`02`
desde el alterno. Llenar esas dos celdas con `'01'`/`'02'` haría innecesario el mapeo en código —
**pendiente, y es un `UPDATE` a un catálogo compartido entre los cuatro equipos**, así que no se
metió el día de una declaración.
## Lo que queda pendiente de correr

De los quince scripts: **catorce corridos**, uno borrado (`e2-09`), **uno cancelado** (`e2-14`) y
**ninguno bloqueando nada**.

| Script | Estado |
|---|---|
| ⛔ **`e2-14`** | **CANCELADO — NO CORRERLO.** El `e2-17` midió que el código del BCE ya está en `BEXTTRJT`. La columna `BEXTCDBC` que este script agregaba **no hace falta**, y los seis códigos que estábamos esperando del usuario **estaban en la tabla desde siempre** |
| `e2-01`, `e2-02` | Verificaciones **de lectura** del frente de beneficios sociales, de principios de septiembre. No arreglan nada ni bloquean nada: contrastan entidades contra el esquema. Correrlas es higiene, no urgencia |

> **Cerrado el 2026-09-07:** el `e2-17` destrabó el frente de archivos bancarios sin una sola línea
> de DDL. **La respuesta era un `SELECT`, no un `ALTER TABLE`** — y el `e2-14`, ya escrito y listo
> para correr, habría agregado una columna duplicada al lado de la que ya tenía el dato.

> **Cerrado el 2026-09-07:** el `e2-13` dejó los 15 comandos de búsqueda en `OK`, y con eso
> `selectByCriteria` —que es transversal a **todos** los módulos, no sólo a los nuestros— deja de
> reventar con `WFLYEJB0034`.

---

## Convención, para que esto no vuelva a pasar

1. **El número es del equipo, la carpeta es del módulo.** `e2-NN` va en `docs/logica-negocio/{modulo}/sql/`, donde `{modulo}` es el que toca el script — no donde se escribió.
2. **Agregar la fila a este índice en el MISMO commit que el script.** Un script sin fila acá es un script que alguien va a buscar en la carpeta equivocada.
3. **Prefijo `e2-`** siempre (§2b del registro de reservas), y **la numeración no se reusa**: el `e2-09` está borrado y su número queda muerto.
4. ⛔ **SQL puro: ni un `PROMPT`, ni un `SET`, ni un `COLUMN`.** Son comandos de **SQL*Plus**, no de
   Oracle, y **el cliente del usuario no los interpreta**: los escupe como texto en el medio de la
   salida y la vuelve ilegible. Pasó el 2026-09-07 con la primera versión del `e2-17`.
   **Para rotular, dos cosas y nada más:**
   - Comentarios `--` como banner de bloque, con una línea `ESPERADO SI ...` que diga qué se espera
     ver. Es lo que ya hacían el `e2-08` y el `e2-15`, los dos que corrieron sin problema.
   - Una **columna literal** al principio de cada `SELECT` (`SELECT 'BLOQUE 2 - anclas' AS bloque, …`),
     para que cada resultado se identifique solo cuando el usuario pega la salida de vuelta.

   El motivo de fondo: **el script lo corre una persona en su cliente, no nosotros en una consola.**
   Todo lo que dependa de la herramienta y no del motor es una suposición sobre una máquina que no
   vemos.
