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
}
