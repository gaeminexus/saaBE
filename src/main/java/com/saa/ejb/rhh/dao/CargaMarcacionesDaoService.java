package com.saa.ejb.rhh.dao;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.CargaMarcaciones;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService CargaMarcaciones.
 */
@Local
public interface CargaMarcacionesDaoService extends EntityDao<CargaMarcaciones> {

	/**
	 * Busca una carga no anulada con el mismo hash en la empresa.
	 *
	 * <p>Es el control antiduplicado de la regla 1 del parser. <b>Ignora las anuladas</b>: si
	 * una carga se anulo porque el archivo venia mal, el archivo corregido --o incluso el
	 * mismo-- debe poder volver a entrar.</p>
	 *
	 * @param hash			: SHA-256 del contenido del archivo
	 * @param idEmpresa		: Id de la empresa
	 * @return				: La carga vigente con ese hash, o null
	 * @throws Throwable	: Excepcion
	 */
	CargaMarcaciones selectVigenteByHash(String hash, Long idEmpresa) throws Throwable;

}
