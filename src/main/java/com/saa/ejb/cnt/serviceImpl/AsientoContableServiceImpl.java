package com.saa.ejb.cnt.serviceImpl;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import com.saa.model.scp.DetalleRubro;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.Titular;
import com.saa.rubros.CntParametroAsientoAutomatico;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.ModuloOrigenAsiento;
import com.saa.rubros.ModuloSistema;
import com.saa.rubros.RolPersona;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoAsientoAutomatico;
import com.saa.rubros.TipoAsientos;
import com.saa.rubros.TipoMoneda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * ImplementaciÃ³n del servicio genÃ©rico de generaciÃ³n de asientos contables.
 *
 * Para la factura de venta el asiento queda asÃ­:
 *
 *   DEBE:
 *     Cuenta CxC del cliente (PersonaCuentaContable, tipoCuenta=1, tipoPersona=1)
 *       â†’ valor = total de la factura
 *
 *   HABER:
 *     Una lÃ­nea por cada grupo de producto (consolidado)
 *       â†’ cuenta = GrupoProductoCobro.planCuenta
 *       â†’ valor  = suma de baseImponible de los detalles de ese grupo
 *     Una lÃ­nea por cada tipo de IVA con valor > 0
 *       â†’ cuenta = Tsri.planCuenta  (lsri.tabla='17', tsri.codigo = codigoIVASRI del detalle)
 *       â†’ valor  = suma de valorIVA de ese tipo
 *
 *   DEBE total = HABER total  (total factura = subtotales + impuestos)
 */
@Stateless
public class AsientoContableServiceImpl implements AsientoContableService {

    @EJB
    private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private AsientoService asientoService;

    @EJB
    private DetalleAsientoService detalleAsientoService;

    @EJB
    private TipoAsientoService tipoAsientoService;

    @EJB
    private com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService personaCuentaContableDaoService;

    @EJB
    private com.saa.ejb.cnt.service.PlantillaService plantillaService;

    @EJB
    private com.saa.ejb.cnt.dao.DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private com.saa.ejb.rhh.dao.ConfiguracionNominaDaoService configuracionNominaDaoService;

    /** Rubro CNT_PARAMETROS_ASIENTOS_AUTOMATICOS: vigencia por empresa de la regla IN-A / EG-A / DI-A. */
    @EJB
    private com.saa.basico.ejb.DetalleRubroDaoService detalleRubroDaoService;

    // ---------------------------------------------------------------
    // Cuadre contra importeTotal — docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md
    // ---------------------------------------------------------------

    // P5 (PLAN-CUENTAS-A-PLANTILLA-2026-11-01.md): la cuenta de ajuste por diferencia de redondeo del SRI ya no es
    // un literal. Sale de la línea CxpLineaAsiento.REDONDEO_SRI de la plantilla PlantillasCxp.COMPRAS de la empresa
    // (ver obtenerCuentaDiferenciaRedondeoSri).

    /**
     * Tolerancia, en dólares, para la diferencia entre {@code importeTotal} (cabecera del XML)
     * y la suma de las líneas DEBE de un asiento de compra. Por debajo de {@link #TOLERANCIA_MINIMA_REDONDEO}
     * no se agrega línea de ajuste; entre esa y esta, se agrega a la cuenta de la plantilla de compras (CxpLineaAsiento.REDONDEO_SRI);
     * por encima, se aborta con {@code IncomeException} en vez de mandar la diferencia en silencio a la
     * cuenta de ajuste — puede ser ICE o propina mal clasificado, no redondeo.
     * <p>
     * Revisable con datos: {@code cxp/sql/lap1-04-diagnostico-descuadre-centavos-fctc.sql} mide la
     * distribución real de las diferencias (DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §4).
     */
    private static final double TOLERANCIA_MAXIMA_REDONDEO_SRI = 0.50;

    /** Diferencias por debajo de esto no generan línea de ajuste: no hay diferencia real. */
    private static final double TOLERANCIA_MINIMA_REDONDEO = 0.01;

