package com.saa.ejb.contabilidad.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.CentroCostoDaoService;
import com.saa.model.cnt.CentroCosto;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

@Stateless
public class CentroCostoDaoServiceImpl extends EntityDaoImpl<CentroCosto>  implements CentroCostoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"nombre",
							"numero",
							"tipo",
							"nivel",
							"idPadre",
							"estado",
							"empresa",
							"fechaInactivo",
							"fechaIngreso",
							"histDetalleAsientos",
							"detalleAsientos"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo deleteByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" Delete from CentroCosto b " +
									 " where  b.empresa.codigo = :empresa");
		query.setParameter("empresa", empresa);		
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectMovimientosByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectMovimientosByEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectMovimientosByEmpresa de empresa: " + empresa);
		List<CentroCosto> resultado = null;
		Query query = em.createQuery(" select b " +
				                     " from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.tipo = :tipo " +
									 "        and   b.nivel != :nivel " +
									 "        and   b.estado = :estado " +
									 " order by b.numero");
		query.setParameter("empresa", empresa);		
		query.setParameter("tipo", Long.valueOf(TipoCuentaContable.MOVIMIENTO));
		query.setParameter("nivel", Long.valueOf(Estado.RAIZ));
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		try {
			resultado = query.getResultList();
		} catch (PersistenceException e) {
			throw new IncomeException("ERROR EN selectMovimientosByEmpresa: " + e.getCause());
		}
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectByCuentaEmpresa(java.lang.String, java.lang.Long)
	 */
	public CentroCosto selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByCuentaEmpresa de empresa: " + empresa + ", y cuenta: " + cuenta);
		Query query = em.createQuery(" select b " +
									 " from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   numero = :cuenta ");
		query.setParameter("empresa", empresa);		
		query.setParameter("cuenta", cuenta);

		return (CentroCosto)query.getSingleResult();
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectByEmpresaCuentaFechaCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		Query query = em.createQuery(" from     CentroCosto c " +
				 					 " where    c.codigo IN " +
				 					 "                      (select   distinct b.centroCosto.codigo " +
									 " 						 from     DetalleAsiento b " +
									 " 						 where    b.asiento.empresa.codigo = :empresa " +
									 " 		    					  and   b.numeroCuenta between :cuentaInicio and :cuentaFin " +
									 " 		    					  and   b.centroCosto.numero between :centroInicio and :centroFin " +
									 "		    					  and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin)" +
									 " order by c.numero");
		query.setParameter("empresa", empresa);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaFin", cuentaFin);
		query.setParameter("centroInicio", centroInicio);
		query.setParameter("centroFin", centroFin);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectHijosByNumeroCuenta(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectHijosByNumeroCuenta(String numeroCentro,
			Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al Metodo selectHijosByNumeroCuenta con numeroCentro: " + numeroCentro + ", idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" select c " +
									 " from     CentroCosto c " +
									 " where    c.empresa.codigo = :idEmpresa " +
									 "			and   c.numero LIKE :numeroCentro " +
									 " order by c.numero");
		query.setParameter("idEmpresa", idEmpresa);
		if("0".equals(numeroCentro)){
			query.setParameter("numeroCentro", "%");	
		}else{
			query.setParameter("numeroCentro", numeroCentro + ".%");	
		}		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectByEmpresaSinRaiz(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectByEmpresaSinRaiz(Long idEmpresa)
			throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaSinRaiz de empresa: " + idEmpresa);
		Query query = em.createQuery(" from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   nivel > :nivel ");
		query.setParameter("empresa", idEmpresa);		
		query.setParameter("nivel", Long.valueOf(TipoCuentaContable.RAIZ));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectRaizByEmpresa(java.lang.Long)
	 */
	public CentroCosto selectRaizByEmpresa(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectRaizByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.nivel = :raiz ");
		query.setParameter("empresa", empresa);		
		query.setParameter("raiz", Long.valueOf(TipoCuentaContable.RAIZ));

		return (CentroCosto)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#numeroRegActivosByIdPadre(java.lang.Long)
	 */
	public int numeroRegActivosByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Ingresa al metodo numeroRegActivosByIdPadre con idPadre: " + idPadre);
		Query query = em.createQuery(" select b " +
									 " from   CentroCosto b " +
									 " where  b.idPadre = :idPadre" +
									 "        and   b.estado = :estado ");
		query.setParameter("idPadre", idPadre);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));

		return query.getResultList().size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectByIdPadre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdPadre con idPadre: " + idPadre);
		Query query = em.createQuery(" from   CentroCosto b " +
									 " where  b.idPadre = :idPadre");
		query.setParameter("idPadre", idPadre);		

		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresa con idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" select b " +
									 " from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa");
		query.setParameter("empresa", idEmpresa);		

		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.CentroCostoDaoService#selectActivosByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<CentroCosto> selectActivosByEmpresa(Long idEmpresa)
			throws Throwable {
		System.out.println("Ingresa al metodo selectActivosByEmpresa con idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" select b " +
									 " from   CentroCosto b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.estado = :estado " +
									 " order by b.nivel");
		query.setParameter("empresa", idEmpresa);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		
		return query.getResultList();
	}
}
