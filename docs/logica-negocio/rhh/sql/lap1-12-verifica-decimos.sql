-- =====================================================================
-- lap1-12  ·  Verificacion del pago de decimos contra la normativa
-- Equipo lap-saa-1 · 2026-09-08
-- =====================================================================
--
-- SOLO CONSULTAS. No modifica nada.
--
-- Acompana a docs/logica-negocio/rhh/AUDITORIA-DECIMOS-CONTRA-NORMATIVA.md
-- y sirve para responder la unica pregunta que el codigo no puede contestar
-- solo: si el defecto del cambio de modalidad (#2 de la auditoria) llego a
-- ocurrir en esta base o quedo latente.
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — ¿CUANTA GENTE PODRIA ESTAR AFECTADA?
--             Modalidades vigentes de decimo tercero y cuarto.
--             Rubro RHH_MODALIDAD_DECIMO_*: 1 mensualizado, 2 acumulado.
-- ---------------------------------------------------------------------
SELECT NVL(TO_CHAR(c.CNTEMDD3), 'NULL') AS MODALIDAD_D3,
       NVL(TO_CHAR(c.CNTEMDD4), 'NULL') AS MODALIDAD_D4,
       COUNT(*) AS CONTRATOS
  FROM RHH.CNTE c
 GROUP BY c.CNTEMDD3, c.CNTEMDD4
 ORDER BY 1, 2;

-- ⚠️ Nombres de columna de RHH.CNTE sin verificar contra el DDL real. Si
--    falla, sacar la lista y ajustar — NO adivinar:
--      SELECT COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS
--       WHERE OWNER='RHH' AND TABLE_NAME='CNTE' ORDER BY COLUMN_ID;
--    Se buscan las columnas de modalidad de decimo tercero y cuarto.


-- ---------------------------------------------------------------------
-- CONTROL 2 — EL QUE IMPORTA: ¿alguien cobro decimo MENSUALIZADO en el rol
--             y ademas tiene una liquidacion ANUAL del mismo anio?
--             Si devuelve filas, el defecto #2 YA OCURRIO y hay dinero
--             pagado dos veces.
--
--             Conceptos de rol por su rol en el motor (RhhRolConceptoMotor):
--               6 = DECIMO_TERCERO, 7 = DECIMO_CUARTO
--             Tipo de beneficio (RhhTipoBeneficioSocial):
--               contrastar con el rubro; en el codigo son las constantes
--               DECIMO_TERCERO / DECIMO_CUARTO
-- ---------------------------------------------------------------------
SELECT b.MPLDCDGO                AS ID_EMPLEADO,
       b.LQBSANOO                AS ANIO,
       b.LQBSTPBN                AS TIPO_BENEFICIO,
       b.LQBSVLOR                AS VALOR_LIQUIDADO_ANUAL,
       b.LQBSVLMN                AS VALOR_MENSUALIZADO_REGISTRADO,
       SUM(r.RGNMVLOR)           AS COBRADO_EN_ROLES,
       COUNT(DISTINCT n.NMNACDGO) AS ROLES_CON_PAGO
  FROM RHH.LQBS b
  JOIN RHH.NMNA n ON n.MPLDCDGO = b.MPLDCDGO
  JOIN RHH.RGNM r ON r.NMNACDGO = n.NMNACDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE c.CPNMROLM IN (6, 7)
   AND b.LQBSVLOR > 0
 GROUP BY b.MPLDCDGO, b.LQBSANOO, b.LQBSTPBN, b.LQBSVLOR, b.LQBSVLMN
HAVING SUM(r.RGNMVLOR) > 0
 ORDER BY b.MPLDCDGO, b.LQBSANOO;

-- Interpretacion:
--   0 filas            -> el defecto esta LATENTE, no causo dano. Se corrige
--                         igual, pero no hay nada que reparar hacia atras.
--   filas con
--   VALOR_MENSUALIZADO_REGISTRADO = 0 y COBRADO_EN_ROLES > 0
--                      -> se pago dos veces esa parte. El monto duplicado es
--                         COBRADO_EN_ROLES dentro de la ventana del beneficio.
--
-- ⚠️ Esta consulta NO acota los roles a la ventana del beneficio (dic-nov
--    para el tercero, ago-jul o mar-feb para el cuarto): trae todos los roles
--    del empleado. Es a proposito, para no esconder casos por un filtro de
--    fechas mal puesto. Si devuelve filas, el siguiente paso es mirar CADA
--    caso con el CONTROL 3, que si acota.


-- ---------------------------------------------------------------------
-- CONTROL 3 — detalle mes a mes de un empleado y anio concretos.
--             Reemplazar <ID_EMPLEADO> y <ANIO>.
--             Muestra que cobro en cada rol por concepto de decimo, para
--             comparar contra la ventana legal del beneficio.
-- ---------------------------------------------------------------------
-- SELECT p.PRDNANOO AS ANIO_ROL,
--        p.PRDNMESS AS MES_ROL,
--        c.CPNMNMBR AS CONCEPTO,
--        c.CPNMROLM AS ROL_MOTOR,
--        r.RGNMVLOR AS VALOR
--   FROM RHH.RGNM r
--   JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
--   JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
--   JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
--  WHERE n.MPLDCDGO = <ID_EMPLEADO>
--    AND c.CPNMROLM IN (6, 7)
--  ORDER BY p.PRDNANOO, p.PRDNMESS;


-- ---------------------------------------------------------------------
-- CONTROL 4 — la base del decimo tercero, concepto por concepto.
--             Es la tabla del #1.1 de la auditoria, contra la base REAL y
--             no contra el script de carga: alguien pudo cambiar una
--             bandera desde la pantalla de parametrizacion.
-- ---------------------------------------------------------------------
SELECT c.CPNMALTR AS CODIGO,
       c.CPNMNMBR AS CONCEPTO,
       c.CPNMBSDT AS BASE_DECIMO_TERCERO,
       c.CPNMBSDC AS BASE_DECIMO_CUARTO,
       c.CPNMIMIE AS IMPONIBLE_IESS,
       c.CPNMESTD AS ESTADO
  FROM RHH.CPNM c
 ORDER BY c.PJRQCDGO, c.CPNMALTR;

-- Lo esperado segun el Art. 95, y lo que hay que mirar fila por fila:
--   BSDT = 'S' en: Sueldo, horas suplementarias, horas extraordinarias,
--                  recargo nocturno, bono de responsabilidad, comisiones
--   BSDT = 'N' en: decimo tercero y cuarto mensualizados, fondos de reserva,
--                  utilidades, movilizacion, alimentacion, subsidio IESS,
--                  reintegro, honorarios
--   'Vacaciones pagadas' -> ver #3 de la auditoria: la respuesta depende de
--                  si el concepto es la remuneracion del periodo vacacional
--                  (entra) o la compensacion de vacaciones no gozadas
--                  (discutible). PREGUNTA PARA LA CONTADORA.


-- ---------------------------------------------------------------------
-- CONTROL 5 — parametros del anio que usan los dos calculos.
--             SBU y dias base tienen que estar cargados por anio: si falta
--             la fila del anio, el calculo no puede correr.
-- ---------------------------------------------------------------------
SELECT p.PJRQCDGO AS EMPRESA,
       p.PRNMANOO AS ANIO,
       p.PRNMSBUU AS SBU,
       p.PRNMDIAS AS DIAS_MES,
       p.PRNMDANO AS DIAS_ANIO,
       p.PRNMFNRS AS PCT_FONDOS_RESERVA
  FROM RHH.PRNM p
 ORDER BY p.PJRQCDGO, p.PRNMANOO;

-- Lo esperado: DIAS_ANIO = 360 (anio comercial) y DIAS_MES = 30. El SBU
-- debe ser el del anio; para 2026 el sistema tiene 482,00 verificado contra
-- la planilla real.


-- ---------------------------------------------------------------------
-- CONTROL 6 — el acumulado muerto del #4: BASE_DECIMO_CUARTO deberia dar
--             cero en todas las filas. Si alguna vez alguien marca un
--             CPNMBSDC = 'S', esto deja de ser cero y conviene saberlo.
--             Reemplazar el tipo por el valor de RhhTipoAcumulado.BASE_DECIMO_CUARTO.
-- ---------------------------------------------------------------------
-- SELECT COUNT(*) AS FILAS, SUM(a.ACMNVLOR) AS SUMA_BASE_D4
--   FROM RHH.ACMN a
--  WHERE a.ACMNTIPO = <TIPO_BASE_DECIMO_CUARTO>;
