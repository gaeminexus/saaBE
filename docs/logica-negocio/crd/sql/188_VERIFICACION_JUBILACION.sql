-- =====================================================================================
-- VERIFICACION DE LA PRUEBA DE JUBILACION — bloques de apoyo de PLAN-PRUEBA-JUBILACION.md
-- FECHA: 2026-09-03   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- Este script NO se corre de una sola vez. Cada bloque corresponde a un momento del plan:
--
--   BLOQUE 1  precondicion P1/P2  — la plantilla 29 y sus lineas          (ANTES de todo)
--   BLOQUE 2  precondicion P3     — los tipos de aporte 1, 2 y 23         (ANTES de todo)
--   BLOQUE 3  precondicion P4     — candidatos y su estado ACTUAL         (ANTES de cada caso)
--   BLOQUE 4  verificacion        — que paso con UN participe             (DESPUES de cada caso)
--   BLOQUE 5  verificacion C7     — que no haya doble contabilizacion     (DESPUES de C7)
--
-- ⛔ EL BLOQUE 3 SE CORRE Y SE GUARDA ANTES DE CADA CASO. Es la unica forma de saber a que
--    estado devolver al participe si hay que deshacer: no existe revertirJubilacion, y
--    ACTIVO y ACTIVO EN MORA son estados distintos (§5 del plan).
-- =====================================================================================

-- (!) CÓMO SE PARAMETRIZA — corregido el 2026-09-04, sin comandos de SQL*Plus.
--
-- Los bloques 3, 4 y 5 miran UN partícipe. Antes de correrlos, buscar y reemplazar en todo
-- el archivo el texto        0 /*<<ENTIDAD>>*/        por el ID del partícipe del caso en
-- curso. Son 6 apariciones y un solo reemplazo las cubre todas.
--
-- Antes esto usaba DEFINE/&ENTIDAD y SET PAGESIZE/LINESIZE. Los tres son comandos de
-- SQL*Plus, no SQL: sólo funcionan en sqlplus y en SQL Developer ejecutando como script.
-- En otros clientes no hacen nada o ensucian la salida — lo reportó el usuario el
-- 2026-09-04 con los scripts 189 y 190. Se sacaron por el mismo motivo.
--
-- La empresa contable no hace falta parametrizarla: el bloque 1 lista TODAS las empresas
-- que tienen la plantilla alterno 29, que es más útil que filtrar por una.


-- =====================================================================================
-- BLOQUE 1 — P1 y P2: la plantilla alterno 29 existe para la empresa y tiene sus lineas.
--
-- Esperado: la cabecera, y entre las lineas los aux1 1, 2 y 5 (los que usa el proceso).
-- Los aux1 3 y 4 PUEDEN existir — estan definidos a proposito y sin consumidor; que esten
-- no es un problema. El problema seria que el asiento generado los USE (ver bloque 4).
--
-- Si no devuelve cabecera: PARAR, todo el plan falla en C1 por esta causa.
-- =====================================================================================
-- (!) TRAMPA DE NOMBRE, verificada en Plantilla.java:64-66 el 2026-09-04:
-- la EMPRESA de una plantilla es la columna PJRQCDGO. El nombre parece de una FK a
-- jerarquia, pero el @JoinColumn mapea ahi la Empresa, y es por esa columna que filtra
-- selectByAlterno(alterno, empresa). No buscar una columna "EMPRCDGO": no existe.
SELECT  p.PLNSCDGO                                              AS ID_PLANTILLA,
        p.PJRQCDGO                                              AS ID_EMPRESA,
        p.PLNSCDAL                                              AS ALTERNO,
        SUBSTR(p.PLNSNMBR,1,60)                                 AS NOMBRE,
        p.PLNSESTD                                              AS ESTADO
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 29
ORDER   BY p.PJRQCDGO;

-- (!) ID_EMPRESA es lo que hay que pasarle a procesarJubilacion. Si la prueba se corre con
--     una empresa que NO aparece en esta lista, el caso C1 va a fallar por falta de
--     plantilla — que es precisamente lo que el caso C6 provoca a proposito.
--     Anotar de esta lista: una empresa CON plantilla (para C1..C5, C7, C8) y una SIN
--     plantilla (para C6). Sin las dos, C6 no se puede montar.

