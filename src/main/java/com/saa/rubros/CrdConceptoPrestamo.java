package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CRD CONCEPTO PRESTAMO (248), columna
 *         {@code CRD.DACC.DACCCPTO}. Los CINCO conceptos del préstamo (no una clasificación
 *         pagado/condonado) — §2 de {@code PLAN-ACUERDOS-PAGO-CONDONACION.md}. Cada uno lleva
 *         adeudado/pagado/condonado; {@link #DESGRAVAMEN} y {@link #SEGURO_INCENDIO} SIEMPRE
 *         con condonado = 0 (K3: los seguros se pagan al 100%, nunca se condonan).
 *
 *         Son 5 y no 6 a propósito: no hay una línea aparte para "interés vencido"
 *         ({@code DetallePrestamo.saldoInteresVencido}/{@code DTPRINVN} no lo alimenta ningún
 *         proceso, vale 0 siempre) — el interés de las cuotas vencidas es una DERIVACIÓN del
 *         interés ordinario, no un concepto propio; exponerlo como sexta línea invitaría a
 *         condonar dos veces lo mismo con otro nombre.
 */
public interface CrdConceptoPrestamo {

	int CAPITAL = 1;
	int INTERES = 2;
	int MORA = 3;
	int DESGRAVAMEN = 4;
	int SEGURO_INCENDIO = 5;

}
