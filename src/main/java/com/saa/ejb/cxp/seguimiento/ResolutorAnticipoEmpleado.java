package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.AnticipoEmpleado;
import com.saa.rubros.EstadoAnticipoEmpleado;

import jakarta.persistence.EntityManager;

/** Clave RHH_ANTICIPO_EMPLEADO -- RHH.ANTE. Sin campo de número propio: se usa el código. */
public class ResolutorAnticipoEmpleado implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		AnticipoEmpleado a = em.find(AnticipoEmpleado.class, idOrigen);
		if (a == null) {
			return null;
		}
		String estadoTexto;
		int estado = (a.getEstado() != null) ? a.getEstado().intValue() : 0;
		switch (estado) {
			case EstadoAnticipoEmpleado.SOLICITADO:   estadoTexto = "Solicitado"; break;
			case EstadoAnticipoEmpleado.APROBADO:     estadoTexto = "Aprobado"; break;
			case EstadoAnticipoEmpleado.PAGADO:        estadoTexto = "Pagado"; break;
			case EstadoAnticipoEmpleado.EN_DESCUENTO: estadoTexto = "En descuento"; break;
			case EstadoAnticipoEmpleado.CANCELADO:    estadoTexto = "Cancelado"; break;
			case EstadoAnticipoEmpleado.ANULADO:      estadoTexto = "Anulado"; break;
			default: estadoTexto = null;
		}
		String numero = "Anticipo a empleado N° " + a.getCodigo();
		String fecha = (a.getFecha() != null) ? a.getFecha().toString() : null;
		return new DocumentoOrigenPago(numero, a.getEstado(), estadoTexto, fecha, a.getValor());
	}
}
