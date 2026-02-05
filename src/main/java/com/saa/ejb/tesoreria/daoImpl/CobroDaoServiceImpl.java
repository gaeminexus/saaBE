/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tesoreria.daoImpl;

import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tesoreria.dao.CobroDaoService;
import com.saa.model.tsr.Cobro;
import com.saa.rubros.EstadoCobro;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion CobroDaoService.
 */
@Stateless
public class CobroDaoServiceImpl extends EntityDaoImpl<Cobro> implements CobroDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"tipoId",
							"numeroId",
							"cliente",
							"descripcion",
							"fecha",
							"nombreUsuario",
							"valor",
							"empresa",
							"cierreCaja",
							"fechaInactivo",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"rubroEstadoP",
							"rubroEstadoH",
							"usuarioPorCaja",
							"cajaLogica",
							"asiento",
							"deposito",
							"detalleDeposito",
							"persona",
							"tipoCobro",
							"numeroAsiento"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroDaoService#recuperaIdCobro(com.compuseg.income.tesoreria.ejb.model.Cobro)
	 */
	public Long recuperaIdCobro(Cobro cobro) throws Throwable {
		System.out.println("Ingresa al metodo recuperaConHijos de cobro: " + cobro.getEmpresa().getCodigo());
		Long idCobro = 0L;
		Query query = em.createQuery( " select   max(b.codigo) " +
									  " from     Cobro b");
		String resultado = null;
		try {
			resultado = query.getSingleResult().toString();
		} catch (Exception e) {
			throw new IncomeException("Error en recuperaIdCobro: " + e.getCause());
		}
		if(resultado != null)
			idCobro = Long.valueOf(resultado);
		return idCobro;
	}

	@SuppressWarnings("unchecked")
	public List<Cobro> selectByCierreCaja(Long idCierreCaja) throws Throwable {
		System.out.println("Ingresa al metodo selectByCierreCaja con id de cierre de caja: " + idCierreCaja);
		Query query = null;
		query = em.createQuery(" select b " +
							   " from   Cobro b " +
							   " where  cierreCaja.codigo = :cierreCaja");
		query.setParameter("cierreCaja", idCierreCaja);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroDaoService#validaCobrosPendientes(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cobro> selectCobrosPendientes(Long idUsuario) throws Throwable {
		System.out.println("Ingresa al Metodo validaCobrosPendientes con idUsuario: " + idUsuario);
		Query query = em.createQuery (" select b " +
									  " from   Cobro b " +
									  " where  b.usuarioPorCaja.codigo = :usuarioPorCaja " +
									  "        and b.cierreCaja is null " +
									  "        and b.rubroEstadoH = :estado ");
		query.setParameter("usuarioPorCaja", idUsuario);
		query.setParameter("estado", Long.valueOf(EstadoCobro.INGRESADO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroDaoService#selectCobroByUsuarioCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cobro> selectCobroByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectCobroByUsuarioCaja con id de usuario caja :" + idUsuarioCaja);
		Query query = em.createQuery (" select c " +
									  " FROM   Cobro c " +
									  " where  c.usuarioPorCaja.codigo = :idUsuario " +
									  "        and    c.cierreCaja.codigo is null " +
									  "        and    c.rubroEstadoH = :estado ");
		
		query.setParameter("idUsuario", idUsuarioCaja);
		query.setParameter("estado", Long.valueOf(EstadoCobro.INGRESADO));
		return query.getResultList();
	}

	@SuppressWarnings({"rawtypes"})
	public List selectVistaByUsuarioCaja(Long idUsuarioCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectCobroByUsuarioCaja con id de usuario caja :" + idUsuarioCaja);
		Query query = em.createQuery (" SELECT   c.codigo, c.cliente, c.fecha, "+
									  " (SELECT SUM(e.valor) FROM CobroEfectivo  e 	WHERE e.cobro.codigo = c.codigo), "+
									  " (SELECT SUM(h.valor) FROM CobroCheque    h 	WHERE h.cobro.codigo = c.codigo), "+       
									  " (SELECT SUM(j.valor) FROM CobroTarjeta   j  	WHERE j.cobro.codigo = c.codigo), "+      
									  " (SELECT SUM(t.valor) FROM CobroTransferencia t WHERE t.cobro.codigo = c.codigo), "+       
									  " (SELECT SUM(r.valor) FROM CobroRetencion r 	WHERE r.cobro.codigo = c.codigo), "+
									  " c.valor, c.nombreUsuario, n.nombre, c.cierreCaja.codigo, c.rubroEstadoH"+
									  " FROM   Cobro c,  CajaLogica n " +
									  " where  c.cajaLogica.codigo = n.codigo" +
									  " 	   and   c.usuarioPorCaja.codigo = :idUsuario " +
									  "        and   c.cierreCaja.codigo is null " +
									  "        and   c.rubroEstadoH = :estado ");
		query.setParameter("idUsuario", idUsuarioCaja);
		query.setParameter("estado", Long.valueOf(EstadoCobro.INGRESADO));
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.CobroDaoService#selectCobroCierreByCierreCaja(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cobro> selectCobroByCierreCaja(Long cierreCaja) throws Throwable {
		System.out.println("Ingresa al Metodo selectCobroCierreByCierreCaja con cierreCaja: " + cierreCaja);
		Query query = em.createQuery(" select b " +
									 " from   Cobro b " +
									 " where  b.cierreCaja.codigo = :cierreCaja" +
									 "        and b.rubroEstadoH = :estado");
		query.setParameter("cierreCaja", cierreCaja);
		query.setParameter("estado", Long.valueOf(EstadoCobro.CERRADO));		
		return query.getResultList();
	}
}
