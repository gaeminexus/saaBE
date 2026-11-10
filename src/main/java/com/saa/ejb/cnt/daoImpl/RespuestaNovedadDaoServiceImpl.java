package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.RespuestaNovedadDaoService;
import com.saa.model.cnt.RespuestaNovedad;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link RespuestaNovedad} (CNT.RSNV).
 */
@Stateless
public class RespuestaNovedadDaoServiceImpl extends EntityDaoImpl<RespuestaNovedad> implements RespuestaNovedadDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) RespuestaNovedad");
		return new String[] {
				"idRequerimiento",
				"idDecision",
				"fecha",
				"canal",
				"area",
				"texto",
				"opcion",
				"estado",
				"fechaRevision",
				"usuarioRevision",
				"observacionRevision",
				"fechaRegistro",
				"usuarioRegistro" };
	}
}
