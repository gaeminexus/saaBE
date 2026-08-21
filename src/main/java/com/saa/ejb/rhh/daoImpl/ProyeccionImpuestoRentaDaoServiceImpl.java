package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService;
import com.saa.model.rhh.ProyeccionImpuestoRenta;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion ProyeccionImpuestoRentaDaoService.
 */
@Stateless
public class ProyeccionImpuestoRentaDaoServiceImpl extends EntityDaoImpl<ProyeccionImpuestoRenta> implements ProyeccionImpuestoRentaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) ProyeccionImpuestoRenta");
		return new String[]{"codigo",
							"empleado",
							"anio",
							"mesDesde",
							"ingresosRealizados",
							"ingresosFuturos",
							"ingresosProyectados",
							"aportePersonalProyectado",
							"baseImponible",
							"impuestoCausado",
							"gastosDeclarados",
							"topeGastos",
							"rebaja",
							"impuestoAPagar",
							"retencionesEfectuadas",
							"mesesRestantes",
							"retencionMensual",
							"numeroCargas",
							"enfermedadCatastrofica",
							"vigente",
							"motivo",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService#selectVigente(java.lang.Long, java.lang.Integer)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ProyeccionImpuestoRenta selectVigente(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo selectVigente de ProyeccionImpuestoRenta, empleado: "
				+ idEmpleado + ", anio: " + anio);
		Query query = em.createQuery(" select   t "
				+ " from     ProyeccionImpuestoRenta t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.anio = :anio "
				+ "          and t.vigente = 'S' "
				+ " order by t.mesDesde desc, t.codigo desc ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		List<ProyeccionImpuestoRenta> encontrados = query.getResultList();
		return (encontrados == null || encontrados.isEmpty()) ? null : encontrados.get(0);
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService#desmarcaVigentes(java.lang.Long, java.lang.Integer)
	 */
	@Override
	public int desmarcaVigentes(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo desmarcaVigentes de ProyeccionImpuestoRenta, empleado: "
				+ idEmpleado + ", anio: " + anio);
		Query query = em.createQuery(" update ProyeccionImpuestoRenta t "
				+ " set    t.vigente = 'N' "
				+ " where  t.empleado.codigo = :idEmpleado "
				+ "        and t.anio = :anio "
				+ "        and t.vigente = 'S' ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		return query.executeUpdate();
	}
}
