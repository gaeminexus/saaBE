-- =====================================================================================
-- LOS 31 PARTICIPES QUE APORTARON SOLO EN JUNIO 2025
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ NO CORRER DE CORRIDO. El BLOQUE 0 es solo lectura y hay que revisarlo.
--
-- QUIENES SON. 31 participes que aparecen en la carga 352 (junio 2025) y **en ninguna de
-- las 14 cargas posteriores**. ~42 filas, ~$4.709,64. Su dinero de junio SI se aplico (las
-- filas de aporte existen); lo unico que les falta es el contrato.
--
-- Los detecta el criterio de GLOSA, no el de CRARCDGO — por eso el script 98 no los
-- alcanzo: sus filas de junio 2025 son anteriores a que existiera la trazabilidad por
-- CRD.CRAR. Medido y confirmado por el equipo 2 (bloque B del 09_PREVUELO_DEL_08.sql).
--
-- ⚠️ POR QUE LA VIGENCIA VA CERRADA AL 2025-06-30, Y NO ABIERTA.
-- Decision del usuario, 2026-08-31. Una vigencia ABIERTA les generaria **14 meses de
-- faltante** a gente que no figura en el archivo de Petro desde junio 2025: deuda contra la
-- que nadie va a cobrar, que ensucia toda medicion de cumplimiento de aca en adelante.
-- Cerrada al 30-jun-2025 registra lo que fue cierto —tuvieron obligacion ese mes y la
-- pagaron— y no espera nada despues.
--
-- ⚠️ Y POR QUE SE LES CREA CONTRATO EN VEZ DE DEJARLOS SIN NADA.
-- Con el cambio de `distribuirAportePorDevengo` (2026-08-31), un participe **sin contrato
-- ACTIVO** con dinero recibido **ABORTA la carga entera**. Si alguno de estos 31 reaparece
-- en una carga futura, sin contrato detendria el proceso del mes para los 2.000. Con
-- contrato y vigencia cerrada, sale como **advertencia visible** en el resumen y el
-- operador decide. Es la diferencia entre frenar a todos y avisar sobre uno.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — CONTROLES PREVIOS. SOLO LECTURA.
-- =====================================================================================

-- 0.1 Cuantos son. Esperado: 31.
SELECT COUNT(DISTINCT a.ENTDCDGO) AS SOLO_GLOSA_SIN_CONTRATO
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01'
AND    (a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' OR a.APRTGLSA LIKE 'Abono al aporte%')
AND    a.CRARCDGO IS NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                   WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1);

-- 0.2 ⭐ EL CONTROL QUE JUSTIFICA LA FECHA DE CIERRE.
--     Esperado: MAX_FECHA = 2025-06-30 para TODOS. Si alguno tiene aportes posteriores,
--     ese NO es un caso de "aporto una vez": PARAR y sacarlo del universo, porque una
--     vigencia cerrada le generaria faltante desde julio.
SELECT a.ENTDCDGO, e.ENTDNMCM AS NOMBRE,
       MIN(a.APRTFCTR) AS PRIMERA, MAX(a.APRTFCTR) AS ULTIMA,
       COUNT(*) AS FILAS, ROUND(SUM(a.APRTVLRR), 2) AS TOTAL
FROM   CRD.APRT a
JOIN   CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01'
AND    (a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' OR a.APRTGLSA LIKE 'Abono al aporte%')
AND    a.CRARCDGO IS NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                   WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1)
GROUP  BY a.ENTDCDGO, e.ENTDNMCM
ORDER  BY MAX(a.APRTFCTR) DESC;

-- 0.3 Cuantos tienen HistorialSueldo activo con monto (los que reciben vigencia).
--     Los que no, reciben contrato SIN vigencia — y eso es correcto aca: el contrato existe
--     para que no aborte, y sin vigencia no se espera nada de ellos, que es lo que se
--     quiere. NO es el mismo caso que en el script 98.
SELECT COUNT(*) AS CON_HISTORIAL_UTIL
FROM ( SELECT DISTINCT a.ENTDCDGO
       FROM   CRD.APRT a
       WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
       AND    a.APRTFCTR >= DATE '2025-06-01'
       AND    (a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' OR a.APRTGLSA LIKE 'Abono al aporte%')
       AND    a.CRARCDGO IS NULL
       AND    NOT EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
       AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                          WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1) ) x
