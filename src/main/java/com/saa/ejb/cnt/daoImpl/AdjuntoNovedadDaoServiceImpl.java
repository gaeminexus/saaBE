package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.AdjuntoNovedadDaoService;
import com.saa.model.cnt.AdjuntoNovedad;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link AdjuntoNovedad} (CNT.ADNV).
 */
@Stateless
public class AdjuntoNovedadDaoServiceImpl extends EntityDaoImpl<AdjuntoNovedad> implements AdjuntoNovedadDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) AdjuntoNovedad");
		return new String[] {
				"idNovedad",
				"idRequerimiento",
				"idRespuesta",
				"idDecision",
				"tipoDocumento",
				"nombreArchivo",
				"url",
				"mimeType",
				"tamanio",
				"hash",
				"observacion",
				"fechaCopiaExpediente",
				"fechaRegistro",
				"usuarioRegistro",
				"estado" };
	}
}
