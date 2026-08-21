package com.saa.ejb.rhh.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.AcumuladoNominaDaoService;
import com.saa.ejb.rhh.dao.ConceptoNominaDaoService;
import com.saa.ejb.rhh.dao.ContratoEmpleadoDaoService;
import com.saa.ejb.rhh.dao.CausalTerminacionDaoService;
import com.saa.ejb.rhh.dao.DescuentoRecurrenteDaoService;
import com.saa.ejb.rhh.dao.DetalleLiquidacionDaoService;
import com.saa.ejb.rhh.dao.EmpleadoDaoService;
import com.saa.ejb.rhh.dao.LiquidacionDaoService;
import com.saa.ejb.rhh.dao.ParametroNominaDaoService;
import com.saa.ejb.rhh.dao.SaldoVacacionesDaoService;
import com.saa.ejb.rhh.service.AcreditacionVacacionesService;
import com.saa.ejb.rhh.service.LiquidacionHaberesService;
import com.saa.ejb.rhh.service.NovedadIessService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.rhh.AcumuladoNomina;
import com.saa.model.rhh.CausalTerminacion;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.DescuentoRecurrente;
import com.saa.model.rhh.DetalleLiquidacion;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.Liquidacion;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ParametroNomina;
import com.saa.model.rhh.RenglonCalculado;
import com.saa.model.rhh.ResultadoLiquidacion;
import com.saa.model.rhh.SaldoVacaciones;
import com.saa.rubros.Estado;
import com.saa.rubros.RhhEstadoEmpleado;
import com.saa.rubros.RhhEstadoLiquidacion;
import com.saa.rubros.RhhRegionDecimoCuarto;
import com.saa.rubros.RhhRolConceptoMotor;
import com.saa.rubros.RhhTipoAcumulado;
import com.saa.rubros.RhhTipoConceptoNomina;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de LiquidacionHaberesService.</p>
 *
 * <h3>Los ocho rubros del finiquito</h3>
 *
 * <p>Se localizan por <code>CPNMROLM</code>, roles 23 a 30, que el script 17 anadio al rubro
 * 221 y asigno a los conceptos 60 a 67. Antes se localizaban por codigo alterno: discriminaba
 * bien, pero dejaba valores de catalogo quemados en Java --contra la regla 1-- y era el unico
 * sitio del modulo que no usaba el rol. <b>Con esto no queda ningun <code>CPNMALTR</code>
 * literal en el modulo.</b></p>
 *
 * <h3>Que rubro corresponde lo decide la causal</h3>
 *
 * <p>No hay ninguna lista en Java: <code>CSTRDSHC</code> decide el desahucio,
 * <code>CSTRDSPD</code> la indemnizacion, <code>CSTRJBPT</code> la jubilacion patronal,
 * <code>CSTRDCPR</code> los decimos proporcionales y <code>CSTRVCPR</code> las vacaciones. Un
 * cambio de criterio legal se resuelve con un <code>UPDATE</code> en <code>RHH.CSTR</code>.</p>
 *
 * <h3>Los importes de ley</h3>
 *
 * <ul>
 *   <li><b>Desahucio</b>: <code>PRNMDSPR</code> por ciento de la ultima remuneracion por cada
 *       anio de servicio (Art. 185 CT).</li>
 *   <li><b>Despido intempestivo</b>: con menos de <code>PRNMDIAN</code> anios,
 *       <code>PRNMDIMN</code> remuneraciones; con mas, una por anio, acotada entre
 *       <code>PRNMDIMN</code> y <code>PRNMDIMX</code> (Art. 188 CT).</li>
 * </ul>
 */
@Stateless
public class LiquidacionHaberesServiceImpl implements LiquidacionHaberesService {

    /** Bandera afirmativa. */
    private static final String SI = "S";

    /**
     * Estado con el que queda <code>CNTE.CNTEESTD</code> tras ejecutar la salida.
     * <p>
     * <b>NO es texto libre.</b> La columna tiene un CHECK del esquema original,
     * <code>RHH.CK_CNTRESTD</code>, que solo admite cuatro valores:
     * <code>'BORRADOR'</code>, <code>'ACTIVO'</code>, <code>'CERRADO'</code> y
     * <code>'ANULADO'</code>. Ese CHECK no esta en ningun script del repositorio
     * --viene de la creacion original del esquema--, asi que la unica forma de
     * conocerlo es preguntarselo a la base:
     * <pre>
     * SELECT SEARCH_CONDITION FROM ALL_CONSTRAINTS
     *  WHERE OWNER = 'RHH' AND TABLE_NAME = 'CNTE' AND CONSTRAINT_TYPE = 'C';
     * </pre>
     * Se eligio <code>'CERRADO'</code> y no <code>'ANULADO'</code> porque anular
     * es deshacer un contrato que no debio existir; aqui el contrato existio y
     * llego a su fin. Un literal fuera de esa lista --como el 'TERMINADO' que
     * habia antes-- hace fallar el commit entero con ORA-02290 y tumba la salida
     * completa, aunque el System.out ya haya dicho que fue bien.
     */
    private static final String ESTADO_CONTRATO_CERRADO = "CERRADO";


    /** Mes en que arranca el periodo del decimo tercero (1-dic a 30-nov). */
    private static final int MES_INICIO_DECIMO_TERCERO = 12;

    /** Mes en que arranca el periodo del decimo cuarto en Sierra y Amazonia. */
    private static final int MES_INICIO_SIERRA = 8;

    /** Mes en que arranca el periodo del decimo cuarto en Costa e Insular. */
    private static final int MES_INICIO_COSTA = 3;

    /** Meses del anio, divisor de los decimos proporcionales. */
    private static final double MESES_ANIO = 12D;

    /** Dias de un anio, para el prorrateo de la antiguedad. */
    private static final double DIAS_ANIO_CALENDARIO = 365D;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private LiquidacionDaoService liquidacionDaoService;

    @EJB
    private DetalleLiquidacionDaoService detalleLiquidacionDaoService;

    @EJB
    private ContratoEmpleadoDaoService contratoEmpleadoDaoService;

    @EJB
    private CausalTerminacionDaoService causalTerminacionDaoService;

    @EJB
    private ConceptoNominaDaoService conceptoNominaDaoService;

    @EJB
    private ParametroNominaDaoService parametroNominaDaoService;

    @EJB
    private AcumuladoNominaDaoService acumuladoNominaDaoService;

