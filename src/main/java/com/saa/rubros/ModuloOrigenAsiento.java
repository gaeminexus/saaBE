package com.saa.rubros;

/**
 * Modulo origen de un asiento automatico (CNT.ASNT.ASNTMDOR), con su propia numeracion mensual por
 * empresa + modulo (ASNTNMMD / ASNTNAMD). Solo asientos con fecha desde
 * {@link TipoAsientoAutomatico#FECHA_VIGENCIA}.
 * Como constantes, no como catalogo: docs/logica-negocio/cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md §2.4.
 */
public interface ModuloOrigenAsiento {

	/** Creditos (crd). */
	public static final String CRE = "CRE";

	/** Cuentas por cobrar. */
	public static final String CXC = "CXC";

	/** Cuentas por pagar. */
	public static final String CXP = "CXP";

	/** Tesoreria. */
	public static final String TSR = "TSR";

	/** Recursos humanos. */
	public static final String RRH = "RRH";

	/** Contabilidad. */
	public static final String CNT = "CNT";

}
