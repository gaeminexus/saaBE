package com.saa.ejb.cxp.seguimiento;

import com.saa.model.cxc.AnticipoCliente;
import com.saa.rubros.EstadoAnticipoCliente;

import jakarta.persistence.EntityManager;

/** Clave CXC_DEVOLUCION_CLIENTE -- CBR.ANTC (AnticipoCliente); el pago es la devolución de su
 *  saldo, no el anticipo original. */
public class ResolutorAnticipoCliente implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		AnticipoCliente a = em.find(AnticipoCliente.class, idOrigen);
		if (a == null) {
			return null;
		}
		String estadoTexto;
		int estado = (a.getEstado() != null) ? a.getEstado().intValue() : 0;
		if (estado == EstadoAnticipoCliente.INGRESADO) {
			estadoTexto = "Ingresado";
		} else if (estado == EstadoAnticipoCliente.CONFIRMADO) {
			estadoTexto = "Confirmado";
		} else if (estado == EstadoAnticipoCliente.ANULADO) {
			estadoTexto = "Anulado";
		} else if (estado == EstadoAnticipoCliente.MIGRADO) {
			estadoTexto = "Migrado";
		} else {
			estadoTexto = null;
		}
		String numero = (a.getNumeroDoc() != null && !a.getNumeroDoc().trim().isEmpty())
				? a.getNumeroDoc() : "Anticipo cliente N° " + a.getId();
		String fecha = (a.getFechaAnticipo() != null) ? a.getFechaAnticipo().toString() : null;
		return new DocumentoOrigenPago(numero, a.getEstado(), estadoTexto, fecha, a.getValor());
	}
}
