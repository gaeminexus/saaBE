-- ============================================================================
-- VERIFICACION (SOLO LECTURA) -- eventos PAGO_MANUAL que /prst/pagarCuota aplico
-- SIN pasar por un cobro de credito. U-5b #18. Reescrita 2026-11-01.
--
-- POR QUE LA VERSION ANTERIOR ERA ENGANOSA (verificado en el codigo):
--   * ContabilidadPrestamoServiceImpl.contabilizarPagoCuota es un STUB que SIEMPRE
--     devuelve null A PROPOSITO: pagarCuota lo llama CobroCreditoServiceImpl
--     (lineas ~1194 y ~1408), y el asiento de ese dinero es el del COBRO
--     (CRD.CBCR.CBCRASN1 transitorio / CBCRASN2 definitivo / CBCRASRP reparto), no el
--     del evento. Por eso TODO EVPR 'PAGO_MANUAL' tiene EVPRNMAS nulo, sea de caja o no.
--   * El enlace evento -> cobro es CRD.DCBC.EVPRCDGO (DCBC.CBCRCDGO -> CBCR).
--   * El unico codigo que crea EVPRTPOO='PAGO_MANUAL' es ProcesoPagoPrestamoServiceImpl
--     .pagarCuota (crearEvento, tipo TIPO_PAGO_MANUAL). Lo invocan: el cobro de credito
--     (PAGO_CUOTA y cada linea de PAGO_MULTIPLE), pagarMultiplesCuotas (que delega en
--     pagarCuota) y el REST /prst/pagarCuota (hoy cerrado, 410). No hay otro INSERT en
--     el codigo ni en los scripts del repo; lo que no tenga cobro y venga de cargas o
--     migracion historica tuvo que entrar por SQL externo a este repo.
--
-- SOLO LISTA. NO ejecuta INSERT/UPDATE/DELETE. Lo corre el usuario.
-- EVPRESTD = 1 es vigente (crearEvento lo graba asi). CBCRESTD: 1 REGISTRADO,
-- 2 APROBADO, 3 PROCESADO, 4 RECHAZADO, 5 ANULADO (rubros.CrdEstadoCobro).
-- ============================================================================

-- 0) PANORAMA: cada PAGO_MANUAL vivo clasificado.
--    CON_COBRO_OK         : ligado a un cobro que tiene algun asiento (transitorio, definitivo o reparto) -> correcto.
--    CON_COBRO_SIN_ASIENTO: ligado a un cobro PROCESADO sin ninguno de los tres asientos -> ANOMALIA REAL, revisar.
--    CON_COBRO_NO_VIGENTE : ligado a un cobro REGISTRADO/APROBADO/RECHAZADO/ANULADO -> informativo.
--    SIN_COBRO_HORA_0000  : sin cobro y con hora 00:00:00 -> probable carga/migracion/cuadre (ver consulta 2).
--    SIN_COBRO            : sin cobro y con hora real -> candidatos a haber salido de /pagarCuota sin contabilizar (consulta 3).
SELECT clasificacion, COUNT(*) AS eventos, SUM(valor) AS valor
  FROM (
        SELECT e.EVPRVLRR AS valor,
               CASE
                 WHEN c.CBCRCDGO IS NOT NULL AND c.CBCRESTD = 3
                      AND c.CBCRASN1 IS NULL AND c.CBCRASN2 IS NULL AND c.CBCRASRP IS NULL THEN 'CON_COBRO_SIN_ASIENTO'
                 WHEN c.CBCRCDGO IS NOT NULL AND (c.CBCRASN1 IS NOT NULL OR c.CBCRASN2 IS NOT NULL OR c.CBCRASRP IS NOT NULL)
                                                                                          THEN 'CON_COBRO_OK'
                 WHEN c.CBCRCDGO IS NOT NULL                                              THEN 'CON_COBRO_NO_VIGENTE'
                 WHEN e.EVPRFCHA = TRUNC(e.EVPRFCHA)                                      THEN 'SIN_COBRO_HORA_0000'
                 ELSE 'SIN_COBRO'
               END AS clasificacion
          FROM CRD.EVPR e
          LEFT JOIN CRD.DCBC d ON d.EVPRCDGO = e.EVPRCDGO
          LEFT JOIN CRD.CBCR c ON c.CBCRCDGO = d.CBCRCDGO
         WHERE e.EVPRTPOO = 'PAGO_MANUAL'
           AND e.EVPRESTD = 1
       )
 GROUP BY clasificacion
 ORDER BY clasificacion;

