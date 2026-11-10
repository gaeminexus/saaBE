package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.VinculoNovedadDaoService;
import com.saa.model.cnt.VinculoNovedad;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link VinculoNovedad} (CNT.VNNV).
 */
@Stateless
public class VinculoNovedadDaoServiceImpl extends EntityDaoImpl<VinculoNovedad> implements VinculoNovedadDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) VinculoNovedad");
		return new String[] {
				"idNovedad",
				"idRequerimiento",
				"tipo",
				"idAsiento",
				"idDetalleAsiento",
				"idPartidaC22",
				"idDecision",
				"idNovedadRelacionada",
				"tipoDocumentoModulo",
				"idDocumentoModulo",
				"valor",
				"observacion",
				"fechaRegistro",
				"usuarioRegistro",
				"estado",
				"fechaAnulacion",
				"usuarioAnulacion",
				"motivoAnulacion" };
	}
}
