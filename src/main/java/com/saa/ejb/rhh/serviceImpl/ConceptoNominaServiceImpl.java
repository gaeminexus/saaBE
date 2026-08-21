package com.saa.ejb.rhh.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.service.ConceptoNominaService;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementacion de la interfaz ConceptoNominaService.
 *  Contiene los servicios relacionados con la entidad ConceptoNomina.</p>
 */
@Stateless
public class ConceptoNominaServiceImpl implements ConceptoNominaService {

	@EJB
	private ConceptoNominaDaoService conceptoNominaDaoService;

	@Override
	public void save(List<ConceptoNomina> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de conceptoNomina service");
		for (ConceptoNomina registro : lista) {
			conceptoNominaDaoService.save(registro, registro.getCodigo());
		}
	}

	@Override
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de conceptoNomina service");
		//INSTANCIA UNA ENTIDAD
		ConceptoNomina conceptoNomina = new ConceptoNomina();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			conceptoNominaDaoService.remove(conceptoNomina, registro);
		}
	}

	@Override
	public List<ConceptoNomina> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo (selectAll) ConceptoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConceptoNomina> result = conceptoNominaDaoService.selectAll(NombreEntidadesRhh.CONCEPTO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda completa de conceptoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConceptoNomina selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById de conceptoNomina con id: " + id);
		return conceptoNominaDaoService.selectById(id, NombreEntidadesRhh.CONCEPTO_NOMINA);
	}

	@Override
	public List<ConceptoNomina> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
		System.out.println("Ingresa al metodo (selectByCriteria) ConceptoNomina");
		//CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<ConceptoNomina> result = conceptoNominaDaoService.selectByCriteria(datos, NombreEntidadesRhh.CONCEPTO_NOMINA);
		if (result.isEmpty()) {
			throw new IncomeException("Busqueda por criterio de conceptoNomina no devolvio ningun registro");
		}
		//RETORNA ARREGLO DE OBJETOS
		return result;
	}

	@Override
	public ConceptoNomina saveSingle(ConceptoNomina conceptoNomina) throws Throwable {
		System.out.println("Ingresa al metodo (saveSingle) ConceptoNomina");
		conceptoNomina = conceptoNominaDaoService.save(conceptoNomina, conceptoNomina.getCodigo());
		return conceptoNomina;
	}
}
