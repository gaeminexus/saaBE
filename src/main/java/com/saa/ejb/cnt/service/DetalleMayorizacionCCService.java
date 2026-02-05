package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.DetalleMayorizacionCC;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.MayorizacionCC;

import jakarta.ejb.Local;

@Local
public interface DetalleMayorizacionCCService extends EntityService <DetalleMayorizacionCC> {
	

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  DetalleMayorizacionCC selectById(Long id) throws Throwable;
	 
	 /**
	  * Crea el detalle de una mayorizacion por CC
	  * @param mayorizacionCC	: Cabecera MayorizacionCC 
	  * @param empresa			: Id de la empresa
	  * @throws Throwable		: Excepcion
	  */
	  void creaDetalleMayorizacionCC(MayorizacionCC mayorizacionCC, Long empresa) throws Throwable;
	 
	 /**
	  * Crea el saldo inicial de los centros de costos. En caso de existir una mayorizacion anterior 
	  *  toma el saldo final del periodo anterior
	  * @param mayorizacionCC	: Cabecera MayorizacionCC
	  * @param anterior			: Mayorizacion anterior
	  * @throws Throwable		: Excepcion
	  */
	  void creaSaldoInicial(MayorizacionCC mayorizacionCC, Mayorizacion anterior) throws Throwable;
	 
	 /**
	  * Crea el desglose de un detalle de mayorizacion
	 * @param mayorizacionCC	: Id de MayorizacionCC
	 * @param empresa			: Id de empresa
	 * @throws Throwable		: Excepcion
	 */
	 void creaDesgloseDetalle(Long mayorizacionCC, Long empresa) throws Throwable;
	
	/**
	 * Genera saldos en desglose de mayorizacion por centro de costo
	 * @param mayorizacionCC	: Mayorizacion por centro de costo
	 * @throws Throwable		: Excepcion
	 */
	 void generaSaldosDesglose(Long mayorizacionCC) throws Throwable;
	
	/**
	 * Genera los saldos de las cuentas padres del desglose
	 * @param mayorizacionCC	: Id de la mayorizacion por centro de costo
	 * @throws Throwable		: Excepcion
	 */
	 void generaSaldosDesglosePadres(Long mayorizacionCC) throws Throwable;
	
	/**
	 * Elimina el detalle de una mayorizacion por centro de costo
	 * @param mayorizacionCC	: Id de la mayorizacion por centro de costo
	 * @throws Throwable		: Excepcion
	 */
	 void eliminaDetalleByMayorizacionCC(Long mayorizacionCC) throws Throwable;
	 
	 /**
	 * Recupera un registro por mayorizacion y centro de costo 
	 * @param mayorizacion	: Id de la mayorizacion
	 * @param cC			: Id del centro de costo
	 * @return				: Registro recuperado
	 * @throws Throwable	: Excepcion
	 */
	DetalleMayorizacionCC selectByMayorizacionAndCC(Long mayorizacion, Long cC) throws Throwable;

}
