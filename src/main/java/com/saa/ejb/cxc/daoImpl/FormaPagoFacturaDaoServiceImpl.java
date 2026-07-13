package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.FormaPagoFacturaDaoService;
import com.saa.model.cxc.FormaPagoFactura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class FormaPagoFacturaDaoServiceImpl extends EntityDaoImpl<FormaPagoFactura> implements FormaPagoFacturaDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id",
			"factura",
			"formaPago",
			"valor",
			"plazo",
			"unidadTiempo"
		};
	}
}
