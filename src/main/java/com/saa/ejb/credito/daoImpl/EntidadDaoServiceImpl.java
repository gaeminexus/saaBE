package com.saa.ejb.credito.daoImpl;

import java.math.BigDecimal;
import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.model.credito.Entidad;
import com.saa.rubros.ASPSensibilidadBusquedaCoincidencias;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class EntidadDaoServiceImpl extends EntityDaoImpl<Entidad> implements EntidadDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	@EJB
	DetalleRubroDaoService detalleRubroDaoService;
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByCodigoPetro(Long codigoPetro) throws Throwable {
		// System.out.println("Ingresa al metodo selectByCodigoPetro con codigoPetro: " + codigoPetro);
		Query query = em.createQuery(" select b " +
									 " from   Entidad b" +
									 " where  b.rolPetroComercial = :codigoPetro");
		query.setParameter("codigoPetro", codigoPetro);
		return  query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BigDecimal> selectCoincidenciasByNombre(String nombre) throws Throwable {
		System.out.println("Ingresa al metodo selectCoincidenciasByNombre de asiento con empresa: " + nombre);
		Double sensibilidad = detalleRubroDaoService.selectValorNumericoByRubAltDetAlt(
				Rubros.ASP_SENSIBILIDAD_BUSQUEDA_COINCIDENCIAS, 
				ASPSensibilidadBusquedaCoincidencias.PORCENTAJE_SENSIBILIDAD);
		Query query = em.createNativeQuery(" select   e.ENTDCDGO " +
									 	   " from     CRD.ENTD e " +
									 	   " where    UTL_MATCH.JARO_WINKLER_SIMILARITY(e.ENTDNMCM, :nombre) > :sensibilidad ");
		query.setParameter("nombre", nombre);		
		query.setParameter("sensibilidad", sensibilidad);
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Entidad> selectByNombrePetro35(String nombre) throws Throwable {
		// System.out.println("Ingresa al metodo selectByNombrePetro35 con nombre: " + nombre);
		Query query = em.createQuery(" select b " +
									 " from   Entidad b " +
									 " where  substring(trim(b.razonSocial),1,35) = trim(:nombre) ");
		query.setParameter("nombre", nombre);
		return  query.getResultList();
	}

}
