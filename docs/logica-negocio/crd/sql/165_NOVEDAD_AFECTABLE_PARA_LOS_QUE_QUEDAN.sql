-- =====================================================================================
-- ⛔ URGENTE — novedad AFECTABLE para los que bloquean y no tienen ninguna
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ ESCRIBE (INSERT en CRD.NVPC). Controles antes y despues, reverso al final.
--    No toca ninguna fila existente.
--
-- POR QUE HACE FALTA UN SEGUNDO SCRIPT DESPUES DEL 163:
--   El 163 genero novedades para los participes que NO TENIAN NINGUNA fila NVPC.
--   Pero quedaron afuera los que SI tienen una — y cuya unica novedad NO ES AFECTABLE.
--
--   Medido con el 164 sobre los que siguen bloqueando la carga 449:
--
--     Rol 4885 SOLANO MIJAS   registro de $389,22, PXCANVCA = 4
--                             su unica novedad: 44062, tipo 17 (DIFERENCIA MENOR A UN
--                             DOLAR), diferencia -0,19 -> INFORMATIVA -> "Sin accion"
--     Rol 10228 PALACIOS      registro de $46,36, PXCANVCA = 4
--                             su unica novedad: 43914, tipo 13, diferencia -4,50
--                             -> COBRANZA
--
--   Las dos novedades estan BIEN clasificadas: son negativas y no deben bloquear. El
--   bloqueo viene del campo PLANO PXCANVCA = 4, que la pantalla no muestra. Resultado:
--   el proceso los rechaza y el operador no tiene NADA que pueda afectar — la pantalla
--   le dice literalmente "Sin accion".
--
--   Este script les crea la novedad que falta, con el mismo criterio del 163.
--
-- ⚠️ ES UN PARCHE DE DATOS DE ESTA CARGA. La correccion de fondo (que el tipo 4 no
--    bloquee en Petrocomercial) esta despachada y necesita WAR. Con ese WAR, estos
--    participes dejan de bloquear solos y este script no vuelve a hacer falta.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220

DEFINE CARGA = 449


-- =====================================================================================
-- CONTROL 0 — ANTES DE EJECUTAR
--
-- 0.1 A quienes se les va a crear la novedad: los que bloquean por campo plano, tienen
--     descuento sin cubrir, y NO tienen ninguna novedad BLOQUEANTE donde afectar.
--     Esperado: las filas de SOLANO (389,22) y PALACIOS (46,36). Revisar la lista.
-- =====================================================================================
SELECT  p.PXCACDGO                                      AS ID_PXCA,
        p.PXCACDPT                                      AS ROL,
        SUBSTR(p.PXCANMBR, 1, 30)                       AS PARTICIPE,
        p.PXCADSDO                                      AS DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA_PLANA,
        (SELECT COUNT(*) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO)                AS NOVEDADES_QUE_TIENE,
        NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0)        AS AFECTADO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0) < p.PXCADSDO - 1
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n
                     WHERE n.PXCACDGO = p.PXCACDGO
                       AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                       AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0))
ORDER   BY p.PXCADSDO DESC;


-- =====================================================================================
-- EJECUCION
--
-- Misma forma que el 163: esperado = 0, recibido = totalDescontado, de modo que la
-- diferencia quede positiva e igual al total. Asi la novedad sale BLOQUEANTE, la
-- pantalla la muestra con accion, y el monto que ofrece el dialogo es exactamente el
-- que validarValoresConDestino exige para dejar procesar.
-- =====================================================================================

INSERT INTO CRD.NVPC (PXCACDGO, NVPCTPNV, NVPCDSCR, NVPCMNES, NVPCMNRC, NVPCMNDF,
                      NVPCCDCA, NVPCESTD)
SELECT  p.PXCACDGO,
        p.PXCANVCA,
        CASE p.PXCANVCA
            WHEN 1  THEN 'PARTICIPE NO ENCONTRADO'
            WHEN 2  THEN 'CODIGO ROL DUPLICADO'
            WHEN 3  THEN 'NOMBRE DE ENTIDAD DUPLICADO'
            WHEN 4  THEN 'CÓDIGO PETRO NO COINCIDE CON EL NOMBRE'
            ELSE 'NOVEDAD ESTRUCTURAL ' || TO_CHAR(p.PXCANVCA)
        END || ' (novedad generada para permitir la afectación manual, 2026-09-02).',
        0,
        p.PXCADSDO,
        p.PXCADSDO,
        &CARGA,
        1
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0) < p.PXCADSDO - 1
AND     NOT EXISTS (SELECT 1 FROM CRD.NVPC n
                     WHERE n.PXCACDGO = p.PXCACDGO
                       AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                       AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0));

COMMIT;


-- =====================================================================================
-- CONTROL 1 — DESPUES DE EJECUTAR
--
-- 1.1 Las novedades creadas ahora. Esperado: las de SOLANO y PALACIOS.
-- =====================================================================================
SELECT  n.NVPCCDGO                                      AS ID_NOVEDAD,
        p.PXCACDPT                                      AS ROL,
        SUBSTR(p.PXCANMBR, 1, 30)                       AS PARTICIPE,
        n.NVPCTPNV                                      AS TIPO,
        n.NVPCMNRC                                      AS A_REPARTIR
FROM    CRD.NVPC n
JOIN    CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
WHERE   n.NVPCCDCA = &CARGA
AND     n.NVPCDSCR LIKE '%generada para permitir%'
ORDER   BY n.NVPCCDGO DESC
FETCH FIRST 10 ROWS ONLY;

-- 1.2 ⛔ EL CONTROL QUE IMPORTA: lo que le queda por repartir al operador para que la
--     carga procese. Cada fila es un participe que hay que afectar en pantalla por su
--     total. Esperado ahora: SOLANO ($389,22) y PALACIOS ($46,36), y nadie mas.
SELECT  p.PXCACDPT                                      AS ROL,
        SUBSTR(p.PXCANMBR, 1, 30)                       AS PARTICIPE,
        p.PXCADSDO                                      AS A_REPARTIR,
        (SELECT COUNT(*) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO
            AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
            AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0))  AS NOVEDADES_AFECTABLES
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
AND     NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0) < p.PXCADSDO - 1
ORDER   BY p.PXCADSDO DESC;

-- ⚠️ NOVEDADES_AFECTABLES tiene que dar >= 1 en TODAS las filas. Si alguna da 0,
--    ese participe sigue bloqueando sin tener donde afectar: AVISAR.


-- =====================================================================================
-- REVERSO — COMENTADO. Igual que el 163: mirar primero si ya hay afectaciones.
-- =====================================================================================
-- SELECT COUNT(*) FROM CRD.AVPC a
--  JOIN CRD.NVPC n ON n.NVPCCDGO = a.NVPCCDGO
--  WHERE n.NVPCCDCA = &CARGA AND n.NVPCDSCR LIKE '%generada para permitir%';
--
-- DELETE FROM CRD.NVPC
--  WHERE NVPCCDCA = &CARGA AND NVPCDSCR LIKE '%generada para permitir%';
-- COMMIT;
