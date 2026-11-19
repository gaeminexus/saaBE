package com.saa.ejb.cnt.service;

import com.saa.ejb.cnt.flujo.ResultadoComparativo;
import com.saa.ejb.cnt.flujo.ResultadoFlujoEfectivo;
import com.saa.ejb.cnt.flujo.SolicitudFlujoEfectivo;

import jakarta.ejb.Local;

/**
 * Formatos de ASOPREP derivados del B17 (C38): estado de flujos de efectivo (metodo indirecto), flujo de caja real
 * (metodo directo, formato BIESS) y balance comparativo. Ver docs/logica-negocio/rpr/FLUJO-EFECTIVO-Y-COMPARATIVO.md.
 */
@Local
public interface FlujoEfectivoService {

	/**
	 * Estado de flujos de efectivo del mes o acumulado del anio, con controles y, si se pide, el flujo de caja directo
	 * mes a mes. Sin {@code preliminar} exige el mes cerrado (REPORTE_MES_NO_CERRADO), igual que el B17.
	 */
	ResultadoFlujoEfectivo generar(SolicitudFlujoEfectivo solicitud) throws Throwable;

	/** Balance comparativo 31-dic del anio anterior (o el arranque) vs el mes, con los codigos del B17. */
	ResultadoComparativo comparativo(SolicitudFlujoEfectivo solicitud) throws Throwable;
}
