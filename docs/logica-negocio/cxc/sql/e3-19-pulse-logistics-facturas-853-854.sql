-- =====================================================================================
-- e3-19 — PULSE LOGISTICS: facturas 001-001-000000853 y 000000854 (emisión 04-11-2026) — SOLO LECTURA
-- Equipo omen-saa-3 · 2026-11-11 · Correo de Contabilidad «Error de duplicidad facturación Cliente PULSE LOGISTICS»
--
-- Qué se quiere demostrar: que son DOS emisiones distintas hechas desde la pantalla, no una copia hecha por el sistema.
-- Por qué el código lo garantiza (FacturaServiceImpl):
--   * Cada emisión toma un secuencial NUEVO de CBR.NUMERACIONPUNTOEMISION (obtenerSecuencial: lee numActual y lo
--     incrementa) y arma una clave de acceso NUEVA. Ningún proceso copia una factura existente a otro secuencial.
--   * El reintento y la consulta de estado trabajan sobre la MISMA factura (mismo ID, misma clave); nunca crean otra.
--   * El asiento de una factura se genera al autorizarse, o MÁS TARDE si la autorización llegó después
--     (consultarYActualizarEstadoFactura / reintentarAutorizacion / «Contabilizar»). Eso explica una factura con
--     emisión 04-11 y asiento de otro día: no es una factura nueva, es el asiento atrasado de la misma.
-- Ojo con lo que esperamos ver: la 853 tiene número MENOR (se emitió PRIMERO) y el asiento MAYOR (0145, se contabilizó
-- después). Si la Q1/Q3 lo confirman, la 853 quedó pendiente de autorización, se emitió de nuevo (854) y más tarde se
-- autorizó/contabilizó la 853.
-- =====================================================================================

-- Q1 — Las dos facturas: números, claves de acceso y autorizaciones DISTINTAS, mismo cliente, mismo total.
SELECT f.ID, f.NUMERO, f.SECUENCIAL, f.FECHA AS FECHA_EMISION,
       f.CLAVE AS CLAVE_ACCESO, f.AUTORIZACION, f.FECHAAUTORIZACION,
       f.ESTADO, f.ESTADOEMISION, f.TOTAL, f.USUARIO,
       t.TTLRIDNT AS RUC_CLIENTE, NVL(t.TTLRRZSC, t.TTLRNMBR) AS CLIENTE,
       f.ASIENTO, f.OBSERVACION
  FROM CBR.FCTR f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE f.NUMERO IN ('001-001-000000853', '001-001-000000854')
 ORDER BY f.SECUENCIAL;

-- Q2 — Detalle de cada una: mismos productos y valores = la usuaria cargó dos veces el mismo contenido.
SELECT f.NUMERO, d.ID AS ID_DETALLE, d.PRODUCTO, d.DESCRIPCION, d.CANTIDAD, d.VALOR,
       d.SUBTOTAL, d.VALORIVA, d.TOTAL
  FROM CBR.FCTR f
  JOIN CBR.DTFC d ON d.FACTURA = f.ID
 WHERE f.NUMERO IN ('001-001-000000853', '001-001-000000854')
 ORDER BY d.DESCRIPCION, f.NUMERO;

-- Q3 — Los dos asientos: cuándo se grabó cada uno (ASNTFCPR = fecha y hora de ingreso) y quién.
SELECT f.NUMERO AS FACTURA, a.ASNTCDGO, a.ASNTNMAL AS NUMERO_ASIENTO,
       a.ASNTFCHA AS FECHA_ASIENTO, a.ASNTFCPR AS GRABADO_EL, a.ASNTUSRO AS USUARIO_ASIENTO,
       a.ASNTESTD AS ESTADO_ASIENTO, a.ASNTOBSR AS GLOSA
  FROM CBR.FCTR f
  JOIN CNT.ASNT a ON a.ASNTCDGO = f.ASIENTO
 WHERE f.NUMERO IN ('001-001-000000853', '001-001-000000854')
 ORDER BY a.ASNTFCPR;

-- Q4 — La secuencia del punto de emisión alrededor de esas dos: los números son correlativos y cada uno es una
-- emisión con su propia clave (puede haber huecos si el SRI rechazó la recepción, pero nunca un número repetido).
SELECT f.ID, f.NUMERO, f.FECHA AS FECHA_EMISION, f.FECHAAUTORIZACION, f.ESTADO,
       NVL(t.TTLRRZSC, t.TTLRNMBR) AS CLIENTE, f.TOTAL, f.USUARIO
  FROM CBR.FCTR f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE f.PTOEMISION = (SELECT PTOEMISION FROM CBR.FCTR WHERE NUMERO = '001-001-000000853')
   AND TO_NUMBER(f.SECUENCIAL) BETWEEN 848 AND 860
 ORDER BY TO_NUMBER(f.SECUENCIAL);

-- Q5 — Ningún secuencial repetido en todo el punto de emisión (esperado: ninguna fila).
SELECT f.PTOEMISION, f.SECUENCIAL, COUNT(*) AS VECES
  FROM CBR.FCTR f
 GROUP BY f.PTOEMISION, f.SECUENCIAL
HAVING COUNT(*) > 1;

-- Q6 — Facturas de PULSE LOGISTICS en noviembre (esperado: las dos, y ninguna otra igual).
SELECT f.ID, f.NUMERO, f.FECHA AS FECHA_EMISION, f.FECHAAUTORIZACION, f.ESTADO, f.TOTAL, f.ASIENTO
  FROM CBR.FCTR f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE UPPER(NVL(t.TTLRRZSC, t.TTLRNMBR)) LIKE '%PULSE%'
   AND f.FECHA >= DATE '2026-10-01'
 ORDER BY f.SECUENCIAL;
