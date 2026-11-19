package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.FacturaSustentoPendiente;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Resolución del <code>codSustento</code> (Tabla 5 del ATS, ver
 * {@link com.saa.rubros.SustentoTributarioSri}) de una factura de compra.</p>
 *
 * <h3>La regla (decisión de negocio, corregida 2026-08-27, no re-litigar)</h3>
 *
 * <p>El código es del <b>documento</b>, no de la línea, y la regla base la decide el
 * <b>IVA de la factura</b>, no el grupo de producto: <code>FCTC.VIVA &gt; 0</code> → "01"
 * (crédito tributario IVA), si no → "02" (costo/gasto IR). La primera versión de esta regla
 * intentaba resolver por el sustento por defecto del grupo de producto (<code>GRPPCSUS</code>)
 * como caso general, y daba <b>131 de 131 facturas sin resolver</b>: un mismo grupo mezcla
 * líneas con y sin IVA (ej. "Servicios Básicos" trae luz y agua al 0% junto con líneas gravadas),
 * así que ningún código por grupo puede representar eso.</p>
 *
 * <p><code>GRPPCSUS</code> no desapareció: pasó a ser una <b>excepción</b>, consultada sólo para
 * los tres casos que el IVA no decide por sí solo — activo fijo (03/04), inventario (06/07) y
 * reembolso de gasto (08). Si el grupo con <b>mayor base imponible acumulada</b> entre las
 * líneas de la factura tiene uno de esos tres códigos configurado, ese gana sobre la regla base
 * del IVA; si no hay ningún grupo con excepción configurada (el caso de hoy: ninguno la tiene),
 * manda el IVA. El valor se <b>guarda</b> en <code>FacturaCompra.sustentoTributario</code>
 * (FCTCCSUS): no se recalcula al generar el ATS, porque el grupo de producto de una línea pudo
 * cambiar después de emitida la factura.</p>
 *
 * <h3>Nunca se pisa una resolución ni una corrección manual</h3>
 *
 * <p>{@link #resolverSiFalta(FacturaCompra)} sólo actúa cuando
 * <code>sustentoTributario</code> todavía es <code>null</code>. Una vez que la factura tiene un
 * valor —resuelto automáticamente o corregido a mano por {@link #corregirSustento}— ninguna
 * llamada posterior lo vuelve a tocar.</p>
 */
@Local
public interface SustentoTributarioService {

    // ========================================================================
    // C37 / D-126 (2026-11-19): el centro de costo entra en la regla base.
    //
    //   1. Excepcion por grupo de producto (03/04/06/07/08): igual que antes, gana.
    //   2. Sin IVA                                   -> 02, sea cual sea el centro.
    //   3. Con IVA y centro de costo en el asiento   -> 01 si el centro esta en el rubro
    //      Rubros.CXP_CENTROS_COSTO_CREDITO_TRIBUTARIO (251; hoy 02 ASOPREP ARRIENDOS), si no 02
    //      (01 ASOPREP ADMINISTRACION y cualquier otro).
    //   4. Con IVA y sin centro, o sin el rubro      -> 01 (la regla del IVA de antes) + aviso.
    //
    // El centro sale de las lineas del asiento del documento (CNT.DTAS.CNCSCDGO): es el unico
    // lugar donde el SAA lo guarda (la cabecera FCTC/LQCC/NTCC/NTDC no tiene centro). Si hay mas
    // de uno, decide el de mayor valor y se avisa. Por eso el sustento se resuelve DESPUES de que
    // el asiento tiene su centro (carga historica C11) o, si todavia no lo tiene, cae al paso 4.
    // El parrafo "La regla" de arriba describe la version del 2026-08-27 (pasos 1, 2 y 4).
    // ========================================================================

    /** Tipos de documento de {@link #evaluarSustento(String, Long)}: los de &lt;compras&gt; del ATS. */
    String FACTURA = "FACTURA";
    String LIQUIDACION = "LIQUIDACION";
    String NOTA_CREDITO = "NOTA_CREDITO";
    String NOTA_DEBITO = "NOTA_DEBITO";

    /**
     * Aplica la regla completa (de arriba) a un documento de compra, SIN guardar nada, y dice que
     * parte de la regla decidio, el centro de costo que se uso y el aviso si hay que revisar.
     *
     * @param tipoDocumento	: {@link #FACTURA}, {@link #LIQUIDACION}, {@link #NOTA_CREDITO} o {@link #NOTA_DEBITO}
     * @param idDocumento	: Id del documento
     * @return				: El resultado; null si el documento no existe
     * @throws Throwable	: IncomeException si el tipo no es uno de los cuatro
     */
    com.saa.model.cxp.ResultadoSustento evaluarSustento(String tipoDocumento, Long idDocumento) throws Throwable;

    /**
     * Control de solo lectura: aplica la regla a todos los documentos de compra activos de un
     * rango (mismo criterio de fecha que el ATS: fecha de registro contable, o la de emision) y
     * compara con lo guardado. No cambia nada: para corregir, un SQL revisable (regla 8) o
     * {@code PUT /fctc/sustento/{id}}.
     *
     * @param idEmpresa		: Empresa
     * @param desde			: Primer dia del rango
     * @param hasta			: Ultimo dia del rango
     * @return				: documentos, coinciden, sinSustento, resumen (tipo | regla | guardado -> regla),
     *						  diferencias y avisos (listas de {@link com.saa.model.cxp.ResultadoSustento})
     * @throws Throwable	: Excepcion
     */
    Map<String, Object> verificarPeriodo(Long idEmpresa, java.time.LocalDate desde, java.time.LocalDate hasta)
            throws Throwable;

    /**
     * Calcula, sin guardar nada, el sustento que le correspondería hoy a una factura de compra
     * ya registrada, aplicando la regla de arriba: excepción por grupo de producto si aplica,
     * si no el IVA de la factura.
     *
     * @param idFactura		: Id de la factura de compra (FCTC.ID)
     * @return				: El código de sustento (Tabla 5); null sólo si la factura no existe
     *						  (con la regla del IVA, una factura existente siempre resuelve)
     * @throws Throwable	: Excepcion
     */
    String calcularSustento(Long idFactura) throws Throwable;

    /**
     * Resuelve y guarda el sustento de una factura de compra, SOLO si todavía no tiene uno
     * asignado.
     *
     * @param factura		: Factura de compra ya persistida (debe tener id)
     * @return				: El código ya asignado a la factura (el que tenía, o el recién
     *						  resuelto), o null si sigue sin poder determinarse
     * @throws Throwable	: Excepcion
     */
    String resolverSiFalta(FacturaCompra factura) throws Throwable;

    /**
     * Corrección manual: fija el sustento de una factura de compra a un código concreto,
     * sin importar el que tuviera antes (resuelto o no). Valida el código contra el catálogo
     * vigente en <code>PGS.LSRI</code>/<code>PGS.TSRI</code>
     * (<code>LSRI.TABLA='{@value com.saa.rubros.SustentoTributarioSri#LSRI_TABLA}'</code>,
     * <code>TSRI.ESTADO=1</code>) — no contra una lista redeclarada en Java.
     *
     * @param idFactura		: Id de la factura de compra
     * @param sustento		: Código de sustento (Tabla 5), dos dígitos
     * @return				: La factura ya actualizada
     * @throws Throwable	: IncomeException si la factura no existe o el código no es válido/vigente
     */
    FacturaCompra corregirSustento(Long idFactura, String sustento) throws Throwable;

    /**
     * Facturas de compra activas cuyo sustento sigue sin resolver: la lista que hay que repasar
     * antes de generar el primer ATS.
     *
     * <p>Devuelve una <b>proyección</b> ({@link FacturaSustentoPendiente}), no la entidad
     * {@link FacturaCompra} completa — serializar la entidad arrastra <code>empresa</code> con
     * su jerarquía, <code>asiento</code>, mayorización y el resto del grafo (medido: 536 KB para
     * 131 facturas). Cada fila incluye <code>sustentoSugerido</code>
     * ({@link #calcularSustento(Long)}, sin guardar) para que el frontend no reimplemente la
     * regla.</p>
     *
     * @param idEmpresa		: Empresa a filtrar; null = todas
     * @return				: Facturas pendientes, más recientes primero
     * @throws Throwable	: Excepcion
     */
    List<FacturaSustentoPendiente> listarPendientes(Long idEmpresa) throws Throwable;

    /**
     * Catálogo vigente de la Tabla 5 (código → descripción), leído de
     * <code>PGS.LSRI</code>/<code>PGS.TSRI</code>. Para que la pantalla de corrección manual
     * pueda ofrecer un combo sin mantener una copia de la lista en el frontend ni en Java.
     *
     * @return				: Mapa código → descripción, en el orden del código
     * @throws Throwable	: Excepcion
     */
    Map<String, String> catalogoVigente() throws Throwable;

    // ========================================================================
    // Extensión a LQCC/NTCC/NTDC (2026-08-28) — misma regla de calcularSustento
    // (excepción por grupo de producto, si no regla base por IVA del documento),
    // trasladada a las otras tres tablas de <compras> del ATS. Ver
    // docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §6.5 punto 2 y §9.
    // ========================================================================

    /**
     * Igual que {@link #calcularSustento(Long)} pero para una liquidación de compra.
     * @param idLiquidacion	: Id de la liquidación de compra (PGS.LQCC.ID)
     * @return				: El código de sustento (Tabla 5); null sólo si no existe
     * @throws Throwable	: Excepcion
     */
    String calcularSustentoLiquidacion(Long idLiquidacion) throws Throwable;

    /**
     * Igual que {@link #resolverSiFalta(FacturaCompra)} pero para una liquidación de compra.
     * @param liquidacion	: Liquidación de compra ya persistida (debe tener id)
     * @return				: El código ya asignado, o null si sigue sin poder determinarse
     * @throws Throwable	: Excepcion
     */
    String resolverSiFaltaLiquidacion(LiquidacionCompraCompra liquidacion) throws Throwable;

    /**
     * Igual que {@link #corregirSustento(Long, String)} pero para una liquidación de compra.
     * @param idLiquidacion	: Id de la liquidación de compra
     * @param sustento		: Código de sustento (Tabla 5), dos dígitos
     * @return				: La liquidación ya actualizada
     * @throws Throwable	: IncomeException si no existe o el código no es válido/vigente
     */
    LiquidacionCompraCompra corregirSustentoLiquidacion(Long idLiquidacion, String sustento) throws Throwable;

    /**
     * Igual que {@link #listarPendientes(Long)} pero para liquidaciones de compra.
     * @param idEmpresa		: Empresa a filtrar; null = todas
     * @return				: Liquidaciones pendientes, más recientes primero
     * @throws Throwable	: Excepcion
     */
    List<FacturaSustentoPendiente> listarPendientesLiquidacion(Long idEmpresa) throws Throwable;

    /**
     * Igual que {@link #calcularSustento(Long)} pero para una nota de crédito de compra.
     * @param idNotaCredito	: Id de la nota de crédito de compra (PGS.NTCC.ID)
     * @return				: El código de sustento (Tabla 5); null sólo si no existe
     * @throws Throwable	: Excepcion
     */
    String calcularSustentoNotaCredito(Long idNotaCredito) throws Throwable;

    /**
     * Igual que {@link #resolverSiFalta(FacturaCompra)} pero para una nota de crédito de compra.
     * @param notaCredito	: Nota de crédito de compra ya persistida (debe tener id)
     * @return				: El código ya asignado, o null si sigue sin poder determinarse
     * @throws Throwable	: Excepcion
     */
    String resolverSiFaltaNotaCredito(NotaCreditoCompra notaCredito) throws Throwable;

    /**
     * Igual que {@link #corregirSustento(Long, String)} pero para una nota de crédito de compra.
     * @param idNotaCredito	: Id de la nota de crédito de compra
     * @param sustento		: Código de sustento (Tabla 5), dos dígitos
     * @return				: La nota de crédito ya actualizada
     * @throws Throwable	: IncomeException si no existe o el código no es válido/vigente
     */
    NotaCreditoCompra corregirSustentoNotaCredito(Long idNotaCredito, String sustento) throws Throwable;

    /**
     * Igual que {@link #listarPendientes(Long)} pero para notas de crédito de compra.
     * @param idEmpresa		: Empresa a filtrar; null = todas
     * @return				: Notas de crédito pendientes, más recientes primero
     * @throws Throwable	: Excepcion
     */
    List<FacturaSustentoPendiente> listarPendientesNotaCredito(Long idEmpresa) throws Throwable;

    /**
     * Igual que {@link #calcularSustento(Long)} pero para una nota de débito de compra.
     *
     * <p><b>Sin excepción por grupo de producto</b>: {@code DetalleNotaDebitoCompra} no tiene
     * ninguna columna de producto (verificado 2026-08-28 — a diferencia de
     * {@code DetalleFacturaCompra}/{@code DetalleNotaCreditoCompra}, que sí tienen un
     * {@code producto} aunque sea un {@code Long} plano). Siempre resuelve por la regla base
     * del IVA del documento. Si en el futuro se agrega esa columna, este método necesita el
     * mismo paso de excepción que los demás.</p>
     *
     * @param idNotaDebito	: Id de la nota de débito de compra (PGS.NTDC.ID)
     * @return				: El código de sustento (Tabla 5); null sólo si no existe
     * @throws Throwable	: Excepcion
     */
    String calcularSustentoNotaDebito(Long idNotaDebito) throws Throwable;

    /**
     * Igual que {@link #resolverSiFalta(FacturaCompra)} pero para una nota de débito de compra.
     * @param notaDebito	: Nota de débito de compra ya persistida (debe tener id)
     * @return				: El código ya asignado, o null si sigue sin poder determinarse
     * @throws Throwable	: Excepcion
     */
    String resolverSiFaltaNotaDebito(NotaDebitoCompra notaDebito) throws Throwable;

    /**
     * Igual que {@link #corregirSustento(Long, String)} pero para una nota de débito de compra.
     * @param idNotaDebito	: Id de la nota de débito de compra
     * @param sustento		: Código de sustento (Tabla 5), dos dígitos
     * @return				: La nota de débito ya actualizada
     * @throws Throwable	: IncomeException si no existe o el código no es válido/vigente
     */
    NotaDebitoCompra corregirSustentoNotaDebito(Long idNotaDebito, String sustento) throws Throwable;

    /**
     * Igual que {@link #listarPendientes(Long)} pero para notas de débito de compra.
     * @param idEmpresa		: Empresa a filtrar; null = todas
     * @return				: Notas de débito pendientes, más recientes primero
     * @throws Throwable	: Excepcion
     */
    List<FacturaSustentoPendiente> listarPendientesNotaDebito(Long idEmpresa) throws Throwable;

}
