package com.saa.ejb.cxp.seguimiento;

import com.saa.model.cxp.AnticipoProveedor;
import com.saa.rubros.EstadoAnticipoProveedor;

import jakarta.persistence.EntityManager;

/** Clave ANTICIPO_PROVEEDOR -- PGS.ANTP. */
public class ResolutorAnticipoProveedor implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		AnticipoProveedor a = em.find(AnticipoProveedor.class, idOrigen);
		if (a == null) {
			return null;
		}
		String estadoTexto;
		int estado = (a.getEstado() != null) ? a.getEstado().intValue() : 0;
		if (estado == EstadoAnticipoProveedor.CONFIRMADO) {
			estadoTexto = "Confirmado";
		} else if (estado == EstadoAnticipoProveedor.ANULADO) {
			estadoTexto = "Anulado";
		} else if (estado == EstadoAnticipoProveedor.MIGRADO) {
			estadoTexto = "Migrado";
		} else {
			estadoTexto = "Ingresado";
		}
		String numero = (a.getNumeroDoc() != null && !a.getNumeroDoc().trim().isEmpty())
				? a.getNumeroDoc() : "Anticipo N° " + a.getId();
		String fecha = (a.getFechaAnticipo() != null) ? a.getFechaAnticipo().toString() : null;
		return new DocumentoOrigenPago(numero, a.getEstado(), estadoTexto, fecha, a.getValor());
	}
}
