-- =====================================================================================
-- DDL — DEVENGO DE APORTES + HISTORIAL DE VIGENCIAS DE CONTRATO
-- Implementa la fase 0 de docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md
-- FECHA: 2026-08-27
--
-- =====================================================================================
-- ESTADO: PROBADO EN LOCAL contra copia exacta de produccion el 2026-08-27.
-- Las correcciones encontradas en esa corrida YA ESTAN APLICADAS en este archivo.
-- =====================================================================================
--
-- GUIA DE EJECUCION — que se corre y que no:
--
--   BLOQUE  ACCION      NOTA
--   ------  ----------  ---------------------------------------------------------------
--   0       EJECUTAR    Solo SELECT. Leer los resultados antes de seguir.
--   1       EJECUTAR    ALTER CRD.APRT + CHECK + indice.               [OK en local]
--   2       EJECUTAR    CREATE CRD.VGCN + secuencia + FK + indices.    [OK en local]
--   3       PARCIAL     Solo el ALTER ... ADD. El MODIFY esta comentado.
--   4       SALTAR      Comentado entero.
--   4b      SALTAR      Comentado entero.
--   5       EJECUTAR    Rubros 235/236/237 con PK EXPLICITOS (PRBR 235-237,
--                       PDTR 1132-1140). NO usa secuencia. Terminar con el 5.5, que
--                       sincroniza las secuencias: si se omite, el proximo INSERT de la
--                       aplicacion falla con ORA-00001.
--   6       EJECUTAR    Grants.
--   7       EJECUTAR    Controles posteriores. 7.1 debe dar 4 filas.
--
-- CORRECCIONES YA APLICADAS (no volver a tropezar con ellas):
--
--   a) Bloque 3 y bloque 4, los MODIFY a NUMBER(5,2) / NUMBER(18,2):
--      daban ORA-01440 "column to be modified must be empty to decrease precision or
--      scale". CNTRPRAI, CNTRPRAJ y PRTCRMUN son NUMBER SIN precision, o sea que Oracle
--      YA GUARDA DECIMALES y bajarlas a (5,2) seria REDUCIR. No hay nada que cambiar en
--      la base. El unico cambio real es el tipo Java (Long -> Double) en las entidades,
--      y lo hace el agente de backend. Los dos MODIFY quedaron comentados.
--
--   b) Bloque 4b, marca de ultima actualizacion del participe (pedido 9):
--      CRD.ENTD YA TIENE fecha y usuario de modificacion. Lo que falta es que la entidad
--      JPA mapee la fecha — Entidad.java solo mapea ENTDIPMD y ENTDUSMD. No falta el
--      campo, falta el mapeo. El bloque quedo comentado: no se crea ninguna columna.
--
-- Contenido:
--   0. Controles PREVIOS (leer antes de ejecutar nada)
--   1. ALTER CRD.APRT  — periodo de devengo + tipo de movimiento
--   2. CREATE CRD.VGCN — vigencias del contrato de adhesion (monto por tipo de aporte)
--   3. ALTER CRD.CNTR  — espejo del monto vigente (el MODIFY va comentado)
--   4. ALTER CRD.PRTC  — COMENTADO, ver correccion (a)
--   4b. (pedido 9)     — COMENTADO, ver correccion (b)
--   5. Rubros 235/236/237 (tipo de movimiento, modo de vigencia, flag contable)
--   6. Grants
--   7. Controles POSTERIORES
--
-- EJECUCION MANUAL, como owner del esquema CRD (bloques 1-4) y SCP (bloque 5).
--
-- ORDEN RESPECTO DEL WAR: este DDL va ANTES del despliegue. Las entidades JPA nuevas
-- mapean estas columnas y el arranque de WildFly falla si no existen.
--
-- IDEMPOTENCIA: los ALTER ADD y el CREATE TABLE fallan si ya se ejecutaron. Es
-- deliberado: se prefiere el error a un script que "parece" haber corrido. Si hay que
-- repetir, revisar antes con los controles del bloque 0.
--
-- NOTA DE DISEÑO: CRD.VGCN es la FUENTE DE VERDAD del monto a cobrar. Las columnas
-- nuevas de CRD.CNTR son un ESPEJO de la vigencia abierta, para que las pantallas de
-- lista no tengan que resolver la vigencia en cada fila. Quien las mantiene es el
-- backend, en la misma transaccion que crea o cierra una vigencia. Ningun proceso de
-- negocio debe leer el espejo para calcular: se lee VGCN.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — ejecutar y leer ANTES de correr el resto
-- =====================================================================================

