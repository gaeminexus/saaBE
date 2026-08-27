/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.tsr.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.tsr.dao.ChequeDaoService;
import com.saa.model.tsr.Cheque;
import com.saa.rubros.EstadoCheque;
import com.saa.rubros.EstadoChequera;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft
 *
 * Implementacion ChequeDaoServices.
 */
@Stateless
public class ChequeDaoServiceImpl extends EntityDaoImpl<Cheque> implements ChequeDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"chequera",
							"numero",
							"egreso",
							"fechaUso",
							"fechaCaduca",
							"fechaAnulacion",
							"rubroEstadoChequeP",
							"rubroEstadoChequeH",
							"fechaImpresion",
							"fechaEntrega",
							"asiento",
							"titular",
							"valor",
							"rubroMotivoAnulacionP",
							"rubroMotivoAnulacionH",
							"beneficiario",
							"idBeneficiario"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.tesoreria.ejb.dao.ChequeDaoService#selectMaxCheque(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<Cheque> selectMaxCheque(Long cuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectMaxCheque con cuenta:  " + cuenta);
		Query query = em.createQuery(" select   max(b.numero) " +
									 " from     Cheque b " +
									 " where    b.chequera.cuentaBancaria.codigo = :cuenta");
		query.setParameter("cuenta" ,cuenta);
		return query.getResultList();
	}

	public Long selectMinChequeActivo(Long idCuenta) throws Throwable {
		System.out.println("Ingresa al Metodo selectMinChequeActivo con cuenta:  " + idCuenta);
		Long codigo = 0L;
		Query query = em.createQuery(" Select min(b.codigo) from Cheque b where b.chequera.cuentaBancaria.codigo = :cuenta and rubroEstadoChequeH = :estado");
		query.setParameter("cuenta" ,idCuenta);
		query.setParameter("estado" ,Long.valueOf(EstadoCheque.ACTIVO));
		String respuesta = null;
		try {
			respuesta = query.getSingleResult().toString();
		} catch (Exception e) {
			throw new IncomeException("ERROR AL RECUPERAR ID DE CHEQUE: "+e.getMessage());
		}
		if(respuesta != null)
			codigo = Long.valueOf(respuesta);
		return codigo;
	}

	@SuppressWarnings("unchecked")
	public List<Cheque> selectByChequera(Long idChequera) throws Throwable {
		System.out.println("Ingresa al Metodo selectByChequera con chequera: " + idChequera);
		Query query = em.createQuery(
				" select c from Cheque c where c.chequera.codigo = :idChequera order by c.numero");
		query.setParameter("idChequera", idChequera);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	public Long selectMinChequeActivoPorCuenta(Long idCuentaBancaria) throws Throwable {
		System.out.println("Ingresa al Metodo selectMinChequeActivoPorCuenta con cuenta: " + idCuentaBancaria);
		// SIN lock: Oracle no admite FETCH FIRST (setMaxResults) junto con FOR
		// UPDATE, así que Hibernate recurriría a follow-on locking (bloquea
		// después, sin volver a evaluar el enunciado) — no protege nada y además
		// este método lo usa GET /dtch/siguiente para el preview del formulario,
		// que no debe tomar un lock de escritura. El lock real y la
		// re-verificación de estado viven en ChequeServiceImpl (tomarSiguienteConLock,
		// usado solo por asignarAPago vía em.refresh con PESSIMISTIC_WRITE).
		//
		// Filtro de chequera NULL-safe (mismo patrón que ChequeraDaoServiceImpl):
		// una chequera con rubroEstadoChequeraH nulo o en SOLICITADA(3) es legado
		// o un estado intermedio, no un motivo para esconder sus cheques ACTIVO;
		// sólo ANULADA(6) y TERMINADA(4) los excluyen de verdad.
		Query query = em.createQuery(
				" select c.codigo from Cheque c " +
				" where c.chequera.cuentaBancaria.codigo = :idCuenta " +
				" and c.rubroEstadoChequeH = :activo " +
				" and (c.chequera.rubroEstadoChequeraH is null " +
				"      or c.chequera.rubroEstadoChequeraH not in (:anulada, :terminada)) " +
				" order by c.numero asc");
		query.setParameter("idCuenta", idCuentaBancaria);
		query.setParameter("activo", Long.valueOf(EstadoCheque.ACTIVO));
		query.setParameter("anulada", Long.valueOf(EstadoChequera.ANULADA));
		query.setParameter("terminada", Long.valueOf(EstadoChequera.TERMINADA));
		query.setMaxResults(1);
		List<Long> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	@SuppressWarnings("unchecked")
	public Long selectIdPagoByCheque(Long idCheque) throws Throwable {
		System.out.println("Ingresa al Metodo selectIdPagoByCheque con cheque: " + idCheque);
		Query query = em.createQuery(
				" select p.id from PagoProgramado p where p.cheque.codigo = :idCheque");
		query.setParameter("idCheque", idCheque);
		List<Long> resultado = query.getResultList();
		return resultado.isEmpty() ? null : resultado.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> selectListado(Long idCuentaBancaria, Long estado, LocalDate desde,
			LocalDate hasta, Long idEmpresa) throws Throwable {
		System.out.println("Ingresa al Metodo selectListado de cheques");

		// fc/eg/an van con LEFT JOIN explícito: son mutuamente excluyentes en un
		// pago (solo uno está lleno), así que un join implícito p.facturaCompra.*
		// lo renderiza Hibernate como INNER JOIN y el where nunca se cumple para
		// los otros dos casos — el listado quedaba vacío siempre. p.facturaCompra.id,
		// p.egreso.id, p.anticipo.id y p.empresa.codigo sí son seguros como
		// implícitos porque Hibernate los resuelve contra la columna FK sin join.
		StringBuilder jpql = new StringBuilder(
				" select c.codigo, c.numero, c.rubroEstadoChequeH, c.valor, c.beneficiario, " +
				"        c.fechaUso, c.fechaImpresion, c.fechaEntrega, cb.numeroCuenta, bnc.nombre, " +
				"        p.id, fc.id, fc.numero, eg.id, eg.descripcion, " +
				"        an.id, an.numeroDoc, p.origenExterno, p.idOrigen " +
				" from Cheque c " +
				" join c.chequera ch " +
				" join ch.cuentaBancaria cb " +
				" left join cb.banco bnc " +
				" left join PagoProgramado p on p.cheque = c " +
				" left join p.facturaCompra fc " +
				" left join p.egreso eg " +
				" left join p.anticipo an " +
				" where 1 = 1 ");
		if (idCuentaBancaria != null) {
			jpql.append(" and cb.codigo = :idCuentaBancaria ");
		}
		if (estado != null) {
			jpql.append(" and c.rubroEstadoChequeH = :estado ");
		}
		if (desde != null) {
			jpql.append(" and c.fechaUso >= :desde ");
		}
		if (hasta != null) {
			jpql.append(" and c.fechaUso <= :hasta ");
		}
		if (idEmpresa != null) {
			jpql.append(" and (p.id is null or p.empresa.codigo = :idEmpresa) ");
		}
		jpql.append(" order by c.numero ");

		Query query = em.createQuery(jpql.toString());
		if (idCuentaBancaria != null) {
			query.setParameter("idCuentaBancaria", idCuentaBancaria);
		}
		if (estado != null) {
			query.setParameter("estado", estado);
		}
		if (desde != null) {
			query.setParameter("desde", desde.atStartOfDay());
		}
		if (hasta != null) {
			query.setParameter("hasta", hasta.atTime(23, 59, 59));
		}
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		return query.getResultList();
	}

}
