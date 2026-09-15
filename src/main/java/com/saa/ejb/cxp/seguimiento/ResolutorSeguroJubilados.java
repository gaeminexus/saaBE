package com.saa.ejb.cxp.seguimiento;

import jakarta.persistence.EntityManager;

/**
 * Clave CRD_SEGURO_JUBILADOS. {@code idOrigen} es SINTÉTICO ({@code anio*100+mes}, ver el
 * javadoc de {@code OrigenPagoExterno.CRD_SEGURO_JUBILADOS}), NO una FK a ninguna tabla -- no
 * busca nada, arma el documento directo del propio {@code idOrigen}. Siempre
 * {@code resuelto=true} (no hay "no encontrado" posible: el valor siempre se puede descomponer).
 */
public class ResolutorSeguroJubilados implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		if (idOrigen == null) {
			return null;
		}
		long valor = idOrigen.longValue();
		long anio = valor / 100;
		long mes = valor % 100;
		String numero = "Seguro médico jubilados " + mes + "/" + anio;
		return new DocumentoOrigenPago(numero, null, null, null, null);
	}
}
