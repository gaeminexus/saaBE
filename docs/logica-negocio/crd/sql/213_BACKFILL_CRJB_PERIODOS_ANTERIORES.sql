-- =====================================================================================
-- BACKFILL de CRD.CRJB para los periodos YA PROCESADOS con el esquema anterior
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 213 (rango 200-249)
--
-- CONTRATO: docs/logica-negocio/crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md §8
--
-- ESCRIBE. Un INSERT por periodo ya corrido. Controles antes y despues; COMMIT y reverso
-- comentados.
--
-- ⛔ CORRER INMEDIATAMENTE DESPUES DEL sql/212 (que crea la tabla) Y ANTES DEL WAR.
--
-- =====================================================================================
-- POR QUE EXISTE — el riesgo es concreto, no teorico
--
--   Dato del usuario (2026-09-07): AGOSTO 2026 ya se proceso y se contabilizo con el esquema
--   actual. El esquema nuevo rige desde SEPTIEMBRE.
--
--   CRD.CRJB nace vacia. Sin este backfill, la pantalla de seguimiento mostraria agosto con
--   los dos procesos en PENDIENTE. Y la guarda del contrato (D2) protege las PENSIONES —exige
--   que el seguro este generado— pero NO protege el seguro.
--
--   ⇒ Alguien podria disparar el proceso de seguro medico de AGOSTO y pagarle al proveedor
--     POR SEGUNDA VEZ. Ese dinero ya salio al banco y no hay anulacion.
--
--   Sembrando la cabecera con los dos estados en 1, los endpoints nuevos rechazan agosto por
--   la MISMA guarda de "ya se genero" que usan para cualquier repeticion. La proteccion sale
--   del DATO y no de una regla de fecha minima que alguien tendria que mantener.
--
-- =====================================================================================
-- LO QUE NO SE PUEDE RECONSTRUIR, Y SE DEJA EN NULL A PROPOSITO
--   - CRJBIDSG (id de la orden al proveedor): PGPC es por jubilado y no guarda el id de la
--     orden agregada del periodo. Queda NULL. Se completa a mano si alguna vez hace falta.
--   - CRJBVLCR (total cruzado a prestamos): no se deriva de PGPC sin recorrer los pagos, y no
--     vale la pena para un historico.
--
--   ⭐ Esas dos ausencias son ademas la MARCA que distingue una fila sembrada de una corrida
--      hecha con el esquema nuevo: son las unicas con CRJBESSG = 1 y CRJBIDSG NULL.
--
-- ⚠️ LA EMPRESA: PGPC **no** guarda PJRQCDGO (tiene ENTDCDGO y FLLLCDGO). Este script usa la
--    empresa 1236 como literal, que es la unica con corridas de jubilados hasta hoy. El bloque
--    0 lista los periodos para confirmarlo ANTES de insertar. Si hubiera corridas de otra
--    empresa, PARAR: habria que decidir a cual pertenece cada periodo.
--
-- ⚠️ SECUENCIAS: CRJBCDGO es IDENTITY. No se insertan PK explicitas y no hay nada que
--    sincronizar.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los bloques 0 y 2.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES. SOLO LECTURA. **REVISAR ESTA SALIDA ANTES DE INSERTAR.**
-- Esperado: los periodos realmente corridos con el esquema viejo (agosto 2026, y lo que haya
-- antes). Si aparece SEPTIEMBRE 2026 o posterior, PARAR y avisar: ese ya seria del esquema
-- nuevo y no se debe sembrar.
-- =====================================================================================

SELECT g.PGPCANNO AS ANIO,
       g.PGPCMESS AS MES,
       COUNT(*)                              AS JUBILADOS,
       ROUND(SUM(NVL(g.PGPCVLPN, 0)), 2)     AS TOTAL_PENSION,
       ROUND(SUM(NVL(g.PGPCVLSG, 0)), 2)     AS TOTAL_SEGURO,
       MIN(g.PGPCFCRG)                       AS PRIMER_REGISTRO,
       MIN(g.PGPCUSRG)                       AS USUARIO
  FROM CRD.PGPC g
 GROUP BY g.PGPCANNO, g.PGPCMESS
 ORDER BY g.PGPCANNO, g.PGPCMESS;

-- 0.2 La tabla tiene que existir y estar vacia (el 212 corrio y este script no se corrio antes)
SELECT (SELECT COUNT(*) FROM all_tables WHERE owner='CRD' AND table_name='CRJB') AS TABLA_EXISTE,
       (SELECT COUNT(*) FROM CRD.CRJB) AS FILAS_ACTUALES
  FROM DUAL;


