/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos  
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.cnt.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.PlanCuentaDaoService;
import com.saa.model.cnt.PlanCuenta;
import com.saa.rubros.Estado;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 *          Clase PlanCuentaDaoServiceImpl.
 */
@Stateless 
public class PlanCuentaDaoServiceImpl extends EntityDaoImpl<PlanCuenta>  implements PlanCuentaDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Dao (campos) Plan Cuenta");
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
							"detalleAsientos",
							"histDetalleAsientos",
							"naturalezaCuenta",
							"manejaCC"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#deleteByEmpresa(java.lang.Long)
	 */
	public void deleteByEmpresa(Long empresa) throws Throwable {
		System.out.println("Dao deleteByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" Delete b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa");
		query.setParameter("empresa", empresa);	
		query.executeUpdate();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked") 
	public List<PlanCuenta> selectByEmpresa(Long empresa) throws Throwable {
		System.out.println("Dao selectByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
								     " where  b.empresa.codigo = :empresa " +
								     " order by b.codigo");
		query.setParameter("empresa", empresa);	
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByEmpresaNaturaleza(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByEmpresaNaturaleza(Long empresa, Long naturaleza) throws Throwable {
		System.out.println("Dao selectByEmpresaNaturaleza de empresa: " + empresa + " y naturaleza = " + naturaleza);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.naturalezaCuenta.numero = :naturaleza");
		query.setParameter("empresa", empresa);
		query.setParameter("naturaleza", naturaleza);

		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByEmpresaManejaCC(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByEmpresaManejaCC(Long empresa) throws Throwable {
		System.out.println("Dao selectByEmpresaManejaCC de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa " +
									 " 		  and   b.naturalezaCuenta.manejaCentroCosto = :manejaCentroCosto");
		query.setParameter("empresa", empresa);
		query.setParameter("manejaCentroCosto", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByCuentaEmpresa(java.lang.String, java.lang.Long)
	 */
	public PlanCuenta selectByCuentaEmpresa(String cuenta, Long empresa) throws Throwable {
		System.out.println("Dao selectByCuentaEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa " +
									 " 		  and   b.cuentaContable = :cuenta");
		query.setParameter("empresa", empresa);
		query.setParameter("cuenta", cuenta);
		return (PlanCuenta)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectMaximaByCuenta(java.lang.Long, java.lang.String)
	 */
	public String selectMaximaByCuenta(Long empresa, String cuentaInicio, String siguienteNaturaleza) throws Throwable {
		System.out.println("Dao selectMaximaByCuenta con empresa : " + empresa);
		Query query = em.createQuery(" select   MAX(b.cuentaContable) " +
									 " from     PlanCuenta b " +
									 " where    b.empresa.codigo = :empresa " +
									 "          and   b.cuentaContable >= :cuentaInicio " +
									 "          and   b.cuentaContable < :siguienteNaturaleza");
		query.setParameter("empresa", empresa);
		query.setParameter("cuentaInicio", cuentaInicio);			
		query.setParameter("siguienteNaturaleza", siguienteNaturaleza);		
		return (String) query.getSingleResult();	
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectMovimientoByEmpresaCuentaFecha(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectMovimientoByEmpresaCuentaFecha(Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
															   String cuentaInicio,String cuentaFin) throws Throwable {
		System.out.println("Dao selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				 ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				 + cuentaInicio + ", cuentaFin: " + cuentaFin);
		Query query = em.createQuery(" select b " +
									 " from     PlanCuenta c " +
									 " where    c.codigo IN " +
									 "                      (select   distinct b.planCuenta.codigo " +
									 "                       from     DetalleAsiento b " +
									 " 						 where    b.asiento.empresa.codigo = :empresa " +
									 " 		    					  and   b.numeroCuenta between :cuentaInicio and :cuentaFin " +
									 "		    					  and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin) " +
									 " order by c.cuentaContable");
		query.setParameter("empresa", empresa);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaFin", cuentaFin);
		return query.getResultList();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByEmpresaCuentaFechaCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Dao selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		Query query = em.createQuery(" select b " +
									 " from     PlanCuenta c " +
				 					 " where    c.codigo IN " +
				 					 "                      (select   distinct b.planCuenta.codigo " +
									 " 						 from     DetalleAsiento b " +
									 " 						 where    b.asiento.empresa.codigo = :empresa " +
									 " 		    					  and   b.numeroCuenta between :cuentaInicio and :cuentaFin " +
									 " 		    					  and   b.centroCosto.numero between :centroInicio and :centroFin " +
									 "		    					  and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin)" +
									 " order by c.cuentaContable");
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
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectHijosByNumeroCuenta(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectHijosByNumeroCuenta(String numeroCuenta,
			Long idEmpresa) throws Throwable {
		System.out.println("Dao selectHijosByNumeroCuenta con numeroCuenta: " + numeroCuenta + ", idEmpresa: " + idEmpresa);
		Query query = em.createQuery(" select b " +
									 " from     PlanCuenta c " +
									 " where    c.empresa.codigo = :idEmpresa " +
									 "			and   c.cuentaContable LIKE :numeroCuenta " +
									 " order by c.cuentaContable");
		query.setParameter("idEmpresa", idEmpresa);
		if("0".equals(numeroCuenta)){
			query.setParameter("numeroCuenta", "%");	
		}else{
			query.setParameter("numeroCuenta", numeroCuenta + ".%");	
		}		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByRangoEmpresaEstado(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByRangoEmpresaEstado(Long empresa,
			String cuentaInicio, String cuentaHasta) throws Throwable {
		System.out.println("Dao selectByRangoEmpresaEstado con empresa : " + empresa + ", cuentaInicio" + cuentaInicio + 
				 ", cuentaHasta: " + cuentaHasta);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b" +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.cuentaContable between :cuentaInicio and :cuentaHasta " +
									 "        and   b.estado = :estado" +
									 " order by b.cuentaContable ");		
		query.setParameter("empresa", empresa);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaHasta", cuentaHasta);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectMovimientoByRangoEmpresaEstado(java.lang.Long, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectMovimientoByRangoEmpresaEstado(Long empresa,
			String cuentaInicio, String cuentaHasta) throws Throwable {
		System.out.println("Dao selectMovimientoByRangoEmpresaEstado con empresa : " + empresa + ", cuentaInicio" + cuentaInicio + 
				 ", cuentaHasta: " + cuentaHasta);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b" +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.cuentaContable between :cuentaInicio and :cuentaHasta " +
									 "        and   b.estado = :estado " +
									 "        and   b.naturalezaCuenta.manejaCentroCosto = :manejaCentro " +
									 "        and   b.tipo = :tipo " +
									 " order by b.cuentaContable ");		
		query.setParameter("empresa", empresa);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaHasta", cuentaHasta);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		query.setParameter("manejaCentro", Long.valueOf(Estado.ACTIVO));
		query.setParameter("tipo", Long.valueOf(TipoCuentaContable.MOVIMIENTO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#recuperaCuentasByRango(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectCuentasByRango(Long empresa, String cuentaInicio, String cuentaFin) throws Throwable {
		System.out.println("Dao selectCuentasByRango con empresa: " + empresa + ", cuentaInicio" + cuentaInicio + ", cuentaFin" + cuentaFin);
		Query query = em.createQuery(" select b " +
									 " from    PlanCuenta b" +
									 " where   b.empresa.codigo = :empresa" +
									 "		   and b.cuentaContable between :cuentaInicio and :cuentaFin");
		query.setParameter("empresa", empresa);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaFin", cuentaFin);
		return query.getResultList();
	}

	public String selectMaxCuentaByNumeroCuenta(Long empresa, String cuenta) throws Throwable {
		System.out.println("Dao selectMaxCuentaByNumeroCuenta con empresa: " + empresa + " con cuenta: " + cuenta);
		String planCuenta = null;
		Query query = em.createQuery(" select max(b.cuentaContable)" +
									 " from    PlanCuenta b" +
									 " where   b.empresa.codigo = :empresa" +
									 "		   and b.cuentaContable like :cuenta");
		query.setParameter("empresa", empresa);
		query.setParameter("cuenta", cuenta);		
		try {
			planCuenta = query.getSingleResult().toString();
		} catch (NoResultException e) {
			planCuenta = null;
		}
		return planCuenta;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByEmpresaSinRaiz(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByEmpresaSinRaiz(Long empresa)
			throws Throwable {
		System.out.println("Dao selectByEmpresaSinRaiz de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.nivel > :raiz ");
		query.setParameter("empresa", empresa);		
		query.setParameter("raiz", Long.valueOf(TipoCuentaContable.RAIZ));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectRaizByEmpresa(java.lang.Long)
	 */
	public PlanCuenta selectRaizByEmpresa(Long empresa) throws Throwable {
		System.out.println("Dao selectRaizByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.nivel = 0 ");
		query.setParameter("empresa", empresa);		
		return (PlanCuenta)query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#numeroRegActivosByIdPadre(java.lang.Long)
	 */
	public int numeroRegActivosByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Dao numeroRegActivosByIdPadre con idPadre: " + idPadre);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.idPadre = :idPadre" +
									 "        and   b.estado = :estado ");
		query.setParameter("idPadre", idPadre);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));

		return query.getResultList().size();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectActivasByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectActivasByEmpresa(Long empresa)
			throws Throwable {
		System.out.println("Dao selectActivasByEmpresa de empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.empresa.codigo = :empresa" +
									 "        and   b.estado = :estado " +
									 " order by b.nivel");
		query.setParameter("empresa", empresa);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PlanCuentaDaoService#selectByIdPadre(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<PlanCuenta> selectByIdPadre(Long idPadre) throws Throwable {
		System.out.println("Dao selectByIdPadre de idPadre: " + idPadre);
		Query query = em.createQuery(" select b " +
							 		 " from   PlanCuenta b " +
								     " where  b.idPadre = :idPadre ");
		query.setParameter("idPadre", idPadre);	
		return query.getResultList();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public List<PlanCuenta> selectByIdNaturalezaCuenta(Long idNaturaleza) throws Throwable {
		System.out.println("selectNaturalezaCuenta: ");
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.naturalezaCuenta.codigo = :idNaturaleza");
		query.setParameter("idNaturaleza", idNaturaleza);		
		return query.getResultList();
	}

	@SuppressWarnings("unchecked") /* hizo mely*/
	@Override
	public List<PlanCuenta> selectByNivelPlanCuenta(Long idNaturaleza, Long nivel) throws Throwable {
		System.out.println(" selectByNivelPlanCuenta de" + idNaturaleza + nivel);
		Query query = em.createQuery(" select b " +
									 " from   PlanCuenta b " +
									 " where  b.naturalezaCuenta.codigo = :idNaturaleza" +
									 "        and   b.nivel = :nivel ");
		query.setParameter("idNaturaleza", idNaturaleza);		
		query.setParameter("nivel", nivel);
		return query.getResultList();
	}
	
}
