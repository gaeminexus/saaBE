package com.saa.ejb.rhh.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.cxp.service.dto.LineaContablePago;
import com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService;
import com.saa.ejb.rhh.dao.DetalleFormatoBancarioDaoService;
import com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.FormatoArchivoBancarioDaoService;
import com.saa.ejb.rhh.dao.NominaDaoService;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.dao.PeriodoNominaDaoService;
import com.saa.ejb.rhh.dao.ReglonNominaDaoService;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
import com.saa.ejb.tsr.dao.EgresoDaoService;
import com.saa.ejb.rhh.service.CierreCuotasDescuentoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.GeneracionOrdenPagoService;
import com.saa.ejb.rhh.util.RedondeoNomina;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.rhh.CuentaBancariaEmpleado;
import com.saa.model.rhh.DetalleFormatoBancario;
import com.saa.model.rhh.DetalleOrdenPagoNomina;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.FormatoArchivoBancario;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.rhh.ValorNoPagado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.tsr.BancoExterno;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.Egreso;
import com.saa.rubros.EstadoEgresoTesoreria;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RhhCampoArchivoBancario;
import com.saa.rubros.RhhEstadoDetalleOrdenPago;
import com.saa.rubros.RhhEstadoOrdenPago;
import com.saa.rubros.RhhEstadoValorNoPagado;
import com.saa.rubros.RhhFormatoArchivoMarcacion;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhModoPeriodoNomina;
import com.saa.rubros.RhhRolConceptoMotor;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 * @author GaemiSoft
 * <p>Implementacion de GeneracionOrdenPagoService.</p>
 *
 * <h3>El reparto entre cuentas</h3>
 *
 * <p>Un empleado puede dividir su sueldo entre varias cuentas con <code>CBEMPRCN</code>. El
 * reparto se calcula sobre el neto y <b>el residuo se acumula en la cuenta principal</b>: si
 * los porcentajes dan 333,33 + 333,33 + 333,33 sobre un neto de 1.000,00, el centavo que falta
 * va a la principal. Sin ese ajuste la suma del detalle no seria el neto y el asiento de pago
 * no cuadraria contra el rol.</p>
 *
 * <h3>El archivo bancario</h3>
 *
 * <p>El formato es dato: sale de <code>RHH.FMBN</code> y sus campos de <code>RHH.DFMB</code>,
 * espejo de salida de <code>FMRC</code>/<code>DFMR</code>. Si la empresa no tiene formato activo,
 * <code>generarArchivoBancario</code> dice que falta <b>crearlo</b>, no que falte codigo.</p>
 *
 * <p><b>El codigo de banco del campo <code>CODIGO_DEL_BANCO</code> no sale del snapshot.</b>
 * <code>DRPGBNCO</code> solo guarda el NOMBRE del banco, para auditoria de a que banco se ordeno
 * pagar aunque el empleado cambie de cuenta despues; ningun formato real acepta un nombre en ese
 * campo. El codigo se resuelve EN VIVO, en cada generacion, desde
 * <code>detalle.getCuentaBancariaEmpleado().getBanco().getTarjeta()</code>
 * (<code>CBEM -&gt; TSR.BEXT.BEXTTRJT</code>), que es el codigo de institucion financiera del BCE
 * pese a su nombre de columna --ver el javadoc de {@link com.saa.model.tsr.BancoExterno#getTarjeta()}--.
 * Si la cuenta, el banco o el codigo faltan, <code>generarArchivoBancario</code> revienta
 * nombrando al beneficiario en vez de mandar el campo vacio o el nombre: en el Banco Internacional
 * un campo 12 vacio significa "cuenta del propio Internacional" y acredita a otro banco distinto
 * del que corresponde (<code>docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md</code> §1.4,
 * trampa 3).</p>
 *
 * <h3>El egreso de tesoreria</h3>
 *
 * <p><code>confirmar</code> crea el <code>TSR.EGRS</code> consolidado y lo enlaza en
 * <code>RDPG.EGRSCDGO</code>, que es lo que permite a la conciliacion bancaria casar el pago
 * con el extracto. Va <b>con titular en nulo</b> --legitimo: el archivo bancario sale de
 * <code>DRPG</code>, no del titular, y la base lo admite-- y con el producto de pago tecnico
 * "PAGO DE NOMINA", localizado <b>por su codigo</b> y nunca por id.</p>
 */
@Stateless
public class GeneracionOrdenPagoServiceImpl implements GeneracionOrdenPagoService {

    /** Bandera de cuenta principal del empleado. */
    private static final String SI = "S";

    /** Bandera de detalle no rechazado. */
    private static final String NO = "N";

    /**
     * Codigo del producto de pago con el que se clasifica el egreso de nomina.
     *
     * <p>Es una <b>clave de busqueda</b>, no un valor normativo: no describe ninguna regla
     * de negocio ni cambia con la ley. Lo crea sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql.</p>
     */
    private static final String CODIGO_PRODUCTO_NOMINA = "NOMINA";

    /** Fin de linea del archivo bancario. */
    private static final String SALTO_LINEA = "\r\n";

    /** Lado de relleno por la izquierda. */
    private static final String LADO_IZQUIERDO = "I";

    /** Patron de fecha cuando ni el campo ni el formato lo declaran. */
    private static final String FORMATO_FECHA_POR_DEFECTO = "ddMMyyyy";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private OrdenPagoNominaDaoService ordenPagoNominaDaoService;

    @EJB
    private DetalleOrdenPagoNominaDaoService detalleOrdenPagoNominaDaoService;

    @EJB
    private PeriodoNominaDaoService periodoNominaDaoService;

    @EJB
    private NominaDaoService nominaDaoService;

    @EJB
    private CuentaBancariaEmpleadoDaoService cuentaBancariaEmpleadoDaoService;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @EJB
    private FormatoArchivoBancarioDaoService formatoArchivoBancarioDaoService;

    @EJB
    private DetalleFormatoBancarioDaoService detalleFormatoBancarioDaoService;

    @EJB
    private EgresoDaoService egresoDaoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
    // Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §7.2.
    @EJB
    private ValorNoPagadoDaoService valorNoPagadoDaoService;

    @EJB
    private ReglonNominaDaoService reglonNominaDaoService;
    // ===== FIN enganche valores no pagados =====

    // ===== INICIO nomina por empleado (docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md) =====
    /**
     * Mismo motivo que {@code PagoPensionComplementariaServiceImpl.self}: un metodo privado de
     * este bean corre en la transaccion de quien lo llama, y {@code sincronizaUnDetalle} necesita
     * {@code REQUIRES_NEW} de verdad -- solo el proxy del EJB lo honra, nunca {@code this.metodo()}.
     */
    @EJB
    private GeneracionOrdenPagoService self;

    @EJB
    private CierreCuotasDescuentoService cierreCuotasDescuentoService;
    // ===== FIN nomina por empleado =====

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generar(java.lang.Long, java.lang.Long, java.lang.String, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenPagoNomina generar(Long idPeriodoNomina, Long idCuentaBancaria, String usuario,
            Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo generar de generacionOrdenPago service, periodo: "
                + idPeriodoNomina + ", cuenta: " + idCuentaBancaria);

        PeriodoNomina periodo = recuperaPeriodo(idPeriodoNomina);
        exigeEstadoPagable(periodo);

        List<Nomina> nominas = nominaDaoService.selectByPeriodo(idPeriodoNomina);
        if (nominas == null || nominas.isEmpty()) {
            throw new IncomeException("El periodo " + idPeriodoNomina
                    + " no tiene nominas calculadas: no hay nada que pagar.");
        }

        OrdenPagoNomina orden = localizaOrdenReutilizable(idPeriodoNomina);
        if (orden == null) {
            orden = new OrdenPagoNomina();
            orden.setPeriodoNomina(periodo);
            orden.setEmpresa(periodo.getEmpresa());
            orden.setFechaRegistro(LocalDateTime.now());
            orden.setUsuarioRegistro(usuario);
        } else {
            // Nomina por empleado: cada DRPG es el idOrigen de su propio PagoProgramado
            // (RHH_NOMINA_EMPLEADO). Regenerar el detalle sin esta guarda borraria filas DRPG
            // que algun pago vivo en tesoreria todavia referencia por id -- las huerfanaria sin
            // avisar. Si ya no queda ningun pago vivo (todos RECHAZADO/ANULADO, o la orden es
            // del camino viejo y nunca tuvo pagos por empleado), se puede rehacer el detalle
            // igual que siempre, para que un cambio de cuenta bancaria del empleado se refleje.
            if (tieneAlgunDrpgConPagoVivo(orden.getCodigo())) {
                throw new IncomeException("La orden de pago " + orden.getCodigo() + " ya tiene pagos de"
                        + " empleados vigentes en tesoreria: no se puede regenerar sin perder su rastro."
                        + " Use 'Actualizar pagos' para seguir el estado de cada empleado, o 'Reenviar'"
                        + " para corregir uno rechazado.");
            }
            detalleOrdenPagoNominaDaoService.eliminaByOrdenPago(orden.getCodigo());
        }

        if (idCuentaBancaria != null) {
            CuentaBancaria cuenta = em.find(CuentaBancaria.class, idCuentaBancaria);
            if (cuenta == null) {
                throw new IncomeException("No existe la cuenta bancaria " + idCuentaBancaria + ".");
            }
            orden.setCuentaBancaria(cuenta);
        }
        orden.setNumero(armaNumero(periodo));
        orden.setFechaEmision(LocalDate.now());
        orden.setEstado(Long.valueOf(RhhEstadoOrdenPago.GENERADA));
        // Se siembran para poder grabar la cabecera antes del detalle, igual que hace el motor
        // con NMNA; al final se sobrescriben con los valores reales.
        orden.setTotal(Double.valueOf(0D));
        orden.setNumeroEmpleados(Integer.valueOf(0));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());
        em.flush();

        // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
        // Periodo P-1, calculado una sola vez fuera del bucle. Ver §7.2 del plan.
        PeriodoNomina periodoAnterior = (periodo.getEmpresa() != null && periodo.getFechaInicio() != null)
                ? periodoNominaDaoService.selectByFechaEmpresa(
                        periodo.getEmpresa().getCodigo(), periodo.getFechaInicio().minusDays(1))
                : null;
        // ===== FIN enganche valores no pagados =====

        Double total = Double.valueOf(0D);
        int empleados = 0;
        for (Nomina nomina : nominas) {
            Double neto = nomina.getNetoPagar();
            if (neto == null || neto.doubleValue() <= 0D) {
                // Un neto en cero o negativo no se acredita. El negativo ya lo bloquea el
                // motor; el cero es real --una licencia sin sueldo el mes entero-- y
                // simplemente no genera linea.
                System.out.println("Empleado " + (nomina.getEmpleado() != null
                        ? nomina.getEmpleado().getIdentificacion() : "?")
                        + " con neto " + neto + ": no entra en la orden de pago.");
                continue;
            }

            // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
            neto = ajustaNetoPorValorNoPagado(nomina, neto, periodo, periodoAnterior, orden);
            // ===== FIN enganche valores no pagados =====

            List<DetalleOrdenPagoNomina> detalles = armaDetalle(orden, nomina, neto, usuario);
            for (DetalleOrdenPagoNomina detalle : detalles) {
                detalleOrdenPagoNominaDaoService.save(detalle, detalle.getCodigo());
                total = RedondeoNomina.suma(total, detalle.getValor());
            }
            empleados++;
        }

        if (empleados == 0) {
            throw new IncomeException("Ningun empleado del periodo " + idPeriodoNomina
                    + " tiene neto por acreditar: no se emite orden de pago.");
        }

        orden.setTotal(total);
        orden.setNumeroEmpleados(Integer.valueOf(empleados));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        if (esHistorico(periodo)) {
            // Mismo interruptor que ContabilizacionNominaServiceImpl: un periodo historico
            // carga datos ya pagados fuera del sistema, no genera contabilidad nueva
            // (contabilizarPago no-opera para el), y por lo mismo no le corresponde pasar
            // por la bandeja de aprobacion de tesoreria: no hay ningun pago real que aprobar.
            System.out.println("Periodo " + idPeriodoNomina + " en modo HISTORICO: la orden de pago"
                    + " no se registra en la bandeja de tesoreria.");
        } else {
            registraPagoEnBandeja(orden, idUsuario);
        }

        System.out.println("Orden de pago " + orden.getCodigo() + " generada por " + total
                + " para " + empleados + " empleado(s).");
        return orden;
    }

    /**
     * Registra UN pago por cada DRPG de la orden en la bandeja de aprobacion de tesoreria,
     * como los jubilados -- docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md §3.1.
     * Reemplaza al pago consolidado RHH_NOMINA (que sigue existiendo, intacto, para las
     * ordenes viejas que ya lo tienen -- {@link #confirmar} y {@link #generarArchivoBancario}
     * las distinguen por eso).
     *
     * <p>Cada linea lleva su propio desglose contable con el producto NOMINA: a diferencia del
     * pago consolidado (sin desglose, porque RRHH contabilizaba aparte), aqui es tesoreria
     * quien genera el asiento de cada pago al confirmarlo (N3), con el camino generico que ya
     * usa un desglose para armar el DEBE.</p>
     *
     * <p>Todo o nada: corre dentro de la transaccion de {@link #generar}, asi que si un
     * empleado falla (p.ej. el producto NOMINA no existe para la empresa) no queda ningun
     * pago nuevo registrado, ni de este ni de los que ya se hubieran procesado en esta misma
     * pasada.</p>
     *
     * <p>Idempotente por DRPG: un detalle con un pago vivo (cualquier estado salvo RECHAZADO o
     * ANULADO) no se vuelve a registrar. En la practica, dado que regenerar una orden con algun
     * pago vivo ya esta bloqueado mas arriba en {@link #generar} (DRPG huerfanos), esta guarda
     * nunca deberia dispararse hoy -- se deja igual, explicitamente pedida por el contrato,
     * como defensa si ese bloqueo cambiara.</p>
     *
     * @param orden			: Orden de pago ya guardada, con el detalle (DRPG) ya persistido
     * @param idUsuario		: Id de SCP.PJRQ del usuario que ejecuta, FK real que exige
     *						  registrarPagoDeOrigenExterno — nunca se resuelve por nombre, ver
     *						  el Javadoc de {@link com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generar}
     * @throws Throwable	: IncomeException si falta empresa/idUsuario, o si un empleado falla
     *						  (nombrandolo)
     */
    private void registraPagoEnBandeja(OrdenPagoNomina orden, Long idUsuario) throws Throwable {
        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        if (idEmpresa == null) {
            throw new IncomeException("La orden de pago " + orden.getCodigo() + " no tiene empresa:"
                    + " sin ella no se puede registrar el pago en la bandeja de tesoreria.");
        }
        exigeIdUsuario(idUsuario, "generar");

        List<DetalleOrdenPagoNomina> detalles = detalleOrdenPagoNominaDaoService
                .selectByOrdenPago(orden.getCodigo());
        if (detalles == null || detalles.isEmpty()) {
            return;
        }

        Long idProductoNomina = idProductoNomina(idEmpresa);
        String periodoTexto = orden.getPeriodoNomina().getMes() + "/" + orden.getPeriodoNomina().getAnio();

        for (DetalleOrdenPagoNomina detalle : detalles) {
            try {
                registraPagoDeUnDetalle(orden, detalle, idProductoNomina, periodoTexto, idUsuario);
            } catch (Throwable e) {
                throw new IncomeException("No se pudo registrar el pago del empleado "
                        + detalle.getNombreBeneficiario() + " (" + detalle.getIdentificacion() + "), detalle "
                        + detalle.getCodigo() + " de la orden " + orden.getCodigo() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Registra el pago de UN DRPG. Ver {@link #registraPagoEnBandeja}.
     */
    private void registraPagoDeUnDetalle(OrdenPagoNomina orden, DetalleOrdenPagoNomina detalle,
            Long idProductoNomina, String periodoTexto, Long idUsuario) throws Throwable {
        if (tienePagoVivoPorDrpg(detalle.getCodigo())) {
            System.out.println("El detalle " + detalle.getCodigo() + " ya tiene un pago vivo en la"
                    + " bandeja de tesoreria: no se registra otro.");
            return;
        }

        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre(detalle.getNombreBeneficiario());
        beneficiario.setIdentificacion(detalle.getIdentificacion());
        if (detalle.getCuentaBancariaEmpleado() != null && detalle.getCuentaBancariaEmpleado().getBanco() != null) {
            beneficiario.setIdBancoExterno(detalle.getCuentaBancariaEmpleado().getBanco().getCodigo());
        }
        beneficiario.setTipoCuenta(detalle.getTipoCuenta());
        beneficiario.setNumeroCuenta(detalle.getNumeroCuenta());

        LineaContablePago linea = new LineaContablePago();
        linea.setIdProductoPago(idProductoNomina);
        linea.setValor(detalle.getValor());
        linea.setConcepto("Nomina " + periodoTexto);
        List<LineaContablePago> desglose = new ArrayList<LineaContablePago>();
        desglose.add(linea);

        String observacion = "Nomina " + periodoTexto + " - " + detalle.getNombreBeneficiario();

        Map<String, Object> resultado = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.RHH_NOMINA_EMPLEADO, detalle.getCodigo(), orden.getEmpresa().getCodigo(),
                null, detalle.getValor(),
                orden.getFechaEmision() != null ? orden.getFechaEmision().toString() : null,
                beneficiario, desglose, observacion,
                idUsuario, false, orden.getNumero());

        System.out.println("Pago del detalle " + detalle.getCodigo() + " (empleado "
                + detalle.getNombreBeneficiario() + ") de la orden " + orden.getCodigo()
                + " registrado en la bandeja de tesoreria: idPago=" + resultado.get("pago")
                + ", estado=" + resultado.get("estado"));
    }

    /**
     * Indica si el DRPG ya tiene un pago vivo (cualquier estado salvo RECHAZADO o ANULADO) en
     * <code>PGS.PGTR</code> para el origen <code>RHH_NOMINA_EMPLEADO</code>. Analogo a
     * {@link #tienePagoVivoEnBandeja}, pero por detalle en vez de por orden.
     *
     * @param idDetalle		: Codigo del detalle (RHH.DRPG.DRPGCDGO)
     * @return				: true si ya existe un pago que no esta RECHAZADO ni ANULADO
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private boolean tienePagoVivoPorDrpg(Long idDetalle) throws Throwable {
        List<PagoProgramado> vivos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen = :idOrigen "
                + "          and p.estado <> :rechazado "
                + "          and p.estado <> :anulado ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA_EMPLEADO)
                .setParameter("idOrigen", idDetalle)
                .setParameter("rechazado", Long.valueOf(EstadoPagoProgramado.RECHAZADO))
                .setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
                .getResultList();
        return !vivos.isEmpty();
    }

    /**
     * Indica si ALGUN DRPG de la orden tiene un pago vivo bajo RHH_NOMINA_EMPLEADO. La usa la
     * guarda de regeneracion en {@link #generar}: ver la nota en ese metodo.
     *
     * @param idOrdenPago	: Codigo de la orden de pago
     * @return				: true si algun DRPG de la orden tiene un pago vivo
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private boolean tieneAlgunDrpgConPagoVivo(Long idOrdenPago) throws Throwable {
        List<Long> idsDrpg = em.createQuery(" select   d.codigo "
                + " from     DetalleOrdenPagoNomina d "
                + " where    d.ordenPagoNomina.codigo = :idOrdenPago ")
                .setParameter("idOrdenPago", idOrdenPago)
                .getResultList();
        if (idsDrpg.isEmpty()) {
            return false;
        }
        // IN (:ids) con una lista en Java, no una subconsulta correlacionada: mismo patron ya
        // probado en este proyecto (ValorNoPagadoDaoServiceImpl.selectVivosByEmpleado, "and
        // t.estado in (:estados)").
        List<PagoProgramado> vivos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen in (:idsDrpg) "
                + "          and p.estado <> :rechazado "
                + "          and p.estado <> :anulado ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA_EMPLEADO)
                .setParameter("idsDrpg", idsDrpg)
                .setParameter("rechazado", Long.valueOf(EstadoPagoProgramado.RECHAZADO))
                .setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
                .getResultList();
        return !vivos.isEmpty();
    }

    /**
     * Indica si la orden es del camino NUEVO (nomina por empleado): nace sin ningun pago
     * consolidado RHH_NOMINA. Lo usan {@link #confirmar} y {@link #generarArchivoBancario}
     * (§3.5/§3.6 del contrato) para responder distinto segun el camino.
     *
     * @param idOrdenPago	: Codigo de la orden de pago
     * @return				: true si la orden NUNCA tuvo un pago RHH_NOMINA (nueva); false si
     *						  tiene uno, vivo o muerto (vieja)
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private boolean esOrdenNueva(Long idOrdenPago) throws Throwable {
        List<PagoProgramado> pagos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen = :idOrigen ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA)
                .setParameter("idOrigen", idOrdenPago)
                .setMaxResults(1)
                .getResultList();
        return pagos.isEmpty();
    }

    /**
     * Localiza el id del producto de pago de nomina por su codigo dentro de la empresa.
     * Mismo patron que <code>PlanillaIessServiceImpl.buscaProductoPago</code>
     * (docs/logica-negocio/rhh/API-PLANILLA-IESS.md), citado por el contrato -- se escribe
     * aparte en vez de reusar {@link #localizaProductoNomina} (que ya existe en esta misma
     * clase para el egreso consolidado) porque ese otro devuelve la ENTIDAD completa con un
     * mensaje de error propio del egreso, y aqui solo hace falta el id para
     * {@link LineaContablePago#setIdProductoPago}, con un mensaje que nombra el pago por
     * empleado en vez del egreso consolidado.
     *
     * @param idEmpresa		: Id de la empresa
     * @return				: El id del producto
     * @throws Throwable	: IncomeException si no existe
     */
    private Long idProductoNomina(Long idEmpresa) throws Throwable {
        try {
            return (Long) em.createQuery("select p.id from ProductoPago p where p.codigo = :codigo "
                    + "and p.empresa.codigo = :idEmpresa")
                    .setParameter("codigo", CODIGO_PRODUCTO_NOMINA)
                    .setParameter("idEmpresa", idEmpresa)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IncomeException("No existe el producto de pago con codigo '" + CODIGO_PRODUCTO_NOMINA
                    + "' para la empresa " + idEmpresa + ". Ejecute sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql:"
                    + " el pago por empleado necesita su id para el desglose contable.");
        }
    }

    /**
     * Indica si la orden ya tiene un pago vivo (cualquier estado salvo RECHAZADO o ANULADO) en
     * <code>PGS.PGTR</code> para el origen <code>RHH_NOMINA</code>.
     *
     * <p>Reimplementa la condicion en vez de llamar a
     * <code>PagoProgramadoDaoService.selectVigentesByOrigen</code>: al escribirse (2026-09,
     * docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md #11) esa consulta excluia
     * <code>POR_APROBAR</code> a proposito -no era "vigente" para el resto de los modulos
     * que la usan-, y aqui hacia falta detectar tambien el pago recien nacido POR_APROBAR
     * para no duplicarlo en una regeneracion de la orden. Desde el 2026-09-04
     * (docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md §22) esa exclusion se corrigio: hoy
     * <code>selectVigentesByOrigen</code> devuelve exactamente el mismo conjunto que esta
     * consulta -cualquier estado salvo <code>RECHAZADO</code> y <code>ANULADO</code>-.</p>
     * <p>Que coincidan HOY no las hace la misma consulta, y la reimplementacion sigue siendo
     * DELIBERADA, no un descuido pendiente de "limpiar": <code>selectVigentesByOrigen</code>
     * es la nocion de CXP de "pago vigente", que CXP puede cambiar cuando le convenga a su
     * propio criterio (como paso hoy mismo); <code>tienePagoVivoEnBandeja</code> es la nocion
     * de RHH de "esta orden ya tiene un pago vivo", una decision de negocio de este modulo.
     * Si rhh llamara al DAO de cxp, el proximo ajuste de esa consulta cambiaria el
     * comportamiento de la nomina EN SILENCIO. NO unificar aunque los conjuntos coincidan.</p>
     *
     * @param idOrdenPago	: Codigo de la orden de pago (RHH.RDPG.RDPGCDGO)
     * @return				: true si ya existe un pago que no esta RECHAZADO ni ANULADO
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private boolean tienePagoVivoEnBandeja(Long idOrdenPago) throws Throwable {
        List<PagoProgramado> vivos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen = :idOrigen "
                + "          and p.estado <> :rechazado "
                + "          and p.estado <> :anulado ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA)
                .setParameter("idOrigen", idOrdenPago)
                .setParameter("rechazado", Long.valueOf(EstadoPagoProgramado.RECHAZADO))
                .setParameter("anulado", Long.valueOf(EstadoPagoProgramado.ANULADO))
                .getResultList();
        return !vivos.isEmpty();
    }

    /**
     * Ultimo pago registrado en <code>PGS.PGTR</code> para un documento origen, SIN FILTRAR
     * por estado -a proposito, y no por una limitacion vieja de
     * <code>PagoProgramadoDaoService.selectVigentesByOrigen</code>-. Esa consulta (vigente al
     * 2026-09-04, docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md §22; antes tampoco traia
     * <code>POR_APROBAR</code>, ver #11) filtra por diseno: incluye <code>POR_APROBAR</code>
     * pero sigue excluyendo <code>RECHAZADO</code> y <code>ANULADO</code>, asi que nunca
     * podria traer el pago rechazado/anulado que <code>exigePagoConfirmadoEnTesoreria</code>
     * necesita ver para distinguir "hay que reintentar" de "hay que esperar". Es una consulta
     * compartida con otros equipos y sigue siendo correcta para sus propios llamadores (la
     * guarda anti-duplicados de <code>PagoProgramadoServiceImpl.registrarPagoDeOrigenExterno</code>);
     * simplemente no sirve para este caso, y no se toca. El mas reciente (<code>id</code> mayor)
     * es el que importa: una orden puede acumular pagos RECHAZADO/ANULADO de intentos previos
     * si se regenero despues de cada uno.
     *
     * @param origen		: Etiqueta de {@link OrigenPagoExterno}
     * @param idOrigen		: Id del documento de origen
     * @return				: El pago mas reciente, o null si nunca se registro ninguno
     * @throws Throwable	: Excepcion
     */
    @SuppressWarnings("unchecked")
    private PagoProgramado ultimoPagoDeOrigen(String origen, Long idOrigen) throws Throwable {
        List<PagoProgramado> pagos = em.createQuery(" select   p "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen "
                + "          and p.idOrigen = :idOrigen "
                + " order by p.id desc ")
                .setParameter("origen", origen)
                .setParameter("idOrigen", idOrigen)
                .setMaxResults(1)
                .getResultList();
        return pagos.isEmpty() ? null : pagos.get(0);
    }

    /**
     * Exige que la orden tenga un pago CONFIRMADO en la bandeja de tesoreria antes de dejar
     * contabilizar. Sin esto la bandeja es decorativa: se podria contabilizar un pago que
     * tesoreria nunca aprobo.
     *
     * <p><b>Corregido (2026-09, docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md #11):</b> antes
     * usaba <code>selectVigentesByOrigen</code>, que en ese momento no traia
     * <code>POR_APROBAR</code> ni <code>RECHAZADO</code>/<code>ANULADO</code>. El control en
     * si funcionaba -lista vacia tambien lanzaba excepcion-, pero el mensaje mentia: decia
     * "no tiene ningun pago vigente" tanto si el pago estaba POR_APROBAR (hay que esperar)
     * como si estaba RECHAZADO o ANULADO (hay que volver a generar la orden, no esperar).
     * Ahora usa {@link #ultimoPagoDeOrigen} -sin filtrar por estado- y distingue las tres
     * situaciones que le importan al usuario: no existe ningun pago, existe y sigue en
     * tramite, o existe y el tramite termino sin pagar. El criterio es que el mensaje le diga
     * cual de las dos acciones tomar -esperar o reintentar-, no solo que algo esta mal.</p>
     * <p><b>Sigue sin poder usar <code>selectVigentesByOrigen</code> ni siquiera hoy</b>
     * (docs/logica-negocio/ESTADO-EQUIPO-OMEN-2.md §22, 2026-09-04): esa consulta ya incluye
     * <code>POR_APROBAR</code>, pero sigue excluyendo <code>RECHAZADO</code> y
     * <code>ANULADO</code> a proposito -es correcto para sus propios llamadores-, asi que
     * jamas devolveria el pago rechazado/anulado que este metodo necesita ver para elegir
     * "reintentar" en vez de "esperar".</p>
     *
     * @param idOrdenPago	: Codigo de la orden de pago
     * @throws Throwable	: IncomeException con el diagnostico exacto si no esta CONFIRMADO
     */
    private void exigePagoConfirmadoEnTesoreria(Long idOrdenPago) throws Throwable {
        PagoProgramado pago = ultimoPagoDeOrigen(OrigenPagoExterno.RHH_NOMINA, idOrdenPago);

        if (pago == null) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene ningun pago"
                    + " registrado en la bandeja de tesoreria (PGS.PGTR). Vuelva a generar la"
                    + " orden para registrarlo.");
        }

        int estado = pago.getEstado() != null ? pago.getEstado().intValue() : -1;
        if (estado == EstadoPagoProgramado.CONFIRMADO) {
            return;
        }

        if (estado == EstadoPagoProgramado.RECHAZADO || estado == EstadoPagoProgramado.ANULADO) {
            // Existe, pero no hay nada que esperar: el circuito termino sin pagar. Es
            // accionable, no hay que esperar a tesoreria -volver a generar registra un pago
            // nuevo, porque tienePagoVivoEnBandeja no cuenta estos dos estados como vivos.
            throw new IncomeException("El pago " + pago.getId() + " de la orden " + idOrdenPago
                    + " fue " + (estado == EstadoPagoProgramado.RECHAZADO ? "RECHAZADO" : "ANULADO")
                    + " en tesoreria: no espere, vuelva a generar la orden de pago para registrar"
                    + " un pago nuevo.");
        }

        // POR_APROBAR, REGISTRADO o EN_ARCHIVO: existe y sigue vivo en el circuito, todavia no
        // llego a CONFIRMADO ni murio. La unica accion correcta es esperar.
        throw new IncomeException("El pago " + pago.getId() + " de la orden " + idOrdenPago
                + " esta en estado " + estado + ", no CONFIRMADO: sigue en tramite en tesoreria"
                + " (no fue rechazado ni anulado). Espere a que lo confirmen antes de"
                + " contabilizar.");
    }

    /**
     * Indica si el periodo esta en modo historico. Mismo interruptor y misma convencion de
     * null que <code>ContabilizacionNominaServiceImpl.esHistorico</code>: un modo nulo se
     * trata como historico, que es el valor que tienen los periodos creados antes de que
     * existiera la columna.
     *
     * @param periodo	: Periodo de nomina
     * @return			: true si el periodo no contabiliza
     */
    private boolean esHistorico(PeriodoNomina periodo) {
        return periodo.getModo() == null
                || Long.valueOf(RhhModoPeriodoNomina.HISTORICO_SIN_CONTABILIZAR).equals(periodo.getModo());
    }

    /**
     * Exige que <code>idUsuario</code> venga informado. NO se resuelve por nombre como
     * respaldo: es un error de integracion del cliente REST y tiene que verse como tal (ver
     * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #4.2 «El
     * idUsuario» — un <code>selectByNombre</code> sobre el texto de auditoria de RRHH rompia
     * <code>generar()</code> segun por donde se hubiera inicializado la sesion en el frontend).
     *
     * @param idUsuario		: Id de SCP.PJRQ recibido en el payload
     * @param operacion		: Nombre de la operacion, para el mensaje
     * @throws Throwable	: IncomeException si es null
     */
    private void exigeIdUsuario(Long idUsuario, String operacion) throws Throwable {
        if (idUsuario == null) {
            throw new IncomeException("Falta idUsuario para registrar el pago en tesoreria"
                    + " (operacion: " + operacion + ").");
        }
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#generarArchivoBancario(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public byte[] generarArchivoBancario(Long idOrdenPago) throws Throwable {
        System.out.println("Ingresa al metodo generarArchivoBancario de generacionOrdenPago service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = recuperaOrden(idOrdenPago);
        if (!esHistorico(orden.getPeriodoNomina()) && esOrdenNueva(idOrdenPago)) {
            // §3.6: esta orden se paga por empleado. El archivo lo genera Tesoreria al aprobar
            // el lote de pagos (uno por DRPG), no RRHH. Los periodos HISTORICOS nunca pasaron
            // por la bandeja (ni con RHH_NOMINA ni con RHH_NOMINA_EMPLEADO): esOrdenNueva los
            // clasificaria mal como "nuevos" si no se excluyeran aqui -- §3.1, sin cambios.
            throw new IncomeException("La orden de pago " + idOrdenPago + " se paga por empleado desde"
                    + " Tesoreria: el archivo bancario ya no lo genera RRHH. Apruebe los pagos en la"
                    + " bandeja de Tesoreria y use 'Actualizar pagos' para seguir el estado de cada"
                    + " empleado.");
        }
        if (orden.getEmpresa() == null || orden.getEmpresa().getCodigo() == null) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene empresa:"
                    + " sin ella no se puede resolver el formato del archivo bancario.");
        }

        FormatoArchivoBancario formato = formatoArchivoBancarioDaoService
                .selectActivoByEmpresa(orden.getEmpresa().getCodigo());
        if (formato == null) {
            // Ya no falta codigo: falta el dato. El mensaje lo dice asi a proposito, para que
            // nadie vuelva a buscar el formato dentro del programa.
            throw new IncomeException("La empresa no tiene un formato de archivo bancario activo"
                    + " (RHH.FMBN). Cree el formato del banco con sus campos (RHH.DFMB) y vuelva a"
                    + " intentarlo: el formato es dato, no codigo.");
        }

        List<DetalleFormatoBancario> campos = detalleFormatoBancarioDaoService
                .selectByFormato(formato.getCodigo());
        if (campos == null || campos.isEmpty()) {
            throw new IncomeException("El formato de archivo bancario '" + formato.getNombre()
                    + "' no tiene ningun campo definido (RHH.DFMB): no se sabe que escribir en cada"
                    + " linea.");
        }

        List<DetalleOrdenPagoNomina> detalles = detalleOrdenPagoNominaDaoService
                .selectByOrdenPago(idOrdenPago);
        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " no tiene detalle:"
                    + " no hay nada que escribir en el archivo.");
        }

        boolean anchoFijo = Long.valueOf(RhhFormatoArchivoMarcacion.ANCHO_FIJO)
                .equals(formato.getTipoFormato());
        String delimitador = formato.getDelimitador() != null ? formato.getDelimitador() : "";
        Map<Long, String> mapaTipoCuenta = leeMapaTipoCuenta(formato.getMapaTipoCuenta());
        LocalDate hoy = LocalDate.now();

        StringBuilder archivo = new StringBuilder();

        // Cabecera. Nula significa que este banco no la pide, no que falte configurarla.
        if (formato.getPlantillaCabecera() != null && !formato.getPlantillaCabecera().trim().isEmpty()) {
            archivo.append(resuelveMarcadores(formato.getPlantillaCabecera(), orden, formato,
                    detalles.size(), hoy)).append(SALTO_LINEA);
        }

        int secuencial = 0;
        for (DetalleOrdenPagoNomina detalle : detalles) {
            secuencial++;
            StringBuilder linea = new StringBuilder();
            for (int i = 0; i < campos.size(); i++) {
                DetalleFormatoBancario campo = campos.get(i);
                String valor = valorDelCampo(campo, detalle, orden, formato, secuencial,
                        mapaTipoCuenta, hoy);
                if (anchoFijo) {
                    linea.append(rellena(valor, campo));
                } else {
                    if (i > 0) {
                        linea.append(delimitador);
                    }
                    linea.append(recorta(valor, campo));
                }
            }
            archivo.append(linea).append(SALTO_LINEA);
        }

        // Pie.
        if (formato.getPlantillaPie() != null && !formato.getPlantillaPie().trim().isEmpty()) {
            archivo.append(resuelveMarcadores(formato.getPlantillaPie(), orden, formato,
                    detalles.size(), hoy)).append(SALTO_LINEA);
        }

        String codificacion = formato.getCodificacion() != null && !formato.getCodificacion().trim().isEmpty()
                ? formato.getCodificacion().trim() : StandardCharsets.UTF_8.name();
        Charset juego;
        try {
            juego = Charset.forName(codificacion);
        } catch (Throwable e) {
            throw new IncomeException("La codificacion '" + codificacion + "' del formato '"
                    + formato.getNombre() + "' no la reconoce esta maquina virtual. Use UTF-8,"
                    + " ISO-8859-1 o windows-1252.");
        }

        System.out.println("Archivo bancario de la orden " + idOrdenPago + " generado con el formato '"
                + formato.getNombre() + "': " + secuencial + " linea(s) de detalle, codificacion "
                + juego.name() + ".");
        return archivo.toString().getBytes(juego);
    }

    /**
     * Resuelve el valor de un campo del detalle segun lo que declara el rubro 224.
     *
     * @param campo				: Definicion del campo
     * @param detalle			: Linea de la orden de pago
     * @param orden				: Orden de pago
     * @param formato			: Formato del archivo
     * @param secuencial		: Numero de linea, base 1
     * @param mapaTipoCuenta	: Mapa del tipo de cuenta al codigo del banco
     * @param hoy				: Fecha de proceso
     * @return					: El valor ya formateado, sin relleno
     * @throws Throwable		: IncomeException si el campo no se reconoce
     */
    private String valorDelCampo(DetalleFormatoBancario campo, DetalleOrdenPagoNomina detalle,
            OrdenPagoNomina orden, FormatoArchivoBancario formato, int secuencial,
            Map<Long, String> mapaTipoCuenta, LocalDate hoy) throws Throwable {

        Long cual = campo.getCampo();
        if (cual == null) {
            throw new IncomeException("El campo de orden " + campo.getOrden() + " del formato '"
                    + formato.getNombre() + "' no dice que dato lleva (DFMBCMPO).");
        }
        int codigo = cual.intValue();

        switch (codigo) {
            case RhhCampoArchivoBancario.SECUENCIAL:
                return String.valueOf(secuencial);
            case RhhCampoArchivoBancario.IDENTIFICACION_DEL_BENEFICIARIO:
                return texto(detalle.getIdentificacion());
            case RhhCampoArchivoBancario.NOMBRE_DEL_BENEFICIARIO:
                return texto(detalle.getNombreBeneficiario());
            case RhhCampoArchivoBancario.NUMERO_DE_CUENTA:
                return texto(detalle.getNumeroCuenta());
            case RhhCampoArchivoBancario.TIPO_DE_CUENTA:
                return codigoTipoCuenta(detalle.getTipoCuenta(), mapaTipoCuenta);
            case RhhCampoArchivoBancario.CODIGO_DEL_BANCO:
                // El codigo BCE se resuelve EN VIVO desde la cuenta bancaria vigente del
                // empleado, nunca del snapshot: DRPGBNCO solo guarda el NOMBRE. Ver la nota
                // de la clase.
                return codigoBceDelBanco(detalle);
            case RhhCampoArchivoBancario.VALOR:
                return importe(detalle.getValor(), campo);
            case RhhCampoArchivoBancario.MONEDA:
                // El sistema opera en una sola moneda; el literal lo pone el formato.
                return texto(campo.getValorFijo());
            case RhhCampoArchivoBancario.REFERENCIA:
                return texto(orden.getNumero());
            case RhhCampoArchivoBancario.FECHA_DE_PROCESO:
                return fecha(hoy, campo, formato);
            case RhhCampoArchivoBancario.LITERAL_FIJO:
                return texto(campo.getValorFijo());
            default:
                throw new IncomeException("El campo " + codigo + " del formato '" + formato.getNombre()
                        + "' no corresponde a ningun detalle del rubro 224.");
        }
    }

    /**
     * Sustituye los marcadores de la plantilla de cabecera o de pie.
     *
     * @param plantilla	: Plantilla con marcadores
     * @param orden		: Orden de pago
     * @param formato	: Formato del archivo
     * @param contador	: Numero de lineas de detalle
     * @param hoy		: Fecha de proceso
     * @return			: La linea resuelta
     */
    private String resuelveMarcadores(String plantilla, OrdenPagoNomina orden,
            FormatoArchivoBancario formato, int contador, LocalDate hoy) {
        String patron = formato.getFormatoFecha() != null && !formato.getFormatoFecha().trim().isEmpty()
                ? formato.getFormatoFecha().trim() : FORMATO_FECHA_POR_DEFECTO;
        String resultado = plantilla;
        resultado = resultado.replace("{FECHA}", hoy.format(DateTimeFormatter.ofPattern(patron)));
        resultado = resultado.replace("{CONTADOR}", String.valueOf(contador));
        resultado = resultado.replace("{TOTAL}", orden.getTotal() != null
                ? RedondeoNomina.redondea(orden.getTotal()).toString() : "0");
        resultado = resultado.replace("{EMPRESA}", orden.getEmpresa() != null
                && orden.getEmpresa().getNombre() != null ? orden.getEmpresa().getNombre() : "");
        resultado = resultado.replace("{SECUENCIAL}", texto(orden.getNumero()));
        return resultado;
    }

    /**
     * Lee el mapa <code>alternoRubro199=codigoBanco</code> separado por punto y coma.
     *
     * @param crudo	: Contenido de FMBNMPTC
     * @return		: Mapa del tipo de cuenta al codigo del banco
     */
    private Map<Long, String> leeMapaTipoCuenta(String crudo) {
        Map<Long, String> mapa = new LinkedHashMap<Long, String>();
        if (crudo == null || crudo.trim().isEmpty()) {
            return mapa;
        }
        for (String pareja : crudo.split(";")) {
            String[] partes = pareja.split("=", 2);
            if (partes.length != 2 || partes[0].trim().isEmpty()) {
                continue;
            }
            try {
                mapa.put(Long.valueOf(partes[0].trim()), partes[1].trim());
            } catch (NumberFormatException e) {
                // Una pareja mal escrita se ignora en vez de abortar el archivo: el efecto
                // visible es que ese tipo de cuenta sale con su codigo alterno, que es
                // diagnosticable, y no que la orden entera deje de generarse.
                System.out.println("Pareja invalida en FMBNMPTC: '" + pareja + "', se ignora.");
            }
        }
        return mapa;
    }

    /**
     * Traduce el tipo de cuenta al codigo que espera el banco.
     *
     * @param tipoCuenta	: Detalle del rubro 199 grabado en el snapshot
     * @param mapa			: Mapa del formato
     * @return				: El codigo del banco, o el alterno si el mapa no lo cubre
     */
    private String codigoTipoCuenta(Long tipoCuenta, Map<Long, String> mapa) {
        if (tipoCuenta == null) {
            return "";
        }
        String codigo = mapa.get(tipoCuenta);
        return codigo != null ? codigo : tipoCuenta.toString();
    }

    /**
     * Resuelve el codigo de banco del BCE que exige el archivo, EN VIVO desde la cuenta
     * bancaria vigente del empleado, nunca del snapshot de la orden.
     *
     * <p><code>BEXTTRJT</code> es el codigo de institucion financiera del BCE pese a su
     * nombre de columna --ver el javadoc de {@link BancoExterno#getTarjeta()}--. Un banco sin
     * codigo cargado no puede resolverse con nada razonable: no hay valor seguro que devolver,
     * porque un campo vacio o el nombre del banco hacen que el Internacional acredite a la
     * cuenta equivocada sin avisar.</p>
     *
     * @param detalle	: Linea de la orden de pago
     * @return			: El codigo BCE del banco de la cuenta del empleado
     * @throws IncomeException	: Si la cuenta, el banco o el codigo BCE faltan
     */
    private String codigoBceDelBanco(DetalleOrdenPagoNomina detalle) throws IncomeException {
        CuentaBancariaEmpleado cuenta = detalle.getCuentaBancariaEmpleado();
        BancoExterno banco = cuenta != null ? cuenta.getBanco() : null;
        Long codigoBce = banco != null ? banco.getTarjeta() : null;
        if (codigoBce == null) {
            throw new IncomeException("No se puede generar el archivo: el banco '"
                    + texto(detalle.getBanco()) + "' de " + texto(detalle.getNombreBeneficiario())
                    + " (" + texto(detalle.getIdentificacion()) + ") no tiene código de institución"
                    + " BCE. Cárguelo en Tesorería → Bancos y vuelva a generar.");
        }
        return codigoBce.toString();
    }

    /**
     * Formatea un importe segun los decimales y el separador que pide el campo.
     *
     * @param valor	: Importe
     * @param campo	: Definicion del campo
     * @return		: El importe como texto
     */
    private String importe(Double valor, DetalleFormatoBancario campo) {
        double v = valor != null ? valor.doubleValue() : 0D;
        int decimales = campo.getDecimales() != null ? campo.getDecimales().intValue() : 2;
        boolean conSeparador = SI.equals(campo.getIncluyeSeparadorDecimal());

        BigDecimal redondeado = BigDecimal.valueOf(v).setScale(decimales, RoundingMode.HALF_UP);
        if (conSeparador) {
            return redondeado.toPlainString();
        }
        // Sin separador: el importe va en unidades minimas --centavos corridos--, que es como
        // lo piden casi todos los formatos de acreditacion masiva.
        return redondeado.movePointRight(decimales).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    /**
     * Formatea una fecha con el patron del campo, o el del formato si el campo no trae uno.
     *
     * @param valor		: Fecha
     * @param campo		: Definicion del campo
     * @param formato	: Formato del archivo
     * @return			: La fecha como texto
     */
    private String fecha(LocalDate valor, DetalleFormatoBancario campo, FormatoArchivoBancario formato) {
        String patron = campo.getFormatoFecha() != null && !campo.getFormatoFecha().trim().isEmpty()
                ? campo.getFormatoFecha().trim()
                : (formato.getFormatoFecha() != null && !formato.getFormatoFecha().trim().isEmpty()
                        ? formato.getFormatoFecha().trim() : FORMATO_FECHA_POR_DEFECTO);
        return valor.format(DateTimeFormatter.ofPattern(patron));
    }

    /**
     * Rellena o recorta el valor a la longitud del campo, en formato de ancho fijo.
     *
     * <p>Un valor mas largo que la longitud se <b>recorta</b>, no se deja pasar: una linea mas
     * larga de lo debido descuadra todas las columnas siguientes y el banco rechaza el archivo
     * entero.</p>
     *
     * @param valor	: Valor ya formateado
     * @param campo	: Definicion del campo
     * @return		: El valor ajustado a la longitud
     */
    private String rellena(String valor, DetalleFormatoBancario campo) {
        if (campo.getLongitud() == null || campo.getLongitud().intValue() <= 0) {
            return valor;
        }
        int longitud = campo.getLongitud().intValue();
        String texto = valor != null ? valor : "";
        if (texto.length() >= longitud) {
            return texto.substring(0, longitud);
        }
        char relleno = campo.getCaracterRelleno() != null && !campo.getCaracterRelleno().isEmpty()
                ? campo.getCaracterRelleno().charAt(0) : ' ';
        StringBuilder paja = new StringBuilder();
        for (int i = texto.length(); i < longitud; i++) {
            paja.append(relleno);
        }
        // I rellena por la izquierda --lo habitual en importes y numeros de cuenta--, D por la
        // derecha, que es lo habitual en nombres.
        return LADO_IZQUIERDO.equals(campo.getLadoRelleno())
                ? paja.toString() + texto : texto + paja.toString();
    }

    /**
     * Recorta el valor a la longitud del campo, en formato DELIMITADO. Sin relleno: en
     * delimitado el separador ya marca donde termina cada campo, y rellenar agregaria
     * caracteres que el banco no espera.
     *
     * <p>La longitud tambien aplica aqui y no solo en ancho fijo: el Internacional acepta como
     * maximo 41 caracteres en el nombre del beneficiario y rechaza el archivo entero si algun
     * campo se pasa (<code>docs/logica-negocio/pagos/FORMATO-ARCHIVO-BANCOS.md</code> §1).</p>
     *
     * @param valor	: Valor ya formateado
     * @param campo	: Definicion del campo
     * @return		: El valor recortado a la longitud, o tal cual si no tiene longitud definida
     */
    private String recorta(String valor, DetalleFormatoBancario campo) {
        if (campo.getLongitud() == null || campo.getLongitud().intValue() <= 0) {
            return valor;
        }
        String texto = valor != null ? valor : "";
        int longitud = campo.getLongitud().intValue();
        return texto.length() > longitud ? texto.substring(0, longitud) : texto;
    }

    /**
     * Devuelve el texto o cadena vacia si es nulo.
     *
     * @param valor	: Texto
     * @return		: El texto o cadena vacia
     */
    private String texto(String valor) {
        return valor != null ? valor : "";
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#confirmar(java.lang.Long, java.time.LocalDate, java.lang.String)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenPagoNomina confirmar(Long idOrdenPago, LocalDate fechaAcreditacion, String usuario,
            Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo confirmar de generacionOrdenPago service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = recuperaOrden(idOrdenPago);
        if (!esHistorico(orden.getPeriodoNomina()) && esOrdenNueva(idOrdenPago)) {
            // §3.6: esta orden se paga por empleado, un pago por DRPG confirmado por separado
            // en Tesoreria -- no hay un solo "confirmar" para toda la orden. Los periodos
            // HISTORICOS siguen por este mismo confirmar(), sin cambios (§3.1): nunca tuvieron
            // ningun pago en la bandeja, ni RHH_NOMINA ni RHH_NOMINA_EMPLEADO, y esOrdenNueva
            // los clasificaria mal si no se excluyeran aqui.
            throw new IncomeException("Esta orden se paga por empleado: use 'Actualizar pagos' en vez"
                    + " de confirmar la orden completa.");
        }
        if (orden.getFechaAcreditacion() != null) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " ya se acredito el "
                    + orden.getFechaAcreditacion() + ".");
        }

        if (!esHistorico(orden.getPeriodoNomina())) {
            // Mismo criterio que en generar(): un periodo historico nunca paso por la bandeja,
            // asi que no hay pago que exigir confirmado ni idUsuario que pedir. contabilizarPago
            // tampoco emite asiento para el, mas abajo.
            exigeIdUsuario(idUsuario, "confirmar");
            exigePagoConfirmadoEnTesoreria(idOrdenPago);
        }

        // El asiento y la fecha los graba contabilizarPago, que respeta el interruptor del
        // modo historico. Aqui solo se marca el estado.
        contabilizacionNominaService.contabilizarPago(idOrdenPago, fechaAcreditacion, usuario);

        orden = recuperaOrden(idOrdenPago);
        orden.setEstado(Long.valueOf(RhhEstadoOrdenPago.CONFIRMADA));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        // El egreso va despues del asiento y con la orden ya confirmada: es el enlace que
        // la conciliacion bancaria necesita para casar el pago con el extracto.
        creaEgresoConsolidado(orden, usuario);

        System.out.println("Orden de pago " + idOrdenPago + " confirmada.");
        return orden;
    }

    // =====================================================================
    // Piezas
    // =====================================================================

    // ===== INICIO enganche valores no pagados (script e2-26, equipo omen-saa-2) =====
    /**
     * Ajusta el neto de un empleado por el ciclo de valores no pagados (§7.2 del plan):
     * resta la retencion del periodo P (si hay una viva, REGISTRADO o RETENIDO) y suma la
     * recuperacion del periodo P-1 (solo si esta RETENIDO). Marca el registro de P como
     * RETENIDO con {@code ordenRetencion = orden} -- idempotente: regenerar la orden de P
     * lo vuelve a marcar igual, sin duplicar nada porque el DAO reemplaza la fila.
     *
     * <p>Si el registro de P-1 esta en REGISTRADO (nunca se retuvo porque la orden de P-1
     * jamas se genero), no se adivina: se rechaza nombrando el registro, tal como pide el
     * plan -es una inconsistencia que el usuario resuelve, no algo que el sistema decida.</p>
     *
     * <p><b>Guarda de consistencia con el rol impreso.</b> Desde que registrar un valor no
     * pagado se permite tambien con el periodo CALCULADO (no solo ABIERTO,
     * {@code ValorNoPagadoServiceImpl.exigePeriodoModificable}), aparece un hueco: si nadie
     * recalcula el rol despues de registrar, esta orden leeria el VNPG igual y pagaria
     * {@code N - X}, pero el rol impreso seguiria mostrando N completo, sin la linea del
     * renglon informativo -- y la validacion "X <= neto" del motor (§7.1.3) nunca habria
     * corrido sobre este registro. Por eso, antes de aplicar cada ajuste, se exige que la
     * nomina YA tenga el renglon del rol correspondiente (34 para la retencion de P, 35 para
     * la recuperacion de P-1): si no lo tiene, el calculo esta desactualizado respecto del
     * VNPG y se rechaza en vez de aplicarlo a ciegas.</p>
     *
     * @param nomina			: Nomina del empleado en el periodo P, ya calculada
     * @param netoOriginal		: Neto calculado por el motor (nomina.getNetoPagar())
     * @param periodo			: Periodo que se esta pagando (P)
     * @param periodoAnterior	: Periodo anterior (P-1), o null si no existe
     * @param orden				: Orden de pago en generacion, ya con codigo asignado
     * @return					: El neto ajustado a acreditar
     * @throws Throwable		: IncomeException si el registro de P-1 nunca se retuvo, o si la
     *							  nomina no tiene el renglon del rol 34/35 que el VNPG exige
     */
    private Double ajustaNetoPorValorNoPagado(Nomina nomina, Double netoOriginal, PeriodoNomina periodo,
            PeriodoNomina periodoAnterior, OrdenPagoNomina orden) throws Throwable {
        Long idEmpleado = nomina.getEmpleado().getCodigo();
        Double neto = netoOriginal;
        List<ReglonNomina> renglones = reglonNominaDaoService.selectByNomina(nomina.getCodigo());

        ValorNoPagado retenidoEnP = valorNoPagadoDaoService.selectVivoByEmpleadoPeriodo(idEmpleado, periodo.getCodigo());
        if (retenidoEnP != null && retenidoEnP.getValor() != null && retenidoEnP.getValor().doubleValue() > 0D) {
            if (!tieneRenglonConRol(renglones, RhhRolConceptoMotor.VALOR_NO_PAGADO_RETENIDO)) {
                throw new IncomeException("El empleado " + idEmpleado + " tiene un valor no pagado"
                        + " registrado (id " + retenidoEnP.getCodigo() + ") despues del ultimo calculo"
                        + " del rol de " + periodo.getMes() + "/" + periodo.getAnio() + ": recalcule el"
                        + " periodo antes de generar la orden de pago.");
            }
            neto = RedondeoNomina.suma(neto, Double.valueOf(-retenidoEnP.getValor().doubleValue()));
            retenidoEnP.setEstado(Long.valueOf(RhhEstadoValorNoPagado.RETENIDO));
            retenidoEnP.setOrdenRetencion(orden);
            valorNoPagadoDaoService.save(retenidoEnP, retenidoEnP.getCodigo());
        }

        if (periodoAnterior != null) {
            ValorNoPagado registroAnterior = valorNoPagadoDaoService
                    .selectVivoByEmpleadoPeriodo(idEmpleado, periodoAnterior.getCodigo());
            if (registroAnterior != null) {
                if (Long.valueOf(RhhEstadoValorNoPagado.RETENIDO).equals(registroAnterior.getEstado())) {
                    if (!tieneRenglonConRol(renglones, RhhRolConceptoMotor.VALOR_NO_PAGADO_RECUPERADO)) {
                        throw new IncomeException("El empleado " + idEmpleado + " tiene un valor no"
                                + " pagado RETENIDO del periodo anterior (id " + registroAnterior.getCodigo()
                                + ") que la nomina de " + periodo.getMes() + "/" + periodo.getAnio()
                                + " todavia no refleja: recalcule el periodo antes de generar la orden"
                                + " de pago.");
                    }
                    if (registroAnterior.getValor() != null) {
                        neto = RedondeoNomina.suma(neto, registroAnterior.getValor());
                    }
                } else {
                    throw new IncomeException("El valor no pagado " + registroAnterior.getCodigo()
                            + " del empleado " + idEmpleado + " quedo REGISTRADO en el periodo anterior ("
                            + periodoAnterior.getMes() + "/" + periodoAnterior.getAnio() + ") sin retenerse"
                            + " nunca: no se genero la orden de pago de ese periodo. Anule el registro o"
                            + " genere esa orden antes de emitir la de este periodo.");
                }
            }
        }

        return RedondeoNomina.redondea(neto);
    }

    /**
     * Indica si algun renglon de la lista pertenece a un concepto con el rol de motor dado.
     */
    private boolean tieneRenglonConRol(List<ReglonNomina> renglones, int rolMotor) {
        for (ReglonNomina renglon : renglones) {
            if (renglon.getConceptoNomina() != null
                    && Long.valueOf(rolMotor).equals(renglon.getConceptoNomina().getRolMotor())) {
                return true;
            }
        }
        return false;
    }
    // ===== FIN enganche valores no pagados =====

    /**
     * Arma el detalle de un empleado, repartiendo el neto entre sus cuentas activas.
     *
     * @param orden			: Orden de pago
     * @param nomina		: Nomina del empleado
     * @param neto			: Neto a acreditar
     * @param usuario		: Usuario que ejecuta
     * @return				: Lineas de detalle del empleado
     * @throws Throwable	: IncomeException si el empleado no tiene cuenta
     */
    private List<DetalleOrdenPagoNomina> armaDetalle(OrdenPagoNomina orden, Nomina nomina,
            Double neto, String usuario) throws Throwable {

        Empleado empleado = nomina.getEmpleado();
        List<DetalleOrdenPagoNomina> detalles = new ArrayList<DetalleOrdenPagoNomina>();

        List<CuentaBancariaEmpleado> cuentas = cuentaBancariaEmpleadoDaoService
                .selectActivasByEmpleado(empleado.getCodigo());
        if (cuentas == null || cuentas.isEmpty()) {
            throw new IncomeException("El empleado " + empleado.getIdentificacion() + " ("
                    + empleado.getApellidos() + " " + empleado.getNombres() + ") no tiene ninguna"
                    + " cuenta bancaria activa registrada: no se le puede acreditar el neto."
                    + " Registrela en la ficha del empleado y vuelva a generar la orden.");
        }

        // Con una sola cuenta se acredita todo alli, sin mirar el porcentaje: es el caso
        // normal y evita que un CBEMPRCN mal cargado parta un pago que no se reparte.
        if (cuentas.size() == 1) {
            detalles.add(nuevoDetalle(orden, nomina, empleado, cuentas.get(0), neto, usuario));
            return detalles;
        }

        Double acumulado = Double.valueOf(0D);
        int indicePrincipal = 0;
        for (int i = 0; i < cuentas.size(); i++) {
            CuentaBancariaEmpleado cuenta = cuentas.get(i);
            if (SI.equals(cuenta.getPrincipal())) {
                indicePrincipal = i;
            }
            Double porcentaje = cuenta.getPorcentaje();
            Double valor = porcentaje != null
                    ? RedondeoNomina.porcentaje(neto, porcentaje) : Double.valueOf(0D);
            acumulado = RedondeoNomina.suma(acumulado, valor);
            detalles.add(nuevoDetalle(orden, nomina, empleado, cuenta, valor, usuario));
        }

        // El residuo va a la principal. La lista viene ordenada con la principal primero, asi
        // que el indice 0 es el respaldo cuando ninguna esta marcada.
        double residuo = neto.doubleValue() - acumulado.doubleValue();
        if (Math.abs(residuo) > 0D) {
            DetalleOrdenPagoNomina principal = detalles.get(indicePrincipal);
            principal.setValor(RedondeoNomina.redondea(Double.valueOf(
                    principal.getValor().doubleValue() + residuo)));
            System.out.println("Residuo de reparto de " + residuo + " asignado a la cuenta"
                    + " principal de " + empleado.getIdentificacion() + ".");
        }

        return detalles;
    }

    /**
     * Crea una linea de detalle con el snapshot de los datos bancarios.
     *
     * @param orden		: Orden de pago
     * @param nomina	: Nomina del empleado
     * @param empleado	: Empleado beneficiario
     * @param cuenta	: Cuenta a la que se acredita
     * @param valor		: Valor a acreditar
     * @param usuario	: Usuario que ejecuta
     * @return			: La linea de detalle
     */
    private DetalleOrdenPagoNomina nuevoDetalle(OrdenPagoNomina orden, Nomina nomina,
            Empleado empleado, CuentaBancariaEmpleado cuenta, Double valor, String usuario) {

        DetalleOrdenPagoNomina detalle = new DetalleOrdenPagoNomina();
        detalle.setOrdenPagoNomina(orden);
        detalle.setEmpleado(empleado);
        detalle.setNomina(nomina);
        detalle.setValor(RedondeoNomina.redondea(valor));
        actualizaSnapshotCuenta(detalle, cuenta, empleado);

        detalle.setRechazado(NO);
        detalle.setEstado(Long.valueOf(RhhEstadoDetalleOrdenPago.PENDIENTE));
        detalle.setFechaRegistro(LocalDateTime.now());
        detalle.setUsuarioRegistro(usuario);
        return detalle;
    }

    /**
     * Copia el snapshot de datos bancarios de una cuenta del empleado al detalle. Comun a
     * {@link #nuevoDetalle} (alta) y a {@link #reenviar} (snapshot al reenviar un rechazado):
     * se copia ahora y no se relee nunca. Si el empleado cambia de banco despues, el detalle
     * sigue mostrando a que cuenta se ordeno pagar en ese momento.
     *
     * @param detalle	: Detalle a actualizar
     * @param cuenta	: Cuenta bancaria vigente del empleado
     * @param empleado	: Empleado beneficiario
     */
    private void actualizaSnapshotCuenta(DetalleOrdenPagoNomina detalle, CuentaBancariaEmpleado cuenta,
            Empleado empleado) {
        detalle.setCuentaBancariaEmpleado(cuenta);
        detalle.setNumeroCuenta(cuenta.getNumeroCuenta());
        detalle.setTipoCuenta(cuenta.getTipoCuenta());
        detalle.setBanco(cuenta.getBanco() != null ? cuenta.getBanco().getNombre() : null);
        detalle.setIdentificacion(cuenta.getIdentificacionTitular() != null
                ? cuenta.getIdentificacionTitular() : empleado.getIdentificacion());
        detalle.setNombreBeneficiario(cuenta.getTitular() != null
                ? cuenta.getTitular()
                : (empleado.getApellidos() + " " + empleado.getNombres()));
    }

    /**
     * Localiza una orden del periodo que todavia se pueda rehacer.
     *
     * @param idPeriodoNomina	: Id del periodo
     * @return					: La orden reutilizable, o null si hay que crear una nueva
     * @throws Throwable		: Excepcion
     */
    private OrdenPagoNomina localizaOrdenReutilizable(Long idPeriodoNomina) throws Throwable {
        List<OrdenPagoNomina> pendientes = ordenPagoNominaDaoService
                .selectPendientesByPeriodo(idPeriodoNomina);
        if (pendientes == null || pendientes.isEmpty()) {
            return null;
        }
        return pendientes.get(0);
    }

    /**
     * Numero de la orden: <code>OP-AAAAMM</code>, con el ano y el mes del periodo.
     *
     * @param periodo	: Periodo de nomina
     * @return			: El numero de la orden
     */
    private String armaNumero(PeriodoNomina periodo) {
        return String.format("OP-%04d%02d",
                Integer.valueOf(periodo.getAnio() != null ? periodo.getAnio().intValue() : 0),
                Integer.valueOf(periodo.getMes() != null ? periodo.getMes().intValue() : 0));
    }

    /**
     * Exige que el periodo este en un estado que admita emitir la orden de pago.
     *
     * @param periodo		: Periodo de nomina
     * @throws Throwable	: IncomeException si todavia no se aprobo
     */
    private void exigeEstadoPagable(PeriodoNomina periodo) throws Throwable {
        Long estado = periodo.getEstado();
        boolean pagable = Long.valueOf(RhhEstadoPeriodoNomina.APROBADO).equals(estado)
                || Long.valueOf(RhhEstadoPeriodoNomina.CONTABILIZADO).equals(estado)
                || Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(estado);
        if (!pagable) {
            throw new IncomeException("La orden de pago se emite sobre un periodo APROBADO ("
                    + RhhEstadoPeriodoNomina.APROBADO + "), CONTABILIZADO ("
                    + RhhEstadoPeriodoNomina.CONTABILIZADO + ") o PAGADO ("
                    + RhhEstadoPeriodoNomina.PAGADO + "). El periodo esta en estado " + estado
                    + " y no la admite.");
        }
    }

    /**
     * Recupera la orden y falla con mensaje explicito si no existe.
     *
     * @param idOrdenPago	: Id de la orden
     * @return				: La orden
     * @throws Throwable	: IncomeException si no existe
     */
    private OrdenPagoNomina recuperaOrden(Long idOrdenPago) throws Throwable {
        OrdenPagoNomina orden = ordenPagoNominaDaoService.selectById(idOrdenPago,
                NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
        if (orden == null) {
            throw new IncomeException("No existe la orden de pago " + idOrdenPago + ".");
        }
        return orden;
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
    /**
     * Crea el egreso de tesoreria consolidado de la orden y lo enlaza en RDPGEGRSCDGO.
     *
     * <p>Es lo que permite que la conciliacion bancaria case el pago de la nomina con el
     * extracto. Se crea despues del asiento, con el asiento ya enlazado.</p>
     *
     * <p><b>Titular en nulo, a proposito.</b> El Javadoc de <code>Egreso</code> lo declara
     * opcional --obligatorio solo cuando el archivo bancario sale del titular, y el de la
     * nomina sale de <code>DRPG</code>-- y la base lo confirma: <code>EGRSTTLR</code> admite
     * nulo. Los empleados no son titulares de tesoreria y <code>RHH.MPLD</code> y
     * <code>CRD.ENTD</code> siguen separados por decision del maestro.</p>
     *
     * <p><b>El producto se localiza por su codigo, nunca por id.</b> <code>PGS.PRDP.ID</code>
     * es IDENTITY y cambia entre instalaciones: fijar un numero aqui funcionaria en esta base
     * y clasificaria el gasto en el producto equivocado en la siguiente. Si no existe, se lanza
     * indicando que falta ejecutar el script 15 en vez de crear la fila al vuelo: crear
     * catalogos desde un proceso de negocio es como aparecen los duplicados.</p>
     *
     * @param orden			: Orden de pago ya contabilizada
     * @param usuario		: Usuario que ejecuta
     * @throws Throwable	: IncomeException si falta el producto del script 15
     */
    private void creaEgresoConsolidado(OrdenPagoNomina orden, String usuario) throws Throwable {
        if (orden.getEgreso() != null) {
            // Ya se creo en un intento anterior: no se duplica.
            return;
        }
        Long idEmpresa = orden.getEmpresa() != null ? orden.getEmpresa().getCodigo() : null;
        ProductoPago producto = localizaProductoNomina(idEmpresa);

        Egreso egreso = new Egreso();
        egreso.setEmpresa(orden.getEmpresa());
        egreso.setTitular(null);
        egreso.setProducto(producto);
        egreso.setDescripcion("Pago de nomina " + orden.getNumero());
        egreso.setValor(orden.getTotal());
        egreso.setFecha(orden.getFechaAcreditacion());
        egreso.setDebitoAutomatico(Long.valueOf(0L));
        egreso.setEstado(Long.valueOf(EstadoEgresoTesoreria.PAGADO));
        egreso.setObservacion("Egreso consolidado de la orden de pago de nomina "
                + orden.getCodigo() + ", generado desde RRHH.");
        egreso.setFechaRegistro(LocalDateTime.now());
        egreso = egresoDaoService.save(egreso, egreso.getId());
        em.flush();

        orden.setEgreso(egreso.getId());
        ordenPagoNominaDaoService.save(orden, orden.getCodigo());
        System.out.println("Egreso de tesoreria " + egreso.getId() + " creado para la orden "
                + orden.getCodigo() + ".");
    }

    /**
     * Localiza el producto de pago de nomina por su codigo dentro de la empresa.
     *
     * @param idEmpresa		: Id de la empresa
     * @return				: El producto
     * @throws Throwable	: IncomeException si no existe
     */
    @SuppressWarnings("unchecked")
    private ProductoPago localizaProductoNomina(Long idEmpresa) throws Throwable {
        List<ProductoPago> lista = em.createQuery(" select   t "
                + " from     ProductoPago t "
                + " where    t.codigo = :codigo "
                + "          and t.empresa.codigo = :idEmpresa "
                + " order by t.id ")
                .setParameter("codigo", CODIGO_PRODUCTO_NOMINA)
                .setParameter("idEmpresa", idEmpresa)
                .getResultList();
        if (lista.isEmpty()) {
            throw new IncomeException("No existe el producto de pago con codigo '"
                    + CODIGO_PRODUCTO_NOMINA + "' para la empresa " + idEmpresa
                    + ". Ejecute sql/15_INSERT_PRODUCTO_PAGO_NOMINA.sql: TSR.EGRS.EGRSPRDP es"
                    + " obligatorio y el egreso consolidado de la nomina no se puede crear sin el.");
        }
        return lista.get(0);
    }

    // =====================================================================
    // Nomina por empleado: sincronizar pagos y reenviar un rechazado
    // (docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md §3.3/§3.4)
    // =====================================================================

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#sincronizarPagos(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public OrdenPagoNomina sincronizarPagos(Long idOrdenPago) throws Throwable {
        System.out.println("Ingresa al metodo sincronizarPagos de generacionOrdenPago service, orden: "
                + idOrdenPago);

        OrdenPagoNomina orden = recuperaOrden(idOrdenPago);
        PeriodoNomina periodo = orden.getPeriodoNomina();
        if (periodo == null) {
            throw new IncomeException("La orden de pago " + idOrdenPago
                    + " no tiene periodo de nomina asociado.");
        }
        if (esHistorico(periodo)) {
            throw new IncomeException("El periodo de la orden " + idOrdenPago + " es HISTORICO: nunca"
                    + " paso por la bandeja de tesoreria, no hay pagos de empleados que sincronizar.");
        }
        if (!esOrdenNueva(idOrdenPago)) {
            throw new IncomeException("La orden de pago " + idOrdenPago + " tiene un pago consolidado"
                    + " (RHH_NOMINA): use Confirmar, no Actualizar pagos.");
        }

        List<DetalleOrdenPagoNomina> detalles = detalleOrdenPagoNominaDaoService.selectByOrdenPago(idOrdenPago);
        List<String> errores = new ArrayList<String>();
        for (DetalleOrdenPagoNomina detalle : detalles) {
            if (!Long.valueOf(RhhEstadoDetalleOrdenPago.PENDIENTE).equals(detalle.getEstado())) {
                continue;
            }
            try {
                // Por el proxy (self), no this: necesita su propia REQUIRES_NEW -- un error en
                // un empleado no frena a los demas.
                self.sincronizaUnDetalle(detalle.getCodigo());
            } catch (Throwable e) {
                errores.add("Detalle " + detalle.getCodigo() + " (" + detalle.getNombreBeneficiario()
                        + "): " + e.getMessage());
                System.out.println("ATENCION: fallo la sincronizacion del detalle " + detalle.getCodigo()
                        + " de la orden " + idOrdenPago + ": " + e.getMessage());
            }
        }

        // Reclasifica el estado de la orden con el DRPG ya actualizado por sincronizaUnDetalle.
        detalles = detalleOrdenPagoNominaDaoService.selectByOrdenPago(idOrdenPago);
        boolean algunoPendiente = false;
        boolean algunoRechazado = false;
        for (DetalleOrdenPagoNomina detalle : detalles) {
            int estado = detalle.getEstado() != null
                    ? detalle.getEstado().intValue() : RhhEstadoDetalleOrdenPago.PENDIENTE;
            if (estado == RhhEstadoDetalleOrdenPago.PENDIENTE) {
                algunoPendiente = true;
            } else if (estado == RhhEstadoDetalleOrdenPago.RECHAZADO) {
                algunoRechazado = true;
            }
        }

        orden = recuperaOrden(idOrdenPago);
        orden.setEstado(Long.valueOf(algunoPendiente ? RhhEstadoOrdenPago.GENERADA
                : (algunoRechazado ? RhhEstadoOrdenPago.RECHAZADA_PARCIAL : RhhEstadoOrdenPago.CONFIRMADA)));
        orden = ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        // Efectos de la orden (periodo PAGADO + cierre de cuotas/anticipos), UNA SOLA VEZ: la
        // primera vez que ya no queda ningun DRPG pendiente -- guardado por el propio estado
        // del periodo, sin bandera aparte. SIN asiento consolidado (N3): cada pago ya generó
        // el suyo en Tesoreria al confirmarse (via el desglose de registraPagoDeUnDetalle).
        if (!algunoPendiente && !Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(periodo.getEstado())) {
            periodo.setEstado(Long.valueOf(RhhEstadoPeriodoNomina.PAGADO));
            periodoNominaDaoService.save(periodo, periodo.getCodigo());

            // Mismo motivo que en ContabilizacionNominaServiceImpl.contabilizarPago: EJB aparte
            // con su propia REQUIRES_NEW, para que un fallo adentro no tumbe el periodo PAGADO
            // que esta transaccion ya esta a punto de comitear.
            try {
                cierreCuotasDescuentoService.descuentaCuotasDelPeriodo(periodo.getCodigo(), idOrdenPago,
                        orden.getUsuarioRegistro());
            } catch (Throwable e) {
                System.out.println("ATENCION: fallo el cierre de cuotas de descuentos recurrentes del"
                        + " periodo " + periodo.getCodigo() + " (orden " + idOrdenPago + "). El periodo"
                        + " ya quedo PAGADO; revise a mano el saldo de"
                        + " CuotaDescuento/DescuentoRecurrente/AnticipoEmpleado. Motivo: " + e.getMessage());
            }
        }

        if (!errores.isEmpty()) {
            System.out.println("Sincronizacion de la orden " + idOrdenPago + " con " + errores.size()
                    + " error(es): " + errores);
        }

        return orden;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#sincronizaUnDetalle(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sincronizaUnDetalle(Long idDetalle) throws Throwable {
        DetalleOrdenPagoNomina detalle = detalleOrdenPagoNominaDaoService.selectById(idDetalle,
                NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
        if (detalle == null) {
            throw new IncomeException("No existe el detalle " + idDetalle + ".");
        }
        if (!Long.valueOf(RhhEstadoDetalleOrdenPago.PENDIENTE).equals(detalle.getEstado())) {
            // Ya resuelto en una pasada anterior: idempotente.
            return;
        }

        PagoProgramado pago = ultimoPagoDeOrigen(OrigenPagoExterno.RHH_NOMINA_EMPLEADO, idDetalle);
        if (pago == null) {
            throw new IncomeException("El detalle " + idDetalle + " no tiene ningun pago registrado en"
                    + " la bandeja de tesoreria (PGS.PGTR).");
        }

        int estado = pago.getEstado() != null ? pago.getEstado().intValue() : -1;
        if (estado == EstadoPagoProgramado.CONFIRMADO) {
            detalle.setEstado(Long.valueOf(RhhEstadoDetalleOrdenPago.PAGADO));
            detalle.setRechazado(NO);
            detalle.setMotivoRechazo(null);
            detalleOrdenPagoNominaDaoService.save(detalle, detalle.getCodigo());

            // Efecto del empleado (§3.3): cierra su valor no pagado recuperado de P-1, si tenia.
            cierraValorNoPagadoDeEmpleado(detalle.getOrdenPagoNomina(), detalle.getEmpleado().getCodigo());

            System.out.println("Detalle " + idDetalle + " PAGADO (pago " + pago.getId() + ").");
        } else if (estado == EstadoPagoProgramado.RECHAZADO || estado == EstadoPagoProgramado.ANULADO) {
            detalle.setEstado(Long.valueOf(RhhEstadoDetalleOrdenPago.RECHAZADO));
            detalle.setRechazado(SI);
            detalle.setMotivoRechazo(pago.getMotivo());
            detalleOrdenPagoNominaDaoService.save(detalle, detalle.getCodigo());
            System.out.println("Detalle " + idDetalle + " RECHAZADO (pago " + pago.getId() + "): "
                    + pago.getMotivo());
        } else {
            System.out.println("Detalle " + idDetalle + ": el pago " + pago.getId() + " sigue en estado "
                    + estado + ", sin cambios.");
        }
    }

    /**
     * Cierra, para UN empleado ya confirmado por Tesoreria, el valor no pagado RETENIDO del
     * periodo anterior (P-1) que se recupero en su neto de este periodo (P).
     *
     * <p>Copia del cuerpo-por-empleado de
     * <code>ContabilizacionNominaServiceImpl.cierraValoresNoPagadosRecuperados</code> (privado
     * en esa clase, period-wide) -- verificado antes de escribir (confirmacion (a) del
     * contrato, API-PAGO-NOMINA-POR-EMPLEADO.md §3.3) que esa logica es autocontenida por
     * empleado: solo mira el VNPG de ESE empleado en P-1, nunca a otro, asi que separarla por
     * empleado da el mismo resultado para quien ya se confirmo. No se llama al metodo de
     * ContabilizacionNominaServiceImpl porque ese recorre TODAS las nominas del periodo, no una.</p>
     *
     * @param orden			: Orden de pago a la que pertenece el DRPG ya confirmado
     * @param idEmpleado	: Id del empleado confirmado
     * @throws Throwable	: Excepcion
     */
    private void cierraValorNoPagadoDeEmpleado(OrdenPagoNomina orden, Long idEmpleado) throws Throwable {
        PeriodoNomina periodo = orden.getPeriodoNomina();
        if (periodo == null || periodo.getEmpresa() == null || periodo.getFechaInicio() == null) {
            return;
        }
        PeriodoNomina periodoAnterior = periodoNominaDaoService.selectByFechaEmpresa(
                periodo.getEmpresa().getCodigo(), periodo.getFechaInicio().minusDays(1));
        if (periodoAnterior == null) {
            return;
        }
        ValorNoPagado registro = valorNoPagadoDaoService
                .selectVivoByEmpleadoPeriodo(idEmpleado, periodoAnterior.getCodigo());
        if (registro == null || !Long.valueOf(RhhEstadoValorNoPagado.RETENIDO).equals(registro.getEstado())) {
            return;
        }
        registro.setEstado(Long.valueOf(RhhEstadoValorNoPagado.PAGADO));
        registro.setOrdenPago(orden);
        registro.setPeriodoRecuperacion(periodo);
        valorNoPagadoDaoService.save(registro, registro.getCodigo());
        System.out.println("Valor no pagado " + registro.getCodigo() + " del empleado " + idEmpleado
                + " recuperado y marcado PAGADO con la orden " + orden.getCodigo()
                + " (sincronizacion por empleado).");
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#reenviar(java.lang.Long, java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DetalleOrdenPagoNomina reenviar(Long idDetalle, Long idUsuario) throws Throwable {
        System.out.println("Ingresa al metodo reenviar de generacionOrdenPago service, detalle: " + idDetalle);

        DetalleOrdenPagoNomina detalle = detalleOrdenPagoNominaDaoService.selectById(idDetalle,
                NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
        if (detalle == null) {
            throw new IncomeException("No existe el detalle " + idDetalle + ".");
        }
        if (!Long.valueOf(RhhEstadoDetalleOrdenPago.RECHAZADO).equals(detalle.getEstado())) {
            throw new IncomeException("El detalle " + idDetalle + " no esta RECHAZADO (estado actual "
                    + detalle.getEstado() + "): solo se puede reenviar un pago rechazado.");
        }
        exigeIdUsuario(idUsuario, "reenviar");

        Empleado empleado = detalle.getEmpleado();
        List<CuentaBancariaEmpleado> cuentas = cuentaBancariaEmpleadoDaoService
                .selectActivasByEmpleado(empleado.getCodigo());
        if (cuentas == null || cuentas.isEmpty()) {
            throw new IncomeException("El empleado " + empleado.getIdentificacion() + " ("
                    + empleado.getApellidos() + " " + empleado.getNombres() + ") no tiene ninguna cuenta"
                    + " bancaria activa: corrijala en su ficha antes de reenviar.");
        }

        // Reenviar NO re-reparte: esta fila sigue siendo la misma cuenta de siempre, ya
        // corregida en la ficha del empleado (misma CBEMCDGO, otros datos). Si el empleado
        // ahora tiene varias cuentas activas y ninguna coincide con la de este detalle, no se
        // puede adivinar cual corregir.
        CuentaBancariaEmpleado cuenta = null;
        if (cuentas.size() == 1) {
            cuenta = cuentas.get(0);
        } else {
            for (CuentaBancariaEmpleado candidata : cuentas) {
                if (detalle.getCuentaBancariaEmpleado() != null
                        && candidata.getCodigo().equals(detalle.getCuentaBancariaEmpleado().getCodigo())) {
                    cuenta = candidata;
                    break;
                }
            }
            if (cuenta == null) {
                throw new IncomeException("El empleado " + empleado.getIdentificacion() + " tiene "
                        + cuentas.size() + " cuentas bancarias activas y ninguna coincide con la que este"
                        + " pago rechazado usaba: no se puede reenviar sin saber a cual corregirle el"
                        + " envio. Deje una sola cuenta activa, o reactive la misma que se uso"
                        + " originalmente, y vuelva a intentar.");
            }
        }

        actualizaSnapshotCuenta(detalle, cuenta, empleado);
        detalle.setRechazado(NO);
        detalle.setMotivoRechazo(null);
        detalle.setEstado(Long.valueOf(RhhEstadoDetalleOrdenPago.PENDIENTE));
        detalle = detalleOrdenPagoNominaDaoService.save(detalle, detalle.getCodigo());

        OrdenPagoNomina orden = detalle.getOrdenPagoNomina();
        Long idProductoNomina = idProductoNomina(orden.getEmpresa().getCodigo());
        String periodoTexto = orden.getPeriodoNomina().getMes() + "/" + orden.getPeriodoNomina().getAnio();
        // El pago anterior de este mismo DRPG esta RECHAZADO/ANULADO: la guarda anti-duplicados
        // de tienePagoVivoPorDrpg (dentro de registraPagoDeUnDetalle) lo deja pasar.
        registraPagoDeUnDetalle(orden, detalle, idProductoNomina, periodoTexto, idUsuario);

        // La orden vuelve a tener un DRPG pendiente: la proxima sincronizarPagos la reclasifica
        // (o esta misma llamada, si el frontend la encadena).
        orden.setEstado(Long.valueOf(RhhEstadoOrdenPago.GENERADA));
        ordenPagoNominaDaoService.save(orden, orden.getCodigo());

        System.out.println("Detalle " + idDetalle + " reenviado con la cuenta " + cuenta.getCodigo() + ".");
        return detalle;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#detalleConEstadoPago(java.lang.Long)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DetalleOrdenPagoNomina> detalleConEstadoPago(Long idOrdenPago) throws Throwable {
        List<DetalleOrdenPagoNomina> detalles = detalleOrdenPagoNominaDaoService.selectByOrdenPago(idOrdenPago);
        for (DetalleOrdenPagoNomina detalle : detalles) {
            PagoProgramado pago = ultimoPagoDeOrigen(OrigenPagoExterno.RHH_NOMINA_EMPLEADO, detalle.getCodigo());
            if (pago != null) {
                detalle.setIdPago(pago.getId());
                detalle.setEstadoPago(pago.getEstado());
            }
        }
        return detalles;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#poblarPagoPorEmpleado(java.util.List)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    @SuppressWarnings("unchecked")
    public List<OrdenPagoNomina> poblarPagoPorEmpleado(List<OrdenPagoNomina> ordenes) throws Throwable {
        if (ordenes == null || ordenes.isEmpty()) {
            return ordenes;
        }
        // UNA sola consulta para toda la lista (ITEM 8, contrato §4): el universo de ordenes
        // con pago consolidado RHH_NOMINA es chico -una por periodo, como mucho, de los
        // periodos del camino viejo- y no vale la pena filtrarlo por los ids de la pagina;
        // traerlo completo evita un IN largo y, sobre todo, evita una consulta por orden.
        List<Long> conPagoConsolidado = em.createQuery(" select   p.idOrigen "
                + " from     PagoProgramado p "
                + " where    p.origenExterno = :origen ")
                .setParameter("origen", OrigenPagoExterno.RHH_NOMINA)
                .getResultList();
        Set<Long> ordenesViejas = new HashSet<Long>(conPagoConsolidado);
        for (OrdenPagoNomina orden : ordenes) {
            // periodoNomina es @ManyToOne sin fetch explicito -> EAGER por defecto de JPA: ya
            // viene cargado con la orden, sin consulta aparte. Un periodo HISTORICO nunca pasa
            // por la bandeja (ni RHH_NOMINA ni RHH_NOMINA_EMPLEADO), asi que el criterio de
            // "pago consolidado" por si solo lo clasificaria mal como pagoPorEmpleado=true, y
            // la pantalla le mostraria 'Actualizar pagos' en vez de 'Confirmar' -- esOrdenNueva
            // tiene la misma trampa, corregida igual en confirmar()/generarArchivoBancario()
            // con el mismo esHistorico(:661).
            boolean historico = orden.getPeriodoNomina() == null || esHistorico(orden.getPeriodoNomina());
            orden.setPagoPorEmpleado(Boolean.valueOf(!historico && !ordenesViejas.contains(orden.getCodigo())));
        }
        return ordenes;
    }

    /* (non-Javadoc)
     * @see com.saa.ejb.rhh.service.GeneracionOrdenPagoService#poblarPagoPorEmpleado(com.saa.model.rhh.OrdenPagoNomina)
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrdenPagoNomina poblarPagoPorEmpleado(OrdenPagoNomina orden) throws Throwable {
        if (orden == null) {
            return null;
        }
        // Mismo criterio que la lista: un periodo HISTORICO nunca pasa por la bandeja, nunca es
        // pagoPorEmpleado aunque esOrdenNueva (sin pago RHH_NOMINA) de true.
        boolean historico = orden.getPeriodoNomina() == null || esHistorico(orden.getPeriodoNomina());
        orden.setPagoPorEmpleado(Boolean.valueOf(!historico && esOrdenNueva(orden.getCodigo())));
        return orden;
    }

}
