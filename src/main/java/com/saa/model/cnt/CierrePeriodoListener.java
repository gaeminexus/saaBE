package com.saa.model.cnt;

import com.saa.basico.util.IncomeException;
import com.saa.rubros.EstadoPeriodos;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

/**
 * C17 — Red de seguridad JPA del cierre contable bloqueante.
 *
 * <p>Se engancha a {@link Asiento} y {@link DetalleAsiento} ({@code @EntityListeners}). Ningún INSERT ni
 * DELETE de cabecera o línea de asiento pasa si el período del asiento está CERRADO, venga de donde venga
 * (servicio central, cargador, motores de módulo, DAO genérico). Los servicios validan antes y con mejor
 * mensaje ({@code EstadoCierreService}); esto es solo la última barrera.</p>
 *
 * <p>No se intercepta UPDATE a propósito: anular un movimiento de un período cerrado deja el asiento
 * original marcado REVERSADO con el enlace al reverso ({@code ASNTASRV}), que es una actualización legítima.
 * Las ediciones de asientos de períodos cerrados las bloquea {@code AsientoServiceImpl.saveSingle}.</p>
 *
 * <p>No hace consultas (regla JPA para callbacks): mira el {@link Periodo} que el asiento ya tiene cargado.
 * Si el período no viene cargado (stub sin estado) no decide: lo decide el servicio.</p>
 */
public class CierrePeriodoListener {

	/** Prefijo del mensaje: mismo código que ya usan los REST de crd para responder 400. */
	public static final String CODIGO = "PERIODO_CERRADO";

	/**
	 * true si el período está cerrado para contabilidad: bandera C17 {@code PRDOCNCR = 1} o el estado legado
	 * {@code EstadoPeriodos.CERRADO} (4).
	 */
	public static boolean contableCerrado(Periodo periodo) {
		if (periodo == null) {
			return false;
		}
		if (periodo.getCerradoContable() != null && periodo.getCerradoContable().longValue() == 1L) {
			return true;
		}
		return periodo.getEstado() != null && periodo.getEstado().longValue() == EstadoPeriodos.CERRADO;
	}

	/** Mensaje uniforme del bloqueo. */
	public static String mensaje(Periodo periodo, String operacion) {
		String mes = periodo.getMes() != null && periodo.getMes() < 10 ? "0" + periodo.getMes() : String.valueOf(periodo.getMes());
		return CODIGO + ": el período contable " + periodo.getAnio() + "-" + mes + " está CERRADO"
				+ (periodo.getUsuarioCierreContable() != null ? " (cerrado por " + periodo.getUsuarioCierreContable()
						+ (periodo.getFechaCierreContable() != null ? " el " + periodo.getFechaCierreContable().toLocalDate() : "") + ")" : "")
				+ "; no admite " + (operacion != null ? operacion : "asientos nuevos ni cambios")
				+ ". Para corregir, reabra el período (POST /prdo/reabrir) o registre el movimiento en un período abierto.";
	}

	@PrePersist
	@PreRemove
	public void verifica(Object entidad) {
		Periodo periodo = null;
		String operacion = null;
		if (entidad instanceof Asiento) {
			periodo = ((Asiento) entidad).getPeriodo();
			operacion = "asientos nuevos ni eliminación de asientos";
		} else if (entidad instanceof DetalleAsiento) {
			Asiento asiento = ((DetalleAsiento) entidad).getAsiento();
			periodo = asiento != null ? asiento.getPeriodo() : null;
			operacion = "líneas de asiento nuevas ni eliminación de líneas";
		}
		if (contableCerrado(periodo)) {
			throw new IncomeException(mensaje(periodo, operacion));
		}
	}
}
