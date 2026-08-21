package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService;
import com.saa.model.rhh.LiquidacionBeneficioSocial;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion LiquidacionBeneficioSocialDaoService.
 */
@Stateless
public class LiquidacionBeneficioSocialDaoServiceImpl extends EntityDaoImpl<LiquidacionBeneficioSocial> implements LiquidacionBeneficioSocialDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) LiquidacionBeneficioSocial");
		return new String[]{"codigo",
							"empleado",
							"tipoBeneficio",
							"anio",
							"fechaInicio",
							"fechaFin",
							"baseCalculo",
							"dias",
							"valor",
							"valorMensualizado",
							"valorPagado",
							"periodoNomina",
							"fechaPago",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.LiquidacionBeneficioSocialDaoService#selectByEmpleadoTipoAnio(java.lang.Long, java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public LiquidacionBeneficioSocial selectByEmpleadoTipoAnio(Long idEmpleado, Long tipoBeneficio,
			Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpleadoTipoAnio de LiquidacionBeneficioSocial, empleado: "
				+ idEmpleado + ", tipo: " + tipoBeneficio + ", anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     LiquidacionBeneficioSocial t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.tipoBeneficio = :tipoBeneficio "
				+ "          and t.anio = :anio ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("tipoBeneficio", tipoBeneficio);
		query.setParameter("anio", anio);
		List<LiquidacionBeneficioSocial> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}
}
