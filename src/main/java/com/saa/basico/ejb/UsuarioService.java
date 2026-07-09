/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Jos� Lucuma E6-95 y Pedro Cornelio
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.basico.ejb;

import com.saa.basico.util.EntityService;
import com.saa.model.scp.Usuario;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Servicio para la entidad Usuario.
 *         Accede a los metodos DAO y procesa los datos para el cliente.
 *         </p>
 */
@Local
public interface UsuarioService extends EntityService<Usuario> {
	/**
	 * Recupera entidad con el id
	 * 
	 * @param id : Id de la entidad
	 * @return : Recupera entidad
	 * @throws Throwable: Excepcion
	 */
	Usuario selectById(Long id) throws Throwable;

}
