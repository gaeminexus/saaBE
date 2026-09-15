package com.saa.ejb.cxp.seguimiento;

import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.rubros.Estado;

import jakarta.persistence.EntityManager;

/** Clave LIQUIDACION_COMPRA -- PGS.LQCC. */
public class ResolutorLiquidacionCompra implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		LiquidacionCompraCompra l = em.find(LiquidacionCompraCompra.class, idOrigen);
		if (l == null) {
			return null;
		}
		Long estado = l.getEstado();
		String estadoTexto = Long.valueOf(Estado.ACTIVO).equals(estado) ? "Pendiente" : "Anulada";
		String fecha = (l.getFecha() != null) ? l.getFecha().toLocalDate().toString() : null;
		return new DocumentoOrigenPago(l.getNumero(), estado, estadoTexto, fecha, l.getTotal());
	}
}
