-- =====================================================================================
-- LOS 404 PARTICIPES QUE APORTAN Y NO TIENEN CONTRATO ACTIVO
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ NO CORRER DE CORRIDO. El BLOQUE 0 es de solo lectura y HAY QUE REVISARLO ANTES.
-- Si cualquiera de sus controles no da lo esperado, PARAR y avisar al arbitro.
--
-- QUE RESUELVE. 404 participes con filas de carga Petro no tienen contrato ACTIVO en
-- CRD.CNTR. Sin contrato, VigenciaContratoServiceImpl.esperadoPorEntidad devuelve 0.0
-- (linea 226-228), distribuirAportePorDevengo salta todos los meses con
-- `if (esperado <= 0.0) continue;` y el dinero descontado NO SE APLICA A NINGUNA CUENTA.
-- Medido por el equipo 2: 1.972 filas, $132.782,01.
--
-- ⚠️ HOY ESO NO FALLA: sale por System.err y la carga sigue. Se esta corrigiendo para que
-- ABORTE. Despues de ese cambio, un participe sin contrato DETIENE la carga del mes. Por eso
-- estos 404 tienen que existir ANTES de la proxima carga.
--
-- ⚠️ DE DONDE SALEN LOS MONTOS, Y POR QUE NO DE LOS APORTES.
-- El monto esperado sale de CRD.HSTR (HistorialSueldo) — HSTRMNAJ jubilacion, HSTRMNAC
-- cesantia — que es la fuente que el camino anterior ya usaba. **NO se deducen de los
-- aportes que la persona pago.** Deducir el contrato de los pagos es leer la obligacion
-- desde su cumplimiento, justo al reves: si alguien pago de menos, le fabricariamos un
-- contrato por lo que pago y el faltante desapareceria para siempre.
--
-- ⚠️ SI UN PARTICIPE NO TIENE HistorialSueldo ACTIVO, NO SE LE CREA CONTRATO. Queda fuera a
-- proposito y sale listado en el control 0.3: es un caso que necesita decision, no un
-- INSERT. Fabricarle un contrato con monto cero seria peor que no tenerlo — pasaria el
-- control de "tiene contrato" y seguiria sin aplicar dinero, ahora sin abortar.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — CONTROLES PREVIOS. SOLO LECTURA. REVISAR ANTES DE SEGUIR.
-- =====================================================================================

-- 0.1 Cuantos son. Esperado: ~404.
SELECT COUNT(DISTINCT a.ENTDCDGO) AS SIN_CONTRATO_ACTIVO
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01'
AND    (a.CRARCDGO IS NOT NULL
        OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS))
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                   WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1);

-- 0.2 ⭐ EL CONTROL QUE DECIDE: de esos, cuantos tienen HistorialSueldo ACTIVO con monto.
--     Los que SI se les crea contrato. Los que NO quedan fuera (ver 0.3).
SELECT COUNT(*) AS CON_HISTORIAL_UTIL
FROM ( SELECT DISTINCT a.ENTDCDGO
       FROM   CRD.APRT a
       WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
       AND    a.APRTFCTR >= DATE '2025-06-01'
       AND    (a.CRARCDGO IS NOT NULL
               OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS))
       AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                          WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1) ) x
WHERE  EXISTS (SELECT 1 FROM CRD.HSTR h
               WHERE h.ENTDCDGO = x.ENTDCDGO AND h.HSTRESTD = 99
               AND   (NVL(h.HSTRMNAJ,0) > 0 OR NVL(h.HSTRMNAC,0) > 0));

-- 0.3 ⛔ LOS QUE QUEDAN FUERA, con nombre. Estos NO reciben contrato en este script.
--     Cada uno necesita decision: o se le carga el HistorialSueldo, o se revisa por que
--     Petro le descuenta. Si esta lista sale grande, PARAR y avisar antes de seguir.
SELECT e.ENTDCDGO, e.ENTDNMCM AS NOMBRE, e.ENTDRLPC AS CODIGO_PETRO,
       (SELECT ROUND(SUM(a2.APRTVLRR),2) FROM CRD.APRT a2
        WHERE  a2.ENTDCDGO = e.ENTDCDGO AND a2.TPAPCDGO IN (9,11)
        AND    a2.APRTFCTR >= DATE '2025-06-01')        AS APORTADO_DESDE_JUN25,
       (SELECT COUNT(*) FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO) AS HISTORIALES_TOTAL,
       (SELECT COUNT(*) FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99) AS HISTORIALES_ACTIVOS
