package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService;
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
import com.saa.ejb.rhh.service.CierreCuotasDescuentoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.ConfiguracionNomina;
import com.saa.model.rhh.DetalleLiquidacion;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.Liquidacion;
import com.saa.model.rhh.LineaAsientoNomina;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.rhh.ValorNoPagado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.RhhEstadoLiquidacion;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhEstadoValorNoPagado;
import com.saa.rubros.RhhLineaAsiento;
import com.saa.rubros.RhhModoPeriodoNomina;
import com.saa.rubros.RhhRolConceptoMotor;
import com.saa.rubros.RhhTipoBeneficioSocial;
import com.saa.rubros.RhhTipoConceptoNomina;
import com.saa.rubros.RhhTipoProvision;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * @author GaemiSoft
 * <p>Implementacion de ContabilizacionNominaService.</p>
 *
 * <h3>Como se arma cada asiento</h3>
 *
 * <p>El proceso acumula los importes en un mapa <code>codigo de linea del rubro 214</code> →
 * <code>valor</code>, y solo entonces resuelve las cuentas. Separar el calculo del armado
 * tiene una consecuencia util: <b>una linea que suma cero no entra en el asiento</b> ni exige
 * cuenta configurada. En la parametria de ASOPREP eso afecta a las lineas 16 y 17 —fondos de
 * reserva y decimos «por pagar»—, que quedan vacias porque con la modalidad MENSUALIZADO esos
 * valores ya viajan dentro del neto de la linea 18; existen en la plantilla para las empresas
 * que los paguen por separado.</p>
 *
 * <h3>El cuadre, comprobado antes de llamar</h3>
 *
 * <p>Por construccion <code>DEBE = ingresos + patronal</code> y
 * <code>HABER = descuentos + patronal + neto</code>, que son iguales porque
 * <code>neto = ingresos − descuentos</code>. Aun asi se comprueba con
 * <code>RedondeoNomina</code> <b>antes</b> de llamar a <code>generarAsiento</code>: sin esa
 * comprobacion el usuario recibiria el mensaje generico de <code>validaDebeHaber</code>, que
 * no dice que linea falta. La diferencia por redondeo menor a <code>CFNMTLCD</code> se ajusta
 * contra la linea de cuadre —el neto en el rol— en vez de rechazar el asiento.</p>
 */
@Stateless
public class ContabilizacionNominaServiceImpl implements ContabilizacionNominaService {

    /** Movimiento DEBE en CNT.DTPL.DTPLMVMN. */
    private static final long MOVIMIENTO_DEBE = 1L;

    /** Tipo de asiento a previsualizar: el rol de pagos. */
    private static final long PREVISUALIZA_ROL = 1L;

    /** Tipo de asiento a previsualizar: las provisiones. */
    private static final long PREVISUALIZA_PROVISIONES = 2L;

    /** Tolerancia de cuadre por defecto cuando CFNMTLCD no esta informada. */
    private static final double TOLERANCIA_POR_DEFECTO = 0.01D;

    @EJB
    private PeriodoNominaDaoService periodoNominaDaoService;

    @EJB
    private NominaDaoService nominaDaoService;

    @EJB
    private ReglonNominaDaoService reglonNominaDaoService;

    @EJB
    private ProvisionNominaDaoService provisionNominaDaoService;

    @EJB
    private ConfiguracionNominaDaoService configuracionNominaDaoService;

    @EJB
    private OrdenPagoNominaDaoService ordenPagoNominaDaoService;

    @EJB
    private LiquidacionDaoService liquidacionDaoService;

    @EJB
    private DetalleLiquidacionDaoService detalleLiquidacionDaoService;

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private PlantillaService plantillaService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private CierreCuotasDescuentoService cierreCuotasDescuentoService;

    // ===== INICIO descontabilizar periodo (equipo omen-saa-2, 2026-09-08) =====
    // AsientoService (cnt), no AsientoContableService: la generacion usa AsientoContableService,
    // pero anular/consultar un asiento existente vive en AsientoService (selectById, anulaAsiento).
    @EJB
    private AsientoService asientoService;
    // ===== FIN descontabilizar periodo =====

    // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
    // Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §7.3.
    @EJB
    private ValorNoPagadoDaoService valorNoPagadoDaoService;
    // ===== FIN enganche valores no pagados =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#validarCuentasContables(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> validarCuentasContables(Long idPeriodoNomina) throws Throwable {
        System.out.println("Ingresa al metodo validarCuentasContables de contabilizacionNomina service, periodo: "
                + idPeriodoNomina);

