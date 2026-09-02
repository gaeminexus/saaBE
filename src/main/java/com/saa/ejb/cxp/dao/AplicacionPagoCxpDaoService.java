package com.saa.ejb.cxp.dao;

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
}
