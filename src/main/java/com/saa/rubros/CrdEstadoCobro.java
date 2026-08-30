package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CRD ESTADO COBRO (246), columna {@code CRD.CBCR.CBCRESTD}.
 *         Tres pasos, mismo comportamiento que la carga Petro:
 *
 * <pre>
 *   REGISTRADO  crédito registró el cobro con su respaldo. Asiento contra la transitoria.
 *   APROBADO    contabilidad revisó y aprobó.
 *   PROCESADO   crédito procesó: se afectaron préstamos/aportes y el asiento pasó a
 *               las cuentas definitivas.
 *   RECHAZADO   contabilidad rechazó con motivo (CBCRMTRC). Vuelve a REGISTRADO cuando
 *               crédito corrige y reenvía — mismo registro, no uno nuevo.
 *   ANULADO     crédito anuló porque el depósito nunca llegó al banco (no es un caso de
 *               "corregir y reenviar": no hubo cobro). Reversa el asiento transitorio si
 *               existía. Válido desde REGISTRADO, APROBADO o RECHAZADO; nunca desde
 *               PROCESADO (ahí el camino es anularOperacion, ya existente).
 * </pre>
 */
public interface CrdEstadoCobro {

	int REGISTRADO = 1;
	int APROBADO = 2;
	int PROCESADO = 3;
	int RECHAZADO = 4;
	int ANULADO = 5;

}
