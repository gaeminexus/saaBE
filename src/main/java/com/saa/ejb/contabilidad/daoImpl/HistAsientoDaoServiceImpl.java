package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.HistAsientoDaoService;
import com.saa.model.contabilidad.HistAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistAsientoDaoServiceImpl extends EntityDaoImpl<HistAsiento>  implements HistAsientoDaoService{
	
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
							"tipoAsiento",
							"fechaAsiento",
							"numero",
							"estado",
							"observaciones",
							"nombreUsuario",
							"idReversion",
							"numeroMes",
							"numeroAnio",
							"moneda",
							"histmayorizacion",
							"rubroModuloClienteP",
							"rubroModuloClienteH",
							"fechaIngreso",
							"rubroModuloSistemaP",
							"rubroModuloSistemaH",
							"idAsientoOriginal"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistAsientoDaoService#deleteByMayorizacion(java.lang.Long)
	 */
	public void deleteByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo deleteByDesmayorizacion con idDesmayorizacion: " + idDesmayorizacion);
		Query query = em.createQuery(" Delete from HistAsiento b" +
									 " where b.histMayorizacion.codigo = :idDesmayorizacion");
		query.setParameter("idDesmayorizacion", idDesmayorizacion);

		query.executeUpdate();
	
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistAsientoDaoService#selectByIdAsientoOrigen(java.lang.Long)
	 */
	public HistAsiento selectByIdAsientoOrigen(Long idAsientoOrigen, Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdAsientoOrigen con idAsientoOrigen: " + idAsientoOrigen);
		HistAsiento resultado = new HistAsiento();
		Query query = em.createQuery(" select b " +
									 " from   HistAsiento b " +
									 " where  b.idAsientoOriginal = :idAsientoOrigen" +
									 "		  and   b.histMayorizacion.codigo = :idDesmayorizacion");
		query.setParameter("idAsientoOrigen", idAsientoOrigen);
		query.setParameter("idDesmayorizacion", idDesmayorizacion);
		resultado = (HistAsiento)query.getSingleResult();

		return resultado;
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.HistAsientoDaoService#selectByDesmayorizacion(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<HistAsiento> selectByDesmayorizacion(Long idDesmayorizacion) throws Throwable {
		System.out.println("Ingresa al metodo selectByDesmayorizacion con idDesmayorizacion: " + idDesmayorizacion);
		Query query = em.createQuery(" select b " +
									 " from   HistAsiento b " +
		 							 " where   b.histMayorizacion.codigo = :idDesmayorizacion");
		query.setParameter("idDesmayorizacion", idDesmayorizacion);
		return query.getResultList();
	}

}
