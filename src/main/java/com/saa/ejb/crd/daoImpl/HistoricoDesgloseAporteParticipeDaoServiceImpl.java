package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.HistoricoDesgloseAporteParticipeDaoService;
import com.saa.model.crd.HistoricoDesgloseAporteParticipe;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class HistoricoDesgloseAporteParticipeDaoServiceImpl extends EntityDaoImpl<HistoricoDesgloseAporteParticipe> 
		implements HistoricoDesgloseAporteParticipeDaoService {

	@PersistenceContext
	EntityManager em;
	
	@Override
	public List<HistoricoDesgloseAporteParticipe> selectByCarga(Long idCarga) throws Throwable {
		try {
			Query query = em.createNamedQuery("HistoricoDesgloseAporteParticipeByCarga");
			query.setParameter("idCarga", idCarga);
			
			@SuppressWarnings("unchecked")
			List<HistoricoDesgloseAporteParticipe> resultados = query.getResultList();
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar desglose por carga: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	@Override
	public List<HistoricoDesgloseAporteParticipe> selectByCedula(String cedula) throws Throwable {
		try {
			Query query = em.createNamedQuery("HistoricoDesgloseAporteParticipeByCedula");
			query.setParameter("cedula", cedula);
			
			@SuppressWarnings("unchecked")
			List<HistoricoDesgloseAporteParticipe> resultados = query.getResultList();
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("Error al buscar desglose por cédula: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
	@Override
	public HistoricoDesgloseAporteParticipe selectByCodigoInternoYCarga(String codigoInterno, Long idCarga) throws Throwable {
		try {
			String jpql = "SELECT h FROM HistoricoDesgloseAporteParticipe h " +
						 "WHERE h.codigoInterno = :codigoInterno " +
						 "AND h.idCarga = :idCarga";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoInterno", codigoInterno);
			query.setParameter("idCarga", idCarga);
			
			@SuppressWarnings("unchecked")
			List<HistoricoDesgloseAporteParticipe> resultados = query.getResultList();
			
			if (resultados != null && !resultados.isEmpty()) {
				return resultados.get(0);
			}
			
			return null;
			
		} catch (Exception e) {
			System.err.println("Error al buscar desglose por código interno y carga: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
}
