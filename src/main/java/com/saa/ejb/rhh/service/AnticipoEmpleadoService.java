package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.EntityService;
import com.saa.model.rhh.AnticipoEmpleado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Ciclo del anticipo de sueldo entregado a un colaborador: SOLICITADO -&gt;
 * APROBADO (dispara el {@code PagoProgramado} de origen externo
 * {@code RHH_ANTICIPO_EMPLEADO}) -&gt; PAGADO (al confirmarse el pago se
 * genera el asiento y el {@code DescuentoRecurrente}) -&gt; EN_DESCUENTO -&gt;
 * CANCELADO. O ANULADO desde SOLICITADO/APROBADO sin pago confirmado. Ver
 * {@code docs/logica-negocio/rhh/ANTICIPOS-TRABAJADORES.md}.</p>
 */
@Local
public interface AnticipoEmpleadoService extends EntityService<AnticipoEmpleado> {

	/**
	 * Solicita un anticipo para un empleado. Valida que el empleado esté
	 * activo, que no tenga ya un anticipo vivo (SOLICITADO, APROBADO, PAGADO
	 * o EN_DESCUENTO), {@code valor > 0} y {@code numeroCuotas >= 1}. Calcula
	 * {@code valorCuota = round(valor / numeroCuotas, 2)}; la última cuota
	 * real (generada al confirmarse el pago, ver
	 * {@code PagoProgramadoServiceImpl.generaCuotasAnticipo}) absorbe el
	 * residuo de redondeo para que la suma cuadre exacto.
	 * @param idEmpleado             : Id del empleado
	 * @param valor                  : Valor total del anticipo
	 * @param numeroCuotas           : Número de cuotas en que se descuenta
	 * @param fechaInicioDescuento   : Mes desde el que empieza a descontarse; null = sin definir aún
	 * @param motivo                 : Motivo de la solicitud
	 * @param observacion            : Observaciones
	 * @param idUsuario              : Id del usuario que solicita
	 * @return                       : El anticipo creado, en estado SOLICITADO
	 * @throws Throwable             : Excepcion
	 */
	AnticipoEmpleado solicitar(Long idEmpleado, Double valor, Integer numeroCuotas,
			LocalDate fechaInicioDescuento, String motivo, String observacion, Long idUsuario)
			throws Throwable;

	/**
	 * Aprueba un anticipo SOLICITADO y registra su pago. {@code idCuentaBancariaOrigen}
	 * y {@code formaPago} son OPCIONALES (decisión 2026-08-30): si se omite la cuenta,
	 * el pago nace POR_APROBAR y tesorería asigna cuenta y forma de pago después desde
	 * la bandeja de aprobación ({@code PagoProgramadoServiceImpl.aprobar}), que rechaza
	 * ahí la Transferencia para este origen. Si se envía la cuenta, {@code formaPago}
	 * debe ser CHEQUE o DÉBITO AUTOMÁTICO — la caja no tiene datos bancarios del
	 * empleado capturados en el sistema, así que no hay forma de armar una transferencia.
	 * @param idAnticipo            : Id del anticipo
	 * @param idCuentaBancariaOrigen : Cuenta bancaria propia de la que sale el dinero; null = queda POR_APROBAR
	 * @param formaPago             : {@code FormaPagoProgramado.CHEQUE} o {@code DEBITO_AUTOMATICO}; ignorado si la cuenta es null
	 * @param debitoAutomatico      : true si es débito automático (consistente con formaPago)
	 * @param referencia            : Referencia del pago
	 * @param idUsuario             : Id del usuario que aprueba
	 * @return                      : Mapa con idAnticipo, idPago, estadoPago, numeroCheque
	 * @throws Throwable            : Excepcion
	 */
	Map<String, Object> aprobar(Long idAnticipo, Long idCuentaBancariaOrigen, Long formaPago,
			boolean debitoAutomatico, String referencia, Long idUsuario) throws Throwable;

	/**
	 * Anula un anticipo. Solo permitido en SOLICITADO, o en APROBADO cuando
	 * el pago todavía no está confirmado (rechaza indicando que hay que
	 * revertir el pago si ya está PAGADO/EN_DESCUENTO).
	 * @param idAnticipo : Id del anticipo
	 * @param motivo     : Motivo de la anulación
	 * @param idUsuario  : Id del usuario que anula
	 * @throws Throwable : Excepcion
	 */
	void anular(Long idAnticipo, String motivo, Long idUsuario) throws Throwable;

	/**
	 * Listado de anticipos con filtros opcionales (null = sin filtrar).
	 * @param idEmpresa  : Empresa del empleado
	 * @param idEmpleado : Empleado
	 * @param estado     : Estado (rubro 234)
	 * @return           : Anticipos que cumplen el filtro
	 * @throws Throwable : Excepcion
	 */
	List<AnticipoEmpleado> listar(Long idEmpresa, Long idEmpleado, Long estado) throws Throwable;

	/**
	 * Anticipo vivo de un empleado (SOLICITADO, APROBADO, PAGADO o
	 * EN_DESCUENTO), si tiene uno.
	 * @param idEmpleado : Id del empleado
	 * @return           : El anticipo vivo, o null si no tiene ninguno
	 * @throws Throwable : Excepcion
	 */
	AnticipoEmpleado consultarPorEmpleado(Long idEmpleado) throws Throwable;

}
