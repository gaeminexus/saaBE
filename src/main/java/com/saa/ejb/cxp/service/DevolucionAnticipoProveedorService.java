package com.saa.ejb.cxp.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.EntityService;
import com.saa.model.cxp.DevolucionAnticipoProveedor;

import jakarta.ejb.Local;

/**
 * Servicio de la devolución del saldo de anticipos de un proveedor: un depósito del proveedor en
 * una cuenta bancaria propia, que baja el saldo de uno o más {@code AnticipoProveedor} —
 * docs/logica-negocio/cxp/API-DEVOLUCION-ANTICIPO-PROVEEDOR.md.
 *
 * Contabilidad: DEBE banco / HABER anticipos del proveedor (espejo del asiento con que nació el
 * anticipo). Un depósito = un asiento = un movimiento bancario por el total, aunque cubra varios
 * anticipos.
 */
@Local
public interface DevolucionAnticipoProveedorService extends EntityService<DevolucionAnticipoProveedor> {

	/**
	 * Busca una devolución por su ID.
	 */
	DevolucionAnticipoProveedor selectById(Long id) throws Throwable;

	/**
	 * Busca devoluciones por criterios dinámicos.
	 */
	List<DevolucionAnticipoProveedor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable;

	/**
	 * Registra la devolución del saldo de uno o más anticipos de un proveedor, con su asiento
	 * (DEBE banco / HABER anticipos) y su movimiento bancario para la conciliación — §4.2.
	 * Validaciones en orden: anticipos no vacío/sin repetidos/valor&gt;0; idEmpresa, idTitular,
	 * idCuentaBancaria y fecha presentes; la cuenta bancaria existe y tiene planCuenta; cada
	 * anticipo existe, es del mismo titular y empresa, está CONFIRMADO y valor &lt;= saldo+0.01;
	 * el proveedor tiene PRCC tipo 2 con saldoInicial &gt;= total.
	 * @param idEmpresa        : Id de la empresa
	 * @param idTitular        : Id del proveedor
	 * @param idCuentaBancaria : Id de la cuenta bancaria propia donde entró el depósito
	 * @param fecha            : Fecha del depósito (yyyy-MM-dd)
	 * @param referencia       : Referencia del depósito, opcional
	 * @param observacion      : Observación libre, opcional
	 * @param idUsuario        : Id del usuario que registra
	 * @param anticipos        : Líneas del depósito: [{idAnticipo, valor}, ...]
	 * @return                 : Mapa con exito, mensaje, devolucion (id) y asiento (numeroAlterno)
	 * @throws Throwable       : IncomeException si no se cumple alguna validación
	 */
	Map<String, Object> registrar(Long idEmpresa, Long idTitular, Long idCuentaBancaria, String fecha,
			String referencia, String observacion, Long idUsuario, List<Map<String, Object>> anticipos)
			throws Throwable;

	/**
	 * Anula una devolución ACTIVA: repone el saldo de cada anticipo y el PRCC del proveedor,
	 * anula el movimiento bancario y el asiento (sin tragarse errores: un fallo aborta toda la
	 * anulación), y deja la devolución ANULADA con motivo y fecha.
	 * @param idDevolucion : Id de la devolución
	 * @param motivo       : Motivo de la anulación (obligatorio)
	 * @param idUsuario    : Id del usuario que anula
	 * @return             : Mapa con exito y mensaje
	 * @throws Throwable   : IncomeException si no existe, ya está anulada o ya está conciliada
	 */
	Map<String, Object> anular(Long idDevolucion, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Devoluciones de un proveedor (o de todos, si {@code idTitular} es null) en una empresa,
	 * más reciente primero, como proyección (no la entidad) con su detalle por anticipo.
	 * @param idEmpresa : Id de la empresa
	 * @param idTitular : Id del proveedor; null = todos
	 * @return          : Mapas con id, fecha, valor, referencia, estado, cuentaBancaria,
	 *                    numeroAsiento, motivoAnulacion y detalle (lista de {idAnticipo,
	 *                    numeroDocAnticipo, valor})
	 * @throws Throwable : Excepcion
	 */
	List<Map<String, Object>> listar(Long idEmpresa, Long idTitular) throws Throwable;
}
