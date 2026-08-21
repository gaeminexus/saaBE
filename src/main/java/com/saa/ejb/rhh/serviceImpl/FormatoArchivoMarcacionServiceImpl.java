package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.FormatoArchivoMarcacionDaoService;
import com.saa.ejb.rhh.service.FormatoArchivoMarcacionService;
import com.saa.model.rhh.FormatoArchivoMarcacion;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz FormatoArchivoMarcacionService.
 *  Contiene los servicios relacionados con la entidad FormatoArchivoMarcacion.</p>
 */
@Stateless
public class FormatoArchivoMarcacionServiceImpl implements FormatoArchivoMarcacionService {

	@EJB
	private FormatoArchivoMarcacionDaoService formatoArchivoMarcacionDaoService;

	@Override
	public void save(List<FormatoArchivoMarcacion> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de formatoArchivoMarcacion service");
		for (FormatoArchivoMarcacion registro : lista) {
			formatoArchivoMarcacionDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de formatoArchivoMarcacion service");
		//INSTANCIA UNA ENTIDAD
		FormatoArchivoMarcacion formatoArchivoMarcacion = new FormatoArchivoMarcacion();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			formatoArchivoMarcacionDaoService.remove(formatoArchivoMarcacion, registro);
		}
	}

	@Override
	public List<FormatoArchivoMarcacion> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) FormatoArchivoMarcacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FormatoArchivoMarcacion> result = formatoArchivoMarcacionDaoService.selectAll(NombreEntidadesRhh.FORMATO_ARCHIVO_MARCACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de formatoArchivoMarcacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public FormatoArchivoMarcacion selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de formatoArchivoMarcacion con id: " + id);
		return formatoArchivoMarcacionDaoService.selectById(id, NombreEntidadesRhh.FORMATO_ARCHIVO_MARCACION);
	}

	@Override
	public List<FormatoArchivoMarcacion> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) FormatoArchivoMarcacion");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<FormatoArchivoMarcacion> result = formatoArchivoMarcacionDaoService.selectByCriteria(datos, NombreEntidadesRhh.FORMATO_ARCHIVO_MARCACION);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de formatoArchivoMarcacion no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public FormatoArchivoMarcacion saveSingle(FormatoArchivoMarcacion formatoArchivoMarcacion) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) FormatoArchivoMarcacion");
		formatoArchivoMarcacion = formatoArchivoMarcacionDaoService.save(formatoArchivoMarcacion, formatoArchivoMarcacion.getCodigo());
		return formatoArchivoMarcacion;
	}
}
