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
package com.saa.basico.ejbImpl;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import com.saa.basico.ejb.UsuarioDaoService;
import com.saa.basico.ejb.UsuarioService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.model.scp.NombreEntidadesSistema;
import com.saa.model.scp.Usuario;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Implementaci�n de la interfaz UsuarioService.
 *         Contiene los servicios relacionados con la entidad Usuario.
 *         </p>
 */
@Stateless
public class UsuarioServiceImpl implements UsuarioService {

	@EJB
	private UsuarioDaoService usuarioDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.service.UsuarioService#remove(java.util.
	 * List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de usuario service");
		// VALIDA LA ENTIDAD
		Usuario usuario = new Usuario();
		// ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			usuarioDaoService.remove(usuario, registro);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saa.basico.util.EntityService#save(java.lang.Object[][],
	 * java.lang.Object[])
	 */
	public void save(List<Usuario> object) throws Throwable {
		System.out.println("Ingresa al metodo save de usuario service");
		// INSTANCIA NUEVA ENTIDAD
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Usuario registro : object) {
			// INSERTA O ACTUALIZA REGISTRO
			usuarioDaoService.save(registro, registro.getCodigo());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saa.basico.util.EntityService#save(java.lang.Object[],
	 * java.lang.Object[])
	 */
	public Usuario saveSingle(Usuario object) throws Throwable {
		System.out.println("Ingresa al metodo save - codigo de usuario service");
		// INSTANCIA NUEVA ENTIDAD
		object = usuarioDaoService.save(object, object.getCodigo());
		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.tesoreria.ejb.service.UsuarioService#selectAll()
	 */
	public List<Usuario> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) usuario Service");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Usuario> result = usuarioDaoService.selectAll(NombreEntidadesSistema.USUARIO);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda completa de usuario no devolvio ningun registro");
			// MENSAJE DE REGISTRO NO ENCONTRADO
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.
	 * Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.service.UsuarioService#selectById(java.lang.
	 * Long)
	 */
	public Usuario selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return usuarioDaoService.selectById(id, NombreEntidadesSistema.USUARIO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityService#selectByCriteria(java.
	 * lang.Object[], java.util.List)
	 */
	public List<Usuario> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Usuario");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Usuario> result = usuarioDaoService.selectByCriteria(datos, NombreEntidadesSistema.USUARIO);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda por criterio de usuario no devolvio ningun registro");
			// MENSAJE DE REGISTRO NO ENCONTRADO
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}
}
