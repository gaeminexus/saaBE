package com.saa.rubros;

/**
 * Forma en que el proveedor nos devuelve un saldo a favor (PGS.DVPR.DVPRFPAG).
 * Como constantes, no como catálogo (Rubro/DetalleRubro): docs/logica-negocio/cxp/sql/e2-109
 * no carga filas en SCP.PRBR/PDTR para este valor.
 */
public interface FormaDevolucionProveedor {

	/** El proveedor depositó por transferencia. Es el valor por defecto y el de las devoluciones anteriores. */
	public static final int TRANSFERENCIA = 1;

	/** El proveedor entregó un cheque que se deposita. */
	public static final int CHEQUE = 2;

}
