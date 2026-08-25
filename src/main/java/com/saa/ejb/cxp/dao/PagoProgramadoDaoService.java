package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.PagoProgramado;

import jakarta.ejb.Local;

@Local
public interface PagoProgramadoDaoService extends EntityDao<PagoProgramado> {

	/**
	 * Recupera los pagos programados de una empresa, filtrando opcionalmente por
	 * estado y por proveedor.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : Estado del pago, null para todos
	 * @param idTitular  : Id del proveedor, null para todos
	 * @return           : Listado de pagos programados
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectByEmpresaEstado(Long idEmpresa, Long estado, Long idTitular)
			throws Throwable;

	/**
	 * Recupera los pagos incluidos en un lote.
	 * @param idLote     : Id del lote
	 * @return           : Listado de pagos del lote
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectByLote(Long idLote) throws Throwable;

	/**
	 * Recupera los pagos de una factura que siguen vigentes (registrados, en
	 * archivo o confirmados). Sirve para no comprometer más valor del que la
	 * factura debe.
	 * @param idFacturaCompra : Id de la factura de compra
	 * @return                : Listado de pagos vigentes
	 * @throws Throwable      : Excepcion
	 */
	List<PagoProgramado> selectVigentesByFactura(Long idFacturaCompra) throws Throwable;

	/**
	 * Recupera los pagos de un egreso de tesorería que siguen vigentes
	 * (registrados, en archivo o confirmados). Un egreso solo admite un pago
	 * vigente a la vez.
	 * @param idEgreso   : Id del egreso (TSR.EGRS)
	 * @return           : Listado de pagos vigentes del egreso
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectVigentesByEgreso(Long idEgreso) throws Throwable;

	/**
	 * Recupera los pagos de un anticipo a proveedor que siguen vigentes
	 * (registrados, en archivo o confirmados). Un anticipo solo admite un pago
	 * vigente a la vez.
	 * @param idAnticipo : Id del anticipo (PGS.ANTP)
	 * @return           : Listado de pagos vigentes del anticipo
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectVigentesByAnticipo(Long idAnticipo) throws Throwable;

	/**
	 * Recupera los pagos de un documento de ORIGEN EXTERNO que siguen vigentes
	 * (registrados, en archivo o confirmados). Un documento origen solo admite un
	 * pago vigente a la vez.
	 * <p>
	 * El par (origen, idOrigen) es un dato OPACO para CXP: aqui solo se compara, nunca
	 * se resuelve contra el modulo que lo produjo.
	 * @param origen     : Etiqueta del proceso origen (ver com.saa.rubros.OrigenPagoExterno)
	 * @param idOrigen   : Id del documento en el modulo origen
	 * @return           : Listado de pagos vigentes de ese documento
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectVigentesByOrigen(String origen, Long idOrigen) throws Throwable;

	/**
	 * Recupera varios pagos por sus identificadores.
	 * @param ids        : Identificadores de los pagos
	 * @return           : Listado de pagos encontrados
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectByIds(List<Long> ids) throws Throwable;
}
