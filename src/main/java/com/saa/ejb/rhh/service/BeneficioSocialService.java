package com.saa.ejb.rhh.service;

import com.saa.model.rhh.LiquidacionBeneficioSocial;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Liquidacion de los beneficios sociales que no se pagan en el rol mensual: decimo
 * tercero, decimo cuarto y fondos de reserva acumulados.</p>
 *
 * <p>Todo sale de los acumulados <code>RHH.ACMN</code> y de los parametros del anio
 * <code>RHH.PRNM</code>. <b>Ningun valor normativo se escribe en este codigo</b>: ni el
 * SBU, ni los dias base del anio, ni el porcentaje de fondos de reserva.</p>
 *
 * <p>Los tres metodos son <b>idempotentes por el unique
 * <code>UQ_LQBS_BNF (MPLDCDGO, LQBSTPBN, LQBSANOO)</code></b>: si el beneficio del anio ya
 * existe se actualiza en vez de duplicarse, de modo que volver a generar tras corregir un
 * acumulado da el valor correcto sin dejar basura.</p>
 */
@Local
public interface BeneficioSocialService {

	/**
	 * Genera el decimo tercero de los empleados con contrato activo y modalidad
	 * ACUMULADO. Los MENSUALIZADO se omiten: ya lo cobran dentro del rol
	 * (ProcesoNominaServiceImpl, paso 9).
	 *
	 * <p>Periodo de acumulacion: del 1 de diciembre del anio anterior al 30 de noviembre
	 * del anio indicado (Art. 111 del Codigo del Trabajo). El valor es la base acumulada
	 * dividida para doce, menos lo que ya se haya pagado mensualizado.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio del beneficio
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Numero de beneficios generados o actualizados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	com.saa.model.rhh.ResultadoGeneracionBeneficio generarDecimoTercero(Long idEmpresa, Integer anio,
			String usuario) throws Throwable;

	/**
	 * Igual que {@link #generarDecimoTercero(Long, Integer, String)}, con la opcion de calcularlo
	 * <b>por anticipado</b> (DISENO-DECIMO-TERCERO-ANTICIPADO.md, A1): hasta el 30 de noviembre con
	 * los roles de la ventana al menos CALCULADOS. La base de cada empleado es la suma de ACMN de los
	 * meses CERRADOS mas la base de provision (PVNM) de los meses no cerrados, nunca los dos del mismo
	 * mes. Si algun mes de la ventana (noviembre es obligatorio) no esta calculado, no se genera nada
	 * y se lanza IncomeException. Las liquidaciones quedan marcadas anticipado = 'S'.
	 *
	 * <p>En ambos modos (A0): una liquidacion ya PAGADA o con orden no se recalcula; se cuenta en
	 * {@code omitidosPagados}.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio del beneficio (el del 30 de noviembre)
	 * @param anticipado	: true para el calculo anticipado
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Generadas y omitidas por pagadas
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	com.saa.model.rhh.ResultadoGeneracionBeneficio generarDecimoTercero(Long idEmpresa, Integer anio,
			boolean anticipado, String usuario) throws Throwable;

	/**
	 * Decimo tercero REAL de un empleado para el anio, calculado solo con los acumulados cerrados
	 * (Σ ACMN dic anio-1 .. nov anio, entre 12, menos lo ya cobrado mensualizado). <b>No toca
	 * ninguna liquidacion</b>, a diferencia de {@link #calcularDecimoTercero}, que devuelve la
	 * entidad gestionada y la modifica. Lo usa el ajuste del anticipo en el rol de diciembre y en
	 * el finiquito (A2/A3).
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio del beneficio (el del 30 de noviembre)
	 * @return				: El valor, redondeado
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	Double valorDecimoTerceroReal(Long idEmpleado, Integer anio) throws Throwable;

	/**
	 * Genera el decimo cuarto de los empleados de una region, con derecho a decimo
	 * cuarto y modalidad ACUMULADO. Los MENSUALIZADO se omiten: ya lo cobran dentro del
	 * rol (ProcesoNominaServiceImpl, paso 10).
	 *
	 * <p>El periodo depende de la region (Art. 113 del Codigo del Trabajo): Sierra y
	 * Amazonia del 1 de agosto al 31 de julio; Costa e Insular del 1 de marzo al ultimo
	 * dia de febrero. El valor es el SBU prorrateado por dias trabajados sobre los dias
	 * base del anio, con tope de un SBU.</p>
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio del beneficio
	 * @param region		: Codigo alterno del detalle del rubro RHH_REGION_DECIMO_CUARTO
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Numero de beneficios generados o actualizados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	com.saa.model.rhh.ResultadoGeneracionBeneficio generarDecimoCuarto(Long idEmpresa, Integer anio,
			Integer region, String usuario) throws Throwable;

	/**
	 * Genera los fondos de reserva acumulados del anio, solo para los contratos en
	 * modalidad ACUMULADO_EN_EL_IESS y solo por los meses posteriores al primer anio de
	 * servicio.
	 *
	 * @param idEmpresa		: Id de la empresa
	 * @param anio			: Anio del beneficio
	 * @param usuario		: Usuario que ejecuta
	 * @return				: Numero de beneficios generados o actualizados
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	int generarFondosReserva(Long idEmpresa, Integer anio, String usuario) throws Throwable;

	/**
	 * Calcula el decimo tercero de un solo empleado sin persistirlo. Sirve para la
	 * liquidacion de haberes, que necesita el proporcional a la fecha de salida.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio del beneficio
	 * @return				: El beneficio calculado, sin guardar
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	LiquidacionBeneficioSocial calcularDecimoTercero(Long idEmpleado, Integer anio) throws Throwable;

	/**
	 * Calcula el decimo cuarto de un solo empleado sin persistirlo.
	 *
	 * @param idEmpleado	: Id del empleado
	 * @param anio			: Anio del beneficio
	 * @return				: El beneficio calculado, sin guardar
	 * @throws Throwable	: Excepcion
	 */
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	LiquidacionBeneficioSocial calcularDecimoCuarto(Long idEmpleado, Integer anio) throws Throwable;

}
