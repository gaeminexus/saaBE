package com.saa.ejb.cxp.daoImpl;

import java.util.List;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraV2DaoService;
import com.saa.model.cxp.DetalleRetencionCompraV2;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Stateless
public class DetalleRetencionCompraV2DaoServiceImpl extends EntityDaoImpl<DetalleRetencionCompraV2>
		implements DetalleRetencionCompraV2DaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) DetalleRetencionCompraV2");
		return new String[]{
			"id", "retencionCompraV2", "tipoDocReten", "numDocReten", "fechaEmiDoc",
			"fechaReg", "docResAutorizacion", "docResTotalSinImpuestos",
			"docResIvaCero", "docResPorIva", "docResTotalIva", "docResTotal",
			"docResForPago", "codImpuesto", "codRetencion", "baseImponible",
			"porcentajeReten", "valorReten", "estado"
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DetalleRetencionCompraV2> selectByRetencionCompraV2(Long idRetencionCompraV2) throws Throwable {
		System.out.println("Ingresa al metodo selectByRetencionCompraV2 con id: " + idRetencionCompraV2);
		Query query = em.createQuery(
			"select e from DetalleRetencionCompraV2 e " +
			"where e.retencionCompraV2.id = :idRetencionCompraV2 " +
			"order by e.id"
		);
		query.setParameter("idRetencionCompraV2", idRetencionCompraV2);
		return query.getResultList();
	}
}
