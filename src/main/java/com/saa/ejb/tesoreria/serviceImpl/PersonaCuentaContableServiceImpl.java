/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.serviceImpl;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.tesoreria.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tesoreria.service.PersonaCuentaContableService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.PersonaCuentaContable;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PersonaCuentaContableService.
 *  Contiene los servicios relacionados con la entidad PersonaCuentaContable.</p>
 */
@Stateless
public class PersonaCuentaContableServiceImpl implements PersonaCuentaContableService {
	
	@EJB
	private PersonaCuentaContableDaoService personaCuentaContableDaoService;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#remove(java.util.List)
	 */
	public void remove(List<Long> id) throws Throwable {
		System.out.println("Ingresa al metodo remove[] de PersonaCuentaContable service ... depurado");
		//INSTANCIA LA ENTIDAD
		PersonaCuentaContable personaCuentaContable = new PersonaCuentaContable();
		//ELIMINA UNO A UNO LOS REGISTROS DEL ARREGLO
		for (Long registro : id) {
			personaCuentaContableDaoService.remove(personaCuentaContable, registro);
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.util.EntityService#save(java.lang.Object[][])
	 */
	public void save(List<PersonaCuentaContable> lista) throws Throwable {
		System.out.println("Ingresa al metodo save de PersonaCuentaContable service");
		// BARRIDA COMPLETA DE LOS REGISTROS
		for (PersonaCuentaContable personaCuentaContable : lista) {			
			personaCuentaContableDaoService.save(personaCuentaContable, personaCuentaContable.getCodigo());
		}
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.BancoExternoService#selectAll()
	 */
	@Override
	public List<PersonaCuentaContable> selectAll() throws Throwable {
		System.out.println("Ingresa al metodo selectAll PersonaCuentaContableService");
		// CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
		List<PersonaCuentaContable> result = personaCuentaContableDaoService.selectAll(NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
		// INICIALIZA EL OBJETO
		if (result.isEmpty()) {
			// NO ENCUENTRA REGISTROS
			throw new IncomeException("Busqueda total PersonaCuentaContable no devolvio ningun registro");
		}
		// RETORNA ARREGLO DE OBJETOS
		return result;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.util.EntityService#selectByCriteria(java.lang.Object[], java.util.List)
	 */
	public List<PersonaCuentaContable> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
	    System.out.println("Ingresa al metodo (selectByCriteria) PersonaCuentaContable");
	    // CREA EL LISTADO CON LOS REGISTROS DE LA BUSQUEDA
	    List<PersonaCuentaContable> result = personaCuentaContableDaoService.selectByCriteria(
	        datos, NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE
	    );
	    // PREGUNTA SI ENCONTRO REGISTROS
	    if (result.isEmpty()) {
	        // NO ENCUENTRA REGISTROS
	        throw new IncomeException("Busqueda por criterio de PersonaCuentaContable no devolvio ningun registro");
	    }
	    // RETORNA ARREGLO DE OBJETOS
	    return result;
	}

	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PersonaCuentaContableService#selectById(java.lang.Long)
	 */
	public PersonaCuentaContable selectById(Long id) throws Throwable {
		System.out.println("Ingresa al selectById con id: " + id);		
		return personaCuentaContableDaoService.selectById(id, NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE);
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.service.PersonaCuentaContableService#selectByPersonaTipoCuenta(java.lang.Long, int, java.lang.Long)
	 */
	public List<PersonaCuentaContable> selectByPersonaTipoCuenta(Long idEmpresa, Long idPersona, int rolPersona, Long tipoCuenta) throws Throwable {
		System.out.println("Ingresa al selectByCodigoPersona con id de persona: " + idPersona);
		return personaCuentaContableDaoService.selectByPersonaTipoCuenta(idEmpresa, idPersona, rolPersona, tipoCuenta);
	}
	
	@Override
	public PersonaCuentaContable saveSingle(PersonaCuentaContable personaCuentaContable) throws Throwable {
		System.out.println("saveSingle - PersonaCuentaContable");
		personaCuentaContable = personaCuentaContableDaoService.save(personaCuentaContable, personaCuentaContable.getCodigo());
		return personaCuentaContable;
	}


}
