-- =====================================================================================
-- e2-104 — Pago 769 («Anticipo a trabajador #181», MARCELO ALEJANDRO, 100,00): no se puede confirmar.
-- SOLO LECTURA. Equipo omen-saa-2 · 2026-10-15.
--
-- La pantalla solo muestra STATUS_MARKED_ROLLBACK (defecto ya corregido en saaBE 93c75a2b, sin desplegar).
-- La causa real es una de las validaciones de la contabilización del anticipo a empleado
-- (PagoProgramadoServiceImpl.contabilizarPagoAnticipoEmpleado + AsientoContableServiceImpl
-- .generarAsientoAnticipoEmpleado). Cada bloque revisa UNA de ellas. El primero que no dé lo ESPERADO es la causa.
-- Columnas copiadas de PagoProgramado (PGS.PGTR), AnticipoEmpleado (RHH.ANTE), ConfiguracionNomina (RHH.CFNM),
-- Plantilla (CNT.PLNS), DetallePlantilla (CNT.DTPL), ConceptoNomina (RHH.CPNM), CuentaBancaria (TSR.CNBC).
-- Constantes: RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS = 14; RhhRolConceptoMotor.ANTICIPO_DE_SUELDO = 14.
-- =====================================================================================

-- BLOQUE 1 — El pago. ESPERADO: PGTRORGN = 'RHH_ANTICIPO_EMPLEADO', PGTRIDOR = 181, PGTRCNBC con valor,
-- cuenta bancaria con PLNNCDGO (cuenta contable) no nulo, PGTRASNT nulo.
SELECT 'BLOQUE 1 - pago' AS bloque, p.PGTRCDGO, p.PGTRORGN, p.PGTRIDOR, p.PGTRPJRQ AS empresa, p.PGTRESTD,
       p.PGTRCNBC, c.CNBCNMRO, c.PLNNCDGO AS cuenta_contable_banco, p.PGTRASNT, p.PGTRRFBN, p.PGTRBFNM
  FROM PGS.PGTR p
  LEFT JOIN TSR.CNBC c ON c.CNBCCDGO = p.PGTRCNBC
 WHERE p.PGTRCDGO = 769;

-- BLOQUE 2 — El anticipo 181. ESPERADO: existe, con empleado (MPLDCDGO), número de cuotas, valor de cuota y
-- sin descuento recurrente (DSRCCDGO nulo) porque todavía no se pagó.
SELECT 'BLOQUE 2 - anticipo' AS bloque, a.ANTECDGO, a.MPLDCDGO AS empleado, a.ANTEVLOR, a.ANTENMRO AS cuotas,
       a.ANTEVLCT AS valor_cuota, a.ANTEFCHI AS inicio_descuento, a.ANTEESTD, a.DSRCCDGO
  FROM RHH.ANTE a
 WHERE a.ANTECDGO = (SELECT PGTRIDOR FROM PGS.PGTR WHERE PGTRCDGO = 769);

-- BLOQUE 3 — Configuración de nómina de la empresa del pago. ESPERADO: una fila con CFNMPLRL (plantilla de rol)
-- no nulo. Anotar CFNMCTMR (cuenta marcadora).
SELECT 'BLOQUE 3 - configuracion nomina' AS bloque, f.CFNMCDGO, f.PJRQCDGO, f.CFNMPLRL AS plantilla_rol_alterno,
       f.CFNMCTMR AS cuenta_marcadora, f.CFNMESTD
  FROM RHH.CFNM f
 WHERE f.PJRQCDGO = (SELECT PGTRPJRQ FROM PGS.PGTR WHERE PGTRCDGO = 769);

-- BLOQUE 4 — Plantilla de rol y su línea 14 «Cuentas por Cobrar Empleados». ESPERADO: una fila activa con
-- PLNNCDGO no nulo y DISTINTO de la cuenta marcadora del bloque 3. Si no sale fila, o sale con la marcadora,
-- ésa es la causa: hay que asignar la cuenta real en Nómina → Plantillas Contables.
SELECT 'BLOQUE 4 - linea 14 plantilla rol' AS bloque, s.PLNSCDGO, s.PLNSCDAL, s.PLNSNMBR, d.DTPLCDGO,
       d.DTPLAXL1, d.DTPLESTD, d.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR,
       CASE WHEN d.PLNNCDGO = f.CFNMCTMR THEN 'ES LA MARCADORA' ELSE 'OK' END AS control
  FROM RHH.CFNM f
  JOIN CNT.PLNS s ON s.PLNSCDAL = f.CFNMPLRL AND s.PJRQCDGO = f.PJRQCDGO
  LEFT JOIN CNT.DTPL d ON d.PLNSCDGO = s.PLNSCDGO AND d.DTPLAXL1 = 14
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE f.PJRQCDGO = (SELECT PGTRPJRQ FROM PGS.PGTR WHERE PGTRCDGO = 769);

-- BLOQUE 5 — Concepto de nómina con rol de motor ANTICIPO_DE_SUELDO (14). ESPERADO: al menos una fila activa
-- (CPNMESTD = 1). Sin él, el pago se contabiliza pero no se puede crear el descuento en el rol.
SELECT 'BLOQUE 5 - concepto anticipo' AS bloque, c.CPNMCDGO, c.CPNMALTR, c.CPNMNMBR, c.CPNMROLM, c.CPNMESTD
  FROM RHH.CPNM c
 WHERE c.CPNMROLM = 14
   AND c.PJRQCDGO = (SELECT PGTRPJRQ FROM PGS.PGTR WHERE PGTRCDGO = 769);

-- BLOQUE 6 — ¿Alguna vez se confirmó un anticipo a empleado en producción? ESPERADO: si hay confirmados con
-- asiento, la configuración funcionó antes y el problema es de ESTE pago/anticipo; si no hay ninguno, este es
-- el primero y lo más probable es que falte configuración (bloques 3 a 5).
SELECT 'BLOQUE 6 - historial' AS bloque, p.PGTRESTD, COUNT(*) AS pagos,
       SUM(CASE WHEN p.PGTRASNT IS NOT NULL THEN 1 ELSE 0 END) AS con_asiento
  FROM PGS.PGTR p
 WHERE p.PGTRORGN = 'RHH_ANTICIPO_EMPLEADO'
 GROUP BY p.PGTRESTD
 ORDER BY p.PGTRESTD;
