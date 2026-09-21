package com.saa.rubros;

/**
 * Estados de una recepción de valor de seguro, columna {@code CRD.RVSG.RVSGESTD}.
 * No es un rubro de base de datos: son constantes fijas del ciclo, igual que
 * {@link EstadoDevolucionAporte}.
 *
 * <pre>
 *   REGISTRADO  crédito registró que el dinero llegó. No hay asiento ni saldo todavía.
 *   APROBADO    contabilidad confirmó el dinero: asiento D banco / H pasivo generado y
 *               aporte positivo registrado en la cuenta del partícipe.
 *   RECHAZADO   contabilidad rechazó con motivo, desde REGISTRADO.
 *   ANULADO     se reversó una recepción APROBADA (asiento y aporte).
 * </pre>
 */
public interface CrdEstadoRecepcionSeguro {

	int REGISTRADO = 1;
	int APROBADO = 2;
	int RECHAZADO = 3;
	int ANULADO = 4;

}
