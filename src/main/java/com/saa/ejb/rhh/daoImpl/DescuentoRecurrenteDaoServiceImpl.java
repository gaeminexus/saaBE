package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.model.rhh.DescuentoRecurrente;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion DescuentoRecurrenteDaoService.
 */
@Stateless
public class DescuentoRecurrenteDaoServiceImpl extends EntityDaoImpl<DescuentoRecurrente> implements DescuentoRecurrenteDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DescuentoRecurrente");
		return new String[]{"codigo",
							"empleado",
							"conceptoNomina",
							"tipoDescuento",
							"numero",
							"valor",
							"saldo",
							"numeroCuotas",
							"cuotasPagadas",
							"valorCuota",
							"porcentaje",
							"fechaInicio",
							"fechaFin",
							"beneficiario",
							"observacion",
							"aperturaMigracion",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService#selectVigentesByEmpleado(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<DescuentoRecurrente> selectVigentesByEmpleado(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectVigentesByEmpleado de DescuentoRecurrente, empleado: "
				+ idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     DescuentoRecurrente t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and (t.fechaFin is null or t.fechaFin >= :hoy) "
				+ " order by t.codigo ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("hoy", LocalDate.now());
		return query.getResultList();
	}
}
