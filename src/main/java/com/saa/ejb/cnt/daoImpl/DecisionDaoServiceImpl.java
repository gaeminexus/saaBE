package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.DecisionDaoService;
import com.saa.model.cnt.Decision;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link Decision} (CNT.DCSN).
 */
@Stateless
public class DecisionDaoServiceImpl extends EntityDaoImpl<Decision> implements DecisionDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) Decision");
		return new String[] {
				"idEmpresa",
				"numero",
				"fecha",
				"tema",
				"detalle",
				"tipoDecide",
				"cargo",
				"evidencia",
				"origen",
				"fechaCopiaExpediente",
				"usuarioCopiaExpediente",
				"fechaRegistro",
				"usuarioRegistro",
				"estado" };
	}
}
