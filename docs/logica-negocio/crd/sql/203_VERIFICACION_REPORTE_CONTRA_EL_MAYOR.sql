-- =====================================================================================
-- 203 - Verificacion del reporte RPRT_PGPC_CRRD contra el mayor contable y las ordenes
-- FECHA: 2026-09-05 - EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Contexto: pedido del arbitro tras el documento de reglas de clasificacion de lap-saa-1
-- (docs/logica-negocio/crd/REGLAS-CLASIFICACION-PARA-REPORTES-FINANCIEROS.md, regla 7 --
-- "la prueba que vale": el reporte no tiene que cuadrar consigo mismo, tiene que cuadrar
-- contra el mayor). RPRT_PGPC_CRRD no clasifica cartera por banda (verificado, no le
-- aplican las reglas 1-5 de ese documento), pero SI totaliza pension/seguro/cruzado/
-- transferido, y ESOS numeros si tienen que cuadrar contra las cuentas y las ordenes reales.
--
-- Cambiar los literales 2026 / 8 / 1236 de mas abajo por el periodo y la empresa reales
-- que se quiera verificar (mismo periodo que se le paso al reporte: PGPCANNO/PGPCMESS,
-- el periodo CUBIERTO, no la fecha de ejecucion de la corrida).
--
-- NOTA sobre el alcance de "periodo" en estas consultas: igual que el propio reporte,
-- BLOQUE 1/2/3 usan la CTE "objetivo" (jubilados con al menos una fila en el periodo
-- pedido, mas TODAS sus filas de la misma fecha de corrida) para que los totales
-- coincidan exactamente con lo que el reporte va a mostrar.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 - Total PENSION (neto, post-H43) contra el saldo de 2.1.02.25.01
--
-- QUE SE ESPERA: el total "transferido al banco" + el total "cruzado a prestamos" +
-- lo retenido (PGPC en estado REGISTRADA, sin cuenta/certificado) tiene que sumar
-- exactamente el total de PGPCVLPN de la seccion 1 del reporte. Si no suma, hay un
-- cuarto destino que el reporte no esta viendo (ver JavaDoc de generarAsientoDevengoPension,
-- identidad remanente+cruce+seguro=valorTotal).
-- =====================================================================================
WITH objetivo AS (
    SELECT DISTINCT p.ENTDCDGO AS ENTDCDGO, TRUNC(p.PGPCFCRG) AS FECHA_CORRIDA
    FROM CRD.PGPC p
    WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 8
),
cruce AS (
    SELECT pr.ENTDCDGO AS ENTDCDGO, TRUNC(g.PGPRFCHA) AS FECHA, SUM(g.PGPRVLRR) AS CRZ_VALOR
    FROM CRD.PGPR g
    JOIN CRD.PRST pr ON pr.PRSTCDGO = g.PRSTCDGO
    WHERE (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)
    GROUP BY pr.ENTDCDGO, TRUNC(g.PGPRFCHA)
)
SELECT
    SUM(p.PGPCVLPN) AS TOTAL_PENSION_NOMINAL,
    SUM(NVL(cr.CRZ_VALOR,0)) AS TOTAL_CRUZADO,
    SUM(CASE WHEN p.PGPCESTD IN (2,3) THEN GREATEST(p.PGPCVLPN - NVL(cr.CRZ_VALOR,0), 0) ELSE 0 END) AS TOTAL_TRANSFERIDO_BANCO,
    SUM(CASE WHEN p.PGPCESTD = 1 THEN GREATEST(p.PGPCVLPN - NVL(cr.CRZ_VALOR,0), 0) ELSE 0 END) AS TOTAL_RETENIDO,
    SUM(p.PGPCVLPN)
        - SUM(NVL(cr.CRZ_VALOR,0))
        - SUM(CASE WHEN p.PGPCESTD IN (2,3) THEN GREATEST(p.PGPCVLPN - NVL(cr.CRZ_VALOR,0), 0) ELSE 0 END)
        - SUM(CASE WHEN p.PGPCESTD = 1 THEN GREATEST(p.PGPCVLPN - NVL(cr.CRZ_VALOR,0), 0) ELSE 0 END)
        AS DIFERENCIA_SIN_EXPLICAR
