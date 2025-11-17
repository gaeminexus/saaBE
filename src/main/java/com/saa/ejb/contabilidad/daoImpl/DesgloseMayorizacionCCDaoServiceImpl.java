package com.saa.ejb.contabilidad.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.contabilidad.dao.DesgloseMayorizacionCCDaoService;
import com.saa.model.contabilidad.DesgloseMayorizacionCC;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DesgloseMayorizacionCCDaoServiceImpl extends EntityDaoImpl<DesgloseMayorizacionCC>  implements DesgloseMayorizacionCCDaoService{
	
	//Inicializa persistence context
	@PersistenceContext
	EntityManager em;
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.sistema.ejb.utilImpl.EntityDaoImpl#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Ambito");
		return new String[]{"codigo",
							"detalleMayorizacionCC",
							"planCuenta",
							"valorDebe",
							"valorHaber",
							"numeroCuenta",
							"codigoPadreCuenta",
							"nombreCuenta",
							"tipoCuenta",
							"nivelCuenta"
							};
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DesgloseMayorizacionCCDaoService#selectNivelesByMayorizacionCC(java.lang.Long)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectNivelesByMayorizacionCC(Long idDetalleCC) throws Throwable {
		System.out.println("Ingresa al metodo selectNivelesByMayorizacion de mayorizacionCC: " + idDetalleCC);
		Query query = em.createQuery(" select   distinct b.nivelCuenta" +
								     " from     DesgloseMayorizacionCC b " +
								     " where    b.detalleMayorizacionCC.codigo = :idDetalleCC " +
									 "          and   b.nivelCuenta > 1 " +
									 " order by b.nivelCuenta desc");
		query.setParameter("idDetalleCC", idDetalleCC);
		return query.getResultList();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DesgloseMayorizacionCCDaoService#selectPadresByNivel(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings({ "rawtypes" })
	public List selectPadresByNivel(Long idDetalleCC, Long nivel) throws Throwable {
		System.out.println("Ingresa al metodo selectPadresByNivel de mayorizacionCC: " + idDetalleCC + " y nivel: " + nivel);
		Query query = em.createQuery(" select   distinct b.codigoPadreCuenta " +
									 " from     DesgloseMayorizacionCC b " +
									 " where    b.detalleMayorizacionCC.codigo = :idDetalleCC " +
									 "          and   b.nivelCuenta = :nivel" +
									 " order by b.codigoPadreCuenta ");
		query.setParameter("idDetalleCC", idDetalleCC);
		query.setParameter("nivel", nivel);
		return query.getResultList();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DesgloseMayorizacionCCDaoService#selectByCuentaPadreNivel(java.lang.Long, java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DesgloseMayorizacionCC> selectByCuentaPadreNivel(Long idDetalleCC, Long nivel, Long padre) throws Throwable {
		System.out.println("Ingresa al metodo selectByCuentaPadreNivel de mayorizacionCC: " + idDetalleCC + " y nivel: " + nivel);
		Query query = em.createQuery(" select b " +
									 " from   DesgloseMayorizacionCC b " +
									 " where  b.detalleMayorizacionCC.codigo = :idDetalleCC " +
									 "        and   b.nivelCuenta = :nivel " +
									 "        and   b.codigoPadreCuenta = :padre");
		query.setParameter("idDetalleCC", idDetalleCC);
		query.setParameter("nivel", nivel);
		query.setParameter("padre", padre);
		return query.getResultList();
	}
	
	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DesgloseMayorizacionCCDaoService#selectByMayorizacionCCCuenta(java.lang.Long, java.lang.Long)
	 */
	@SuppressWarnings("unchecked")
	public List<DesgloseMayorizacionCC> selectByMayorizacionCCCuenta(Long idDetalleCC, Long cuenta) throws Throwable {
		System.out.println("Ingresa al metodo selectByMayorizacionCCCuenta de mayorizacionCC: " + idDetalleCC + ", cuenta: " + cuenta);
		Query query = em.createQuery(" select b " +
									 " from   DesgloseMayorizacionCC b " +
									 " where  b.detalleMayorizacionCC.codigo = :idDetalleCC " +
									 "        and   b.planCuenta.codigo = :cuenta");
		query.setParameter("idDetalleCC", idDetalleCC);		
		query.setParameter("cuenta", cuenta);
		return query.getResultList();
	}
	

	/* (non-Javadoc)
	 * @see com.compuseg.income.contabilidad.ejb.dao.DesgloseMayorizacionCCDaoService#deleteByDetalleCC(java.lang.Long)
	 */
	public void deleteByDetalleCC(Long idDetalleCC) throws Throwable {
		System.out.println("Ingresa al metodo deleteByMayorizacionCC de desglose: " + idDetalleCC);
		Query query = em.createQuery(" Delete   from DesgloseMayorizacionCC b " +
								     " where    b.detalleMayorizacionCC.codigo = :idMayorizacionCC ");
		query.setParameter("idMayorizacionCC", idDetalleCC);

		query.executeUpdate();
	
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DesgloseMayorizacionCC> selectByIdDetalleMayorizacionCC(Long idDetalleMayorizacionCC) throws Throwable {
		System.out.println("Ingresa al metodo selectNivelesByMayorizacion de mayorizacionCC: " + idDetalleMayorizacionCC);
		Query query = em.createQuery(" select   b " +
								     " from     DesgloseMayorizacionCC b " +
								     " where    b.detalleMayorizacionCC.codigo = :idDetalleCC " +
									 "          and   b.nivelCuenta > 1 " +
									 " order by b.nivelCuenta desc");
		query.setParameter("idDetalleCC", idDetalleMayorizacionCC);
		return query.getResultList();
	}
	
}
