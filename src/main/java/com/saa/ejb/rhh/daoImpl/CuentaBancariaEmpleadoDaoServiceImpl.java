package com.saa.ejb.rhh.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService;
import com.saa.model.rhh.CuentaBancariaEmpleado;

import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion CuentaBancariaEmpleadoDaoService.
 */
@Stateless
public class CuentaBancariaEmpleadoDaoServiceImpl extends EntityDaoImpl<CuentaBancariaEmpleado> implements CuentaBancariaEmpleadoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CuentaBancariaEmpleado");
		return new String[]{"codigo",
							"empleado",
							"banco",
							"tipoCuenta",
							"numeroCuenta",
							"titular",
							"identificacionTitular",
							"principal",
							"porcentaje",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService#selectActivasByEmpleado(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<CuentaBancariaEmpleado> selectActivasByEmpleado(Long idEmpleado) throws Throwable {
		System.out.println("Ingresa al metodo selectActivasByEmpleado de CuentaBancariaEmpleado, empleado: "
				+ idEmpleado);
		Query query = em.createQuery(" select   t "
				+ " from     CuentaBancariaEmpleado t "
				+ " where    t.empleado.codigo = :idEmpleado "
				+ "          and (t.estado is null or t.estado = :activo) "
				+ " order by case when t.principal = 'S' then 0 else 1 end, t.codigo ");
		query.setParameter("idEmpleado", idEmpleado);
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

}
