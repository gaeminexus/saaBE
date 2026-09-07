-- =====================================================================
-- e2-20 — ¿Por que el archivo del Internacional invierte AHORROS y CORRIENTE?
-- Modulo: TSR/PGS  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚪ SOLO LECTURA. No modifica nada. URGENTE.
--
-- EL SINTOMA
--   El usuario reporta que en el archivo del Banco Internacional las cuentas de
--   ahorro salen como corriente y las corrientes como ahorro.
--
-- LO QUE YA ESTA VERIFICADO EN EL CODIGO, sin necesidad de esta consulta
--   Hay DOS rubros para el mismo concepto, con los alternos INVERTIDOS entre si:
--
--     rubro  23  TipoCuentasBancarias      CORRIENTE = 1 · AHORROS   = 2   (TSR, titulares)
--     rubro 199  RhhTipoCuentaBancaria     AHORROS   = 1 · CORRIENTE = 2   (RRHH, empleados)
--
--   Y lo confirma el DDL de RRHH, dos veces:
--     14_DDL_FORMATO_ARCHIVO_BANCARIO.sql:73 y su COMMENT ON COLUMN:
--       "Mapa de tipo de cuenta: alterno rubro 199=codigo banco (1=AH;2=CC)"
--
--   InternacionalArchivoPagoFormateador y PacificoArchivoPagoFormateador
--   comparan SIEMPRE contra TipoCuentasBancarias (rubro 23), lean el tipo de
--   donde lo lean. El campo del que lo leen puede traer cualquiera de las dos
--   convenciones: PGS.PGTR.PGTRTPCT lo puebla el modulo que origina el pago, y
--   el javadoc del DTO BeneficiarioOcasional dice "codigoAlterno del rubro de
--   tipo de cuenta bancaria" SIN decir cual de los dos.
--
-- ⭐ LA REGLA, dada por el usuario el 2026-09-07 — esto ya NO se discute
--   1. SIEMPRE se almacena el CODIGO ALTERNO, nunca la PK del detalle.
--   2. El rubro valido es el que usa la pantalla de actualizacion de datos de
--      entidad. Verificado: esa pantalla es
--      saaFE/src/app/modules/crd/forms/entidad-participe/entidad-participe-info/
--      y en sus lineas 391-397 carga los tipos de cuenta filtrando
--      `d.rubro?.codigoAlterno === 23`, y los resuelve por codigoAlterno.
--      => EL RUBRO VALIDO ES EL 23. El 199 de RRHH queda como convencion
--         interna de ese modulo, y quien mande un pago desde RRHH tiene que
--         TRADUCIR a 23 antes de entregarlo.
--
-- LO QUE FALTA SABER, y es lo unico que decide donde se corrige
--   Si el rubro 23 EN ESTA BASE coincide con la constante Java
--   TipoCuentasBancarias (CORRIENTE=1, AHORROS=2) o esta al reves.
--   Invertir el codigo a ciegas daria vuelta tambien las que hoy salen bien.
--
-- QUE DEVOLVER: la salida de los cuatro bloques, completa.
-- =====================================================================


-- =====================================================================
-- BLOQUE 1 -- ⭐ LA PRUEBA. Que dice cada catalogo, con nombre y todo.
-- =====================================================================
-- ESPERADO SI EL CODIGO ESTA BIEN:
--     rubro  23 -> alterno 1 = algo que diga CORRIENTE, alterno 2 = AHORROS
--     rubro 199 -> alterno 1 = algo que diga AHORROS,   alterno 2 = CORRIENTE
-- Si el rubro 23 sale al reves de eso, la constante Java TipoCuentasBancarias
-- esta invertida respecto de la base y ESA es la causa.
SELECT 'BLOQUE 1 - catalogos' AS bloque,
       d.PRBRCDGO             AS rubro,
       d.PDTRALTR             AS alterno,
       d.PDTRNMBR             AS nombre,
       CASE
         WHEN d.PRBRCDGO = 23  AND d.PDTRALTR = 1 AND UPPER(d.PDTRNMBR) LIKE '%CORRIENTE%' THEN 'OK - coincide con el codigo'
         WHEN d.PRBRCDGO = 23  AND d.PDTRALTR = 2 AND UPPER(d.PDTRNMBR) LIKE '%AHORRO%'    THEN 'OK - coincide con el codigo'
         WHEN d.PRBRCDGO = 199 AND d.PDTRALTR = 1 AND UPPER(d.PDTRNMBR) LIKE '%AHORRO%'    THEN 'OK - coincide con el codigo'
         WHEN d.PRBRCDGO = 199 AND d.PDTRALTR = 2 AND UPPER(d.PDTRNMBR) LIKE '%CORRIENTE%' THEN 'OK - coincide con el codigo'
         ELSE '*** NO COINCIDE — revisar ***'
       END                    AS veredicto
  FROM SCP.PDTR d
 WHERE d.PRBRCDGO IN (23, 199)
 ORDER BY d.PRBRCDGO, d.PDTRALTR;


