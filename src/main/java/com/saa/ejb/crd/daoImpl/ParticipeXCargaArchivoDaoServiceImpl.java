package com.saa.ejb.crd.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.ParticipeXCargaArchivoDaoService;
import com.saa.model.crd.ParticipeXCargaArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class ParticipeXCargaArchivoDaoServiceImpl extends EntityDaoImpl<ParticipeXCargaArchivo> implements ParticipeXCargaArchivoDaoService{

	@PersistenceContext
	EntityManager em;
	
	@Override
	public ParticipeXCargaArchivo selectByCodigoPetroYProductoEnCarga(Long codigoPetro, String codigoProducto, Long codigoCargaArchivo) throws Throwable {
		try {
			String codigoProductoTrim = (codigoProducto != null) ? codigoProducto.trim() : null;
			
			String jpql = "SELECT p FROM ParticipeXCargaArchivo p " +
						 "WHERE p.codigoPetro = :codigoPetro " +
						 "AND TRIM(p.detalleCargaArchivo.codigoPetroProducto) = :codigoProducto " +
						 "AND p.detalleCargaArchivo.cargaArchivo.codigo = :codigoCargaArchivo";
			
			Query query = em.createQuery(jpql);
			query.setParameter("codigoPetro", codigoPetro);
			query.setParameter("codigoProducto", codigoProductoTrim);
			query.setParameter("codigoCargaArchivo", codigoCargaArchivo);
			
			@SuppressWarnings("unchecked")
			List<ParticipeXCargaArchivo> resultados = query.getResultList();
			
			if (resultados != null && !resultados.isEmpty()) {
				return resultados.get(0);
			}
			
			return null;
			
		} catch (Exception e) {
			System.err.println("Error al buscar partícipe por código Petro y producto: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}
	
}
