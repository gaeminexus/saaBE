-- =====================================================================
-- RRHH URGENTE: el arrastre de vacaciones cuenta los dias dos veces
-- Modulo: RHH
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- EL DEFECTO
--   AcreditacionVacacionesServiceImpl, al crear el saldo del año nuevo,
--   hace:
--       diasPendientes = diasAsignados + arrastre - diasUsados
--   donde `arrastre` son los dias no gozados del año anterior. Pero el
--   año anterior NO se descuenta: sigue con sus mismos diasPendientes.
--
--   Y `diasDisponibles` suma los pendientes de TODOS los años no
--   caducados. Resultado: los dias arrastrados se cuentan DOS VECES.
--
--   Caso real, Katherine Pardo (empleado 58) en produccion:
--       2025: asignados 7.75   arrastrados 0      pendientes  7.75
--       2026: asignados 15     arrastrados 7.75   pendientes 22.75
--       /sldv/disponible/58 devuelve 30.50, cuando lo correcto es 22.75
--
--   Detectado el 2026-08-27 al acreditar el periodo 2026 en produccion
--   para que contabilidad pudiera liquidar la nomina de agosto.
--
-- EL ALCANCE
--   7 empleados acreditados en 2026, 51.63 dias duplicados.
--   El total de 2026 dice 156.63 y deberia decir 105.00.
--   Los saldos de 2025 estan BIEN: no hay que tocarlos.
--
-- LA CORRECCION, Y POR QUE ESTA
--   `diasPendientes` del año nuevo NO debe incluir el arrastre:
--       diasPendientes = diasAsignados - diasUsados
--   `diasArrastrados` queda como dato INFORMATIVO -- muestra cuanto viene
--   de atras-- y los dias siguen viviendo en SU año de origen.
--
--   Es lo correcto y no solo lo simple: la caducidad se marca POR AÑO
--   (SLDVCDCD). Si los dias se mudaran al año nuevo, perderian su año de
--   origen y no caducarian nunca. Manteniendolos en su año, el consumo
--   FIFO del mas antiguo primero sigue funcionando y la caducidad tambien.
--
-- OJO: ESTO NO ALCANZA SIN EL CAMBIO DE CODIGO
--   Este script arregla las 7 filas que ya se crearon. Mientras
--   AcreditacionVacacionesServiceImpl siga sumando el arrastre, la
--   proxima acreditacion vuelve a duplicar. El cambio de codigo va
--   aparte y es de una linea.
--
-- EJECUTADO EN PRODUCCION EL 2026-08-27, RESULTADO VERIFICADO
--   Filas que seguian mal: 0
--   2025: 22 saldos, 103.47 dias  (sin cambios, estaban bien)
--   2026:  7 saldos, 105.00 dias  (venia de 156.63)
--   Katherine (58): 2025 pendientes 7.75 + 2026 pendientes 15 = 22.75
--   GET /sldv/disponible devuelve ahora, para los siete:
--     58 -> 22.75   45 -> 22.71   53 -> 21.04   56 -> 22.75
--     57 -> 22.75   62 -> 21.88   65 -> 22.75
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: diagnostico. Las filas mal calculadas.
--   Esperado en produccion al 2026-08-27: 7 filas, 51.63 dias de exceso.
-- ---------------------------------------------------------------------
SELECT S.SLDVCDGO, S.MPLDCDGO, M.MPLDNMBR || ' ' || M.MPLDAPLL AS EMPLEADO,
       S.SLDVANOO AS ANIO, S.SLDVASGN AS ASIGNADOS, S.SLDVDIAR AS ARRASTRADOS,
       S.SLDVUSDO AS USADOS, S.SLDVPNDE AS PENDIENTES_HOY,
       (S.SLDVASGN - NVL(S.SLDVUSDO,0)) AS PENDIENTES_CORRECTO,
       (S.SLDVPNDE - (S.SLDVASGN - NVL(S.SLDVUSDO,0))) AS EXCESO
  FROM RHH.SLDV S JOIN RHH.MPLD M ON M.MPLDCDGO = S.MPLDCDGO
 WHERE NVL(S.SLDVDIAR,0) > 0
   AND S.SLDVPNDE <> (S.SLDVASGN - NVL(S.SLDVUSDO,0))
 ORDER BY S.SLDVANOO, S.MPLDCDGO;

-- Totales antes de tocar nada
SELECT SLDVANOO AS ANIO, COUNT(*) AS SALDOS, SUM(SLDVPNDE) AS DIAS_PENDIENTES
  FROM RHH.SLDV GROUP BY SLDVANOO ORDER BY 1;

-- ---------------------------------------------------------------------
-- BLOQUE 1: la correccion
--   El WHERE es defensivo por partida doble: solo toca filas que TIENEN
--   arrastre y que ademas estan efectivamente mal. Una fila ya correcta
--   no se toca, asi que el script se puede repetir sin daño.
-- ---------------------------------------------------------------------
UPDATE RHH.SLDV S
   SET S.SLDVPNDE = S.SLDVASGN - NVL(S.SLDVUSDO,0)
 WHERE NVL(S.SLDVDIAR,0) > 0
   AND S.SLDVPNDE <> (S.SLDVASGN - NVL(S.SLDVUSDO,0));

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   (a) la consulta del bloque 0 debe devolver CERO filas
--   (b) 2025 debe seguir igual (103.47); 2026 debe pasar a 105.00
--   (c) Katherine (empleado 58) debe quedar con 7.75 + 15 = 22.75
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS FILAS_QUE_SIGUEN_MAL
  FROM RHH.SLDV S
 WHERE NVL(S.SLDVDIAR,0) > 0
   AND S.SLDVPNDE <> (S.SLDVASGN - NVL(S.SLDVUSDO,0));

SELECT SLDVANOO AS ANIO, COUNT(*) AS SALDOS, SUM(SLDVPNDE) AS DIAS_PENDIENTES
  FROM RHH.SLDV GROUP BY SLDVANOO ORDER BY 1;

SELECT SLDVANOO AS ANIO, SLDVASGN AS ASIGNADOS, SLDVDIAR AS ARRASTRADOS,
       SLDVUSDO AS USADOS, SLDVPNDE AS PENDIENTES
  FROM RHH.SLDV WHERE MPLDCDGO = 58 ORDER BY SLDVANOO;

-- La suma de estos dos ultimos pendientes es lo que devolvera
-- GET /sldv/disponible/58 : debe dar 22.75, no 30.50.

COMMIT;
