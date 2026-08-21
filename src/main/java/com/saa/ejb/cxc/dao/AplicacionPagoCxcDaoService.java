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
}