        List<String> faltantes = new ArrayList<String>();
        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);

        if (esHistorico(periodo)) {
            // Sin comprobar nada, a proposito: un periodo historico no emite asiento, asi
            // que exigirle cuentas contables bloquearia la carga de enero a julio por un
            // requisito que ese periodo nunca va a usar.
            System.out.println("Periodo " + idPeriodoNomina
                    + " en modo HISTORICO: no se validan cuentas contables.");
            return faltantes;
        }

        ConfiguracionNomina configuracion = recuperaConfiguracion(periodo);
        Long marcadora = exigeCuentaMarcadora(configuracion);

        // Solo se validan las lineas que este periodo va a usar de verdad. Exigir cuenta a
        // una linea que suma cero bloquearia la contabilizacion por un rubro que la empresa
        // no aplica.
        Map<Integer, Double> importesRol = importesDelRol(periodo);
        // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
        // Se saca ANTES de revisaLineas(plantillaRol, ...): esa linea nunca vivio ahi, y
        // dejarla adentro reporta un faltante inventado (ver LINEA_ROL_FUERA_DE_PLANTILLA_ROL).
        // Se revisa aparte, contra plantillaProvision -la plantilla real de esa cuenta-, en
        // vez de confiar en que importesDeProvisiones tambien la traiga este periodo (puede
        // no traerla: un periodo sin devengo de vacaciones ese mes, pero con gozadas por
        // saldos de meses anteriores, no tendria con que validarla si no se hace aparte).
        Double totalProvisionVacaciones = extraeLineaFueraDePlantillaRol(importesRol);
        // ===== FIN baja de provision de vacaciones gozadas =====
        revisaLineas(faltantes, configuracion.getPlantillaRol(), marcadora,
                importesRol, "rol de pagos");
        if (totalProvisionVacaciones != null && totalProvisionVacaciones.doubleValue() != 0D) {
            Map<Integer, Double> importesVacaciones = new LinkedHashMap<Integer, Double>();
            importesVacaciones.put(Integer.valueOf(LINEA_ROL_FUERA_DE_PLANTILLA_ROL), totalProvisionVacaciones);
            revisaLineas(faltantes, configuracion.getPlantillaProvision(), marcadora,
                    importesVacaciones, "baja de provision de vacaciones gozadas (rol de pagos)");
        }
        revisaLineas(faltantes, configuracion.getPlantillaProvision(), marcadora,
                importesDeProvisiones(periodo), "provisiones");

        return faltantes;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#contabilizarRol(java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Asiento contabilizarRol(Long idPeriodoNomina, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo contabilizarRol de contabilizacionNomina service, periodo: "
                + idPeriodoNomina);

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        exigeAprobado(periodo);

        if (esHistorico(periodo)) {
            // Rama historica: el periodo avanza sin asiento. PRDNASNT se deja explicitamente
            // en nulo para que quede constancia de que no hubo contabilizacion, y no por un
            // olvido.
            periodo.setAsientoRol(null);
            periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO));
            periodo.setObservaciones("Calculado sin contabilizacion (carga historica).");
            periodoNominaDaoService.save(periodo, periodo.getCodigo());
            System.out.println("Periodo " + idPeriodoNomina + " en modo HISTORICO:"
                    + " no se genera asiento, pasa a CONTABILIZADO.");
            return null;
        }

        ConfiguracionNomina configuracion = recuperaConfiguracion(periodo);
        Map<Integer, Double> importes = importesDelRol(periodo);

        // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
        // Esta linea NO sale de plantillaRol -no tiene por que tener esa cuenta configurada,
        // nunca antes hizo falta ahi- sino de la MISMA plantilla que ya usa
        // contabilizarProvisiones para darla de alta, igual que contabilizarBajaProvisionBeneficioSocial
        // con decimos/FR. Y el lado se IMPONE (DEBE): esa linea, en la plantilla del devengo,
        // va al HABER -correcto ahi-, y aca se esta dando de baja, direccion contraria. Mismo
        // fix que descuadro el asiento del decimo cuarto esta manana si se hereda el lado.
        Double totalProvisionVacaciones = extraeLineaFueraDePlantillaRol(importes);
        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
        if (totalProvisionVacaciones != null && totalProvisionVacaciones.doubleValue() > 0D) {
            Long idPlantillaProvision = resuelvePlantilla(configuracion.getPlantillaProvision(),
                    "rol de pagos", periodo.getEmpresa().getCodigo());
            Long marcadora = exigeCuentaMarcadora(configuracion);
            DetallePlantilla lineaProvisionVacaciones = exigeLinea(idPlantillaProvision,
                    RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR, "rol de pagos");
            exigeCuentaReal(lineaProvisionVacaciones, marcadora, "rol de pagos");
            lineas.add(construyeLinea(lineaProvisionVacaciones, totalProvisionVacaciones, true));
        }
        // ===== FIN baja de provision de vacaciones gozadas =====

        lineas.addAll(armaLineas(configuracion.getPlantillaRol(), importes, configuracion, "rol de pagos"));
        comprobarCuadre(lineas, configuracion, RhhLineaAsiento.SUELDOS_POR_PAGAR,
                configuracion.getPlantillaRol());

        Asiento asiento = asientoContableService.generarAsiento(
                periodo.getEmpresa().getCodigo(),
                TipoAsientos.RECURSOS_HUMANOS,
                fechaContable(periodo),
                "Rol de pagos " + periodo.getMes() + "/" + periodo.getAnio(),
                usuario,
                lineas,
                Long.valueOf(ModuloSistema.RECURSOS_HUMANOS));

        periodo.setAsientoRol(asiento.getCodigo());
        periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO));
        periodoNominaDaoService.save(periodo, periodo.getCodigo());

        System.out.println("Periodo " + idPeriodoNomina + " contabilizado con el asiento "
                + asiento.getCodigo());
        return asiento;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#contabilizarProvisiones(java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Asiento contabilizarProvisiones(Long idPeriodoNomina, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo contabilizarProvisiones de contabilizacionNomina service, periodo: "
                + idPeriodoNomina);

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        exigeAprobado(periodo);

        if (esHistorico(periodo)) {
            System.out.println("Periodo " + idPeriodoNomina
                    + " en modo HISTORICO: no se genera asiento de provisiones.");
            return null;
        }

        Map<Integer, Double> importes = importesDeProvisiones(periodo);
        if (importes.isEmpty()) {
            // Un asiento sin lineas no aporta nada y ademas no cuadraria. Es un caso real:
            // un periodo cuyos contratos estan todos mensualizados y sin base de vacaciones.
            System.out.println("Periodo " + idPeriodoNomina
                    + " no genero provisiones: no hay asiento que emitir.");
            return null;
        }

        ConfiguracionNomina configuracion = recuperaConfiguracion(periodo);
        List<DetalleAsiento> lineas = armaLineas(configuracion.getPlantillaProvision(), importes,
                configuracion, "provisiones");
        // La linea de cuadre de las provisiones es la de vacaciones por pagar: es la unica
        // que existe siempre, porque las vacaciones se provisionan para todos.
        comprobarCuadre(lineas, configuracion, RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR,
                configuracion.getPlantillaProvision());

        Asiento asiento = asientoContableService.generarAsiento(
                periodo.getEmpresa().getCodigo(),
                TipoAsientos.RECURSOS_HUMANOS,
                fechaContable(periodo),
                "Provisiones de nomina " + periodo.getMes() + "/" + periodo.getAnio(),
                usuario,
                lineas,
                Long.valueOf(ModuloSistema.RECURSOS_HUMANOS));

        // Se guarda en su propia columna: rol y provisiones son dos asientos distintos.
        periodo.setAsientoProvisiones(asiento.getCodigo());
        periodoNominaDaoService.save(periodo, periodo.getCodigo());

        System.out.println("Provisiones del periodo " + idPeriodoNomina
                + " contabilizadas con el asiento " + asiento.getCodigo());
        return asiento;
    }

    // ===== INICIO descontabilizar periodo (equipo omen-saa-2, 2026-09-08) =====
    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#descontabilizarPeriodo(java.lang.Long, java.lang.String, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void descontabilizarPeriodo(Long idPeriodoNomina, String motivo, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo descontabilizarPeriodo de contabilizacionNomina service, periodo: "
                + idPeriodoNomina);

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("La descontabilizacion de un periodo exige un motivo.");
        }

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        if (!Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(periodo.getEstado())) {
            throw new IncomeException("Solo se puede descontabilizar un periodo CONTABILIZADO ("
                    + RhhEstadoPeriodoNomina.CONTABILIZADO + "). El periodo " + idPeriodoNomina
                    + " esta en estado " + periodo.getEstado() + ".");
        }

        // Una orden de pago ya generada referencia este periodo (RHH.RDPG.PRDNCDGO). Recalcular
        // por debajo la dejaria huerfana -sin la contabilidad que la origino- asi que el
        // usuario tiene que anular o revertir esa orden primero.
        List<OrdenPagoNomina> ordenes = ordenPagoNominaDaoService.selectByPeriodo(idPeriodoNomina);
        if (ordenes != null && !ordenes.isEmpty()) {
            throw new IncomeException("El periodo " + idPeriodoNomina
                    + " ya tiene " + ordenes.size() + " orden(es) de pago generada(s)."
                    + " Anule o revierta esas ordenes antes de descontabilizar.");
        }

        exigeAsientoAnulable(periodo.getAsientoRol(), "rol de pagos");
        exigeAsientoAnulable(periodo.getAsientoProvisiones(), "provisiones");

        if (periodo.getAsientoRol() != null) {
            asientoService.anulaAsiento(periodo.getAsientoRol(), usuario, motivo);
        }
        if (periodo.getAsientoProvisiones() != null) {
            asientoService.anulaAsiento(periodo.getAsientoProvisiones(), usuario, motivo);
        }

        periodo.setAsientoRol(null);
        periodo.setAsientoProvisiones(null);
        // Vuelve a CALCULADO, el mismo estado al que reabrirPeriodo lleva un periodo CERRADO:
        // el flujo completo (descontabilizar -> aprobar novedades pendientes -> recalcular ->
        // aprobar -> contabilizar) queda consistente con ese precedente.
        periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.CALCULADO));
        periodo.setObservaciones("Descontabilizado por " + usuario + ": " + motivo);
        periodoNominaDaoService.save(periodo, periodo.getCodigo());

        System.out.println("Periodo " + idPeriodoNomina + " descontabilizado, vuelve a CALCULADO.");
    }

    /**
     * Verifica que un asiento se pueda anular de verdad antes de tocar nada. No delega esta
     * verificacion en {@code asientoService.anulaAsiento}: si el periodo contable (CNT) del
     * asiento esta MAYORIZADO, ese metodo no rechaza -reversa en silencio en su lugar-, lo que
     * dejaria dos asientos en los libros en vez de cero. Y si esta CERRADO, {@code AsientoService}
     * no lo valida en absoluto. Las dos cosas se comprueban aqui, antes de anular nada.
     *
     * @param idAsiento		: Codigo del asiento a verificar; si es null no hace nada (el
     *						  periodo puede no tener ese asiento, p.ej. provisiones en un
     *						  periodo sin base de vacaciones)
     * @param etiqueta		: Nombre del asiento para el mensaje de error ("rol de pagos", "provisiones")
     * @throws Throwable	: IncomeException si el asiento esta en un periodo contable
     *						  MAYORIZADO o CERRADO
     */
    private void exigeAsientoAnulable(Long idAsiento, String etiqueta) throws Throwable {
        if (idAsiento == null) {
            return;
        }
        Asiento asiento = asientoService.selectById(idAsiento);
        if (asiento == null || asiento.getPeriodo() == null) {
            return;
        }
        Long estadoPeriodoContable = asiento.getPeriodo().getEstado();
        if (Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(estadoPeriodoContable)
                || Long.valueOf(EstadoPeriodos.CERRADO).equals(estadoPeriodoContable)) {
            boolean mayorizado = Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(estadoPeriodoContable);
            throw new IncomeException("El asiento de " + etiqueta + " (" + idAsiento
                    + ") esta en un periodo contable " + (mayorizado ? "MAYORIZADO" : "CERRADO")
                    + ": no se puede anular. " + (mayorizado
                            ? "Desmayorice el periodo contable primero."
                            : "Reabra el periodo contable primero."));
        }
    }
    // ===== FIN descontabilizar periodo =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#contabilizarPago(java.lang.Long, java.time.LocalDate, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Asiento contabilizarPago(Long idOrdenPago, LocalDate fechaAcreditacion,
            String usuario) throws Throwable {
        System.out.println("Ingresa al metodo contabilizarPago de contabilizacionNomina service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = ordenPagoNominaDaoService.selectById(idOrdenPago,
                NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
        if (orden == null) {
            throw new IncomeException("No existe la orden de pago " + idOrdenPago + ".");
        }
        if (orden.getTotal() == null || orden.getTotal().doubleValue() <= 0D) {
            throw new IncomeException("La orden de pago " + idOrdenPago
                    + " no tiene valor: no hay nada que contabilizar.");
        }

        PeriodoNomina periodo = orden.getPeriodoNomina();
        if (periodo == null) {
            throw new IncomeException("La orden de pago " + idOrdenPago
                    + " no tiene periodo de nomina asociado.");
        }

        LocalDate fecha = fechaAcreditacion != null ? fechaAcreditacion : LocalDate.now();

        if (esHistorico(periodo)) {
            // El pago de un periodo historico se registra igual --la fecha de acreditacion
            // es un hecho-- pero no genera asiento, por el mismo interruptor.
            orden.setFechaAcreditacion(fecha);
            orden.setAsientoPago(null);
            ordenPagoNominaDaoService.save(orden, orden.getCodigo());
            System.out.println("Orden " + idOrdenPago + " de un periodo HISTORICO:"
                    + " se registra la acreditacion sin asiento.");
            return null;
        }

        ConfiguracionNomina configuracion = recuperaConfiguracion(periodo);
        Long idPlantilla = resuelvePlantilla(configuracion.getPlantillaPago(), "pago",
                periodo.getEmpresa().getCodigo());
        Long marcadora = exigeCuentaMarcadora(configuracion);
        Double total = RedondeoNomina.redondea(orden.getTotal());

        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();

        // DEBE: se cancela la obligacion con el empleado.
        DetallePlantilla lineaSueldos = exigeLinea(idPlantilla,
                RhhLineaAsiento.SUELDOS_POR_PAGAR_DEBE, "pago");
        exigeCuentaReal(lineaSueldos, marcadora, "pago");
        lineas.add(construyeLinea(lineaSueldos, total));

        // HABER: sale el dinero del banco. La cuenta se toma de la propia cuenta bancaria de
        // la orden, que es lo correcto cuando la empresa paga desde varios bancos; la linea
        // 51 de la plantilla queda como respaldo para quien siempre paga desde el mismo.
        DetallePlantilla lineaBanco = exigeLinea(idPlantilla, RhhLineaAsiento.BANCO, "pago");
        PlanCuenta cuentaBanco = orden.getCuentaBancaria() != null
                ? orden.getCuentaBancaria().getPlanCuenta() : null;
        if (cuentaBanco == null || esMarcadora(cuentaBanco, marcadora)) {
            exigeCuentaReal(lineaBanco, marcadora, "pago");
            cuentaBanco = lineaBanco.getPlanCuenta();
        }
        DetalleAsiento detalleBanco = construyeLinea(lineaBanco, total);
        detalleBanco.setPlanCuenta(cuentaBanco);
        detalleBanco.setNumeroCuenta(cuentaBanco.getCuentaContable());
        detalleBanco.setNombreCuenta(cuentaBanco.getNombre());
        lineas.add(detalleBanco);

        comprobarCuadre(lineas, configuracion, RhhLineaAsiento.BANCO, configuracion.getPlantillaPago());

        // ModuloSistema.TESORERIA y no RECURSOS_HUMANOS: aqui el dinero sale de tesoreria.
        Asiento asiento = asientoContableService.generarAsiento(
                periodo.getEmpresa().getCodigo(),
                TipoAsientos.RECURSOS_HUMANOS,
                fecha,
                "Pago de nomina " + periodo.getMes() + "/" + periodo.getAnio()
                        + " orden " + (orden.getNumero() != null ? orden.getNumero() : idOrdenPago),
                usuario,
                lineas,
                Long.valueOf(ModuloSistema.TESORERIA));

        orden.setFechaAcreditacion(fecha);
        orden.setAsientoPago(asiento.getCodigo());
        ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        periodo.setAsientoPago(asiento.getCodigo());
        periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.PAGADO));
        periodoNominaDaoService.save(periodo, periodo.getCodigo());

        // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
        // §7.3 del plan: esta orden acredita el periodo P; si algun empleado tenia un valor
        // no pagado RETENIDO del periodo anterior (P-1) -recuperado en el neto de P por
        // GeneracionOrdenPagoServiceImpl.ajustaNetoPorValorNoPagado, §7.2-, aqui se cierra:
        // pasa a PAGADO con la orden y el periodo de recuperacion. La contabilidad no cambia,
        // ya salio sola con orden.getTotal() en las lineas de arriba.
        cierraValoresNoPagadosRecuperados(periodo, orden);
        // ===== FIN enganche valores no pagados =====

        // T4: cierre del ciclo de descuentos recurrentes (incluye anticipos a empleados) en el
        // rol. Ver docs/logica-negocio/rhh/ANTICIPOS-TRABAJADORES.md #6. A partir de este punto
        // el pago YA esta contabilizado y el periodo YA quedo PAGADO -irreversible, reabrirPeriodo
        // lo rechaza-, asi que esta llamada no puede impedir que contabilizarPago termine con
        // exito. cierreCuotasDescuentoService es un EJB APARTE que corre en su propia transaccion
        // (REQUIRES_NEW): si algo falla adentro, esa transaccion nueva se pierde ella sola sin
        // tocar la de este metodo, que ya comiteo el asiento y el estado del periodo. No cambiar
        // esto a una llamada interna (this.metodo()): un metodo privado comparte la transaccion
        // de este bean, y un fallo ahi marcaria TODA la transaccion del pago como rollback-only
        // -eso es exactamente lo que este diseno evita. Se envuelve igual en un try/catch como
        // ultima red: la implementacion ya no deberia lanzar nada, pero si el propio arranque de
        // la transaccion nueva fallara, tampoco debe tumbar el pago.
        try {
            cierreCuotasDescuentoService.descuentaCuotasDelPeriodo(periodo.getCodigo(), idOrdenPago, usuario);
        } catch (Throwable e) {
            System.out.println("ATENCION: fallo la llamada al cierre de cuotas de descuentos"
                    + " recurrentes del periodo " + periodo.getCodigo() + " (orden " + idOrdenPago
                    + "), incluso antes de entrar a su transaccion aislada. El pago de nomina ya se"
                    + " contabilizo y sigue en pie; revise a mano el saldo de"
                    + " CuotaDescuento/DescuentoRecurrente/AnticipoEmpleado. Motivo: " + e.getMessage());
        }

        System.out.println("Orden " + idOrdenPago + " contabilizada con el asiento "
                + asiento.getCodigo());
        return asiento;
    }

    // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
    /**
     * Cierra, para cada empleado pagado en esta orden, el valor no pagado RETENIDO del
     * periodo anterior (P-1) que se recupero en el neto de este periodo (P). Ver §7.3 del
     * plan. No hace nada si no hay periodo anterior o si el empleado no tenia ningun
     * registro RETENIDO de ese periodo -el caso normal, sin valores no pagados pendientes.
     *
     * @param periodo	: Periodo que esta orden acredita (P)
     * @param orden		: Orden de pago ya contabilizada
     * @throws Throwable	: Excepcion
     */
    private void cierraValoresNoPagadosRecuperados(PeriodoNomina periodo, OrdenPagoNomina orden) throws Throwable {
        if (periodo.getEmpresa() == null || periodo.getFechaInicio() == null) {
            return;
        }
        PeriodoNomina periodoAnterior = periodoNominaDaoService.selectByFechaEmpresa(
                periodo.getEmpresa().getCodigo(), periodo.getFechaInicio().minusDays(1));
        if (periodoAnterior == null) {
            return;
        }

        List<Nomina> nominas = nominaDaoService.selectByPeriodo(periodo.getCodigo());
        for (Nomina nomina : nominas) {
            if (nomina.getEmpleado() == null) {
                continue;
            }
            ValorNoPagado registro = valorNoPagadoDaoService
                    .selectVivoByEmpleadoPeriodo(nomina.getEmpleado().getCodigo(), periodoAnterior.getCodigo());
            if (registro == null || !Long.valueOf(RhhEstadoValorNoPagado.RETENIDO).equals(registro.getEstado())) {
                continue;
            }
            registro.setEstado(Long.valueOf(RhhEstadoValorNoPagado.PAGADO));
            registro.setOrdenPago(orden);
            registro.setPeriodoRecuperacion(periodo);
            valorNoPagadoDaoService.save(registro, registro.getCodigo());
            System.out.println("Valor no pagado " + registro.getCodigo() + " del empleado "
                    + nomina.getEmpleado().getCodigo() + " recuperado y marcado PAGADO con la orden "
                    + orden.getCodigo() + ".");
        }
    }
    // ===== FIN enganche valores no pagados =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#contabilizarLiquidacion(java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Asiento contabilizarLiquidacion(Long idLiquidacion, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo contabilizarLiquidacion de contabilizacionNomina service,"
                + " liquidacion: " + idLiquidacion);

        Liquidacion liquidacion = liquidacionDaoService.selectById(idLiquidacion,
                NombreEntidadesRhh.LIQUIDACION);
        if (liquidacion == null) {
            throw new IncomeException("No existe la liquidacion " + idLiquidacion + ".");
        }
        if (!Long.valueOf(RhhEstadoLiquidacion.APROBADA).equals(liquidacion.getEstado())) {
            throw new IncomeException("La liquidacion debe estar APROBADA ("
                    + RhhEstadoLiquidacion.APROBADA + ") para contabilizarse. La " + idLiquidacion
                    + " esta en estado " + liquidacion.getEstado() + ".");
        }
        if (liquidacion.getAsiento() != null) {
            throw new IncomeException("La liquidacion " + idLiquidacion + " ya se contabilizo con el"
                    + " asiento " + liquidacion.getAsiento() + ".");
        }

        Empleado empleado = liquidacion.getEmpleado();
        if (empleado == null || empleado.getEmpresa() == null) {
            throw new IncomeException("La liquidacion " + idLiquidacion + " no tiene empresa: sin"
                    + " ella no se puede emitir el asiento.");
        }
        Long idEmpresa = empleado.getEmpresa().getCodigo();
        ConfiguracionNomina configuracion = configuracionNominaDaoService.selectByEmpresa(idEmpresa);
        if (configuracion == null) {
            throw new IncomeException("No existe configuracion de nomina (RHH.CFNM) para la empresa "
                    + idEmpresa + ".");
        }

        List<DetalleLiquidacion> rubros = detalleLiquidacionDaoService
                .selectByLiquidacion(idLiquidacion);
        if (rubros == null || rubros.isEmpty()) {
            throw new IncomeException("La liquidacion " + idLiquidacion + " no tiene rubros: no hay"
                    + " asiento que emitir.");
        }

        // Cada rubro va a su linea del rubro 214 segun el ROL de su concepto. Es la razon por
        // la que TMLQ necesitaba CPNMCDGO: sin el concepto no hay forma de saber si un importe
        // es desahucio o vacaciones, y los dos van a cuentas distintas.
        Map<Integer, Double> importes = new LinkedHashMap<Integer, Double>();
        Double descuentos = Double.valueOf(0D);
        for (DetalleLiquidacion rubro : rubros) {
            Double valor = rubro.getValor();
            if (valor == null || valor.doubleValue() == 0D) {
                continue;
            }
            if (Long.valueOf(RhhTipoConceptoNomina.EGRESO).equals(rubro.getTipoConcepto())) {
                descuentos = RedondeoNomina.suma(descuentos, valor);
                continue;
            }
            // Jubilacion patronal y desahucio se reparten entre la provision acumulada y el
            // gasto, para no reconocer el gasto dos veces (ver #4.1bis del documento de
            // diseno). Los demas rubros siguen resolviendo una unica linea.
            Long rol = rubro.getConceptoNomina() != null
                    ? rubro.getConceptoNomina().getRolMotor() : null;
            if (esRol(rol, RhhRolConceptoMotor.FINIQUITO_JUBILACION_PATRONAL)) {
                descargaProvisionActuarial(importes, empleado.getCodigo(),
                        RhhTipoProvision.JUBILACION_PATRONAL, RhhLineaAsiento.PROVISION_JUBILACION_PATRONAL,
                        RhhLineaAsiento.GASTO_JUBILACION_PATRONAL, valor);
                continue;
            }
            if (esRol(rol, RhhRolConceptoMotor.FINIQUITO_DESAHUCIO)) {
                descargaProvisionActuarial(importes, empleado.getCodigo(),
                        RhhTipoProvision.DESAHUCIO, RhhLineaAsiento.PROVISION_DESAHUCIO,
                        RhhLineaAsiento.GASTO_DESAHUCIO, valor);
                continue;
            }
            suma(importes, lineaDeRubroFiniquito(rubro), valor);
        }

        // HABER: lo que se debe al trabajador y lo que se le cruza.
        if (descuentos.doubleValue() != 0D) {
            suma(importes, RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS, descuentos);
        }
        Double neto = liquidacion.getNeto() != null ? liquidacion.getNeto() : Double.valueOf(0D);
        if (neto.doubleValue() != 0D) {
            suma(importes, RhhLineaAsiento.LIQUIDACIONES_POR_PAGAR, neto);
        }

        List<DetalleAsiento> lineas = armaLineas(configuracion.getPlantillaLiquidacion(),
                sinCeros(importes), configuracion, "liquidacion");
        comprobarCuadre(lineas, configuracion, RhhLineaAsiento.LIQUIDACIONES_POR_PAGAR,
                configuracion.getPlantillaLiquidacion());

        Asiento asiento = asientoContableService.generarAsiento(
                idEmpresa,
                TipoAsientos.RECURSOS_HUMANOS,
                liquidacion.getFechaSalida(),
                "Liquidacion de haberes de " + empleado.getApellidos() + " " + empleado.getNombres(),
                usuario,
                lineas,
                Long.valueOf(ModuloSistema.RECURSOS_HUMANOS));

        liquidacion.setAsiento(asiento.getCodigo());
        liquidacionDaoService.save(liquidacion, liquidacion.getCodigo());

        System.out.println("Liquidacion " + idLiquidacion + " contabilizada con el asiento "
                + asiento.getCodigo());
        return asiento;
    }

    /**
     * Linea del rubro 214 que corresponde a un rubro del finiquito.
     *
     * <p>Se resuelve por <code>CPNMROLM</code>, roles 23 a 30, desde el script 17. El mapeo
     * rol -> linea no cambio al migrar desde el codigo alterno: lo unico que cambio es por que
     * campo se localiza el concepto. Cualquier rubro que el cliente agregue al catalogo sin rol
     * ni linea propia cae en el gasto de sueldos de liquidacion, que es donde contablemente
     * corresponde.</p>
     *
     * @param rubro	: Rubro del finiquito
     * @return		: Codigo de linea del rubro 214
     */
    private int lineaDeRubroFiniquito(DetalleLiquidacion rubro) {
        Long rol = rubro.getConceptoNomina() != null
                ? rubro.getConceptoNomina().getRolMotor() : null;
        if (rol == null) {
            return RhhLineaAsiento.GASTO_SUELDOS_LIQUIDACION;
        }
        int codigo = rol.intValue();
        switch (codigo) {
            case RhhRolConceptoMotor.FINIQUITO_DECIMO_TERCERO:
                return RhhLineaAsiento.PROVISION_DECIMO_TERCERO_POR_PAGAR;
            case RhhRolConceptoMotor.FINIQUITO_DECIMO_CUARTO:
                return RhhLineaAsiento.PROVISION_DECIMO_CUARTO_POR_PAGAR;
            case RhhRolConceptoMotor.FINIQUITO_VACACIONES:
                return RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR;
            case RhhRolConceptoMotor.FINIQUITO_DESAHUCIO:
                // Inalcanzable: el for de contabilizarLiquidacion intercepta este rol antes
                // de llegar aca y llama a descargaProvisionActuarial. Se deja este case como
                // red: si alguien quita esa intercepcion sin darse cuenta, el rol cae aqui en
                // vez de en el default (que lo mandaria a GASTO_SUELDOS_LIQUIDACION, un
                // comportamiento peor y distinto del que hubo siempre).
                return RhhLineaAsiento.GASTO_DESAHUCIO;
            case RhhRolConceptoMotor.FINIQUITO_DESPIDO_INTEMPESTIVO:
                return RhhLineaAsiento.GASTO_DESPIDO_INTEMPESTIVO;
            case RhhRolConceptoMotor.FINIQUITO_JUBILACION_PATRONAL:
                // Inalcanzable por la misma razon que FINIQUITO_DESAHUCIO arriba: lo
                // intercepta el for de contabilizarLiquidacion antes de llamar a este metodo.
                return RhhLineaAsiento.GASTO_JUBILACION_PATRONAL;
            default:
                // Remuneracion pendiente, fondos de reserva pendientes y cualquier rubro nuevo.
                return RhhLineaAsiento.GASTO_SUELDOS_LIQUIDACION;
        }
    }

    /**
     * Reparte el rubro de jubilacion patronal o desahucio del finiquito entre la provision
     * acumulada y el gasto, para no reconocer el gasto dos veces.
     *
     * <p>Descarga la provision (linea 44/45) hasta el saldo acumulado en <code>RHH.PVNM</code>
     * y manda el exceso a gasto (linea 62/60). Con saldo cero degrada exactamente al
     * comportamiento anterior -todo a gasto-, que es lo que permite desplegar esto sin haber
     * medido antes si el estudio actuarial esta cargado (ver
     * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.1bis).</p>
     *
     * <p><b>Esta es LA EXCEPCION a la decision "A" (2026-09-08, sin tope contra RHH.PVNM en
     * {@code contabilizarBajaProvisionBeneficioSocial} ni en
     * {@code acumulaBajaProvisionVacaciones}).</b> Confirmada explicitamente por el usuario
     * despues de que este metodo se paro a consultar antes de tocarla. La diferencia de fondo:
     * decimos y vacaciones se devengan todos los meses via el motor -el pasivo real siempre
     * existe, {@code RHH.PVNM} solo lo subreporta porque excluye periodos historicos (ver
     * {@code ProvisionNominaDaoService#sumaValorByEmpleadoYTipo}), asi que quitarle el tope ahi
     * solo deja de esconder un pasivo que de verdad esta en libros por saldos iniciales.
     * Jubilacion patronal y desahucio son distintos: esa provision <b>solo existe si alguien
     * cargo un estudio actuarial</b>, y puede no haberse cargado nunca en ninguno de los dos
     * sistemas. Quitar el tope aqui no destaparia un pasivo subreportado -fabricaria uno donde
     * el estudio nunca se cargo-, mandando todo el rubro a gasto sin ninguna provision que
     * descargar seria, en ese caso, lo correcto. El tope protege exactamente esa distincion:
     * mientras haya saldo actuarial cargado se descarga contra el, y el resto va a gasto.</p>
     *
     * <p>Se suma sin restar consumos porque hoy nada consume esas provisiones: la suma de
     * <code>PVNMVLOR</code> es el saldo completo. El dia que algo las consuma, esta cuenta deja
     * de ser el saldo real y hay que restar lo ya descargado.</p>
     *
     * <p>Redondea el valor del rubro y la parte de provision, y saca la parte de gasto por
     * diferencia entre las dos ya redondeadas -la regla del modulo (ver RedondeoNomina)-, en
     * vez de redondear las dos por separado: redondear cada una independientemente puede dejar
     * a la suma sin cuadrar contra el valor del rubro y colar una linea de gasto fantasma de
     * 0,01 o 0,00 en el asiento.</p>
     *
     * @param importes			: Mapa que se va llenando
     * @param idEmpleado		: Id del empleado de la liquidacion
     * @param tipoProvision		: Codigo alterno del detalle del rubro RHH_TIPO_PROVISION
     * @param lineaProvision	: Linea de provision por pagar (44 o 45)
     * @param lineaGasto		: Linea de gasto (62 o 60)
     * @param valorRubro		: Valor del rubro en el finiquito
     * @throws Throwable		: Excepcion
     */
    private void descargaProvisionActuarial(Map<Integer, Double> importes, Long idEmpleado,
            int tipoProvision, int lineaProvision, int lineaGasto, Double valorRubro) throws Throwable {
        Double saldoProvision = provisionNominaDaoService.sumaValorByEmpleadoYTipo(
                idEmpleado, Long.valueOf(tipoProvision));
        // Piso en cero: PVNMVLOR no deberia ser negativo, pero un ajuste actuarial mal cargado
        // no debe volverse un DEBE negativo en el asiento.
        double saldo = saldoProvision != null ? Math.max(saldoProvision.doubleValue(), 0D) : 0D;

        Double valor = RedondeoNomina.redondea(valorRubro);
        Double parteProvision = RedondeoNomina.redondea(
                Double.valueOf(Math.min(saldo, valor.doubleValue())));
        Double parteGasto = RedondeoNomina.redondea(
                Double.valueOf(valor.doubleValue() - parteProvision.doubleValue()));

        if (parteProvision.doubleValue() > 0D) {
            suma(importes, lineaProvision, parteProvision);
        }
        if (parteGasto.doubleValue() > 0D) {
            suma(importes, lineaGasto, parteGasto);
        }
    }


    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#previsualizar(java.lang.Long, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<LineaAsientoNomina> previsualizar(Long idPeriodoNomina, Long tipoAsiento) throws Throwable {
        System.out.println("Ingresa al metodo previsualizar de contabilizacionNomina service, periodo: "
                + idPeriodoNomina + ", tipo: " + tipoAsiento);

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        ConfiguracionNomina configuracion = recuperaConfiguracion(periodo);

        Map<Integer, Double> importes;
        Long codigoAlternoPlantilla;
        // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
        Double totalProvisionVacaciones = null;
        // ===== FIN baja de provision de vacaciones gozadas =====
        if (Long.valueOf(PREVISUALIZA_PROVISIONES).equals(tipoAsiento)) {
            importes = importesDeProvisiones(periodo);
            codigoAlternoPlantilla = configuracion.getPlantillaProvision();
        } else if (tipoAsiento == null || Long.valueOf(PREVISUALIZA_ROL).equals(tipoAsiento)) {
            importes = importesDelRol(periodo);
            // Se saca ANTES de resolver el resto contra plantillaRol: esa linea nunca vivio
            // ahi (ver LINEA_ROL_FUERA_DE_PLANTILLA_ROL), y dejarla adentro la mostraria con
            // "la plantilla no define esta linea" en un asiento que en realidad cuadra bien.
            totalProvisionVacaciones = extraeLineaFueraDePlantillaRol(importes);
            codigoAlternoPlantilla = configuracion.getPlantillaRol();
        } else {
            throw new IncomeException("Tipo de asiento a previsualizar no reconocido: " + tipoAsiento
                    + ". Use 1 para el rol de pagos o 2 para las provisiones.");
        }

        Long idPlantilla = resuelvePlantilla(codigoAlternoPlantilla, "previsualizacion",
                periodo.getEmpresa().getCodigo());
        Long marcadora = configuracion.getCuentaMarcadora();

        List<LineaAsientoNomina> resultado = new ArrayList<LineaAsientoNomina>();
        // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
        if (totalProvisionVacaciones != null && totalProvisionVacaciones.doubleValue() != 0D) {
            resultado.add(lineaPrevisualizacionProvisionVacaciones(configuracion, marcadora,
                    totalProvisionVacaciones));
        }
        // ===== FIN baja de provision de vacaciones gozadas =====
        for (Map.Entry<Integer, Double> entrada : importes.entrySet()) {
            DetallePlantilla plantilla = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, entrada.getKey().intValue());
            LineaAsientoNomina linea = new LineaAsientoNomina();
            linea.setCodigoLinea(Long.valueOf(entrada.getKey().longValue()));
            Double valor = RedondeoNomina.redondea(entrada.getValue());
            if (plantilla == null) {
                // Se muestra igual, con la cuenta en blanco: la previsualizacion sirve
                // justamente para descubrir que a la plantilla le falta una linea.
                linea.setDescripcion("(la plantilla no define esta linea)");
                linea.setDebe(valor);
                linea.setHaber(Double.valueOf(0D));
                resultado.add(linea);
                continue;
            }
            PlanCuenta cuenta = plantilla.getPlanCuenta();
            if (cuenta != null) {
                linea.setCuenta(cuenta.getCuentaContable());
                linea.setNombreCuenta(esMarcadora(cuenta, marcadora)
                        ? cuenta.getNombre() + "  (SIN CONFIGURAR: cuenta marcadora)"
                        : cuenta.getNombre());
            }
            linea.setDescripcion(plantilla.getDescripcion());
            if (esDebe(plantilla)) {
                linea.setDebe(valor);
                linea.setHaber(Double.valueOf(0D));
            } else {
                linea.setDebe(Double.valueOf(0D));
                linea.setHaber(valor);
            }
            resultado.add(linea);
        }
        return resultado;
    }

    // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
    /**
     * Linea de previsualizacion de la baja de provision de vacaciones gozadas (ver
     * {@link #LINEA_ROL_FUERA_DE_PLANTILLA_ROL}), resuelta contra {@code plantillaProvision}
     * -la plantilla real de esa cuenta, no {@code plantillaRol}- y con el DEBE impuesto: en
     * {@code plantillaProvision} esa linea va al HABER porque esa plantilla es la del
     * devengo mensual (se acredita el pasivo al provisionar), y aca se esta dando de baja,
     * direccion contraria. Mismo defecto que descuadro el asiento del decimo cuarto por
     * 1.285,44 si se hereda el lado de la plantilla en vez de imponerlo.
     *
     * @param configuracion	: Configuracion de nomina de la empresa
     * @param marcadora		: PLNNCDGO de la cuenta marcadora
     * @param valor			: Valor a debitar, ya positivo
     * @return				: La linea de previsualizacion
     * @throws Throwable	: Excepcion
     */
    private LineaAsientoNomina lineaPrevisualizacionProvisionVacaciones(ConfiguracionNomina configuracion,
            Long marcadora, Double valor) throws Throwable {
        Double valorRedondeado = RedondeoNomina.redondea(valor);
        LineaAsientoNomina linea = new LineaAsientoNomina();
        linea.setCodigoLinea(Long.valueOf(LINEA_ROL_FUERA_DE_PLANTILLA_ROL));
        linea.setDebe(valorRedondeado);
        linea.setHaber(Double.valueOf(0D));

        Long idPlantillaProvision = resuelvePlantilla(configuracion.getPlantillaProvision(),
                "previsualizacion", configuracion.getEmpresa() != null
                        ? configuracion.getEmpresa().getCodigo() : null);
        DetallePlantilla plantilla = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                idPlantillaProvision, LINEA_ROL_FUERA_DE_PLANTILLA_ROL);
        if (plantilla == null) {
            linea.setDescripcion("(la plantilla de provisiones no define esta linea)");
            return linea;
        }
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        if (cuenta != null) {
            linea.setCuenta(cuenta.getCuentaContable());
            linea.setNombreCuenta(esMarcadora(cuenta, marcadora)
                    ? cuenta.getNombre() + "  (SIN CONFIGURAR: cuenta marcadora)"
                    : cuenta.getNombre());
        }
        linea.setDescripcion(plantilla.getDescripcion());
        return linea;
    }
    // ===== FIN baja de provision de vacaciones gozadas =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.ContabilizacionNominaService#contabilizarBajaProvisionBeneficioSocial(java.lang.Long, int, java.util.List, java.lang.Double, java.time.LocalDate, java.lang.String, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Asiento contabilizarBajaProvisionBeneficioSocial(Long idEmpresa, int tipoBeneficio,
            List<Long> idsEmpleados, Double total, LocalDate fecha, String descripcion, String usuario)
            throws Throwable {
        System.out.println("Ingresa al metodo contabilizarBajaProvisionBeneficioSocial de"
                + " contabilizacionNomina service, empresa: " + idEmpresa + ", tipo: " + tipoBeneficio
                + ", " + (idsEmpleados != null ? idsEmpleados.size() : 0) + " empleado(s)");

        int lineaProvision = lineaProvisionPorPagarBeneficio(tipoBeneficio);

        ConfiguracionNomina configuracion = configuracionNominaDaoService.selectByEmpresa(idEmpresa);
        if (configuracion == null) {
            throw new IncomeException("No existe configuracion de nomina (RHH.CFNM) para la empresa "
                    + idEmpresa + ".");
        }
        Long marcadora = exigeCuentaMarcadora(configuracion);
        Double valor = RedondeoNomina.redondea(total);

        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();

        // DEBE: la baja va INTEGRA contra la provision, sin tope contra RHH.PVNM (decision
        // del usuario, 2026-09-08, "A" para las tres bajas de provision del sistema).
        //
        // Hasta esta version se topeaba contra sumaValorByEmpleadosYTipo, que excluye los
        // periodos no productivos (RHH.PRDN.PRDNMODO<>1). En una empresa migrada eso es
        // exactamente lo que descuadra: los meses previos a la puesta en marcha vinieron por
        // saldos iniciales, el pasivo SI esta en libros (2.5.09/2.5.14), pero PVNM solo ve la
        // porcion generada DENTRO de este sistema. Medido en produccion 2026-09-08 (script
        // e2-30): el tope del decimo cuarto vio 682,89 de un pasivo real de 5.580,95, y
        // 5.833,48 (de un pago de 6.476,20) cayeron a gasto en silencio -el "degrada a todo a
        // gasto, que es lo correcto" que decia este comentario NO lo era para ese caso: el
        // gasto ya estaba reconocido, en otro sistema, y el pasivo esta en la cuenta desde el
        // saldo inicial.
        //
        // Consecuencia aceptada: si alguien cobra un beneficio que nunca se provisiono (ni
        // aca ni en libros), esta cuenta queda en negativo por esa parte -es un ajuste de
        // catalogo/carga que corrige el contador, no algo que este metodo pueda adivinar.
        //
        // EL LADO SE IMPONE, NO SE HEREDA DE LA PLANTILLA. Esa linea sale de la plantilla de
        // PROVISION MENSUAL, donde el pasivo se acredita (HABER) al devengar -correcto ahi-.
        // Aca se esta dando de BAJA el pasivo, que es lo contrario: hay que debitarlo. Usar
        // construyeLinea(plantilla, valor) sin el tercer argumento heredaria el HABER de la
        // plantilla en silencio. Medido en produccion 2026-09-08: asiento del pago de decimo
        // cuarto descuadrado por exactamente el doble del valor de la provision (642,72 x 2 =
        // 1.285,44) antes de este fix.
        Long idPlantillaProvision = resuelvePlantilla(configuracion.getPlantillaProvision(),
                "baja de provision", idEmpresa);
        DetallePlantilla lineaDebeProvision = exigeLinea(idPlantillaProvision, lineaProvision,
                "baja de provision");
        exigeCuentaReal(lineaDebeProvision, marcadora, "baja de provision");
        lineas.add(construyeLinea(lineaDebeProvision, valor, true));

        // HABER: banco, de la misma plantilla que ya usa contabilizarPago para esa linea.
        // Verificado, no supuesto: esta MISMA linea (plantillaPago, RhhLineaAsiento.BANCO) la
        // usa contabilizarPago() de HABER sin forzar nada -alli el dinero tambien SALE del
        // banco, un pago normal-, y esta contabilizacion tambien es un pago saliendo del
        // banco: la direccion coincide, a diferencia de la linea de provision de arriba (que
        // venia de una plantilla pensada para el devengo, direccion opuesta a esta baja). Por
        // eso esta linea SI puede heredar el lado de la plantilla sin forzarlo.
        Long idPlantillaPago = resuelvePlantilla(configuracion.getPlantillaPago(),
                "baja de provision", idEmpresa);
        DetallePlantilla lineaHaber = exigeLinea(idPlantillaPago, RhhLineaAsiento.BANCO, "baja de provision");
        exigeCuentaReal(lineaHaber, marcadora, "baja de provision");
        lineas.add(construyeLinea(lineaHaber, valor));
        // Sin comprobarCuadre: una sola linea DEBE (provision = valor) y una sola HABER
        // (banco = valor) cuadran trivialmente, por construccion.

        Asiento asiento = asientoContableService.generarAsiento(
                idEmpresa,
                TipoAsientos.RECURSOS_HUMANOS,
                fecha,
                descripcion,
                usuario,
                lineas,
                Long.valueOf(ModuloSistema.TESORERIA));

        System.out.println("Baja de provision de beneficio social contabilizada con el asiento "
                + asiento.getCodigo() + " | provision: " + valor);
        return asiento;
    }

    /**
     * Linea de provision por pagar (rubro 214) de un tipo de beneficio social acumulado.
     *
     * @param tipoBeneficio	: Codigo alterno del detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL
     * @return				: Codigo de linea del rubro 214
     * @throws Throwable	: IncomeException si el tipo no tiene linea de provision
     */
    private int lineaProvisionPorPagarBeneficio(int tipoBeneficio) throws Throwable {
        switch (tipoBeneficio) {
            case RhhTipoBeneficioSocial.DECIMO_TERCERO:
                return RhhLineaAsiento.PROVISION_DECIMO_TERCERO_POR_PAGAR;
            case RhhTipoBeneficioSocial.DECIMO_CUARTO:
                return RhhLineaAsiento.PROVISION_DECIMO_CUARTO_POR_PAGAR;
            case RhhTipoBeneficioSocial.FONDOS_DE_RESERVA:
                return RhhLineaAsiento.PROVISION_FONDOS_DE_RESERVA_POR_PAGAR;
            default:
                throw new IncomeException("El tipo de beneficio " + tipoBeneficio + " no tiene linea de"
                        + " provision por pagar definida. La orden de beneficio social solo admite"
                        + " decimo tercero (1), decimo cuarto (2) o fondos de reserva (3).");
        }
    }

    // =====================================================================
    // Importes por linea del rubro 214
    // =====================================================================

    // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
    /**
     * Codigo de linea que {@link #importesDelRol} puede sumar pero que NO se resuelve contra
     * {@code plantillaRol}: vive en {@code plantillaProvision} porque el asiento del rol la
     * reutiliza en la direccion contraria a la que esa plantilla fue pensada (el pasivo se
     * acredita, HABER, al devengar; aca se debita para darlo de baja).
     *
     * <p><b>Los tres consumidores de {@code importesDelRol} tienen que tratarla igual</b>
     * ({@link #contabilizarRol}, {@link #previsualizar}, {@link #validarCuentasContables}):
     * antes de este bloque solo uno la sacaba del mapa antes de resolverlo contra
     * {@code plantillaRol}, y los otros dos se rompian -uno visible para el usuario
     * (previsualizar mostraba "la plantilla no define esta linea" en un asiento que en
     * realidad cuadra perfecto), el otro una validacion inventada (validarCuentasContables
     * reportaba un faltante que no existe). Centralizado aca para que agregar el proximo caso
     * igual no repita el problema.</p>
     */
    private static final int LINEA_ROL_FUERA_DE_PLANTILLA_ROL = RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR;

    /**
     * Saca de {@code importes} la linea que no se resuelve contra {@code plantillaRol} (ver
     * {@link #LINEA_ROL_FUERA_DE_PLANTILLA_ROL}) y devuelve su valor. El mapa queda
     * modificado: lo que le quede adentro ya se puede resolver entero contra
     * {@code plantillaRol}, sin excepciones ni casos especiales.
     *
     * @param importes	: Mapa de {@link #importesDelRol}, se modifica
     * @return			: El valor extraido, o {@code null} si el periodo no tuvo ninguno
     */
    private Double extraeLineaFueraDePlantillaRol(Map<Integer, Double> importes) {
        return importes.remove(Integer.valueOf(LINEA_ROL_FUERA_DE_PLANTILLA_ROL));
    }
    // ===== FIN baja de provision de vacaciones gozadas =====

    /**
     * Acumula los importes del asiento de rol por codigo de linea.
     *
     * <p>Recorre los renglones de todas las nominas del periodo y los clasifica por el rol del
     * concepto (<code>CPNMROLM</code>), nunca por su codigo alterno ni por la terna. Las lineas
     * que suman cero no entran en el mapa.</p>
     *
     * @param periodo		: Periodo de nomina
     * @return				: Mapa codigo de linea → valor
     * @throws Throwable	: Excepcion
     */
    private Map<Integer, Double> importesDelRol(PeriodoNomina periodo) throws Throwable {
        Map<Integer, Double> importes = new LinkedHashMap<Integer, Double>();
        List<Nomina> nominas = nominaDaoService.selectByPeriodo(periodo.getCodigo());
        if (nominas == null || nominas.isEmpty()) {
            throw new IncomeException("El periodo " + periodo.getCodigo()
                    + " no tiene nominas calculadas: no hay asiento que armar.");
        }

        Double neto = Double.valueOf(0D);
        for (Nomina nomina : nominas) {
            neto = RedondeoNomina.suma(neto, nomina.getNetoPagar());
            List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());
            if (renglones == null) {
                continue;
            }
            // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
            // Los renglones del rol VACACIONES_GOZADAS se sacan del clasificador generico y se
            // suman aparte: hace falta el total POR EMPLEADO de este nomina, antes de tocar el
            // mapa, para topearlo contra el saldo de ESE empleado (no el de todos juntos).
            Double valorVacacionesGozadas = Double.valueOf(0D);
            for (ReglonNomina renglon : renglones) {
                if (esVacacionesGozadas(renglon)) {
                    valorVacacionesGozadas = RedondeoNomina.suma(valorVacacionesGozadas, renglon.getValor());
                } else {
                    avisaSiConceptoVacacionesSinRol(renglon);
                    acumulaRenglon(importes, renglon);
                }
            }
            if (valorVacacionesGozadas.doubleValue() > 0D) {
                acumulaBajaProvisionVacaciones(importes, valorVacacionesGozadas);
            }
            // ===== FIN baja de provision de vacaciones gozadas =====
        }

        // El neto va siempre a la linea de sueldos por pagar: es la contrapartida de todo el
        // gasto y la linea de cuadre del asiento.
        suma(importes, RhhLineaAsiento.SUELDOS_POR_PAGAR, neto);
        return sinCeros(importes);
    }

    // ===== INICIO baja de provision de vacaciones gozadas (equipo omen-saa-2, script e2-29) =====
    /**
     * Indica si un renglon es de vacaciones gozadas (rol de motor 36).
     *
     * @param renglon	: Renglon a evaluar
     * @return			: true si el concepto del renglon tiene el rol VACACIONES_GOZADAS
     */
    private boolean esVacacionesGozadas(ReglonNomina renglon) {
        ConceptoNomina concepto = renglon.getConceptoNomina();
        return concepto != null && esRol(concepto.getRolMotor(), RhhRolConceptoMotor.VACACIONES_GOZADAS)
                && renglon.getValor() != null && renglon.getValor().doubleValue() != 0D;
    }

    /**
     * Codigo alterno historico del concepto "Vacaciones pagadas" (RHH.CPNM.CPNMALTR = 12),
     * el que crea {@code SolicitudVacacionesServiceImpl} al aprobar una solicitud. Solo se usa
     * para el aviso de {@link #avisaSiConceptoVacacionesSinRol}, NUNCA para clasificar un
     * renglon -esta clase clasifica siempre por {@code CPNMROLM} (ver el javadoc de
     * {@link #importesDelRol}). Se compara aparte porque el rol de motor 36 (script e2-29)
     * es una parametrizacion que puede faltar por empresa sin que sea un error de programa.
     */
    private static final Long CODIGO_ALTERNO_VACACIONES_PAGADAS = Long.valueOf(12L);

    /**
     * Deja traza si un renglon del concepto historico de vacaciones pagadas (alterno 12)
     * todavia no tiene el rol de motor 36: sigue yendo a gasto de sueldos como siempre (no
     * revienta, es una parametrizacion pendiente, no un error), pero sin la traza el
     * descuadre de la provision vuelve a pasar en silencio -exactamente el defecto medido en
     * produccion 2026-09-08 (164 filas, $6.840,03 en RHH.PVNM sin bajar nunca).
     *
     * @param renglon	: Renglon ya descartado por {@link #esVacacionesGozadas}
     */
    private void avisaSiConceptoVacacionesSinRol(ReglonNomina renglon) {
        ConceptoNomina concepto = renglon.getConceptoNomina();
        if (concepto != null && CODIGO_ALTERNO_VACACIONES_PAGADAS.equals(concepto.getCodigoAlterno())) {
            System.out.println("⚠ Concepto '" + concepto.getNombre() + "' (alterno 12, vacaciones pagadas)"
                    + " sin rol de motor " + RhhRolConceptoMotor.VACACIONES_GOZADAS + " en la empresa "
                    + (concepto.getEmpresa() != null ? concepto.getEmpresa().getCodigo() : "?")
                    + ": el renglon " + renglon.getCodigo() + " va a gasto de sueldos sin descargar la"
                    + " provision. Falta el script e2-29 (UPDATE RHH.CPNM SET CPNMROLM="
                    + RhhRolConceptoMotor.VACACIONES_GOZADAS + " WHERE CPNMALTR=12) para esta empresa.");
        }
    }

    /**
     * Acumula el valor de vacaciones gozadas de un empleado INTEGRO contra la provision
     * acumulada (rubro 214, linea 42), sin tope contra {@code RHH.PVNM} (decision del
     * usuario, 2026-09-08, "A" para las tres bajas de provision del sistema).
     *
     * <p>Hasta esta version se topeaba contra {@code sumaValorByEmpleadoYTipo} -el saldo
     * REAL de ese empleado, no el de todos juntos, que es el patron correcto cuando SI hace
     * falta topear (ver {@code descargaProvisionActuarial}, mas abajo)-, pero en una empresa
     * migrada esa consulta excluye los periodos no productivos: el pasivo de vacaciones
     * previo a la puesta en marcha SI esta en libros por saldo inicial, y {@code PVNM} solo
     * ve la porcion generada dentro de este sistema. Medido en produccion 2026-09-08: el tope
     * por empleado veia ~30 de ~230 acumulados reales.</p>
     *
     * <p>Consecuencia aceptada: si un empleado goza vacaciones sin tener provision real
     * detras (ni aca ni en libros), esta cuenta queda en negativo por esa parte -lo ajusta el
     * contador, este metodo no puede distinguir ese caso de una empresa migrada.</p>
     *
     * @param importes			: Mapa que se va llenando
     * @param valorVacaciones	: Valor total de vacaciones gozadas de este empleado en el periodo
     */
    private void acumulaBajaProvisionVacaciones(Map<Integer, Double> importes, Double valorVacaciones) {
        suma(importes, RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR, RedondeoNomina.redondea(valorVacaciones));
    }
    // ===== FIN baja de provision de vacaciones gozadas =====

    /**
     * Clasifica un renglon en su linea del asiento de rol.
     *
     * @param importes	: Mapa que se va llenando
     * @param renglon	: Renglon a clasificar
     */
    private void acumulaRenglon(Map<Integer, Double> importes, ReglonNomina renglon) {
        Double valor = renglon.getValor();
        if (valor == null || valor.doubleValue() == 0D) {
            return;
        }
        ConceptoNomina concepto = renglon.getConceptoNomina();
        Long rol = concepto != null ? concepto.getRolMotor() : null;
        Long tipo = renglon.getTipoConcepto();

        if (Long.valueOf(RhhTipoConceptoNomina.APORTE_PATRONAL).equals(tipo)) {
            // DEBE: el gasto patronal, separando el IESS del IECE y el SECAP porque son dos
            // cuentas de gasto distintas. HABER: los tres van al IESS, que es quien los
            // recauda en la misma planilla.
            if (esRol(rol, RhhRolConceptoMotor.APORTE_PATRONAL)) {
                suma(importes, RhhLineaAsiento.GASTO_APORTE_PATRONAL_IESS, valor);
            } else {
                suma(importes, RhhLineaAsiento.GASTO_IECE_Y_SECAP, valor);
            }
            suma(importes, RhhLineaAsiento.IESS_POR_PAGAR_APORTE_PATRONAL, valor);
            return;
        }

        if (Long.valueOf(RhhTipoConceptoNomina.EGRESO).equals(tipo)) {
            suma(importes, lineaDeDescuento(rol), valor);
            return;
        }

        if (Long.valueOf(RhhTipoConceptoNomina.INGRESO).equals(tipo)) {
            suma(importes, lineaDeIngreso(rol), valor);
            return;
        }
        // Los tipos PROVISION e INFORMATIVO no entran en el asiento de rol: las provisiones
        // tienen su propio asiento y los informativos no mueven dinero.
    }

    /**
     * Linea de gasto que corresponde a un renglon de ingreso.
     *
     * @param rol	: Rol del concepto, o null
     * @return		: Codigo de linea del rubro 214
     */
    private int lineaDeIngreso(Long rol) {
        if (esRol(rol, RhhRolConceptoMotor.FONDOS_DE_RESERVA)) {
            return RhhLineaAsiento.GASTO_FONDOS_DE_RESERVA;
        }
        if (esRol(rol, RhhRolConceptoMotor.DECIMO_TERCERO)) {
            return RhhLineaAsiento.GASTO_DECIMO_TERCERO;
        }
        if (esRol(rol, RhhRolConceptoMotor.DECIMO_CUARTO)) {
            return RhhLineaAsiento.GASTO_DECIMO_CUARTO;
        }
        if (esRol(rol, RhhRolConceptoMotor.HORA_SUPLEMENTARIA)
                || esRol(rol, RhhRolConceptoMotor.HORA_EXTRAORDINARIA)
                || esRol(rol, RhhRolConceptoMotor.RECARGO_NOCTURNO)) {
            return RhhLineaAsiento.GASTO_HORAS_EXTRA;
        }
        // Sueldo, bonos, comisiones, subsidios y cualquier ingreso que el cliente agregue:
        // todos son gasto de sueldos y salarios mientras no tengan linea propia.
        return RhhLineaAsiento.GASTO_SUELDOS_Y_SALARIOS;
    }

    /**
     * Linea de obligacion que corresponde a un renglon de egreso.
     *
     * @param rol	: Rol del concepto, o null
     * @return		: Codigo de linea del rubro 214
     */
    private int lineaDeDescuento(Long rol) {
        if (esRol(rol, RhhRolConceptoMotor.APORTE_PERSONAL)) {
            return RhhLineaAsiento.IESS_POR_PAGAR_APORTE_PERSONAL;
        }
        if (esRol(rol, RhhRolConceptoMotor.IMPUESTO_A_LA_RENTA)) {
            return RhhLineaAsiento.SRI_RETENCION_EN_LA_FUENTE_RD;
        }
        if (esRol(rol, RhhRolConceptoMotor.PRESTAMO_QUIROGRAFARIO)) {
            return RhhLineaAsiento.IESS_POR_PAGAR_PRESTAMOS;
        }
        if (esRol(rol, RhhRolConceptoMotor.PRESTAMO_HIPOTECARIO)) {
            return RhhLineaAsiento.IESS_POR_PAGAR_PRESTAMOS_HIPOTECARIOS;
        }
        if (esRol(rol, RhhRolConceptoMotor.RETENCION_JUDICIAL)) {
            return RhhLineaAsiento.RETENCIONES_JUDICIALES_POR_PAGAR;
        }
        // Anticipos, prestamos internos y cualquier otro descuento son un derecho de cobro de
        // la empresa contra el empleado.
        return RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS;
    }

    /**
     * Acumula los importes del asiento de provisiones por codigo de linea.
     *
     * <p>Cada tipo de provision aporta dos lineas: el gasto al DEBE y la provision por pagar
     * al HABER, por el mismo valor. Es lo que hace que este asiento cuadre por
     * construccion.</p>
     *
     * @param periodo		: Periodo de nomina
     * @return				: Mapa codigo de linea → valor
     * @throws Throwable	: Excepcion
     */
    private Map<Integer, Double> importesDeProvisiones(PeriodoNomina periodo) throws Throwable {
        Map<Integer, Double> importes = new LinkedHashMap<Integer, Double>();
        List<ProvisionNomina> provisiones = provisionNominaDaoService.selectByPeriodo(periodo.getCodigo());
        if (provisiones == null) {
            return importes;
        }
        for (ProvisionNomina provision : provisiones) {
            Double valor = provision.getValor();
            if (valor == null || valor.doubleValue() == 0D) {
                continue;
            }
            Long tipo = provision.getTipoProvision();
            int lineaGasto = lineaGastoProvision(tipo);
            int lineaPorPagar = lineaPorPagarProvision(tipo);
            if (lineaGasto == 0 || lineaPorPagar == 0) {
                // El unico tipo del rubro 206 sin linea es APORTE_PATRONAL, que quedo sin uso
                // a proposito: el asiento de rol ya lo registra completo.
                System.out.println("Provision de tipo " + tipo + " sin linea de asiento definida:"
                        + " se omite del asiento de provisiones.");
                continue;
            }
            suma(importes, lineaGasto, valor);
            suma(importes, lineaPorPagar, valor);
        }
        return sinCeros(importes);
    }

    /**
     * Linea de gasto de una provision.
     *
     * @param tipo	: Detalle del rubro RHH_TIPO_PROVISION
     * @return		: Codigo de linea, o 0 si ese tipo no tiene linea
     */
    private int lineaGastoProvision(Long tipo) {
        if (Long.valueOf(RhhTipoProvision.DECIMO_TERCERO).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_DECIMO_TERCERO;
        }
        if (Long.valueOf(RhhTipoProvision.DECIMO_CUARTO).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_DECIMO_CUARTO;
        }
        if (Long.valueOf(RhhTipoProvision.VACACIONES).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_VACACIONES;
        }
        if (Long.valueOf(RhhTipoProvision.FONDOS_DE_RESERVA).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_FONDOS_DE_RESERVA;
        }
        if (Long.valueOf(RhhTipoProvision.JUBILACION_PATRONAL).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_JUBILACION_PATRONAL;
        }
        if (Long.valueOf(RhhTipoProvision.DESAHUCIO).equals(tipo)) {
            return RhhLineaAsiento.GASTO_PROVISION_DESAHUCIO;
        }
        return 0;
    }

    /**
     * Linea de provision por pagar.
     *
     * @param tipo	: Detalle del rubro RHH_TIPO_PROVISION
     * @return		: Codigo de linea, o 0 si ese tipo no tiene linea
     */
    private int lineaPorPagarProvision(Long tipo) {
        if (Long.valueOf(RhhTipoProvision.DECIMO_TERCERO).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_DECIMO_TERCERO_POR_PAGAR;
        }
        if (Long.valueOf(RhhTipoProvision.DECIMO_CUARTO).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_DECIMO_CUARTO_POR_PAGAR;
        }
        if (Long.valueOf(RhhTipoProvision.VACACIONES).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_VACACIONES_POR_PAGAR;
        }
        if (Long.valueOf(RhhTipoProvision.FONDOS_DE_RESERVA).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_FONDOS_DE_RESERVA_POR_PAGAR;
        }
        if (Long.valueOf(RhhTipoProvision.JUBILACION_PATRONAL).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_JUBILACION_PATRONAL;
        }
        if (Long.valueOf(RhhTipoProvision.DESAHUCIO).equals(tipo)) {
            return RhhLineaAsiento.PROVISION_DESAHUCIO;
        }
        return 0;
    }

    // =====================================================================
    // Armado y cuadre
    // =====================================================================

    /**
     * Convierte el mapa de importes en lineas de asiento, resolviendo cada cuenta por
     * <code>DTPLAXL1</code>.
     *
     * @param codigoAlternoPlantilla	: Codigo alterno de la plantilla, leido de CFNM
     * @param importes					: Mapa codigo de linea → valor
     * @param configuracion				: Configuracion de nomina de la empresa
     * @param etiqueta					: Nombre del asiento, para los mensajes
     * @return							: Lineas listas para generarAsiento
     * @throws Throwable				: Excepcion
     */
    private List<DetalleAsiento> armaLineas(Long codigoAlternoPlantilla, Map<Integer, Double> importes,
            ConfiguracionNomina configuracion, String etiqueta) throws Throwable {

        Long idPlantilla = resuelvePlantilla(codigoAlternoPlantilla, etiqueta,
                configuracion.getEmpresa() != null ? configuracion.getEmpresa().getCodigo() : null);
        Long marcadora = exigeCuentaMarcadora(configuracion);

        List<DetalleAsiento> lineas = new ArrayList<DetalleAsiento>();
        for (Map.Entry<Integer, Double> entrada : importes.entrySet()) {
            DetallePlantilla plantilla = exigeLinea(idPlantilla, entrada.getKey().intValue(), etiqueta);
            exigeCuentaReal(plantilla, marcadora, etiqueta);
            lineas.add(construyeLinea(plantilla, RedondeoNomina.redondea(entrada.getValue())));
        }
        return lineas;
    }

    /**
     * Construye una linea de asiento a partir de su definicion en la plantilla.
     *
     * @param plantilla	: Linea de la plantilla
     * @param valor		: Importe, ya redondeado
     * @return			: La linea del asiento
     */
    private DetalleAsiento construyeLinea(DetallePlantilla plantilla, Double valor) {
        return construyeLinea(plantilla, valor, esDebe(plantilla));
    }

    // ===== INICIO fix descuadre baja de provision (equipo omen-saa-2, 2026-09-08) =====
    /**
     * Igual que {@link #construyeLinea(DetallePlantilla, Double)}, pero con el lado
     * (DEBE/HABER) impuesto por el llamador en vez de heredado de {@code plantilla}.
     *
     * <p>Hace falta cuando la linea de una plantilla se reutiliza para el asiento
     * INVERSO al que esa plantilla fue pensada -por ejemplo, la linea de "provision por
     * pagar" de la plantilla de PROVISION MENSUAL, donde el pasivo se acredita (HABER)
     * al devengar, reutilizada para dar de BAJA esa misma provision, donde hay que
     * debitarla. {@link #esDebe(DetallePlantilla)} en ese caso devuelve el lado
     * correcto para el asiento de ALTA, no para este. Ver
     * {@link #contabilizarBajaProvisionBeneficioSocial}, unico llamador hoy.</p>
     *
     * <p>⛔ No usar para ningun otro llamador de {@link #construyeLinea(DetallePlantilla, Double)}:
     * el resto de las contabilizaciones de esta clase SI dependen del lado que trae su
     * propia plantilla y funcionan bien con la sobrecarga de dos argumentos.</p>
     *
     * @param plantilla	: Linea de la plantilla, solo para la cuenta/descripcion
     * @param valor		: Importe, ya redondeado
     * @param forzarDebe: true = la linea va al DEBE; false = va al HABER; sin mirar el
     *					  movimiento configurado en la plantilla
     * @return			: La linea del asiento
     */
    private DetalleAsiento construyeLinea(DetallePlantilla plantilla, Double valor, boolean forzarDebe) {
        DetalleAsiento detalle = new DetalleAsiento();
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta != null ? cuenta.getCuentaContable() : null);
        detalle.setNombreCuenta(cuenta != null ? cuenta.getNombre() : null);
        detalle.setDescripcion(plantilla.getDescripcion());
        if (forzarDebe) {
            detalle.setValorDebe(valor);
            detalle.setValorHaber(Double.valueOf(0D));
        } else {
            detalle.setValorDebe(Double.valueOf(0D));
            detalle.setValorHaber(valor);
        }
        return detalle;
    }
    // ===== FIN fix descuadre baja de provision (equipo omen-saa-2, 2026-09-08) =====

    /**
     * Comprueba el cuadre antes de llamar a generarAsiento y ajusta la diferencia por redondeo
     * contra la linea indicada.
     *
     * <p>Sin esta comprobacion el usuario recibe el <code>IncomeException</code> generico de
     * <code>validaDebeHaber</code>, que enumera las lineas pero no dice cual falta ni por que.
     * Una diferencia dentro de <code>CFNMTLCD</code> se ajusta; una mayor se rechaza aqui, con
     * el importe exacto en el mensaje.</p>
     *
     * @param lineas					: Lineas del asiento
     * @param configuracion				: Configuracion, de donde sale la tolerancia
     * @param codigoLineaCuadre			: Linea contra la que se ajusta el redondeo
     * @param codigoAlternoPlantilla	: Plantilla, para localizar esa linea
     * @throws Throwable				: IncomeException si la diferencia supera la tolerancia
     */
    private void comprobarCuadre(List<DetalleAsiento> lineas, ConfiguracionNomina configuracion,
            int codigoLineaCuadre, Long codigoAlternoPlantilla) throws Throwable {

        Double debe = Double.valueOf(0D);
        Double haber = Double.valueOf(0D);
        for (DetalleAsiento linea : lineas) {
            debe = RedondeoNomina.suma(debe, linea.getValorDebe());
            haber = RedondeoNomina.suma(haber, linea.getValorHaber());
        }
        if (RedondeoNomina.sonIguales(debe, haber)) {
            return;
        }

        double diferencia = debe.doubleValue() - haber.doubleValue();
        double tolerancia = configuracion.getToleranciaCuadre() != null
                ? configuracion.getToleranciaCuadre().doubleValue() : TOLERANCIA_POR_DEFECTO;

        if (Math.abs(diferencia) > tolerancia) {
            throw new IncomeException("El asiento no cuadra: DEBE " + debe + " y HABER " + haber
                    + ", diferencia " + RedondeoNomina.redondea(Double.valueOf(diferencia))
                    + ". Supera la tolerancia de cuadre configurada (" + tolerancia + ") y no se"
                    + " emite. Revise que el periodo este calculado por completo y que la"
                    + " plantilla defina todas las lineas que el periodo usa.");
        }

        // Diferencia de centavos por redondeo: se ajusta contra la linea de cuadre.
        DetallePlantilla plantillaCuadre = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                resuelvePlantilla(codigoAlternoPlantilla, "cuadre",
                        configuracion.getEmpresa() != null ? configuracion.getEmpresa().getCodigo() : null),
                codigoLineaCuadre);
        if (plantillaCuadre == null) {
            throw new IncomeException("El asiento difiere en "
                    + RedondeoNomina.redondea(Double.valueOf(diferencia))
                    + " por redondeo, pero la plantilla no define la linea de cuadre "
                    + codigoLineaCuadre + " contra la que ajustarlo.");
        }
        String cuentaCuadre = plantillaCuadre.getPlanCuenta() != null
                ? plantillaCuadre.getPlanCuenta().getCuentaContable() : null;
        for (DetalleAsiento linea : lineas) {
            if (cuentaCuadre != null && cuentaCuadre.equals(linea.getNumeroCuenta())) {
                if (esDebe(plantillaCuadre)) {
                    linea.setValorDebe(RedondeoNomina.redondea(Double.valueOf(
                            linea.getValorDebe().doubleValue() - diferencia)));
                } else {
                    linea.setValorHaber(RedondeoNomina.redondea(Double.valueOf(
                            linea.getValorHaber().doubleValue() + diferencia)));
                }
                System.out.println("Cuadre por redondeo de " + diferencia
                        + " ajustado contra la linea " + codigoLineaCuadre + ".");
                return;
            }
        }
        throw new IncomeException("El asiento difiere en "
                + RedondeoNomina.redondea(Double.valueOf(diferencia))
                + " por redondeo, pero la linea de cuadre " + codigoLineaCuadre
                + " no esta entre las lineas del asiento.");
    }

    // =====================================================================
    // Apoyo
    // =====================================================================

    /**
     * Revisa que las lineas que el periodo va a usar tengan una cuenta distinta de la marcadora.
     *
     * @param faltantes					: Lista que se va llenando con los mensajes
     * @param codigoAlternoPlantilla	: Codigo alterno de la plantilla
     * @param marcadora					: PLNNCDGO de la cuenta marcadora
     * @param importes					: Lineas que el periodo usa
     * @param etiqueta					: Nombre del asiento, para los mensajes
     * @throws Throwable				: Excepcion
     */
    private void revisaLineas(List<String> faltantes, Long codigoAlternoPlantilla, Long marcadora,
            Map<Integer, Double> importes, String etiqueta) throws Throwable {

        if (importes.isEmpty()) {
            return;
        }
        if (codigoAlternoPlantilla == null) {
            faltantes.add("La configuracion de nomina (RHH.CFNM) no tiene la plantilla del "
                    + etiqueta + " asignada.");
            return;
        }
        Long idPlantilla;
        try {
            idPlantilla = resuelvePlantilla(codigoAlternoPlantilla, etiqueta, null);
        } catch (Throwable e) {
            faltantes.add(e.getMessage());
            return;
        }
        for (Integer codigoLinea : importes.keySet()) {
            DetallePlantilla plantilla = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                    idPlantilla, codigoLinea.intValue());
            if (plantilla == null) {
                faltantes.add("La plantilla del " + etiqueta + " no define la linea " + codigoLinea
                        + " (rubro 214), y el periodo tiene valores para ella.");
                continue;
            }
            if (esMarcadora(plantilla.getPlanCuenta(), marcadora)) {
                faltantes.add("La linea " + codigoLinea + " del " + etiqueta + " ("
                        + plantilla.getDescripcion() + ") sigue apuntando a la cuenta marcadora "
                        + marcadora + ": falta asignarle su cuenta contable real.");
            }
        }
    }

    /**
     * Indica si una cuenta es la marcadora, es decir, si esa linea sigue sin configurar.
     *
     * @param cuenta	: Cuenta de la linea
     * @param marcadora	: PLNNCDGO de la cuenta marcadora
     * @return			: true si la linea esta sin configurar
     */
    private boolean esMarcadora(PlanCuenta cuenta, Long marcadora) {
        if (cuenta == null) {
            return true;
        }
        return marcadora != null && marcadora.equals(cuenta.getCodigo());
    }

    /**
     * Exige que la configuracion tenga la cuenta marcadora informada.
     *
     * @param configuracion	: Configuracion de nomina
     * @return				: PLNNCDGO de la cuenta marcadora
     * @throws Throwable	: IncomeException si no esta informada
     */
    private Long exigeCuentaMarcadora(ConfiguracionNomina configuracion) throws Throwable {
        Long marcadora = configuracion.getCuentaMarcadora();
        if (marcadora == null) {
            // No se supone un valor por defecto: si la columna esta vacia, ninguna linea se
            // reconoceria como pendiente y el sistema emitiria asientos con todas las cuentas
            // iguales sin avisar. Es exactamente el fallo que este control existe para evitar.
            throw new IncomeException("RHH.CFNM.CFNMCTMR no esta informada para la empresa: sin la"
                    + " cuenta marcadora no se puede distinguir una linea configurada de una que no"
                    + " lo esta. Ejecute el script 13 o asigne el valor en la configuracion de"
                    + " nomina.");
        }
        return marcadora;
    }

    /**
     * Exige que una linea de plantilla exista.
     *
     * @param idPlantilla	: Id de la plantilla
     * @param codigoLinea	: Codigo alterno del detalle del rubro 214
     * @param etiqueta		: Nombre del asiento, para el mensaje
     * @return				: La linea
     * @throws Throwable	: IncomeException si la plantilla no la define
     */
    private DetallePlantilla exigeLinea(Long idPlantilla, int codigoLinea, String etiqueta)
            throws Throwable {
        DetallePlantilla plantilla = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                idPlantilla, codigoLinea);
        if (plantilla == null) {
            throw new IncomeException("La plantilla del " + etiqueta + " no define la linea "
                    + codigoLinea + " del rubro 214, y el periodo tiene valores para ella.");
        }
        return plantilla;
    }

    /**
     * Exige que una linea tenga cuenta real, no la marcadora.
     *
     * @param plantilla		: Linea de la plantilla
     * @param marcadora		: PLNNCDGO de la cuenta marcadora
     * @param etiqueta		: Nombre del asiento, para el mensaje
     * @throws Throwable	: IncomeException si sigue con el marcador
     */
    private void exigeCuentaReal(DetallePlantilla plantilla, Long marcadora, String etiqueta)
            throws Throwable {
        if (esMarcadora(plantilla.getPlanCuenta(), marcadora)) {
            throw new IncomeException("La linea " + plantilla.getAuxiliar1() + " del " + etiqueta
                    + " (" + plantilla.getDescripcion() + ") sigue apuntando a la cuenta marcadora "
                    + marcadora + ". Asigne su cuenta contable real antes de contabilizar.");
        }
    }

    /**
     * Traduce el codigo alterno de la plantilla a su id.
     *
     * @param codigoAlterno	: Codigo alterno leido de CFNM
     * @param etiqueta		: Nombre del asiento, para el mensaje
     * @param idEmpresa		: Id de la empresa
     * @return				: Id de la plantilla
     * @throws Throwable	: IncomeException si no esta configurada o no existe
     */
    private Long resuelvePlantilla(Long codigoAlterno, String etiqueta, Long idEmpresa) throws Throwable {
        if (codigoAlterno == null) {
            throw new IncomeException("La configuracion de nomina (RHH.CFNM) no tiene la plantilla del "
                    + etiqueta + " asignada.");
        }
        Long idPlantilla = plantillaService.codigoByAlterno(codigoAlterno.intValue(), idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable con codigo alterno "
                    + codigoAlterno + " (" + etiqueta + ") para la empresa " + idEmpresa + ".");
        }
        return idPlantilla;
    }

    /**
     * Indica si una linea de plantilla es de DEBE.
     *
     * @param plantilla	: Linea de la plantilla
     * @return			: true si es DEBE
     */
    private boolean esDebe(DetallePlantilla plantilla) {
        return plantilla.getMovimiento() != null
                && plantilla.getMovimiento().longValue() == MOVIMIENTO_DEBE;
    }

    /**
     * Compara el rol de un concepto con un rol del rubro 221.
     *
     * @param rol			: Rol del concepto, puede ser null
     * @param rolEsperado	: Rol contra el que se compara
     * @return				: true si coinciden
     */
    private boolean esRol(Long rol, int rolEsperado) {
        return rol != null && rol.longValue() == rolEsperado;
    }

    /**
     * Suma un valor a una linea del mapa.
     *
     * @param importes		: Mapa de importes
     * @param codigoLinea	: Codigo de linea del rubro 214
     * @param valor			: Valor a sumar
     */
    private void suma(Map<Integer, Double> importes, int codigoLinea, Double valor) {
        Integer clave = Integer.valueOf(codigoLinea);
        Double actual = importes.get(clave);
        importes.put(clave, RedondeoNomina.suma(actual != null ? actual : Double.valueOf(0D), valor));
    }

    /**
     * Retira del mapa las lineas que quedaron en cero.
     *
     * <p>Una linea en cero no aporta al asiento y exigirle cuenta configurada bloquearia la
     * contabilizacion por un rubro que la empresa no aplica.</p>
     *
     * @param importes	: Mapa de importes
     * @return			: El mismo mapa sin las lineas en cero
     */
    private Map<Integer, Double> sinCeros(Map<Integer, Double> importes) {
        Map<Integer, Double> limpio = new LinkedHashMap<Integer, Double>();
        for (Map.Entry<Integer, Double> entrada : importes.entrySet()) {
            if (entrada.getValue() != null && entrada.getValue().doubleValue() != 0D) {
                limpio.put(entrada.getKey(), entrada.getValue());
            }
        }
        return limpio;
    }

    /**
     * Fecha del asiento: la contable del periodo si esta informada, si no la de fin.
     *
     * @param periodo	: Periodo de nomina
     * @return			: Fecha del asiento
     */
    private LocalDate fechaContable(PeriodoNomina periodo) {
        if (periodo.getFechaContable() != null) {
            return periodo.getFechaContable();
        }
        return periodo.getFechaFin() != null ? periodo.getFechaFin() : LocalDate.now();
    }

    /**
     * Indica si el periodo esta en modo historico.
     *
     * <p>Un modo nulo se trata como historico: es el valor que tienen los periodos creados
     * antes de que existiera la columna, y para ellos lo seguro es no contabilizar.</p>
     *
     * @param periodo	: Periodo de nomina
     * @return			: true si no debe generar asiento
     */
    private boolean esHistorico(PeriodoNomina periodo) {
        return periodo.getModo() == null
                || Long.valueOf(RhhModoPeriodoNomina.HISTORICO_SIN_CONTABILIZAR).equals(periodo.getModo());
    }

    /**
     * Exige que el periodo este aprobado o mas adelante en el flujo.
     *
     * @param periodo		: Periodo de nomina
     * @throws Throwable	: IncomeException si todavia no se aprobo
     */
    private void exigeAprobado(PeriodoNomina periodo) throws Throwable {
        Long estado = periodo.getEstado();
        boolean contabilizable = Long.valueOf(RhhEstadoPeriodoNomina.APROBADO).equals(estado)
                || Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(estado);
        if (!contabilizable) {
            throw new IncomeException("La contabilizacion se hace sobre un periodo APROBADO ("
                    + RhhEstadoPeriodoNomina.APROBADO + ") o CONTABILIZADO ("
                    + RhhEstadoPeriodoNomina.CONTABILIZADO + ", para emitir el segundo asiento)."
                    + " El periodo esta en estado " + estado + " y no lo admite.");
        }
    }

    /**
     * Recupera la configuracion de nomina de la empresa del periodo.
     *
     * @param periodo		: Periodo de nomina
     * @return				: La configuracion
     * @throws Throwable	: IncomeException si no existe
     */
    private ConfiguracionNomina recuperaConfiguracion(PeriodoNomina periodo) throws Throwable {
        if (periodo.getEmpresa() == null || periodo.getEmpresa().getCodigo() == null) {
            throw new IncomeException("El periodo " + periodo.getCodigo()
                    + " no tiene empresa asignada: sin ella no se puede resolver la configuracion"
                    + " de nomina ni emitir el asiento.");
        }
        ConfiguracionNomina configuracion = configuracionNominaDaoService.selectByEmpresa(
                periodo.getEmpresa().getCodigo());
        if (configuracion == null) {
            throw new IncomeException("No existe configuracion de nomina (RHH.CFNM) para la empresa "
                    + periodo.getEmpresa().getCodigo() + ".");
        }
        return configuracion;
    }

    /**
     * Recupera el periodo y falla con mensaje explicito si no existe.
     *
     * @param idPeriodoNomina	: Id del periodo
     * @return					: El periodo
     * @throws Throwable		: IncomeException si no existe
     */
    private PeriodoNomina recuperaPeriodo(Long idPeriodoNomina) throws Throwable {
        PeriodoNomina periodo = periodoNominaDaoService.selectById(idPeriodoNomina,
                NombreEntidadesRhh.PERIODO_NOMINA);
        if (periodo == null) {
            throw new IncomeException("No existe el periodo de nomina " + idPeriodoNomina + ".");
        }
        return periodo;
    }
}
