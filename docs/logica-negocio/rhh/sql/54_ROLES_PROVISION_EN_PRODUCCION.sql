-- =====================================================
-- MODULO: RHH - LOS SEIS ROLES DE PROVISION QUE QUEDARON EN NULO
-- DESCRIPCION: Asigna CPNMROLM 17..22 a los conceptos de provision y rellena
--              el concepto de las provisiones ya escritas.
-- ORDEN DE EJECUCION: 54
-- FECHA: 2026-08-21
-- PARAMETRO: :EMPRESA -- 1236
-- DESTINO: PRODUCCION
-- =====================================================
-- COMO SE ENCONTRO. El bloque 1B del contraste de enero saco UNA sola fila de
-- PROVISION, con el alterno y el nombre en blanco y 22 personas, cuando en
-- local salen CUATRO con sus nombres. El LEFT JOIN a CPNM devolvia nulo y el
-- GROUP BY las fundia en una.
-- .
-- QUE PASA. generaProvision (ProcesoNominaServiceImpl:1591) recibe el concepto
-- de conceptoPorRol(conceptos, PROVISION_*) y LO ACEPTA NULO SIN DECIR NADA:
-- escribe la provision con PVNM.CPNMCDGO en nulo. El importe es correcto --se
-- calculo y se guardo bien--; lo que falta es la clasificacion, y el concepto
-- es lo que aporta la CUENTA CONTABLE.
-- .
-- POR QUE ESTABA EN NULO. El sql/11 asigna los roles con
--   UPDATE RHH.CPNM SET CPNMROLM = 17 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 50;
-- y ese bloque no surtio efecto en produccion. UN UPDATE QUE NO ENCUENTRA
-- FILAS NO DA ERROR: es la regla operativa 2, en silencio y en produccion.
-- Verificado el 2026-08-21: los seis conceptos EXISTEN, estan ACTIVOS
-- (CPNMESTD = 1) y tienen sus alternos 50..55; solo les falta CPNMROLM.
-- .
-- ALCANCE VERIFICADO ANTES DE ESCRIBIR ESTE SCRIPT. El censo completo de
-- CPNMROLM en produccion da los roles 1..16 y 23..31, sin huecos ni repetidos.
-- FALTAN EXACTAMENTE LOS SEIS DE PROVISION. El 32 (patronal del finiquito)
-- no existe todavia a proposito: es el punto 3 de la lista del final de la
-- calibracion, no un dano.
-- .
-- POR QUE IMPORTA MAS DE LO QUE PARECE. conceptoPorRol gobierna ONCE ramas del
-- motor, y casi todas fallan igual de calladas: si el concepto viene nulo,
-- sencillamente no se genera el renglon. Enero cuadro al centavo, asi que los
-- roles que enero ejercito estaban; eso no dice nada de los que no ejercito.
-- .
-- QUE NO HACE ESTE SCRIPT: no toca ningun importe. Los valores de las
-- provisiones de enero son correctos y no cambian.
-- =====================================================


-- =====================================================
-- CONTROL 1 - LOS SEIS CONCEPTOS, ANTES.
-- Esperado: seis filas, CPNMROLM NULO, CPNMESTD = 1.
-- Si alguno trae ya su rol, ese UPDATE simplemente no cambiara nada.
-- Si alguno esta INACTIVO, PARAR: selectActivosByEmpresa no lo veria y el
-- concepto seguiria saliendo nulo aunque el rol quedara puesto.
-- =====================================================
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR, CPNMROLM, CPNMESTD
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR BETWEEN 50 AND 55
 ORDER BY CPNMALTR;


-- =====================================================
-- CONTROL 2 - NADIE MAS PUEDE TENER YA LOS ROLES 17..22.
-- RHH.CPNM lleva el indice unico UQ_CPNM_ROLM(PJRQCDGO, CPNMROLM) que puso el
-- sql/11: si otro concepto se hubiera quedado con uno de estos roles, el
-- UPDATE reventaria con ORA-00001 a mitad del bloque.
-- Esperado: SIN FILAS.
-- =====================================================
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR, CPNMROLM
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM BETWEEN 17 AND 22
 ORDER BY CPNMROLM;


-- =====================================================
-- PASO 1 - LOS SEIS ROLES.
-- Identicos a las lineas 84..89 del sql/11. Reejecutables sin dano: asignan
-- el mismo valor. MIRAR CUANTAS FILAS TOCA CADA UNO: tiene que ser 1.
-- =====================================================
UPDATE RHH.CPNM SET CPNMROLM = 17 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 50; -- Provision decimo tercero
UPDATE RHH.CPNM SET CPNMROLM = 18 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 51; -- Provision decimo cuarto
UPDATE RHH.CPNM SET CPNMROLM = 19 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 52; -- Provision vacaciones
UPDATE RHH.CPNM SET CPNMROLM = 20 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 53; -- Provision fondos de reserva
UPDATE RHH.CPNM SET CPNMROLM = 21 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 54; -- Provision jubilacion patronal
UPDATE RHH.CPNM SET CPNMROLM = 22 WHERE PJRQCDGO = :EMPRESA AND CPNMALTR = 55; -- Provision desahucio

