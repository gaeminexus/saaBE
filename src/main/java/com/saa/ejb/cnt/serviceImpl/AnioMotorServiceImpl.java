/**
 * Copyright � Gaemi Soft C�a. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. J�se Fern�ndez.
 * Quito - Ecuador
 * Este programa est� protegido por las leyes de derechos de autor y otros tratados internacionales.
 * La reproducci�n o la distribuci�n no autorizadas de este programa, o de cualquier parte del mismo, 
 * est� penada por la ley y con severas sanciones civiles y penales, y ser� objeto de todas las
 * acciones judiciales que correspondan.
 * Usted no puede divulgar dicha Informaci�n confidencial y se utilizar� s�lo en  conformidad  
 * con los t�rminos del acuerdo de licencia que ha introducido dentro de Gaemi Soft.
**/
package com.saa.ejb.cnt.serviceImpl;

import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.AnioMotorDaoService;
import com.saa.ejb.cnt.service.AnioMotorService;
import com.saa.model.cnt.AnioMotor;
import com.saa.model.cnt.NombreEntidadesContabilidad;

/**
 * @author GaemiSoft.
 *
 *         <p>
 *         Implementaci�n de la interfaz AnioMotorService.
 *         Contiene los servicios relacionados con la entidad AnioMotor.
 *         </p>
 */
@Stateless
public class AnioMotorServiceImpl implements AnioMotorService {

	@EJB
	private AnioMotorDaoService anioMotorDaoService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.access.util.EntityService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Service remove[] de AnioMotor service");
		// VALIDA LA ENTIDAD
		AnioMotor anioMotor = new AnioMotor();
		// ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			anioMotorDaoService.remove(anioMotor, registro);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.basico.sistema.ejb.util.EntityService#save(java.lang.List<
	 * AnioMotor>, java.lang.Object[])
	 */
	public void save(List<AnioMotor> object) throws Throwable {
		System.out.println("Service save de AnioMotor service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (AnioMotor registro : object) {
			// INSERTA O ACTUALIZA REGISTRO
			if (registro.getCodigo() == 0L) {
				registro.setCodigo(null);
			}
			anioMotorDaoService.save(registro, registro.getCodigo());
		}
	}

	public AnioMotor saveSingle(AnioMotor object) throws Throwable {
		System.out.println("Ingresa al metodo save - codigo de calificadora service:" + object.getCodigo() + '/'
				+ object.getAnio());
		object = anioMotorDaoService.save(object, object.getCodigo());
		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.basico.sistema.ejb.util.EntityService#selectAll(java.lang.
	 * Object[])
	 */
	public List<AnioMotor> selectAll() throws Throwable {
		System.out.println("Service (selectAll) AnioMotor Service");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AnioMotor> result = anioMotorDaoService.selectAll(NombreEntidadesContabilidad.ANIO_MOTOR);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.service.AccionesService#selectById(java.lang
	 * .Long)
	 */
	public AnioMotor selectById(Long id) throws Throwable {
		System.out.println("Service selectById con id: " + id);
		return anioMotorDaoService.selectById(id, NombreEntidadesContabilidad.ANIO_MOTOR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.gaemisoft.basico.sistema.ejb.util.EntityService#selectByCriteria(java.
	 * lang.Object[], java.util.List)
	 */
	public List<AnioMotor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Service (selectByCriteria) AnioMotor");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AnioMotor> result = anioMotorDaoService.selectByCriteria(datos, NombreEntidadesContabilidad.ANIO_MOTOR);
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.gaemisoft.aot.service.AnioMotorService#selectOrderDesc()
	 */
	public List<AnioMotor> selectOrderDesc() throws Throwable {
		System.out.println("Service (selectOrderDesc) AnioMotor Service");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<AnioMotor> result = anioMotorDaoService.selectOrderDesc();
		// PREGUNTA SI ENCONTRO REGISTROS
		if (result.isEmpty()) {
			result = null;
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}
}