-- =====================================================================
-- BLOQUE 2 -- De donde salen los tipos de los pagos que YA se generaron
-- =====================================================================
-- PGTRTPCT es el tipo del beneficiario ocasional; PGTRCTBN apunta a la cuenta
-- del titular. Un pago usa uno u otro, nunca los dos.
-- ESPERADO: ver si los pagos que salieron mal traen el tipo por beneficiario
-- ocasional (que puede venir con la convencion de RRHH) o por cuenta de titular.
SELECT 'BLOQUE 2 - origen del tipo' AS bloque,
       p.PGTRORGX                   AS origen_externo,
       COUNT(*)                     AS pagos,
       SUM(CASE WHEN p.PGTRCTBN IS NOT NULL THEN 1 ELSE 0 END) AS con_cuenta_titular,
       SUM(CASE WHEN p.PGTRCTBN IS NULL AND p.PGTRTPCT IS NOT NULL THEN 1 ELSE 0 END) AS con_beneficiario_ocasional,
       MIN(p.PGTRTPCT)              AS tipo_min,
       MAX(p.PGTRTPCT)              AS tipo_max
  FROM PGS.PGTR p
 GROUP BY p.PGTRORGX
 ORDER BY pagos DESC;


-- =====================================================================
-- BLOQUE 3 -- El contraste que no miente: tipo contra numero de cuenta real
-- =====================================================================
-- Las cuentas de titular con su tipo y su banco. Contrastar A MANO una decena
-- contra lo que el usuario sabe que son: si las que el sabe que son de AHORROS
-- tienen CTBNTPCT = 1, entonces en ESTA base el 1 es ahorros y la constante
-- Java (que dice 1 = CORRIENTE) esta invertida.
SELECT 'BLOQUE 3 - cuentas de titular' AS bloque,
       c.CTBNCDGO   AS id_cuenta,
       c.CTBNNMRO   AS numero_cuenta,
       c.CTBNTPCT   AS tipo_guardado,
       (SELECT d.PDTRNMBR FROM SCP.PDTR d
         WHERE d.PRBRCDGO = 23 AND d.PDTRALTR = c.CTBNTPCT AND ROWNUM = 1) AS nombre_segun_rubro_23,
       b.BEXTNMBR   AS banco
  FROM TSR.CTBN c
  LEFT JOIN TSR.BEXT b ON b.BEXTCDGO = c.BEXTCDGO
 WHERE c.CTBNTPCT IS NOT NULL
   AND ROWNUM <= 20
 ORDER BY c.CTBNTPCT, c.CTBNCDGO;


-- =====================================================================
-- BLOQUE 4 -- Lo mismo para las cuentas de empleado (rubro 199)
-- =====================================================================
-- Si estas usan la convencion inversa, cualquier pago de RRHH que llegue al
-- archivo sale invertido aunque el rubro 23 este perfecto.
SELECT 'BLOQUE 4 - cuentas de empleado' AS bloque,
       e.CBEMTPCT   AS tipo_guardado,
       (SELECT d.PDTRNMBR FROM SCP.PDTR d
         WHERE d.PRBRCDGO = 199 AND d.PDTRALTR = e.CBEMTPCT AND ROWNUM = 1) AS nombre_segun_rubro_199,
       COUNT(*)     AS cuantas_cuentas
  FROM RHH.CBEM e
 WHERE e.CBEMTPCT IS NOT NULL
 GROUP BY e.CBEMTPCT
 ORDER BY e.CBEMTPCT;


-- =====================================================================
-- COMO LEER EL RESULTADO, y que se corrige en cada caso
-- =====================================================================
--
--  CASO A — El bloque 1 muestra el rubro 23 INVERTIDO respecto del codigo
--     (alterno 1 = AHORROS en la base, mientras TipoCuentasBancarias dice
--      CORRIENTE = 1).
--     -> La constante Java esta mal. Se corrige TipoCuentasBancarias.
--     ⚠️ OJO: esa constante tambien la usa CertificadoServiceImpl de crd, que
--        imprime "cuenta de ahorros"/"cuenta corriente" en los certificados.
--        O sea que los certificados vienen saliendo mal desde siempre, y el
--        arreglo toca un modulo de OTRO EQUIPO. Hay que avisarles.
--
--  CASO B — El bloque 1 sale OK en los dos rubros, pero el bloque 2 muestra que
--     los pagos traen el tipo por beneficiario ocasional con origen de RRHH.
--     -> El problema es que PGTRTPCT recibe valores del rubro 199 y el
--        formateador los lee como rubro 23. Se corrige NORMALIZANDO EN EL
--        ORIGEN: que RRHH traduzca 199 -> 23 al armar el BeneficiarioOcasional,
--        y que el javadoc del DTO diga de una vez cual rubro es.
--     ⛔ NO se corrige poniendo un "if origen == RHH" en el formateador: eso es
--        enumerar origenes, y el origen N+1 vuelve a salir mal en silencio.
--
--  CASO C — Los dos bloques OK y los pagos son de cuentas de titular.
--     -> Entonces las cuentas estan cargadas con el tipo cambiado en TSR.CTBN,
--        y es correccion de DATOS, no de codigo. El bloque 3 dice cuantas.
--
-- ⛔ EN NINGUN CASO se invierte el mapeo dentro del formateador sin saber cual
--    de los tres es: daria vuelta tambien las cuentas que hoy salen bien.
--
-- FIN — no se modifico nada.
-- =====================================================================
