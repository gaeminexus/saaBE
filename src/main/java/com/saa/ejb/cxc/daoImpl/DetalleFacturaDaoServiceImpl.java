package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleFacturaDaoService;
import com.saa.model.cxc.DetalleFactura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class DetalleFacturaDaoServiceImpl extends EntityDaoImpl<DetalleFactura> implements DetalleFacturaDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id",
			"factura",
			"descripcion",
			"cantidad",
			"valor",
			"subTotal",
			"descuento",
			"baseImponible",
			"porcentajeIVA",
			"valorIVA",
			"porcentajeICE",
			"valorICE",
			"subsidio",
			"precioSinSub",
			"total",
			"producto",
			"codigoIVASRI",
			"estado"
		};
	}
}
