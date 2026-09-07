-- =====================================================================================
-- POR QUE NO SE REVERSARON LOS APORTES DEL ACUERDO — cobro 43
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 208 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cinco bloques son SELECT.
--
-- ⛔⛔ EL sql/207 QUEDA ANULADO. NO CORRERLO.
--   Su hipotesis era que `AcuerdoCondonacionServiceImpl:566` dejaba la FK PGAP.PGPRCDGO en
--   NULL por no reasignar el retorno de saveSingle. **Es FALSA.**
--   `EntityDaoImpl.save():306` NO hace merge cuando la entidad es nueva:
--
--       if (id == null) { selloAuditoria(tipo); em.persist(tipo); }
--       else            { em.merge(tipo); }
--
--   `persist()` escribe el id EN EL OBJETO QUE SE LE PASA. Como `saveSingle` llama
--   `save(pago, pago.getCodigo())` con codigo nulo, `pago` sale de ahi con su codigo puesto.
--   La FK nunca estuvo en null — y el usuario lo confirmo: la consulta de control del 207
--   devolvio 0 filas.
--
--   Correr el 207 sobre datos sanos CREARIA contra-movimientos positivos de aporte que le
--   devolverian el dinero al socio POR SEGUNDA VEZ.
--
--   (El cambio `pago = pagoPrestamoService.saveSingle(pago)` que quedo commiteado es
--   INOFENSIVO pero INERTE: alinea el estilo con el resto del proyecto y no arregla nada.)
--
-- QUE SI SE SABE, leido en el codigo:
--   - `consumirAportes` NO modifica ningun aporte existente: inserta una fila NEGATIVA en
--     CRD.APRT (APRTVLRR = -valor, APRTTPMV = 4 PAGO_PRESTAMO) mas un CRD.PGAP que la enlaza
--     al PagoPrestamo.
--   - El saldo disponible es la SUMA de APRTVLRR (`sumValorByEntidadYTipo`).
--   - `revertirAportes` inserta una fila POSITIVA (APRTTPMV = 5 REVERSO) y pone PGAP.PGAPIDST
--     en 0. Esa suma es la que devuelve el saldo.
--   O sea que la maquinaria es coherente. Falta ver QUE PASO de verdad con estos datos.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cinco bloques.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — El cobro y su acuerdo
-- Esperado: el cobro 43 en estado 2 (APROBADO, o sea reversado) y el acuerdo en 1 (VIGENTE).
-- =====================================================================================

SELECT c.CBCRCDGO, c.CBCRTPOO, c.CBCRESTD, c.CBCRVLRR, c.CBCRFCHA,
       c.CBCRUSRV, c.CBCRFCRV, c.CBCRNMRV
  FROM CRD.CBCR c
 WHERE c.CBCRCDGO = 43;

SELECT * FROM CRD.ACCN ac WHERE ac.CBCRCDGO = 43;


-- =====================================================================================
-- BLOQUE 2 — Cuanto se iba a cruzar con aportes, segun el acuerdo
-- CRD.DAAP es el desglose que `aplicarAcuerdo` convierte en el llamado a consumirAportes.
-- Si esto viene VACIO, el acuerdo no tenia parte de aportes y el problema es otro.
-- =====================================================================================

SELECT d.DAAPCDGO, d.ACCNCDGO, d.TPAPCDGO, d.DAAPVLOR
  FROM CRD.DAAP d
 WHERE d.ACCNCDGO IN (SELECT ac.ACCNCDGO FROM CRD.ACCN ac WHERE ac.CBCRCDGO = 43)
 ORDER BY d.DAAPCDGO;


-- =====================================================================================
-- BLOQUE 3 — El evento y sus pagos de prestamo
-- Esperado: el evento anulado y su(s) PagoPrestamo con PGPRANUL = 1.
-- =====================================================================================

SELECT * FROM CRD.EVPR e
 WHERE e.EVPRCDGO IN (SELECT ac.EVPRCDGO FROM CRD.ACCN ac WHERE ac.CBCRCDGO = 43)
    OR e.EVPRCDGO IN (SELECT dc.EVPRCDGO FROM CRD.DCBC dc WHERE dc.CBCRCDGO = 43);

