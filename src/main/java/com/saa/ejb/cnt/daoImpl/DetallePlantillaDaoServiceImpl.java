package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.rubros.Estado;

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
	
	/* (non-Javadoc)
	 * @see com.saa.ejb.cnt.dao.DetallePlantillaDaoService#selectByPlantillaYAuxiliar(java.lang.Long, int)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DetallePlantilla selectByPlantillaYAuxiliar(Long idPlantilla, int auxiliar1) throws Throwable {
		System.out.println("Ingresa al metodo selectByPlantillaYAuxiliar, plantilla: " + idPlantilla
				+ ", auxiliar1: " + auxiliar1);
		Query query = em.createQuery(" select   b "
				+ " from     DetallePlantilla b "
				+ " where    b.plantilla.codigo = :idPlantilla "
				+ "          and b.auxiliar1 = :auxiliar1 "
				+ "          and b.estado = :activo "
				+ " order by b.codigo ");
		query.setParameter("idPlantilla", idPlantilla);
		query.setParameter("auxiliar1", Long.valueOf(auxiliar1));
		query.setParameter("activo", Long.valueOf(Estado.ACTIVO));
		List<DetallePlantilla> lista = query.getResultList();
		// Devuelve null en vez de lanzar: que una plantilla no defina una linea es normal
		// --el asiento de pago no tiene linea de decimos-- y el llamador decide si le hace
		// falta o no.
		return lista.isEmpty() ? null : lista.get(0);
	}

}
