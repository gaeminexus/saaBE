package com.saa.ejb.cxp.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.FacturaCompra;

import jakarta.ejb.Local;

@Local
public interface AplicacionPagoCxpDaoService extends EntityDao<AplicacionPagoCxp> {

	/**
	 * Recupera las aplicaciones ACTIVAS de una factura de compra.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Listado de aplicaciones activas
	 * @throws Throwable      : Excepcion
	 */
	List<AplicacionPagoCxp> selectActivasByFactura(Long idFacturaCompra) throws Throwable;

	/**
	 * Recupera TODAS las aplicaciones de una factura de compra, activas y
	 * reversadas, para mostrar el historial completo.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Listado de aplicaciones
	 * @throws Throwable      : Excepcion
	 */
	List<AplicacionPagoCxp> selectByFactura(Long idFacturaCompra) throws Throwable;

	/**
	 * Suma los montos aplicados ACTIVOS de una factura de compra.
	 * Las notas de débito se guardan con monto negativo, por lo que la suma ya
	 * refleja el incremento del saldo.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Total aplicado, 0.0 si no hay aplicaciones
	 * @throws Throwable      : Excepcion
	 */
	Double sumaAplicadoByFactura(Long idFacturaCompra) throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS de una liquidación de compra.
	 * Equivalente de {@link #selectActivasByFactura(Long)} para el documento
	 * afectado alternativo (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md).
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @return                    : Listado de aplicaciones activas
	 * @throws Throwable          : Excepcion
	 */
	List<AplicacionPagoCxp> selectActivasByLiquidacion(Long idLiquidacionCompra) throws Throwable;

	/**
	 * Recupera TODAS las aplicaciones de una liquidación de compra, activas y
	 * reversadas. Equivalente de {@link #selectByFactura(Long)}.
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @return                    : Listado de aplicaciones
	 * @throws Throwable          : Excepcion
	 */
	List<AplicacionPagoCxp> selectByLiquidacion(Long idLiquidacionCompra) throws Throwable;

	/**
	 * Suma los montos aplicados ACTIVOS de una liquidación de compra.
	 * Equivalente de {@link #sumaAplicadoByFactura(Long)}.
	 * @param idLiquidacionCompra : Id de la liquidación de compra
	 * @return                    : Total aplicado, 0.0 si no hay aplicaciones
	 * @throws Throwable          : Excepcion
	 */
	Double sumaAplicadoByLiquidacion(Long idLiquidacionCompra) throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS generadas por un documento concreto.
	 * @param tipoDocumento : RETENCION, RETENCION_V2, NOTA_CREDITO o NOTA_DEBITO
	 * @param idDocumento   : Id del documento que originó la aplicación
	 * @return              : Listado de aplicaciones activas del documento
	 * @throws Throwable    : Excepcion
	 */
	List<AplicacionPagoCxp> selectActivasByDocumento(String tipoDocumento, Long idDocumento) throws Throwable;

	/**
	 * Busca una factura de compra por su número de documento, para resolver los
	 * documentos que la referencian solo por número (retenciones y notas).
	 * Compara normalizando el número (sin guiones), de modo que
	 * '001-001-000000123' y '001001000000123' se consideran el mismo documento.
	 * @param numeroDocumento : Número del documento tal como viene en el otro documento
	 * @param idTitular       : Id del proveedor emisor de la factura
	 * @param idEmpresa       : Id de la empresa
	 * @return                : Listado de facturas que coinciden (normalmente 0 ó 1)
	 * @throws Throwable      : Excepcion
	 */
	List<FacturaCompra> selectFacturaByNumero(String numeroDocumento, Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * ÍTEM 17 (2026-09-15, docs/logica-negocio/tsr/AUDITORIA-ESTADO-CUENTA-TITULAR.md P2): igual
	 * criterio que {@link #selectFacturaByNumero}, pero sobre {@code CBR.LQCS}
	 * ({@code com.saa.model.cxc.LiquidacionCompra}) — la liquidación de compra EMITIDA (recibida
	 * electrónicamente), no {@code PGS.LQCC}. Una retención con {@code tipoDocReten='03'} nombra el
	 * número de la LQCS; su {@code documentoCxp} (si existe) es la {@code LiquidacionCompraCompra}
	 * contra la que se cruza de verdad.
	 * @param numeroDocumento : Número del documento tal como viene en el otro documento
	 * @param idTitular       : Id del proveedor emisor de la liquidación
	 * @param idEmpresa       : Id de la empresa (vía {@code facturador.empresa}, la LQCS no tiene
	 *                          empresa propia)
	 * @return                : Listado de liquidaciones que coinciden (normalmente 0 ó 1)
	 * @throws Throwable      : Excepcion
	 */
	List<com.saa.model.cxc.LiquidacionCompra> selectLiquidacionEmitidaByNumero(String numeroDocumento,
			Long idTitular, Long idEmpresa) throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS de tipo ANTICIPO (cruces del saldo de
	 * anticipos) de un proveedor en una empresa, de la más reciente a la más
	 * antigua.
	 * <p>
	 * El cruce de anticipo no se enlaza al anticipo original sino al movimiento
	 * negativo que deja en PGS.ANTP, así que para saber si un anticipo concreto
	 * fue cruzado hay que mirar los cruces del proveedor y contrastarlos contra
	 * el saldo global de anticipos. El orden descendente permite reversarlos en
	 * LIFO: primero el cruce más reciente.
	 * @param idTitular  : Id del proveedor
	 * @param idEmpresa  : Id de la empresa; null para no filtrar
	 * @return           : Listado de cruces activos, del más reciente al más antiguo
	 * @throws Throwable : Excepcion
	 */
	List<AplicacionPagoCxp> selectCrucesAnticipoActivos(Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Cruces registrados contra un anticipo concreto (FK APLPANTO).
	 * <p>
	 * Es la consulta exacta que reemplaza a la heurística de
	 * {@link #selectCrucesAnticipoActivos(Long, Long)}: desde 2026-08-20 cada
	 * cruce sabe de qué anticipo salió, así que anular un anticipo ya no tiene
	 * que adivinar qué abonos deshacer.
	 * @param idAnticipo  : Id del anticipo de origen
	 * @param soloActivas : true para excluir los cruces ya reversados
	 * @return            : Cruces del anticipo, del más reciente al más antiguo
	 * @throws Throwable  : Excepcion
	 */
	List<AplicacionPagoCxp> selectCrucesByAnticipoOrigen(Long idAnticipo, boolean soloActivas)
			throws Throwable;

	// =====================================================================
	// Cartera por pagar (docs/logica-negocio/cxp/API-CARTERA-CXP-CXC.md §3.2, P1-P4)
	// =====================================================================

	/**
	 * P1 (facturas): documentos VIGENTES de {@code FacturaCompra} (factura y nota de venta, según
	 * {@code tipoComprobante}) a una fecha de corte, proyección escalar -- nunca la entidad
	 * completa, el grafo EAGER ya tumbó la base con ORA-04036 (§2.5).
	 * <p>
	 * Columnas del {@code Object[]}: 0 id, 1 tipoComprobante, 2 numEstablecimiento,
	 * 3 numPtoEmision, 4 secuencial, 5 fecha, 6 total, 7 esIntermediario, 8 titular.codigo,
	 * 9 titular.identificacion, 10 titular.razonSocial, 11 titular.nombre.
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte -- {@code fecha}
	 *                          de {@code FacturaCompra} es {@code LocalDateTime}, se filtra con
	 *                          {@code fecha < corteMasUnDia}
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas de facturas/notas de venta vigentes, ordenadas por titular y fecha
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectCarteraFacturasCompra(Long idEmpresa, LocalDateTime corteMasUnDia, Long idTitular)
			throws Throwable;

	/**
	 * P1 (liquidaciones): equivalente de {@link #selectCarteraFacturasCompra} para
	 * {@code LiquidacionCompraCompra}. Sin {@code esIntermediario}: esa marca solo existe en
	 * {@code FCTC}.
	 * <p>
	 * Columnas del {@code Object[]}: 0 id, 1 tipoComprobante, 2 numEstablecimiento,
	 * 3 numPtoEmision, 4 secuencial, 5 fecha, 6 total, 7 titular.codigo, 8 titular.identificacion,
	 * 9 titular.razonSocial, 10 titular.nombre.
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas de liquidaciones vigentes, ordenadas por titular y fecha
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectCarteraLiquidacionesCompra(Long idEmpresa, LocalDateTime corteMasUnDia, Long idTitular)
			throws Throwable;

	/**
	 * P2 (facturas): suma de {@code montoAplicado} agrupada por documento y {@code tipoDocPago},
	 * de las aplicaciones ACTIVAS aplicadas hasta la fecha de corte, sobre facturas que cumplen
	 * las MISMAS condiciones de {@link #selectCarteraFacturasCompra} (join, nunca {@code in :ids}
	 * -- §2.5). Sin agregación por titular: el {@code group by} ya es por documento.
	 * <p>
	 * Columnas del {@code Object[]}: 0 facturaCompra.id, 1 tipoDocPago, 2 sum(montoAplicado).
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte (condición del documento)
	 * @param fechaCorte      : Fecha de corte -- {@code fechaAplicacion <= fechaCorte}
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas (documento, tipo, suma)
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectAplicacionesCarteraFacturaCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
			LocalDate fechaCorte, Long idTitular) throws Throwable;

	/**
	 * P2 (liquidaciones): equivalente de {@link #selectAplicacionesCarteraFacturaCompra} para
	 * {@code liquidacionCompra}.
	 * <p>
	 * Columnas del {@code Object[]}: 0 liquidacionCompra.id, 1 tipoDocPago, 2 sum(montoAplicado).
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte
	 * @param fechaCorte      : Fecha de corte
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas (documento, tipo, suma)
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectAplicacionesCarteraLiquidacionCompra(Long idEmpresa, LocalDateTime corteMasUnDia,
			LocalDate fechaCorte, Long idTitular) throws Throwable;

	/**
	 * P3 (facturas): plazo y unidad de tiempo declarados en {@code FormaPagoFacturaCompra}, uno
	 * por fila (un documento puede tener varias formas de pago con distinto plazo -- el mayor lo
	 * decide el llamador, §3.4.1). Mismas condiciones de {@link #selectCarteraFacturasCompra} vía
	 * join sobre {@code p.factura}.
	 * <p>
	 * Columnas del {@code Object[]}: 0 factura.id, 1 plazo, 2 unidadTiempo.
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas (documento, plazo, unidad)
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectPlazosCarteraFacturaCompra(Long idEmpresa, LocalDateTime corteMasUnDia, Long idTitular)
			throws Throwable;

	/**
	 * P3 (liquidaciones): equivalente de {@link #selectPlazosCarteraFacturaCompra} para
	 * {@code FormaPagoLiquidacionCompraCompra}.
	 * <p>
	 * Columnas del {@code Object[]}: 0 liquidacion.id, 1 plazo, 2 unidadTiempo.
	 * @param idEmpresa       : Id de la empresa
	 * @param corteMasUnDia   : Medianoche del día siguiente a la fecha de corte
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas (documento, plazo, unidad)
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> selectPlazosCarteraLiquidacionCompra(Long idEmpresa, LocalDateTime corteMasUnDia, Long idTitular)
			throws Throwable;

	/**
	 * P4: saldo disponible de anticipos por proveedor, misma condición que
	 * {@code AnticipoProveedorDaoServiceImpl.sumaSaldoDisponible} ({@code estado = CONFIRMADO},
	 * {@code valor > 0}) pero agregada por titular en vez de recibir uno solo -- evita un bucle de
	 * una consulta por proveedor (§2.4).
	 * <p>
	 * Columnas del {@code Object[]}: 0 titular.codigo, 1 sum(saldo).
	 * @param idEmpresa       : Id de la empresa
	 * @param idTitular       : Id del proveedor; null = todos
	 * @return                : Filas (titular, saldo disponible)
	 * @throws Throwable      : Excepcion
	 */
	List<Object[]> sumaSaldoDisponibleAnticiposPorTitular(Long idEmpresa, Long idTitular) throws Throwable;
}
