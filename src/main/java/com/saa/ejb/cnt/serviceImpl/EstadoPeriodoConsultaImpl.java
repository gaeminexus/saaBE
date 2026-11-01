package com.saa.ejb.cnt.serviceImpl;

import com.saa.ejb.cnt.service.EstadoCierreService;
import com.saa.ejb.cnt.service.EstadoPeriodoConsulta;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * {@link EstadoPeriodoConsulta} sobre el servicio central de cierres de C17 ({@link EstadoCierreService}): el
 * periodo es DEFINITIVO solo si el cierre contable esta marcado (CNT.PRDO.PRDOCNCR = 1), el mismo criterio de
 * GET /prdo/estado/{idEmpresa}/{anio}/{mes}. Ya no se mira el estado de mayorizacion (PRDOESTD).
 */
@Stateless
public class EstadoPeriodoConsultaImpl implements EstadoPeriodoConsulta {

	@EJB
	private EstadoCierreService estadoCierreService;

	@Override
	public EstadoPeriodo estadoPeriodo(Long idEmpresa, int anio, int mes) throws Throwable {
		com.saa.ejb.cnt.service.dto.EstadoPeriodo e = estadoCierreService.estadoPeriodo(idEmpresa, Long.valueOf(anio),
				Long.valueOf(mes));
		if (e == null) {
			return new EstadoPeriodo(false, false, null, "Sin periodo contable para " + mes + "/" + anio);
		}
		String descripcion = e.isExistePeriodo() ? e.getEstado() : "SIN PERIODO CONTABLE";
		return new EstadoPeriodo(e.isExistePeriodo(), e.isDefinitivo(), null, descripcion);
	}
}
