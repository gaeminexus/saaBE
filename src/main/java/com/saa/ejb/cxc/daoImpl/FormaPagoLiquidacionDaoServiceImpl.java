package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.FormaPagoLiquidacionDaoService;
import com.saa.model.cxc.FormaPagoLiquidacion;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class FormaPagoLiquidacionDaoServiceImpl extends EntityDaoImpl<FormaPagoLiquidacion> implements FormaPagoLiquidacionDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id",
			"liquidacion",
			"formaPago",
			"valor",
			"plazo",
			"unidadTiempo"
		};
	}
}
