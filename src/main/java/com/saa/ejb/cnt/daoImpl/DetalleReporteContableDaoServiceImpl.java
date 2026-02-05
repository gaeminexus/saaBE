package com.saa.ejb.cnt.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.DetalleReporteContableDaoService;
import com.saa.model.cnt.DetalleReporteContable;
import com.saa.rubros.Estado;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleReporteContableDaoServiceImpl extends EntityDaoImpl<DetalleReporteContable>  implements DetalleReporteContableDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"reporteContable",
							"cuentaDesde",
							"numeroDesde",
							"nombreDesde",
							"cuentaHasta",
							"numeroHasta",
							"nombreHasta",
							"signo"};
	}

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DetalleReporteContableDaoService#selectByDetalleReporteContable(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List <DetalleReporteContable> selectByDetalleReporteContable(Long empresa, Long codigoAlterno) throws Throwable {
		System.out.println("Ingresa al selectByDetalleReporteContable con empresa: " + empresa + ", codigoAlterno: " + codigoAlterno);
		Query query = em.createQuery (" select b " +
									 " from   DetalleReporteContable b " +
									  " where  b.reporteContable.codigoAlterno = :codigoAlterno" +
									  " 	   and   b.reporteContable.empresa.codigo = :empresa" +
									  " 	   and   b.reporteContable.estado = :estado");
		query.setParameter("empresa", empresa);
		query.setParameter("codigoAlterno", codigoAlterno);		
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}
}
