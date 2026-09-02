-- =====================================================================================
-- PREVUELO DEL DESEMBOLSO DE PRESTAMOS — fase 0 de PLAN-DESEMBOLSO-PRESTAMO.md
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- QUE RESPONDE, y las cuatro cosas hay que saberlas ANTES de escribir una linea de Java:
--   Bloque 1 — Que PRSTIDPG no exista todavia en CRD.PRST.
--   Bloque 2 — CUAL es el producto de pago que apunta a 2.3.90.90.10 SOCIOS POR PAGAR.
--              Es el dato que hace que el asiento de bancos salga bien, y no se puede
--              inventar: con el producto equivocado, tesoreria paga y el asiento descarga
--              una cuenta que no es, CUADRADO IGUAL y sin ningun error.
--   Bloque 3 — Como esta armado un pago de origen externo que YA funciona (los de
--              devolucion de aportes), para copiar la forma en vez de deducirla.
--   Bloque 4 — Que productos de credito hay, para saber cuantos quedan sin plantilla.
--
-- ⚠️ NOTA SOBRE LOS NOMBRES DE PGS.PRDP: esa tabla NO sigue la convencion de 8 caracteres
--    del resto del sistema. Sus columnas se llaman ID, NOMBRE, CODIGO, GRUPOPRODUCTO,
--    ESTADO — verificado en com.saa.model.cxp.ProductoPago. No "corregir" esos nombres al
--    leer el script: son asi en la base.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230


-- =====================================================================================
-- BLOQUE 1 — PRSTIDPG no debe existir todavia
--
-- Esperado: 0 filas. Si aparece, alguien ya agrego la columna y hay que mirar por que
-- antes de correr el ALTER.
-- =====================================================================================
SELECT  c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'PRST'
AND     c.COLUMN_NAME = 'PRSTIDPG';

-- 1.b El patron a copiar: la columna equivalente de la devolucion de aportes, que ya
--     alimenta el mismo circuito. Esperado: 1 fila, NUMBER, nullable.
SELECT  c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
FROM    ALL_TAB_COLUMNS c
WHERE   c.OWNER = 'CRD'
AND     c.TABLE_NAME = 'DVAP'
AND     c.COLUMN_NAME = 'DVAPIDPG';


-- =====================================================================================
-- BLOQUE 2 — EL DATO QUE IMPORTA: el producto de pago de SOCIOS POR PAGAR
--
-- CXP arma el asiento del pago con UNA LINEA DEBE POR PRODUCTO, usando la cuenta
-- contable del GRUPO del producto, y una linea HABER al banco por el total.
-- Para que el desembolso descargue 2.3.90.90.10 —la misma cuenta que el asiento de
-- entrega acredita— hace falta el producto cuyo grupo apunte a esa cuenta.
--
-- Como leerlo:
--   * UNA fila     -> ese ID es el que va en el desglose. Anotarlo.
--   * VARIAS filas -> hay que elegir, y no lo elige el agente: AVISAR con la lista.
--   * NINGUNA      -> hay que CREAR el producto de pago (y quiza su grupo) antes de
--                     implementar. AVISAR: cambia el alcance de la fase 1.
-- =====================================================================================
SELECT  pp.ID                                      AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        pp.CODIGO                                  AS CODIGO_PRODUCTO,
        pp.ESTADO                                  AS ESTADO_PRODUCTO,
        gp.GRPPCDGO                                AS ID_GRUPO,
        gp.GRPPNMBR                                AS GRUPO,
        c.PLNNCDGO                                 AS ID_CUENTA,
        c.PLNNCNTA                                 AS CUENTA,
        c.PLNNNMBR                                 AS NOMBRE_CUENTA
FROM    PGS.PRDP pp
JOIN    PGS.GRPP gp ON gp.GRPPCDGO = pp.GRUPOPRODUCTO
JOIN    CNT.PLNN c  ON c.PLNNCDGO  = gp.PLNNCDGO
WHERE   c.PLNNCNTA = '2.3.90.90.10'
ORDER   BY pp.ID;

-- 2.b Si el bloque 2 vino vacio: todos los productos de pago activos con su cuenta, para
--     ver si hay alguno equivalente con otro nombre. Puede ser una lista larga.
SELECT  pp.ID                                      AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        gp.GRPPNMBR                                AS GRUPO,
        c.PLNNCNTA                                 AS CUENTA,
        c.PLNNNMBR                                 AS NOMBRE_CUENTA
FROM    PGS.PRDP pp
LEFT    JOIN PGS.GRPP gp ON gp.GRPPCDGO = pp.GRUPOPRODUCTO
LEFT    JOIN CNT.PLNN c  ON c.PLNNCDGO  = gp.PLNNCDGO
WHERE   pp.ESTADO = 1
ORDER   BY c.PLNNCNTA NULLS LAST, pp.ID;


