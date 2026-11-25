package com.saa.rubros;

/**
 * CRD.MVIC.MVICTPMV — tipo de movimiento del libro de intereses por cuota.
 * {@code docs/logica-negocio/crd/DISENO-PROVISION-INTERESES-Y-FECHA-AFECTACION.md} §3 y §7bis.
 */
public interface TipoMovimientoInteresCuota {

	/** Cierre de cartera, paso ⑦: lo que no se había provisionado todavía. */
	public static final long PROVISION = 1L;
	/** Un cobro reversa lo provisionado de lo que efectivamente cobró. */
	public static final long REVERSO_POR_COBRO = 2L;
	/** Cobro tardío: exceso de MORA provisionada sobre la mora recalculada a la fecha de pago. */
	public static final long REVERSO_POR_COBRO_TARDIO = 3L;
	/** Se anuló o reversó un cobro que había generado un tipo 2: se re-provisiona. */
	public static final long RE_PROVISION = 4L;
	/** Cierre de cartera, paso ④: devengo de mora por cuota. */
	public static final long DEVENGO_MORA = 5L;
	/** Cobro tardío: exceso del devengo de mora (tipo 5) sobre la mora recalculada. */
	public static final long REVERSO_DEVENGO_POR_COBRO_TARDIO = 6L;
	/** Cierre de cartera: clasificación de capital por banda y tipo de cartera, por cuota (R1/7bis). */
	public static final long CLASIFICACION_CAPITAL = 7L;
	/** Cobro tardío: reclasificación de bandas (capital que el cierre ya había movido). */
	public static final long RECLASIFICACION_POR_COBRO_TARDIO = 8L;
	/** Condonación: reversa la provisión de lo condonado (R2). */
	public static final long REVERSO_POR_CONDONACION = 9L;
	/**
	 * Ajuste del tope de la provisión de mora (D-186, lote 7): reversa lo provisionado de más que el tope del ⑦ no deja
	 * provisionar (la mora provisionada no puede pasar de la mora contabilizada pendiente). Lo graba
	 * {@code AjusteTopeProvisionService} contra el asiento que el cuadre contabilizó; resta del saldo provisionado igual que el 2, el 3 y el 9.
	 * Sin corrida: se anula por asiento, no por {@code anularByCorrida}.
	 */
	public static final long AJUSTE_TOPE_PROVISION = 10L;
	/**
	 * Lote 19 (nivel 1.4): devengo de INTERÉS por cuota (componente 1), con el asiento que lo contabiliza en la 1.4.02. Los tipos 5 y 6 siguen
	 * siendo solo de mora. Requiere {@code sql/406} (CK_MVIC_TIPO hasta 12).
	 */
	public static final long DEVENGO_INTERES = 11L;
	/** Lote 19: reverso del devengo de interés (abono, precancelación, acuerdo), por cuota y con el asiento que acredita la 1.4.02. */
	public static final long REVERSO_DEVENGO_INTERES = 12L;

}
