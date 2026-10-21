-- =====================================================================================
-- MEDICION DEL CORREO PERSONAL DE LOS PARTICIPES - para el "olvide mi clave" de la app
-- =====================================================================================
-- Equipo: app movil ASOPREP (app1). Escrito por el arbitro el 2026-10-21.
-- Diseno: saaBE/docs/logica-negocio/crd/DISENO-RECUPERACION-CLAVE-CORREO-APP-MOVIL.md
--
-- SOLO CONSULTA. No hay un solo INSERT/UPDATE/DELETE en este archivo.
--
-- POR QUE. El usuario decidio (2026-10-21) que la app tenga "olvide mi clave": llega un
-- codigo de 6 digitos al CORREO PERSONAL del participe (CRD.ENTD.ENTDCRPR). Antes de
-- construir hay que saber si ese dato sirve. Cuatro preguntas:
--
--   A. Cuantos participes tienen algo en ENTDCRPR, y cuantos tienen algo con forma de correo?
--   B. Hay correos REPETIDOS entre participes? Es la pregunta mas importante. Si la oficina
--      cargo su propio correo, o uno generico, como relleno, el codigo de recuperacion de
--      todos esos participes le llegaria a la misma persona. Un correo compartido por varios
--      participes NO se puede usar para recuperar la clave.
--   C. Que dice la bandera ENTDCPVR ("tiene correo personal")? Si coincide con lo que de
--      verdad hay en ENTDCRPR, podria servir de filtro; si no coincide, se ignora.
--   D. De los que ya tienen cuenta en la app (CRD.USAP), cuantos tienen correo usable?
--      Hoy son pocos o ninguno; el bloque se deja para volver a correrlo despues.
--
-- Columnas (CRD.ENTD): ENTDCDGO pk, ENTDCRPR correo personal (VARCHAR2 2000),
-- ENTDCRIN correo institucional, ENTDCPVR bandera "tiene correo personal".
-- "Con forma de correo" = algo@algo.algo, sin espacios. Es un filtro grueso, no una
-- validacion: solo sirve para separar lo vacio y la basura evidente.
--
-- Pasame los resultados de los cuatro bloques tal como salen.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- A. Cobertura: total, con algo escrito, con forma de correo
-- -------------------------------------------------------------------------------------
SELECT COUNT(*)                                                         AS total_entidades,
       COUNT(CASE WHEN TRIM(e.ENTDCRPR) IS NOT NULL THEN 1 END)         AS con_algo_escrito,
       COUNT(CASE WHEN REGEXP_LIKE(TRIM(e.ENTDCRPR),
                       '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
                  THEN 1 END)                                           AS con_forma_de_correo
  FROM CRD.ENTD e;


-- -------------------------------------------------------------------------------------
-- B. Correos repetidos entre participes (los 30 mas repetidos)
-- -------------------------------------------------------------------------------------
SELECT LOWER(TRIM(e.ENTDCRPR)) AS correo,
       COUNT(*)                AS participes
  FROM CRD.ENTD e
 WHERE TRIM(e.ENTDCRPR) IS NOT NULL
 GROUP BY LOWER(TRIM(e.ENTDCRPR))
HAVING COUNT(*) > 1
 ORDER BY COUNT(*) DESC
 FETCH FIRST 30 ROWS ONLY;

-- B2. Resumen: cuantos participes tienen un correo UNICO (el unico caso usable)
SELECT COUNT(*) AS participes_con_correo_unico
  FROM (SELECT LOWER(TRIM(e.ENTDCRPR)) AS correo
          FROM CRD.ENTD e
         WHERE REGEXP_LIKE(TRIM(e.ENTDCRPR), '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
         GROUP BY LOWER(TRIM(e.ENTDCRPR))
        HAVING COUNT(*) = 1);


-- -------------------------------------------------------------------------------------
-- C. La bandera ENTDCPVR contra lo que de verdad hay en ENTDCRPR
-- -------------------------------------------------------------------------------------
SELECT NVL(TO_CHAR(e.ENTDCPVR), '(nulo)')                                AS bandera_cpvr,
       CASE WHEN TRIM(e.ENTDCRPR) IS NULL THEN 'vacio'
            WHEN REGEXP_LIKE(TRIM(e.ENTDCRPR), '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
                 THEN 'forma de correo'
            ELSE 'otra cosa' END                                         AS contenido,
       COUNT(*)                                                          AS entidades
  FROM CRD.ENTD e
 GROUP BY NVL(TO_CHAR(e.ENTDCPVR), '(nulo)'),
          CASE WHEN TRIM(e.ENTDCRPR) IS NULL THEN 'vacio'
               WHEN REGEXP_LIKE(TRIM(e.ENTDCRPR), '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
                    THEN 'forma de correo'
               ELSE 'otra cosa' END
 ORDER BY 1, 2;

-- C2. Ejemplos de "otra cosa" (los 20 primeros), para ver que basura hay
SELECT e.ENTDCDGO, e.ENTDCRPR
  FROM CRD.ENTD e
 WHERE TRIM(e.ENTDCRPR) IS NOT NULL
   AND NOT REGEXP_LIKE(TRIM(e.ENTDCRPR), '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
 FETCH FIRST 20 ROWS ONLY;


-- -------------------------------------------------------------------------------------
-- D. Participes con cuenta en la app y correo usable
-- -------------------------------------------------------------------------------------
SELECT COUNT(*)                                                          AS con_cuenta_app,
       COUNT(CASE WHEN REGEXP_LIKE(TRIM(e.ENTDCRPR),
                       '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
                  THEN 1 END)                                            AS de_esos_con_correo
  FROM CRD.USAP u
  JOIN CRD.ENTD e ON e.ENTDCDGO = u.ENTDCDGO;