-- =====================================================================================
-- BLOQUE 3 — Un pago de origen externo que YA funciona, con su desglose
--
-- Sirve para copiar la forma real en vez de deducirla del codigo: que valores llevan
-- PGTRORGN / PGTRIDOR, con que estado nacen, y como se ve el desglose en PGS.DPGT.
--
-- Como leerlo: si no hay ninguna fila, todavia no se registro ningun pago de origen
-- externo en esta base y no hay ejemplo que copiar. No bloquea, pero conviene saberlo.
-- =====================================================================================
SELECT  t.PGTRCDGO                                 AS ID_PAGO,
        t.PGTRORGN                                 AS ORIGEN,
        t.PGTRIDOR                                 AS ID_ORIGEN,
        t.PGTRESTD                                 AS ESTADO_PAGO,
        t.PGTRVLOR                                 AS VALOR,
        t.PGTRASNT                                 AS ASIENTO,
        TO_CHAR(t.PGTRFPRG, 'YYYY-MM-DD')          AS FECHA_PROGRAMADA
FROM    PGS.PGTR t
WHERE   t.PGTRORGN IS NOT NULL
ORDER   BY t.PGTRCDGO DESC
FETCH FIRST 10 ROWS ONLY;

-- 3.b El desglose de esos pagos: los pares (producto, valor) que CXP convierte en el
--     asiento. Es la forma exacta que tiene que armar el desembolso.
SELECT  d.PGTRCDGO                                 AS ID_PAGO,
        d.DPGTPRDP                                 AS ID_PRODUCTO_PAGO,
        pp.NOMBRE                                  AS PRODUCTO,
        d.DPGTVLRR                                 AS VALOR,
        d.DPGTCNCP                                 AS CONCEPTO
FROM    PGS.DPGT d
LEFT    JOIN PGS.PRDP pp ON pp.ID = d.DPGTPRDP
WHERE   d.PGTRCDGO IN (SELECT t.PGTRCDGO FROM PGS.PGTR t
                        WHERE t.PGTRORGN IS NOT NULL
                        ORDER BY t.PGTRCDGO DESC
                        FETCH FIRST 10 ROWS ONLY)
ORDER   BY d.PGTRCDGO DESC, d.DPGTPRDP;


-- =====================================================================================
-- BLOQUE 4 — Productos de credito, para saber cuantos quedan sin plantilla de entrega
--
-- Hoy hay plantilla para prendario (alterno 9) e hipotecario (13), y se creo la de
-- quirografario (34, script 156, sin ejecutar).
--
-- Como leerlo: si aparece un producto de credito que no sea de esos tres, el desembolso
-- lo va a RECHAZAR con un mensaje — por diseno, no se elige una plantilla por defecto.
-- Si esos productos tienen CARTERA_VIVA > 0, hay que decidir que plantilla les
-- corresponde ANTES de que alguien intente otorgar uno.
-- =====================================================================================
SELECT  p.PRDCCDGO                                 AS ID_PRODUCTO,
        p.PRDCNMBR                                 AS PRODUCTO,
        p.PRDCCDPT                                 AS CODIGO_PETRO,
        p.PRDCESTD                                 AS ESTADO,
        (SELECT COUNT(*) FROM CRD.PRST pr
          WHERE pr.PRDCCDGO = p.PRDCCDGO
            AND pr.PRSTIDST IN (2, 8, 11))         AS CARTERA_VIVA
FROM    CRD.PRDC p
ORDER   BY p.PRDCCDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 5 — EL MAPEO PRODUCTO -> FAMILIA CONTABLE (agregado 2026-09-01)
--
-- POR QUE HIZO FALTA: el asiento de entrega elige la plantilla segun la familia del
-- producto (prendario -> 9, hipotecario -> 13, quirografario -> 34). El agente de
-- backend resolvio la familia comparando CRD.TPPR.TPPRNMBR contra los textos literales
-- "PRENDARIO" / "HIPOTECARIO" / "QUIROGRAFARIO", porque NO HAY NINGUN PRECEDENTE en el
-- codigo que clasifique productos por familia — lo busco y no existe.
--
-- El riesgo que el mismo detecto: CARGA-INICIAL-BANDAS-PRODUCTO.md muestra que la
-- familia quirografaria incluye EMERGENTE, CENAPRO, EXPRESS, SUST. BIESS/MERCADO y las
-- variantes RESTR./NOVACION. Es muy improbable que todos esos tengan TPPRNMBR
-- literalmente "QUIROGRAFARIO". Con la implementacion actual esos productos se
-- RECHAZAN al aprobar — que es el comportamiento seguro (mejor frenar que clasificar
-- mal en silencio), pero significa que no se podrian otorgar.
--
-- Este bloque dice, con datos, si esa comparacion por texto alcanza o hace falta otra
-- fuente para la familia.
--
-- Como leerlo:
--   * Si TPPRNMBR trae exactamente PRENDARIO / HIPOTECARIO / QUIROGRAFARIO y nada mas,
--     la comparacion por texto sirve tal cual.
--   * Si TPPRTPOO (la otra columna, un VARCHAR2(50) llamado "tipo") agrupa las familias
--     — p.ej. varios productos distintos compartiendo el mismo valor — ESA es la fuente
--     correcta y hay que cambiar la implementacion.
--   * Si ninguna de las dos agrupa, hace falta una tabla de mapeo nueva y eso cambia el
--     alcance: AVISAR antes de seguir.
-- =====================================================================================
SELECT  t.TPPRCDGO                                 AS ID_TIPO,
        t.TPPRNMBR                                 AS NOMBRE_TIPO,
        t.TPPRTPOO                                 AS TIPO_AGRUPADOR,
        t.TPPRCSPB                                 AS CODIGO_SBS,
        (SELECT COUNT(*) FROM CRD.PRDC p
          WHERE p.TPPRCDGO = t.TPPRCDGO)           AS PRODUCTOS
