-- =====================================================================================
-- ⛔⛔ ANULADO EL 2026-09-07 — NO CORRER ESTE SCRIPT. HIPOTESIS FALSA.
--
-- Este script asumia que `AcuerdoCondonacionServiceImpl:566` dejaba la FK PGAP.PGPRCDGO en
-- NULL por no reasignar el retorno de `saveSingle`. ES FALSO.
--
-- `EntityDaoImpl.save():306` NO hace merge cuando la entidad es nueva:
--     if (id == null) { selloAuditoria(tipo); em.persist(tipo); }
--     else            { em.merge(tipo); }
-- y `persist()` escribe el id EN EL OBJETO QUE SE LE PASA. La FK nunca estuvo en null.
-- Confirmado con datos: la consulta de control devolvio 0 filas en produccion.
--
-- ⛔ CORRERLO SOBRE DATOS SANOS LE DEVOLVERIA EL DINERO AL SOCIO POR SEGUNDA VEZ:
--    insertaria contra-movimientos positivos de aporte que nadie debe.
--
-- El diagnostico de verdad esta en 208_DIAGNOSTICO_APORTES_ACUERDO_43.sql (solo SELECT).
--
-- Se conserva el archivo, y no se borra, para que quede el rastro del error: deduje la
-- semantica de save() en vez de leerla, y escribi un script de reparacion sobre esa deduccion.
-- =====================================================================================

-- =====================================================================================
-- APORTES CONSUMIDOS Y NUNCA DEVUELTOS — acuerdos de condonacion mixtos
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 207 (rango 200-249)
--
-- ESCRIBE (bloque 2). Controles antes y despues; COMMIT y reverso comentados.
--
-- ⛔ EL DEFECTO, medido contra el codigo, no deducido:
--   `AcuerdoCondonacionServiceImpl:566` hacia `pagoPrestamoService.saveSingle(pago);` SIN
--   reasignar el retorno. `saveSingle` -> `EntityDaoImpl.save()` -> `em.merge()`, que devuelve
--   una instancia NUEVA administrada: el objeto que se le paso NO recibe el codigo generado.
--   Veinte lineas mas abajo ese mismo `pago` se le pasa a `consumirAportes`, que hace
--   `pagoAporte.setPagoPrestamo(pagoPrestamo)` — con el codigo en null, la FK PGAP.PGPRCDGO
--   quedo en NULL.
--
--   Consecuencia: al reversar, `revertirAportes` busca por
--   `pagoAporteDaoService.selectByPagoPrestamo(pago.getCodigo())` y NO ENCUENTRA NADA.
--   El aporte del socio quedo consumido (movimiento negativo en CRD.APRT) y nunca se devolvio.
--
--   El resto del proyecto si reasigna: MotorPagoPrestamoServiceImpl:593,
--   ProcesoCargaPetroServiceImpl:340, AbonoCapitalPrestamoServiceImpl:256. La :566 era la unica.
--   NO es un defecto del reverso: existe desde que se construyo el acuerdo mixto (2026-08-30).
--   Hasta hoy no habia forma de reversar un acuerdo, por eso nunca se vio.
--
-- ⚠️ EL ARREGLO DEL CODIGO NO REPARA LO YA GRABADO. Las filas de PGAP con FK nula siguen ahi.
--    Este script las encuentra y crea el contra-movimiento que el reverso no pudo crear.
--
-- ⛔ NO REPROCESAR EL COBRO 43 (ni ningun acuerdo afectado) ANTES DE CORRER ESTE SCRIPT:
--    los aportes de la primera aplicacion siguen consumidos. Reprocesar los consumiria OTRA
--    VEZ y el socio quedaria descontado por partida doble.
--
-- ⚠️ SECUENCIAS: CRD.APRT y CRD.PGAP usan GenerationType.IDENTITY — la base genera el PK.
--    NO se insertan claves explicitas y NO hay ninguna secuencia que sincronizar.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los bloques 0, 1 y 3.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES: quienes son y cuanto es
-- El localizador es PGAPCNCP: consumirAportes graba ahi la glosa que arma aplicarAcuerdo,
-- con la forma 'Acuerdo <n> - Evento <m>'. Un PGAP de acuerdo con PGPRCDGO NULL es,
-- por construccion, una victima de este defecto.
-- Esperado: al menos la fila del cobro 43. Si devuelve 0 filas, PARAR y avisar.
-- =====================================================================================

