package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AnticipoProveedorService;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.AplicacionPagoCxp;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.FormaPagoProgramado;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Implementación del servicio de anticipos a proveedores.
 *
 * Estados del anticipo ({@link EstadoAnticipoProveedor}):
 *   1 = Ingresado  (grabado, con su pago pendiente en el circuito)
 *   2 = Confirmado (pago confirmado: asiento, movimiento bancario y saldo)
 *   3 = Anulado
 *
 * El anticipo se paga a través del circuito de PagoProgramado (PGS.PGTR),
 * igual que los egresos de tesorería: la contabilidad se genera recién
 * cuando el banco confirma el pago (o de inmediato con débito automático),
 * y siempre con el asiento de ANTICIPO, no el de egreso.
 */
@Stateless
public class AnticipoProveedorServiceImpl implements AnticipoProveedorService {

    /** Tolerancia de centavos al comparar el valor del anticipo contra el saldo. */
    private static final double TOLERANCIA = 0.01;

    @EJB
    private AnticipoProveedorDaoService anticipoDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    @EJB
    private PagoProgramadoDaoService pagoProgramadoDaoService;

    @EJB
    private AplicacionPagoCxpService aplicacionPagoCxpService;

    @EJB
    private AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @PersistenceContext
    private EntityManager em;

    // =========================================================================
    // CRUD básico
    // =========================================================================

    @Override
    public AnticipoProveedor selectById(Long id) throws Throwable {
        return anticipoDaoService.selectById(id, NombreEntidadesPago.ANTICIPO_PROVEEDOR);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        AnticipoProveedor entidad = new AnticipoProveedor();
        for (Long id : ids) {
            anticipoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<AnticipoProveedor> lista) throws Throwable {
        for (AnticipoProveedor reg : lista) {
            anticipoDaoService.save(reg, reg.getId());
        }
    }

    @Override
    public List<AnticipoProveedor> selectAll() throws Throwable {
        List<AnticipoProveedor> result =
                anticipoDaoService.selectAll(NombreEntidadesPago.ANTICIPO_PROVEEDOR);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron anticipos de proveedores.");
        }
        return result;
    }

