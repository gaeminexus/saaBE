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
	 * Excedente de la carga Petro enviado a un aporte (opción ③ del §3.7 del levantamiento
	 * contable). PDTR 1180, reservado en REGISTRO-RESERVAS-EQUIPOS.md (2026-08-31) — va en el
	 * alterno 8, no en el 7 (reservado para JUBILACION por el script crd/sql/81, sin correr
	 * todavía). DDL: crd/sql/87_EXCEDENTE_PETRO_A_APORTES.sql.
	 */
	int EXCEDENTE_PETRO = 8;

}
