package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.FormatoArchivoBancario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService FormatoArchivoBancario.
 */
@Local
public interface FormatoArchivoBancarioDaoService extends EntityDao<FormatoArchivoBancario> {

	/**
	 * Recupera el formato activo de una empresa.
	 *
	 * <p>Devuelve <code>null</code> si la empresa todavia no tiene formato: no es un error,
	 * es lo que hace que <code>generarArchivoBancario</code> pueda decir que falta crearlo.
	 * Si hubiera mas de uno activo devuelve el mas reciente, que es el que la operacion
	 * espera despues de cargar un formato nuevo.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @return				: El formato activo, o null
	 * @throws Throwable	: Excepcion
	 */
	FormatoArchivoBancario selectActivoByEmpresa(Long idEmpresa) throws Throwable;

}