    // ---------------------------------------------------------------
    // validarCuentasContables
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContables(Titular titular,
            List<DetalleFactura> detalles, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxC del cliente — ESTRICTO, sin el fallback "sin
        // filtro de rol" de PersonaCuentaContableDaoServiceImpl
        // .selectByTitularRolTipoCuenta (ver existeCuentaConRolEstricto):
        // medido contra la base, 61 de 87 titulares con cuenta sólo la
        // tienen bajo rol Proveedor, así que facturar a uno de esos clientes
        // tomaba en silencio su cuenta de proveedor.
        if (titular == null) {
            errores.add("No se especificÃ³ el titular (cliente) de la factura.");
        } else if (!existeCuentaConRolEstricto(titular.getCodigo(), idEmpresa, 1L, RolPersona.CLIENTE)) {
            errores.add("El titular " + titular.getCodigo()
                    + " no tiene cuenta contable bajo el rol 1 (Cliente); parametrícela antes de emitir.");
        }

        // 2. Validar cuentas de grupos de producto e IVA por cada detalle
        if (detalles != null) {
            // Grupos ya validados (evitar mensajes duplicados)
            java.util.Set<Long> gruposValidados = new java.util.HashSet<>();
            java.util.Set<Long> ivaValidados    = new java.util.HashSet<>();

            for (DetalleFactura detalle : detalles) {
                String desc = "'" + (detalle.getDescripcion() != null
                        ? detalle.getDescripcion() : "sin descripciÃ³n") + "'";

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
                                    + "Configure la cuenta en FacturaciÃ³n â†’ Grupos de Producto.");
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
                                    + "' (cÃ³digo SRI: " + codigoIVA
                                    + ") no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
                        }
                    }
                }
            }
        }

        return errores;
    }

    // ---------------------------------------------------------------
    // validarCuentasContablesNC (Nota de CrÃ©dito)
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesNC(Titular titular,
            List<com.saa.model.cxc.DetalleNotaCredito> detalles, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxC del cliente
        if (titular == null) {
            errores.add("No se especificÃ³ el titular (cliente) de la nota de crÃ©dito.");
        } else {
            PlanCuenta cuentaCliente = obtenerCuentaCliente(titular.getCodigo(), idEmpresa);
            if (cuentaCliente == null) {
                errores.add("El cliente '" + titular.getNombre()
                        + "' (ID: " + titular.getCodigo()
                        + ") no tiene cuenta contable de facturas configurada. "
                        + "Configure la cuenta en TesorerÃ­a â†’ Persona â†’ Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Cliente).");
            }
        }

        // 2. Validar cuentas de grupos de producto e IVA por cada detalle
        if (detalles != null) {
            java.util.Set<Long> productosValidados = new java.util.HashSet<>();
            java.util.Set<Long> ivaValidados       = new java.util.HashSet<>();

            for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
                String desc = "'" + (detalle.getDescripcion() != null
                        ? detalle.getDescripcion() : "sin descripciÃ³n") + "'";

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
                                        + ") no se encontrÃ³ o no tiene grupo asignado.");
                            } else {
                                PlanCuenta pc = (PlanCuenta) grupoRows.get(0)[2];
                                String nomGrupo = (String) grupoRows.get(0)[1];
                                if (pc == null) {
                                    errores.add("El grupo de producto '" + nomGrupo
                                            + "' no tiene cuenta contable asignada. "
                                            + "Configure la cuenta en FacturaciÃ³n â†’ Grupos de Producto.");
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
                            errores.add("El IVA al " + porc + "% (cÃ³digo SRI: " + codigoSRI
                                    + ") no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
                        }
                    }
                }
            }
        }

        return errores;
    }

    // ---------------------------------------------------------------
    // validarCuentasContablesND (Nota de DÃ©bito)
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesND(com.saa.model.cxc.NotaDebito notaDebito, Long idEmpresa) {

        List<String> errores = new ArrayList<>();

        if (notaDebito == null) {
            errores.add("No se proporcionÃ³ la nota de dÃ©bito.");
            return errores;
        }

        // 1. Validar cuenta CxC del cliente
        if (notaDebito.getTitular() == null) {
            errores.add("La nota de dÃ©bito no tiene titular (cliente) asignado.");
        } else {
            PlanCuenta cuentaCliente = obtenerCuentaCliente(
                    notaDebito.getTitular().getCodigo(), idEmpresa);
            if (cuentaCliente == null) {
                errores.add("El cliente '" + notaDebito.getTitular().getNombre()
                        + "' (ID: " + notaDebito.getTitular().getCodigo()
                        + ") no tiene cuenta contable de facturas configurada. "
                        + "Configure la cuenta en TesorerÃ­a â†’ Persona â†’ Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Cliente).");
            }
        }

        // 2. Validar cuentas de ingreso desde la factura relacionada
        if (notaDebito.getFactura() == null) {
            errores.add("La nota de dÃ©bito no tiene factura relacionada. "
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
                                    + "Configure la cuenta en FacturaciÃ³n â†’ Grupos de Producto.");
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
                errores.add("El IVA al " + notaDebito.getpIVA().intValue() + "% (cÃ³digo SRI: " + codigoSRI
                        + ") no tiene cuenta contable asignada. "
                        + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
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
            throw new IncomeException("No se encontrÃ³ la factura con ID: " + idFactura);
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

        // 3. Construir lÃ­neas del asiento
        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ DEBE: cuenta CxC del cliente â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                factura.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontrÃ³ cuenta contable (tipo factura) "
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

        // â”€â”€ HABER: una lÃ­nea por grupo de producto (consolidado) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ HABER: una lÃ­nea por tipo de IVA con valor > 0 â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Agrupar por codigoIVASRI (como String = campo CODIGO de TSRI) â†’ sumar valorIVA
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
                        "No se encontrÃ³ cuenta contable para el IVA con cÃ³digo SRI: "
                        + codigoIVASRI
                        + ". Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
            }
            DetalleAsiento lineaIVA = new DetalleAsiento();
            lineaIVA.setPlanCuenta(cuentaIVA);
            lineaIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            lineaIVA.setNombreCuenta(cuentaIVA.getNombre());
            lineaIVA.setDescripcion("IVA cÃ³digo SRI: " + codigoIVASRI);
            lineaIVA.setValorDebe(0.0);
            lineaIVA.setValorHaber(valorIVA);
            lineas.add(lineaIVA);
        }

        // 4. Generar el asiento con las lÃ­neas construidas
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    // ---------------------------------------------------------------
    // generarAsiento  (mÃ©todo genÃ©rico de bajo nivel)
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas) throws Throwable {

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR));
    }

    @Override
    public Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas, Long moduloSistema) throws Throwable {

        // Llamadores directos (crd, rhh, cxp PagoProgramado): el modulo origen se deduce, sin tocar su codigo.
        // docs/logica-negocio/cnt/DISENO-TIPOS-ASIENTO-AUTOMATICOS.md §6.
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones, usuario, lineas,
                moduloSistema, moduloOrigenDeLlamadorDirecto(codigoAltTipoAsiento, moduloSistema));
    }

    /**
     * Modulo origen de un asiento cuyo llamador usa generarAsiento directo: alterno 1 (CREDITOS) es CRE y 6
     * (RECURSOS_HUMANOS) es RRH; si no, sale del moduloSistema que pasa (TESORERIA, CXP, RRHH, CXC, CNT).
     */
    private String moduloOrigenDeLlamadorDirecto(int codigoAltTipoAsiento, Long moduloSistema) {
        if (codigoAltTipoAsiento == TipoAsientos.CREDITOS) {
            return ModuloOrigenAsiento.CRE;
        }
        if (codigoAltTipoAsiento == TipoAsientos.RECURSOS_HUMANOS) {
            return ModuloOrigenAsiento.RRH;
        }
        int ms = (moduloSistema != null) ? moduloSistema.intValue() : ModuloSistema.CUENTAS_POR_COBRAR;
        switch (ms) {
            case ModuloSistema.TESORERIA:        return ModuloOrigenAsiento.TSR;
            case ModuloSistema.CUENTAS_POR_PAGAR: return ModuloOrigenAsiento.CXP;
            case ModuloSistema.RECURSOS_HUMANOS: return ModuloOrigenAsiento.RRH;
            case ModuloSistema.CONTABILIDAD:     return ModuloOrigenAsiento.CNT;
            default:                             return ModuloOrigenAsiento.CXC;
        }
    }

    /**
     * Modulo origen del asiento de una liquidacion de compra (LQCC): CXP si el documento nacio en la bandeja de
     * documentos (tiene un DocumentoCxp que lo apunta) y CXC si lo emitio cxc (LiquidacionCompraServiceImpl crea
     * la LQCC y su asiento antes de enlazarla). Los dos llaman al mismo generador, y la tabla del diseno (§6)
     * da CXP a la primera y CXC a la segunda.
     */
    private String moduloOrigenLiquidacionCompra(Long idLiquidacionCompraCompra) {
        Long enBandeja = (Long) em.createQuery(
                "select count(d) from DocumentoCxp d where d.tipoTablaDestino = 'LIQUIDACION_COMPRA_COMPRA' "
                + "and d.idDocumentoBD = :id")
                .setParameter("id", idLiquidacionCompraCompra)
                .getSingleResult();
        return (enBandeja != null && enBandeja > 0) ? ModuloOrigenAsiento.CXP : ModuloOrigenAsiento.CXC;
    }

    /**
     * Tipo de asiento automatico segun las lineas (desde la vigencia de la empresa): lineas cuya cuenta contable es la de una
     * cuenta bancaria propia de un banco real; neto (debe - haber) de esas lineas &gt; 0 es IN-A, &lt; 0 es EG-A,
     * 0 con lineas de banco a los dos lados es EG-A, y sin lineas de banco real es DI-A. Una sola consulta con
     * los planes de cuenta de los bancos reales de la empresa (sin N+1).
     */
    private int clasificarTipoAutomatico(Long idEmpresa, List<DetalleAsiento> lineas) {
        @SuppressWarnings("unchecked")
        List<Long> planesBancoReal = em.createQuery(
                "select distinct c.planCuenta.codigo from CuentaBancaria c "
                + "where c.banco.empresa.codigo = :empresa and c.banco.bancoReal = 1 and c.planCuenta is not null")
                .setParameter("empresa", idEmpresa)
                .getResultList();
        java.util.Set<Long> reales = new java.util.HashSet<>(planesBancoReal);

        boolean hayBanco = false;
        boolean ladoDebe = false;
        boolean ladoHaber = false;
        double neto = 0.0;
        for (DetalleAsiento linea : lineas) {
            if (linea.getPlanCuenta() == null || linea.getPlanCuenta().getCodigo() == null
                    || !reales.contains(linea.getPlanCuenta().getCodigo())) {
                continue;
            }
            double debe = (linea.getValorDebe() != null) ? linea.getValorDebe() : 0.0;
            double haber = (linea.getValorHaber() != null) ? linea.getValorHaber() : 0.0;
            hayBanco = true;
            neto += debe - haber;
            if (debe > 0.005) {
                ladoDebe = true;
            }
            if (haber > 0.005) {
                ladoHaber = true;
            }
        }
        if (!hayBanco) {
            return TipoAsientoAutomatico.DI_A;
        }
        if (neto > 0.005) {
            return TipoAsientoAutomatico.IN_A;
        }
        if (neto < -0.005) {
            return TipoAsientoAutomatico.EG_A;
        }
        // Neto 0: transferencia entre cuentas propias (banco a los dos lados) es EG-A; lineas de banco en cero
        // sin los dos lados no mueven dinero: DI-A.
        return (ladoDebe && ladoHaber) ? TipoAsientoAutomatico.EG_A : TipoAsientoAutomatico.DI_A;
    }

    @Override
    public Asiento generarAsiento(Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas, Long moduloSistema, String moduloOrigen) throws Throwable {

        System.out.println("=== generarAsiento | empresa=" + idEmpresa
                + " | tipoAlt=" + codigoAltTipoAsiento + " | modulo=" + moduloSistema
                + " | moduloOrigen=" + moduloOrigen + " ===");

        // Desde la vigencia de la empresa (rubro CNT_PARAMETROS_ASIENTOS_AUTOMATICOS; sin configurar, 2026-11-01)
        // el tipo lo decide la regla IN-A / EG-A / DI-A (reemplaza al alterno recibido) y el asiento lleva modulo
        // origen con su propio consecutivo mensual. Antes de esa fecha, todo igual que siempre. La vigencia se
        // compara con la FECHA DEL ASIENTO, nunca con la de hoy: el re-proceso jun-2025 -> nov-2026 del cuadre con
        // la Superintendencia (D-006) la fija en 2025-06-01 y regenera esos meses con esta numeracion.
        // La regla aplica a TODOS los modulos, tambien Creditos (CRE) y Nomina (RRH): decision del lider del cuadre
        // del 2026-10-28 (D-006). No hay modulos con tipo propio.
        LocalDate vigencia = fechaVigenciaTiposAutomaticos(idEmpresa);
        boolean automatico = fechaAsiento != null && !fechaAsiento.isBefore(vigencia);
        if (automatico) {
            System.out.println("generarAsiento: fecha " + fechaAsiento + " >= vigencia " + vigencia
                    + " -> regla de tipos automaticos (modulo " + moduloOrigen + ")");
        }

        // 1. Obtener TipoAsiento por codigoAlterno y sistema=1
        TipoAsiento tipoAsiento;
        if (automatico) {
            int alternoAutomatico = clasificarTipoAutomatico(idEmpresa, lineas);
            System.out.println("generarAsiento: tipo automatico segun las lineas = " + alternoAutomatico
                    + " (recibido " + codigoAltTipoAsiento + ")");
            tipoAsiento = tipoAsientoService.tipoAutomatico(alternoAutomatico, idEmpresa);
        } else {
            Long idTipoAsiento = tipoAsientoService.codigoByAlterno(codigoAltTipoAsiento, idEmpresa);
            if (idTipoAsiento == null || idTipoAsiento == 0L) {
                throw new IncomeException(
                        "No existe TipoAsiento con codigoAlterno=" + codigoAltTipoAsiento
                        + " y sistema=1 en la empresa " + idEmpresa
                        + ". Cree el tipo de asiento en Contabilidad.");
            }
            tipoAsiento = tipoAsientoService.selectById(idTipoAsiento);
        }

        return grabarAsiento(idEmpresa, tipoAsiento, fechaAsiento, observaciones, usuario, lineas, moduloSistema,
                automatico, moduloOrigen);
    }

    @Override
    public LocalDate fechaVigenciaTiposAutomaticos(Long idEmpresa) {
        // Una sola consulta por lista (getResultList): sin fila no hay NoResultException, que al cruzar el limite
        // del EJB del DAO marcaria la transaccion del asiento para rollback. Solo detalles activos.
        List<DetalleRubro> detalles;
        try {
            detalles = detalleRubroDaoService.selectByCodigoAlternoRubro(
                    Rubros.CNT_PARAMETROS_ASIENTOS_AUTOMATICOS, Long.valueOf(Estado.ACTIVO));
        } catch (Throwable e) {
            System.out.println("fechaVigenciaTiposAutomaticos: no se pudo leer el rubro "
                    + Rubros.CNT_PARAMETROS_ASIENTOS_AUTOMATICOS + " (" + e.getMessage() + "); se usa la vigencia"
                    + " por defecto " + TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO);
            return TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO;
        }
        String valorEmpresa = null;
        String valorTodas = null;
        if (detalles != null) {
            for (DetalleRubro d : detalles) {
                if (d.getCodigoAlterno() == null) {
                    continue;
                }
                if (idEmpresa != null && d.getCodigoAlterno().longValue() == idEmpresa.longValue()) {
                    valorEmpresa = d.getValorAlfanumerico();
                } else if (d.getCodigoAlterno().intValue() == CntParametroAsientoAutomatico.VIGENCIA_TODAS_LAS_EMPRESAS) {
                    valorTodas = d.getValorAlfanumerico();
                }
            }
        }
        String valor = (valorEmpresa != null && !valorEmpresa.trim().isEmpty()) ? valorEmpresa : valorTodas;
        if (valor == null || valor.trim().isEmpty()) {
            return TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            System.out.println("fechaVigenciaTiposAutomaticos: el valor '" + valor + "' del rubro "
                    + Rubros.CNT_PARAMETROS_ASIENTOS_AUTOMATICOS + " para la empresa " + idEmpresa
                    + " no es una fecha yyyy-MM-dd; se usa la vigencia por defecto "
                    + TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO);
            return TipoAsientoAutomatico.FECHA_VIGENCIA_DEFECTO;
        }
    }

    @Override
    public Asiento generarAsientoManual(Long idEmpresa, TipoAsiento tipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas) throws Throwable {

        System.out.println("=== generarAsientoManual | empresa=" + idEmpresa
                + " | tipo=" + (tipoAsiento != null ? tipoAsiento.getCodigo() : null) + " ===");
        if (tipoAsiento == null || tipoAsiento.getCodigo() == null) {
            throw new IncomeException("generarAsientoManual: el tipo de asiento es obligatorio.");
        }
        // Asiento manual: tipo explicito (sin la regla IN-A / EG-A / DI-A), modulo CONTABILIDAD, sin modulo origen
        // (DISENO-TIPOS-ASIENTO-AUTOMATICOS.md §2.6: los manuales no cambian).
        return grabarAsiento(idEmpresa, tipoAsiento, fechaAsiento, observaciones, usuario, lineas,
                Long.valueOf(ModuloSistema.CONTABILIDAD), false, null);
    }

    /**
     * Pasos comunes de {@link #generarAsiento(Long, int, LocalDate, String, String, List, Long, String)} y
     * {@link #generarAsientoManual}: empresa, cabecera, saveSingle (periodo, numero, numero alterno), lineas y
     * validacion debe = haber. Con {@code automatico} (asiento del sistema con fecha desde la vigencia de la
     * empresa) se graba el modulo origen y su consecutivo mensual; los manuales van con false.
     */
    private Asiento grabarAsiento(Long idEmpresa, TipoAsiento tipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario,
            List<DetalleAsiento> lineas, Long moduloSistema, boolean automatico, String moduloOrigen)
            throws Throwable {

        // D-034 (29-oct-2026): la cabecera se graba ANTES que las líneas; sin líneas quedaba un asiento
        // ACTIVO vacío (pensión complementaria, asientos 12767 y 12974 de junio 2025, y 8892-11615 de
        // ago-oct 2026). El llamador decide si "no hay nada que contabilizar" ANTES de llamar; aquí, un
        // asiento sin líneas es un error y no se graba.
        if (lineas == null || lineas.isEmpty()) {
            throw new IncomeException("ASIENTO_SIN_LINEAS: no se genera un asiento sin líneas ("
                    + observaciones + ", fecha " + fechaAsiento + ").");
        }

        // 2. Obtener Empresa
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        if (empresa == null) {
            throw new IncomeException("No se encontrÃ³ la empresa con ID: " + idEmpresa);
        }

        // 3. Construir cabecera del asiento
        Asiento asiento = new Asiento();
        asiento.setCodigo(null);
        asiento.setEmpresa(empresa);
        asiento.setTipoAsiento(tipoAsiento);
        asiento.setFechaAsiento(fechaAsiento);
        asiento.setObservaciones(observaciones);
        // U-5b (defensa central): sin relleno a "SISTEMA". El usuario debe existir; si no, UsuarioInvalidoException (rollback)
        // y no se graba ningun asiento.
        asiento.setNombreUsuario(usuarioValidacionService.validar(usuario));
        asiento.setEstado(Long.valueOf(EstadoAsiento.ACTIVO));
        asiento.setMoneda(Long.valueOf(TipoMoneda.DOLAR));
        // Rubro mÃ³dulo del sistema al que se clasifica el asiento
        Long modulo = (moduloSistema != null)
                ? moduloSistema : Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR);
        asiento.setRubroModuloClienteP(Long.valueOf(Rubros.MODULO_SISTEMA));
        asiento.setRubroModuloClienteH(modulo);
        asiento.setRubroModuloSistemaP(Long.valueOf(Rubros.MODULO_SISTEMA));
        asiento.setRubroModuloSistemaH(modulo);
        if (automatico) {
            asiento.setModuloOrigen(moduloOrigen);
        }

        // 4. saveSingle: asigna perÃ­odo, nÃºmero, numeroAlterno y graba
        asiento = asientoService.saveSingle(asiento);
        // Consecutivo mensual por modulo origen (doble control), solo asientos automaticos desde la vigencia;
        // va despues del saveSingle porque necesita el mes y el anio que el asigna (de la fecha del asiento).
        if (automatico) {
            asiento = asientoService.asignaNumeroModulo(asiento);
        }

        // 5. Grabar cada lÃ­nea de detalle
        for (DetalleAsiento linea : lineas) {
            linea.setAsiento(asiento);
            detalleAsientoService.saveDetalle(linea);
        }

        // 6. Validar que debe == haber â€” si no cuadra, lanzar excepciÃ³n para revertir todo
        boolean cuadrado = detalleAsientoService.validaDebeHaber(asiento.getCodigo());
        if (!cuadrado) {
            double totalDebe  = 0.0;
            double totalHaber = 0.0;
            StringBuilder detalle = new StringBuilder();
            detalle.append("El asiento ").append(asiento.getCodigo())
                   .append(" no estÃ¡ cuadrado (debe â‰  haber). Detalle de lÃ­neas:\n");
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

        System.out.println("âœ“ Asiento contable generado: " + asiento.getNumeroAlterno()
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

        // â”€â”€ DEBE: cuenta caja/banco (tipoCuenta=3, tipoPersona=1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCaja = obtenerCuentaPorTipo(codigoTitular, idEmpresa, 3L);
        if (cuentaCaja == null) {
            throw new com.saa.basico.util.IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta de caja/banco (Tipo 3) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables.");
        }

        // â”€â”€ HABER: cuenta de anticipos del cliente (tipoCuenta=2, tipoPersona=1) â”€
        PlanCuenta cuentaAnticipo = obtenerCuentaPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new com.saa.basico.util.IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables.");
        }

        // â”€â”€ Construir lÃ­neas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Generar asiento â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        String obs = "Anticipo cliente: " + nomCliente
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : "")
                + " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                anticipo.getFechaAnticipo(), obs, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
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

        // â”€â”€ DEBE: planCuenta de la cuenta bancaria â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontrÃ³ la cuenta bancaria con ID: " + idCuentaBancaria);
        }
        com.saa.model.cnt.PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        if (cuentaBanco == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada.");
        }

        // â”€â”€ HABER: cuenta de anticipos del cliente (tipoCuenta=2, rol Cliente) â”€
        com.saa.model.cnt.PlanCuenta cuentaAnticipo =
                obtenerCuentaPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Cliente).");
        }

        // â”€â”€ Construir lÃ­neas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Generar asiento â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        String obs = "Anticipo cliente: " + nomCliente
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : "")
                + " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                anticipo.getFechaAnticipo(), obs, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    // ---------------------------------------------------------------
    // generarAsientoAnticipoProveedor
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String usuario) throws Throwable {
        return generarAsientoAnticipoProveedor(anticipo, idCuentaBancaria, codigoAltTipoAsiento,
                fechaAsiento, usuario, null);
    }

    @Override
    public Asiento generarAsientoAnticipoProveedor(com.saa.model.cxp.AnticipoProveedor anticipo,
            Long idCuentaBancaria, int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String usuario, String observaciones) throws Throwable {

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

        // â”€â”€ DEBE: cuenta de anticipos del proveedor (tipoCuenta=2, tipoPersona=2) â”€â”€
        PlanCuenta cuentaAnticipo = obtenerCuentaProveedorPorTipo(codigoTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Proveedor).");
        }

        // â”€â”€ HABER: planCuenta de la cuenta bancaria â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontrÃ³ la cuenta bancaria con ID: " + idCuentaBancaria);
        }
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();
        if (cuentaBanco == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada.");
        }

        // â”€â”€ Construir lÃ­neas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Generar asiento â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        String obs = "Anticipo proveedor: " + nomProv
                + " | Doc: " + (anticipo.getNumeroDoc() != null ? anticipo.getNumeroDoc() : "")
                + " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);
        if (observaciones != null && !observaciones.trim().isEmpty()) {
            obs += " | " + observaciones.trim();
        }

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                (fechaAsiento != null) ? fechaAsiento : anticipo.getFechaAnticipo(),
                obs, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    // ---------------------------------------------------------------
    // generarAsientoDevolucionAnticipoProveedor (API-DEVOLUCION-ANTICIPO-PROVEEDOR.md §4.2)
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoDevolucionAnticipoProveedor(Long idTitular, Long idCuentaBancaria,
            Double valor, Long idEmpresa, LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {
        // Devolución solo de anticipos: todo el valor va al HABER de anticipos, como siempre.
        return generarAsientoDevolucionAnticipoProveedor(idTitular, idCuentaBancaria, valor, valor, 0.0,
                idEmpresa, fechaAsiento, observaciones, usuario);
    }

    @Override
    public Asiento generarAsientoDevolucionAnticipoProveedor(Long idTitular, Long idCuentaBancaria,
            Double valor, Double valorAnticipos, Double valorNotasCredito, Long idEmpresa,
            LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoDevolucionAnticipoProveedor | titular=" + idTitular
                + " | cuenta=" + idCuentaBancaria + " | valor=" + valor + " | anticipos=" + valorAnticipos
                + " | notasCredito=" + valorNotasCredito + " | empresa=" + idEmpresa + " ===");

        validaDatosAplicacion(idTitular, valor, idEmpresa);

        double sumaAnticipos = (valorAnticipos != null) ? valorAnticipos : 0.0;
        double sumaNotasCredito = (valorNotasCredito != null) ? valorNotasCredito : 0.0;
        if (Math.abs((sumaAnticipos + sumaNotasCredito) - valor) > 0.01) {
            throw new IncomeException("El total de la devolución ($" + String.format(java.util.Locale.US, "%.2f", valor)
                    + ") no es la suma de los anticipos ($" + String.format(java.util.Locale.US, "%.2f", sumaAnticipos)
                    + ") y las notas de crédito ($" + String.format(java.util.Locale.US, "%.2f", sumaNotasCredito) + ").");
        }

        Titular titular = em.find(Titular.class, idTitular);
        String nomProv = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // DEBE: planCuenta de la cuenta bancaria propia donde entró el depósito
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        // HABER: cuenta de anticipos del proveedor (tipoCuenta=2, rol Proveedor); solo si hay anticipos
        PlanCuenta cuentaAnticipo = (sumaAnticipos > 0.005)
                ? obtenerCuentaProveedorPorTipo(idTitular, idEmpresa, 2L) : null;
        if (sumaAnticipos > 0.005 && cuentaAnticipo == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Proveedor).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaBanco);
        debe.setNumeroCuenta(cuentaBanco.getCuentaContable());
        debe.setNombreCuenta(cuentaBanco.getNombre());
        debe.setDescripcion("Devolución de anticipo: " + nomProv
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta());
        debe.setValorDebe(valor);
        debe.setValorHaber(0.0);
        lineas.add(debe);

        if (sumaAnticipos > 0.005) {
            DetalleAsiento haber = new DetalleAsiento();
            haber.setPlanCuenta(cuentaAnticipo);
            haber.setNumeroCuenta(cuentaAnticipo.getCuentaContable());
            haber.setNombreCuenta(cuentaAnticipo.getNombre());
            haber.setDescripcion("Devolución de anticipo: " + nomProv);
            haber.setValorDebe(0.0);
            haber.setValorHaber(Math.round(sumaAnticipos * 100.0) / 100.0);
            lineas.add(haber);
        }

        // HABER: cuenta por pagar del proveedor (tipoCuenta=1), la misma que debitó la nota de
        // crédito al registrarse (generarAsientoNotaCreditoCompra); solo si hay notas de crédito.
        if (sumaNotasCredito > 0.005) {
            PlanCuenta cuentaProveedor = obtenerCuentaProveedor(idTitular, idEmpresa);
            if (cuentaProveedor == null) {
                throw new IncomeException(
                        "El proveedor '" + nomProv + "' no tiene cuenta contable de facturas (Tipo 1) "
                        + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Proveedor).");
            }
            DetalleAsiento haberNc = new DetalleAsiento();
            haberNc.setPlanCuenta(cuentaProveedor);
            haberNc.setNumeroCuenta(cuentaProveedor.getCuentaContable());
            haberNc.setNombreCuenta(cuentaProveedor.getNombre());
            haberNc.setDescripcion("Devolución de saldo de nota de crédito: " + nomProv);
            haberNc.setValorDebe(0.0);
            haberNc.setValorHaber(Math.round((valor - (sumaAnticipos > 0.005 ? Math.round(sumaAnticipos * 100.0) / 100.0 : 0.0)) * 100.0) / 100.0);
            lineas.add(haberNc);
        }

        String obs = "Devolución de anticipo: " + nomProv
                + " | Valor: $" + String.format(java.util.Locale.US, "%.2f", valor);
        if (observaciones != null && !observaciones.trim().isEmpty()) {
            obs += " | " + observaciones.trim();
        }

        // Mismo tipo de asiento y mismo módulo que generarAsientoAnticipoProveedor: ese método
        // llama al generarAsiento de 6 argumentos (sin módulo explícito), que internamente usa
        // ModuloSistema.CUENTAS_POR_COBRAR -- verificado leyendo ese overload, no asumido.
        return generarAsiento(idEmpresa, TipoAsientos.ANTICIPOS_PROVEEDOR, fechaAsiento, obs, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    // ---------------------------------------------------------------
    // TesorerÃ­a: aplicaciÃ³n de pagos y cobros a facturas
    // ---------------------------------------------------------------

    @Override
    public Asiento generarAsientoAplicacionAnticipoProveedor(Long idTitular, Double valor,
            Long idEmpresa, int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {
        // ÍTEM 2 (API-CRUCE-ANTICIPO-OTRO-PROVEEDOR.md §3.4): delega en la variante de dos
        // titulares, con el mismo titular a los dos lados -- resultado idéntico a antes.
        return generarAsientoAplicacionAnticipoProveedor(idTitular, idTitular, valor, idEmpresa,
                codigoAltTipoAsiento, fechaAsiento, observaciones, usuario);
    }

    @Override
    public Asiento generarAsientoAplicacionAnticipoProveedor(Long idTitularFactura, Long idTitularAnticipo,
            Double valor, Long idEmpresa, int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAplicacionAnticipoProveedor | titularFactura=" + idTitularFactura
                + " | titularAnticipo=" + idTitularAnticipo + " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

        validaDatosAplicacion(idTitularFactura, valor, idEmpresa);
        if (idTitularAnticipo == null) {
            throw new IncomeException("Debe indicar el titular del anticipo.");
        }

        Titular titularFactura = em.find(Titular.class, idTitularFactura);
        String nomProvFactura = (titularFactura != null) ? titularFactura.getNombre() : String.valueOf(idTitularFactura);
        Titular titularAnticipo = idTitularFactura.equals(idTitularAnticipo)
                ? titularFactura : em.find(Titular.class, idTitularAnticipo);
        String nomProvAnticipo = (titularAnticipo != null) ? titularAnticipo.getNombre() : String.valueOf(idTitularAnticipo);

        // â”€â”€ DEBE: cuenta CxP del proveedor DE LA FACTURA (tipoCuenta=1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaProveedor = obtenerCuentaProveedorPorTipo(idTitularFactura, idEmpresa, 1L);
        if (cuentaProveedor == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProvFactura + "' no tiene cuenta contable de facturas (Tipo 1) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Proveedor).");
        }

        // â”€â”€ HABER: cuenta de anticipos del proveedor DEL ANTICIPO (tipoCuenta=2) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaAnticipo = obtenerCuentaProveedorPorTipo(idTitularAnticipo, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProvAnticipo + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Proveedor).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaProveedor, "Cruce anticipo proveedor: " + nomProvFactura, valor, true));
        lineas.add(creaLinea(cuentaAnticipo,  "Anticipo aplicado: " + nomProvAnticipo,        valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_PAGAR), ModuloOrigenAsiento.CXP);
    }

    @Override
    public Asiento generarAsientoAplicacionAnticipoCliente(Long idTitular, Double valor,
            Long idEmpresa, int codigoAltTipoAsiento, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAplicacionAnticipoCliente | titular=" + idTitular
                + " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

        validaDatosAplicacion(idTitular, valor, idEmpresa);

        Titular titular = em.find(Titular.class, idTitular);
        String nomCliente = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // â”€â”€ DEBE: cuenta de anticipos del cliente (tipoCuenta=2) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaAnticipo = obtenerCuentaPorTipo(idTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Cliente).");
        }

        // â”€â”€ HABER: cuenta CxC del cliente (tipoCuenta=1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCliente = obtenerCuentaCliente(idTitular, idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de facturas (Tipo 1) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Cliente).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaAnticipo, "Anticipo aplicado: " + nomCliente,        valor, true));
        lineas.add(creaLinea(cuentaCliente,  "Cruce anticipo cliente: " + nomCliente,   valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    @Override
    public Asiento generarAsientoPagoTransferenciaCxp(Long idTitular, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoPagoTransferenciaCxp | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        validaDatosAplicacion(idTitular, valor, idEmpresa);

        Titular titular = em.find(Titular.class, idTitular);
        String nomProv = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // â”€â”€ DEBE: cuenta CxP del proveedor (tipoCuenta=1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaProveedor = obtenerCuentaProveedorPorTipo(idTitular, idEmpresa, 1L);
        if (cuentaProveedor == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de facturas (Tipo 1) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Proveedor).");
        }

        // â”€â”€ HABER: cuenta contable del banco â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaProveedor, "Pago a proveedor: " + nomProv, valor, true));
        lineas.add(creaLinea(cuentaBanco,
                "Transferencia a proveedor: " + nomProv
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.CXP);
    }

    @Override
    public Asiento generarAsientoCobroTransferenciaCxc(Long idTitular, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoCobroTransferenciaCxc | titular=" + idTitular
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        validaDatosAplicacion(idTitular, valor, idEmpresa);

        Titular titular = em.find(Titular.class, idTitular);
        String nomCliente = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // â”€â”€ DEBE: cuenta contable del banco â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        // â”€â”€ HABER: cuenta CxC del cliente (tipoCuenta=1) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCliente = obtenerCuentaCliente(idTitular, idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException(
                    "El cliente '" + nomCliente + "' no tiene cuenta contable de facturas (Tipo 1) "
                    + "configurada en TesorerÃ­a â†’ Persona â†’ Cuentas Contables (Rol: Cliente).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaBanco,
                "Transferencia recibida de: " + nomCliente
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, true));
        lineas.add(creaLinea(cuentaCliente, "Cobro a cliente: " + nomCliente, valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.CXC);
    }

    @Override
    public Asiento generarAsientoEgresoTesoreria(Long idProductoPago, String concepto, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoEgresoTesoreria | producto=" + idProductoPago
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del egreso debe ser mayor a cero.");
        }

        // â”€â”€ DEBE: cuenta del grupo del producto CXP â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaGasto = obtenerCuentaGrupoProductoPago(idProductoPago);

        // â”€â”€ HABER: cuenta contable del banco â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaGasto, "Egreso tesorerÃ­a: " + concepto, valor, true));
        lineas.add(creaLinea(cuentaBanco,
                "Egreso tesorerÃ­a: " + concepto
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoEgresoContraAnticipo(Long idProductoPago, Long idTitular, Double valor,
            Long idEmpresa, LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoEgresoContraAnticipo | producto=" + idProductoPago
                + " | titular=" + idTitular + " | valor=" + valor + " | empresa=" + idEmpresa + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del egreso debe ser mayor a cero.");
        }

        Titular titular = em.find(Titular.class, idTitular);
        String nomProv = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // DEBE: cuenta del grupo del producto CXP, igual que generarAsientoEgresoTesoreria
        PlanCuenta cuentaGasto = obtenerCuentaGrupoProductoPago(idProductoPago);

        // HABER: cuenta de anticipos del proveedor (tipoCuenta=2, rol Proveedor)
        PlanCuenta cuentaAnticipo = obtenerCuentaProveedorPorTipo(idTitular, idEmpresa, 2L);
        if (cuentaAnticipo == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de anticipos (Tipo 2) "
                    + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Proveedor).");
        }

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaGasto, "Egreso pagado con anticipo: " + nomProv, valor, true));
        lineas.add(creaLinea(cuentaAnticipo, "Egreso pagado con anticipo: " + nomProv, valor, false));

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoIngresoTesoreria(Long idProductoCobro, String concepto, Double valor,
            Long idCuentaBancaria, Long idEmpresa, int codigoAltTipoAsiento,
            LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoIngresoTesoreria | producto=" + idProductoCobro
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del ingreso debe ser mayor a cero.");
        }

        // â”€â”€ DEBE: cuenta contable del banco â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        // â”€â”€ HABER: cuenta del grupo del producto CXC â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaIngreso = obtenerCuentaGrupoProductoCobro(idProductoCobro);

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaBanco,
                "Ingreso tesorerÃ­a: " + concepto
                + " | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, true));
        lineas.add(creaLinea(cuentaIngreso, "Ingreso tesorerÃ­a: " + concepto, valor, false));

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    // ---------------------------------------------------------------
    // Caja chica
    // ---------------------------------------------------------------

    /**
     * Recorta la observación al largo de la columna de observaciones del asiento (ASNTOBSR, 2000), para
     * que un concepto largo agregado al texto no aborte el grabado del asiento.
     */
    private String recortaObservacionAsiento(String observaciones) {
        return (observaciones != null && observaciones.length() > 2000)
                ? observaciones.substring(0, 2000) : observaciones;
    }

    @Override
    public Asiento generarAsientoGastoCajaChica(Long idProductoPago, String nombreCaja, String descripcion,
            Double valor, Long idPlanCuentaCaja, Long idEmpresa, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoGastoCajaChica | producto=" + idProductoPago
                + " | valor=" + valor + " | caja=" + nombreCaja + " ===");

        observaciones = recortaObservacionAsiento(observaciones);

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del gasto debe ser mayor a cero.");
        }

        // ── DEBE: cuenta del grupo del producto CXP ──────────────────────────────
        PlanCuenta cuentaGasto = obtenerCuentaGrupoProductoPago(idProductoPago);

        // ── HABER: cuenta contable de la caja chica ──────────────────────────────
        PlanCuenta cuentaCaja = obtenerPlanCuenta(idPlanCuentaCaja,
                "La caja chica no tiene cuenta contable configurada.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaGasto, "Gasto caja chica: " + descripcion, valor, true));
        lineas.add(creaLinea(cuentaCaja, "Caja chica " + nombreCaja + ": " + descripcion, valor, false));

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoAplicacionCajaChica(Long idTitular, Double valor, Long idPlanCuentaCaja,
            Long idEmpresa, LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAplicacionCajaChica | titular=" + idTitular
                + " | valor=" + valor + " ===");

        observaciones = recortaObservacionAsiento(observaciones);

        validaDatosAplicacion(idTitular, valor, idEmpresa);

        Titular titular = em.find(Titular.class, idTitular);
        String nomProv = (titular != null) ? titular.getNombre() : String.valueOf(idTitular);

        // ── DEBE: cuenta CxP del proveedor (tipoCuenta=1) ────────────────────────
        // Mismo camino que generarAsientoAplicacionAnticipoProveedor: obtenerCuentaProveedorPorTipo
        // es generico, sin acoplamiento a anticipo (verificado item 25b del plan).
        PlanCuenta cuentaProveedor = obtenerCuentaProveedorPorTipo(idTitular, idEmpresa, 1L);
        if (cuentaProveedor == null) {
            throw new IncomeException(
                    "El proveedor '" + nomProv + "' no tiene cuenta contable de facturas (Tipo 1) "
                    + "configurada en Tesorería → Persona → Cuentas Contables (Rol: Proveedor).");
        }

        // ── HABER: cuenta contable de la caja chica ──────────────────────────────
        PlanCuenta cuentaCaja = obtenerPlanCuenta(idPlanCuentaCaja,
                "La caja chica no tiene cuenta contable configurada.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaProveedor, "Pago con caja chica: " + nomProv, valor, true));
        lineas.add(creaLinea(cuentaCaja, "Caja chica: " + nomProv, valor, false));

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoReposicionCajaChica(Long idPlanCuentaCaja, Long idCuentaBancaria,
            Double valor, Long idEmpresa, LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoReposicionCajaChica | caja=" + idPlanCuentaCaja
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor de la reposición debe ser mayor a cero.");
        }

        // ── DEBE: cuenta contable de la caja chica ───────────────────────────────
        PlanCuenta cuentaCaja = obtenerPlanCuenta(idPlanCuentaCaja,
                "La caja chica no tiene cuenta contable configurada.");

        // ── HABER: cuenta contable del banco ──────────────────────────────────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaCaja, "Reposición caja chica", valor, true));
        lineas.add(creaLinea(cuentaBanco,
                "Reposición caja chica | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, false));

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoAjusteCajaChica(Long idPlanCuentaCaja, Long idPlanCuentaDiferencia,
            Double valor, boolean sobrante, Long idEmpresa, LocalDate fechaAsiento,
            String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAjusteCajaChica | caja=" + idPlanCuentaCaja
                + " | valor=" + valor + " | sobrante=" + sobrante + " ===");

        observaciones = recortaObservacionAsiento(observaciones);

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del ajuste debe ser mayor a cero.");
        }

        PlanCuenta cuentaCaja = obtenerPlanCuenta(idPlanCuentaCaja,
                "La caja chica no tiene cuenta contable configurada.");
        PlanCuenta cuentaDiferencia = obtenerPlanCuenta(idPlanCuentaDiferencia,
                "Debe indicar la cuenta de faltantes/sobrantes de caja para el ajuste.");

        String tipoTexto = sobrante ? "Sobrante" : "Faltante";
        List<DetalleAsiento> lineas = new ArrayList<>();
        if (sobrante) {
            lineas.add(creaLinea(cuentaCaja, "Ajuste caja chica - " + tipoTexto, valor, true));
            lineas.add(creaLinea(cuentaDiferencia, "Ajuste caja chica - " + tipoTexto, valor, false));
        } else {
            lineas.add(creaLinea(cuentaDiferencia, "Ajuste caja chica - " + tipoTexto, valor, true));
            lineas.add(creaLinea(cuentaCaja, "Ajuste caja chica - " + tipoTexto, valor, false));
        }

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.TSR);
    }

    @Override
    public Asiento generarAsientoAnticipoEmpleado(Long idEmpleado, Double valor, Long idCuentaBancaria,
            Long idEmpresa, LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        System.out.println("=== generarAsientoAnticipoEmpleado | empleado=" + idEmpleado
                + " | valor=" + valor + " | cuentaBancaria=" + idCuentaBancaria + " ===");

        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor del anticipo debe ser mayor a cero.");
        }

        // ── DEBE: cuenta "Cuentas por Cobrar Empleados" (línea 14 del rubro 214,
        // plantilla de ROL) — la misma que usa ContabilizacionNominaServiceImpl
        // para el descuento del rol, resuelta con el mismo mecanismo.
        PlanCuenta cuentaAnticipos = obtenerCuentaCuentasPorCobrarEmpleados(idEmpresa);

        // ── HABER: cuenta contable del banco ──────────────────────────────────────
        com.saa.model.tsr.CuentaBancaria cuentaBancaria = obtenerCuentaBancaria(idCuentaBancaria);
        PlanCuenta cuentaBanco = cuentaBancaria.getPlanCuenta();

        List<DetalleAsiento> lineas = new ArrayList<>();
        lineas.add(creaLinea(cuentaAnticipos, "Anticipo a empleado", valor, true));
        lineas.add(creaLinea(cuentaBanco,
                "Anticipo a empleado | Cta Banco: " + cuentaBancaria.getNumeroCuenta(), valor, false));

        return generarAsiento(idEmpresa, TipoAsientos.EGRESO_TESORERIA, fechaAsiento, observaciones,
                usuario, lineas, Long.valueOf(ModuloSistema.TESORERIA), ModuloOrigenAsiento.RRH);
    }

    /**
     * Resuelve la cuenta contable de {@code RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS}
     * (línea 14 del rubro 214) contra la plantilla de ROL de la empresa
     * ({@code ConfiguracionNomina.plantillaRol}) — el mismo mecanismo de
     * resolución que usa {@code ContabilizacionNominaServiceImpl} para el
     * descuento del rol (plantilla → línea por auxiliar1 → cuenta real,
     * rechazando la cuenta marcadora), sin duplicarlo como una cuenta
     * hardcodeada aquí.
     * @param idEmpresa  : Id de la empresa contable
     * @return           : Cuenta contable real de "Cuentas por Cobrar Empleados"
     * @throws Throwable : IncomeException si la configuración de nómina, la
     *                     plantilla, la línea o la cuenta real no existen
     */
    private PlanCuenta obtenerCuentaCuentasPorCobrarEmpleados(Long idEmpresa) throws Throwable {
        com.saa.model.rhh.ConfiguracionNomina configuracion =
                configuracionNominaDaoService.selectByEmpresa(idEmpresa);
        if (configuracion == null) {
            throw new IncomeException("No existe configuración de nómina (RHH.CFNM) para la empresa "
                    + idEmpresa + ": sin ella no se puede resolver la cuenta de anticipos a empleados.");
        }
        Long codigoAlternoPlantilla = configuracion.getPlantillaRol();
        if (codigoAlternoPlantilla == null) {
            throw new IncomeException("La configuración de nómina (RHH.CFNM) no tiene la plantilla de"
                    + " ROL asignada para la empresa " + idEmpresa + ".");
        }
        Long idPlantilla = plantillaService.codigoByAlterno(codigoAlternoPlantilla.intValue(), idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No existe la plantilla contable con código alterno "
                    + codigoAlternoPlantilla + " (rol) para la empresa " + idEmpresa + ".");
        }
        com.saa.model.cnt.DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(
                idPlantilla, com.saa.rubros.RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS);
        if (linea == null) {
            throw new IncomeException("La plantilla de rol no define la línea "
                    + com.saa.rubros.RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS
                    + " (Cuentas por Cobrar Empleados) del rubro 214.");
        }
        Long marcadora = configuracion.getCuentaMarcadora();
        if (linea.getPlanCuenta() == null
                || (marcadora != null && marcadora.equals(linea.getPlanCuenta().getCodigo()))) {
            throw new IncomeException("La línea " + com.saa.rubros.RhhLineaAsiento.CUENTAS_POR_COBRAR_EMPLEADOS
                    + " (Cuentas por Cobrar Empleados) de la plantilla de rol sigue apuntando a la cuenta"
                    + " marcadora o no tiene cuenta: asigne su cuenta contable real en Nómina → Plantillas"
                    + " Contables antes de entregar anticipos.");
        }
        return linea.getPlanCuenta();
    }

    /**
     * Recupera una cuenta contable (PlanCuenta) por id, con mensaje accionable.
     * @param idPlanCuenta : Id de la cuenta contable
     * @param mensajeError : Mensaje a lanzar si no se encuentra
     * @return             : Cuenta contable
     * @throws Throwable   : IncomeException si no existe
     */
    private PlanCuenta obtenerPlanCuenta(Long idPlanCuenta, String mensajeError) throws Throwable {
        if (idPlanCuenta == null) {
            throw new IncomeException(mensajeError);
        }
        PlanCuenta cuenta = em.find(PlanCuenta.class, idPlanCuenta);
        if (cuenta == null) {
            throw new IncomeException("No se encontró la cuenta contable con ID: " + idPlanCuenta);
        }
        return cuenta;
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    /**
     * Recupera la cuenta contable del grupo de un producto CXP, validando la
     * cadena producto â†’ grupo â†’ planCuenta con mensajes accionables.
     * @param idProductoPago : Id del producto CXP (PGS.PRDP)
     * @return               : Cuenta contable del grupo
     * @throws Throwable     : Excepcion si falta el producto, el grupo o la cuenta
     */
    private PlanCuenta obtenerCuentaGrupoProductoPago(Long idProductoPago) throws Throwable {
        if (idProductoPago == null) {
            throw new IncomeException("Debe indicar el producto que clasifica el egreso.");
        }
        com.saa.model.cxp.ProductoPago producto =
                em.find(com.saa.model.cxp.ProductoPago.class, idProductoPago);
        if (producto == null) {
            throw new IncomeException("No se encontrÃ³ el producto CXP con ID: " + idProductoPago);
        }
        if (producto.getGrupoProducto() == null) {
            throw new IncomeException("El producto '" + producto.getNombre()
                    + "' no tiene grupo asignado. ClasifÃ­quelo en CXP â†’ Productos antes de usarlo.");
        }
        if (producto.getGrupoProducto().getPlanCuenta() == null) {
            throw new IncomeException("El grupo '" + producto.getGrupoProducto().getNombre()
                    + "' del producto '" + producto.getNombre()
                    + "' no tiene cuenta contable configurada (Contabilidad â†’ Grupos de Producto).");
        }
        return producto.getGrupoProducto().getPlanCuenta();
    }

    /**
     * Recupera la cuenta contable del grupo de un producto CXC, validando la
     * cadena producto â†’ grupo â†’ planCuenta con mensajes accionables.
     * @param idProductoCobro : Id del producto CXC (CBR.PRDC)
     * @return                : Cuenta contable del grupo
     * @throws Throwable      : Excepcion si falta el producto, el grupo o la cuenta
     */
    private PlanCuenta obtenerCuentaGrupoProductoCobro(Long idProductoCobro) throws Throwable {
        if (idProductoCobro == null) {
            throw new IncomeException("Debe indicar el producto que clasifica el ingreso.");
        }
        com.saa.model.cxc.ProductoCobro producto =
                em.find(com.saa.model.cxc.ProductoCobro.class, idProductoCobro);
        if (producto == null) {
            throw new IncomeException("No se encontrÃ³ el producto CXC con ID: " + idProductoCobro);
        }
        if (producto.getGrupoProducto() == null) {
            throw new IncomeException("El producto '" + producto.getNombre()
                    + "' no tiene grupo asignado. ClasifÃ­quelo en CXC â†’ Productos antes de usarlo.");
        }
        if (producto.getGrupoProducto().getPlanCuenta() == null) {
            throw new IncomeException("El grupo '" + producto.getGrupoProducto().getNombre()
                    + "' del producto '" + producto.getNombre()
                    + "' no tiene cuenta contable configurada (Contabilidad â†’ Grupos de Producto).");
        }
        return producto.getGrupoProducto().getPlanCuenta();
    }

    /**
     * Valida los datos mÃ­nimos de un asiento de aplicaciÃ³n de pago/cobro.
     * @param idTitular : Id del titular (cliente o proveedor)
     * @param valor     : Valor a aplicar, debe ser mayor a cero
     * @param idEmpresa : Id de la empresa contable
     * @throws Throwable : Excepcion
     */
    private void validaDatosAplicacion(Long idTitular, Double valor, Long idEmpresa) throws Throwable {
        if (idTitular == null) {
            throw new IncomeException("Debe indicar el titular (cliente o proveedor).");
        }
        if (idEmpresa == null) {
            throw new IncomeException("Debe indicar la empresa contable.");
        }
        if (valor == null || valor <= 0) {
            throw new IncomeException("El valor a aplicar debe ser mayor a cero.");
        }
    }

    /**
     * Recupera una cuenta bancaria propia validando que tenga cuenta contable.
     * @param idCuentaBancaria : Id de la cuenta bancaria (TSR.CNBC)
     * @return                 : Cuenta bancaria con su PlanCuenta cargado
     * @throws Throwable       : Excepcion
     */
    private com.saa.model.tsr.CuentaBancaria obtenerCuentaBancaria(Long idCuentaBancaria) throws Throwable {
        if (idCuentaBancaria == null) {
            throw new IncomeException("Debe indicar la cuenta bancaria.");
        }
        com.saa.model.tsr.CuentaBancaria cuentaBancaria =
                em.find(com.saa.model.tsr.CuentaBancaria.class, idCuentaBancaria);
        if (cuentaBancaria == null) {
            throw new IncomeException("No se encontrÃ³ la cuenta bancaria con ID: " + idCuentaBancaria);
        }
        if (cuentaBancaria.getPlanCuenta() == null) {
            throw new IncomeException(
                    "La cuenta bancaria '" + cuentaBancaria.getNumeroCuenta()
                    + "' no tiene una cuenta contable (PlanCuenta) asociada.");
        }
        return cuentaBancaria;
    }

    /**
     * Construye una lÃ­nea de asiento al DEBE o al HABER.
     * @param cuenta      : Cuenta contable de la lÃ­nea
     * @param descripcion : DescripciÃ³n de la lÃ­nea
     * @param valor       : Valor de la lÃ­nea
     * @param esDebe      : true = DEBE, false = HABER
     * @return            : Detalle de asiento listo para agregar a la lista
     */
    private DetalleAsiento creaLinea(PlanCuenta cuenta, String descripcion,
            Double valor, boolean esDebe) {
        DetalleAsiento linea = new DetalleAsiento();
        linea.setPlanCuenta(cuenta);
        linea.setNumeroCuenta(cuenta.getCuentaContable());
        linea.setNombreCuenta(cuenta.getNombre());
        linea.setDescripcion(descripcion);
        linea.setValorDebe(esDebe  ? valor : 0.0);
        linea.setValorHaber(esDebe ? 0.0   : valor);
        return linea;
    }

    /**
     * Obtiene la cuenta contable de un titular para un ROL y un tipo de cuenta.
     * <p>
     * El rol es obligatorio: un mismo titular puede ser cliente Y proveedor a la
     * vez, y entonces tiene dos {@code PersonaCuentaContable} con el mismo
     * tipoCuenta y la misma empresa. Sin filtrar por rol, la consulta devuelve
     * una fila arbitraria y el asiento sale con la cuenta del rol equivocado.
     * <p>
     * La resoluciÃ³n vive en
     * {@code PersonaCuentaContableDaoService.selectByTitularRolTipoCuenta},
     * compartida con los flujos de anticipos de CXP y CXC.
     *
     * @param codigoTitular : CÃ³digo del titular
     * @param idEmpresa     : Empresa contable
     * @param tipoCuenta    : 1=Facturas, 2=Anticipos, 3=Caja/Banco
     * @param rolPersona    : {@link RolPersona#CLIENTE} o {@link RolPersona#PROVEEDOR}
     * @return : Cuenta contable, o null si no estÃ¡ configurada
     */
    private PlanCuenta obtenerCuentaPersona(Long codigoTitular, Long idEmpresa,
            Long tipoCuenta, int rolPersona) {

        String nombreRol = (rolPersona == RolPersona.PROVEEDOR) ? "Proveedor" : "Cliente";
        System.out.println("  [obtenerCuentaPersona] titular=" + codigoTitular
                + " | empresa=" + idEmpresa + " | tipoCuenta=" + tipoCuenta
                + " | rol=" + nombreRol + "(" + rolPersona + ")");
        try {
            List<com.saa.model.tsr.PersonaCuentaContable> cuentas = personaCuentaContableDaoService
                    .selectByTitularRolTipoCuenta(idEmpresa, codigoTitular, rolPersona, tipoCuenta);

            if (cuentas.isEmpty()) {
                diagnosticoCuentaPersona(codigoTitular, nombreRol, tipoCuenta);
                return null;
            }
            PlanCuenta pc = cuentas.get(0).getPlanCuenta();
            if (pc == null) {
                System.err.println("  [obtenerCuentaPersona] âœ— La cuenta contable del titular "
                        + codigoTitular + " (rol " + nombreRol + ", tipoCuenta " + tipoCuenta
                        + ") no tiene PlanCuenta asignado.");
                return null;
            }
            System.out.println("  [obtenerCuentaPersona] âœ“ Cuenta " + nombreRol + ": "
                    + pc.getCuentaContable() + " - " + pc.getNombre());
            return pc;
        } catch (Throwable e) {
            System.err.println("âš  Error buscando cuenta tipo " + tipoCuenta
                    + " (rol " + nombreRol + ") del titular " + codigoTitular
                    + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Vuelca en el log las PersonaCuentaContable reales del titular cuando no se
     * encontrÃ³ ninguna cuenta, para poder diagnosticar quÃ© falta configurar.
     * @param codigoTitular : Titular consultado
     * @param nombreRol     : Rol que se buscaba (para el mensaje)
     * @param tipoCuenta    : Tipo de cuenta que se buscaba
     */
    private void diagnosticoCuentaPersona(Long codigoTitular, String nombreRol, Long tipoCuenta) {
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
            System.err.println("  [obtenerCuentaPersona] âœ— No encontrado."
                    + " rol=" + nombreRol + " | tipoCuenta=" + tipoCuenta
                    + " | PersonaRol del titular: " + totalPrrl
                    + " | PersonaCuentaContable del titular (sin filtros de empresa/tipo): " + totalPrcc);
            @SuppressWarnings("unchecked")
            List<Object[]> rawRows = em.createQuery(
                    "SELECT pcc.codigo, pcc.tipoCuenta, pcc.tipoPersona, "
                    + "pcc.empresa.codigo, pr.estado, pcc.planCuenta.cuentaContable, "
                    + "pr.rubroRolPersonaP, pr.rubroRolPersonaH "
                    + "FROM PersonaCuentaContable pcc "
                    + "JOIN pcc.personaRol pr "
                    + "WHERE pr.titular.codigo = :t")
                    .setParameter("t", codigoTitular)
                    .getResultList();
            for (Object[] row : rawRows) {
                System.err.println("  [obtenerCuentaPersona] PRCC registro:"
                        + " PRCCCDGO=" + row[0]
                        + " | tipoCuenta(PRCCTPOO)=" + row[1]
                        + " | tipoPersona(PRCCCLPR)=" + row[2]
                        + " | empresa(PJRQCDGO)=" + row[3]
                        + " | pr.estado(PRRLESTD)=" + row[4]
                        + " | cuentaContable=" + row[5]
                        + " | rubroPadre(PRRLRYYA)=" + row[6]
                        + " | rol(PRRLRZZA)=" + row[7]);
            }
        } catch (Exception ex) {
            System.err.println("  [obtenerCuentaPersona] âœ— No encontrado "
                    + "(diagnÃ³stico fallÃ³: " + ex.getMessage() + ")");
        }
    }

    /**
     * Obtiene la cuenta contable de un cliente por tipo de cuenta.
     * tipoCuenta: 1=Facturas, 2=Anticipos, 3=Caja/Banco
     */
    private PlanCuenta obtenerCuentaPorTipo(Long codigoTitular, Long idEmpresa, Long tipoCuenta) {
        return obtenerCuentaPersona(codigoTitular, idEmpresa, tipoCuenta, RolPersona.CLIENTE);
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
     */
    private PlanCuenta obtenerCuentaProveedorPorTipo(Long codigoTitular, Long idEmpresa, Long tipoCuenta) {
        return obtenerCuentaPersona(codigoTitular, idEmpresa, tipoCuenta, RolPersona.PROVEEDOR);
    }

    /**
     * Obtiene la cuenta contable del IVA desde TSRI.
     * Busca por tsri.codigo (campo String) dentro de la categorÃ­a lsri.tabla = '17' (IVA).
     * El campo codigoIVASRI de DetalleFactura almacena el valor numÃ©rico del campo CODIGO de TSRI.
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
            System.err.println("âš  Error buscando cuenta de IVA codigo=" + codigoIVASRI + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene el texto descriptivo de un tipo de IVA desde TSRI para mensajes de error.
     * Busca por tsri.codigo (String) dentro de la categorÃ­a lsri.tabla='17'.
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
    // Stubs CXC â€” Documentos de Cobro
    // =========================================================================
    // Estos mÃ©todos estÃ¡n listos para recibir la plantilla (codigoAltTipoAsiento)
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

        // 1. Cargar la Nota de CrÃ©dito
        com.saa.model.cxc.NotaCredito nc =
                em.find(com.saa.model.cxc.NotaCredito.class, idNotaCredito);
        if (nc == null) {
            throw new IncomeException("No se encontrÃ³ la Nota de CrÃ©dito con ID: " + idNotaCredito);
        }

        // 2. Cargar detalles activos
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxc.DetalleNotaCredito> detalles = em.createQuery(
                "SELECT d FROM DetalleNotaCredito d WHERE d.notaCredito.id = :id AND d.estado = 1")
                .setParameter("id", idNotaCredito)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La Nota de CrÃ©dito " + idNotaCredito + " no tiene detalles activos.");
        }

        // 3. Construir lÃ­neas del asiento
        // NOTA: La lÃ³gica es idÃ©ntica a la de factura pero con DEBE y HABER invertidos.
        //   Factura:     DEBE=CxC cliente | HABER=Ingresos/IVA
        //   Nota CrÃ©dito:HABER=CxC cliente | DEBE=Ingresos/IVA  (anulaciÃ³n/reducciÃ³n)
        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ HABER: cuenta CxC del cliente (en factura era DEBE) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                nc.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontrÃ³ cuenta contable (tipo factura) "
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

        // â”€â”€ DEBE: una lÃ­nea por grupo de producto (en factura era HABER) â”€â”€â”€â”€â”€â”€
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        for (com.saa.model.cxc.DetalleNotaCredito detalle : detalles) {
            if (detalle.getProducto() == null) {
                throw new IncomeException("El detalle '" + detalle.getDescripcion()
                        + "' no tiene producto asignado.");
            }
            // Obtener grupo del producto vÃ­a JPQL (producto es Long FK)
            @SuppressWarnings("unchecked")
            List<Object[]> grupoRows = em.createQuery(
                    "SELECT p.grupoProducto.codigo, p.grupoProducto.nombre, p.grupoProducto.planCuenta "
                    + "FROM ProductoCobro p WHERE p.id = :id")
                    .setParameter("id", detalle.getProducto())
                    .setMaxResults(1)
                    .getResultList();
            if (grupoRows.isEmpty()) {
                throw new IncomeException("El producto ID " + detalle.getProducto()
                        + " ('" + detalle.getDescripcion() + "') no se encontrÃ³ o no tiene grupo asignado.");
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

        // â”€â”€ DEBE: una lÃ­nea por tipo de IVA (en factura era HABER) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // DetalleNotaCredito.porcentajeIVA es el % (0,5,8,15). Mapeamos a cÃ³digo SRI.
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
                        "No se encontrÃ³ cuenta contable para el IVA con cÃ³digo SRI: " + codigoIVASRI
                        + ". Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
            }
            DetalleAsiento lineaIVA = new DetalleAsiento();
            lineaIVA.setPlanCuenta(cuentaIVA);
            lineaIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            lineaIVA.setNombreCuenta(cuentaIVA.getNombre());
            lineaIVA.setDescripcion("NC IVA cÃ³digo SRI: " + codigoIVASRI);
            lineaIVA.setValorDebe(valorIVA);
            lineaIVA.setValorHaber(0.0);
            lineas.add(lineaIVA);
        }

        // 4. Generar el asiento con las lÃ­neas construidas
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    /** Mapea porcentaje de IVA (Long: 0,5,8,15) al cÃ³digo SRI de TSRI (tabla='17'). */
    private String mapPorcentajeIVAaCodigo(Long porcentaje) {
        if (porcentaje == null) return "0";
        switch (porcentaje.intValue()) {
            case 0:  return "0";   // IVA 0%
            case 5:  return "5";   // IVA 5%
            case 8:  return "8";   // IVA tarifa especial 8%
            case 12: return "2";   // IVA 12% (histÃ³rico, cÃ³digo SRI = 2)
            case 14: return "3";   // IVA 14% (histÃ³rico, cÃ³digo SRI = 3)
            case 15: return "4";   // IVA 15% (cÃ³digo SRI = 4)
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

        // 1. Cargar la Nota de DÃ©bito
        com.saa.model.cxc.NotaDebito nd =
                em.find(com.saa.model.cxc.NotaDebito.class, idNotaDebito);
        if (nd == null) {
            throw new IncomeException("No se encontrÃ³ la Nota de DÃ©bito con ID: " + idNotaDebito);
        }

        // 2. La ND no tiene lÃ­neas de producto propias: obtener cuentas de ingreso
        //    desde la factura relacionada.
        List<DetalleFactura> detallesFact = obtenerDetallesParaND(nd);

        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ DEBE: cuenta CxC del cliente (igual que Factura) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        PlanCuenta cuentaCliente = obtenerCuentaCliente(
                nd.getTitular().getCodigo(), idEmpresa);
        if (cuentaCliente == null) {
            throw new IncomeException("No se encontrÃ³ cuenta contable (tipo factura) "
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

        // â”€â”€ HABER: cuentas de ingreso por grupo de producto (factura relacionada) â”€
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();

        if (detallesFact != null && !detallesFact.isEmpty()) {
            // Calcular proporciÃ³n total de bases imponibles de la factura
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
            // Sin factura relacionada: usar subcero + subtotal en una sola lÃ­nea genÃ©rica
            // Intentar obtener cuenta del primer grupo del facturador
            throw new IncomeException(
                    "La Nota de DÃ©bito no tiene factura relacionada con detalles de producto. "
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

        // â”€â”€ HABER: IVA (si aplica) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (nd.getvIVA() != null && nd.getvIVA() > 0) {
            // cÃ³digo SRI del IVA: porcentaje pIVA â†’ mapeamos a cÃ³digo SRI
            String codigoIVASRI = mapPorcentajeNDaCodigo(nd.getpIVA());
            PlanCuenta cuentaIVA = obtenerCuentaIVA(codigoIVASRI);
            if (cuentaIVA == null) {
                throw new IncomeException(
                        "No se encontrÃ³ cuenta contable para el IVA cÃ³digo SRI: " + codigoIVASRI
                        + ". Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI â†’ IVA (categorÃ­a 17).");
            }
            DetalleAsiento haberIVA = new DetalleAsiento();
            haberIVA.setPlanCuenta(cuentaIVA);
            haberIVA.setNumeroCuenta(cuentaIVA.getCuentaContable());
            haberIVA.setNombreCuenta(cuentaIVA.getNombre());
            haberIVA.setDescripcion("ND IVA cÃ³digo SRI: " + codigoIVASRI);
            haberIVA.setValorDebe(0.0);
            haberIVA.setValorHaber(nd.getvIVA());
            lineas.add(haberIVA);
        }

        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    /** Mapea el porcentaje de IVA (Double: 0,5,8,15) al cÃ³digo SRI de TSRI. */
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
            System.err.println("âš  No se pudieron cargar detalles de factura para ND: " + e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------------
    // validarCuentasContablesLiquidacion
    // La liquidación de compra emitida (CXC) no tiene asiento propio: al
    // autorizarse crea un documento CXP (PGS.LQCC) y se contabiliza como
    // liquidación recibida (LIQUIDACIONES_COMPRA_RECIBIDAS). Esta validación
    // corre ANTES de emitir, con los mismos criterios que esa recepción,
    // para no descubrir una cuenta faltante después de que el SRI ya
    // autorizó el comprobante (momento en el que ya no se puede revertir).
    // ---------------------------------------------------------------

    @Override
    public List<String> validarCuentasContablesLiquidacion(
            com.saa.model.cxc.LiquidacionCompra liquidacion,
            List<com.saa.model.cxc.DetalleLiquidacionCompra> detalles,
            Long idEmpresa) throws Throwable {

        List<String> errores = new ArrayList<>();

        // 1. Validar cuenta CxP del proveedor/prestador — ESTRICTO, sin el
        // fallback "sin filtro de rol" de PersonaCuentaContableDaoServiceImpl
        // .selectByTitularRolTipoCuenta. Ese fallback (pensado para datos
        // antiguos sin rubroRolPersonaH poblado) puede devolver una cuenta
        // de CLIENTE cuando se pide la de PROVEEDOR: verificado en un caso
        // real donde tomó "1.4.90.10 - ARRIENDOS CUENTAS POR COBRAR" (una
        // cuenta de ingreso) como si fuera la CxP del proveedor — el asiento
        // cuadra igual, pero contra la cuenta equivocada, y nadie se entera
        // porque la validación decía "OK". Aquí NO se tolera: si no hay una
        // fila con el rol PROVEEDOR explícito, se bloquea la emisión.
        if (liquidacion.getTitular() == null) {
            errores.add("La liquidación de compra no tiene proveedor/prestador asignado.");
        } else if (!existeCuentaConRolEstricto(liquidacion.getTitular().getCodigo(), idEmpresa,
                Long.valueOf(1L), RolPersona.PROVEEDOR)) {
            String identificacion = liquidacion.getTitular().getIdentificacion();
            errores.add("El titular " + liquidacion.getTitular().getNombre()
                    + " (" + (identificacion != null ? identificacion : "sin identificación")
                    + ") no tiene cuenta contable de proveedor configurada. Parametrícela en"
                    + " Titular → Cuentas contables antes de emitir.");
        }

        // 2. Validar cuenta de IVA crédito tributario por cada porcentaje usado
        if (detalles != null) {
            java.util.Set<String> ivaValidados = new java.util.HashSet<>();
            for (com.saa.model.cxc.DetalleLiquidacionCompra d : detalles) {
                if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getPorcentajeIVA() != null) {
                    String codSRI = mapPorcentajeIVAaCodigo(d.getPorcentajeIVA());
                    if (!ivaValidados.contains(codSRI)) {
                        ivaValidados.add(codSRI);
                        PlanCuenta cuentaIVA = obtenerCuentaIVACxpPorCodigo(parseLongSafe(codSRI)); // P12: tabla 17, sin respaldo en cxc
                        if (cuentaIVA == null) {
                            errores.add("El IVA " + d.getPorcentajeIVA() + "% (código SRI: " + codSRI
                                    + ") no tiene cuenta de crédito tributario configurada. "
                                    + "Configure la cuenta en Compras → Tipos SRI.");
                        }
                    }
                }
            }
        }

        // 3. Validar producto y cuenta del grupo de cada detalle — sin
        //    clasificar, PRODUCTOS_SIN_CLASIFICAR es bloqueante en emisión
        //    (a diferencia de la recepción por SRI, donde puede quedar null).
        List<String> productosSinClasificar = new ArrayList<>();
        if (detalles != null) {
            java.util.Set<Long> gruposValidados = new java.util.HashSet<>();
            for (com.saa.model.cxc.DetalleLiquidacionCompra d : detalles) {
                String desc = "'" + (d.getDescripcion() != null ? d.getDescripcion() : "sin descripción") + "'";
                if (d.getProducto() == null) {
                    productosSinClasificar.add(desc);
                } else if (d.getProducto().getGrupoProducto() == null) {
                    productosSinClasificar.add("'" + d.getProducto().getNombre() + "' (sin grupo)");
                } else {
                    Long idGrupo = d.getProducto().getGrupoProducto().getCodigo();
                    if (!gruposValidados.contains(idGrupo)) {
                        gruposValidados.add(idGrupo);
                        if (d.getProducto().getGrupoProducto().getPlanCuenta() == null) {
                            errores.add("El grupo de producto '" + d.getProducto().getGrupoProducto().getNombre()
                                    + "' no tiene cuenta contable asignada. "
                                    + "Configure la cuenta en Compras → Grupos de Producto.");
                        }
                    }
                }
            }
        }
        if (!productosSinClasificar.isEmpty()) {
            errores.add("PRODUCTOS_SIN_CLASIFICAR: los siguientes detalles no tienen producto "
                    + "clasificado, y la liquidación de compra emitida no admite productos "
                    + "sin clasificar: " + productosSinClasificar);
        }

        return errores;
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
            errores.add("La retenciÃ³n no tiene proveedor asignado.");
        } else {
            PlanCuenta cuentaProveedor = obtenerCuentaProveedor(
                    retencion.getProveedor().getCodigo(), idEmpresa);
            if (cuentaProveedor == null) {
                errores.add("El proveedor '" + retencion.getProveedor().getNombre()
                        + "' (ID: " + retencion.getProveedor().getCodigo()
                        + ") no tiene cuenta contable configurada. "
                        + "Configure la cuenta en TesorerÃ­a â†’ Persona â†’ Cuentas Contables "
                        + "(Tipo: Facturas, Rol: Proveedor).");
            }
        }

        // 2. Validar cuenta contable de cada cÃ³digo de retenciÃ³n en TSRI
        //    La clave de deduplicaciÃ³n es (codImpuesto, codRetencion): el mismo
        //    cÃ³digo puede existir en la categorÃ­a de Renta y en la de IVA con
        //    cuentas distintas.
        if (detalles != null) {
            java.util.Set<String> codigosValidados = new java.util.HashSet<>();
            for (com.saa.model.cxc.DetalleRetencion d : detalles) {
                String cod = d.getCodRetencion();
                String codImpuesto = d.getCodImpuesto();
                if (cod == null || cod.isEmpty()) {
                    continue;
                }
                String clave = codImpuesto + "|" + cod;
                if (codigosValidados.contains(clave)) {
                    continue;
                }
                codigosValidados.add(clave);

                if (lsriPorCodImpuesto(codImpuesto, cod) == null) {
                    errores.add("El detalle con cÃ³digo de retenciÃ³n '" + cod
                            + "' no indica el tipo de impuesto (codImpuesto): se espera "
                            + "'1' (Renta) o '2' (IVA), y llegÃ³ '" + codImpuesto + "'.");
                    continue;
                }
                PlanCuenta pc = obtenerCuentaRetencionEmitida(codImpuesto, cod);
                if (pc == null) {
                    errores.add("El cÃ³digo de retenciÃ³n '" + cod + "' ("
                            + ("1".equals(codImpuesto) ? "Renta" : "IVA")
                            + ") no tiene cuenta contable asignada en TSRI. "
                            + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI.");
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

        // 1. Cargar la retenciÃ³n
        com.saa.model.cxc.Retencion retencion =
                em.find(com.saa.model.cxc.Retencion.class, idRetencion);
        if (retencion == null) {
            throw new IncomeException("No se encontrÃ³ la RetenciÃ³n con ID: " + idRetencion);
        }

        // 2. Cargar detalles activos
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxc.DetalleRetencion> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencion d WHERE d.retencion.id = :id AND d.estado = 1")
                .setParameter("id", idRetencion)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La RetenciÃ³n " + idRetencion + " no tiene detalles activos.");
        }

        // 3. Construir lÃ­neas del asiento
        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ HABER: una lÃ­nea por detalle de retenciÃ³n, cuenta desde TSRI.codRetencion â”€â”€
        double totalRetenido = 0.0;
        for (com.saa.model.cxc.DetalleRetencion det : detalles) {
            String codRetencion = det.getCodRetencion();
            String codImpuesto  = det.getCodImpuesto();
            PlanCuenta pcReten = obtenerCuentaRetencionEmitida(codImpuesto, codRetencion);
            if (pcReten == null) {
                throw new IncomeException(
                        "No se encontrÃ³ cuenta contable para el cÃ³digo de retenciÃ³n '"
                        + codRetencion + "' (codImpuesto=" + codImpuesto + ") en TSRI. "
                        + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI.");
            }
            double valor = nvl(det.getValorReten());
            totalRetenido += valor;

            DetalleAsiento lineaHaber = new DetalleAsiento();
            lineaHaber.setPlanCuenta(pcReten);
            lineaHaber.setNumeroCuenta(pcReten.getCuentaContable());
            lineaHaber.setNombreCuenta(pcReten.getNombre());
            lineaHaber.setDescripcion("RetenciÃ³n cÃ³digo " + codRetencion
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(det.getBaseImponible()))
                    + " | " + nvl2(det.getPorcentajeReten()) + "%");
            lineaHaber.setValorDebe(0.0);
            lineaHaber.setValorHaber(valor);
            lineas.add(lineaHaber);
        }

        // â”€â”€ DEBE: cuenta CxP del proveedor por el total retenido â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (retencion.getProveedor() == null) {
            throw new IncomeException("La RetenciÃ³n no tiene proveedor asignado.");
        }
        PlanCuenta cuentaProveedor = obtenerCuentaProveedor(
                retencion.getProveedor().getCodigo(), idEmpresa);
        if (cuentaProveedor == null) {
            throw new IncomeException("No se encontrÃ³ cuenta contable (tipo factura) "
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
                observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    /**
     * Obtiene la cuenta contable CxP del proveedor (tipoCuenta=1, rol Proveedor).
     * <p>
     * Antes hacÃ­a su propia consulta SIN filtrar por rol, asÃ­ que un titular que
     * es cliente Y proveedor a la vez devolvÃ­a la cuenta del rol equivocado.
     */
    private PlanCuenta obtenerCuentaProveedor(Long codigoTitular, Long idEmpresa) {
        return obtenerCuentaProveedorPorTipo(codigoTitular, idEmpresa, 1L);
    }

    /**
     * Verifica ESTRICTAMENTE que el titular tenga una
     * {@code PersonaCuentaContable} con el rol pedido — a propósito NO usa
     * {@code obtenerCuentaPersona}/{@code PersonaCuentaContableDaoService
     * .selectByTitularRolTipoCuenta}, que cuando no encuentra el rol exacto
     * cae a un fallback "sin filtro de rol" (pensado para datos antiguos sin
     * {@code rubroRolPersonaH} poblado) y puede devolver una cuenta de otro
     * rol — verificado en un caso real: le dio a una liquidación de compra
     * la cuenta de CLIENTE del titular ("ARRIENDOS CUENTAS POR COBRAR") como
     * si fuera su CxP de proveedor. El asiento cuadra igual, pero contra la
     * cuenta equivocada, y sin este chequeo nadie se entera.
     * <p>
     * Medido contra la base: de 87 titulares con cuenta contable, 61 la
     * tienen sólo bajo rol Proveedor, 24 sólo bajo Cliente y 2 bajo ambos —
     * el fallback se dispara para 85 de 87 en cuanto se usa el titular en
     * el rol contrario. Público (no {@code private}) para que
     * {@code ProcesoCargaDocumentosServiceImpl} (que ya inyecta
     * {@code AsientoContableService}) lo reutilice sin duplicar la consulta
     * ni tocar {@code PersonaCuentaContableDaoService} — el fallback sigue
     * sirviendo, sin cambios, a cualquier llamador que no pase por aquí.
     * Usado hoy por {@link #validarCuentasContablesLiquidacion},
     * {@link #validarCuentasContables} (Factura) y
     * {@code ProcesoCargaDocumentosServiceImpl.verificarCuentaContableProveedor}
     * (carga automática de CxP).
     * @param codigoTitular : Código del titular
     * @param idEmpresa     : Empresa contable
     * @param tipoCuenta    : 1=Facturas, 2=Anticipos, 3=Caja/Banco
     * @param rolPersona    : {@link RolPersona#CLIENTE} o {@link RolPersona#PROVEEDOR}
     * @return : true si existe al menos una fila con el rol pedido (rubroRolPersonaH), sin fallback
     */
    @Override
    public boolean existeCuentaConRolEstricto(Long codigoTitular, Long idEmpresa,
            Long tipoCuenta, int rolPersona) {
        try {
            Long total = (Long) em.createQuery(
                    "select count(pcc) from PersonaCuentaContable pcc "
                    + "join pcc.personaRol pr "
                    + "where pr.titular.codigo = :titular and pcc.tipoCuenta = :tipoCuenta "
                    + "and pcc.empresa.codigo = :idEmpresa and pr.rubroRolPersonaH = :rolPersona "
                    + "and pcc.planCuenta is not null")
                    .setParameter("titular", codigoTitular)
                    .setParameter("tipoCuenta", tipoCuenta)
                    .setParameter("idEmpresa", idEmpresa)
                    .setParameter("rolPersona", Long.valueOf(rolPersona))
                    .getSingleResult();
            return total != null && total.longValue() > 0;
        } catch (Exception e) {
            System.err.println("⚠ existeCuentaConRolEstricto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la cuenta contable desde TSRI por su campo CODIGO.
     * Se usa para mapear DetalleRetencion.codRetencion â†’ PlanCuenta.
     */
    /**
     * Busca la cuenta contable en TSRI para un cÃ³digo de retenciÃ³n recibida (CXP).
     * El LSRI correcto depende del tipo de impuesto que viene en el XML del SRI:
     *   codImpuesto = "1" (Renta)  â†’ lsri.tabla = '608'
     *   codImpuesto = "2" (IVA)    â†’ lsri.tabla = '20'
     *
     * @param codImpuesto    valor del tag <codigo> del XML ("1" o "2")
     * @param codRetencion   valor del tag <codigoRetencion> del XML (ej. "320", "10")
     */
    private PlanCuenta obtenerCuentaRetencionCompra(String codImpuesto, String codRetencion) {
        String lsriTabla;
        if ("1".equals(codImpuesto)) {
            lsriTabla = "608"; // RetenciÃ³n de Renta
        } else if ("2".equals(codImpuesto)) {
            lsriTabla = "20";  // RetenciÃ³n de IVA
        } else {
            System.err.println("âš  obtenerCuentaRetencionCompra: codImpuesto desconocido='" + codImpuesto
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
                System.err.println("âš  No se encontrÃ³ cuenta en TSRI: lsri.tabla=" + lsriTabla
                        + " | codRetencion=" + codRetencion);
                return null;
            }
            return (PlanCuenta) result.get(0);
        } catch (Exception e) {
            System.err.println("âš  Error buscando cuenta TSRI lsri=" + lsriTabla
                    + " codRetencion=" + codRetencion + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Busca la cuenta contable en CBR.TSRI para un cÃ³digo de retenciÃ³n emitida
     * (CXC). Equivalente a {@link #obtenerCuentaRetencionCompra(String, String)}
     * pero contra la entidad {@code Tsri} (esquema CBR) en vez de
     * {@code TsriCompra} (esquema PGS).
     * <p>
     * El filtro por {@code lsri.tabla} es obligatorio: TSRI es un catÃ¡logo
     * genÃ©rico donde CODIGO sÃ³lo es Ãºnico DENTRO de su categorÃ­a LSRI. El mismo
     * cÃ³digo (p. ej. '320') existe en varias categorÃ­as, asÃ­ que buscar sÃ³lo por
     * CODIGO devuelve una fila arbitraria â€” y con ella una cuenta contable de
     * otro concepto.
     *
     * @param codImpuesto  : Tipo de impuesto retenido ("1"=Renta, "2"=IVA)
     * @param codRetencion : CÃ³digo de retenciÃ³n del SRI (ej. "320", "10")
     * @return : Cuenta contable configurada, o null si no estÃ¡ configurada
     */
    private PlanCuenta obtenerCuentaRetencionEmitida(String codImpuesto, String codRetencion) {
        String lsriTabla = lsriPorCodImpuesto(codImpuesto, codRetencion);
        if (lsriTabla == null) {
            return null;
        }
        try {
            String sql = "SELECT t.planCuenta FROM Tsri t "
                    + "WHERE t.lsri.tabla = :lsriTabla AND t.codigo = :codigo AND t.estado = 1";
            Query q = em.createQuery(sql);
            q.setParameter("lsriTabla", lsriTabla);
            q.setParameter("codigo", codRetencion);
            q.setMaxResults(1);
            List<?> result = q.getResultList();
            if (result.isEmpty()) {
                System.err.println("âš  No se encontrÃ³ cuenta en CBR.TSRI: lsri.tabla=" + lsriTabla
                        + " | codRetencion=" + codRetencion);
                return null;
            }
            return (PlanCuenta) result.get(0);
        } catch (Exception e) {
            System.err.println("âš  Error buscando cuenta TSRI lsri=" + lsriTabla
                    + " codRetencion=" + codRetencion + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Traduce el cÃ³digo de impuesto del SRI a la categorÃ­a LSRI que agrupa sus
     * cÃ³digos de retenciÃ³n.
     * @param codImpuesto  : "1"=Renta, "2"=IVA
     * @param codRetencion : SÃ³lo para el mensaje de log
     * @return : "608" (Renta), "20" (IVA), o null si el cÃ³digo es desconocido
     */
    private String lsriPorCodImpuesto(String codImpuesto, String codRetencion) {
        if ("1".equals(codImpuesto)) {
            return "608"; // RetenciÃ³n de Renta
        }
        if ("2".equals(codImpuesto)) {
            return "20";  // RetenciÃ³n de IVA
        }
        System.err.println("âš  codImpuesto desconocido='" + codImpuesto
                + "' para codRetencion='" + codRetencion
                + "'. No se puede determinar la categorÃ­a LSRI.");
        return null;
    }

    private String nvl2(Double val) {
        if (val == null) return "0";
        if (val == Math.floor(val)) return String.valueOf(val.intValue());
        return String.format(java.util.Locale.US, "%.2f", val);
    }

    // =========================================================================
    // generarAsientoRetencionV2
    // Estructura idÃ©ntica a generarAsientoRetencion (V1) pero usando
    // DetalleRetencionV2 (que tiene codImpuesto + codRetencion) y la
    // entidad RetencionV2.
    //
    // DEBE:  cuenta CxP del proveedor (PersonaCuentaContable tipoCuenta=1) por
    //        el total retenido.
    // HABER: una lÃ­nea por detalle â†’ cuenta desde Tsri segÃºn (codImpuesto,
    //        codRetencion). Usa obtenerCuentaRetencionEmitida igual que V1:
    //        las retenciones emitidas (CXC) leen CBR.TSRI (no PGS.TSRI), pero
    //        el filtro por lsri.tabla es igual de obligatorio â€” CODIGO sÃ³lo es
    //        Ãºnico dentro de su categorÃ­a LSRI.
    // =========================================================================

    @Override
    public com.saa.model.cnt.Asiento generarAsientoRetencionV2(
            Long idRetencionV2, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario)
            throws Throwable {

        System.out.println("=== generarAsientoRetencionV2 | idRetencionV2=" + idRetencionV2
                + " | empresa=" + idEmpresa + " ===");

        // 1. Cargar la retenciÃ³n V2
        com.saa.model.cxc.RetencionV2 retencion =
                em.find(com.saa.model.cxc.RetencionV2.class, idRetencionV2);
        if (retencion == null) {
            throw new IncomeException("No se encontrÃ³ la RetencionV2 con ID: " + idRetencionV2);
        }

        // 2. Cargar detalles activos
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxc.DetalleRetencionV2> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencionV2 d WHERE d.retencionV2.id = :id AND d.estado = 1")
                .setParameter("id", idRetencionV2)
                .getResultList();

        if (detalles == null || detalles.isEmpty()) {
            throw new IncomeException("La RetencionV2 " + idRetencionV2 + " no tiene detalles activos.");
        }

        // 3. Construir lÃ­neas del asiento
        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ HABER: una lÃ­nea por detalle â†’ cuenta desde Tsri por codRetencion â”€â”€
        double totalRetenido = 0.0;
        for (com.saa.model.cxc.DetalleRetencionV2 det : detalles) {
            String codRetencion = det.getCodRetencion();
            String codImpuesto  = det.getCodImpuesto();
            PlanCuenta pcReten = obtenerCuentaRetencionEmitida(codImpuesto, codRetencion);
            if (pcReten == null) {
                throw new IncomeException(
                        "No se encontrÃ³ cuenta contable para el cÃ³digo de retenciÃ³n '"
                        + codRetencion + "' (codImpuesto=" + codImpuesto + ") en TSRI. "
                        + "Configure la cuenta en FacturaciÃ³n â†’ Tipos SRI.");
            }
            double valor = nvl(det.getValorReten());
            totalRetenido += valor;

            DetalleAsiento lineaHaber = new DetalleAsiento();
            lineaHaber.setPlanCuenta(pcReten);
            lineaHaber.setNumeroCuenta(pcReten.getCuentaContable());
            lineaHaber.setNombreCuenta(pcReten.getNombre());
            lineaHaber.setDescripcion("RetenciÃ³n V2 cÃ³digo " + codRetencion
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(det.getBaseImponible()))
                    + " | " + nvl2(det.getPorcentajeReten()) + "%"
                    + " | Doc: " + (det.getNumDocReten() != null ? det.getNumDocReten() : ""));
            lineaHaber.setValorDebe(0.0);
            lineaHaber.setValorHaber(valor);
            lineas.add(lineaHaber);
        }

        // â”€â”€ DEBE: cuenta CxP del proveedor por el total retenido â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (retencion.getProveedor() == null) {
            throw new IncomeException("La RetencionV2 no tiene proveedor asignado.");
        }
        PlanCuenta cuentaProveedor = obtenerCuentaProveedor(
                retencion.getProveedor().getCodigo(), idEmpresa);
        if (cuentaProveedor == null) {
            throw new IncomeException("No se encontrÃ³ cuenta contable (tipo factura) "
                    + "para el proveedor ID: " + retencion.getProveedor().getCodigo()
                    + " en la empresa: " + idEmpresa);
        }

        DetalleAsiento lineaDebe = new DetalleAsiento();
        lineaDebe.setPlanCuenta(cuentaProveedor);
        lineaDebe.setNumeroCuenta(cuentaProveedor.getCuentaContable());
        lineaDebe.setNombreCuenta(cuentaProveedor.getNombre());
        lineaDebe.setDescripcion("Proveedor: " + retencion.getProveedor().getNombre()
                + " | Ret. V2: " + (retencion.getNumero() != null
                        ? retencion.getNumero() : retencion.getClave()));
        lineaDebe.setValorDebe(totalRetenido);
        lineaDebe.setValorHaber(0.0);
        // Insertar DEBE al inicio
        lineas.add(0, lineaDebe);

        // 4. Generar asiento
        return generarAsiento(idEmpresa, codigoAltTipoAsiento, fechaAsiento,
                observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    // =========================================================================
    // CXP â€” Documentos de Compra (recibidos del proveedor)
    // =========================================================================
    // ESTRUCTURA GENERAL DEL ASIENTO DE COMPRA:
    //   DEBE:  Gasto / Costo por grupo de producto (GrupoProductoPago.planCuenta)
    //          + IVA CrÃ©dito Tributario (Tsri/Lsri, tabla='17')
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
            throw new IncomeException("No se encontrÃ³ FacturaCompra con ID: " + idFacturaCompra);

        // ── Rama intermediario (§5 DISENO-FACTURA-INTERMEDIARIO.md) ───────────
        // Va ANTES que la de reembolso: son mutuamente excluyentes (la marca se
        // decide al registrar, no al subir el XML), pero si algún día se marcaran
        // ambas, el intermediario gana porque su asiento es más simple y explícito
        // sobre la intención (todo el total a una sola cuenta, sin tocar detalles).
        boolean esIntermediario = fc.getEsIntermediario() != null && fc.getEsIntermediario() == 1L;

        if (esIntermediario) {
            return generarAsientoFacturaCompraIntermediario(fc, idEmpresa, codigoAltTipoAsiento,
                    fechaAsiento, observaciones, usuario);
        }

        // ── Rama reembolso de gastos (§8 CAMBIO-REEMBOLSO-GASTOS-BACKEND.md) ──
        // Cuando la factura es reembolso, el DEBE se construye desde los sustentos
        // (PGS.RMBF), NO desde los detalles (PGS.DFCC). Solo cambia la fuente de
        // las líneas de DEBE; cuentas, redondeos, validaciones y HABER se reutilizan.
        boolean esReembolso = fc.getEsReembolso() != null && fc.getEsReembolso() == 1L;

        if (esReembolso) {
            return generarAsientoFacturaCompraReembolso(fc, idEmpresa, codigoAltTipoAsiento,
                    fechaAsiento, observaciones, usuario);
        }

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleFacturaCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleFacturaCompra d WHERE d.factura.id = :id AND d.estado = 1")
                .setParameter("id", idFacturaCompra).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ DEBE: una lÃ­nea por grupo de producto (GrupoProductoPago.planCuenta) â”€â”€
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
                        + "') no tiene grupo asignado. ClasifÃ­quelo antes de registrar la factura.");

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

        // â”€â”€ DEBE: IVA crÃ©dito tributario â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // El VALOR del IVA se toma de la CABECERA de la factura
        // (FacturaCompra.vIVA = <totalConImpuestos> del XML), NO de la sumatoria
        // de los detalles. Los detalles solo determinan QUE cuentas intervienen:
        // se agrupan por codigoIVASRI y la cuenta se busca en PGS.TSRI donde
        // lsri.tabla = '17' y codigo = codigoIVASRI.
        Map<Long, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleFacturaCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getCodigoIVASRI() != null) {
                ivaMap.merge(d.getCodigoIVASRI(), nvl(d.getValorIVA()), Double::sum);
            }
        }
        ivaMap = distribuirIvaCabecera("FacturaCompra", idFacturaCompra, fc.getvIVA(),
                ivaMap, codigoIvaDesdeTarifa(fc.getpIVA()));
        for (Map.Entry<Long, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(e.getKey());
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta de IVA crÃ©dito tributario para cÃ³digo SRI: "
                        + e.getKey() + " (PGS.TSRI lsri.tabla=17). Configure en Compras â†’ Tipos SRI.");
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pcIVA); ln.setNumeroCuenta(pcIVA.getCuentaContable());
            ln.setNombreCuenta(pcIVA.getNombre());
            ln.setDescripcion("IVA crÃ©dito tributario cÃ³digo SRI: " + e.getKey());
            ln.setValorDebe(e.getValue()); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // â”€â”€ HABER: CxP proveedor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (fc.getTitular() == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(fc.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + fc.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada (Tipo 1, Rol Proveedor).");
        // El HABER cuadra contra el importeTotal de la cabecera (<importeTotal> del
        // XML, FacturaCompra.total), no contra la suma del DEBE: sumar el DEBE
        // hacia el HABER hace que el asiento cuadre siempre sin importar si está
        // clasificando la cuenta correcta — es undetectable en silencio. La
        // diferencia entre importeTotal y el DEBE construido (redondeo legítimo,
        // o un ICE/propina no clasificado) se resuelve en agregarDiferenciaRedondeoSri
        // (docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §3).
        double totalDebe = lineas.stream().mapToDouble(l -> nvl(l.getValorDebe())).sum();
        double importeTotal = redondear2(nvl(fc.getTotal()));
        agregarDiferenciaRedondeoSri(lineas, importeTotal, totalDebe,
                "FacturaCompra " + idFacturaCompra + " (N° " + fc.getNumero() + ")", idEmpresa);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Proveedor: " + fc.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(importeTotal);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    /**
     * Genera el asiento de una factura de INTERMEDIARIO: el fondo no es dueño del
     * gasto, es un movimiento de paso entre un arrendatario y el gasto real, así
     * que el asiento ignora detalles e impuestos y manda el total completo —con
     * impuestos incluidos— a la cuenta del grupo del producto elegido al registrar
     * (docs/logica-negocio/cxp/DISENO-FACTURA-INTERMEDIARIO.md §5).
     * <p>
     * DEBE = cuenta del grupo de {@code fc.getIdProductoIntermediario()}, por {@code fc.getTotal()}.
     * HABER = CxP proveedor, por el mismo {@code fc.getTotal()}.
     * <p>
     * <b>No se llama a {@link #agregarDiferenciaRedondeoSri}</b>: las dos líneas salen
     * del mismo número, así que la diferencia siempre sería cero — agregar el
     * helper sugeriría que hay algo que reconciliar donde no lo hay (§5.1 del
     * diseño; es la misma propiedad que hace correcta a la ND de compra, ver
     * DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §6.2).
     * <p>
     * Repite las validaciones de producto/grupo/cuenta que ya corrió
     * {@code ProcesoCargaDocumentosServiceImpl.registrarFacturaCompra} al registrar:
     * el asiento puede generarse por otro camino (recontabilización, corrección)
     * que no pase por el registro.
     * @param fc : FacturaCompra con esIntermediario=1 e idProductoIntermediario poblado
     * @throws IncomeException : si falta el producto, no existe, o su grupo/cuenta no está configurada
     */
    private com.saa.model.cnt.Asiento generarAsientoFacturaCompraIntermediario(
            com.saa.model.cxp.FacturaCompra fc, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        Long idFacturaCompra = fc.getId();
        System.out.println("  [intermediario] generarAsientoFacturaCompraIntermediario | id=" + idFacturaCompra);

        Long idProducto = fc.getIdProductoIntermediario();
        if (idProducto == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra
                    + " está marcada como intermediario pero no tiene idProductoIntermediario.");
        com.saa.model.cxp.ProductoPago producto = em.find(com.saa.model.cxp.ProductoPago.class, idProducto);
        if (producto == null)
            throw new IncomeException("Producto ID " + idProducto + " (intermediario de FacturaCompra "
                    + idFacturaCompra + ") no encontrado en BD.");
        com.saa.model.cxp.GrupoProductoPago grupo = producto.getGrupoProducto();
        if (grupo == null)
            throw new IncomeException("Producto '" + producto.getNombre() + "' (intermediario de FacturaCompra "
                    + idFacturaCompra + ") no tiene grupo asignado.");
        PlanCuenta cuentaGrupo = grupo.getPlanCuenta();
        if (cuentaGrupo == null)
            throw new IncomeException("GrupoProductoPago '" + grupo.getNombre() + "' (producto: '"
                    + producto.getNombre() + "', intermediario de FacturaCompra " + idFacturaCompra
                    + ") no tiene cuenta contable.");

        if (fc.getTitular() == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(fc.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + fc.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada (Tipo 1, Rol Proveedor).");

        double importeTotal = redondear2(nvl(fc.getTotal()));

        List<DetalleAsiento> lineas = new ArrayList<>();

        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaGrupo); debe.setNumeroCuenta(cuentaGrupo.getCuentaContable());
        debe.setNombreCuenta(cuentaGrupo.getNombre());
        debe.setDescripcion("Factura intermediario: " + grupo.getNombre());
        debe.setValorDebe(importeTotal); debe.setValorHaber(0.0);
        lineas.add(debe);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Proveedor (intermediario): " + fc.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(importeTotal);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    /**
     * Genera el asiento de una factura de reembolso de gastos.
     * Las líneas de DEBE se construyen desde los sustentos (PGS.RMBF),
     * agrupando por GrupoProductoPago igual que lo hace el método estándar con DFCC.
     * DEBE(grupo) = sum(baseImponibleCero + baseImponibleGravada + valorIce) de las filas RMBF.
     * IVA crédito tributario = sum(RMBF.valorIva).
     * HABER = CxP del proveedor por {@code fc.getTotal()} (importeTotal de la cabecera), con la
     * diferencia contra la suma del DEBE resuelta por {@link #agregarDiferenciaRedondeoSri}
     * (docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §3 y §6).
     */
    private com.saa.model.cnt.Asiento generarAsientoFacturaCompraReembolso(
            com.saa.model.cxp.FacturaCompra fc, Long idEmpresa, int codigoAltTipoAsiento,
            java.time.LocalDate fechaAsiento, String observaciones, String usuario) throws Throwable {

        Long idFacturaCompra = fc.getId();
        System.out.println("  [reembolso] generarAsientoFacturaCompraReembolso | id=" + idFacturaCompra);

        List<com.saa.model.cxp.ReembolsoFacturaCompra> reembolsos = em.createNamedQuery(
                "ReembolsoFacturaCompraByFactura", com.saa.model.cxp.ReembolsoFacturaCompra.class)
                .setParameter("idFactura", idFacturaCompra).getResultList();

        if (reembolsos == null || reembolsos.isEmpty())
            throw new IncomeException("FacturaCompra " + idFacturaCompra
                    + " es reembolso pero no tiene registros en PGS.RMBF activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: una línea por grupo de producto de los sustentos ───────────
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();
        double totalValorIva = 0.0;

        for (com.saa.model.cxp.ReembolsoFacturaCompra r : reembolsos) {
            if (r.getProducto() == null)
                throw new IncomeException("Un sustento de reembolso (RMBF id=" + r.getId()
                        + ") no tiene producto asignado. Clasifíquelo antes de contabilizar.");

            com.saa.model.cxp.ProductoPago producto =
                    em.find(com.saa.model.cxp.ProductoPago.class, r.getProducto());
            if (producto == null)
                throw new IncomeException("Producto ID " + r.getProducto() + " del sustento RMBF "
                        + r.getId() + " no encontrado en BD.");
            if (producto.getGrupoProducto() == null)
                throw new IncomeException("Producto '" + producto.getNombre()
                        + "' del sustento no tiene grupo asignado.");
            com.saa.model.cxp.GrupoProductoPago grupo = producto.getGrupoProducto();
            if (grupo.getPlanCuenta() == null)
                throw new IncomeException("GrupoProductoPago '" + grupo.getNombre()
                        + "' no tiene cuenta contable asignada.");

            Long idGrupo = grupo.getCodigo();
            // DEBE del sustento = bases + ICE (el IVA va a su propia línea)
            double baseGrupo = nvl(r.getBaseImponibleCero()) + nvl(r.getBaseImponibleGravada())
                             + nvl(r.getValorIce());
            subtotalPorGrupo.merge(idGrupo, baseGrupo, Double::sum);
            cuentaPorGrupo.putIfAbsent(idGrupo, grupo.getPlanCuenta());
            nombreGrupo.putIfAbsent(idGrupo, grupo.getNombre());
            totalValorIva += nvl(r.getValorIva());
        }

        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pc); ln.setNumeroCuenta(pc.getCuentaContable());
            ln.setNombreCuenta(pc.getNombre());
            ln.setDescripcion("Reembolso gastos: " + nombreGrupo.get(idGrupo));
            ln.setValorDebe(subtotalPorGrupo.get(idGrupo)); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // ── DEBE: IVA crédito tributario (sum RMBF.valorIva) ─────────────────
        totalValorIva = redondear2(totalValorIva);
        if (totalValorIva > 0) {
            // Usar el código SRI del IVA desde la tarifa de la factura
            String codigoIvaSri = codigoIvaTextoDesdeTarifa(fc.getpIVA());
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(
                    parseLongSafe(codigoIvaSri));
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta de IVA crédito tributario (código SRI: "
                        + codigoIvaSri + ") en PGS.TSRI lsri.tabla=17.");
            DetalleAsiento lnIva = new DetalleAsiento();
            lnIva.setPlanCuenta(pcIVA); lnIva.setNumeroCuenta(pcIVA.getCuentaContable());
            lnIva.setNombreCuenta(pcIVA.getNombre());
            lnIva.setDescripcion("IVA crédito tributario reembolso código SRI: " + codigoIvaSri);
            lnIva.setValorDebe(totalValorIva); lnIva.setValorHaber(0.0);
            lineas.add(lnIva);
        }

        // ── HABER: CxP proveedor ──────────────────────────────────────────────
        if (fc.getTitular() == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra + " no tiene proveedor.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(fc.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El proveedor '" + fc.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada (Tipo 1, Rol Proveedor).");
        // Mismo cuadre contra importeTotal que generarAsientoFacturaCompra (§3 y §6
        // de DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md): fc es la misma FacturaCompra
        // de origen, así que fc.getTotal() es la misma fuente de importeTotal.
        double totalDebe = lineas.stream().mapToDouble(l -> nvl(l.getValorDebe())).sum();
        double importeTotal = redondear2(nvl(fc.getTotal()));
        agregarDiferenciaRedondeoSri(lineas, importeTotal, totalDebe,
                "FacturaCompra (reembolso) " + idFacturaCompra + " (N° " + fc.getNumero() + ")", idEmpresa);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Proveedor reembolso: " + fc.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(importeTotal);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas,
                Long.valueOf(com.saa.rubros.ModuloSistema.CUENTAS_POR_PAGAR), ModuloOrigenAsiento.CXP);
    }

    /** Convierte un String a Long con fallback 0L (evita NPE en la línea de IVA del reembolso). */
    private static Long parseLongSafe(String s) {
        if (s == null || s.isEmpty()) return 0L;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return 0L; }
    }

    /**
     * Agrega, si corresponde, la línea de ajuste por diferencia de redondeo del
     * SRI entre {@code importeTotal} (cabecera del XML) y {@code totalDebe} (suma
     * de las líneas DEBE ya construidas: gasto por grupo + IVA), o aborta si la
     * diferencia es demasiado grande para ser redondeo — ver
     * {@link #TOLERANCIA_MAXIMA_REDONDEO_SRI} y
     * docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §4.
     * <p>
     * Usado por {@code generarAsientoFacturaCompra} y
     * {@code generarAsientoFacturaCompraReembolso}: mismo mecanismo, misma
     * fuente de {@code importeTotal} ({@code FacturaCompra.total}), solo cambia
     * de dónde sale {@code totalDebe}.
     * @param lineas            : Líneas del asiento en construcción; se le agrega la línea de
     *                            ajuste acá si corresponde (mutación intencional)
     * @param importeTotal      : {@code <importeTotal>} del XML, ya redondeado a 2 decimales
     * @param totalDebe         : Suma de las líneas DEBE ya construidas
     * @param etiquetaDocumento : Identificación del documento, para el mensaje de error
     * @throws IncomeException  : Si {@code |diferencia|} supera {@link #TOLERANCIA_MAXIMA_REDONDEO_SRI}
     *                            (puede ser ICE o propina sin clasificar, no redondeo), o si hace falta
     *                            la línea de ajuste y la plantilla de compras de la empresa no la tiene
     *                            (o su cuenta no existe o está inactiva)
     * @param idEmpresa         : Empresa contable del asiento (P5: la cuenta se busca en SU plantilla)
     */
    private void agregarDiferenciaRedondeoSri(List<DetalleAsiento> lineas, double importeTotal,
            double totalDebe, String etiquetaDocumento, Long idEmpresa) throws Throwable {

        double diferencia = redondear2(importeTotal - totalDebe);
        double absDiferencia = Math.abs(diferencia);

        if (absDiferencia > TOLERANCIA_MAXIMA_REDONDEO_SRI) {
            throw new IncomeException(etiquetaDocumento + ": el importeTotal ($"
                    + String.format(java.util.Locale.US, "%.2f", importeTotal)
                    + ") difiere de la suma contabilizada ($"
                    + String.format(java.util.Locale.US, "%.2f", totalDebe)
                    + ") en $" + String.format(java.util.Locale.US, "%.2f", diferencia)
                    + ", mas de la tolerancia de redondeo ($"
                    + String.format(java.util.Locale.US, "%.2f", TOLERANCIA_MAXIMA_REDONDEO_SRI)
                    + "). Puede ser ICE, propina, u otro rubro sin clasificar: revise la factura "
                    + "antes de contabilizar.");
        }
        if (absDiferencia < TOLERANCIA_MINIMA_REDONDEO) {
            return;
        }

        PlanCuenta cuentaAjuste = obtenerCuentaDiferenciaRedondeoSri(idEmpresa, etiquetaDocumento);
        DetalleAsiento ajuste = new DetalleAsiento();
        ajuste.setPlanCuenta(cuentaAjuste);
        ajuste.setNumeroCuenta(cuentaAjuste.getCuentaContable());
        ajuste.setNombreCuenta(cuentaAjuste.getNombre());
        ajuste.setDescripcion("Diferencia por redondeo SRI");
        if (diferencia > 0) {
            ajuste.setValorDebe(diferencia);
            ajuste.setValorHaber(0.0);
        } else {
            ajuste.setValorDebe(0.0);
            ajuste.setValorHaber(absDiferencia);
        }
        lineas.add(ajuste);
    }

    // =========================================================================
    // IVA de los documentos de compra: SIEMPRE el de la cabecera del XML
    // =========================================================================
    // El asiento debe registrar el IVA que el SRI autorizo en la cabecera
    // (<totalConImpuestos>), no la sumatoria de los detalles: cuando el emisor
    // calcula y redondea el impuesto linea por linea ambas cifras difieren en
    // centavos, y con la sumatoria el asiento no coincide con el <importeTotal>
    // que realmente se le paga al proveedor.
    //
    // Los detalles siguen siendo la fuente de QUE cuentas intervienen. Aplica a
    // Factura, Nota de Credito, Nota de Debito y Liquidacion de compra.
    // =========================================================================

    /**
     * Reparte el IVA declarado en la cabecera de un documento de compra entre
     * los codigos de IVA del SRI que aparecen en sus detalles.
     *
     * El total de la cabecera se reparte proporcionalmente al IVA de cada
     * codigo y el residuo del redondeo se carga al codigo de mayor valor, de
     * modo que lo repartido suma exactamente el IVA de la cabecera.
     *
     * @param documento     : Etiqueta del documento para las trazas (ej. "FacturaCompra")
     * @param idDocumento   : Id del documento, para las trazas
     * @param ivaCabecera   : IVA de la cabecera (vIVA). Nulo = no se registro al cargar
     * @param ivaDetalle    : IVA acumulado por codigo SRI segun los detalles
     * @param codigoTarifa  : Codigo SRI deducido de la tarifa de cabecera, para
     *                        el caso en que los detalles no desglosen el IVA
     * @return              : Mapa codigo SRI -> valor de IVA a registrar
     */
    private <K> Map<K, Double> distribuirIvaCabecera(
            String documento, Long idDocumento, Double ivaCabecera,
            Map<K, Double> ivaDetalle, K codigoTarifa) {

        // Cabecera sin IVA registrado (XML sin <totalImpuesto> de IVA, o
        // documentos cargados antes de este cambio) -> conservar los detalles.
        if (ivaCabecera == null) {
            System.out.println("  IVA: " + documento + " " + idDocumento + " sin IVA en cabecera; "
                    + "se usa la sumatoria de los detalles.");
            return ivaDetalle;
        }

        double ivaCab = redondear2(nvl(ivaCabecera));
        double ivaDet = 0.0;
        for (Double v : ivaDetalle.values()) ivaDet += nvl(v);
        ivaDet = redondear2(ivaDet);

        // La cabecera manda: si declara 0.00 el asiento no lleva linea de IVA.
        if (ivaCab <= 0.0) {
            if (ivaDet > 0.0)
                System.out.println("  IVA: " + documento + " " + idDocumento + " cabecera 0.00 vs "
                        + "detalles " + ivaDet + "; el asiento no registra IVA.");
            return new LinkedHashMap<>();
        }

        // Cabecera con IVA pero ningun detalle lo desglosa -> usar el codigo
        // deducido de la tarifa de la cabecera.
        if (ivaDetalle.isEmpty()) {
            System.out.println("  IVA: " + documento + " " + idDocumento + " sin desglose en los "
                    + "detalles; se registra " + ivaCab + " en el codigo SRI " + codigoTarifa + ".");
            Map<K, Double> unico = new LinkedHashMap<>();
            unico.put(codigoTarifa, ivaCab);
            return unico;
        }

        if (Math.abs(ivaCab - ivaDet) < 0.005) return ivaDetalle;

        System.out.println("  IVA: " + documento + " " + idDocumento + " cabecera=" + ivaCab
                + " vs detalles=" + ivaDet + "; se registra el de la cabecera.");

        // Una sola tarifa -> todo el IVA de cabecera va a esa cuenta.
        if (ivaDetalle.size() == 1) {
            Map<K, Double> unico = new LinkedHashMap<>();
            unico.put(ivaDetalle.keySet().iterator().next(), ivaCab);
            return unico;
        }

        // Varias tarifas -> prorratear y ajustar el residuo en la de mayor valor.
        Map<K, Double> ajustado = new LinkedHashMap<>();
        K codigoMayor = null;
        double valorMayor = -1.0;
        double asignado = 0.0;
        for (Map.Entry<K, Double> e : ivaDetalle.entrySet()) {
            double valor = redondear2(ivaCab * (nvl(e.getValue()) / ivaDet));
            ajustado.put(e.getKey(), valor);
            asignado += valor;
            if (nvl(e.getValue()) > valorMayor) {
                valorMayor  = nvl(e.getValue());
                codigoMayor = e.getKey();
            }
        }
        double residuo = redondear2(ivaCab - asignado);
        if (residuo != 0.0 && codigoMayor != null)
            ajustado.put(codigoMayor, redondear2(ajustado.get(codigoMayor) + residuo));
        return ajustado;
    }

    /**
     * Codigo SRI de IVA (como Long) deducido de la tarifa de la cabecera.
     * @param tarifa : Tarifa de la cabecera (pIVA)
     * @return       : Codigo del SRI; 0 si no se puede deducir
     */
    private Long codigoIvaDesdeTarifa(Double tarifa) {
        try { return Long.valueOf(codigoIvaTextoDesdeTarifa(tarifa)); }
        catch (NumberFormatException nfe) { return 0L; }
    }

    /**
     * Codigo SRI de IVA (como texto) deducido de la tarifa de la cabecera.
     * @param tarifa : Tarifa de la cabecera (pIVA)
     * @return       : Codigo del SRI
     */
    private String codigoIvaTextoDesdeTarifa(Double tarifa) {
        return mapPorcentajeIVAaCodigo(tarifa != null ? Long.valueOf(Math.round(tarifa)) : null);
    }

    /**
     * Redondea a 2 decimales para evitar arrastre de error en punto flotante.
     * @param valor : Valor a redondear
     * @return      : El valor con 2 decimales
     */
    private double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    // ---------------------------------------------------------------
    // generarAsientoNotaCreditoCompra  (inverso de FacturaCompra)
    // ---------------------------------------------------------------
    /**
     * Asiento de una nota de crédito de compra: el ESPEJO del asiento de la factura que modifica (N2, 2026-11-01).
     * Antes acreditaba siempre la cuenta de gasto del grupo de producto de menor código, sin mirar la factura: dos NC
     * de facturas de INTERMEDIARIO acreditaron «servicios básicos» (correo de contabilidad del 29-10).
     *
     * <ul>
     * <li>Se resuelve la factura modificada (proveedor + {@code numDocModificado}, viva). Si no se encuentra, o hay más
     * de una, error claro: nunca una cuenta por defecto.</li>
     * <li><b>Factura de intermediario</b> ({@code FCTCESIN = 1}): DEBE proveedor / HABER cuenta del grupo del producto
     * intermediario de ESA factura, por el total de la NC, sin IVA ni ajuste de redondeo (espejo exacto de
     * {@code generarAsientoFacturaCompraIntermediario}).</li>
     * <li><b>Factura normal:</b> el reverso del gasto va a la cuenta del grupo del producto de cada detalle de la NC. Un
     * detalle sin producto se reparte, en proporción, entre las cuentas de gasto del asiento de la factura modificada
     * (sin IVA ni proveedor). IVA por {@link #obtenerCuentaIVACxpPorCodigo} (tabla 17, sin respaldo).</li>
     * </ul>
     */
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

        if (nc.getTitular() == null)
            throw new IncomeException("NotaCreditoCompra " + idNotaCreditoCompra + " no tiene proveedor.");

        // La factura que la NC modifica decide a qué cuenta se acredita: nunca una cuenta por defecto.
        com.saa.model.cxp.FacturaCompra facturaModificada = resolverFacturaModificadaPorNcCompra(nc, idEmpresa);

        List<DetalleAsiento> lineas = new ArrayList<>();

        // ── DEBE: CxP Proveedor (reduce lo que le debemos) ──────────────────────
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

        // ── Factura de INTERMEDIARIO: espejo exacto, sin IVA y sin redondeo ─────────
        if (facturaModificada.getEsIntermediario() != null && facturaModificada.getEsIntermediario().longValue() == 1L) {
            PlanCuenta cuentaGrupo = cuentaDelGrupoIntermediario(facturaModificada);
            double totalNcInterm = redondear2(nvl(nc.getTotal()));
            debe.setValorDebe(totalNcInterm);
            DetalleAsiento haberInterm = new DetalleAsiento();
            haberInterm.setPlanCuenta(cuentaGrupo); haberInterm.setNumeroCuenta(cuentaGrupo.getCuentaContable());
            haberInterm.setNombreCuenta(cuentaGrupo.getNombre());
            haberInterm.setDescripcion("NC Compra intermediario (reverso de la factura "
                    + facturaModificada.getNumero() + "): " + cuentaGrupo.getNombre());
            haberInterm.setValorDebe(0.0); haberInterm.setValorHaber(totalNcInterm);
            lineas.add(haberInterm);
            return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                    fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
        }

        // ── HABER: reverso del gasto, por la cuenta del grupo del producto de cada detalle ──
        Map<Long, Double> baseCuenta = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorId = new LinkedHashMap<>();
        double baseSinProducto = 0.0;
        for (com.saa.model.cxp.DetalleNotaCreditoCompra d : detalles) {
            if (d.getProducto() == null) {
                baseSinProducto += nvl(d.getSubTotal());
                continue;
            }
            com.saa.model.cxp.ProductoPago producto = em.find(com.saa.model.cxp.ProductoPago.class, d.getProducto());
            if (producto == null)
                throw new IncomeException("Producto ID " + d.getProducto() + " del detalle '" + d.getDescripcion()
                        + "' de la NC de compra no encontrado en BD.");
            com.saa.model.cxp.GrupoProductoPago grupo = producto.getGrupoProducto();
            if (grupo == null)
                throw new IncomeException("Producto ID " + d.getProducto() + " ('" + producto.getNombre()
                        + "') no tiene grupo asignado. Clasifíquelo antes de contabilizar la NC.");
            if (grupo.getPlanCuenta() == null)
                throw new IncomeException("GrupoProductoPago '" + grupo.getNombre() + "' no tiene cuenta contable.");
            Long idCuenta = grupo.getPlanCuenta().getCodigo();
            baseCuenta.merge(idCuenta, nvl(d.getSubTotal()), Double::sum);
            cuentaPorId.putIfAbsent(idCuenta, grupo.getPlanCuenta());
        }
        baseSinProducto = redondear2(baseSinProducto);
        if (baseSinProducto > 0.0) {
            // Detalle sin producto: se reparte en proporción entre las cuentas de gasto del asiento de la factura modificada.
            Map<PlanCuenta, Double> gastoFactura = cuentasDeGastoDelAsientoDeFactura(facturaModificada, cuentaProv);
            double totalGasto = 0.0;
            for (Double v : gastoFactura.values()) totalGasto += v;
            if (gastoFactura.isEmpty() || totalGasto <= 0.0)
                throw new IncomeException("La NC de compra " + nc.getNumero() + " tiene detalle(s) sin producto ($" + baseSinProducto
                        + ") y no se pudo repartir entre las cuentas de gasto de la factura modificada "
                        + facturaModificada.getNumero() + " (sin asiento de gasto). Asigne un producto a esos detalles.");
            double asignado = 0.0;
            int i = 0;
            for (Map.Entry<PlanCuenta, Double> e : gastoFactura.entrySet()) {
                i++;
                double parte = (i == gastoFactura.size())
                        ? redondear2(baseSinProducto - asignado)
                        : redondear2(baseSinProducto * e.getValue() / totalGasto);
                asignado += parte;
                Long idCuenta = e.getKey().getCodigo();
                baseCuenta.merge(idCuenta, parte, Double::sum);
                cuentaPorId.putIfAbsent(idCuenta, e.getKey());
            }
        }
        for (Map.Entry<Long, Double> e : baseCuenta.entrySet()) {
            double valor = redondear2(e.getValue());
            if (valor <= 0.0) continue;
            PlanCuenta pc = cuentaPorId.get(e.getKey());
            DetalleAsiento haberGasto = new DetalleAsiento();
            haberGasto.setPlanCuenta(pc); haberGasto.setNumeroCuenta(pc.getCuentaContable());
            haberGasto.setNombreCuenta(pc.getNombre());
            // EXACTAMENTE «NC Compra reverso gasto», sin sufijo: el cargador histórico C11 identifica esta línea por equals()
            // (CargaHistoricaDocumentoServiceImpl.DESCRIPCION_HABER_GASTO_NC_COMPRA). No cambiar el texto sin avisar al cuadre.
            haberGasto.setDescripcion("NC Compra reverso gasto");
            haberGasto.setValorDebe(0.0); haberGasto.setValorHaber(valor);
            lineas.add(haberGasto);
        }

        // ── HABER: IVA crédito tributario reverso ───────────────────────────────
        // El VALOR sale de la cabecera de la NC (vIVA); los detalles solo definen qué cuentas intervienen.
        Map<String, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleNotaCreditoCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getPorcentajeIVA() != null) {
                String codSRI = mapPorcentajeIVAaCodigo(d.getPorcentajeIVA());
                ivaMap.merge(codSRI, nvl(d.getValorIVA()), Double::sum);
            }
        }
        ivaMap = distribuirIvaCabecera("NotaCreditoCompra", idNotaCreditoCompra, nc.getvIVA(),
                ivaMap, codigoIvaTextoDesdeTarifa(nc.getpIVA()));
        for (Map.Entry<String, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(parseLongSafe(e.getKey()));
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta de IVA crédito tributario para código SRI: " + e.getKey()
                        + " (PGS.TSRI lsri.tabla=17). Configure en Compras → Tipos SRI.");
            DetalleAsiento haberIVA = new DetalleAsiento();
            haberIVA.setPlanCuenta(pcIVA); haberIVA.setNumeroCuenta(pcIVA.getCuentaContable());
            haberIVA.setNombreCuenta(pcIVA.getNombre());
            haberIVA.setDescripcion("NC IVA crédito tributario reverso código SRI: " + e.getKey());
            haberIVA.setValorDebe(0.0); haberIVA.setValorHaber(e.getValue());
            lineas.add(haberIVA);
        }

        // El DEBE (CxP proveedor) cuadra contra el importeTotal de la cabecera (nc.getTotal()), no contra la suma del
        // HABER: es el espejo de generarAsientoFacturaCompra, con la CxP del lado del DEBE
        // (docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §6.1). agregarDiferenciaRedondeoSri(a, b) pone la
        // diferencia a - b en el DEBE si es positiva y en el HABER si es negativa; acá se le pasa (totalHaber, importeTotalNc)
        // CRUZADO respecto de la factura a propósito.
        double totalHaber = lineas.stream().mapToDouble(l -> nvl(l.getValorHaber())).sum();
        double importeTotalNc = redondear2(nvl(nc.getTotal()));
        agregarDiferenciaRedondeoSri(lineas, totalHaber, importeTotalNc,
                "NotaCreditoCompra " + idNotaCreditoCompra + " (N° " + nc.getNumero() + ")", idEmpresa);
        debe.setValorDebe(importeTotalNc);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    /**
     * N2: la factura de compra que modifica una NC de compra, por proveedor y número (con o sin guiones), en la empresa
     * y viva (no anulada). Cero o más de una: error claro. Misma búsqueda que
     * {@code AplicacionPagoCxpServiceImpl.resolverFacturaCompraPorNumero}, más el filtro de estado.
     */
    private com.saa.model.cxp.FacturaCompra resolverFacturaModificadaPorNcCompra(
            com.saa.model.cxp.NotaCreditoCompra nc, Long idEmpresa) throws Throwable {
        String numero = nc.getNumDocModificado();
        if (numero == null || numero.trim().isEmpty())
            throw new IncomeException("La NC de compra " + nc.getNumero() + " no indica el número de la factura que modifica:"
                    + " no se puede saber a qué cuenta se acredita.");
        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.FacturaCompra> facturas = em.createQuery(
                "SELECT f FROM FacturaCompra f WHERE FUNCTION('replace', f.numero, '-', '') = :numero "
                + "AND f.titular.codigo = :titular AND f.empresa.codigo = :empresa AND f.estado = :vivo")
                .setParameter("numero", numero.trim().replace("-", ""))
                .setParameter("titular", nc.getTitular().getCodigo())
                .setParameter("empresa", idEmpresa)
                .setParameter("vivo", Long.valueOf(Estado.ACTIVO))
                .getResultList();
        if (facturas.isEmpty())
            throw new IncomeException("No existe (viva) la factura de compra N° " + numero + " del proveedor '"
                    + nc.getTitular().getNombre() + "' que modifica la NC " + nc.getNumero() + ". Cargue primero la factura:"
                    + " la NC se contabiliza contra su cuenta, nunca contra una cuenta por defecto.");
        if (facturas.size() > 1)
            throw new IncomeException("Existe más de una factura de compra viva con el número " + numero + " del proveedor '"
                    + nc.getTitular().getNombre() + "'. Revise los documentos duplicados antes de contabilizar la NC "
                    + nc.getNumero() + ".");
        return facturas.get(0);
    }

    /** N2: cuenta del grupo del producto intermediario de la factura, con las mismas validaciones del asiento de la factura. */
    private PlanCuenta cuentaDelGrupoIntermediario(com.saa.model.cxp.FacturaCompra fc) throws Throwable {
        Long idFacturaCompra = fc.getId();
        Long idProducto = fc.getIdProductoIntermediario();
        if (idProducto == null)
            throw new IncomeException("FacturaCompra " + idFacturaCompra
                    + " está marcada como intermediario pero no tiene idProductoIntermediario.");
        com.saa.model.cxp.ProductoPago producto = em.find(com.saa.model.cxp.ProductoPago.class, idProducto);
        if (producto == null)
            throw new IncomeException("Producto ID " + idProducto + " (intermediario de FacturaCompra "
                    + idFacturaCompra + ") no encontrado en BD.");
        com.saa.model.cxp.GrupoProductoPago grupo = producto.getGrupoProducto();
        if (grupo == null)
            throw new IncomeException("Producto '" + producto.getNombre() + "' (intermediario de FacturaCompra "
                    + idFacturaCompra + ") no tiene grupo asignado.");
        if (grupo.getPlanCuenta() == null)
            throw new IncomeException("GrupoProductoPago '" + grupo.getNombre() + "' (producto: '"
                    + producto.getNombre() + "', intermediario de FacturaCompra " + idFacturaCompra
                    + ") no tiene cuenta contable.");
        return grupo.getPlanCuenta();
    }

    /**
     * N2: cuentas de GASTO del asiento de una factura de compra y lo que debita cada una: las líneas de DEBE sin IVA
     * crédito tributario (cuentas de PGS.TSRI tabla 17), sin la cuenta del proveedor y sin la de diferencia por redondeo.
     * Vacío si la factura no tiene asiento.
     */
    private Map<PlanCuenta, Double> cuentasDeGastoDelAsientoDeFactura(com.saa.model.cxp.FacturaCompra fc,
            PlanCuenta cuentaProveedor) {
        Map<PlanCuenta, Double> gasto = new LinkedHashMap<>();
        if (fc.getAsiento() == null || fc.getAsiento().getCodigo() == null)
            return gasto;
        java.util.Set<Long> excluidas = new java.util.HashSet<>();
        if (cuentaProveedor != null) excluidas.add(cuentaProveedor.getCodigo());
        @SuppressWarnings("unchecked")
        List<PlanCuenta> cuentasIva = em.createQuery(
                "SELECT t.planCuenta FROM TsriCompra t WHERE t.lsri.tabla = '17' AND t.planCuenta IS NOT NULL")
                .getResultList();
        for (PlanCuenta c : cuentasIva) excluidas.add(c.getCodigo());
        @SuppressWarnings("unchecked")
        List<DetalleAsiento> lineasFactura = em.createQuery(
                "SELECT d FROM DetalleAsiento d WHERE d.asiento.codigo = :asiento AND d.valorDebe > 0")
                .setParameter("asiento", fc.getAsiento().getCodigo()).getResultList();
        for (DetalleAsiento d : lineasFactura) {
            if (d.getPlanCuenta() == null || excluidas.contains(d.getPlanCuenta().getCodigo())) continue;
            gasto.merge(d.getPlanCuenta(), nvl(d.getValorDebe()), Double::sum);
        }
        return gasto;
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
            throw new IncomeException("No se encontrÃ³ NotaDebitoCompra con ID: " + idNotaDebitoCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleNotaDebitoCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleNotaDebitoCompra d WHERE d.notaDebito.id = :id AND d.estado = 1")
                .setParameter("id", idNotaDebitoCompra).getResultList();

        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ DEBE: gasto adicional (cuenta default de gastos CXP) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Los <motivo> de la ND vienen SIN impuesto, asÃ­ que el IVA se toma de
        // la cabecera (vIVA) y se separa del gasto: antes el total entraba
        // completo a la cuenta de gasto y el crÃ©dito tributario se perdÃ­a.
        double totalND = redondear2(nvl(nd.getTotal()));
        double ivaND   = redondear2(nvl(nd.getvIVA()));
        if (ivaND < 0.0 || ivaND >= totalND) {
            if (ivaND != 0.0)
                System.out.println("  IVA: NotaDebitoCompra " + idNotaDebitoCompra + " cabecera "
                        + ivaND + " incoherente con el total " + totalND
                        + "; el asiento no separa el IVA.");
            ivaND = 0.0;
        }
        double baseND = redondear2(totalND - ivaND);

        // P11: la cuenta de gasto sin clasificar sale de la plantilla de compras (línea GASTO_SIN_CLASIFICAR), no del
        // grupo de producto de menor código.
        PlanCuenta cuentaGasto = obtenerCuentaGastoSinClasificarCxp(idEmpresa,
                "la ND de compra " + (nd.getNumero() != null ? nd.getNumero() : nd.getClave()));
        DetalleAsiento debe = new DetalleAsiento();
        debe.setPlanCuenta(cuentaGasto); debe.setNumeroCuenta(cuentaGasto.getCuentaContable());
        debe.setNombreCuenta(cuentaGasto.getNombre());
        String motivoND = detalles != null && !detalles.isEmpty()
                ? detalles.get(0).getDescripcion() : "Nota de dÃ©bito compra";
        debe.setDescripcion("ND Compra: " + motivoND);
        debe.setValorDebe(baseND); debe.setValorHaber(0.0);
        lineas.add(debe);

        // â”€â”€ DEBE: IVA crÃ©dito tributario de la cabecera â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (ivaND > 0.0) {
            String codIVA = codigoIvaTextoDesdeTarifa(nd.getpIVA());
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(parseLongSafe(codIVA)); // P12: tabla 17, sin respaldo en cxc
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta IVA crÃ©dito tributario para cÃ³digo SRI: "
                        + codIVA + ", requerida por la ND de compra "
                        + (nd.getNumero() != null ? nd.getNumero() : nd.getClave())
                        + ". ConfigÃºrela en Compras â†’ Tipos SRI.");
            DetalleAsiento debeIVA = new DetalleAsiento();
            debeIVA.setPlanCuenta(pcIVA); debeIVA.setNumeroCuenta(pcIVA.getCuentaContable());
            debeIVA.setNombreCuenta(pcIVA.getNombre());
            debeIVA.setDescripcion("ND IVA crÃ©dito tributario cÃ³digo SRI: " + codIVA);
            debeIVA.setValorDebe(ivaND); debeIVA.setValorHaber(0.0);
            lineas.add(debeIVA);
        }

        // â”€â”€ HABER: CxP Proveedor â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXP);
    }

    // ---------------------------------------------------------------
    // generarAsientoLiquidacionCompraCompra  (misma lÃ³gica que FacturaCompra)
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
            throw new IncomeException("No se encontrÃ³ LiquidacionCompraCompra con ID: " + idLiquidacion);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleLiquidacionCompraCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleLiquidacionCompraCompra d WHERE d.liquidacion.id = :id AND d.estado = 1")
                .setParameter("id", idLiquidacion).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("LiquidacionCompraCompra " + idLiquidacion + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();

        // â”€â”€ DEBE: una lÃ­nea por grupo de producto (GrupoProductoPago.planCuenta),
        // igual que generarAsientoFacturaCompra. Los detalles sin producto
        // clasificado (la carga automÃ¡tica del SRI puede dejarlo null; la
        // emisiÃ³n propia lo exige vÃ­a validarCuentasContablesLiquidacion, asÃ­
        // que aquÃ­ NO deberÃ­a pasar) no bloquean el asiento: se agrupan aparte
        // contra la cuenta de gasto default y se advierte, para no dejar un
        // documento ya autorizado por el SRI sin contabilizar.
        Map<Long, Double> subtotalPorGrupo = new LinkedHashMap<>();
        Map<Long, PlanCuenta> cuentaPorGrupo = new LinkedHashMap<>();
        Map<Long, String> nombreGrupo = new LinkedHashMap<>();
        double totalSinClasificar = 0.0;
        List<String> detallesSinClasificar = new ArrayList<>();

        for (com.saa.model.cxp.DetalleLiquidacionCompraCompra d : detalles) {
            com.saa.model.cxp.ProductoPago producto = d.getProducto();
            com.saa.model.cxp.GrupoProductoPago grupo = producto != null ? producto.getGrupoProducto() : null;
            PlanCuenta pc = grupo != null ? grupo.getPlanCuenta() : null;
            if (producto == null || grupo == null || pc == null) {
                totalSinClasificar += nvl(d.getSubTotal());
                detallesSinClasificar.add(d.getDescripcion() != null ? d.getDescripcion() : "Detalle ID " + d.getId());
                continue;
            }
            Long idGrupo = grupo.getCodigo();
            subtotalPorGrupo.merge(idGrupo, nvl(d.getSubTotal()), Double::sum);
            cuentaPorGrupo.putIfAbsent(idGrupo, pc);
            nombreGrupo.putIfAbsent(idGrupo, grupo.getNombre());
        }
        for (Long idGrupo : subtotalPorGrupo.keySet()) {
            PlanCuenta pc = cuentaPorGrupo.get(idGrupo);
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pc); ln.setNumeroCuenta(pc.getCuentaContable());
            ln.setNombreCuenta(pc.getNombre());
            ln.setDescripcion("Gasto liquidaciÃ³n compra: " + nombreGrupo.get(idGrupo));
            ln.setValorDebe(subtotalPorGrupo.get(idGrupo)); ln.setValorHaber(0.0);
            lineas.add(ln);
        }
        if (totalSinClasificar > 0) {
            // P11: la cuenta de gasto sin clasificar sale de la plantilla de compras (línea GASTO_SIN_CLASIFICAR).
            PlanCuenta cuentaGasto = obtenerCuentaGastoSinClasificarCxp(idEmpresa,
                    "la liquidación de compra (detalles sin producto clasificado: " + detallesSinClasificar
                    + "; también puede clasificar los productos)");
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(cuentaGasto); ln.setNumeroCuenta(cuentaGasto.getCuentaContable());
            ln.setNombreCuenta(cuentaGasto.getNombre());
            ln.setDescripcion("Gasto liquidaciÃ³n compra (SIN CLASIFICAR): " + lq.getNumero());
            ln.setValorDebe(totalSinClasificar); ln.setValorHaber(0.0);
            lineas.add(ln);
            System.err.println("âš  PRODUCTOS_SIN_CLASIFICAR en LiquidacionCompraCompra " + idLiquidacion
                    + ": " + detallesSinClasificar + " â€” contabilizados contra la cuenta de gasto default.");
        }

        // â”€â”€ DEBE: IVA crÃ©dito tributario â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // El VALOR sale de la cabecera de la liquidaciÃ³n (vIVA); los detalles
        // solo definen quÃ© cuentas intervienen. Ver distribuirIvaCabecera().
        Map<String, Double> ivaMap = new LinkedHashMap<>();
        for (com.saa.model.cxp.DetalleLiquidacionCompraCompra d : detalles) {
            if (d.getValorIVA() != null && d.getValorIVA() > 0 && d.getPorcentajeIVA() != null) {
                String codSRI = mapPorcentajeIVAaCodigo(d.getPorcentajeIVA());
                ivaMap.merge(codSRI, nvl(d.getValorIVA()), Double::sum);
            }
        }
        ivaMap = distribuirIvaCabecera("LiquidacionCompraCompra", idLiquidacion, lq.getvIVA(),
                ivaMap, codigoIvaTextoDesdeTarifa(lq.getpIVA()));
        for (Map.Entry<String, Double> e : ivaMap.entrySet()) {
            PlanCuenta pcIVA = obtenerCuentaIVACxpPorCodigo(parseLongSafe(e.getKey())); // P12: tabla 17, sin respaldo en cxc
            if (pcIVA == null)
                throw new IncomeException("No hay cuenta IVA crÃ©dito tributario para cÃ³digo SRI: " + e.getKey());
            DetalleAsiento ln = new DetalleAsiento();
            ln.setPlanCuenta(pcIVA); ln.setNumeroCuenta(pcIVA.getCuentaContable());
            ln.setNombreCuenta(pcIVA.getNombre());
            ln.setDescripcion("IVA crÃ©dito tributario liquidaciÃ³n compra cÃ³digo SRI: " + e.getKey());
            ln.setValorDebe(e.getValue()); ln.setValorHaber(0.0);
            lineas.add(ln);
        }

        // â”€â”€ HABER: CxP Proveedor / Prestador â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (lq.getTitular() == null)
            throw new IncomeException("LiquidacionCompraCompra " + idLiquidacion + " no tiene proveedor/titular.");
        PlanCuenta cuentaProv = obtenerCuentaProveedor(lq.getTitular().getCodigo(), idEmpresa);
        if (cuentaProv == null)
            throw new IncomeException("El prestador '" + lq.getTitular().getNombre()
                    + "' no tiene cuenta CxP configurada.");
        // El HABER cuadra contra el importeTotal de la cabecera (lq.getTotal()),
        // no contra la suma del DEBE ya construido — igual que
        // generarAsientoFacturaCompra. La diferencia entre importeTotal y el
        // DEBE construido (redondeo legítimo, o un rubro no clasificado) se
        // resuelve en agregarDiferenciaRedondeoSri, que ahora sí puede fallar
        // ruidosamente si la diferencia es demasiado grande para ser redondeo
        // (docs/logica-negocio/cxp/DISENO-CUADRE-CONTRA-IMPORTE-TOTAL.md §6).
        double totalDebeLq = lineas.stream().mapToDouble(l -> nvl(l.getValorDebe())).sum();
        double importeTotalLq = redondear2(nvl(lq.getTotal()));
        agregarDiferenciaRedondeoSri(lineas, importeTotalLq, totalDebeLq,
                "LiquidacionCompraCompra " + idLiquidacion + " (N° " + lq.getNumero() + ")", idEmpresa);

        DetalleAsiento haber = new DetalleAsiento();
        haber.setPlanCuenta(cuentaProv); haber.setNumeroCuenta(cuentaProv.getCuentaContable());
        haber.setNombreCuenta(cuentaProv.getNombre());
        haber.setDescripcion("CxP Prestador: " + lq.getTitular().getNombre());
        haber.setValorDebe(0.0); haber.setValorHaber(importeTotalLq);
        lineas.add(haber);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), moduloOrigenLiquidacionCompra(idLiquidacion));
    }

    // ---------------------------------------------------------------
    // generarAsientoRetencionCompra
    // Retención recibida del cliente: reduce lo que ese cliente nos debe.
    //   DEBE:  Cuenta de retención recibida por código SRI
    //   HABER: CxC Cliente (el monto retenido baja lo que nos debe)
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
            throw new IncomeException("No se encontrÃ³ RetencionCompra con ID: " + idRetencionCompra);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleRetencionCompra> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencionCompra d WHERE d.retencion.id = :id AND d.estado = 1")
                .setParameter("id", idRetencionCompra).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("RetencionCompra " + idRetencionCompra + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalRetenido = 0.0;

        // ── DEBE: una línea por código de retención (cuenta desde TSRI) ────────
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
            DetalleAsiento debeReten = new DetalleAsiento();
            debeReten.setPlanCuenta(pcReten); debeReten.setNumeroCuenta(pcReten.getCuentaContable());
            debeReten.setNombreCuenta(pcReten.getNombre());
            debeReten.setDescripcion("Retención recibida código " + codReten
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(d.getBaseImponible()))
                    + " | " + nvl2(d.getPorcentajeReten()) + "%");
            debeReten.setValorDebe(valor); debeReten.setValorHaber(0.0);
            lineas.add(debeReten);
        }

        // ── HABER: CxC del cliente ──────────────────────────────────────────────
        if (rc.getProveedor() == null)
            throw new IncomeException("RetencionCompra " + idRetencionCompra + " no tiene proveedor.");
        PlanCuenta cuentaCliente = obtenerCuentaCliente(rc.getProveedor().getCodigo(), idEmpresa);
        if (cuentaCliente == null)
            throw new IncomeException("El cliente '" + rc.getProveedor().getNombre()
                    + "' no tiene cuenta contable CxC configurada.");
        DetalleAsiento haberCliente = new DetalleAsiento();
        haberCliente.setPlanCuenta(cuentaCliente); haberCliente.setNumeroCuenta(cuentaCliente.getCuentaContable());
        haberCliente.setNombreCuenta(cuentaCliente.getNombre());
        haberCliente.setDescripcion("CxC Cliente retención: " + rc.getProveedor().getNombre());
        haberCliente.setValorDebe(0.0); haberCliente.setValorHaber(totalRetenido);
        lineas.add(0, haberCliente);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    // ---------------------------------------------------------------
    // generarAsientoRetencionCompraV2  (misma lÃ³gica V1, diferente entidad)
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
            throw new IncomeException("No se encontrÃ³ RetencionCompraV2 con ID: " + idRetencionCompraV2);

        @SuppressWarnings("unchecked")
        List<com.saa.model.cxp.DetalleRetencionCompraV2> detalles = em.createQuery(
                "SELECT d FROM DetalleRetencionCompraV2 d WHERE d.retencionCompraV2.id = :id AND d.estado = 1")
                .setParameter("id", idRetencionCompraV2).getResultList();
        if (detalles == null || detalles.isEmpty())
            throw new IncomeException("RetencionCompraV2 " + idRetencionCompraV2 + " no tiene detalles activos.");

        List<DetalleAsiento> lineas = new ArrayList<>();
        double totalRetenido = 0.0;

        // ── DEBE: por código de retención desde TSRI ────────────────────────────
        for (com.saa.model.cxp.DetalleRetencionCompraV2 d : detalles) {
            String codImpuesto = d.getCodImpuesto();
            String codReten = d.getCodRetencion();
            PlanCuenta pcReten = obtenerCuentaRetencionCompra(codImpuesto, codReten);
            if (pcReten == null)
                throw new IncomeException("No hay cuenta contable para retención V2 "
                        + "(codImpuesto='" + codImpuesto + "', codRetencion='" + codReten + "') en TSRI.");
            double valor = nvl(d.getValorReten());
            totalRetenido += valor;
            DetalleAsiento debeReten = new DetalleAsiento();
            debeReten.setPlanCuenta(pcReten); debeReten.setNumeroCuenta(pcReten.getCuentaContable());
            debeReten.setNombreCuenta(pcReten.getNombre());
            debeReten.setDescripcion("Retención V2 recibida código " + codReten
                    + " | Base: " + String.format(java.util.Locale.US, "%.2f", nvl(d.getBaseImponible()))
                    + " | " + nvl2(d.getPorcentajeReten()) + "%");
            debeReten.setValorDebe(valor); debeReten.setValorHaber(0.0);
            lineas.add(debeReten);
        }

        // ── HABER: CxC del cliente ────────────────────────────────────────────
        if (rc.getProveedor() == null)
            throw new IncomeException("RetencionCompraV2 " + idRetencionCompraV2 + " no tiene proveedor.");
        PlanCuenta cuentaCliente = obtenerCuentaCliente(rc.getProveedor().getCodigo(), idEmpresa);
        if (cuentaCliente == null)
            throw new IncomeException("El cliente '" + rc.getProveedor().getNombre()
                    + "' no tiene cuenta contable CxC configurada.");
        DetalleAsiento haberCliente = new DetalleAsiento();
        haberCliente.setPlanCuenta(cuentaCliente); haberCliente.setNumeroCuenta(cuentaCliente.getCuentaContable());
        haberCliente.setNombreCuenta(cuentaCliente.getNombre());
        haberCliente.setDescripcion("CxC Cliente retención V2: " + rc.getProveedor().getNombre());
        haberCliente.setValorDebe(0.0); haberCliente.setValorHaber(totalRetenido);
        lineas.add(0, haberCliente);

        return generarAsiento(idEmpresa, codigoAltTipoAsiento,
                fechaAsiento, observaciones, usuario, lineas, Long.valueOf(ModuloSistema.CUENTAS_POR_COBRAR), ModuloOrigenAsiento.CXC);
    }

    // ---------------------------------------------------------------
    // Helpers privados CXP
    // ---------------------------------------------------------------

    /**
     * Obtiene la cuenta contable de IVA crÃ©dito tributario (compras) desde PGS.TSRI
     * donde lsri.tabla = '17' y codigo = codigoIVASRI (cÃ³digo que viene directamente de la factura SRI).
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
            System.err.println("âš  No se encontrÃ³ cuenta IVA en PGS.TSRI para codigoIVASRI=" + codigoIVASRI + " (lsri.tabla=17)");
            return null;
        } catch (Exception e) {
            System.err.println("âš  obtenerCuentaIVACxpPorCodigo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cuenta contable de ajuste por diferencia de redondeo del SRI, leída de la línea
     * {@link com.saa.rubros.CxpLineaAsiento#REDONDEO_SRI} de la plantilla
     * {@link com.saa.rubros.PlantillasCxp#COMPRAS} DE LA EMPRESA del asiento (P5, 2026-11-01). Antes era el
     * literal {@code 4.8.90.90.35} buscado en todo CNT.PLNN sin filtrar por empresa: con dos empresas podía
     * traer la cuenta de la otra. Si falta la plantilla, la línea o su cuenta, o la cuenta está inactiva,
     * lanza {@code IncomeException} que lo dice: nunca "no ajusto nada" en silencio ni una cuenta de respaldo.
     */
    private PlanCuenta obtenerCuentaDiferenciaRedondeoSri(Long idEmpresa, String etiquetaDocumento) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(com.saa.rubros.PlantillasCxp.COMPRAS, idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la empresa " + idEmpresa
                    + " no tiene la plantilla contable de compras (alterno " + com.saa.rubros.PlantillasCxp.COMPRAS
                    + ") con la cuenta de diferencia por redondeo SRI.");
        }
        com.saa.model.cnt.DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla,
                com.saa.rubros.CxpLineaAsiento.REDONDEO_SRI);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la plantilla de compras (alterno "
                    + com.saa.rubros.PlantillasCxp.COMPRAS + ") de la empresa " + idEmpresa
                    + " no tiene la línea " + com.saa.rubros.CxpLineaAsiento.REDONDEO_SRI
                    + " (cuenta de diferencia por redondeo SRI) o no tiene cuenta asignada.");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        if (cuenta.getEstado() == null || cuenta.getEstado().longValue() != Estado.ACTIVO) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la cuenta de diferencia por"
                    + " redondeo SRI de la plantilla de compras (" + cuenta.getCuentaContable() + " " + cuenta.getNombre()
                    + ") está inactiva.");
        }
        return cuenta;
    }

    /**
     * Cuenta de gasto para lo que no tiene producto clasificado, leída de la línea
     * {@link com.saa.rubros.CxpLineaAsiento#GASTO_SIN_CLASIFICAR} de la plantilla
     * {@link com.saa.rubros.PlantillasCxp#COMPRAS} de la empresa del asiento (P11, 2026-11-01). Antes era la cuenta del
     * grupo de producto de menor código. Si falta la plantilla, la línea o su cuenta, o la cuenta está inactiva, lanza
     * {@code IncomeException} que lo dice (plantilla y aux1): nunca una cuenta de respaldo.
     */
    private PlanCuenta obtenerCuentaGastoSinClasificarCxp(Long idEmpresa, String etiquetaDocumento) throws Throwable {
        Long idPlantilla = plantillaService.codigoByAlterno(com.saa.rubros.PlantillasCxp.COMPRAS, idEmpresa);
        if (idPlantilla == null || idPlantilla.longValue() == 0L) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la empresa " + idEmpresa
                    + " no tiene la plantilla contable de compras (alterno " + com.saa.rubros.PlantillasCxp.COMPRAS
                    + ") con la cuenta de gasto sin clasificar (aux1 " + com.saa.rubros.CxpLineaAsiento.GASTO_SIN_CLASIFICAR + ").");
        }
        com.saa.model.cnt.DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla,
                com.saa.rubros.CxpLineaAsiento.GASTO_SIN_CLASIFICAR);
        if (linea == null || linea.getPlanCuenta() == null) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la plantilla de compras (alterno "
                    + com.saa.rubros.PlantillasCxp.COMPRAS + ") de la empresa " + idEmpresa
                    + " no tiene la línea aux1 " + com.saa.rubros.CxpLineaAsiento.GASTO_SIN_CLASIFICAR
                    + " (cuenta de gasto sin clasificar) o no tiene cuenta asignada.");
        }
        PlanCuenta cuenta = linea.getPlanCuenta();
        if (cuenta.getEstado() == null || cuenta.getEstado().longValue() != Estado.ACTIVO) {
            throw new IncomeException("No se pudo contabilizar " + etiquetaDocumento + ": la cuenta de gasto sin clasificar"
                    + " de la plantilla de compras (" + cuenta.getCuentaContable() + " " + cuenta.getNombre()
                    + ") está inactiva.");
        }
        return cuenta;
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

            // No existe â†’ crear
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

