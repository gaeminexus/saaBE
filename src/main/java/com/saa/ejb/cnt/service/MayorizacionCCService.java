package com.saa.ejb.cnt.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cnt.Mayorizacion;
import com.saa.model.cnt.MayorizacionCC;
import com.saa.model.cnt.Periodo;

import jakarta.ejb.Local;

@Local
public interface MayorizacionCCService extends EntityService<MayorizacionCC> {
	
	 
	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  MayorizacionCC selectById(Long id) throws Throwable;
	 
	 /**
	  * Mayoriza por centro de costo
	  * @param mayorizacion	: Mayorizacion a mayorizar por centro de costo
	  * @param periodo		: Periodo en el que se realiza la mayorizacion
	  * @param maximaAnterior: Maxima mayorizacion anterior a la mayorizacion a procesar
	  * @throws Throwable	: Excepcion
	  */
	  void mayorizacionCC(Mayorizacion mayorizacion, Periodo periodo, Mayorizacion maximaAnterior) throws Throwable;
	 
	 /**
	  * Almacena un objeto del tipo mayorizacionCC
	  * @param mayorizacionCC	: Objeto a persistir
	  * @throws Throwable		: Excepcion
	  */
	  void save(MayorizacionCC mayorizacionCC) throws Throwable;
	 
	 /**
	  * Crea la cabecera de la mayorizacion por centro de costo en base a una mayorización
	  * @param idMayorizacion	: Id de la mayorizacion origen
	  * @param periodo			: Periodo a mayorizar
	  * @throws Throwable		: Excepcion
	  */
	  MayorizacionCC creaByMayorizacion(Long idMayorizacion, Periodo periodo) throws Throwable;
	
	 /**
	  * Genera los datos en las entidades de mayorizacion por centro de costo
	  * @param mayorizacion	: Mayorizacion a mayorizar por centro de costo
	  * @param periodo		: Periodo en el que se realiza la mayorizacion
	  * @param maximaAnterior: Maxima mayorizacion anterior a la mayorizacion a procesar
	  * @return 			: Mayorizacion por centro de costo generada
	  * @throws Throwable	: Excepcion
	  */
	  MayorizacionCC generaDatosMayorizacionCC(Mayorizacion mayorizacion, Periodo periodo, Mayorizacion maximaAnterior) throws Throwable;
	 
	 /**
	  * Genera los saldos de las cuentas de una mayorizacion por centro de costo
	  * @param mayorizacionCC	: Id de mayorizacion por centro de costo 
	  * @throws Throwable		: Excepcion
	  */
	  void generaSaldosByCC(Long mayorizacionCC) throws Throwable;
	 
	 /**
	  * Elimina todos los registros relacionados con una mayorizacio por centro de costo
	  * @param mayorizacionCC	: Id de mayorizacion por centro de costo
	  * @throws Throwable		: Excepcion
	  */
	  void eliminaByMayorizacionCC(Long mayorizacionCC) throws Throwable;

}
