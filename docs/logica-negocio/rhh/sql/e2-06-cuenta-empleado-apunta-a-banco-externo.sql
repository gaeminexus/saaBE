-- =====================================================================
-- RHH.CBEM: la cuenta del empleado debe apuntar a BANCO EXTERNO
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-03
--
-- EL PROBLEMA
--   RHH.CBEM.BNCOCDGO apunta a TSR.BNCO — los bancos INTERNOS, o sea aquellos
--   donde la EMPRESA tiene cuenta. Esa tabla tiene un punado de filas y hasta
--   una bandera de conciliacion de descuadres: es el catalogo de la tesoreria
--   propia.
--
--   Pero un empleado cobra en SU banco, que puede ser cualquiera del sistema
--   financiero. Ese catalogo es TSR.BEXT, con 389 bancos activos (medido por el
--   usuario el 2026-09-03).
--
--   El sintoma con el que aparecio: el usuario no encontraba su banco en el
--   combo y lo reporto como «falta un buscador». El buscador ya estaba — lo que
--   faltaba era el banco, porque la lista era la equivocada.
--
-- POR QUE SE PUEDE HACER LIMPIO
--   RHH.CBEM esta VACIA. Verificado por el usuario el 2026-09-03: la consulta
--   de cuentas agrupadas por banco devolvio CERO filas. No hay migracion de
--   datos, no hay que mapear nombres, no hay riesgo de perder nada.
--
--   ⚠️ El control 0.1 vuelve a comprobarlo justo antes de ejecutar. Si para
--   entonces alguien cargo una cuenta, PARAR: este script la dejaria sin banco.
--
-- POR QUE NO ROMPE EL ARCHIVO BANCARIO DE LA NOMINA
--   GeneracionOrdenPagoServiceImpl:864 guarda el NOMBRE del banco como texto en
--   RHH.DRPG.DRPGBNCO, no el codigo: es un snapshot. Y solo hay DOS usos de
--   cuenta.getBanco() en todo ejb/rhh, los dos en ese archivo. El cambio no
--   toca lo que se le manda al banco.
--
-- ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES DEL WAR que trae la entidad cambiada. Si el WAR sube
--   primero, CuentaBancariaEmpleado mapea BEXTCDGO y esa columna no existe:
--   ORA-00904 en toda lectura de la entidad.
--   ⛔ Y no se mergea a main el cambio de entidad hasta que esto este corrido
--      (registro de reservas, seccion 7 «No mergear a main un mapeo cuya columna no
--      esta en la base» — hay DOS secciones 7: citar por titulo).
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 ⚠️ EL CONTROL QUE NO SE SALTEA. La tabla debe seguir vacia.
--     ESPERADO: 0. Si devuelve cualquier otra cosa, PARAR Y AVISAR: el bloque 2
--     borra la columna y con ella el banco de esas cuentas.
SELECT COUNT(*) AS CUENTAS_EXISTENTES FROM RHH.CBEM;

-- 0.2 Como esta hoy la columna y su FK.
--     ESPERADO: BNCOCDGO presente, y una constraint R hacia BNCO.
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'CBEM'
   AND column_name IN ('BNCOCDGO','BEXTCDGO');

SELECT c.constraint_name, c.constraint_type, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'CBEM' AND c.constraint_type = 'R';

-- 0.3 Hay bancos externos activos para elegir. ESPERADO: 389 al 2026-09-03.
SELECT COUNT(*) AS BANCOS_EXTERNOS_ACTIVOS FROM TSR.BEXT WHERE BEXTESTD = 1;


-- =====================================================================
-- BLOQUE 1 -- QUITAR LA FK VIEJA
-- =====================================================================
-- El nombre real de la constraint sale del control 0.2. El de abajo sigue la
-- convencion de la casa; si el control devolvio otro nombre, usar ese.

ALTER TABLE RHH.CBEM DROP CONSTRAINT FK_CBEM_BNCO;


-- =====================================================================
-- BLOQUE 2 -- REEMPLAZAR LA COLUMNA
-- =====================================================================
-- Se BORRA la vieja en vez de dejarla: con la tabla vacia no aporta nada, y
-- dejarla seria una columna muerta que el proximo lector tendria que descartar.

ALTER TABLE RHH.CBEM DROP COLUMN BNCOCDGO;

ALTER TABLE RHH.CBEM ADD (BEXTCDGO NUMBER NULL);

COMMENT ON COLUMN RHH.CBEM.BEXTCDGO IS
    'Banco del empleado: TSR.BEXT, catalogo del sistema financiero. NO es TSR.BNCO, que son los bancos donde la EMPRESA tiene cuenta';


-- =====================================================================
-- BLOQUE 3 -- LA FK NUEVA
-- =====================================================================
-- BEXT vive en TSR, asi que hace falta el GRANT REFERENCES ANTES del ALTER.
-- Si el usuario que ejecuta no es TSR, pedirselo al DBA.
-- GRANT REFERENCES ON TSR.BEXT TO RHH;

ALTER TABLE RHH.CBEM ADD CONSTRAINT FK_CBEM_BEXT
    FOREIGN KEY (BEXTCDGO) REFERENCES TSR.BEXT (BEXTCDGO);

-- El prefijo de schema va tambien en el INDICE, no solo en la tabla. Es la
-- trampa que costo el e2-04: sin el, el indice queda en el schema del ejecutor.
CREATE INDEX RHH.IX_CBEM_BEXT ON RHH.CBEM (BEXTCDGO);


-- =====================================================================
-- BLOQUE 4 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 4.1 La columna nueva existe y la vieja no. ESPERADO: 1 fila, BEXTCDGO.
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'CBEM'
   AND column_name IN ('BNCOCDGO','BEXTCDGO');

-- 4.2 La FK apunta a BEXT y esta habilitada. ESPERADO: FK_CBEM_BEXT / ENABLED.
SELECT c.constraint_name, c.status, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'RHH' AND c.table_name = 'CBEM' AND c.constraint_type = 'R';

-- 4.3 El indice quedo en RHH y no en el schema del ejecutor.
SELECT owner, index_name FROM all_indexes
 WHERE table_name = 'CBEM' ORDER BY owner, index_name;

-- 4.4 La tabla sigue vacia y sana. ESPERADO: 0.
SELECT COUNT(*) AS CUENTAS FROM RHH.CBEM;


-- =====================================================================
-- BLOQUE 5 -- REVERSO. COMENTADO A PROPOSITO.
-- Solo sirve si NO se cargo ninguna cuenta todavia.
-- =====================================================================
--
-- SELECT COUNT(*) FROM RHH.CBEM;   -- debe dar 0 para poder revertir
--
-- DROP INDEX RHH.IX_CBEM_BEXT;
-- ALTER TABLE RHH.CBEM DROP CONSTRAINT FK_CBEM_BEXT;
-- ALTER TABLE RHH.CBEM DROP COLUMN BEXTCDGO;
-- ALTER TABLE RHH.CBEM ADD (BNCOCDGO NUMBER NULL);
-- ALTER TABLE RHH.CBEM ADD CONSTRAINT FK_CBEM_BNCO
--     FOREIGN KEY (BNCOCDGO) REFERENCES TSR.BNCO (BNCOCDGO);
-- =====================================================================
