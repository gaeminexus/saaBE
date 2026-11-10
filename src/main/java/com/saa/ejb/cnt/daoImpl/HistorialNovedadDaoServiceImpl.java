package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.HistorialNovedadDaoService;
import com.saa.model.cnt.HistorialNovedad;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link HistorialNovedad} (CNT.HSNV).
 */
@Stateless
public class HistorialNovedadDaoServiceImpl extends EntityDaoImpl<HistorialNovedad> implements HistorialNovedadDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) HistorialNovedad");
		return new String[] {
				"idNovedad",
				"idRequerimiento",
				"accion",
				"estadoAnterior",
				"estadoNuevo",
				"motivo",
				"fecha",
				"usuario" };
	}
}
