package com.saa.rubros;

/**
 * Tipo de concepto de una liquidacion de ex-colaborador (RHH.DLEX.DLEXTPCN). No es un
 * catalogo Rubro/DetalleRubro -- decision D5 de
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md §5.1, mismo criterio que
 * {@link EstadoUsuarioApp}.
 */
public interface RhhConceptoLiquidacionExterna {

	// Ingresos (1 a 9)
	int REMUNERACION_PENDIENTE = 1;
	int DECIMO_TERCER_SUELDO = 2;
	int DECIMO_CUARTO_SUELDO = 3;
	int VACACIONES_NO_GOZADAS = 4;
	int FONDOS_DE_RESERVA = 5;
	int BONIFICACION_DESAHUCIO = 6;
	int INDEMNIZACION_DESPIDO = 7;
	int OTRO_INGRESO_GRAVADO = 8;
	int OTRO_INGRESO_NO_GRAVADO = 9;

	// Descuentos (20 a 23)
	int APORTE_PERSONAL_IESS = 20;
	int RETENCION_IMPUESTO_RENTA = 21;
	int PRESTAMO_O_ANTICIPO = 22;
	int OTRO_DESCUENTO = 23;

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si es un ingreso (1 a 9)
	 */
	static boolean esIngreso(long tipoConcepto) {
		return tipoConcepto >= REMUNERACION_PENDIENTE && tipoConcepto <= OTRO_INGRESO_NO_GRAVADO;
	}

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si es un descuento (20 a 23)
	 */
	static boolean esDescuento(long tipoConcepto) {
		return tipoConcepto >= APORTE_PERSONAL_IESS && tipoConcepto <= OTRO_DESCUENTO;
	}

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si el CHECK de RHH.DLEX lo admite (§5.1: 1-9, 20-23)
	 */
	static boolean esValido(long tipoConcepto) {
		return esIngreso(tipoConcepto) || esDescuento(tipoConcepto);
	}

	/**
	 * Gravado IR segun la regla general (§5.1): los decimos, los fondos de reserva, el
	 * desahucio y la indemnizacion no se gravan. Si un caso se aparta, se registra con el
	 * tipo 8 o el 9, sin tocar codigo -- supuesto pendiente de confirmar con el contador.
	 *
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si el RDEP lo suma a ingresoGravado (tipos 1, 4 y 8; §8)
	 */
	static boolean esGravadoIr(long tipoConcepto) {
		return tipoConcepto == REMUNERACION_PENDIENTE || tipoConcepto == VACACIONES_NO_GOZADAS
				|| tipoConcepto == OTRO_INGRESO_GRAVADO;
	}

}
