package com.saa.ejb.rhh.service;

import com.saa.model.rhh.ResultadoProyeccionIr;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Proyeccion y retencion del impuesto a la renta bajo relacion de dependencia.</p>
 *
 * <p>La tabla progresiva, la fraccion basica, el porcentaje de rebaja y el numero de
 * canastas salen de <code>RHH.TBIR</code>, <code>RHH.PRNM</code> y <code>RHH.TPGP</code>.
 * <b>Ningun valor normativo se escribe en este codigo.</b></p>
 *
 * <p>Se reproyecta en enero, al ingresar un empleado, al cambiar el sueldo y cuando el
 * empleado presenta su anexo de gastos personales. La proyeccion anterior se marca
 * <code>PYIRVGNT = 'N'</code> y se inserta la nueva.</p>
 *
 * <p><b>Los servicios profesionales sin relacion de dependencia no entran aqui</b>
 * (<code>CNTERTFN = 'S'</code>): se les aplica <code>honorario x CNTEPRRF / 100</code> como
 * retencion puntual, y su comprobante es una retencion emitida en CXC, no el RDEP.</p>
 */
@Local
public interface RetencionRentaService {

	/**
	 * Proyecta el impuesto a la renta de un empleado para un anio y deja la proyeccion
	 * vigente. Desmarca la anterior.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param mesDesde		: Mes desde el que rige la proyeccion
	 * @param usuario		: Usuario que registra
	 * @return				: El resultado de la proyeccion
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	ResultadoProyeccionIr proyectar(Long idEmpleado, Integer anio, Integer mesDesde,
			String usuario) throws Throwable;

	/**
	 * Proyecta a todos los empleados con contrato activo de una empresa.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio fiscal
	 * @param usuario		: Usuario que registra
	 * @return				: Numero de empleados proyectados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int proyectarTodos(Long idEmpresa, Integer anio, String usuario) throws Throwable;

	/**
	 * Devuelve la retencion mensual que corresponde descontar. Si no existe proyeccion
	 * vigente, la genera en linea.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param mes			: Mes del periodo
	 * @return				: La retencion mensual, nunca negativa
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	Double obtenerRetencionMensual(Long idEmpleado, Integer anio, Integer mes) throws Throwable;

	/**
	 * Aplica la tabla progresiva del anio a una base imponible.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param baseImponible	: Base imponible anual
	 * @param anio			: Anio fiscal
	 * @return				: El impuesto causado
	 * @throws Throwable	: IncomeException si el anio no tiene tabla cargada
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	Double calcularImpuestoSegunTabla(Long idEmpresa, Double baseImponible, Integer anio) throws Throwable;

	/**
	 * Calcula el tope de gastos personales de un empleado: el ampliado por enfermedad
	 * catastrofica, o el que corresponde a su numero de cargas familiares.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @return				: El tope de gasto deducible
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	Double calcularTopeGastosPersonales(Long idEmpleado, Integer anio) throws Throwable;

	/**
	 * Liquida el anio: reproyecta con los valores reales ya acumulados y sin meses
	 * futuros, para determinar el ajuste de diciembre.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio fiscal
	 * @param usuario		: Usuario que registra
	 * @return				: El resultado de la liquidacion anual
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	ResultadoProyeccionIr liquidarAnio(Long idEmpleado, Integer anio, String usuario) throws Throwable;

}
