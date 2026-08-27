package com.saa.ejb.tsr.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.CierreCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * Dao Service CierreCajaChica.
 */
@Local
public interface CierreCajaChicaDaoService extends EntityDao<CierreCajaChica> {

	/**
	 * Cierres de una caja, del más reciente al más antiguo.
	 * @param idCaja : Id de la caja chica
	 * @return       : Cierres de la caja
	 * @throws Throwable : Excepcion
	 */
	List<CierreCajaChica> selectByCaja(Long idCaja) throws Throwable;

	/**
	 * Último cierre CERRADO de la caja (mayor fechaFin).
	 * @param idCaja : Id de la caja chica
	 * @return       : Cierre, o null si la caja no tiene ninguno cerrado
	 * @throws Throwable : Excepcion
	 */
	CierreCajaChica selectUltimoCerrado(Long idCaja) throws Throwable;

	/**
	 * Indica si la caja tiene un cierre en estado BORRADOR.
	 * @param idCaja : Id de la caja chica
	 * @return       : true si ya tiene un borrador pendiente
	 * @throws Throwable : Excepcion
	 */
	boolean existeBorrador(Long idCaja) throws Throwable;

	/**
	 * El cierre en estado BORRADOR de la caja, si tiene uno (a lo sumo uno,
	 * garantizado por {@code CierreCajaChicaServiceImpl.prepararCierre}).
	 * @param idCaja : Id de la caja chica
	 * @return       : Cierre BORRADOR, o null si no tiene ninguno
	 * @throws Throwable : Excepcion
	 */
	CierreCajaChica selectBorrador(Long idCaja) throws Throwable;

}
