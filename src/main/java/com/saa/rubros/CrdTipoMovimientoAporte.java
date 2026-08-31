package com.saa.rubros;

/**
 * @author GaemiSoft
 *         Interfaz del rubro CrdTipoMovimientoAporte (235), naturaleza del movimiento de
 *         un aporte (CRD.APRT.APRTTPMV).
 */
public interface CrdTipoMovimientoAporte {

	int APORTE_MENSUAL = 1;
	int AJUSTE_MANUAL = 2;
	int DEVOLUCION = 3;
	int PAGO_PRESTAMO = 4;
	int REVERSO = 5;
	int MIGRADO = 6;

	/**
	 * Traslado de saldo al jubilarse un partícipe (movimientos NEGATIVOS en cesantía/jubilación
	 * + POSITIVO en pensión complementaria, tipo de aporte 23) — J3 de
	 * {@code LEVANTAMIENTO-TRES-FRENTES-2026-08-30.md} §4.b. Rubro 235 alterno 7, PDTR 1178
	 * ({@code crd/sql/81_RUBRO_MOVIMIENTO_JUBILACION.sql}, ejecutado y verificado contra la
	 * base 2026-08-31).
	 */
	int JUBILACION = 7;

	/**
	 * Excedente de la carga Petro enviado a un aporte (opción ③ del §3.7 del levantamiento
	 * contable). PDTR 1180, reservado en REGISTRO-RESERVAS-EQUIPOS.md (2026-08-31) — va en el
	 * alterno 8, no en el 7 (reservado para JUBILACION por el script crd/sql/81, sin correr
	 * todavía). DDL: crd/sql/87_EXCEDENTE_PETRO_A_APORTES.sql.
	 */
	int EXCEDENTE_PETRO = 8;

	/**
	 * Descuento mensual de una pensión complementaria (ítem 4 de jubilados, 2026-08-31):
	 * movimiento NEGATIVO en pensión complementaria (23) por cada pago del mes generado por
	 * {@code CRD.PGPC}. Distinto de {@link #JUBILACION} a propósito — el traslado inicial es
	 * único e irrepetible, el pago mensual es recurrente; bajo el mismo tipo el histórico no
	 * permitiría distinguir uno de otro. Reservado en el rubro 235 alterno 9, PDTR 1200
	 * (primer código del rango 1200-1299 del equipo A) — {@code REGISTRO-RESERVAS-EQUIPOS.md}.
	 *
	 * ⚠️ <b>La fila de {@code SCP.PDTR} NO EXISTE todavía</b> (2026-08-31): el árbitro escribe
	 * el script. Misma situación que {@link #JUBILACION} — no bloquea la compilación, sí el
	 * significado hasta que el script corra.
	 */
	int PAGO_PENSION = 9;

}
