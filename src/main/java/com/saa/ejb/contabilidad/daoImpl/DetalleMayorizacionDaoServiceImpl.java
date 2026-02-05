package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionDaoService;
import com.saa.model.cnt.DetalleMayorizacion;
import com.saa.rubros.GrupoCuentasBasicas;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleMayorizacionDaoServiceImpl extends EntityDaoImpl<DetalleMayorizacion>  implements DetalleMayorizacionDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"mayorizacion",
							"planCuenta",
							"saldoAnterior",
							"valorDebe",
							"valorHaber",
							"saldoActual",
							"numeroCuenta",
							"codigoPadreCuenta",
							"nombreCuenta",
							"tipoCuenta",
							"nivelCuenta"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectByMayorizacionCuenta(java.lang.Long, java.lang.Long)
	 */
	public DetalleMayorizacion selectByMayorizacionCuenta(Long mayorizacion, Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacionCuenta de mayorizacion: " + mayorizacion + ", cuenta: " + cuenta);
		DetalleMayorizacion resultado = new DetalleMayorizacion();
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :mayorizacion " +
									 "        and   b.planCuenta.codigo = :cuenta");
		query.setParameter("mayorizacion", mayorizacion);		
		query.setParameter("cuenta", cuenta);
		try {
			resultado = (DetalleMayorizacion)query.getSingleResult();
		} catch (NoResultException e) {
			resultado = null;
		} 
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectNivelesByMayorizacion(java.lang.Long)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectNivelesByMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectNivelesByMayorizacion de mayorizacion: " + mayorizacion);
		Query query = em.createQuery(" select   distinct b.nivelCuenta " +
									 " from     DetalleMayorizacion b " +
									 " where    b.mayorizacion.codigo = :mayorizacion " +
									 "          and   b.nivelCuenta > 1 " +
									 " order by b.nivelCuenta desc");
		query.setParameter("mayorizacion", mayorizacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectPadresByNivel(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectPadresByNivel(Long mayorizacion, Long nivel) throws Throwable {
		System.out.println("Ingresa al metodo selectPadresByNivel de mayorizacion: " + mayorizacion + " y nivel: " + nivel);
		Query query = em.createQuery(" select   distinct b.codigoPadreCuenta " +
									 " from     DetalleMayorizacion b " +
									 " where    b.mayorizacion.codigo = :mayorizacion " +
									 "          and   b.nivelCuenta = :nivel " +
									 " order by b.codigoPadreCuenta ");
		query.setParameter("mayorizacion", mayorizacion);
		query.setParameter("nivel", nivel);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectByCuentaPadreNivel(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleMayorizacion> selectByCuentaPadreNivel(Long mayorizacion, Long nivel, Long padre) throws Throwable {
		System.out.println("Ingresa al metodo selectByPadre de mayorizacion: " + mayorizacion + " y nivel: " + nivel);
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :mayorizacion " +
									 "        and   b.nivelCuenta = :nivel " +
									 "        and   b.codigoPadreCuenta = :padre");
		query.setParameter("mayorizacion", mayorizacion);
		query.setParameter("nivel", nivel);
		query.setParameter("padre", padre);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectForCierre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleMayorizacion> selectForCierre(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectForCierre de mayorizacion: " + mayorizacion);
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :mayorizacion " +
									 "        and   b.planCuenta.naturalezaCuenta.numero in (:ingresos, :egresos) " +
									 "        and   b.planCuenta.tipo = :tipo " +
									 "        and   b.saldoActual != 0 " +
									 " order by b.numeroCuenta");
		query.setParameter("mayorizacion", mayorizacion);
		query.setParameter("ingresos", Long.valueOf(GrupoCuentasBasicas.INGRESOS));
		query.setParameter("egresos", Long.valueOf(GrupoCuentasBasicas.GASTOS));
		query.setParameter("tipo", Long.valueOf(TipoCuentaContable.MOVIMIENTO));		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectByCodigoMayorizacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleMayorizacion> selectByCodigoMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoMayorizacion de mayorizacion: " + mayorizacion);
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :mayorizacion ");
		query.setParameter("mayorizacion", mayorizacion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#deleteByMayorizacion(java.lang.Long)
	 */
	public void deleteByMayorizacion(Long idMayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacion de mayorizacion: " + idMayorizacion);
		Query query = em.createQuery(" Delete " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :idMayorizacion ");
		query.setParameter("idMayorizacion", idMayorizacion);
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleMayorizacionDaoService#selectMovimientosByMayorizacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleMayorizacion> selectMovimientosByMayorizacion(Long mayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectMovimientosByMayorizacion de mayorizacion: " + mayorizacion);
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.mayorizacion.codigo = :mayorizacion " +
									 " 		  and   b.tipoCuenta = :tipoCuenta");
		query.setParameter("mayorizacion", mayorizacion);
		query.setParameter("tipoCuenta", Long.valueOf(TipoCuentaContable.MOVIMIENTO));
		
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleMayorizacion> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdPlanCuenta de idPlanCuenta: " + idPlanCuenta);
		Query query = em.createQuery(" select b " +
									 " from   DetalleMayorizacion b " +
									 " where  b.planCuenta.codigo = :idPlanCuenta ");
		query.setParameter("idPlanCuenta", idPlanCuenta);		
		return query.getResultList();
	}
	
}
