package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cnt.service.AsientoContableService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.DetalleAsientoService;
import com.saa.ejb.cnt.service.TipoAsientoService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetalleAsiento;
import com.saa.model.cnt.PlanCuenta;
import com.saa.model.cnt.TipoAsiento;
import com.saa.model.cxc.AnticipoCliente;
import com.saa.model.cxc.DetalleFactura;
import com.saa.model.cxc.Factura;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.Titular;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoMoneda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Implementación del servicio genérico de generación de asientos contables.
 *
 * Para la factura de venta el asiento queda así:
 *
 *   DEBE:
 *     Cuenta CxC del cliente (PersonaCuentaContable, tipoCuenta=1, tipoPersona=1)
 *       → valor = total de la factura
 *
 *   HABER:
 *     Una línea por cada grupo de producto (consolidado)
 *       → cuenta = GrupoProductoCobro.planCuenta
 *       → valor  = suma de baseImponible de los detalles de ese grupo
 *     Una línea por cada tipo de IVA con valor > 0
 *       → cuenta = Tsri.planCuenta  (lsri.tabla='17', tsri.codigo = codigoIVASRI del detalle)
 *       → valor  = suma de valorIVA de ese tipo
 *
 *   DEBE total = HABER total  (total factura = subtotales + impuestos)
 */
@Stateless
public class AsientoContableServiceImpl implements AsientoContableService {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AsientoService asientoService;

    @EJB
    private DetalleAsientoService detalleAsientoService;

    @EJB
    private TipoAsientoService tipoAsientoService;

    // ---------------------------------------------------------------
    // validarCuentasContables
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContables(Titular titular,
            List<DetalleFactura> detalles, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxC del cliente
        if (titular == null) {
            errores.add("No se especificó el titular (cliente) de la factura.");
        } else {
            PlanCuenta cuentaCliente = obtenerCuentaCliente(titular.getCodigo(), idEmpresa);
            if (cuentaCliente == null) {
                errores.add("El cliente '" + titular.getNombre()
                        + "' (ID: " + titular.getCodigo()
                        + ") no tiene cuenta contable de facturas configurada. "
                        + "Configure la cuenta en Tesorería → Persona → Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Cliente).");
            }
        }

        // 2. Validar cuentas de grupos de producto e IVA por cada detalle
        if (detalles != null) {
            // Grupos ya validados (evitar mensajes duplicados)
            java.util.Set<Long> gruposValidados = new java.util.HashSet<>();
            java.util.Set<Long> ivaValidados    = new java.util.HashSet<>();

            for (DetalleFactura detalle : detalles) {
                String desc = "'" + (detalle.getDescripcion() != null
                        ? detalle.getDescripcion() : "sin descripción") + "'";

                // 2a. Validar grupo de producto
                if (detalle.getProducto() == null) {
                    errores.add("El detalle " + desc + " no tiene producto asignado.");
                } else if (detalle.getProducto().getGrupoProducto() == null) {
                    errores.add("El producto '" + detalle.getDescripcion()
                            + "' no tiene grupo de producto asignado.");
                } else {
                    Long idGrupo = detalle.getProducto().getGrupoProducto().getCodigo();
                    if (!gruposValidados.contains(idGrupo)) {
                        gruposValidados.add(idGrupo);
                        PlanCuenta pc = detalle.getProducto().getGrupoProducto().getPlanCuenta();
                        if (pc == null) {
                            errores.add("El grupo de producto '"
                                    + detalle.getProducto().getGrupoProducto().getNombre()
                                    + "' no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en Facturación → Grupos de Producto.");
                        }
                    }
                }

                // 2b. Validar cuenta de IVA (solo si tiene valor de IVA > 0)
                if (detalle.getValorIVA() != null && detalle.getValorIVA() > 0
                        && detalle.getCodigoIVASRI() != null) {
                    Long codigoIVA = detalle.getCodigoIVASRI();
                    if (!ivaValidados.contains(codigoIVA)) {
                        ivaValidados.add(codigoIVA);
                        // Buscar por tsri.codigo (String) dentro de lsri.tabla='17'
                        PlanCuenta cuentaIVA = obtenerCuentaIVA(String.valueOf(codigoIVA));
                        if (cuentaIVA == null) {
                            String detalleIVA = obtenerDetalleIVA(String.valueOf(codigoIVA));
                            errores.add("El tipo de IVA '" + detalleIVA
                                    + "' (código SRI: " + codigoIVA
                                    + ") no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en Facturación → Tipos SRI → IVA (categoría 17).");
                        }
                    }
                }
            }
        }

        return errores;
    }