SELECT p.PGAPCDGO        AS PAGO_APORTE,
       p.PGAPCNCP        AS CONCEPTO,
       p.APRTCDGO        AS APORTE_CONSUMIDO,
       p.PGAPVLRR        AS VALOR,
       p.PGAPIDST        AS ESTADO_PGAP,
       p.PGPRCDGO        AS FK_PAGO_PRESTAMO,
       a.ENTDCDGO        AS ENTIDAD,
       e.ENTDRZNS        AS PARTICIPE,
       a.TPAPCDGO        AS TIPO_APORTE,
       a.APRTTPMV        AS TIPO_MOVIMIENTO,
       a.APRTVLRR        AS VALOR_APRT,
       p.PGAPFCRG        AS FECHA_REGISTRO
  FROM CRD.PGAP p
  JOIN CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %'
 ORDER BY p.PGAPCDGO;

-- 0.2 Totales, para dimensionar antes de escribir
SELECT COUNT(*) AS FILAS_HUERFANAS,
       COUNT(DISTINCT a.ENTDCDGO) AS PARTICIPES,
       ROUND(SUM(p.PGAPVLRR), 2)  AS TOTAL_NO_DEVUELTO
  FROM CRD.PGAP p
  JOIN CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %';


-- =====================================================================================
-- BLOQUE 1 — ¿Cual de esos acuerdos YA se reverso? Solo esos hay que devolver.
-- Un acuerdo aplicado y NO reversado tiene sus aportes consumidos con razon: no se tocan.
-- Se devuelven SOLO los de acuerdos cuyo cobro esta hoy fuera de PROCESADO(3) — o sea,
-- reversado (APROBADO=2) o anulado (ANULADO=5).
-- Esperado: la fila del cobro 43, en estado 2.
-- =====================================================================================

SELECT p.PGAPCDGO   AS PAGO_APORTE,
       p.PGAPVLRR   AS VALOR,
       ac.ACCNCDGO  AS ACUERDO,
       ac.ACCNESTD  AS ESTADO_ACUERDO,
       c.CBCRCDGO   AS COBRO,
       c.CBCRESTD   AS ESTADO_COBRO,
       CASE WHEN c.CBCRESTD = 3 THEN 'NO TOCAR - sigue procesado'
            ELSE 'DEVOLVER' END AS ACCION
  FROM CRD.PGAP p
  JOIN CRD.APRT a  ON a.APRTCDGO = p.APRTCDGO
  LEFT JOIN CRD.ACCN ac ON ac.ACCNCDGO =
        TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, '^Acuerdo ([0-9]+)', 1, 1, NULL, 1))
  LEFT JOIN CRD.CBCR c ON c.CBCRCDGO = ac.CBCRCDGO
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %'
 ORDER BY p.PGAPCDGO;


-- =====================================================================================
-- BLOQUE 2 — LA REPARACION
-- Replica exactamente lo que `revertirAportes` (ProcesoPagoPrestamoServiceImpl:1434) habria
-- hecho: un contra-movimiento POSITIVO en CRD.APRT por cada PagoAporte, y el PGAP a estado 0.
--
-- Constantes, tomadas del codigo y no inventadas:
--   APRTIDST = 4  -> ESTADO_APORTE_CONSUMIDO = EstadoCuotaPrestamo.PAGADA
--   APRTTPMV = 5  -> CrdTipoMovimientoAporte.REVERSO
--
-- ⚠️ Solo toca los que el BLOQUE 1 marco 'DEVOLVER'. La condicion del WHERE es la misma.
-- =====================================================================================

INSERT INTO CRD.APRT (ENTDCDGO, FLLLCDGO, TPAPCDGO, APRTVLRR, APRTVLPG, APRTSLDO,
                      APRTIDST, APRTIDAS, APRTFCTR, APRTPRDV, APRTTPMV, APRTGLSA,
                      APRTUSRG, APRTFCRG)
