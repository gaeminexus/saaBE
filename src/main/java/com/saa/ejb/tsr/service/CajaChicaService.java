package com.saa.ejb.tsr.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.CajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CajaChica: fondo fijo con límite, cuenta
 * contable propia y reposición desde el circuito de pagos.</p>
 */
@Local
public interface CajaChicaService extends EntityService<CajaChica> {

	/**
	 * Saldo y estado de alerta de una caja chica.
	 * @param idCaja : Id de la caja chica
	 * @return : Mapa con idCaja, nombre, fondo, saldo, porcentaje, alerta,
	 *           montoSugeridoReposicion, ultimoCierre
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> saldo(Long idCaja) throws Throwable;

	/**
	 * Saldo y alerta de todas las cajas chicas ACTIVAS de una empresa.
	 * @param idEmpresa : Id de la empresa
	 * @return : Lista de mapas (mismo shape que {@link #saldo(Long)})
	 * @throws Throwable : Excepcion
	 */
	List<Map<String, Object>> saldos(Long idEmpresa) throws Throwable;

	/**
	 * Registra una caja chica nueva. Valida nombre único por empresa, fondo
	 * mayor a cero y cuenta contable obligatoria. Si {@code saldoInicialMigrado}
	 * es mayor a cero, crea además el movimiento de APERTURA migrada (sin
	 * asiento: el saldo ya está en la cuenta contable de la cuenta bancaria
	 * que se está retirando).
	 * @param caja                 : Caja chica a registrar (sin código)
	 * @param saldoInicialMigrado  : Saldo a migrar desde una cuenta bancaria legada, o null/0
	 * @param idUsuario            : Id del usuario que registra
	 * @return                     : Caja chica registrada
	 * @throws Throwable           : Excepcion
	 */
	CajaChica registrar(CajaChica caja, Double saldoInicialMigrado, Long idUsuario) throws Throwable;

	/**
	 * Cajas chicas activas de una empresa (sin saldo, para selectores).
	 * @param idEmpresa : Id de la empresa
	 * @return : Cajas chicas activas
	 * @throws Throwable : Excepcion
	 */
	List<CajaChica> activas(Long idEmpresa) throws Throwable;

}
