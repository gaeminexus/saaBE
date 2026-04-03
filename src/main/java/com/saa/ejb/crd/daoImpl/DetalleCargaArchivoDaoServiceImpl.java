package com.saa.ejb.crd.daoImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.crd.dao.DetalleCargaArchivoDaoService;
import com.saa.model.crd.DetalleCargaArchivo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleCargaArchivoDaoServiceImpl extends EntityDaoImpl<DetalleCargaArchivo> implements DetalleCargaArchivoDaoService{

	@PersistenceContext
	EntityManager em;

	@Override
	@SuppressWarnings("unchecked")
	public List<DetalleCargaArchivo> selectByCargaArchivo(Long codigoCargaArchivo) {
		System.out.println("DetalleCargaArchivoDaoService.selectByCargaArchivo - CargaArchivo: " + codigoCargaArchivo);
		
		try {
			Query query = em.createQuery(
				"SELECT d " +
				"FROM DetalleCargaArchivo d " +
				"WHERE d.cargaArchivo.codigo = :codigoCargaArchivo " +
				"ORDER BY d.codigo ASC"
			);
			query.setParameter("codigoCargaArchivo", codigoCargaArchivo);
			
			List<DetalleCargaArchivo> resultados = query.getResultList();
			System.out.println("  Detalles encontrados: " + resultados.size());
			
			return resultados;
			
		} catch (Exception e) {
			System.err.println("ERROR en selectByCargaArchivo: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

}
