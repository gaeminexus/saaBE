package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.rubros.RhhEstadoOrdenBeneficio;

import jakarta.persistence.EntityManager;

/** Clave RHH_BENEFICIO_SOCIAL -- RHH.ODBS. */
public class ResolutorOrdenBeneficioSocial implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		OrdenBeneficioSocial o = em.find(OrdenBeneficioSocial.class, idOrigen);
		if (o == null) {
			return null;
		}
		String estadoTexto;
		int estado = (o.getEstado() != null) ? o.getEstado().intValue() : 0;
		switch (estado) {
			case RhhEstadoOrdenBeneficio.GENERADA: estadoTexto = "Generada"; break;
			case RhhEstadoOrdenBeneficio.ENVIADA_A_TESORERIA: estadoTexto = "Enviada a tesorería"; break;
			case RhhEstadoOrdenBeneficio.PAGADA: estadoTexto = "Pagada"; break;
			case RhhEstadoOrdenBeneficio.ANULADA: estadoTexto = "Anulada"; break;
			case RhhEstadoOrdenBeneficio.REVERTIDA: estadoTexto = "Revertida"; break;
			default: estadoTexto = null;
		}
		String numero = (o.getNumero() != null && !o.getNumero().trim().isEmpty())
				? o.getNumero() : "Orden de beneficio social N° " + o.getCodigo();
		String fecha = (o.getFechaEmision() != null) ? o.getFechaEmision().toString() : null;
		return new DocumentoOrigenPago(numero, o.getEstado(), estadoTexto, fecha, o.getTotal());
	}
}