SELECT  d.DTPLCDGO                                              AS ID_LINEA,
        d.DTPLAXL1                                              AS AUX1,
        d.DTPLMVMN                                              AS MOVIMIENTO,
        SUBSTR(d.DTPLDSCR,1,60)                                 AS DESCRIPCION,
        d.DTPLESTD                                              AS ESTADO,
        d.DTPLFCIN                                              AS VIGENTE_DESDE,
        d.DTPLFCFN                                              AS VIGENTE_HASTA
FROM    CNT.DTPL d
JOIN    CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO
WHERE   p.PLNSCDAL = 29
ORDER   BY d.DTPLAXL1;


-- =====================================================================================
-- BLOQUE 2 — P3: los tipos de aporte que intervienen.
--
-- Esperado: las tres filas. 1 = cesantia, 2 = jubilacion, 23 = pension complementaria.
-- =====================================================================================
SELECT  t.TPAPCDGO                                              AS TIPO,
        SUBSTR(t.TPAPNMBR,1,50)                                 AS NOMBRE
FROM    CRD.TPAP t
WHERE   t.TPAPCDGO IN (1, 2, 23)
ORDER   BY t.TPAPCDGO;


-- =====================================================================================
-- BLOQUE 3 — P4 + ⛔ EL RESPALDO DEL ESTADO. Candidatos, con sus saldos y su estado ACTUAL.
--
-- Como elegir para cada caso:
--   C1  una fila con SALDO_CESANTIA > 0 Y SALDO_JUBILACION > 0
--   C2  una con una de las dos en 0
--   C3  una con las DOS en 0
--   C4  un participe cuyo ESTADO_ACTUAL no sea ACTIVO ni ACTIVO EN MORA
--
-- ⛔ GUARDAR ESTA SALIDA. La columna ESTADO_ACTUAL es a donde hay que devolver al
--    participe si hay que deshacer a mano — no hay reverso automatico.
-- =====================================================================================
SELECT  e.ENTDCDGO                                              AS ID_ENTIDAD,
        SUBSTR(e.ENTDRZNS,1,35)                                 AS PARTICIPE,
        e.ENTDRLPC                                              AS ROL,
        e.ENTDIDST                                              AS ESTADO_ACTUAL,
        ROUND(NVL(ces.SALDO,0), 2)                              AS SALDO_CESANTIA,
        ROUND(NVL(jub.SALDO,0), 2)                              AS SALDO_JUBILACION,
        ROUND(NVL(pen.SALDO,0), 2)                              AS SALDO_PENSION,
        ROUND(NVL(ces.SALDO,0) + NVL(jub.SALDO,0), 2)           AS A_TRASLADAR
FROM    CRD.ENTD e
LEFT JOIN (SELECT a.ENTDCDGO, SUM(NVL(a.APRTVLRR,0)) AS SALDO FROM CRD.APRT a
           WHERE a.TPAPCDGO = 1  GROUP BY a.ENTDCDGO) ces ON ces.ENTDCDGO = e.ENTDCDGO
LEFT JOIN (SELECT a.ENTDCDGO, SUM(NVL(a.APRTVLRR,0)) AS SALDO FROM CRD.APRT a
           WHERE a.TPAPCDGO = 2  GROUP BY a.ENTDCDGO) jub ON jub.ENTDCDGO = e.ENTDCDGO
LEFT JOIN (SELECT a.ENTDCDGO, SUM(NVL(a.APRTVLRR,0)) AS SALDO FROM CRD.APRT a
           WHERE a.TPAPCDGO = 23 GROUP BY a.ENTDCDGO) pen ON pen.ENTDCDGO = e.ENTDCDGO
