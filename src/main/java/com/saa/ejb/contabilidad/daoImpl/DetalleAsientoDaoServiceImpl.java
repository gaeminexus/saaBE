package com.saa.ejb.contabilidad.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DetalleAsientoDaoService;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.rubros.EstadoAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;

@Stateless
public class DetalleAsientoDaoServiceImpl extends EntityDaoImpl<DetalleAsiento>  implements DetalleAsientoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"asiento",
							"planCuenta",
							"descripcion",
							"valorDebe",
							"valorHaber",
							"nombreCuenta",
							"centroCosto",
							"numeroCuenta"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByMesAnio(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectByMesAnio(Long mes, Long anio, Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByMesAnio de anio: " + anio + ", mes: " + mes + ", empresa: " + empresa);
		Query query = em.createQuery(" select   distinct b.planCuenta.codigo " +
									 " from     DetalleAsiento b " +
									 " where    b.asiento.empresa.codigo = :empresa " +
									 "          and   b.asiento.numeroMes = :mes" +
									 "          and   b.asiento.numeroAnio = :anio " +
									 "			and   b.asiento.estado in (" + EstadoAsiento.ACTIVO + "," + EstadoAsiento.REVERSADO + ")");
		query.setParameter("empresa", empresa);		
		query.setParameter("mes", mes);
		query.setParameter("anio", anio);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByCuentaMes(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByCuentaMes(Long mes, Long anio, Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByCuentaMes de anio: " + anio + ", mes: " + mes + ", cuenta: " + cuenta);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b " +
									 " where  b.asiento.numeroMes = :mes " +
									 "		  and   b.asiento.numeroAnio = :anio" +
									 " 		  and   b.planCuenta.codigo = :cuenta " +
									 "		  and   b.asiento.estado in (" + EstadoAsiento.ACTIVO + "," + EstadoAsiento.REVERSADO + ")");
		query.setParameter("cuenta", cuenta);		
		query.setParameter("mes", mes);
		query.setParameter("anio", anio);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByAll(com.compuseg.income.contabilidad.ejb.model.DetalleAsiento)
	 */
	public Long selectByAll(DetalleAsiento detalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectByAll");
		Long idDetalle = 0L;
		Query query = em.createQuery(" select   codigo " +
									 " from     DetalleAsiento " +
									 " where    asiento.codigo = :asiento " +
									 "          and   planCuenta.codigo = :planCuenta" +
									 "          and   descripcion = :descripcion " +
									 "	        and   valorDebe = :valorDebe " +
									 "			and   valorHaber = :valorHaber " +
									 "		    and   nombreCuenta = :nombreCuenta" +
									 "          and   numeroCuenta = :numeroCuenta");
		query.setParameter("asiento", detalleAsiento.getAsiento().getCodigo());		
		query.setParameter("planCuenta", detalleAsiento.getPlanCuenta().getCodigo());
		query.setParameter("descripcion", detalleAsiento.getDescripcion());
		query.setParameter("valorDebe", detalleAsiento.getValorDebe());
		query.setParameter("valorHaber", detalleAsiento.getValorHaber());
		query.setParameter("nombreCuenta", detalleAsiento.getNombreCuenta());
		query.setParameter("numeroCuenta", detalleAsiento.getNumeroCuenta());
		String resultado = null;
		try {
			resultado = query.getSingleResult().toString();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en selectByAll: " + e.getCause());
		}
		if(resultado != null)
			idDetalle = Long.valueOf(resultado);
		
		return idDetalle;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectSumaDebeHaberByAsiento(java.lang.Long)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectSumaDebeHaberByAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaDebeHaberByAsiento con id de asiento: " + idAsiento);
		Query query = em.createQuery(" select   sum(valorDebe), sum(valorHaber) " +
									 " from     DetalleAsiento " +
									 " where    asiento.codigo = :asiento");
		query.setParameter("asiento", idAsiento);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByIdAsiento(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByIdAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdAsiento con id de asiento: " + idAsiento);
		List<DetalleAsiento> resultado = null;
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b " +
									 " where  asiento.codigo = :idAsiento");
		query.setParameter("idAsiento", idAsiento);
		try {
			resultado = query.getResultList();
		} catch (Exception e) {
			throw new IncomeException("Error en selectByIdAsiento con idAsiento = " + idAsiento + ": " + e.getCause());
		}
		return resultado;
	}
	
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByPeriodoEstadoAndCc(Long mes, Long anio, Long empresa, Long cenCosto, Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByPeriodoEstadoAndCc con mes: " + mes + ", anio: " + anio + " en empresa: " + empresa + ", con cc: " + cenCosto + " y cuenta: " + cuenta);
		List<DetalleAsiento> resultado = null;
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b " +
									 " where  b.asiento.numeroMes = :mes " +
									 "        and   b.asiento.numeroAnio = :anio " +
									 "		  and   b.asiento.estado in (:activo,:reversado) " +
									 "	      and   b.asiento.empresa.codigo = :empresa " +
									 "	      and   b.centroCosto.codigo =  :cenCosto " +
									 "		  and   b.planCuenta.codigo = :cuenta");
		query.setParameter("mes", mes);
		query.setParameter("anio", anio);
		query.setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO));
		query.setParameter("reversado", Long.valueOf(EstadoAsiento.REVERSADO));
		query.setParameter("empresa", empresa);
		query.setParameter("cenCosto", cenCosto);
		query.setParameter("cuenta", cuenta);
		try {
			resultado = query.getResultList();
		} catch (PersistenceException e) {
			throw new IncomeException("Error en selectByPeriodoEstadoAndCc de selectByPeriodoEstadoAndCc: " + e.getCause());
		}	
		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectDetalleAsiento(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByEmpresaCuentaFechas(Long empresa, Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresaCuentaFechas con enpresa(CORREGIDO) : " + empresa
				 + ", cuenta = " + idCuenta + ", entre " + fechaInicio + " y " + fechaFin);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b" +
									 " where  b.planCuenta.codigo = :idCuenta" +
									 "        and    b.asiento.empresa.codigo = :empresa" +
									 "        and    b.asiento.fechaAsiento between :fechaInicio and :fechaFin" +
									 " order by b.asiento.fechaAsiento, b.asiento.codigo");
		query.setParameter("empresa", empresa);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin );		
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectMovimientoByEmpresaCuentaFecha(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectMovimientoByEmpresaCuentaFecha(Long empresa, LocalDate fechaInicio, LocalDate fechaFin,
															   String cuentaInicio,String cuentaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectMovimientoByEmpresaCuentaFecha con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
																					  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
																					  + cuentaInicio + ", cuentaFin: " + cuentaFin);
		Query query = em.createQuery(" select   distinct b" +
									 " from     DetalleAsiento b" +
									 " where    b.asiento.empresa.codigo = :empresa" +
									 " 		    and   b.numeroCuenta between :cuentaInicio and :cuentaFin" +
									 "		    and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin" +
									 " order by b.numeroCuenta");
		query.setParameter("empresa", empresa);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaFin", cuentaFin);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByEmpresaCuentaFechaCentro(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByEmpresaCuentaFechaCentro(Long empresa,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin, String centroInicio, String centroFin)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectByEmpresaCuentaFechaCentro con empresa: " + empresa + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin + ", centroInicio: " +
				  centroInicio + ", centroFin: " + centroFin);
		Query query = em.createQuery(" select   distinct b" +
									 " from     DetalleAsiento b" +
									 " where    b.asiento.empresa.codigo = :empresa" +
									 " 		    and   b.numeroCuenta between :cuentaInicio and :cuentaFin" +
									 " 		    and   b.centroCosto.numero between :centroInicio and :centroFin" +
									 "		    and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin" +
									 " order by b.numeroCuenta");
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
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectSumaDebeHaberByFechaEmpresa(java.lang.Long, java.util.LocalDate)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectSumaDebeHaberByFechasEmpresaCuenta(Long idEmpresa, Long idCuenta, LocalDate fechaDesde, LocalDate fechaFin)
			throws Throwable {
		System.out.println("Ingresa al metodo selectSumaDebeHaberByFechasEmpresaCuenta con id de empresa: " + idEmpresa + ", fechaDesde: " + 
				 fechaDesde + ", fechaFin: " + fechaFin);
		Query query = em.createQuery(" select   sum(valorDebe), sum(valorHaber) " +
									 " from     DetalleAsiento " +
									 " where    asiento.empresa.codigo = :idEmpresa " +
									 "			and   planCuenta.codigo = :idCuenta " +
									 "          and   asiento.fechaAsiento between :fechaDesde and :fechaFin " +
									 "          and   asiento.estado in (:activo, :reversado)");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO));
		query.setParameter("reversado", Long.valueOf(EstadoAsiento.REVERSADO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByCuentaFechasCentros(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByCuentaFechasCentros(Long idCuenta,
			LocalDate fechaInicio, LocalDate fechaFin, String centroInicio,
			String centroFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCuentaFechasCentros con idCuenta: " + idCuenta + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", centroInicio: " 
				  + centroInicio + ", centroFin: " + centroFin);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b" +
									 " where  b.planCuenta.codigo = :idCuenta" +
									 " 		  and   b.centroCosto.numero between :centroInicio and :centroFin" +
									 "		  and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin" +
									 " order by b.asiento.fechaAsiento, b.numeroCuenta");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("centroInicio", centroInicio);
		query.setParameter("centroFin", centroFin);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectByCentroFechasCuentas(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.String, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public List<DetalleAsiento> selectByCentroFechasCuentas(Long idCentro,
			LocalDate fechaInicio, LocalDate fechaFin, String cuentaInicio,
			String cuentaFin) throws Throwable {
		System.out.println("Ingresa al Metodo selectByCentroFechasCuentas con idCentro: " + idCentro + ", fechaInicio: " + fechaInicio +
				  ", fechaFin: " + fechaFin + ", cuentaInicio: " 
				  + cuentaInicio + ", cuentaFin: " + cuentaFin);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b" +
									 " where  b.centroCosto.codigo = :idCentro" +
									 " 		  and   b.planCuenta.cuentaContable between :cuentaInicio and :cuentaFin" +
									 "		  and   b.asiento.fechaAsiento between :fechaInicio and :fechaFin" +
									 " order by b.asiento.fechaAsiento, b.numeroCuenta");
		query.setParameter("idCentro", idCentro);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("cuentaInicio", cuentaInicio);
		query.setParameter("cuentaFin", cuentaFin);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectSumaDebeHaberByFechasEmpresaCentro(java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectSumaDebeHaberByFechasEmpresaCentro(Long idEmpresa,
			Long idCentro, LocalDate fechaDesde, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaDebeHaberByFechasEmpresaCentro con id de empresa: " + idEmpresa + ", fechaDesde: " + 
				 fechaDesde + ", fechaFin: " + fechaFin + ", idCentro: " + idCentro);
		Query query = em.createQuery(" select   sum(valorDebe), sum(valorHaber) " +
									 " from     DetalleAsiento " +
									 " where    asiento.empresa.codigo = :idEmpresa " +
									 "			and   centroCosto.codigo = :idCentro " +
									 "          and   asiento.fechaAsiento between :fechaDesde and :fechaFin " +
									 "          and   asiento.estado in (:activo, :reversado)");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("idCentro", idCentro);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO));
		query.setParameter("reversado", Long.valueOf(EstadoAsiento.REVERSADO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectSumaDebeHaberByFechasEmpresaCentro(java.lang.Long, java.lang.Long, java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectSumaDebeHaberByFechasEmpresaCentroCuenta(Long idEmpresa,
			Long idCentro, Long idCuenta, LocalDate fechaDesde, LocalDate fechaFin)
			throws Throwable {
		System.out.println("Ingresa al metodo selectSumaDebeHaberByFechasEmpresaCentroCuenta " +
				 " con id de empresa: " + idEmpresa + ", fechaDesde: " + 
				 fechaDesde + ", fechaFin: " + fechaFin + ", idCentro: " + idCentro 
				 + ", idCuenta: " + idCuenta);
		Query query = em.createQuery(" select   sum(valorDebe), sum(valorHaber) " +
									 " from     DetalleAsiento " +
									 " where    asiento.empresa.codigo = :idEmpresa " +
									 "			and   centroCosto.codigo = :idCentro " +
									 "			and   planCuenta.codigo = :idCuenta " +
									 "          and   asiento.fechaAsiento between :fechaDesde and :fechaFin " +
									 "          and   asiento.estado in (:activo, :reversado)");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("idCentro", idCentro);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO));
		query.setParameter("reversado", Long.valueOf(EstadoAsiento.REVERSADO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleAsientoDaoService#selectSumaDebeHaberAFechaByEmpresaCentroCuenta(java.lang.Long, java.lang.Long, java.lang.Long, java.util.LocalDate)
	 */
	@SuppressWarnings({"rawtypes" })
	public List selectSumaDebeHaberAFechaByEmpresaCentroCuenta(Long idEmpresa,
			Long idCentro, Long idCuenta, LocalDate fechaHasta) throws Throwable {
		System.out.println("Ingresa al metodo selectSumaDebeHaberAFechaByEmpresaCentroCuenta " +
				 " con id de empresa: " + idEmpresa + ", fechaHasta: " + fechaHasta + 
				 ", idCentro: " + idCentro + ", idCuenta: " + idCuenta);
		Query query = em.createQuery(" select   sum(valorDebe), sum(valorHaber) " +
									 " from     DetalleAsiento " +
									 " where    asiento.empresa.codigo = :idEmpresa " +
									 "			and   centroCosto.codigo = :idCentro " +
									 "			and   planCuenta.codigo = :idCuenta " +
									 "          and   asiento.fechaAsiento <= :fechaHasta " +
									 "          and   asiento.estado in (:activo, :reversado)");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("idCentro", idCentro);
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("activo", Long.valueOf(EstadoAsiento.ACTIVO));
		query.setParameter("reversado", Long.valueOf(EstadoAsiento.REVERSADO));
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetalleAsiento> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdPlanCuenta de idPlanCuenta: " + idPlanCuenta);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b " +
									 " where  b.planCuenta.codigo = :idPlanCuenta ");
		query.setParameter("idPlanCuenta", idPlanCuenta);		
		return query.getResultList();
	}

	@SuppressWarnings({ "unchecked"})
	@Override
	public List<DetalleAsiento> selectByIdCentroCosto(Long idCentroCosto) throws Throwable {
		System.out.println("Ingresa al metodo selectByCentroCosto " + idCentroCosto);
		Query query = em.createQuery(" select b " +
									 " from   DetalleAsiento b " +
									 " where  b.centroCosto.codigo = :idCentroCosto ");
		query.setParameter("idCentroCosto", idCentroCosto);		
		return query.getResultList();
	}

}