-- 0.1 Tipo REAL de las columnas que se van a modificar.
--     Las entidades JPA las mapean como Long, pero eso es el lado Java. Si en Oracle ya
--     son NUMBER sin precision, YA ADMITEN DECIMALES y los MODIFY de los bloques 3 y 4
--     son innecesarios: en ese caso solo cambia el tipo Java (Long -> Double), que hace
--     el agente de backend. Ejecutar esto primero y decidir.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE,
        c.DATA_PRECISION, c.DATA_SCALE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   (c.OWNER = 'CRD' AND c.TABLE_NAME = 'CNTR' AND c.COLUMN_NAME IN ('CNTRPRAI','CNTRPRAJ'))
   OR   (c.OWNER = 'CRD' AND c.TABLE_NAME = 'PRTC' AND c.COLUMN_NAME = 'PRTCRMUN')
ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

-- 0.2 Las columnas nuevas no deben existir todavia. Esperado: 0 filas.
SELECT  c.TABLE_NAME, c.COLUMN_NAME
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.COLUMN_NAME IN ('APRTPRDV','APRTTPMV','CNTRMNAJ','CNTRMNAC');

-- 0.2b ⚠ DECIDE SI EL BLOQUE 4b SE EJECUTA O NO (pedido 9).
--      El usuario reporta que CRD.ENTD ya tiene fecha y usuario de modificacion. En la
--      ENTIDAD JPA solo estan mapeados ENTDIPMD y ENTDUSMD: NO hay campo de fecha. Por
--      eso la pantalla nunca la actualiza — la columna existe en la base pero ninguna
--      linea de codigo la escribe.
--      Si esta consulta devuelve una columna de fecha de modificacion en ENTD:
--          NO EJECUTAR EL BLOQUE 4b. No hace falta DDL; solo mapearla en la entidad.
--          Anotar el NOMBRE EXACTO: el prompt de backend lo necesita.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD' AND c.TABLE_NAME IN ('ENTD','PRTC')
AND     (c.COLUMN_NAME LIKE '%FCMD' OR c.COLUMN_NAME LIKE '%USMD'
      OR c.COLUMN_NAME LIKE '%FCIN' OR c.COLUMN_NAME LIKE '%USIN')
ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

-- 0.3 CRD.VGCN no debe existir. Esperado: 0 filas.
SELECT t.OWNER, t.TABLE_NAME FROM ALL_TABLES t WHERE t.OWNER = 'CRD' AND t.TABLE_NAME = 'VGCN';
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_VGCNCDGO';

-- 0.4 Indices que ya existen sobre CRD.APRT, para no crear uno redundante.
SELECT  i.INDEX_NAME, i.UNIQUENESS,
        LISTAGG(c.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY c.COLUMN_POSITION) AS COLUMNAS
FROM    ALL_INDEXES i
JOIN    ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
WHERE   i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'APRT'
GROUP BY i.INDEX_NAME, i.UNIQUENESS
ORDER BY 1;

-- 0.5 Los alternos 235/236/237 deben estar libres. Esperado: 0 filas.
--     Si alguno esta ocupado, PARAR: hay que renumerar aqui y en com.saa.rubros.Rubros.
--
--     ⚠ ESTE CONTROL YA ATRAPO UN CHOQUE REAL (2026-08-27): la numeracion original era
--     231/232/233 y los alternos 232 y 233 YA ESTABAN OCUPADOS por rubros de TSR
--     ("Tipo de movimiento de caja chica" y "Estado del cierre de caja chica"). Se
--     renumero a 235/236/237 para que PRBRCDGO y PRBRALTR coincidan.
--
--     ⚠ com.saa.rubros.Rubros NO ES FUENTE CONFIABLE de que alternos estan libres: los
--     rubros de TSR que ocupaban 232 y 233 no tienen constante ahi. La autoridad es
--     esta consulta contra la base.
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR
FROM   SCP.PRBR r WHERE r.PRBRALTR IN (235, 236, 237);

