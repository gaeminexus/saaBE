package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.dao.DetallePlantillaDaoService;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.PlantillaService;
import com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService;
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ProvisionNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
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
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.RhhEstadoLiquidacion;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhLineaAsiento;
import com.saa.rubros.RhhModoPeriodoNomina;
import com.saa.rubros.RhhRolConceptoMotor;
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
        revisaLineas(faltantes, configuracion.getPlantillaRol(), marcadora,
                importesDelRol(periodo), "rol de pagos");
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
        List<DetalleAsiento> lineas = armaLineas(configuracion.getPlantillaRol(), importes,
                configuracion, "rol de pagos");
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
                return RhhLineaAsiento.GASTO_DESAHUCIO;
            case RhhRolConceptoMotor.FINIQUITO_DESPIDO_INTEMPESTIVO:
                return RhhLineaAsiento.GASTO_DESPIDO_INTEMPESTIVO;
            case RhhRolConceptoMotor.FINIQUITO_JUBILACION_PATRONAL:
                return RhhLineaAsiento.GASTO_JUBILACION_PATRONAL;
            default:
                // Remuneracion pendiente, fondos de reserva pendientes y cualquier rubro nuevo.
                return RhhLineaAsiento.GASTO_SUELDOS_LIQUIDACION;
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
        if (Long.valueOf(PREVISUALIZA_PROVISIONES).equals(tipoAsiento)) {
            importes = importesDeProvisiones(periodo);
            codigoAlternoPlantilla = configuracion.getPlantillaProvision();
        } else if (tipoAsiento == null || Long.valueOf(PREVISUALIZA_ROL).equals(tipoAsiento)) {
            importes = importesDelRol(periodo);
            codigoAlternoPlantilla = configuracion.getPlantillaRol();
        } else {
            throw new IncomeException("Tipo de asiento a previsualizar no reconocido: " + tipoAsiento
                    + ". Use 1 para el rol de pagos o 2 para las provisiones.");
        }

        Long idPlantilla = resuelvePlantilla(codigoAlternoPlantilla, "previsualizacion",
                periodo.getEmpresa().getCodigo());
        Long marcadora = configuracion.getCuentaMarcadora();

        List<LineaAsientoNomina> resultado = new ArrayList<LineaAsientoNomina>();
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

    // =====================================================================
    // Importes por linea del rubro 214
    // =====================================================================

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
            for (ReglonNomina renglon : renglones) {
                acumulaRenglon(importes, renglon);
            }
        }

        // El neto va siempre a la linea de sueldos por pagar: es la contrapartida de todo el
        // gasto y la linea de cuadre del asiento.
        suma(importes, RhhLineaAsiento.SUELDOS_POR_PAGAR, neto);
        return sinCeros(importes);
    }

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
        if (esRol(rol, RhhRolConceptoMotor.PRESTAMO_QUIROGRAFARIO)
                || esRol(rol, RhhRolConceptoMotor.PRESTAMO_HIPOTECARIO)) {
            return RhhLineaAsiento.IESS_POR_PAGAR_PRESTAMOS;
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
        DetalleAsiento detalle = new DetalleAsiento();
        PlanCuenta cuenta = plantilla.getPlanCuenta();
        detalle.setPlanCuenta(cuenta);
        detalle.setNumeroCuenta(cuenta != null ? cuenta.getCuentaContable() : null);
        detalle.setNombreCuenta(cuenta != null ? cuenta.getNombre() : null);
        detalle.setDescripcion(plantilla.getDescripcion());
        if (esDebe(plantilla)) {
            detalle.setValorDebe(valor);
            detalle.setValorHaber(Double.valueOf(0D));
        } else {
            detalle.setValorDebe(Double.valueOf(0D));
            detalle.setValorHaber(valor);
        }
        return detalle;
    }

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