    @Override
    public List<AnticipoProveedor> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        List<AnticipoProveedor> result =
                anticipoDaoService.selectByCriteria(datos, NombreEntidadesPago.ANTICIPO_PROVEEDOR);
        if (result.isEmpty()) {
            throw new IncomeException("La búsqueda de anticipos a proveedores no devolvió registros.");
        }
        return result;
    }

    @Override
    public List<AnticipoProveedor> selectByTitularEmpresa(Long codigoTitular, Long idEmpresa)
            throws Throwable {
        TypedQuery<AnticipoProveedor> q = em.createQuery(
                "SELECT a FROM AnticipoProveedor a "
                + "WHERE a.titular.codigo = :titular "
                + "AND a.empresa.codigo = :empresa "
                + "AND a.estado <> 3 "
                + "ORDER BY a.fechaAnticipo DESC",
                AnticipoProveedor.class);
        q.setParameter("titular", codigoTitular);
        q.setParameter("empresa", idEmpresa);
        return q.getResultList();
    }

    @Override
    public AnticipoProveedor saveSingle(AnticipoProveedor entidad) throws Throwable {
        if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
            throw new IncomeException("El anticipo debe tener un titular (proveedor) asignado.");
        }
        if (entidad.getFechaAnticipo() == null) {
            throw new IncomeException("El anticipo debe tener fecha de anticipo.");
        }
        if (entidad.getValor() == null || entidad.getValor() <= 0) {
            throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
        }

        boolean esNuevo = (entidad.getId() == null);
        if (esNuevo) {
            entidad.setEstado(1L); // 1 = Ingresado
            entidad.setFechaRegistro(LocalDateTime.now());
            if (entidad.getSaldo() == null) {
                entidad.setSaldo(entidad.getValor());
            }
        }

        entidad = anticipoDaoService.save(entidad, entidad.getId());
        System.out.println("✓ AnticipoProveedor guardado ID=" + entidad.getId());
        return entidad;
    }

    // =========================================================================
    // procesarAnticipo — graba el anticipo y crea su pago en el circuito
    // =========================================================================

    @Override
    public Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion,
            Long idCuentaDestinoTitular, boolean debitoAutomatico) throws Throwable {
        return procesarAnticipo(idTitular, valor, idCuentaBancaria, idEmpresa, idUsuario,
                fechaAnticipo, numeroDoc, observacion, idCuentaDestinoTitular, debitoAutomatico, null);
    }

    @Override
    public Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion,
            Long idCuentaDestinoTitular, boolean debitoAutomatico, Long formaPago) throws Throwable {

        System.out.println("=== procesarAnticipoProveedor | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria
                + " | debitoAutomatico=" + debitoAutomatico + " | formaPago=" + formaPago + " ===");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("exito", false);

        // ── Validaciones de entrada ────────────────────────────────────────────
        if (idTitular == null)       throw new IncomeException("El id del titular es obligatorio.");
        if (valor == null || valor <= 0) throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
        if (idCuentaBancaria == null) throw new IncomeException("La cuenta bancaria es obligatoria.");
        if (idEmpresa == null)       throw new IncomeException("La empresa es obligatoria.");
        if (idUsuario == null)       throw new IncomeException("El usuario es obligatorio.");
        if (fechaAnticipo == null || fechaAnticipo.isBlank())
            throw new IncomeException("La fecha del anticipo es obligatoria (formato yyyy-MM-dd).");
        // Se valida aquí, antes de grabar el anticipo, para no dejar un
        // anticipo Ingresado sin pago si el circuito rechaza el registro.
        // El cheque, igual que el débito automático, no exige cuenta destino.
        boolean esCheque = formaPago != null && formaPago.longValue() == FormaPagoProgramado.CHEQUE;
        if (!debitoAutomatico && !esCheque && idCuentaDestinoTitular == null)
            throw new IncomeException("Debe indicar la cuenta bancaria del proveedor "
                    + "para incluir el pago en el archivo del banco.");

        LocalDate fecha;
        try {
            fecha = LocalDate.parse(fechaAnticipo);
        } catch (Exception e) {
            throw new IncomeException("Formato de fecha inválido. Use yyyy-MM-dd. Recibido: " + fechaAnticipo);
        }

        // ── Cargar referencias JPA ─────────────────────────────────────────────
        Titular titular = em.find(Titular.class, idTitular);
        if (titular == null) throw new IncomeException("No se encontró el titular con ID: " + idTitular);

        Empresa empresa = em.find(Empresa.class, idEmpresa);
        if (empresa == null) throw new IncomeException("No se encontró la empresa con ID: " + idEmpresa);

        Usuario usuario = em.find(Usuario.class, idUsuario);
        if (usuario == null) throw new IncomeException("No se encontró el usuario con ID: " + idUsuario);

        // ── Validar cuenta bancaria y su PlanCuenta ANTES de guardar ───────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException(
                    "No se encontró la cuenta bancaria con ID: " + idCuentaBancaria
                    + ". Verifique la configuración en Tesorería → Cuentas Bancarias.");
        }
        if (cuentaBancaria.getPlanCuenta() == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada. "
                    + "Configure la cuenta contable en Tesorería → Cuentas Bancarias antes de continuar.");
        }

        // ── Validar cuenta de anticipos del proveedor ANTES de guardar ─────────
        // Filtra por rol Proveedor: un titular que además es cliente tiene dos
        // cuentas de anticipos (tipoCuenta=2) y hay que tomar la del proveedor.
        java.util.List<PersonaCuentaContable> cuentasAnticipo = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
        if (cuentasAnticipo.isEmpty()) {
            throw new IncomeException(
                    "El proveedor '" + titular.getNombre() + "' (ID: " + idTitular + ") "
                    + "no tiene cuenta contable de anticipos (Tipo 2, Rol: Proveedor) configurada "
                    + "para la empresa " + idEmpresa + ". "
                    + "Configure la cuenta en Tesorería → Persona → Cuentas Contables "
                    + "(Tipo: Anticipos, Rol: Proveedor) antes de registrar el anticipo.");
        }

        // ── Construir entidad ──────────────────────────────────────────────────
        // El saldo definitivo (saldoInicial del PRCC + valor) y el asiento se
        // calculan recién al confirmarse el pago; mientras tanto el anticipo
        // queda Ingresado con su valor nominal.
        AnticipoProveedor anticipo = new AnticipoProveedor();
        anticipo.setTitular(titular);
        anticipo.setEmpresa(empresa);
        anticipo.setUsuario(usuario);
        anticipo.setFechaAnticipo(fecha);
        anticipo.setValor(valor);
        anticipo.setSaldo(valor);
        anticipo.setNumeroDoc(numeroDoc);
        anticipo.setObservacion(observacion);
        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.INGRESADO));
        anticipo.setFechaRegistro(LocalDateTime.now());
        // ── Datos de la cuenta bancaria de pago ────────────────────────────────
        anticipo.setReferencia(cuentaBancaria.getNumeroCuenta());
        // La forma real (incluido cheque=3) la decide el circuito de pagos; aquí
        // se deja la indicada (o Transferencia por defecto) y se corrige la
        // referencia más abajo si el pago terminó asignando un cheque.
        anticipo.setFormaPago(formaPago != null ? formaPago : 2L);
        String nombreBanco = (cuentaBancaria.getBanco() != null
                && cuentaBancaria.getBanco().getNombre() != null)
                ? cuentaBancaria.getBanco().getNombre()
                : "BANCO";
        anticipo.setBanco(nombreBanco + " - " + cuentaBancaria.getNumeroCuenta());

        // ── Guardar anticipo ───────────────────────────────────────────────────
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());
        em.flush();
        System.out.println("✓ AnticipoProveedor guardado ID=" + anticipo.getId());

        // ── Crear su pago en el circuito de PagoProgramado ─────────────────────
        // Con débito automático el pago nace confirmado y el circuito llama de
        // vuelta a contabilizarAnticipoConfirmado en esta misma transacción.
        Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeAnticipo(
                anticipo.getId(), idCuentaBancaria, idCuentaDestinoTitular,
                idUsuario, debitoAutomatico, numeroDoc, formaPago);

        // La forma de pago que manda como autoridad es la que devuelve el circuito
        // (ya normalizada por PagoProgramadoServiceImpl.validarFormaPago, que puede
        // ajustarla si vino en desacuerdo con debitoAutomatico), no la que se
        // recibió cruda aquí. Con cheque, además PGS.ANTP.ANTPFPAG no debe seguir
        // diciendo "transferencia": se corrige la referencia con el número real
        // (el banco ya quedó grabado como banco + cuenta de origen).
        Object formaPagoEfectiva = resultadoPago.get("formaPago");
        Object numeroCheque = resultadoPago.get("numeroCheque");
        boolean cambia = false;
        if (formaPagoEfectiva instanceof Number) {
            anticipo.setFormaPago(((Number) formaPagoEfectiva).longValue());
            cambia = true;
        }
        if (numeroCheque != null) {
            anticipo.setReferencia("CHQ-" + numeroCheque);
            cambia = true;
        }
        if (cambia) {
            anticipo = anticipoDaoService.save(anticipo, anticipo.getId());
        }

        resultado.putAll(resultadoPago);
        resultado.put("anticipo", anticipo.getId());
        return resultado;
    }

    // =========================================================================
    // Contabilización y reversión — invocadas por el circuito de pagos
    // =========================================================================

    @Override
    public Asiento contabilizarAnticipoConfirmado(Long idAnticipo, Long idCuentaBancaria,
            LocalDate fechaPago, Long idUsuario) throws Throwable {
        return contabilizarAnticipoConfirmado(idAnticipo, idCuentaBancaria, fechaPago, idUsuario, null);
    }

    @Override
    public Asiento contabilizarAnticipoConfirmado(Long idAnticipo, Long idCuentaBancaria,
            LocalDate fechaPago, Long idUsuario, String observaciones) throws Throwable {

        System.out.println("=== contabilizarAnticipoConfirmado | anticipo=" + idAnticipo
                + " | fechaPago=" + fechaPago + " ===");

        AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }
        if (anticipo.getEstado() == null
                || anticipo.getEstado().intValue() != EstadoAnticipoProveedor.INGRESADO) {
            throw new IncomeException("El anticipo " + idAnticipo + " no está Ingresado: "
                    + "no se puede contabilizar (estado actual " + anticipo.getEstado() + ").");
        }

        Long idTitular = anticipo.getTitular().getCodigo();
        Long idEmpresa = anticipo.getEmpresa().getCodigo();
        LocalDate fecha = (fechaPago != null) ? fechaPago : LocalDate.now();

        Usuario usuario = (idUsuario != null) ? em.find(Usuario.class, idUsuario) : null;
        String nombreUsuario = (usuario != null && usuario.getNombre() != null)
                ? usuario.getNombre() : "SISTEMA";

        // 1. Asiento de ANTICIPO: DEBE cuenta de anticipos del proveedor /
        //    HABER banco, con la fecha real del pago.
        Asiento asiento = asientoContableService.generarAsientoAnticipoProveedor(
                anticipo, idCuentaBancaria, TipoAsientos.ANTICIPOS_PROVEEDOR,
                fecha, nombreUsuario, observaciones);

        // 2. El saldo del anticipo es su propio saldo DISPONIBLE (lo que queda
        //    por cruzar): nace igual al valor y lo van descontando los cruces
        //    que lo consumen. El saldo global del proveedor vive en el PRCC.
        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
        anticipo.setAsiento(asiento);
        anticipo.setFechaRecepcion(fecha);
        anticipo.setSaldo(anticipo.getValor());
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());

        // 3. Acreditar el saldo de anticipos del proveedor (PRCC)
        actualizarSaldoInicialPrcc(idTitular, idEmpresa, RolPersona.PROVEEDOR,
                anticipo.getValor());

        System.out.println("✓ AnticipoProveedor " + anticipo.getId()
                + " confirmado | Asiento: " + asiento.getNumeroAlterno());
        return asiento;
    }

    @Override
    public void revertirContabilidadAnticipo(Long idAnticipo, String motivo) throws Throwable {

        System.out.println("=== revertirContabilidadAnticipo | anticipo=" + idAnticipo + " ===");

        AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }
        if (anticipo.getEstado() == null
                || anticipo.getEstado().intValue() != EstadoAnticipoProveedor.CONFIRMADO) {
            throw new IncomeException("El anticipo " + idAnticipo
                    + " no está Confirmado: no hay contabilidad que reversar.");
        }

        // El anticipo ya cruzado tiene su dinero aplicado a facturas: reversar
        // el pago sin deshacer antes esos abonos dejaría el saldo en negativo.
        double consumido = anticipo.getValor()
                - ((anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0);
        if (consumido > TOLERANCIA) {
            throw new IncomeException("El anticipo " + idAnticipo + " ya fue cruzado con "
                    + "facturas por $" + formato(consumido) + ". Reverse esos abonos antes "
                    + "de revertir el pago, o anule el anticipo desde la pantalla de "
                    + "anticipos, que lo hace en un solo paso.");
        }

        Long idAsiento = (anticipo.getAsiento() != null)
                ? anticipo.getAsiento().getCodigo() : null;
        if (idAsiento != null) {
            try {
                asientoService.anulaAsiento(idAsiento);
                System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
            } catch (Throwable e) {
                System.err.println("⚠ No se pudo anular el asiento " + idAsiento
                        + ": " + e.getMessage());
            }
        }

        // Descontar del PRCC lo que la confirmación había acreditado.
        actualizarSaldoInicialPrcc(anticipo.getTitular().getCodigo(),
                anticipo.getEmpresa().getCodigo(), RolPersona.PROVEEDOR,
                -anticipo.getValor());

        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.INGRESADO));
        anticipo.setAsiento(null);
        anticipo.setFechaRecepcion(null);
        anticipo.setSaldo(anticipo.getValor());
        anticipo.setObservacion(((anticipo.getObservacion() != null)
                ? anticipo.getObservacion() : "") + " | PAGO REVERSADO: " + motivo);
        anticipoDaoService.save(anticipo, anticipo.getId());

        System.out.println("✓ Anticipo " + idAnticipo + " vuelve a Ingresado.");
    }

    // =========================================================================
    // Anulación
    // =========================================================================

    @Override
    public Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario)
            throws Throwable {
        return anularAnticipo(idAnticipo, motivo, idUsuario, false);
    }

    @Override
    public Map<String, Object> verificarAnulacion(Long idAnticipo) throws Throwable {

        System.out.println("=== verificarAnulacion anticipo proveedor | anticipo="
                + idAnticipo + " ===");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("anticipo", idAnticipo);
        resultado.put("puedeAnular", false);
        resultado.put("requiereConfirmacion", false);
        resultado.put("cruces", new ArrayList<Map<String, Object>>());

        AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        if (anticipo == null) {
            resultado.put("mensaje", "No se encontró el anticipo con ID: " + idAnticipo);
            return resultado;
        }

        String bloqueo = motivoBloqueo(anticipo);
        if (bloqueo != null) {
            resultado.put("mensaje", bloqueo);
            return resultado;
        }

        int estado = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
        resultado.put("estado", estado);
        resultado.put("valorAnticipo", anticipo.getValor());
        resultado.put("puedeAnular", true);

        if (estado != EstadoAnticipoProveedor.CONFIRMADO) {
            // Ingresado: todavía no hay asiento ni saldo acreditado, así que
            // tampoco pudo haberse cruzado con ninguna factura.
            resultado.put("saldoDisponible", 0.0);
            resultado.put("saldoGlobalAnticipos", 0.0);
            resultado.put("montoACruzar", 0.0);
            resultado.put("crucesEstimados", 0);
            resultado.put("mensaje", "El anticipo aún no está confirmado: se anulará junto con "
                    + "su pago pendiente, sin afectar contabilidad ni facturas.");
            return resultado;
        }

        AnalisisCruces analisis = analizarCruces(anticipo);
        resultado.put("saldoDisponible", analisis.saldoDisponible);
        resultado.put("saldoGlobalAnticipos", analisis.saldoGlobal);
        resultado.put("montoACruzar", analisis.deficit);
        resultado.put("cruces", analisis.detalleCruces());
        resultado.put("crucesEstimados", analisis.crucesEstimados);
        resultado.put("requiereConfirmacion", !analisis.cruces.isEmpty());

        if (analisis.cruces.isEmpty()) {
            resultado.put("mensaje", "El anticipo no fue cruzado con ninguna factura. "
                    + "Se anulará el anticipo y su asiento contable, y se descontará el saldo "
                    + "de anticipos del proveedor.");
        } else {
            resultado.put("mensaje", "El anticipo ya fue cruzado con "
                    + analisis.cruces.size() + " factura(s) por un total de $"
                    + formato(analisis.totalCruces) + ". Para anularlo hay que eliminar esos "
                    + "abonos: las facturas volverán a quedar pendientes de pago.");
        }
        if (analisis.crucesEstimados > 0) {
            resultado.put("estimacion", analisis.crucesEstimados + " de los cruces listados no "
                    + "declaran de qué anticipo salieron (son anteriores a la migración) y se "
                    + "eligieron por antigüedad. Verifíquelos antes de confirmar.");
        }
        if (analisis.faltante > TOLERANCIA) {
            resultado.put("advertencia", "Los cruces registrados no alcanzan a cubrir $"
                    + formato(analisis.faltante) + " del anticipo. El saldo de anticipos del "
                    + "proveedor quedará negativo tras la anulación; revise los movimientos "
                    + "de anticipos antes de continuar.");
        }
        return resultado;
    }

    @Override
    public Map<String, Object> anularAnticipo(Long idAnticipo, String motivo, Long idUsuario,
            boolean confirmaReversionCruces) throws Throwable {

        System.out.println("=== anularAnticipo | anticipo=" + idAnticipo
                + " | confirmaCruces=" + confirmaReversionCruces + " ===");

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }
        String motivoLimpio = motivo.trim();

        AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }

        String bloqueo = motivoBloqueo(anticipo);
        if (bloqueo != null) {
            throw new IncomeException(bloqueo);
        }

        Map<String, Object> resultado = new HashMap<>();
        int estado = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
        int crucesReversados = 0;

        if (estado == EstadoAnticipoProveedor.CONFIRMADO) {
            // 1. ¿El anticipo fue cruzado con facturas? Sin confirmación del
            //    usuario no se toca ningún abono: se devuelve el detalle para
            //    que la pantalla pregunte.
            AnalisisCruces analisis = analizarCruces(anticipo);
            if (!analisis.cruces.isEmpty() && !confirmaReversionCruces) {
                resultado.put("exito", false);
                resultado.put("requiereConfirmacion", true);
                resultado.put("anticipo", idAnticipo);
                resultado.put("valorAnticipo", anticipo.getValor());
                resultado.put("saldoDisponible", analisis.saldoDisponible);
                resultado.put("montoACruzar", analisis.deficit);
                resultado.put("cruces", analisis.detalleCruces());
                resultado.put("mensaje", "El anticipo ya fue cruzado con "
                        + analisis.cruces.size() + " factura(s) por un total de $"
                        + formato(analisis.totalCruces) + ". Confirme la eliminación de esos "
                        + "abonos para poder anular el anticipo.");
                return resultado;
            }

            // 2. Eliminar los abonos que el anticipo hizo a las facturas. La
            //    reversión devuelve el saldo a cada factura, recalcula su estado
            //    de pago, anula el asiento del cruce y su movimiento negativo en
            //    PGS.ANTP, y devuelve el saldo global de anticipos.
            for (AplicacionPagoCxp cruce : analisis.cruces) {
                aplicacionPagoCxpService.revertirAplicacion(cruce.getId(),
                        "Anulación del anticipo " + idAnticipo + ": " + motivoLimpio, idUsuario);
                crucesReversados++;
            }
            em.flush();

            // Un cruce estimado (sin FK al anticipo) devuelve el saldo global
            // pero no el del anticipo, porque no sabe que salió de éste. Se
            // normaliza aquí: sus abonos ya quedaron reversados.
            anticipo = em.find(AnticipoProveedor.class, idAnticipo);
            double valorAnticipo = (anticipo.getValor() != null) ? anticipo.getValor() : 0.0;
            if (anticipo.getSaldo() == null || anticipo.getSaldo() < valorAnticipo - TOLERANCIA) {
                anticipo.setSaldo(valorAnticipo);
                anticipoDaoService.save(anticipo, anticipo.getId());
                em.flush();
            }

            // 3. Anular el movimiento bancario del asiento del anticipo: en el
            //    circuito de pagos lo hace PagoProgramadoServiceImpl, no
            //    revertirContabilidadAnticipo.
            anularMovimientoBancarioDelAsiento(anticipo);

            // 4. Anular el asiento del anticipo y descontar el saldo de
            //    anticipos del proveedor. El anticipo vuelve a Ingresado.
            revertirContabilidadAnticipo(idAnticipo, motivoLimpio);
            anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        }

        // 5. El pago del circuito muere junto con el anticipo. Un pago En
        //    archivo está en poder del banco y bloquea la anulación antes de
        //    llegar aquí (lo verifica motivoBloqueo); el resto se anula.
        for (PagoProgramado pago : pagoProgramadoDaoService.selectVigentesByAnticipo(idAnticipo)) {
            if (Long.valueOf(EstadoPagoProgramado.ANULADO).equals(pago.getEstado())) {
                continue;
            }
            pago.setEstado(Long.valueOf(EstadoPagoProgramado.ANULADO));
            pago.setMotivo("Anulación del anticipo " + idAnticipo + ": " + motivoLimpio);
            pagoProgramadoDaoService.save(pago, pago.getId());
            System.out.println("✓ Pago " + pago.getId() + " anulado junto con el anticipo.");
        }

        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.ANULADO));
        anticipo.setSaldo(0.0);
        anticipo.setObservacion(((anticipo.getObservacion() != null)
                ? anticipo.getObservacion() : "") + " | ANULADO: " + motivoLimpio);
        anticipoDaoService.save(anticipo, anticipo.getId());
        em.flush();

        resultado.put("exito", true);
        resultado.put("requiereConfirmacion", false);
        resultado.put("anticipo", idAnticipo);
        resultado.put("crucesReversados", crucesReversados);
        resultado.put("mensaje", (crucesReversados > 0)
                ? "Anticipo anulado correctamente. Se eliminaron " + crucesReversados
                  + " abono(s) a facturas y se anuló el asiento del anticipo."
                : "Anticipo anulado correctamente.");
        System.out.println("✓ Anticipo " + idAnticipo + " anulado | cruces reversados: "
                + crucesReversados);
        return resultado;
    }

    // =========================================================================
    // Consulta y seguimiento
    // =========================================================================

    @Override
    public List<AnticipoProveedor> selectDisponibles(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("=== selectDisponibles anticipos proveedor | titular=" + idTitular
                + " | empresa=" + idEmpresa + " ===");
        return anticipoDaoService.selectDisponiblesByTitular(idTitular, idEmpresa);
    }

    @Override
    public Map<String, Object> seguimiento(Long idTitular, Long idEmpresa) throws Throwable {

        System.out.println("=== seguimiento anticipos proveedor | titular=" + idTitular
                + " | empresa=" + idEmpresa + " ===");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("titular", idTitular);
        resultado.put("empresa", idEmpresa);

        List<AnticipoProveedor> anticipos =
                anticipoDaoService.selectMovimientosByTitular(idTitular, idEmpresa);

        double totalAnticipos = 0.0;
        double totalCruzado = 0.0;
        double totalDisponible = 0.0;
        List<Map<String, Object>> filas = new ArrayList<>();

        for (AnticipoProveedor anticipo : anticipos) {
            Map<String, Object> fila = new HashMap<>();
            fila.put("id", anticipo.getId());
            fila.put("numeroDoc", anticipo.getNumeroDoc());
            fila.put("fechaAnticipo", anticipo.getFechaAnticipo());
            fila.put("fechaRecepcion", anticipo.getFechaRecepcion());
            fila.put("fechaRegistro", anticipo.getFechaRegistro());
            fila.put("valor", anticipo.getValor());
            fila.put("saldo", anticipo.getSaldo());
            fila.put("estado", anticipo.getEstado());
            fila.put("estadoDescripcion", descripcionEstado(anticipo.getEstado()));
            fila.put("formaPago", anticipo.getFormaPago());
            fila.put("referencia", anticipo.getReferencia());
            fila.put("banco", anticipo.getBanco());
            fila.put("observacion", anticipo.getObservacion());
            fila.put("usuario", (anticipo.getUsuario() != null)
                    ? anticipo.getUsuario().getNombre() : null);
            fila.put("asiento", detalleAsiento(anticipo.getAsiento()));

            // Cruces del anticipo, activos y reversados: el historial completo
            // es justamente lo que permite seguir una anulación.
            List<Map<String, Object>> cruces = new ArrayList<>();
            double cruzadoActivo = 0.0;
            for (AplicacionPagoCxp cruce
                    : aplicacionPagoCxpDaoService.selectCrucesByAnticipoOrigen(
                            anticipo.getId(), false)) {

                boolean activo = (cruce.getEstado() != null)
                        && cruce.getEstado().intValue() == EstadoAplicacionPago.ACTIVO;

                Map<String, Object> detalle = new HashMap<>();
                detalle.put("idAplicacion", cruce.getId());
                detalle.put("montoAplicado", cruce.getMontoAplicado());
                detalle.put("fechaAplicacion", cruce.getFechaAplicacion());
                detalle.put("fechaRegistro", cruce.getFechaRegistro());
                detalle.put("estado", cruce.getEstado());
                detalle.put("estadoDescripcion", activo ? "Activo" : "Reversado");
                detalle.put("observacion", cruce.getObservacion());
                detalle.put("usuario", (cruce.getUsuario() != null)
                        ? cruce.getUsuario().getNombre() : null);
                detalle.put("asiento", detalleAsiento(cruce.getAsiento()));
                if (cruce.getFacturaCompra() != null) {
                    detalle.put("idFactura", cruce.getFacturaCompra().getId());
                    detalle.put("numeroFactura", cruce.getFacturaCompra().getNumero());
                }
                cruces.add(detalle);

                if (activo) {
                    cruzadoActivo += (cruce.getMontoAplicado() != null)
                            ? cruce.getMontoAplicado() : 0.0;
                }
            }
            fila.put("cruces", cruces);
            fila.put("totalCruzado", cruzadoActivo);
            filas.add(fila);

            boolean vigente = (anticipo.getEstado() != null)
                    && anticipo.getEstado().intValue() == EstadoAnticipoProveedor.CONFIRMADO;
            if (vigente) {
                totalAnticipos += (anticipo.getValor() != null) ? anticipo.getValor() : 0.0;
                totalDisponible += (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
                totalCruzado += cruzadoActivo;
            }
        }

        // Cuadre: la suma de los saldos por anticipo debe coincidir con el
        // saldo global de la cuenta contable de anticipos del proveedor. Si no
        // coincide hay movimientos sin atribuir (típicamente cruces anteriores
        // a la migración) y conviene revisarlo antes de operar.
        List<PersonaCuentaContable> cuentas = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
        double saldoGlobal = (!cuentas.isEmpty() && cuentas.get(0).getSaldoInicial() != null)
                ? cuentas.get(0).getSaldoInicial() : 0.0;
        double diferencia = saldoGlobal - totalDisponible;

        resultado.put("anticipos", filas);
        resultado.put("totalAnticipos", totalAnticipos);
        resultado.put("totalCruzado", totalCruzado);
        resultado.put("saldoDisponible", totalDisponible);
        resultado.put("saldoGlobalAnticipos", saldoGlobal);
        resultado.put("diferencia", diferencia);
        resultado.put("cuadra", Math.abs(diferencia) <= TOLERANCIA);
        if (Math.abs(diferencia) > TOLERANCIA) {
            resultado.put("advertencia", "El saldo global de anticipos ($"
                    + formato(saldoGlobal) + ") no coincide con la suma de los saldos por "
                    + "anticipo ($" + formato(totalDisponible) + "). Diferencia: $"
                    + formato(diferencia) + ". Revise MIGRACION-CRUCES-ANTICIPO.md.");
        }
        return resultado;
    }

    /**
     * Datos del asiento contable para las pantallas de seguimiento.
     * @param asiento : Asiento vinculado, puede ser null
     * @return        : Mapa con codigo, numero, numeroAlterno, fecha y estado; null si no hay
     */
    private Map<String, Object> detalleAsiento(Asiento asiento) {
        if (asiento == null) {
            return null;
        }
        Map<String, Object> detalle = new HashMap<>();
        detalle.put("codigo", asiento.getCodigo());
        detalle.put("numero", asiento.getNumero());
        detalle.put("numeroAlterno", asiento.getNumeroAlterno());
        detalle.put("fechaAsiento", asiento.getFechaAsiento());
        detalle.put("estado", asiento.getEstado());
        return detalle;
    }

    /**
     * Nombre legible del estado de un anticipo.
     * @param estado : Estado del anticipo
     * @return       : Descripción para la pantalla
     */
    private String descripcionEstado(Long estado) {
        if (estado == null) {
            return "Sin estado";
        }
        switch (estado.intValue()) {
            case EstadoAnticipoProveedor.INGRESADO:  return "Ingresado";
            case EstadoAnticipoProveedor.CONFIRMADO: return "Confirmado";
            case EstadoAnticipoProveedor.ANULADO:    return "Anulado";
            case EstadoAnticipoProveedor.MIGRADO:    return "Movimiento histórico";
            default: return "Estado " + estado;
        }
    }

    // =========================================================================
    // Helpers de anulación
    // =========================================================================

    /**
     * Devuelve el motivo por el que un anticipo NO puede anularse, o null si sí
     * puede. Lo usan igual la verificación previa y la anulación real, para que
     * la pantalla y el servicio nunca discrepen.
     * @param anticipo   : Anticipo a evaluar
     * @return           : Mensaje de bloqueo, null si es anulable
     * @throws Throwable : Excepcion
     */
    private String motivoBloqueo(AnticipoProveedor anticipo) throws Throwable {

        // Los cruces dejan un movimiento NEGATIVO en PGS.ANTP que aparece en el
        // mismo listado que los anticipos. Ese movimiento no se anula desde
        // aquí: se deshace reversando el abono desde la factura.
        if (anticipo.getValor() != null && anticipo.getValor() < 0) {
            return "El registro " + anticipo.getId() + " no es un anticipo sino el movimiento "
                    + "de un cruce con factura. Para deshacerlo reverse el abono desde la "
                    + "factura correspondiente.";
        }

        int estado = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
        if (estado == EstadoAnticipoProveedor.ANULADO) {
            return "El anticipo " + anticipo.getId() + " ya está anulado.";
        }
        if (estado == EstadoAnticipoProveedor.MIGRADO) {
            return "El registro " + anticipo.getId() + " es un movimiento histórico de un "
                    + "cruce anterior a la migración, no un anticipo: no se anula desde aquí.";
        }

        for (PagoProgramado pago
                : pagoProgramadoDaoService.selectVigentesByAnticipo(anticipo.getId())) {
            int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
            if (estadoPago == EstadoPagoProgramado.EN_ARCHIVO) {
                return "El pago " + pago.getId() + " del anticipo ya fue enviado al banco. "
                        + "Procese la respuesta del banco antes de anular.";
            }
        }
        return null;
    }

    /**
     * Determina qué cruces con facturas hay que reversar para poder anular el
     * anticipo.
     * <p>
     * Desde el 2026-08-20 cada cruce guarda de qué anticipo salió el dinero
     * (FK APLPANTO), así que la respuesta es exacta: los cruces activos de ESTE
     * anticipo y ninguno más.
     * <p>
     * Los cruces anteriores a esa fecha no tienen esa FK. Si la migración
     * (MIGRACION-CRUCES-ANTICIPO.md) no llegó a atribuirlos, el consumo del
     * anticipo no queda explicado por sus cruces directos; para esos casos se
     * completa la diferencia con la heurística vieja: cruces del titular sin
     * anticipo de origen, del más reciente al más antiguo.
     * @param anticipo   : Anticipo confirmado a anular
     * @return           : Analisis con el saldo, el deficit y los cruces a reversar
     * @throws Throwable : Excepcion
     */
    private AnalisisCruces analizarCruces(AnticipoProveedor anticipo) throws Throwable {

        AnalisisCruces analisis = new AnalisisCruces();
        double valor = (anticipo.getValor() != null) ? anticipo.getValor() : 0.0;

        Long idTitular = (anticipo.getTitular() != null) ? anticipo.getTitular().getCodigo() : null;
        Long idEmpresa = (anticipo.getEmpresa() != null) ? anticipo.getEmpresa().getCodigo() : null;

        analisis.saldoDisponible = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;

        List<PersonaCuentaContable> cuentas = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
        analisis.saldoGlobal = (!cuentas.isEmpty() && cuentas.get(0).getSaldoInicial() != null)
                ? cuentas.get(0).getSaldoInicial() : 0.0;

        // 1. Cruces exactos: los que declaran a este anticipo como origen.
        double acumulado = 0.0;
        for (AplicacionPagoCxp cruce
                : aplicacionPagoCxpDaoService.selectCrucesByAnticipoOrigen(anticipo.getId(), true)) {
            analisis.cruces.add(cruce);
            acumulado += (cruce.getMontoAplicado() != null) ? cruce.getMontoAplicado() : 0.0;
        }

        // 2. Lo que el anticipo perdió y sus cruces directos no explican: son
        //    cruces viejos sin FK. Se completan por LIFO, como antes.
        analisis.deficit = valor - analisis.saldoDisponible;
        double sinAtribuir = analisis.deficit - acumulado;
        if (sinAtribuir > TOLERANCIA) {
            for (AplicacionPagoCxp cruce : aplicacionPagoCxpDaoService
                    .selectCrucesAnticipoActivos(idTitular, idEmpresa)) {
                if (acumulado + TOLERANCIA >= analisis.deficit) {
                    break;
                }
                // Los que ya tienen origen pertenecen a otro anticipo (los de
                // este ya entraron en el paso 1): tomarlos sería reversar
                // abonos ajenos.
                if (cruce.getAnticipoOrigen() != null) {
                    continue;
                }
                analisis.cruces.add(cruce);
                acumulado += (cruce.getMontoAplicado() != null) ? cruce.getMontoAplicado() : 0.0;
                analisis.crucesEstimados++;
            }
        }

        analisis.totalCruces = acumulado;
        analisis.faltante = analisis.deficit - acumulado;
        if (analisis.deficit < TOLERANCIA) {
            analisis.deficit = 0.0;
        }
        return analisis;
    }

    /**
     * Anula el movimiento bancario generado por el asiento del anticipo. En el
     * circuito de pagos lo hace PagoProgramadoServiceImpl; al anular desde la
     * pantalla de anticipos hay que hacerlo aquí.
     * @param anticipo : Anticipo confirmado
     */
    private void anularMovimientoBancarioDelAsiento(AnticipoProveedor anticipo) {
        Long idAsiento = (anticipo.getAsiento() != null)
                ? anticipo.getAsiento().getCodigo() : null;
        if (idAsiento == null) {
            return;
        }
        try {
            movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
                    Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
        } catch (Throwable e) {
            System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
                    + idAsiento + ": " + e.getMessage());
        }
    }

    /**
     * Formatea un valor monetario para los mensajes al usuario.
     * @param valor : Valor a formatear
     * @return      : Valor con dos decimales
     */
    private String formato(double valor) {
        return String.format(java.util.Locale.US, "%.2f", valor);
    }

    /**
     * Resultado del análisis de cruces de un anticipo que se quiere anular.
     */
    private static class AnalisisCruces {

        /** Saldo disponible de ESTE anticipo (ANTP.ANTPSALD). */
        private double saldoDisponible = 0.0;

        /** Saldo global de anticipos del proveedor (TSR.PRCC.PRCCSLIN). */
        private double saldoGlobal = 0.0;

        /** Cruces incluidos por estimación LIFO, no por FK: datos previos a la migración. */
        private int crucesEstimados = 0;

        /** Parte del anticipo que ya no está disponible: salió por cruces. */
        private double deficit = 0.0;

        /** Suma de los cruces seleccionados para reversar. */
        private double totalCruces = 0.0;

        /** Déficit que los cruces registrados no alcanzan a cubrir. */
        private double faltante = 0.0;

        /** Cruces a reversar, del más reciente al más antiguo. */
        private final List<AplicacionPagoCxp> cruces = new ArrayList<>();

        /**
         * Detalle serializable de los cruces, para que la pantalla muestre qué
         * facturas se van a ver afectadas.
         * @return : Lista de mapas con la factura, el monto y la fecha del cruce
         */
        private List<Map<String, Object>> detalleCruces() {
            List<Map<String, Object>> detalle = new ArrayList<>();
            for (AplicacionPagoCxp cruce : cruces) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("idAplicacion", cruce.getId());
                fila.put("montoAplicado", cruce.getMontoAplicado());
                fila.put("fechaAplicacion", cruce.getFechaAplicacion());
                fila.put("observacion", cruce.getObservacion());
                if (cruce.getFacturaCompra() != null) {
                    fila.put("idFactura", cruce.getFacturaCompra().getId());
                    fila.put("numeroFactura", cruce.getFacturaCompra().getNumero());
                }
                detalle.add(fila);
            }
            return detalle;
        }
    }

    // =========================================================================
    // Helper: sumar valor al saldoInicial de PersonaCuentaContable (tipoCuenta=2)
    // =========================================================================

    /**
     * Busca el registro PersonaCuentaContable (TSR.PRCC) correspondiente al titular,
     * empresa, tipoCuenta=2 (Anticipos) y ROL indicado (1=Cliente, 2=Proveedor),
     * y suma el valor al campo saldoInicial.
     * <p>
     * El filtro por rol es crítico: antes se ignoraba el parámetro y se sumaba
     * el valor a TODAS las cuentas de anticipos del titular, así que un titular
     * con rol cliente y proveedor veía el anticipo acreditado dos veces.
     * @param idTitular  : Id del titular
     * @param idEmpresa  : Id de la empresa contable
     * @param rolPersona : {@link RolPersona#CLIENTE} o {@link RolPersona#PROVEEDOR}
     * @param valor      : Valor a sumar (negativo para restar)
     */
    private void actualizarSaldoInicialPrcc(Long idTitular, Long idEmpresa,
            int rolPersona, Double valor) {
        try {
            java.util.List<PersonaCuentaContable> lista = personaCuentaContableDaoService
                    .selectByTitularRolTipoCuenta(idEmpresa, idTitular, rolPersona, 2L);

            if (lista.isEmpty()) {
                System.err.println("⚠ actualizarSaldoInicialPrcc: no se encontró PRCC "
                        + "para titular=" + idTitular + " empresa=" + idEmpresa
                        + " tipoCuenta=2 rol=" + rolPersona);
                return;
            }

            // Sólo la cuenta del rol pedido: si hubiera más de una fila para el
            // mismo rol se toma la primera, nunca se acumula en varias.
            PersonaCuentaContable pcc = lista.get(0);
            double saldoActual = pcc.getSaldoInicial() != null ? pcc.getSaldoInicial() : 0.0;
            pcc.setSaldoInicial(saldoActual + valor);
            em.merge(pcc);
            System.out.println("✓ PRCC id=" + pcc.getCodigo()
                    + " saldoInicial actualizado: " + saldoActual + " → " + pcc.getSaldoInicial());
        } catch (Throwable e) {
            System.err.println("✗ Error actualizarSaldoInicialPrcc: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
