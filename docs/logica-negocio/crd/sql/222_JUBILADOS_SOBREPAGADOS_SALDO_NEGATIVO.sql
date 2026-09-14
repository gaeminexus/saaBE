-- =====================================================================================
-- DIAGNOSTICO — Jubilados a los que se les pago MAS de lo que tenian en pension
--               complementaria (aporte 23), y cualquier otra cuenta de aportes en negativo
-- FECHA: 2026-09-14   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 222 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Todos los bloques son SELECT. Se puede correr en horario laboral.
--    Sin DEFINE ni PROMPT: se corre de corrido.
--
-- =====================================================================================
-- QUE PASA Y POR QUE ESTE SCRIPT  (H60 en ESTADO-EQUIPO-OMEN-1.md)
--
--   1) PENSION: PagoPensionComplementariaServiceImpl.generarMesesRetroactivos calcula la
--      pension del mes como  (pension+seguro) − cruce − seguro  SIN topar contra el saldo
--      que queda. En el mes en que el saldo ya no alcanza, sale la pension COMPLETA y el
--      aporte 23 queda en negativo. Regresion del commit a18b1b80 (2026-09-04).
--
--   2) SEGURO: generarSeguroDelMes fija el seguro NOMINAL de cada jubilado y emite la orden
--      al proveedor por la suma, sin mirar el saldo. Despues la pension descuenta el seguro
--      topado por saldo. Si el saldo no alcanzaba, al proveedor se le pago MAS de lo que
--      se desconto de la cuenta del jubilado. El BLOQUE 4 lo mide.
--
--   El saldo se calcula EXACTAMENTE como lo calcula el sistema:
--   SUM(APRTVLRR) por entidad y tipo, sin filtro de estado
--   (AporteDaoServiceImpl.sumValorByEntidadYTipo).
--
-- COMO USARLO
--   Correr los cinco bloques y pasarme las salidas. El BLOQUE 1 es el que dice cuantos
--   casos hay; el 3 dice cuanto dinero salio de mas y por que via.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 — Barrido general: TODA cuenta de aportes con saldo NEGATIVO, de cualquier
--            tipo y de cualquier participe (no solo jubilados).
-- Esperado en un sistema sano: CERO filas.
-- =====================================================================================
SELECT a.ENTDCDGO,
       e.ENTDNMID                      AS identificacion,
       e.ENTDRZNS                      AS nombre,
       e.ENTDIDST                      AS estado_participe,
       a.TPAPCDGO                      AS tipo_aporte,
       ROUND(SUM(a.APRTVLRR), 2)       AS saldo,
       COUNT(*)                        AS movimientos
FROM   CRD.APRT a
JOIN   CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
GROUP  BY a.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDIDST, a.TPAPCDGO
HAVING ROUND(SUM(a.APRTVLRR), 2) < 0
ORDER  BY saldo;


-- =====================================================================================
-- BLOQUE 2 — Movimientos del aporte 23 con SALDO ACUMULADO, solo de los jubilados que
--            hoy estan en negativo. La fila donde el acumulado cruza de positivo a
--            negativo es el pago que sobregiro la cuenta.
-- APRTTPMV: 3 DEVOLUCION · 4 PAGO_PRESTAMO · 5 REVERSO · 7 JUBILACION · 9 PAGO_PENSION
-- =====================================================================================
SELECT a.ENTDCDGO,
       a.APRTCDGO,
       a.APRTFCTR                      AS fecha_transaccion,
       a.APRTFCRG                      AS fecha_registro,
       a.APRTTPMV                      AS tipo_movimiento,
       a.APRTVLRR                      AS valor,
       ROUND(SUM(a.APRTVLRR) OVER (PARTITION BY a.ENTDCDGO
                                   ORDER BY a.APRTFCTR, a.APRTCDGO), 2) AS saldo_acumulado,
       a.APRTGLSA                      AS glosa
FROM   CRD.APRT a
WHERE  a.TPAPCDGO = 23
  AND  a.ENTDCDGO IN (SELECT x.ENTDCDGO
                      FROM   CRD.APRT x
                      WHERE  x.TPAPCDGO = 23
                      GROUP  BY x.ENTDCDGO
                      HAVING ROUND(SUM(x.APRTVLRR), 2) < 0)
ORDER  BY a.ENTDCDGO, a.APRTFCTR, a.APRTCDGO;


