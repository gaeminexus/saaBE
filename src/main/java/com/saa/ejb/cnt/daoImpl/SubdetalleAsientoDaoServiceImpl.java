package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.SubdetalleAsientoDaoService;
import com.saa.model.cnt.SubdetalleAsiento;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class SubdetalleAsientoDaoServiceImpl extends EntityDaoImpl<SubdetalleAsiento> implements SubdetalleAsientoDaoService {
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;	
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) SubdetalleAsiento");
		return new String[]{
			"codigo",
			"detalleAsiento",
			"codigoActivo",
			"nombreBien",
			"categoria",
			"tipo",
			"fechaAdquisicion",
			"costoAdquisicion",
			"mejorasCapitalizadas",
			"valorResidual",
			"baseDepreciar",
			"vidaUtilTotal",
			"vidaUtilRemanente",
			"porcentajeDepreciacion",
			"cuotaDepreciacion",
			"depreciacionAcumulada",
			"valorNetoLibros",
			"ubicacionGeneral",
			"ubicacionEspecifica",
			"responsable",
			"estadoFisico",
			"factura",
			"observaciones"
		};
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<SubdetalleAsiento> selectByIdDetalleAsiento(Long idDetalleAsiento) throws Throwable {
		System.out.println("Ingresa al metodo selectByIdDetalleAsiento con ID: " + idDetalleAsiento);
		Query query = em.createQuery(
			"select s from SubdetalleAsiento s " +
			"where s.detalleAsiento.codigo = :idDetalleAsiento " +
			"order by s.codigo"
		);
		query.setParameter("idDetalleAsiento", idDetalleAsiento);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<SubdetalleAsiento> selectByCodigoActivo(String codigoActivo) throws Throwable {
		System.out.println("Ingresa al metodo selectByCodigoActivo con código: " + codigoActivo);
		Query query = em.createQuery(
			"select s from SubdetalleAsiento s " +
			"where s.codigoActivo = :codigoActivo " +
			"order by s.codigo"
		);
		query.setParameter("codigoActivo", codigoActivo);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<SubdetalleAsiento> selectByCategoria(String categoria) throws Throwable {
		System.out.println("Ingresa al metodo selectByCategoria con categoría: " + categoria);
		Query query = em.createQuery(
			"select s from SubdetalleAsiento s " +
			"where s.categoria = :categoria " +
			"order by s.codigo"
		);
		query.setParameter("categoria", categoria);
		return query.getResultList();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<SubdetalleAsiento> selectByResponsable(String responsable) throws Throwable {
		System.out.println("Ingresa al metodo selectByResponsable con responsable: " + responsable);
		Query query = em.createQuery(
			"select s from SubdetalleAsiento s " +
			"where s.responsable = :responsable " +
			"order by s.codigo"
		);
		query.setParameter("responsable", responsable);
		return query.getResultList();
	}
}
