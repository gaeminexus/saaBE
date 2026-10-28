package com.saa.ejb.historico.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.historico.service.CargaHistoricaNominaPasoService;
import com.saa.ejb.rhh.service.ContabilizacionNominaService;
import com.saa.ejb.rhh.service.ProcesoNominaService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.historico.LoteHistoricoNomina;
import com.saa.model.historico.LoteHistoricoNomina.ComponentePlanillaLote;
import com.saa.model.historico.LoteHistoricoNomina.DetallePagoLote;
import com.saa.model.historico.LoteHistoricoNomina.EmpleadoLote;
import com.saa.model.historico.LoteHistoricoNomina.LineaPagoLote;
import com.saa.model.historico.LoteHistoricoNomina.PagoLote;
import com.saa.model.historico.LoteHistoricoNomina.PlanillaIessLote;
import com.saa.model.historico.LoteHistoricoNomina.ProvisionLote;
import com.saa.model.historico.LoteHistoricoNomina.RenglonLote;
import com.saa.model.historico.LoteHistoricoNomina.RolLote;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;
import com.saa.model.rhh.ConceptoNomina;
import com.saa.model.rhh.ContratoEmpleado;
import com.saa.model.rhh.DetalleOrdenPagoNomina;
import com.saa.model.rhh.DetallePlanillaIess;
import com.saa.model.rhh.Empleado;
import com.saa.model.rhh.Nomina;
import com.saa.model.rhh.OrdenPagoNomina;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.PlanillaIess;
import com.saa.model.rhh.ProvisionNomina;
import com.saa.model.rhh.ReglonNomina;
import com.saa.model.rhh.TipoContratoEmpleado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.RhhEstadoDetalleOrdenPago;
import com.saa.rubros.RhhEstadoEmpleado;
import com.saa.rubros.RhhEstadoNomina;
import com.saa.rubros.RhhEstadoOrdenPago;
import com.saa.rubros.RhhEstadoPeriodoNomina;
import com.saa.rubros.RhhModoPeriodoNomina;
import com.saa.rubros.RhhOrigenRenglon;
import com.saa.rubros.RhhTipoPeriodoNomina;
import com.saa.rubros.RhhTipoProvision;
import com.saa.rubros.RhhTipoRelacionLaboral;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCuentaContable;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Pasos de la carga histórica de nómina (C13). Ver {@link CargaHistoricaNominaPasoService} y
 * docs/logica-negocio/rhh/CARGA-HISTORICA-NOMINA.md.
 *
 * <p>Decisiones de diseño que explican el código:</p>
 * <ul>
 * <li><b>Sin recalcular.</b> NMNA, RNGL y PVNM se graban con los valores del lote (rol del contador). No se llama a
 * {@code calcularPeriodo} ni a {@code aprobarPeriodo} (que recalcularía y emitiría roles): el período se crea
 * CALCULADO y se pasa a APROBADO directamente.</li>
 * <li><b>Asientos con los motores.</b> El período se crea en modo PRODUCTIVO (2) —en modo histórico (1) el motor no
 * genera asientos— con {@code PRDNFCCN} = último día del mes; rol y provisiones los arma
 * {@code ContabilizacionNominaService} desde esos valores, y el pago {@code contabilizarPago} con la fecha del banco.
 * No se usa {@code /rdpg/generar}: en modo productivo manda los pagos a la bandeja de Tesorería.</li>
 * <li><b>Centro de costo y traza.</b> Ningún asiento de nómina lleva centro de costo: se impone después en las líneas
 * de gasto {@code 4.3.*} (como C11). La observación del asiento recibe la traza {@code CARGA HISTORICA [...]}.</li>
 * <li><b>Planilla IESS.</b> {@code PlanillaIessServiceImpl.registrar} exige el período CALCULADO y {@code pagar} crea un
 * pago de Tesorería por débito automático: aquí se graban PLIS/DLIS directamente y el pago con
 * {@code generarAsiento} y las líneas explícitas del lote.</li>
 * <li><b>Idempotencia.</b> El período del mes con la traza se retoma (cada paso mira si ya está hecho); sin la traza,
 * se rechaza (decisión N-1 del análisis).</li>
 * </ul>
 */
@Stateless
public class CargaHistoricaNominaPasoServiceImpl implements CargaHistoricaNominaPasoService {

    static final String PREFIJO_OBSERVACION = "CARGA HISTORICA [";
    static final String PREFIJO_ORDEN = "OPH-";
    static final double TOLERANCIA = 0.011;
    static final long TIPO_CONTRATO_INDEFINIDO = 1L;
    static final String RAIZ_CUENTAS_GASTO = "4.3.";

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private ContabilizacionNominaService contabilizacionNominaService;

    @EJB
    private ProcesoNominaService procesoNominaService;

    @EJB
    private AsientoContableService asientoContableService;

