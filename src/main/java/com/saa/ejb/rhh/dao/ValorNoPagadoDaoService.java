package com.saa.ejb.rhh.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.rhh.ValorNoPagado;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 * DaoService ValorNoPagado (RHH.VNPG). Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md.
 */
@Local
public interface ValorNoPagadoDaoService extends EntityDao<ValorNoPagado> {

	/**
	 * El registro "vivo" (estado REGISTRADO o RETENIDO, ver
	 * {@link com.saa.rubros.RhhEstadoValorNoPagado}) de un empleado en un periodo. Replica el
	 * indice unico funcional del DDL ({@code UQ_VNPG_VIVA} sobre estados 1 y 2). Sirve para
	 * tres cosas, distinguidas por el estado que trae de vuelta:
	 * <ul>
	 * <li>Al registrar en el periodo P: si devuelve algo, ya hay uno vivo, no se puede crear otro.</li>
	 * <li>El motor de P: busca VNPG(E,P) — si esta en REGISTRADO o RETENIDO, arma el renglon -X.</li>
	 * <li>La orden de pago de P: busca VNPG(E,P-1) — si esta en RETENIDO, arma el +X. Si esta
	 * en REGISTRADO (nunca se retuvo porque la orden de P no se genero), es la inconsistencia
	 * del §7.2 que el service debe rechazar.</li>
	 * </ul>
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param idPeriodo		: Id del periodo de nomina
	 * @return				: El registro vivo, o null si no hay ninguno
	 * @throws Throwable	: Excepcion
	 */
	ValorNoPagado selectVivoByEmpleadoPeriodo(Long idEmpleado, Long idPeriodo) throws Throwable;

	/**
	 * Todos los registros RETENIDO de un empleado, sin importar el periodo. Para el finiquito
	 * (§7.5): un valor retenido y nunca devuelto porque el empleado salio antes de P+1.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @return				: Listado de registros RETENIDO; vacio si no hay
	 * @throws Throwable	: Excepcion
	 */
	List<ValorNoPagado> selectRetenidosByEmpleado(Long idEmpleado) throws Throwable;

	/**
	 * Listado con filtros de servidor (null = sin filtrar por ese criterio), mas reciente
	 * primero. {@code estados} es repetible; null o vacio trae todos los estados.
	 *
	 * @param idEmpresa		: Id de la empresa, obligatorio
	 * @param idPeriodo		: Id del periodo de nomina; null = todos
	 * @param idEmpleado	: Id del empleado; null = todos
	 * @param estados		: Codigos alternos del detalle del rubro RHH_ESTADO_VALOR_NO_PAGADO; null/vacio = todos
	 * @return				: Filas encontradas
	 * @throws Throwable	: Excepcion
	 */
	List<ValorNoPagado> selectListado(Long idEmpresa, Long idPeriodo, Long idEmpleado,
			List<Long> estados) throws Throwable;

	/**
	 * Cuenta cuantos registros de valor no pagado referencian una orden de pago de nomina,
	 * como retencion o como pago ({@code VNPGORRT}/{@code VNPGORPG}). Sirve para bloquear el
	 * borrado fisico de una {@code OrdenPagoNomina} que dejaria esas FK apuntando a una fila
	 * inexistente. Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §7.4.
	 *
	 * @param idOrdenPago	: Id de la orden de pago
	 * @return				: Cantidad de registros que la referencian
	 * @throws Throwable	: Excepcion
	 */
	long countByOrden(Long idOrdenPago) throws Throwable;

}