FROM   CRD.ENTD e
WHERE  EXISTS (SELECT 1 FROM CRD.APRT a
               WHERE a.ENTDCDGO = e.ENTDCDGO AND a.TPAPCDGO IN (9,11) AND a.APRTVLRR > 0
               AND   a.APRTFCTR >= DATE '2025-06-01'
               AND   (a.CRARCDGO IS NOT NULL
                      OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)))
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c WHERE c.ENTDCDGO = e.ENTDCDGO AND c.CNTRESTD = 1)
AND    NOT EXISTS (SELECT 1 FROM CRD.HSTR h
                   WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99
                   AND  (NVL(h.HSTRMNAJ,0) > 0 OR NVL(h.HSTRMNAC,0) > 0))
ORDER  BY 4 DESC NULLS LAST;

-- 0.4 ⭐ QUE VALORES DE CATALOGO USAN LOS CONTRATOS QUE YA FUNCIONAN.
--     El INSERT del bloque 1 COPIA estos, no los inventa. Esperado: idealmente UNA fila
--     dominante. Si hay varias combinaciones repartidas, PARAR: hay que decidir cual va
--     para los nuevos, y eso no lo decide un script.
SELECT c.TPCNCDGO, c.FLLLCDGO, c.CNTRIDST, COUNT(*) AS CUANTOS
FROM   CRD.CNTR c
WHERE  c.CNTRESTD = 1
GROUP  BY c.TPCNCDGO, c.FLLLCDGO, c.CNTRIDST
ORDER  BY COUNT(*) DESC;

-- 0.5 Las vigencias que ya existen, para copiar su forma. Esperado: todas con
--     VGCNFCIN = 2025-06-01, VGCNFCFN NULL, VGCNIDST activo.
SELECT v.TPAPCDGO, MIN(v.VGCNFCIN) AS DESDE, COUNT(v.VGCNFCFN) AS CON_FIN,
       v.VGCNMODO, v.VGCNIDST, COUNT(*) AS CUANTAS
FROM   CRD.VGCN v
GROUP  BY v.TPAPCDGO, v.VGCNMODO, v.VGCNIDST
ORDER  BY 1;

-- 0.6 La PK de CNTR es IDENTITY y la de VGCN es secuencia. Confirmarlo antes de insertar.
SELECT 'CNTR.CNTRCDGO' AS COLUMNA, c.IDENTITY_COLUMN
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER='CRD' AND c.TABLE_NAME='CNTR' AND c.COLUMN_NAME='CNTRCDGO';

SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER
FROM   ALL_SEQUENCES s WHERE s.SEQUENCE_NAME = 'SQ_VGCNCDGO';


-- =====================================================================================
-- ⛔ ALTO. No sigas sin haber mirado 0.2, 0.3, 0.4, 0.5 y 0.6.
--    Antes de correr el bloque 1, reemplaza los dos literales de abajo por los valores
--    que dio el 0.4. NO los dejes como estan: el script falla a proposito si no los tocas.
-- =====================================================================================

-- Pega aca los valores dominantes del control 0.4:
--   TPCNCDGO (tipo de contrato)  -> reemplazar &&TPCN
--   FLLLCDGO (filial)            -> reemplazar &&FLLL
--   CNTRIDST (estado interno)    -> reemplazar &&IDST


-- =====================================================================================
-- BLOQUE 1 — LOS CONTRATOS
-- =====================================================================================
-- CNTRCDGO es IDENTITY: no se da. Los montos salen de HSTR, no de los aportes.

INSERT INTO CRD.CNTR (FLLLCDGO, TPCNCDGO, ENTDCDGO, CNTRFCIN,
                      CNTRMNAJ, CNTRMNAC, CNTRMNAA,
                      CNTROBSR, CNTRESTD, CNTRIDST, CNTRFCRG, CNTRUSRG)
