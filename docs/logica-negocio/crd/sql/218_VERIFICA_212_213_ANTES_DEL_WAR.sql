-- =====================================================================================
-- ⛔ CONTROL OBLIGATORIO ANTES DE SUBIR EL WAR — ¿corrieron de verdad el 212 y el 213?
-- FECHA: 2026-09-08   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 218 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cinco bloques son SELECT. Tarda segundos.
--
-- =====================================================================================
-- POR QUE EXISTE ESTE SCRIPT
--
--   Llego por el equipo de tesoreria un mensaje de un usuario diciendo "ya se corrieron esos
--   scripts en la base, pueden desplegar tranquilos". **Eso no se puede tomar como verificacion**,
--   por tres razones, y ninguna es desconfianza:
--
--     1. No consta en que AMBIENTE se corrieron.
--     2. No consta si corrieron los DOS (el 212 crea la tabla, el 213 la siembra) ni si el 213
--        sembro todos los periodos que tenia que sembrar.
--     3. El 213 es el que impide **pagarle dos veces al proveedor del seguro medico de agosto**.
--        Esa plata sale al banco y NO tiene anulacion. Un dato asi se confirma contra la base,
--        no contra una frase.
--
--   Este script lo confirma en un minuto y contra la base que importa: la que va a levantar el WAR.
--
-- =====================================================================================
-- COMO LEERLO — LOS TRES SEMAFOROS
--
--   BLOQUE 1 → la tabla existe con sus 16 columnas y su indice unico EN EL SCHEMA CRD.
--   BLOQUE 2 → hay una cabecera por cada periodo ya procesado, con los DOS estados en 1.
--   BLOQUE 3 → ⭐ EL QUE DECIDE: cero periodos de PGPC sin cabecera. Si devuelve UNA sola fila,
--              ⛔ NO SE DESPLIEGA: ese periodo queda "pendiente" y se puede volver a pagar.
--
--   Si los tres dan lo esperado → los scripts corrieron bien y el WAR puede subir.
--   Si alguno no → PARAR y avisar. No se despliega y se corre lo que falte.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — ¿EN QUE BASE ESTOY? Para que quede escrito en la misma salida que el resto y
-- nadie confunda despues un control corrido en el ambiente equivocado.
-- =====================================================================================

SELECT SYS_CONTEXT('USERENV', 'DB_NAME')        AS BASE,
       SYS_CONTEXT('USERENV', 'CON_NAME')       AS CONTENEDOR,
       SYS_CONTEXT('USERENV', 'SERVER_HOST')    AS SERVIDOR,
       SYS_CONTEXT('USERENV', 'SESSION_USER')   AS USUARIO,
       TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI')   AS AHORA
  FROM DUAL;


-- =====================================================================================
-- BLOQUE 1 — ¿CORRIO EL 212? La tabla, sus columnas y su indice unico.
--
-- Esperado 1.1: TABLA_EXISTE = 1 y COLUMNAS = 16.
-- Esperado 1.2: dos filas — PK_CRJB (UNIQUE) y UQ_CRJB_PERIODO (UNIQUE), las dos con OWNER CRD.
--   ⚠️ Si UQ_CRJB_PERIODO no aparece, el indice quedo en el schema de la sesion y NO en CRD.
--      Ya paso en este proyecto. Sin ese indice la idempotencia del proceso no esta garantizada
--      por la base y dos usuarios simultaneos pueden crear dos cabeceras del mismo mes.
-- =====================================================================================

-- 1.1 La tabla y cuantas columnas tiene
SELECT (SELECT COUNT(*) FROM all_tables
         WHERE owner = 'CRD' AND table_name = 'CRJB')        AS TABLA_EXISTE,
       (SELECT COUNT(*) FROM all_tab_columns
         WHERE owner = 'CRD' AND table_name = 'CRJB')        AS COLUMNAS
  FROM DUAL;

-- 1.2 PK e indice unico, y en que schema quedaron
SELECT index_name, uniqueness, owner AS OWNER_INDICE, table_owner, status
  FROM all_indexes
 WHERE table_owner = 'CRD' AND table_name = 'CRJB'
 ORDER BY index_name;


-- =====================================================================================
-- BLOQUE 2 — ¿CORRIO EL 213? Las cabeceras sembradas.
--
-- Esperado: una fila por cada periodo ya procesado (agosto 2026 y lo anterior que haya), con
-- EST_SEGURO = 1 y EST_PENSION = 1, y ORIGEN = 'sembrada (esquema anterior)'.
--
-- ⛔ Si aparece SEPTIEMBRE 2026 o posterior, PARAR y avisar: ese periodo es del esquema nuevo
--    y no debia sembrarse. Sembrado, bloquearia un proceso que todavia no se corrio.
-- =====================================================================================

