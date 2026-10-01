package com.saa.rubros;

/**
 * Estado de una devolución de anticipo a proveedor (PGS.DVPR.DVPRESTD) — un depósito del
 * proveedor en una cuenta bancaria propia, que baja el saldo de uno o más
 * {@link com.saa.model.cxp.AnticipoProveedor}. Ver
 * docs/logica-negocio/cxp/API-DEVOLUCION-ANTICIPO-PROVEEDOR.md.
 *
 * Como constantes, no como catálogo (Rubro/DetalleRubro): docs/logica-negocio/cxp/sql/e2-78
 * no carga filas en SCP.PRBR/PDTR para este estado.
 */
public interface EstadoDevolucionAnticipoProveedor {

	/** Activa: tiene asiento y movimiento bancario vigentes, bajó el saldo de los anticipos. */
	public static final int ACTIVA = 1;

	/** Anulada: el saldo de los anticipos y el PRCC se repusieron. */
	public static final int ANULADA = 2;

}
