package com.saa.ejb.rpr.serviceImpl;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.service.CierreCarteraService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ContabilizacionIndividualCreditoService;
import com.saa.ejb.crd.service.ProductoService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.BandaSnapshotCierre;
import com.saa.ejb.crd.service.dto.CierreCartera;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoCuotasPrestamosMensualService;
import com.saa.ejb.rpr.service.EjecucionReporteCarteraService;
import com.saa.ejb.rpr.service.dto.ResumenBandaCcpm;
import com.saa.model.crd.ConfiguracionBandaProducto;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import com.saa.model.rpr.EjecucionReporteCartera;
import com.saa.model.rpr.NombreEntidadesReporte;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCorridaCierreCartera;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CreditoCuotasPrestamosMensualServiceImpl implements CreditoCuotasPrestamosMensualService {

    @EJB
    private CreditoCuotasPrestamosMensualDaoService creditoCuotasPrestamosMensualDaoService;
    @EJB private EjecucionReporteCarteraService                ejecucionReporteCarteraService;
    @EJB private CierreCarteraService                          cierreCarteraService;
    @EJB private CierreCarteraDaoService                       cierreCarteraDaoService;
    @EJB private ClasificadorBandaService                      clasificadorBandaService;
    @EJB private ContabilizacionIndividualCreditoService       contabilizacionIndividualCreditoService;
    @EJB private ConfiguracionBandaProductoDaoService          configuracionBandaProductoDaoService;
    @EJB private ProductoService                               productoService;

    @Override
    public CreditoCuotasPrestamosMensual selectById(Long id) throws Throwable {
        System.out.println("Ingresa al selectById CreditoCuotasPrestamosMensual con id: " + id);
        return creditoCuotasPrestamosMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
    }

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de CreditoCuotasPrestamosMensualService");
        CreditoCuotasPrestamosMensual entidad = new CreditoCuotasPrestamosMensual();
        for (Long registro : id) {
            creditoCuotasPrestamosMensualDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<CreditoCuotasPrestamosMensual> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de CreditoCuotasPrestamosMensualService");
        for (CreditoCuotasPrestamosMensual registro : lista) {
            creditoCuotasPrestamosMensualDaoService.save(registro, registro.getCodigo());
        }
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll CreditoCuotasPrestamosMensualService");
        List<CreditoCuotasPrestamosMensual> result = creditoCuotasPrestamosMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda total CreditoCuotasPrestamosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public CreditoCuotasPrestamosMensual saveSingle(CreditoCuotasPrestamosMensual entidad) throws Throwable {
        System.out.println("saveSingle - CreditoCuotasPrestamosMensual");
        entidad = creditoCuotasPrestamosMensualDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo selectByCriteria CreditoCuotasPrestamosMensualService");
        List<CreditoCuotasPrestamosMensual> result = creditoCuotasPrestamosMensualDaoService.selectByCriteria(datos, NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio CreditoCuotasPrestamosMensual no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable {
        return creditoCuotasPrestamosMensualDaoService.selectByEjecucion(codigoEjecucion);
    }

    @Override
    public CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable {
        return creditoCuotasPrestamosMensualDaoService.selectByEjecucionYOperacion(codigoEjecucion, numeroOperacion);
    }

    // =====================================================================
    // Resumen financiero por bandas (2026-10-13, CCPM adopta las bandas del cierre)
    // =====================================================================

    @Override
    public List<ResumenBandaCcpm> resumenBandas(Long idEjcc) throws Throwable {
        System.out.println("Ingresa al metodo resumenBandas CreditoCuotasPrestamosMensualService - idEjcc: " + idEjcc);

        // selectById del generico lanza NoResultException si no existe (CLAUDE.md) — se deja
        // propagar: el REST la traduce a 404.
        EjecucionReporteCartera ejecucion = ejecucionReporteCarteraService.selectById(idEjcc);
        Long anio = ejecucion.getAnio();
        Long mes  = ejecucion.getMes();
        LocalDate corte = YearMonth.of(anio.intValue(), mes.intValue()).atEndOfMonth();
        Long idEmpresa = resolverEmpresaUnicaConBandas(corte);

        CierreCartera cierre = null;
        try {
            cierre = cierreCarteraService.consultar(idEmpresa, anio, mes);
        } catch (IncomeException e) {
            // Periodo no corrido todavia: se calcula en vivo (rama PRELIMINAR mas abajo).
            cierre = null;
        }

        List<ResumenBandaCcpm> resultado;
        if (cierre != null
                && Long.valueOf(EstadoCorridaCierreCartera.EJECUTADA).equals(cierre.getIdEstado())) {
            resultado = new ArrayList<>();
            // ITEM 5 (2026-10-13): CRD.BDCC no guarda la etiqueta de la banda —
            // CierreCarteraServiceImpl.armaDesdeGrabado la deja en null al reconstruir el
            // snapshot grabado. Se resuelve acá, derivada de la parametrización vigente al
            // corte de la corrida (nunca tocando BDCC ni CierreCarteraServiceImpl).
            Map<String, List<BandaProductoDetalle>> cacheBandasCierre = new HashMap<>();
            for (BandaSnapshotCierre fila : cierre.getSnapshot()) {
                ResumenBandaCcpm r = new ResumenBandaCcpm();
                r.setIdProducto(fila.getIdProducto());
                r.setNombreProducto(fila.getNombreProducto());
                r.setTipoCartera(fila.getTipoCartera());
                r.setNombreTipoCartera(fila.getNombreTipoCartera());
                r.setNumeroBanda(fila.getNumeroBanda());
                String etiqueta = fila.getEtiquetaBanda();
                if (etiqueta == null) {
                    // La etiqueta es una comodidad de presentación, no un dato financiero: si
                    // la parametrización cambió después de la corrida y resolverRangos ya no
                    // encuentra configuración vigente al corte, no tiene que tumbar el resto
                    // del resumen (que sí es correcto) por una sola etiqueta sin resolver.
                    try {
                        etiqueta = etiquetaBandaDesdeParametrizacion(fila.getIdProducto(), idEmpresa,
                                fila.getTipoCartera(), fila.getNumeroBanda(), cierre.getFechaCorte(),
                                cacheBandasCierre);
                    } catch (Throwable e) {
                        System.out.println("CCPM resumenBandas - no se pudo resolver etiquetaBanda para"
                                + " producto " + fila.getIdProducto() + " tipoCartera " + fila.getTipoCartera()
                                + " banda " + fila.getNumeroBanda() + ": " + e.getMessage());
                        etiqueta = null;
                    }
                }
                r.setEtiquetaBanda(etiqueta);
                r.setCuenta(fila.getCuenta());
                r.setNombreCuenta(fila.getNombreCuenta());
                r.setCapital(fila.getCapital());
                r.setCantidad(fila.getCantidad());
                r.setFuente("CIERRE");
                r.setIdCorrida(cierre.getIdCorrida());
                r.setFechaCorte(cierre.getFechaCorte());
                resultado.add(r);
            }
        } else {
            // Sin corrida EJECUTADA (no corrida, PREPARADA o REVERSADA): se calcula en vivo.
            resultado = calculaResumenPreliminar(idEmpresa, corte);
        }

        resultado.sort(Comparator
                .comparing(ResumenBandaCcpm::getIdProducto, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ResumenBandaCcpm::getTipoCartera, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ResumenBandaCcpm::getNumeroBanda, Comparator.nullsLast(Comparator.naturalOrder())));
        return resultado;
    }

    /**
     * Etiqueta de una banda del snapshot GRABADO (CRD.BDCC no la guarda), resuelta contra la
     * parametrización vigente al corte de la corrida — cacheada por "idProducto-tipoCartera"
     * como {@link #calculaResumenPreliminar}. Si ninguna banda de la configuración vigente
     * coincide en número con la del snapshot, devuelve null: no se inventa texto (ITEM 5).
     */
    private String etiquetaBandaDesdeParametrizacion(Long idProducto, Long idEmpresa, Long tipoCartera,
            Long numeroBanda, LocalDate corte, Map<String, List<BandaProductoDetalle>> cache) throws Throwable {
        if (idProducto == null || tipoCartera == null || numeroBanda == null) {
            return null;
        }
        String clave = idProducto + "-" + tipoCartera;
        List<BandaProductoDetalle> bandas = cache.get(clave);
        if (bandas == null) {
            bandas = clasificadorBandaService.resolverRangos(idProducto, idEmpresa, tipoCartera, corte);
            cache.put(clave, bandas);
        }
        for (BandaProductoDetalle banda : bandas) {
            if (numeroBanda.equals(banda.getNumero())) {
                return banda.getEtiqueta();
            }
        }
        return null;
    }

    /**
     * Calcula el resumen EN VIVO cuando el período todavía no tiene una corrida EJECUTADA del
     * cierre de cartera: mismo universo ({@link CierreCarteraDaoService#selectCuotasPendientesPorPrestamo})
     * y misma clasificación ({@code ContabilizacionIndividualCreditoService#tipoCarteraYDias} +
     * {@code ClasificadorBandaService}) que usaría el cierre, agregado por (producto,
     * tipoCartera, banda). Incluye las bandas parametrizadas sin ninguna cuota, en cero —
     * mismo criterio que {@code CierreCarteraServiceImpl#completaBandasVacias}.
     */
    private List<ResumenBandaCcpm> calculaResumenPreliminar(Long idEmpresa, LocalDate corte) throws Throwable {
        Map<Long, Producto> productos = new HashMap<>();
        Map<String, List<BandaProductoDetalle>> cacheBandas = new LinkedHashMap<>();
        Map<String, ResumenBandaCcpm> distribucion = new LinkedHashMap<>();

        // Bandas de TODA la parametrizacion vigente de la empresa, no solo la de los productos
        // con cartera viva hoy — asi un producto que se vacio sigue apareciendo en cero.
        for (ConfiguracionBandaProducto configuracion : configuracionBandaProductoDaoService
                .selectVigentesByEmpresa(idEmpresa, corte)) {
            if (configuracion.getProducto() == null) {
                continue;
            }
            Long idProducto = configuracion.getProducto().getCodigo();
            Long tipoCartera = configuracion.getTipoCartera();
            String clave = idProducto + "-" + tipoCartera;
            if (cacheBandas.containsKey(clave)) {
                continue;
            }
            List<BandaProductoDetalle> bandas = clasificadorBandaService
                    .resolverRangos(idProducto, idEmpresa, tipoCartera, corte);
            cacheBandas.put(clave, bandas);
            for (BandaProductoDetalle banda : bandas) {
                String claveBanda = claveBanda(idProducto, tipoCartera, banda.getNumero());
                distribucion.put(claveBanda, nuevaFilaResumen(idProducto, tipoCartera, banda, productos, corte));
            }
        }

        List<Object[]> cuotas = cierreCarteraDaoService.selectCuotasPendientesPorPrestamo(idEmpresa);
        for (Object[] c : cuotas) {
            Long idProducto = (Long) c[1];
            LocalDate vencimiento = (LocalDate) c[4];
            Double capitalPendiente = (Double) c[5];

            long[] td = contabilizacionIndividualCreditoService.tipoCarteraYDias(vencimiento, corte);
            Long tipoCartera = Long.valueOf(td[0]);
            Long dias = Long.valueOf(td[1]);

            String claveConfig = idProducto + "-" + tipoCartera;
            List<BandaProductoDetalle> bandas = cacheBandas.get(claveConfig);
            if (bandas == null) {
                // No estaba entre las configuraciones vigentes leidas arriba (no deberia pasar
                // si la cuota tiene producto parametrizado) — se resuelve igual: si de verdad
                // no hay configuracion, resolverRangos falla fuerte con el producto y el tipo.
                bandas = clasificadorBandaService.resolverRangos(idProducto, idEmpresa, tipoCartera, corte);
                cacheBandas.put(claveConfig, bandas);
            }
            BandaProductoDetalle banda = clasificadorBandaService.clasificarEnBandas(bandas, dias);

            String claveBanda = claveBanda(idProducto, tipoCartera, banda.getNumero());
            ResumenBandaCcpm fila = distribucion.get(claveBanda);
            if (fila == null) {
                fila = nuevaFilaResumen(idProducto, tipoCartera, banda, productos, corte);
                distribucion.put(claveBanda, fila);
            }
            fila.setCapital(fila.getCapital() + capitalPendiente);
            fila.setCantidad(fila.getCantidad() + 1L);
        }

        return new ArrayList<>(distribucion.values());
    }

    /** Fila nueva del resumen preliminar, en cero, con la banda y la cuenta ya resueltas. */
    private ResumenBandaCcpm nuevaFilaResumen(Long idProducto, Long tipoCartera, BandaProductoDetalle banda,
            Map<Long, Producto> productos, LocalDate corte) throws Throwable {
        ResumenBandaCcpm fila = new ResumenBandaCcpm();
        fila.setIdProducto(idProducto);
        fila.setNombreProducto(nombreProducto(productos, idProducto));
        fila.setTipoCartera(tipoCartera);
        fila.setNombreTipoCartera(Long.valueOf(TipoCarteraBanda.VENCIDO).equals(tipoCartera) ? "VENCIDO" : "POR VENCER");
        fila.setNumeroBanda(banda.getNumero());
        fila.setEtiquetaBanda(banda.getEtiqueta());
        fila.setCuenta(banda.getCuentaContable());
        fila.setNombreCuenta(banda.getNombreCuenta());
        fila.setCapital(0.0);
        fila.setCantidad(0L);
        fila.setFuente("PRELIMINAR");
        fila.setIdCorrida(null);
        fila.setFechaCorte(corte);
        return fila;
    }

    /** Nombre del producto, cacheado para no repetir la consulta dentro de la misma corrida. */
    private String nombreProducto(Map<Long, Producto> productos, Long idProducto) throws Throwable {
        if (idProducto == null) {
            return null;
        }
        Producto producto = productos.get(idProducto);
        if (producto == null) {
            producto = productoService.selectById(idProducto);
            productos.put(idProducto, producto);
        }
        return producto != null ? producto.getNombre() : null;
    }

    /** Clave de una banda dentro de la distribución: producto + tipo de cartera + número. */
    private String claveBanda(Long idProducto, Long tipoCartera, Long numero) {
        return idProducto + "-" + tipoCartera + "-" + numero;
    }

    /**
     * Resuelve la empresa cuando quien llama no la indica explícitamente: la única que tenga
     * configuración de bandas vigente a la fecha. Falla fuerte si hay cero o más de una — hoy
     * solo existe la empresa 1236, pero el día que haya una segunda, adivinar sería silencioso.
     *
     * <p>Fallback deliberado (2026-10-13, DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md §2.1): existe
     * SOLO hasta que el frontend mande {@code idEmpresa} en la solicitud. {@code selectAll()}
     * aquí es sobre {@code CRD.CBPR} —la parametrización de bandas, un puñado de filas— no
     * sobre una tabla transaccional: no es el {@code selectAll()} que CLAUDE.md prohíbe en
     * procesos de carga/generación/consultas pesadas.</p>
     */
    private Long resolverEmpresaUnicaConBandas(LocalDate fecha) throws Throwable {
        List<ConfiguracionBandaProducto> todas = configuracionBandaProductoDaoService
                .selectAll(NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
        Set<Long> empresas = new HashSet<>();
        for (ConfiguracionBandaProducto c : todas) {
            if (c.getEmpresa() == null || c.getProducto() == null) {
                continue;
            }
            if (c.getEstado() == null || !c.getEstado().equals(Long.valueOf(Estado.ACTIVO))) {
                continue;
            }
            if (c.getFechaDesde() != null && c.getFechaDesde().isAfter(fecha)) {
                continue;
            }
            if (c.getFechaHasta() != null && c.getFechaHasta().isBefore(fecha)) {
                continue;
            }
            empresas.add(c.getEmpresa().getCodigo());
        }
        if (empresas.isEmpty()) {
            throw new IncomeException("CCPM: no hay ninguna empresa con configuracion de bandas"
                    + " vigente al " + fecha + ". El frontend tiene que enviar idEmpresa explicitamente.");
        }
        if (empresas.size() > 1) {
            throw new IncomeException("CCPM: hay " + empresas.size()
                    + " empresas con configuracion de bandas vigente (" + empresas
                    + "); el frontend tiene que enviar idEmpresa explicitamente, no se puede adivinar.");
        }
        return empresas.iterator().next();
    }
}
