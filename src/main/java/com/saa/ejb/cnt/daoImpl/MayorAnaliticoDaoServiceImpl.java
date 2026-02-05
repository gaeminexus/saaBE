package com.saa.ejb.cnt.daoImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.MayorAnaliticoDaoService;
import com.saa.model.cnt.MayorAnalitico;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class MayorAnaliticoDaoServiceImpl extends EntityDaoImpl<MayorAnalitico>  implements MayorAnaliticoDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"planCuenta",
							"nombreCuenta",
							"numeroCuenta",
							"secuencia",
							"saldoAnterior",
							"debe",
							"haber",
							"saldoActual",
							"detalleMayorAnaliticos"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.MayorAnaliticoDaoService#obtieneSecuenciaReporte()
	 */
	public Long obtieneSecuenciaReporte() throws Throwable {
		System.out.println("Ingresa al Metodo obtieneSecuenciaReporte ");
		Long valor = 0L;
		String sql = " select   max(secuencial)" +
					 " from     MayorAnalitico";
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

	@SuppressWarnings("unchecked")
	public List<MayorAnalitico> selectBySecuencia(Long secuencia)
			throws Throwable {
		System.out.println("Ingresa al Metodo selectBySecuencia con secuencial: " + secuencia);
		Query query = em.createQuery(" select b " +
									 " from   MayorAnalitico b " +
								     " where  b.secuencial = :secuencia" +
								     " order by b.numeroCuenta");
		query.setParameter("secuencia", secuencia);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.MayorAnaliticoDaoService#selectPeriodosMayorizadoNoMayorizado(java.lang.Long, java.util.LocalDate, java.util.LocalDate, java.util.LocalDate, java.lang.Long)
	 */
	public Long selectPeriodosMayorizadoNoMayorizado(Long empresa, LocalDate fechaInicio, LocalDate fechaFin, 
													 Long estado1, Long estado2, Long estado3) throws Throwable {
		System.out.println("Ingresa al Metodo selectPeriodosMayorizadoNoMayorizado con empresa:" + empresa + ", fechaInicio" + fechaInicio + ", fechaFin" + fechaFin 
																					   + ", estado1" + estado1 + ", estado2" + estado2 + ", estado3" + estado3);
		Query query = em.createQuery(" select   count(*)" +
									 " from     Periodo b" +
									 " where    b.empresa.codigo = :empresa" +
									 " 		    and b.codigo between " +
									 "							  ( select   d.codigo" +
									 "								from     Periodo d" +
									 "								where    d.empresa.codigo = :empresa" +
									 "										 and :fechaInicio between primerDia and ultimoDia)" +
									 "					  	  and ( select   e.codigo" +
									 "								from     Periodo e" +
									 "								where    e.empresa.codigo = :empresa" +
									 "										 and :fechaFin between primerDia and ultimoDia)" +
									 "			and b.estado in (:estado1, :estado2, :estado3)");
		query.setParameter("empresa", empresa);
		query.setParameter("fechaInicio", fechaInicio);
		query.setParameter("fechaFin", fechaFin);
		query.setParameter("estado1", estado1);
		query.setParameter("estado2", estado2);
		query.setParameter("estado3", estado3);
		return (Long) query.getSingleResult();
	}

}
