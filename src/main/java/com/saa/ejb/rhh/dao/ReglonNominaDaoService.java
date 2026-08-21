/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ReglonNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ReglonNomina. 
 */
@Local
public interface ReglonNominaDaoService  extends EntityDao<ReglonNomina>  {
	

	/**
	 * Recupera los renglones de una nomina, en orden de presentacion.
	 *
	 * @param idNomina		: Id de la nomina
	 * @return				: Listado de renglones; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ReglonNomina> selectByNomina(Long idNomina) throws Throwable;

	/**
	 * Recupera los renglones editados a mano de una nomina. Son los que el recalculo
	 * debe preservar cuando se pide preservarManuales.
	 *
	 * @param idNomina		: Id de la nomina
	 * @return				: Listado de renglones manuales; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ReglonNomina> selectManualesByNomina(Long idNomina) throws Throwable;

	/**
	 * Elimina los renglones generados automaticamente de una nomina, conservando los
	 * marcados como manuales.
	 *
	 * @param idNomina		: Id de la nomina
	 * @return				: Numero de renglones eliminados
	 * @throws Throwable	: Excepcion
	 */
	int eliminaGeneradosByNomina(Long idNomina) throws Throwable;
}
