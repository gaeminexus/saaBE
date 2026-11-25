-- C24 / D-053 — La cuenta 4.8.99.08 GND POR AJUSTES en la estructura B17 del motor (CNT.CRRP, reporte 90).
-- Sin esta fila el B17 que genera el SAA no reporta el saldo de la cuenta (la ecuación 1 − 2 − 3 − (5 − 4) queda descuadrada
-- por los centavos de redondeo). Va al renglón 489090 (OTROS), no al 4890 de sus hermanas 4.8.99.01–07, para que el padre
-- 4890 siga siendo la suma de sus hijas. VA TAMBIÉN A PRODUCCIÓN (PASE-A-PRODUCCION.md §4). Idempotente. Sin COMMIT.

-- 1 Antes: hermanas y la cuenta (esperado: 7 filas de 4.8.99.01–07 al 4890; ninguna de 4.8.99.08)
SELECT CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPESTD FROM CNT.CRRP WHERE CRRPCNTA LIKE '4.8.99.%' ORDER BY CRRPCNTA;

-- 2 Alta
INSERT INTO CNT.CRRP (CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPFCDS, CRRPFCHS, CRRPOBSR, CRRPESTD)
SELECT CNT.SQ_CRRPCDGO.NEXTVAL, h.RPRTCDGO, '4.8.99.08', h.CRRPALCN, '489090', h.CRRPMDSG, h.CRRPRPRT, h.CRRPFCDS, h.CRRPFCHS,
       'D-053: GND por ajustes (centavos de redondeo) al renglon 489090 OTROS', 1
FROM CNT.CRRP h
WHERE h.CRRPCNTA = '4.8.99.07' AND NOT EXISTS (SELECT 1 FROM CNT.CRRP x WHERE x.CRRPCNTA = '4.8.99.08' AND x.RPRTCDGO = h.RPRTCDGO);

-- 3 Después (esperado: 1 fila, renglón 489090, activa)
SELECT CRRPCDGO, RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPESTD FROM CNT.CRRP WHERE CRRPCNTA = '4.8.99.08';