-- 0.5a Los PK explicitos del bloque 5 deben estar libres. Esperado: 0 filas en ambas.
--      El bloque 5 inserta PRBR 235-237 y PDTR 1132-1140 con codigo explicito, no con
--      secuencia. Si alguno esta ocupado, PARAR y elegir otro rango.
SELECT 'PRBR' AS TABLA, r.PRBRCDGO AS CODIGO_OCUPADO, r.PRBRDSCR AS DESCRIPCION
FROM   SCP.PRBR r WHERE r.PRBRCDGO BETWEEN 235 AND 237
UNION ALL
SELECT 'PDTR', d.PDTRCDGO, d.PDTRDSCR
FROM   SCP.PDTR d WHERE d.PDTRCDGO BETWEEN 1132 AND 1140;

-- 0.5b Estado actual de las secuencias de rubros, para comparar contra el 5.5.
--      El bloque 5 inserta PK explicitos (235-237 y 1132-1140), asi que despues HAY QUE
--      sincronizar las secuencias o el proximo INSERT de la aplicacion dara ORA-00001.
--      Anotar estos valores: el 5.5 los vuelve a pedir.
SELECT  'PRBR' AS TABLA,
        (SELECT MAX(p.PRBRCDGO) FROM SCP.PRBR p)                       AS MAX_PK,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE_VALOR
FROM    DUAL
UNION ALL
SELECT  'PDTR',
        (SELECT MAX(d.PDTRCDGO) FROM SCP.PDTR d),
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- 0.6 Volumen de lo que se va a rellenar despues (solo informativo).
SELECT  COUNT(*)                                                        AS FILAS_APRT,
        SUM(CASE WHEN a.APRTFCTR >= DATE '2025-06-01' THEN 1 ELSE 0 END) AS DESDE_JUN_2025,
        SUM(CASE WHEN a.APRTUSRG = 'SAA_AH' THEN 1 ELSE 0 END)          AS DE_CARGA
FROM    CRD.APRT a;

SELECT  COUNT(*) AS ENTIDADES_CON_HSTR_99
FROM  ( SELECT h.ENTDCDGO,
               ROW_NUMBER() OVER (PARTITION BY h.ENTDCDGO
                                  ORDER BY h.HSTRFCIN DESC, h.HSTRCDGO DESC) rn
        FROM   CRD.HSTR h WHERE h.HSTRESTD = 99 ) x
JOIN    CRD.ENTD e ON e.ENTDCDGO = x.ENTDCDGO
WHERE   x.rn = 1 AND e.ENTDIDST IN (1, 8);


-- =====================================================================================
-- 1. ALTER TABLE: CRD.APRT — periodo de devengo y tipo de movimiento
-- =====================================================================================
-- APRTPRDV: mes al que PERTENECE el aporte, siempre el primer dia del mes.
--           NULL = no aplica (movimiento que no es aporte mensual) o dato anterior a
--           esta solucion. Las consultas de cartera leen SIEMPRE
--                NVL(a.APRTPRDV, TRUNC(a.APRTFCTR, 'MM'))
--           para que los historicos sin backfill sigan respondiendo como hoy.
--
-- APRTFCTR NO CAMBIA DE SIGNIFICADO: sigue siendo la fecha de CAJA (fin del mes del
--           periodo de la carga) y sigue siendo la que lee CONTABILIDAD. Ninguna
--           consulta contable se toca en este cambio.
--
-- APRTTPMV: naturaleza del movimiento (rubro 235). Hoy eso solo se infiere del texto de
--           la glosa con LIKE, que es fragil. Se llena una vez en el backfill.
-- =====================================================================================

ALTER TABLE CRD.APRT ADD (
    APRTPRDV DATE,      -- periodo de devengo: primer dia del mes al que pertenece
    APRTTPMV NUMBER     -- tipo de movimiento; rubro 235 CRD_TIPO_MOVIMIENTO_APORTE
);

