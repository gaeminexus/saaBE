package com.saa.ejb.tesoreria.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.CierreCaja;
import com.saa.model.tesoreria.DetalleCierre;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DetalleCierre.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface DetalleCierreService extends EntityService<DetalleCierre>{
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	DetalleCierre selectById(Long id) throws Throwable;
	
	/**
	 * Inserta el detalle de un cierre de caja
	 * @param idUsuarioCaja : Id del usuario por caja
	 * @param idCierreCaja	: Id de cierre de caja
	 * @throws Throwable	: Excepcion
	 */
	void insertaDetalleCierreCaja(Long idUsuarioCaja, CierreCaja cierreCaja) throws Throwable;
	
	/**
	 * Recupera la lista de detalle de cierres
	 * @param idCobro	: Id de cierre caja
	 * @return			: Elementos recuperados 
	 * @throws Throwable: Excepcion
	 */
	List<DetalleCierre> selectByCierreCaja(Long idCierreCaja) throws Throwable;
	
	/**
	 * Select de cajas logicas con suma de efectivo y cheque para asiento de cierre
	 * @param idCierreCaja	: Id de cierre de caja
	 * @return				: cajas logicas y suma de efectivo y cheque por caja
	 * @throws Throwable	: Excepcion
	 */
	@SuppressWarnings("rawtypes")
	List selectDistinctCajaLogicaByCierreCaja(Long idCierreCaja) throws Throwable;
}