package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CuotaDescuentoDaoService;
import com.saa.model.rhh.CuotaDescuento;
import com.saa.rubros.RhhEstadoCuotaDescuento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion CuotaDescuentoDaoService.
 */
@Stateless
public class CuotaDescuentoDaoServiceImpl extends EntityDaoImpl<CuotaDescuento> implements CuotaDescuentoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CuotaDescuentoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CuotaDescuento");
		return new String[]{"codigo",
							"descuentoRecurrente",
							"numeroCuota",
							"fechaVencimiento",
							"total",
							"capital",
							"interes",
							"valorDescontado",
							"saldo",
							"periodoNomina",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CuotaDescuentoDaoService#selectPendientesPorVencer(java.lang.Long, java.time.LocalDate, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<CuotaDescuento> selectPendientesPorVencer(Long idEmpleado, java.time.LocalDate desde,
			java.time.LocalDate hasta) throws Throwable {
		System.out.println("Ingresa al metodo selectPendientesPorVencer de CuotaDescuento, empleado: "
				+ idEmpleado + ", del " + desde + " al " + hasta);
		// PENDIENTE y PARCIAL: la cuota parcial vuelve a intentarse en el siguiente periodo.
		Query query = em.createQuery(" select   t "
				+ " from     CuotaDescuento t "
				+ " where    t.descuentoRecurrente.empleado.codigo = :idEmpleado "
				+ "          and t.descuentoRecurrente.estado = 1 "
				+ "          and t.fechaVencimiento between :desde and :hasta "
				+ "          and t.estado in (:pendiente, :parcial) "
				+ " order by t.descuentoRecurrente.conceptoNomina.orden, t.numeroCuota ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("pendiente", Long.valueOf(RhhEstadoCuotaDescuento.PENDIENTE));
		query.setParameter("parcial", Long.valueOf(RhhEstadoCuotaDescuento.PARCIAL));
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
		return query.getResultList();
	}
}
