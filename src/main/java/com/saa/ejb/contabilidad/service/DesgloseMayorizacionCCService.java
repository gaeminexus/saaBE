package com.saa.ejb.contabilidad.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DesgloseMayorizacionCC;
import com.saa.model.cnt.DetalleMayorizacionCC;

import jakarta.ejb.Local;

@Local
public interface DesgloseMayorizacionCCService extends EntityService <DesgloseMayorizacionCC> {
		 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  DesgloseMayorizacionCC selectById(Long id) throws Throwable;
	 
	 /**
	  * Crea el desglose de cada detalle de mayorizacionCC con los saldos de cada cuenta contable que afecta los saldos de un centro de costo
	  * @param detalleMayorizacionCC	: Detalle de mayorizacionCC
	  * @param empresa					: Id de la empresa donde se realizan los movimientos
	  * @throws Throwable				: Excepcion
	  */
	  void creaDesgloseMayorizacionCC(DetalleMayorizacionCC detalleMayorizacionCC, Long empresa) throws Throwable;
	 
	 /**
	  * Persiste un objeto de tipo desgloseMayorizacionCC
	  * @param desgloseMayorizacionCC	: Objeto a persistir
	  * @throws Throwable				: Excepcion
	  */
	  void save(DesgloseMayorizacionCC desgloseMayorizacionCC) throws Throwable;
	 
	 /**
	  * Genera los saldos de las cuentas de acumulacion
	  * @param idDetalleCC	: Id de detalle de mayorizacion de acumulacion	
	  * @throws Throwable	: Excepcion
	  */
	  void generaSaldosAcumulacion(Long idDetalleCC) throws Throwable;
	 
	 /**
	  * Elimina el desglose de una mayorizacion por centro de costo
	  * @param idDetalleCC	: Id del detalle de mayorizacion por centro de costo
	  * @throws Throwable	: Excepcion
	  */
	  void eliminaDesgloseByDetalleCC(Long idDetalleCC) throws Throwable;

}
