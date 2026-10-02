package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.LiquidacionExterna;
import com.saa.rubros.RhhEstadoLiquidacionExterna;

import jakarta.persistence.EntityManager;

/**
 * Clave RHH_LIQ_EXCOLABORADOR -- RHH.LQEX (liquidaciones de ex-colaboradores de la
 * administracion anterior). Copiado del patron de {@link ResolutorPlanillaIess}.
 */
public class ResolutorLiquidacionExterna implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		LiquidacionExterna l = em.find(LiquidacionExterna.class, idOrigen);
		if (l == null) {
			return null;
		}
		String estadoTexto;
		int estado = (l.getEstado() != null) ? l.getEstado().intValue() : 0;
		switch (estado) {
			case RhhEstadoLiquidacionExterna.REGISTRADA: estadoTexto = "Registrada"; break;
			case RhhEstadoLiquidacionExterna.EN_TESORERIA: estadoTexto = "En tesorería"; break;
			case RhhEstadoLiquidacionExterna.PAGADA: estadoTexto = "Pagada"; break;
			case RhhEstadoLiquidacionExterna.ANULADA: estadoTexto = "Anulada"; break;
			default: estadoTexto = null;
		}
		// LQEX no tiene un numero de comprobante propio: se identifica por el nombre del
		// ex-colaborador, igual que lo vería un usuario de tesoreria en la bandeja.
		String numero = ((l.getApellidos() != null ? l.getApellidos() : "") + " "
				+ (l.getNombres() != null ? l.getNombres() : "")).trim();
		if (numero.isEmpty()) {
			numero = "Liquidación ex-colaborador N° " + l.getCodigo();
		}
		String fecha = (l.getFechaRegistro() != null) ? l.getFechaRegistro().toLocalDate().toString() : null;
		return new DocumentoOrigenPago(numero, l.getEstado(), estadoTexto, fecha, l.getNeto());
	}
}
