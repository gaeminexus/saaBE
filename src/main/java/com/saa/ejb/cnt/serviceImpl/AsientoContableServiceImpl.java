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
    // validarCuentasContablesNC (Nota de Crédito)
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesNC(Titular titular,
            List<com.saa.model.cxc.DetalleNotaCredito> detalles, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxC del cliente
        if (titular == null) {
            errores.add("No se especificó el titular (cliente) de la nota de crédito.");
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
            java.util.Set<Long> productosValidados = new java.util.HashSet<>();
            java.util.Set<Long> ivaValidados       = new java.util.HashSet<>();

            for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
                String desc = "'" + (detalle.getDescripcion() != null
                        ? detalle.getDescripcion() : "sin descripción") + "'";

                if (detalle.getProducto() == null) {
                    errores.add("El detalle " + desc + " no tiene producto asignado.");
                } else {
                    Long idProd = detalle.getProducto();
                    if (!productosValidados.contains(idProd)) {
                        productosValidados.add(idProd);
                        try {
                            @SuppressWarnings("unchecked")
                            List<Object[]> grupoRows = em.createQuery(
                                    "SELECT p.grupoProducto.codigo, p.grupoProducto.nombre, p.grupoProducto.planCuenta "
                                    + "FROM ProductoCobro p WHERE p.id = :id")
                                    .setParameter("id", idProd)
                                    .setMaxResults(1)
                                    .getResultList();
                            if (grupoRows.isEmpty()) {
                                errores.add("El producto ID " + idProd + " (" + desc
                                        + ") no se encontró o no tiene grupo asignado.");
                            } else {
                                PlanCuenta pc = (PlanCuenta) grupoRows.get(0)[2];
                                String nomGrupo = (String) grupoRows.get(0)[1];
                                if (pc == null) {
                                    errores.add("El grupo de producto '" + nomGrupo
                                            + "' no tiene cuenta contable asignada. "
                                            + "Configure la cuenta en Facturación → Grupos de Producto.");
                                }
                            }
                        } catch (Exception e) {
                            errores.add("Error consultando grupo del producto ID " + idProd + ": " + e.getMessage());
                        }
                    }
                }

                // 2b. Validar cuenta de IVA
                if (detalle.getValorIVA() != null && detalle.getValorIVA() > 0
                        && detalle.getPorcentajeIVA() != null) {
                    Long porc = detalle.getPorcentajeIVA();
                    if (!ivaValidados.contains(porc)) {
                        ivaValidados.add(porc);
                        String codigoSRI = mapPorcentajeIVAaCodigo(porc);
                        PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoSRI);
                        if (cuentaIVA == null) {
                            errores.add("El IVA al " + porc + "% (código SRI: " + codigoSRI
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
    // validarCuentasContablesND (Nota de Débito)
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesND(com.saa.model.cxc.NotaDebito notaDebito, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        if (notaDebito == null) {
            errores.add("No se proporcionó la nota de débito.");
            return errores;
        }

        // 1. Validar cuenta CxC del cliente
        if (notaDebito.getTitular() == null) {
            errores.add("La nota de débito no tiene titular (cliente) asignado.");
        } else {
            PlanCuenta cuentaCliente = obtenerCuentaCliente(
                    notaDebito.getTitular().getCodigo(), idEmpresa);
            if (cuentaCliente == null) {
                errores.add("El cliente '" + notaDebito.getTitular().getNombre()
                        + "' (ID: " + notaDebito.getTitular().getCodigo()
                        + ") no tiene cuenta contable de facturas configurada. "
                        + "Configure la cuenta en Tesorería → Persona → Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Cliente).");
            }
        }

        // 2. Validar cuentas de ingreso desde la factura relacionada
        if (notaDebito.getFactura() == null) {
            errores.add("La nota de débito no tiene factura relacionada. "
                    + "Vincule la ND a una factura para poder generar el asiento contable.");
        } else {
            List<DetalleFactura> detallesFact = obtenerDetallesParaND(notaDebito);
            if (detallesFact == null || detallesFact.isEmpty()) {
                errores.add("La factura relacionada (ID: " + notaDebito.getFactura().getId()
                        + ") no tiene detalles activos. Sin detalles no se puede distribuir "
                        + "el asiento por grupo de producto.");
            } else {
                java.util.Set<Long> gruposValidados = new java.util.HashSet<>();
                for (DetalleFactura d : detallesFact) {
                    if (d.getProducto() == null || d.getProducto().getGrupoProducto() == null) continue;
                    Long idGrupo = d.getProducto().getGrupoProducto().getCodigo();
                    if (!gruposValidados.contains(idGrupo)) {
                        gruposValidados.add(idGrupo);
                        PlanCuenta pc = d.getProducto().getGrupoProducto().getPlanCuenta();
                        if (pc == null) {
                            errores.add("El grupo de producto '"
                                    + d.getProducto().getGrupoProducto().getNombre()
                                    + "' no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en Facturación → Grupos de Producto.");
                        }
                    }
                }
            }
        }

        // 3. Validar cuenta de IVA si aplica
        if (notaDebito.getvIVA() != null && notaDebito.getvIVA() > 0 && notaDebito.getpIVA() != null) {
            String codigoSRI = mapPorcentajeNDaCodigo(notaDebito.getpIVA());
            PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoSRI);
            if (cuentaIVA == null) {
                errores.add("El IVA al " + notaDebito.getpIVA().intValue() + "% (código SRI: " + codigoSRI
                        + ") no tiene cuenta contable asignada. "
                        + "Configure la cuenta en Facturación → Tipos SRI → IVA (categoría 17).");
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

        // 6. Validar que debe == haber — si no cuadra, lanzar excepción para revertir todo
        boolean cuadrado = detalleAsientoService.validaDebeHaber(asiento.getCodigo());
        if (!cuadrado) {
            double totalDebe  = 0.0;
            double totalHaber = 0.0;
            StringBuilder detalle = new StringBuilder();
            detalle.append("El asiento ").append(asiento.getCodigo())
                   .append(" no está cuadrado (debe ≠ haber). Detalle de líneas:\n");
            for (DetalleAsiento ln : lineas) {
                double debe  = ln.getValorDebe()  != null ? ln.getValorDebe()  : 0.0;
                double haber = ln.getValorHaber() != null ? ln.getValorHaber() : 0.0;
                totalDebe  += debe;
                totalHaber += haber;
                String cuenta = ln.getNumeroCuenta() != null ? ln.getNumeroCuenta() : "(sin cuenta)";
                String nombre = ln.getNombreCuenta()  != null ? ln.getNombreCuenta()  : "(sin nombre)";
                String desc   = ln.getDescripcion()   != null ? ln.getDescripcion()   : "";
                detalle.append(String.format("  [%s] %s | %s | DEBE=%.2f | HABER=%.2f%n",
                        cuenta, nombre, desc, debe, haber));
            }
            detalle.append(String.format("  TOTAL DEBE=%.2f | TOTAL HABER=%.2f | DIFERENCIA=%.2f",
                    totalDebe, totalHaber, Math.abs(totalDebe - totalHaber)));
            throw new IncomeException(detalle.toString());
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
    // generarAsientoAnticipoCliente (proceso unificado con cuenta bancaria)
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoAnticipoCliente(AnticipoCliente anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAnticipoCliente | idAnticipo=" + anticipo.getId() + " ===");

        if (anticipo.getEmpresa() == null || anticipo.getEmpresa().getCodigo() == null) {
            throw new IncomeException("El anticipo no tiene empresa contable configurada.");
        }
        if (idCuentaBancaria == null) {
            throw new IncomeException("Debe indicar la cuenta bancaria en la que se recibe el anticipo.");
        }

        Long idEmpresa     = anticipo.getEmpresa().getCodigo();
        Long codigoTitular = anticipo.getTitular().getCodigo();
        Double valor       = anticipo.getValor();
        String nomCliente  = anticipo.getTitular().getNombre();

        // ── DEBE: planCuenta de la cuenta bancaria ──────────────────────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria);
        }
        com.saa.model.cnt.PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        if (cuentaBanco == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada.");
        }

        // ── HABER: cuenta de anticipos del cliente (tipoCuenta=2, rol Cliente) ─
        com.saa.model.cnt.PlanCuenta cuentaAnticipo =
                obtenerCuentaPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Cliente).");
        }

        // ── Construir líneas ───────────────────────────────────────────────────
        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaBanco);
        debe.setNumeroCuenta(cuentaBanco.getCuentaContable());
        debe.setNombreCuenta(cuentaBanco.getNombre());
        debe.setDescripcion("Anticipo recibido de cliente: " + nomCliente
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta());
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
    // generarAsientoAnticipoProveedor
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAnticipoProveedor | idAnticipo=" + anticipo.getId() + " ===");

        if (anticipo.getEmpresa() == null || anticipo.getEmpresa().getCodigo() == null) {
            throw new IncomeException("El anticipo no tiene empresa contable configurada.");
        }
        if (idCuentaBancaria == null) {
            throw new IncomeException("Debe indicar la cuenta bancaria desde la que se paga el anticipo.");
        }

        Long idEmpresa     = anticipo.getEmpresa().getCodigo();
        Long codigoTitular = anticipo.getTitular().getCodigo();
        Double valor       = anticipo.getValor();
        String nomProv     = anticipo.getTitular().getNombre();

        // ── DEBE: cuenta de anticipos del proveedor (tipoCuenta=2, tipoPersona=2) ──
        PlanCuenta cuentaAnticipo = obtenerCuentaProveedorPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Proveedor).");
        }

        // ── HABER: planCuenta de la cuenta bancaria ─────────────────────────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontró la cuenta bancaria con ID: " + idCuentaBancaria);
        }
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        if (cuentaBanco == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada.");
        }

        // ── Construir líneas ───────────────────────────────────────────────────
        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaAnticipo);
        debe.setNumeroCuenta(cuentaAnticipo.getCuentaContable());
        debe.setNombreCuenta(cuentaAnticipo.getNombre());
        debe.setDescripcion("Anticipo a proveedor: " + nomProv
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : ""));
        debe.setValorDebe(valor);
        debe.setValorHaber(0.0);
        lineas.add(debe);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaBanco);
        haber.setNumeroCuenta(cuentaBanco.getCuentaContable());
        haber.setNombreCuenta(cuentaBanco.getNombre());
        haber.setDescripcion("Pago anticipo proveedor: " + nomProv
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta());
        haber.setValorDebe(0.0);
        haber.setValorHaber(valor);
        lineas.add(haber);

        // ── Generar asiento ────────────────────────────────────────────────────
        String obs = "Anticipo proveedor: " + nomProv
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
     * Obtiene la cuenta contable de un proveedor por tipo de cuenta.
     * tipoCuenta: 1=Facturas, 2=Anticipos
     * Filtra por rubroRolPersonaP=2 (rol Proveedor) en PersonaRol.
     */
    private PlanCuenta obtenerCuentaProveedorPorTipo(Long codigoTitular, Long idEmpresa, Long tipoCuenta) {
        System.out.println("  [obtenerCuentaProveedorPorTipo] titular=" + codigoTitular
                + " | empresa=" + idEmpresa + " | tipoCuenta=" + tipoCuenta
                + " | rol=Proveedor(2)");
        try {
            // Busca en PersonaCuentaContable cuya PersonaRol tenga rubroRolPersonaP=2 (Proveedor)
            String sql = "SELECT pcc.planCuenta FROM PersonaCuentaContable pcc "
                    + "JOIN pcc.personaRol pr "
                    + "WHERE pr.titular.codigo = :titular "
                    + "AND pcc.tipoCuenta = :tipo "
                    + "AND pcc.empresa.codigo = :empresa "
                    + "AND pr.rubroRolPersonaP = 2";
            Query q = em.createQuery(sql);
            q.setParameter("titular", codigoTitular);
            q.setParameter("tipo", tipoCuenta);
            q.setParameter("empresa", idEmpresa);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            if (result.isEmpty()) {
                // segundo intento sin filtro de rol (por compatibilidad)
                String sql2 = "SELECT pcc.planCuenta FROM PersonaCuentaContable pcc "
                        + "JOIN pcc.personaRol pr "
                        + "WHERE pr.titular.codigo = :titular "
                        + "AND pcc.tipoCuenta = :tipo "
                        + "AND pcc.empresa.codigo = :empresa";
                Query q2 = em.createQuery(sql2);
                q2.setParameter("titular", codigoTitular);
                q2.setParameter("tipo", tipoCuenta);
                q2.setParameter("empresa", idEmpresa);
                q2.setMaxResults(1);
                result = q2.getResultList();
            }
            if (result.isEmpty()) {
                System.err.println("  [obtenerCuentaProveedorPorTipo] ✗ No encontrado."
                        + " titular=" + codigoTitular + " tipoCuenta=" + tipoCuenta);
                return null;
            }
            PlanCuenta pc = (PlanCuenta) result.get(0);
            System.out.println("  [obtenerCuentaProveedorPorTipo] ✓ Cuenta: "
                    + pc.getCuentaContable() + " - " + pc.getNombre());
            return pc;
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta proveedor tipo " + tipoCuenta
                    + " del titular " + codigoTitular + ": " + e.getMessage());
            return null;
        }
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

        System.out.println("=== generarAsientoNotaCredito | idNC=" + idNotaCredito
                + " | empresa=" + idEmpresa + " ===");

        // 1. Cargar la Nota de Crédito
        com.saa.model.cxc.NotaCredito nc =
                em.find(com.saa.model.cxc.NotaCredito.class, idNotaCredito);
        if (nc == null) {
            throw new IncomeException("No se encontró la Nota de Crédito con ID: " + idNotaCredito);
        }

        // 2. Cargar detalles activos
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxc.DetalleNotaCredito> detalles = em.createQuery(
                "SELECT d FROM DetalleNotaCredito d WHERE d.notaCredito.id = :id AND d.estado = 1")
                .setParameter("id", idNotaCredito)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La Nota de Crédito " + idNotaCredito + " no tiene detalles activos.");
        }

        // 3. Construir líneas del asiento
        // NOTA: La lógica es idéntica a la de factura pero con DEBE y HABER invertidos.
        //   Factura:     DEBE=CxC cliente | HABER=Ingresos/IVA
        //   Nota Crédito:HABER=CxC cliente | DEBE=Ingresos/IVA  (anulación/reducción)
        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── HABER: cuenta CxC del cliente (en factura era DEBE) ──────────────
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                nc.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontró cuenta contable (tipo factura) "
                    + "para el cliente ID: " + nc.getTitular().getCodigo()
                    + " en la empresa: " + idEmpresa);
        }

        DetalleAsiento lineaHaberCliente = new DetalleAsiento();
        lineaHaberCliente.setPlanCuenta(cuentaCliente);
        lineaHaberCliente.setNumeroCuenta(cuentaCliente.getCuentaContable());
        lineaHaberCliente.setNombreCuenta(cuentaCliente.getNombre());
        lineaHaberCliente.setDescripcion("NC cliente: " + nc.getTitular().getNombre());
        lineaHaberCliente.setValorDebe(0.0);
        lineaHaberCliente.setValorHaber(nvl(nc.getTotal()));
        lineas.add(lineaHaberCliente);

        // ── DEBE: una línea por grupo de producto (en factura era HABER) ──────
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
            if (detalle.getProducto() == null) {
                throw new IncomeException("El detalle '" + detalle.getDescripcion()
                        + "' no tiene producto asignado.");
            }
            // Obtener grupo del producto vía JPQL (producto es Long FK)
            @SuppressWarnings("unchecked")
            List<Object[]> grupoRows = em.createQuery(
                    "SELECT p.grupoProducto.codigo, p.grupoProducto.nombre, p.grupoProducto.planCuenta "
                    + "FROM ProductoCobro p WHERE p.id = :id")
                    .setParameter("id", detalle.getProducto())
                    .setMaxResults(1)
                    .getResultList();
            if (grupoRows.isEmpty()) {
                throw new IncomeException("El producto ID " + detalle.getProducto()
                        + " ('" + detalle.getDescripcion() + "') no se encontró o no tiene grupo asignado.");
            }
            Object[] gr = grupoRows.get(0);
            Long idGrupo = (Long) gr[0];
            String nomGrupo = (String) gr[1];
            PlanCuenta pc = (PlanCuenta) gr[2];
            if (pc == null) {
                throw new IncomeException("El grupo de producto '" + nomGrupo
                        + "' no tiene cuenta contable asignada.");
            }
            subtotalPorGrupo.merge(idGrupo, nvl(detalle.getBaseImponible()), Double::sum);
            cuentaPorGrupo.putIfAbsent(idGrupo, pc);
            nombreGrupo.putIfAbsent(idGrupo, nomGrupo);
        }

        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento lineaDebe = new DetalleAsiento();
            lineaDebe.setPlanCuenta(pc);
            lineaDebe.setNumeroCuenta(pc.getCuentaContable());
            lineaDebe.setNombreCuenta(pc.getNombre());
            lineaDebe.setDescripcion("NC Ventas: " + nombreGrupo.get(idGrupo));
            lineaDebe.setValorDebe(subtotalPorGrupo.get(idGrupo));
            lineaDebe.setValorHaber(0.0);
            lineas.add(lineaDebe);
        }

        // ── DEBE: una línea por tipo de IVA (en factura era HABER) ───────────
        // DetalleNotaCredito.porcentajeIVA es el % (0,5,8,15). Mapeamos a código SRI.
        Map<String, Double> ivaParaTipo = new LinkedHashMap<>();
        for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
            if (detalle.getValorIVA() != null && detalle.getValorIVA() > 0
                    && detalle.getPorcentajeIVA() != null) {
                String codigoSRI = mapPorcentajeIVAaCodigo(detalle.getPorcentajeIVA());
                ivaParaTipo.merge(codigoSRI, nvl(detalle.getValorIVA()), Double::sum);
            }
        }

        for (Map.Entry<String, Double> entry : ivaParaTipo.entrySet()) {
            String codigoIVASRI = entry.getKey();
            Double valorIVA     = entry.getValue();

            PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoIVASRI);
            if (cuentaIVA == null) {
                throw new IncomeException(
                        "No se encontró cuenta contable para el IVA con código SRI: " + codigoIVASRI
                        + ". Configure la cuenta en Facturación → Tipos SRI → IVA (categoría 17).");
            }
            DetalleAsiento lineaIVA = new DetalleAsiento();
            lineaIVA.setPlanCuenta(cuentaIVA);
            lineaIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            lineaIVA.setNombreCuenta(cuentaIVA.getNombre());
            lineaIVA.setDescripcion("NC IVA código SRI: " + codigoIVASRI);
            lineaIVA.setValorDebe(valorIVA);
            lineaIVA.setValorHaber(0.0);
            lineas.add(lineaIVA);
        }

        // 4. Generar el asiento con las líneas construidas
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas);
    }

    /** Mapea porcentaje de IVA (Long: 0,5,8,15) al código SRI de TSRI (tabla='17'). */
    private String mapPorcentajeIVAaCodigo(Long porcentaje) {
        if (porcentaje == null) return "0";
        switch (porcentaje.intValue()) {
            case 0:  return "0";   // IVA 0%
            case 5:  return "5";   // IVA 5%
            case 8:  return "8";   // IVA tarifa especial 8%
            case 15: return "4";   // IVA 15% (código SRI = 4)
            default: return String.valueOf(porcentaje);
        }
    }

    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaDebito(
            Long idNotaDebito, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoNotaDebito | idND=" + idNotaDebito
                + " | empresa=" + idEmpresa + " ===");

        // 1. Cargar la Nota de Débito
        com.saa.model.cxc.NotaDebito nd =
                em.find(com.saa.model.cxc.NotaDebito.class, idNotaDebito);
        if (nd == null) {
            throw new IncomeException("No se encontró la Nota de Débito con ID: " + idNotaDebito);
        }

        // 2. La ND no tiene líneas de producto propias: obtener cuentas de ingreso
        //    desde la factura relacionada.
        List<DetalleFactura> detallesFact = obtenerDetallesParaND(nd);

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: cuenta CxC del cliente (igual que Factura) ─────────────────
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                nd.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontró cuenta contable (tipo factura) "
                    + "para el cliente ID: " + nd.getTitular().getCodigo()
                    + " en la empresa: " + idEmpresa);
        }
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaCliente);
        debe.setNumeroCuenta(cuentaCliente.getCuentaContable());
        debe.setNombreCuenta(cuentaCliente.getNombre());
        debe.setDescripcion("ND cliente: " + nd.getTitular().getNombre());
        debe.setValorDebe(nvl(nd.getTotal()));
        debe.setValorHaber(0.0);
        lineas.add(debe);

        // ── HABER: cuentas de ingreso por grupo de producto (factura relacionada) ─
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        if (detallesFact != null && !detallesFact.isEmpty()) {
            // Calcular proporción total de bases imponibles de la factura
            double totalBaseFact = 0.0;
            for (DetalleFactura d : detallesFact) totalBaseFact += nvl(d.getBaseImponible());
            double totalBaseND = nvl(nd.getSubtotal()) + nvl(nd.getSubcero());

            for (DetalleFactura d : detallesFact) {
                if (d.getProducto() == null || d.getProducto().getGrupoProducto() == null) continue;
                Long idGrupo = d.getProducto().getGrupoProducto().getCodigo();
                PlanCuenta pc = d.getProducto().getGrupoProducto().getPlanCuenta();
                if (pc == null) continue;
                // Distribuir el total de la ND proporcionalmente a la base de cada grupo
                double proporcion = totalBaseFact > 0 ? nvl(d.getBaseImponible()) / totalBaseFact : 0.0;
                double valorGrupo = totalBaseND * proporcion;
                subtotalPorGrupo.merge(idGrupo, valorGrupo, Double::sum);
                cuentaPorGrupo.putIfAbsent(idGrupo, pc);
                nombreGrupo.putIfAbsent(idGrupo, d.getProducto().getGrupoProducto().getNombre());
            }
        }

        if (subtotalPorGrupo.isEmpty()) {
            // Sin factura relacionada: usar subcero + subtotal en una sola línea genérica
            // Intentar obtener cuenta del primer grupo del facturador
            throw new IncomeException(
                    "La Nota de Débito no tiene factura relacionada con detalles de producto. "
                    + "Vincule la ND a una factura para generar el asiento contable.");
        }

        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento lineaHaber = new DetalleAsiento();
            lineaHaber.setPlanCuenta(pc);
            lineaHaber.setNumeroCuenta(pc.getCuentaContable());
            lineaHaber.setNombreCuenta(pc.getNombre());
            lineaHaber.setDescripcion("ND Ingresos: " + nombreGrupo.get(idGrupo));
            lineaHaber.setValorDebe(0.0);
            lineaHaber.setValorHaber(subtotalPorGrupo.get(idGrupo));
            lineas.add(lineaHaber);
        }

        // ── HABER: IVA (si aplica) ────────────────────────────────────────────
        if (nd.getvIVA() != null && nd.getvIVA() > 0) {
            // código SRI del IVA: porcentaje pIVA → mapeamos a código SRI
            String codigoIVASRI = mapPorcentajeNDaCodigo(nd.getpIVA());
            PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoIVASRI);
            if (cuentaIVA == null) {
                throw new IncomeException(
                        "No se encontró cuenta contable para el IVA código SRI: " + codigoIVASRI
                        + ". Configure la cuenta en Facturación → Tipos SRI → IVA (categoría 17).");
            }
            DetalleAsiento haberIVA = new DetalleAsiento();
            haberIVA.setPlanCuenta(cuentaIVA);
            haberIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            haberIVA.setNombreCuenta(cuentaIVA.getNombre());
            haberIVA.setDescripcion("ND IVA código SRI: " + codigoIVASRI);
            haberIVA.setValorDebe(0.0);
            haberIVA.setValorHaber(nd.getvIVA());
            lineas.add(haberIVA);
        }

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas);
    }

    /** Mapea el porcentaje de IVA (Double: 0,5,8,15) al código SRI de TSRI. */
    private String mapPorcentajeNDaCodigo(Double porcentaje) {
        if (porcentaje == null) return "0";
        int p = porcentaje.intValue();
        switch (p) {
            case 0:  return "0";
            case 5:  return "5";
            case 8:  return "8";
            case 15: return "4";
            default: return String.valueOf(p);
        }
    }

    /** Carga los detalles de la factura relacionada a una ND (puede ser null). */
    @SuppressWarnings("unchecked")
    private List<DetalleFactura> obtenerDetallesParaND(com.saa.model.cxc.NotaDebito nd) {
        if (nd.getFactura() == null) return null;
        try {
            return em.createQuery(
                    "SELECT d FROM DetalleFactura d WHERE d.factura.id = :id AND d.estado = 1")
                    .setParameter("id", nd.getFactura().getId())
                    .getResultList();
        } catch (Exception e) {
            System.err.println("⚠ No se pudieron cargar detalles de factura para ND: " + e.getMessage());
            return null;
        }
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

    // ---------------------------------------------------------------
    // validarCuentasContablesRetencion
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesRetencion(
            com.saa.model.cxc.Retencion retencion,
            List<com.saa.model.cxc.DetalleRetencion> detalles,
            Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxP del proveedor (tipoCuenta=1, rol proveedor)
        if (retencion.getProveedor() == null) {
            errores.add("La retención no tiene proveedor asignado.");
        } else {
            PlanCuenta cuentaProveedor = obtenerCuentaProveedor(
                    retencion.getProveedor().getCodigo(), idEmpresa);
            if (cuentaProveedor == null) {
                errores.add("El proveedor '" + retencion.getProveedor().getNombre()
                        + "' (ID: " + retencion.getProveedor().getCodigo()
                        + ") no tiene cuenta contable configurada. "
                        + "Configure la cuenta en Tesorería → Persona → Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Proveedor).");
            }
        }

        // 2. Validar cuenta contable de cada código de retención en TSRI
        if (detalles != null) {
            java.util.Set<String> codigosValidados = new java.util.HashSet<>();
            for (com.saa.model.cxc.DetalleRetencion d : detalles) {
                String cod = d.getCodRetencion();
                if (cod != null && !cod.isEmpty() && !codigosValidados.contains(cod)) {
                    codigosValidados.add(cod);
                    PlanCuenta pc = obtenerCuentaPorCodigoTsri(cod);
                    if (pc == null) {
                        errores.add("El código de retención '" + cod
                                + "' no tiene cuenta contable asignada en TSRI. "
                                + "Configure la cuenta en Facturación → Tipos SRI.");
                    }
                }
            }
        }

        return errores;
    }

    // ---------------------------------------------------------------
    // generarAsientoRetencion
    // ---------------------------------------------------------------

    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencion(
            Long idRetencion, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoRetencion | idRetencion=" + idRetencion
                + " | empresa=" + idEmpresa + " ===");

        // 1. Cargar la retención
        com.saa.model.cxc.Retencion retencion =
                em.find(com.saa.model.cxc.Retencion.class, idRetencion);
        if (retencion == null) {
            throw new IncomeException("No se encontró la Retención con ID: " + idRetencion);
        }

        // 2. Cargar detalles activos
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxc.DetalleRetencion> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencion d WHERE d.retencion.id = :id AND d.estado = 1")
                .setParameter("id", idRetencion)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La Retención " + idRetencion + " no tiene detalles activos.");
        }

        // 3. Construir líneas del asiento
        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── HABER: una línea por detalle de retención, cuenta desde TSRI.codRetencion ──
        double totalRetenido = 0.0;
        for (com.saa.model.cxc.DetalleRetencion det : detalles) {
            String codRetencion = det.getCodRetencion();
            PlanCuenta pcReten = obtenerCuentaPorCodigoTsri(codRetencion);
            if (pcReten == null) {
                throw new IncomeException(
                        "No se encontró cuenta contable para el código de retención '"
                        + codRetencion + "' en TSRI. "
                        + "Configure la cuenta en Facturación → Tipos SRI.");
            }
            double valor = nvl(det.getValorReten());
            totalRetenido += valor;

            DetalleAsiento lineaHaber = new DetalleAsiento();
            lineaHaber.setPlanCuenta(pcReten);
            lineaHaber.setNumeroCuenta(pcReten.getCuentaContable());
            lineaHaber.setNombreCuenta(pcReten.getNombre());
            lineaHaber.setDescripcion("Retención código " + codRetencion
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(det.getBaseImponible()))
                    + " | " + nvl2(det.getPorcentajeReten()) + "%");
            lineaHaber.setValorDebe(0.0);
            lineaHaber.setValorHaber(valor);
            lineas.add(lineaHaber);
        }

        // ── DEBE: cuenta CxP del proveedor por el total retenido ──────────────
        if (retencion.getProveedor() == null) {
            throw new IncomeException("La Retención no tiene proveedor asignado.");
        }
        PlanCuenta cuentaProveedor = obtenerCuentaProveedor(
                retencion.getProveedor().getCodigo(), idEmpresa);
        if (cuentaProveedor == null) {
            throw new IncomeException("No se encontró cuenta contable (tipo factura) "
                    + "para el proveedor ID: " + retencion.getProveedor().getCodigo()
                    + " en la empresa: " + idEmpresa);
        }

        DetalleAsiento lineaDebe = new DetalleAsiento();
        lineaDebe.setPlanCuenta(cuentaProveedor);
        lineaDebe.setNumeroCuenta(cuentaProveedor.getCuentaContable());
        lineaDebe.setNombreCuenta(cuentaProveedor.getNombre());
        lineaDebe.setDescripcion("Proveedor: " + retencion.getProveedor().getNombre());
        lineaDebe.setValorDebe(totalRetenido);
        lineaDebe.setValorHaber(0.0);
        // Insertar DEBE al inicio
        lineas.add(0, lineaDebe);

        // 4. Generar asiento
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas);
    }

    /**
     * Obtiene la cuenta contable CxP del proveedor (tipoCuenta=1, rol proveedor).
     * Usa la misma tabla PersonaCuentaContable pero buscando el titular en rol proveedor.
     */
    private PlanCuenta obtenerCuentaProveedor(Long codigoTitular, Long idEmpresa) {
        System.out.println("  [obtenerCuentaProveedor] titular=" + codigoTitular
                + " | empresa=" + idEmpresa);
        try {
            String sql = "SELECT pcc.planCuenta FROM PersonaCuentaContable pcc "
                    + "JOIN pcc.personaRol pr "
                    + "WHERE pr.titular.codigo = :titular "
                    + "AND pcc.tipoCuenta = 1 "
                    + "AND pcc.empresa.codigo = :empresa";
            Query q = em.createQuery(sql);
            q.setParameter("titular", codigoTitular);
            q.setParameter("empresa", idEmpresa);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            if (result.isEmpty()) return null;
            PlanCuenta pc = (PlanCuenta) result.get(0);
            System.out.println("  [obtenerCuentaProveedor] ✓ " + pc.getCuentaContable());
            return pc;
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta del proveedor " + codigoTitular + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la cuenta contable desde TSRI por su campo CODIGO.
     * Se usa para mapear DetalleRetencion.codRetencion → PlanCuenta.
     */
    /**
     * Busca la cuenta contable en TSRI para un código de retención recibida (CXP).
     * El LSRI correcto depende del tipo de impuesto que viene en el XML del SRI:
     *   codImpuesto = "1" (Renta)  → lsri.tabla = '608'
     *   codImpuesto = "2" (IVA)    → lsri.tabla = '20'
     *
     * @param codImpuesto    valor del tag <codigo> del XML ("1" o "2")
     * @param codRetencion   valor del tag <codigoRetencion> del XML (ej. "320", "10")
     */
    private PlanCuenta obtenerCuentaRetencionCompra(String codImpuesto, String codRetencion) {
        String lsriTabla;
        if ("1".equals(codImpuesto)) {
            lsriTabla = "608"; // Retención de Renta
        } else if ("2".equals(codImpuesto)) {
            lsriTabla = "20";  // Retención de IVA
        } else {
            System.err.println("⚠ obtenerCuentaRetencionCompra: codImpuesto desconocido='" + codImpuesto
                    + "' para codRetencion='" + codRetencion + "'.");
            return null;
        }
        try {
            String sql = "SELECT t.planCuenta FROM TsriCompra t "
                    + "WHERE t.lsri.tabla = :lsriTabla AND t.codigo = :codigo AND t.estado = 1";
            Query q = em.createQuery(sql);
            q.setParameter("lsriTabla", lsriTabla);
            q.setParameter("codigo", codRetencion);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            if (result.isEmpty()) {
                System.err.println("⚠ No se encontró cuenta en TSRI: lsri.tabla=" + lsriTabla
                        + " | codRetencion=" + codRetencion);
                return null;
            }
            return (PlanCuenta) result.get(0);
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta TSRI lsri=" + lsriTabla
                    + " codRetencion=" + codRetencion + ": " + e.getMessage());
            return null;
        }
    }

    /** @deprecated Usar {@link #obtenerCuentaRetencionCompra(String, String)} para retenciones CXP. */
    @Deprecated
    private PlanCuenta obtenerCuentaPorCodigoTsri(String codigoTsri) {
        try {
            String sql = "SELECT t.planCuenta FROM Tsri t "
                    + "WHERE t.codigo = :codigo AND t.estado = 1";
            Query q = em.createQuery(sql);
            q.setParameter("codigo", codigoTsri);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            return result.isEmpty() ? null : (PlanCuenta) result.get(0);
        } catch (Exception e) {
            System.err.println("⚠ Error buscando cuenta TSRI codigo=" + codigoTsri + ": " + e.getMessage());
            return null;
        }
    }

    private String nvl2(Double val) {
        if (val == null) return "0";
        if (val == Math.floor(val)) return String.valueOf(val.intValue());
        return String.format(java.util.Locale.US, "%.2f", val);
    }

    // =========================================================================
    // Stub generarAsientoRetencionV2
    // =========================================================================

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
    // CXP — Documentos de Compra (recibidos del proveedor)
    // =========================================================================
    // ESTRUCTURA GENERAL DEL ASIENTO DE COMPRA:
    //   DEBE:  Gasto / Costo por grupo de producto (GrupoProductoPago.planCuenta)
    //          + IVA Crédito Tributario (Tsri/Lsri, tabla='17')
    //   HABER: CxP Proveedor (PersonaCuentaContable tipoCuenta=1, rol proveedor)
    //
    // PREREQ en BD antes de activar:
    //   1. TipoAsiento con codigoAlterno = TipoAsientos.FACTURAS_COMPRA (9) creado en CNT.TPAS
    //   2. GrupoProductoPago.planCuenta configurado para cada grupo
    //   3. PersonaCuentaContable (tipoCuenta=1) configurada para cada proveedor
    //   4. Tsri (tabla='17') con planCuenta configurada para cada tarifa de IVA
    // =========================================================================

    // ---------------------------------------------------------------
    // generarAsientoFacturaCompra
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoFacturaCompra(
            Long idFacturaCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoFacturaCompra | id=" + idFacturaCompra
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.FacturaCompra fc =
                em.find(com.saa.model.cxp.FacturaCompra.class, idFacturaCompra);
        if (fc == null)
            throw new IncomeException("No se encontró FacturaCompra con ID: " + idFacturaCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleFacturaCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleFacturaCompra d WHERE d.factura.id = :id AND d.estado = 1")
                .setParameter("id", idFacturaCompra).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: una línea por grupo de producto (GrupoProductoPago.planCuenta) ──
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        for (com.saa.model.cxp.DetalleFacturaCompra d : detalles) {
            if (d.getProducto() == null)
                throw new IncomeException("El detalle '" + d.getDescripcion() + "' no tiene producto asignado.");

            com.saa.model.cxp.ProductoPago producto = em.find(com.saa.model.cxp.ProductoPago.class, d.getProducto());
            if (producto == null)
                throw new IncomeException("Producto ID " + d.getProducto() + " no encontrado en BD.");

            if (producto.getGrupoProducto() == null)
                throw new IncomeException("Producto ID " + d.getProducto() + " ('" + producto.getNombre()
                        + "') no tiene grupo asignado. Clasifíquelo antes de registrar la factura.");

            com.saa.model.cxp.GrupoProductoPago grupo = producto.getGrupoProducto();
            Long idGrupo = grupo.getCodigo();
            String nomGrupo = grupo.getNombre();
            PlanCuenta pc = grupo.getPlanCuenta();
            if (pc == null)
                throw new IncomeException("GrupoProductoPago '" + nomGrupo + "' no tiene cuenta contable.");
            subtotalPorGrupo.merge(idGrupo, nvl(d.getSubTotal()), Double::sum);
            cuentaPorGrupo.putIfAbsent(idGrupo, pc);
            nombreGrupo.putIfAbsent(idGrupo, nomGrupo);
        }
        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pc); ln.setNumeroCuenta(pc.getCuentaContable());
            ln.setNombreCuenta(pc.getNombre());
            ln.setDescripcion("Gasto compra: " + nombreGrupo.get(idGrupo));
            ln.setValorDebe(subtotalPorGrupo.get(idGrupo)); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // ── DEBE: IVA crédito tributario ──────────────────────────────────────
        // Agrupa por codigoIVASRI del detalle y busca la cuenta en PGS.TSRI
        // donde lsri.tabla = '17' y codigo = codigoIVASRI
        Map<Long, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleFacturaCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getCodigoIVASRI() != null) {
                ivaMap.merge(d.getCodigoIVASRI(), nvl(d.getValorIVA()), Double::sum);
            }
        }
        for (Map.Entry<Long, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(e.getKey());
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta de IVA crédito tributario para código SRI: "
                        + e.getKey() + " (PGS.TSRI lsri.tabla=17). Configure en Compras → Tipos SRI.");
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pcIVA); ln.setNumeroCuenta(pcIVA.getCuentaContable());
            ln.setNombreCuenta(pcIVA.getNombre());
            ln.setDescripcion("IVA crédito tributario código SRI: " + e.getKey());
            ln.setValorDebe(e.getValue()); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // ── HABER: CxP proveedor ──────────────────────────────────────────────
        if (fc.getTitular() == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(fc.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + fc.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada (Tipo 1, Rol Proveedor).");
        // El HABER se calcula como la suma exacta de todas las líneas DEBE ya construidas
        // (subtotales por grupo + IVA) para garantizar que el asiento siempre cuadre
        // independientemente de acumulaciones en punto flotante.
        double totalDebe = lineas.stream().mapToDouble(l -> nvl(l.getValorDebe())).sum();
        double haberRedondeado = Math.round(totalDebe * 100.0) / 100.0;
        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Proveedor: " + fc.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(haberRedondeado);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsientoNotaCreditoCompra  (inverso de FacturaCompra)
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaCreditoCompra(
            Long idNotaCreditoCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoNotaCreditoCompra | id=" + idNotaCreditoCompra
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.NotaCreditoCompra nc =
                em.find(com.saa.model.cxp.NotaCreditoCompra.class, idNotaCreditoCompra);
        if (nc == null)
            throw new IncomeException("No se encontró NotaCreditoCompra con ID: " + idNotaCreditoCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleNotaCreditoCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleNotaCreditoCompra d WHERE d.notaCredito.id = :id AND d.estado = 1")
                .setParameter("id", idNotaCreditoCompra).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("NotaCreditoCompra " + idNotaCreditoCompra + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: CxP Proveedor (reduce lo que le debemos) ───────────────────
        if (nc.getTitular() == null)
            throw new IncomeException("NotaCreditoCompra " + idNotaCreditoCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(nc.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + nc.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaProv); debe.setNumeroCuenta(cuentaProv.getCuentaContable());
        debe.setNombreCuenta(cuentaProv.getNombre());
        debe.setDescripcion("NC Proveedor: " + nc.getTitular().getNombre());
        debe.setValorDebe(nvl(nc.getTotal())); debe.setValorHaber(0.0);
        lineas.add(debe);

        // ── HABER: reverso gasto por grupo (sin grupo en NC compra → usar descripción) ─
        // NC compra no trae GrupoProductoPago en el detalle; se acredita gasto genérico
        double totalBase = 0.0;
        for (com.saa.model.cxp.DetalleNotaCreditoCompra d : detalles)
            totalBase += nvl(d.getSubTotal());
        // Intentar obtener cuenta de gasto del primer grupo activo de la empresa
        PlanCuenta cuentaGasto = obtenerCuentaGastoDefaultCxp(idEmpresa);
        if (cuentaGasto == null)
            throw new IncomeException("No se encontró cuenta de gasto para la NC de compra. "
                    + "Configure un GrupoProductoPago con cuenta contable y tipo GASTOS GENERALES.");
        if (totalBase > 0) {
            DetalleAsiento haberGasto = new DetalleAsiento();
            haberGasto.setPlanCuenta(cuentaGasto); haberGasto.setNumeroCuenta(cuentaGasto.getCuentaContable());
            haberGasto.setNombreCuenta(cuentaGasto.getNombre());
            haberGasto.setDescripcion("NC Compra reverso gasto");
            haberGasto.setValorDebe(0.0); haberGasto.setValorHaber(totalBase);
            lineas.add(haberGasto);
        }

        // ── HABER: IVA crédito tributario reverso ─────────────────────────────
        Map<String, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleNotaCreditoCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getPorcentajeIVA() != null) {
                String codSRI = mapPorcentajeIVAaCodigo(d.getPorcentajeIVA());
                ivaMap.merge(codSRI, nvl(d.getValorIVA()), Double::sum);
            }
        }
        for (Map.Entry<String, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxp(e.getKey());
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta IVA crédito tributario para código SRI: " + e.getKey());
            DetalleAsiento haberIVA = new DetalleAsiento();
            haberIVA.setPlanCuenta(pcIVA); haberIVA.setNumeroCuenta(pcIVA.getCuentaContable());
            haberIVA.setNombreCuenta(pcIVA.getNombre());
            haberIVA.setDescripcion("NC IVA crédito tributario reverso código SRI: " + e.getKey());
            haberIVA.setValorDebe(0.0); haberIVA.setValorHaber(e.getValue());
            lineas.add(haberIVA);
        }

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsientoNotaDebitoCompra
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoNotaDebitoCompra(
            Long idNotaDebitoCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoNotaDebitoCompra | id=" + idNotaDebitoCompra
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.NotaDebitoCompra nd =
                em.find(com.saa.model.cxp.NotaDebitoCompra.class, idNotaDebitoCompra);
        if (nd == null)
            throw new IncomeException("No se encontró NotaDebitoCompra con ID: " + idNotaDebitoCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleNotaDebitoCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleNotaDebitoCompra d WHERE d.notaDebito.id = :id AND d.estado = 1")
                .setParameter("id", idNotaDebitoCompra).getResultList();

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: gasto adicional (cuenta default de gastos CXP) ─────────────
        double totalND = nvl(nd.getTotal());
        PlanCuenta cuentaGasto = obtenerCuentaGastoDefaultCxp(idEmpresa);
        if (cuentaGasto == null)
            throw new IncomeException("No se encontró cuenta de gasto para la ND de compra. "
                    + "Configure un GrupoProductoPago con cuenta contable y tipo GASTOS GENERALES.");
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaGasto); debe.setNumeroCuenta(cuentaGasto.getCuentaContable());
        debe.setNombreCuenta(cuentaGasto.getNombre());
        String motivoND = detalles != null && !detalles.isEmpty()
                ? detalles.get(0).getDescripcion() : "Nota de débito compra";
        debe.setDescripcion("ND Compra: " + motivoND);
        debe.setValorDebe(totalND); debe.setValorHaber(0.0);
        lineas.add(debe);

        // ── HABER: CxP Proveedor ──────────────────────────────────────────────
        if (nd.getTitular() == null)
            throw new IncomeException("NotaDebitoCompra " + idNotaDebitoCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(nd.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + nd.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("ND CxP Proveedor: " + nd.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(totalND);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsientoLiquidacionCompraCompra  (misma lógica que FacturaCompra)
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoLiquidacionCompraCompra(
            Long idLiquidacion, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoLiquidacionCompraCompra | id=" + idLiquidacion
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.LiquidacionCompraCompra lq =
                em.find(com.saa.model.cxp.LiquidacionCompraCompra.class, idLiquidacion);
        if (lq == null)
            throw new IncomeException("No se encontró LiquidacionCompraCompra con ID: " + idLiquidacion);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleLiquidacionCompraCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleLiquidacionCompraCompra d WHERE d.liquidacion.id = :id AND d.estado = 1")
                .setParameter("id", idLiquidacion).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("LiquidacionCompraCompra " + idLiquidacion + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: gasto por descripción (Liquidación no tiene GrupoProductoPago) ─
        // Agrupa por descripción única y usa cuenta de gasto default
        double totalBase = 0.0;
        for (com.saa.model.cxp.DetalleLiquidacionCompraCompra d : detalles)
            totalBase += nvl(d.getSubTotal());
        PlanCuenta cuentaGasto = obtenerCuentaGastoDefaultCxp(idEmpresa);
        if (cuentaGasto == null)
            throw new IncomeException("No se encontró cuenta de gasto para la Liquidación de Compra. "
                    + "Configure un GrupoProductoPago con cuenta contable y tipo GASTOS GENERALES.");
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaGasto); debe.setNumeroCuenta(cuentaGasto.getCuentaContable());
        debe.setNombreCuenta(cuentaGasto.getNombre());
        debe.setDescripcion("Gasto liquidación compra: " + lq.getNumero());
        debe.setValorDebe(totalBase); debe.setValorHaber(0.0);
        lineas.add(debe);

        // ── DEBE: IVA crédito tributario ──────────────────────────────────────
        Map<String, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleLiquidacionCompraCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getPorcentajeIVA() != null) {
                String codSRI = mapPorcentajeIVAaCodigo(d.getPorcentajeIVA());
                ivaMap.merge(codSRI, nvl(d.getValorIVA()), Double::sum);
            }
        }
        for (Map.Entry<String, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxp(e.getKey());
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta IVA crédito tributario para código SRI: " + e.getKey());
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pcIVA); ln.setNumeroCuenta(pcIVA.getCuentaContable());
            ln.setNombreCuenta(pcIVA.getNombre());
            ln.setDescripcion("IVA crédito tributario liquidación compra código SRI: " + e.getKey());
            ln.setValorDebe(e.getValue()); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // ── HABER: CxP Proveedor / Prestador ─────────────────────────────────
        if (lq.getTitular() == null)
            throw new IncomeException("LiquidacionCompraCompra " + idLiquidacion + " no tiene proveedor/titular.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(lq.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El prestador '" + lq.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Prestador: " + lq.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(nvl(lq.getTotal()));
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsientoRetencionCompra
    // Retención recibida del proveedor: reduce lo que nos deben / aumenta CxP
    //   DEBE:  CxP Proveedor (monto retenido disminuye la deuda)
    //   HABER: Cuenta de retención recibida por código SRI
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionCompra(
            Long idRetencionCompra, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoRetencionCompra | id=" + idRetencionCompra
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.RetencionCompra rc =
                em.find(com.saa.model.cxp.RetencionCompra.class, idRetencionCompra);
        if (rc == null)
            throw new IncomeException("No se encontró RetencionCompra con ID: " + idRetencionCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleRetencionCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencionCompra d WHERE d.retencion.id = :id AND d.estado = 1")
                .setParameter("id", idRetencionCompra).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("RetencionCompra " + idRetencionCompra + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalRetenido = 0.0;

        // ── HABER: una línea por código de retención (cuenta desde TSRI) ──────
        for (com.saa.model.cxp.DetalleRetencionCompra d : detalles) {
            String codImpuesto = d.getCodImpuesto();
            String codReten = d.getCodRetencion();
            PlanCuenta pcReten = obtenerCuentaRetencionCompra(codImpuesto, codReten);
            if (pcReten == null)
                throw new IncomeException("No hay cuenta contable para retención "
                        + "(codImpuesto='" + codImpuesto + "', codRetencion='" + codReten + "') en TSRI. "
                        + "Configure en Compras → Tipos SRI.");
            double valor = nvl(d.getValorReten());
            totalRetenido += valor;
            DetalleAsiento haberReten = new DetalleAsiento();
            haberReten.setPlanCuenta(pcReten); haberReten.setNumeroCuenta(pcReten.getCuentaContable());
            haberReten.setNombreCuenta(pcReten.getNombre());
            haberReten.setDescripcion("Retención recibida código " + codReten
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(d.getBaseImponible()))
                    + " | " + nvl2(d.getPorcentajeReten()) + "%");
            haberReten.setValorDebe(valor); haberReten.setValorHaber(0.0);
            lineas.add(haberReten);
        }

        // ── HABER: CxP Cliente (cuenta del proveedor/cliente) ────────────────
        if (rc.getProveedor() == null)
            throw new IncomeException("RetencionCompra " + idRetencionCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(rc.getProveedor().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + rc.getProveedor().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaProv); debe.setNumeroCuenta(cuentaProv.getCuentaContable());
        debe.setNombreCuenta(cuentaProv.getNombre());
        debe.setDescripcion("CxP Cliente retención: " + rc.getProveedor().getNombre());
        debe.setValorDebe(0.0); debe.setValorHaber(totalRetenido);
        lineas.add(0, debe);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // generarAsientoRetencionCompraV2  (misma lógica V1, diferente entidad)
    // ---------------------------------------------------------------
    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionCompraV2(
            Long idRetencionCompraV2, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoRetencionCompraV2 | id=" + idRetencionCompraV2
                + " | empresa=" + idEmpresa + " ===");

        com.saa.model.cxp.RetencionCompraV2 rc =
                em.find(com.saa.model.cxp.RetencionCompraV2.class, idRetencionCompraV2);
        if (rc == null)
            throw new IncomeException("No se encontró RetencionCompraV2 con ID: " + idRetencionCompraV2);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleRetencionCompraV2> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencionCompraV2 d WHERE d.retencion.id = :id AND d.estado = 1")
                .setParameter("id", idRetencionCompraV2).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("RetencionCompraV2 " + idRetencionCompraV2 + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalRetenido = 0.0;

        // ── HABER: por código de retención desde TSRI ─────────────────────────
        for (com.saa.model.cxp.DetalleRetencionCompraV2 d : detalles) {
            String codImpuesto = d.getCodImpuesto();
            String codReten = d.getCodRetencion();
            PlanCuenta pcReten = obtenerCuentaRetencionCompra(codImpuesto, codReten);
            if (pcReten == null)
                throw new IncomeException("No hay cuenta contable para retención V2 "
                        + "(codImpuesto='" + codImpuesto + "', codRetencion='" + codReten + "') en TSRI.");
            double valor = nvl(d.getValorReten());
            totalRetenido += valor;
            DetalleAsiento haberReten = new DetalleAsiento();
            haberReten.setPlanCuenta(pcReten); haberReten.setNumeroCuenta(pcReten.getCuentaContable());
            haberReten.setNombreCuenta(pcReten.getNombre());
            haberReten.setDescripcion("Retención V2 recibida código " + codReten
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(d.getBaseImponible()))
                    + " | " + nvl2(d.getPorcentajeReten()) + "%");
            haberReten.setValorDebe(valor); haberReten.setValorHaber(0.0);
            lineas.add(haberReten);
        }

        // ── HABER: CxP Cliente ────────────────────────────────────────────────
        if (rc.getProveedor() == null)
            throw new IncomeException("RetencionCompraV2 " + idRetencionCompraV2 + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(rc.getProveedor().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + rc.getProveedor().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaProv); debe.setNumeroCuenta(cuentaProv.getCuentaContable());
        debe.setNombreCuenta(cuentaProv.getNombre());
        debe.setDescripcion("CxP Cliente retención V2: " + rc.getProveedor().getNombre());
        debe.setValorDebe(0.0); debe.setValorHaber(totalRetenido);
        lineas.add(0, debe);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas);
    }

    // ---------------------------------------------------------------
    // Helpers privados CXP
    // ---------------------------------------------------------------

    /**
     * Obtiene la cuenta contable de IVA crédito tributario (compras) desde PGS.TSRI
     * donde lsri.tabla = '17' y codigo = codigoIVASRI (código que viene directamente de la factura SRI).
     */
    private PlanCuenta obtenerCuentaIVACxpPorCodigo(Long codigoIVASRI) {
        try {
            @SuppressWarnings("unchecked")
            List<PlanCuenta> r = em.createQuery(
                    "SELECT t.planCuenta FROM TsriCompra t "
                    + "WHERE t.lsri.tabla = '17' "
                    + "AND t.codigo = :cod "
                    + "AND t.estado = 1 "
                    + "AND t.planCuenta IS NOT NULL")
                    .setParameter("cod", String.valueOf(codigoIVASRI))
                    .setMaxResults(1).getResultList();
            if (!r.isEmpty() && r.get(0) != null) return r.get(0);
            System.err.println("⚠ No se encontró cuenta IVA en PGS.TSRI para codigoIVASRI=" + codigoIVASRI + " (lsri.tabla=17)");
            return null;
        } catch (Exception e) {
            System.err.println("⚠ obtenerCuentaIVACxpPorCodigo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene la cuenta de IVA crédito tributario (compras) desde LSRI tabla='17'.
     * Usa el mismo método que CXC pero busca en Lsri de compras (com.saa.model.cxp.Lsri).
     * Si no encuentra en CXP, cae a la cuenta CXC como fallback.
     */
    private PlanCuenta obtenerCuentaIVACxp(String codigoSRI) {
        // Intentar primero en LSRI de compras (PGS.LSRC)
        try {
            List<?> r = em.createQuery(
                    "SELECT t.planCuenta FROM TsriCompra t WHERE t.codigo = :cod AND t.estado = 1")
                    .setParameter("cod", codigoSRI).setMaxResults(1).getResultList();
            if (!r.isEmpty() && r.get(0) != null) return (PlanCuenta) r.get(0);
        } catch (Exception e) { /* fallback */ }
        // Fallback: usar la misma cuenta de IVA de CXC
        return obtenerCuentaIVA(codigoSRI);
    }

    /**
     * Obtiene la cuenta de gasto por defecto para CXP.
     * Busca el GrupoProductoPago con tipo GASTOS_GENERALES (o el primero con cuenta configurada).
     */
    private PlanCuenta obtenerCuentaGastoDefaultCxp(Long idEmpresa) {
        try {
            List<?> r = em.createQuery(
                    "SELECT g.planCuenta FROM GrupoProductoPago g "
                    + "WHERE g.empresa.codigo = :emp AND g.planCuenta IS NOT NULL AND g.estado = 1 "
                    + "ORDER BY g.codigo ASC")
                    .setParameter("emp", idEmpresa).setMaxResults(1).getResultList();
            return r.isEmpty() ? null : (PlanCuenta) r.get(0);
        } catch (Exception e) {
            System.err.println("⚠ obtenerCuentaGastoDefaultCxp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene o crea el grupo "POR CLASIFICAR" de la empresa para productos sin grupo asignado.
     */
    @SuppressWarnings("unused")
	private com.saa.model.cxp.GrupoProductoPago obtenerOCrearGrupoPorClasificar(Long idEmpresa) {
        try {
            @SuppressWarnings("unchecked")
            List<com.saa.model.cxp.GrupoProductoPago> lista = em.createQuery(
                    "SELECT g FROM GrupoProductoPago g "
                    + "WHERE g.rubroTipoGrupoH = :tipo AND g.empresa.codigo = :idEmpresa")
                    .setParameter("tipo", (long) com.saa.rubros.TipoGrupoProductos.POR_CLASIFICAR)
                    .setParameter("idEmpresa", idEmpresa)
                    .setMaxResults(1).getResultList();
            if (!lista.isEmpty()) return lista.get(0);

            // No existe → crear
            com.saa.model.cxp.GrupoProductoPago grupo = new com.saa.model.cxp.GrupoProductoPago();
            grupo.setNombre("POR CLASIFICAR");
            grupo.setRubroTipoGrupoH((long) com.saa.rubros.TipoGrupoProductos.POR_CLASIFICAR);
            grupo.setEmpresa(em.find(com.saa.model.scp.Empresa.class, idEmpresa));
            grupo.setEstado(1L);
            em.persist(grupo);
            em.flush();
            return grupo;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo obtener/crear grupo POR CLASIFICAR: " + e.getMessage(), e);
        }
    }
}