    // =====================================================================================================
    // Validación del lote
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> validarLote(Contexto ctx, LoteHistoricoNomina lote) throws Throwable {
        System.out.println("Ingresa al metodo validarLote de CargaHistoricaNominaPasoService, mes " + ctx.mes);
        List<String> errores = new ArrayList<>();
        if (em.find(Empresa.class, ctx.idEmpresa) == null) errores.add("No existe la empresa " + ctx.idEmpresa);
        Usuario usuario = ctx.idUsuario == null ? null : em.find(Usuario.class, ctx.idUsuario);
        if (usuario == null) errores.add("El usuario " + ctx.idUsuario + " no existe en SCP.PJRQ (por defecto 38)");
        else if (ctx.usuario == null || ctx.usuario.trim().isEmpty()) ctx.usuario = usuario.getNombre() != null ? usuario.getNombre() : "SISTEMA";

        String mesPeriodo = String.format("%04d-%02d", lote.periodo.anio, lote.periodo.mes);
        if (!mesPeriodo.equals(ctx.mes)) errores.add("El periodo del lote (" + mesPeriodo + ") no es el mes del lote (" + ctx.mes + ")");
        try {
            LocalDate fc = fecha(lote.periodo.fechaContable, "fechaContable");
            if (!fc.toString().startsWith(ctx.mes)) errores.add("La fecha contable " + fc + " no es del mes " + ctx.mes);
        } catch (IncomeException e) { errores.add(e.getMessage()); }

        // Períodos contables de todas las fechas que generan asiento
        Set<String> meses = new LinkedHashSet<>();
        meses.add(ctx.mes);
        for (PagoLote p : lote.pagos) if (p.fecha != null) meses.add(p.fecha.substring(0, 7));
        for (PlanillaIessLote p : lote.planillasIess) if (p.pago != null && p.pago.fecha != null) meses.add(p.pago.fecha.substring(0, 7));
        for (String m : meses) { String e = revisarPeriodoContable(ctx.idEmpresa, m); if (e != null) errores.add(e); }

        // Configuración de nómina
        Long cfnm = numero(unico(em.createNativeQuery("select count(*) from RHH.CFNM where PJRQCDGO = ?1").setParameter(1, ctx.idEmpresa).getResultList()));
        if (cfnm == null || cfnm == 0L) errores.add("La empresa " + ctx.idEmpresa + " no tiene configuracion de nomina (RHH.CFNM)");

        // Conceptos
        Set<String> abreviaturas = new LinkedHashSet<>();
        for (RolLote r : lote.roles) for (RenglonLote g : r.renglones) abreviaturas.add(g.concepto);
        for (ProvisionLote p : lote.provisiones) abreviaturas.add(p.concepto);
        for (String a : abreviaturas) if (concepto(ctx.idEmpresa, a) == null) errores.add("No existe el concepto de nomina " + a + " (RHH.CPNM.CPNMABRV) en la empresa " + ctx.idEmpresa);

        // Centro de costo
        if (ctx.centroCosto != null && !ctx.centroCosto.trim().isEmpty()) {
            CentroCosto cc = centroCosto(ctx.idEmpresa, ctx.centroCosto);
            if (cc == null) errores.add("No existe el centro de costo " + ctx.centroCosto + " (CNT.CNCS)");
            else if (!Long.valueOf(Estado.ACTIVO).equals(cc.getEstado())) errores.add("El centro de costo " + ctx.centroCosto + " esta inactivo");
        }

        // Cuentas bancarias y cuentas contables de los pagos de planillas
        Set<Long> bancos = new LinkedHashSet<>();
        for (PagoLote p : lote.pagos) if (p.idCuentaBancaria != null) bancos.add(p.idCuentaBancaria); else errores.add("El pago " + p.ref + " no trae idCuentaBancaria");
        Set<String> cuentas = new LinkedHashSet<>();
        for (PlanillaIessLote p : lote.planillasIess) if (p.pago != null) {
            if (p.pago.idCuentaBancaria != null) bancos.add(p.pago.idCuentaBancaria); else errores.add("El pago de la planilla " + p.ref + " no trae idCuentaBancaria");
            for (LineaPagoLote l : p.pago.lineas) cuentas.add(l.cuenta);
        }
        for (Long id : bancos) {
            CuentaBancaria cb = em.find(CuentaBancaria.class, id);
            if (cb == null) errores.add("No existe la cuenta bancaria " + id + " (TSR.CNBC)");
            else if (cb.getPlanCuenta() == null) errores.add("La cuenta bancaria " + id + " no tiene cuenta contable");
            else if (cb.getBanco() == null || cb.getBanco().getEmpresa() == null || !ctx.idEmpresa.equals(cb.getBanco().getEmpresa().getCodigo()))
                errores.add("La cuenta bancaria " + id + " no es de la empresa " + ctx.idEmpresa);
        }
        for (String c : cuentas) { try { cuentaMovimiento(ctx.idEmpresa, c); } catch (IncomeException e) { errores.add(e.getMessage()); } }

        // Cada renglón del rol y cada pago tienen su empleado en el lote
        Set<String> enLote = new LinkedHashSet<>();
        for (EmpleadoLote e : lote.empleados) enLote.add(e.identificacion);
        for (RolLote r : lote.roles) if (!enLote.contains(r.identificacion)) errores.add("El rol " + r.ref + " no tiene su empleado en el bloque empleados");
        for (PagoLote p : lote.pagos) for (DetallePagoLote d : p.detalle) if (!enLote.contains(d.identificacion)) errores.add("El pago " + p.ref + " trae un empleado (" + d.ref + ") que no esta en el lote");
        return errores;
    }