-- Guardarrail: si alguien graba un dia distinto del primero, el mes deja de agrupar.
ALTER TABLE CRD.APRT ADD CONSTRAINT CK_APRT_PRDV_MES
    CHECK (APRTPRDV IS NULL OR APRTPRDV = TRUNC(APRTPRDV, 'MM'));

-- Sostiene "cuanto lleva aportado este participe de este tipo en este mes", que es la
-- consulta que ejecutan el padron, la generacion y el estado de cuenta.
CREATE INDEX CRD.IDX_APRT_DEVENGO ON CRD.APRT (ENTDCDGO, TPAPCDGO, APRTPRDV);

COMMENT ON COLUMN CRD.APRT.APRTPRDV IS
    'Mes al que pertenece el aporte (primer dia del mes). NULL = no aplica o dato historico. NO es la fecha contable: esa es APRTFCTR.';
COMMENT ON COLUMN CRD.APRT.APRTTPMV IS
    'Tipo de movimiento. Rubro 235: 1 APORTE_MENSUAL, 2 AJUSTE_MANUAL, 3 DEVOLUCION, 4 PAGO_PRESTAMO, 5 REVERSO, 6 MIGRADO.';


-- =====================================================================================
-- 2. CREATE TABLE: CRD.VGCN — vigencias del contrato de adhesion
-- =====================================================================================
-- Una fila por (contrato, tipo de aporte, periodo de vigencia). Por tipo y no por
-- contrato porque un participe puede tener solo jubilacion, solo cesantia o los dos,
-- y pueden empezar en fechas distintas.
--
-- POR QUE VIGENCIAS Y NO UN MONTO FIJO EN EL CONTRATO: para saber si un mes PASADO
-- quedo cubierto hay que compararlo contra el monto que regia ESE mes. Con un unico
-- monto actual, subirle el aporte a alguien haria que sus meses viejos aparecieran
-- incompletos y la generacion se los volveria a cobrar.
--
-- VGCNMODO CALCULADO significa que VGCNMNTO se obtuvo de VGCNRMUN * VGCNPRCN. Aun asi,
-- EL VALOR OPERATIVO ES SIEMPRE VGCNMNTO: el porcentaje solo recalcula el monto al
-- CREAR una vigencia nueva, nunca al vuelo. Si el monto se calculara en cada generacion,
-- el dia que se depure PRTC.PRTCRMUN cambiaria en silencio lo que se le descuenta a la
-- gente. Un cambio de remuneracion CIERRA la vigencia y ABRE otra.
-- =====================================================================================

CREATE SEQUENCE CRD.SQ_VGCNCDGO START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE CRD.VGCN (
    VGCNCDGO NUMBER          NOT NULL,   -- PK
    CNTRCDGO NUMBER          NOT NULL,   -- FK CRD.CNTR
    TPAPCDGO NUMBER          NOT NULL,   -- FK CRD.TPAP (9 jubilacion / 11 cesantia)
    VGCNFCIN DATE            NOT NULL,   -- inicio de vigencia
    VGCNFCFN DATE,                       -- fin de vigencia; NULL = vigente
    VGCNMNTO NUMBER(18,2)    NOT NULL,   -- monto mensual OPERATIVO
    VGCNPRCN NUMBER(5,2),                -- porcentaje aplicado; NULL si modo FIJO
    VGCNRMUN NUMBER(18,2),               -- remuneracion usada en el calculo; NULL si FIJO
    VGCNMODO NUMBER          NOT NULL,   -- rubro 236: 1 CALCULADO, 2 FIJO
    VGCNIDHS NUMBER,                     -- HSTR de origen en la migracion. SIN FK, trazabilidad
    VGCNOBSR VARCHAR2(2000),
    VGCNIDST NUMBER          DEFAULT 1 NOT NULL,  -- 1 activo, 0 anulado
    VGCNUSRG VARCHAR2(50),
    VGCNFCRG TIMESTAMP
);

ALTER TABLE CRD.VGCN ADD CONSTRAINT PK_VGCN PRIMARY KEY (VGCNCDGO);