-- =====================================================================================
-- BLOQUE 1 — EL BACKFILL
-- Una fila por periodo, los dos estados en 1 (GENERADO). El NOT EXISTS lo hace idempotente:
-- correrlo dos veces no duplica nada, y el indice unico del 212 lo impediria igual.
-- =====================================================================================

INSERT INTO CRD.CRJB (PJRQCDGO, CRJBANNO, CRJBMESS,
                      CRJBESSG, CRJBFCSG, CRJBUSSG, CRJBVLSG, CRJBIDSG, CRJBCTSG,
                      CRJBESPN, CRJBFCPN, CRJBUSPN, CRJBVLPN, CRJBVLCR, CRJBCTPN)
SELECT 1236,
       p.ANIO,
       p.MES,
       1, p.FECHA, p.USUARIO, p.TOTAL_SEGURO,  NULL, p.JUBILADOS,
       1, p.FECHA, p.USUARIO, p.TOTAL_PENSION, NULL, p.JUBILADOS
  FROM (SELECT g.PGPCANNO AS ANIO,
               g.PGPCMESS AS MES,
               COUNT(*) AS JUBILADOS,
               ROUND(SUM(NVL(g.PGPCVLPN, 0)), 2) AS TOTAL_PENSION,
               ROUND(SUM(NVL(g.PGPCVLSG, 0)), 2) AS TOTAL_SEGURO,
               MIN(g.PGPCFCRG) AS FECHA,
               MIN(g.PGPCUSRG) AS USUARIO
          FROM CRD.PGPC g
         GROUP BY g.PGPCANNO, g.PGPCMESS) p
 WHERE NOT EXISTS (SELECT 1 FROM CRD.CRJB c
                    WHERE c.PJRQCDGO = 1236
                      AND c.CRJBANNO = p.ANIO
                      AND c.CRJBMESS = p.MES);


-- =====================================================================================
-- BLOQUE 2 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado 2.1: una fila por cada periodo del bloque 0, con los dos estados en 1 y los totales
--               coincidiendo.
-- Esperado 2.2: 0 filas — ningun periodo de PGPC quedo sin su cabecera.
-- Si no da eso: ROLLBACK y avisar.
-- =====================================================================================

-- 2.1 Como quedaron las cabeceras sembradas
SELECT c.CRJBCDGO, c.PJRQCDGO AS EMPRESA, c.CRJBANNO AS ANIO, c.CRJBMESS AS MES,
       c.CRJBESSG AS EST_SEGURO, c.CRJBVLSG AS TOTAL_SEGURO, c.CRJBCTSG AS JUB_SEGURO,
       c.CRJBESPN AS EST_PENSION, c.CRJBVLPN AS TOTAL_PENSION, c.CRJBCTPN AS JUB_PENSION,
       c.CRJBUSSG AS USUARIO, c.CRJBFCSG AS FECHA,
       CASE WHEN c.CRJBIDSG IS NULL AND c.CRJBVLCR IS NULL
            THEN 'sembrada (esquema anterior)' ELSE 'corrida con el esquema nuevo' END AS ORIGEN
  FROM CRD.CRJB c
 ORDER BY c.CRJBANNO, c.CRJBMESS;

-- 2.2 Ningun periodo de PGPC debe quedar sin cabecera
SELECT g.PGPCANNO AS ANIO, g.PGPCMESS AS MES, COUNT(*) AS JUBILADOS_SIN_CABECERA
  FROM CRD.PGPC g
 WHERE NOT EXISTS (SELECT 1 FROM CRD.CRJB c
                    WHERE c.CRJBANNO = g.PGPCANNO AND c.CRJBMESS = g.PGPCMESS)
 GROUP BY g.PGPCANNO, g.PGPCMESS;


-- =====================================================================================
-- BLOQUE 3 — CONFIRMAR
-- Descomentar recien despues de leer el BLOQUE 2.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 4 — REVERSO (comentado)
-- Borra SOLO las filas sembradas por este script (las que quedaron sin id de orden y sin
-- cruzado a prestamos). Una corrida hecha con el esquema nuevo NO se toca.
-- =====================================================================================

-- DELETE FROM CRD.CRJB WHERE CRJBIDSG IS NULL AND CRJBVLCR IS NULL AND CRJBESSG = 1;
-- COMMIT;
