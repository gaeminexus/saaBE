-- =====================================================================
-- e2-38 — ¿Las retenciones de compra de agosto estan en PGS.RCV2 o en
--         PGS.RTCM? El generador del ATS solo lee la primera
-- Modulo: sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-09
--
-- SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
-- Correr entero y pegar la salida completa.
--
-- POR QUE EXISTE
--   El e2-38 sale de un numero raro del e2-37: agosto tiene 71 compras en el
--   ATS y el BLOQUE 5 encontro apenas SEIS lineas de retencion en PGS.DRC2,
--   por 761,58 en total. El ATS de julio autorizado por el SRI trae una
--   retencion en casi cada una de sus 79 compras.
--
--   El sistema tiene DOS juegos de tablas de retencion de compra:
--     PGS.RTCM / PGS.DRCM  (la "V1")
--     PGS.RCV2 / PGS.DRC2  (la "V2")
--   y el generador del ATS lee SOLO la V2. Si las retenciones reales se
--   graban en la V1, el anexo va a declarar 761,58 en vez de lo que
--   corresponde, y eso VALIDA igual: el SRI lo acepta y no avisa nada.
--
--   Este script no arregla nada. Dice en cual de las dos estan.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Cuantas retenciones de compra hay en cada juego de tablas
-- ESPERADO: el juego que tenga el grueso de las lineas es el que el
-- generador tiene que leer. Si la V1 tiene decenas y la V2 seis, hay que
-- cambiar la fuente del ATS antes de presentar.
-- ---------------------------------------------------------------------
select 'BLOQUE 1 - V2 (PGS.RCV2/DRC2) - la que lee el ATS hoy' as bloque,
       count(distinct r.ID) as comprobantes,
       count(d.ID)          as lineas,
       sum(nvl(d.VALORRETEN,0)) as valor_retenido
  from PGS.RCV2 r
  left join PGS.DRC2 d on d.RETENCIONV2 = r.ID
 where r.FECHA >= date '2026-08-01'
   and r.FECHA <  date '2026-09-01'
union all
select 'BLOQUE 1 - V1 (PGS.RTCM/DRCM) - la que el ATS NO lee',
       count(distinct r.ID),
       count(d.ID),
       sum(nvl(d.VALORRETEN,0))
  from PGS.RTCM r
  left join PGS.DRCM d on d.RETENCION = r.ID
 where r.FECHA >= date '2026-08-01'
   and r.FECHA <  date '2026-09-01';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — El detalle de la V1, por si resulta ser la buena
-- ESPERADO: si el BLOQUE 1 mostro que la V1 tiene el grueso, aca se ve el
-- reparto por impuesto y codigo, igual que el BLOQUE 5 del e2-37.
-- CODIMPUESTO 1 = renta (va a <air>), 2 = IVA (va a los seis campos de la
-- Tabla 11).
-- ---------------------------------------------------------------------
select 'BLOQUE 2 - detalle V1' as bloque,
       d.CODIMPUESTO   as cod_impuesto,
       d.CODRETENCION  as cod_retencion,
       count(*)        as lineas,
       sum(nvl(d.VALORRETEN,0)) as valor_retenido
  from PGS.DRCM d
  join PGS.RTCM r on r.ID = d.RETENCION
 where r.FECHA >= date '2026-08-01'
   and r.FECHA <  date '2026-09-01'
 group by d.CODIMPUESTO, d.CODRETENCION
 order by 2, 3;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Historico de los dos juegos, para saber cual esta vigente
-- ESPERADO: el juego vigente es el que tiene filas RECIENTES. Si la V1 se
-- corto en una fecha y la V2 arranca ahi, la migracion ya se hizo y agosto
-- simplemente tuvo pocas retenciones. Si la V1 sigue recibiendo filas hoy,
-- es la vigente y el ATS esta leyendo la tabla equivocada.
-- ---------------------------------------------------------------------
select 'BLOQUE 3 - vigencia' as bloque, 'V2 (RCV2)' as juego,
       count(*)     as comprobantes,
       min(r.FECHA) as primera,
       max(r.FECHA) as ultima
  from PGS.RCV2 r
union all
select 'BLOQUE 3 - vigencia', 'V1 (RTCM)',
       count(*), min(r.FECHA), max(r.FECHA)
  from PGS.RTCM r;
