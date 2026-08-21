package com.saa.ejb.rhh.service;

import java.util.List;

import com.saa.model.rhh.ResultadoCalculoNomina;
import com.saa.model.rhh.ResultadoCalculoPeriodo;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Motor de calculo de la nomina. Es la pieza central del modulo.</p>
 *
 * <p>El calculo se dirige por el catalogo <code>RHH.CPNM</code> y por los parametros
 * normativos de <code>RHH.PRNM</code>: agregar un concepto nuevo no requiere tocar este
 * codigo, solo insertar una fila en el catalogo.</p>
 *
 * <p><b>Idempotencia:</b> <code>calcularPeriodo</code> borra y regenera <code>NMNA</code>,
 * <code>RNGL</code> y <code>PVNM</code> del periodo, preservando los renglones con
 * <code>RNGLMNAL = 'S'</code>. <b>Los acumulados <code>ACMN</code> no se tocan aqui</b>: se
 * escriben en <code>cerrarPeriodo</code>, para que recalcular no los duplique. Ese es el
 * origen clasico de los decimos inflados.</p>
 */
@Local
public interface ProcesoNominaService {

	/**
	 * Comprueba las precondiciones del periodo antes de calcularlo: parametria del anio,
	 * catalogo de conceptos, contratos seleccionables y, si el periodo es productivo,
	 * cuentas contables.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @return					: Listado de mensajes; vacio significa que se puede calcular
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	List<String> validarPeriodo(Long idPeriodoNomina) throws Throwable;

	/**
	 * Calcula todo el periodo. Es idempotente: se puede repetir cuantas veces haga falta.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param usuario			: Usuario que ejecuta
	 * @return					: Totales del periodo y errores por empleado
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	ResultadoCalculoPeriodo calcularPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

	/**
	 * Recalcula un solo empleado dentro de un periodo ya calculado.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param idEmpleado		: Id del empleado
	 * @param preservarManuales	: Conservar los renglones editados a mano
	 * @param usuario			: Usuario que ejecuta
	 * @return					: El detalle calculado del empleado
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	ResultadoCalculoNomina recalcularEmpleado(Long idPeriodoNomina, Long idEmpleado,
			boolean preservarManuales, String usuario) throws Throwable;

	/**
	 * Simula el calculo de un contrato en un periodo sin persistir nada. Sirve para
	 * previsualizar el efecto de un cambio antes de aplicarlo.
	 *
	 * @param idContrato		: Id del contrato
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @return					: El detalle calculado, sin guardar
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	ResultadoCalculoNomina simular(Long idContrato, Long idPeriodoNomina) throws Throwable;

	/**
	 * Aprueba el periodo. Es el momento en que se validan las cuentas contables, para
	 * que el problema aparezca antes de contabilizar y no durante.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param usuario			: Usuario que aprueba
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void aprobarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

	/**
	 * Reabre un periodo aprobado y revierte los acumulados que hubiera escrito el cierre.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param motivo			: Motivo de la reapertura, obligatorio
	 * @param usuario			: Usuario que reabre
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void reabrirPeriodo(Long idPeriodoNomina, String motivo, String usuario) throws Throwable;

	/**
	 * Cierra el periodo y escribe los acumulados ACMN. Es el unico punto donde se
	 * escriben, precisamente para que los recalculos no los dupliquen.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param usuario			: Usuario que cierra
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void cerrarPeriodo(Long idPeriodoNomina, String usuario) throws Throwable;

	/**
	 * Excluye a un empleado del periodo, dejando su nomina en estado EXCLUIDA.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param idEmpleado		: Id del empleado
	 * @param motivo			: Motivo de la exclusion
	 * @param usuario			: Usuario que excluye
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void excluirEmpleado(Long idPeriodoNomina, Long idEmpleado, String motivo,
			String usuario) throws Throwable;

}