    @EJB
    private SaldoVacacionesDaoService saldoVacacionesDaoService;

    @EJB
    private DescuentoRecurrenteDaoService descuentoRecurrenteDaoService;

    @EJB
    private EmpleadoDaoService empleadoDaoService;

    @EJB
    private AcreditacionVacacionesService acreditacionVacacionesService;

    @EJB
    private NovedadIessService novedadIessService;

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.LiquidacionHaberesService#simular(java.lang.Long, java.time.LocalDate, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ResultadoLiquidacion simular(Long idContrato, LocalDate fechaSalida, Long idCausal)
            throws Throwable {
        System.out.println("Ingresa al metodo simular de liquidacionHaberes service, contrato: " + idContrato);
        List<RenglonCalculado> rubros = new ArrayList<RenglonCalculado>();
        Liquidacion liquidacion = calculaFiniquito(idContrato, fechaSalida, idCausal, null, "SIMULACION",
                false, rubros);
        return armaResultado(liquidacion, rubros);
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.LiquidacionHaberesService#calcular(java.lang.Long, java.time.LocalDate, java.lang.Long, java.lang.String, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Liquidacion calcular(Long idContrato, LocalDate fechaSalida, Long idCausal,
            String observaciones, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo calcular de liquidacionHaberes service, contrato: " + idContrato);
        List<RenglonCalculado> rubros = new ArrayList<RenglonCalculado>();
        return calculaFiniquito(idContrato, fechaSalida, idCausal, observaciones, usuario, true, rubros);
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.LiquidacionHaberesService#aprobar(java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void aprobar(Long idLiquidacion, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo aprobar de liquidacionHaberes service, liquidacion: "
                + idLiquidacion);
        Liquidacion liquidacion = recuperaLiquidacion(idLiquidacion);
        if (Long.valueOf(RhhEstadoLiquidacion.APROBADA).equals(liquidacion.getEstado())) {
            throw new IncomeException("La liquidacion " + idLiquidacion + " ya estaba aprobada.");
        }
        if (!Long.valueOf(RhhEstadoLiquidacion.CALCULADA).equals(liquidacion.getEstado())
                && !Long.valueOf(RhhEstadoLiquidacion.BORRADOR).equals(liquidacion.getEstado())) {
            throw new IncomeException("Solo se aprueba una liquidacion en BORRADOR ("
                    + RhhEstadoLiquidacion.BORRADOR + ") o CALCULADA ("
                    + RhhEstadoLiquidacion.CALCULADA + "). La " + idLiquidacion + " esta en estado "
                    + liquidacion.getEstado() + ".");
        }
        liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacion.APROBADA));
        liquidacion.setFechaAprobacion(LocalDate.now());
        liquidacion.setUsuarioAprueba(usuario);
        liquidacionDaoService.save(liquidacion, liquidacion.getCodigo());
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.LiquidacionHaberesService#ejecutarSalida(java.lang.Long, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void ejecutarSalida(Long idLiquidacion, String usuario) throws Throwable {
        System.out.println("Ingresa al metodo ejecutarSalida de liquidacionHaberes service, liquidacion: "
                + idLiquidacion);

        Liquidacion liquidacion = recuperaLiquidacion(idLiquidacion);
        if (!Long.valueOf(RhhEstadoLiquidacion.APROBADA).equals(liquidacion.getEstado())) {
            throw new IncomeException("La liquidacion debe estar APROBADA ("
                    + RhhEstadoLiquidacion.APROBADA + ") para ejecutar la salida. La "
                    + idLiquidacion + " esta en estado " + liquidacion.getEstado()
                    + ": ejecutar la salida sobre un finiquito que todavia se puede recalcular"
                    + " dejaria al empleado cesante con un finiquito que cambia.");
        }

        ContratoEmpleado contrato = liquidacion.getContrato();
        Empleado empleado = liquidacion.getEmpleado();

        // 1. El contrato se cierra con la fecha de salida del finiquito.
        contrato.setFechaTerminacion(liquidacion.getFechaSalida());
        // Y su estado deja de decir ACTIVO. No cambia ningun calculo --el DAO selecciona los
        // contratos por fechas y por el estado del EMPLEADO, nunca por CNTEESTD-- pero un
        // contrato que se queda en 'ACTIVO' con su titular cesante es un dato que miente:
        // cualquiera que lo consulte por estado ve un contrato vigente de alguien que ya no
        // esta. Ademas es lo que rompia la comprobacion 1 del guion de carga, que cuenta
        // contratos activos.
        //
        // El valor sale del vocabulario que impone el CHECK RHH.CK_CNTRESTD; ver la
        // constante. NO es texto libre, aunque la columna sea VARCHAR2.
        contrato.setEstado(ESTADO_CONTRATO_CERRADO);
        if (liquidacion.getCausalTerminacion() != null) {
            contrato.setCausalTerminacion(liquidacion.getCausalTerminacion());
        }
        em.merge(contrato);

        // 2. El empleado pasa a CESANTE: es lo que hace que el motor deje de incluirlo.
        empleado.setEstado(Long.valueOf(RhhEstadoEmpleado.CESANTE));
        em.merge(empleado);

        // 3. Aviso de salida al IESS, si la causal lo exige. El plazo lo sabe el servicio.
        CausalTerminacion causal = liquidacion.getCausalTerminacion();
        if (causal == null || SI.equals(causal.getRequiereAvisoSalida())) {
            novedadIessService.generarAvisoSalida(idLiquidacion, usuario);
        }

        // 4. Los descuentos recurrentes vigentes se cancelan: el saldo ya se cruzo en el
        //    finiquito, dejarlos vivos los volveria a descontar de una nomina que no habra.
        int cancelados = cancelaDescuentos(empleado.getCodigo(), usuario);

        // 5. Los saldos de vacaciones caducan: ya se pagaron como rubro del finiquito.
        int caducados = caducaSaldosVacaciones(empleado.getCodigo());

        // 6. Los acumulados del finiquito.
        int acumulados = escribeAcumuladosDelFiniquito(liquidacion, usuario);

        System.out.println("Salida ejecutada para " + empleado.getIdentificacion() + ": contrato"
                + " cerrado, empleado CESANTE, " + cancelados + " descuento(s) cancelado(s), "
                + caducados + " saldo(s) de vacaciones caducado(s), " + acumulados
                + " acumulado(s) escrito(s).");
    }

    // =====================================================================
    // El calculo
    // =====================================================================

    /**
     * Calcula el finiquito y, si se pide, lo persiste con sus rubros.
     *
     * @param idContrato		: Id del contrato
     * @param fechaSalida		: Fecha de salida
     * @param idCausal			: Causal de terminacion
     * @param observaciones		: Observaciones
     * @param usuario			: Usuario que ejecuta
     * @param persistir			: true para grabar
     * @param rubros			: Lista de salida con los rubros calculados
     * @return					: La liquidacion
     * @throws Throwable		: Excepcion
     */
    private Liquidacion calculaFiniquito(Long idContrato, LocalDate fechaSalida, Long idCausal,
            String observaciones, String usuario, boolean persistir, List<RenglonCalculado> rubros)
            throws Throwable {

        if (fechaSalida == null) {
            throw new IncomeException("La liquidacion exige la fecha de salida.");
        }
        ContratoEmpleado contrato = contratoEmpleadoDaoService.selectById(idContrato,
                NombreEntidadesRhh.CONTRATO_EMPLEADO);
        if (contrato == null) {
            throw new IncomeException("No existe el contrato " + idContrato + ".");
        }
        Empleado empleado = contrato.getEmpleado();
        Long idEmpresa = empleado.getEmpresa() != null ? empleado.getEmpresa().getCodigo() : null;

        CausalTerminacion causal = null;
        if (idCausal != null) {
            causal = causalTerminacionDaoService.selectById(idCausal,
                    NombreEntidadesRhh.CAUSAL_TERMINACION);
            if (causal == null) {
                throw new IncomeException("No existe la causal de terminacion " + idCausal + ".");
            }
        }

        ParametroNomina prnm = parametroNominaDaoService.selectByAnio(idEmpresa,
                Integer.valueOf(fechaSalida.getYear()));
        if (prnm == null) {
            throw new IncomeException("No existen parametros de nomina (RHH.PRNM) para el anio "
                    + fechaSalida.getYear() + ": sin ellos no se puede liquidar.");
        }
        List<ConceptoNomina> conceptos = conceptoNominaDaoService.selectActivosByEmpresa(idEmpresa);

        LocalDate fechaIngreso = empleado.getFechaIngreso() != null
                ? empleado.getFechaIngreso() : contrato.getFechaInicio();
        if (fechaIngreso == null) {
            throw new IncomeException("El empleado " + empleado.getIdentificacion()
                    + " no tiene fecha de ingreso: sin ella no se puede calcular la antiguedad.");
        }
        double aniosServicio = ChronoUnit.DAYS.between(fechaIngreso, fechaSalida) / DIAS_ANIO_CALENDARIO;
        Double ultimaRemuneracion = contrato.getSalarioBase() != null
                ? contrato.getSalarioBase() : Double.valueOf(0D);

        // --- Remuneracion pendiente del mes en curso -------------------------------
        double diasBase = prnm.getDiasMes() != null ? prnm.getDiasMes().doubleValue() : 30D;
        double diasTrabajados = Math.min(fechaSalida.getDayOfMonth(), diasBase);
        Double remuneracion = RedondeoNomina.redondea(Double.valueOf(
                ultimaRemuneracion.doubleValue() * diasTrabajados / diasBase));
        agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_REMUNERACION_PENDIENTE,
                ultimaRemuneracion, diasTrabajados, remuneracion);

        // --- Aporte personal al IESS ------------------------------------------------
        // La base es la remuneracion pendiente y NADA MAS: indemnizaciones, decimos y
        // vacaciones no son materia gravada. Es ley, no criterio -- 9,45 % de 1.000,00, no de
        // los 7.650,91 de ingresos del finiquito. Si el importe sale del total, el porcentaje
        // se esta aplicando donde no debe.
        if (SI.equals(contrato.getAportaIess())) {
            ConceptoNomina conceptoAporte = conceptoPorRol(conceptos,
                    RhhRolConceptoMotor.FINIQUITO_APORTE_PERSONAL);
            if (conceptoAporte == null) {
                // No se llama a agrega sin concepto: cuando falta, el rubro se etiquetaria como
                // INGRESO por defecto y el aporte SUMARIA al neto en vez de restarlo. Preferible
                // no generarlo y dejar traza.
                System.out.println("Aviso: no hay concepto con rol "
                        + RhhRolConceptoMotor.FINIQUITO_APORTE_PERSONAL + " (aporte personal del"
                        + " finiquito): el descuento no se genera. Ejecute el script 22.");
            } else {
                Double aporte = RedondeoNomina.porcentaje(remuneracion,
                        porcentajeAporte(conceptoAporte, prnm));
                agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_APORTE_PERSONAL,
                        remuneracion, null, aporte);
            }
        }

        // --- Decimos proporcionales ------------------------------------------------
        // LOS DOS SE DEVENGAN EN TODO EL PERIODO, NO EN EL MES QUE SE LIQUIDA.
        //
        // Hasta el 2026-08-21 esta rama pagaba remuneracion/12 y sbu/12 x dias/30, es decir
        // SOLO la fraccion del ultimo mes. A quien cobra los decimos mensualizados le da
        // igual --no acumula-- y por eso el finiquito de enero de Torres Chavez cuadro con
        // el acta; pero a quien los ACUMULA le faltaba todo lo anterior. Castro Arce, con
        // tres meses de servicio, cobraba 8,03 de cada decimo en vez de 118,40 y 119,16.
        //
        // El mes en curso no tiene fila de ACMN --se escribe al cerrar-- asi que entra
        // aparte: la remuneracion pendiente en el decimo tercero, y los dias trabajados en
        // el cuarto. El rango de los acumulados llega al mes ANTERIOR, para no contarlo dos
        // veces si el periodo del mes de salida llegara a cerrarse.
        LocalDate mesAnterior = fechaSalida.withDayOfMonth(1).minusMonths(1L);
        if (causal == null || SI.equals(causal.getPagaDecimosProporcionales())) {
            // Decimo tercero: el periodo va del 1 de diciembre al 30 de noviembre.
            int anioPeriodo13 = fechaSalida.getMonthValue() >= MES_INICIO_DECIMO_TERCERO
                    ? fechaSalida.getYear() : fechaSalida.getYear() - 1;
            Double baseAcumulada = acumuladoNominaDaoService.sumaValorRango(empleado.getCodigo(),
                    Long.valueOf(RhhTipoAcumulado.BASE_DECIMO_TERCERO),
                    Integer.valueOf(anioPeriodo13), Integer.valueOf(MES_INICIO_DECIMO_TERCERO),
                    Integer.valueOf(mesAnterior.getYear()),
                    Integer.valueOf(mesAnterior.getMonthValue()));
            Double base13 = RedondeoNomina.suma(baseAcumulada, remuneracion);
            Double decimoTercero = RedondeoNomina.divide(base13, Double.valueOf(MESES_ANIO));
            agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_DECIMO_TERCERO, base13,
                    null, decimoTercero);

            // Decimo cuarto: SBU por dias trabajados del periodo regional sobre PRNMDANO.
            // Los dias salen de tres sitios y los tres hacen falta: los meses cerrados
            // (ACMN tipo DIAS_TRABAJADOS), la fila de apertura de la migracion --que trae
            // los dias anteriores al sistema en el acumulado del propio decimo cuarto-- y
            // los dias del mes que se liquida.
            boolean sierra = empleado.getRegion() == null
                    || Long.valueOf(RhhRegionDecimoCuarto.SIERRA_Y_AMAZONIA).equals(empleado.getRegion());
            int mesInicio14 = sierra ? MES_INICIO_SIERRA : MES_INICIO_COSTA;
            int anioPeriodo14 = fechaSalida.getMonthValue() >= mesInicio14
                    ? fechaSalida.getYear() : fechaSalida.getYear() - 1;
            Double diasCerrados = acumuladoNominaDaoService.sumaDiasRango(empleado.getCodigo(),
                    Long.valueOf(RhhTipoAcumulado.DIAS_TRABAJADOS),
                    Integer.valueOf(anioPeriodo14), Integer.valueOf(mesInicio14),
                    Integer.valueOf(mesAnterior.getYear()),
                    Integer.valueOf(mesAnterior.getMonthValue()));
            Double diasApertura = acumuladoNominaDaoService.sumaDiasRango(empleado.getCodigo(),
                    Long.valueOf(RhhTipoAcumulado.BASE_DECIMO_CUARTO),
                    Integer.valueOf(anioPeriodo14), Integer.valueOf(mesInicio14),
                    Integer.valueOf(mesAnterior.getYear()),
                    Integer.valueOf(mesAnterior.getMonthValue()));
            double dias14 = (diasCerrados != null ? diasCerrados.doubleValue() : 0D)
                    + (diasApertura != null ? diasApertura.doubleValue() : 0D)
                    + diasTrabajados;

            Double sbu = prnm.getSbu() != null ? prnm.getSbu() : Double.valueOf(0D);
            double diasAnio = prnm.getDiasAnio() != null ? prnm.getDiasAnio().doubleValue() : 0D;
            Double decimoCuarto = diasAnio != 0D
                    ? RedondeoNomina.redondea(Double.valueOf(sbu.doubleValue() * dias14 / diasAnio))
                    : Double.valueOf(0D);
            // Tope de un SBU, igual que en el calculo anual.
            if (decimoCuarto.doubleValue() > sbu.doubleValue()) {
                decimoCuarto = RedondeoNomina.redondea(sbu);
            }
            agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_DECIMO_CUARTO, sbu,
                    RedondeoNomina.redondeaCantidad(Double.valueOf(dias14)).doubleValue(),
                    decimoCuarto);
        }

        // --- Vacaciones no gozadas -------------------------------------------------
        if (causal == null || SI.equals(causal.getPagaVacacionesProporcionales())) {
            // TRES TRAMOS, Y ANTES SOLO SE PAGABAN DOS.
            //
            //   1. El saldo de SLDV, que es lo acreditado hasta el ultimo cierre de periodo
            //      vacacional --para ASOPREP, el saldo de apertura al 31-dic-2025--.
            //   2. Los meses YA CERRADOS que nadie acredito. acreditar() solo escribe a
            //      quien cumple un anio de servicio, asi que entre la apertura y la salida
            //      queda un hueco que no estaba en ningun sitio: a Castro Arce le faltaban
            //      enero y febrero enteros, 2,5 dias.
            //   3. El mes en curso, que no tiene fila de acumulado todavia.
            //
            // Cada tramo se redondea antes de sumarse, que es la regla 4.
            double diasSaldo = 0D;
            double importeSaldo = 0D;
            int anioAcreditado = 0;
            for (SaldoVacaciones saldo : saldoVacacionesDaoService.selectDisponibles(empleado.getCodigo())) {
                if (saldo.getDiasPendientes() == null) {
                    continue;
                }
                diasSaldo += saldo.getDiasPendientes().doubleValue();
                if (saldo.getValorDia() != null) {
                    // A SU PROPIA tarifa, no a la del mes de salida: es la unica valoracion
                    // que devuelve el importe con el que se migro el saldo. La de Castro Arce
                    // es de 2025 y vale 15,6354, no los 16,0667 del sueldo de hoy.
                    importeSaldo += saldo.getDiasPendientes().doubleValue()
                            * saldo.getValorDia().doubleValue();
                }
                if (saldo.getAnio() != null && saldo.getAnio().intValue() > anioAcreditado) {
                    anioAcreditado = saldo.getAnio().intValue();
                }
            }
            Double valorSaldo = RedondeoNomina.redondea(Double.valueOf(importeSaldo));

            // Lo devengado se valora a la remuneracion del momento en que se gano, que con
            // sueldo estable es la misma tarifa de ventana que calcula AcreditacionVacaciones
            // --482/30 = 16,0667 en estos dos casos--.
            Double valorDiaActual = RedondeoNomina.redondea(
                    Double.valueOf(ultimaRemuneracion.doubleValue() / diasBase),
                    RedondeoNomina.DECIMALES_CANTIDAD);
            double factorVacaciones = prnm.getDiasVacaciones() != null && prnm.getDiasAnio() != null
                    && prnm.getDiasAnio().doubleValue() != 0D
                    ? prnm.getDiasVacaciones().doubleValue() / prnm.getDiasAnio().doubleValue()
                    : 0D;
            if (factorVacaciones == 0D) {
                System.out.println("Aviso: PRNMDIVC o PRNMDANO no estan informados, asi que el"
                        + " finiquito no devenga vacaciones fuera del saldo acreditado.");
            }

            // Tramo 2: del anio siguiente al ultimo acreditado hasta el mes anterior a la
            // salida. Sin saldo ninguno se arranca en el mes de ingreso.
            int anioDesdeVac = anioAcreditado > 0 ? anioAcreditado + 1 : fechaIngreso.getYear();
            int mesDesdeVac = anioAcreditado > 0 ? 1 : fechaIngreso.getMonthValue();
            Double diasCerradosVac = acumuladoNominaDaoService.sumaDiasRango(empleado.getCodigo(),
                    Long.valueOf(RhhTipoAcumulado.DIAS_TRABAJADOS),
                    Integer.valueOf(anioDesdeVac), Integer.valueOf(mesDesdeVac),
                    Integer.valueOf(mesAnterior.getYear()),
                    Integer.valueOf(mesAnterior.getMonthValue()));
            double diasVacCerrados = RedondeoNomina.redondeaCantidad(Double.valueOf(
                    (diasCerradosVac != null ? diasCerradosVac.doubleValue() : 0D)
                            * factorVacaciones)).doubleValue();
            Double valorCerrados = RedondeoNomina.redondea(Double.valueOf(
                    diasVacCerrados * valorDiaActual.doubleValue()));

            // Tramo 3: el mes que se liquida.
            double diasVacMes = RedondeoNomina.redondeaCantidad(
                    Double.valueOf(diasTrabajados * factorVacaciones)).doubleValue();
            Double valorMes = RedondeoNomina.redondea(Double.valueOf(
                    diasVacMes * valorDiaActual.doubleValue()));

            double diasTotales = diasSaldo + diasVacCerrados + diasVacMes;
            Double vacaciones = RedondeoNomina.suma(valorSaldo, valorCerrados, valorMes);
            if (diasTotales > 0D && vacaciones.doubleValue() > 0D) {
                // La base del rubro es la tarifa EFECTIVA --importe entre dias-- porque el
                // renglon mezcla la del saldo migrado con la del sueldo actual. Asi base por
                // cantidad sigue devolviendo el importe.
                Double baseEfectiva = RedondeoNomina.redondea(
                        Double.valueOf(vacaciones.doubleValue() / diasTotales),
                        RedondeoNomina.DECIMALES_CANTIDAD);
                agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_VACACIONES, baseEfectiva,
                        RedondeoNomina.redondeaCantidad(Double.valueOf(diasTotales)).doubleValue(),
                        vacaciones);
            }
        }

        // --- Desahucio (Art. 185 CT) -----------------------------------------------
        Double desahucio = Double.valueOf(0D);
        if (causal != null && SI.equals(causal.getGeneraDesahucio()) && prnm.getPorcentajeDesahucio() != null) {
            desahucio = RedondeoNomina.redondea(Double.valueOf(
                    ultimaRemuneracion.doubleValue() * prnm.getPorcentajeDesahucio().doubleValue()
                            / 100D * aniosServicio));
            agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_DESAHUCIO, ultimaRemuneracion,
                    aniosServicio, desahucio);
        }

        // --- Despido intempestivo (Art. 188 CT) ------------------------------------
        Double despido = Double.valueOf(0D);
        if (causal != null && SI.equals(causal.getGeneraDespido())) {
            despido = calculaIndemnizacion(ultimaRemuneracion, aniosServicio, prnm);
            agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_DESPIDO_INTEMPESTIVO,
                    ultimaRemuneracion, aniosServicio, despido);
        }

        // --- Jubilacion patronal ---------------------------------------------------
        Double jubilacion = Double.valueOf(0D);
        if (causal != null && SI.equals(causal.getGeneraJubilacionPatronal())) {
            // El importe viene del estudio actuarial: aqui se deja el rubro en cero para que
            // el usuario lo complete. Calcularlo con una formula propia contradiria que la
            // provision actuarial se carga desde fuera.
            agrega(rubros, conceptos, RhhRolConceptoMotor.FINIQUITO_JUBILACION_PATRONAL,
                    ultimaRemuneracion, aniosServicio, jubilacion);
            System.out.println("La causal genera jubilacion patronal: el rubro entra en cero, el"
                    + " importe sale del estudio actuarial y se completa a mano.");
        }

        // --- Totales ---------------------------------------------------------------
        Double ingresos = Double.valueOf(0D);
        Double descuentos = Double.valueOf(0D);
        for (RenglonCalculado rubro : rubros) {
            if (Long.valueOf(RhhTipoConceptoNomina.EGRESO).equals(rubro.getTipoConcepto())) {
                descuentos = RedondeoNomina.suma(descuentos, rubro.getValor());
            } else {
                ingresos = RedondeoNomina.suma(ingresos, rubro.getValor());
            }
        }
        // Los descuentos recurrentes con saldo se cruzan contra el finiquito.
        descuentos = RedondeoNomina.suma(descuentos, saldoDescuentos(empleado.getCodigo()));

        Double neto = RedondeoNomina.redondea(Double.valueOf(
                ingresos.doubleValue() - descuentos.doubleValue()));
        if (neto.doubleValue() < 0D) {
            // A diferencia del rol, aqui NO se lanza: un neto negativo significa que el
            // trabajador debe dinero a la empresa, y ese saldo hay que registrarlo para
            // gestionar su cobro, no hacerlo desaparecer.
            System.out.println("Aviso: el finiquito de " + empleado.getIdentificacion()
                    + " queda en " + neto + ". Se registra para gestion de cobro.");
        }

        Liquidacion liquidacion = persistir
                ? localizaLiquidacion(idContrato, fechaSalida) : null;
        if (liquidacion == null) {
            liquidacion = new Liquidacion();
            liquidacion.setEmpleado(empleado);
            liquidacion.setContrato(contrato);
            liquidacion.setFechaRegistro(LocalDate.now());
            liquidacion.setUsuarioRegistro(usuario);
        } else if (Long.valueOf(RhhEstadoLiquidacion.APROBADA).equals(liquidacion.getEstado())) {
            throw new IncomeException("La liquidacion " + liquidacion.getCodigo()
                    + " ya esta aprobada y no se puede recalcular.");
        }

        liquidacion.setFechaSalida(fechaSalida);
        liquidacion.setCausalTerminacion(causal);
        liquidacion.setFechaIngreso(fechaIngreso);
        liquidacion.setAniosServicio(RedondeoNomina.redondeaCantidad(Double.valueOf(aniosServicio)));
        liquidacion.setUltimaRemuneracion(ultimaRemuneracion);
        liquidacion.setTotalIngresos(ingresos);
        liquidacion.setTotalDescuentos(descuentos);
        liquidacion.setDesahucio(desahucio);
        liquidacion.setDespidoIntempestivo(despido);
        liquidacion.setJubilacionPatronal(jubilacion);
        liquidacion.setNeto(neto);
        liquidacion.setEstado(Long.valueOf(RhhEstadoLiquidacion.CALCULADA));
        if (observaciones != null) {
            liquidacion.setMotivo(observaciones);
        }

        if (persistir) {
            liquidacion = liquidacionDaoService.save(liquidacion, liquidacion.getCodigo());
            em.flush();
            detalleLiquidacionDaoService.eliminaByLiquidacion(liquidacion.getCodigo());
            int orden = 1;
            for (RenglonCalculado rubro : rubros) {
                DetalleLiquidacion detalle = new DetalleLiquidacion();
                detalle.setLiquidacion(liquidacion);
                detalle.setConceptoNomina(conceptoPorCodigo(conceptos, rubro.getCodigoConcepto()));
                detalle.setDescripcion(rubro.getNombreConcepto());
                detalle.setTipoConcepto(rubro.getTipoConcepto());
                detalle.setBaseCalculo(rubro.getBase());
                detalle.setDias(rubro.getCantidad());
                detalle.setValor(rubro.getValor());
                detalle.setOrden(Integer.valueOf(orden++));
                detalle.setFechaRegistro(LocalDate.now());
                detalle.setUsuarioRegistro(usuario);
                detalleLiquidacionDaoService.save(detalle, detalle.getCodigo());
            }
        }

        return liquidacion;
    }

    /**
     * Indemnizacion por despido intempestivo (Art. 188 CT).
     *
     * @param remuneracion	: Ultima remuneracion
     * @param anios			: Anios de servicio
     * @param prnm			: Parametros del anio
     * @return				: El importe
     */
    private Double calculaIndemnizacion(Double remuneracion, double anios, ParametroNomina prnm) {
        double minimo = prnm.getIndemnizacionMinima() != null
                ? prnm.getIndemnizacionMinima().doubleValue() : 0D;
        double maximo = prnm.getIndemnizacionMaxima() != null
                ? prnm.getIndemnizacionMaxima().doubleValue() : Double.MAX_VALUE;
        double umbral = prnm.getAniosIndemnizacionMinima() != null
                ? prnm.getAniosIndemnizacionMinima().doubleValue() : 0D;

        double remuneraciones = anios < umbral ? minimo : Math.max(minimo, Math.min(anios, maximo));
        return RedondeoNomina.redondea(Double.valueOf(remuneracion.doubleValue() * remuneraciones));
    }

    /**
     * Agrega un rubro al finiquito, localizando su concepto por codigo alterno.
     *
     * <p><b>Es el unico sitio del modulo que localiza un concepto por <code>CPNMALTR</code>.</b>
     * Lo fija el §8 del plan, y el script 08 deja los rubros de liquidacion sin
     * <code>CPNMROLM</code> a proposito. Si se decide darles rol, este es el unico metodo que
     * cambia.</p>
     *
     * @param rubros	: Lista de rubros
     * @param conceptos	: Catalogo de conceptos de la empresa
     * @param alterno	: Codigo alterno del concepto
     * @param base		: Base del calculo
     * @param cantidad	: Dias o anios considerados
     * @param valor		: Importe
     */
    private void agrega(List<RenglonCalculado> rubros, List<ConceptoNomina> conceptos, int rolMotor,
            Double base, Double cantidad, Double valor) {
        ConceptoNomina concepto = conceptoPorRol(conceptos, rolMotor);
        RenglonCalculado rubro = new RenglonCalculado();
        // El DTO lleva el codigo ALTERNO, no la PK: es lo que pone el motor en
        // armaResultado, y RenglonCalculado lo comparten los dos. Si aqui fuera la PK, la
        // misma propiedad significaria cosas distintas segun quien la produjo.
        rubro.setCodigoConcepto(concepto != null ? concepto.getCodigoAlterno() : null);
        rubro.setNombreConcepto(concepto != null ? concepto.getNombre() : "Rubro con rol " + rolMotor);
        rubro.setTipoConcepto(concepto != null ? concepto.getTipoConcepto()
                : Long.valueOf(RhhTipoConceptoNomina.INGRESO));
        rubro.setBase(base);
        rubro.setCantidad(cantidad);
        rubro.setValor(valor);
        rubro.setOrden(Integer.valueOf(rubros.size() + 1));
        rubros.add(rubro);
    }

    /**
     * Variante de <code>agrega</code> que recibe la cantidad como primitivo.
     *
     * @param rubros	: Lista de rubros
     * @param conceptos	: Catalogo de conceptos
     * @param alterno	: Codigo alterno
     * @param base		: Base del calculo
     * @param cantidad	: Cantidad
     * @param valor		: Importe
     */
    private void agrega(List<RenglonCalculado> rubros, List<ConceptoNomina> conceptos, int rolMotor,
            Double base, double cantidad, Double valor) {
        agrega(rubros, conceptos, rolMotor, base, Double.valueOf(cantidad), valor);
    }

    /**
     * Porcentaje del aporte personal, con la misma precedencia que usa el motor mensual en
     * <code>porcentajeVigente</code>: <b>manda <code>CPNMPRCN</code>, con caida a la
     * parametria del anio</b>.
     *
     * <p>El concepto del script 22 nace con <code>CPNMPRCN</code> en <b>NULL</b> a proposito,
     * de modo que hoy el valor sale de <code>PRNM.aportePersonal</code> y sigue solo un cambio
     * de ley. Escribir 9,45 en el catalogo obligaria a acordarse de dos sitios, y el mensual y
     * el finiquito podrian quedar con tasas distintas sin que nada avisara. La anulacion por
     * empresa queda disponible sin estar usada.</p>
     *
     * <p>No se reutiliza <code>porcentajeEnParametria</code> del motor: es privado, el motor
     * esta congelado, y su tabla de roles no contempla el 31 --devolveria null--. El aporte del
     * finiquito es la misma tasa normativa que el del rol mensual, asi que su respaldo es
     * <code>PRNMAPPR</code>.</p>
     *
     * @param concepto	: Concepto del aporte, ya localizado por su rol
     * @param prnm		: Parametros del anio
     * @return			: El porcentaje vigente, o null si no hay ninguno
     */
    private Double porcentajeAporte(ConceptoNomina concepto, ParametroNomina prnm) {
        if (concepto != null && concepto.getPorcentaje() != null) {
            return concepto.getPorcentaje();
        }
        return prnm != null ? prnm.getAportePersonal() : null;
    }

    /**
     * Localiza un concepto por su rol del motor dentro del catalogo ya cargado.
     *
     * <p>Desde el script 17 los ocho rubros del finiquito tienen rol propio (23 a 30), asi
     * que <b>ya no queda ningun <code>CPNMALTR</code> literal en el modulo</b>. El indice
     * <code>UQ_CPNM_ROLM</code> garantiza que solo un concepto por empresa reclame cada rol.</p>
     *
     * @param conceptos	: Catalogo
     * @param rolMotor	: Detalle del rubro RHH_ROL_CONCEPTO_MOTOR
     * @return			: El concepto, o null
     */
    private ConceptoNomina conceptoPorRol(List<ConceptoNomina> conceptos, int rolMotor) {
        if (conceptos == null) {
            return null;
        }
        for (ConceptoNomina concepto : conceptos) {
            if (concepto.getRolMotor() != null
                    && concepto.getRolMotor().longValue() == rolMotor) {
                return concepto;
            }
        }
        return null;
    }

    /**
     * Suma el saldo pendiente de los descuentos recurrentes vigentes del empleado.
     *
     * @param idEmpleado	: Id del empleado
     * @return				: Saldo total
     * @throws Throwable	: Excepcion
     */
    private Double saldoDescuentos(Long idEmpleado) throws Throwable {
        Double total = Double.valueOf(0D);
        List<DescuentoRecurrente> descuentos = descuentoRecurrenteDaoService
                .selectVigentesByEmpleado(idEmpleado);
        if (descuentos == null) {
            return total;
        }
        for (DescuentoRecurrente descuento : descuentos) {
            if (descuento.getSaldo() != null && descuento.getSaldo().doubleValue() > 0D) {
                total = RedondeoNomina.suma(total, descuento.getSaldo());
            }
        }
        return total;
    }

    /**
     * Cancela los descuentos recurrentes vigentes del empleado.
     *
     * @param idEmpleado	: Id del empleado
     * @param usuario		: Usuario que ejecuta
     * @return				: Numero de descuentos cancelados
     * @throws Throwable	: Excepcion
     */
    private int cancelaDescuentos(Long idEmpleado, String usuario) throws Throwable {
        List<DescuentoRecurrente> descuentos = descuentoRecurrenteDaoService
                .selectVigentesByEmpleado(idEmpleado);
        if (descuentos == null) {
            return 0;
        }
        int cancelados = 0;
        for (DescuentoRecurrente descuento : descuentos) {
            descuento.setSaldo(Double.valueOf(0D));
            descuento.setFechaFin(LocalDate.now());
            descuento.setObservacion(concatena(descuento.getObservacion(),
                    "Cancelado por liquidacion de haberes (" + usuario + ")"));
            em.merge(descuento);
            cancelados++;
        }
        return cancelados;
    }

    /**
     * Caduca los saldos de vacaciones del empleado: ya se pagaron en el finiquito.
     *
     * @param idEmpleado	: Id del empleado
     * @return				: Numero de saldos caducados
     */
    /**
     * Escribe los acumulados del finiquito.
     *
     * <p><b>Sin esto, quien sale por liquidacion no existe para el RDEP.</b> Los acumulados
     * solo se escribian al cerrar un periodo, y el mes en que alguien se va su finiquito no
     * pasa por ninguna nomina: Torres Chavez cobro 7.556,41 en enero de 2026 y para el
     * declarativo del SRI era como si no hubiera cobrado nada. El finiquito es la ultima
     * cosa que le pagamos y tiene que dejar la misma huella que un mes normal.</p>
     *
     * <p><b>Que se escribe y de donde sale, sin inventar reglas:</b> el gravado de IR es la
     * suma de los renglones de ingreso cuyo concepto esta marcado como gravado
     * --<code>CPNMIMIR</code>; hoy son la remuneracion pendiente y las vacaciones no
     * gozadas, y las indemnizaciones y los decimos quedan fuera, que es lo correcto--; el
     * aporte personal es el renglon del rol 31; y el imponible del IESS es la base sobre la
     * que ese aporte se calculo, no el total del finiquito.</p>
     *
     * <p><b>Idempotente</b>, como el cierre: se busca el acumulado de esa clave y se
     * actualiza. Ejecutar la salida dos veces no duplica --hoy no se puede, pero un reverso
     * y una reejecucion si--.</p>
     *
     * <p>Falla sin ruido y deja traza: no puede impedir una salida ya aprobada. Lo que no se
     * escribio se ve en el RDEP, que es donde importa.</p>
     *
     * @param liquidacion	: Liquidacion ya ejecutada
     * @param usuario		: Usuario que ejecuta
     * @return				: Cuantos acumulados se escribieron
     */
    private int escribeAcumuladosDelFiniquito(Liquidacion liquidacion, String usuario) {
        int escritos = 0;
        try {
            Empleado empleado = liquidacion.getEmpleado();
            LocalDate salida = liquidacion.getFechaSalida();
            if (empleado == null || salida == null) {
                return 0;
            }
            Integer anio = Integer.valueOf(salida.getYear());
            Integer mes = Integer.valueOf(salida.getMonthValue());

            double gravadoIr = 0D;
            double aportePersonal = 0D;
            double baseAporte = 0D;

            for (DetalleLiquidacion detalle : detalleLiquidacionDaoService.selectByLiquidacion(
                    liquidacion.getCodigo())) {
                ConceptoNomina concepto = detalle.getConceptoNomina();
                Double valor = detalle.getValor() != null ? detalle.getValor() : Double.valueOf(0D);
                if (concepto == null) {
                    continue;
                }
                boolean esIngreso = Long.valueOf(RhhTipoConceptoNomina.INGRESO)
                        .equals(detalle.getTipoConcepto());
                if (esIngreso && SI.equals(concepto.getImponibleIr())) {
                    gravadoIr += valor.doubleValue();
                }
                if (concepto.getRolMotor() != null
                        && concepto.getRolMotor().longValue() == RhhRolConceptoMotor.FINIQUITO_APORTE_PERSONAL) {
                    aportePersonal += valor.doubleValue();
                    if (detalle.getBaseCalculo() != null) {
                        baseAporte += detalle.getBaseCalculo().doubleValue();
                    }
                }
            }

            escritos += acumulaDelFiniquito(empleado, anio, mes, RhhTipoAcumulado.GRAVADO_IR,
                    RedondeoNomina.redondea(Double.valueOf(gravadoIr)), usuario);
            escritos += acumulaDelFiniquito(empleado, anio, mes, RhhTipoAcumulado.APORTE_PERSONAL,
                    RedondeoNomina.redondea(Double.valueOf(aportePersonal)), usuario);
            escritos += acumulaDelFiniquito(empleado, anio, mes, RhhTipoAcumulado.IMPONIBLE_IESS,
                    RedondeoNomina.redondea(Double.valueOf(baseAporte)), usuario);
        } catch (Throwable e) {
            System.out.println("Aviso: no se pudieron escribir los acumulados del finiquito "
                    + liquidacion.getCodigo() + "; el empleado no aparecera en el RDEP del anio."
                    + " Detalle: " + e.getMessage());
        }
        return escritos;
    }

    /**
     * Graba o actualiza un acumulado del finiquito. Los ceros no se escriben: una fila en
     * cero no aporta nada y ensucia el contraste de acumulados.
     *
     * @param empleado	: Empleado
     * @param anio		: Anio de la salida
     * @param mes		: Mes de la salida
     * @param tipo		: Tipo de acumulado
     * @param valor		: Importe
     * @param usuario	: Usuario
     * @return			: 1 si se escribio, 0 si no
     */
    private int acumulaDelFiniquito(Empleado empleado, Integer anio, Integer mes, int tipo,
            Double valor, String usuario) {
        if (valor == null || valor.doubleValue() == 0D) {
            return 0;
        }
        try {
            AcumuladoNomina acumulado = acumuladoNominaDaoService.selectByClave(empleado.getCodigo(),
                    anio, mes, Long.valueOf(tipo));
            if (acumulado == null) {
                acumulado = new AcumuladoNomina();
                acumulado.setEmpleado(empleado);
                acumulado.setAnio(anio);
                acumulado.setMes(mes);
                acumulado.setTipoAcumulado(Long.valueOf(tipo));
                acumulado.setEstado(Long.valueOf(Estado.ACTIVO));
            }
            acumulado.setValor(valor);
            acumulado.setFechaRegistro(LocalDateTime.now());
            acumulado.setUsuarioRegistro(usuario);
            acumuladoNominaDaoService.save(acumulado, acumulado.getCodigo());
            return 1;
        } catch (Throwable e) {
            System.out.println("Aviso: no se pudo escribir el acumulado tipo " + tipo
                    + " del finiquito de " + empleado.getIdentificacion() + ": " + e.getMessage());
            return 0;
        }
    }

    private int caducaSaldosVacaciones(Long idEmpleado) {
        return em.createQuery(" update SaldoVacaciones t "
                + " set    t.caducado = 'S' "
                + " where  t.empleado.codigo = :idEmpleado "
                + "        and (t.caducado is null or t.caducado <> 'S') ")
                .setParameter("idEmpleado", idEmpleado)
                .executeUpdate();
    }

    /**
     * Localiza la liquidacion de un contrato y fecha, si ya existe.
     *
     * @param idContrato	: Id del contrato
     * @param fechaSalida	: Fecha de salida
     * @return				: La liquidacion, o null
     */
    @SuppressWarnings("unchecked")
    private Liquidacion localizaLiquidacion(Long idContrato, LocalDate fechaSalida) {
        List<Liquidacion> lista = em.createQuery(" select t "
                + " from   Liquidacion t "
                + " where  t.contratoEmpleado.codigo = :idContrato "
                + "        and t.fechaSalida = :fechaSalida ")
                .setParameter("idContrato", idContrato)
                .setParameter("fechaSalida", fechaSalida)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * Recupera la liquidacion y falla con mensaje explicito si no existe.
     *
     * @param idLiquidacion	: Id de la liquidacion
     * @return				: La liquidacion
     * @throws Throwable	: IncomeException si no existe
     */
    private Liquidacion recuperaLiquidacion(Long idLiquidacion) throws Throwable {
        Liquidacion liquidacion = liquidacionDaoService.selectById(idLiquidacion,
                NombreEntidadesRhh.LIQUIDACION);
        if (liquidacion == null) {
            throw new IncomeException("No existe la liquidacion " + idLiquidacion + ".");
        }
        return liquidacion;
    }

    /**
     * Arma el DTO de salida de la simulacion.
     *
     * @param liquidacion	: Liquidacion calculada
     * @param rubros		: Rubros del finiquito
     * @return				: El DTO
     */
    private ResultadoLiquidacion armaResultado(Liquidacion liquidacion, List<RenglonCalculado> rubros) {
        ResultadoLiquidacion resultado = new ResultadoLiquidacion();
        resultado.setIdEmpleado(liquidacion.getEmpleado() != null
                ? liquidacion.getEmpleado().getCodigo() : null);
        resultado.setFechaSalida(liquidacion.getFechaSalida());
        resultado.setCausal(liquidacion.getCausalTerminacion() != null
                ? liquidacion.getCausalTerminacion().getNombre() : null);
        resultado.setAniosServicio(liquidacion.getAniosServicio());
        resultado.setRubros(rubros);
        resultado.setTotalIngresos(liquidacion.getTotalIngresos());
        resultado.setTotalDescuentos(liquidacion.getTotalDescuentos());
        resultado.setNeto(liquidacion.getNeto());
        return resultado;
    }

    /**
     * Concatena una entrada a una observacion existente.
     *
     * @param actual	: Observacion actual
     * @param entrada	: Entrada nueva
     * @return			: La observacion con la entrada al final
     */
    private String concatena(String actual, String entrada) {
        if (actual == null || actual.trim().isEmpty()) {
            return entrada;
        }
        return actual + " | " + entrada;
    }

    /**
     * Localiza un concepto por el codigo alterno que el rubro ya lleva grabado.
     *
     * <p>No es una busqueda por valor de catalogo quemado: el alterno sale del propio concepto
     * que <code>agrega</code> localizo por rol un momento antes.</p>
     *
     * @param conceptos	: Catalogo
     * @param codigo		: Codigo alterno del concepto
     * @return			: El concepto, o null
     */
    private ConceptoNomina conceptoPorCodigo(List<ConceptoNomina> conceptos, Long codigo) {
        if (conceptos == null || codigo == null) {
            return null;
        }
        for (ConceptoNomina concepto : conceptos) {
            if (codigo.equals(concepto.getCodigoAlterno())) {
                return concepto;
            }
        }
        return null;
    }
}
