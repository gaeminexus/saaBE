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

import com.saa.basico.ejb.EmpresaDaoService;
import com.saa.basico.ejb.EmpresaService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.NombreEntidadesSistema;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Implementaci�n de la interfaz EmpresaService.
 *         Contiene los servicios relacionados con la entidad Empresa.
 *         </p>
 */
@Stateless
public class EmpresaServiceImpl implements EmpresaService {

	@EJB
	private EmpresaDaoService empresaDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.tesoreria.ejb.service.EmpresaService#remove(java.util.
	 * List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de empresa service");
		// VALIDA LA ENTIDAD
		Empresa empresa = new Empresa();
		// ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			empresaDaoService.remove(empresa, registro);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saa.basico.util.EntityService#save(java.lang.Object[][],
	 * java.lang.Object[])
	 */
	public void save(List<Empresa> object) throws Throwable {
		System.out.println("Ingresa al metodo save de empresa service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (Empresa registro : object) {
			// INSERTA O ACTUALIZA REGISTRO
			empresaDaoService.save(registro, registro.getCodigo());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.saa.basico.util.EntityService#save(java.lang.Object[],
	 * java.lang.Object[])
	 */
	public Empresa saveSingle(Empresa object) throws Throwable {
		System.out.println("Ingresa al metodo save - codigo de empresa service");
		// INSERTA O ACTUALIZA REGISTRO
		if (object.getCodigo() == 0) {
			object.setCodigo(null);
		}
		object = empresaDaoService.save(object, object.getCodigo());
		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.income.tesoreria.ejb.service.EmpresaService#selectAll()
	 */
	public List<Empresa> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) empresa Service");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Empresa> result = empresaDaoService.selectAll(NombreEntidadesSistema.EMPRESA);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda completa de empresa no devolvio ningun registro");
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
	 * com.gaemisoft.income.tesoreria.ejb.manager.BancoManager#selectByEmpresa(java.
	 * lang.Long)
	 */
	public List<Empresa> selectArbolEmpresas() throws Throwable {
		// VARIABLE PARA MENSAJES
		System.out.println("Ingresa al metodo (selectArbolEmpresas) Empresa: ");
		// INICIALIZA LA VARIABLE PARA LOS CAMPOS A RECUPERAR
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Empresa> result = empresaDaoService.selectArbolEmpresas();
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda empresa no devolvio ningun registro");
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
	 * com.gaemisoft.income.sistema.ejb.service.EmpresaService#selectById(java.lang.
	 * Long)
	 */
	public Empresa selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);
		return empresaDaoService.selectById(id, NombreEntidadesSistema.EMPRESA);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.income.sistema.ejb.util.EntityService#selectByCriteria(java.
	 * lang.Object[], java.util.List)
	 */
	public List<Empresa> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) Empresa");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<Empresa> result = empresaDaoService.selectByCriteria(datos, NombreEntidadesSistema.EMPRESA);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			System.out.println("Busqueda por criterio de empresa no devolvio ningun registro");
			// MENSAJE DE REGISTRO NO ENCONTRADO
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}
}