-- =====================================================================================
-- BLOQUE 3 — Cuanto salio de mas, por jubilado, y por que via.
--   sobregiro          = lo que falta para que la cuenta quede en cero (= -saldo)
--   pagado_banco       = movimientos de PAGO_PENSION que NO son seguro (salieron al banco)
--   seguro_descontado  = movimientos de PAGO_PENSION marcados "SEGURO MEDICO"
--   cruzado_prestamos  = movimientos de PAGO_PRESTAMO (cruce contra prestamo)
-- =====================================================================================
SELECT a.ENTDCDGO,
       e.ENTDRZNS                                                         AS nombre,
       ROUND(-SUM(a.APRTVLRR), 2)                                         AS sobregiro,
       ROUND(-SUM(CASE WHEN a.APRTTPMV = 9
                        AND a.APRTGLSA NOT LIKE '%SEGURO MEDICO' THEN a.APRTVLRR END), 2) AS pagado_banco,
       ROUND(-SUM(CASE WHEN a.APRTTPMV = 9
                        AND a.APRTGLSA LIKE '%SEGURO MEDICO'     THEN a.APRTVLRR END), 2) AS seguro_descontado,
       ROUND(-SUM(CASE WHEN a.APRTTPMV = 4                        THEN a.APRTVLRR END), 2) AS cruzado_prestamos
FROM   CRD.APRT a
JOIN   CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE  a.TPAPCDGO = 23
GROUP  BY a.ENTDCDGO, e.ENTDRZNS
HAVING ROUND(SUM(a.APRTVLRR), 2) < 0
ORDER  BY sobregiro DESC;


-- =====================================================================================
-- BLOQUE 4 — SEGURO: lo que se le pago al proveedor (PGPCVLSG, fijado nominal por el
--            proceso de seguro) contra lo que de verdad se desconto de la cuenta del
--            jubilado en ese periodo (movimiento "... m/aaaa - Entidad n - SEGURO MEDICO").
-- Esperado: diferencia = 0 en todas las filas. Una diferencia > 0 es seguro pagado al
-- proveedor sin respaldo en el saldo del jubilado. Una fila con PGPCVLPN nulo es un stub
-- de seguro cuya pension todavia no se genero (la diferencia ahi es esperable hasta que
-- corra la pension).
-- =====================================================================================
SELECT p.ENTDCDGO,
       p.PGPCANNO,
       p.PGPCMESS,
       p.PGPCESTD                                   AS estado_pgpc,
       p.PGPCVLPN                                   AS pension_nominal,
       p.PGPCVLSG                                   AS seguro_al_proveedor,
       NVL(ROUND(-SUM(a.APRTVLRR), 2), 0)           AS seguro_descontado,
       ROUND(p.PGPCVLSG - NVL(-SUM(a.APRTVLRR), 0), 2) AS diferencia
FROM   CRD.PGPC p
LEFT JOIN CRD.APRT a
       ON  a.ENTDCDGO = p.ENTDCDGO
       AND a.TPAPCDGO = 23
       AND a.APRTTPMV = 9
       AND a.APRTGLSA LIKE '% ' || p.PGPCMESS || '/' || p.PGPCANNO
                           || ' - Entidad ' || p.ENTDCDGO || ' - SEGURO MEDICO'
WHERE  NVL(p.PGPCVLSG, 0) > 0
GROUP  BY p.ENTDCDGO, p.PGPCANNO, p.PGPCMESS, p.PGPCESTD, p.PGPCVLPN, p.PGPCVLSG
HAVING ROUND(p.PGPCVLSG - NVL(-SUM(a.APRTVLRR), 0), 2) <> 0
ORDER  BY p.PGPCANNO, p.PGPCMESS, p.ENTDCDGO;


-- =====================================================================================
-- BLOQUE 5 — Los PGPC de los jubilados en negativo: que registro cada mes y en que
--            estado quedo (1 REGISTRADA · 2 EN_PAGO · 3 PAGADA · 4 RECHAZADA · 5 ANULADA
--            · 6 SEGURO_GENERADO). PGPCIDPG es la orden de pago en CXP: si esta en
--            EN_PAGO y la orden NO se confirmo todavia en el banco, todavia se puede frenar.
-- =====================================================================================
SELECT p.PGPCCDGO,
       p.ENTDCDGO,
       p.PGPCANNO,
       p.PGPCMESS,
       p.PGPCVLRR                                   AS valor_total_nominal,
       p.PGPCVLPN                                   AS pension_nominal,
       p.PGPCVLSG                                   AS seguro,
       p.PGPCESTD                                   AS estado,
       p.PGPCIDPG                                   AS orden_pago_cxp,
       p.PGPCIDAP                                   AS aporte_referenciado,
       p.PGPCFCRG                                   AS fecha_registro
FROM   CRD.PGPC p
WHERE  p.ENTDCDGO IN (SELECT x.ENTDCDGO
                      FROM   CRD.APRT x
                      WHERE  x.TPAPCDGO = 23
                      GROUP  BY x.ENTDCDGO
                      HAVING ROUND(SUM(x.APRTVLRR), 2) < 0)
ORDER  BY p.ENTDCDGO, p.PGPCANNO, p.PGPCMESS;