SELECT a.ENTDCDGO,
       a.FLLLCDGO,
       a.TPAPCDGO,
       ROUND(ABS(p.PGAPVLRR), 2),
       0,
       0,
       4,
       NULL,
       SYSDATE,
       a.APRTPRDV,
       5,
       'REVERSO MANUAL sql/207 - ' || p.PGAPCNCP || ' (PGAP ' || p.PGAPCDGO || ')',
       'SISTEMA',
       SYSDATE
  FROM CRD.PGAP p
  JOIN CRD.APRT a  ON a.APRTCDGO = p.APRTCDGO
  LEFT JOIN CRD.ACCN ac ON ac.ACCNCDGO =
        TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, '^Acuerdo ([0-9]+)', 1, 1, NULL, 1))
  LEFT JOIN CRD.CBCR c ON c.CBCRCDGO = ac.CBCRCDGO
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %'
   AND p.PGAPIDST <> 0
   AND c.CBCRESTD <> 3;

UPDATE CRD.PGAP p
   SET p.PGAPIDST = 0
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %'
   AND p.PGAPIDST <> 0
   AND EXISTS (SELECT 1
                 FROM CRD.ACCN ac
                 JOIN CRD.CBCR c ON c.CBCRCDGO = ac.CBCRCDGO
                WHERE ac.ACCNCDGO =
                      TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, '^Acuerdo ([0-9]+)', 1, 1, NULL, 1))
                  AND c.CBCRESTD <> 3);


-- =====================================================================================
-- BLOQUE 3 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado 3.1: una fila de REVERSO por cada fila que el BLOQUE 1 marco 'DEVOLVER',
--               con el mismo valor en positivo.
-- Esperado 3.2: 0 filas — ningun PGAP de acuerdo huerfano queda con estado distinto de 0
--               entre los que habia que devolver.
-- Si no da eso: ROLLBACK y avisar.
-- =====================================================================================

-- 3.1 Los contra-movimientos creados
SELECT a.APRTCDGO AS APORTE_NUEVO, a.ENTDCDGO AS ENTIDAD, a.TPAPCDGO AS TIPO_APORTE,
       a.APRTVLRR AS VALOR, a.APRTTPMV AS TIPO_MOV, a.APRTGLSA AS GLOSA
  FROM CRD.APRT a
 WHERE a.APRTGLSA LIKE 'REVERSO MANUAL sql/207 -%'
 ORDER BY a.APRTCDGO;

-- 3.2 No debe quedar ninguno pendiente de devolver
SELECT p.PGAPCDGO, p.PGAPIDST, p.PGAPVLRR, p.PGAPCNCP
  FROM CRD.PGAP p
  LEFT JOIN CRD.ACCN ac ON ac.ACCNCDGO =
        TO_NUMBER(REGEXP_SUBSTR(p.PGAPCNCP, '^Acuerdo ([0-9]+)', 1, 1, NULL, 1))
  LEFT JOIN CRD.CBCR c ON c.CBCRCDGO = ac.CBCRCDGO
 WHERE p.PGPRCDGO IS NULL
   AND p.PGAPCNCP LIKE 'Acuerdo %'
   AND p.PGAPIDST <> 0
   AND c.CBCRESTD <> 3;

-- 3.3 Saldo del participe, para verlo con ojos de negocio
SELECT a.ENTDCDGO AS ENTIDAD, a.TPAPCDGO AS TIPO_APORTE,
       ROUND(SUM(CASE WHEN a.APRTTPMV = 5 THEN a.APRTVLRR ELSE 0 END), 2) AS REVERSOS,
       ROUND(SUM(a.APRTVLRR), 2) AS TOTAL_MOVIMIENTOS
  FROM CRD.APRT a
 WHERE a.ENTDCDGO IN (SELECT DISTINCT a2.ENTDCDGO
                        FROM CRD.PGAP p2
                        JOIN CRD.APRT a2 ON a2.APRTCDGO = p2.APRTCDGO
                       WHERE p2.PGAPCNCP LIKE 'Acuerdo %')
 GROUP BY a.ENTDCDGO, a.TPAPCDGO
 ORDER BY a.ENTDCDGO, a.TPAPCDGO;


-- =====================================================================================
-- BLOQUE 4 — CONFIRMAR
-- Descomentar recien despues de leer el BLOQUE 3.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 5 — REVERSO (comentado)
-- Solo sirve ANTES del COMMIT (ahi alcanza ROLLBACK) o para deshacerlo despues.
-- =====================================================================================

-- UPDATE CRD.PGAP SET PGAPIDST = 1
--  WHERE PGPRCDGO IS NULL AND PGAPCNCP LIKE 'Acuerdo %' AND PGAPIDST = 0;
-- DELETE FROM CRD.APRT WHERE APRTGLSA LIKE 'REVERSO MANUAL sql/207 -%';
-- COMMIT;
