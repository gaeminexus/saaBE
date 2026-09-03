package com.saa.rubros;

/**
 * Hecho que originó una distribución en bandas (CRD.DSBN.DSBNORGN) — ver
 * PLAN-AUDITORIA-BANDAS.md §2. La carga Petro es UN origen, no la estructura de la tabla:
 * cualquier proceso que clasifica capital/interés/mora por banda puede colgar sus filas de
 * su propio (origen, idOrigen).
 */
public interface DsbnOrigen {

	/** Carga Petro/ARCH. {@code idOrigen} = {@code CRD.CRAR.CRARCDGO}. */
	public static final String CARGA_PETRO = "CARGA_PETRO";

	/** Cobro individual de un préstamo. {@code idOrigen} = {@code CRD.CBCR.CBCRCDGO}. */
	public static final String COBRO_INDIVIDUAL = "COBRO_INDIVIDUAL";

	/** Abono a capital, precancelación u otra operación del motor de pagos sobre un evento.
	 * {@code idOrigen} = {@code CRD.EVPR.EVPRCDGO}. */
	public static final String EVENTO_PRESTAMO = "EVENTO_PRESTAMO";

	/** Pago mensual de pensión complementaria. {@code idOrigen} = {@code CRD.PGPC.PGPCCDGO}. */
	public static final String PAGO_PENSION = "PAGO_PENSION";

}
