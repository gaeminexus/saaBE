/* ============================================================================
   lap1-09  MARCAR COMO INTERMEDIARIO UNA FACTURA YA REGISTRADA
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxp
   ============================================================================

   ⚠️ ESTE MODIFICA DATOS. Y hay un ORDEN que no se puede alterar.

   EL CASO
   -------
   Una factura se registro ANTES de que existiera la opcion de intermediario, y
   su asiento salio con las cuentas mal: tomo detalles, IVA y grupos de producto
   en vez de mandar el total a la cuenta del grupo del producto correcto.

   POR QUE NO ALCANZA CON RECONTABILIZAR SOLO
   ------------------------------------------
   recontabilizarDocumento regenera el asiento llamando a
   generarAsientoFacturaCompra, que decide la rama mirando FCTCESIN. Esa factura
   tiene FCTCESIN = 0, asi que recontabilizar **regeneraria exactamente el mismo
   asiento equivocado**. Primero hay que marcarla.

   POR QUE NO SE PUEDE REVERSAR NI RECARGAR
   ----------------------------------------
   Reversar BORRA la factura, y hay un PagoProgramado cuya FK apunta a ella. Esa
   FK es excluyente con egreso y anticipo: ponerla en nulo dejaria al pago sin
   ninguna de las tres y rompe su modelo. El bloqueo es deliberado y correcto.
   Marcar + recontabilizar deja el pago intacto y arregla lo unico que estaba
   mal, que es el asiento.

   ⛔ ORDEN OBLIGATORIO — los tres pasos, en este orden
   ----------------------------------------------------
     1. DESPLEGAR EL WAR con las facturas de intermediario. Sin el, el generador
        no tiene la rama y recontabilizar vuelve a hacer lo mismo.
        (El DDL lap1-08 ya esta corrido.)
     2. Correr ESTE script.
     3. Desde la pantalla: ANULAR CONTABILIDAD y despues RECONTABILIZAR.

   Saltarse el paso 1 no rompe nada, pero no arregla nada: se recontabiliza
   igual de mal y hay que repetir todo.
   ============================================================================ */


/* ============================================================================
   BLOQUE 0 · Encontrar la factura y elegir el producto
   ----------------------------------------------------------------------------
   Reemplazar el numero por el de la factura a corregir. Anotar el ID_FACTURA
   que devuelva: es el que va en el UPDATE del BLOQUE 2.

   QUE MIRAR: ES_INTERMEDIARIO tiene que decir 0. Si ya dice 1, la factura ya
   esta marcada y solo falta recontabilizar.
   ============================================================================ */
SELECT f.ID              AS id_factura,
       f.NUMERO          AS numero,
       f.CLAVE           AS clave_acceso,
       t.TTLRNMBR        AS proveedor,
       f.TOTAL           AS importe_total,
       f.FCTCESIN        AS es_intermediario,
       f.FCTCPRIN        AS producto_intermediario,
       f.ESTADOEMISION   AS estado_emision
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.NUMERO = '<NUMERO_O_CLAVE>'
    OR f.CLAVE  = '<NUMERO_O_CLAVE>';


/* ============================================================================
   BLOQUE 1 · Elegir el producto al que se va a contabilizar
   ----------------------------------------------------------------------------
   Solo sirven los productos que tienen grupo Y cuenta contable: el generador
   falla ruidoso si falta cualquiera de las dos. Esta consulta solo lista los
   que sirven, para no elegir uno que despues rebote.

   Anotar el ID_PRODUCTO elegido.
   ============================================================================ */
SELECT p.ID          AS id_producto,
       p.NOMBRE      AS producto,
       g.GRPPNMBR    AS grupo,
       c.PLNNCNTA    AS cuenta_contable,
       c.PLNNNMBR    AS nombre_cuenta
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN c ON c.PLNNCDGO = g.PLNNCDGO
 ORDER BY g.GRPPNMBR, p.NOMBRE;


/* ============================================================================
   BLOQUE 2 · La marca
   ----------------------------------------------------------------------------
   Reemplazar <ID_FACTURA> y <ID_PRODUCTO> por los dos valores anotados.

   El WHERE incluye FCTCESIN = 0 como salvaguarda: si alguien ya la marco, este
   UPDATE no toca nada en vez de pisar una eleccion previa.
   ============================================================================ */
UPDATE PGS.FCTC
   SET FCTCESIN = 1,
       FCTCPRIN = <ID_PRODUCTO>
 WHERE ID = <ID_FACTURA>
   AND NVL(FCTCESIN, 0) = 0;

COMMIT;


/* ============================================================================
   BLOQUE 3 · CONTROL DESPUES — antes de recontabilizar
   ----------------------------------------------------------------------------
   Esperado: ES_INTERMEDIARIO = 1, y CUENTA_DESTINO con la cuenta a la que va a
   ir el total. **Leer esa cuenta y confirmar que es la correcta ANTES de
   recontabilizar** — despues del asiento, corregirla es otra vuelta completa.
   ============================================================================ */
SELECT f.ID           AS id_factura,
       f.NUMERO       AS numero,
       f.TOTAL        AS importe_total,
       f.FCTCESIN     AS es_intermediario,
       p.NOMBRE       AS producto,
       g.GRPPNMBR     AS grupo,
       c.PLNNCNTA     AS cuenta_destino,
       c.PLNNNMBR     AS nombre_cuenta
  FROM PGS.FCTC f
  JOIN PGS.PRDP p ON p.ID = f.FCTCPRIN
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  JOIN CNT.PLNN c ON c.PLNNCDGO = g.PLNNCDGO
 WHERE f.ID = <ID_FACTURA>;


/* ============================================================================
   DESPUES DE ESTE SCRIPT — desde la PANTALLA, no por SQL
   ----------------------------------------------------------------------------
     1. ANULAR CONTABILIDAD del documento
     2. RECONTABILIZAR

   ⛔ NO tocar el asiento con SQL. Esos dos procesos anulan el asiento viejo
   dejando su rastro y generan el nuevo con su numeracion; un UPDATE a mano
   dejaria el asiento viejo vivo y la factura apuntando a otro.

   El asiento nuevo debe tener exactamente DOS lineas: el total al DEBE en la
   cuenta del BLOQUE 3, y el mismo total al HABER en la CxP del proveedor. Sin
   linea de IVA y sin una linea por grupo.

   REVERSO · COMENTADO
   -------------------
   Solo tiene sentido ANTES de recontabilizar. Despues, deshacer la marca sin
   volver a recontabilizar deja la factura con un asiento de intermediario y la
   marca apagada, que es peor que cualquiera de los dos estados.

   -- UPDATE PGS.FCTC SET FCTCESIN = 0, FCTCPRIN = NULL WHERE ID = <ID_FACTURA>;
   -- COMMIT;
   ============================================================================ */