SELECT &&FLLL, &&TPCN, x.ENTDCDGO, DATE '2025-06-01',
       NVL(h.HSTRMNAJ, 0), NVL(h.HSTRMNAC, 0), NVL(h.HSTRMNAA, 0),
       'Contrato creado por el script 98 (2026-08-31): el participe aportaba por carga Petro sin contrato registrado. Montos tomados de CRD.HSTR ' || h.HSTRCDGO || ', no deducidos de sus aportes.',
       1, &&IDST, SYSTIMESTAMP, 'SCRIPT_98'
FROM ( SELECT DISTINCT a.ENTDCDGO
       FROM   CRD.APRT a
       WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
       AND    a.APRTFCTR >= DATE '2025-06-01'
       AND    (a.CRARCDGO IS NOT NULL
               OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS))
       AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                          WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1) ) x
JOIN   CRD.HSTR h ON h.ENTDCDGO = x.ENTDCDGO AND h.HSTRESTD = 99
WHERE  (NVL(h.HSTRMNAJ,0) > 0 OR NVL(h.HSTRMNAC,0) > 0)
AND    h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                     WHERE h2.ENTDCDGO = x.ENTDCDGO AND h2.HSTRESTD = 99);
-- El MAX(HSTRCDGO) es por si un participe tiene mas de un historial activo: se toma el
-- ultimo. Si el control 0.2 y el 1.1 no coinciden, es que hay duplicados y hay que mirarlo.

-- 1.1 Cuantos contratos se crearon. Debe coincidir con el 0.2.
SELECT COUNT(*) AS CONTRATOS_CREADOS FROM CRD.CNTR WHERE CNTRUSRG = 'SCRIPT_98';


-- =====================================================================================
-- BLOQUE 2 — LAS VIGENCIAS (una por tipo con monto > 0)
-- =====================================================================================
-- El esperado del devengo sale de VGCNMNTO (VigenciaContratoServiceImpl:217), asi que sin
-- vigencia el contrato no sirve de nada. Abiertas (VGCNFCFN NULL) e iniciando 2025-06-01,
-- igual que las 2.166 que ya existen.

-- 2.1 Jubilacion (tipo 9)
INSERT INTO CRD.VGCN (VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN,
                      VGCNMNTO, VGCNPRCN, VGCNRMUN, VGCNMODO, VGCNIDHS, VGCNOBSR, VGCNIDST, VGCNFCRG, VGCNUSRG)
SELECT CRD.SQ_VGCNCDGO.NEXTVAL, c.CNTRCDGO, 9, DATE '2025-06-01', NULL,
       h.HSTRMNAJ, NULL, NULL, 2, h.HSTRCDGO,
       'Vigencia creada por el script 98 (2026-08-31). Monto de CRD.HSTR.',
       1, SYSTIMESTAMP, 'SCRIPT_98'
FROM   CRD.CNTR c
JOIN   CRD.HSTR h ON h.ENTDCDGO = c.ENTDCDGO AND h.HSTRESTD = 99
                 AND h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                                   WHERE h2.ENTDCDGO = c.ENTDCDGO AND h2.HSTRESTD = 99)
WHERE  c.CNTRUSRG = 'SCRIPT_98'
AND    NVL(h.HSTRMNAJ, 0) > 0;

-- 2.2 Cesantia (tipo 11)
INSERT INTO CRD.VGCN (VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN,
                      VGCNMNTO, VGCNPRCN, VGCNRMUN, VGCNMODO, VGCNIDHS, VGCNOBSR, VGCNIDST, VGCNFCRG, VGCNUSRG)
SELECT CRD.SQ_VGCNCDGO.NEXTVAL, c.CNTRCDGO, 11, DATE '2025-06-01', NULL,
       h.HSTRMNAC, NULL, NULL, 2, h.HSTRCDGO,
       'Vigencia creada por el script 98 (2026-08-31). Monto de CRD.HSTR.',
       1, SYSTIMESTAMP, 'SCRIPT_98'
FROM   CRD.CNTR c
JOIN   CRD.HSTR h ON h.ENTDCDGO = c.ENTDCDGO AND h.HSTRESTD = 99
                 AND h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                                   WHERE h2.ENTDCDGO = c.ENTDCDGO AND h2.HSTRESTD = 99)
WHERE  c.CNTRUSRG = 'SCRIPT_98'
AND    NVL(h.HSTRMNAC, 0) > 0;

-- ⛔ NO HACER COMMIT TODAVIA. Correr el bloque 3.


