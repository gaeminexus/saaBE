package com.saa.ejb.cnt.daoImpl;

import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cnt.dao.NovedadSeguimientoDaoService;
import com.saa.model.cnt.NovedadSeguimiento;

import jakarta.ejb.Stateless;

/**
 * C25 — Implementación del DAO de {@link NovedadSeguimiento} (CNT.NVDD).
 */
@Stateless
public class NovedadSeguimientoDaoServiceImpl extends EntityDaoImpl<NovedadSeguimiento> implements NovedadSeguimientoDaoService {

	@Override
	public String[] obtieneCampos() {
		System.out.println("Ingresa al metodo (campos) NovedadSeguimiento");
		return new String[] {
				"idEmpresa",
				"idPeriodo",
				"anio",
				"mes",
				"modulo",
				"codigoNovedad",
				"titulo",
				"hallazgo",
				"resolucion",
				"tipoRespuesta",
				"area",
				"valor",
				"criterios",
				"fuentes",
				"archivo",
				"version",
				"lote",
				"estado",
				"fechaEnvio",
				"usuarioEnvio",
				"fechaLimite",
				"fechaCierre",
				"usuarioCierre",
				"motivoCierre",
				"fechaRegistro",
				"usuarioRegistro" };
	}
}
