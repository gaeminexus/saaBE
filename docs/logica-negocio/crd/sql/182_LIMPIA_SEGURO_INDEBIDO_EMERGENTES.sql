-- =====================================================================================
-- ⛔ LIMPIA EL SEGURO DE INCENDIO INDEBIDO DE LOS DOS PRESTAMOS EMERGENTES
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ ESTE SCRIPT ESCRIBE. Correrlo ANTES de reprocesar la carga 449.
--
-- QUE LO ORIGINA: la carga 449 se traba porque el asiento de aplicacion no tiene cuenta
-- donde poner seguro de incendio de un prestamo EMERGENTE (tipo 1). Un emergente no tiene
-- garantia real: ese dato nunca debio existir.
--
-- ⛔ MEDIDO ANTES DE TOCAR NADA (sql/180, resultado del usuario):
--
--   PRESTAMO  IDASOPREP  SEGURO_EN_CUOTAS  SEGURO_COBRADO  PAGOS_HISTORICOS
--   6381      70843      53,95             0               6
--   6022      70607      21,58             0               22
--
--   SEGURO_COBRADO = 0 EN LOS DOS. Nunca se le cobro a nadie, pese a que entre los dos
--   tienen 28 pagos historicos. Entonces esto es SOLO limpiar un dato: no hay que
--   devolverle plata a ningun participe. Si hubiera dado > 0, este script no alcanzaria.
--
-- ⛔ EL PUNTO QUE HACE QUE ESTO NO SEA UN SIMPLE "PONER EN CERO", y es el error que casi
--    cometo:
--
--   DTPRTTLL (el total de la cuota) INCLUYE el seguro. Esta medido: el sql/170 dio
--   DTPRTTLL - mora = capital + interes + desgravamen + seguro, con una brecha de 0,01 en
--   1.092 cuotas. O sea que el total y sus componentes cuadran hoy.
--
--   Si se pone DTPRVLSI = 0 y NO se baja DTPRTTLL por el mismo importe, el total queda
--   $75,53 por encima de la suma de sus partes — que es EXACTAMENTE el descuadre que se
--   paso el dia entero persiguiendo y cerrando. Se estaria recreando el defecto a mano.
--
--   Por eso el UPDATE toca las DOS columnas, y el control de despues lo verifica.
--
-- ⚠️ DTPRSLDO (saldo) no se toca acá a proposito: lo recalcula el motor al aplicar el
--    proximo pago, y tocarlo a mano sin conocer los pagos previos de cada cuota es
--    arriesgarse a pisar un valor correcto. El control D.3 lo muestra para que quede a la
--    vista si alguna cuota queda con saldo raro.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220


-- =====================================================================================
-- CONTROL ANTES — ⛔ GUARDAR ESTA SALIDA. Es el respaldo en papel del cambio.
-- =====================================================================================

-- A.1 — Las 16 cuotas tal como estan HOY. Deben salir 16 filas (13 del 6381 + 3 del 6022).
--       Si sale otra cantidad, PARAR Y AVISAR: el universo cambio desde la medicion.
SELECT  d.DTPRCDGO                          AS ID_CUOTA,
        d.PRSTCDGO                          AS PRESTAMO,
        d.DTPRNMCT                          AS NRO_CUOTA,
        d.DTPRESTD                          AS ESTADO,
        d.DTPRVLSI                          AS SEGURO_A_QUITAR,
        d.DTPRTTLL                          AS TOTAL_ACTUAL,
        d.DTPRTTLL - NVL(d.DTPRVLSI,0)      AS TOTAL_QUE_QUEDARA,
        d.DTPRSLDO                          AS SALDO_ACTUAL
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (6381, 6022)
AND     NVL(d.DTPRVLSI, 0) > 0
ORDER   BY d.PRSTCDGO, d.DTPRNMCT;

-- A.2 — El total a quitar. Debe dar 16 cuotas y 75,53.
SELECT  COUNT(*)                            AS CUOTAS,
        ROUND(SUM(NVL(d.DTPRVLSI,0)), 2)    AS SEGURO_TOTAL_A_QUITAR
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (6381, 6022)
AND     NVL(d.DTPRVLSI, 0) > 0;

-- A.3 — ⛔ Confirmar que NO se cobro nunca. Debe dar 0 en SEGURO_COBRADO.
--       Si diera > 0, PARAR: hay que devolver plata y este script no es el correcto.
SELECT  g.PRSTCDGO                          AS PRESTAMO,
        COUNT(*)                            AS PAGOS,
        ROUND(SUM(NVL(g.PGPRVLSI,0)), 2)    AS SEGURO_COBRADO
FROM    CRD.PGPR g
WHERE   g.PRSTCDGO IN (6381, 6022)
AND     NVL(g.PGPRANUL, 0) = 0
GROUP   BY g.PRSTCDGO;


