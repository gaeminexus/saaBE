package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.ejb.cxp.dao.PagoProgramadoDaoService;
import com.saa.ejb.cxp.service.AnticipoProveedorService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAnticipoProveedor;
import com.saa.rubros.EstadoPagoProgramado;
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

        System.out.println("=== procesarAnticipoProveedor | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria
                + " | debitoAutomatico=" + debitoAutomatico + " ===");

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
        if (!debitoAutomatico && idCuentaDestinoTitular == null)
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
        anticipo.setFormaPago(2L); // 2 = Transferencia (pago bancario)
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
                idUsuario, debitoAutomatico, numeroDoc);

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
                fecha, nombreUsuario);

        // 2. Saldo real acumulado: saldoInicial del PRCC (rol Proveedor,
        //    tipoCuenta=2) + valor, igual que hacía el proceso en un paso.
        java.util.List<PersonaCuentaContable> cuentasAnticipo = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.PROVEEDOR, 2L);
        Double saldoInicialPrcc = (!cuentasAnticipo.isEmpty()
                && cuentasAnticipo.get(0).getSaldoInicial() != null)
                ? cuentasAnticipo.get(0).getSaldoInicial() : 0.0;

        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.CONFIRMADO));
        anticipo.setAsiento(asiento);
        anticipo.setFechaRecepcion(fecha);
        anticipo.setSaldo(saldoInicialPrcc + anticipo.getValor());
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

        System.out.println("=== anularAnticipo | anticipo=" + idAnticipo + " ===");

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }

        AnticipoProveedor anticipo = em.find(AnticipoProveedor.class, idAnticipo);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }

        int estado = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
        if (estado == EstadoAnticipoProveedor.ANULADO) {
            throw new IncomeException("El anticipo " + idAnticipo + " ya está anulado.");
        }
        if (estado == EstadoAnticipoProveedor.CONFIRMADO) {
            throw new IncomeException("El anticipo " + idAnticipo + " ya está confirmado y tiene "
                    + "contabilidad generada. Reverse el pago (pgtr/revertirConfirmado) primero.");
        }

        // Un pago Registrado se anula junto con el anticipo; uno En archivo está
        // en poder del banco y bloquea la anulación hasta procesar la respuesta.
        List<PagoProgramado> vigentes = pagoProgramadoDaoService.selectVigentesByAnticipo(idAnticipo);
        for (PagoProgramado pago : vigentes) {
            int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;
            if (estadoPago == EstadoPagoProgramado.EN_ARCHIVO) {
                throw new IncomeException("El pago " + pago.getId() + " del anticipo ya fue enviado "
                        + "al banco. Procese la respuesta del banco antes de anular.");
            }
            if (estadoPago == EstadoPagoProgramado.CONFIRMADO) {
                throw new IncomeException("El pago " + pago.getId() + " del anticipo ya fue "
                        + "confirmado. Reverse el pago (pgtr/revertirConfirmado) primero.");
            }
            if (estadoPago == EstadoPagoProgramado.REGISTRADO) {
                pago.setEstado(Long.valueOf(EstadoPagoProgramado.ANULADO));
                pago.setMotivo("Anulación del anticipo " + idAnticipo + ": " + motivo.trim());
                pagoProgramadoDaoService.save(pago, pago.getId());
                System.out.println("✓ Pago " + pago.getId() + " anulado junto con el anticipo.");
            }
        }

        anticipo.setEstado(Long.valueOf(EstadoAnticipoProveedor.ANULADO));
        anticipo.setObservacion(((anticipo.getObservacion() != null)
                ? anticipo.getObservacion() : "") + " | ANULADO: " + motivo.trim());
        anticipoDaoService.save(anticipo, anticipo.getId());
        em.flush();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("exito", true);
        resultado.put("mensaje", "Anticipo anulado correctamente.");
        resultado.put("anticipo", idAnticipo);
        return resultado;
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
