package com.saa.ejb.tsr.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Chequera;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Chequera.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface ChequeraService extends EntityService<Chequera>{

	/**
	 * Sugiere el número inicial de una nueva chequera: max(finaliza)+1 de las
	 * chequeras no anuladas de la cuenta, o 1 si no tiene ninguna.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Número inicial sugerido
	 * @throws Throwable		: Excepcion
	 */
	Long sugerirNumeroInicial(Long idCuentaBancaria) throws Throwable;

	/**
	 * Registra la recepción de una chequera: valida la cuenta, el rango y que
	 * no se solape con otra chequera de la misma cuenta, graba la chequera en
	 * estado ACTIVA y genera un Cheque ACTIVO por cada número del rango.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria (debe manejar chequera)
	 * @param comienza			: Número inicial del rango (>= 1)
	 * @param finaliza			: Número final del rango (>= comienza)
	 * @param fechaEntrega		: Fecha de entrega de la chequera (null = ahora)
	 * @param idUsuario			: Id del usuario que registra
	 * @return					: Chequera registrada
	 * @throws Throwable		: Excepcion
	 */
	Chequera registrarRecepcion(Long idCuentaBancaria, Long comienza, Long finaliza,
			LocalDateTime fechaEntrega, Long idUsuario) throws Throwable;

	/**
	 * Resumen de una chequera: rango, total de cheques y su distribución por
	 * estado, más el siguiente número disponible.
	 * @param idChequera	: Id de la chequera
	 * @return				: Mapa con comienza, finaliza, total, disponibles,
	 * 						  generados, impresos, entregados, anulados, siguiente
	 * @throws Throwable	: Excepcion
	 */
	Map<String, Object> resumen(Long idChequera) throws Throwable;

	/**
	 * Chequeras de una cuenta bancaria.
	 * @param idCuentaBancaria	: Id de la cuenta bancaria
	 * @return					: Chequeras de la cuenta
	 * @throws Throwable		: Excepcion
	 */
	List<Chequera> selectByCuentaBancaria(Long idCuentaBancaria) throws Throwable;

	/**
	 * Anula una chequera completa: sólo si no tiene cheques en GENERADO,
	 * IMPRESO o ENTREGADO. Los cheques ACTIVO pasan a ANULADO con motivo
	 * CHEQUERA_ANULADA; la chequera pasa a ANULADA.
	 * @param idChequera	: Id de la chequera
	 * @param motivo		: Motivo de la anulación
	 * @param idUsuario		: Id del usuario que anula
	 * @throws Throwable	: Excepcion
	 */
	void anularChequera(Long idChequera, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Pasa la chequera a TERMINADA cuando ya no le queda ningún cheque ACTIVO.
	 * No hace nada si la chequera ya está TERMINADA o ANULADA, o si todavía
	 * tiene cheques disponibles.
	 * @param idChequera	: Id de la chequera
	 * @throws Throwable	: Excepcion
	 */
	void cerrarSiTerminada(Long idChequera) throws Throwable;

}
