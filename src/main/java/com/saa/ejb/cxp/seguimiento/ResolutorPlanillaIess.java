package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.PlanillaIess;
import com.saa.rubros.EstadoPlanillaIess;

import jakarta.persistence.EntityManager;

/** Clave RHH_PLANILLA_IESS -- RHH.PLIS. */
public class ResolutorPlanillaIess implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		PlanillaIess p = em.find(PlanillaIess.class, idOrigen);
		if (p == null) {
			return null;
		}
		String estadoTexto;
		int estado = (p.getEstado() != null) ? p.getEstado().intValue() : 0;
		switch (estado) {
			case EstadoPlanillaIess.REGISTRADA: estadoTexto = "Registrada"; break;
			case EstadoPlanillaIess.CONCILIADA: estadoTexto = "Conciliada"; break;
			case EstadoPlanillaIess.PAGADA: estadoTexto = "Pagada"; break;
			case EstadoPlanillaIess.ANULADA: estadoTexto = "Anulada"; break;
			default: estadoTexto = null;
		}
		String numero = (p.getNumeroComprobante() != null && !p.getNumeroComprobante().trim().isEmpty())
				? p.getNumeroComprobante() : "Planilla IESS N° " + p.getCodigo();
		String fecha = (p.getFechaEmision() != null) ? p.getFechaEmision().toString() : null;
		return new DocumentoOrigenPago(numero, p.getEstado(), estadoTexto, fecha, p.getValorIess());
	}
}