WHERE  EXISTS (SELECT 1 FROM CRD.HSTR h
               WHERE h.ENTDCDGO = x.ENTDCDGO AND h.HSTRESTD = 99
               AND  (NVL(h.HSTRMNAJ,0) > 0 OR NVL(h.HSTRMNAC,0) > 0));


-- =====================================================================================
-- ⛔ ALTO. Revisar 0.1, 0.2 y 0.3. Si el 0.2 muestra a alguien con aportes posteriores a
--    junio 2025, sacarlo antes de seguir.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 — LOS CONTRATOS (catalogo 2/1/1, el mismo dominante del script 98)
-- =====================================================================================

INSERT INTO CRD.CNTR (FLLLCDGO, TPCNCDGO, ENTDCDGO, CNTRFCIN,
                      CNTRMNAJ, CNTRMNAC, CNTRMNAA,
                      CNTROBSR, CNTRESTD, CNTRIDST, CNTRFCRG, CNTRUSRG)
SELECT 1, 2, x.ENTDCDGO, DATE '2025-06-01',
       NVL(h.HSTRMNAJ, 0), NVL(h.HSTRMNAC, 0), NVL(h.HSTRMNAA, 0),
       'Contrato creado por el script 99 (2026-08-31). El participe aporto SOLO en junio 2025 (carga 352) y no reaparece en 14 cargas posteriores. La vigencia va CERRADA al 30-jun-2025 a proposito: abierta le generaria 14 meses de faltante inexistente. El contrato existe para que, si reaparece, la carga avise en vez de abortar.',
       1, 1, SYSTIMESTAMP, 'SCRIPT_99'
FROM ( SELECT DISTINCT a.ENTDCDGO
       FROM   CRD.APRT a
       WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
       AND    a.APRTFCTR >= DATE '2025-06-01'
       AND    (a.APRTGLSA LIKE 'Aporte %CargaArchivo: %' OR a.APRTGLSA LIKE 'Abono al aporte%')
       AND    a.CRARCDGO IS NULL
       AND    NOT EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
       AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                          WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1) ) x
LEFT   JOIN CRD.HSTR h ON h.ENTDCDGO = x.ENTDCDGO AND h.HSTRESTD = 99
                      AND h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                                        WHERE h2.ENTDCDGO = x.ENTDCDGO AND h2.HSTRESTD = 99);
-- LEFT JOIN, no JOIN: aca SI se le crea contrato a quien no tiene historial. Es la
-- diferencia con el 98 y es deliberada — el contrato existe para evitar el abort, no para
-- generar expectativa de aporte.

-- 1.1 Esperado: 31.
SELECT COUNT(*) AS CONTRATOS_CREADOS FROM CRD.CNTR WHERE CNTRUSRG = 'SCRIPT_99';


-- =====================================================================================
-- BLOQUE 2 — LAS VIGENCIAS, CERRADAS AL 30-JUN-2025
-- =====================================================================================
-- Solo para quien tiene monto en el historial. Modo 2 (FIJO), igual que el 98: el monto se
-- copia, no se deriva de un porcentaje (ver el hallazgo de HSTRPRJB en el script 98).

-- 2.1 Jubilacion (tipo 9)
INSERT INTO CRD.VGCN (VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN,
                      VGCNMNTO, VGCNPRCN, VGCNRMUN, VGCNMODO, VGCNIDHS, VGCNOBSR,
                      VGCNIDST, VGCNFCRG, VGCNUSRG)
SELECT CRD.SQ_VGCNCDGO.NEXTVAL, c.CNTRCDGO, 9, DATE '2025-06-01', DATE '2025-06-30',
       h.HSTRMNAJ, NULL, NULL, 2, h.HSTRCDGO,
       'Vigencia CERRADA al 30-jun-2025 (script 99). El participe no reaparece despues de esa carga.',
       1, SYSTIMESTAMP, 'SCRIPT_99'
FROM   CRD.CNTR c
JOIN   CRD.HSTR h ON h.ENTDCDGO = c.ENTDCDGO AND h.HSTRESTD = 99
                 AND h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                                   WHERE h2.ENTDCDGO = c.ENTDCDGO AND h2.HSTRESTD = 99)