    // =====================================================================================================
    // Empleados
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarEmpleado(Contexto ctx, EmpleadoLote e) throws Throwable {
        ItemResultado item = new ItemResultado("empleado", e.ref, null);
        try {
            if (e.identificacion == null || e.identificacion.trim().isEmpty()) return item.rechazado("identificacion vacia");
            Empleado emp = empleado(ctx.idEmpresa, e.identificacion);
            if (emp != null) {
                ContratoEmpleado c = contratoDelMes(emp.getCodigo(), ctx.mes);
                String extra = c == null ? "; ATENCION: no tiene contrato que cubra " + ctx.mes : "";
                return ctx.soloValidar ? item.validado("ya existe (id " + emp.getCodigo() + ")" + extra) : item.omitido(emp.getCodigo(), "ya existe" + extra);
            }
            if (ctx.soloValidar) return item.validado("se crearia el empleado con su contrato desde " + e.fechaIngreso + (e.fechaSalida != null ? " hasta " + e.fechaSalida : ""));

            LocalDate ingreso = fecha(e.fechaIngreso, "fechaIngreso");
            LocalDate salida = e.fechaSalida == null ? null : fecha(e.fechaSalida, "fechaSalida");
            CentroCosto cc = ctx.centroCosto == null ? null : centroCosto(ctx.idEmpresa, ctx.centroCosto);
            emp = new Empleado();
            emp.setIdentificacion(e.identificacion.trim());
            emp.setApellidos(recortar(nvl(e.apellidos, "SIN APELLIDOS"), 100));
            emp.setNombres(recortar(nvl(e.nombres, "SIN NOMBRES"), 100));
            emp.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            emp.setEstado((long) (salida != null ? RhhEstadoEmpleado.CESANTE : RhhEstadoEmpleado.ACTIVO));
            emp.setFechaIngreso(ingreso);
            emp.setRegion(1L);
            emp.setCentroCosto(cc);
            emp.setFechaRegistro(LocalDate.now());
            emp.setUsuarioRegistro(recortar(ctx.usuario, 60));
            em.persist(emp);

            ContratoEmpleado c = new ContratoEmpleado();
            c.setEmpleado(emp);
            c.setTipoContrato(em.find(TipoContratoEmpleado.class, TIPO_CONTRATO_INDEFINIDO));
            c.setNumero("CT-" + e.identificacion.trim());
            c.setFechaInicio(ingreso);
            c.setFechaFin(salida);
            c.setFechaTerminacion(salida);
            c.setSalarioBase(e.sueldo);
            c.setEstado(salida != null ? "CERRADO" : "ACTIVO");
            c.setTipoRelacionLaboral((long) RhhTipoRelacionLaboral.INDEFINIDO_TIEMPO_COMPLETO);
            c.setJornada(1L);
            c.setHorasSemanales(40D);
            c.setModalidadDecimoTercero(e.modalidadDecimoTercero);
            c.setModalidadDecimoCuarto(e.modalidadDecimoCuarto);
            c.setModalidadFondosReserva(e.modalidadFondosReserva);
            c.setDerechoDecimoCuarto("S");
            c.setAportaIess("S");
            c.setRetieneFuente("N");
            c.setCentroCosto(cc);
            c.setObservacion(recortar(ctx.observacion + " | " + e.ref, 500));
            c.setFechaRegistro(LocalDate.now());
            c.setUsuarioRegistro(recortar(ctx.usuario, 60));
            em.persist(c);
            em.flush();
            return item.creado(emp.getCodigo(), null, null, "empleado y contrato " + c.getCodigo() + " desde " + ingreso + (salida != null ? " hasta " + salida : ""));
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    // =====================================================================================================
    // Período, nóminas, renglones y provisiones
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarPeriodo(Contexto ctx, LoteHistoricoNomina lote) throws Throwable {
        ItemResultado item = new ItemResultado("periodo", ctx.mes, null);
        try {
            PeriodoNomina existente = periodoDelMes(ctx.idEmpresa, lote.periodo.anio, lote.periodo.mes);
            if (existente != null) {
                boolean nuestro = existente.getObservaciones() != null && existente.getObservaciones().startsWith(PREFIJO_OBSERVACION);
                if (!nuestro) return item.rechazado("ya existe el periodo de nomina " + existente.getCodigo() + " del mes (modo " + existente.getModo()
                        + ", estado " + existente.getEstado() + ") que no es de la carga historica: decidir si se reemplaza (N-1)");
                return ctx.soloValidar ? item.validado("ya cargado (periodo " + existente.getCodigo() + ")") : item.omitido(existente.getCodigo(), "ya cargado, estado " + existente.getEstado());
            }

            // Validaciones de contenido (también en soloValidar)
            List<String> problemas = new ArrayList<>();
            double ti = 0, td = 0, tn = 0, tp = 0;
            for (RolLote r : lote.roles) {
                double ing = 0, des = 0;
                for (RenglonLote g : r.renglones) {
                    ConceptoNomina cn = concepto(ctx.idEmpresa, g.concepto);
                    if (cn == null) { problemas.add(r.ref + ": concepto " + g.concepto + " inexistente"); continue; }
                    long tipo = cn.getTipoConcepto() == null ? 0L : cn.getTipoConcepto();
                    if (tipo == 1L) ing += nz(g.valor);
                    else if (tipo == 2L) des += nz(g.valor);
                }
                if (Math.abs(ing - nz(r.totalIngresos)) > TOLERANCIA || Math.abs(des - nz(r.totalDescuentos)) > TOLERANCIA)
                    problemas.add(r.ref + ": los renglones no suman los totales del rol (ingresos " + redondea(ing) + "/" + r.totalIngresos + ", descuentos " + redondea(des) + "/" + r.totalDescuentos + ")");
                if (Math.abs(ing - des - nz(r.neto)) > TOLERANCIA)
                    problemas.add(r.ref + ": ingresos - descuentos (" + redondea(ing - des) + ") no es el neto " + r.neto);
                if (!ctx.soloValidar && empleado(ctx.idEmpresa, r.identificacion) == null) problemas.add(r.ref + ": el empleado no existe (fallo su alta)");
                ti += nz(r.totalIngresos); td += nz(r.totalDescuentos); tn += nz(r.neto); tp += nz(r.aportePatronal);
            }
            if (!problemas.isEmpty()) return item.rechazado(String.join("; ", problemas));
            if (ctx.soloValidar) return item.validado("se crearia el periodo " + ctx.mes + " con " + lote.roles.size() + " nominas, neto " + redondea(tn)
                    + ", " + lote.provisiones.size() + " provisiones; fecha contable " + lote.periodo.fechaContable);

            LocalDate fin = fecha(lote.periodo.fechaFin, "fechaFin");
            PeriodoNomina p = new PeriodoNomina();
            p.setAnio(lote.periodo.anio);
            p.setMes(lote.periodo.mes);
            p.setFechaInicio(fecha(lote.periodo.fechaInicio, "fechaInicio"));
            p.setFechaFin(fin);
            p.setFechaContable(fecha(lote.periodo.fechaContable, "fechaContable"));
            p.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            p.setModo((long) RhhModoPeriodoNomina.PRODUCTIVO_CONTABILIZA);
            p.setTipoPeriodo((long) RhhTipoPeriodoNomina.MENSUAL);
            p.setEstado((long) RhhEstadoPeriodoNomina.CALCULADO);
            p.setTotalIngresos(redondea(ti));
            p.setTotalDescuentos(redondea(td));
            p.setTotalNeto(redondea(tn));
            p.setTotalPatronal(redondea(tp));
            p.setNumeroEmpleados(lote.roles.size());
            p.setObservaciones(recortar(ctx.observacion + (lote.fuente != null ? " | " + lote.fuente : ""), 500));
            p.setFechaRegistro(LocalDate.now());
            p.setUsuarioRegistro(recortar(ctx.usuario, 60));
            em.persist(p);

            Map<String, Empleado> empleados = new HashMap<>();
            int nRenglones = 0;
            for (RolLote r : lote.roles) {
                Empleado emp = empleado(ctx.idEmpresa, r.identificacion);
                empleados.put(r.identificacion, emp);
                ContratoEmpleado contrato = contratoDelMes(emp.getCodigo(), ctx.mes);
                if (contrato == null) throw new IncomeException(r.ref + ": el empleado " + emp.getCodigo() + " no tiene contrato");
                Nomina n = new Nomina();
                n.setPeriodoNomina(p);
                n.setEmpleado(emp);
                n.setContrato(contrato);
                n.setSalarioBase(r.sueldo);
                n.setTotalIngresos(redondea(nz(r.totalIngresos)));
                n.setTotalDescuentos(redondea(nz(r.totalDescuentos)));
                n.setNetoPagar(redondea(nz(r.neto)));
                n.setEstado((long) RhhEstadoNomina.APROBADA);
                n.setDiasTrabajados(r.dias);
                n.setHorasTrabajadas(0D);
                n.setBaseIess(r.baseIess);
                n.setBaseImpuestoRenta(0D);
                n.setBaseFondosReserva(r.baseIess);
                n.setBaseDecimoTercero(r.baseIess);
                n.setBaseDecimoCuarto(0D);
                n.setAportePersonal(nz(r.aportePersonal));
                n.setAportePatronal(nz(r.aportePatronal));
                n.setAporteIeceSecap(0D);
                n.setFondosReserva(nz(r.fondosReserva));
                n.setRetencionImpuestoRenta(0D);
                n.setTotalPatronal(nz(r.aportePatronal));
                n.setObservacion(recortar(ctx.observacion + " | " + r.ref, 500));
                n.setFechaRegistro(LocalDate.now());
                n.setUsuarioRegistro(recortar(ctx.usuario, 60));
                em.persist(n);
                int orden = 0;
                for (RenglonLote g : r.renglones) {
                    ConceptoNomina cn = concepto(ctx.idEmpresa, g.concepto);
                    ReglonNomina rg = new ReglonNomina();
                    rg.setNomina(n);
                    rg.setConceptoNomina(cn);
                    rg.setCantidad(0D);
                    rg.setValor(redondea(nz(g.valor)));
                    rg.setImponible(nvl(cn.getImponibleIess(), "N"));
                    rg.setOrden(++orden);
                    rg.setDescripcion(recortar(nvl(g.descripcion, cn.getNombre()), 200));
                    rg.setTipoConcepto(cn.getTipoConcepto());
                    rg.setOrigen((long) RhhOrigenRenglon.INGRESO_MANUAL);
                    rg.setManual("S");
                    rg.setImponibleIess(nvl(cn.getImponibleIess(), "N"));
                    rg.setGravadoIr(nvl(cn.getImponibleIr(), "N"));
                    rg.setPatronal(nvl(cn.getPatronal(), "N"));
                    rg.setTablaReferencia("CARGA_HISTORICA");
                    rg.setFechaRegistro(LocalDate.now());
                    rg.setUsuarioRegistro(recortar(ctx.usuario, 60));
                    em.persist(rg);
                    nRenglones++;
                }
            }
            int nProv = 0;
            for (ProvisionLote pv : lote.provisiones) {
                Empleado emp = empleados.get(pv.identificacion);
                if (emp == null) emp = empleado(ctx.idEmpresa, pv.identificacion);
                if (emp == null) throw new IncomeException("provision " + pv.ref + ": empleado inexistente");
                ProvisionNomina v = new ProvisionNomina();
                v.setPeriodoNomina(p);
                v.setEmpleado(emp);
                v.setConceptoNomina(concepto(ctx.idEmpresa, pv.concepto));
                v.setTipoProvision(tipoProvision(pv.tipo));
                v.setValor(redondea(nz(pv.valor)));
                v.setEstado(1L);
                v.setFechaRegistro(LocalDateTime.now());
                v.setUsuarioRegistro(recortar(ctx.usuario, 60));
                em.persist(v);
                nProv++;
            }
            // Sin recalcular: el período queda APROBADO, que es lo que exige la contabilización.
            p.setEstado((long) RhhEstadoPeriodoNomina.APROBADO);
            p.setFechaAprobacion(fin);
            p.setUsuarioAprueba(recortar(ctx.usuario, 60));
            em.flush();
            return item.creado(p.getCodigo(), null, null, lote.roles.size() + " nominas, " + nRenglones + " renglones, " + nProv + " provisiones; neto " + redondea(tn));
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    // =====================================================================================================
    // Asientos de rol y provisiones
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado contabilizarRol(Contexto ctx, Long idPeriodo) throws Throwable {
        ItemResultado item = new ItemResultado("asiento-rol", ctx.mes, null);
        if (ctx.soloValidar) return item.validado("se contabilizaria el rol con la plantilla de rol (163), fecha contable del periodo");
        try {
            PeriodoNomina p = em.find(PeriodoNomina.class, idPeriodo);
            if (p.getAsientoRol() != null) return item.omitido(p.getAsientoRol(), "el rol ya tiene asiento");
            Asiento a = contabilizacionNominaService.contabilizarRol(idPeriodo, ctx.usuario);
            if (a == null) return item.rechazado("el motor no genero asiento de rol (periodo historico?)");
            int n = marcarAsiento(ctx, a.getCodigo(), "rol");
            return item.creado(idPeriodo, a.getCodigo(), numeroAsiento(a), "asiento de rol; centro de costo en " + n + " lineas de gasto");
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado contabilizarProvisiones(Contexto ctx, Long idPeriodo) throws Throwable {
        ItemResultado item = new ItemResultado("asiento-provisiones", ctx.mes, null);
        if (ctx.soloValidar) return item.validado("se contabilizarian las provisiones con la plantilla de provisiones (164)");
        try {
            PeriodoNomina p = em.find(PeriodoNomina.class, idPeriodo);
            if (p.getAsientoProvisiones() != null) return item.omitido(p.getAsientoProvisiones(), "las provisiones ya tienen asiento");
            if (p.getAsientoRol() == null) return item.rechazado("primero debe contabilizarse el rol");
            Asiento a = contabilizacionNominaService.contabilizarProvisiones(idPeriodo, ctx.usuario);
            if (a == null) return item.omitido(idPeriodo, "el periodo no tiene provisiones: no hay asiento");
            int n = marcarAsiento(ctx, a.getCodigo(), "provisiones");
            return item.creado(idPeriodo, a.getCodigo(), numeroAsiento(a), "asiento de provisiones; centro de costo en " + n + " lineas de gasto");
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    // =====================================================================================================
    // Pagos de líquidos
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarPago(Contexto ctx, Long idPeriodo, PagoLote pl) throws Throwable {
        ItemResultado item = new ItemResultado("pago", pl.ref, pl.fecha);
        try {
            if (pl.inferida && !ctx.aceptarInferidos)
                return item.rechazado("fecha de pago inferida (" + nvl(pl.motivo, "sin evidencia directa") + "): se carga solo con aceptarInferidos y decision registrada");
            LocalDate fechaPago = fecha(pl.fecha, "fecha del pago");
            double total = 0;
            for (DetallePagoLote d : pl.detalle) total += nz(d.valor);
            if (pl.total != null && Math.abs(total - pl.total) > TOLERANCIA) return item.rechazado("el detalle suma " + redondea(total) + " y el total dice " + pl.total);
            if (total <= 0) return item.rechazado("pago sin valor");
            if (ctx.soloValidar) return item.validado("se pagarian " + pl.detalle.size() + " liquidos por " + redondea(total) + " el " + fechaPago + " desde la cuenta bancaria " + pl.idCuentaBancaria + (pl.inferida ? " (fecha inferida)" : ""));

            String numero = PREFIJO_ORDEN + pl.ref;
            List<OrdenPagoNomina> previas = em.createQuery("select o from OrdenPagoNomina o where o.periodoNomina.codigo = :p and o.numero = :n", OrdenPagoNomina.class)
                    .setParameter("p", idPeriodo).setParameter("n", numero).getResultList();
            if (!previas.isEmpty()) return item.omitido(previas.get(0).getCodigo(), "la orden " + numero + " ya existe");
            PeriodoNomina p = em.find(PeriodoNomina.class, idPeriodo);
            if (p.getAsientoRol() == null) return item.rechazado("primero debe contabilizarse el rol");

            OrdenPagoNomina o = new OrdenPagoNomina();
            o.setEmpresa(p.getEmpresa());
            o.setPeriodoNomina(p);
            o.setCuentaBancaria(em.find(CuentaBancaria.class, pl.idCuentaBancaria));
            o.setNumero(numero);
            o.setFechaEmision(fechaPago);
            o.setTotal(redondea(total));
            o.setNumeroEmpleados(pl.detalle.size());
            o.setEstado((long) RhhEstadoOrdenPago.GENERADA);
            o.setPagoPorEmpleado(Boolean.FALSE);
            o.setObservaciones(recortar(ctx.observacion + " | " + pl.ref + (pl.inferida ? " | fecha inferida: " + nvl(pl.motivo, "") : " | " + nvl(pl.motivo, "fecha del banco")), 500));
            o.setFechaRegistro(LocalDateTime.now());
            o.setUsuarioRegistro(recortar(ctx.usuario, 60));
            em.persist(o);
            for (DetallePagoLote d : pl.detalle) {
                Empleado emp = empleado(ctx.idEmpresa, d.identificacion);
                if (emp == null) throw new IncomeException(d.ref + ": empleado inexistente");
                Nomina n = unico(em.createQuery("select n from Nomina n where n.periodoNomina.codigo = :p and n.empleado.codigo = :e", Nomina.class)
                        .setParameter("p", idPeriodo).setParameter("e", emp.getCodigo()).getResultList());
                DetalleOrdenPagoNomina dp = new DetalleOrdenPagoNomina();
                dp.setOrdenPagoNomina(o);
                dp.setEmpleado(emp);
                dp.setNomina(n);
                dp.setValor(redondea(nz(d.valor)));
                dp.setIdentificacion(recortar(emp.getIdentificacion(), 20));
                dp.setNombreBeneficiario(recortar((nvl(emp.getApellidos(), "") + " " + nvl(emp.getNombres(), "")).trim(), 200));
                dp.setRechazado("N");
                dp.setEstado((long) RhhEstadoDetalleOrdenPago.PAGADO);
                dp.setFechaRegistro(LocalDateTime.now());
                dp.setUsuarioRegistro(recortar(ctx.usuario, 60));
                em.persist(dp);
                if (n != null) n.setEstado((long) RhhEstadoNomina.PAGADA);
            }
            em.flush();
            Asiento a = contabilizacionNominaService.contabilizarPago(o.getCodigo(), fechaPago, ctx.usuario);
            o = em.find(OrdenPagoNomina.class, o.getCodigo());
            o.setEstado((long) RhhEstadoOrdenPago.CONFIRMADA);
            if (a != null) marcarAsiento(ctx, a.getCodigo(), "pago " + pl.ref);
            return item.creado(o.getCodigo(), a != null ? a.getCodigo() : null, numeroAsiento(a), pl.detalle.size() + " liquidos por " + redondea(total) + (pl.inferida ? " (fecha inferida)" : ""));
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    // =====================================================================================================
    // Planillas IESS
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarPlanillaIess(Contexto ctx, Long idPeriodo, PlanillaIessLote pl) throws Throwable {
        ItemResultado item = new ItemResultado("planilla-iess", pl.ref, pl.numeroComprobante);
        try {
            if (pl.tipo == null || pl.tipo < 1L || pl.tipo > 4L) return item.rechazado("tipo de planilla invalido: " + pl.tipo);
            if (pl.numeroComprobante == null || pl.numeroComprobante.trim().isEmpty()) return item.rechazado("sin numero de comprobante");
            double debe = 0;
            if (pl.pago != null) for (LineaPagoLote l : pl.pago.lineas) debe += nz(l.valor);
            boolean hayTablas = tablasPlanillaIess();
            String trazaPago = ctx.observacion + " | pago planilla IESS " + pl.ref;
            if (ctx.soloValidar) return item.validado("se registraria la planilla tipo " + pl.tipo + " por " + pl.valorIess
                    + (pl.pago != null ? " pagada el " + pl.pago.fecha + " (" + redondea(debe) + ")" : " sin pago")
                    + (hayTablas ? "" : "; ATENCION: la base no tiene RHH.PLIS/DLIS (script lap1-08): solo se generaria el asiento del pago"));

            if (!hayTablas) {
                // Base sin las tablas de planillas (producción anterior a lap1-08): se registra solo el pago contable.
                if (pl.pago == null) return item.omitido(null, "sin RHH.PLIS en la base y sin pago: nada que registrar");
                List<Asiento> previos = em.createQuery("select a from Asiento a where a.empresa.codigo = :e and a.observaciones like :o and a.estado = 1", Asiento.class)
                        .setParameter("e", ctx.idEmpresa).setParameter("o", "%" + trazaPago + " comprobante " + pl.numeroComprobante + "%").getResultList();
                if (!previos.isEmpty()) return item.omitido(previos.get(0).getCodigo(), "el pago de la planilla ya tiene asiento");
                Asiento a = asientoPagoPlanilla(ctx, pl, debe, trazaPago);
                return item.creado(null, a.getCodigo(), numeroAsiento(a), "pago de planilla tipo " + pl.tipo + " por " + redondea(debe) + " (sin RHH.PLIS en la base: solo asiento)");
            }

            List<PlanillaIess> previas = em.createQuery("select p from PlanillaIess p where p.empresa.codigo = :e and p.tipo = :t and p.numeroComprobante = :n", PlanillaIess.class)
                    .setParameter("e", ctx.idEmpresa).setParameter("t", pl.tipo).setParameter("n", pl.numeroComprobante.trim()).getResultList();
            if (!previas.isEmpty()) return item.omitido(previas.get(0).getCodigo(), "la planilla ya existe");

            PeriodoNomina periodo = em.find(PeriodoNomina.class, idPeriodo);
            PlanillaIess p = new PlanillaIess();
            p.setEmpresa(periodo.getEmpresa());
            p.setPeriodo(periodo);
            p.setTipo(pl.tipo);
            p.setNumeroComprobante(recortar(pl.numeroComprobante.trim(), 50));
            p.setFechaEmision(pl.fechaEmision == null ? null : fecha(pl.fechaEmision, "fechaEmision"));
            p.setFechaMaximaPago(pl.fechaMaximaPago == null ? null : fecha(pl.fechaMaximaPago, "fechaMaximaPago"));
            p.setValorIess(redondea(nz(pl.valorIess)));
            p.setValorControl(redondea(nz(pl.valorIess)));
            p.setDiferencia(0D);
            p.setEstado(2L);
            p.setObservacion(recortar(ctx.observacion + " | " + pl.ref, 1000));
            p.setFechaRegistro(LocalDateTime.now());
            p.setUsuario(ctx.idUsuario);
            em.persist(p);
            for (ComponentePlanillaLote c : pl.componentes) {
                DetallePlanillaIess d = new DetallePlanillaIess();
                d.setPlanilla(p);
                d.setConcepto(recortar(nvl(c.concepto, "componente"), 200));
                d.setConceptoTipo(c.conceptoTipo);
                d.setValorIess(redondea(nz(c.valor)));
                d.setValorControl(redondea(nz(c.valor)));
                d.setDiferencia(0D);
                em.persist(d);
            }
            Asiento a = null;
            if (pl.pago != null) {
                a = asientoPagoPlanilla(ctx, pl, debe, trazaPago);
                p.setAsiento(a);
                p.setFechaPago(fecha(pl.pago.fecha, "fecha del pago de la planilla"));
                p.setEstado(3L);
            }
            em.flush();
            return item.creado(p.getCodigo(), a != null ? a.getCodigo() : null, numeroAsiento(a), "planilla tipo " + pl.tipo + " por " + pl.valorIess + (a != null ? ", pagada" : ", sin pago"));
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    /** Asiento del débito del IESS: DEBE los pasivos del lote, HABER la cuenta contable de la cuenta bancaria. */
    private Asiento asientoPagoPlanilla(Contexto ctx, PlanillaIessLote pl, double debe, String traza) throws Throwable {
        LocalDate fp = fecha(pl.pago.fecha, "fecha del pago de la planilla");
        CuentaBancaria cb = em.find(CuentaBancaria.class, pl.pago.idCuentaBancaria);
        List<DetalleAsiento> lineas = new ArrayList<>();
        for (LineaPagoLote l : pl.pago.lineas) {
            if (nz(l.valor) == 0D) continue;
            lineas.add(linea(cuentaMovimiento(ctx.idEmpresa, l.cuenta), nvl(l.descripcion, "Planilla IESS"), redondea(nz(l.valor)), 0D));
        }
        lineas.add(linea(cb.getPlanCuenta(), "Pago planilla IESS " + pl.numeroComprobante, 0D, redondea(debe)));
        return asientoContableService.generarAsiento(ctx.idEmpresa, TipoAsientos.RECURSOS_HUMANOS, fp,
                recortar(traza + " comprobante " + pl.numeroComprobante, 1900), ctx.usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), "RRH");
    }

    /** RHH.PLIS y RHH.DLIS existen solo desde el script lap1-08; la base de producción del 28-oct-2026 no los tiene. */
    private boolean tablasPlanillaIess() {
        Long n = numero(unico(em.createNativeQuery("select count(*) from ALL_TABLES where OWNER = 'RHH' and TABLE_NAME in ('PLIS', 'DLIS')").getResultList()));
        return n != null && n == 2L;
    }

    // =====================================================================================================
    // Cierre
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cerrarPeriodo(Contexto ctx, Long idPeriodo) throws Throwable {
        ItemResultado item = new ItemResultado("cierre", ctx.mes, null);
        if (ctx.soloValidar) return item.validado("se cerraria el periodo si queda PAGADO");
        PeriodoNomina p = em.find(PeriodoNomina.class, idPeriodo);
        if (Long.valueOf(RhhEstadoPeriodoNomina.CERRADO).equals(p.getEstado())) return item.omitido(idPeriodo, "el periodo ya esta cerrado");
        if (!Long.valueOf(RhhEstadoPeriodoNomina.PAGADO).equals(p.getEstado()))
            return item.omitido(idPeriodo, "el periodo queda en estado " + p.getEstado() + " (no PAGADO): no se cierra");
        try {
            procesoNominaService.cerrarPeriodo(idPeriodo, ctx.usuario);
            return item.creado(idPeriodo, null, null, "periodo cerrado (acumulados escritos)");
        } catch (Throwable t) {
            sessionContext.setRollbackOnly();
            throw t;
        }
    }

    // =====================================================================================================
    // Utilidades
    // =====================================================================================================

    /** Centro de costo en las líneas de gasto 4.3.* y traza en la observación del asiento que generó el motor. */
    private int marcarAsiento(Contexto ctx, Long idAsiento, String que) {
        Asiento a = em.find(Asiento.class, idAsiento);
        if (a != null) a.setObservaciones(recortar(nvl(a.getObservaciones(), "") + " | " + ctx.observacion + " | " + que, 1900));
        CentroCosto cc = ctx.centroCosto == null ? null : centroCosto(ctx.idEmpresa, ctx.centroCosto);
        if (cc == null) return 0;
        List<DetalleAsiento> lineas = em.createQuery("select d from DetalleAsiento d where d.asiento.codigo = :a", DetalleAsiento.class).setParameter("a", idAsiento).getResultList();
        int n = 0;
        for (DetalleAsiento d : lineas) {
            String cuenta = d.getNumeroCuenta() != null ? d.getNumeroCuenta() : (d.getPlanCuenta() != null ? d.getPlanCuenta().getCuentaContable() : null);
            if (cuenta != null && cuenta.startsWith(RAIZ_CUENTAS_GASTO)) { d.setCentroCosto(cc); n++; }
        }
        return n;
    }

    private static DetalleAsiento linea(PlanCuenta cuenta, String descripcion, Double debe, Double haber) {
        DetalleAsiento d = new DetalleAsiento();
        d.setPlanCuenta(cuenta);
        d.setNumeroCuenta(cuenta.getCuentaContable());
        d.setNombreCuenta(cuenta.getNombre());
        d.setDescripcion(recortar(descripcion, 200));
        d.setValorDebe(debe);
        d.setValorHaber(haber);
        return d;
    }

    private PeriodoNomina periodoDelMes(Long idEmpresa, Integer anio, Integer mes) {
        return unico(em.createQuery("select p from PeriodoNomina p where p.empresa.codigo = :e and p.anio = :a and p.mes = :m and p.tipoPeriodo = :t "
                + "and (p.estado is null or p.estado <> :anulado) order by p.codigo", PeriodoNomina.class)
                .setParameter("e", idEmpresa).setParameter("a", anio).setParameter("m", mes).setParameter("t", (long) RhhTipoPeriodoNomina.MENSUAL)
                .setParameter("anulado", (long) RhhEstadoPeriodoNomina.ANULADO).getResultList());
    }

    private Empleado empleado(Long idEmpresa, String identificacion) {
        return unico(em.createQuery("select e from Empleado e where e.empresa.codigo = :e and e.identificacion = :i order by e.codigo", Empleado.class)
                .setParameter("e", idEmpresa).setParameter("i", identificacion.trim()).getResultList());
    }

    /** Contrato que cubre el mes (el de inicio más reciente hasta el fin de mes); si ninguno empieza antes, el primero. */
    private ContratoEmpleado contratoDelMes(Long idEmpleado, String mes) {
        LocalDate fin = LocalDate.parse(mes + "-01").plusMonths(1).minusDays(1);
        List<ContratoEmpleado> l = em.createQuery("select c from ContratoEmpleado c where c.empleado.codigo = :e order by c.fechaInicio desc, c.codigo desc", ContratoEmpleado.class)
                .setParameter("e", idEmpleado).getResultList();
        for (ContratoEmpleado c : l) if (c.getFechaInicio() == null || !c.getFechaInicio().isAfter(fin)) return c;
        return l.isEmpty() ? null : l.get(l.size() - 1);
    }

    private ConceptoNomina concepto(Long idEmpresa, String abreviatura) {
        if (abreviatura == null) return null;
        return unico(em.createQuery("select c from ConceptoNomina c where c.empresa.codigo = :e and upper(c.abreviatura) = :a order by c.codigo", ConceptoNomina.class)
                .setParameter("e", idEmpresa).setParameter("a", abreviatura.trim().toUpperCase()).getResultList());
    }

    private static Long tipoProvision(String tipo) {
        switch (tipo == null ? "" : tipo.trim().toUpperCase()) {
            case "D13": return (long) RhhTipoProvision.DECIMO_TERCERO;
            case "D14": return (long) RhhTipoProvision.DECIMO_CUARTO;
            case "VAC": return (long) RhhTipoProvision.VACACIONES;
            case "FR": return (long) RhhTipoProvision.FONDOS_DE_RESERVA;
            default: throw new IncomeException("tipo de provision desconocido: " + tipo);
        }
    }

    private String revisarPeriodoContable(Long idEmpresa, String mes) {
        int anio = Integer.parseInt(mes.substring(0, 4)), m = Integer.parseInt(mes.substring(5, 7));
        List<Periodo> periodos = em.createQuery("select p from Periodo p where p.empresa.codigo = :e and p.anio = :a and p.mes = :m", Periodo.class)
                .setParameter("e", idEmpresa).setParameter("a", (long) anio).setParameter("m", (long) m).getResultList();
        if (periodos.isEmpty()) return "No existe el periodo contable " + m + "/" + anio + " (CNT.PRDO)";
        Long estado = periodos.get(0).getEstado();
        if (Long.valueOf(EstadoPeriodos.CERRADO).equals(estado)) return "El periodo contable " + m + "/" + anio + " esta CERRADO";
        if (Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(estado)) return "El periodo contable " + m + "/" + anio + " esta MAYORIZADO: desmayorizar antes de cargar";
        return null;
    }

    private PlanCuenta cuentaMovimiento(Long idEmpresa, String cuenta) {
        if (cuenta == null || cuenta.trim().isEmpty()) throw new IncomeException("cuenta contable vacia");
        List<PlanCuenta> l = em.createQuery("select p from PlanCuenta p where p.empresa.codigo = :e and p.cuentaContable = :c", PlanCuenta.class)
                .setParameter("e", idEmpresa).setParameter("c", cuenta.trim()).getResultList();
        if (l.isEmpty()) throw new IncomeException("la cuenta " + cuenta + " no existe en el plan de la empresa " + idEmpresa);
        PlanCuenta p = l.get(0);
        if (!Long.valueOf(Estado.ACTIVO).equals(p.getEstado())) throw new IncomeException("la cuenta " + cuenta + " esta inactiva");
        if (!Long.valueOf(TipoCuentaContable.MOVIMIENTO).equals(p.getTipo())) throw new IncomeException("la cuenta " + cuenta + " es de agrupacion");
        return p;
    }

    private CentroCosto centroCosto(Long idEmpresa, String numero) {
        return unico(em.createQuery("select c from CentroCosto c where c.empresa.codigo = :e and c.numero = :n", CentroCosto.class)
                .setParameter("e", idEmpresa).setParameter("n", numero.trim()).getResultList());
    }

    private static String numeroAsiento(Asiento a) {
        return a == null ? null : (a.getNumeroAlternoModulo() != null ? a.getNumeroAlterno() + " / " + a.getNumeroAlternoModulo() : a.getNumeroAlterno());
    }

    private static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) throw new IncomeException(campo + " vacia");
        try {
            return LocalDate.parse(texto.trim().substring(0, 10));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            throw new IncomeException(campo + " invalida: " + texto + " (se espera AAAA-MM-DD)");
        }
    }

    private static <T> T unico(List<T> l) {
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    private static Long numero(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    static String recortar(String s, int largo) {
        if (s == null) return null;
        return s.length() <= largo ? s : s.substring(0, largo);
    }

    private static String nvl(String a, String b) {
        return a != null && !a.trim().isEmpty() ? a : b;
    }

    private static double nz(Double d) {
        return d == null ? 0d : d;
    }

    private static double redondea(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