SELECT c.CRJBCDGO, c.PJRQCDGO AS EMPRESA, c.CRJBANNO AS ANIO, c.CRJBMESS AS MES,
       c.CRJBESSG AS EST_SEGURO,  c.CRJBVLSG AS TOTAL_SEGURO,  c.CRJBCTSG AS JUB_SEGURO,
       c.CRJBESPN AS EST_PENSION, c.CRJBVLPN AS TOTAL_PENSION, c.CRJBCTPN AS JUB_PENSION,
       CASE WHEN c.CRJBIDSG IS NULL AND c.CRJBVLCR IS NULL
            THEN 'sembrada (esquema anterior)' ELSE 'corrida con el esquema nuevo' END AS ORIGEN,
       CASE WHEN c.CRJBANNO > 2026 OR (c.CRJBANNO = 2026 AND c.CRJBMESS >= 9)
            THEN '⛔ PERIODO DEL ESQUEMA NUEVO — NO DEBIA SEMBRARSE' ELSE '' END AS ALERTA
  FROM CRD.CRJB c
 ORDER BY c.CRJBANNO, c.CRJBMESS;


-- =====================================================================================
-- BLOQUE 3 — ⭐ EL QUE DECIDE SI SE DESPLIEGA O NO.
--
-- Periodos con pagos de jubilados en CRD.PGPC que NO tienen cabecera en CRJB.
--
-- ESPERADO: 0 FILAS.
--
-- ⛔ SI DEVUELVE AUNQUE SEA UNA FILA: **NO SE DESPLIEGA.** Ese periodo va a aparecer en la
--    pantalla nueva con los dos procesos en PENDIENTE, y la guarda que protege las pensiones
--    NO protege el seguro medico: alguien puede disparar el proceso de seguro de ese mes y
--    pagarle al proveedor POR SEGUNDA VEZ. Esa plata ya salio al banco y no tiene anulacion.
--    Se corre el 213 (es idempotente, no duplica nada) y se vuelve a controlar.
-- =====================================================================================

SELECT g.PGPCANNO                          AS ANIO,
       g.PGPCMESS                          AS MES,
       COUNT(*)                            AS JUBILADOS,
       ROUND(SUM(NVL(g.PGPCVLSG, 0)), 2)   AS TOTAL_SEGURO_YA_PAGADO,
       ROUND(SUM(NVL(g.PGPCVLPN, 0)), 2)   AS TOTAL_PENSION_YA_PAGADA,
       '⛔ SIN CABECERA — NO DESPLEGAR'   AS ESTADO
  FROM CRD.PGPC g
 WHERE NOT EXISTS (SELECT 1 FROM CRD.CRJB c
                    WHERE c.CRJBANNO = g.PGPCANNO
                      AND c.CRJBMESS = g.PGPCMESS)
 GROUP BY g.PGPCANNO, g.PGPCMESS
 ORDER BY g.PGPCANNO, g.PGPCMESS;


-- =====================================================================================
-- BLOQUE 4 — CUADRE FINO: que los totales sembrados coincidan con lo realmente pagado.
--
-- Que exista la cabecera no prueba que tenga los valores correctos. Compara CRJB contra PGPC.
-- Esperado: 0 filas. Diferencias de centavos tambien salen: se comparan importes redondeados.
-- =====================================================================================

SELECT p.ANIO, p.MES,
       p.JUBILADOS      AS PGPC_JUBILADOS,   c.CRJBCTSG AS CRJB_JUB_SEGURO,
       p.TOTAL_SEGURO   AS PGPC_SEGURO,      c.CRJBVLSG AS CRJB_SEGURO,
       p.TOTAL_PENSION  AS PGPC_PENSION,     c.CRJBVLPN AS CRJB_PENSION,
       'revisar' AS ESTADO
  FROM (SELECT g.PGPCANNO AS ANIO, g.PGPCMESS AS MES, COUNT(*) AS JUBILADOS,
               ROUND(SUM(NVL(g.PGPCVLSG, 0)), 2) AS TOTAL_SEGURO,
               ROUND(SUM(NVL(g.PGPCVLPN, 0)), 2) AS TOTAL_PENSION
          FROM CRD.PGPC g
         GROUP BY g.PGPCANNO, g.PGPCMESS) p
  JOIN CRD.CRJB c ON c.CRJBANNO = p.ANIO AND c.CRJBMESS = p.MES
 WHERE NVL(c.CRJBVLSG, -1) <> p.TOTAL_SEGURO
    OR NVL(c.CRJBVLPN, -1) <> p.TOTAL_PENSION
    OR NVL(c.CRJBCTSG, -1) <> p.JUBILADOS
 ORDER BY p.ANIO, p.MES;
