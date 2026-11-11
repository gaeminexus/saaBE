package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.RequerimientoNovedadDaoService;
import com.saa.model.cnt.RequerimientoNovedad;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link RequerimientoNovedad} (CNT.RQNV).
 */
@Stateless
public class RequerimientoNovedadDaoServiceImpl extends EntityDaoImpl<RequerimientoNovedad> implements RequerimientoNovedadDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) RequerimientoNovedad");
		return new String[] {
				"idNovedad",
				"secuencia",
				"tipo",
				"area",
				"descripcion",
				"tipoReferencia",
				"referencia",
				"valor",
				"fechaReferencia",
				"opciones",
				"supuesto",
				"estado",
				"fechaRegistro",
				"usuarioRegistro" };
	}
}
