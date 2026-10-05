-- =====================================================================================
-- 313 — MEDICION (solo lectura): cobro tardio del prestamo idAsoprep 67023 sin reverso de la mora
-- FECHA: 2026-10-05 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- El usuario proceso un pago tardio que cubria julio-septiembre. Salio la reclasificacion de bandas
-- (MVIC tipo 8) pero NO el reverso de la mora devengada en el cierre de agosto / apertura de
-- septiembre (tipo 6). Hipotesis: ese devengo se contabilizo ANTES de que existiera el libro MVIC,
-- asi que la cuota no tiene tipo 5 y el reverso, acotado a lo devengado en el libro, da 0.
-- Correr en el MISMO ambiente donde se hizo la prueba y pasar los 5 resultados.
-- =====================================================================================

-- 1. Las cuotas 86-92: mora persistida HOY (despues del recalculo).
SELECT d.DTPRCDGO, d.DTPRNMCT, d.DTPRFCVN, d.DTPRESTD, d.DTPRINTR, d.DTPRINVN, d.DTPRMRAA
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE p.PRSTIDAS = 67023 AND d.DTPRNMCT BETWEEN 86 AND 92
 ORDER BY d.DTPRNMCT;

-- 2. Los pagos de las cuotas 88-90: fecha, mora pagada, anulados.
SELECT g.PGPRCDGO, g.PGPRNMCT, g.PGPRFCHA, g.PGPRFCRG, g.PGPRMRPG AS MORA_PAGADA, g.PGPRINPG AS INTERES,
       g.PGPRINVP AS INT_VENCIDO, g.PGPRCPPG AS CAPITAL, g.PGPRSLOT AS OTROS, g.PGPRTPOO AS TIPO, g.PGPRANUL
  FROM CRD.PGPR g JOIN CRD.PRST p ON p.PRSTCDGO = g.PRSTCDGO
 WHERE p.PRSTIDAS = 67023 AND g.PGPRNMCT BETWEEN 88 AND 90
 ORDER BY g.PGPRNMCT, g.PGPRFCHA, g.PGPRCDGO;

-- 3. El libro MVIC del prestamo (que tipos se escribieron y cuales no).
SELECT m.MVICCDGO, m.DTPRCDGO, m.MVICTPMV AS TIPO, m.MVICCMPN AS COMPONENTE, m.MVICVLOR, m.MVICFCCT,
       m.CRCTCDGO, m.PGPRCDGO, m.MVICORGN, m.MVICIDOR, m.ASNTCDGO, m.MVICANUL, m.MVICFCRG
  FROM CRD.MVIC m JOIN CRD.PRST p ON p.PRSTCDGO = m.PRSTCDGO
 WHERE p.PRSTIDAS = 67023
 ORDER BY m.MVICFCRG, m.MVICCDGO;

-- 4. Las corridas de cierre: cuales existen y si alguna escribio MVIC (las anteriores al WAR no).
SELECT c.CRCTCDGO, c.CRCTANOO, c.CRCTMESS, c.CRCTFCCR AS CORTE, c.CRCTFCPR AS PROCESO, c.CRCTIDST, c.CRCTFCRG,
       (SELECT COUNT(*) FROM CRD.MVIC m WHERE m.CRCTCDGO = c.CRCTCDGO) AS FILAS_MVIC
  FROM CRD.CRCT c
 ORDER BY c.CRCTFCCR DESC FETCH FIRST 5 ROWS ONLY;

-- 5. El cobro de la prueba: sus dos fechas y estado.
SELECT b.CBCRCDGO, b.CBCRFCHA AS FECHA_PAGO, b.CBCRFCAF AS FECHA_AFECTACION, b.CBCRESTD, b.CBCRVLRR
  FROM CRD.CBCR b
 WHERE b.CBCRCDGO IN (SELECT dc.CBCRCDGO FROM CRD.DCBC dc JOIN CRD.PRST p ON p.PRSTCDGO = dc.PRSTCDGO
                       WHERE p.PRSTIDAS = 67023)
 ORDER BY b.CBCRCDGO DESC;
