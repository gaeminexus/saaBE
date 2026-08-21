package com.saa.ejb.rhh.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.SaldoAperturaDaoService;
import com.saa.model.rhh.SaldoApertura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * @author GaemiSoft.
 * Implementacion SaldoAperturaDaoService.
 */
@Stateless
public class SaldoAperturaDaoServiceImpl extends EntityDaoImpl<SaldoApertura> implements SaldoAperturaDaoService {

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoAperturaDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) SaldoApertura");
		return new String[]{"codigo",
							"empresa",
							"empleado",
							"identificacion",
							"fechaCorte",
							"tipoSaldo",
							"valor",
							"dias",
							"fecha",
							"anio",
							"numeroCuotas",
							"observacion",
							"aplicado",
							"fechaAplicacion",
							"tablaReferencia",
							"idReferencia",
							"fechaAnterior",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoAperturaDaoService#selectByEmpresaYCorte(java.lang.Long, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SaldoApertura> selectByEmpresaYCorte(Long idEmpresa, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo selectByEmpresaYCorte de SaldoApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);
		Query query = em.createQuery(" select   t "
				+ " from     SaldoApertura t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.fechaCorte = :fechaCorte "
				+ " order by t.identificacion, t.tipoSaldo ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoAperturaDaoService#selectPendientesPorAplicar(java.lang.Long, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SaldoApertura> selectPendientesPorAplicar(Long idEmpresa, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo selectPendientesPorAplicar de SaldoApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);
		Query query = em.createQuery(" select   t "
				+ " from     SaldoApertura t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.fechaCorte = :fechaCorte "
				+ "          and (t.aplicado is null or t.aplicado <> 'S') "
				+ " order by t.tipoSaldo, t.identificacion ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoAperturaDaoService#selectAplicados(java.lang.Long, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<SaldoApertura> selectAplicados(Long idEmpresa, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo selectAplicados de SaldoApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);
		Query query = em.createQuery(" select   t "
				+ " from     SaldoApertura t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.fechaCorte = :fechaCorte "
				+ "          and t.aplicado = 'S' "
				+ " order by t.tipoSaldo desc, t.identificacion desc ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.SaldoAperturaDaoService#selectDuplicados(java.lang.Long, java.time.LocalDate)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<Object[]> selectDuplicados(Long idEmpresa, LocalDate fechaCorte) throws Throwable {
		System.out.println("Ingresa al metodo selectDuplicados de SaldoApertura, empresa: " + idEmpresa
				+ ", corte: " + fechaCorte);
		Query query = em.createQuery(" select   t.identificacion, t.tipoSaldo, t.anio, count(t) "
				+ " from     SaldoApertura t "
				+ " where    t.empresa.codigo = :idEmpresa "
				+ "          and t.fechaCorte = :fechaCorte "
				+ " group by t.identificacion, t.tipoSaldo, t.anio "
				+ " having   count(t) > 1 ");
		query.setParameter("idEmpresa", idEmpresa);
		query.setParameter("fechaCorte", fechaCorte);
		return query.getResultList();
	}
}
