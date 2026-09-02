/* ============================================================================
   lap1-05  QUE MOVIMIENTOS TIENEN LAS 5 FACTURAS QUE HAY QUE RECARGAR
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxp
   ============================================================================

   SOLO LECTURA. Ni un INSERT, ni un UPDATE, ni DDL.

   PARA QUE SIRVE
   --------------
   Anular una factura de compra NO es gratis. Si tiene pagos, retenciones o
   anticipos cruzados, el endpoint responde 409 y no hace nada; solo procede
   con anularEnCascada=true, que REVERSA todos esos movimientos.

   Antes de anular las 5 del reporte hay que saber cual tiene que, porque
   reversar el pago de una factura para corregir UN CENTAVO puede no valer la
   pena — y esa es una decision del usuario, no tecnica.

   Las 5 facturas son las de REPORTE-FACTURAS-DESCUADRADAS-2026-09-02.md §2.
   Las ids 166 y 138 NO estan aca a proposito: su cuenta por pagar ya es
   correcta (§3 del reporte) y no hay que tocarlas.
   ============================================================================ */


/* ============================================================================
   BLOQUE 1 · Estado de las 5 facturas
   ----------------------------------------------------------------------------
   QUE MIRAR: ESTADOEMISION. Si alguna ya esta anulada (3), esa parte del
   trabajo ya esta hecha. FCTCEPAG dice el estado de pago.
   ============================================================================ */
SELECT f.ID              AS id_factura,
       f.NUMERO          AS numero,
       f.SUBTOTAL        AS subtotal_cab,
       f.VIVA            AS iva_cab,
       f.TOTAL           AS importe_total,
       f.ESTADO          AS estado,
       f.ESTADOEMISION   AS estado_emision,
       f.FCTCEPAG        AS estado_pago
  FROM PGS.FCTC f
 WHERE f.ID IN (118, 120, 121, 446, 447)
 ORDER BY f.ID;


/* ============================================================================
   BLOQUE 2 · Movimientos activos aplicados a cada una
   ----------------------------------------------------------------------------
   Es lo mismo que consulta GET /fctc/movimientosRelacionados/{id} antes de
   ofrecer la anulacion en cascada.

   QUE MIRAR:
     - CERO filas para una factura  -> se puede anular sin cascada, sin costo.
     - UNA O MAS filas              -> anularla REVERSA esos movimientos.
                                       Decidir si vale la pena por un centavo.

   APLPTDPG es el tipo de documento de pago de la aplicacion.
   ============================================================================ */
SELECT a.APLPFCTC   AS id_factura,
       a.APLPCDGO   AS id_aplicacion,
       a.APLPTDPG   AS tipo_doc_pago,
       a.APLPMAPL   AS monto_aplicado,
       a.APLPFPAG   AS fecha_aplicacion,
       a.APLPREFR   AS referencia,
       a.APLPESTD   AS estado_aplicacion
  FROM PGS.APLP a
 WHERE a.APLPFCTC IN (118, 120, 121, 446, 447)
   AND a.APLPESTD = 1
 ORDER BY a.APLPFCTC, a.APLPCDGO;


/* ============================================================================
   BLOQUE 3 · Resumen: cuales se pueden anular sin tocar nada
   ----------------------------------------------------------------------------
   Una linea por factura, con cuantos movimientos activos tiene y cuanto suman.

   MOVIMIENTOS = 0  ->  anulacion limpia
   MOVIMIENTOS > 0  ->  la anulacion arrastra esos movimientos
   ============================================================================ */
SELECT f.ID                                  AS id_factura,
       f.NUMERO                              AS numero,
       f.TOTAL                               AS importe_total,
       COUNT(a.APLPCDGO)                     AS movimientos_activos,
       NVL(SUM(a.APLPMAPL), 0)               AS total_aplicado,
       CASE WHEN COUNT(a.APLPCDGO) = 0 THEN 'ANULACION LIMPIA'
            ELSE 'ARRASTRA MOVIMIENTOS - DECIDIR'
       END                                   AS veredicto
  FROM PGS.FCTC f
  LEFT JOIN PGS.APLP a ON a.APLPFCTC = f.ID AND a.APLPESTD = 1
 WHERE f.ID IN (118, 120, 121, 446, 447)
 GROUP BY f.ID, f.NUMERO, f.TOTAL
 ORDER BY f.ID;


/* ============================================================================
   NO HAY NADA QUE EJECUTAR ACA
   ----------------------------------------------------------------------------
   Este script no modifica nada y no lleva reverso porque no hay nada que
   reversar. La anulacion se hace desde la pantalla, NO con SQL: pasa por
   POST /fctc/anular/{id}, que ademas anula el asiento contable. Un UPDATE a
   mano sobre FCTC dejaria el asiento vivo y la factura anulada.
   ============================================================================ */
