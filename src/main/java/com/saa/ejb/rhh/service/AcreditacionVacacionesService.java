package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Acreditacion, consumo y caducidad de los saldos de vacaciones.</p>
 *
 * <p>Se separa de <code>SaldoVacacionesService</code>, que es el CRUD generado de la tabla,
 * para que la logica de negocio no se mezcle con la persistencia basica — el mismo criterio
 * con el que <code>ProcesoNominaService</code> convive con <code>NominaService</code>.</p>
 *
 * <p>Toda la escala sale de <code>RHH.PRNM</code>: los dias base
 * (<code>PRNMDIVC</code>), el anio a partir del cual se suma un dia
 * (<code>PRNMANVC</code>), el tope (<code>PRNMMXVC</code>), los anios de caducidad
 * (<code>PRNMCDVC</code>) y los dias base del anio (<code>PRNMDANO</code>).
 * <b>Ninguno se escribe en este codigo.</b></p>
 */
@Local
public interface AcreditacionVacacionesService {

	/**
	 * Acredita el periodo anual de vacaciones a todos los empleados que cumplieron un
	 * anio de servicio en la fecha de corte o antes.
	 *
	 * <p>Es idempotente: si el empleado ya tiene el saldo de ese anio, se recalculan los
	 * dias asignados sin tocar los ya usados. Al acreditar arrastra el saldo no gozado
	 * del periodo anterior y caduca los que superan el plazo legal.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha a la que se evalua la antiguedad
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Numero de periodos acreditados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int acreditar(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;

	/**
	 * Dias de vacaciones disponibles de un empleado, sumando todos los periodos no
	 * caducados.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @return				: Dias disponibles
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	Double diasDisponibles(Long idEmpleado) throws Throwable;

	/**
	 * Consume dias de vacaciones en orden FIFO, del periodo mas antiguo al mas reciente.
	 * Se invoca al aprobar una solicitud.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param dias			: Dias a consumir
	 * @param usuario		: Usuario que ejecuta
	 * @throws Throwable	: IncomeException si no hay saldo suficiente
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void consumir(Long idEmpleado, Double dias, String usuario) throws Throwable;

	/**
	 * Devuelve dias a los saldos, en orden inverso al consumo. Se invoca al anular una
	 * solicitud ya aprobada.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param dias			: Dias a devolver
	 * @param usuario		: Usuario que ejecuta
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	void revertirConsumo(Long idEmpleado, Double dias, String usuario) throws Throwable;

	/**
	 * Marca como caducados los saldos que superaron el plazo legal de acumulacion.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param fechaCorte	: Fecha a la que se evalua la caducidad
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Listado de avisos con los saldos caducados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	List<String> caducarSaldos(Long idEmpresa, LocalDate fechaCorte, String usuario) throws Throwable;

	/**
	 * Valor del dia de vacaciones de un empleado: el acumulado de la base de vacaciones
	 * de los ultimos doce meses dividido para los dias base del anio. Incluye horas
	 * extra y comisiones, no solo el sueldo nominal (Art. 71 CT).
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param fechaCorte	: Fecha de corte de los doce meses
	 * @return				: Valor del dia
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	Double valorDiaVacaciones(Long idEmpleado, LocalDate fechaCorte) throws Throwable;

}
