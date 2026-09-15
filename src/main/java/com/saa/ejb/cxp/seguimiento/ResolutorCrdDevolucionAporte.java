package com.saa.ejb.cxp.seguimiento;

import com.saa.model.crd.DevolucionAporte;

import jakarta.persistence.EntityManager;

/**
 * Clave CRD_DEVOLUCION_APORTE -- CRD.DVAP. SÓLO LECTURA vía {@link EntityManager#find}: no
 * importa ni llama ningún archivo de {@code com.saa.ejb.crd} (módulo de otro equipo), sólo el
 * modelo JPA, igual que ya hace {@code PagoProgramadoServiceImpl} en otros puntos de este mismo
 * origen (registrarPagoDeOrigenExterno no conoce CRD, sólo el id de vuelta).
 */
public class ResolutorCrdDevolucionAporte implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		DevolucionAporte d = em.find(DevolucionAporte.class, idOrigen);
		if (d == null) {
			return null;
		}
		// EstadoDevolucionAporte (com.saa.rubros, no com.saa.ejb.crd): 1 REGISTRADA,
		// 2 EN_PAGO, 3 PAGADA, 4 RECHAZADA (histórico), 5 ANULADA.
		String estadoTexto;
		int estado = (d.getEstado() != null) ? d.getEstado().intValue() : 0;
		switch (estado) {
			case 1: estadoTexto = "Registrada"; break;
			case 2: estadoTexto = "En pago"; break;
			case 3: estadoTexto = "Pagada"; break;
			case 4: estadoTexto = "Rechazada"; break;
			case 5: estadoTexto = "Anulada"; break;
			default: estadoTexto = "Desconocido";
		}
		String numero = "Devolución de aportes N° " + d.getCodigo();
		String fecha = (d.getFecha() != null) ? d.getFecha().toString() : null;
		return new DocumentoOrigenPago(numero, d.getEstado(), estadoTexto, fecha, d.getValor());
	}
}