FROM objetivo o
JOIN CRD.PGPC p ON p.ENTDCDGO = o.ENTDCDGO AND TRUNC(p.PGPCFCRG) = o.FECHA_CORRIDA
LEFT JOIN cruce cr ON cr.ENTDCDGO = p.ENTDCDGO AND cr.FECHA = p.PGPCFCHA;

-- COMO SE LEE: DIFERENCIA_SIN_EXPLICAR tiene que dar 0 (o una diferencia de centavos por
-- redondeo, TOLERANCIA=0.01 en el codigo). Si no da 0: revisar si algun PGPC quedo en
-- estado 4/5 (RECHAZADA/ANULADA) con PGPCVLPN>0 -- ese caso hoy no se contempla en ninguno
-- de los tres destinos y hay que sumarlo aparte antes de alarmarse.


-- =====================================================================================
-- BLOQUE 2 - Total TRANSFERIDO AL BANCO contra la suma de ordenes de pago generadas
--            (PGS.PGTR, origen CRD_PAGO_PENSION_COMPLEMENTARIA) -- el que mas le importa
--            al usuario: "lo que dice el reporte es lo que efectivamente se le pidio al banco"
-- =====================================================================================
WITH objetivo AS (
    SELECT DISTINCT p.ENTDCDGO AS ENTDCDGO, TRUNC(p.PGPCFCRG) AS FECHA_CORRIDA
    FROM CRD.PGPC p
    WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 8
),
cruce AS (
    SELECT pr.ENTDCDGO AS ENTDCDGO, TRUNC(g.PGPRFCHA) AS FECHA, SUM(g.PGPRVLRR) AS CRZ_VALOR
    FROM CRD.PGPR g
    JOIN CRD.PRST pr ON pr.PRSTCDGO = g.PRSTCDGO
    WHERE (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)
    GROUP BY pr.ENTDCDGO, TRUNC(g.PGPRFCHA)
),
reporte AS (
    SELECT SUM(CASE WHEN p.PGPCESTD IN (2,3) THEN GREATEST(p.PGPCVLPN - NVL(cr.CRZ_VALOR,0), 0) ELSE 0 END) AS TOTAL_REPORTE
    FROM objetivo o
    JOIN CRD.PGPC p ON p.ENTDCDGO = o.ENTDCDGO AND TRUNC(p.PGPCFCRG) = o.FECHA_CORRIDA
    LEFT JOIN cruce cr ON cr.ENTDCDGO = p.ENTDCDGO AND cr.FECHA = p.PGPCFCHA
),
ordenes AS (
    SELECT COUNT(*) AS CANTIDAD_ORDENES, SUM(g.PGTRVLOR) AS TOTAL_ORDENES
    FROM PGS.PGTR g
    WHERE g.PGTRORGN = 'CRD_PENSION_COMPLEMENTARIA'
      AND g.PGTRPJRQ = 1236
      AND g.PGTRIDOR IN (
          SELECT p.PGPCCDGO FROM CRD.PGPC p
          JOIN (SELECT DISTINCT ENTDCDGO, TRUNC(PGPCFCRG) AS FECHA_CORRIDA FROM CRD.PGPC
                WHERE PGPCANNO = 2026 AND PGPCMESS = 8) o
            ON o.ENTDCDGO = p.ENTDCDGO AND TRUNC(p.PGPCFCRG) = o.FECHA_CORRIDA
      )
)
SELECT r.TOTAL_REPORTE, o.CANTIDAD_ORDENES, o.TOTAL_ORDENES,
       r.TOTAL_REPORTE - NVL(o.TOTAL_ORDENES,0) AS DIFERENCIA
FROM reporte r, ordenes o;

