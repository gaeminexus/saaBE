package com.saa.rubros;

/**
 * Tipo de concepto de una liquidacion de ex-colaborador (RHH.DLEX.DLEXTPCN). No es un
 * catalogo Rubro/DetalleRubro -- decision D5 de
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md §5.1, mismo criterio que
 * {@link EstadoUsuarioApp}.
 *
 * <p><b>REVISIÓN 2026-10-02 (R1 del contrato): los codigos 2 a 9 cambiaron de significado</b>
 * respecto del diseno original (e3-05). Se pudo porque RHH.LQEX/DLEX estaban vacias al
 * momento del cambio (e3-10). Ingreso = 1 a 11, descuento = 20 a 25 (antes 1-9/20-23).</p>
 */
public interface RhhConceptoLiquidacionExterna {

	// Ingresos (1 a 11)
	int REMUNERACION_PENDIENTE = 1;
	int VACACIONES_NO_GOZADAS = 2;
	int DECIMO_TERCER_SUELDO = 3;
	int DECIMO_CUARTO_SUELDO = 4;
	int FONDOS_DE_RESERVA = 5;
	int BONIFICACION_DESAHUCIO = 6;
	int INDEMNIZACION_DESPIDO = 7;
	int PARTICIPACION_UTILIDADES = 8;
	int COMPENSACION_SALARIO_DIGNO = 9;
	int OTRO_INGRESO_GRAVADO = 10;
	int OTRO_INGRESO_NO_GRAVADO = 11;

	// Descuentos (20 a 25)
	int APORTE_PERSONAL_IESS = 20;
	int RETENCION_IMPUESTO_RENTA = 21;
	int ANTICIPO_QUINCENA = 22;
	int ANTICIPO_REMUNERACION = 23;
	int OTROS_CONCEPTOS_POR_COBRAR = 24;
	int OTROS_INGRESOS_UNIFORMES = 25;

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si es un ingreso (1 a 11)
	 */
	static boolean esIngreso(long tipoConcepto) {
		return tipoConcepto >= REMUNERACION_PENDIENTE && tipoConcepto <= OTRO_INGRESO_NO_GRAVADO;
	}

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si es un descuento (20 a 25)
	 */
	static boolean esDescuento(long tipoConcepto) {
		return tipoConcepto >= APORTE_PERSONAL_IESS && tipoConcepto <= OTROS_INGRESOS_UNIFORMES;
	}

	/**
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si el CHECK de RHH.DLEX lo admite (R1: 1-11, 20-25)
	 */
	static boolean esValido(long tipoConcepto) {
		return esIngreso(tipoConcepto) || esDescuento(tipoConcepto);
	}

	/**
	 * Gravado IR para el RDEP tal como lo generan hoy (R3, caso real verificado con el
	 * contador: remuneracion + vacaciones + decimo tercero + decimo cuarto = 2.316,22; el
	 * desahucio y la indemnizacion son exentos). <b>No es la misma regla que "casillaRdep"</b>:
	 * esto es especificamente a que tipos suma {@code ingresoGravado} en el XML simple de
	 * {@code GeneracionSalidasOficialesServiceImpl.generarRdep}.
	 *
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: true si el RDEP lo suma a ingresoGravado (tipos 1, 2, 3, 4 y 10)
	 */
	static boolean esGravadoIrEnRdep(long tipoConcepto) {
		return tipoConcepto == REMUNERACION_PENDIENTE || tipoConcepto == VACACIONES_NO_GOZADAS
				|| tipoConcepto == DECIMO_TERCER_SUELDO || tipoConcepto == DECIMO_CUARTO_SUELDO
				|| tipoConcepto == OTRO_INGRESO_GRAVADO;
	}

	/**
	 * Nombre para mostrar del concepto (columna "Concepto" de R1), usado en mensajes al
	 * usuario (ej. "Falta la cuenta contable del concepto «...»") y como respaldo del acta
	 * cuando {@code DLEXDSCR} viene vacio.
	 *
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: El nombre, o "Concepto &lt;codigo&gt;" si no se reconoce
	 */
	static String nombre(long tipoConcepto) {
		switch ((int) tipoConcepto) {
			case REMUNERACION_PENDIENTE: return "Remuneración pendiente";
			case VACACIONES_NO_GOZADAS: return "Vacaciones no gozadas";
			case DECIMO_TERCER_SUELDO: return "Décimo tercer sueldo";
			case DECIMO_CUARTO_SUELDO: return "Décimo cuarto sueldo";
			case FONDOS_DE_RESERVA: return "Fondos de reserva";
			case BONIFICACION_DESAHUCIO: return "Bonificación por desahucio";
			case INDEMNIZACION_DESPIDO: return "Indemnización por despido intempestivo";
			case PARTICIPACION_UTILIDADES: return "Participación de utilidades";
			case COMPENSACION_SALARIO_DIGNO: return "Compensación económica salario digno";
			case OTRO_INGRESO_GRAVADO: return "Otro ingreso gravado de IR";
			case OTRO_INGRESO_NO_GRAVADO: return "Otro ingreso no gravado de IR";
			case APORTE_PERSONAL_IESS: return "Aporte personal al IESS";
			case RETENCION_IMPUESTO_RENTA: return "Retención de impuesto a la renta";
			case ANTICIPO_QUINCENA: return "Anticipo de quincena";
			case ANTICIPO_REMUNERACION: return "Anticipo de remuneración";
			case OTROS_CONCEPTOS_POR_COBRAR: return "Otros conceptos por cobrar";
			case OTROS_INGRESOS_UNIFORMES: return "Otros ingresos (uniformes y similares)";
			default: return "Concepto " + tipoConcepto;
		}
	}

	/**
	 * Casilla del RDEP que el contador asignó a cada concepto (columna "Casilla del RDEP"
	 * de R1), registrada para el RDEP completo de más adelante -- hoy
	 * {@code GeneracionSalidasOficialesServiceImpl.generarRdep} no la usa, solo
	 * {@link #esGravadoIrEnRdep}, porque el RDEP actual es un XML propio de tres montos, no
	 * el formato del SRI con todas sus casillas (R3).
	 *
	 * @param tipoConcepto	: Codigo del tipo, DLEXTPCN
	 * @return				: La casilla, o "—" si el contador no indicó ninguna
	 */
	static String casillaRdep(long tipoConcepto) {
		switch ((int) tipoConcepto) {
			case REMUNERACION_PENDIENTE: return "Sueldos y salarios gravados (materia gravada IESS)";
			case VACACIONES_NO_GOZADAS: return "Otros ingresos gravados de IR (no materia IESS)";
			case DECIMO_TERCER_SUELDO: return "Décimo tercer sueldo";
			case DECIMO_CUARTO_SUELDO: return "Décimo cuarto sueldo";
			case FONDOS_DE_RESERVA: return "Fondo de reserva";
			case BONIFICACION_DESAHUCIO: return "Otros ingresos no gravados de IR";
			case INDEMNIZACION_DESPIDO: return "Otros ingresos no gravados de IR";
			case PARTICIPACION_UTILIDADES: return "Participación utilidades";
			case COMPENSACION_SALARIO_DIGNO: return "Compensación salario digno";
			case OTRO_INGRESO_GRAVADO: return "Otros ingresos gravados de IR";
			case OTRO_INGRESO_NO_GRAVADO: return "Otros ingresos no gravados de IR";
			case APORTE_PERSONAL_IESS: return "Aporte personal con este empleador";
			case RETENCION_IMPUESTO_RENTA: return "Impuesto retenido";
			default: return "—";
		}
	}

}
