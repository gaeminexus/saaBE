-- =====================================================================
-- e2-20 — ¿Por que el archivo del Internacional invierte AHORROS y CORRIENTE?
-- Modulo: TSR/PGS  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚪ SOLO LECTURA. URGENTE. Correr el BLOQUE 1 primero: solo con eso alcanza
--    para decidir el arreglo.
--
-- ⚠️ CORREGIDO el 2026-09-07: la v1 usaba PDTRNMBR y PRBRNMBR, que NO EXISTEN.
--    Las columnas reales, sacadas de las entidades DetalleRubro y Rubro:
--      SCP.PDTR: PDTRCDGO · PRBRCDGO(FK) · PDTRDSCR · PDTRVLRN · PDTRVLRV · PDTRALTR · PDTRESTD
--      SCP.PRBR: PRBRCDGO · PRBRDSCR · PRBRFCHA · PRBRALTR · PRBRTPOO
--    Y el rubro se identifica por su CODIGO ALTERNO (PRBRALTR), no por la PK:
--    el frontend hace `d.rubro?.codigoAlterno === 23` y el DAO de la casa busca
--    por "RubAltDetAlt" (rubro alterno + detalle alterno).
--
-- EL SINTOMA
--   En el archivo del Banco Internacional las cuentas de ahorro salen como
--   corriente y las corrientes como ahorro.
--
-- LA CAUSA RAIZ, ya verificada en el codigo
--   Hay DOS rubros para el mismo concepto, con los alternos INVERTIDOS:
--     rubro  23  TipoCuentasBancarias    CORRIENTE=1 · AHORROS=2   (TSR, titulares)
--     rubro 199  RhhTipoCuentaBancaria   AHORROS=1 · CORRIENTE=2   (RRHH, empleados)
--   Lo confirma el DDL de RRHH: "alterno rubro 199=codigo banco (1=AH;2=CC)".
--   Los formateadores comparan SIEMPRE contra el 23, lean el valor de donde lo
--   lean, y el javadoc de BeneficiarioOcasional no dice cual rubro es.
--
-- LA REGLA, dada por el usuario
--   Siempre se almacena el CODIGO ALTERNO, y el rubro valido es el que usa la
--   pantalla de datos de entidad. Verificado: entidad-participe-info filtra por
--   codigoAlterno === 23. => EL RUBRO VALIDO ES EL 23.
-- =====================================================================


-- =====================================================================
-- BLOQUE 1 -- ⭐ LA PRUEBA. Correr esto y mandar la salida.
-- =====================================================================
-- ESPERADO SI LA CONSTANTE JAVA ESTA BIEN:
--     rubro 23  -> alterno 1 = CORRIENTE , alterno 2 = AHORROS
--     rubro 199 -> alterno 1 = AHORROS   , alterno 2 = CORRIENTE
-- Si el rubro 23 sale al reves, la constante TipoCuentasBancarias esta
-- invertida respecto de esta base y ESA es la causa.
--
-- Se muestran la PK y el alterno del rubro juntos a proposito, para que quede
-- claro cual de los dos es el "23".
SELECT 'BLOQUE 1 - catalogos' AS bloque,
       r.PRBRCDGO             AS rubro_pk,
       r.PRBRALTR             AS rubro_alterno,
       r.PRBRDSCR             AS rubro_descripcion,
       d.PDTRALTR             AS detalle_alterno,
       d.PDTRDSCR             AS detalle_descripcion
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR IN (23, 199)
    OR r.PRBRCDGO IN (23, 199)
 ORDER BY r.PRBRALTR, r.PRBRCDGO, d.PDTRALTR;


-- =====================================================================
-- BLOQUE 2 -- Que tipo traen las cuentas de titular (las del archivo)
-- =====================================================================
-- ESPERADO: contrastar A MANO unas pocas contra lo que el usuario sabe que son.
-- Si las que el sabe que son de AHORROS tienen CTBNTPCT = 1, entonces en esta
-- base el 1 es ahorros y la constante Java esta invertida.
SELECT 'BLOQUE 2 - cuentas de titular' AS bloque,
       c.CTBNCDGO   AS id_cuenta,
       c.CTBNNMRO   AS numero_cuenta,
       c.CTBNTPCT   AS tipo_guardado,
       b.BEXTNMBR   AS banco
  FROM TSR.CTBN c
  LEFT JOIN TSR.BEXT b ON b.BEXTCDGO = c.BEXTCDGO
 WHERE c.CTBNTPCT IS NOT NULL
   AND ROWNUM <= 20
 ORDER BY c.CTBNTPCT, c.CTBNCDGO;


-- =====================================================================
-- BLOQUE 3 -- Cuantas cuentas hay de cada tipo, en los dos mundos
-- =====================================================================
SELECT 'BLOQUE 3 - titulares (rubro 23)' AS bloque,
       c.CTBNTPCT AS tipo, COUNT(*) AS cuantas
  FROM TSR.CTBN c WHERE c.CTBNTPCT IS NOT NULL
 GROUP BY c.CTBNTPCT ORDER BY c.CTBNTPCT;

SELECT 'BLOQUE 3 - empleados (rubro 199)' AS bloque,
       e.CBEMTPCT AS tipo, COUNT(*) AS cuantas
  FROM RHH.CBEM e WHERE e.CBEMTPCT IS NOT NULL
 GROUP BY e.CBEMTPCT ORDER BY e.CBEMTPCT;


-- =====================================================================
-- COMO SE CORRIGE EN CADA CASO
-- =====================================================================
--  CASO A — El bloque 1 muestra el rubro 23 INVERTIDO respecto del codigo
--     (en la base, alterno 1 = AHORROS).
--     -> Se corrige la constante TipoCuentasBancarias. Una linea.
--     ⚠️ Esa constante tambien la usa CertificadoServiceImpl de crd para
--        imprimir "cuenta de ahorros"/"cuenta corriente": los certificados de
--        OTRO EQUIPO vienen saliendo mal desde siempre. Hay que avisarles.
--
--  CASO B — El bloque 1 sale OK y las cuentas del archivo son de empleado.
--     -> Llegan valores del rubro 199 a un campo que se lee como 23. Se corrige
--        NORMALIZANDO EN EL ORIGEN: RRHH traduce 199 -> 23 al armar el
--        BeneficiarioOcasional, y el javadoc del DTO dice de una vez cual rubro.
--     ⛔ NO con un "if origen == RHH" en el formateador: eso es enumerar
--        origenes, y el origen N+1 vuelve a salir mal en silencio.
--
--  CASO C — Todo OK en catalogos y el archivo usa cuentas de titular.
--     -> Las cuentas estan cargadas con el tipo cambiado en TSR.CTBN. Es
--        correccion de DATOS. El bloque 3 dice cuantas.
--
-- ⛔ En ningun caso se invierte el mapeo dentro del formateador sin saber cual
--    de los tres es: daria vuelta tambien las que hoy salen bien.
--
-- FIN — no se modifico nada.
-- =====================================================================
