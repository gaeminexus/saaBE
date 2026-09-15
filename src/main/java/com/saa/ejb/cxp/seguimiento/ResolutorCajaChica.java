package com.saa.ejb.cxp.seguimiento;

import com.saa.model.tsr.MovimientoCajaChica;
import com.saa.rubros.EstadoMovimientoCajaChica;

import jakarta.persistence.EntityManager;

/** Clave TSR_CAJA_CHICA -- TSR.MVCH. */
public class ResolutorCajaChica implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		MovimientoCajaChica m = em.find(MovimientoCajaChica.class, idOrigen);
		if (m == null) {
			return null;
		}
		String estadoTexto;
		int estado = (m.getEstado() != null) ? m.getEstado().intValue() : 0;
		if (estado == EstadoMovimientoCajaChica.ACTIVO) {
			estadoTexto = "Activo";
		} else if (estado == EstadoMovimientoCajaChica.ANULADO) {
			estadoTexto = "Anulado";
		} else {
			estadoTexto = null;
		}
		String numero = (m.getNumeroDocumento() != null && !m.getNumeroDocumento().trim().isEmpty())
				? m.getNumeroDocumento() : "Movimiento de caja chica N° " + m.getCodigo();
		String fecha = (m.getFecha() != null) ? m.getFecha().toString() : null;
		return new DocumentoOrigenPago(numero, m.getEstado(), estadoTexto, fecha, m.getValor());
	}
}
