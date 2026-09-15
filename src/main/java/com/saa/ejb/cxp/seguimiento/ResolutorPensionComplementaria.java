package com.saa.ejb.cxp.seguimiento;

import com.saa.model.crd.PagoPensionComplementaria;
import com.saa.rubros.EstadoPagoPensionComplementaria;

import jakarta.persistence.EntityManager;

/** Clave CRD_PENSION_COMPLEMENTARIA -- CRD.PGPC. SÓLO LECTURA vía {@link EntityManager#find},
 *  sin importar ningún servicio de {@code com.saa.ejb.crd} -- mismo patrón que
 *  {@link ResolutorCrdDevolucionAporte}. */
public class ResolutorPensionComplementaria implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		PagoPensionComplementaria p = em.find(PagoPensionComplementaria.class, idOrigen);
		if (p == null) {
			return null;
		}
		String estadoTexto;
		int estado = (p.getEstado() != null) ? p.getEstado().intValue() : 0;
		switch (estado) {
			case EstadoPagoPensionComplementaria.REGISTRADA: estadoTexto = "Registrada"; break;
			case EstadoPagoPensionComplementaria.EN_PAGO: estadoTexto = "En pago"; break;
			case EstadoPagoPensionComplementaria.PAGADA: estadoTexto = "Pagada"; break;
			case EstadoPagoPensionComplementaria.RECHAZADA: estadoTexto = "Rechazada"; break;
			case EstadoPagoPensionComplementaria.ANULADA: estadoTexto = "Anulada"; break;
			case EstadoPagoPensionComplementaria.SEGURO_GENERADO: estadoTexto = "Seguro generado"; break;
			default: estadoTexto = null;
		}
		String numero = "Pensión complementaria N° " + p.getCodigo()
				+ (p.getMes() != null && p.getAnio() != null ? " (" + p.getMes() + "/" + p.getAnio() + ")" : "");
		String fecha = (p.getFecha() != null) ? p.getFecha().toString() : null;
		return new DocumentoOrigenPago(numero, p.getEstado(), estadoTexto, fecha, p.getValor());
	}
}
