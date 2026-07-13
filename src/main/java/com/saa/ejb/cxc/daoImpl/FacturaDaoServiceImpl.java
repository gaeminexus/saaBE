package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.FacturaDaoService;
import com.saa.model.cxc.Factura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class FacturaDaoServiceImpl extends EntityDaoImpl<Factura> implements FacturaDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id",
			"tipoComprobante",
			"facturador",
			"comprador",
			"tipoDoc",
			"numero",
			"numEstablecimiento",
			"numPtoEmision",
			"secuencial",
			"ambiente",
			"clave",
			"fecha",
			"observacion",
			"subtotal",
			"subcero",
			"subtotal5",
			"subtotal8",
			"pIVA",
			"vIVA",
			"vIVA5",
			"vIVA8",
			"vICE",
			"vIRBPNR",
			"descuento",
			"porDescuento",
			"propina",
			"subsidio",
			"totalSinSub",
			"ahorroSub",
			"total",
			"ptoEmision",
			"usuario",
			"pathGen",
			"autorizacion",
			"fechaAutorizacion",
			"formaPago",
			"estado",
			"estadoEmision"
		};
	}
}
