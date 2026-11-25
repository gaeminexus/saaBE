package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.TempReportesDaoService;
import com.saa.model.cnt.TempReportes;
import com.saa.rubros.TipoCuentaContable;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class TempReportesDaoServiceImpl extends EntityDaoImpl<TempReportes>  implements TempReportesDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"secuencia",
							"planCuenta",
							"saldoCuenta",
							"valorDebe",
							"valorHaber",
							"valorActual",
							"cuentaContable",
							"codigoCuentaPadre",
							"nombreCuenta",
							"tipo",
							"nivel",
							"mayorizacion",
							"idCentroCosto",
							"nombreCentroCosto",
							"numeroCentroCosto"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectBySecuencia(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TempReportes> selectBySecuencia(Long codigo) throws Throwable {		
		System.out.println("Ingresa al Metodo selectBySecuencia con secuencia: " + codigo);
		Query query = em.createQuery(" select b " +
									 " from   TempReportes b " +
									 " where  b.secuencia = :codigo" );
		query.setParameter("codigo",codigo);
		return query.getResultList();
		
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectNivelesByIdEjecucion(java.lang.Long)
	 */
	
	@SuppressWarnings("unchecked")
	public List<Long> selectNivelesByIdEjecucion(Long idEjecucion) throws Throwable {
	        System.out.println("Ingresa la Metodo selectNivelesByIdEjecucion con idEjecucion : " + idEjecucion);
	        Query query = em.createQuery(
	            "SELECT DISTINCT b.nivel " +
	            "FROM TempReportes b " +
	            "WHERE b.secuencia = :idEjecucion " +
	            "AND b.nivel > 1 " +
	            "ORDER BY b.nivel DESC"
	        );
	        query.setParameter("idEjecucion", idEjecucion);
	        return query.getResultList();
	    }

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#tempReportesByCuentaPadre(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings({ "unchecked"})
	public List<Long>  selectPadresByNivel(Long idEjecucion, Long nivel) throws Throwable {
		System.out.println("Ingresa la Metodo selectPadresByNivel con idEjecucion : " + idEjecucion + ", nivel: " + nivel);
		Query query = em.createQuery(" select   distinct b.codigoCuentaPadre" +
									 " from	    TempReportes b " +
									 " where    b.secuencia = :idEjecucion" +
									 "		    and   b.nivel = :nivel" +
									 " order by b.codigoCuentaPadre ");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("nivel", nivel);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectByCuentaPadreNivel(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TempReportes> selectByCuentaPadreNivel(Long idEjecucion, Long cuentaPadre, Long nivel) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaPadreNivel con idEjecucion : " + idEjecucion + 
				 ", CuentaPadre " + cuentaPadre + ", nivel: " + nivel);
		Query query = em.createQuery(" select b " +
									 " from   TempReportes b" +
									 " where  b.secuencia = :idEjecucion" +
									 " 		  and   b.nivel = :nivel" +
									 "		  and   b.codigoCuentaPadre = :cuentaPadre");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("nivel", nivel);
		query.setParameter("cuentaPadre", cuentaPadre);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectByIdEjecucion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<TempReportes> selectMovimientosByIdEjecucion(Long idEjecucion) throws Throwable {
		System.out.println("ingresa al Metodo selectMovimientosByIdEjecucion con idEjecucion : " + idEjecucion);
		Query query = em.createQuery(" select b " +
									 " from   TempReportes b" +
									 " where  b.secuencia = :idEjecucion" +
									 " 		  and   b.tipo = :tipo");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("tipo", Long.valueOf(TipoCuentaContable.MOVIMIENTO));		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#obtieneSecuenciaReporte()
	 */
	public Long obtieneSecuenciaReporte() throws Throwable {
		System.out.println("Ingresa al Metodo obtieneSecuenciaReporte");
		Long valor = 0L;
		String sql = " select   max(secuencia)" +
					 " from     TempReportes";
		Query query = em.createQuery(sql);
		try {
			valor = (Long)query.getSingleResult();
		} catch (NoResultException e) {
			valor = 0L;
		}
		if(valor == null){
			valor = 0L;
		}
		return valor + 1L;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectByIdEjecucionCuenta(java.lang.Long, java.lang.Long)
	 */
	public TempReportes selectByIdEjecucionCuenta(Long idEjecucion,
			Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectByIdEjecucionCuenta con idEjecucion: " + idEjecucion
				 + ", idCuenta: " + idCuenta);
		TempReportes resultado = new TempReportes();
		Query query = em.createQuery(" select b " +
									 " from   TempReportes b" +
				 					 " where  b.secuencia = :idEjecucion" +
				 					 " 		  and   b.planCuenta.codigo = :idCuenta");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("idCuenta", idCuenta);	
		try {
			resultado = (TempReportes)query.getSingleResult();
		} catch (NoResultException e) {
			resultado = null;
		} 
		return resultado;
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectNumerosCentroByIdEjecucion(java.lang.Long)
	 */
	@SuppressWarnings({ "unchecked"})
	public List<Long>  selectNumerosCentroByIdEjecucion(Long idEjecucion)
			throws Throwable {
		System.out.println("Ingresa la Metodo selectNumerosCentroByIdEjecucion con idEjecucion : " + idEjecucion);
		Query query = em.createQuery(" select   distinct b.numeroCentroCosto " +
									 " from     TempReportes b " + 
									 " where    b.secuencia = :idEjecucion " +
									 " order by b.numeroCentroCosto ");
		query.setParameter("idEjecucion", idEjecucion);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectNivelesByCentroIdEjecucion(java.lang.Long, java.lang.String)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectNivelesByCentroIdEjecucion(Long idEjecucion,
			String numeroCentro) throws Throwable {
		System.out.println("Ingresa la Metodo selectNivelesByCentroIdEjecucion con idEjecucion: "
				 + idEjecucion + ", numeroCentro: " + numeroCentro);
		Query query = em.createQuery(" select   distinct b.nivel " +
									 " from     TempReportes b " +
									 " where    b.secuencia = :idEjecucion " +
									 " 		    and   b.numeroCentroCosto = :numeroCentro" +
									 " 		    and   b.nivel > 1" +
									 " order by b.nivel desc ");
		query.setParameter("idEjecucion", idEjecucion );
		query.setParameter("numeroCentro", numeroCentro );
		return query.getResultList();
	}	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectPadresByNivelCentro(java.lang.Long, java.lang.Long, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<Long> selectPadresByNivelCentro(Long idEjecucion, Long nivel, String numeroCentro) throws Throwable {
		System.out.println("Ingresa la Metodo selectPadresByNivelCentro con idEjecucion: "
				 + idEjecucion + ", nivel: " + nivel + ", numeroCentro: " + numeroCentro);
		Query query = em.createQuery(" select   distinct b.codigoCuentaPadre" +
									 " from	    TempReportes b " +
									 " where    b.secuencia = :idEjecucion" +
									 "		    and   b.nivel = :nivel " +
									 " 		    and   b.numeroCentroCosto = :numeroCentro" +
									 " order by b.codigoCuentaPadre ");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("nivel", nivel);
		query.setParameter("numeroCentro", numeroCentro);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectByCuentaPadreNivelCentro(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	
	@SuppressWarnings("rawtypes")
	public List selectByCuentaPadreNivelCentro(Long idEjecucion,
			Long cuentaPadre, Long nivel, String numeroCentro) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaPadreNivel con idEjecucion : " + idEjecucion + ", CuentaPadre " + cuentaPadre);
		Query query = em.createQuery(" select   distinct b.planCuenta.codigo, b.saldoCuenta, b.valorDebe, b.valorHaber, b.valorActual " +
									 " from     TempReportes b" +
									 " where    b.secuencia = :idEjecucion" +
									 " 		    and   b.nivel = :nivel" +
									 "		    and   b.codigoCuentaPadre = :cuentaPadre" + 
									 " 		    and   b.numeroCentroCosto = :numeroCentro");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("nivel", nivel);
		query.setParameter("cuentaPadre", cuentaPadre);
		query.setParameter("numeroCentro", numeroCentro);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#selectByIdEjecucionCuentaCentro(java.lang.Long, java.lang.Long, java.lang.String)
	 */
	public TempReportes selectByIdEjecucionCuentaCentro(Long idEjecucion,
			Long idCuenta, String numeroCentro) throws Throwable {
		System.out.println("Ingresa al Metodo selectByIdEjecucionCuentaCentro con idEjecucion: " + idEjecucion
				 + ", idCuenta: " + idCuenta + ", centro: " + numeroCentro);
		TempReportes resultado = new TempReportes();
		Query query = em.createQuery(" select b " +
									 " from   TempReportes b" +
				 					 " where  b.secuencia = :idEjecucion" +
				 					 " 		  and   b.planCuenta.codigo = :idCuenta" + 
				 					" 		  and   b.numeroCentroCosto = :numeroCentro");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("idCuenta", idCuenta);	
		query.setParameter("numeroCentro", numeroCentro);
		try {
			resultado = (TempReportes)query.getSingleResult();
		} catch (NoResultException e) {
			resultado = null;
		} 
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.TempReportesDaoService#deleteBySaldosCeroIdEjecucion(java.lang.Long)
	 */
	public void deleteBySaldosCeroIdEjecucion(Long idEjecucion)
			throws Throwable {
		System.out.println("Ingresa al Metodo deleteBySaldosCeroIdEjecucion con idEjecucion: " + idEjecucion);
		Query query = em.createQuery(" Delete   from   TempReportes b" +
				 					 " where    b.secuencia = :idEjecucion" +
				 					 " 		    and   b.saldoCuenta = :valor" + 
				 					 " 		    and   b.valorDebe = :valor" +
				 					 " 		    and   b.valorHaber = :valor" +				 					
				 					 " 		    and   b.valorActual = :valor");
		query.setParameter("idEjecucion", idEjecucion);
		query.setParameter("valor", 0D);
		query.executeUpdate();
	}

	

	@SuppressWarnings("unchecked")
	public java.util.List<com.saa.ejb.cnt.estructura.SaldoCuenta> selectSaldosPorCuenta(Long empresa,
			java.time.LocalDate desde, java.time.LocalDate hasta) throws Throwable {
		Query q = em.createNativeQuery(
				"SELECT p.PLNNCNTA, n.NTRLTPOO, NVL(SUM(d.DTASDBEE), 0), NVL(SUM(d.DTASHBRR), 0)"
						+ "  FROM CNT.DTAS d"
						+ "  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO"
						+ "  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO"
						+ "  JOIN CNT.NTRL n ON n.NTRLCDGO = p.NTRLCDGO"
						+ " WHERE a.PJRQCDGO = ?1 AND a.ASNTESTD IN (?2, ?3)"
						+ "   AND a.ASNTFCHA >= ?4 AND a.ASNTFCHA < ?5"
						+ " GROUP BY p.PLNNCNTA, n.NTRLTPOO ORDER BY p.PLNNCNTA");
		q.setParameter(1, empresa);
		q.setParameter(2, com.saa.rubros.EstadoAsiento.ACTIVO);
		q.setParameter(3, com.saa.rubros.EstadoAsiento.REVERSADO);
		q.setParameter(4, java.sql.Date.valueOf(desde == null ? java.time.LocalDate.of(1900, 1, 1) : desde));
		q.setParameter(5, java.sql.Date.valueOf(hasta.plusDays(1)));
		java.util.List<com.saa.ejb.cnt.estructura.SaldoCuenta> out = new java.util.ArrayList<>();
		for (Object[] f : (List<Object[]>) q.getResultList()) {
			out.add(new com.saa.ejb.cnt.estructura.SaldoCuenta(f[0] == null ? null : f[0].toString(),
					((Number) f[1]).intValue(), decimal(f[2]), decimal(f[3])));
		}
		return out;
	}

	private static java.math.BigDecimal decimal(Object o) {
		if (o == null) {
			return java.math.BigDecimal.ZERO;
		}
		return o instanceof java.math.BigDecimal ? (java.math.BigDecimal) o : new java.math.BigDecimal(o.toString());
	}

}
