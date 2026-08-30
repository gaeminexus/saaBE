package com.saa.ejb.cxc.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.AnticipoCliente;

import jakarta.ejb.Local;

@Local
public interface AnticipoClienteDaoService extends EntityDao<AnticipoCliente> {

	/**
	 * Anticipos de un cliente que todavía tienen saldo para cruzar:
	 * Confirmados, con valor positivo y saldo disponible mayor a cero,
	 * ordenados del más antiguo al más nuevo (FIFO).
	 * <p>
	 * Es la lista que alimenta la pantalla de cruce: el usuario elige de aquí
	 * de qué anticipo sale el dinero de cada abono.
	 * @param idTitular  : Id del cliente
	 * @param idEmpresa  : Id de la empresa contable
	 * @return           : Anticipos con saldo, del más antiguo al más nuevo
	 * @throws Throwable : Excepcion
	 */
	List<AnticipoCliente> selectDisponiblesByTitular(Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Anticipos REALES de un cliente (excluye los movimientos negativos
	 * históricos, estado Migrado), del más nuevo al más antiguo. Alimenta las
	 * pantallas de consulta y seguimiento.
	 * @param idTitular  : Id del cliente
	 * @param idEmpresa  : Id de la empresa contable
	 * @return           : Anticipos del cliente
	 * @throws Throwable : Excepcion
	 */
	List<AnticipoCliente> selectMovimientosByTitular(Long idTitular, Long idEmpresa)
			throws Throwable;

	/**
	 * Suma el saldo disponible de todos los anticipos vigentes de un cliente.
	 * Es el saldo de anticipos "por anticipo", que debe cuadrar con el saldo
	 * global de TSR.PRCC.PRCCSLIN.
	 * @param idTitular  : Id del cliente
	 * @param idEmpresa  : Id de la empresa contable
	 * @return           : Suma de saldos disponibles, 0.0 si no hay anticipos
	 * @throws Throwable : Excepcion
	 */
	Double sumaSaldoDisponible(Long idTitular, Long idEmpresa) throws Throwable;

	/**
	 * Anticipos con una devolución de saldo en curso todavía sin aplicar (ANTCIDPG no nulo,
	 * ANTCAPLC=0). Alimenta el reconciliador {@code AnticipoClienteServiceImpl.sincronizarDevoluciones}.
	 * @return           : Anticipos con devolución pendiente de aplicar
	 * @throws Throwable : Excepcion
	 */
	List<AnticipoCliente> selectConDevolucionPendiente() throws Throwable;
}
