package com.saa.ejb.cxp.seguimiento;

import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoPrestamo;

import jakarta.persistence.EntityManager;

/**
 * Clave CRD_DESEMBOLSO_PRESTAMO -- CRD.PRST. SÓLO LECTURA vía {@link EntityManager#find}, sin
 * importar ningún servicio de {@code com.saa.ejb.crd}.
 * <p>
 * ⚠️ El estado operativo es {@code Prestamo.idEstado} (PRSTIDST) -- {@code Prestamo
 * .estadoPrestamo} es {@code ESPSCDGO}, la FK al catálogo, NO el estado (trampa documentada en
 * CLAUDE.md, "qué columna lleva realmente el estado"). Este resolutor usa {@code getIdEstado()}
 * a propósito; no cambiar a {@code getEstadoPrestamo()}.
 */
public class ResolutorDesembolsoPrestamo implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		Prestamo p = em.find(Prestamo.class, idOrigen);
		if (p == null) {
			return null;
		}
		String estadoTexto;
		int estado = (p.getIdEstado() != null) ? p.getIdEstado().intValue() : -1;
		switch (estado) {
			case EstadoPrestamo.GENERADO: estadoTexto = "Generado"; break;
			case EstadoPrestamo.VIGENTE: estadoTexto = "Vigente"; break;
			case EstadoPrestamo.CANCELADO: estadoTexto = "Cancelado"; break;
			case EstadoPrestamo.CANCELADO_ANTICIPADO: estadoTexto = "Cancelado anticipado"; break;
			case EstadoPrestamo.CANCELADO_POR_NOVACION: estadoTexto = "Cancelado por novación"; break;
			case EstadoPrestamo.PENDIENTE_DE_APROBACION: estadoTexto = "Pendiente de aprobación"; break;
			case EstadoPrestamo.RECHAZADO: estadoTexto = "Rechazado"; break;
			case EstadoPrestamo.DE_PLAZO_VENCIDO: estadoTexto = "De plazo vencido"; break;
			case EstadoPrestamo.CANCELADO_POR_REVISAR: estadoTexto = "Cancelado por revisar"; break;
			case EstadoPrestamo.VIGENTE_POR_REVISAR: estadoTexto = "Vigente por revisar"; break;
			case EstadoPrestamo.EN_MORA: estadoTexto = "En mora"; break;
			default: estadoTexto = null;
		}
		// Sin campo "numero" en Prestamo: se usa el código. "total" = montoSolicitado -- no hay
		// un campo "montoDesembolsado" propio en la entidad; se documenta la elección, no se
		// adivina un desglose de retenciones que no está en este modelo.
		String numero = "Préstamo N° " + p.getCodigo();
		// Prestamo.fecha es LocalDateTime (a diferencia del resto de entidades de este
		// paquete, que usan LocalDate) -- recortar a fecha para cumplir "yyyy-MM-dd" sin hora.
		String fecha = (p.getFecha() != null) ? p.getFecha().toLocalDate().toString() : null;
		return new DocumentoOrigenPago(numero, p.getIdEstado(), estadoTexto, fecha, p.getMontoSolicitado());
	}
}
