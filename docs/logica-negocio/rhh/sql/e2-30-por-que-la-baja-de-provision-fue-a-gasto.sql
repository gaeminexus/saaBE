-- =====================================================================
-- e2-30 — Por que la baja de provision (vacaciones y decimo cuarto) fue a gasto
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- 🔴 POR QUE EXISTE
--   El usuario contabilizo el rol 8/2026 (REC-2026-08-0002) y no aparecio ninguna
--   linea de baja de provision de vacaciones. Y el pago del decimo cuarto mando
--   642,72 contra la provision y 5.833,48 a gasto, cuando el esperaba TODO contra
--   la provision.
--
-- LA CAUSA, LEIDA EN EL CODIGO (no supuesta)
--   Las dos bajas topean la parte que va contra la provision con el saldo de
--   RHH.PVNM, y ese saldo se calcula EXCLUYENDO los periodos cuyo PRDNMODO no sea
--   2 (PRODUCTIVO_CONTABILIZA): ProvisionNominaDaoServiceImpl.sumaValorByEmpleadoYTipo
--   y sumaValorByEmpleadosYTipo, "and t.periodoNomina.modo = :productivo".
--
--   La intencion de ese tope (plan #4bis, 2026-09-01) era sana: si una provision
--   NUNCA se contabilizo, no se puede dar de baja un pasivo que no existe, y lo
--   correcto es reconocer el gasto ahora.
--
--   Pero esta empresa MIGRO: los periodos 1-7/2026 se cargaron en modo historico
--   (sin asiento en este sistema) porque esa contabilidad YA EXISTIA en el sistema
--   anterior y entro por saldos iniciales. Para el codigo esas provisiones "nunca
--   se contabilizaron"; para la contabilidad real, SI estan, en el saldo inicial
--   de 2.5.09 / 2.5.14. El tope mira la tabla equivocada para este caso.
--
--   Resultado: el unico periodo productivo es 8/2026, el saldo topeado es solo lo
--   provisionado en agosto, y todo lo demas cae a gasto EN SILENCIO — el codigo
--   lo llama "degrada a todo a gasto, que es lo correcto". No lo es cuando la
--   provision vive en saldos iniciales.
--
--   Para vacaciones hay ademas una segunda pregunta previa: si en 8/2026 no hubo
--   ningun renglon del concepto de vacaciones gozadas (rol 36), la baja no tenia
--   nada que procesar. El punto 2 lo distingue.
-- =====================================================================


-- =====================================================================
-- 1 — El modo de cada periodo. ESTO ES LO QUE DECIDE.
-- =====================================================================
-- modo 1 = HISTORICO_SIN_CONTABILIZAR, 2 = PRODUCTIVO_CONTABILIZA.
-- ESPERADO segun la hipotesis: 1-7/2026 en modo 1 (o NULL), 8/2026 en modo 2.
SELECT '1 - modo de los periodos' AS control,
       p.PRDNCDGO AS periodo, p.PRDNMSEE AS mes, p.PRDNANOO AS anio,
       p.PRDNESTD AS estado, p.PRDNMODO AS modo,
       CASE p.PRDNMODO WHEN 2 THEN 'PRODUCTIVO - cuenta para el tope'
                       ELSE 'HISTORICO/NULL - EXCLUIDO del tope' END AS efecto
  FROM RHH.PRDN p
 ORDER BY p.PRDNANOO, p.PRDNMSEE;


-- =====================================================================
-- 2 — ¿Hubo renglones de vacaciones gozadas en 8/2026?
-- =====================================================================
-- Si esto devuelve 0 filas, la baja de vacaciones no tenia nada que procesar en
-- agosto y la pregunta es en que mes se tomaron esas vacaciones. Si devuelve
-- filas, el renglon existio y la causa es el tope del punto 3.
SELECT '2 - renglones de vacaciones gozadas en 8/2026' AS control,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       c.CPNMNMBR AS concepto, c.CPNMALTR AS alterno, c.CPNMROLM AS rol_motor,
       r.RNGLVLRO AS valor
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = n.MPLDCDGO
 WHERE n.PRDNCDGO = 81
   AND (c.CPNMALTR = 12 OR c.CPNMROLM = 36)
 ORDER BY empleado;


-- =====================================================================
-- 3 — El saldo de provision de VACACIONES por empleado: lo que el tope VE
--     contra lo que REALMENTE hay acumulado.
-- =====================================================================
-- Si "ve_el_tope" es mucho menor que "acumulado_total", esa diferencia es lo que
-- fue a gasto en vez de a la provision.
SELECT '3 - provision de vacaciones por empleado' AS control,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       SUM(CASE WHEN p.PRDNMODO = 2 THEN v.PVNMVLOR ELSE 0 END) AS ve_el_tope,
       SUM(v.PVNMVLOR) AS acumulado_total,
       SUM(v.PVNMVLOR) - SUM(CASE WHEN p.PRDNMODO = 2 THEN v.PVNMVLOR ELSE 0 END) AS excluido
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = v.MPLDCDGO
 WHERE v.PVNMTPPR = 3
 GROUP BY e.MPLDAPLL, e.MPLDNMBR
 ORDER BY excluido DESC;


-- =====================================================================
-- 4 — Lo mismo para el DECIMO CUARTO (tipo 2), en total.
-- =====================================================================
-- ESPERADO segun la hipotesis: ve_el_tope ≈ 642,72 (solo agosto) y
-- acumulado_total bastante mayor. La diferencia es lo que fue a gasto.
SELECT '4 - provision de decimo cuarto, total' AS control,
       SUM(CASE WHEN p.PRDNMODO = 2 THEN v.PVNMVLOR ELSE 0 END) AS ve_el_tope,
       SUM(v.PVNMVLOR) AS acumulado_total
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
 WHERE v.PVNMTPPR = 2;


-- =====================================================================
-- 5 — Y cuanto de eso esta en la CONTABILIDAD (saldo de las cuentas de
--     provision), que es contra lo que el usuario quiere dar de baja.
-- =====================================================================
-- Reemplazar los codigos de cuenta si no son estos (salen de la captura del
-- usuario: 2.5.09 decimo cuarto, 2.5.14 vacaciones). Solo asientos ACTIVOS.
-- Columnas verificadas contra la entidad DetalleAsiento el 2026-09-08 (la primera
-- version de este bloque las tenia inventadas y fallo con ORA-00904):
--   DTASCNTA = numero de cuenta · DTASDBEE = debe · DTASHBRR = haber
SELECT '5 - saldo contable de las cuentas de provision' AS control,
       d.DTASCNTA AS cuenta,
       SUM(NVL(d.DTASHBRR, 0)) - SUM(NVL(d.DTASDBEE, 0)) AS saldo_acreedor
  FROM CNT.DTAS d
  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE d.DTASCNTA IN ('2.5.09', '2.5.14')
   AND a.ASNTESTD = 1
 GROUP BY d.DTASCNTA;


-- =====================================================================
-- QUE SIGUE — ES UNA DECISION DEL USUARIO, NO UN BUG
-- =====================================================================
-- El tope existe para no dar de baja un pasivo que no esta en libros. En una
-- empresa que migro con saldos iniciales, el pasivo SI esta en libros aunque no
-- este en RHH.PVNM en modo productivo. Opciones:
--
--   A. SIN TOPE: toda la baja va contra la provision, siempre. Es lo que el
--      usuario dijo esperar ("debia enviar todo contra la provision"). Riesgo:
--      si un empleado nuevo cobra decimo sin que nadie lo haya provisionado, la
--      cuenta de provision queda en negativo por esa parte, y lo ajusta el
--      contador. Es el comportamiento de la mayoria de los sistemas de nomina.
--
--   B. TOPE CONTRA TODO PVNM (historico + productivo), no solo productivo. Mas
--      fiel al acumulado real, pero sigue dependiendo de que la carga historica
--      haya llenado PVNM para los meses migrados — y el e2-27 mostro que la
--      apertura no cargo ni los dias trabajados, asi que probablemente tampoco
--      las provisiones de agosto-diciembre 2025.
--
--   C. TOPE CONTRA EL SALDO CONTABLE de la cuenta (punto 5). Es el unico que
--      mira el pasivo real. Mas trabajo: hay que consultar CNT desde RHH.
--
-- Recomendacion del arbitro: A. Es lo que el usuario espera, es lo mas simple,
-- y el caso que el tope protegia (provision nunca contabilizada en ningun lado)
-- no es el de esta empresa. Aplicar a las TRES bajas que usan el tope: decimos
-- (contabilizarBajaProvisionBeneficioSocial), vacaciones gozadas
-- (acumulaBajaProvisionVacaciones) y finiquito (descargaProvisionActuarial).
-- =====================================================================
