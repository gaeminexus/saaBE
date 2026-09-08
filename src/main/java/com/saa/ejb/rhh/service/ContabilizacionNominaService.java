package com.saa.ejb.rhh.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.model.cnt.Asiento;
import com.saa.model.rhh.LineaAsientoNomina;

import jakarta.ejb.Local;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Contabilizacion de la nomina: los cuatro asientos del modulo.</p>
 *
 * <p>El modulo <b>no inventa</b> su mecanismo contable: usa el de CNT. Cada asiento se arma
 * localizando sus lineas en la plantilla por <code>DTPLAXL1</code> —el codigo alterno del
 * detalle del rubro 214 <code>RHH_LINEA_ASIENTO</code>— y se emite con
 * <code>AsientoContableService.generarAsiento</code>, que asigna periodo y numeracion y
 * revierte toda la transaccion si el asiento no cuadra.</p>
 *
 * <h3>Los dos frenos</h3>
 *
 * <p><b>El interruptor del modo.</b> Si <code>PRDN.PRDNMODO = 1</code>
 * (HISTORICO_SIN_CONTABILIZAR) no se genera ningun asiento y no se valida ninguna cuenta, pero
 * el periodo avanza igual por su maquina de estados. Es lo que permite cargar enero a julio de
 * 2026 sin plan de cuentas.</p>
 *
 * <p><b>La cuenta marcadora.</b> <code>CNT.DTPL.PLNNCDGO</code> es <code>NOT NULL</code>, asi
 * que el script 09 creo todas las lineas apuntando a una cuenta marcadora temporal. La
 * condicion de «sin configurar» es <b>apuntar a esa cuenta</b>, no ser nula, y el valor se lee
 * de <code>CFNM.CFNMCTMR</code> — nunca escrito en Java. Sin este control el sistema emitiria
 * asientos cuadrados con todas las lineas contra la misma cuenta: cuadran, pasan
 * <code>validaDebeHaber</code>, y nadie lo nota hasta conciliar el mayor.</p>
 */
@Local
public interface ContabilizacionNominaService {

