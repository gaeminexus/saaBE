package com.saa.ejb.cnt.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.PeriodoDaoService;
import com.saa.model.cnt.Periodo;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.ProcesosMayorizacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class PeriodoDaoServiceImpl extends EntityDaoImpl<Periodo>  implements PeriodoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"empresa",
							"mes",
							"anio",
							"nombre",
							"estado",
							"idMayorizacion",
							"idDesmayorizacion",
							"idMayorizacionCierre",
							"idDesmayorizacionCierre",
							"periodoCierre",
							"primerDia",
							"ultimoDia",};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<String> selectPeriodo(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectPeriodo con empresa: " + empresa);
		Query query = em.createQuery(" select   distinct b.numeroMes, c.nombre, b.numeroAnio " +
									 " from     Periodo c, Asiento b " +
									 " where    c.codigo = b.periodo.codigo " +
									 "          AND b.empresa.codigo = :empresa " +
									 " order by b.numeroMes");
		query.setParameter("empresa", empresa);		
		return query.getResultList(); 
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#periodoMayorizacionDesmayorizacion(int, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> periodoMayorizacionDesmayorizacion(int proceso, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectPeriodo con empresa: " + empresa);
		String estados = null;
		String recupera = null;
		if (proceso == ProcesosMayorizacion.MAYORIZACION) {
			estados = "(" + EstadoPeriodos.ACTIVO + "," + EstadoPeriodos.DESMAYORIZADO + ")";
			recupera = "Min";
		}else{
			estados = "(" + EstadoPeriodos.MAYORIZADO + ")";
			recupera = "Max";
		}
		Query query = em.createQuery(" select " + recupera + "(b.codigo) " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        AND b.estado IN " + estados);
		query.setParameter("empresa", empresa);		
		return query.getResultList(); 
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectRangoPeriodos(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectRangoPeriodos(Long empresa, Long periodoInicia, Long periodoFin, int proceso) throws Throwable {
		System.out.println("Ingresa al metodo selectRangoPeriodos con empresa: " + empresa);
		String orden = null;
		if ((proceso == ProcesosMayorizacion.MAYORIZACION) || (proceso == ProcesosMayorizacion.MAYORIZACION_CIERRE)) {
			orden = "asc";
		}else{
			orden = "desc";
		}
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where   b.empresa.codigo = :empresa " +
									 "         and (b.codigo between :inicio and :fin) " +
									 " order by b.codigo " + orden);
		query.setParameter("empresa", empresa);		
		query.setParameter("inicio", periodoInicia);
		query.setParameter("fin", periodoFin);
		return query.getResultList(); 
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectRecuperaAnio(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> selectRecuperaAnio(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectRecuperaAnio con empresa: " + empresa);
		Query query = em.createQuery(" select   distinct b.anio " +
									 " from     Periodo b " +
									 " where    b.empresa.codigo = :empresa " +
									 " order by b.anio");
		query.setParameter("empresa", empresa);
		return query.getResultList(); 
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectByMesAnioEmpresa(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Periodo selectByMesAnioEmpresa(Long empresa, Long mes, Long anio) throws Throwable {
		System.out.println("Ingresa al metodo selectByMesAnioEmpresa con empresa: " + empresa + ", mes" +mes+ " y año " + anio);
		Periodo periodo = null;
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.anio = :anio " +
									 "        and   b.mes = :mes");
		query.setParameter("empresa", empresa);
		query.setParameter("anio", anio);
		query.setParameter("mes", mes);
		try {
			periodo = (Periodo) query.getSingleResult();
		} catch (NoResultException e) {
			throw new IncomeException("NO EXISTE EL PERIODO CREADO PARA ESTE ASIENTO");
		}
		return periodo; 		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectByEmpresaMaxFecha(java.lang.Long)
	 */
	public Periodo selectByEmpresaMaxFecha(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaMaxFecha con empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.codigo = ( select   max(p.codigo) " +
					                 "                     from     Periodo p " +
						             "                     where    p.empresa.codigo = :empresa)");
		query.setParameter("empresa", empresa);
		return (Periodo) query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectMayorizacionDesmayorizacionByIdPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectByPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al Metodo selectMayorizacionDesmayorizacionByIdPeriodo con idPeriodo : " + idPeriodo);
		Query query = em.createQuery(" select b " +
									 " from     Periodo b " +
									 " where    b.codigo = :idPeriodo");
		query.setParameter("idPeriodo", idPeriodo);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectMaximoAnteriorByEstadoEmpresa(java.lang.Long, int, java.util.LocalDate)
	 */
	public Periodo selectMaximoAnteriorByEstadoEmpresa(Long empresa,
			int estado, LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al Metodo selectMaximoAnteriorByEstadoEmpresa con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		Periodo periodo = new Periodo();
		String sentencia = " select b " +
						   " from   Periodo b " +
		  			       " where	b.codigo = ( Select   Max(c.codigo)" +
		  			       "					 from     Periodo c" +
		  			       "					 where    c.empresa.codigo = :empresa " +
		  			       "							  and   c.ultimoDia < :fecha ";
		if(estado != 0){
			sentencia += " and   c.estado = :estado";
		}
		sentencia += ")";
		Query query = em.createQuery(sentencia);
		query.setParameter("empresa", empresa);
		query.setParameter("fecha", fecha);
		if(estado != 0){
			query.setParameter("estado", Long.valueOf(estado));
		}
		try {
			periodo = (Periodo)query.getSingleResult();
		} catch (NoResultException e) {
			periodo = null;
		} 
		return periodo;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectByFecha(java.util.LocalDate, java.lang.Long)
	 */
	public Periodo selectByFecha(LocalDate fecha, Long empresa) throws Throwable {
		System.out.println("Ingresa al Metodo selectByFecha con empresa : " + empresa + ", fecha = " + fecha);
		Periodo periodo = new Periodo();
		Query query = em.createQuery(" select b " +
									 " from   Periodo b" +
					  				 " where  b.empresa.codigo = :empresa " +
					  				 "        and   :fecha between primerDia and ultimoDia ");
		query.setParameter("empresa", empresa);
		query.setParameter("fecha", fecha);
		try {
			periodo = (Periodo) query.getSingleResult();
		} catch (NoResultException e) {
			throw new IncomeException("NO EXISTE PERIODO PARA LA FECHA INGRESADA EN LA EMPRESA");
		}
		return periodo;
	}

	public Periodo selectMinimoAnteriorByEstadoEmpresa(Long empresa,
			int estado, LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al Metodo selectMinimoAnteriorByEstadoEmpresa con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		Periodo periodo = new Periodo();
		String sentencia = " select b " +
				           " from   Periodo b " +
		  			       " where	b.codigo = ( Select   Min(c.codigo)" +
		  			       "					 from     Periodo c" +
		  			       "					 where    c.empresa.codigo = :empresa " +
		  			       "							  and   c.primerDia < :fecha ";
		if(estado != 0){
			sentencia += " and   c.estado = :estado";
		}
		sentencia += ")";
		Query query = em.createQuery(sentencia);
		query.setParameter("empresa", empresa);
		query.setParameter("fecha", fecha);
		if(estado != 0){
			query.setParameter("estado", Long.valueOf(estado));
		}
		try {
			periodo = (Periodo)query.getSingleResult();
		} catch (NoResultException e) {
			periodo = null;
		} 
		return periodo;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectByEmpresa(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectByEmpresa(Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresa con empresa : " + idEmpresa);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b" +
					  				 " where  b.empresa.codigo = :empresa ");
		query.setParameter("empresa", idEmpresa);
		
	    return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectAnteriorMayorizado(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectAnteriorMayorizado(Long periodo, Long empresa)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectAnteriorMayorizado con periodo : " + periodo + " y empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.codigo = (Select   Max(c.codigo) " +
									 "					  from     Periodo c" +
									 " 					  where    c.empresa.codigo = :empresa " +
									 "	                           and   c.estado = :estado" + 
									 " 							   and   c.codigo < :periodo)");
		query.setParameter("empresa", empresa);
		query.setParameter("estado", Long.valueOf(EstadoPeriodos.MAYORIZADO));
		query.setParameter("periodo", periodo);
		
	    return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectPeriodoAnterior(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectPeriodoAnterior(Long periodo, Long empresa)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectPeriodoAnterior con periodo : " + periodo + " y empresa: " + empresa);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.codigo = (Select   Max(c.codigo) " +
									 "					  from     Periodo c" +
									 " 					  where    c.empresa.codigo = :empresa " +
									 " 							   and   c.codigo < :periodo)");
		query.setParameter("empresa", empresa);
		query.setParameter("periodo", periodo);
		
	    return query.getResultList();
	}
		
}
