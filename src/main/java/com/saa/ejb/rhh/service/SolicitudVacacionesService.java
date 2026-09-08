/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 * Este software es la información confidencial y patentada de   Compuseg Cía. Ltda. ( "Información Confidencial"). 
 * Usted no puede divulgar dicha Información confidencial y se utilizará sólo en  conformidad con los términos del acuerdo de licencia que ha introducido dentro de Compuseg
 */
package com.saa.ejb.rhh.service;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.SolicitudVacaciones;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad SolicitudVacaciones.
 *  Accede a los metodos DAO y procesa los datos para el SolicitudVacaciones.</p>
 */
@Local
public interface SolicitudVacacionesService extends EntityService<SolicitudVacaciones>{

	 /**
	  * Recupera entidad con el id
	  * @param id			: Id de la entidad
	  * @return				: Recupera entidad
	  * @throws Throwable	: Excepcion
	  */
	  SolicitudVacaciones selectById(Long id) throws Throwable;

	/**
	 * Aprueba una solicitud de vacaciones: recalcula los dias del rango, valida el
	 * saldo disponible en el momento (no el que se grabo al solicitar), consume el
	 * saldo FIFO del empleado y crea la novedad del periodo de la fecha de inicio con
	 * el concepto "Vacaciones pagadas". Todo en una sola transaccion.
	 *
	 * @param idSolicitud	: Id de la solicitud
	 * @param idUsuario		: Id del usuario que aprueba
	 * @param observacion	: Observacion, opcional
	 * @return				: La solicitud aprobada
	 * @throws Throwable	: IncomeException si el estado no lo permite, si no hay saldo
	 *						  suficiente, o si el periodo de la fecha de inicio no existe
	 *						  o no esta abierto
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	SolicitudVacaciones aprobar(Long idSolicitud, Long idUsuario, String observacion) throws Throwable;

	/**
	 * Rechaza una solicitud de vacaciones. No toca saldo ni novedad: una solicitud
	 * rechazada nunca llego a consumir nada.
	 *
	 * @param idSolicitud	: Id de la solicitud
	 * @param idUsuario		: Id del usuario que rechaza
	 * @param motivo		: Motivo del rechazo
	 * @return				: La solicitud rechazada
	 * @throws Throwable	: IncomeException si el estado no lo permite
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	SolicitudVacaciones rechazar(Long idSolicitud, Long idUsuario, String motivo) throws Throwable;

	/**
	 * Anula una solicitud de vacaciones. Admite dos origenes (2026-09-08, para que el FE
	 * tenga un solo boton "anular" sin tener que saber en que estado esta la solicitud):
	 *
	 * <ul>
	 * <li><b>Estaba APROBADA:</b> devuelve los dias al saldo y anula la novedad que la
	 * aprobacion habia creado -- el comportamiento original de este metodo.</li>
	 * <li><b>Nunca se aprobo (PENDIENTE/SOLICITADA):</b> no hay saldo consumido ni novedad
	 * que retirar, pasa directo a ANULADA con el motivo. RECHAZADA y ANULADA no admiten
	 * "anular" de nuevo: ya son estados terminales.</li>
	 * </ul>
	 *
	 * <p><b>Nota de precision, pendiente de la tabla de detalle propuesta en
	 * docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md:</b> hoy la devolucion usa
	 * el mismo criterio de {@code AcreditacionVacacionesService.revertirConsumo} --
	 * orden inverso al FIFO de consumo, del anio mas reciente al mas antiguo -- porque
	 * todavia no existe un registro de que anios consumio exactamente esta solicitud.
	 * Si entre la aprobacion y la anulacion se aprobaron otras solicitudes del mismo
	 * empleado, la devolucion puede no caer en los mismos anios de los que se tomo. Con
	 * la tabla de detalle esto se vuelve exacto.</p>
	 *
	 * @param idSolicitud	: Id de la solicitud
	 * @param motivo		: Motivo de la anulacion
	 * @param idUsuario		: Id del usuario que anula
	 * @return				: La solicitud con la aprobacion anulada
	 * @throws Throwable	: IncomeException si el estado no lo permite, o si la novedad
	 *						  ya entro en un rol pagado
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	SolicitudVacaciones anularAprobacion(Long idSolicitud, String motivo, Long idUsuario) throws Throwable;

	// ===== INICIO reparacion consumo sin novedad (equipo omen-saa-2, 2026-09-08) =====
	/**
	 * <b>Endpoint de REPARACION, no parte del ciclo normal.</b> Descuenta el saldo (RHH.SLDV +
	 * RHH.DVAC, mismo FIFO que {@code aprobar}) de una solicitud que quedo APROBADA por el
	 * camino viejo -un <code>PUT /slct</code> que grababa el estado directo, sin pasar por
	 * <code>aprobar</code>, sin consumir saldo y sin {@code NovedadNomina}- sin generar la
	 * novedad: el asiento contable de esos periodos ya se hizo a mano. Ver el javadoc de la
	 * implementacion para el detalle completo del caso (seis solicitudes de agosto 2026,
	 * empresa 1236) y el guard de idempotencia.
	 *
	 * @param idSolicitud	: Id de la solicitud APROBADA a reparar
	 * @param idUsuario		: Usuario que ejecuta la reparacion
	 * @param motivo		: Motivo/referencia de la reparacion, obligatorio
	 * @return				: La solicitud actualizada
	 * @throws Throwable	: IncomeException si la solicitud no existe, no esta APROBADA, ya
	 *						  tiene consumo registrado, o falta el motivo
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	SolicitudVacaciones consumirSaldoSinNovedad(Long idSolicitud, Long idUsuario, String motivo) throws Throwable;
	// ===== FIN reparacion consumo sin novedad =====

}