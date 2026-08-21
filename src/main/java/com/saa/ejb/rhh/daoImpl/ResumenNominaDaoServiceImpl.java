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
import com.saa.ejb.rhh.dao.ResumenNominaDaoService;
import com.saa.model.rhh.ResumenNomina;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ResumenNominaDaoService. 
 */
@Stateless
public class ResumenNominaDaoServiceImpl extends EntityDaoImpl<ResumenNomina>  implements ResumenNominaDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ResumenNominaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ResumenNomina");
		return new String[]{"codigo",
							"empleado",
							"fecha",
							"horaEntrada",
							"horaSalida",
							"minutosTarde",
							"minutosExtra",
							"ausencia",
							"justificado",
							"fuente",
							"tipoAusencia",
							"horasTrabajadas",
							"horasSuplementarias",
							"horasExtraordinarias",
							"horasNocturnas",
							"minutosSalidaAnticipada",
							"entradaReal",
							"salidaReal",
							"inconsistente",
							"procesado",
							"justificacion",
							"fechaRegistro",
							"usuarioRegistro"};
	}
	

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ResumenNominaDaoService#contarDiasAusenciaNoRemunerada(java.lang.Long, java.time.LocalDate, java.time.LocalDate, java.util.List)
	 */
	@Override
	public Double contarDiasAusenciaNoRemunerada(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta, List<Long> tipos) throws Throwable {
		System.out.println("Ingresa al metodo contarDiasAusenciaNoRemunerada de ResumenNomina, empleado: "
				+ idEmpleado + ", del " + desde + " al " + hasta);
		if (tipos == null || tipos.isEmpty()) {
			return Double.valueOf(0D);
		}
		Query query = em.createQuery(" select   count(t) "
				+ " from     ResumenNomina t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.fecha between :desde and :hasta "
				+ "          and t.tipoAusencia in (:tipos) ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		query.setParameter("tipos", tipos);
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}
}
