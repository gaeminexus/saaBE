package com.saa.ejb.historico.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cxc.service.AplicacionPagoCxcService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.historico.service.CargaHistoricaDocumentoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.CentroCosto;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.Periodo;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.cxc.DetalleRetencionV2;
import com.saa.model.cxc.Establecimiento;
import com.saa.model.cxc.Factura;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxc.GrupoProductoCobro;
import com.saa.model.cxc.ProductoCobro;
import com.saa.model.cxc.PuntoEmision;
import com.saa.model.cxc.RetencionV2;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.DetalleFacturaCompra;
import com.saa.model.cxp.DetalleRetencionCompraV2;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.historico.LoteHistoricoCxcCxp;
import com.saa.model.historico.LoteHistoricoCxcCxp.CobroLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.CompraLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.LineaLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.LineaRetencionLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.NotaCreditoCompraLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.NotaCreditoVentaLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.PagoLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.RetencionEmitidaLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.RetencionRecibidaLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.TitularLote;
import com.saa.model.historico.LoteHistoricoCxcCxp.VentaLote;
import com.saa.model.historico.ResultadoCargaHistorica.ItemResultado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.PersonaRol;
import com.saa.model.tsr.Titular;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.EstadoPagoFactura;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.EstadoPeriodos;
import com.saa.rubros.FormaPagoProgramado;
import com.saa.rubros.RolPersona;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoCuentaContable;
import com.saa.rubros.TipoDocPagoAplicacion;
import com.saa.rubros.TipoGrupoProductos;
import com.saa.rubros.TipoIdentificacion;
import com.saa.rubros.TipoPersona;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Trabajo por documento de la carga histórica de CxC/CxP (C11). Ver {@link CargaHistoricaDocumentoService}
 * y docs/logica-negocio/cxp/CARGA-HISTORICA-CXC-CXP.md.
 *
 * <p>Decisiones de diseño que explican el código:</p>
 * <ul>
 * <li><b>Cuenta contable por línea.</b> El motor de CxP toma la cuenta del gasto del GRUPO del producto
 * ({@code PGS.PRDP → PGS.GRPP.PLNNCDGO}); el de CxC, del grupo del producto de venta ({@code CBR.GRPC}). Para
 * imponer la cuenta del Excel sin tocar los motores, cada línea del lote se graba con un <i>producto histórico</i>
 * que vive en un grupo cuya cuenta es exactamente la de la línea ({@link #productoCompraParaCuenta},
 * {@link #productoVentaParaCuenta}): se reutiliza el grupo activo que ya tenga esa cuenta y, si no existe, se
 * crea uno {@code CARGA HISTORICA <cuenta>}. La operación normal no cambia: son productos como cualquier otro.</li>
 * <li><b>Centro de costo.</b> Ningún motor lo llena. Después de que el motor genera el asiento, se marca
 * {@code CNT.DTAS.CNCSCDGO} en las líneas de gasto (las de la cuenta de la línea), no en IVA ni proveedor
 * ({@link #imponerCentroCosto}).</li>
 * <li><b>Documentos ya autorizados sin SRI.</b> Las facturas de compra se graban como lo hace la bandeja
 * ({@code estado 1, estadoEmision 2, autorizacion = clave}); las ventas y retenciones emitidas se graban con su
 * número y clave del Excel en estado 5 (autorizada) directamente por JPA, sin pasar por {@code saveSingle} para
 * no consumir el secuencial de {@code CBR.NXPE}; el punto de emisión 002 de 2025 se crea si no existe.</li>
 * <li><b>Fecha contable.</b> Siempre la del documento (D-003): los motores de factura, NC y retención ya la usan;
 * los pagos van con {@code fechaProgramada = fechaRespuesta = fechaPago} y los cobros con {@code fechaCobro}.</li>
 * <li><b>Transacción por documento.</b> Cada método es REQUIRES_NEW y ante cualquier error marca rollback y
 * relanza; el orquestador lo registra como RECHAZADO y sigue con el siguiente.</li>
 * </ul>
 */
@Stateless
public class CargaHistoricaDocumentoServiceImpl implements CargaHistoricaDocumentoService {

    static final String PREFIJO_OBSERVACION = "CARGA HISTORICA [";
    static final String PREFIJO_PRODUCTO = "CARGA HISTORICA ";
    static final String TIPO_COMPROBANTE_FACTURA = "01";
    static final String TIPO_COMPROBANTE_RETENCION = "07";
    static final long ESTADO_EMISION_REGISTRADA_CXP = 2L;
    static final long ESTADO_AUTORIZADA_CXC = 5L;
    static final long ESTADO_EMISION_OK_CXC = 1L;
    static final String COD_IMPUESTO_RENTA = "1";
    static final String COD_IMPUESTO_IVA = "2";
    static final String TIPO_DOC_SUSTENTO_FACTURA = "01";
    static final String FORMA_PAGO_SRI_DEFECTO = "20";
    static final double TOLERANCIA = 0.011;
    static final long RUBRO_TIPO_GRUPO_P = 2L;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AplicacionPagoCxpService aplicacionPagoCxpService;

    @EJB
    private AplicacionPagoCxcService aplicacionPagoCxcService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    // =====================================================================================================
    // Validación del lote
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<String> validarLote(Contexto ctx, LoteHistoricoCxcCxp lote) throws Throwable {
        System.out.println("Ingresa al metodo validarLote de CargaHistoricaDocumentoService, mes " + ctx.mes);
        List<String> errores = new ArrayList<>();
        if (em.find(Empresa.class, ctx.idEmpresa) == null) errores.add("No existe la empresa " + ctx.idEmpresa);
        Usuario usuario = ctx.idUsuario == null ? null : em.find(Usuario.class, ctx.idUsuario);
        if (usuario == null) errores.add("El usuario " + ctx.idUsuario + " no existe en SCP.PJRQ (por defecto 38, usuario 1)");
        else if (ctx.usuario == null || ctx.usuario.trim().isEmpty()) ctx.usuario = usuario.getNombre() != null ? usuario.getNombre() : "SISTEMA";
        if (ctx.mes == null || !ctx.mes.matches("\\d{4}-\\d{2}")) { errores.add("mes debe ser AAAA-MM"); return errores; }

        // Períodos: el del mes y los de las fechas de todos los documentos (un pago de junio puede ser de una factura de mayo, pero su fecha es junio)
        Set<String> meses = new LinkedHashSet<>();
        boolean soloSaldosIniciales = lote.compras.isEmpty() && lote.retencionesEmitidas.isEmpty() && lote.pagos.isEmpty() && lote.ventas.isEmpty()
                && lote.cobros.isEmpty() && lote.retencionesRecibidas.isEmpty() && lote.ncCompras.isEmpty() && lote.ncVentas.isEmpty();
        // Un lote que solo trae saldos iniciales (D-013) no genera asientos: no exige el período del mes abierto
        if (!soloSaldosIniciales) meses.add(ctx.mes);
        if (lote.saldosIniciales != null) errores.addAll(validarSaldosIniciales(ctx, lote.saldosIniciales));
        for (CompraLote c : lote.compras) if (c.fechaEmision != null) meses.add(c.fechaEmision.substring(0, 7));
        for (RetencionEmitidaLote r : lote.retencionesEmitidas) if (r.fechaEmision != null) meses.add(r.fechaEmision.substring(0, 7));
        for (PagoLote p : lote.pagos) if (p.fechaPago != null) meses.add(p.fechaPago.substring(0, 7));
        for (VentaLote v : lote.ventas) if (v.fechaEmision != null) meses.add(v.fechaEmision.substring(0, 7));
        for (CobroLote c : lote.cobros) if (c.fechaCobro != null) meses.add(c.fechaCobro.substring(0, 7));
        for (RetencionRecibidaLote r : lote.retencionesRecibidas) if (r.fechaEmision != null) meses.add(r.fechaEmision.substring(0, 7));
        for (NotaCreditoCompraLote n : lote.ncCompras) if (n.fechaEmision != null) meses.add(n.fechaEmision.substring(0, 7));
        for (NotaCreditoVentaLote n : lote.ncVentas) if (n.fechaEmision != null) meses.add(n.fechaEmision.substring(0, 7));
        for (String m : meses) {
            String e = revisarPeriodo(ctx.idEmpresa, m);
            if (e != null) errores.add(e);
        }
        if (!meses.isEmpty() && (!ctx.mes.equals(meses.iterator().next()) || meses.size() > 1)) {
            List<String> otros = new ArrayList<>(meses);
            otros.remove(ctx.mes);
            if (!otros.isEmpty()) System.out.println("ℹ El lote " + ctx.mes + " trae documentos fechados en otros meses: " + otros);
        }

        // Facturador (ventas y retenciones emitidas)
        if (!lote.ventas.isEmpty() || !lote.retencionesEmitidas.isEmpty() || !lote.ncVentas.isEmpty()) {
            Facturador f = facturadorDeEmpresa(ctx.idEmpresa);
            if (f == null) errores.add("La empresa " + ctx.idEmpresa + " no tiene facturador (CBR.FCDR)");
            else if (!Long.valueOf(1L).equals(f.getGeneraConta())) errores.add("El facturador " + f.getId() + " no genera contabilidad (GENERACONTA <> 1)");
        }

        // Cuentas y centros de costo de las líneas
        Set<String> cuentas = new LinkedHashSet<>();
        Set<String> centros = new LinkedHashSet<>();
        for (CompraLote c : lote.compras) {
            for (LineaLote l : c.lineas) { if (l.cuenta != null) cuentas.add(l.cuenta); if (l.centroCosto != null) centros.add(l.centroCosto); }
            if (c.centroCosto != null) centros.add(c.centroCosto);
        }
        for (NotaCreditoCompraLote n : lote.ncCompras) { if (n.cuenta != null) cuentas.add(n.cuenta); if (n.centroCosto != null) centros.add(n.centroCosto); }
        for (VentaLote v : lote.ventas) for (LineaLote l : v.lineas) if (l.cuenta != null) cuentas.add(l.cuenta);
        for (NotaCreditoVentaLote n : lote.ncVentas) if (n.cuentaIngreso != null) cuentas.add(n.cuentaIngreso);
        for (String c : cuentas) {
            try { cuentaMovimiento(ctx.idEmpresa, c); } catch (IncomeException e) { errores.add(e.getMessage()); }
        }
        for (String c : centros) {
            CentroCosto cc = centroCosto(ctx.idEmpresa, c);
            if (cc == null) errores.add("No existe el centro de costo " + c + " en CNT.CNCS para la empresa " + ctx.idEmpresa
                    + " (correr docs/logica-negocio/cnt/sql/c11-01-centros-de-costo-asoprep.sql)");
            else if (!Long.valueOf(Estado.ACTIVO).equals(cc.getEstado())) errores.add("El centro de costo " + c + " está inactivo");
        }

        // Cuentas bancarias de pagos y cobros
        Set<Long> cuentasBancarias = new LinkedHashSet<>();
        for (PagoLote p : lote.pagos) if (p.idCuentaBancaria != null) cuentasBancarias.add(p.idCuentaBancaria);
        for (CobroLote c : lote.cobros) if (c.idCuentaBancaria != null) cuentasBancarias.add(c.idCuentaBancaria);
        for (Long id : cuentasBancarias) {
            CuentaBancaria cb = em.find(CuentaBancaria.class, id);
            if (cb == null) errores.add("No existe la cuenta bancaria " + id + " (TSR.CNBC)");
            else if (cb.getPlanCuenta() == null) errores.add("La cuenta bancaria " + id + " (" + cb.getNumeroCuenta() + ") no tiene cuenta contable");
            else if (cb.getBanco() == null || cb.getBanco().getEmpresa() == null || !ctx.idEmpresa.equals(cb.getBanco().getEmpresa().getCodigo()))
                errores.add("La cuenta bancaria " + id + " (" + cb.getNumeroCuenta() + ") no es de la empresa " + ctx.idEmpresa);
        }
        return errores;
    }

    private String revisarPeriodo(Long idEmpresa, String mes) {
        int anio = Integer.parseInt(mes.substring(0, 4)), m = Integer.parseInt(mes.substring(5, 7));
        List<Periodo> periodos = em.createQuery("select p from Periodo p where p.empresa.codigo = :e and p.anio = :a and p.mes = :m", Periodo.class)
                .setParameter("e", idEmpresa).setParameter("a", (long) anio).setParameter("m", (long) m).getResultList();
        if (periodos.isEmpty()) return "No existe el periodo contable " + m + "/" + anio + " (CNT.PRDO)";
        Long estado = periodos.get(0).getEstado();
        if (Long.valueOf(EstadoPeriodos.CERRADO).equals(estado)) return "El periodo contable " + m + "/" + anio + " esta CERRADO";
        if (Long.valueOf(EstadoPeriodos.MAYORIZADO).equals(estado)) return "El periodo contable " + m + "/" + anio + " esta MAYORIZADO: desmayorizar antes de cargar";
        return null;
    }

    // =====================================================================================================
    // Titulares
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarTitular(Contexto ctx, TitularLote t) throws Throwable {
        ItemResultado item = new ItemResultado("titular", t.identificacion, descripcionTitular(t));
        try {
            if (t.identificacion == null || t.identificacion.trim().isEmpty()) return item.rechazado("identificacion vacia");
            Titular titular = titularPorIdentificacion(t.identificacion);
            boolean nuevo = titular == null;
            List<String> hechos = new ArrayList<>();
            if (ctx.soloValidar) {
                return item.validado(nuevo ? "se crearia el titular" + (t.esProveedor ? " proveedor" : "") + (t.esCliente ? " cliente" : "") : "ya existe (id " + titular.getCodigo() + ")");
            }
            titular = asegurarTitular(ctx, t, titular, hechos);
            em.flush();
            if (hechos.isEmpty()) return item.omitido(titular.getCodigo(), "ya existe con sus roles y cuentas");
            return item.creado(titular.getCodigo(), null, null, String.join("; ", hechos));
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /**
     * Crea el titular si {@code titular} es null o completa banderas, y garantiza rol y cuenta PRCC de cada rol
     * pedido. Anota en {@code hechos} lo que hizo. Lo usan la carga de titulares y la reasignación de saldo inicial.
     */
    private Titular asegurarTitular(Contexto ctx, TitularLote t, Titular titular, List<String> hechos) throws Throwable {
        {
            boolean nuevo = titular == null;
            if (nuevo) {
                titular = new Titular();
                titular.setIdentificacion(t.identificacion.trim());
                boolean juridica = "JURIDICA".equalsIgnoreCase(t.tipoPersona);
                String nombre = t.razonSocial != null && !t.razonSocial.trim().isEmpty() ? t.razonSocial.trim() : t.identificacion;
                titular.setNombre(nombre);
                titular.setRazonSocial(juridica ? nombre : null);
                titular.setRubroTipoPersonaP((long) Rubros.TIPO_PERSONA);
                titular.setRubroTipoPersonaH((long) (juridica ? TipoPersona.JURIDICO : TipoPersona.NATURAL));
                titular.setRubroTipoIdentificacionP((long) Rubros.TIPO_IDENTIFICACION);
                int tipoId = "RUC".equalsIgnoreCase(t.tipoIdentificacion) ? TipoIdentificacion.RUC
                        : "CEDULA".equalsIgnoreCase(t.tipoIdentificacion) ? TipoIdentificacion.CEDULA_IDENTIDAD : TipoIdentificacion.PASAPORTE;
                titular.setRubroTipoIdentificacionH((long) tipoId);
                titular.setTipoCliente(t.esCliente ? 1L : 0L);
                titular.setTipoProveedor(t.esProveedor ? 1L : 0L);
                titular.setTipoBeneficiario(0L);
                titular.setTipoEmpleado(0L);
                titular.setTipoSocio(0L);
                titular.setAplicaIVA(1L);
                titular.setAplicaRetencion(1L);
                titular.setExtranjero(0L);
                titular.setEstado((long) Estado.ACTIVO);
                titular.setTipoProveedorAts(juridica ? "02" : "01");
                titular.setDireccion(ctx.observacion);
                em.persist(titular);
                hechos.add("titular creado");
            } else {
                if (t.esProveedor && !Long.valueOf(1L).equals(titular.getTipoProveedor())) { titular.setTipoProveedor(1L); hechos.add("marcado proveedor"); }
                if (t.esCliente && !Long.valueOf(1L).equals(titular.getTipoCliente())) { titular.setTipoCliente(1L); hechos.add("marcado cliente"); }
            }
            if (t.esProveedor) hechos.addAll(asegurarRolYCuenta(ctx, titular, RolPersona.PROVEEDOR, "2.3.90.10"));
            if (t.esCliente) hechos.addAll(asegurarRolYCuenta(ctx, titular, RolPersona.CLIENTE, "1.4.90.10"));
            return titular;
        }
    }

    /**
     * Garantiza el rol (TSR.PRRL, rubro 55) y la cuenta de facturas (TSR.PRCC tipo 1) del titular en la empresa,
     * con la cuenta de cartera de la práctica actual del SAA: 2.3.90.10 proveedores, 1.4.90.10 clientes. Es lo
     * que los motores exigen ({@code existeCuentaConRolEstricto}) y lo que la creación automática desde el XML
     * del SRI no hace.
     */
    private List<String> asegurarRolYCuenta(Contexto ctx, Titular titular, int rol, String cuenta) throws Throwable {
        List<String> hechos = new ArrayList<>();
        List<PersonaRol> roles = em.createQuery("select r from PersonaRol r where r.titular.codigo = :t and r.empresa.codigo = :e and r.rubroRolPersonaH = :rol", PersonaRol.class)
                .setParameter("t", titular.getCodigo()).setParameter("e", ctx.idEmpresa).setParameter("rol", (long) rol).getResultList();
        PersonaRol pr;
        if (roles.isEmpty()) {
            pr = new PersonaRol();
            pr.setTitular(titular);
            pr.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            pr.setRubroRolPersonaP((long) Rubros.ROL_PERSONA);
            pr.setRubroRolPersonaH((long) rol);
            pr.setEstado((long) Estado.ACTIVO);
            pr.setDiasVencimientoFactura(0L);
            pr.setDiasCredito(0L);
            em.persist(pr);
            hechos.add("rol " + (rol == RolPersona.PROVEEDOR ? "proveedor" : "cliente") + " creado");
        } else {
            pr = roles.get(0);
            if (!Long.valueOf(Estado.ACTIVO).equals(pr.getEstado())) { pr.setEstado((long) Estado.ACTIVO); hechos.add("rol reactivado"); }
        }
        List<PersonaCuentaContable> cuentas = em.createQuery("select c from PersonaCuentaContable c where c.personaRol.codigo = :r and c.tipoCuenta = 1", PersonaCuentaContable.class)
                .setParameter("r", pr.getCodigo()).getResultList();
        if (cuentas.isEmpty()) {
            PersonaCuentaContable pcc = new PersonaCuentaContable();
            pcc.setPersonaRol(pr);
            pcc.setEmpresa(pr.getEmpresa());
            pcc.setTipoCuenta(1L);
            pcc.setTipoPersona((long) rol);
            pcc.setPlanCuenta(cuentaMovimiento(ctx.idEmpresa, cuenta));
            pcc.setSaldoInicial(0d);
            em.persist(pcc);
            hechos.add("cuenta " + cuenta + " asignada");
        }
        return hechos;
    }

    // =====================================================================================================
    // Compras
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarCompra(Contexto ctx, CompraLote c) throws Throwable {
        ItemResultado item = new ItemResultado("compra", c.ref, c.numero + " " + nvl(c.razonSocial, ""));
        try {
            if (c.claveAcceso == null || !c.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            if (c.requiereConfirmacion && !ctx.aceptarInferidos) return item.rechazado("cuenta inferida sin confirmar (" + c.origenCuenta + "); cargar con aceptarInferidos=true o corregir el Excel");
            FacturaCompra existente = unico(em.createQuery("select f from FacturaCompra f where f.clave = :c and f.estado = 1", FacturaCompra.class).setParameter("c", c.claveAcceso).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la factura de compra con esa clave (id " + existente.getId() + ", asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular proveedor = titularPorIdentificacion(c.proveedor);
            if (proveedor == null) return item.rechazado("el proveedor " + c.proveedor + " no existe en TSR.TTLR (cargar titulares primero)");
            if (!asientoContableService.existeCuentaConRolEstricto(proveedor.getCodigo(), ctx.idEmpresa, 1L, RolPersona.PROVEEDOR))
                return item.rechazado("el proveedor no tiene cuenta contable con rol proveedor (TSR.PRCC tipo 1)");
            LocalDate fe = fecha(c.fechaEmision, "fechaEmision");
            if (c.lineas == null || c.lineas.isEmpty()) return item.rechazado("sin lineas");
            double sumaBases = 0, sumaIva = 0;
            for (LineaLote l : c.lineas) { sumaBases += nz(l.base); sumaIva += nz(l.iva); }
            double dif = redondea(nz(c.total) - sumaBases - sumaIva);
            if (Math.abs(dif) > 0.50) return item.rechazado("bases + IVA difieren del total en " + dif + " (el motor tolera 0,50)");
            Set<String> cuentasGasto = new LinkedHashSet<>();
            CentroCosto centro = null;
            for (LineaLote l : c.lineas) {
                cuentaMovimiento(ctx.idEmpresa, l.cuenta);
                cuentasGasto.add(l.cuenta);
                String cc = l.centroCosto != null ? l.centroCosto : c.centroCosto;
                if (cc == null) return item.rechazado("linea sin centro de costo");
                CentroCosto cco = centroCosto(ctx.idEmpresa, cc);
                if (cco == null) return item.rechazado("no existe el centro de costo " + cc);
                if (centro != null && !centro.getCodigo().equals(cco.getCodigo())) return item.rechazado("mas de un centro de costo en la misma factura: no soportado");
                centro = cco;
            }
            if (ctx.soloValidar) return item.validado("se crearia la factura de compra por " + c.total + " a " + String.join(",", cuentasGasto) + " centro " + centro.getNumero());

            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            FacturaCompra fc = new FacturaCompra();
            fc.setTipoComprobante(TIPO_COMPROBANTE_FACTURA);
            fc.setTipoDoc(TIPO_COMPROBANTE_FACTURA);
            fc.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            fc.setTitular(proveedor);
            fc.setNumero(c.numero);
            String[] partes = partesNumero(c.numero);
            fc.setNumEstablecimiento(partes[0]);
            fc.setNumPtoEmision(partes[1]);
            fc.setSecuencial(partes[2]);
            fc.setAmbiente(ambienteDeClave(c.claveAcceso));
            fc.setClave(c.claveAcceso);
            fc.setFecha(fe.atStartOfDay());
            fc.setObservacion(recortar(observacionDocumento(ctx, c.ref, "Factura de compra " + c.numero, c.tipoGasto, c.observacionExcel, c.cuentaInferida ? "cuenta " + c.cuenta + " por " + c.origenCuenta : null), 2000));
            double base0 = 0, base5 = 0, base15 = 0, base8 = 0, baseNoObj = 0, baseExenta = 0, iva5 = 0, iva8 = 0, iva15 = 0;
            for (LineaLote l : c.lineas) {
                int tarifa = l.tarifa == null ? 0 : l.tarifa;
                int cod = l.codigoIvaSri == null ? (tarifa == 15 ? 4 : tarifa == 5 ? 5 : tarifa == 8 ? 8 : 0) : l.codigoIvaSri;
                if (cod == 6) baseNoObj += nz(l.base);
                else if (cod == 7) baseExenta += nz(l.base);
                else if (tarifa == 15) { base15 += nz(l.base); iva15 += nz(l.iva); }
                else if (tarifa == 5) { base5 += nz(l.base); iva5 += nz(l.iva); }
                else if (tarifa == 8) { base8 += nz(l.base); iva8 += nz(l.iva); }
                else base0 += nz(l.base);
            }
            fc.setSubtotal(redondea(sumaBases));
            fc.setSubcero(redondea(base0));
            fc.setSubnoobj(redondea(baseNoObj));
            fc.setSubexent(redondea(baseExenta));
            fc.setSubtotal5(redondea(base5));
            fc.setSubtotal8(redondea(base8));
            fc.setpIVA(base15 > 0 ? 15d : base5 > 0 ? 5d : base8 > 0 ? 8d : 0d);
            fc.setvIVA(redondea(nz(c.iva) > 0 ? nz(c.iva) : iva15 + iva5 + iva8));
            fc.setvIVA5(redondea(iva5));
            fc.setvIVA8(redondea(iva8));
            fc.setvICE(0d);
            fc.setvIRBPNR(0d);
            fc.setDescuento(0d);
            fc.setPorDescuento(0d);
            fc.setPropina(0d);
            fc.setTotal(redondea(nz(c.total)));
            fc.setUsuario(usuario);
            fc.setAutorizacion(c.claveAcceso);
            fc.setFechaAutorizacion(fecha(nvl(c.fechaAutorizacion, c.fechaEmision), "fechaAutorizacion").atStartOfDay());
            fc.setEstado((long) Estado.ACTIVO);
            fc.setEstadoEmision(ESTADO_EMISION_REGISTRADA_CXP);
            fc.setEstadoPago((long) EstadoPagoFactura.PENDIENTE);
            fc.setEsIntermediario(0L);
            fc.setEsReembolso(0L);
            fc.setEstadoSeguro(0L);
            fc.setFechaRegistroContable(fe);
            em.persist(fc);

            int n = 0;
            for (LineaLote l : c.lineas) {
                ProductoPago producto = productoCompraParaCuenta(ctx, l.cuenta);
                DetalleFacturaCompra d = new DetalleFacturaCompra();
                d.setFactura(fc);
                d.setDescripcion(recortar(nvl(l.descripcion, "Linea " + (++n)), 500));
                d.setCantidad(1d);
                d.setValor(redondea(nz(l.base)));
                d.setSubTotal(redondea(nz(l.base)));
                d.setDescuento(0d);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeIVA(l.tarifa == null ? 0L : l.tarifa.longValue());
                d.setValorIVA(redondea(nz(l.iva)));
                d.setPorcentajeICE(0L);
                d.setValorICE(0d);
                d.setTotal(redondea(nz(l.base) + nz(l.iva)));
                d.setProducto(producto.getId());
                d.setCodigoIVASRI(l.codigoIvaSri == null ? null : l.codigoIvaSri.longValue());
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();

            String obsAsiento = recortar("Factura compra: " + c.numero + " | Proveedor: " + nvl(proveedor.getNombre(), c.proveedor) + " | " + ctx.observacion, 2000);
            Asiento asiento = asientoContableService.generarAsientoFacturaCompra(fc.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_COMPRA, fe, obsAsiento, ctx.usuario);
            Asiento asientoAttached = em.find(Asiento.class, asiento.getCodigo());
            fc.setAsiento(asientoAttached != null ? asientoAttached : asiento);
            em.merge(fc);
            int marcadas = imponerCentroCosto(asiento.getCodigo(), cuentasGasto, centro);
            em.flush();
            return item.creado(fc.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "factura de compra creada; " + marcadas + " linea(s) de gasto con centro " + centro.getNumero());
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /**
     * Marca el centro de costo en las líneas del asiento cuya cuenta es una de las cuentas de gasto de la factura
     * (no en IVA, proveedor ni redondeo). Es el único punto donde la carga histórica escribe sobre el asiento que
     * generó el motor.
     */
    private int imponerCentroCosto(Long idAsiento, Set<String> cuentasGasto, CentroCosto centro) {
        if (centro == null) return 0;
        List<DetalleAsiento> lineas = em.createQuery("select d from DetalleAsiento d where d.asiento.codigo = :a", DetalleAsiento.class).setParameter("a", idAsiento).getResultList();
        int n = 0;
        for (DetalleAsiento d : lineas) {
            String cuenta = d.getNumeroCuenta() != null ? d.getNumeroCuenta() : (d.getPlanCuenta() != null ? d.getPlanCuenta().getCuentaContable() : null);
            if (cuenta != null && cuentasGasto.contains(cuenta)) {
                d.setCentroCosto(centro);
                em.merge(d);
                n++;
            }
        }
        return n;
    }

    /**
     * Producto de compra cuyo grupo tiene exactamente la cuenta pedida. Reutiliza el grupo activo de la empresa
     * con esa cuenta (el de más productos si hay varios) y dentro de él el producto {@code CARGA HISTORICA <cuenta>};
     * crea grupo y/o producto si faltan.
     */
    private ProductoPago productoCompraParaCuenta(Contexto ctx, String cuenta) throws Throwable {
        PlanCuenta plan = cuentaMovimiento(ctx.idEmpresa, cuenta);
        String nombre = PREFIJO_PRODUCTO + cuenta;
        List<ProductoPago> existentes = em.createQuery("select p from ProductoPago p where p.empresa.codigo = :e and upper(p.nombre) = :n and p.grupoProducto.planCuenta.codigo = :pc", ProductoPago.class)
                .setParameter("e", ctx.idEmpresa).setParameter("n", nombre.toUpperCase()).setParameter("pc", plan.getCodigo()).getResultList();
        if (!existentes.isEmpty()) return existentes.get(0);
        List<GrupoProductoPago> grupos = em.createQuery("select g from GrupoProductoPago g where g.empresa.codigo = :e and g.planCuenta.codigo = :pc and g.estado = 1 "
                + "and (g.rubroTipoGrupoH is null or g.rubroTipoGrupoH <> :porClasificar) order by (select count(x) from ProductoPago x where x.grupoProducto = g) desc, g.codigo", GrupoProductoPago.class)
                .setParameter("e", ctx.idEmpresa).setParameter("pc", plan.getCodigo()).setParameter("porClasificar", (long) TipoGrupoProductos.POR_CLASIFICAR).getResultList();
        GrupoProductoPago grupo;
        if (grupos.isEmpty()) {
            grupo = new GrupoProductoPago();
            grupo.setNombre(recortar(PREFIJO_PRODUCTO + cuenta + " " + nvl(plan.getNombre(), ""), 100));
            grupo.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            grupo.setPlanCuenta(plan);
            grupo.setRubroTipoGrupoP(RUBRO_TIPO_GRUPO_P);
            grupo.setRubroTipoGrupoH((long) TipoGrupoProductos.SERVICIO);
            // CK_GRPP_CSUS admite NULL o '03','04','06','07','08'; los 72 grupos existentes lo tienen en NULL y el sustento
            // de cada factura lo resuelve resolverSiFalta: se deja NULL, igual que los grupos creados a mano.
            grupo.setSustentoTributarioDefecto(null);
            grupo.setEstado((long) Estado.ACTIVO);
            em.persist(grupo);
            System.out.println("ℹ Grupo de producto de compras creado para la cuenta " + cuenta + ": " + grupo.getNombre());
        } else {
            grupo = grupos.get(0);
        }
        ProductoPago p = new ProductoPago();
        p.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
        p.setGrupoProducto(grupo);
        p.setNombre(nombre);
        p.setDescripcion("Producto generico de la carga historica de CxP (C11) para la cuenta " + cuenta);
        p.setPrecioUnitario(0d);
        p.setEstado((long) Estado.ACTIVO);
        em.persist(p);
        em.flush();
        return p;
    }

    // =====================================================================================================
    // Retenciones emitidas (CBR.RTV2 / DRV2)
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarRetencionEmitida(Contexto ctx, RetencionEmitidaLote r) throws Throwable {
        ItemResultado item = new ItemResultado("retencion-emitida", r.ref, r.numero + " s/ " + r.facturaNumero);
        try {
            if (r.claveAcceso == null || !r.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            RetencionV2 existente = unico(em.createQuery("select x from RetencionV2 x where x.clave = :c and x.estadoEmision <> 3", RetencionV2.class).setParameter("c", r.claveAcceso).getResultList());
            if (existente == null) existente = unico(em.createQuery("select x from RetencionV2 x where x.numero = :n and x.facturador.empresa.codigo = :e and x.estadoEmision <> 3", RetencionV2.class).setParameter("n", r.numero).setParameter("e", ctx.idEmpresa).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la retencion " + existente.getNumero() + " (estado " + existente.getEstado() + ", asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular proveedor = titularPorIdentificacion(r.proveedor);
            if (proveedor == null) return item.rechazado("el proveedor " + r.proveedor + " no existe en TSR.TTLR");
            FacturaCompra factura = facturaCompraDe(ctx, r.facturaClave, r.facturaNumero, proveedor.getCodigo());
            if (factura == null) return item.rechazado("la factura sustento " + r.facturaNumero + " no esta cargada en PGS.FCTC");
            if (r.lineas == null || r.lineas.isEmpty()) return item.rechazado("sin lineas de retencion");
            LocalDate fe = fecha(r.fechaEmision, "fechaEmision");
            double total = 0;
            for (LineaRetencionLote l : r.lineas) {
                if (!"RENTA".equalsIgnoreCase(l.impuesto) && !"IVA".equalsIgnoreCase(l.impuesto)) return item.rechazado("impuesto desconocido " + l.impuesto);
                if (l.codigoSri == null || l.codigoSri.isEmpty()) return item.rechazado("linea " + l.impuesto + " sin codigo del SRI");
                if (cuentaRetencionEmitida(l.impuesto, l.codigoSri) == null) return item.rechazado("el codigo " + l.codigoSri + " (" + l.impuesto + ") no tiene cuenta en CBR.TSRI");
                total += nz(l.valor);
            }
            total = redondea(total);
            if (Math.abs(total - nz(r.total)) > TOLERANCIA) return item.rechazado("la suma de lineas " + total + " no coincide con el total " + r.total);
            Map<String, Object> saldo = aplicacionPagoCxpService.saldoFactura(factura.getId());
            double saldoPendiente = numero(saldo.get("saldoPendiente"));
            if (total > saldoPendiente + TOLERANCIA) return item.rechazado("la retencion " + total + " supera el saldo de la factura " + saldoPendiente + " (ya pagada o con otra retencion)");
            Facturador facturador = facturadorDeEmpresa(ctx.idEmpresa);
            if (facturador == null) return item.rechazado("la empresa no tiene facturador");
            String[] partes = partesNumero(r.numero);
            if (ctx.soloValidar) return item.validado("se crearia la retencion " + r.numero + " por " + total + " contra la factura " + factura.getNumero() + " (saldo " + saldoPendiente + ")");

            PuntoEmision punto = puntoEmision(facturador, partes[0], partes[1], true);
            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            RetencionV2 ret = new RetencionV2();
            ret.setTipoComprobante(TIPO_COMPROBANTE_RETENCION);
            ret.setFacturador(facturador);
            ret.setProveedor(proveedor);
            ret.setTipoDoc(TIPO_DOC_SUSTENTO_FACTURA);
            ret.setPeriodoFiscal(nvl(r.periodoFiscal, fe.getMonthValue() < 10 ? "0" + fe.getMonthValue() + "/" + fe.getYear() : fe.getMonthValue() + "/" + fe.getYear()));
            ret.setNumero(r.numero);
            ret.setNumEstablecimiento(partes[0]);
            ret.setNumPtoEmision(partes[1]);
            ret.setSecuencial(partes[2]);
            ret.setAmbiente(facturador.getAmbiente() != null ? facturador.getAmbiente() : ambienteDeClave(r.claveAcceso));
            ret.setClave(r.claveAcceso);
            ret.setFecha(fe.atStartOfDay());
            ret.setObservacion(recortar(observacionDocumento(ctx, r.ref, "Retencion " + r.numero + " sobre " + r.facturaNumero, r.fuente, null, null), 2000));
            ret.setTotal(total);
            ret.setPtoEmision(punto);
            ret.setUsuario(usuario);
            ret.setAutorizacion(r.claveAcceso);
            ret.setFechaAutorizacion(fecha(nvl(r.fechaAutorizacion, r.fechaEmision), "fechaAutorizacion").atStartOfDay());
            ret.setEstado(ESTADO_AUTORIZADA_CXC);
            ret.setEstadoEmision(ESTADO_EMISION_OK_CXC);
            em.persist(ret);
            for (LineaRetencionLote l : r.lineas) {
                DetalleRetencionV2 d = new DetalleRetencionV2();
                d.setRetencionV2(ret);
                d.setTipoDocReten(TIPO_DOC_SUSTENTO_FACTURA);
                d.setNumDocReten(factura.getNumero());
                d.setFechaEmiDoc(factura.getFecha() != null ? factura.getFecha().toLocalDate() : null);
                d.setFechaReg(fe);
                d.setDocResAutorizacion(factura.getAutorizacion());
                d.setDocResTotalSinImpuestos(factura.getSubtotal());
                d.setDocResIvaCero(factura.getSubcero());
                d.setDocResPorIva(factura.getpIVA());
                d.setDocResTotalIva(factura.getvIVA());
                d.setDocResTotal(factura.getTotal());
                d.setDocResForPago(FORMA_PAGO_SRI_DEFECTO);
                d.setCodImpuesto("RENTA".equalsIgnoreCase(l.impuesto) ? COD_IMPUESTO_RENTA : COD_IMPUESTO_IVA);
                d.setCodRetencion(l.codigoSri);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeReten(l.porcentaje);
                d.setValorReten(redondea(nz(l.valor)));
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();
            String obs = recortar("Retención V2 N° " + r.numero + " | Proveedor: " + nvl(proveedor.getNombre(), r.proveedor) + " | Factura: " + factura.getNumero() + " | " + ctx.observacion, 2000);
            Asiento asiento = asientoContableService.generarAsientoRetencionV2(ret.getId(), ctx.idEmpresa, TipoAsientos.RETENCIONES_EMITIDAS_V2, fe, obs, ctx.usuario);
            Asiento attached = em.find(Asiento.class, asiento.getCodigo());
            ret.setAsiento(attached != null ? attached : asiento);
            em.merge(ret);
            em.flush();
            AplicacionPagoCxp apl = aplicacionPagoCxpService.aplicarRetencionEmitida(ret, ret.getAsiento(), ctx.idEmpresa, ctx.usuario);
            em.flush();
            return item.creado(ret.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "retencion creada y cruzada (aplicacion " + (apl != null ? apl.getId() : null) + ")");
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    private PlanCuenta cuentaRetencionEmitida(String impuesto, String codigo) {
        String lista = "RENTA".equalsIgnoreCase(impuesto) ? "608" : "20";
        List<PlanCuenta> l = em.createQuery("select t.planCuenta from Tsri t where t.lsri.tabla = :l and t.codigo = :c and t.estado = 1", PlanCuenta.class)
                .setParameter("l", lista).setParameter("c", codigo).setMaxResults(1).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    private PlanCuenta cuentaRetencionRecibida(String impuesto, String codigo) {
        String lista = "RENTA".equalsIgnoreCase(impuesto) ? "608" : "20";
        List<PlanCuenta> l = em.createQuery("select t.planCuenta from TsriCompra t where t.lsri.tabla = :l and t.codigo = :c and t.estado = 1", PlanCuenta.class)
                .setParameter("l", lista).setParameter("c", codigo).setMaxResults(1).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    /** Punto de emisión del facturador por códigos de establecimiento y punto; lo crea si falta (el 002 de 2025 no existe en el SAA). */
    private PuntoEmision puntoEmision(Facturador facturador, String establecimiento, String punto, boolean crear) throws Throwable {
        List<PuntoEmision> puntos = em.createQuery("select p from PuntoEmision p where p.establecimiento.facturador.id = :f and p.establecimiento.codigo = :e and p.codigo = :c order by p.id", PuntoEmision.class)
                .setParameter("f", facturador.getId()).setParameter("e", establecimiento).setParameter("c", punto).getResultList();
        if (!puntos.isEmpty()) return puntos.get(0);
        if (!crear) return null;
        List<Establecimiento> estabs = em.createQuery("select e from Establecimiento e where e.facturador.id = :f and e.codigo = :c order by e.id", Establecimiento.class)
                .setParameter("f", facturador.getId()).setParameter("c", establecimiento).getResultList();
        if (estabs.isEmpty()) throw new IncomeException("El facturador no tiene el establecimiento " + establecimiento + " (CBR.ESTB)");
        PuntoEmision p = new PuntoEmision();
        p.setEstablecimiento(estabs.get(0));
        p.setCodigo(punto);
        p.setNombre("Punto " + punto + " (historico, creado por la carga C11)");
        p.setCreado(LocalDateTime.now());
        p.setEstado((long) Estado.ACTIVO);
        em.persist(p);
        em.flush();
        System.out.println("ℹ Punto de emision " + establecimiento + "-" + punto + " creado en CBR.PTEM (id " + p.getId() + ")");
        return p;
    }

    // =====================================================================================================
    // Pagos a proveedores (PGS.PGTR + APLP, motor de CxP)
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarPago(Contexto ctx, PagoLote p) throws Throwable {
        ItemResultado item = new ItemResultado("pago", p.ref, "pago " + p.facturaNumero + " " + nvl(p.razonSocial, "") + " " + p.fechaPago);
        try {
            Titular proveedor = titularPorIdentificacion(p.proveedor);
            FacturaCompra factura = facturaCompraDe(ctx, p.facturaClave, p.facturaNumero, proveedor != null ? proveedor.getCodigo() : null);
            if (factura == null) return item.rechazado("la factura " + p.facturaNumero + " no esta cargada en PGS.FCTC (cargar primero el lote de " + p.facturaMes + ")");
            LocalDate fp = fecha(p.fechaPago, "fechaPago");
            if (fp.isAfter(LocalDate.now())) return item.rechazado("fecha de pago futura");
            if (nz(p.valor) <= 0) return item.rechazado("valor en cero");
            if (p.idCuentaBancaria == null) return item.rechazado("sin cuenta bancaria de origen");
            boolean debito = "DEBITO_AUTOMATICO".equalsIgnoreCase(p.formaPago);
            if (!debito && !"TRANSFERENCIA".equalsIgnoreCase(p.formaPago)) return item.rechazado("forma de pago no soportada: " + p.formaPago);
            // Idempotencia: ya hay un pago directo activo de la misma fecha y valor sobre la factura
            List<AplicacionPagoCxp> previas = em.createQuery("select a from AplicacionPagoCxp a where a.facturaCompra.id = :f and a.tipoDocPago = :t and a.estado = :e and a.fechaAplicacion = :d and abs(a.montoAplicado - :v) < 0.011", AplicacionPagoCxp.class)
                    .setParameter("f", factura.getId()).setParameter("t", (long) TipoDocPagoAplicacion.COBRO_DIRECTO).setParameter("e", (long) EstadoAplicacionPago.ACTIVO)
                    .setParameter("d", fp).setParameter("v", nz(p.valor)).getResultList();
            if (!previas.isEmpty()) return item.omitido(previas.get(0).getId(), "ya existe un pago directo de " + p.valor + " el " + p.fechaPago + " sobre esa factura (aplicacion " + previas.get(0).getId() + ")");
            Map<String, Object> saldo = aplicacionPagoCxpService.saldoFactura(factura.getId());
            double saldoPendiente = numero(saldo.get("saldoPendiente"));
            if (nz(p.valor) > saldoPendiente + TOLERANCIA) return item.rechazado("el pago " + p.valor + " supera el saldo de la factura " + saldoPendiente
                    + (p.retencionEnOtroMes != null ? " (la retencion de " + p.retencionEnOtroMes + " debe estar cargada antes)" : ""));
            if (ctx.soloValidar) return item.validado("se registraria el pago " + p.formaPago + " de " + p.valor + " el " + p.fechaPago + " desde la cuenta " + p.cuentaBancaria + " (saldo " + saldoPendiente + ")");

            String obs = recortar(ctx.observacion + " | " + p.ref + " | " + nvl(p.formaPagoExcel, "") + " | " + nvl(p.referencia, ""), 2000);
            String referencia = referenciaCorta(p.ref, p.facturaNumero, p.referencia);
            Map<String, Object> res = pagoProgramadoService.registrarPago(factura.getId(), p.idCuentaBancaria, null, redondea(nz(p.valor)), p.fechaPago,
                    ctx.idEmpresa, ctx.idUsuario, obs, debito, referencia, debito ? FormaPagoProgramado.DEBITO_AUTOMATICO : FormaPagoProgramado.TRANSFERENCIA);
            Long idPago = res.get("pago") != null ? Long.valueOf(res.get("pago").toString()) : null;
            if (idPago == null) throw new IncomeException("registrarPago no devolvio el id del pago: " + res);
            PagoProgramado pago = em.find(PagoProgramado.class, idPago);
            Asiento asiento;
            if (debito) {
                asiento = pago.getAplicacion() != null ? pago.getAplicacion().getAsiento() : null;
            } else {
                // Transferencia: el pago nace REGISTRADO (esperando al banco). La orden de 2025 ya se ejecutó, así que se
                // confirma aquí mismo con la fecha del Excel, igual que confirmarUnPagoManual pero dentro de esta transacción.
                pago.setFechaRespuesta(fp);
                pago.setReferenciaBanco(referencia);
                AplicacionPagoCxp apl = aplicacionPagoCxpService.aplicarPagoTransferencia(pago, ctx.idUsuario);
                pago.setAplicacion(apl);
                pago.setEstado((long) EstadoPagoProgramado.CONFIRMADO);
                em.merge(pago);
                asiento = apl.getAsiento();
            }
            em.flush();
            return item.creado(idPago, asiento != null ? asiento.getCodigo() : null, asiento != null ? asiento.getNumeroAlterno() : null,
                    "pago " + (debito ? "por debito automatico" : "por transferencia") + " confirmado; saldo factura " + numero(aplicacionPagoCxpService.saldoFactura(factura.getId()).get("saldoPendiente")));
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    // =====================================================================================================
    // Ventas (CBR.FCTR), cobros (CBR.APLC) y retenciones recibidas (PGS.RCV2): fase 2
    // =====================================================================================================

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarVenta(Contexto ctx, VentaLote v) throws Throwable {
        ItemResultado item = new ItemResultado("venta", v.ref, v.numero);
        try {
            if (v.claveAcceso == null || !v.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            Factura existente = unico(em.createQuery("select f from Factura f where f.clave = :c and f.estadoEmision <> 3", Factura.class).setParameter("c", v.claveAcceso).getResultList());
            if (existente == null) existente = unico(em.createQuery("select f from Factura f where f.numero = :n and f.facturador.empresa.codigo = :e and f.estado = 5 and f.estadoEmision <> 3", Factura.class)
                    .setParameter("n", v.numero).setParameter("e", ctx.idEmpresa).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la factura de venta " + existente.getNumero() + " (estado " + existente.getEstado() + ", asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular cliente = titularPorIdentificacion(v.cliente);
            if (cliente == null) return item.rechazado("el cliente " + v.cliente + " no existe en TSR.TTLR (cargar titulares primero)");
            if (!asientoContableService.existeCuentaConRolEstricto(cliente.getCodigo(), ctx.idEmpresa, 1L, RolPersona.CLIENTE))
                return item.rechazado("el cliente no tiene cuenta contable con rol cliente (TSR.PRCC tipo 1)");
            Facturador facturador = facturadorDeEmpresa(ctx.idEmpresa);
            if (facturador == null) return item.rechazado("la empresa no tiene facturador");
            LocalDate fe = fecha(v.fechaEmision, "fechaEmision");
            if (v.lineas == null || v.lineas.isEmpty()) return item.rechazado("sin lineas");
            double sumaBases = 0, sumaIva = 0, base15 = 0, base0 = 0, iva15 = 0;
            for (LineaLote l : v.lineas) {
                cuentaMovimiento(ctx.idEmpresa, l.cuenta);
                sumaBases += nz(l.base); sumaIva += nz(l.iva);
                if (l.tarifa != null && l.tarifa == 15) { base15 += nz(l.base); iva15 += nz(l.iva); } else base0 += nz(l.base);
            }
            double dif = redondea(nz(v.total) - sumaBases - sumaIva);
            if (Math.abs(dif) > TOLERANCIA) return item.rechazado("bases + IVA difieren del total en " + dif);
            String[] partes = partesNumero(v.numero);
            if (ctx.soloValidar) return item.validado("se crearia la factura de venta por " + v.total + " (ingreso " + v.cuentaIngreso + ")");

            PuntoEmision punto = puntoEmision(facturador, partes[0], partes[1], true);
            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            Factura f = new Factura();
            f.setTipoComprobante(TIPO_COMPROBANTE_FACTURA);
            f.setTipoDoc(TIPO_COMPROBANTE_FACTURA);
            f.setFacturador(facturador);
            f.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            f.setTitular(cliente);
            f.setNumero(v.numero);
            f.setNumEstablecimiento(partes[0]);
            f.setNumPtoEmision(partes[1]);
            f.setSecuencial(partes[2]);
            f.setAmbiente(facturador.getAmbiente() != null ? facturador.getAmbiente() : ambienteDeClave(v.claveAcceso));
            f.setClave(v.claveAcceso);
            f.setFecha(fe);
            f.setObservacion(recortar(observacionDocumento(ctx, v.ref, "Factura de venta " + v.numero, v.estadoCobroExcel != null ? "estado Excel: " + v.estadoCobroExcel : null), 2000));
            f.setSubtotal(redondea(sumaBases));
            f.setSubcero(redondea(base0));
            f.setSubtotal5(0d);
            f.setSubtotal8(0d);
            f.setpIVA(base15 > 0 ? 15d : 0d);
            f.setvIVA(redondea(nz(v.iva) > 0 ? nz(v.iva) : iva15));
            f.setvIVA5(0d);
            f.setvIVA8(0d);
            f.setvICE(0d);
            f.setvIRBPNR(0d);
            f.setDescuento(0d);
            f.setPorDescuento(0d);
            f.setPropina(0d);
            f.setSubsidio(0d);
            f.setTotalSinSub(redondea(nz(v.total)));
            f.setAhorroSub(0d);
            f.setTotal(redondea(nz(v.total)));
            f.setPtoEmision(punto);
            f.setUsuario(usuario);
            f.setAutorizacion(v.claveAcceso);
            f.setFechaAutorizacion(fecha(nvl(v.fechaAutorizacion, v.fechaEmision), "fechaAutorizacion").atStartOfDay());
            f.setEstado(ESTADO_AUTORIZADA_CXC);
            f.setEstadoEmision(ESTADO_EMISION_OK_CXC);
            f.setEstadoPago((long) EstadoPagoFactura.PENDIENTE);
            em.persist(f);
            int n = 0;
            for (LineaLote l : v.lineas) {
                ProductoCobro producto = productoVentaParaCuenta(ctx, l.cuenta, l.tarifa);
                DetalleFactura d = new DetalleFactura();
                d.setFactura(f);
                d.setDescripcion(recortar(nvl(l.descripcion, "Linea " + (++n)), 500));
                d.setCantidad(1d);
                d.setValor(redondea(nz(l.base)));
                d.setSubTotal(redondea(nz(l.base)));
                d.setDescuento(0d);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeIVA(l.tarifa == null ? 0L : l.tarifa.longValue());
                d.setValorIVA(redondea(nz(l.iva)));
                d.setPorcentajeICE(0L);
                d.setValorICE(0d);
                d.setSubsidio(0d);
                d.setPrecioSinSub(redondea(nz(l.base)));
                d.setTotal(redondea(nz(l.base) + nz(l.iva)));
                d.setProducto(producto);
                d.setCodigoIVASRI(l.codigoIvaSri == null ? (l.tarifa != null && l.tarifa == 15 ? 4L : 0L) : l.codigoIvaSri.longValue());
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();
            String obs = recortar("Factura N° " + v.numero + " | Cliente: " + nvl(cliente.getNombre(), v.cliente) + " | " + ctx.observacion, 2000);
            Asiento asiento = asientoContableService.generarAsientoFactura(f.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_VENTA, fe, obs, ctx.usuario);
            Asiento attached = em.find(Asiento.class, asiento.getCodigo());
            f.setAsiento(attached != null ? attached : asiento);
            em.merge(f);
            em.flush();
            return item.creado(f.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "factura de venta creada (autorizada, punto " + partes[0] + "-" + partes[1] + ")");
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /** Producto de venta cuyo grupo tiene la cuenta de ingreso pedida (hoy 5.3.01 ARRIENDOS, grupo con 23 productos). */
    private ProductoCobro productoVentaParaCuenta(Contexto ctx, String cuenta, Integer tarifa) throws Throwable {
        PlanCuenta plan = cuentaMovimiento(ctx.idEmpresa, cuenta);
        String nombre = PREFIJO_PRODUCTO + cuenta;
        List<ProductoCobro> existentes = em.createQuery("select p from ProductoCobro p where p.empresa.codigo = :e and upper(p.nombre) = :n and p.grupoProducto.planCuenta.codigo = :pc", ProductoCobro.class)
                .setParameter("e", ctx.idEmpresa).setParameter("n", nombre.toUpperCase()).setParameter("pc", plan.getCodigo()).getResultList();
        if (!existentes.isEmpty()) return existentes.get(0);
        List<GrupoProductoCobro> grupos = em.createQuery("select g from GrupoProductoCobro g where g.empresa.codigo = :e and g.planCuenta.codigo = :pc and g.estado = 1 "
                + "order by (select count(x) from ProductoCobro x where x.grupoProducto = g) desc, g.codigo", GrupoProductoCobro.class)
                .setParameter("e", ctx.idEmpresa).setParameter("pc", plan.getCodigo()).getResultList();
        GrupoProductoCobro grupo;
        if (grupos.isEmpty()) {
            grupo = new GrupoProductoCobro();
            grupo.setNombre(recortar(PREFIJO_PRODUCTO + cuenta + " " + nvl(plan.getNombre(), ""), 100));
            grupo.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            grupo.setPlanCuenta(plan);
            grupo.setRubroTipoGrupoP(0L);
            grupo.setRubroTipoGrupoH((long) TipoGrupoProductos.SERVICIO);
            grupo.setEstado((long) Estado.ACTIVO);
            em.persist(grupo);
            System.out.println("ℹ Grupo de producto de ventas creado para la cuenta " + cuenta + ": " + grupo.getNombre());
        } else {
            grupo = grupos.get(0);
        }
        ProductoCobro p = new ProductoCobro();
        p.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
        p.setGrupoProducto(grupo);
        p.setNombre(nombre);
        p.setDescripcion("Producto generico de la carga historica de CxC (C11) para la cuenta " + cuenta);
        p.setPrecioUnitario(0d);
        p.setTipoIVA(tarifa != null && tarifa == 15 ? 4L : 0L);
        p.setEstado((long) Estado.ACTIVO);
        em.persist(p);
        em.flush();
        return p;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarCobro(Contexto ctx, CobroLote c) throws Throwable {
        ItemResultado item = new ItemResultado("cobro", c.ref, "cobro " + c.facturaNumero + " " + c.fechaCobro);
        try {
            Factura factura = facturaVentaDe(ctx, c.facturaClave, c.facturaNumero);
            if (factura == null) return item.rechazado("la factura de venta " + c.facturaNumero + " no esta cargada en CBR.FCTR (cargar primero el lote de " + c.facturaMes + ")");
            LocalDate fc = fecha(c.fechaCobro, "fechaCobro");
            if (fc.isAfter(LocalDate.now())) return item.rechazado("fecha de cobro futura");
            if (nz(c.valor) <= 0) return item.rechazado("valor en cero");
            if (c.idCuentaBancaria == null) return item.rechazado("sin cuenta bancaria");
            List<com.saa.model.cxc.AplicacionPagoCxc> previas = em.createQuery("select a from AplicacionPagoCxc a where a.factura.id = :f and a.tipoDocPago = :t and a.estado = :e and a.fechaAplicacion = :d and abs(a.montoAplicado - :v) < 0.011", com.saa.model.cxc.AplicacionPagoCxc.class)
                    .setParameter("f", factura.getId()).setParameter("t", (long) TipoDocPagoAplicacion.COBRO_DIRECTO).setParameter("e", (long) EstadoAplicacionPago.ACTIVO)
                    .setParameter("d", fc).setParameter("v", nz(c.valor)).getResultList();
            if (!previas.isEmpty()) return item.omitido(previas.get(0).getId(), "ya existe un cobro de " + c.valor + " el " + c.fechaCobro + " sobre esa factura (aplicacion " + previas.get(0).getId() + ")");
            Map<String, Object> saldo = aplicacionPagoCxcService.saldoFactura(factura.getId());
            double saldoPendiente = numero(saldo.get("saldoPendiente"));
            if (nz(c.valor) > saldoPendiente + TOLERANCIA) return item.rechazado("el cobro " + c.valor + " supera el saldo de la factura " + saldoPendiente + " (la retencion recibida debe cargarse antes)");
            if (ctx.soloValidar) return item.validado("se registraria el cobro por transferencia de " + c.valor + " el " + c.fechaCobro + " en la cuenta " + c.cuentaBancaria + " (saldo " + saldoPendiente + ")");
            String referencia = referenciaCorta(c.ref, c.facturaNumero, c.referencia);
            String obs = recortar(ctx.observacion + " | " + c.ref + " | " + nvl(c.estadoCobroExcel, "") + " | " + nvl(c.referencia, ""), 2000);
            Map<String, Object> res = aplicacionPagoCxcService.aplicarCobroTransferencia(factura.getId(), redondea(nz(c.valor)), c.fechaCobro, referencia, c.idCuentaBancaria, ctx.idEmpresa, ctx.idUsuario, obs);
            Long idAplicacion = res.get("aplicacion") != null ? Long.valueOf(res.get("aplicacion").toString()) : null;
            Long idAsiento = res.get("idAsiento") != null ? Long.valueOf(res.get("idAsiento").toString()) : null;
            em.flush();
            return item.creado(idAplicacion, idAsiento, res.get("asiento") != null ? res.get("asiento").toString() : null,
                    "cobro aplicado; saldo factura " + numero(res.get("saldoPendiente")));
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarRetencionRecibida(Contexto ctx, RetencionRecibidaLote r) throws Throwable {
        ItemResultado item = new ItemResultado("retencion-recibida", r.ref, r.numero + " s/ " + r.facturaNumero);
        try {
            if (r.claveAcceso == null || !r.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            RetencionCompraV2 existente = unico(em.createQuery("select x from RetencionCompraV2 x where x.clave = :c and x.estado = 1", RetencionCompraV2.class).setParameter("c", r.claveAcceso).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la retencion recibida " + existente.getNumero() + " (asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular cliente = titularPorIdentificacion(r.cliente);
            if (cliente == null) return item.rechazado("el cliente " + r.cliente + " no existe en TSR.TTLR");
            Factura factura = facturaVentaDe(ctx, r.facturaClave, r.facturaNumero);
            if (factura == null) return item.rechazado("la factura de venta " + r.facturaNumero + " no esta cargada en CBR.FCTR");
            if (r.lineas == null || r.lineas.isEmpty()) return item.rechazado("sin lineas");
            LocalDate fe = fecha(r.fechaEmision, "fechaEmision");
            double total = 0;
            for (LineaRetencionLote l : r.lineas) {
                if (!"RENTA".equalsIgnoreCase(l.impuesto) && !"IVA".equalsIgnoreCase(l.impuesto)) return item.rechazado("impuesto desconocido " + l.impuesto);
                if (cuentaRetencionRecibida(l.impuesto, l.codigoSri) == null) return item.rechazado("el codigo " + l.codigoSri + " (" + l.impuesto + ") no tiene cuenta en PGS.TSRI");
                total += nz(l.valor);
            }
            total = redondea(total);
            if (total <= 0) return item.rechazado("retencion sin valor");
            if (Math.abs(total - nz(r.total)) > TOLERANCIA) return item.rechazado("la suma de lineas " + total + " no coincide con el total " + r.total);
            double saldoPendiente = numero(aplicacionPagoCxcService.saldoFactura(factura.getId()).get("saldoPendiente"));
            if (total > saldoPendiente + TOLERANCIA) return item.rechazado("la retencion " + total + " supera el saldo de la factura " + saldoPendiente);
            String[] partes = partesNumero(r.numero);
            if (ctx.soloValidar) return item.validado("se crearia la retencion recibida " + r.numero + " por " + total + " contra la factura " + factura.getNumero());

            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            RetencionCompraV2 ret = new RetencionCompraV2();
            ret.setTipoComprobante(TIPO_COMPROBANTE_RETENCION);
            ret.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            ret.setProveedor(cliente);
            ret.setTipoDoc(TIPO_DOC_SUSTENTO_FACTURA);
            ret.setPeriodoFiscal(nvl(r.periodoFiscal, String.format("%02d/%d", fe.getMonthValue(), fe.getYear())));
            ret.setNumero(r.numero);
            ret.setNumEstablecimiento(partes[0]);
            ret.setNumPtoEmision(partes[1]);
            ret.setSecuencial(partes[2]);
            ret.setAmbiente(ambienteDeClave(r.claveAcceso));
            ret.setClave(r.claveAcceso);
            ret.setFecha(fe.atStartOfDay());
            ret.setObservacion(recortar(observacionDocumento(ctx, r.ref, "Retencion recibida " + r.numero + " sobre " + r.facturaNumero), 2000));
            ret.setTotal(total);
            ret.setUsuario(usuario);
            ret.setAutorizacion(r.claveAcceso);
            ret.setFechaAutorizacion(fecha(nvl(r.fechaAutorizacion, r.fechaEmision), "fechaAutorizacion").atStartOfDay());
            ret.setEstado((long) Estado.ACTIVO);
            ret.setEstadoEmision(ESTADO_EMISION_REGISTRADA_CXP);
            em.persist(ret);
            for (LineaRetencionLote l : r.lineas) {
                DetalleRetencionCompraV2 d = new DetalleRetencionCompraV2();
                d.setRetencionCompraV2(ret);
                d.setTipoDocReten(TIPO_DOC_SUSTENTO_FACTURA);
                d.setNumDocReten(factura.getNumero().replace("-", ""));
                d.setFechaEmiDoc(factura.getFecha());
                d.setFechaReg(fe);
                d.setDocResAutorizacion(factura.getAutorizacion());
                d.setDocResTotalSinImpuestos(factura.getSubtotal());
                d.setDocResIvaCero(factura.getSubcero());
                d.setDocResPorIva(factura.getpIVA());
                d.setDocResTotalIva(factura.getvIVA());
                d.setDocResTotal(factura.getTotal());
                d.setDocResForPago(FORMA_PAGO_SRI_DEFECTO);
                d.setCodImpuesto("RENTA".equalsIgnoreCase(l.impuesto) ? COD_IMPUESTO_RENTA : COD_IMPUESTO_IVA);
                d.setCodRetencion(l.codigoSri);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeReten(l.porcentaje);
                d.setValorReten(redondea(nz(l.valor)));
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();
            String obs = recortar("Retención recibida N° " + r.numero + " | Cliente: " + nvl(cliente.getNombre(), r.cliente) + " | Factura: " + factura.getNumero() + " | " + ctx.observacion, 2000);
            Asiento asiento = asientoContableService.generarAsientoRetencionCompraV2(ret.getId(), ctx.idEmpresa, TipoAsientos.RETENCIONES_RECIBIDAS_V2, fe, obs, ctx.usuario);
            Asiento attached = em.find(Asiento.class, asiento.getCodigo());
            ret.setAsiento(attached != null ? attached : asiento);
            em.merge(ret);
            em.flush();
            com.saa.model.cxc.AplicacionPagoCxc apl = aplicacionPagoCxcService.aplicarRetencionRecibidaV2(ret, ret.getAsiento(), ctx.idEmpresa, ctx.usuario);
            em.flush();
            return item.creado(ret.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "retencion recibida creada y cruzada (aplicacion " + (apl != null ? apl.getId() : null) + ")");
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    private Factura facturaVentaDe(Contexto ctx, String clave, String numero) {
        if (clave != null && clave.matches("\\d{49}")) {
            Factura f = unico(em.createQuery("select f from Factura f where f.clave = :c and f.estado = 5 and f.estadoEmision <> 3", Factura.class).setParameter("c", clave).getResultList());
            if (f != null) return f;
        }
        if (numero == null || numero.trim().isEmpty()) return null;
        List<Factura> l = em.createQuery("select f from Factura f where FUNCTION('replace', f.numero, '-', '') = :n and f.estado = 5 and f.estadoEmision <> 3 and f.facturador.empresa.codigo = :e", Factura.class)
                .setParameter("n", numero.replace("-", "")).setParameter("e", ctx.idEmpresa).getResultList();
        return l.size() == 1 ? l.get(0) : null;
    }

    // =====================================================================================================
    // Notas de crédito: fase 3
    // =====================================================================================================

    static final String DESCRIPCION_HABER_GASTO_NC_COMPRA = "NC Compra reverso gasto"; // literal que escribe generarAsientoNotaCreditoCompra
    static final String DESCRIPCION_HABER_INTERMEDIARIO_NC_COMPRA = "NC Compra intermediario"; // espejo de la factura de intermediario (N2): no se toca

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarNotaCreditoCompra(Contexto ctx, NotaCreditoCompraLote n) throws Throwable {
        ItemResultado item = new ItemResultado("nc-compra", n.ref, "NC " + n.numero + " s/ " + n.facturaAfectadaNumero + " " + nvl(n.razonSocial, ""));
        try {
            if (n.claveAcceso == null || !n.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            com.saa.model.cxp.NotaCreditoCompra existente = unico(em.createQuery("select x from NotaCreditoCompra x where x.clave = :c and x.estado = 1", com.saa.model.cxp.NotaCreditoCompra.class).setParameter("c", n.claveAcceso).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la NC de compra " + existente.getNumero() + " (asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular proveedor = titularPorIdentificacion(n.proveedor);
            if (proveedor == null) return item.rechazado("el proveedor " + n.proveedor + " no existe en TSR.TTLR");
            FacturaCompra factura = facturaCompraDe(ctx, n.facturaAfectadaClave, n.facturaAfectadaNumero, proveedor.getCodigo());
            if (factura == null) return item.rechazado("la factura afectada " + n.facturaAfectadaNumero + " no esta cargada en PGS.FCTC (lote " + n.facturaAfectadaMes + ")");
            LocalDate fe = fecha(n.fechaEmision, "fechaEmision");
            PlanCuenta cuentaGasto = cuentaMovimiento(ctx.idEmpresa, n.cuenta);
            CentroCosto centro = n.centroCosto != null ? centroCosto(ctx.idEmpresa, n.centroCosto) : null;
            if (n.centroCosto != null && centro == null) return item.rechazado("no existe el centro de costo " + n.centroCosto);
            if (n.lineas == null || n.lineas.isEmpty()) return item.rechazado("sin lineas");
            double sumaBases = 0, sumaIva = 0, base0 = 0, base5 = 0, base15 = 0;
            for (LineaLote l : n.lineas) {
                sumaBases += nz(l.base); sumaIva += nz(l.iva);
                int t = l.tarifa == null ? 0 : l.tarifa;
                if (t == 15) base15 += nz(l.base); else if (t == 5) base5 += nz(l.base); else base0 += nz(l.base);
            }
            double dif = redondea(nz(n.total) - sumaBases - sumaIva);
            if (Math.abs(dif) > 0.50) return item.rechazado("bases + IVA difieren del total en " + dif);
            double saldoFactura = numero(aplicacionPagoCxpService.saldoFactura(factura.getId()).get("saldoPendiente"));
            String[] partes = partesNumero(n.numero);
            if (ctx.soloValidar) return item.validado("se crearia la NC de compra por " + n.total + " contra " + factura.getNumero() + " (saldo " + saldoFactura + "; si es menor, el resto queda como saldo de la NC)");

            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            com.saa.model.cxp.NotaCreditoCompra nc = new com.saa.model.cxp.NotaCreditoCompra();
            nc.setTipoComprobante("04");
            nc.setTipoDoc("04");
            nc.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
            nc.setTitular(proveedor);
            nc.setNumero(n.numero);
            nc.setNumEstablecimiento(partes[0]);
            nc.setNumPtoEmision(partes[1]);
            nc.setSecuencial(partes[2]);
            nc.setAmbiente(ambienteDeClave(n.claveAcceso));
            nc.setClave(n.claveAcceso);
            nc.setFecha(fe.atStartOfDay());
            nc.setTipoDocModificado(TIPO_COMPROBANTE_FACTURA);
            nc.setNumDocModificado(factura.getNumero());
            nc.setFechaEmisionDM(factura.getFecha());
            nc.setObservacion(recortar(observacionDocumento(ctx, n.ref, "Nota de credito de compra " + n.numero + " sobre " + factura.getNumero(), "cuenta " + n.cuenta + " (" + nvl(n.origenCuenta, "") + ")"), 2000));
            nc.setSubtotal(redondea(sumaBases));
            nc.setSubcero(redondea(base0));
            nc.setSubnoobj(0d);
            nc.setSubexent(0d);
            nc.setSubtotal5(redondea(base5));
            nc.setSubtotal8(0d);
            nc.setpIVA(base15 > 0 ? 15d : base5 > 0 ? 5d : 0d);
            nc.setvIVA(redondea(nz(n.iva) > 0 ? nz(n.iva) : sumaIva));
            nc.setvICE(0d);
            nc.setvIRBPNR(0d);
            nc.setDescuento(0d);
            nc.setPorDescuento(0d);
            nc.setPropina(0d);
            nc.setSubsidio(0d);
            nc.setTotal(redondea(nz(n.total)));
            nc.setUsuario(usuario);
            nc.setAutorizacion(n.claveAcceso);
            nc.setFechaAutorizacion(fecha(nvl(n.fechaAutorizacion, n.fechaEmision), "fechaAutorizacion").atStartOfDay());
            nc.setEstado((long) Estado.ACTIVO);
            nc.setEstadoEmision(ESTADO_EMISION_REGISTRADA_CXP);
            nc.setEstadoSeguro(0L);
            nc.setFechaRegistroContable(fe);
            em.persist(nc);
            int i = 0;
            for (LineaLote l : n.lineas) {
                com.saa.model.cxp.DetalleNotaCreditoCompra d = new com.saa.model.cxp.DetalleNotaCreditoCompra();
                d.setNotaCredito(nc);
                d.setDescripcion(recortar(nvl(l.descripcion, "Linea " + (++i)), 500));
                d.setCantidad(1d);
                d.setValor(redondea(nz(l.base)));
                d.setSubTotal(redondea(nz(l.base)));
                d.setDescuento(0d);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeIVA(l.tarifa == null ? 0L : l.tarifa.longValue());
                d.setValorIVA(redondea(nz(l.iva)));
                d.setPorcentajeICE(0L);
                d.setValorICE(0d);
                d.setTotal(redondea(nz(l.base) + nz(l.iva)));
                d.setProducto(productoCompraParaCuenta(ctx, l.cuenta != null ? l.cuenta : n.cuenta).getId());
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();
            String obs = recortar("Nota de credito compra: " + n.numero + " | Proveedor: " + nvl(proveedor.getNombre(), n.proveedor) + " | Factura: " + factura.getNumero() + " | " + ctx.observacion, 2000);
            Asiento asiento = asientoContableService.generarAsientoNotaCreditoCompra(nc.getId(), ctx.idEmpresa, TipoAsientos.NOTAS_CREDITO_COMPRA, fe, obs, ctx.usuario);
            Asiento attached = em.find(Asiento.class, asiento.getCodigo());
            nc.setAsiento(attached != null ? attached : asiento);
            em.merge(nc);
            // El motor acredita el "gasto genérico" de la empresa (primer grupo con cuenta): se impone la cuenta de la NC
            // (la de la factura afectada o la sugerida por el contador) y su centro de costo en esa línea.
            int corregidas = imponerCuentaHaberGastoNc(asiento.getCodigo(), cuentaGasto, centro);
            em.flush();
            java.util.Map<String, Object> resultadoAplicacion = new java.util.HashMap<>();
            AplicacionPagoCxp apl = aplicacionPagoCxpService.aplicarNotaCredito(nc, nc.getAsiento(), ctx.idEmpresa, ctx.usuario, resultadoAplicacion);
            em.flush();
            String aviso = resultadoAplicacion.get("advertenciaAplicacion") != null ? " | " + resultadoAplicacion.get("advertenciaAplicacion") : "";
            return item.creado(nc.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "NC de compra creada; " + corregidas + " linea(s) de gasto a " + n.cuenta
                    + (apl != null ? "; aplicada a la factura (aplicacion " + apl.getId() + ")" : "; factura sin saldo: la NC queda con saldo a favor") + aviso);
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /**
     * En el asiento de la NC de compra reemplaza la cuenta de la línea HABER de gasto por la cuenta de la NC y le pone
     * el centro de costo. Identifica la línea solo por la descripción que escribe el motor (N2, 2026-11-01): las de
     * IVA, redondeo e intermediario no se tocan nunca, y no hay respaldo por número de cuenta (las cuentas salen de
     * plantilla). Si la NC es de intermediario el motor no escribe línea de gasto y no se cambia nada.
     */
    private int imponerCuentaHaberGastoNc(Long idAsiento, PlanCuenta cuenta, CentroCosto centro) {
        List<DetalleAsiento> lineas = em.createQuery("select d from DetalleAsiento d where d.asiento.codigo = :a", DetalleAsiento.class).setParameter("a", idAsiento).getResultList();
        int n = 0;
        for (DetalleAsiento d : lineas) {
            boolean haber = d.getValorHaber() != null && d.getValorHaber() > 0;
            if (!haber) continue;
            String descripcion = d.getDescripcion() != null ? d.getDescripcion() : "";
            boolean esGasto = descripcion.startsWith(DESCRIPCION_HABER_GASTO_NC_COMPRA)
                    && !descripcion.startsWith(DESCRIPCION_HABER_INTERMEDIARIO_NC_COMPRA);
            if (!esGasto) continue;
            d.setPlanCuenta(cuenta);
            d.setNumeroCuenta(cuenta.getCuentaContable());
            d.setNombreCuenta(cuenta.getNombre());
            if (centro != null) d.setCentroCosto(centro);
            em.merge(d);
            n++;
        }
        return n;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarNotaCreditoVenta(Contexto ctx, NotaCreditoVentaLote n) throws Throwable {
        ItemResultado item = new ItemResultado("nc-venta", n.ref, "NC " + n.numero + " s/ " + n.facturaAfectadaNumero);
        try {
            if (n.claveAcceso == null || !n.claveAcceso.matches("\\d{49}")) return item.rechazado("clave de acceso invalida");
            com.saa.model.cxc.NotaCredito existente = unico(em.createQuery("select x from NotaCredito x where x.clave = :c and x.estadoEmision <> 3", com.saa.model.cxc.NotaCredito.class).setParameter("c", n.claveAcceso).getResultList());
            if (existente == null) existente = unico(em.createQuery("select x from NotaCredito x where x.numero = :n and x.facturador.empresa.codigo = :e and x.estado = 5 and x.estadoEmision <> 3", com.saa.model.cxc.NotaCredito.class)
                    .setParameter("n", n.numero).setParameter("e", ctx.idEmpresa).getResultList());
            if (existente != null) return item.omitido(existente.getId(), "ya existe la NC de venta " + existente.getNumero() + " (estado " + existente.getEstado() + ", asiento " + numeroAsiento(existente.getAsiento()) + ")");
            Titular cliente = titularPorIdentificacion(n.cliente);
            if (cliente == null) return item.rechazado("el cliente " + n.cliente + " no existe en TSR.TTLR");
            Factura factura = facturaVentaDe(ctx, n.facturaAfectadaClave, n.facturaAfectadaNumero);
            if (factura == null) return item.rechazado("la factura afectada " + n.facturaAfectadaNumero + " no esta cargada en CBR.FCTR (lote " + n.facturaAfectadaMes + ")");
            Facturador facturador = facturadorDeEmpresa(ctx.idEmpresa);
            if (facturador == null) return item.rechazado("la empresa no tiene facturador");
            LocalDate fe = fecha(n.fechaEmision, "fechaEmision");
            if (n.lineas == null || n.lineas.isEmpty()) return item.rechazado("sin lineas");
            double sumaBases = 0, sumaIva = 0, base0 = 0;
            for (LineaLote l : n.lineas) { cuentaMovimiento(ctx.idEmpresa, l.cuenta); sumaBases += nz(l.base); sumaIva += nz(l.iva); if (l.tarifa == null || l.tarifa == 0) base0 += nz(l.base); }
            if (Math.abs(redondea(nz(n.total) - sumaBases - sumaIva)) > TOLERANCIA) return item.rechazado("bases + IVA difieren del total");
            double saldoFactura = numero(aplicacionPagoCxcService.saldoFactura(factura.getId()).get("saldoPendiente"));
            if (nz(n.total) > saldoFactura + TOLERANCIA) return item.rechazado("la NC " + n.total + " supera el saldo de la factura " + saldoFactura + " (el cobro ya la cerro: revisar el orden o el cobro)");
            String[] partes = partesNumero(n.numero);
            if (ctx.soloValidar) return item.validado("se crearia la NC de venta por " + n.total + " contra " + factura.getNumero() + " (saldo " + saldoFactura + ")");

            PuntoEmision punto = puntoEmision(facturador, partes[0], partes[1], true);
            Usuario usuario = em.find(Usuario.class, ctx.idUsuario);
            com.saa.model.cxc.NotaCredito nc = new com.saa.model.cxc.NotaCredito();
            nc.setTipoComprobante("04");
            nc.setTipoDoc("04");
            nc.setFacturador(facturador);
            nc.setTitular(cliente);
            nc.setNumero(n.numero);
            nc.setNumEstablecimiento(partes[0]);
            nc.setNumPtoEmision(partes[1]);
            nc.setSecuencial(partes[2]);
            nc.setAmbiente(facturador.getAmbiente() != null ? facturador.getAmbiente() : ambienteDeClave(n.claveAcceso));
            nc.setClave(n.claveAcceso);
            nc.setFecha(fe.atStartOfDay());
            nc.setTipoDocModificado(TIPO_COMPROBANTE_FACTURA);
            nc.setNumDocModificado(factura.getNumero());
            nc.setFechaEmisionDM(factura.getFecha() != null ? factura.getFecha().atStartOfDay() : null);
            nc.setObservacion(recortar(observacionDocumento(ctx, n.ref, "Nota de credito de venta " + n.numero + " sobre " + factura.getNumero()), 2000));
            nc.setSubtotal(redondea(sumaBases));
            nc.setSubcero(redondea(base0));
            nc.setpIVA(sumaIva > 0 ? 15d : 0d);
            nc.setvIVA(redondea(nz(n.iva) > 0 ? nz(n.iva) : sumaIva));
            nc.setvICE(0d);
            nc.setvIRBPNR(0d);
            nc.setDescuento(0d);
            nc.setPorDescuento(0d);
            nc.setPropina(0d);
            nc.setSubsidio(0d);
            nc.setTotal(redondea(nz(n.total)));
            nc.setPtoEmision(punto);
            nc.setUsuario(usuario);
            nc.setAutorizacion(n.claveAcceso);
            nc.setFechaAutorizacion(fecha(nvl(n.fechaAutorizacion, n.fechaEmision), "fechaAutorizacion").atStartOfDay());
            nc.setEstado(ESTADO_AUTORIZADA_CXC);
            nc.setEstadoEmision(ESTADO_EMISION_OK_CXC);
            nc.setFactura(factura);
            em.persist(nc);
            int i = 0;
            for (LineaLote l : n.lineas) {
                com.saa.model.cxc.DetalleNotaCredito d = new com.saa.model.cxc.DetalleNotaCredito();
                d.setNotaCredito(nc);
                d.setDescripcion(recortar(nvl(l.descripcion, "Linea " + (++i)), 500));
                d.setCantidad(1d);
                d.setValor(redondea(nz(l.base)));
                d.setSubTotal(redondea(nz(l.base)));
                d.setDescuento(0d);
                d.setBaseImponible(redondea(nz(l.base)));
                d.setPorcentajeIVA(l.tarifa == null ? 0L : l.tarifa.longValue());
                d.setValorIVA(redondea(nz(l.iva)));
                d.setPorcentajeICE(0L);
                d.setValorICE(0d);
                d.setTotal(redondea(nz(l.base) + nz(l.iva)));
                d.setProducto(productoVentaParaCuenta(ctx, l.cuenta, l.tarifa).getId());
                d.setEstado((long) Estado.ACTIVO);
                em.persist(d);
            }
            em.flush();
            String obs = recortar("Nota de crédito N° " + n.numero + " | Cliente: " + nvl(cliente.getNombre(), n.cliente) + " | Factura: " + factura.getNumero() + " | " + ctx.observacion, 2000);
            // Mismo tipo que usa NotaCreditoServiceImpl.generarContabilidadNotaCredito (FACTURAS_VENTA): la regla automática lo reclasifica igual.
            Asiento asiento = asientoContableService.generarAsientoNotaCredito(nc.getId(), ctx.idEmpresa, TipoAsientos.FACTURAS_VENTA, fe, obs, ctx.usuario);
            Asiento attached = em.find(Asiento.class, asiento.getCodigo());
            nc.setAsiento(attached != null ? attached : asiento);
            em.merge(nc);
            em.flush();
            com.saa.model.cxc.AplicacionPagoCxc apl = aplicacionPagoCxcService.aplicarNotaCredito(nc, nc.getAsiento(), ctx.idEmpresa, ctx.usuario);
            em.flush();
            return item.creado(nc.getId(), asiento.getCodigo(), asiento.getNumeroAlterno(), "NC de venta creada y aplicada (aplicacion " + (apl != null ? apl.getId() : null) + ")");
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    // =====================================================================================================
    // D-013: saldos iniciales de clientes y proveedores al corte (31-may-2025) y su reasignación
    // =====================================================================================================
    //
    // La contabilidad de esos saldos ya está en el asiento de apertura (2.3.90.10 y 1.4.90.10). En los módulos se
    // representan como documentos SIN líneas de detalle y SIN asiento: el saldo (total − aplicaciones) es lo único
    // que los pagos, cobros, retenciones, NC y la cartera necesitan, y las cuentas del asiento de cada pago/cobro
    // salen de la PRCC del titular, no del documento. Sin líneas, "contabilizar"/"recontabilizar" no puede generarles
    // un asiento por error (los motores exigen detalles activos), así que no hay riesgo de doble contabilización.

    static final String CUENTA_CARTERA_PROVEEDORES = "2.3.90.10";
    static final String CUENTA_CARTERA_CLIENTES = "1.4.90.10";
    static final String IDENTIFICACION_SALDO_INICIAL = "9999999999001";
    static final String NUMERO_GENERICO_SALDO_INICIAL = "999-999-000000001";
    static final String PREFIJO_NUMERO_REASIGNADO = "999-998-";
    static final String MARCA_SALDO_INICIAL = "SALDO INICIAL D-013";

    /** Valida el bloque de saldos iniciales contra las líneas del asiento de apertura (lo que manda es CNT.DTAS). */
    private List<String> validarSaldosIniciales(Contexto ctx, LoteHistoricoCxcCxp.SaldosInicialesLote si) {
        List<String> errores = new ArrayList<>();
        LocalDate corte;
        try { corte = fecha(si.fecha, "saldosIniciales.fecha"); } catch (IncomeException e) { errores.add(e.getMessage()); return errores; }
        if (si.idAsientoApertura == null) { errores.add("saldosIniciales.idAsientoApertura es obligatorio (CNT.ASNT.ASNTCDGO de la apertura)"); return errores; }
        Asiento apertura = em.find(Asiento.class, si.idAsientoApertura);
        if (apertura == null) { errores.add("No existe el asiento de apertura " + si.idAsientoApertura); return errores; }
        if (apertura.getEmpresa() == null || !ctx.idEmpresa.equals(apertura.getEmpresa().getCodigo())) errores.add("El asiento " + si.idAsientoApertura + " no es de la empresa " + ctx.idEmpresa);
        if (!Long.valueOf(Estado.ACTIVO).equals(apertura.getEstado())) errores.add("El asiento de apertura " + si.idAsientoApertura + " no esta activo");
        double aperturaProveedores = redondea(-saldoCuentaEnAsiento(si.idAsientoApertura, CUENTA_CARTERA_PROVEEDORES));
        double aperturaClientes = redondea(saldoCuentaEnAsiento(si.idAsientoApertura, CUENTA_CARTERA_CLIENTES));
        errores.addAll(validarDocumentosSaldoInicial(si, si.proveedores, "proveedores", CUENTA_CARTERA_PROVEEDORES, aperturaProveedores, corte));
        errores.addAll(validarDocumentosSaldoInicial(si, si.clientes, "clientes", CUENTA_CARTERA_CLIENTES, aperturaClientes, corte));
        return errores;
    }

    private List<String> validarDocumentosSaldoInicial(LoteHistoricoCxcCxp.SaldosInicialesLote si, List<LoteHistoricoCxcCxp.DocumentoSaldoInicialLote> docs,
            String lado, String cuenta, double saldoApertura, LocalDate corte) {
        List<String> errores = new ArrayList<>();
        double suma = 0;
        Set<String> claves = new LinkedHashSet<>();
        for (LoteHistoricoCxcCxp.DocumentoSaldoInicialLote d : docs) {
            String id = d.esGenerico ? (si.titularGenerico != null ? si.titularGenerico.identificacion : null) : d.identificacion;
            if (id == null || id.trim().isEmpty()) errores.add("Saldo inicial " + d.ref + ": sin titular (identificacion o titularGenerico)");
            if (nz(d.monto) <= 0) errores.add("Saldo inicial " + d.ref + ": monto debe ser mayor a cero");
            try { partesNumero(d.numero); } catch (IncomeException e) { errores.add("Saldo inicial " + d.ref + ": " + e.getMessage()); }
            if (d.fechaEmision != null && !d.fechaEmision.trim().isEmpty()) {
                try { if (fecha(d.fechaEmision, "fechaEmision").isAfter(corte)) errores.add("Saldo inicial " + d.ref + ": fecha de emision posterior al corte " + corte); }
                catch (IncomeException e) { errores.add("Saldo inicial " + d.ref + ": " + e.getMessage()); }
            }
            if (!claves.add(id + "|" + d.numero)) errores.add("Saldo inicial " + d.ref + ": documento repetido en el lote (" + d.numero + ")");
            suma += nz(d.monto);
        }
        suma = redondea(suma);
        if (!docs.isEmpty() && Math.abs(suma - saldoApertura) > 0.005)
            errores.add("Los documentos de saldo inicial de " + lado + " suman " + suma + " y el asiento de apertura " + si.idAsientoApertura
                    + " tiene " + saldoApertura + " en " + cuenta + ": tienen que ser iguales (el generico es la diferencia)");
        return errores;
    }

    /** Debe − haber de una cuenta en un asiento. */
    private double saldoCuentaEnAsiento(Long idAsiento, String cuenta) {
        Object[] r = em.createQuery("select coalesce(sum(d.valorDebe), 0), coalesce(sum(d.valorHaber), 0) from DetalleAsiento d "
                + "where d.asiento.codigo = :a and d.planCuenta.cuentaContable = :c", Object[].class)
                .setParameter("a", idAsiento).setParameter("c", cuenta).getSingleResult();
        return numero(r[0]) - numero(r[1]);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado cargarSaldoInicial(Contexto ctx, String lado, LoteHistoricoCxcCxp.SaldosInicialesLote si,
            LoteHistoricoCxcCxp.DocumentoSaldoInicialLote d) throws Throwable {
        boolean proveedor = "PROVEEDOR".equalsIgnoreCase(lado);
        ItemResultado item = new ItemResultado(proveedor ? "saldo-inicial-proveedor" : "saldo-inicial-cliente", d.ref, d.numero);
        try {
            String identificacion = d.esGenerico ? si.titularGenerico.identificacion : d.identificacion;
            Titular titular = titularPorIdentificacion(identificacion);
            if (titular == null) return ctx.soloValidar ? item.validado("el titular " + identificacion + " se crearia con el lote; se crearia el documento por " + d.monto)
                    : item.rechazado("el titular " + identificacion + " no existe en TSR.TTLR (va en titulares del lote)");
            String numero = normalizarNumero(d.numero);
            LocalDate corte = fecha(si.fecha, "saldosIniciales.fecha");
            LocalDate fe = d.fechaEmision != null && !d.fechaEmision.trim().isEmpty() ? fecha(d.fechaEmision, "fechaEmision") : corte;
            String obs = recortar(observacionDocumento(ctx, d.ref, MARCA_SALDO_INICIAL + " al " + corte + ", sin asiento: la contabilidad esta en el asiento de apertura "
                    + nvl(si.asientoApertura, "") + " (id " + si.idAsientoApertura + ", " + (proveedor ? CUENTA_CARTERA_PROVEEDORES : CUENTA_CARTERA_CLIENTES) + ")",
                    "monto original " + redondea(nz(d.monto)), d.origen != null ? "origen: " + d.origen : null), 2000);
            Long existente = documentoSaldoInicialExistente(ctx, proveedor, titular, numero);
            if (existente != null) return item.omitido(existente, "ya existe el documento " + numero + " del titular (id " + existente + ")");
            if (!asientoContableService.existeCuentaConRolEstricto(titular.getCodigo(), ctx.idEmpresa, 1L, proveedor ? RolPersona.PROVEEDOR : RolPersona.CLIENTE))
                return item.rechazado("el titular no tiene cuenta contable con rol " + (proveedor ? "proveedor" : "cliente") + " (TSR.PRCC tipo 1): los pagos/cobros no podrian contabilizarse");
            if (ctx.soloValidar) return item.validado("se crearia el documento de saldo inicial " + numero + " por " + d.monto + " con fecha " + fe + ", sin asiento");
            Long id = proveedor ? crearFacturaCompraSaldoInicial(ctx, titular, numero, d.claveAcceso, fe, redondea(nz(d.monto)), obs)
                    : crearFacturaVentaSaldoInicial(ctx, titular, numero, d.claveAcceso, fe, redondea(nz(d.monto)), obs);
            em.flush();
            return item.creado(id, null, null, "documento de saldo inicial creado sin asiento (" + (proveedor ? "PGS.FCTC" : "CBR.FCTR") + ") por " + redondea(nz(d.monto)));
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /** Id del documento activo del titular con ese número (FCTC estado 1 / FCTR estado 5 no anulada), o null. */
    private Long documentoSaldoInicialExistente(Contexto ctx, boolean proveedor, Titular titular, String numero) {
        if (proveedor) {
            List<Long> l = em.createQuery("select f.id from FacturaCompra f where f.empresa.codigo = :e and f.titular.codigo = :t and f.numero = :n and f.estado = 1", Long.class)
                    .setParameter("e", ctx.idEmpresa).setParameter("t", titular.getCodigo()).setParameter("n", numero).getResultList();
            return l.isEmpty() ? null : l.get(0);
        }
        List<Long> l = em.createQuery("select f.id from Factura f where f.facturador.empresa.codigo = :e and f.titular.codigo = :t and f.numero = :n and f.estado = 5 and f.estadoEmision <> 3", Long.class)
                .setParameter("e", ctx.idEmpresa).setParameter("t", titular.getCodigo()).setParameter("n", numero).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    /** PGS.FCTC de saldo inicial: sin DFCC, sin asiento, sin clave salvo que se conozca. */
    private Long crearFacturaCompraSaldoInicial(Contexto ctx, Titular titular, String numero, String clave, LocalDate fe, double monto, String obs) {
        String[] partes = partesNumero(numero);
        FacturaCompra fc = new FacturaCompra();
        fc.setTipoComprobante(TIPO_COMPROBANTE_FACTURA);
        fc.setTipoDoc(TIPO_COMPROBANTE_FACTURA);
        fc.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
        fc.setTitular(titular);
        fc.setNumero(numero);
        fc.setNumEstablecimiento(partes[0]);
        fc.setNumPtoEmision(partes[1]);
        fc.setSecuencial(partes[2]);
        boolean conClave = clave != null && clave.matches("\\d{49}");
        fc.setAmbiente(conClave ? ambienteDeClave(clave) : 2L);
        fc.setClave(conClave ? clave : null);
        fc.setAutorizacion(conClave ? clave : null);
        fc.setFecha(fe.atStartOfDay());
        fc.setFechaAutorizacion(fe.atStartOfDay());
        fc.setObservacion(obs);
        fc.setSubtotal(monto);
        fc.setSubcero(monto);
        fc.setSubnoobj(0d);
        fc.setSubexent(0d);
        fc.setSubtotal5(0d);
        fc.setSubtotal8(0d);
        fc.setpIVA(0d);
        fc.setvIVA(0d);
        fc.setvIVA5(0d);
        fc.setvIVA8(0d);
        fc.setvICE(0d);
        fc.setvIRBPNR(0d);
        fc.setDescuento(0d);
        fc.setPorDescuento(0d);
        fc.setPropina(0d);
        fc.setTotal(monto);
        fc.setUsuario(em.find(Usuario.class, ctx.idUsuario));
        fc.setEstado((long) Estado.ACTIVO);
        fc.setEstadoEmision(ESTADO_EMISION_REGISTRADA_CXP);
        fc.setEstadoPago((long) EstadoPagoFactura.PENDIENTE);
        fc.setEsIntermediario(0L);
        fc.setEsReembolso(0L);
        fc.setEstadoSeguro(0L);
        fc.setFechaRegistroContable(fe);
        em.persist(fc);
        return fc.getId();
    }

    /** CBR.FCTR de saldo inicial: sin DTFC, sin asiento; punto de emisión solo si el número es de un punto real. */
    private Long crearFacturaVentaSaldoInicial(Contexto ctx, Titular titular, String numero, String clave, LocalDate fe, double monto, String obs) throws Throwable {
        Facturador facturador = facturadorDeEmpresa(ctx.idEmpresa);
        if (facturador == null) throw new IncomeException("la empresa no tiene facturador (CBR.FCDR)");
        String[] partes = partesNumero(numero);
        Factura f = new Factura();
        f.setTipoComprobante(TIPO_COMPROBANTE_FACTURA);
        f.setTipoDoc(TIPO_COMPROBANTE_FACTURA);
        f.setFacturador(facturador);
        f.setEmpresa(em.find(Empresa.class, ctx.idEmpresa));
        f.setTitular(titular);
        f.setNumero(numero);
        f.setNumEstablecimiento(partes[0]);
        f.setNumPtoEmision(partes[1]);
        f.setSecuencial(partes[2]);
        boolean conClave = clave != null && clave.matches("\\d{49}");
        f.setAmbiente(facturador.getAmbiente() != null ? facturador.getAmbiente() : 2L);
        f.setClave(conClave ? clave : null);
        f.setAutorizacion(conClave ? clave : null);
        f.setFecha(fe);
        f.setFechaAutorizacion(fe.atStartOfDay());
        f.setObservacion(obs);
        f.setSubtotal(monto);
        f.setSubcero(monto);
        f.setSubtotal5(0d);
        f.setSubtotal8(0d);
        f.setpIVA(0d);
        f.setvIVA(0d);
        f.setvIVA5(0d);
        f.setvIVA8(0d);
        f.setvICE(0d);
        f.setvIRBPNR(0d);
        f.setDescuento(0d);
        f.setPorDescuento(0d);
        f.setPropina(0d);
        f.setSubsidio(0d);
        f.setTotalSinSub(monto);
        f.setAhorroSub(0d);
        f.setTotal(monto);
        f.setPtoEmision(puntoEmision(facturador, partes[0], partes[1], false)); // 999-999 / 999-998 no existen: queda null
        f.setUsuario(em.find(Usuario.class, ctx.idUsuario));
        f.setEstado(ESTADO_AUTORIZADA_CXC);
        f.setEstadoEmision(ESTADO_EMISION_OK_CXC);
        f.setEstadoPago((long) EstadoPagoFactura.PENDIENTE);
        em.persist(f);
        return f.getId();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public ItemResultado reasignarSaldoInicial(Contexto ctx, com.saa.model.historico.ReasignacionSaldoInicial r) throws Throwable {
        boolean proveedor = "PROVEEDOR".equalsIgnoreCase(r.lado);
        ItemResultado item = new ItemResultado("reasignacion-saldo-inicial", r.decision, (proveedor ? "proveedor " : "cliente ") + r.identificacionDestino + " " + r.monto);
        try {
            Titular generico = titularPorIdentificacion(nvl(r.identificacionGenerico, IDENTIFICACION_SALDO_INICIAL));
            if (generico == null) return item.rechazado("no existe el titular de saldo inicial " + nvl(r.identificacionGenerico, IDENTIFICACION_SALDO_INICIAL));
            String numeroGenerico = normalizarNumero(nvl(r.numeroGenerico, NUMERO_GENERICO_SALDO_INICIAL));
            Long idGenerico = documentoSaldoInicialExistente(ctx, proveedor, generico, numeroGenerico);
            if (idGenerico == null) return item.rechazado("no existe el documento de saldo inicial " + numeroGenerico + " del titular SALDO INICIAL (" + r.lado + ")");
            Titular destino = titularPorIdentificacion(r.identificacionDestino);
            if (destino != null && destino.getCodigo().equals(generico.getCodigo())) return item.rechazado("el destino es el propio titular SALDO INICIAL");
            double monto = redondea(nz(r.monto));

            // Idempotencia: el documento destino ya existe (por número) o ya hay una reasignación con la misma decisión al mismo titular
            String numeroDestino = r.numeroDocumento != null && !r.numeroDocumento.trim().isEmpty() ? normalizarNumero(r.numeroDocumento) : null;
            if (destino != null) {
                Long previo = numeroDestino != null ? documentoSaldoInicialExistente(ctx, proveedor, destino, numeroDestino)
                        : reasignacionPrevia(ctx, proveedor, destino, r.decision.trim());
                if (previo != null) return item.omitido(previo, "ya existe el documento reasignado (id " + previo + "); no se mueve nada");
            }

            // Saldo disponible en el genérico (total − aplicaciones activas), con bloqueo para que dos reasignaciones no lo pasen
            double saldoGenerico;
            LocalDate fechaGenerico;
            if (proveedor) {
                FacturaCompra g = em.find(FacturaCompra.class, idGenerico, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                saldoGenerico = numero(aplicacionPagoCxpService.saldoFactura(g.getId()).get("saldoPendiente"));
                fechaGenerico = g.getFecha().toLocalDate();
            } else {
                Factura g = em.find(Factura.class, idGenerico, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
                saldoGenerico = numero(aplicacionPagoCxcService.saldoFactura(g.getId()).get("saldoPendiente"));
                fechaGenerico = g.getFecha();
            }
            if (monto > saldoGenerico + TOLERANCIA) return item.rechazado("el monto " + monto + " supera el saldo del documento SALDO INICIAL " + numeroGenerico + " (" + saldoGenerico + ")");
            LocalDate fe = r.fechaEmisionDocumento != null && !r.fechaEmisionDocumento.trim().isEmpty() ? fecha(r.fechaEmisionDocumento, "fechaEmisionDocumento") : fechaGenerico;
            if (fe.isAfter(fechaGenerico)) return item.rechazado("la fecha de emision " + fe + " es posterior al corte " + fechaGenerico + ": un documento posterior no es saldo inicial, va por la carga del mes");
            if (destino == null && (r.razonSocialDestino == null || r.tipoIdentificacionDestino == null || r.tipoPersonaDestino == null))
                return item.rechazado("el titular " + r.identificacionDestino + " no existe: enviar razonSocialDestino, tipoIdentificacionDestino y tipoPersonaDestino para crearlo");
            if (ctx.soloValidar) return item.validado("se moverian " + monto + " del documento " + numeroGenerico + " (saldo " + saldoGenerico + ") a "
                    + (numeroDestino != null ? numeroDestino : PREFIJO_NUMERO_REASIGNADO + "…") + " con fecha " + fe + (destino == null ? "; se crearia el titular" : ""));

            List<String> hechos = new ArrayList<>();
            TitularLote tl = new TitularLote();
            tl.identificacion = r.identificacionDestino.trim();
            tl.razonSocial = r.razonSocialDestino;
            tl.tipoIdentificacion = r.tipoIdentificacionDestino;
            tl.tipoPersona = r.tipoPersonaDestino;
            tl.esProveedor = proveedor;
            tl.esCliente = !proveedor;
            destino = asegurarTitular(ctx, tl, destino, hechos);
            em.flush();
            if (numeroDestino == null) numeroDestino = siguienteNumeroReasignado(ctx, proveedor, destino);
            else if (documentoSaldoInicialExistente(ctx, proveedor, destino, numeroDestino) != null)
                return item.omitido(documentoSaldoInicialExistente(ctx, proveedor, destino, numeroDestino), "ya existe el documento " + numeroDestino + " del titular");

            String traza = "REASIG " + r.decision.trim() + " " + r.fecha + ": " + monto + " de " + numeroGenerico + " (id " + idGenerico + ")";
            String obsDestino = recortar(ctx.observacion + " | " + MARCA_SALDO_INICIAL + " reasignado desde SALDO INICIAL, sin asiento: la contabilidad esta en la apertura | "
                    + traza + " | motivo: " + r.motivo.trim(), 2000);
            Long idDestino = proveedor ? crearFacturaCompraSaldoInicial(ctx, destino, numeroDestino, r.claveAcceso, fe, monto, obsDestino)
                    : crearFacturaVentaSaldoInicial(ctx, destino, numeroDestino, r.claveAcceso, fe, monto, obsDestino);
            em.flush();

            // Rebaja del genérico: solo el total (no tiene líneas ni asiento); la historia queda en su observación
            String nota = " | " + traza + " -> " + numeroDestino + " (id " + idDestino + ")";
            if (proveedor) {
                FacturaCompra g = em.find(FacturaCompra.class, idGenerico);
                double nuevo = redondea(nz(g.getTotal()) - monto);
                g.setTotal(nuevo);
                g.setSubtotal(nuevo);
                g.setSubcero(nuevo);
                g.setObservacion(anexarTraza(g.getObservacion(), nota));
                em.merge(g);
            } else {
                Factura g = em.find(Factura.class, idGenerico);
                double nuevo = redondea(nz(g.getTotal()) - monto);
                g.setTotal(nuevo);
                g.setTotalSinSub(nuevo);
                g.setSubtotal(nuevo);
                g.setSubcero(nuevo);
                g.setObservacion(anexarTraza(g.getObservacion(), nota));
                em.merge(g);
            }
            em.flush();
            return item.creado(idDestino, null, null, "reasignados " + monto + " a " + numeroDestino + (hechos.isEmpty() ? "" : " (" + String.join("; ", hechos) + ")")
                    + "; saldo que queda en SALDO INICIAL " + redondea(saldoGenerico - monto));
        } catch (Throwable e) {
            sessionContext.setRollbackOnly();
            throw e;
        }
    }

    /** Documento de saldo inicial ya reasignado al titular con la misma decisión (para reasignaciones sin número real). */
    private Long reasignacionPrevia(Contexto ctx, boolean proveedor, Titular destino, String decision) {
        String patron = "%REASIG " + decision + " %";
        List<Long> l = proveedor
                ? em.createQuery("select f.id from FacturaCompra f where f.empresa.codigo = :e and f.titular.codigo = :t and f.estado = 1 and f.observacion like :p", Long.class)
                        .setParameter("e", ctx.idEmpresa).setParameter("t", destino.getCodigo()).setParameter("p", patron).getResultList()
                : em.createQuery("select f.id from Factura f where f.facturador.empresa.codigo = :e and f.titular.codigo = :t and f.estado = 5 and f.estadoEmision <> 3 and f.observacion like :p", Long.class)
                        .setParameter("e", ctx.idEmpresa).setParameter("t", destino.getCodigo()).setParameter("p", patron).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    /** 999-998-NNNNNNNNN siguiente para el titular (número interno cuando el comprobante real no se conoce). */
    private String siguienteNumeroReasignado(Contexto ctx, boolean proveedor, Titular destino) {
        Long n = proveedor
                ? em.createQuery("select count(f) from FacturaCompra f where f.empresa.codigo = :e and f.titular.codigo = :t and f.numero like :p", Long.class)
                        .setParameter("e", ctx.idEmpresa).setParameter("t", destino.getCodigo()).setParameter("p", PREFIJO_NUMERO_REASIGNADO + "%").getSingleResult()
                : em.createQuery("select count(f) from Factura f where f.facturador.empresa.codigo = :e and f.titular.codigo = :t and f.numero like :p", Long.class)
                        .setParameter("e", ctx.idEmpresa).setParameter("t", destino.getCodigo()).setParameter("p", PREFIJO_NUMERO_REASIGNADO + "%").getSingleResult();
        return PREFIJO_NUMERO_REASIGNADO + String.format("%09d", (n == null ? 0 : n) + 1);
    }

    /** Agrega la traza al final de la observación; si no cabe, recorta el texto del medio y conserva el inicio y la última traza. */
    private static String anexarTraza(String observacion, String nota) {
        String base = nvl(observacion, "");
        if (base.length() + nota.length() <= 2000) return base + nota;
        return recortar(base, 2000 - nota.length() - 4) + " ..." + nota;
    }

    /** Número con guiones 001-002-000000531 a partir de cualquier forma con 15 dígitos. */
    private static String normalizarNumero(String numero) {
        String[] p = partesNumero(numero);
        return p[0] + "-" + p[1] + "-" + p[2];
    }

    // =====================================================================================================
    // Utilitarios
    // =====================================================================================================

    private Titular titularPorIdentificacion(String identificacion) {
        if (identificacion == null) return null;
        List<Titular> l = em.createQuery("select t from Titular t where t.identificacion = :i order by case when t.estado = 1 then 0 else 1 end, t.codigo", Titular.class)
                .setParameter("i", identificacion.trim()).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    private FacturaCompra facturaCompraDe(Contexto ctx, String clave, String numero, Long idTitular) throws Throwable {
        if (clave != null && clave.matches("\\d{49}")) {
            FacturaCompra f = unico(em.createQuery("select f from FacturaCompra f where f.clave = :c and f.estado = 1", FacturaCompra.class).setParameter("c", clave).getResultList());
            if (f != null) return f;
        }
        if (numero == null || numero.trim().isEmpty()) return null;
        try {
            return aplicacionPagoCxpService.resolverFacturaCompraPorNumero(numero, idTitular, ctx.idEmpresa);
        } catch (IncomeException e) {
            return null;
        }
    }

    private Facturador facturadorDeEmpresa(Long idEmpresa) {
        List<Facturador> l = em.createQuery("select f from Facturador f where f.empresa.codigo = :e order by f.id", Facturador.class).setParameter("e", idEmpresa).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    /** Cuenta contable de movimiento, activa, de la empresa; con puntos. Lanza IncomeException con el motivo. */
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
        List<CentroCosto> l = em.createQuery("select c from CentroCosto c where c.empresa.codigo = :e and c.numero = :n", CentroCosto.class)
                .setParameter("e", idEmpresa).setParameter("n", numero.trim()).getResultList();
        return l.isEmpty() ? null : l.get(0);
    }

    private static <T> T unico(List<T> l) {
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    /** Descripción del titular para el resultado: razón social solo si es jurídica (sin nombres de personas naturales). */
    private static String descripcionTitular(TitularLote t) {
        boolean juridica = "JURIDICA".equalsIgnoreCase(t.tipoPersona);
        return (juridica ? nvl(t.razonSocial, "") : "persona natural") + " (" + nvl(t.tipoIdentificacion, "") + " " + t.identificacion + ")";
    }

    private static String numeroAsiento(Asiento a) {
        return a == null ? "sin asiento" : a.getNumeroAlterno();
    }

    private static LocalDate fecha(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) throw new IncomeException(campo + " vacia");
        try {
            return LocalDate.parse(texto.trim().substring(0, 10));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            throw new IncomeException(campo + " invalida: " + texto + " (se espera AAAA-MM-DD)");
        }
    }

    /** Establecimiento, punto y secuencial de un número 001-002-000000544 (o sin guiones). */
    private static String[] partesNumero(String numero) {
        String d = numero == null ? "" : numero.replaceAll("\\D", "");
        if (d.length() != 15) throw new IncomeException("numero de comprobante invalido: " + numero);
        return new String[] { d.substring(0, 3), d.substring(3, 6), d.substring(6) };
    }

    /** Ambiente del SRI que trae la clave de acceso (posición 24: 1 pruebas, 2 producción). */
    private static Long ambienteDeClave(String clave) {
        return clave != null && clave.length() == 49 ? Long.valueOf(clave.substring(23, 24)) : 2L;
    }

    private static String observacionDocumento(Contexto ctx, String ref, String titulo, String... extras) {
        StringBuilder sb = new StringBuilder(ctx.observacion).append(" | ").append(ref).append(" | ").append(titulo);
        for (String x : extras) if (x != null && !x.trim().isEmpty()) sb.append(" | ").append(x.trim());
        return sb.toString();
    }

    /**
     * Largo de la referencia bancaria que se pasa a los motores de pago y cobro. Las columnas son de 200
     * (PGS.PGTR.PGTRRFBN, PGS.APLP.APLPREFR, CBR.APLC.APLCREFR), pero los motores arman con ella la descripción del
     * movimiento bancario ("Pago proveedor: <nombre> | Factura: <n> | Ref: <referencia>", TSR.MVCB.MVCBDSCR de 200):
     * con 50 queda sitio para el nombre del titular. El texto completo del Excel va en la observación (2000).
     */
    static final int LARGO_REFERENCIA_BANCARIA = 50;

    /** Referencia corta y única: ref del lote + número de factura + el dato identificable de la referencia del Excel (orden, OCP, "Ref:"). */
    static String referenciaCorta(String ref, String numeroFactura, String textoExcel) {
        String base = (nvl(ref, "") + " " + nvl(numeroFactura, "")).trim();
        String fragmento = null;
        if (textoExcel != null && !textoExcel.trim().isEmpty()) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(orden|OCP|Ref)\\s*:?\\s*([A-Za-z0-9-]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(textoExcel);
            fragmento = m.find() ? m.group(1) + " " + m.group(2) : textoExcel.trim();
        }
        String r = fragmento == null ? base : base + " | " + fragmento;
        return recortar(r.trim(), LARGO_REFERENCIA_BANCARIA);
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

    private static double numero(Object o) {
        if (o == null) return 0d;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (NumberFormatException e) { return 0d; }
    }

}
