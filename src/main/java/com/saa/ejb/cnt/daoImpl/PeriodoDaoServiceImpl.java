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
		String orden = null;
		if (proceso == ProcesosMayorizacion.MAYORIZACION) {
			estados = "(" + EstadoPeriodos.ACTIVO + "," + EstadoPeriodos.DESMAYORIZADO + ")";
			orden = "asc";
		}else{
			estados = "(" + EstadoPeriodos.MAYORIZADO + ")";
			orden = "desc";
		}
		// e3-08 (2026-10-02): el orden va por primerDia (fecha real de inicio), no por codigo
		// (secuencia de creacion) -- los periodos de 2025 creados ahora tendrian codigo mayor
		// que los de 2026 pero primerDia menor. docs/regulatorio/PLAN-CAMBIOS-SAA-OMEN3.md §C1.
		// e3-09 (2026-10-02): a prueba de empates -- dos periodos de la misma empresa con el
		// mismo primerDia (ej. un periodo de cierre que comparta fecha con el regular) ya no
		// pueden reventar ni elegir una fila distinta en cada corrida: se ordena en vez de
		// comparar por igualdad, con el periodo normal antes que el de cierre (null = 0) y el
		// codigo como ultimo desempate, y se toma una sola fila.
		Query query = em.createQuery(" select b.codigo " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        AND b.estado IN " + estados +
									 " order by b.primerDia " + orden +
									 ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
									 ", b.codigo " + orden);
		query.setParameter("empresa", empresa);
		query.setMaxResults(1);
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
		// e3-08 (2026-10-02): el rango ya no es por codigo (secuencia), es por la fecha real
		// de cada periodo (primerDia) -- ver periodoMayorizacionDesmayorizacion arriba.
		// periodoInicia/periodoFin siguen siendo codigos de periodo (la firma no cambia); se
		// resuelve su primerDia y se ordenan los dos extremos por si vienen invertidos.
		LocalDate fechaInicia = primerDiaDe(empresa, periodoInicia);
		LocalDate fechaFin = primerDiaDe(empresa, periodoFin);
		LocalDate desde = fechaInicia.isBefore(fechaFin) ? fechaInicia : fechaFin;
		LocalDate hasta = fechaInicia.isBefore(fechaFin) ? fechaFin : fechaInicia;

		// e3-09 (2026-10-02): desempate por codigo, para que el orden sea deterministico aun
		// si dos periodos de la misma empresa comparten primerDia.
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where   b.empresa.codigo = :empresa " +
									 "         and (b.primerDia between :desde and :hasta) " +
									 " order by b.primerDia " + orden + ", b.codigo " + orden);
		query.setParameter("empresa", empresa);
		query.setParameter("desde", desde);
		query.setParameter("hasta", hasta);
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
	@SuppressWarnings("unchecked")
	public Periodo selectByEmpresaMaxFecha(Long empresa) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaMaxFecha con empresa: " + empresa);
		// e3-08 (2026-10-02): por primerDia, no por codigo -- ver periodoMayorizacionDesmayorizacion.
		// e3-09 (2026-10-02): a prueba de empates -- order by + setMaxResults(1) en vez de
		// "primerDia = max(...)" con getSingleResult, que reventaria con NonUniqueResultException
		// si dos periodos de la empresa comparten primerDia. Vacio: mismo resultado que antes
		// (NoResultException sin capturar, el llamador no lo esperaba atrapado).
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 " order by b.primerDia desc" +
									 ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
									 ", b.codigo desc");
		query.setParameter("empresa", empresa);
		query.setMaxResults(1);
		List<Periodo> resultado = query.getResultList();
		if (resultado.isEmpty()) {
			throw new NoResultException("No existe ningun periodo para la empresa " + empresa + ".");
		}
		return resultado.get(0);
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
	@SuppressWarnings("unchecked")
	public Periodo selectMaximoAnteriorByEstadoEmpresa(Long empresa,
			int estado, LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al Metodo selectMaximoAnteriorByEstadoEmpresa con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		// e3-08 (2026-10-02): por primerDia, no por codigo.
		// e3-09 (2026-10-02): a prueba de empates -- order by + setMaxResults(1) en vez de
		// "primerDia = max(...)" con getSingleResult. Vacio: null, igual que antes.
		String sentencia = " select b " +
						   " from   Periodo b " +
						   " where  b.empresa.codigo = :empresa " +
		  			       "        and   b.ultimoDia < :fecha ";
		if(estado != 0){
			sentencia += " and   b.estado = :estado";
		}
		sentencia += " order by b.primerDia desc" +
				     ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
				     ", b.codigo desc";
		Query query = em.createQuery(sentencia);
		query.setParameter("empresa", empresa);
		query.setParameter("fecha", fecha);
		if(estado != 0){
			query.setParameter("estado", Long.valueOf(estado));
		}
		query.setMaxResults(1);
		List<Periodo> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
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

	@SuppressWarnings("unchecked")
	public Periodo selectMinimoAnteriorByEstadoEmpresa(Long empresa,
			int estado, LocalDate fecha) throws Throwable {
		System.out.println("Ingresa al Metodo selectMinimoAnteriorByEstadoEmpresa con empresa : " + empresa + ", estado = " + estado + ", fecha = " + fecha);
		// e3-08 (2026-10-02): por primerDia, no por codigo.
		// e3-09 (2026-10-02): a prueba de empates -- order by + setMaxResults(1) en vez de
		// "primerDia = min(...)" con getSingleResult. Vacio: null, igual que antes.
		String sentencia = " select b " +
				           " from   Periodo b " +
				           " where  b.empresa.codigo = :empresa " +
		  			       "        and   b.primerDia < :fecha ";
		if(estado != 0){
			sentencia += " and   b.estado = :estado";
		}
		sentencia += " order by b.primerDia asc" +
				     ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
				     ", b.codigo asc";
		Query query = em.createQuery(sentencia);
		query.setParameter("empresa", empresa);
		query.setParameter("fecha", fecha);
		if(estado != 0){
			query.setParameter("estado", Long.valueOf(estado));
		}
		query.setMaxResults(1);
		List<Periodo> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
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
		// e3-08 (2026-10-02): "anterior" es el de mayor primerDia menor que el primerDia del
		// periodo actual, no el de mayor codigo menor que el codigo actual.
		// e3-09 (2026-10-02): a prueba de empates -- order by + setMaxResults(1) en vez de
		// "primerDia = max(...)". Vacio: lista vacia, igual que antes.
		LocalDate fechaActual = primerDiaDe(empresa, periodo);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.estado = :estado " +
									 "        and   b.primerDia < :fechaActual " +
									 " order by b.primerDia desc" +
									 ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
									 ", b.codigo desc");
		query.setParameter("empresa", empresa);
		query.setParameter("estado", Long.valueOf(EstadoPeriodos.MAYORIZADO));
		query.setParameter("fechaActual", fechaActual);
		query.setMaxResults(1);

	    return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.PeriodoDaoService#selectPeriodoAnterior(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Periodo> selectPeriodoAnterior(Long periodo, Long empresa)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectPeriodoAnterior con periodo : " + periodo + " y empresa: " + empresa);
		// e3-08 (2026-10-02): mismo criterio que selectAnteriorMayorizado, sin el filtro de estado.
		// e3-09 (2026-10-02): a prueba de empates, mismo patron que selectAnteriorMayorizado.
		LocalDate fechaActual = primerDiaDe(empresa, periodo);
		Query query = em.createQuery(" select b " +
									 " from   Periodo b " +
									 " where  b.empresa.codigo = :empresa " +
									 "        and   b.primerDia < :fechaActual " +
									 " order by b.primerDia desc" +
									 ", CASE WHEN b.periodoCierre IS NULL THEN 0 ELSE b.periodoCierre END asc" +
									 ", b.codigo desc");
		query.setParameter("empresa", empresa);
		query.setParameter("fechaActual", fechaActual);
		query.setMaxResults(1);

	    return query.getResultList();
	}

	/**
	 * Resuelve el primerDia (PRDOINCO) de un periodo por su codigo, para las consultas que
	 * reciben un codigo de periodo pero tienen que comparar por fecha (e3-08, 2026-10-02).
	 *
	 * @param empresa	: Id de la empresa
	 * @param codigo	: Codigo del periodo (PRDOCDGO)
	 * @return			: El primerDia de ese periodo
	 * @throws Throwable	: IncomeException si el periodo no existe en esa empresa
	 */
	private LocalDate primerDiaDe(Long empresa, Long codigo) throws Throwable {
		try {
			return (LocalDate) em.createQuery(" select b.primerDia " +
										 " from   Periodo b " +
										 " where  b.codigo = :codigo " +
										 "        and b.empresa.codigo = :empresa")
					.setParameter("codigo", codigo)
					.setParameter("empresa", empresa)
					.getSingleResult();
		} catch (NoResultException e) {
			throw new IncomeException("No existe el periodo " + codigo + " de la empresa " + empresa + ".");
		}
	}

}