WHERE   NVL(ces.SALDO,0) + NVL(jub.SALDO,0) > 0
ORDER   BY A_TRASLADAR DESC
FETCH FIRST 30 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — ⛔ DESPUES DE CADA CASO: que le paso a ESE participe.
--
-- ⚠️ Reemplazar el marcador <<ENTIDAD>> por el ID del participe antes de correrlo (ver cabecera).
--
-- 4.a — Los movimientos de jubilacion generados (APRTTPMV = 7).
--
--   C1  esperado: TRES filas. Dos NEGATIVAS (tipos 1 y 2) y una POSITIVA (tipo 23).
--                 |suma de las negativas| = la positiva.
--   C2  esperado: DOS filas.
--   C3  esperado: NINGUNA.
--   C4/C5/C6 esperado: NINGUNA. Si aparecen en C6, EL ROLLBACK NO FUNCIONO -> parar todo
--                 y avisar: es el hallazgo mas grave que este plan puede producir.
-- =====================================================================================
SELECT  a.APRTCDGO                                              AS APORTE,
        a.TPAPCDGO                                              AS TIPO,
        ROUND(NVL(a.APRTVLRR,0), 2)                             AS VALOR,
        a.APRTFCTR                                              AS FECHA_TRANSACCION,
        a.APRTUSRG                                              AS USUARIO,
        SUBSTR(a.APRTGLSA,1,60)                                 AS GLOSA
FROM    CRD.APRT a
WHERE   a.ENTDCDGO = 0 /*<<ENTIDAD>>*/
AND     a.APRTTPMV = 7
ORDER   BY a.APRTCDGO;


-- 4.b — Los saldos DESPUES.
--
--   C1/C2 esperado: cesantia y jubilacion en 0; pension subio por el total trasladado.
--   C3    esperado: los tres igual que antes.
--   C6    esperado: IDENTICOS a los del bloque 3 corrido antes del caso.
SELECT  ROUND(NVL(SUM(CASE WHEN a.TPAPCDGO = 1  THEN a.APRTVLRR END),0), 2) AS SALDO_CESANTIA,
        ROUND(NVL(SUM(CASE WHEN a.TPAPCDGO = 2  THEN a.APRTVLRR END),0), 2) AS SALDO_JUBILACION,
        ROUND(NVL(SUM(CASE WHEN a.TPAPCDGO = 23 THEN a.APRTVLRR END),0), 2) AS SALDO_PENSION
FROM    CRD.APRT a
WHERE   a.ENTDCDGO = 0 /*<<ENTIDAD>>*/;


-- 4.c — El estado del participe.
--
--   C1/C2/C3 esperado: JUBILADO COMPLEMENTARIO.
--   C4/C5/C6 esperado: el MISMO que tenia en el bloque 3.
SELECT  e.ENTDCDGO                                              AS ID_ENTIDAD,
        SUBSTR(e.ENTDRZNS,1,35)                                 AS PARTICIPE,
        e.ENTDIDST                                              AS ESTADO_ACTUAL
FROM    CRD.ENTD e
WHERE   e.ENTDCDGO = 0 /*<<ENTIDAD>>*/;


-- 4.d — ⛔ EL ASIENTO, Y LA CUENTA QUE NO DEBE ESTAR.
--
-- ⚠️ CNT.DTAS **no tiene columna de aux1 ni de "movimiento"** — verificado contra
--    CNT.DetalleAsiento el 2026-09-03. El debe y el haber son DOS COLUMNAS distintas
--    (DTASDBEE / DTASHBRR) y el aux1 vive en la LINEA DE PLANTILLA, no en el asiento. Por eso
--    la verificacion se hace por CUENTA CONTABLE (DTASCNTA), que ademas es mas fuerte: dice
--    a donde fue la plata, no que posicion de la plantilla se uso.
--
--   C1 esperado: TRES lineas
--                  DEBE  2.1.01.05.01  APORTES PERSONALES CESANTIA
--                  DEBE  2.1.02.05.01  APORTES PERSONALES JUBILACION
--                  HABER 2.3.01.10.03  PENSIONES COMPLEMENTARIAS POR PAGAR
--   C2 esperado: DOS lineas (falta la de la cuenta que estaba en cero).
--   C3 esperado: NINGUN asiento — numeroAsiento null es CORRECTO acá, no un fallo (§C3).
--
-- ⛔ NINGUNA linea con cuenta 2.3.01.05.01 (LIQUIDACION APORTES CESANTIA) ni 2.3.01.10.01
--    (LIQUIDACION APORTES JUBILACION). Esas son las patas aux1 3 y 4, que el cruce y la
--    devolucion ya contabilizan por su cuenta: si aparecen acá, el mismo dinero esta
--    asentado dos veces.
--
-- ⛔ Y SUMA_DEBE tiene que ser igual a SUMA_HABER al centavo. Con el precedente del centavo
--    de la carga 449 (sql/186/187), esto se mira, no se asume.
SELECT  a.ASNTCDGO                                              AS ASIENTO,
        a.ASNTFCHA                                              AS FECHA,
        d.DTASCNTA                                              AS CUENTA,
        SUBSTR(d.DTASNMCT,1,40)                                 AS NOMBRE_CUENTA,
        ROUND(NVL(d.DTASDBEE,0), 2)                             AS DEBE,
        ROUND(NVL(d.DTASHBRR,0), 2)                             AS HABER,
        SUBSTR(d.DTASDSCR,1,50)                                 AS DESCRIPCION
