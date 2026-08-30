package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CRD ESTADO ACUERDO CONDONACION (247), columna
 *         {@code CRD.ACCN.ACCNESTD}.
 *
 *         ⚠️ RECICLADO el 2026-08-30 al derogarse K4/K10 (ya no hay aprobación de
 *         condonación — la previsualización en pantalla cumple ese papel). Los VALORES
 *         numéricos (1/2/3) y las filas de {@code SCP.PDTR} son las mismas de antes; solo
 *         cambió su significado. Ver §5 de {@code PLAN-ACUERDOS-PAGO-CONDONACION.md}.
 *
 * <pre>
 *   VIGENTE   el acuerdo se confirmó (montos ya decididos) y su cobro en CBCR está
 *             registrado, esperando que se procese. Ningún préstamo/aporte afectado todavía.
 *   APLICADO  el PROCESO del cobro corrió: cierre de cuotas + condonación + préstamo
 *             CANCELADO (K11). Terminal — el reverso es anularOperacion, no un cambio acá.
 *   ANULADO   se anuló el CBCR del acuerdo ANTES de procesarlo (el depósito nunca llegó, o
 *             se corrigió por otra vía). CONSERVA su registro: sigue siendo cierto que
 *             alguien negoció perdonar dinero, aunque no se haya cobrado.
 * </pre>
 */
public interface CrdEstadoAcuerdoCondonacion {

	int VIGENTE = 1;
	int APLICADO = 2;
	int ANULADO = 3;

}
