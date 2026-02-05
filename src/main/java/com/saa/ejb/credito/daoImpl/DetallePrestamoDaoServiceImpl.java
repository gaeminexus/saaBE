package com.saa.ejb.credito.daoImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.DetallePrestamoDaoService;
import com.saa.model.crd.DetallePrestamo;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetallePrestamoDaoServiceImpl extends EntityDaoImpl<DetallePrestamo> implements DetallePrestamoDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePrestamo> selectByRangoFechas(LocalDateTime fechaDesde, LocalDateTime fechaHasta)
			throws Throwable {
		// System.out.println("Ingresa al metodo selectByCodigoPetro con codigoPetro: " + codigoPetro);
		Query query = em.createQuery(" select b " +
									 " from   DetallePrestamo b" +
									 " where  b.fechaVencimiento between :fechaDesde and :fechaHasta " +
									 " order by b.prestamo.entidad.razonSocial, b.fechaVencimiento ");
		query.setParameter("fechaDesde", fechaDesde);
		query.setParameter("fechaHasta", fechaHasta);
		return  query.getResultList();
	}

}
