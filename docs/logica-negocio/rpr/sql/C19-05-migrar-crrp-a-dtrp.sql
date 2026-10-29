-- =====================================================================================
-- C19-05 — MIGRACION en la base de trabajo (saa-oracle-23ai), donde ya corrieron las versiones anteriores de
--          C19-01..03 (tabla CNT.CRRP de un "motor paralelo"): pasa las reglas del B17 y del flujo de caja al
--          DETALLE DEL MOTOR DE BALANCES (CNT.DTRP) y retira CNT.CRRP. Decision D-031 (2026-10-29).
-- Cuadre Superintendencia, tarea C19b · Diseño: docs/logica-negocio/rpr/ESTRUCTURAS-DE-RENGLONES.md
--
-- Estado verificado el 2026-10-29 (solo SELECT): CNT.RPRT ya tiene RPRTTPRP..RPRTTTLO; reportes 90 B17 (alterno 6,
-- 534 renglones, 33 reglas en CRRP) y 91 FLC (alterno 7, 26 renglones, 12 reglas en CRRP). CNT.RNRP se QUEDA: es el
-- detalle de renglones del reporte del motor.
-- En produccion NO se corre este script: alli van C19-01 (version final) + C19-02 + C19-03 + C19-04.
--
-- ⛔ ORDEN: BLOQUES 2-3 (DDL, commit implicito) -> BLOQUE 4 (INSERT, SIN COMMIT) -> BLOQUE 5 (controles) ->
--    COMMIT manual de quien lo corre -> BLOQUE 6 (DROP de CRRP, DDL). Desplegar el WAR de C19b despues del BLOQUE 3.
-- SQL PURO (sin SET/DEFINE/WHENEVER). NO SE HA EJECUTADO.
-- =====================================================================================

-- BLOQUE 1 — CONTROL ANTES
-- 1.1 Reglas a migrar. ESPERADO: B17 33, FLC 12.
SELECT r.RPRTCDES, COUNT(*) reglas FROM CNT.CRRP c JOIN CNT.RPRT r ON r.RPRTCDGO = c.RPRTCDGO WHERE c.CRRPESTD = 1 GROUP BY r.RPRTCDES;
-- 1.2 Los reportes de renglones no tienen filas en DTRP. ESPERADO: 0.
SELECT COUNT(*) FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO WHERE r.RPRTTPRP = 2;
-- 1.3 Columnas nuevas aun no creadas. ESPERADO: 0 filas.
SELECT column_name FROM all_tab_columns WHERE owner = 'CNT' AND ((table_name = 'DTRP' AND column_name LIKE 'DTRP%' AND column_name NOT IN ('DTRPCDGO', 'DTRPSGNO'))
    OR (table_name = 'RPRT' AND column_name = 'RPRTNVMX'));

-- BLOQUE 2 — DETALLE DEL REPORTE (CNT.DTRP): columnas de las reglas cuenta -> renglon
ALTER TABLE CNT.DTRP ADD (
  DTRPCDRN VARCHAR2(30),       -- renglon destino (CNT.RNRP.RNRPCDRN); NULL = mapeo automatico del reporte
  DTRPALCN NUMBER(1),          -- alcance: 1 la cuenta y sus subcuentas · 2 solo la cuenta exacta
  DTRPMDSG NUMBER(1),          -- signo: 1 naturaleza · 2 deudor · 3 acreedor · 4 valor absoluto por cuenta
  DTRPRPRT NUMBER(1),          -- 1 se reporta · 0 excluida a proposito
  DTRPFCDS DATE,               -- vigencia desde
  DTRPFCHS DATE,               -- vigencia hasta
  DTRPOBSR VARCHAR2(500)       -- sustento
);

-- BLOQUE 3 — nivel de profundidad del reporte
ALTER TABLE CNT.RPRT ADD (RPRTNVMX NUMBER(2));

-- BLOQUE 4 — MIGRACION CRRP -> DTRP (sin COMMIT). NUMCTADS = cuenta, PLNNCDDS/NMBCTADS = su cuenta del plan.
INSERT INTO CNT.DTRP (DTRPCDGO, RPRTCDGO, PLNNCDDS, NUMCTADS, NMBCTADS, PLNNCDHS, NUMCTAHS, NMBCTAHS, DTRPSGNO,
                      DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR)
