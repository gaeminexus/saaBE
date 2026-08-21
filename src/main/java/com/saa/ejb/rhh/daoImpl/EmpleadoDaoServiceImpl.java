/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.model.rhh.Empleado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion EmpleadoDaoService. 
 */
@Stateless
public class EmpleadoDaoServiceImpl extends EntityDaoImpl<Empleado>  implements EmpleadoDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.EmpleadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Empleado");
		return new String[]{"codigo",
							"identificacion",
							"apellidos",
							"nombres",
							"fechaNacimiento",
							"email",
							"telefono",
							"direccion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro",
							"empresa",
							"tipoIdentificacion",
							"estadoCivil",
							"genero",
							"nacionalidad",
							"nivelInstruccion",
							"profesion",
							"tipoSangre",
							"discapacidad",
							"porcentajeDiscapacidad",
							"carneConadis",
							"enfermedadCatastrofica",
							"codigoAfiliacion",
							"fechaIngreso",
							"region",
							"codigoBiometrico",
							"contactoEmergencia",
							"telefonoEmergencia",
							"centroCosto",
							"foto"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.EmpleadoDaoService#selectByIdentificacion(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Empleado selectByIdentificacion(String identificacion, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdentificacion de Empleado: " + identificacion
				+ ", empresa: " + idEmpresa);
		if (identificacion == null || identificacion.trim().isEmpty()) {
			return null;
		}
		// La empresa se compara con OR a null porque los empleados cargados antes de
		// que MPLD tuviera PJRQCDGO (script 05) todavia no la tienen asignada.
		Query query = em.createQuery(" select   t "
				+ " from     Empleado t "
				+ " where    trim(t.identificacion) = :identificacion "
				+ "          and (t.empresa is null or t.empresa.codigo = :idEmpresa) ");
		query.setParameter("identificacion", identificacion.trim());
		query.setParameter("idEmpresa", idEmpresa);
		List<Empleado> encontrados = query.getResultList();
		// Una identificacion duplicada es un problema de datos, no algo que este DAO
		// deba resolver eligiendo: devuelve null y la validacion lo reporta.
		if (encontrados == null || encontrados.size() != 1) {
			return null;
		}
		return encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.EmpleadoDaoService#selectByCodigoBiometrico(java.lang.String, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Empleado selectByCodigoBiometrico(String codigoBiometrico, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoBiometrico de Empleado, codigo: "
				+ codigoBiometrico);
		if (codigoBiometrico == null || codigoBiometrico.trim().isEmpty()) {
			return null;
		}
		Query query = em.createQuery(" select t "
				+ " from   Empleado t "
				+ " where  t.codigoBiometrico = :codigoBiometrico "
				+ "        and (t.empresa.codigo = :idEmpresa or t.empresa is null) ");
		query.setParameter("codigoBiometrico", codigoBiometrico.trim());
		query.setParameter("idEmpresa", idEmpresa);
		List<Empleado> lista = query.getResultList();
		if (lista.size() != 1) {
			// Cero o mas de uno: en ambos casos el importador debe decidir, no el DAO.
			return null;
		}
		return lista.get(0);
	}
}
