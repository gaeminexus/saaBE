package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CRD TIPO OPERACION COBRO (245), columna
 *         {@code CRD.CBCR.CBCRTPOO}. Decide qué método del motor de pago se invoca al
 *         procesar el cobro (paso 3 de la autorización de contabilidad). Los valores
 *         coinciden exactamente con {@code SCP.PDTR.PDTRVLRV} del detalle de este rubro.
 *
 *         El cruce de valores / pago con aportes queda deliberadamente fuera de este
 *         catálogo: no entra plata de afuera, no pasa por autorización de contabilidad.
 */
public interface CrdTipoOperacionCobro {

	String PAGO_CUOTA = "PAGO_CUOTA";
	String PAGO_MULTIPLE = "PAGO_MULTIPLE";
	String ABONO_CAPITAL = "ABONO_CAPITAL";
	String PRECANCELACION = "PRECANCELACION";
	String REGISTRO_APORTE = "REGISTRO_APORTE";

	/**
	 * Cobro de la parte NO condonada de un acuerdo de pago con condonación (Frente K). El
	 * monto ya viene fijo de un acuerdo APROBADO — ver
	 * {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md} §5. La línea de
	 * detalle lleva {@code prestamo} (como los demás tipos, salvo REGISTRO_APORTE) Y ADEMÁS
	 * {@code DetalleCobroCredito.acuerdoCondonacion}, para que el PROCESO sepa qué acuerdo
	 * aplicar.
	 */
	String ACUERDO_CONDONACION = "ACUERDO_CONDONACION";

	/**
	 * Cobro con líneas de detalle MEZCLADAS — algunas de préstamo (cuota/abono/precancelación
	 * de uno o varios préstamos) y otras de aporte — en un solo depósito. Nace del defecto de
	 * producción del 2026-08-30: "un depósito = un cobro = una aprobación = un reverso". NO es
	 * una relajación de {@link #PAGO_MULTIPLE} (ese sigue siendo solo-préstamos); cada línea
	 * se valida con las reglas de su propia clase (préstamo o aporte) y {@code modalidad}
	 * (abono a capital) sigue prohibida acá.
	 */
	String COBRO_MIXTO = "COBRO_MIXTO";

}
