package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.rubros.RhhEstadoOrdenPago;

import jakarta.persistence.EntityManager;

/** Clave RHH_NOMINA -- RHH.RDPG. */
public class ResolutorOrdenPagoNomina implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		OrdenPagoNomina o = em.find(OrdenPagoNomina.class, idOrigen);
		if (o == null) {
			return null;
		}
		String estadoTexto;
		int estado = (o.getEstado() != null) ? o.getEstado().intValue() : 0;
		switch (estado) {
			case RhhEstadoOrdenPago.GENERADA: estadoTexto = "Generada"; break;
			case RhhEstadoOrdenPago.ENVIADA_AL_BANCO: estadoTexto = "Enviada al banco"; break;
			case RhhEstadoOrdenPago.CONFIRMADA: estadoTexto = "Confirmada"; break;
			case RhhEstadoOrdenPago.RECHAZADA_PARCIAL: estadoTexto = "Rechazada parcial"; break;
			case RhhEstadoOrdenPago.ANULADA: estadoTexto = "Anulada"; break;
			default: estadoTexto = null;
		}
		String numero = (o.getNumero() != null && !o.getNumero().trim().isEmpty())
				? o.getNumero() : "Orden de pago N° " + o.getCodigo();
		String fecha = (o.getFechaEmision() != null) ? o.getFechaEmision().toString() : null;
		return new DocumentoOrigenPago(numero, o.getEstado(), estadoTexto, fecha, o.getTotal());
	}
}
