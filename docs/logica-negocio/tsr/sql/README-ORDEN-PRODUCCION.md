# Orden de ejecución en PRODUCCIÓN — scripts de esta tanda

**Fecha:** 2026-08-27 · **Contexto:** cheques, caja chica, liquidaciones de compra.

En local los scripts se fueron corrigiendo sobre la marcha, así que hay archivos
que **solo sirvieron para reparar la base local** y no deben ejecutarse en
producción. Los scripts que sí van ya están consolidados: cada uno se corre una
vez, de arriba abajo, y deja la base en el estado final.

**Regla general:** el DDL va **antes** de desplegar el WAR, y una fase solo pasa
a producción cuando está probada en local.

---

## Qué ejecutar, en este orden

| # | Script | Contenido | Notas |
|---|---|---|---|
| 1 | `tsr/sql/01-cheques-pago-programado.sql` | `CNBC.CNBCCHQR`, `PGTR.PGTRFPAG` + `PGTRDTCH` con su FK y el índice **único** `UQ_PGTR_DTCH`, backfill de la forma de pago, detalles 3 y 4 del rubro 38 | Ya consolidado: crea el índice único directamente |
| 2 | `tsr/sql/04-cheques-fk-titular.sql` | Repunta `DTCH.PRSNCDGO` de `TSR.PRSN` a `TSR.TTLR` y agrega la FK que faltaba en `DTCHIDBN` | **Necesario también en producción**: `DTCH` es tabla heredada y trae la FK vieja |
| 3 | `tsr/sql/06-pgtr-titular-nullable.sql` | `PGTR.PGTRTTLR` pasa a nullable | Sin esto ningún pago de origen externo (caja chica, devolución de aportes CRD) puede grabarse |
| 4 | `tsr/sql/02-caja-chica.sql` | Bloque 0 de `GRANT REFERENCES`, las 4 tablas (`CJCH`, `CRCH`, `MVCH`, `PTCH`) y los rubros 232/233 | Ya consolidado: el custodio nace apuntando a `RHH.MPLD` y los GRANT van primero. **El bloque 6 NO se ejecuta** en esta tanda |
| 5 | `cxc/sql/add-liquidacion-compra-emision.sql` | Fila `CBR.NXPE` para tipo `03` y `LQCS.LQCSLQCC` con su FK, incluido el `GRANT REFERENCES ON PGS.LQCC TO CBR` | El `GRANT` va **antes** del `ALTER ... ADD CONSTRAINT` |
| 6 | `tsr/sql/07-conciliacion-transito.sql` | Secuencia `TSR.SQ_DTCNCDGO`, tabla **`TSR.DTCN`** con sus 4 índices, y `ALTER TABLE TSR.CNCL ADD` (`CNCLESTD`) | ⛔ **Agregado el 2026-08-31, ver abajo.** Sin esto **cualquier lectura de `DetalleTransito` da ORA-00942**, porque la entidad está mapeada (`model/tsr/DetalleTransito.java:29`) |
| 7 | `tsr/sql/08-rubros-partidas-transito.sql` | Rubros **239** (tipo de partida), **240** (estado de partida) y **241** (estado de cierre) en `SCP.PRBR`, con sus 9 detalles `PDTR` 1151-1159 | Va **después** del `07`: su bloque de control consulta `TSR.DTCN`. Revalidar el `MAX` de `PRBR`/`PDTR` justo antes de ejecutar (regla 2 del registro de reservas) |

## Qué NO ejecutar

| Script | Por qué |
|---|---|
| `tsr/sql/03-cheques-unicidad.sql` | Histórico. Reparó la base local, donde el script 01 se había corrido en una versión anterior que creaba un índice no único y sin prefijo de schema. El 01 consolidado ya crea el único |
| `tsr/sql/05-caja-chica-custodio.sql` | Histórico. Repuntaba el custodio de `SCP.PJRQ` a `RHH.MPLD`; el script 02 consolidado ya lo crea así |

---

## Dos trampas de Oracle que costaron tiempo aquí

1. **Una FK hacia otro schema exige `GRANT REFERENCES` directo.** Oracle no
   considera los privilegios heredados por **rol** al crear un constraint, así
   que ni con rol DBA basta. Muchas tablas viejas ya lo tienen concedido a
   `PUBLIC` y por eso la mayoría de FK cross-schema no falla; `PGS.PGTR`,
   `PGS.PRDP`, `PGS.LQCC` y `RHH.MPLD` **no** lo tienen. Por eso los `GRANT`
   van primero en los scripts.

2. **`CREATE INDEX` sin prefijo de schema deja el índice en el schema de la
   sesión**, no en el de la tabla. Ocupa la columna igual (un
   `CREATE UNIQUE INDEX` posterior falla con **ORA-01408**) pero es invisible si
   el control filtra por `OWNER`: hay que filtrar por **`TABLE_OWNER`**. Todos
   los `CREATE INDEX` de estos scripts van prefijados.

## Después de ejecutar

Correr los `SELECT` de control de cada bloque y compararlos con lo que dieron en
local. Las dos bases arrancan iguales, así que cualquier diferencia es un script
ya aplicado en un lado y no en el otro — no una diferencia de fondo.
