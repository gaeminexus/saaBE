
/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tsr.dao.PersonaRolDaoService;
import com.saa.ejb.tsr.service.PersonaRolService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PersonaRol;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PersonaRolService.
 *  Contiene los servicios relacionados con la entidad PersonaRol.</p>
 */
@Stateless
public class PersonaRolServiceImpl implements PersonaRolService {
	
	@EJB
	private PersonaRolDaoService personaRolDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PersonaRol service ... depurado");
		//INSTANCIA LA ENTIDAD
		PersonaRol personaRol = new PersonaRol();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			personaRolDaoService.remove(personaRol, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#save(java.lang.Object[][])
	 */
	public void save(List<PersonaRol> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PersonaRol service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (PersonaRol personaRol : lista) {			
			personaRolDaoService.save(personaRol, personaRol.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<PersonaRol> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PersonaRolService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PersonaRol> result = personaRolDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_ROL);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total PersonaRol no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#verificaHijos(java.lang.Long)
	 */
	public boolean verificaHijos(Long id) throws Throwable {
		System.out.println("Ingresa al metodo verificaHijos con id: " + id);			
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PersonaRol> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) PersonaRol");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<PersonaRol> result = personaRolDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.PERSONA_ROL
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de PersonaRol no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PersonaRolService#selectById(java.lang.Long)
	 */
	public PersonaRol selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return personaRolDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA_ROL);
	}
	
	
	@Override
	public PersonaRol saveSingle(PersonaRol personaRol) throws Throwable {
		System.out.println("saveSingle - PersonaRol");
		personaRol = personaRolDaoService.save(personaRol, personaRol.getCodigo());
		return personaRol;
	}


}
