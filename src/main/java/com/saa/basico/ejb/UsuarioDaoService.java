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

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.scp.Usuario;

/**
 * @author GaemiSoft.
 *
 *         Clase UsuarioDao.
 */
public interface UsuarioDaoService extends EntityDao<Usuario> {

List<Usuario> selectUsuarios() throws Throwable;
	
	List<Usuario> selectUsuariosActivos() throws Throwable;
	
	String validaUsuario(String idUsuario, String clave) throws Throwable;
	
	String validaUsuarioSucursal(String idUsuario, String clave, String idEmpresa) throws Throwable;
	
	String cambiaClave(String idUsuario, String anterior, String nueva) throws Throwable;
	
	Usuario selectByNombre(String nombre) throws Throwable;
	
	String verificaPermiso(Long idEmpresa, Long idUsuario, Long idPermiso) throws Throwable;
}
