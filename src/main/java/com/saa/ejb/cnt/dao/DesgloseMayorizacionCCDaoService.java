package com.saa.ejb.cnt.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cnt.DesgloseMayorizacionCC;

import jakarta.ejb.Local;

/**
 * 
 */
@Local
public interface DesgloseMayorizacionCCDaoService  extends EntityDao<DesgloseMayorizacionCC>  {	
		
		/**
		 * Recupera los niveles incluidos en una mayorizacion
		 * @param idDetalleCC	: Id de detalle mayorizacionCC
		 * @return				: Niveles
		 * @throws Throwable	: Excepcion
		 */
		@SuppressWarnings({"rawtypes" })
		List selectNivelesByMayorizacionCC(Long idDetalleCC) throws Throwable;
		
		/**
		 * Recupera las cuentas padres de un nivel de una mayorizacion
		 * @param idDetalleCC	: Id de detalle mayorizacionCC
		 * @param nivel			: Nivel
		 * @return				: Cuentas padre
		 * @throws Throwable	: Excepcion
		 */
		@SuppressWarnings({ "rawtypes" })
		List selectPadresByNivel(Long idDetalleCC, Long nivel) throws Throwable;
		
		/**
		 * Recupera por codigo de padre de una mayorizacionCC
		 * @param idDetalleCC: Id de detalle mayorizacionCC
		 * @param nivel		: Nivel
		 * @param padre		: Id de la cuenta padre
		 * @return			: Listado de detalle de mayorizacionCC
		 * @throws Throwable: Excepcion
		 */
		List<DesgloseMayorizacionCC> selectByCuentaPadreNivel(Long idDetalleCC, Long nivel, Long padre) throws Throwable;
		
		/**
		 * Recupera el detalle perteneciente a una mayorizacionCC y a una cuenta
		 * @param mayorizacionCC: Id de mayorizacionCC
		 * @param cuenta		: Id de cuenta
		 * @return				: Detalle de mayorizacionCC
		 * @throws Throwable	: Exception
		 */
		List<DesgloseMayorizacionCC> selectByMayorizacionCCCuenta(Long mayorizacionCC, Long cuenta) throws Throwable;
		
		/**
		 * Elimina los registros por id de mayorizacionCC
		 * @param idDetalleCC	: Id del detalle de mayorizacionCC
		 * @throws Throwable	: Excepcion
		 */
		void deleteByDetalleCC(Long idDetalleCC) throws Throwable;
		
		/**
		 * Recupera los registros por id de detalle mayorizacionCC
		 * @param idDetalleMayorizacionCC: Id de detalle mayorizacionCC
		 * @return: Listado de desglose mayorizacionCC
		 * @throws Throwable: Excepcion
		 */
		List<DesgloseMayorizacionCC> selectByIdDetalleMayorizacionCC(Long idDetalleMayorizacionCC) throws Throwable;

	
}
