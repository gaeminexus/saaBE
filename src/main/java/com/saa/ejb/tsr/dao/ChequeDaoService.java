/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.tsr.Cheque;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 * Dao Sevice Cheque.
 */
@Local
public interface ChequeDaoService extends EntityDao<Cheque> {

	/**
	 * Recupera el Maximo Cheqhe se la chequera segun la cuenta
	 * @param cuenta		: Numero de la cuenta
	 * @return
	 * @throws Throwable	: Excepcion
	 */
	List<Cheque> selectMaxCheque (Long cuenta)throws Throwable;

	 /**
	  * Recupera el Maximo Cheqhe se la chequera segun la cuenta
	  * @param cuenta		: Id de la cuenta
	  * @return				: Id de primer cheque activo de una cuenta
	  * @throws Throwable	: Excepcion
	  */
	Long selectMinChequeActivo (Long idCuenta)throws Throwable;

	/**
	 * Recupera todos los cheques de una chequera, ordenados por número.
	 * @param idChequera	: Id de la chequera
	 * @return				: Cheques de la chequera
	 * @throws Throwable	: Excepcion
	 */
	List<Cheque> selectByChequera(Long idChequera) throws Throwable;

	/**
	 * Id del cheque ACTIVO de menor número entre las chequeras ACTIVAS de la
	 * cuenta bancaria indicada.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Id del cheque, o null si no hay disponibles
	 * @throws Throwable		: Excepcion
	 */
	Long selectMinChequeActivoPorCuenta(Long idCuentaBancaria) throws Throwable;

	/**
	 * Ids de los PagoProgramado que respaldan un cheque, ordenados. Antes de
	 * "un cheque para varios pagos" (docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md)
	 * esta lista tenía a lo sumo un elemento; con el índice único retirado puede tener varios.
	 * @param idCheque		: Id del cheque
	 * @return				: Ids de los pagos, ordenados; lista vacía si el cheque no tiene ninguno
	 * @throws Throwable	: Excepcion
	 */
	List<Long> selectIdsPagoByCheque(Long idCheque) throws Throwable;

	/**
	 * Listado de cheques con los datos del pago que los usó (si lo hay), para
	 * la pantalla de consulta de cheques.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria, null para todas
	 * @param estado			: Estado del cheque (rubro EstadoCheque), null para todos
	 * @param desde				: Fecha de uso desde, null sin límite inferior
	 * @param hasta				: Fecha de uso hasta, null sin límite superior
	 * @param idEmpresa			: Id de la empresa; sólo filtra los cheques que ya tienen
	 * 							  pago asociado (un cheque sin pago no tiene empresa que filtrar)
	 * @return					: Filas [idCheque, numero, estado, valor, beneficiario, fechaUso,
	 * 							  fechaImpresion, fechaEntrega, numeroCuenta, banco, idPago,
	 * 							  idFactura, numeroFactura, idEgreso, descripcionEgreso,
	 * 							  idAnticipo, numeroDocAnticipo, origenExterno, idOrigen]
	 * @throws Throwable		: Excepcion
	 */
	List<Object[]> selectListado(Long idCuentaBancaria, Long estado, LocalDate desde, LocalDate hasta,
			Long idEmpresa) throws Throwable;

}
