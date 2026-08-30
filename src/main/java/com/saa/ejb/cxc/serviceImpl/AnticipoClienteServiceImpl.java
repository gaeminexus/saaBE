package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService;
import com.saa.ejb.cxc.service.AnticipoClienteService;
import com.saa.ejb.cxc.service.AplicacionPagoCxcService;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.ejb.cxp.service.dto.BeneficiarioOcasional;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.ejb.tsr.service.MovimientoBancoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.AplicacionPagoCxc;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.cxp.PagoProgramado;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAnticipoCliente;
import com.saa.rubros.EstadoAplicacionPago;
import com.saa.rubros.EstadoPagoProgramado;
import com.saa.rubros.OrigenPagoExterno;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Estados del anticipo:
 *   1 = Ingresado  (grabado, pendiente de confirmación)
 *   2 = Confirmado (asiento contable generado)
 *   3 = Anulado
 */
@Stateless
public class AnticipoClienteServiceImpl implements AnticipoClienteService {

    /** Estado Confirmado: el anticipo tiene asiento y saldo acreditado. */
    private static final int ESTADO_CONFIRMADO = 2;

    /** Estado Anulado. */
    private static final int ESTADO_ANULADO = 3;

    /** Tolerancia de centavos al comparar el valor del anticipo contra el saldo. */
    private static final double TOLERANCIA = 0.01;

    @EJB
    private AnticipoClienteDaoService anticipoDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private AplicacionPagoCxcService aplicacionPagoCxcService;

    @EJB
    private AplicacionPagoCxcDaoService aplicacionPagoCxcDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    /**
     * Auto-inyección: permite que el bucle de {@link #sincronizarDevoluciones} invoque
     * {@link #sincronizarDevolucion(Long)} a TRAVÉS del proxy EJB, para que cada anticipo
     * corra en su propia transacción (REQUIRES_NEW) — mismo patrón que
     * {@code DevolucionAporteServiceImpl.self} en CRD. Una llamada directa
     * {@code this.sincronizarDevolucion(...)} se saltaría el interceptor y todo el lote
     * quedaría en una sola transacción.
     */
    @EJB
    private AnticipoClienteService self;

    @PersistenceContext
    private EntityManager em;

    @Override
    public AnticipoCliente selectById(Long id) throws Throwable {
        System.out.println("selectById AnticipoCliente id=" + id);
        return anticipoDaoService.selectById(id, NombreEntidadesCobro.ANTICIPO_CLIENTE);
    }

    @Override
    public void remove(List<Long> ids) throws Throwable {
        System.out.println("remove AnticipoCliente");
        AnticipoCliente entidad = new AnticipoCliente();
        for (Long id : ids) {
            anticipoDaoService.remove(entidad, id);
        }
    }

    @Override
    public void save(List<AnticipoCliente> lista) throws Throwable {
        System.out.println("save[] AnticipoCliente");
        for (AnticipoCliente reg : lista) {
            anticipoDaoService.save(reg, reg.getId());
        }
    }

