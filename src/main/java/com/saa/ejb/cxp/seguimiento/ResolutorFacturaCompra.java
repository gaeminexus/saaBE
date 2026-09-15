package com.saa.ejb.cxp.seguimiento;

import com.saa.model.cxp.FacturaCompra;
import com.saa.rubros.Estado;

import jakarta.persistence.EntityManager;

/** Claves FACTURA_COMPRA y NOTA_VENTA (FCTC tipoComprobante='02') -- ambas viven en PGS.FCTC. */
public class ResolutorFacturaCompra implements ResolutorOrigenPago {

	@Override
	public DocumentoOrigenPago resolver(Long idOrigen, EntityManager em) throws Throwable {
		FacturaCompra f = em.find(FacturaCompra.class, idOrigen);
		if (f == null) {
			return null;
		}
		Long estado = f.getEstado();
		String estadoTexto = Long.valueOf(Estado.ACTIVO).equals(estado) ? "Pendiente" : "Anulada";
		String fecha = (f.getFecha() != null) ? f.getFecha().toLocalDate().toString() : null;
		return new DocumentoOrigenPago(f.getNumero(), estado, estadoTexto, fecha, f.getTotal());
	}
}