FROM    CNT.ASNT a
JOIN    CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE   d.DTASDSCR LIKE '%Entidad ' || 0 /*<<ENTIDAD>>*/ || ' %'
ORDER   BY a.ASNTCDGO, d.DTASCDGO;

-- El cuadre del asiento. Las dos columnas tienen que dar IGUAL.
SELECT  a.ASNTCDGO                                              AS ASIENTO,
        ROUND(SUM(NVL(d.DTASDBEE,0)), 2)                        AS SUMA_DEBE,
        ROUND(SUM(NVL(d.DTASHBRR,0)), 2)                        AS SUMA_HABER,
        ROUND(SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)), 2) AS DESCUADRE
FROM    CNT.ASNT a
JOIN    CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE   d.DTASDSCR LIKE '%Entidad ' || 0 /*<<ENTIDAD>>*/ || ' %'
GROUP   BY a.ASNTCDGO
ORDER   BY a.ASNTCDGO;


-- =====================================================================================
-- BLOQUE 5 — VERIFICACION DE C7: los tres pasos no contabilizan lo mismo dos veces.
--
-- Se corre DESPUES de haber hecho, sobre el mismo participe: cruce contra prestamo ->
-- devolucion en efectivo -> procesarJubilacion.
--
-- Como leerlo: TOTAL_DEBE tiene que ser igual al saldo que el participe tenia ANTES de
-- empezar (columna A_TRASLADAR del bloque 3, guardada antes de C7).
--
--   IGUAL   -> correcto: cada peso se conto una sola vez.
--   MAYOR   -> ⛔ doble contabilizacion. Mirar el 4.d: casi seguro aparecio una linea de
--              LIQUIDACION (2.3.01.05.01 / 2.3.01.10.01) repitiendo lo que el cruce o la
--              devolucion ya asentaron.
--   MENOR   -> falta un asiento: alguno de los tres pasos no contabilizo.
--
-- ⚠️ El LIKE de la descripcion es el unico hilo que une los tres asientos con el participe.
--    Si alguno de los otros dos procesos (pagarConAportes / registrar devolucion) no escribe
--    "Entidad N" en la descripcion de sus lineas, este bloque va a contar de menos y hay que
--    buscar esos asientos por su propio criterio antes de concluir que falta contabilidad.
-- =====================================================================================
SELECT  COUNT(DISTINCT a.ASNTCDGO)                              AS ASIENTOS,
        ROUND(SUM(NVL(d.DTASDBEE,0)), 2)                        AS TOTAL_DEBE,
        ROUND(SUM(NVL(d.DTASHBRR,0)), 2)                        AS TOTAL_HABER
FROM    CNT.ASNT a
JOIN    CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE   d.DTASDSCR LIKE '%Entidad ' || 0 /*<<ENTIDAD>>*/ || ' %';


-- =====================================================================================
-- FIN.
--
-- Nombres de columna verificados el 2026-09-03 contra las entidades JPA:
--   CNT.ASNT   ASNTCDGO, ASNTFCHA, ASNTOBSR   (NO existe ASNTGLSA)
--   CNT.DTAS   DTASCDGO, DTASDSCR, DTASDBEE, DTASHBRR, DTASNMCT, DTASCNTA
--              (NO existe DTASAXL1 ni DTASMVMN ni DTASVLRR — el aux1 vive en CNT.DTPL)
-- =====================================================================================
