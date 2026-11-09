-- =====================================================================================
-- 373 - MEDICION (solo lectura): pensiones de jubilados RETENIDAS (sin cuenta bancaria o
--       sin certificado) que igual se contabilizaron como "por pagar"
-- omen-saa-1-arb - 2026-11-09 - diseno: crd/DISENO-PENSION-RETENIDA-SIN-CUENTA.md
--
-- Que mide: filas CRD.PGPC en estado 1 REGISTRADA, sin orden de pago (PGPCIDPG null), cuyo
-- asiento de devengo (PGPCNMDV) tiene la linea HABER a 2.3.01.10.03 (pensiones por pagar).
-- Es el remanente que NO salio del aporte 23 (no hay movimiento negativo de pension) pero
-- que el mayor ya muestra como pasivo por pagar.
-- Se excluyen las del re-proceso C20 (esas SI debitaron el neto: su PGPCIDAP apunta a un
-- negativo de pension, no de seguro).
--
-- NO MODIFICA NADA. Correr bloque por bloque y pasar la salida al arbitro.
-- =====================================================================================

-- BLOQUE 1 - Resumen: cuantas filas, cuantos jubilados, cuanto quedo devengado sin pagar.
SELECT  COUNT(*)                               AS FILAS_PGPC,
        COUNT(DISTINCT p.ENTDCDGO)             AS JUBILADOS,
        SUM(d.DTASHBRR)                        AS VALOR_DEVENGADO_RETENIDO,
        MIN(p.PGPCANNO * 100 + p.PGPCMESS)     AS DESDE_PERIODO,
        MAX(p.PGPCANNO * 100 + p.PGPCMESS)     AS HASTA_PERIODO
FROM    CRD.PGPC p
JOIN    CNT.DTAS d ON d.ASNTCDGO = p.PGPCNMDV
WHERE   p.PGPCESTD = 1
AND     p.PGPCIDPG IS NULL
AND     d.DTASCNTA = '2.3.01.10.03'
AND     d.DTASHBRR > 0
AND     NOT EXISTS (SELECT 1 FROM CRD.APRT a
                    WHERE a.APRTCDGO = p.PGPCIDAP
                    AND   a.APRTVLRR < 0
                    AND   a.APRTGLSA NOT LIKE '% - SEGURO MEDICO');

-- BLOQUE 2 - Detalle por jubilado y periodo (para cruzar con contabilidad).
SELECT  p.ENTDCDGO,
        p.PGPCANNO, p.PGPCMESS,
        p.PGPCCDGO,
        p.PGPCVLPN                             AS PENSION_NOMINAL,
        d.DTASHBRR                             AS RETENIDO_DEVENGADO,
        p.PGPCNMDV                             AS ASIENTO_DEVENGO,
        a.ASNTNMRO, a.ASNTFCHA, a.ASNTESTD
FROM    CRD.PGPC p
JOIN    CNT.DTAS d ON d.ASNTCDGO = p.PGPCNMDV
JOIN    CNT.ASNT a ON a.ASNTCDGO = p.PGPCNMDV
WHERE   p.PGPCESTD = 1
AND     p.PGPCIDPG IS NULL
AND     d.DTASCNTA = '2.3.01.10.03'
AND     d.DTASHBRR > 0
AND     NOT EXISTS (SELECT 1 FROM CRD.APRT x
                    WHERE x.APRTCDGO = p.PGPCIDAP
                    AND   x.APRTVLRR < 0
                    AND   x.APRTGLSA NOT LIKE '% - SEGURO MEDICO')
ORDER   BY p.ENTDCDGO, p.PGPCANNO, p.PGPCMESS;

-- BLOQUE 3 - De esos jubilados, cuales YA tienen hoy una cuenta bancaria activa
--            (candidatos a cobrar lo acumulado apenas entre el cambio).
SELECT  p.ENTDCDGO,
        COUNT(*)                               AS MESES_RETENIDOS,
        SUM(d.DTASHBRR)                        AS RETENIDO_TOTAL,
        (SELECT COUNT(*) FROM CRD.CNBP c
         WHERE c.ENTDCDGO = p.ENTDCDGO AND c.CNBPIDST = 1) AS CUENTAS_ACTIVAS
FROM    CRD.PGPC p
JOIN    CNT.DTAS d ON d.ASNTCDGO = p.PGPCNMDV
WHERE   p.PGPCESTD = 1
AND     p.PGPCIDPG IS NULL
AND     d.DTASCNTA = '2.3.01.10.03'
AND     d.DTASHBRR > 0
AND     NOT EXISTS (SELECT 1 FROM CRD.APRT x
                    WHERE x.APRTCDGO = p.PGPCIDAP
                    AND   x.APRTVLRR < 0
                    AND   x.APRTGLSA NOT LIKE '% - SEGURO MEDICO')
GROUP   BY p.ENTDCDGO
ORDER   BY p.ENTDCDGO;
