package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ProvisionNomina;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ProvisionNomina.
 */
@Local
public interface ProvisionNominaDaoService extends EntityDao<ProvisionNomina> {


	/**
	 * Elimina las provisiones de un periodo. El calculo es idempotente: borra y regenera.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Numero de provisiones eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Elimina las provisiones de un empleado en un periodo, para el recalculo individual.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @param idEmpleado	: Id del empleado
	 * @return				: Numero de provisiones eliminadas
	 * @throws Throwable	: Excepcion
	 */
	int eliminaByPeriodoYEmpleado(Long idPeriodo, Long idEmpleado) throws Throwable;

	/**
	 * Recupera las provisiones de un periodo, para armar el asiento.
	 *
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: Listado de provisiones; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ProvisionNomina> selectByPeriodo(Long idPeriodo) throws Throwable;

	/**
	 * Suma el valor acumulado de las provisiones de un empleado para un tipo, SOLO de
	 * periodos en modo PRODUCTIVO_CONTABILIZA. Es el saldo de provision disponible: hoy nada
	 * la consume, asi que la suma completa es el saldo (ver
	 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.1bis).
	 * Pensado para jubilacion patronal y desahucio, que se cargan una vez por periodo desde
	 * el estudio actuarial.
	 *
	 * <p><b>Excluye periodos historicos (2026-09-01, #4bis).</b> {@code generaProvision} no
	 * esta gateado por modo historico y SI escribe filas reales en periodos que despues nunca
	 * generan asiento ({@code contabilizarProvisiones} si corta ahi) — sumarlas convertiria
	 * PVNM en el devengo operativo en vez del saldo contable real.</p>
	 *
	 * @param idEmpleado		: Id del empleado
	 * @param tipoProvision		: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
	 * @return					: Suma de PVNMVLOR de periodos productivos; cero si no hay
	 * @throws Throwable		: Excepcion
	 */
	Double sumaValorByEmpleadoYTipo(Long idEmpleado, Long tipoProvision) throws Throwable;

	/**
	 * Igual que {@link #sumaValorByEmpleadoYTipo}, pero agregada sobre varios empleados a la
	 * vez — una sola consulta en vez de N. Pensada para el frente 1 (decimo acumulado /
	 * fondos de reserva): la orden de beneficio social paga a muchos empleados con un unico
	 * asiento consolidado, y el saldo de provision que lo topea es la suma de todos ellos.
	 * Mismo criterio de exclusion de periodos historicos.
	 *
	 * @param idsEmpleados		: Ids de los empleados; vacio o null devuelve cero sin consultar
	 * @param tipoProvision		: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
	 * @return					: Suma de PVNMVLOR de periodos productivos; cero si no hay
	 * @throws Throwable		: Excepcion
	 */
	Double sumaValorByEmpleadosYTipo(List<Long> idsEmpleados, Long tipoProvision) throws Throwable;
}
