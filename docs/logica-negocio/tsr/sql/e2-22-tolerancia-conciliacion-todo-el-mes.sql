-- =====================================================================
-- e2-22 — La tolerancia de fechas de la conciliación pasa a TODO EL MES
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Hace un UPDATE. Pero NO hay que recompilar ni
--    desplegar nada: la tolerancia es un PARAMETRO, no una constante del codigo.
--
-- EL SINTOMA
--   Al conciliar: "Las fechas involucradas difieren 27 dia(s) entre si, fuera de
--   la tolerancia configurada (5 dia(s))".
--
-- DONDE VIVE
--   ConciliacionContableMatchServiceImpl:687-691
--       detalleRubroService.selectValorNumericoByRubAltDetAlt(
--           Rubros.ASP_TOLERANCIA_DIAS_CONCILIACION_CONTABLE, 1);
--   O sea: rubro ALTERNO 178, detalle ALTERNO 1, campo PDTRVLRN.
--
-- ⚠️ Se resuelve por CODIGO ALTERNO (PRBRALTR), no por la PK. No son lo mismo:
--    hoy mismo se midio que el rubro con PK 199 es "ESTADO DEL DESCUENTO
--    RECURRENTE" mientras el de tipo de cuenta tiene PK 200 y alterno 199.
--    Filtrar por PK no falla: devuelve el rubro equivocado.
--
-- QUE VALOR
--   El usuario pidio que se pueda conciliar un movimiento del dia 1 con uno del
--   ultimo dia del mes. Entre el 1 y el 31 hay 30 dias de diferencia, y la
--   validacion es "diasEntreFechas > tolerancia". Con 31 entra cualquier mes,
--   incluidos los de 31 dias, con un dia de margen.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo y LEER.
-- =====================================================================
-- ESPERADO: una fila, con PDTRVLRN = 5 (la tolerancia actual del mensaje).
-- Si devuelve mas de una fila o ninguna, PARAR y avisar.
SELECT 'BLOQUE 0 - antes' AS control,
       r.PRBRCDGO AS rubro_pk,
       r.PRBRALTR AS rubro_alterno,
       r.PRBRDSCR AS rubro_descripcion,
       d.PDTRCDGO AS detalle_pk,
       d.PDTRALTR AS detalle_alterno,
       d.PDTRDSCR AS detalle_descripcion,
       d.PDTRVLRN AS tolerancia_actual
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 178
   AND d.PDTRALTR = 1;


-- =====================================================================
-- BLOQUE 1 — El cambio
-- =====================================================================
UPDATE SCP.PDTR d
   SET d.PDTRVLRN = 31
 WHERE d.PDTRALTR = 1
   AND d.PRBRCDGO IN (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 178);

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL POSTERIOR
-- =====================================================================
-- ESPERADO: la misma fila del bloque 0, ahora con tolerancia_actual = 31.
SELECT 'BLOQUE 2 - despues' AS control,
       r.PRBRALTR AS rubro_alterno,
       d.PDTRALTR AS detalle_alterno,
       d.PDTRDSCR AS detalle_descripcion,
       d.PDTRVLRN AS tolerancia_nueva
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 178
   AND d.PDTRALTR = 1;


-- =====================================================================
-- BLOQUE 3 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Devuelve la tolerancia a 5 dias, que es como estaba antes del 2026-09-07.
--
-- UPDATE SCP.PDTR d
--    SET d.PDTRVLRN = 5
--  WHERE d.PDTRALTR = 1
--    AND d.PRBRCDGO IN (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 178);
-- COMMIT;


-- =====================================================================
-- QUE HAY QUE SABER DE ESTE CAMBIO
-- =====================================================================
--  · NO hace falta recompilar ni desplegar: el servicio lee el parametro en
--    cada conciliacion. El efecto es inmediato.
--
--  · La tolerancia se usa en DOS lugares del mismo servicio:
--      - La validacion manual, la del mensaje (linea 232).
--      - El emparejamiento AUTOMATICO (lineas 504-559): con que movimientos del
--        extracto se sugiere emparejar cada asiento.
--    Subirla a 31 afloja las dos. La automatica va a proponer mas candidatos, y
--    con un mes entero de ventana puede sugerir pares que antes descartaba sola.
--    ⚠️ Conviene mirar las sugerencias automaticas la primera vez que se concilie
--    despues de este cambio: el operador ahora filtra lo que antes filtraba el
--    parametro.
--
--  · Queda guardado por grupo: GrupoConciliacionContable.toleranciaDiasAplicada
--    registra con que tolerancia se concilio cada grupo, asi que las
--    conciliaciones viejas siguen diciendo que se hicieron con 5.
-- =====================================================================
