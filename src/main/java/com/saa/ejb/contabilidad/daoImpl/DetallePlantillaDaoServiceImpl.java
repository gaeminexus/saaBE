package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetallePlantillaDaoServiceImpl extends EntityDaoImpl<DetallePlantilla>  implements DetallePlantillaDaoService{

	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"plantilla",
							"planCuenta",
							"descripcion",
							"movimiento",
							"fechaDesde",
							"fechaHasta",
							"auxiliar1",
							"auxiliar2",
							"auxiliar3",
							"auxiliar4",
							"auxiliar5",
							"estado",
							"fechaInactivo"};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetallePlantillaDaoService#selectByPlantilla(java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DetallePlantilla> selectByPlantilla(Long plantilla) throws Throwable {
		System.out.println("Ingresa al metodo selectByPlantilla con plantilla: " + plantilla);
		Query query = em.createQuery(" select b " +
									 " from   DetallePlantilla b " +
									 " where   b.plantilla.codigo = :plantilla");
		query.setParameter("plantilla", plantilla);
		return query.getResultList();
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetallePlantillaDaoService#recuperaCuentaContable(java.lang.Long)
	 */
	public PlanCuenta recuperaCuentaContable(Long idDetallePlantilla) throws Throwable {
		System.out.println("Ingresa al recuperaCuentaContable con id: " + idDetallePlantilla);
		PlanCuenta planCuenta = new PlanCuenta();
		Query query = em.createQuery(" select b " +
									 " from   DetallePlantilla " +
									 " where   codigo = :codigo");
		query.setParameter("codigo", idDetallePlantilla);
		DetallePlantilla detallePlantilla = (DetallePlantilla)query.getSingleResult();
		planCuenta = detallePlantilla.getPlanCuenta();
		return planCuenta;
	}
	

	@SuppressWarnings("unchecked")
	public List<DetallePlantilla> selectHermanosExcepto(Long codigoPlantillaPadre, Long codigoDetalleExcluir) throws Throwable {
	    Query query = em.createQuery(
	        "SELECT d " +
	        "FROM DetallePlantilla d " +
	        "WHERE d.plantilla.codigo = :codigoPlantillaPadre " +
	        "AND d.codigo != :codigoExcluir"
	    );
	    
	    query.setParameter("codigoPlantillaPadre", codigoPlantillaPadre);
	    query.setParameter("codigoExcluir", codigoDetalleExcluir);
	    
	    return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DetallePlantilla> selectByIdPlanCuenta(Long idPlanCuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdPlanCuenta de idPlanCuenta: " + idPlanCuenta);
		Query query = em.createQuery(" select b " +
									 " from   DetallePlantilla b " +
									 " where  b.planCuenta.codigo = :idPlanCuenta ");
		query.setParameter("idPlanCuenta", idPlanCuenta);		
		return query.getResultList();
	}
	
}