WHERE  c.CNTRUSRG = 'SCRIPT_99' AND NVL(h.HSTRMNAJ, 0) > 0;

-- 2.2 Cesantia (tipo 11)
INSERT INTO CRD.VGCN (VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN,
                      VGCNMNTO, VGCNPRCN, VGCNRMUN, VGCNMODO, VGCNIDHS, VGCNOBSR,
                      VGCNIDST, VGCNFCRG, VGCNUSRG)
SELECT CRD.SQ_VGCNCDGO.NEXTVAL, c.CNTRCDGO, 11, DATE '2025-06-01', DATE '2025-06-30',
       h.HSTRMNAC, NULL, NULL, 2, h.HSTRCDGO,
       'Vigencia CERRADA al 30-jun-2025 (script 99). El participe no reaparece despues de esa carga.',
       1, SYSTIMESTAMP, 'SCRIPT_99'
FROM   CRD.CNTR c
JOIN   CRD.HSTR h ON h.ENTDCDGO = c.ENTDCDGO AND h.HSTRESTD = 99
                 AND h.HSTRCDGO = (SELECT MAX(h2.HSTRCDGO) FROM CRD.HSTR h2
                                   WHERE h2.ENTDCDGO = c.ENTDCDGO AND h2.HSTRESTD = 99)
WHERE  c.CNTRUSRG = 'SCRIPT_99' AND NVL(h.HSTRMNAC, 0) > 0;

-- ⛔ NO HACER COMMIT. Correr el bloque 3.


-- =====================================================================================
-- BLOQUE 3 — CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 ⭐ NINGUNA vigencia de este script quedo abierta. Esperado: 0 filas.
--     Una abierta reintroduce exactamente el faltante fantasma que este script evita.
SELECT COUNT(*) AS VIGENCIAS_ABIERTAS_POR_ERROR
FROM   CRD.VGCN WHERE VGCNUSRG = 'SCRIPT_99' AND VGCNFCFN IS NULL;

-- 3.2 Nadie quedo con dos contratos activos. Esperado: 0 filas.
SELECT c.ENTDCDGO, COUNT(*) FROM CRD.CNTR c WHERE c.CNTRESTD = 1
GROUP BY c.ENTDCDGO HAVING COUNT(*) > 1;

-- 3.3 ⭐ Ya NADIE con filas de carga queda sin contrato activo. Esperado: 0.
--     Es el control que cierra los dos scripts, 98 y 99, juntos.
SELECT COUNT(DISTINCT a.ENTDCDGO) AS SIGUEN_SIN_CONTRATO
FROM   CRD.APRT a
WHERE  a.TPAPCDGO IN (9, 11) AND a.APRTVLRR > 0
AND    a.APRTFCTR >= DATE '2025-06-01'
AND    (a.CRARCDGO IS NOT NULL
        OR EXISTS (SELECT 1 FROM CRD.CRAR cr WHERE cr.CRARCDGO = a.APRTIDAS)
        OR a.APRTGLSA LIKE 'Aporte %CargaArchivo: %'
        OR a.APRTGLSA LIKE 'Abono al aporte%')
AND    NOT EXISTS (SELECT 1 FROM CRD.CNTR c
                   WHERE c.ENTDCDGO = a.ENTDCDGO AND c.CNTRESTD = 1);

-- 3.4 Los contratos sin ninguna vigencia (los que no tenian historial). Es INFORMATIVO:
--     aca es correcto que existan, al reves que en el script 98.
SELECT COUNT(*) AS CONTRATOS_SIN_VIGENCIA_OK
FROM   CRD.CNTR c WHERE c.CNTRUSRG = 'SCRIPT_99'
AND    NOT EXISTS (SELECT 1 FROM CRD.VGCN v WHERE v.CNTRCDGO = c.CNTRCDGO);

-- Si 3.1 y 3.2 dan 0 y 3.3 da 0:  COMMIT;   Si no:  ROLLBACK;


-- =====================================================================================
-- 4. REVERSO — comentado a proposito.
-- =====================================================================================
-- DELETE FROM CRD.VGCN WHERE VGCNUSRG = 'SCRIPT_99';
-- DELETE FROM CRD.CNTR WHERE CNTRUSRG = 'SCRIPT_99';
-- COMMIT;
-- =====================================================================================