ALTER TABLE CRD.VGCN ADD CONSTRAINT FK_VGCN_CNTR
    FOREIGN KEY (CNTRCDGO) REFERENCES CRD.CNTR(CNTRCDGO);
ALTER TABLE CRD.VGCN ADD CONSTRAINT FK_VGCN_TPAP
    FOREIGN KEY (TPAPCDGO) REFERENCES CRD.TPAP(TPAPCDGO);

ALTER TABLE CRD.VGCN ADD CONSTRAINT CK_VGCN_FECHAS
    CHECK (VGCNFCFN IS NULL OR VGCNFCFN >= VGCNFCIN);
ALTER TABLE CRD.VGCN ADD CONSTRAINT CK_VGCN_MONTO
    CHECK (VGCNMNTO >= 0);
ALTER TABLE CRD.VGCN ADD CONSTRAINT CK_VGCN_MODO
    CHECK (VGCNMODO IN (1, 2));

-- Como maximo UNA vigencia abierta por contrato y tipo. El CASE hace que las filas ya
-- cerradas produzcan NULL y queden fuera del indice unico.
CREATE UNIQUE INDEX CRD.UK_VGCN_ABIERTA ON CRD.VGCN (
    CNTRCDGO, TPAPCDGO, CASE WHEN VGCNFCFN IS NULL THEN 1 END);

CREATE INDEX CRD.IDX_VGCN_BUSQ ON CRD.VGCN (CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN);

COMMENT ON TABLE CRD.VGCN IS
    'Vigencias del contrato de adhesion: monto mensual de aporte por tipo y periodo. Fuente de verdad de lo que se cobra.';