SELECT CNT.SQ_DTRPCDGO.NEXTVAL, c.RPRTCDGO, p.PLNNCDGO, c.CRRPCNTA, p.PLNNNMBR, NULL, NULL, NULL, 1,
       c.CRRPCDRN, c.CRRPALCN, c.CRRPMDSG, c.CRRPRPRT, c.CRRPFCDS, c.CRRPFCHS, c.CRRPOBSR
  FROM CNT.CRRP c
  JOIN CNT.RPRT r ON r.RPRTCDGO = c.RPRTCDGO
  LEFT JOIN CNT.PLNN p ON p.PJRQCDGO = r.PJRQCDGO AND p.PLNNCNTA = c.CRRPCNTA
 WHERE c.CRRPESTD = 1;

-- BLOQUE 5 — CONTROL DESPUES (antes del COMMIT)
-- 5.1 ESPERADO: B17 33, FLC 12.
SELECT r.RPRTCDES, COUNT(*) reglas FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO WHERE r.RPRTTPRP = 2 GROUP BY r.RPRTCDES;
-- 5.2 Lo migrado es exactamente lo de CRRP (en los dos sentidos). ESPERADO: 0 filas en cada consulta.
SELECT RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPFCDS, CRRPFCHS, CRRPOBSR FROM CNT.CRRP WHERE CRRPESTD = 1
MINUS
SELECT d.RPRTCDGO, d.NUMCTADS, d.DTRPALCN, d.DTRPCDRN, d.DTRPMDSG, d.DTRPRPRT, d.DTRPFCDS, d.DTRPFCHS, d.DTRPOBSR
  FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO WHERE r.RPRTTPRP = 2;
SELECT d.RPRTCDGO, d.NUMCTADS, d.DTRPALCN, d.DTRPCDRN, d.DTRPMDSG, d.DTRPRPRT, d.DTRPFCDS, d.DTRPFCHS, d.DTRPOBSR
  FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO WHERE r.RPRTTPRP = 2
MINUS
SELECT RPRTCDGO, CRRPCNTA, CRRPALCN, CRRPCDRN, CRRPMDSG, CRRPRPRT, CRRPFCDS, CRRPFCHS, CRRPOBSR FROM CNT.CRRP WHERE CRRPESTD = 1;
-- 5.3 Reglas sin cuenta del plan. ESPERADO: 0 filas.
SELECT d.RPRTCDGO, d.NUMCTADS FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO WHERE r.RPRTTPRP = 2 AND d.PLNNCDDS IS NULL;
-- 5.4 Los balances de siempre no cambian: ninguna fila de reportes tipo NULL/1 tiene datos de regla. ESPERADO: 0.
SELECT COUNT(*) FROM CNT.DTRP d JOIN CNT.RPRT r ON r.RPRTCDGO = d.RPRTCDGO
 WHERE NVL(r.RPRTTPRP, 1) = 1 AND (d.DTRPCDRN IS NOT NULL OR d.DTRPALCN IS NOT NULL OR d.DTRPMDSG IS NOT NULL OR d.DTRPRPRT IS NOT NULL);

-- >>> COMMIT manual aqui, solo si los controles del BLOQUE 5 dieron lo esperado. <<<

-- BLOQUE 6 — RETIRO DEL MOTOR PARALELO (DDL; solo despues del COMMIT y con el WAR de C19b, que ya no lee CRRP)
-- DROP TABLE CNT.CRRP;
-- DROP SEQUENCE CNT.SQ_CRRPCDGO;

-- REVERSO (antes del BLOQUE 6)
-- DELETE FROM CNT.DTRP WHERE RPRTCDGO IN (SELECT RPRTCDGO FROM CNT.RPRT WHERE RPRTTPRP = 2);
-- ALTER TABLE CNT.RPRT DROP (RPRTNVMX);
-- ALTER TABLE CNT.DTRP DROP (DTRPCDRN, DTRPALCN, DTRPMDSG, DTRPRPRT, DTRPFCDS, DTRPFCHS, DTRPOBSR);
