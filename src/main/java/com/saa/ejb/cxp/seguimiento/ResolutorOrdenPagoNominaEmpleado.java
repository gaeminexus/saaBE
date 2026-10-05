package com.saa.ejb.cxp.seguimiento;

import com.saa.model.rhh.DetalleOrdenPagoNomina;
import com.saa.rubros.RhhEstadoDetalleOrdenPago;

import jakarta.persistence.EntityManager;

/**
 * Clave RHH_NOMINA_EMPLEADO -- RHH.DRPG, idOrigen = DRPGCDGO.
 *
 * <p>Copia de {@link ResolutorOrdenPagoNomina} (RHH_NOMINA, que resuelve la orden
 * consolidada) pero para el pago de UN empleado dentro de una orden -- docs/logica-negocio/
 * rhh/API-PAGO-NOMINA-POR-EMPLEADO.md.</p>
 */
public class ResolutorOrdenPagoNominaEmpleado implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		DetalleOrdenPagoNomina d = em.find(DetalleOrdenPagoNomina.class, idOrigen);
		if (d == null) {
			return null;
		}
		String estadoTexto;
		int estado = (d.getEstado() != null) ? d.getEstado().intValue() : 0;
		switch (estado) {
			case RhhEstadoDetalleOrdenPago.PENDIENTE: estadoTexto = "Pendiente"; break;
			case RhhEstadoDetalleOrdenPago.PAGADO: estadoTexto = "Pagado"; break;
			case RhhEstadoDetalleOrdenPago.RECHAZADO: estadoTexto = "Rechazado"; break;
			default: estadoTexto = null;
		}
		String numeroOrden = (d.getOrdenPagoNomina() != null && d.getOrdenPagoNomina().getNumero() != null)
				? d.getOrdenPagoNomina().getNumero() : null;
		String numero = (numeroOrden != null ? numeroOrden + " - " : "")
				+ (d.getNombreBeneficiario() != null ? d.getNombreBeneficiario() : "Detalle N° " + d.getCodigo());
		String fecha = (d.getFechaRegistro() != null) ? d.getFechaRegistro().toLocalDate().toString() : null;
		return new DocumentoOrigenPago(numero, d.getEstado(), estadoTexto, fecha, d.getValor());
	}
}