-- COMO SE LEE: DIFERENCIA tiene que dar 0. PGTRIDOR es el PGPCCDGO (ver
-- generarOrdenPagoPension, idOrigen=pago.getCodigo()) -- por eso el IN contra los PGPCCDGO
-- de la misma "objetivo" que arma el reporte. Si CANTIDAD_ORDENES es menor que la cantidad
-- de filas con estado EN_PAGO/PAGADA del bloque 1, hay PGPC marcados como pagados sin una
-- orden real detras -- eso es mas grave que una diferencia de monto.


-- =====================================================================================
-- BLOQUE 3 - Total SEGURO contra la orden agregada al proveedor (PGS.PGTR, origen
--            CRD_SEGURO_JUBILADOS) -- idOrigen sintetico anio*100+mes, NO usa la CTE
--            "objetivo" porque la orden del seguro es UNA por corrida real (fecha de
--            ejecucion), no por periodo cubierto -- ver OrigenPagoExterno.CRD_SEGURO_JUBILADOS.
-- =====================================================================================
SELECT
    (SELECT SUM(p.PGPCVLSG) FROM CRD.PGPC p
       WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 8) AS TOTAL_SEGURO_REPORTE,
    (SELECT g.PGTRVLOR FROM PGS.PGTR g
       WHERE g.PGTRORGN = 'CRD_SEGURO_JUBILADOS'
         AND g.PGTRPJRQ = 1236
         AND g.PGTRIDOR = (2026 * 100 + 8)) AS VALOR_ORDEN_PROVEEDOR;

-- COMO SE LEE: estos dos SOLO van a coincidir si la corrida de la orden al proveedor fue
-- la MISMA que genero estos PGPC (idOrigen = anio*100+mes del PERIODO, no de la fecha de
-- ejecucion -- ver generarOrdenPagoProveedorSeguro). Si el reporte pidio un periodo que no
-- corresponde 1 a 1 con una corrida (por ejemplo, un jubilado con seguro de un mes atrasado
-- dentro de una corrida de otro mes), esta comparacion puede no cuadrar por diseño -- no es
-- necesariamente un error, revisar contra el caso real antes de alarmarse.


-- =====================================================================================
-- BLOQUE 4 - Total CRUZADO A PRESTAMOS contra el movimiento agregado de CRD.APRT
--            (tipo 23, aporte consumido por prestamo) del mismo periodo -- la cuenta
--            2.1.02.25.01 del lado del aporte, no del lado del prestamo.
-- =====================================================================================
WITH objetivo AS (
    SELECT DISTINCT p.ENTDCDGO AS ENTDCDGO, TRUNC(p.PGPCFCRG) AS FECHA_CORRIDA, p.PGPCFCHA AS PGPCFCHA
    FROM CRD.PGPC p
    WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 8
)
SELECT
    (SELECT SUM(g.PGPRVLRR)
       FROM CRD.PGPR g
       JOIN CRD.PRST pr ON pr.PRSTCDGO = g.PRSTCDGO
       JOIN objetivo o ON o.ENTDCDGO = pr.ENTDCDGO AND TRUNC(g.PGPRFCHA) = o.FECHA_CORRIDA
       WHERE (g.PGPRANUL IS NULL OR g.PGPRANUL = 0)) AS TOTAL_CRUZADO_PGPR,
    (SELECT SUM(ABS(a.APRTVLRR))
       FROM CRD.APRT a
       JOIN objetivo o ON o.ENTDCDGO = a.ENTDCDGO AND TRUNC(a.APRTFCTR) = o.PGPCFCHA
       WHERE a.TPAPCDGO = 23 AND a.APRTTPMV = 4) AS TOTAL_CRUZADO_APRT
FROM DUAL;

-- COMO SE LEE: TOTAL_CRUZADO_PGPR (lo que muestra el reporte) y TOTAL_CRUZADO_APRT (el
-- lado del aporte 23 consumido) tienen que coincidir -- son las dos caras del mismo cruce
-- (pagarConAportes/consumirAportes graba los dos juntos, en la misma transaccion). Si
-- difieren, algo se aplico a un prestamo sin descontar el aporte correspondiente o
-- viceversa -- revisar con prioridad, seria un defecto de integridad del cruce, no del
-- reporte.
