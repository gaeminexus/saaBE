# Índice de los scripts `e2-*` — equipo `omen-saa-2`

**Por qué existe:** los scripts se numeran **en serie por equipo** (`e2-01`, `e2-02`, …) pero se
guardan **en la carpeta del módulo al que pertenecen**. O sea que **el número no dice dónde está el
archivo**, y buscar «e2-08» en `rhh/sql/` no lo encuentra porque vive en `tsr/sql/`.

Eso hizo perder tiempo el 2026-09-07. Este índice lo arregla: **una sola tabla con la ruta completa
de cada uno.**

> Última actualización: **2026-09-07** (e2-10 cerrado). Al agregar un `e2-*` nuevo, agregar la fila acá **en el mismo
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
| **e2-18** | **`rhh/sql/e2-18-conceptos-decimo-acumulado-pagado.sql`** | ⚠️ **NO es lectura.** Crea los dos detalles del rubro **221** (alternos 32 y 33) y los dos conceptos **INFORMATIVOS** de `RHH.CPNM` con los que el décimo acumulado pagado vuelve al rol como novedad | 🔴 **PENDIENTE.** Va **antes del WAR**. Su bloque 0 es control previo: correrlo solo y leerlo antes de seguir |
| **e2-17** | **`tsr/sql/e2-17-es-bexttrjt-el-codigo-del-bce.sql`** | ¿`TSR.BEXT.BEXTTRJT` es el código del BCE? **Solo lectura** | ✅ **CORRIDO el 2026-09-07. Respuesta: SÍ.** 387 valores distintos de 389 filas (10 a 9997), las dos anclas OK (Machala 25, Pacífico 30) y `32` = `BANCO INTERNACIONAL`, que es lo que la especificación asume. **Cancela el `e2-14`.** El bloque 5 dio **cero daño**: nadie guardó nunca desde esa pantalla |

---

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
