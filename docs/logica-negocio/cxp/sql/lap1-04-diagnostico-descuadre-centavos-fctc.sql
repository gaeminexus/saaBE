/* ============================================================================
   lap1-04  DESCUADRE DE CENTAVOS AL CARGAR FACTURAS DE COMPRA DESDE XML
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxp
   ============================================================================

   QUE ES ESTO
   -----------
   SOLO LECTURA. Ni un INSERT, ni un UPDATE, ni DDL. Seguro de correr de
   corrido en local y en produccion.

   PARA QUE SIRVE
   --------------
   Mide el descuadre ANTES de tocar codigo, y confirma que la cuenta contable
   4.8.90.90.35 existe. Sin estos numeros no se sabe cuantas facturas estan
   afectadas ni de cuanto es la diferencia real.

   HAY DOS DESCUADRES DISTINTOS Y NO SON EL MISMO
   ----------------------------------------------
     (1) DETALLE vs CABECERA:  SUM(DFCC.SUBTOTAL) != FCTC.SUBTOTAL
         El asiento arma el DEBE del gasto sumando los DETALLES, no el total
         de cabecera. Si no coinciden, el gasto queda contabilizado por un
         valor distinto al que dice la factura.

     (2) CABECERA CONSIGO MISMA:  FCTC.SUBTOTAL + FCTC.VIVA != FCTC.TOTAL
         Es el que reporto el usuario: el propio XML trae un importeTotal que
         no es exactamente subtotal + impuestos.

   El BLOQUE 2 mide (1), el BLOQUE 3 mide (2) y el BLOQUE 4 los cruza.

   POR QUE NADIE LO NOTO
   ---------------------
   El asiento NO usa importeTotal en ningun momento:
     - DEBE gasto  = suma de los DETALLES agrupados por grupo de producto
     - DEBE IVA    = valor de CABECERA (FacturaCompra.vIVA)
     - HABER CxP   = suma de las lineas DEBE, redondeada
                     (AsientoContableServiceImpl:2406-2412)
   Como el HABER se calcula sumando el DEBE, el asiento SIEMPRE cuadra. No
   falla nunca: simplemente registra una cuenta por pagar que no es la que
   dice la factura. Es la misma familia que ya esta registrada en el proyecto
   — un asiento mal clasificado y cuadrado igual no se detecta solo.
   ============================================================================ */


/* ============================================================================
   BLOQUE 1 · La cuenta 4.8.90.90.35 tiene que existir
   ----------------------------------------------------------------------------
   QUE MIRAR: tiene que devolver UNA fila, con PLNNESTD activo. Si devuelve
   cero, hay que crearla en el plan de cuentas ANTES de tocar codigo — el
   proceso no puede mandar la diferencia a una cuenta que no existe.

   Se busca tambien por descripcion por si esta cargada con otro codigo.
   ============================================================================ */
SELECT p.PLNNCDGO AS id_cuenta,
       p.PLNNCNTA AS cuenta_contable,
       p.PLNNNMBR AS nombre,
       p.PLNNTPOO AS tipo,
       p.PLNNNVLL AS nivel,
       p.PLNNESTD AS estado
  FROM CNT.PLNN p
 WHERE p.PLNNCNTA = '4.8.90.90.35'
    OR UPPER(p.PLNNNMBR) LIKE '%REDONDEO%';


/* ============================================================================
   BLOQUE 2 · Descuadre DETALLE vs CABECERA  (el que afecta el DEBE del gasto)
   ----------------------------------------------------------------------------
   QUE MIRAR: cuantas facturas tienen la suma de sus detalles distinta del
   subtotal de cabecera, y de cuanto es la diferencia mayor.

   Si DIFERENCIA sale siempre en centavos (|dif| <= 0.05) es redondeo del
   emisor. Si aparece algo mas grande, NO es redondeo y hay que mirarlo aparte
   antes de mandarlo a la cuenta de diferencias.
   ============================================================================ */
SELECT COUNT(*)                                   AS facturas_descuadradas,
       MIN(ROUND(f.SUBTOTAL - d.suma_detalle, 2)) AS diferencia_minima,
       MAX(ROUND(f.SUBTOTAL - d.suma_detalle, 2)) AS diferencia_maxima,
       SUM(ABS(ROUND(f.SUBTOTAL - d.suma_detalle, 2))) AS suma_absoluta
  FROM PGS.FCTC f
  JOIN (SELECT FACTURA, SUM(SUBTOTAL) AS suma_detalle
          FROM PGS.DFCC
         WHERE ESTADO = 1
         GROUP BY FACTURA) d ON d.FACTURA = f.ID
 WHERE f.ESTADO = 1
   AND ABS(ROUND(f.SUBTOTAL - d.suma_detalle, 2)) >= 0.01;