-- =====================================================================================
-- RESPALDO — copia de las filas antes de tocarlas. Es lo que permite revertir.
-- Si la tabla ya existiera de una corrida anterior, PARAR: no re-ejecutar a ciegas.
-- =====================================================================================
CREATE TABLE CRD.BAK_182_DTPR AS
SELECT  d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRVLSI, d.DTPRTTLL, d.DTPRSLDO,
        SYSTIMESTAMP AS FECHA_RESPALDO
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (6381, 6022)
AND     NVL(d.DTPRVLSI, 0) > 0;

COMMIT;


-- =====================================================================================
-- EJECUCION — las DOS columnas, en un solo UPDATE
--
-- El orden dentro del SET importa en otros motores; en Oracle no, porque el lado derecho
-- se evalua con los valores PREVIOS a la asignacion. Aun asi se escribe primero el total
-- para que se lea en el orden en que hay que pensarlo.
-- =====================================================================================
UPDATE  CRD.DTPR d
SET     d.DTPRTTLL = NVL(d.DTPRTTLL, 0) - NVL(d.DTPRVLSI, 0),
        d.DTPRVLSI = 0
WHERE   d.PRSTCDGO IN (6381, 6022)
AND     NVL(d.DTPRVLSI, 0) > 0;

-- Debe informar 16 filas actualizadas.
COMMIT;


-- =====================================================================================
-- CONTROL DESPUES — ⛔ SI ESTO NO DA LO ESPERADO, REVERTIR CON EL BLOQUE DEL FINAL
-- =====================================================================================

-- D.1 — Ya no debe quedar seguro en tipos sin cuenta. Debe salir 0 filas.
--       Es la misma consulta del bloque 2 del sql/179, que fue la que encontro el problema.
SELECT  pd.TPPRCDGO                         AS TIPO_PRESTAMO,
        pd.PRDCCDGO                         AS ID_PRODUCTO,
        COUNT(*)                            AS CUOTAS_CON_SEGURO,
        ROUND(SUM(NVL(d.DTPRVLSI,0)), 2)    AS SEGURO_TOTAL
FROM    CRD.DTPR d
JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
WHERE   NVL(d.DTPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
GROUP   BY pd.TPPRCDGO, pd.PRDCCDGO;

-- D.2 — ⛔ EL CONTROL QUE IMPORTA: el total sigue cuadrando con sus componentes.
--       BRECHA debe dar 0 (o centavos de redondeo). Si da ~75,53, el UPDATE bajo el seguro
--       y NO bajo el total: se recreo el descuadre y hay que revertir.
SELECT  d.PRSTCDGO                          AS PRESTAMO,
        COUNT(*)                            AS CUOTAS,
        ROUND(SUM(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0))
            - SUM(NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0)
                + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)), 2) AS BRECHA
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (6381, 6022)
GROUP   BY d.PRSTCDGO;

-- D.3 — Las 16 cuotas despues, contra el respaldo. Para leerlo de un vistazo.
SELECT  b.DTPRCDGO                          AS ID_CUOTA,
        b.PRSTCDGO                          AS PRESTAMO,
        b.DTPRNMCT                          AS NRO_CUOTA,
        b.DTPRVLSI                          AS SEGURO_ANTES,
        d.DTPRVLSI                          AS SEGURO_AHORA,
        b.DTPRTTLL                          AS TOTAL_ANTES,
        d.DTPRTTLL                          AS TOTAL_AHORA,
        ROUND(b.DTPRTTLL - d.DTPRTTLL, 2)   AS BAJO_EL_TOTAL,
        d.DTPRSLDO                          AS SALDO
FROM    CRD.BAK_182_DTPR b
JOIN    CRD.DTPR d ON d.DTPRCDGO = b.DTPRCDGO
ORDER   BY b.PRSTCDGO, b.DTPRNMCT;


-- =====================================================================================
-- REVERSO — comentado a proposito. Descomentar SOLO si el control de arriba falla.
-- Restaura desde el respaldo, no desde valores escritos a mano.
-- =====================================================================================
-- UPDATE CRD.DTPR d
-- SET   (d.DTPRVLSI, d.DTPRTTLL, d.DTPRSLDO) =
--       (SELECT b.DTPRVLSI, b.DTPRTTLL, b.DTPRSLDO
--          FROM CRD.BAK_182_DTPR b WHERE b.DTPRCDGO = d.DTPRCDGO)
-- WHERE d.DTPRCDGO IN (SELECT b2.DTPRCDGO FROM CRD.BAK_182_DTPR b2);
-- COMMIT;
--
-- Y una vez verificado que todo quedo bien y ya no hace falta el respaldo:
-- DROP TABLE CRD.BAK_182_DTPR;
