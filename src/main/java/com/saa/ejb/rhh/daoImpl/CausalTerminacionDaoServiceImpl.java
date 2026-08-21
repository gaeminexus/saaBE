package com.saa.ejb.rhh.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.rhh.dao.CausalTerminacionDaoService;
import com.saa.model.rhh.CausalTerminacion;

import jakarta.ejb.Stateless;

/**
 * @author GaemiSoft.
 * Implementacion CausalTerminacionDaoService.
 */
@Stateless
public class CausalTerminacionDaoServiceImpl extends EntityDaoImpl<CausalTerminacion> implements CausalTerminacionDaoService {

	/* (non-Javadoc)
	 * @see com.saa.ejb.rhh.dao.CausalTerminacionDaoService#obtieneCampos()
	 */
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) CausalTerminacion");
		return new String[]{"codigo",
							"empresa",
							"nombre",
							"codigoAlterno",
							"articulo",
							"generaDesahucio",
							"generaDespido",
							"pagaVacacionesProporcionales",
							"pagaDecimosProporcionales",
							"generaJubilacionPatronal",
							"requiereAvisoSalida",
							"requiereActaSut",
							"estado",
							"fechaRegistro",
							"usuarioRegistro"};
	}

}
