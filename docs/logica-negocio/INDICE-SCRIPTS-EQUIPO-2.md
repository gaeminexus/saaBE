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
| **e2-05** | `cxp/sql/e2-05-urgente-aplpfctc-debe-aceptar-null.sql` | `PGS.APLP.APLPFCTC` pasa a aceptar `NULL` (cruce contra liquidación) | ⚪ **sin confirmar** |
| **e2-06** | `rhh/sql/e2-06-cuenta-empleado-apunta-a-banco-externo.sql` | `RHH.CBEM`: de banco interno (`TSR.BNCO`) a banco externo (`TSR.BEXT`) | ✅ corrido. Su FK falló en silencio; completada por el `e2-11` |
| **e2-07** | `tsr/sql/e2-07-aplicacion-desde-caja-chica.sql` | `PGS.APLP.APLPMVCH`: un gasto de caja chica puede originar un pago | ✅ **corrido** (confirmado el 2026-09-04) |
| **e2-08** | **`tsr/sql/e2-08-diagnostico-comandos-busqueda.sql`** | Qué fila falta en el catálogo de comandos de búsqueda (`SCP.PDTR`, rubro alterno 71). Es la causa del `WFLYEJB0034` de `selectByCriteria`. **Solo lectura** | 🟡 **PENDIENTE de correr** |
| ~~e2-09~~ | — | *Borrado el 2026-09-04.* Diagnosticaba si `MVCHTPOO` estaba nulo; el DDL de `tsr/sql/02` ya lo garantiza con `NOT NULL` + `CHECK`. Un `.sql` que no hay que correr es ruido | ⛔ no existe |
| **e2-10** | **`cxp/sql/e2-10-retenciones-cargadas-con-cuenta-de-proveedor.sql`** | Retenciones ya cargadas con la cuenta de proveedor de un cliente (bloque A2) y titulares sin cuenta de cliente (B1). **Solo lectura** | ✅ **CERRADO por el usuario el 2026-09-07.** El B1 quedó descartado el 09-04: la cuenta de cliente se parametriza sobre la marcha |
| **e2-11** | `rhh/sql/e2-11-completa-la-fk-que-el-e2-06-no-pudo-crear.sql` | `GRANT` + `FK_CBEM_BEXT` + índice, que el `e2-06` no pudo crear | ✅ **corrido y cerrado** (`FK_CBEM_BEXT ENABLED`) |
| **e2-12** | `rhh/sql/e2-12-verifica-y-completa-fk-e-indices-de-odbs.sql` | Verifica y completa `FK_ODBS_PJRQ` y los índices de `ODBS`/`LQBS` | ✅ **corrido y cerrado** (`FK_ODBS_PJRQ ENABLED`) |

---

## Lo que queda pendiente de correr, en orden de utilidad

| Script | Qué responde | Riesgo |
|---|---|---|
| **`e2-08`** | Cuál de los 15 comandos de búsqueda falta en `SCP.PDTR`. Con eso se escribe el `INSERT` | ninguno, es lectura |
| `e2-05` | — | ⚠️ **no es lectura**: es un `ALTER`. Hay que confirmar si ya corrió antes de nada |
| `e2-01`, `e2-02` | Verificaciones viejas del frente de beneficios | ninguno, son lectura |

---

## Convención, para que esto no vuelva a pasar

1. **El número es del equipo, la carpeta es del módulo.** `e2-NN` va en `docs/logica-negocio/{modulo}/sql/`, donde `{modulo}` es el que toca el script — no donde se escribió.
2. **Agregar la fila a este índice en el MISMO commit que el script.** Un script sin fila acá es un script que alguien va a buscar en la carpeta equivocada.
3. **Prefijo `e2-`** siempre (§2b del registro de reservas), y **la numeración no se reusa**: el `e2-09` está borrado y su número queda muerto.