FROM    CRD.TPPR t
ORDER   BY t.TPPRCDGO;

-- 5.b El cruce que decide: cada producto con el nombre de su tipo, y si ese nombre
--     coincide EXACTAMENTE con alguno de los tres literales que hoy busca el codigo.
--
-- Como leerlo: la columna MATCH_LITERAL dice si el producto se puede desembolsar hoy.
-- Todo producto con CARTERA_VIVA > 0 y MATCH_LITERAL = 'NO' es uno que, cuando alguien
-- intente otorgarlo, va a ser rechazado.
SELECT  p.PRDCCDGO                                 AS ID_PRODUCTO,
        p.PRDCNMBR                                 AS PRODUCTO,
        t.TPPRNMBR                                 AS TIPO,
        t.TPPRTPOO                                 AS TIPO_AGRUPADOR,
        CASE WHEN UPPER(TRIM(t.TPPRNMBR)) IN ('PRENDARIO','HIPOTECARIO','QUIROGRAFARIO')
             THEN 'SI' ELSE 'NO' END               AS MATCH_LITERAL,
        (SELECT COUNT(*) FROM CRD.PRST pr
          WHERE pr.PRDCCDGO = p.PRDCCDGO
            AND pr.PRSTIDST IN (2, 8, 11))         AS CARTERA_VIVA
FROM    CRD.PRDC p
LEFT    JOIN CRD.TPPR t ON t.TPPRCDGO = p.TPPRCDGO
ORDER   BY MATCH_LITERAL, CARTERA_VIVA DESC, p.PRDCCDGO;


-- =====================================================================================
-- BLOQUE 6 — PRSTVLAS (valorAsegurado): confirmar que esta vacia
--
-- POR QUE IMPORTA: las plantillas 9 y 13 tienen una linea para el BIEN en garantia
-- (7.4.01.10 VEHICULOS / 7.4.01.15 BIENES INMUEBLES). El codigo la alimenta con
-- Prestamo.valorAsegurado (PRSTVLAS).
--
-- Y PRSTVLAS es una de las CUATRO COLUMNAS DE SEGURO MUERTAS de CRD.PRST que ya habia
-- documentado el levantamiento de seguros (ESTADO-EQUIPO-SEGUROS.md §1.3): mapeadas en
-- la entidad, presentes en la base, y NADIE las escribe. Verificado hoy: el unico lector
-- en todo el backend es el codigo del desembolso recien escrito.
--
-- Consecuencia si esta en 0/NULL: la linea del bien NO se genera nunca. El asiento cuadra
-- igual (esta hecho para eso), pero un prendario o un hipotecario quedarian SIN registrar
-- su garantia en cuentas de orden, que es justamente para lo que existe esa linea.
--
-- Como leerlo:
--   * CON_VALOR = 0 -> la linea del bien nunca se va a generar. Hay que decidir de donde
--     sale ese valor ANTES de otorgar el primer prendario o hipotecario. NO bloquea el
--     quirografario, que no tiene linea de bien.
--   * CON_VALOR > 0 -> hay datos y la linea se genera para esos prestamos.
-- =====================================================================================
SELECT  COUNT(*)                                            AS PRESTAMOS,
        SUM(CASE WHEN NVL(p.PRSTVLAS,0) > 0 THEN 1 ELSE 0 END) AS CON_VALOR_ASEGURADO,
        SUM(CASE WHEN p.PRSTVLAS IS NULL THEN 1 ELSE 0 END) AS EN_NULL,
        ROUND(SUM(NVL(p.PRSTVLAS,0)), 2)                    AS SUMA
FROM    CRD.PRST p;


-- =====================================================================================
-- FIN (bloques 1 a 6). Pegar la salida completa.
-- =====================================================================================
