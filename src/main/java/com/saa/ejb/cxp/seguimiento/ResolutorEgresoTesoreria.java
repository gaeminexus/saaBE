package com.saa.ejb.cxp.seguimiento;

import com.saa.model.tsr.Egreso;
import com.saa.rubros.EstadoEgresoTesoreria;

import jakarta.persistence.EntityManager;

/** Clave EGRESO_TESORERIA -- TSR.EGRS, sin documento físico ni número propio (usa el id). */
public class ResolutorEgresoTesoreria implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		Egreso e = em.find(Egreso.class, idOrigen);
		if (e == null) {
			return null;
		}
		String estadoTexto;
		int estado = (e.getEstado() != null) ? e.getEstado().intValue() : 0;
		if (estado == EstadoEgresoTesoreria.PAGADO) {
			estadoTexto = "Pagado";
		} else if (estado == EstadoEgresoTesoreria.ANULADO) {
			estadoTexto = "Anulado";
		} else {
			estadoTexto = "Pendiente de pago";
		}
		String numero = "Egreso N° " + e.getId()
				+ (e.getDescripcion() != null && !e.getDescripcion().trim().isEmpty()
						? " - " + e.getDescripcion() : "");
		String fecha = (e.getFecha() != null) ? e.getFecha().toString() : null;
		return new DocumentoOrigenPago(numero, e.getEstado(), estadoTexto, fecha, e.getValor());
	}
}
