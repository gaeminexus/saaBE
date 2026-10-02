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
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.PersonaCuentaContableService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.PersonaRol;
import com.saa.rubros.RolPersona;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementación de la interfaz PersonaCuentaContableService.
 *  Contiene los servicios relacionados con la entidad PersonaCuentaContable.</p>
 */
@Stateless
public class PersonaCuentaContableServiceImpl implements PersonaCuentaContableService {
	
	@EJB
	private PersonaCuentaContableDaoService personaCuentaContableDaoService;

	@EJB
	private AnticipoProveedorDaoService anticipoProveedorDaoService;

	@EJB
	private AnticipoClienteDaoService anticipoClienteDaoService;

	@PersistenceContext
	private EntityManager em;

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
			protegerSaldoInicialAnticipos(personaCuentaContable);
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
		protegerSaldoInicialAnticipos(personaCuentaContable);
		personaCuentaContable = personaCuentaContableDaoService.save(personaCuentaContable, personaCuentaContable.getCodigo());
		return personaCuentaContable;
	}

	/**
	 * ÍTEM 2 (docs/logica-negocio/cxp/DISENO-SALDO-GLOBAL-ANTICIPOS.md §3.2): el saldo de la
	 * cuenta de anticipos (tipoCuenta=2) no se edita a mano -- lo mueven los anticipos, los
	 * cruces y las devoluciones. Si la cuenta ES o PASA A SER tipo 2, protege su saldoInicial
	 * antes de grabar:
	 * <ul>
	 * <li>Edición de una fila tipo 2 ya persistida: se ignora el saldoInicial que manda el
	 * cliente, se conserva el persistido. No es error: el resto de los campos se graba
	 * normal.</li>
	 * <li>Alta, o una fila de otro tipo que pasa a ser tipo 2: el saldoInicial nace igual a la
	 * suma de los anticipos CONFIRMADOS del titular/empresa de la cuenta, según su rol.</li>
	 * </ul>
	 * @param personaCuentaContable : Cuenta a grabar (se modifica in-place antes de persistir)
	 * @throws Throwable            : Excepcion
	 */
	private void protegerSaldoInicialAnticipos(PersonaCuentaContable personaCuentaContable) throws Throwable {
		if (personaCuentaContable.getTipoCuenta() == null || personaCuentaContable.getTipoCuenta() != 2L) {
			return;
		}

		Long codigo = personaCuentaContable.getCodigo();
		PersonaCuentaContable persistida = (codigo != null)
				? personaCuentaContableDaoService.selectById(codigo, NombreEntidadesTesoreria.PERSONA_CUENTA_CONTABLE)
				: null;

		if (persistida != null && persistida.getTipoCuenta() != null && persistida.getTipoCuenta() == 2L) {
			// Edición de una fila tipo 2 ya persistida.
			Double enviado = personaCuentaContable.getSaldoInicial();
			Double persistido = persistida.getSaldoInicial();
			if (enviado != null && !enviado.equals(persistido)) {
				System.out.println("⚠ PRCC " + codigo + ": saldoInicial de anticipos ignorado ("
						+ enviado + "); se conserva " + persistido);
			}
			personaCuentaContable.setSaldoInicial(persistido);
			return;
		}

		// Alta, o una fila que pasa a ser tipo 2: nace cuadrada.
		personaCuentaContable.setSaldoInicial(saldoAnticiposConfirmados(personaCuentaContable));
	}

	/**
	 * Suma de los anticipos CONFIRMADOS del titular/empresa de la cuenta, según el rol que trae
	 * su {@link PersonaRol} ({@code rubroRolPersonaH}) -- el payload del FE sólo trae
	 * {@code {codigo}}, así que se carga por id. Proveedor -&gt; {@link AnticipoProveedorDaoService};
	 * cliente -&gt; {@link AnticipoClienteDaoService}; cualquier otro rol -&gt; 0 (no hay
	 * anticipos de ese rol).
	 * @param personaCuentaContable : Cuenta cuyo saldo inicial se calcula
	 * @return                      : Suma de saldos disponibles, 0.0 si no aplica
	 * @throws Throwable            : Excepcion
	 */
	private double saldoAnticiposConfirmados(PersonaCuentaContable personaCuentaContable) throws Throwable {
		if (personaCuentaContable.getPersonaRol() == null
				|| personaCuentaContable.getPersonaRol().getCodigo() == null
				|| personaCuentaContable.getEmpresa() == null
				|| personaCuentaContable.getEmpresa().getCodigo() == null) {
			return 0.0;
		}
		PersonaRol personaRol = em.find(PersonaRol.class, personaCuentaContable.getPersonaRol().getCodigo());
		if (personaRol == null || personaRol.getRubroRolPersonaH() == null || personaRol.getTitular() == null) {
			return 0.0;
		}

		Long idTitular = personaRol.getTitular().getCodigo();
		Long idEmpresa = personaCuentaContable.getEmpresa().getCodigo();
		long rol = personaRol.getRubroRolPersonaH();

		Double saldo;
		if (rol == RolPersona.PROVEEDOR) {
			saldo = anticipoProveedorDaoService.sumaSaldoDisponible(idTitular, idEmpresa);
		} else if (rol == RolPersona.CLIENTE) {
			saldo = anticipoClienteDaoService.sumaSaldoDisponible(idTitular, idEmpresa);
		} else {
			saldo = 0.0;
		}
		return (saldo != null) ? saldo : 0.0;
	}

}
