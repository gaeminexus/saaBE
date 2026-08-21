package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.HoraExtraDaoService;
import com.saa.model.rhh.HoraExtra;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion HoraExtraDaoService.
 */
@Stateless
public class HoraExtraDaoServiceImpl extends EntityDaoImpl<HoraExtra> implements HoraExtraDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.HoraExtraDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) HoraExtra");
		return new String[]{"codigo",
							"empleado",
							"resumenNomina",
							"periodoNomina",
							"tipoHoraExtra",
							"fecha",
							"horas",
							"valorHora",
							"recargo",
							"valor",
							"aprobada",
							"usuarioAprueba",
							"fechaAprobacion",
							"excedeTope",
							"observacion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.HoraExtraDaoService#selectAprobadasPendientes(java.lang.Long, java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<HoraExtra> selectAprobadasPendientes(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectAprobadasPendientes de HoraExtra, empleado: " + idEmpleado
				+ ", del " + desde + " al " + hasta);
		Query query = em.createQuery(" select   t "
				+ " from     HoraExtra t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.fecha between :desde and :hasta "
				+ "          and t.aprobada = 'S' "
				+ "          and t.estado = 1 "
				+ "          and t.periodoNomina is null "
				+ " order by t.fecha, t.tipoHoraExtra ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}
}