SELECT g.PGPRCDGO, g.EVPRCDGO, g.PRSTCDGO, g.DTPRCDGO, g.PGPRVLRR, g.PGPRANUL,
       g.PGPRUSAN, g.PGPRFCAN
  FROM CRD.PGPR g
 WHERE g.EVPRCDGO IN (SELECT ac.EVPRCDGO FROM CRD.ACCN ac WHERE ac.CBCRCDGO = 43)
    OR g.EVPRCDGO IN (SELECT dc.EVPRCDGO FROM CRD.DCBC dc WHERE dc.CBCRCDGO = 43)
 ORDER BY g.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 4 — ⭐ EL BLOQUE QUE CONTESTA: los PagoAporte y sus movimientos
--
-- PGAPIDST = 0 significa "ya revertido" (lo pone revertirAportes).
-- PGAPIDST = 1 significa que sigue vigente, o sea que NO se revirtio.
--
-- APRTTPMV: 4 = PAGO_PRESTAMO (el consumo, valor negativo)
--           5 = REVERSO       (la devolucion, valor positivo)
-- =====================================================================================

SELECT p.PGAPCDGO      AS PAGO_APORTE,
       p.PGPRCDGO      AS FK_PAGO_PRESTAMO,
       p.PGAPIDST      AS ESTADO_PGAP,
       p.PGAPVLRR      AS VALOR_PGAP,
       p.PGAPCNCP      AS CONCEPTO,
       a.APRTCDGO      AS APRT_CONSUMO,
       a.APRTVLRR      AS VALOR_APRT,
       a.APRTTPMV      AS TIPO_MOV,
       a.ENTDCDGO      AS ENTIDAD,
       a.TPAPCDGO      AS TIPO_APORTE,
       a.APRTGLSA      AS GLOSA
  FROM CRD.PGAP p
  JOIN CRD.APRT a ON a.APRTCDGO = p.APRTCDGO
 WHERE p.PGPRCDGO IN (
        SELECT g.PGPRCDGO FROM CRD.PGPR g
         WHERE g.EVPRCDGO IN (SELECT ac.EVPRCDGO FROM CRD.ACCN ac WHERE ac.CBCRCDGO = 43)
            OR g.EVPRCDGO IN (SELECT dc.EVPRCDGO FROM CRD.DCBC dc WHERE dc.CBCRCDGO = 43))
 ORDER BY p.PGAPCDGO;

-- 4.2 Por si el PGAP quedo colgado sin FK (la hipotesis del 207, para descartarla del todo)
SELECT p.PGAPCDGO, p.PGPRCDGO, p.PGAPIDST, p.PGAPVLRR, p.PGAPCNCP, p.PGAPFCRG
  FROM CRD.PGAP p
 WHERE p.PGPRCDGO IS NULL
 ORDER BY p.PGAPCDGO DESC
 FETCH FIRST 20 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 5 — ⭐ EL SALDO DEL SOCIO, que es lo que el usuario esta mirando
-- Todos los movimientos de aporte de la entidad del acuerdo, del mas nuevo al mas viejo.
-- Si hay una fila con APRTTPMV = 5 (REVERSO) que compense la de APRTTPMV = 4 (consumo),
-- el reverso SI ocurrio y el problema es de lo que muestra la pantalla, no de los datos.
-- Si NO hay ninguna fila 5, el reverso NO ocurrio y el defecto esta en el codigo.
-- =====================================================================================

SELECT a.APRTCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTTPMV, a.APRTIDST,
       a.APRTFCTR, a.APRTFCRG, a.APRTGLSA
  FROM CRD.APRT a
 WHERE a.ENTDCDGO IN (SELECT c.ENTDCDGO FROM CRD.CBCR c WHERE c.CBCRCDGO = 43)
 ORDER BY a.APRTCDGO DESC
 FETCH FIRST 40 ROWS ONLY;

-- 5.2 El saldo por tipo de aporte, como lo calcula la aplicacion (suma de APRTVLRR)
SELECT a.TPAPCDGO AS TIPO_APORTE, ROUND(SUM(a.APRTVLRR), 2) AS SALDO_DISPONIBLE
  FROM CRD.APRT a
 WHERE a.ENTDCDGO IN (SELECT c.ENTDCDGO FROM CRD.CBCR c WHERE c.CBCRCDGO = 43)
 GROUP BY a.TPAPCDGO
 ORDER BY a.TPAPCDGO;