-- 1) ANOMALIA REAL: eventos ligados a un cobro PROCESADO que no tiene ningun asiento.
SELECT e.EVPRCDGO AS evento, e.EVPRFCHA AS fecha_evento, e.EVPRVLRR AS valor, e.EVPRUSAR AS usuario_evento,
       c.CBCRCDGO AS cobro, c.CBCRFCHA AS fecha_cobro, c.CBCRVLRR AS valor_cobro, c.CBCRUSRG AS usuario_cobro
  FROM CRD.EVPR e
  JOIN CRD.DCBC d ON d.EVPRCDGO = e.EVPRCDGO
  JOIN CRD.CBCR c ON c.CBCRCDGO = d.CBCRCDGO
 WHERE e.EVPRTPOO = 'PAGO_MANUAL' AND e.EVPRESTD = 1
   AND c.CBCRESTD = 3
   AND c.CBCRASN1 IS NULL AND c.CBCRASN2 IS NULL AND c.CBCRASRP IS NULL
 ORDER BY e.EVPRFCHA;

-- 2) CARGAS / MIGRACION: PAGO_MANUAL vivo SIN cobro. El codigo NO distingue un usuario tecnico ni un campo de
--    origen en EVPR, asi que se agrupa por usuario y por "hora 00:00" para que el usuario los reconozca.
--    Un usuario que no es de caja y/o todo en 00:00:00 (jun-jul) indica carga historica o cuadre.
SELECT e.EVPRUSAR AS usuario,
       CASE WHEN e.EVPRFCHA = TRUNC(e.EVPRFCHA) THEN 'HORA_0000' ELSE 'HORA_REAL' END AS hora,
       TO_CHAR(e.EVPRFCHA, 'YYYY-MM') AS mes,
       COUNT(*) AS eventos, SUM(e.EVPRVLRR) AS valor,
       MIN(e.EVPRFCHA) AS desde, MAX(e.EVPRFCHA) AS hasta
  FROM CRD.EVPR e
 WHERE e.EVPRTPOO = 'PAGO_MANUAL' AND e.EVPRESTD = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.DCBC d WHERE d.EVPRCDGO = e.EVPRCDGO)
 GROUP BY e.EVPRUSAR,
          CASE WHEN e.EVPRFCHA = TRUNC(e.EVPRFCHA) THEN 'HORA_0000' ELSE 'HORA_REAL' END,
          TO_CHAR(e.EVPRFCHA, 'YYYY-MM')
 ORDER BY mes, usuario;

-- 3) LO QUE QUEDA = candidatos reales de /pagarCuota sin contabilizar: PAGO_MANUAL vivo, sin cobro, con hora real.
--    Un usuario de carga con hora real aparecera aqui tambien: descartalo con la consulta 2.
SELECT p.PRSTCDGO AS prestamo, p.DTPRCDGO AS cuota, p.PGPRCDGO AS pago, p.EVPRCDGO AS evento,
       p.PGPRFCHA AS fecha_pago, p.PGPRVLRR AS valor, e.EVPRUSAR AS usuario, p.PGPRANUL AS anulado
  FROM CRD.PGPR p
  JOIN CRD.EVPR e ON e.EVPRCDGO = p.EVPRCDGO
 WHERE e.EVPRTPOO = 'PAGO_MANUAL' AND e.EVPRESTD = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.DCBC d WHERE d.EVPRCDGO = e.EVPRCDGO)
   AND e.EVPRFCHA <> TRUNC(e.EVPRFCHA)
 ORDER BY p.PRSTCDGO, p.DTPRCDGO, p.PGPRFCHA;
