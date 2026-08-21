package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CargaFamiliarDaoService;
import com.saa.model.rhh.CargaFamiliar;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion CargaFamiliarDaoService.
 */
@Stateless
public class CargaFamiliarDaoServiceImpl extends EntityDaoImpl<CargaFamiliar> implements CargaFamiliarDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CargaFamiliarDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CargaFamiliar");
		return new String[]{"codigo",
							"empleado",
							"parentesco",
							"identificacion",
							"apellidos",
							"nombres",
							"fechaNacimiento",
							"discapacidad",
							"porcentajeDiscapacidad",
							"calificaIr",
							"calificaUtilidades",
							"dependeEconomicamente",
							"fechaInicio",
							"fechaFin",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CargaFamiliarDaoService#contarVigentesParaIr(java.lang.Long, java.time.LocalDate)
	 */
	@Override
	public Integer contarVigentesParaIr(Long idEmpleado, java.time.LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al metodo contarVigentesParaIr de CargaFamiliar, empleado: " + idEmpleado);
		Query query = em.createQuery(" select   count(t) "
				+ " from     CargaFamiliar t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.calificaIr = 'S' "
				+ "          and t.estado = 1 "
				+ "          and (t.fechaInicio is null or t.fechaInicio <= :fecha) "
				+ "          and (t.fechaFin is null or t.fechaFin >= :fecha) ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("fecha", fecha);
		Object resultado = query.getSingleResult();
		return resultado == null ? Integer.valueOf(0) : Integer.valueOf(resultado.toString());
	}
}
