-- ============================================================================
-- 63 - RESOLVER LA CONTRADICCION DEL 1B, MOVER EL INSTRUMENTO A JULIO
-- ==
-- Se corre en PRODUCCION con julio ya CALCULADO y en estado 3, ANTES del ajuste
-- y ANTES de aprobar. El bloque 1 solo lee; del 2 en adelante escribe una fila
-- de dos columnas en la tabla del instrumento. No toca ningun dato de nomina.
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - LA CONTRADICCION, Y ES LA PUERTA: si esto no sale bien, PARAR.
-- ==
-- El reporte de la replica del 2026-08-25 dice DOS COSAS INCOMPATIBLES sobre la
-- provision de fondo de reserva de Viteri en julio:
--   su punto 3  ->  183,26
--   su nota final -> "30,54 sobre 366,67, exacto igual que junio"
-- y la segunda quedo escrita en el guion, en el paragrafo 6.1.
-- ==
-- LO QUE DICE EL CODIGO ES 183,26 SOBRE BASE 2 200, y no admite otra lectura:
-- el prorrateo es del MES DEL ANIVERSARIO. Viteri cumplio el 25-06, asi que
-- junio le dio 5 dias -base 366,67- y julio le da el mes entero:
--   inicioFr = 26-06, que ya quedo ANTES del 01-07
--   dias = 30 - 1 + 1 = 30, factor = 1, base = 2 200 -> 183,26
-- ==
-- ESPERADO: UNA fila, mes 7, VITERI, base 2 200,00, valor 183,26.
--   Si sale 183,26 -> el motor esta bien y hay que CORREGIR EL GUION.
--   Si sale  30,54 -> es un defecto del motor y julio NO se aprueba: el
--                     prorrateo se estaria arrastrando a un mes que no toca.
-- Y si aparece una segunda persona, tambien PARAR: Rodriguez Valencia esta en
-- modalidad 1 y su fondo va al rol, no a provision.
-- ============================================================================
SELECT p.PRDNMSEE AS MES, e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       v.PVNMBSCL AS BASE, v.PVNMVLOR AS VALOR
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = v.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND v.PVNMTPPR = 4;

-- ============================================================================
-- BLOQUE 2 - El periodo que se va a contrastar.
-- ESPERADO: PRDN 62, ano 2026, mes 7, estado 3 CALCULADO, 20 nominas.
-- ============================================================================
SELECT p.PRDNCDGO AS PRDN, p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES,
       p.PRDNESTD AS ESTADO, COUNT(n.NMNACDGO) AS NOMINAS,
       p.PRDNTTIN AS INGRESOS, p.PRDNTTDS AS DESCUENTOS, p.PRDNTTNT AS NETO
  FROM RHH.PRDN p
  LEFT JOIN RHH.NMNA n ON n.PRDNCDGO = p.PRDNCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
 GROUP BY p.PRDNCDGO, p.PRDNANOO, p.PRDNMSEE, p.PRDNESTD,
          p.PRDNTTIN, p.PRDNTTDS, p.PRDNTTNT;

-- ============================================================================
-- BLOQUE 3 - Mover el instrumento a julio.
-- Hoy esta en 2026 . 6, que es JUNIO: cerrado, contrastado y cuadrado. Correr
-- el contraste sin mover esto daria un junio impecable con aspecto de julio
-- perfecto, y nada lo delataria.
-- ============================================================================
SELECT ANIO, MES FROM RHH.CTRL_PARAM;

UPDATE RHH.CTRL_PARAM SET ANIO = 2026, MES = 7;
COMMIT;

SELECT ANIO, MES, COUNT(*) OVER () AS FILAS_EN_LA_TABLA FROM RHH.CTRL_PARAM;

-- ============================================================================
-- BLOQUE 4 - Ahora si, el instrumento: CONTRASTE_MES_CONTRA_ROL_REAL.sql
-- ==
-- ORDEN DE LECTURA: bloque 4 primero, luego 3, luego 1 y 2, y el 1B con sus
-- DOS consultas. PERIODO_LEIDO = 2026-07 en cada bloque antes de mirar ninguna
-- cifra.
-- ==
-- ESPERADO EN JULIO, y dos avisos que no estaban en los meses anteriores:
--   BLOQUE 4 : 20/20, PERIODO_LEIDO 2026-07, estado 3.
--   BLOQUE 3 : VACIO, Y NO ES UN CUADRE. Julio no tiene planilla del IESS
--              cargada -REF-06 §11-, asi que ese bloque esta APAGADO. Un vacio
--              que parece un exito. NO se lee como verde.
--   BLOQUE 1 : 17 filas. 3 sueldo . 2 fondo de reserva . 3 vacaciones .
--              3 aporte . 1 anticipo de Calderon . 5 otros descuentos.
--   BLOQUE 1B: UNA sola persona en la provision de fondos de reserva.
--   BLOQUE 2 : el liquido del periodo en +31,43 sobre el del cliente.
-- ==
-- SOLO SI EL BLOQUE 2 DA +31,43 se aplica el AJUSTE-JULIO-2026.md. El ajuste
-- esta calculado sobre ese numero: aplicarlo sobre otro lo empeora.
-- ============================================================================