/* ============================================================================
   BLOQUE 3 · Descuadre de la CABECERA consigo misma
   ----------------------------------------------------------------------------
   Es el que reporto el usuario: SUBTOTAL + VIVA != TOTAL en el propio XML.

   ⚠️ OJO AL INTERPRETARLO: una diferencia aca NO siempre es redondeo. El
   importeTotal del SRI puede incluir legitimamente ICE y propina, que no
   estan en SUBTOTAL + VIVA. Si aparecen diferencias grandes, revisar esas
   facturas una por una antes de meterlas en la regla — el BLOQUE 5 las lista.
   ============================================================================ */
SELECT COUNT(*)                                                    AS facturas,
       SUM(CASE WHEN ABS(dif) <= 0.05 THEN 1 ELSE 0 END)           AS son_centavos,
       SUM(CASE WHEN ABS(dif) >  0.05 THEN 1 ELSE 0 END)           AS mas_que_centavos,
       MIN(dif)                                                    AS diferencia_minima,
       MAX(dif)                                                    AS diferencia_maxima
  FROM (SELECT ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2) AS dif
          FROM PGS.FCTC f
         WHERE f.ESTADO = 1
           AND f.TOTAL IS NOT NULL)
 WHERE ABS(dif) >= 0.01;


/* ============================================================================
   BLOQUE 4 · Las dos diferencias juntas, factura por factura
   ----------------------------------------------------------------------------
   Las 100 peores. Es la lista con la que se decide la regla: si las dos
   columnas de diferencia son siempre de centavos, la correccion propuesta
   aplica; si hay casos grandes, esos NO son redondeo.

   DIF_DETALLE  = cabecera menos suma de detalles  (afecta el DEBE del gasto)
   DIF_CABECERA = total menos (subtotal + IVA)     (lo que reporto el usuario)
   ============================================================================ */
SELECT * FROM (
    SELECT f.ID                                              AS id_factura,
           f.NUMERO                                          AS numero,
           f.CLAVE                                           AS clave_acceso,
           f.SUBTOTAL                                        AS subtotal_cab,
           d.suma_detalle                                    AS subtotal_detalles,
           ROUND(f.SUBTOTAL - d.suma_detalle, 2)             AS dif_detalle,
           f.VIVA                                            AS iva_cab,
           f.TOTAL                                           AS total_cab,
           ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2) AS dif_cabecera
      FROM PGS.FCTC f
      LEFT JOIN (SELECT FACTURA, SUM(SUBTOTAL) AS suma_detalle
                   FROM PGS.DFCC
                  WHERE ESTADO = 1
                  GROUP BY FACTURA) d ON d.FACTURA = f.ID
     WHERE f.ESTADO = 1
       AND (ABS(ROUND(NVL(f.SUBTOTAL,0) - NVL(d.suma_detalle,0), 2)) >= 0.01
        OR  ABS(ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2)) >= 0.01)
     ORDER BY ABS(ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2)) DESC,
              ABS(ROUND(NVL(f.SUBTOTAL,0) - NVL(d.suma_detalle,0), 2)) DESC
) WHERE ROWNUM <= 100;


/* ============================================================================
   BLOQUE 5 · Los casos que NO son redondeo — mirarlos aparte
   ----------------------------------------------------------------------------
   Diferencia de cabecera mayor a 5 centavos. Candidatos a ICE, propina o a un
   defecto distinto. NO deben entrar en la regla de la cuenta de diferencias
   sin mirarlos: mandar a "diferencia por redondeo" algo que es ICE seria
   esconder un error de clasificacion detras de una cuenta de ajuste.

   Esperado: cero filas. Si hay, hay que decidir caso por caso.
   ============================================================================ */
SELECT f.ID       AS id_factura,
       f.NUMERO   AS numero,
       f.CLAVE    AS clave_acceso,
       f.SUBTOTAL AS subtotal_cab,
       f.VIVA     AS iva_cab,
       f.TOTAL    AS total_cab,
       ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2) AS dif_cabecera
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.TOTAL IS NOT NULL
   AND ABS(ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2)) > 0.05
 ORDER BY ABS(ROUND(f.TOTAL - (NVL(f.SUBTOTAL,0) + NVL(f.VIVA,0)), 2)) DESC;


/* ============================================================================
   NO HAY NADA QUE EJECUTAR ACA
   ----------------------------------------------------------------------------
   Este script no modifica nada y no lleva reverso porque no hay nada que
   reversar. La correccion de codigo y, si hace falta, el alta de la cuenta
   4.8.90.90.35 van en un script aparte una vez leidos estos numeros.
   ============================================================================ */
