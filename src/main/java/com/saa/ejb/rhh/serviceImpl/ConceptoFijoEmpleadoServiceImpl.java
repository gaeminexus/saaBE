package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ConceptoFijoEmpleadoDaoService;
import com.saa.ejb.rhh.service.ConceptoFijoEmpleadoService;
import com.saa.model.rhh.ConceptoFijoEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ConceptoFijoEmpleadoService.
 *  Contiene los servicios relacionados con la entidad ConceptoFijoEmpleado.</p>
 */
@Stateless
public class ConceptoFijoEmpleadoServiceImpl implements ConceptoFijoEmpleadoService {

	@EJB
	private ConceptoFijoEmpleadoDaoService conceptoFijoEmpleadoDaoService;

	@Override
	public void save(List<ConceptoFijoEmpleado> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de conceptoFijoEmpleado service");
		for (ConceptoFijoEmpleado registro : lista) {
			conceptoFijoEmpleadoDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de conceptoFijoEmpleado service");
		//INSTANCIA UNA ENTIDAD
		ConceptoFijoEmpleado conceptoFijoEmpleado = new ConceptoFijoEmpleado();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			conceptoFijoEmpleadoDaoService.remove(conceptoFijoEmpleado, registro);
		}
	}

	@Override
	public List<ConceptoFijoEmpleado> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ConceptoFijoEmpleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConceptoFijoEmpleado> result = conceptoFijoEmpleadoDaoService.selectAll(NombreEntidadesRhh.CONCEPTO_FIJO_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de conceptoFijoEmpleado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConceptoFijoEmpleado selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de conceptoFijoEmpleado con id: " + id);
		return conceptoFijoEmpleadoDaoService.selectById(id, NombreEntidadesRhh.CONCEPTO_FIJO_EMPLEADO);
	}

	@Override
	public List<ConceptoFijoEmpleado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ConceptoFijoEmpleado");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConceptoFijoEmpleado> result = conceptoFijoEmpleadoDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONCEPTO_FIJO_EMPLEADO);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de conceptoFijoEmpleado no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConceptoFijoEmpleado saveSingle(ConceptoFijoEmpleado conceptoFijoEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ConceptoFijoEmpleado");
		conceptoFijoEmpleado = conceptoFijoEmpleadoDaoService.save(conceptoFijoEmpleado, conceptoFijoEmpleado.getCodigo());
		return conceptoFijoEmpleado;
	}
}
