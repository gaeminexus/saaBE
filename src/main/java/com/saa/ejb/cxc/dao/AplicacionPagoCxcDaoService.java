package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.Factura;

import jakarta.ejb.Local;

@Local
public interface AplicacionPagoCxcDaoService extends EntityDao<AplicacionPagoCxc> {

	/**
	 * Recupera las aplicaciones ACTIVAS de una factura de venta.
	 * @param idFactura  : Id de la factura de venta
	 * @return           : Listado de aplicaciones activas
	 * @throws Throwable : Excepcion
	 */
	List<AplicacionPagoCxc> selectActivasByFactura(Long idFactura) throws Throwable;

	/**
	 * Recupera TODAS las aplicaciones de una factura de venta, activas y
	 * reversadas, para mostrar el historial completo.
	 * @param idFactura  : Id de la factura de venta
	 * @return           : Listado de aplicaciones
	 * @throws Throwable : Excepcion
	 */
	List<AplicacionPagoCxc> selectByFactura(Long idFactura) throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS de una liquidación de compra emitida.
	 * @param idLiquidacion : Id de la liquidación
	 * @return              : Listado de aplicaciones activas
	 * @throws Throwable    : Excepcion
	 */
	List<AplicacionPagoCxc> selectActivasByLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Suma los montos aplicados ACTIVOS de una factura de venta.
	 * Las notas de débito se guardan con monto negativo, por lo que la suma ya
	 * refleja el incremento del saldo.
	 * @param idFactura  : Id de la factura de venta
	 * @return           : Total aplicado, 0.0 si no hay aplicaciones
	 * @throws Throwable : Excepcion
	 */
	Double sumaAplicadoByFactura(Long idFactura) throws Throwable;

	/**
	 * Suma los montos aplicados ACTIVOS de una liquidación de compra emitida.
	 * @param idLiquidacion : Id de la liquidación
	 * @return              : Total aplicado, 0.0 si no hay aplicaciones
	 * @throws Throwable    : Excepcion
	 */
	Double sumaAplicadoByLiquidacion(Long idLiquidacion) throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS generadas por un documento concreto.
	 * @param tipoDocumento : RETENCION, RETENCION_V2, NOTA_CREDITO o NOTA_DEBITO
	 * @param idDocumento   : Id del documento que originó la aplicación
	 * @return              : Listado de aplicaciones activas del documento
	 * @throws Throwable    : Excepcion
	 */
	List<AplicacionPagoCxc> selectActivasByDocumento(String tipoDocumento, Long idDocumento)
			throws Throwable;

	/**
	 * Busca una factura de venta por su número de documento, para resolver los
	 * documentos que la referencian solo por número (retenciones recibidas).
	 * Compara normalizando el número (sin guiones).
	 * @param numeroDocumento : Número del documento tal como viene en el otro documento
	 * @param idTitular       : Id del cliente, null si no se conoce
	 * @param idEmpresa       : Id de la empresa
	 * @return                : Listado de facturas que coinciden (normalmente 0 ó 1)
	 * @throws Throwable      : Excepcion
	 */
	List<Factura> selectFacturaByNumero(String numeroDocumento, Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Recupera las aplicaciones ACTIVAS de tipo ANTICIPO (cruces del saldo de
	 * anticipos) de un cliente en una empresa, de la más reciente a la más
	 * antigua. Incluye los cruces hechos sobre facturas de venta y sobre
	 * liquidaciones de compra emitidas.
	 * <p>
	 * El cruce de anticipo no se enlaza al anticipo original sino al movimiento
	 * negativo que deja en CBR.ANTC, así que para saber si un anticipo concreto
	 * fue cruzado hay que mirar los cruces del cliente y contrastarlos contra el
	 * saldo global de anticipos. El orden descendente permite reversarlos en
	 * LIFO: primero el cruce más reciente.
	 * @param idTitular  : Id del cliente
	 * @param idEmpresa  : Id de la empresa; null para no filtrar
	 * @return           : Listado de cruces activos, del más reciente al más antiguo
	 * @throws Throwable : Excepcion
	 */
	List<AplicacionPagoCxc> selectCrucesAnticipoActivos(Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Cruces registrados contra un anticipo concreto (FK APLCANTO).
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
	List<AplicacionPagoCxc> selectCrucesByAnticipoOrigen(Long idAnticipo, boolean soloActivas)
			throws Throwable;

	/**
	 * Listado de aplicaciones de cobro con los datos ya resueltos para
	 * pantalla (titular, documento afectado, asiento), filtrado
	 * opcionalmente. Cualquier parámetro en null no filtra por ese criterio.
	 * <p>
	 * El documento afectado es factura O liquidación (mutuamente
	 * excluyentes): se exponen ambos pares id/número por separado — igual
	 * que {@code ChequeDaoServiceImpl.selectListado} con
	 * facturaCompra/egreso/anticipo — y el llamador se queda con el que no
	 * sea null. Todos los joins hacia asociaciones opcionales son
	 * {@code left join} EXPLÍCITO: encadenar una navegación implícita sobre
	 * un alias ya left-joined (p.ej. {@code f.titular.nombre} sin volver a
	 * escribir {@code left join}) Hibernate la renderiza como INNER JOIN, y
	 * las filas cuyo lado opuesto (liquidación en vez de factura) esté
	 * activo desaparecen del resultado — el mismo bug que tuvo el listado
	 * de cheques.
	 * <p>
	 * Orden de columnas del {@code Object[]}: 0 id, 1 fechaAplicacion,
	 * 2 idTitularFactura, 3 nombreTitularFactura, 4 idTitularLiquidacion,
	 * 5 nombreTitularLiquidacion, 6 idFactura, 7 numeroFactura,
	 * 8 idLiquidacion, 9 numeroLiquidacion, 10 tipoDocPago, 11 formaPago,
	 * 12 montoAplicado, 13 idAsiento, 14 numeroAlternoAsiento, 15 estado.
	 * @param idEmpresa  : Empresa contable; null = todas
	 * @param idTitular  : Cliente/proveedor (busca en ambos lados, factura y
	 *                     liquidación); null = todos
	 * @param desde      : Fecha de aplicación desde (inclusive); null = sin límite
	 * @param hasta      : Fecha de aplicación hasta (inclusive); null = sin límite
	 * @param formaPago  : 1 Efectivo, 2 Transferencia, 3 Cheque, 4 Tarjeta; null = todas
	 * @param estado     : 1 Activo, 2 Reversado; null = todos
	 * @return           : Filas del listado, más recientes primero
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectListado(Long idEmpresa, Long idTitular, java.time.LocalDate desde,
			java.time.LocalDate hasta, Long formaPago, Long estado) throws Throwable;
}
