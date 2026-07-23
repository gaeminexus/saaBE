package com.saa.ejb.cxc.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleRetencionV2DaoService;
import com.saa.model.cxc.DetalleRetencionV2;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleRetencionV2DaoServiceImpl extends EntityDaoImpl<DetalleRetencionV2>
		implements DetalleRetencionV2DaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleRetencionV2");
		return new String[]{
			"id", "retencionV2", "tipoDocReten", "numDocReten", "fechaEmiDoc",
			"fechaReg", "docResAutorizacion", "docResTotalSinImpuestos",
			"docResIvaCero", "docResPorIva", "docResTotalIva", "docResTotal",
			"docResForPago", "codImpuesto", "codRetencion", "baseImponible",
			"porcentajeReten", "valorReten", "estado"
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetalleRetencionV2> selectByRetencionV2(Long idRetencionV2) throws Throwable {
		System.out.println("Ingresa al metodo selectByRetencionV2 con id: " + idRetencionV2);
		Query query = em.createQuery(
			"select e from DetalleRetencionV2 e " +
			"where e.retencionV2.id = :idRetencionV2 " +
			"order by e.id"
		);
		query.setParameter("idRetencionV2", idRetencionV2);
		return query.getResultList();
	}
}