    @Override
    public List<AnticipoCliente> selectAll() throws Throwable {
        System.out.println("selectAll AnticipoCliente");
        List<AnticipoCliente> result =
                anticipoDaoService.selectAll(NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (result.isEmpty()) {
            throw new IncomeException("No se encontraron anticipos de clientes.");
        }
        return result;
    }

    @Override
    public List<AnticipoCliente> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria AnticipoCliente");
        List<AnticipoCliente> result =
                anticipoDaoService.selectByCriteria(datos, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (result.isEmpty()) {
            throw new IncomeException("La búsqueda de anticipos no devolvió registros.");
        }
        return result;
    }

    @Override
    public List<AnticipoCliente> selectByTitularEmpresa(Long codigoTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("selectByTitularEmpresa titular=" + codigoTitular
                + " empresa=" + idEmpresa);
        reconciliarDevolucionesPendientes(codigoTitular, idEmpresa);
        TypedQuery<AnticipoCliente> q = em.createQuery(
                "SELECT a FROM AnticipoCliente a "
                + "WHERE a.titular.codigo = :titular "
                + "AND a.empresa.codigo = :empresa "
                + "AND a.estado <> 3 "
                + "ORDER BY a.fechaAnticipo DESC",
                AnticipoCliente.class);
        q.setParameter("titular", codigoTitular);
        q.setParameter("empresa", idEmpresa);
        return q.getResultList();
    }

    @Override
    public AnticipoCliente saveSingle(AnticipoCliente entidad) throws Throwable {
        System.out.println("saveSingle AnticipoCliente");

        // Validaciones básicas
        if (entidad.getTitular() == null || entidad.getTitular().getCodigo() == null) {
            throw new IncomeException("El anticipo debe tener un titular (cliente) asignado.");
        }
        if (entidad.getFechaAnticipo() == null) {
            throw new IncomeException("El anticipo debe tener fecha de anticipo.");
        }
        if (entidad.getValor() == null || entidad.getValor() <= 0) {
            throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
        }

        boolean esNuevo = (entidad.getId() == null);

        // En creación: estado=1 (Ingresado), fechaRegistro y saldo automáticos
        if (esNuevo) {
            entidad.setEstado(1L); // 1 = Ingresado
            entidad.setFechaRegistro(LocalDateTime.now());
            // El saldo disponible arranca igual al valor del anticipo. Antes lo
            // hacía un trigger de BD; ahora la lógica vive en el backend.
            if (entidad.getSaldo() == null || entidad.getSaldo() == 0) {
                entidad.setSaldo(entidad.getValor());
            }
        }

        // No se genera asiento aquí — se genera en confirmarAnticipo
        entidad = anticipoDaoService.save(entidad, entidad.getId());
        System.out.println("✓ Anticipo guardado con ID: " + entidad.getId()
                + " | Estado: Ingresado (1)");
        return entidad;
    }

    // =========================================================================
    // confirmarAnticipo
    // =========================================================================

    @Override
    public java.util.Map<String, Object> confirmarAnticipo(Long idAnticipo, String usuario)
            throws Throwable {
        System.out.println("=== confirmarAnticipo | id=" + idAnticipo + " ===");

        java.util.Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("exito", false);

        // 1. Cargar el anticipo
        AnticipoCliente anticipo = anticipoDaoService.selectById(
                idAnticipo, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (anticipo == null) {
            resultado.put("mensaje", "No se encontró el anticipo con ID: " + idAnticipo);
            return resultado;
        }

        // 2. Validar estado actual
        if (Long.valueOf(2L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo ya está CONFIRMADO"
                    + (anticipo.getAsiento() != null
                       ? ". Asiento: " + anticipo.getAsiento().getNumeroAlterno() : "."));
            resultado.put("exito", true);
            resultado.put("estado", "YA_CONFIRMADO");
            resultado.put("anticipo", anticipo);
            return resultado;
        }
        if (Long.valueOf(3L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo está ANULADO y no puede confirmarse.");
            return resultado;
        }
        if (!Long.valueOf(1L).equals(anticipo.getEstado())) {
            resultado.put("mensaje", "El anticipo tiene un estado desconocido: "
                    + anticipo.getEstado());
            return resultado;
        }

        // 3. Validar que la empresa esté configurada
        if (anticipo.getEmpresa() == null || anticipo.getEmpresa().getCodigo() == null) {
            resultado.put("mensaje", "El anticipo no tiene empresa contable configurada. "
                    + "Edite el anticipo y asigne la empresa antes de confirmar.");
            return resultado;
        }

        // 4. Generar asiento contable
        try {
            Asiento asiento = asientoContableService.generarAsientoAnticipo(
                    anticipo, TipoAsientos.ANTICIPOS_CLIENTE,
                    usuario != null ? usuario : "SISTEMA");

            // 5. Actualizar anticipo: estado=2 (Confirmado) + vincular asiento
            anticipo.setEstado(2L); // 2 = Confirmado
            anticipo.setAsiento(asiento);
            anticipo = anticipoDaoService.save(anticipo, anticipo.getId());

            // 6. Sumar el valor al saldoInicial de PersonaCuentaContable (tipoCuenta=2, rol Cliente)
            actualizarSaldoInicialPrcc(
                    anticipo.getTitular().getCodigo(),
                    anticipo.getEmpresa().getCodigo(),
                    RolPersona.CLIENTE,
                    anticipo.getValor());

            resultado.put("exito", true);
            resultado.put("estado", "CONFIRMADO");
            resultado.put("mensaje", "Anticipo confirmado correctamente. "
                    + "Asiento generado: " + asiento.getNumeroAlterno());
            resultado.put("asiento", asiento.getNumeroAlterno());
            resultado.put("anticipo", anticipo);
            System.out.println("✓ Anticipo " + idAnticipo + " confirmado | Asiento: "
                    + asiento.getNumeroAlterno());

        } catch (Exception e) {
            resultado.put("mensaje", "Error al generar el asiento contable: " + e.getMessage()
                    + ". Verifique que el cliente tenga las cuentas contables configuradas "
                    + "(Tipo 2=Anticipos, Tipo 3=Caja/Banco) y que exista un período contable "
                    + "abierto para la fecha del anticipo.");
            resultado.put("error", e.getMessage());
            System.err.println("✗ Error en confirmarAnticipo: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    // =========================================================================
    // procesarAnticipo — graba + asiento en un solo paso
    // =========================================================================

    @Override
    public java.util.Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion) throws Throwable {

        System.out.println("=== procesarAnticipoCliente | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        java.util.Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("exito", false);

        // ── Validaciones ───────────────────────────────────────────────────────
        if (idTitular == null)        throw new com.saa.basico.util.IncomeException("El id del titular es obligatorio.");
        if (valor == null || valor <= 0) throw new com.saa.basico.util.IncomeException("El valor del anticipo debe ser mayor a cero.");
        if (idCuentaBancaria == null) throw new com.saa.basico.util.IncomeException("La cuenta bancaria es obligatoria.");
        if (idEmpresa == null)        throw new com.saa.basico.util.IncomeException("La empresa es obligatoria.");
        if (idUsuario == null)        throw new com.saa.basico.util.IncomeException("El usuario es obligatorio.");
        if (fechaAnticipo == null || fechaAnticipo.isBlank())
            throw new com.saa.basico.util.IncomeException("La fecha del anticipo es obligatoria (formato yyyy-MM-dd).");

        java.time.LocalDate fecha;
        try {
            fecha = java.time.LocalDate.parse(fechaAnticipo);
        } catch (Exception e) {
            throw new com.saa.basico.util.IncomeException(
                    "Formato de fecha inválido. Use yyyy-MM-dd. Recibido: " + fechaAnticipo);
        }

        // ── Cargar referencias JPA ─────────────────────────────────────────────
        com.saa.model.tsr.Titular titular = em.find(com.saa.model.tsr.Titular.class, idTitular);
        if (titular == null) throw new com.saa.basico.util.IncomeException("No se encontró el titular con ID: " + idTitular);

        com.saa.model.scp.Empresa empresa = em.find(com.saa.model.scp.Empresa.class, idEmpresa);
        if (empresa == null) throw new com.saa.basico.util.IncomeException("No se encontró la empresa con ID: " + idEmpresa);

        com.saa.model.scp.Usuario usuario = em.find(com.saa.model.scp.Usuario.class, idUsuario);
        if (usuario == null) throw new com.saa.basico.util.IncomeException("No se encontró el usuario con ID: " + idUsuario);

        // ── Validar cuenta bancaria y su PlanCuenta ANTES de guardar ───────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new com.saa.basico.util.IncomeException(
                    "No se encontró la cuenta bancaria con ID: " + idCuentaBancaria
                    + ". Verifique la configuración en Tesorería → Cuentas Bancarias.");
        }
        if (cuentaBancaria.getPlanCuenta() == null) {
            throw new com.saa.basico.util.IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada. "
                    + "Configure la cuenta contable en Tesorería → Cuentas Bancarias antes de continuar.");
        }

        // ── Validar cuenta de anticipos del cliente ANTES de guardar ───────────
        // Filtra por rol Cliente: un titular que además es proveedor tiene dos
        // cuentas de anticipos (tipoCuenta=2) y hay que tomar la del cliente.
        java.util.List<PersonaCuentaContable> cuentasAnticipo = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.CLIENTE, 2L);
        if (cuentasAnticipo.isEmpty()) {
            throw new com.saa.basico.util.IncomeException(
                    "El cliente '" + titular.getNombre() + "' (ID: " + idTitular + ") "
                    + "no tiene cuenta contable de anticipos (Tipo 2, Rol: Cliente) configurada "
                    + "para la empresa " + idEmpresa + ". "
                    + "Configure la cuenta en Tesorería → Persona → Cuentas Contables "
                    + "(Tipo: Anticipos, Rol: Cliente) antes de registrar el anticipo.");
        }

        // ── Construir entidad ──────────────────────────────────────────────────
        // El saldo del anticipo es su propio saldo DISPONIBLE (lo que queda por
        // cruzar), no el saldo global del titular: nace igual al valor y lo van
        // descontando los cruces que lo consumen.

        AnticipoCliente anticipo = new AnticipoCliente();
        anticipo.setTitular(titular);
        anticipo.setEmpresa(empresa);
        anticipo.setUsuario(usuario);
        anticipo.setFechaAnticipo(fecha);
        anticipo.setFechaRecepcion(fecha);
        anticipo.setValor(valor);
        anticipo.setSaldo(valor);
        anticipo.setNumeroDoc(numeroDoc);
        anticipo.setObservacion(observacion);
        anticipo.setEstado(1L); // Ingresado
        anticipo.setFechaRegistro(LocalDateTime.now());
        // ── Datos de la cuenta bancaria receptora ──────────────────────────────
        anticipo.setReferencia(cuentaBancaria.getNumeroCuenta());
        anticipo.setFormaPago(2L); // 2 = Transferencia (pago bancario)
        String nombreBanco = (cuentaBancaria.getBanco() != null
                && cuentaBancaria.getBanco().getNombre() != null)
                ? cuentaBancaria.getBanco().getNombre()
                : "BANCO";
        anticipo.setBanco(nombreBanco + " - " + cuentaBancaria.getNumeroCuenta());

        // ── Guardar anticipo ───────────────────────────────────────────────────
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());
        System.out.println("✓ AnticipoCliente guardado ID=" + anticipo.getId());

        // ── Generar asiento contable ───────────────────────────────────────────
        Asiento asiento = asientoContableService.generarAsientoAnticipoCliente(
                anticipo, idCuentaBancaria,
                TipoAsientos.ANTICIPOS_CLIENTE,
                usuario.getNombre() != null ? usuario.getNombre() : usuario.getCodigo().toString());

        // ── Confirmar anticipo ─────────────────────────────────────────────────
        anticipo.setEstado(2L); // Confirmado
        anticipo.setAsiento(asiento);
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());

        // ── Sumar valor al saldoInicial de PersonaCuentaContable (tipoCuenta=2, Cliente=1) ──
        actualizarSaldoInicialPrcc(idTitular, idEmpresa, RolPersona.CLIENTE, valor);

        resultado.put("exito", true);
        resultado.put("estado", "CONFIRMADO");
        resultado.put("mensaje", "Anticipo de cliente procesado correctamente. "
                + "Asiento generado: " + asiento.getNumeroAlterno());
        resultado.put("asiento", asiento.getNumeroAlterno());
        resultado.put("anticipo", anticipo);
        System.out.println("✓ AnticipoCliente " + anticipo.getId()
                + " confirmado | Asiento: " + asiento.getNumeroAlterno());

        return resultado;
    }