COMMIT;


-- =====================================================
-- PASO 2 - RELLENAR EL CONCEPTO DE LAS PROVISIONES YA ESCRITAS.
-- .
-- POR QUE HACE FALTA, y por que no basta con arreglar el catalogo: EN
-- PRODUCCION LOS MESES NO SE VUELVEN A CALCULAR. El recalculo de enero..julio
-- con el motor corregido se hizo en LOCAL; produccion se carga una sola vez y
-- ya con el WAR final. Sin este paso, las provisiones de enero se quedarian
-- con el concepto en nulo PARA SIEMPRE, y arreglarlo despues obligaria a
-- reabrir un periodo cerrado --el punto 6, que reabrirPeriodo no avisa--.
-- .
-- LA EQUIVALENCIA NO SE INVENTA: sale de las cuatro llamadas a generaProvision
-- del motor, que emparejan cada RhhTipoProvision con su RhhRolConceptoMotor.
--   PVNMTPPR 1 decimo tercero      -> CPNMROLM 17
--   PVNMTPPR 2 decimo cuarto       -> CPNMROLM 18
--   PVNMTPPR 3 vacaciones          -> CPNMROLM 19
--   PVNMTPPR 4 fondos de reserva   -> CPNMROLM 20
--   PVNMTPPR 6 jubilacion patronal -> CPNMROLM 21   (hoy sin generador)
--   PVNMTPPR 7 desahucio           -> CPNMROLM 22   (hoy sin generador)
-- .
-- SOLO TOCA LAS FILAS CON EL CONCEPTO EN NULO. Una provision que ya lo tenga
-- no se pisa: si algun dia se reasignara un concepto a mano, este script no
-- lo deshace.
-- =====================================================
UPDATE RHH.PVNM v
   SET v.CPNMCDGO = (
        SELECT c.CPNMCDGO FROM RHH.CPNM c
         WHERE c.PJRQCDGO = :EMPRESA
           AND c.CPNMROLM = CASE v.PVNMTPPR
                                 WHEN 1 THEN 17 WHEN 2 THEN 18 WHEN 3 THEN 19
                                 WHEN 4 THEN 20 WHEN 6 THEN 21 WHEN 7 THEN 22
                            END)
 WHERE v.CPNMCDGO IS NULL
   AND v.PVNMTPPR IN (1, 2, 3, 4, 6, 7)
   AND EXISTS (SELECT 1 FROM RHH.CPNM c
                WHERE c.PJRQCDGO = :EMPRESA
                  AND c.CPNMROLM = CASE v.PVNMTPPR
                                        WHEN 1 THEN 17 WHEN 2 THEN 18 WHEN 3 THEN 19
                                        WHEN 4 THEN 20 WHEN 6 THEN 21 WHEN 7 THEN 22
                                   END);

COMMIT;
-- Esperado con solo enero cargado: 61 filas (19 + 19 + 22 + 1).


-- =====================================================
-- CONTROL 3 - EL CENSO COMPLETO, DESPUES.
-- Tienen que salir 31 filas con CPNMROLM del 1 al 31, sin huecos ni repetidos.
-- Es el control de la cabecera del sql/11, actualizado: aquel decia 22 porque se
-- escribio antes de que existieran los roles del finiquito (23..31).
-- El rol 32 (patronal del finiquito) NO sale, y es correcto: no existe todavia.
-- =====================================================
SELECT CPNMROLM, CPNMALTR, CPNMNMBR
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL
 ORDER BY CPNMROLM;


-- =====================================================
-- CONTROL 4 - NINGUNA PROVISION PUEDE QUEDAR SIN CONCEPTO.
-- Esperado: SIN FILAS.
-- =====================================================
SELECT p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES, v.PVNMTPPR AS TIPO, COUNT(*) AS SIN_CONCEPTO
  FROM RHH.PVNM v JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
 WHERE v.CPNMCDGO IS NULL
 GROUP BY p.PRDNANOO, p.PRDNMSEE, v.PVNMTPPR
 ORDER BY 1, 2, 3;


-- =====================================================
-- CONTROL 5 - EL BLOQUE 1B, AHORA COMO EN LOCAL.
-- Cuatro filas con su nombre. Enero: 19 / 19 / 22 / 1 personas y
-- 1.292,52 / 720,38 / 823,19 / 183,26. La de FONDOS DE RESERVA con UNA
-- persona, que es Viteri Lopez, y ese 1 es el que exige el esperado.
-- Los importes NO cambian: este script no toca ningun valor.
-- =====================================================
SELECT c.CPNMALTR AS ALTERNO, c.CPNMNMBR AS CONCEPTO,
       COUNT(DISTINCT v.MPLDCDGO) AS PERSONAS, SUM(v.PVNMVLOR) AS TOTAL
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
  LEFT JOIN RHH.CPNM c ON c.CPNMCDGO = v.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 1
 GROUP BY c.CPNMALTR, c.CPNMNMBR
 ORDER BY 1;