-- =====================================================================================
-- 3. ALTER TABLE: CRD.CNTR — espejo del monto vigente + porcentajes con decimales
-- =====================================================================================
-- CNTRMNAJ / CNTRMNAC son ESPEJO de la vigencia abierta, no la verdad. Existen para que
-- la grilla de contratos no resuelva VGCN fila por fila.
--
-- CNTRPRAI es el porcentaje de CESANTIA ("aporte individual") y CNTRPRAJ el de
-- JUBILACION — confirmado con el usuario el 2026-08-27; el nombre del campo no lo dice.
--
-- ⛔ EL MODIFY NO SE EJECUTA. Verificado el 2026-08-27: da ORA-01440 ("column to be
--    modified must be empty to decrease precision or scale") porque las columnas son
--    NUMBER SIN precision — es decir, YA ADMITEN DECIMALES y bajarlas a NUMBER(5,2)
--    seria REDUCIR la precision. No hay nada que cambiar en la base: el unico cambio
--    es el tipo Java (Long -> Double) en la entidad, y lo hace el agente de backend.
--
--    Si en otro ambiente el control 0.1 mostrara DATA_PRECISION = p con DATA_SCALE = 0,
--    entonces NO se baja a (5,2): se SUBE, p. ej. NUMBER(p+2, 2). Oracle permite
--    aumentar precision y escala, nunca reducirlas sobre una columna con datos.
-- =====================================================================================

ALTER TABLE CRD.CNTR ADD (
    CNTRMNAJ NUMBER(18,2),   -- espejo: monto mensual JUBILACION de la vigencia abierta
    CNTRMNAC NUMBER(18,2)    -- espejo: monto mensual CESANTIA de la vigencia abierta
);

-- NO EJECUTAR — ver el bloque de arriba.
-- ALTER TABLE CRD.CNTR MODIFY (
--     CNTRPRAI NUMBER(5,2),    -- porcentaje CESANTIA
--     CNTRPRAJ NUMBER(5,2)     -- porcentaje JUBILACION
-- );

COMMENT ON COLUMN CRD.CNTR.CNTRMNAJ IS
    'ESPEJO del monto de jubilacion de la vigencia abierta en CRD.VGCN. No calcular con este campo: leer VGCN.';
COMMENT ON COLUMN CRD.CNTR.CNTRMNAC IS
    'ESPEJO del monto de cesantia de la vigencia abierta en CRD.VGCN. No calcular con este campo: leer VGCN.';
COMMENT ON COLUMN CRD.CNTR.CNTRPRAI IS 'Porcentaje de aporte CESANTIA (aporte individual).';
COMMENT ON COLUMN CRD.CNTR.CNTRPRAJ IS 'Porcentaje de aporte JUBILACION.';


-- =====================================================================================
-- 4. ALTER TABLE: CRD.PRTC — remuneracion unificada con centavos
-- =====================================================================================
-- ⛔ NO EJECUTAR, por la misma razon del bloque 3: la columna es NUMBER sin precision y
--    ya admite centavos. El MODIFY daria ORA-01440.
--    El cambio real es el tipo Java: Participe.remuneracionUnificada pasa de Long a
--    Double. Mientras siga siendo Long, el backend TRUNCA los centavos al leer, aunque
--    la base los guarde — y el porcentaje derivado saldria torcido.
-- =====================================================================================

-- NO EJECUTAR — ver el bloque de arriba.
-- ALTER TABLE CRD.PRTC MODIFY (PRTCRMUN NUMBER(18,2));


-- =====================================================================================
-- 4b. ⛔ NO EJECUTAR SALVO QUE EL CONTROL 0.2b DIGA LO CONTRARIO (pedido 9)
-- =====================================================================================
-- El usuario verifico el 2026-08-27 que CRD.ENTD YA TIENE fecha y usuario de
-- modificacion. Entonces el pedido 9 NO NECESITA DDL: la columna existe y lo que falta
-- es que la ENTIDAD JPA la mapee (Entidad.java hoy solo mapea ENTDIPMD y ENTDUSMD) y
-- que los services la sellen al guardar. Eso lo hace el agente de backend.
--
-- Este bloque queda aqui solo por si el control 0.2b revelara que la columna NO existe.
-- En ese caso, descomentarlo y ejecutarlo; si no, saltarlo entero y no crear nada.
--
-- Si se ejecuta, la regla es la misma: UNA SOLA MARCA para toda la pantalla, sellada
-- desde el backend y nunca con un trigger — un trigger no sabe que usuario hizo el
-- cambio y el campo de usuario quedaria vacio.
-- =====================================================================================

-- ALTER TABLE CRD.PRTC ADD (
--     PRTCFCMD TIMESTAMP,      -- fecha/hora de la ultima modificacion del participe
--     PRTCUSMD VARCHAR2(50)    -- usuario de esa modificacion
-- );
--
-- COMMENT ON COLUMN CRD.PRTC.PRTCFCMD IS
--     'Ultima modificacion del participe. La sella el backend, no un trigger. No confundir con PRTCFCIN (fecha de ingreso).';


-- =====================================================================================
-- 5. RUBROS 235, 236 y 237
-- =====================================================================================
-- PRBRTPOO se copia del rubro 169 (ASPNovedadesCargaArchivo) para no inventar un valor:
-- es un rubro del mismo modulo y ya esta clasificado como corresponde.
-- =====================================================================================

-- ⚠ CODIGOS EXPLICITOS, NO SECUENCIA (decision del usuario, 2026-08-27).
--   PRBR: 235, 236, 237     PDTR: 1132 a 1140
--   PRBRCDGO Y PRBRALTR COINCIDEN A PROPOSITO (235/236/237): asi no hay forma de
--   confundirlos. La aplicacion lee siempre por ALTERNO
--   (selectValorNumericoByRubAltDetAlt), nunca por PK; en el detalle lee PDTRALTR.
--   El detalle NO sigue esa regla: PDTRCDGO va 1132-1140 y PDTRALTR va 1..6 / 1..2 / 1.
--   Consecuencia obligatoria: al insertar PK explicitos, las secuencias quedan atras y
--   el proximo INSERT de la aplicacion chocaria con ORA-00001. Se corrige en el 5.4.

-- 5.1 Rubro 235 (PK = alterno) — tipo de movimiento de aporte
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (235, 'CRD TIPO MOVIMIENTO APORTE', SYSDATE, 235,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 5.2 Rubro 236 (PK = alterno) — modo de la vigencia
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (236, 'CRD MODO VIGENCIA CONTRATO', SYSDATE, 236,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 5.3 Rubro 237 (PK = alterno) — parametros de contabilidad de CRD
-- El flag es GLOBAL por decision del usuario (2026-08-27): "o se alimenta todo o no se
-- alimenta nada". Se lee con selectValorNumericoByRubAltDetAlt(237, 1).
-- Se crea APAGADO (0). Encenderlo es el UPDATE del bloque 7.4.
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (237, 'CRD PARAMETROS CONTABILIDAD', SYSDATE, 237,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 5.4 Detalles de los tres rubros — PDTR 1132 a 1140
INSERT ALL
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1132, 235, 'APORTE MENSUAL',  1, NULL, 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1133, 235, 'AJUSTE MANUAL',   2, NULL, 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1134, 235, 'DEVOLUCION',      3, NULL, 3, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1135, 235, 'PAGO PRESTAMO',   4, NULL, 4, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1136, 235, 'REVERSO',         5, NULL, 5, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1137, 235, 'MIGRADO',         6, NULL, 6, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1138, 236, 'CALCULADO (remuneracion x porcentaje)', 1, NULL, 1, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1139, 236, 'FIJO (monto migrado de HSTR)',          2, NULL, 2, 1)
  INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
       VALUES (1140, 237, 'CONTABILIDAD CRD ACTIVA (0 apagada, 1 encendida)', 0, NULL, 1, 1)
SELECT * FROM DUAL;

COMMIT;


-- 5.5 ⚠ SINCRONIZAR LAS SECUENCIAS — obligatorio despues de insertar PK explicitos
-- -------------------------------------------------------------------------------------
-- Sin esto, el proximo rubro o detalle que cree la aplicacion pedira un NEXTVAL que ya
-- esta ocupado y fallara con ORA-00001.
--
-- Corre PRIMERO esta consulta. Si SIGUIENTE_VALOR ya es MAYOR que el PK usado, la
-- secuencia esta adelantada y NO SE TOCA: reiniciarla hacia abajo provocaria el choque
-- que se quiere evitar.
SELECT  'SQ_PRBRCDGO' AS SECUENCIA, 237 AS PK_USADO,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE_VALOR
FROM    DUAL
UNION ALL
SELECT  'SQ_PDTRCDGO', 1140,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- Ejecutar SOLO la linea cuya secuencia haya quedado en o por debajo del PK usado.
-- (RESTART START WITH requiere Oracle 12.2 o superior.)
-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 238;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1141;


-- =====================================================================================
-- 6. GRANTS
-- =====================================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON CRD.VGCN TO ROLE_CRD;
GRANT SELECT ON CRD.SQ_VGCNCDGO TO ROLE_CRD;


-- =====================================================================================
-- 7. CONTROLES POSTERIORES
-- =====================================================================================

-- 7.1 Las cuatro columnas nuevas existen con el tipo correcto. Esperado: 4 filas.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_PRECISION, c.DATA_SCALE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.COLUMN_NAME IN ('APRTPRDV','APRTTPMV','CNTRMNAJ','CNTRMNAC')
ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

-- 7.2 CRD.VGCN quedo con PK, 2 FK, 3 CHECK y 2 indices.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.SEARCH_CONDITION
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'VGCN' ORDER BY c.CONSTRAINT_TYPE, 1;

SELECT i.INDEX_NAME, i.UNIQUENESS FROM ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'VGCN' ORDER BY 1;

-- 7.3 Los tres rubros y sus detalles. Esperado: 235 -> 6 filas, 236 -> 2, 237 -> 1.
SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR, d.PDTRVLRN, d.PDTRESTD
FROM    SCP.PRBR r
LEFT    JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR IN (235, 236, 237)
ORDER BY r.PRBRALTR, d.PDTRALTR;

-- 7.4 ENCENDER LA CONTABILIDAD DE CRD.
--     NO ejecutar junto con el resto: se enciende cuando el cierre de cartera este
--     verificado. Para apagarla, el mismo UPDATE con 0.
-- UPDATE SCP.PDTR SET PDTRVLRN = 1
-- WHERE  PDTRALTR = 1
-- AND    PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 237);
-- COMMIT;
