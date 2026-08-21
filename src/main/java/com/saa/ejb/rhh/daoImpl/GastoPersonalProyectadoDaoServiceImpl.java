package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.GastoPersonalProyectadoDaoService;
import com.saa.model.rhh.GastoPersonalProyectado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion GastoPersonalProyectadoDaoService.
 */
@Stateless
public class GastoPersonalProyectadoDaoServiceImpl extends EntityDaoImpl<GastoPersonalProyectado> implements GastoPersonalProyectadoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.GastoPersonalProyectadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) GastoPersonalProyectado");
		return new String[]{"codigo",
							"empleado",
							"anio",
							"tipoGasto",
							"valor",
							"fechaPresentacion",
							"vigente",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}


	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.GastoPersonalProyectadoDaoService#sumaVigentes(java.lang.Long, java.lang.Integer)
	 */
	@Override
	public Double sumaVigentes(Long idEmpleado, Integer anio) throws Throwable {
		System.out.println("Ingresa al metodo sumaVigentes de GastoPersonalProyectado, empleado: "
				+ idEmpleado + ", anio: " + anio);
		Query query = em.createQuery(" select   sum(t.valor) "
				+ " from     GastoPersonalProyectado t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and t.anio = :anio "
				+ "          and t.vigente = 'S' "
				+ "          and t.estado = 1 ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("anio", anio);
		Object resultado = query.getSingleResult();
		return resultado == null ? Double.valueOf(0D) : Double.valueOf(resultado.toString());
	}
}
