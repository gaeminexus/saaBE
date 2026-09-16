-- =====================================================================================
-- 227 — CORRIGE RPR.CPRM.CPRMSTEN: el nombre del estado del partícipe
-- FECHA: 2026-09-16   EQUIPO: omen-saa-1
--
-- ⛔ ESTE SCRIPT ESCRIBE. Es un UPDATE sobre RPR.CPRM. Tiene control ANTES, control DESPUÉS,
--    COMMIT explícito y el reverso comentado al final. Se corre de corrido.
--
-- QUÉ CORRIGE: la columna «Estado» de Reportes → Créditos → Informes Mensuales, pestaña
-- Partícipes, muestra «Estado 1», «Estado 3»… en vez de ACTIVO, JUBILADO COMPLEMENTARIO, etc.
-- Causa (docs/logica-negocio/reportes/CORRECCION-ESTADO-PARTICIPE-CPRM.md): la generación busca
-- el nombre por la PK del catálogo (ESPRCDGO) mientras CRD.ENTD.ENTDIDST guarda el código
-- alterno (ESPRCDEX) desde la migración del 2026-08-11. El único estado que sale bien es
-- CESANTE, porque su PK y su alterno valen 2.
--
-- ⚠️ SOLO toca el TEXTO del estado. Ningún monto, ninguna otra columna, ninguna otra tabla.
-- ⚠️ El nombre que deja es el estado que el partícipe tiene HOY: CPRM no guarda el código del
--    estado y CRD.ENTD sólo conserva el vigente. Para las filas que hoy dicen «Estado N» es una
--    mejora (hoy no dicen nada), pero tenerlo presente antes de correrlo sobre meses ya
--    entregados a la Superintendencia.
-- ⚠️ El arreglo del código va aparte y es el que evita que vuelva a pasar en la próxima
--    generación. Este script no lo reemplaza.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- CONTROL ANTES — cuántas filas están mal, por período y por estado
-- =====================================================================================
SELECT  j.EJCCANOO          AS anio,
        j.EJCCMESS          AS mes,
        c.CPRMSTEN          AS estado_actual,
        COUNT(*)            AS filas
FROM    RPR.CPRM c
JOIN    RPR.EJCC j       ON j.EJCCCDGO = c.CPRMEJCC
GROUP BY j.EJCCANOO, j.EJCCMESS, c.CPRMSTEN
ORDER BY 1, 2, 3;

-- Las que este script va a tocar: las que quedaron con el texto de respaldo
SELECT  COUNT(*)            AS filas_a_corregir
FROM    RPR.CPRM c
WHERE   c.CPRMSTEN LIKE 'Estado %';

-- Y de esas, cuántas se pueden resolver contra el catálogo por código alterno
SELECT  COUNT(*)            AS filas_resolubles
FROM    RPR.CPRM c
JOIN    CRD.ENTD e       ON e.ENTDCDGO = c.ENTDCDGO
JOIN    CRD.ESPR p       ON p.ESPRCDEX = e.ENTDIDST
WHERE   c.CPRMSTEN LIKE 'Estado %';


-- =====================================================================================
-- CORRECCIÓN
--
-- Sólo las filas con el texto de respaldo, y sólo si el estado del partícipe resuelve
-- contra CRD.ESPR por el código alterno. Una fila sin equivalencia se queda como está: es
-- preferible a dejarla con un nombre inventado.
-- =====================================================================================
UPDATE  RPR.CPRM c
SET     c.CPRMSTEN = (SELECT p.ESPRNMBR
                      FROM   CRD.ENTD e
                             JOIN CRD.ESPR p ON p.ESPRCDEX = e.ENTDIDST
                      WHERE  e.ENTDCDGO = c.ENTDCDGO)
WHERE   c.CPRMSTEN LIKE 'Estado %'
AND     EXISTS       (SELECT 1
                      FROM   CRD.ENTD e
                             JOIN CRD.ESPR p ON p.ESPRCDEX = e.ENTDIDST
                      WHERE  e.ENTDCDGO = c.ENTDCDGO);


-- =====================================================================================
-- CONTROL DESPUÉS — no debe quedar ninguna fila con «Estado N» que fuera resoluble
-- =====================================================================================
SELECT  j.EJCCANOO          AS anio,
        j.EJCCMESS          AS mes,
        c.CPRMSTEN          AS estado_resultante,
        COUNT(*)            AS filas
FROM    RPR.CPRM c
JOIN    RPR.EJCC j       ON j.EJCCCDGO = c.CPRMEJCC
GROUP BY j.EJCCANOO, j.EJCCMESS, c.CPRMSTEN
ORDER BY 1, 2, 3;

SELECT  COUNT(*)            AS filas_sin_corregir
FROM    RPR.CPRM c
WHERE   c.CPRMSTEN LIKE 'Estado %';


COMMIT;


-- =====================================================================================
-- REVERSO — comentado a propósito. Devuelve el texto de respaldo a las filas corregidas.
-- Sólo tiene sentido ANTES del COMMIT de arriba, o para volver atrás a mano.
-- =====================================================================================
-- ROLLBACK;
--
-- UPDATE  RPR.CPRM c
-- SET     c.CPRMSTEN = 'Estado ' || (SELECT e.ENTDIDST
--                                    FROM   CRD.ENTD e
--                                    WHERE  e.ENTDCDGO = c.ENTDCDGO)
-- WHERE   c.CPRMSTEN IN (SELECT p.ESPRNMBR FROM CRD.ESPR p);
-- COMMIT;
