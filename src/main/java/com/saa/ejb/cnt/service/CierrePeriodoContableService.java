package com.saa.ejb.cnt.service;

import com.saa.ejb.cnt.service.dto.EstadoPeriodo;

import jakarta.ejb.Local;

/**
 * C17 — Cierre y reapertura del período contable (CNT.PRDO.PRDOCNCR), con bitácora en CNT.HCPR y las
 * reglas de orden compartidas con el cierre de cartera. docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md §5.
 */
@Local
public interface CierrePeriodoContableService {

	/**
	 * Cierra el período contable (anio, mes) de la empresa. Exige: período existente y abierto, MAYORIZADO,
	 * cartera del mes cerrada (corrida EJECUTADA) y orden (ningún mes posterior cerrado y, si ya hay cierres,
	 * el mes anterior cerrado) salvo {@code -Dsaa.cierres.permitirFueraDeOrden=true}.
	 */
	EstadoPeriodo cerrar(Long idEmpresa, Long anio, Long mes, String usuario, String ip, String motivo) throws Throwable;

	/**
	 * Reabre el período contable. Solo el último cerrado (reapertura en cascada desde el más reciente), salvo
	 * el parámetro de re-proceso. Motivo obligatorio. Deja el período MAYORIZADO y abierto.
	 */
	EstadoPeriodo reabrir(Long idEmpresa, Long anio, Long mes, String usuario, String ip, String motivo) throws Throwable;

	/**
	 * Reglas de orden de un CIERRE (tipo CONTABLE o CARTERA). Lanza {@code CIERRE_FUERA_DE_ORDEN} si no se
	 * cumplen y el parámetro está apagado. Devuelve true si se aceptó fuera de orden por el parámetro.
	 * {@code esArranque}: el primer cierre (arranque de cartera) no exige mes anterior cerrado.
	 */
	boolean validarOrdenCierre(String tipo, Long idEmpresa, Long anio, Long mes, boolean esArranque) throws Throwable;

	/** Reglas de orden de una REAPERTURA: ningún mes posterior cerrado del mismo tipo. */
	boolean validarOrdenReapertura(String tipo, Long idEmpresa, Long anio, Long mes) throws Throwable;

	/** Escribe una fila en la bitácora CNT.HCPR. */
	void registrarBitacora(String tipo, String accion, Long idEmpresa, Long anio, Long mes, String usuario, String ip,
			String motivo, Long idPeriodo, Long idCorrida, boolean fueraDeOrden) throws Throwable;

	/**
	 * Bitácora CNT.HCPR de la empresa, de la más reciente a la más antigua. Solo lectura. {@code tipo} (CONTABLE o
	 * CARTERA) es opcional: nulo o vacío devuelve las dos.
	 */
	java.util.List<com.saa.model.cnt.HistorialCierrePeriodo> listarBitacora(Long idEmpresa, String tipo) throws Throwable;
}
