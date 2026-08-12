package com.saa.ejb.cxp.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cxp.dao.AnticipoProveedorDaoService;
import com.saa.ejb.cxp.service.AnticipoProveedorService;
import com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cxp.AnticipoProveedor;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.PersonaCuentaContable;
import com.saa.model.tsr.Titular;
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
 * Estados del anticipo:
 *   1 = Ingresado  (grabado, pendiente de confirmación)
 *   2 = Confirmado (asiento contable generado)
 *   3 = Anulado
 */
@Stateless
public class AnticipoProveedorServiceImpl implements AnticipoProveedorService {

    @EJB
    private AnticipoProveedorDaoService anticipoDaoService;

    @EJB
    private AsientoContableService asientoContableService;

    @EJB
    private PersonaCuentaContableDaoService personaCuentaContableDaoService;

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
    // procesarAnticipo — graba + asiento en un solo paso
    // =========================================================================

    @Override
    public Map<String, Object> procesarAnticipo(
            Long idTitular, Double valor, Long idCuentaBancaria,
            Long idEmpresa, Long idUsuario, String fechaAnticipo,
            String numeroDoc, String observacion) throws Throwable {

        System.out.println("=== procesarAnticipoProveedor | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

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
        // Leer saldoInicial del PRCC (tipoCuenta=2=Anticipos, rol Proveedor)
        // y sumarlo al valor del anticipo para obtener el saldo real acumulado.
        Double saldoInicialPrcc = (cuentasAnticipo.get(0).getSaldoInicial() != null)
                ? cuentasAnticipo.get(0).getSaldoInicial() : 0.0;

        AnticipoProveedor anticipo = new AnticipoProveedor();
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
        System.out.println("✓ AnticipoProveedor guardado ID=" + anticipo.getId());

        // ── Generar asiento contable ───────────────────────────────────────────
        Asiento asiento = asientoContableService.generarAsientoAnticipoProveedor(
                anticipo, idCuentaBancaria,
                TipoAsientos.ANTICIPOS_PROVEEDOR,
                usuario.getNombre() != null ? usuario.getNombre() : usuario.getCodigo().toString());

        // ── Confirmar anticipo ─────────────────────────────────────────────────
        anticipo.setEstado(2L); // Confirmado
        anticipo.setAsiento(asiento);
        anticipo = anticipoDaoService.save(anticipo, anticipo.getId());

        // ── Sumar valor al saldoInicial de PersonaCuentaContable (tipoCuenta=2, rol Proveedor) ─
        actualizarSaldoInicialPrcc(idTitular, idEmpresa, RolPersona.PROVEEDOR, valor);

        resultado.put("exito", true);
        resultado.put("estado", "CONFIRMADO");
        resultado.put("mensaje", "Anticipo a proveedor procesado correctamente. "
                + "Asiento generado: " + asiento.getNumeroAlterno());
        resultado.put("asiento", asiento.getNumeroAlterno());
        resultado.put("anticipo", anticipo);
        System.out.println("✓ AnticipoProveedor " + anticipo.getId()
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
