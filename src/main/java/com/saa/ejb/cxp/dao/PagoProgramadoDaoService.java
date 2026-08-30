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

	/**
	 * Recupera los pagos POR_APROBAR de una empresa para la bandeja de aprobación (punto 14).
	 * Devuelve las entidades, no la proyección: la arma
	 * {@code PagoProgramadoServiceImpl.porAprobar} porque el mapeo a
	 * {@link com.saa.model.cxp.PagoPorAprobar} depende de cuál de facturaCompra/egreso/
	 * anticipo/origenExterno está no-nulo, algo que no vale la pena forzar en un
	 * {@code select new}.
	 *
	 * @param idEmpresa : Id de la empresa
	 * @param origen    : {@link com.saa.rubros.OrigenPagoCxp} u {@link com.saa.rubros.OrigenPagoExterno};
	 *                    null para todos los orígenes
	 * @param desde     : Fecha programada desde (inclusive); null = sin límite inferior
	 * @param hasta     : Fecha programada hasta (inclusive); null = sin límite superior
	 * @return          : Pagos POR_APROBAR, más antiguos primero
	 * @throws Throwable : Excepcion
	 */
	List<PagoProgramado> selectPorAprobar(Long idEmpresa, String origen,
			java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable;

	/**
	 * Recupera el pago programado vinculado a una aplicación de pago (COBRO_DIRECTO), si lo
	 * hay. Necesario para la anulación en cascada de una factura de compra (ítem 13, 2026-08-28):
	 * {@code PagoProgramadoService.revertirPagoConfirmado} es el único camino que mantiene
	 * sincronizado el estado del {@code PagoProgramado} al reversar su aplicación — reversar la
	 * aplicación directo (sin pasar por aquí) dejaría el pago en CONFIRMADO apuntando a una
	 * aplicación ya REVERSADA.
	 *
	 * @param idAplicacion	: Id de la AplicacionPagoCxp (PGS.APLP.APLPCDGO)
	 * @return				: El pago programado vinculado, o null si la aplicación no vino de
	 *						  un pago directo (nota de crédito/débito/retención/anticipo no
	 *						  tienen PagoProgramado propio)
	 * @throws Throwable	: Excepcion
	 */
	PagoProgramado selectByAplicacion(Long idAplicacion) throws Throwable;

	/**
	 * Suma comprometida de una cuenta bancaria: pagos REGISTRADO o EN_ARCHIVO con
	 * fecha programada hasta la fecha indicada — ya aprobados pero todavía sin
	 * confirmar contra el banco. Imprescindible para la validación de disponibilidad
	 * (PLAN-REDISENO-APROBACION-PAGOS.md §3.3): sin esto, dos aprobaciones seguidas
	 * pasan la validación cada una por su lado y juntas sobregiran la cuenta.
	 *
	 * @param idCuentaBancaria : Id de la cuenta bancaria
	 * @param fecha            : Fecha límite (inclusive) de fechaProgramada
	 * @return                 : Suma de PagoProgramado.valor comprometidos; 0.0 si no hay ninguno
	 * @throws Throwable       : Excepcion
	 */
	Double sumaPagosComprometidos(Long idCuentaBancaria, java.time.LocalDate fecha) throws Throwable;
}
