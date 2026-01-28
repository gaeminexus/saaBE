/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.MovimientoBancoDaoService;
import com.saa.model.tesoreria.Conciliacion;
import com.saa.model.tesoreria.MovimientoBanco;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoMovimientoBanco;
import com.saa.rubros.EstadosConciliacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion MovimientoBancoDaoService.
 */
@Stateless
public class MovimientoBancoDaoServiceImpl extends EntityDaoImpl<MovimientoBanco> implements MovimientoBancoDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"empresa",
							"descripcion",
							"asiento",
							"valor",
							"conciliado",
							"fechaConciliacion",
							"numeroCheque",
							"conciliacion",
							"rubroTipoMovimientoP",
							"rubroTipoMovimientoH",
							"fechaRegistro",
							"numeroAsiento",
							"idMovimiento",
							"estado",
							"cheque",
							"cuentaBancaria",
							"detalleDeposito",
							"periodo",
							"numeroMes",
							"numeroAnio",
							"rubroOrigenP",
							"rubroOrigenH"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#obtieneFechaPrimerMovimiento(java.lang.Long)
	 */
	public LocalDate obtieneFechaPrimerMovimiento(Long idCuenta)throws Throwable {
		//004
		System.out.println("Ingresa al metodo obtieneFechaMovimiento con Cuenta:" + idCuenta);
		LocalDate fecha = null;
		Query query = em.createQuery(" select   MIN(b.fechaRegistro) " +
									 " from     MovimientoBanco b " +
									 " where    b.cuentaBancaria.codigo = :idCuenta " +
									 "		    and   b.estado = :estado");
		query.setParameter("idCuenta",idCuenta);
		query.setParameter("estado",Estado.ACTIVO);
		if(query.getResultList().get(0) != null)
			fecha = (LocalDate)query.getResultList().get(0);
		return fecha;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#recuperaValorTrancitoCuentaBancaria(java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	public Double recuperaValorTrancitoCuentaBancaria(Long idCuenta, LocalDate fechaInicio, LocalDate fechaFin)throws Throwable {
		// 008
		System.out.println("Ingresa al metodo recuperaValorTrancitoCuentaBancaria con :" + idCuenta + " Fecha Inicio " + fechaInicio + " FechaFin " + fechaFin );
		Double valor = 0.0D;
		Double credito = 0.0D;
		Double debito = 0.0D;
		Query creditos = em.createQuery(" select   SUM(b.valor) " +
										" from     MovimientoBanco b " +
										" where    b.cuentaBancaria.codigo = :idCuenta " +
										"		   AND trunc(b.fechaRegistro) BETWEEN :fechaInicio " +
										" 		   AND :fechaFin " +
										"          AND b.rubroOrigenH IN (1,3,5) AND b.estado = 1");			
		creditos.setParameter("idCuenta", idCuenta);
		creditos.setParameter("fechaInicio", fechaInicio);
		creditos.setParameter("fechaFin", fechaFin);
		
		Query debitos = em.createQuery(" select   SUM(b.valor) " +
									   " from     MovimientoBanco b " +
									   " where    b.cuentaBancaria.codigo = :idCuenta " +
									   "          AND trunc(b.fechaRegistro) BETWEEN :fechaInicio " +
									   "		  AND :fechaFin " +
									   "          AND b.rubroOrigenH IN (2,4,6) AND b.estado = 1");
		debitos.setParameter("idCuenta", idCuenta);
		debitos.setParameter("fechaInicio", fechaInicio);
		debitos.setParameter("fechaFin", fechaFin);
		
		if(creditos.getResultList().get(0) != null)
			credito = Double.valueOf(creditos.getResultList().get(0).toString());
		if(debitos.getResultList().get(0) != null)
			credito = Double.valueOf(debitos.getResultList().get(0).toString());
		valor = credito - debito;	
		return valor;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectByAsiento(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<MovimientoBanco> selectByAsiento(Long idAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectByAsiento con id de asiento :" + idAsiento);
		Query query = em.createQuery(" select b " +
									 " from   MovimientoBanco b " +
									 " where  b.asiento.codigo = :idAsiento");			
		query.setParameter("idAsiento", idAsiento);
		return query.getResultList();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectCuentasByIdPeriodo(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<MovimientoBanco> selectCuentasByIdPeriodo(Long idPeriodo) throws Throwable {
		System.out.println("Ingresa al Metodo selectCuentasByIdPeriodo con idPeriodo :" + idPeriodo);
		Query query = em.createQuery(" select distinct b.cuentaBancaria, b.rubroOrigenP " +
									 " from   MovimientoBanco b " +
									 " where  b.periodo = :idPeriodo" +
									 "		  and b.estado = :estado");
		query.setParameter("idPeriodo",idPeriodo);
		query.setParameter("estado", Long.valueOf(EstadoMovimientoBanco.ACTIVO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectSumValorByCuentaPeriodoTipoConciliadoEstado(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, int, int)
	 */
	public Double selectSumValorByCuentaPeriodoTipoConciliadoEstado
			(Long idCuenta, Long idPeriodo, int codigoAlternoRubro, 
			 int codigoAlternoDetalle, int conciliado, int estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectSumValorByCuentaPeriodoTipoConciliadoEstado con idCuenta " + 
				 idCuenta + ", codigoAlternoDetalle: " + codigoAlternoDetalle);
		Query query = em.createQuery(" select   sum(b.valor) " +
									 " from     MovimientoBanco b " +
									 " where    b.cuentaBancaria.codigo = :idCuenta" +
									 "          and   b.periodo.codigo = :idPeriodo" +
									 "		  	and   b.rubroOrigenP = :codigoAlternoRubro" +
									 "        	and   b.rubroOrigenH = :codigoAlternoDetalle" +
									 "        	and   b.conciliado = :conciliado " +
									 "        	and   b.estado = :estado");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("idPeriodo", idPeriodo);
		query.setParameter("codigoAlternoRubro", Long.valueOf(codigoAlternoRubro));
		query.setParameter("codigoAlternoDetalle", Long.valueOf(codigoAlternoDetalle));
		query.setParameter("conciliado", Long.valueOf(conciliado));
		query.setParameter("estado", Long.valueOf(estado));
		return (Double)query.getSingleResult();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectValorByDepositoTrancitoConfirmado(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */		
	public Double selectMovimientoBancarioByTipo(Long empresa, Long cuentaBancaria, Long periodo, Long tipoMovimiento1, Long tipoMovimiento2, Long estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectMovimientoBancarioByTipo con empresa:" + empresa + ", cuentaBancaria" + cuentaBancaria +
																						", periodo" + periodo + 
																						", estado" + estado);																							
		Query query = em.createQuery(" select b " +
									 " from   MovimientoBanco b " +
									 " where  b.empresa.codigo = :empresa" +
									 "		  and b.cuentaBancaria = :cuentaBancaria" +
									 "		  and b.periodo = :periodo" +
									 "		  and b.rubroTipoMoviminetoH in (:tipoMovimiento1,:tipoMovimiento2)" +
									 "		  and b.estado = :estado");
		query.setParameter("empresa", empresa);
		query.setParameter("cuentaBancaria", cuentaBancaria);
		query.setParameter("periodo", periodo);
		query.setParameter("tipoMovimiento1", tipoMovimiento1);
		query.setParameter("tipoMovimiento2", tipoMovimiento2);
		query.setParameter("estado", estado);
		return (Double) query.getSingleResult();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectSaldosCuentaByRangoFechas(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	public Long selectSaldosCuentaByRangoFechas(Long idCuenta,LocalDate fechaInicio, LocalDate fechaFin,
												Long codigoAlternoRubro, Long tipoMovimiento1, Long tipoMovimiento2,
												Long tipoMovimiento3, Long estado) throws Throwable {
		System.out.println("Ingresa al Metodo selectSaldosCuentaByRangoFechas con idCuenta : " + idCuenta + ", FechaInicio" + fechaInicio + ", FechaFin" + fechaFin +
																				   ", codigoAlternoRubro" + codigoAlternoRubro + ", tipoMovimiento1" + tipoMovimiento1 + ", tipoMovimiento2" +
																				   tipoMovimiento2 + ", tipoMovimiento3" + tipoMovimiento3 + ", Estado" + estado);
		Query query = em.createQuery(" select sum (valor)" +
									 " from   MovimientoBanco b " +
									 " where  b.cuentaBancaria = :idCuenta" +
									 "		  and b.fechaRegistro between :fechaInicio and :fechaFin" +
									 "		  and b.rubroOrigenP in (:tipoMovimiento1, :tipoMovimiento2, :tipoMovimiento3" +
									 "		  and b.estado = estado)");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("codigoAlternoRubro", codigoAlternoRubro);
		query.setParameter("tipoMovimiento1", tipoMovimiento1);
		query.setParameter("tipoMovimiento2", tipoMovimiento2);
		query.setParameter("tipoMovimiento3", tipoMovimiento3);
		query.setParameter("estado", estado);
		return (Long) query.getSingleResult();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#cuentaByCuentaBancariaEstadoMenorAFecha(java.lang.Long, java.util.LocalDate)
	 */
	public Long cuentaByCuentaBancariaEstadoMenorAFecha(Long idCuentaBancaria,
			LocalDate fecha) throws Throwable {
		System.out.println(" Ingresa cuentaByCuentaBancariaEstadoMenorAFecha con idCuentaBancaria, : "
				 + idCuentaBancaria +  ", fecha : " + fecha) ;
		Query query = em.createQuery (" select   count(*)" +
									  " from     MovimientoBanco b " +
									  " where    b.cuentaBancaria.codigo = :idCuentaBancaria " +
									  "		     and   b.fechaRegistro < :fecha " + 
									  "		     and   b.estado =:estado" +
									  "		     and   b.conciliado =:conciliado");
		query.setParameter("idCuentaBancaria", idCuentaBancaria);
		query.setParameter("fecha", fecha);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		query.setParameter("conciliado", Long.valueOf(EstadosConciliacion.RAIZ));
		return (Long)query.getSingleResult();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectSinConsByCuentaEstadoMenorAFecha(java.lang.Long, java.util.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	public List<MovimientoBanco>  selectSinConsByCuentaEstadoMenorAFecha(Long idCuentaBancaria,
			LocalDate fecha) throws Throwable {
		System.out.println(" Ingresa selectSinConsByCuentaEstadoMenorAFecha con idCuentaBancaria, : "
				 + idCuentaBancaria +  ", fecha : " + fecha);
		Query query = em.createQuery (" select b " +
									  " from     MovimientoBanco b " +
									  " where    b.cuentaBancaria.codigo = :idCuentaBancaria " +
									  "		     and   b.fechaRegistro <= :fecha " + 
									  "		     and   b.conciliacion is null " +
									  "		     and   b.estado =:estado" +
									  " order by b.codigo");
		query.setParameter("idCuentaBancaria", idCuentaBancaria);
		query.setParameter("fecha", fecha);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#updateEstadoFechaConciliaById(java.lang.Long, java.util.LocalDate, com.compuseg.income.tesoreria.ejb.model.Conciliacion, int)
	 */
	public void updateEstadoFechaConciliaById(Long idMovimiento, LocalDate fechaConciliacion,
			Conciliacion conciliacion, int estado) throws Throwable {
		System.out.println(" Ingresa updateEstadoFechaConciliaById con idMovimiento: " + idMovimiento
				 + ",fechaConciliacion, : " + fechaConciliacion +  ", conciliacion: " 
				 + conciliacion.getCodigo() + ", estado: " + estado);
		Query query = em.createQuery (" Update   MovimientoBanco b " +
									  " set      b.fechaConciliacion = :fechaConciliacion, " +
									  " 	     b.conciliacion = :conciliacion, " +
									  " 	     b.rubroTipoMovimientoH = :estado " +
									  " where    b.codigo = :idMovimiento ");
		query.setParameter("fechaConciliacion", fechaConciliacion);
		query.setParameter("conciliacion", conciliacion);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		query.setParameter("idMovimiento", idMovimiento);
		query.executeUpdate();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectSumValorCuentaRangoFechas3Origenes(java.lang.Long, java.util.LocalDate, java.util.LocalDate, int, int, int)
	 */
	public Double selectSumValorCuentaRangoFechas3Origenes(Long idCuenta,
			LocalDate fechaDesde, LocalDate fechaHasta, int rubroOrigen1,
			int rubroOrigen2, int rubroOrigen3) throws Throwable {
		System.out.println(" Ingresa selectSumValorCuentaRangoFechas3Origenes con idCuenta: " + idCuenta
				 + ",fechaDesde, : " + fechaDesde +  ", fechaHasta: " 
				 + fechaHasta + ", rubroOrigen1: " + rubroOrigen1
				 + ", rubroOrigen2: " + rubroOrigen2 + ", rubroOrigen3: " + rubroOrigen2);
		Double suma = 0D;
		Query query = em.createQuery(" Select   sum(b.valor)" +
									 " from     MovimientoBanco b " +
				 					 " where    b.cuentaBancaria.codigo = :idCuenta " +
				 					 "		    and   b.fechaRegistro between :fechaDesde and :fechaHasta " +
				 					 "		    and   b.rubroOrigenH in (:rubroOrigen1, :rubroOrigen2, :rubroOrigen3) " +
				 					 "		    and   b.estado = :estado ");
		query.setParameter("idCuenta", idCuenta);
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		query.setParameter("rubroOrigen1", Long.valueOf(rubroOrigen1));
		query.setParameter("rubroOrigen2", Long.valueOf(rubroOrigen2));
		query.setParameter("rubroOrigen3", Long.valueOf(rubroOrigen3));
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		try {
			suma = (Double)query.getSingleResult();
		} catch (NoResultException e) {
			suma = 0D;
		}
		if(suma == null)
			suma = 0D;
		System.out.println("SUMA: "+suma);
		return suma;
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectConciliacion(java.lang.Long, java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<MovimientoBanco> selectConciliacion(Long idCuentaBancaria, 
			Long idPeriodo, Long conciliacion,
			Long estado) throws Throwable {
		System.out.println("Ingresa al metodo (selectConciliacion) idCtaBco: [" + idCuentaBancaria+
				"] idPeriodo: ["+idPeriodo+"] Conciliacion: ["+conciliacion+"] estado: ["+
				estado+"]");		
		Query query = em.createQuery(" select b " +
									 " from   MovimientoBanco b " +
									 " where  b.cuentaBancaria.codigo = :idCuentaBancaria" +
									 "       and b.periodo.codigo <= :idPeriodo"+
									 "       and b.conciliacion.codigo = :conciliacion"+
									 "       and b.estado = :estado");
		query.setParameter("idCuentaBancaria",idCuentaBancaria);
		query.setParameter("idPeriodo",idPeriodo);
		query.setParameter("conciliacion",conciliacion);
		query.setParameter("estado", estado);
		return query.getResultList();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#selectRIED(java.lang.Long, java.util.LocalDate, java.util.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	public List<MovimientoBanco> selectRIED(Long idCuentaBancaria, LocalDate fechaInicio, LocalDate fechaFin) throws Throwable {
		System.out.println("Ingresa al metodo (selectRIED) idCuenta: [" + idCuentaBancaria+
				"] fechaInicio: ["+fechaInicio+"] fechaFin: ["+fechaFin+"]");
		String strQuery = " from     MovimientoBanco b " +
				  		  " where    b.cuentaBancaria.codigo = :idCuentaBancaria ";
		if(fechaInicio!=null){
			if(fechaFin!=null){
				strQuery+="		and   trunc(b.fechaRegistro) between :fechaInicio and :fechaFin "; 	
			}else{
				strQuery+=" 	and   trunc(b.fechaRegistro) = :fechaInicio";
			}
		}
		strQuery +=  "		    and   b.estado =:estado"+
				     " 			order by b.fechaRegistro";
		Query query = em.createQuery (strQuery);
		query.setParameter("idCuentaBancaria", idCuentaBancaria);
		if(fechaInicio!=null){
			if(fechaFin!=null){
				query.setParameter("fechaInicio", fechaInicio);
				query.setParameter("fechaFin", fechaFin); 	
			}else{
				query.setParameter("fechaInicio", fechaInicio);
			}
		}
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}


	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.MovimientoBancoDaoService#updateTipoConciliaFechaByConciliaTipo(java.lang.Long, java.lang.Long)
	 */
	public void updateTipoConciliaFechaByConciliaOrigen(Long idConciliacion,
			int tipoMovimiento, int origenMovimiento) throws Throwable {
		System.out.println("Ingresa a updateTipoConciliaFechaByConciliaOrigen idConciliacion: " 
				 + idConciliacion +	", tipoMovimiento: " + tipoMovimiento 
				 + ", origenMovimiento: " + origenMovimiento);
		Query query = em.createQuery (" update   MovimientoBanco b " +
									  " set      b.conciliacion = null, " +
									  " 	     b.fechaConciliacion = null, " +
									  " 	     b.rubroTipoMovimientoH = :tipoMovimiento " +
									  " where    b.conciliacion.codigo = :idConciliacion " +
									  "          and   b.rubroOrigenH = :origenMovimiento ");
		query.setParameter("tipoMovimiento", Long.valueOf(tipoMovimiento));
		query.setParameter("idConciliacion", Long.valueOf(idConciliacion));
		query.setParameter("origenMovimiento", origenMovimiento);
		query.executeUpdate();		
	}

}
