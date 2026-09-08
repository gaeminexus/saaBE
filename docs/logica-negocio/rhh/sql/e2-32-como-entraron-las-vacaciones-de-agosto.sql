-- =====================================================================
-- e2-32 — Como entraron las vacaciones de agosto 2026 (por que no hay renglon)
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- 🔴 POR QUE EXISTE
--   El usuario: "solo me interesa que se de de baja la provision de vacaciones de
--   los dias que los empleados tomaron vacaciones en agosto". Y "si habia
--   provision de vacaciones hasta el momento".
--
--   Las dos cosas son ciertas y NO son la causa. El e2-30 (punto 2) midio que en
--   el periodo 81 NO EXISTE NINGUN RENGLON del concepto de vacaciones (alterno
--   12 / rol 36). La baja se calcula a partir de ese renglon: sin renglon, no
--   hay nada que dar de baja, con o sin provision.
--
--   El renglon lo crea UNA sola cosa: aprobar una solicitud de vacaciones
--   (SolicitudVacacionesServiceImpl -> NovedadNomina con el concepto alterno 12
--   -> el calculo del rol la convierte en renglon). Y esa aprobacion exige hoy
--   que el periodo este ABIERTO estricto — si el usuario intento aprobar con el
--   periodo ya CALCULADO, le fallo con "no esta abierto" y la vacacion quedo sin
--   novedad. Es la hipotesis principal. Este script la mide.
-- =====================================================================


-- =====================================================================
-- 1 — Las solicitudes de vacaciones que caen en agosto 2026, con su estado.
-- =====================================================================
-- SLCTAPRB = 'S' aprobada. SLCTFHAP = fecha de aprobacion.
-- 🔴 Si aparecen filas con SLCTAPRB <> 'S' (o NULL): esas vacaciones NUNCA
--    generaron novedad. Es la causa. Se aprueban despues de descontabilizar.
-- 🔴 Si aparecen con SLCTAPRB = 'S' pero el punto 2 no muestra su novedad:
--    la aprobacion no llego a crear la novedad; avisar con el resultado.
-- ⚠️ Si NO aparece ninguna fila: las vacaciones no se registraron por la
--    solicitud sino de otra forma (a mano en novedades, o solo en asistencia).
--    Entonces hay que registrarlas por solicitud, que es lo que el motor lee.
SELECT '1 - solicitudes que tocan agosto' AS control,
       s.SLCTCDGO AS solicitud,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       s.SLCTFCHD AS desde, s.SLCTFCHH AS hasta, s.SLCTDIAS AS dias,
       s.SLCTESTD AS estado, s.SLCTAPRB AS aprobada, s.SLCTFHAP AS fecha_aprobacion,
       s.SLCTOBSR AS observacion
  FROM RHH.SLCT s
  JOIN RHH.MPLD e ON e.MPLDCDGO = s.MPLDCDGO
 WHERE s.SLCTFCHD <= DATE '2026-08-31'
   AND s.SLCTFCHH >= DATE '2026-08-01'
 ORDER BY s.SLCTFCHD, empleado;


-- =====================================================================
-- 2 — Todas las novedades del periodo 81, agrupadas por concepto.
-- =====================================================================
-- Muestra que SI entro al rol de agosto. Si el concepto de alterno 12
-- ("Vacaciones pagadas") no aparece, confirma que la aprobacion nunca creo la
-- novedad. Si aparece OTRO concepto con "vacacion" en el nombre, las vacaciones
-- se cargaron a mano con un concepto que el motor no reconoce como rol 36.
SELECT '2 - novedades de 8/2026 por concepto' AS control,
       c.CPNMCDGO, c.CPNMNMBR AS concepto, c.CPNMALTR AS alterno, c.CPNMROLM AS rol_motor,
       COUNT(*) AS novedades, SUM(NVL(n.NVNMVLRR, 0)) AS suma_valor,
       SUM(CASE WHEN n.NVNMAPRB = 'S' THEN 1 ELSE 0 END) AS aprobadas
  FROM RHH.NVNM n
  JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
 WHERE n.PRDNCDGO = 81
 GROUP BY c.CPNMCDGO, c.CPNMNMBR, c.CPNMALTR, c.CPNMROLM
 ORDER BY c.CPNMNMBR;


-- =====================================================================
-- 3 — Conceptos con "vacac" en el nombre, por si hay mas de uno en juego.
-- =====================================================================
SELECT '3 - conceptos de vacaciones' AS control,
       c.CPNMCDGO, c.PJRQCDGO AS empresa, c.CPNMNMBR, c.CPNMALTR AS alterno,
       c.CPNMROLM AS rol_motor, c.CPNMTPCN AS tipo, c.CPNMESTD AS estado
  FROM RHH.CPNM c
 WHERE UPPER(c.CPNMNMBR) LIKE '%VACAC%'
 ORDER BY c.PJRQCDGO, c.CPNMCDGO;


-- =====================================================================
-- QUE SIGUE, segun lo que devuelva
-- =====================================================================
-- CASO A — Punto 1 muestra solicitudes SIN aprobar. Es el caso esperado. Camino:
--   descontabilizar 81 (funcion nueva, en construccion) -> aprobar esas
--   solicitudes (el guard de ABIERTO estricto se relaja a ABIERTO/CALCULADO en el
--   mismo cambio) -> recalcular -> aprobar -> contabilizar. El asiento del rol
--   sale con la linea 42 debitando 2.5.14 por el valor integro de esas
--   vacaciones (decision "A": sin tope).
--
-- CASO B — Punto 1 no muestra nada y el punto 2 tampoco tiene nada de
--   vacaciones. Las vacaciones no estan en el sistema como tales. Hay que
--   cargar las solicitudes (fecha desde/hasta de agosto) y aprobarlas, y despues
--   el mismo camino del caso A.
--
-- CASO C — Punto 2 muestra un concepto de vacaciones DISTINTO al de alterno 12
--   con novedades en agosto. Se cargaron a mano con otro concepto. Decidir si
--   ese concepto tambien gana el rol 36 (un UPDATE, como el e2-29) o si se
--   reemplazan por solicitudes.
--
-- ⛔ EN CUALQUIER CASO: NO GENERAR LA ORDEN DE PAGO DE AGOSTO TODAVIA. No existe
--    forma de anularla una vez generada, y descontabilizar el periodo con una
--    orden encima recalcularia la nomina por debajo de ella.
-- =====================================================================
