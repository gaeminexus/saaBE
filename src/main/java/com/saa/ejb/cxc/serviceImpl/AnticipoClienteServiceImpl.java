package com.saa.ejb.cxc.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cxc.dao.AnticipoClienteDaoService;
import com.saa.ejb.cxc.service.AnticipoClienteService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.NombreEntidadesCobro;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoAsientos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
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

    @EJB
    private AnticipoClienteDaoService anticipoDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

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
        // Leer saldoInicial del PRCC (tipoCuenta=2=Anticipos, rol Cliente)
        // y sumarlo al valor del anticipo para obtener el saldo real acumulado.
        Double saldoInicialPrcc = (cuentasAnticipo.get(0).getSaldoInicial() != null)
                ? cuentasAnticipo.get(0).getSaldoInicial() : 0.0;

        AnticipoCliente anticipo = new AnticipoCliente();
        anticipo.setTitular(titular);
        anticipo.setEmpresa(empresa);
        anticipo.setUsuario(usuario);
        anticipo.setFechaAnticipo(fecha);
        anticipo.setFechaRecepcion(fecha);
        anticipo.setValor(valor);
        anticipo.setSaldo(saldoInicialPrcc + valor);
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
}