    // =========================================================================
    // Anulación
    // =========================================================================

    @Override
    public java.util.Map<String, Object> verificarAnulacion(Long idAnticipo) throws Throwable {

        System.out.println("=== verificarAnulacion anticipo cliente | anticipo="
                + idAnticipo + " ===");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("anticipo", idAnticipo);
        resultado.put("puedeAnular", false);
        resultado.put("requiereConfirmacion", false);
        resultado.put("cruces", new ArrayList<Map<String, Object>>());

        AnticipoCliente anticipo = em.find(AnticipoCliente.class, idAnticipo);
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

        if (estado != ESTADO_CONFIRMADO) {
            // Ingresado: todavía no hay asiento ni saldo acreditado, así que
            // tampoco pudo haberse cruzado con ninguna factura.
            resultado.put("saldoDisponible", 0.0);
            resultado.put("saldoGlobalAnticipos", 0.0);
            resultado.put("montoACruzar", 0.0);
            resultado.put("crucesEstimados", 0);
            resultado.put("mensaje", "El anticipo aún no está confirmado: se anulará sin "
                    + "afectar contabilidad ni facturas.");
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
                    + "de anticipos del cliente.");
        } else {
            resultado.put("mensaje", "El anticipo ya fue cruzado con "
                    + analisis.cruces.size() + " factura(s) por un total de $"
                    + formato(analisis.totalCruces) + ". Para anularlo hay que eliminar esos "
                    + "abonos: las facturas volverán a quedar pendientes de cobro.");
        }
        if (analisis.crucesEstimados > 0) {
            resultado.put("estimacion", analisis.crucesEstimados + " de los cruces listados no "
                    + "declaran de qué anticipo salieron (son anteriores a la migración) y se "
                    + "eligieron por antigüedad. Verifíquelos antes de confirmar.");
        }
        if (analisis.faltante > TOLERANCIA) {
            resultado.put("advertencia", "Los cruces registrados no alcanzan a cubrir $"
                    + formato(analisis.faltante) + " del anticipo. El saldo de anticipos del "
                    + "cliente quedará negativo tras la anulación; revise los movimientos "
                    + "de anticipos antes de continuar.");
        }
        return resultado;
    }

    @Override
    public java.util.Map<String, Object> anularAnticipo(Long idAnticipo, String motivo,
            Long idUsuario, boolean confirmaReversionCruces) throws Throwable {

        System.out.println("=== anularAnticipoCliente | anticipo=" + idAnticipo
                + " | confirmaCruces=" + confirmaReversionCruces + " ===");

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException("Debe indicar el motivo de la anulación.");
        }
        String motivoLimpio = motivo.trim();

        AnticipoCliente anticipo = em.find(AnticipoCliente.class, idAnticipo);
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

        if (estado == ESTADO_CONFIRMADO) {
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
            //    de cobro, anula el asiento del cruce y su movimiento negativo
            //    en CBR.ANTC, y devuelve el saldo global de anticipos.
            for (AplicacionPagoCxc cruce : analisis.cruces) {
                aplicacionPagoCxcService.revertirAplicacion(cruce.getId(),
                        "Anulación del anticipo " + idAnticipo + ": " + motivoLimpio, idUsuario);
                crucesReversados++;
            }
            em.flush();

            // 3. Anular el movimiento bancario y el asiento del anticipo.
            Long idAsiento = (anticipo.getAsiento() != null)
                    ? anticipo.getAsiento().getCodigo() : null;
            if (idAsiento != null) {
                try {
                    movimientoBancoService.actualizaEstadoMovimiento(idAsiento,
                            Long.valueOf(com.saa.rubros.EstadoMovimientoBanco.ANULADO));
                } catch (Throwable e) {
                    System.err.println("⚠ No se pudo anular el movimiento bancario del asiento "
                            + idAsiento + ": " + e.getMessage());
                }
                try {
                    asientoService.anulaAsiento(idAsiento);
                    System.out.println("✓ Asiento " + idAsiento + " anulado / reversado.");
                } catch (Throwable e) {
                    System.err.println("⚠ No se pudo anular el asiento " + idAsiento
                            + ": " + e.getMessage());
                }
            }

            // 4. Descontar del saldo de anticipos del cliente lo que la
            //    confirmación había acreditado.
            actualizarSaldoInicialPrcc(anticipo.getTitular().getCodigo(),
                    anticipo.getEmpresa().getCodigo(), RolPersona.CLIENTE,
                    -anticipo.getValor());
        }

        anticipo.setEstado(Long.valueOf(ESTADO_ANULADO));
        anticipo.setSaldo(0.0);
        anticipo.setObservacion(((anticipo.getObservacion() != null)
                ? anticipo.getObservacion() : "") + " | ANULADO: " + motivoLimpio);
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());
        em.flush();

        resultado.put("exito", true);
        resultado.put("requiereConfirmacion", false);
        resultado.put("anticipo", idAnticipo);
        resultado.put("crucesReversados", crucesReversados);
        resultado.put("mensaje", (crucesReversados > 0)
                ? "Anticipo anulado correctamente. Se eliminaron " + crucesReversados
                  + " abono(s) a facturas y se anuló el asiento del anticipo."
                : "Anticipo anulado correctamente.");
        System.out.println("✓ AnticipoCliente " + idAnticipo + " anulado | cruces reversados: "
                + crucesReversados);
        return resultado;
    }

    // =========================================================================
    // Consulta y seguimiento
    // =========================================================================

    @Override
    public List<AnticipoCliente> selectDisponibles(Long idTitular, Long idEmpresa)
            throws Throwable {
        System.out.println("=== selectDisponibles anticipos cliente | titular=" + idTitular
                + " | empresa=" + idEmpresa + " ===");
        return anticipoDaoService.selectDisponiblesByTitular(idTitular, idEmpresa);
    }

    @Override
    public Map<String, Object> seguimiento(Long idTitular, Long idEmpresa) throws Throwable {

        System.out.println("=== seguimiento anticipos cliente | titular=" + idTitular
                + " | empresa=" + idEmpresa + " ===");

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("titular", idTitular);
        resultado.put("empresa", idEmpresa);

        reconciliarDevolucionesPendientes(idTitular, idEmpresa);

        List<AnticipoCliente> anticipos =
                anticipoDaoService.selectMovimientosByTitular(idTitular, idEmpresa);

        double totalAnticipos = 0.0;
        double totalCruzado = 0.0;
        double totalDisponible = 0.0;
        List<Map<String, Object>> filas = new ArrayList<>();

        for (AnticipoCliente anticipo : anticipos) {
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
            for (AplicacionPagoCxc cruce
                    : aplicacionPagoCxcDaoService.selectCrucesByAnticipoOrigen(
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
                if (cruce.getFactura() != null) {
                    detalle.put("idFactura", cruce.getFactura().getId());
                    detalle.put("numeroFactura", cruce.getFactura().getNumero());
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
                    && anticipo.getEstado().intValue() == EstadoAnticipoCliente.CONFIRMADO;
            if (vigente) {
                totalAnticipos += (anticipo.getValor() != null) ? anticipo.getValor() : 0.0;
                totalDisponible += (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;
                totalCruzado += cruzadoActivo;
            }
        }

        // Cuadre: la suma de los saldos por anticipo debe coincidir con el
        // saldo global de la cuenta contable de anticipos del cliente. Si no
        // coincide hay movimientos sin atribuir (típicamente cruces anteriores
        // a la migración) y conviene revisarlo antes de operar.
        List<PersonaCuentaContable> cuentas = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.CLIENTE, 2L);
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
            case EstadoAnticipoCliente.INGRESADO:  return "Ingresado";
            case EstadoAnticipoCliente.CONFIRMADO: return "Confirmado";
            case EstadoAnticipoCliente.ANULADO:    return "Anulado";
            case EstadoAnticipoCliente.MIGRADO:    return "Movimiento histórico";
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
     * @param anticipo : Anticipo a evaluar
     * @return         : Mensaje de bloqueo, null si es anulable
     */
    private String motivoBloqueo(AnticipoCliente anticipo) {

        // Los cruces dejan un movimiento NEGATIVO en CBR.ANTC que aparece en el
        // mismo listado que los anticipos. Ese movimiento no se anula desde
        // aquí: se deshace reversando el abono desde la factura.
        if (anticipo.getValor() != null && anticipo.getValor() < 0) {
            return "El registro " + anticipo.getId() + " no es un anticipo sino el movimiento "
                    + "de un cruce con factura. Para deshacerlo reverse el abono desde la "
                    + "factura correspondiente.";
        }
        int estado = (anticipo.getEstado() != null) ? anticipo.getEstado().intValue() : 0;
        if (estado == ESTADO_ANULADO) {
            return "El anticipo " + anticipo.getId() + " ya está anulado.";
        }
        if (estado == EstadoAnticipoCliente.MIGRADO) {
            return "El registro " + anticipo.getId() + " es un movimiento histórico de un "
                    + "cruce anterior a la migración, no un anticipo: no se anula desde aquí.";
        }
        return null;
    }

    /**
     * Determina qué cruces con facturas hay que reversar para poder anular el
     * anticipo.
     * <p>
     * Desde el 2026-08-20 cada cruce guarda de qué anticipo salió el dinero
     * (FK APLCANTO), así que la respuesta es exacta: los cruces activos de ESTE
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
    private AnalisisCruces analizarCruces(AnticipoCliente anticipo) throws Throwable {

        AnalisisCruces analisis = new AnalisisCruces();
        double valor = (anticipo.getValor() != null) ? anticipo.getValor() : 0.0;

        Long idTitular = (anticipo.getTitular() != null) ? anticipo.getTitular().getCodigo() : null;
        Long idEmpresa = (anticipo.getEmpresa() != null) ? anticipo.getEmpresa().getCodigo() : null;

        analisis.saldoDisponible = (anticipo.getSaldo() != null) ? anticipo.getSaldo() : 0.0;

        List<PersonaCuentaContable> cuentas = personaCuentaContableDaoService
                .selectByTitularRolTipoCuenta(idEmpresa, idTitular, RolPersona.CLIENTE, 2L);
        analisis.saldoGlobal = (!cuentas.isEmpty() && cuentas.get(0).getSaldoInicial() != null)
                ? cuentas.get(0).getSaldoInicial() : 0.0;

        // 1. Cruces exactos: los que declaran a este anticipo como origen.
        double acumulado = 0.0;
        for (AplicacionPagoCxc cruce
                : aplicacionPagoCxcDaoService.selectCrucesByAnticipoOrigen(anticipo.getId(), true)) {
            analisis.cruces.add(cruce);
            acumulado += (cruce.getMontoAplicado() != null) ? cruce.getMontoAplicado() : 0.0;
        }

        // 2. Lo que el anticipo perdió y sus cruces directos no explican: son
        //    cruces viejos sin FK. Se completan por LIFO, como antes.
        analisis.deficit = valor - analisis.saldoDisponible;
        double sinAtribuir = analisis.deficit - acumulado;
        if (sinAtribuir > TOLERANCIA) {
            for (AplicacionPagoCxc cruce : aplicacionPagoCxcDaoService
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

        /** Saldo disponible de ESTE anticipo (ANTC.ANTCSALD). */
        private double saldoDisponible = 0.0;

        /** Saldo global de anticipos del cliente (TSR.PRCC.PRCCSLIN). */
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
        private final List<AplicacionPagoCxc> cruces = new ArrayList<>();

        /**
         * Detalle serializable de los cruces, para que la pantalla muestre qué
         * facturas se van a ver afectadas.
         * @return : Lista de mapas con la factura, el monto y la fecha del cruce
         */
        private List<Map<String, Object>> detalleCruces() {
            List<Map<String, Object>> detalle = new ArrayList<>();
            for (AplicacionPagoCxc cruce : cruces) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("idAplicacion", cruce.getId());
                fila.put("montoAplicado", cruce.getMontoAplicado());
                fila.put("fechaAplicacion", cruce.getFechaAplicacion());
                fila.put("observacion", cruce.getObservacion());
                if (cruce.getFactura() != null) {
                    fila.put("idFactura", cruce.getFactura().getId());
                    fila.put("numeroFactura", cruce.getFactura().getNumero());
                } else if (cruce.getLiquidacion() != null) {
                    fila.put("idFactura", cruce.getLiquidacion().getId());
                    fila.put("numeroFactura", cruce.getLiquidacion().getNumero());
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
                        + " rol=" + rolPersona + " tipoCuenta=2");
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

    @Override
    public java.util.Map<String, Object> solicitarDevolucion(Long idAnticipo, Double valor, Long idUsuario)
            throws Throwable {
        System.out.println("=== solicitarDevolucion (anticipo cliente) | idAnticipo=" + idAnticipo
                + " | valor=" + valor + " ===");

        if (idAnticipo == null) {
            throw new IncomeException("Debe indicar el anticipo a devolver.");
        }
        AnticipoCliente anticipo = anticipoDaoService.selectById(idAnticipo, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }
        if (anticipo.getEstado() == null
                || anticipo.getEstado().intValue() != EstadoAnticipoCliente.CONFIRMADO) {
            throw new IncomeException("Sólo se puede devolver un anticipo CONFIRMADO. Estado actual: "
                    + anticipo.getEstado());
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor a devolver debe ser mayor a cero.");
        }
        double saldo = anticipo.getSaldo() != null ? anticipo.getSaldo().doubleValue() : 0.0;
        if (valor.doubleValue() > saldo + TOLERANCIA) {
            throw new IncomeException("El valor a devolver ($" + valor + ") supera el saldo disponible "
                    + "del anticipo ($" + saldo + ").");
        }
        // Idempotencia (ANTCIDPG/ANTCAPLC, ver docs/logica-negocio/cxc/sql/
        // add-anticipo-cliente-devolucion.sql): con aplicado==0 hay una devolución previa
        // todavía sin confirmar/aplicar -- selectVigentesByOrigen no la cubre mientras el
        // pago sigue POR_APROBAR, así que el guardián real es este.
        if (anticipo.getIdPagoDevolucion() != null
                && Long.valueOf(0L).equals(anticipo.getAplicado())) {
            throw new IncomeException("El anticipo " + idAnticipo + " ya tiene una devolución en curso "
                    + "(pago " + anticipo.getIdPagoDevolucion() + ", todavía sin confirmar/aplicar). "
                    + "Debe resolverse antes de solicitar otra.");
        }

        Titular titular = anticipo.getTitular();
        if (titular == null) {
            throw new IncomeException("El anticipo " + idAnticipo + " no tiene titular asociado.");
        }
        Long idEmpresa = anticipo.getEmpresa() != null ? anticipo.getEmpresa().getCodigo() : null;
        if (idEmpresa == null) {
            throw new IncomeException("El anticipo " + idAnticipo + " no tiene empresa asignada.");
        }

        BeneficiarioOcasional beneficiario = new BeneficiarioOcasional();
        beneficiario.setNombre(titular.getRazonSocial() != null && !titular.getRazonSocial().trim().isEmpty()
                ? titular.getRazonSocial() : titular.getNombre());
        beneficiario.setIdentificacion(titular.getIdentificacion());

        // Cuenta de origen nula (punto 14): la solicitud nace POR_APROBAR, sin cuenta ni
        // forma de pago -- tesorería las asigna después con POST /pgtr/aprobar. Sin desglose
        // contable a propósito: no hay parametrización todavía para este origen (mismo
        // criterio que CRD_DEVOLUCION_APORTE antes de su fase 7).
        Map<String, Object> resultadoPago = pagoProgramadoService.registrarPagoDeOrigenExterno(
                OrigenPagoExterno.CXC_DEVOLUCION_CLIENTE, anticipo.getId(), idEmpresa,
                null, valor, java.time.LocalDate.now().toString(), beneficiario, null,
                "Devolución anticipo cliente #" + idAnticipo, idUsuario, false, null);

        Long idPago = (Long) resultadoPago.get("pago");

        anticipo.setIdPagoDevolucion(idPago);
        anticipo.setAplicado(Long.valueOf(0L));
        anticipoDaoService.save(anticipo, anticipo.getId());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("exito", true);
        resultado.put("idAnticipo", idAnticipo);
        resultado.put("idPago", idPago);
        resultado.put("mensaje", "Devolución solicitada por $" + valor + ". Queda pendiente de "
                + "aprobación en el circuito único de pagos.");
        System.out.println("✓ Devolución solicitada para anticipo " + idAnticipo + " | idPago=" + idPago);
        return resultado;
    }

    // ========================================================================
    // Reconciliador de la devolución (ítem 5, 2026-08-28) — mismo patrón que
    // CRD.DevolucionAporteServiceImpl.sincronizarDevolucion/sincronizarPagos.
    // ========================================================================

    /**
     * Reconcilia ANTES de listar/consultar, para que la pantalla nunca dependa de un timer
     * (mismo criterio que {@code DevolucionAporteServiceImpl.listarPorEntidad} en CRD — que
     * de hecho tiene su propio timer con el {@code @Schedule} comentado a propósito: el
     * mecanismo vivo es este, no un timer). No hay timer nuevo para CXC por ahora.
     * <p>
     * Cada anticipo se reconcilia en su propia transacción vía {@link #self}; un fallo no
     * impide devolver el listado.
     */
    private void reconciliarDevolucionesPendientes(Long idTitular, Long idEmpresa) {
        try {
            List<AnticipoCliente> pendientes = anticipoDaoService.selectConDevolucionPendiente();
            if (pendientes == null) {
                return;
            }
            for (AnticipoCliente pendiente : pendientes) {
                boolean mismoTitular = pendiente.getTitular() != null
                        && pendiente.getTitular().getCodigo() != null
                        && pendiente.getTitular().getCodigo().equals(idTitular);
                boolean mismaEmpresa = pendiente.getEmpresa() != null
                        && pendiente.getEmpresa().getCodigo() != null
                        && pendiente.getEmpresa().getCodigo().equals(idEmpresa);
                if (!mismoTitular || !mismaEmpresa) {
                    continue;
                }
                try {
                    self.sincronizarDevolucion(pendiente.getId());
                } catch (Throwable e) {
                    System.err.println("Error al reconciliar la devolución del anticipo "
                            + pendiente.getId() + " antes de listar: " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            System.err.println("Error al buscar devoluciones pendientes antes de listar: "
                    + e.getMessage());
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, Object> sincronizarDevoluciones() throws Throwable {
        System.out.println("========================================");
        System.out.println("SINCRONIZACIÓN DE DEVOLUCIONES DE ANTICIPOS DE CLIENTE");
        System.out.println("========================================");

        Map<String, Object> resumen = new HashMap<>();
        int evaluadas = 0, aplicadas = 0, conError = 0;
        List<String> errores = new ArrayList<>();

        List<AnticipoCliente> pendientes = anticipoDaoService.selectConDevolucionPendiente();
        int universo = (pendientes != null) ? pendientes.size() : 0;
        System.out.println("Anticipos con devolución pendiente a evaluar: " + universo);

        if (pendientes != null) {
            for (AnticipoCliente pendiente : pendientes) {
                try {
                    // A través del proxy: cada anticipo commitea por separado.
                    Map<String, Object> parcial = self.sincronizarDevolucion(pendiente.getId());
                    evaluadas++;
                    if (Boolean.TRUE.equals(parcial.get("aplicado"))) {
                        aplicadas++;
                    }
                } catch (Throwable e) {
                    // Un anticipo con datos malos no aborta el lote.
                    evaluadas++;
                    conError++;
                    errores.add("Anticipo " + pendiente.getId() + ": " + e.getMessage());
                    System.err.println("Error al reconciliar la devolución del anticipo "
                            + pendiente.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        resumen.put("evaluadas", evaluadas);
        resumen.put("aplicadas", aplicadas);
        resumen.put("conError", conError);
        resumen.put("errores", errores);
        System.out.println("SINCRONIZACIÓN TERMINADA - Evaluadas: " + evaluadas
                + " - Aplicadas: " + aplicadas + " - Con error: " + conError);
        return resumen;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<String, Object> sincronizarDevolucion(Long idAnticipo) throws Throwable {
        System.out.println("=== sincronizarDevolucion (anticipo cliente) | idAnticipo=" + idAnticipo + " ===");

        if (idAnticipo == null) {
            throw new IncomeException("Debe indicar el anticipo a sincronizar.");
        }
        AnticipoCliente anticipo = anticipoDaoService.selectById(idAnticipo, NombreEntidadesCobro.ANTICIPO_CLIENTE);
        if (anticipo == null) {
            throw new IncomeException("No se encontró el anticipo con ID: " + idAnticipo);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("idAnticipo", idAnticipo);
        resultado.put("aplicado", false);

        // Idempotencia (ANTCIDPG/ANTCAPLC): sin pago en curso, o ya aplicado, no hay nada
        // que hacer. Este es el guardián: una segunda corrida sobre el mismo pago ya
        // CONFIRMADO ve aplicado==1 y no vuelve a descontar.
        if (anticipo.getIdPagoDevolucion() == null) {
            resultado.put("mensaje", "El anticipo no tiene ninguna devolución en curso.");
            return resultado;
        }
        if (Long.valueOf(1L).equals(anticipo.getAplicado())) {
            resultado.put("mensaje", "La devolución del anticipo ya fue aplicada.");
            return resultado;
        }

        // cxc → cxp: se LEE el estado real del pago. CXP no avisa: no puede nombrar a CXC.
        PagoProgramado pago = em.find(PagoProgramado.class, anticipo.getIdPagoDevolucion());
        if (pago == null) {
            // La orden de pago ya no existe. Se deja el anticipo como está y se registra:
            // es un dato para investigar, no un error que aborte la corrida.
            resultado.put("mensaje", "El pago " + anticipo.getIdPagoDevolucion()
                    + " de la devolución ya no existe en Cuentas por Pagar.");
            System.err.println("  ⚠ Devolución del anticipo " + idAnticipo + " huérfana: el pago "
                    + anticipo.getIdPagoDevolucion() + " no existe.");
            return resultado;
        }

        int estadoPago = (pago.getEstado() != null) ? pago.getEstado().intValue() : 0;

        if (estadoPago == EstadoPagoProgramado.CONFIRMADO) {
            double saldo = anticipo.getSaldo() != null ? anticipo.getSaldo().doubleValue() : 0.0;
            double valorPago = pago.getValor() != null ? pago.getValor().doubleValue() : 0.0;
            anticipo.setSaldo(saldo - valorPago);
            anticipo.setAplicado(Long.valueOf(1L));
            anticipoDaoService.save(anticipo, anticipo.getId());
            resultado.put("aplicado", true);
            resultado.put("mensaje", "Devolución aplicada: saldo descontado por $" + valorPago + ".");
            System.out.println("  ✅ Devolución del anticipo " + idAnticipo + " APLICADA - Pago: "
                    + pago.getId() + " - Nuevo saldo: " + anticipo.getSaldo());

        } else if (estadoPago == EstadoPagoProgramado.RECHAZADO
                || estadoPago == EstadoPagoProgramado.ANULADO) {
            // Nada que descontar: el pago no llegó a confirmarse. Se marca aplicado igual
            // para liberar el "en curso" y permitir una nueva solicitud sobre este anticipo.
            anticipo.setAplicado(Long.valueOf(1L));
            anticipoDaoService.save(anticipo, anticipo.getId());
            resultado.put("mensaje", "El pago quedó " + (estadoPago == EstadoPagoProgramado.RECHAZADO
                    ? "RECHAZADO" : "ANULADO") + ": no se descuenta saldo. Queda libre para "
                    + "una nueva solicitud.");
            System.out.println("  ↩ Devolución del anticipo " + idAnticipo + " no aplicada "
                    + "(pago en estado " + estadoPago + ").");

        } else {
            resultado.put("mensaje", "El pago sigue en curso (estado " + estadoPago + "). Sin cambios.");
            System.out.println("  Devolución del anticipo " + idAnticipo + ": el pago sigue en curso "
                    + "(estado " + estadoPago + "). Sin cambios.");
        }

        return resultado;
    }
}
