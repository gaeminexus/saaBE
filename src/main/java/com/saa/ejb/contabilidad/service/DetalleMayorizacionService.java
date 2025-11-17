package com.saa.ejb.contabilidad.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.model.contabilidad.DetalleMayorizacion;
import com.saa.model.contabilidad.Mayorizacion;
import com.saa.model.contabilidad.Periodo;

import jakarta.ejb.Remote;

@Remote
public interface DetalleMayorizacionService extends EntityService<DetalleMayorizacion> {
	

	 /**
	  * Crea detalle para mayorizacion de un periodo de una empresa
	 * @param mayorizacion	: Mayorizacion en la que incluira el detalle
	 * @param empresa		: Id de empresa en la que se crea el detalle
	 * @throws Throwable	: Excepcion
	 */
	 void creaDetalleMayorizacion(Mayorizacion mayorizacion, Long empresa) throws Throwable;
	
	/**
	 * Crea los saldos iniciales en el detalle de mayorizacion en base a los saldos finales de la mayorizacion anterior
	 * @param mayorizacionActual	: Mayorizacion actual
	 * @param mayorizacionAnterior	: Mayorizacion anterior
	 * @throws Throwable			: Excepcion 
	 */
	 void creaSaldoInicialMayorizacion(Mayorizacion mayorizacionActual, Mayorizacion mayorizacionAnterior) throws Throwable;
	
	/**
	 * Obtiene los movimientos de una cuenta y calcula el saldo final
	 * @param empresa		: Id. de la empresa
	 * @param mayorizacion	: Información de mayorizacion
	 * @throws Throwable	: Excepcion	
	 */
	 void calculaSaldoFinalMovimiento(Long empresa, Mayorizacion mayorizacion, Periodo periodo) throws Throwable;
	
	/**
	 * Calcula los saldos finales de las cuentas de acumulacion
	 * @param mayorizacion	: Id de la mayorizacion
	 * @throws Throwable	: Excepcion
	 */
	 void calculaSaldosAcumulacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Recupera entidad con el id
	 * @param id			: Id de la entidad
	 * @return				: Recupera entidad
	 * @throws Throwable	: Excepcion
	 */
	 DetalleMayorizacion selectById(Long id) throws Throwable;
	
	/**
	 * Recupera las cuentas para generar el asiento de cierre
	 * @param mayorizacion	: Id mayorizacion
	 * @return				: Listado de cuentas
	 * @throws Throwable	: Excepcion
	 */
	 List<DetalleMayorizacion> selectForCierre(Long mayorizacion) throws Throwable;
	
	/**
	 * Recupera datos por codigo de mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @return				: Listado de registros
	 * @throws Throwable	: Excepcion
	 */
	 List<DetalleMayorizacion> selectByCodigoMayorizacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Elimina los datos del detalle de una mayorizacion
	 * @param mayorizacion	: Id de mayorizacion
	 * @throws Throwable	: Excepcion
	 */
	 void eliminaDetalleMayorizacion(Long mayorizacion) throws Throwable;
	
	/**
	 * Elimina los registros por codigo de mayorizacin
	 * @param idMayorizacion	: Codigo de mayorizacion
	 * @throws Throwable		: Excepcion
	 */
	 void deleteByMayorizacion(Long idMayorizacion) throws Throwable;
	 
	 /**
	  * Recupera un detalle mayorizacion por 
	 * @param idCuenta		: Id de la cuenta a buscar
	 * @param idMayorizacion: Id de la mayorizacion a buscar
	 * @return				: Detalle mayorizacion con saldos de cuenta
	 * @throws Throwable	: Excepcion
	 */
	DetalleMayorizacion selectByCuentaMayorizacion(Long idCuenta, Long idMayorizacion) throws Throwable;
	 
}
