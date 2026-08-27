package com.saa.ejb.tsr.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.CierreCajaChica;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CierreCajaChica: arqueo periódico que compara
 * el saldo según libros con el saldo físico contado, y ajusta la diferencia.</p>
 */
@Local
public interface CierreCajaChicaService extends EntityService<CierreCajaChica> {

	/**
	 * Prepara un cierre en estado BORRADOR: calcula el periodo (desde el día
	 * siguiente al fin del último cierre CERRADO, o desde el primer movimiento
	 * si la caja no tiene ninguno, hasta la fecha indicada), el saldo inicial,
	 * los totales del periodo y el saldo según libros. Rechaza si la caja ya
	 * tiene un cierre en BORRADOR.
	 * @param idCaja    : Id de la caja chica
	 * @param fecha     : Fecha de corte del cierre (fin del periodo)
	 * @param idUsuario : Id del usuario que prepara el cierre
	 * @return          : Mapa con {@code cierre} (el CierreCajaChica en BORRADOR,
	 *                    ya grabado) y {@code movimientos} (los del periodo)
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> prepararCierre(Long idCaja, LocalDate fecha, Long idUsuario) throws Throwable;

	/**
	 * Confirma un cierre en BORRADOR con el saldo físico contado. Si hay
	 * diferencia, genera el movimiento y el asiento de ajuste contra la cuenta
	 * de faltantes/sobrantes que indique el usuario. Marca con el cierre todos
	 * los movimientos activos del periodo (incluido el ajuste, si lo hubo) y
	 * deja el cierre en estado CERRADO.
	 * @param idCierre               : Id del cierre en BORRADOR
	 * @param saldoFisico            : Saldo físico contado
	 * @param observacion            : Observación del arqueo
	 * @param idPlanCuentaDiferencia : Cuenta de faltantes/sobrantes; obligatoria sólo si hay diferencia
	 * @param idUsuario              : Id del usuario que confirma
	 * @return                       : Cierre confirmado (CERRADO)
	 * @throws Throwable             : Excepcion
	 */
	CierreCajaChica confirmarCierre(Long idCierre, Double saldoFisico, String observacion,
			Long idPlanCuentaDiferencia, Long idUsuario) throws Throwable;

	/**
	 * Anula un cierre CERRADO: sólo el último cierre CERRADO de la caja. Desmarca
	 * el cierre de sus movimientos; si hubo ajuste por diferencia, anula su
	 * asiento y el propio movimiento de ajuste. Deja el cierre en ANULADO.
	 * @param idCierre  : Id del cierre a anular
	 * @param motivo    : Motivo de la anulación
	 * @param idUsuario : Id del usuario que anula
	 * @throws Throwable : Excepcion
	 */
	void anularCierre(Long idCierre, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Cierres de una caja, del más reciente al más antiguo.
	 * @param idCaja : Id de la caja chica
	 * @return       : Cierres de la caja
	 * @throws Throwable : Excepcion
	 */
	List<CierreCajaChica> listar(Long idCaja) throws Throwable;

	/**
	 * Movimientos incluidos en un cierre (para el detalle del arqueo).
	 * @param idCierre : Id del cierre
	 * @return         : Movimientos del cierre
	 * @throws Throwable : Excepcion
	 */
	List<com.saa.model.tsr.MovimientoCajaChica> movimientos(Long idCierre) throws Throwable;

}