    // ---------------------------------------------------------------
    // generarAsientoFactura
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoFactura(Long idFactura, Long idEmpresa,
            int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoFactura | idFactura=" + idFactura
                + " | empresa=" + idEmpresa + " ===");

        // 1. Cargar la factura
        Factura factura = em.find(Factura.class, idFactura);
        if (factura == null) {
            throw new IncomeException("No se encontró la factura con ID: " + idFactura);
        }

        // 2. Cargar detalles de la factura
        @SuppressWarnings("unchecked")
        List<DetalleFactura> detalles = em.createQuery(
                "SELECT d FROM DetalleFactura d WHERE d.factura.id = :id AND d.estado = 1")
                .setParameter("id", idFactura)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La factura " + idFactura + " no tiene detalles activos.");
        }

        // 3. Construir líneas del asiento
        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: cuenta CxC del cliente ──────────────────────────────────────
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                factura.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontró cuenta contable (tipo factura) "
                    + "para el cliente ID: " + factura.getTitular().getCodigo()
                    + " en la empresa: " + idEmpresa);
        }

        DetalleAsiento lineaDebe = new DetalleAsiento();
        lineaDebe.setPlanCuenta(cuentaCliente);
        lineaDebe.setNumeroCuenta(cuentaCliente.getCuentaContable());
        lineaDebe.setNombreCuenta(cuentaCliente.getNombre());
        lineaDebe.setDescripcion("Cliente: " + factura.getTitular().getNombre());
        lineaDebe.setValorDebe(nvl(factura.getTotal()));
        lineaDebe.setValorHaber(0.0);
        lineas.add(lineaDebe);

        // ── HABER: una línea por grupo de producto (consolidado) ──────────────
        // Agrupar detalles por GrupoProductoCobro
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        for (DetalleFactura detalle : detalles) {
            if (detalle.getProducto() == null
                    || detalle.getProducto().getGrupoProducto() == null) {
                throw new IncomeException("El detalle '" + detalle.getDescripcion()
                        + "' no tiene grupo de producto asignado. "
                        + "Configure el grupo de producto para generar el asiento.");
            }
            Long idGrupo = detalle.getProducto().getGrupoProducto().getCodigo();
            PlanCuenta pc = detalle.getProducto().getGrupoProducto().getPlanCuenta();
            if (pc == null) {
                throw new IncomeException("El grupo de producto '"
                        + detalle.getProducto().getGrupoProducto().getNombre()
                        + "' no tiene cuenta contable asignada.");
            }
            subtotalPorGrupo.merge(idGrupo, nvl(detalle.getBaseImponible()), Double::sum);
            cuentaPorGrupo.putIfAbsent(idGrupo, pc);
            nombreGrupo.putIfAbsent(idGrupo,
                    detalle.getProducto().getGrupoProducto().getNombre());
        }

        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento lineaHaber = new DetalleAsiento();
            lineaHaber.setPlanCuenta(pc);
            lineaHaber.setNumeroCuenta(pc.getCuentaContable());
            lineaHaber.setNombreCuenta(pc.getNombre());
            lineaHaber.setDescripcion("Ventas: " + nombreGrupo.get(idGrupo));
            lineaHaber.setValorDebe(0.0);
            lineaHaber.setValorHaber(subtotalPorGrupo.get(idGrupo));
            lineas.add(lineaHaber);
        }

        // ── HABER: una línea por tipo de IVA con valor > 0 ───────────────────
        // Agrupar por codigoIVASRI (como String = campo CODIGO de TSRI) → sumar valorIVA
        Map<String, Double> ivaParaTipo = new LinkedHashMap<>();
        for (DetalleFactura detalle : detalles) {
            if (detalle.getValorIVA() != null && detalle.getValorIVA() > 0
                    && detalle.getCodigoIVASRI() != null) {
                String codigoStr = String.valueOf(detalle.getCodigoIVASRI());
                ivaParaTipo.merge(codigoStr, nvl(detalle.getValorIVA()), Double::sum);
            }
        }

        for (Map.Entry<String, Double> entry : ivaParaTipo.entrySet()) {
            String codigoIVASRI = entry.getKey();
            Double valorIVA     = entry.getValue();

            PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoIVASRI);
            if (cuentaIVA == null) {
                throw new IncomeException(
                        "No se encontró cuenta contable para el IVA con código SRI: "
                        + codigoIVASRI
                        + ". Configure la cuenta en Facturación → Tipos SRI → IVA (categoría 17).");
            }
            DetalleAsiento lineaIVA = new DetalleAsiento();
            lineaIVA.setPlanCuenta(cuentaIVA);
            lineaIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            lineaIVA.setNombreCuenta(cuentaIVA.getNombre());
            lineaIVA.setDescripcion("IVA código SRI: " + codigoIVASRI);
            lineaIVA.setValorDebe(0.0);
            lineaIVA.setValorHaber(valorIVA);
            lineas.add(lineaIVA);
        }

        // 4. Generar el asiento con las líneas construidas
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsiento  (método genérico de bajo nivel)
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas) throws Throwable {

        System.out.println("=== generarAsiento | empresa=" + idEmpresa
                + " | tipoAlt=" + codigoAltTipoAsiento + " ===");

        // 1. Obtener TipoAsiento por codigoAlterno y sistema=1
        Long idTipoAsiento = tipoAsientoService.codigoByAlterno(codigoAltTipoAsiento, idEmpresa);
        if (idTipoAsiento == null || idTipoAsiento == 0L) {
            throw new IncomeException(
                    "No existe TipoAsiento con codigoAlterno=" + codigoAltTipoAsiento
                    + " y sistema=1 en la empresa " + idEmpresa
                    + ". Cree el tipo de asiento en Contabilidad.");
        }
        TipoAsiento tipoAsiento = tipoAsientoService.selectById(idTipoAsiento);

        // 2. Obtener Empresa
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        if (empresa == null) {
            throw new IncomeException("No se encontró la empresa con ID: " + idEmpresa);
        }

        // 3. Construir cabecera del asiento
        Asiento asiento = new Asiento();
        asiento.setCodigo(null);
        asiento.setEmpresa(empresa);
        asiento.setTipoAsiento(tipoAsiento);
        asiento.setFechaAsiento(fechaAsiento);
        asiento.setObservaciones(observaciones);
        asiento.setNombreUsuario(usuario != null ? usuario : "SISTEMA");
        asiento.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
        asiento.setMoneda(Long.valueOf(TipoMoneda.DOLAR));
        // Rubro módulo: CXC
        asiento.setRubroModuloClienteP(Long.valueOf(Rubros.MODULO_SISTEMA));
        asiento.setRubroModuloClienteH(Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
        asiento.setRubroModuloSistemaP(Long.valueOf(Rubros.MODULO_SISTEMA));
        asiento.setRubroModuloSistemaH(Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));

        // 4. saveSingle: asigna período, número, numeroAlterno y graba
        asiento = asientoService.saveSingle(asiento);

        // 5. Grabar cada línea de detalle
        for (DetalleAsiento linea : lineas) {
            linea.setAsiento(asiento);
            detalleAsientoService.saveDetalle(linea);
        }

        // 6. Validar que debe == haber
        boolean cuadrado = detalleAsientoService.validaDebeHaber(asiento.getCodigo());
        if (!cuadrado) {
            System.err.println("⚠ ADVERTENCIA: El asiento " + asiento.getCodigo()
                    + " no está cuadrado (debe ≠ haber). Revisar cuentas configuradas.");
        }

        System.out.println("✓ Asiento contable generado: " + asiento.getNumeroAlterno()
                + " | ID: " + asiento.getCodigo());
        return asiento;
    }

    // ---------------------------------------------------------------
    // generarAsientoAnticipo
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoAnticipo(AnticipoCliente anticipo,
            int codigoAltTipoAsiento, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAnticipo | idAnticipo=" + anticipo.getId() + " ===");

        if (anticipo.getEmpresa() == null || anticipo.getEmpresa().getCodigo() == null) {
            throw new com.saa.basico.util.IncomeException(
                    "El anticipo no tiene empresa contable configurada.");
        }

        Long idEmpresa       = anticipo.getEmpresa().getCodigo();
        Long codigoTitular   = anticipo.getTitular().getCodigo();
        Double valor         = anticipo.getValor();
        String nomCliente    = anticipo.getTitular().getNombre();

        // ── DEBE: cuenta caja/banco (tipoCuenta=3, tipoPersona=1) ──────────────
        PlanCuenta cuentaCaja = obtenerCuentaPorTipo(codigoTitular, idEmpresa, 3L);
        if (cuentaCaja == null) {
            throw new com.saa.basico.util.IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta de caja/banco (Tipo 3) "
                    + "configurada en Tesorería → Persona → Cuentas Contables.");
        }

        // ── HABER: cuenta de anticipos del cliente (tipoCuenta=2, tipoPersona=1) ─
        PlanCuenta cuentaAnticipo = obtenerCuentaPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new com.saa.basico.util.IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en Tesorería → Persona → Cuentas Contables.");
        }

        // ── Construir líneas ───────────────────────────────────────────────────
        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaCaja);
        debe.setNumeroCuenta(cuentaCaja.getCuentaContable());
        debe.setNombreCuenta(cuentaCaja.getNombre());
        debe.setDescripcion("Anticipo recibido de: " + nomCliente
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : ""));
        debe.setValorDebe(valor);
        debe.setValorHaber(0.0);
        lineas.add(debe);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaAnticipo);
        haber.setNumeroCuenta(cuentaAnticipo.getCuentaContable());
        haber.setNombreCuenta(cuentaAnticipo.getNombre());
        haber.setDescripcion("Anticipo cliente: " + nomCliente
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : ""));
        haber.setValorDebe(0.0);
        haber.setValorHaber(valor);
        lineas.add(haber);

        // ── Generar asiento ────────────────────────────────────────────────────
        String obs = "Anticipo cliente: " + nomCliente
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : "")
                + " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                anticipo.getFechaAnticipo(), obs, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    /**
     * Obtiene la cuenta contable de un cliente por tipo de cuenta.
     * tipoCuenta: 1=Facturas, 2=Anticipos, 3=Caja/Banco
     */
    private PlanCuenta obtenerCuentaPorTipo(Long codigoTitular, Long idEmpresa, Long tipoCuenta) {
        System.out.println("  [obtenerCuentaPorTipo] titular=" + codigoTitular
                + " | empresa=" + idEmpresa + " | tipoCuenta=" + tipoCuenta
                + " | tipoPersona=1 (Cliente)");
        try {
            // NOTA: No se filtra por pcc.tipoPersona porque en BD ese campo es null en todos
            // los registros — el rol (cliente/proveedor) ya queda determinado por la tabla
            // PersonaRol (PRRL) a través del join. El criterio correcto es:
            // titular + empresa + tipoCuenta.
            String sql = "SELECT pcc.planCuenta FROM PersonaCuentaContable pcc "
                    + "JOIN pcc.personaRol pr "
                    + "WHERE pr.titular.codigo = :titular "
                    + "AND pcc.tipoCuenta = :tipo "
                    + "AND pcc.empresa.codigo = :empresa";
            Query q = em.createQuery(sql);
            q.setParameter("titular", codigoTitular);
            q.setParameter("tipo", tipoCuenta);
            q.setParameter("empresa", idEmpresa);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            if (result.isEmpty()) {
                // Log de diagnóstico: verificar cuántos PersonaRol existen para este titular
                try {
                    long totalPrrl = ((Number) em.createQuery(
                            "SELECT COUNT(pr) FROM PersonaRol pr WHERE pr.titular.codigo = :t")
                            .setParameter("t", codigoTitular)
                            .getSingleResult()).longValue();
                    long totalPrcc = ((Number) em.createQuery(
                            "SELECT COUNT(pcc) FROM PersonaCuentaContable pcc "
                            + "JOIN pcc.personaRol pr "
                            + "WHERE pr.titular.codigo = :t")
                            .setParameter("t", codigoTitular)
                            .getSingleResult()).longValue();
                    System.err.println("  [obtenerCuentaPorTipo] ✗ No encontrado."
                            + " PersonaRol del titular: " + totalPrrl
                            + " | PersonaCuentaContable del titular (sin filtros de empresa/tipo): " + totalPrcc);
                    // Mostrar valores REALES almacenados en cada PersonaCuentaContable del titular
                    @SuppressWarnings("unchecked")
                    List<Object[]> rawRows = em.createQuery(
                            "SELECT pcc.codigo, pcc.tipoCuenta, pcc.tipoPersona, "
                            + "pcc.empresa.codigo, pr.estado, pcc.planCuenta.cuentaContable "
                            + "FROM PersonaCuentaContable pcc "
                            + "JOIN pcc.personaRol pr "
                            + "WHERE pr.titular.codigo = :t")
                            .setParameter("t", codigoTitular)
                            .getResultList();
                    for (Object[] row : rawRows) {
                        System.err.println("  [obtenerCuentaPorTipo] PRCC registro:"
                                + " PRCCCDGO=" + row[0]
                                + " | tipoCuenta(PRCCTPOO)=" + row[1]
                                + " | tipoPersona(PRCCCLPR)=" + row[2]
                                + " | empresa(PJRQCDGO)=" + row[3]
                                + " | pr.estado(PRRLESTD)=" + row[4]
                                + " | cuentaContable=" + row[5]);
                    }
                } catch (Exception ex) {
                    System.err.println("  [obtenerCuentaPorTipo] ✗ No encontrado (diagnóstico falló: " + ex.getMessage() + ")");
                }
                return null;
            }
            PlanCuenta pc = (PlanCuenta) result.get(0);
            System.out.println("  [obtenerCuentaPorTipo] ✓ Cuenta encontrada: "
                    + pc.getCuentaContable() + " - " + pc.getNombre());
            return pc;
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta tipo " + tipoCuenta
                    + " del cliente " + codigoTitular + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene la cuenta contable CxC del cliente (tipoCuenta=1).
     */
    private PlanCuenta obtenerCuentaCliente(Long codigoTitular, Long idEmpresa) {
        return obtenerCuentaPorTipo(codigoTitular, idEmpresa, 1L);
    }

    /**
     * Obtiene la cuenta contable del IVA desde TSRI.
     * Busca por tsri.codigo (campo String) dentro de la categoría lsri.tabla = '17' (IVA).
     * El campo codigoIVASRI de DetalleFactura almacena el valor numérico del campo CODIGO de TSRI.
     */
    private PlanCuenta obtenerCuentaIVA(String codigoIVASRI) {
        try {
            String sql = "SELECT t.planCuenta FROM Tsri t "
                    + "WHERE t.codigo = :codigo "
                    + "AND t.lsri.tabla = '17' "
                    + "AND t.estado = 1";
            Query q = em.createQuery(sql);
            q.setParameter("codigo", codigoIVASRI);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            return result.isEmpty() ? null : (PlanCuenta) result.get(0);
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta de IVA codigo=" + codigoIVASRI + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el texto descriptivo de un tipo de IVA desde TSRI para mensajes de error.
     * Busca por tsri.codigo (String) dentro de la categoría lsri.tabla='17'.
     */
    private String obtenerDetalleIVA(String codigoIVASRI) {
        try {
            String sql = "SELECT t.detalle FROM Tsri t "
                    + "WHERE t.codigo = :codigo AND t.lsri.tabla = '17' AND t.estado = 1";
            Query q = em.createQuery(sql);
            q.setParameter("codigo", codigoIVASRI);
            q.setMaxResults(1);
            List<?> r = q.getResultList();
            return r.isEmpty() ? codigoIVASRI : (String) r.get(0);
        } catch (Exception e) {
            return codigoIVASRI;
        }
    }

    private double nvl(Double val) {
        return val != null ? val : 0.0;
    }

    // =========================================================================
    // Stubs CXC — Documentos de Cobro
    // =========================================================================
    // Estos métodos están listos para recibir la plantilla (codigoAltTipoAsiento)
    // y los auxiliares correspondientes. Por ahora lanzan UnsupportedOperationException
    // con un mensaje descriptivo de lo que se debe configurar.
    // =========================================================================

    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaCredito(
            Long idNotaCredito, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.NOTAS_CREDITO_VENTA (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxC del cliente (PersonaCuentaContable, tipoCuenta=1, tipoPersona=1)
        //   · AuxiliarUno HABER: cuenta contable del grupo de producto (GrupoProductoCobro.planCuenta)
        //                        + cuenta de IVA (Tsri.planCuenta, lsri.tabla='17')
        throw new UnsupportedOperationException(
                "generarAsientoNotaCredito aún no implementado. "
                + "Defina la plantilla TipoAsientos.NOTAS_CREDITO_VENTA en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaDebito(
            Long idNotaDebito, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.NOTAS_DEBITO_VENTA (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxC del cliente (PersonaCuentaContable, tipoCuenta=1, tipoPersona=1)
        //   · AuxiliarUno HABER: cuenta contable del grupo de producto (GrupoProductoCobro.planCuenta)
        //                        + cuenta de IVA (Tsri.planCuenta, lsri.tabla='17')
        throw new UnsupportedOperationException(
                "generarAsientoNotaDebito aún no implementado. "
                + "Defina la plantilla TipoAsientos.NOTAS_DEBITO_VENTA en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoLiquidacionCompra(
            Long idLiquidacion, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxP del proveedor/prestador de servicio
        //   · AuxiliarUno HABER: cuenta contable del grupo de producto del detalle
        //                        + cuenta de IVA (Tsri.planCuenta, lsri.tabla='17')
        throw new UnsupportedOperationException(
                "generarAsientoLiquidacionCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.LIQUIDACIONES_COMPRA_EMITIDAS en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionV2(
            Long idRetencionV2, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.RETENCIONES_EMITIDAS_V2 (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta de retención por código SRI del impuesto (DetalleRetencionV2.codRetencion)
        //   · AuxiliarUno HABER: cuenta CxP del proveedor sujeto a retención
        throw new UnsupportedOperationException(
                "generarAsientoRetencionV2 aún no implementado. "
                + "Defina la plantilla TipoAsientos.RETENCIONES_EMITIDAS_V2 en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    // =========================================================================
    // Stubs CXP — Documentos de Compra (recibidos del proveedor)
    // =========================================================================

    @Override
    public com.saa.model.cnt.Asiento generarAsientoFacturaCompra(
            Long idFacturaCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.FACTURAS_COMPRA (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta de gasto/costo del grupo de producto (GrupoProductoPago.planCuenta)
        //                        + cuenta de IVA en compras (Tsri.planCuenta, lsri.tabla='17')
        //   · AuxiliarUno HABER: cuenta CxP del proveedor (PersonaCuentaContable, tipoCuenta=?, tipoPersona=2)
        throw new UnsupportedOperationException(
                "generarAsientoFacturaCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.FACTURAS_COMPRA en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaCreditoCompra(
            Long idNotaCreditoCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.NOTAS_CREDITO_COMPRA (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxP del proveedor
        //   · AuxiliarUno HABER: cuenta de gasto/costo del grupo de producto
        //                        + cuenta de IVA (Tsri.planCuenta, lsri.tabla='17')
        throw new UnsupportedOperationException(
                "generarAsientoNotaCreditoCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.NOTAS_CREDITO_COMPRA en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaDebitoCompra(
            Long idNotaDebitoCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.NOTAS_DEBITO_COMPRA (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta de gasto/costo del detalle (motivo)
        //   · AuxiliarUno HABER: cuenta CxP del proveedor
        throw new UnsupportedOperationException(
                "generarAsientoNotaDebitoCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.NOTAS_DEBITO_COMPRA en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoLiquidacionCompraCompra(
            Long idLiquidacion, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta de gasto/costo del grupo de producto del detalle
        //                        + cuenta de IVA en compras
        //   · AuxiliarUno HABER: cuenta CxP del prestador de servicio (proveedor)
        throw new UnsupportedOperationException(
                "generarAsientoLiquidacionCompraCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionCompra(
            Long idRetencionCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.RETENCIONES_RECIBIDAS (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxP del proveedor (monto retenido disminuye deuda)
        //   · AuxiliarUno HABER: cuenta de retención recibida por código SRI (DetalleRetencionCompra.codRetencion)
        throw new UnsupportedOperationException(
                "generarAsientoRetencionCompra aún no implementado. "
                + "Defina la plantilla TipoAsientos.RETENCIONES_RECIBIDAS en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionCompraV2(
            Long idRetencionCompraV2, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // TODO — Implementar cuando se defina:
        //   · La plantilla de asiento: TipoAsientos.RETENCIONES_RECIBIDAS_V2 (codigoAlterno en BD)
        //   · AuxiliarUno DEBE:  cuenta CxP del proveedor (monto retenido disminuye deuda)
        //   · AuxiliarUno HABER: cuenta de retención recibida por código SRI del impuesto
        throw new UnsupportedOperationException(
                "generarAsientoRetencionCompraV2 aún no implementado. "
                + "Defina la plantilla TipoAsientos.RETENCIONES_RECIBIDAS_V2 en BD "
                + "y configure las cuentas auxiliares antes de activar este método.");
    }
}
