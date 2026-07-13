package com.saa.ejb.cxc.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathFacturaDaoService;
import com.saa.model.cxc.PathFactura;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class PathFacturaDaoServiceImpl extends EntityDaoImpl<PathFactura> implements PathFacturaDaoService {

	@PersistenceContext
	EntityManager em;

	@Override
	public String[] obtieneCampos() {
		return new String[]{
			"id",
			"factura",
			"path",
			"alterno"
		};
	}
}
