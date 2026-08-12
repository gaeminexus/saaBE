package com.saa.ejb.tsr.service;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Egreso;

import jakarta.ejb.Local;

/**
 * Egresos de tesorería sin respaldo de un documento físico (TSR.EGRS):
 * comisiones y débitos por administración de cuentas bancarias, servicios
 * bancarios, etc.
 *
 * La cuenta contable del gasto sale del grupo del producto CXP elegido
 * (ProductoPago.grupoProducto.planCuenta) — no se configura por egreso.
 *
 * El egreso SÍ pasa por el circuito de pagos: al procesarlo se crea su
 * PagoProgramado (PGS.PGTR con FK al egreso), que aparece en el listado de
 * pagos a realizar y sigue lote → archivo → confirmación. Con débito
 * automático el pago nace confirmado y contabiliza en el mismo paso.
 */
@Local
public interface EgresoService extends EntityService<Egreso> {

	/**
	 * Registra un egreso de tesorería y crea su pago en el mismo paso.
	 * Si NO es débito automático el pago queda Registrado y aparece en el
	 * listado de pagos a realizar (requiere beneficiario y su cuenta bancaria
	 * para el archivo del banco). Si es débito automático, el pago nace
	 * Confirmado y aquí mismo se generan el asiento y el movimiento bancario.
	 * @param idEmpresa              : Id de la empresa
	 * @param idTitular              : Id del beneficiario (obligatorio si no es débito automático)
	 * @param idProductoPago         : Id del producto CXP que clasifica el gasto
	 * @param descripcion            : Concepto del egreso
	 * @param valor                  : Valor del egreso
	 * @param fecha                  : Fecha del egreso / del débito (yyyy-MM-dd, null = hoy)
	 * @param idCuentaBancariaOrigen : Id de la cuenta bancaria propia
	 * @param idCuentaDestinoTitular : Id de la cuenta del beneficiario (obligatoria si no es débito automático)
	 * @param debitoAutomatico       : true si el banco ya debitó la cuenta
	 * @param referencia             : Referencia del débito (opcional)
	 * @param observacion            : Observaciones (opcional)
	 * @param idUsuario              : Id del usuario que registra
	 * @return                       : Mapa con exito, mensaje, egreso, pago y (si aplica) asiento
	 * @throws Throwable             : Excepcion
	 */
	Map<String, Object> procesarEgreso(Long idEmpresa, Long idTitular, Long idProductoPago,
			String descripcion, Double valor, String fecha, Long idCuentaBancariaOrigen,
			Long idCuentaDestinoTitular, boolean debitoAutomatico, String referencia,
			String observacion, Long idUsuario) throws Throwable;

	/**
	 * Anula un egreso pendiente de pago. Si tiene un pago Registrado lo anula
	 * también; si el pago está En archivo o Confirmado, la anulación se
	 * bloquea (procesar la respuesta del banco o revertir el pago primero).
	 * @param idEgreso   : Id del egreso
	 * @param motivo     : Motivo de la anulación
	 * @param idUsuario  : Id del usuario que anula
	 * @return           : Mapa con exito y mensaje
	 * @throws Throwable : Excepcion
	 */
	Map<String, Object> anularEgreso(Long idEgreso, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Lista los egresos de una empresa, opcionalmente por estado.
	 * @param idEmpresa  : Id de la empresa
	 * @param estado     : 1=Pendiente 2=Pagado 3=Anulado, null para todos
	 * @return           : Listado de egresos
	 * @throws Throwable : Excepcion
	 */
	List<Egreso> listar(Long idEmpresa, Long estado) throws Throwable;
}
