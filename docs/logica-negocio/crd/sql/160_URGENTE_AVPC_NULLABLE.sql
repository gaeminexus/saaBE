-- =====================================================================================
-- ⛔ URGENTE — CRD.AVPC: PRSTCDGO y DTPRCDGO tienen que aceptar NULL
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT ESCRIBE (dos ALTER TABLE MODIFY). Controles antes y despues, y reverso
--    al final. NO borra datos, NO modifica ninguna fila: solo relaja dos restricciones.
--
-- EL ERROR QUE CORRIGE, visto en produccion el 2026-09-02 al mandar el excedente de una
-- novedad SOLO a un aporte:
--
--     ORA-01400: no se puede realizar una insercion NULL en ("CRD"."AVPC"."PRSTCDGO")
--
-- LA CAUSA, y es una implementacion que quedo a medias:
--
--   CRD.AVPC se creo cuando TODA afectacion iba contra una cuota de un prestamo, con
--        PRSTCDGO NUMBER NOT NULL
--        DTPRCDGO NUMBER NOT NULL
--
--   El script 87 (EXCEDENTE_PETRO_A_APORTES) agrego despues TPAPCDGO para poder afectar
--   a un APORTE, y diseño el caso correctamente — su propio comentario lo dice:
--
--        "De aporte: TPAPCDGO presente, PRSTCDGO/DTPRCDGO NULL, SIN desglose"
--
--   y hasta creo el CHECK que lo formaliza:
--
--        CK_AVPC_PRST_XOR_TPAP CHECK (
--            (PRSTCDGO IS NOT NULL AND DTPRCDGO IS NOT NULL AND TPAPCDGO IS NULL)
--         OR (PRSTCDGO IS NULL     AND DTPRCDGO IS NULL     AND TPAPCDGO IS NOT NULL))
--
--   ⛔ PERO NUNCA QUITO LAS DOS NOT NULL. El CHECK permite el NULL; la columna lo
--      rechaza. Las dos reglas se contradicen, y la de la columna gana: la segunda rama
--      del CHECK —la de aporte— era IMPOSIBLE de satisfacer desde el dia uno.
--
--   Por eso esto no se detecto antes: el CHECK esta bien, el codigo esta bien, y el
--   diseño esta bien documentado. Lo unico que falta son dos MODIFY.
--
-- ESTE SCRIPT NO CAMBIA EL DISEÑO: lo termina de aplicar. El CHECK sigue garantizando
-- que una fila sea de prestamo O de aporte, nunca de las dos ni de ninguna.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- CONTROL 0 — ANTES DE EJECUTAR
-- =====================================================================================

-- 0.1 Estado actual de nulabilidad. Esperado: las dos con NULLABLE = 'N' (por eso falla).
--     Si ya salen 'Y', el ALTER no hace falta y el error es OTRO: PARAR Y AVISAR.
SELECT  c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND     c.COLUMN_NAME IN ('PRSTCDGO','DTPRCDGO','TPAPCDGO')
ORDER   BY c.COLUMN_NAME;

-- 0.2 El CHECK del script 87 tiene que existir y estar habilitado: es lo que sigue
--     protegiendo la coherencia cuando las columnas acepten NULL.
--     Esperado: 1 fila, ENABLED / VALIDATED.
--     ⚠️ Si NO existe, PARAR: sin ese CHECK, relajar las NOT NULL dejaria insertar filas
--        sin prestamo Y sin aporte, que no significan nada.
SELECT  c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.VALIDATED, c.SEARCH_CONDITION
FROM    ALL_CONSTRAINTS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND     c.CONSTRAINT_NAME = 'CK_AVPC_PRST_XOR_TPAP';

-- 0.3 Cuantas filas hay hoy. Solo para tenerlo antes/despues: este script NO toca filas.
SELECT  COUNT(*)                                                  AS FILAS_TOTALES,
        SUM(CASE WHEN PRSTCDGO IS NOT NULL THEN 1 ELSE 0 END)     AS DE_PRESTAMO,
        SUM(CASE WHEN TPAPCDGO IS NOT NULL THEN 1 ELSE 0 END)     AS DE_APORTE
FROM    CRD.AVPC;


-- =====================================================================================
-- EJECUCION — dos ALTER, nada mas
--
-- MODIFY ... NULL solo relaja la restriccion. No reescribe la tabla, no toca datos, y es
-- inmediato incluso con la tabla poblada.
-- =====================================================================================

ALTER TABLE CRD.AVPC MODIFY (PRSTCDGO NULL);

ALTER TABLE CRD.AVPC MODIFY (DTPRCDGO NULL);

COMMENT ON COLUMN CRD.AVPC.PRSTCDGO IS
    'Prestamo afectado. NULL cuando la afectacion va a un aporte (ver CK_AVPC_PRST_XOR_TPAP). Se relajo el NOT NULL el 2026-09-02: el script 87 habilito la afectacion a aporte pero dejo la columna obligatoria, y la rama de aporte del CHECK era imposible de satisfacer.';

COMMENT ON COLUMN CRD.AVPC.DTPRCDGO IS
    'Cuota afectada. NULL cuando la afectacion va a un aporte (ver CK_AVPC_PRST_XOR_TPAP). Mismo motivo que PRSTCDGO.';

COMMIT;


-- =====================================================================================
-- CONTROL 1 — DESPUES DE EJECUTAR
-- =====================================================================================

-- 1.1 Las dos tienen que quedar NULLABLE = 'Y'. TPAPCDGO ya era 'Y' y no se toca.
SELECT  c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND     c.COLUMN_NAME IN ('PRSTCDGO','DTPRCDGO','TPAPCDGO')
ORDER   BY c.COLUMN_NAME;

-- 1.2 El CHECK tiene que seguir ENABLED / VALIDATED. Es lo que ahora hace todo el
--     trabajo de coherencia. Esperado: la misma fila del control 0.2.
SELECT  c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS, c.VALIDATED
FROM    ALL_CONSTRAINTS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND     c.CONSTRAINT_NAME = 'CK_AVPC_PRST_XOR_TPAP';

-- 1.3 Las dos FK a PRST y DTPR tienen que seguir intactas. Relajar un NOT NULL no las
--     toca (una FK con valor NULL simplemente no se verifica), pero se controla igual.
--     Esperado: 2 filas, las dos ENABLED.
SELECT  c.CONSTRAINT_NAME, c.STATUS, c.VALIDATED
FROM    ALL_CONSTRAINTS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME = 'AVPC'
AND     c.CONSTRAINT_TYPE = 'R'
ORDER   BY c.CONSTRAINT_NAME;


-- =====================================================================================
-- PRUEBA FUNCIONAL — no la hace este script, la hace el usuario en pantalla
--
-- Volver a mandar el excedente de una novedad SOLO a un aporte. Tiene que grabar sin
-- ORA-01400. No hace falta desplegar nada: es solo base de datos, el codigo ya estaba
-- bien.
-- =====================================================================================


-- =====================================================================================
-- REVERSO — COMENTADO
--
-- ⛔ OJO: volver a NOT NULL FALLA si ya se grabo alguna afectacion a aporte (esas filas
--    tienen PRSTCDGO NULL). Habria que borrarlas primero, y eso SI es perdida de datos.
--    Antes de revertir, correr el control 0.3 y mirar DE_APORTE: si es > 0, el reverso
--    no es una operacion segura y hay que decidirlo con el usuario.
-- =====================================================================================
--
-- ALTER TABLE CRD.AVPC MODIFY (PRSTCDGO NOT NULL);
-- ALTER TABLE CRD.AVPC MODIFY (DTPRCDGO NOT NULL);
-- COMMIT;