    /**
     * Comprueba que todas las lineas de plantilla que el periodo va a usar tengan una cuenta
     * contable real asignada.
     *
     * <p>Se invoca en la <b>aprobacion</b>, no en la contabilizacion, para que el problema
     * salga antes de que el periodo avance. En modo historico devuelve lista vacia sin
     * comprobar nada: es lo que desacopla la carga de enero a julio del plan de cuentas.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina
     * @return					: Un mensaje por cada linea sin configurar; vacio significa que se puede contabilizar
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<String> validarCuentasContables(Long idPeriodoNomina) throws Throwable;

    /**
     * Contabiliza el rol de pagos del periodo y lo deja en estado CONTABILIZADO.
     *
     * <p>DEBE el gasto —sueldos, horas extra, aportes patronales, fondos de reserva y
     * decimos— y HABER las obligaciones: IESS, SRI, descuentos y el neto por pagar.</p>
     *
     * <p>En modo HISTORICO_SIN_CONTABILIZAR no genera asiento, deja <code>PRDNASNT</code> en
     * nulo y devuelve <code>null</code>, pero el periodo avanza igual.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina
     * @param usuario			: Usuario que ejecuta
     * @return					: El asiento generado, o null si el periodo es historico
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarRol(Long idPeriodoNomina, String usuario) throws Throwable;

    /**
     * Contabiliza las provisiones del periodo.
     *
     * <p>Es un asiento <b>distinto</b> del rol y se guarda por separado en
     * <code>PRDNASPR</code>. Agrupa las filas de <code>RHH.PVNM</code> por tipo de provision:
     * gasto al DEBE y provision por pagar al HABER.</p>
     *
     * <p>Un periodo sin provisiones —posible si todos los contratos estan mensualizados y
     * nadie tiene base de vacaciones— devuelve <code>null</code> sin emitir asiento: un
     * asiento vacio no aporta nada y no cuadraria.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina
     * @param usuario			: Usuario que ejecuta
     * @return					: El asiento generado, o null si es historico o no hay provisiones
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarProvisiones(Long idPeriodoNomina, String usuario) throws Throwable;

    /**
     * Contabiliza el pago de una orden y registra la fecha de acreditacion.
     *
     * <p>Dos lineas: sueldos por pagar al DEBE y banco al HABER. La cuenta del banco se
     * resuelve desde <code>CuentaBancaria.planCuenta</code> de la cuenta indicada en la orden,
     * con caida a la linea 51 de la plantilla si la cuenta no la tiene configurada.</p>
     *
     * <p>Se emite con <code>ModuloSistema.TESORERIA</code>, no con RECURSOS_HUMANOS: aqui el
     * dinero sale de tesoreria.</p>
     *
     * @param idOrdenPago			: Id de la orden de pago
     * @param fechaAcreditacion		: Fecha en que el banco acredito
     * @param usuario				: Usuario que ejecuta
     * @return						: El asiento generado, o null si el periodo es historico
     * @throws Throwable			: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarPago(Long idOrdenPago, LocalDate fechaAcreditacion,
            String usuario) throws Throwable;

    /**
     * Contabiliza la liquidacion de haberes de un empleado.
     *
     * <p>Cada rubro del finiquito va a su linea del rubro 214 segun el codigo alterno de su
     * concepto: los decimos y las vacaciones cancelan su provision, el desahucio, la
     * indemnizacion y la jubilacion son gasto, y el neto queda en liquidaciones por pagar.
     * Es lo que <code>TMLQ.CPNMCDGO</code> hace posible.</p>
     *
     * @param idLiquidacion	: Id de la liquidacion
     * @param usuario		: Usuario que ejecuta
     * @return				: El asiento generado
     * @throws Throwable	: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarLiquidacion(Long idLiquidacion, String usuario) throws Throwable;

    /**
     * Devuelve las lineas que tendria el asiento, sin emitirlo.
     *
     * <p>Sirve para que el usuario vea el asiento antes de contabilizar, y para diagnosticar
     * un descuadre sin dejar rastro. <b>Funciona tambien en modo historico</b>: ahi es la
     * unica forma de ver que asiento se emitiria cuando el periodo pase a productivo.</p>
     *
     * @param idPeriodoNomina	: Id del periodo de nomina
     * @param tipoAsiento		: 1 rol, 2 provisiones
     * @return					: Lineas del asiento, con su cuenta y su valor
     * @throws Throwable		: Excepcion
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    List<LineaAsientoNomina> previsualizar(Long idPeriodoNomina, Long tipoAsiento) throws Throwable;

    /**
     * Contabiliza la baja de la provision de un beneficio social acumulado (decimo tercero,
     * decimo cuarto o fondos de reserva), al confirmar el pago de una orden
     * {@code RHH.ODBS} (frente 1, 2026-09-01).
     *
     * <p><b>Sin tope contra {@code RHH.PVNM}: la baja va siempre INTEGRA contra la provision
     * (decision del usuario, 2026-09-08, "A" para las tres bajas de provision del
     * sistema).</b> Hasta el 2026-09-08 se topeaba contra la provision "realmente
     * contabilizada" ({@code ProvisionNominaDaoService.sumaValorByEmpleadosYTipo}, que excluye
     * los periodos historicos) y el excedente iba a gasto — pensado para el caso de una
     * provision que nunca se contabilizo. Pero en una empresa migrada eso descuadra al reves:
     * los periodos previos a la puesta en marcha vinieron por saldos iniciales, el pasivo SI
     * esta en libros, y {@code PVNM} solo ve la porcion generada dentro de este sistema.
     * Medido en produccion 2026-09-08 (script e2-30): el tope del decimo cuarto vio 682,89 de
     * un pasivo real de 5.580,95, y 5.833,48 de un pago de 6.476,20 cayeron a gasto en
     * silencio. Consecuencia aceptada de quitar el tope: si alguien cobra un beneficio que
     * nunca se provisiono (ni aca ni en libros), la cuenta de provision queda en negativo por
     * esa parte — es un ajuste de catalogo/carga que corrige el contador, no algo que este
     * metodo pueda distinguir de una empresa migrada.</p>
     *
     * <p>Dos lineas: DEBE la provision por pagar que corresponda al tipo (40 decimo tercero,
     * 41 decimo cuarto, 43 fondos de reserva — rubro 214, <code>RhhLineaAsiento</code>), por
     * el total completo, resuelta contra <code>ConfiguracionNomina.plantillaProvision</code>
     * —la misma plantilla que ya usa {@link #contabilizarProvisiones} para dar de alta esa
     * misma linea, con el lado DEBE impuesto porque esa plantilla es la del devengo (HABER
     * ahi)—; HABER banco (linea 51), resuelta contra
     * <code>ConfiguracionNomina.plantillaPago</code> —la misma que ya usa
     * {@link #contabilizarPago}. Cuadra trivialmente: una linea DEBE y una HABER, ambas por
     * el mismo total.</p>
     *
     * <p>Se emite con <code>ModuloSistema.TESORERIA</code>, igual que
     * {@link #contabilizarPago}: aqui tambien sale el dinero de tesoreria.</p>
     *
     * <p>No respeta el interruptor de modo historico: a diferencia del rol y las provisiones
     * mensuales, esto lo dispara un evento puntual (confirmar el pago de una orden ya
     * enviada a tesoreria), no el cierre de un periodo historico.</p>
     *
     * @param idEmpresa			: Id de la empresa
     * @param tipoBeneficio		: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
     *							  (1 decimo tercero, 2 decimo cuarto, 3 fondos de reserva)
     * @param idsEmpleados		: Ids de los empleados cuyas liquidaciones paga la orden (solo
     *							  para el log; ya no se usa para topear la baja)
     * @param total				: Total de la orden a dar de baja, integro contra la provision
     * @param fecha				: Fecha del asiento
     * @param descripcion		: Descripcion del asiento
     * @param usuario			: Usuario que ejecuta
     * @return					: El asiento generado
     * @throws Throwable		: IncomeException si el tipo no tiene linea de provision, o si
     *							  falta la configuracion/plantilla/cuenta de la empresa
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    Asiento contabilizarBajaProvisionBeneficioSocial(Long idEmpresa, int tipoBeneficio,
            List<Long> idsEmpleados, Double total, LocalDate fecha, String descripcion, String usuario)
            throws Throwable;

}
