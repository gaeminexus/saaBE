package com.saa.ejb.rhh.service;

import java.util.List;

import com.saa.model.rhh.ProvisionNomina;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Carga de las provisiones que no se calculan: jubilacion patronal y desahucio.</p>
 *
 * <p>Las provisiones mensuales ordinarias —decimo tercero, decimo cuarto, vacaciones,
 * fondos de reserva y aporte patronal— <b>las genera el motor</b> dentro de
 * <code>calcularPeriodo</code>, porque salen de las bases del propio periodo. Estas dos no:
 * dependen de un <b>estudio actuarial externo</b> que calcula un actuario con tablas de
 * mortalidad y rotacion, y que entra al sistema como un dato, no como una formula.</p>
 *
 * <p>Ambas estan condicionadas por la configuracion de la empresa: solo se cargan si
 * <code>CFNMAPJP</code> (jubilacion patronal) o <code>CFNMAPDS</code> (desahucio) valen
 * <code>'S'</code>. En ASOPREP las dos estan en <code>'N'</code>, pero el flujo se
 * construye completo porque el sistema se comercializara a otras companias.</p>
 */
@Local
public interface ProvisionActuarialService {

	/**
	 * Carga la provision actuarial de un empleado para un periodo.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param idEmpleado		: Id del empleado
	 * @param tipoProvision		: JUBILACION_PATRONAL o DESAHUCIO del rubro RHH_TIPO_PROVISION
	 * @param valor				: Valor que determino el estudio actuarial
	 * @param usuario			: Usuario que ejecuta
	 * @return					: La provision creada o actualizada
	 * @throws Throwable		: IncomeException si la empresa no tiene activada esa provision
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	ProvisionNomina cargarProvisionActuarial(Long idPeriodoNomina, Long idEmpleado, int tipoProvision,
			Double valor, String usuario) throws Throwable;

	/**
	 * Carga masiva de un estudio actuarial: una lista de pares empleado y valor para un
	 * mismo tipo de provision y periodo.
	 *
	 * @param idPeriodoNomina	: Id del periodo de nomina
	 * @param tipoProvision		: JUBILACION_PATRONAL o DESAHUCIO del rubro RHH_TIPO_PROVISION
	 * @param idsEmpleado		: Ids de los empleados, en el mismo orden que los valores
	 * @param valores			: Valores del estudio, en el mismo orden que los empleados
	 * @param usuario			: Usuario que ejecuta
	 * @return					: Numero de provisiones cargadas
	 * @throws Throwable		: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int cargarEstudioActuarial(Long idPeriodoNomina, int tipoProvision, List<Long> idsEmpleado,
			List<Double> valores, String usuario) throws Throwable;

}