-- =====================================================================================
-- BLOQUE 3 — CONTROLES POSTERIORES. REVISAR ANTES DEL COMMIT.
-- =====================================================================================

-- 3.1 ⭐ EL QUE MANDA: ningun contrato nuevo quedo sin vigencia. Esperado: 0 filas.
--     Un contrato sin vigencia hace que esperado() siga dando 0: pasaria el control de
--     "tiene contrato" y el dinero seguiria sin aplicarse, ahora SIN abortar. Es peor que
--     no haberlo creado.
SELECT c.CNTRCDGO, c.ENTDCDGO
FROM   CRD.CNTR c
WHERE  c.CNTRUSRG = 'SCRIPT_98'
AND    NOT EXISTS (SELECT 1 FROM CRD.VGCN v WHERE v.CNTRCDGO = c.CNTRCDGO);

-- 3.2 Ningun participe quedo con DOS contratos activos. Esperado: 0 filas.
SELECT c.ENTDCDGO, COUNT(*) AS CUANTOS
FROM   CRD.CNTR c WHERE c.CNTRESTD = 1
GROUP  BY c.ENTDCDGO HAVING COUNT(*) > 1;

-- 3.3 Vigencias creadas, por tipo. Contrastar contra 1.1.
SELECT v.TPAPCDGO, COUNT(*) AS CUANTAS, ROUND(SUM(v.VGCNMNTO),2) AS SUMA_ESPERADO
FROM   CRD.VGCN v WHERE v.VGCNUSRG = 'SCRIPT_98'
GROUP  BY v.TPAPCDGO ORDER BY 1;

-- 3.4 ⭐ LA PRUEBA REAL: ¿cuantos de los 404 siguen sin contrato activo?
--     Esperado: solo los del control 0.3 (sin HistorialSueldo util).
SELECT COUNT(DISTINCT a.ENTDCDGO) AS SIGUEN_SIN_CONTRATO
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01'
AND    (a.CRARCDGO IS NOT NULL
        OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS))
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                   WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1);

-- 3.5 Muestra de 10, para mirar montos con ojo humano antes del COMMIT.
SELECT c.CNTRCDGO, c.ENTDCDGO, e.ENTDNMCM,
       c.CNTRMNAJ AS MONTO_JUB, c.CNTRMNAC AS MONTO_CES,
       (SELECT ROUND(AVG(a.APRTVLRR),2) FROM CRD.APRT a
        WHERE a.ENTDCDGO = c.ENTDCDGO AND a.TPAPCDGO = 9
        AND   a.APRTFCTR >= DATE '2025-06-01' AND a.APRTVLRR > 0) AS PROM_APORTADO_JUB,
       (SELECT ROUND(AVG(a.APRTVLRR),2) FROM CRD.APRT a
        WHERE a.ENTDCDGO = c.ENTDCDGO AND a.TPAPCDGO = 11
        AND   a.APRTFCTR >= DATE '2025-06-01' AND a.APRTVLRR > 0) AS PROM_APORTADO_CES
FROM   CRD.CNTR c JOIN CRD.ENTD e ON e.ENTDCDGO = c.ENTDCDGO
WHERE  c.CNTRUSRG = 'SCRIPT_98' AND ROWNUM <= 10;
-- ⚠️ El monto del contrato y el promedio aportado NO tienen por que coincidir — uno es la
-- obligacion y el otro lo pagado. Pero si difieren MUCHO en muchas filas, el HistorialSueldo
-- puede estar desactualizado y conviene mirarlo antes del COMMIT.

-- Si 3.1 y 3.2 dan 0 filas y 3.4 coincide con el 0.3:  COMMIT;
-- Si algo no da:                                       ROLLBACK;


-- =====================================================================================
-- 4. REVERSO — comentado a proposito.
-- =====================================================================================
-- Todo lo creado por este script queda marcado con USRG = 'SCRIPT_98', asi que se puede
-- deshacer con precision. Solo tiene sentido ANTES de que corra una carga que los use.
--
-- DELETE FROM CRD.VGCN WHERE VGCNUSRG = 'SCRIPT_98';
-- DELETE FROM CRD.CNTR WHERE CNTRUSRG = 'SCRIPT_98';
-- COMMIT;
-- =====================================================================================
