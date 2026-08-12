package com.saa.ejb.tsr.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Ingreso;

import jakarta.ejb.Local;

/**
 * Ingresos de tesorería sin respaldo de un documento físico (TSR.INGR):
 * intereses ganados, créditos bancarios, devoluciones, etc.
 *
 * La cuenta contable del ingreso sale del grupo del producto CXC elegido
 * (ProductoCobro.grupoProducto.planCuenta) — no se configura por ingreso.
 *
 * A diferencia de los egresos, el ingreso se registra cuando el dinero YA
 * entró a la cuenta: en un solo paso se graba, se genera el asiento
 * (DEBE banco / HABER cuenta del grupo) y el movimiento bancario.
 */
@Local
public interface IngresoService extends EntityService<Ingreso> {

	/**
	 * Registra un ingreso ya recibido: graba el registro, genera el asiento
	 * contable y el movimiento bancario en la misma transacción.
	 * @param idEmpresa        : Id de la empresa
	 * @param idTitular        : Id del titular que origina el ingreso (opcional)
	 * @param idProductoCobro  : Id del producto CXC que clasifica el ingreso
	 * @param descripcion      : Concepto del ingreso
	 * @param valor            : Valor recibido
	 * @param fecha            : Fecha en que entró el dinero (yyyy-MM-dd, null = hoy)
	 * @param idCuentaBancaria : Id de la cuenta bancaria propia que recibió
	 * @param referencia       : Referencia del crédito bancario (opcional)
	 * @param observacion      : Observaciones (opcional)
	 * @param idUsuario        : Id del usuario que registra
	 * @return                 : Mapa con exito, mensaje, ingreso y asiento
	 * @throws Throwable       : Excepcion
	 */
	Map<String, Object> procesarIngreso(Long idEmpresa, Long idTitular, Long idProductoCobro,
			String descripcion, Double valor, String fecha, Long idCuentaBancaria,
			String referencia, String observacion, Long idUsuario) throws Throwable;

	/**
	 * Anula un ingreso: reversa el asiento, anula el movimiento bancario y
	 * deja el ingreso como Anulado.
	 * @param idIngreso  : Id del ingreso
	 * @param motivo     : Motivo de la anulación
	 * @param idUsuario  : Id del usuario que anula
	 * @return           : Mapa con exito y mensaje
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> anularIngreso(Long idIngreso, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Lista los ingresos de una empresa, opcionalmente por estado.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : 1=Activo 2=Anulado, null para todos
	 * @return           : Listado de ingresos
	 * @throws Throwable : Excepcion
	 */
	List<Ingreso> listar(Long idEmpresa, Long estado) throws Throwable;
}
