package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService;
import com.saa.model.rhh.TablaImpuestoRenta;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion TablaImpuestoRentaDaoService.
 */
@Stateless
public class TablaImpuestoRentaDaoServiceImpl extends EntityDaoImpl<TablaImpuestoRenta> implements TablaImpuestoRentaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) TablaImpuestoRenta");
		return new String[]{"codigo",
							"empresa",
							"anio",
							"orden",
							"fraccionBasica",
							"excesoHasta",
							"impuestoFraccionBasica",
							"porcentaje",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService#selectTramo(java.lang.Long, java.lang.Integer, java.lang.Double)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public TablaImpuestoRenta selectTramo(Long idEmpresa, Integer anio, Double base) throws Throwable {
		System.out.println("Ingresa al metodo selectTramo de TablaImpuestoRenta, anio: " + anio
				+ ", base: " + base);
		Query query = em.createQuery(" select   t "
				+ " from     TablaImpuestoRenta t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.estado = 1 "
				+ "          and t.fraccionBasica <= :base "
				+ "          and (t.excesoHasta is null or t.excesoHasta > :base) "
				+ " order by t.orden ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		query.setParameter("base", base);
		List<TablaImpuestoRenta> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.TablaImpuestoRentaDaoService#selectByAnio(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<TablaImpuestoRenta> selectByAnio(Long idEmpresa, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByAnio de TablaImpuestoRenta, anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     TablaImpuestoRenta t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.anio = :anio "
				+ "          and t.estado = 1 "
				+ " order by t.orden ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("anio", anio);
		return query.getResultList();
	}
}
