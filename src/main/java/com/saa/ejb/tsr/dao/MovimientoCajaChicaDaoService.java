package com.saa.ejb.tsr.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.MovimientoCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * Dao Service MovimientoCajaChica.
 */
@Local
public interface MovimientoCajaChicaDaoService extends EntityDao<MovimientoCajaChica> {

	/**
	 * Listado de movimientos de una caja, con filtros opcionales.
	 * @param idCaja  : Id de la caja chica
	 * @param desde   : Fecha desde (null = sin límite inferior)
	 * @param hasta   : Fecha hasta (null = sin límite superior)
	 * @param tipo    : Tipo (rubro TipoMovimientoCajaChica), null para todos
	 * @param estado  : Estado (rubro EstadoMovimientoCajaChica), null para todos
	 * @return        : Movimientos ordenados por fecha, código
	 * @throws Throwable : Excepcion
	 */
	List<MovimientoCajaChica> selectByCaja(Long idCaja, LocalDate desde, LocalDate hasta,
			Long tipo, Long estado) throws Throwable;

	/**
	 * Suma de valores activos agrupada por tipo, en un rango de fechas opcional.
	 * Es la base para calcular el saldo de la caja (fuera de rango) y los
	 * totales de un periodo de cierre (con rango).
	 * @param idCaja : Id de la caja chica
	 * @param desde  : Fecha desde (inclusive), null = sin límite inferior
	 * @param hasta  : Fecha hasta (inclusive), null = sin límite superior
	 * @return       : Filas [tipo (Long), suma (Double)] solo de movimientos ACTIVOS
	 * @throws Throwable : Excepcion
	 */
	List<Object[]> selectSumasPorTipo(Long idCaja, LocalDate desde, LocalDate hasta) throws Throwable;

	/**
	 * Fecha del movimiento más antiguo (activo o no) de la caja.
	 * @param idCaja : Id de la caja chica
	 * @return       : Fecha, o null si la caja no tiene movimientos
	 * @throws Throwable : Excepcion
	 */
	LocalDate selectFechaPrimerMovimiento(Long idCaja) throws Throwable;

	/**
	 * Movimientos ACTIVOS de la caja en un rango de fechas (inclusive), para
	 * marcarlos con el cierre.
	 * @param idCaja : Id de la caja chica
	 * @param desde  : Fecha desde
	 * @param hasta  : Fecha hasta
	 * @return       : Movimientos activos del periodo
	 * @throws Throwable : Excepcion
	 */
	List<MovimientoCajaChica> selectActivosEnRango(Long idCaja, LocalDate desde, LocalDate hasta)
			throws Throwable;

	/**
	 * Movimientos marcados con un cierre determinado (para desmarcarlos al anular el cierre).
	 * @param idCierre : Id del cierre
	 * @return         : Movimientos del cierre
	 * @throws Throwable : Excepcion
	 */
	List<MovimientoCajaChica> selectByCierre(Long idCierre) throws Throwable;

}
