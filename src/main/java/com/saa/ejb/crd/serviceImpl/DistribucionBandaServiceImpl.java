package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.dao.DistribucionBandaDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.ParticipeXCargaArchivoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.DistribucionBandaService;
import com.saa.ejb.crd.service.dto.AsientoResumenDsbn;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.FilaDistribucionBanda;
import com.saa.ejb.crd.service.dto.OrigenDistribucionBandaResumen;
import com.saa.ejb.crd.service.dto.DiferenciaParticipeDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoCuadreDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDiferenciaDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResumenConceptoDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResumenJerarquicoConcepto;
import com.saa.ejb.crd.service.dto.ResumenJerarquicoCuentaBanda;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.crd.Aporte;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.DistribucionBanda;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.CrdLineaAsiento;
import com.saa.rubros.DsbnConcepto;
import com.saa.rubros.DsbnOrigen;
import com.saa.rubros.EstadoAsiento;
import com.saa.rubros.PlantillasCredito;
import com.saa.rubros.TipoCarteraBanda;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class DistribucionBandaServiceImpl implements DistribucionBandaService {

    private static final double TOLERANCIA = 0.01;

    /**
     * Tolerancia del CUADRE (recibido vs. distribuido), distinta de {@link #TOLERANCIA}: esta
     * es de redondeo, no de "¿hay algo mal aplicado?" — mismo criterio que
     * {@code FamiliaNovedadCarga.TOLERANCIA} (2026-09-02, hallazgo de producción carga 449: con
     * $0,01 el cuadre marcaba "no cuadra" por puro redondeo y entrenaba al usuario a ignorar el
     * rojo).
     */
    private static final double TOLERANCIA_CUADRE = com.saa.model.crd.FamiliaNovedadCarga.TOLERANCIA;

    private static final List<String> ORIGENES_VALIDOS = Arrays.asList(
        DsbnOrigen.CARGA_PETRO, DsbnOrigen.COBRO_INDIVIDUAL,
        DsbnOrigen.EVENTO_PRESTAMO, DsbnOrigen.PAGO_PENSION);

    /**
     * Duplicadas A PROPÓSITO desde {@code CobroPetroContableServiceImpl} (2026-09-02): mismo
     * mapeo concepto/tipo → línea de plantilla que arma el asiento de aplicación, para que esta
     * pantalla de SOLO LECTURA muestre la MISMA cuenta que terminó afectada — nunca se calculan
     * de forma independiente. Ver {@link #resolverLineaPlantilla}. Si el asiento cambia de
     * línea o de código, estas constantes tienen que cambiar junto con las de allá.
     */
    private static final long TIPO_APORTE_JUBILACION = 9L;
    private static final long TIPO_APORTE_CESANTIA = 11L;
    private static final long TIPO_PRESTAMO_HIPOTECARIO = 2L;
    private static final long TIPO_PRESTAMO_PRENDARIO = 3L;
    private static final int AUX1_SEGURO_HIPOTECARIO = 42;
    private static final int AUX1_SEGURO_PRENDARIO = 43;

    @EJB
    private DistribucionBandaDaoService distribucionBandaDaoService;

    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @EJB
    private ConfiguracionContabilidadService configuracionContabilidadService;

    @EJB
    private CargaArchivoDaoService cargaArchivoDaoService;

    @EJB
    private com.saa.ejb.crd.dao.TransferenciaCargaPetroDaoService transferenciaCargaPetroDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private ParticipeXCargaArchivoDaoService participeXCargaArchivoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private com.saa.ejb.cnt.dao.AsientoDaoService asientoDaoService;

    @EJB
    private com.saa.ejb.cnt.service.PlantillaService plantillaService;

    @EJB
    private com.saa.ejb.cnt.dao.DetallePlantillaDaoService detallePlantillaDaoService;

    // ========================================================================
    // Escritura — AL APLICAR el pago, no al contabilizar (PLAN-AUDITORIA-BANDAS.md §1)
    // ========================================================================

    @Override
    public Map<Long, ResultadoClasificacionBanda> registrarDistribucionCargaPetro(Long idCarga, Long idEmpresa,
            List<PagoPrestamo> pagos, String usuario) throws Throwable {
        System.out.println("DistribucionBandaService.registrarDistribucionCargaPetro - Carga: " + idCarga
            + " - Pagos: " + (pagos != null ? pagos.size() : 0));

        // Idempotente por (origen, idOrigen) — reprocesar REEMPLAZA, no duplica (§5.1 punto 2).
        // UNA sola llamada por carga, así que puede borrar antes de escribir sin riesgo (a
        // diferencia de registrarDistribucionPorPagos, que puede recibir varias llamadas para
        // el mismo origen dentro de un mismo proceso — ver su javadoc).
        int borradas = distribucionBandaDaoService.eliminarPorOrigen(DsbnOrigen.CARGA_PETRO, idCarga);
        if (borradas > 0) {
            System.out.println("  " + borradas + " fila(s) previa(s) de la carga " + idCarga + " reemplazadas.");
        }

        // Corrección 2026-09-02: se devuelve para que contabilizarAplicacion NO vuelva a llamar
        // a clasificar() por los mismos pagos al armar el asiento — "clasificar una vez por
        // pago, que ambos consuman lo mismo", no duplicar las consultas de clasificación en el
        // mismo proceso de 20+ minutos que ya se estabilizó por otro motivo hoy.
        Map<Long, ResultadoClasificacionBanda> clasificacionPorPago =
            registrarDistribucionPorPagos(DsbnOrigen.CARGA_PETRO, idCarga, idEmpresa, pagos, usuario);

        // Aportes (2026-09-02, hueco real: la pantalla nunca mostraba el detalle de aportes —
        // "también debe darme ese detalle por aportes", carga 449: $116.857,06 de $354.603,67
        // invisibles). Con el MISMO criterio de filtro (idAsoprep) que usa
        // CobroPetroContableServiceImpl.contabilizarAplicacion (AporteDaoService#selectByCarga,
        // hermano de #sumValorPorTipoAporteByCarga: ver su javadoc para el porqué transitorio),
        // para que la pantalla y el asiento nunca puedan divergir por leer columnas distintas.
        // Sin banda ni cuota (DsbnConcepto: "solo CAPITAL lleva banda") — la cuenta contable de
        // este concepto se resuelve al LEER, por tipoAporte, igual que la banda se resuelve al
        // leer para CAPITAL. Específico de Petro: los demás orígenes no tienen esta fuente.
        LocalDateTime ahora = LocalDateTime.now();
        int filasAportes = 0;
        List<Aporte> aportes = aporteDaoService.selectByCarga(idCarga);
        if (aportes != null && !aportes.isEmpty()) {
            for (Aporte aporte : aportes) {
                double valorAporte = nvl(aporte.getValor());
                if (valorAporte <= TOLERANCIA) {
                    continue;
                }
                if (aporte.getEntidad() == null) {
                    throw new IncomeException("El aporte " + aporte.getCodigo() + " de la carga " + idCarga
                        + " no tiene partícipe asociado; no se puede auditar su distribución.");
                }
                LocalDate fechaAplicacionAporte = aporte.getFechaTransaccion() != null
                    ? aporte.getFechaTransaccion().toLocalDate() : LocalDate.now();
                Long idTipoAporte = aporte.getTipoAporte() != null ? aporte.getTipoAporte().getCodigo() : null;

                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.APORTE, valorAporte, aporte.getEntidad(), null, null, null, null,
                    idTipoAporte, fechaAplicacionAporte, usuario, ahora), null);
                filasAportes++;
            }
        }
        System.out.println("  ✅ Distribución en bandas (aportes) registrada - Carga " + idCarga
            + " - " + filasAportes + " fila(s)");

        return clasificacionPorPago;
    }

    @Override
    public int eliminarDistribucion(String origen, Long idOrigen) throws Throwable {
        return distribucionBandaDaoService.eliminarPorOrigen(origen, idOrigen);
    }

    @Override
    public Map<Long, ResultadoClasificacionBanda> registrarDistribucionPorPagos(String origen, Long idOrigen,
            Long idEmpresa, List<PagoPrestamo> pagos, String usuario) throws Throwable {
        System.out.println("DistribucionBandaService.registrarDistribucionPorPagos - " + origen + "/" + idOrigen
            + " - Pagos: " + (pagos != null ? pagos.size() : 0));

        Map<Long, ResultadoClasificacionBanda> clasificacionPorPago = new LinkedHashMap<>();
        if (pagos == null || pagos.isEmpty()) {
            return clasificacionPorPago;
        }

        LocalDateTime ahora = LocalDateTime.now();
        int filasEscritas = 0;

        // Mismo cache LOCAL a esta llamada que registrarDistribucionCargaPetro (URGENTE
        // 2026-09-02) — acá el volumen por llamada es chico (un puñado de cuotas de una
        // operación individual, no miles de una carga), pero el criterio es el mismo: nunca un
        // campo de clase ni entre llamadas.
        Map<String, List<BandaProductoDetalle>> cacheRangosPorClave = new LinkedHashMap<>();

        for (PagoPrestamo pago : pagos) {
            Prestamo prestamo = pago.getPrestamo();
            if (prestamo == null || prestamo.getEntidad() == null) {
                throw new IncomeException("El pago " + pago.getCodigo() + " de " + origen + " " + idOrigen
                    + " no tiene préstamo o partícipe asociado; no se puede auditar su distribución.");
            }
            Producto producto = prestamo.getProducto();
            if (producto == null) {
                throw new IncomeException("El pago " + pago.getCodigo() + " de " + origen + " " + idOrigen
                    + " no tiene producto asignado; no se puede clasificar su distribución.");
            }
            Long idProducto = producto.getCodigo();
            Long idTipoPrestamo = producto.getTipoPrestamo() != null ? producto.getTipoPrestamo().getCodigo() : null;

            double capital = nvl(pago.getCapitalPagado());
            double interes = nvl(pago.getInteresPagado());
            double mora = nvl(pago.getMoraPagada());
            double interesVencido = nvl(pago.getInteresVencidoPagado());
            double desgravamen = nvl(pago.getDesgravamen());
            double seguroIncendio = nvl(pago.getValorSeguroIncendio());

            LocalDate fechaAplicacion = pago.getFecha() != null ? pago.getFecha().toLocalDate() : LocalDate.now();

            if (capital > TOLERANCIA) {
                DetallePrestamo cuota = pago.getDetallePrestamo();
                LocalDateTime fechaVencimiento = cuota != null ? cuota.getFechaVencimiento() : null;
                if (fechaVencimiento == null) {
                    throw new IncomeException("El pago " + pago.getCodigo() + " de " + origen + " " + idOrigen
                        + " no tiene fecha de vencimiento de cuota; no se puede clasificar su capital por banda.");
                }
                LocalDate vencimiento = fechaVencimiento.toLocalDate();

                long tipoCartera;
                long dias;
                if (!fechaAplicacion.isAfter(vencimiento)) {
                    tipoCartera = TipoCarteraBanda.POR_VENCER;
                    dias = Math.max(1, ChronoUnit.DAYS.between(fechaAplicacion, vencimiento));
                } else {
                    tipoCartera = TipoCarteraBanda.VENCIDO;
                    dias = ChronoUnit.DAYS.between(vencimiento, fechaAplicacion);
                }

                String claveRangos = idProducto + "|" + idEmpresa + "|" + tipoCartera + "|" + fechaAplicacion;
                List<BandaProductoDetalle> rangos = cacheRangosPorClave.get(claveRangos);
                if (rangos == null) {
                    rangos = clasificadorBandaService.resolverRangos(
                        idProducto, idEmpresa, tipoCartera, fechaAplicacion);
                    cacheRangosPorClave.put(claveRangos, rangos);
                }
                BandaProductoDetalle bandaClasificada = clasificadorBandaService.clasificarEnBandas(rangos, dias);

                ResultadoClasificacionBanda resultado = new ResultadoClasificacionBanda();
                resultado.setIdProducto(idProducto);
                resultado.setIdEmpresa(idEmpresa);
                resultado.setTipoCartera(tipoCartera);
                resultado.setFecha(fechaAplicacion);
                resultado.setDias(dias);
                resultado.setBanda(bandaClasificada);
                clasificacionPorPago.put(pago.getCodigo(), resultado);
                BandaProductoDetalle bandaDetalle = resultado.getBanda();

                DistribucionBanda fila = filaBase(origen, idOrigen, DsbnConcepto.CAPITAL,
                    capital, prestamo.getEntidad(), prestamo, cuota, producto, idTipoPrestamo, null,
                    fechaAplicacion, usuario, ahora);
                fila.setTipoCartera(tipoCartera);
                fila.setDias(dias);
                if (bandaDetalle != null && bandaDetalle.getIdBanda() != null) {
                    BandaProducto bandaRef = new BandaProducto();
                    bandaRef.setCodigo(bandaDetalle.getIdBanda());
                    fila.setBanda(bandaRef);
                    fila.setEtiqueta(bandaDetalle.getEtiqueta());
                }
                fila.setFechaVencimiento(vencimiento);
                distribucionBandaDaoService.save(fila, null);
                filasEscritas++;
            }

            // Mismo guardarraíl que CobroPetroContableServiceImpl.contabilizarAplicacion: sin
            // tipo de préstamo no se puede resolver la cuenta de interés/mora/seguro — acá
            // tampoco se puede saber a qué familia pertenece la distribución.
            if ((interes > TOLERANCIA || mora > TOLERANCIA || seguroIncendio > TOLERANCIA) && idTipoPrestamo == null) {
                throw new IncomeException("El pago " + pago.getCodigo() + " de " + origen + " " + idOrigen
                    + " tiene interés, mora o seguro de incendio pero su producto no tiene tipo de"
                    + " préstamo asignado; no se puede clasificar su distribución.");
            }

            if (interes > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(origen, idOrigen,
                    DsbnConcepto.INTERES_ORDINARIO, interes, prestamo.getEntidad(), prestamo,
                    pago.getDetallePrestamo(), producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (mora > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(origen, idOrigen,
                    DsbnConcepto.INTERES_MORA, mora, prestamo.getEntidad(), prestamo,
                    pago.getDetallePrestamo(), producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (interesVencido > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(origen, idOrigen,
                    DsbnConcepto.INTERES_VENCIDO, interesVencido, prestamo.getEntidad(), prestamo,
                    pago.getDetallePrestamo(), producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (desgravamen > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(origen, idOrigen,
                    DsbnConcepto.SEGURO_DESGRAVAMEN, desgravamen, prestamo.getEntidad(), prestamo,
                    pago.getDetallePrestamo(), producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (seguroIncendio > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(origen, idOrigen,
                    DsbnConcepto.SEGURO_INCENDIO, seguroIncendio, prestamo.getEntidad(), prestamo,
                    pago.getDetallePrestamo(), producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
        }

        System.out.println("  ✅ Distribución en bandas registrada - " + origen + " " + idOrigen
            + " - " + filasEscritas + " fila(s)");
        return clasificacionPorPago;
    }

    private DistribucionBanda filaBase(String origen, Long idOrigen, String concepto, double valor,
            Entidad entidad, Prestamo prestamo, DetallePrestamo cuota, Producto producto, Long idTipoPrestamo,
            Long idTipoAporte, LocalDate fechaAplicacion, String usuario, LocalDateTime ahora) {
        DistribucionBanda fila = new DistribucionBanda();
        fila.setOrigen(origen);
        fila.setIdOrigen(idOrigen);
        fila.setConcepto(concepto);
        fila.setValor(redondear(valor));
        fila.setEntidad(entidad);
        fila.setPrestamo(prestamo);
        fila.setDetallePrestamo(cuota);
        fila.setProducto(producto);
        fila.setTipoPrestamo(idTipoPrestamo);
        fila.setTipoAporte(idTipoAporte);
        fila.setFechaAplicacion(fechaAplicacion);
        fila.setFechaRegistro(ahora);
        fila.setUsuarioRegistro(usuario);
        fila.setEstado(1L);
        return fila;
    }

    @Override
    public void actualizarAsiento(String origen, Long idOrigen, Long idAsiento) throws Throwable {
        if (idAsiento == null) {
            return;
        }
        distribucionBandaDaoService.actualizarAsientoPorOrigen(origen, idOrigen, idAsiento);
    }

    // ========================================================================
    // Lectura — API-AUDITORIA-BANDAS.md
    // ========================================================================

    @Override
    public ResultadoCuadreDistribucionBanda obtenerCuadre(String origen, Long idOrigen) throws Throwable {
        System.out.println("DistribucionBandaService.obtenerCuadre - " + origen + "/" + idOrigen);
        validarOrigen(origen);
        if (idOrigen == null) {
            throw new IncomeException("idOrigen es obligatorio");
        }

        List<DistribucionBanda> filas = distribucionBandaDaoService.selectByOrigen(origen, idOrigen);
        if (filas == null || filas.isEmpty()) {
            throw new IncomeException(ERR_ORIGEN_NO_ENCONTRADO + ": no hay distribución registrada para "
                + origen + " " + idOrigen);
        }

        ResultadoCuadreDistribucionBanda resultado = new ResultadoCuadreDistribucionBanda();
        resultado.setOrigen(origen);
        resultado.setIdOrigen(idOrigen);
        resultado.setContabilidadConectada(configuracionContabilidadService.contabilidadActiva());

        double distribuido = 0.0;
        Long idAsiento = null;
        Map<Long, BandaProductoDetalle> bandasUsadas = new LinkedHashMap<>();
        for (DistribucionBanda fila : filas) {
            distribuido += nvl(fila.getValor());
            if (fila.getIdAsiento() != null) {
                idAsiento = fila.getIdAsiento();
            }
            if (fila.getBanda() != null && !bandasUsadas.containsKey(fila.getBanda().getCodigo())) {
                BandaProductoDetalle detalle = new BandaProductoDetalle();
                detalle.setIdBanda(fila.getBanda().getCodigo());
                detalle.setNumero(fila.getBanda().getNumero());
                detalle.setEtiqueta(fila.getEtiqueta());
                bandasUsadas.put(fila.getBanda().getCodigo(), detalle);
            }
        }
        resultado.setDistribuido(redondear(distribuido));
        resultado.setBandas(new ArrayList<>(bandasUsadas.values()));

        if (idAsiento != null) {
            Asiento asiento = asientoDaoService.find(new Asiento(), idAsiento);
            if (asiento != null) {
                AsientoResumenDsbn resumenAsiento = new AsientoResumenDsbn();
                resumenAsiento.setIdAsiento(asiento.getCodigo());
                resumenAsiento.setTipo(asiento.getTipoAsiento() != null ? asiento.getTipoAsiento().getNombre() : null);
                resumenAsiento.setFecha(asiento.getFechaAsiento());
                resumenAsiento.setEstado(nombreEstadoAsiento(asiento.getEstado()));
                resultado.getAsientos().add(resumenAsiento);
            }
        }

        // Solo CARGA_PETRO tiene, por ahora, una fuente de "recibido" independiente conectada
        // (PLAN-AUDITORIA-BANDAS.md, alcance de esta entrega). Para los demás orígenes,
        // recibido/diferencia/cuadra quedan null — ausencia de cobertura, no un error.
        if (DsbnOrigen.CARGA_PETRO.equals(origen)) {
            CargaArchivo carga = cargaArchivoDaoService.find(new CargaArchivo(), idOrigen);
            resultado.setDescripcionOrigen("Carga Petro"
                + (carga != null && carga.getMesAfectacion() != null ? " " + carga.getMesAfectacion() : "")
                + (carga != null && carga.getAnioAfectacion() != null ? "/" + carga.getAnioAfectacion() : "")
                + " (" + idOrigen + ")");

            // 2026-09-02 (URGENTE, hallazgo de producción carga 449): "recibido" salía de sumar
            // los PagoPrestamo, pero esos son la SALIDA del reparto (DSBN se escribe a partir de
            // los mismos pagos) — el cuadre comparaba el reparto contra sí mismo y siempre daba
            // ~0, sin poder detectar que se aplicó de más. El "recibido" real de una carga Petro
            // son las TRANSFERENCIAS con las que Petro pagó (TransferenciaCargaPetro vigentes),
            // el mismo número que usa el asiento transitorio del paso 1. Medido: transferido
            // $354.491,37, aplicado $354.603,67 — $112,30 aplicados que nadie transfirió, y con
            // la fuente vieja el cuadre lo escondía.
            double recibido = redondear(transferenciaCargaPetroDaoService.sumaValorVigentesByCarga(idOrigen));
            double diferencia = redondear(recibido - resultado.getDistribuido());
            resultado.setRecibido(recibido);
            resultado.setDiferencia(diferencia);
            resultado.setCuadra(Math.abs(diferencia) <= TOLERANCIA_CUADRE);
        } else {
            resultado.setDescripcionOrigen(origen + " " + idOrigen);
        }

        return resultado;
    }

    /**
     * «¿De quién es la diferencia?» — API-AUDITORIA-BANDAS.md §4, sql/184. Todo agregado en la
     * consulta (nunca fila por fila en Java sobre miles de PGPR/PXCA), y unido por ENTIDAD
     * (ENTDCDGO), no por rol Petro — igual que sql/184: el rol solo sirve para mostrar, la
     * identidad real de un partícipe es su Entidad.
     */
    @Override
    public ResultadoDiferenciaDistribucionBanda obtenerDiferencia(String origen, Long idOrigen) throws Throwable {
        System.out.println("DistribucionBandaService.obtenerDiferencia - " + origen + "/" + idOrigen);
        validarOrigen(origen);
        if (idOrigen == null) {
            throw new IncomeException("idOrigen es obligatorio");
        }
        if (!DsbnOrigen.CARGA_PETRO.equals(origen)) {
            throw new IncomeException("El detalle de diferencia por partícipe solo está disponible hoy"
                + " para CARGA_PETRO: es el único origen con una fuente independiente de \"descontado\""
                + " (CRD.PXCA) contra la cual comparar — mismo motivo por el que su cuadre es null para"
                + " los demás orígenes.");
        }

        // --- Descontado, por rol Petro (PXCA, todos los productos) ---
        List<Object[]> descontadoFilas = participeXCargaArchivoDaoService.selectDescontadoPorRolEnCarga(idOrigen);
        Map<Long, Double> descontadoPorRol = new LinkedHashMap<>();
        List<Long> codigosPetro = new ArrayList<>();
        for (Object[] fila : descontadoFilas) {
            Long codigoPetro = (Long) fila[0];
            double descontado = ((Number) fila[2]).doubleValue();
            descontadoPorRol.put(codigoPetro, descontado);
            codigosPetro.add(codigoPetro);
        }

        // --- Aplicado, por ENTIDAD: préstamos (con desglose manual/automático) y aportes ---
        List<Object[]> prestamosFilas = pagoPrestamoDaoService.selectAplicadoPorEntidadEnCarga(idOrigen);
        List<Object[]> aportesFilas = aporteDaoService.selectAplicadoPorEntidadEnCarga(idOrigen);

        // --- Entidades: primero las que tienen fila PXCA (por rol)... ---
        Map<Long, Entidad> entidadPorId = new LinkedHashMap<>();
        Map<Long, Long> entidadPorRolInverso = new LinkedHashMap<>();
        for (Entidad entidad : entidadDaoService.selectByCodigosPetro(codigosPetro)) {
            entidadPorId.put(entidad.getCodigo(), entidad);
            if (entidad.getRolPetroComercial() != null) {
                entidadPorRolInverso.put(entidad.getRolPetroComercial(), entidad.getCodigo());
            }
        }

        // ...y las que aparecen en lo APLICADO pero NO tienen fila PXCA — exactamente el
        // hallazgo que este endpoint tiene que poder señalar, no ocultar (§4: "si no coinciden,
        // hay casos que este endpoint no está viendo, y eso es un hallazgo, no un redondeo").
        Set<Long> idsAplicado = new LinkedHashSet<>();
        for (Object[] fila : prestamosFilas) {
            idsAplicado.add((Long) fila[0]);
        }
        for (Object[] fila : aportesFilas) {
            idsAplicado.add((Long) fila[0]);
        }
        List<Long> idsFaltantes = new ArrayList<>();
        for (Long idEntidad : idsAplicado) {
            if (idEntidad != null && !entidadPorId.containsKey(idEntidad)) {
                idsFaltantes.add(idEntidad);
            }
        }
        if (!idsFaltantes.isEmpty()) {
            for (Entidad entidad : entidadDaoService.selectByCodigos(idsFaltantes)) {
                entidadPorId.put(entidad.getCodigo(), entidad);
            }
        }

        // --- Descontado, re-indexado por ENTIDAD (la misma clave que prestamos/aportes) ---
        Map<Long, Double> descontadoPorEntidad = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : descontadoPorRol.entrySet()) {
            Long idEntidad = entidadPorRolInverso.get(entry.getKey());
            if (idEntidad == null) {
                // Rol Petro sin Entidad resoluble — dato legacy, no bloquea el resto.
                continue;
            }
            descontadoPorEntidad.merge(idEntidad, entry.getValue(), Double::sum);
        }

        Map<Long, Double> aplicadoManualPorEntidad = new LinkedHashMap<>();
        Map<Long, Double> aplicadoAutomaticoPorEntidad = new LinkedHashMap<>();
        Map<Long, Double> aplicadoPrestamosPorEntidad = new LinkedHashMap<>();
        for (Object[] fila : prestamosFilas) {
            Long idEntidad = (Long) fila[0];
            aplicadoManualPorEntidad.put(idEntidad, ((Number) fila[1]).doubleValue());
            aplicadoAutomaticoPorEntidad.put(idEntidad, ((Number) fila[2]).doubleValue());
            aplicadoPrestamosPorEntidad.put(idEntidad, ((Number) fila[3]).doubleValue());
        }
        Map<Long, Double> aplicadoAportesPorEntidad = new LinkedHashMap<>();
        for (Object[] fila : aportesFilas) {
            aplicadoAportesPorEntidad.put((Long) fila[0], ((Number) fila[1]).doubleValue());
        }

        Set<Long> todasLasEntidades = new LinkedHashSet<>();
        todasLasEntidades.addAll(descontadoPorEntidad.keySet());
        todasLasEntidades.addAll(aplicadoPrestamosPorEntidad.keySet());
        todasLasEntidades.addAll(aplicadoAportesPorEntidad.keySet());

        List<DiferenciaParticipeDistribucionBanda> detalle = new ArrayList<>();
        for (Long idEntidad : todasLasEntidades) {
            double descontado = redondear(descontadoPorEntidad.getOrDefault(idEntidad, 0.0));
            double aplicadoPrestamos = redondear(aplicadoPrestamosPorEntidad.getOrDefault(idEntidad, 0.0));
            double aplicadoAportes = redondear(aplicadoAportesPorEntidad.getOrDefault(idEntidad, 0.0));
            double aplicadoTotal = redondear(aplicadoPrestamos + aplicadoAportes);
            double diferencia = redondear(aplicadoTotal - descontado);
            if (Math.abs(diferencia) <= TOLERANCIA_CUADRE) {
                continue;
            }

            Entidad entidad = entidadPorId.get(idEntidad);
            DiferenciaParticipeDistribucionBanda item = new DiferenciaParticipeDistribucionBanda();
            item.setCodigoPetro(entidad != null ? entidad.getRolPetroComercial() : null);
            item.setCedula(entidad != null ? entidad.getNumeroIdentificacion() : null);
            item.setParticipe(entidad != null ? entidad.getRazonSocial() : null);
            item.setDescontado(descontado);
            item.setAplicadoPrestamos(aplicadoPrestamos);
            item.setAplicadoAportes(aplicadoAportes);
            item.setAplicadoTotal(aplicadoTotal);
            item.setDiferencia(diferencia);
            item.setAplicadoManual(redondear(aplicadoManualPorEntidad.getOrDefault(idEntidad, 0.0)));
            item.setAplicadoAutomatico(redondear(aplicadoAutomaticoPorEntidad.getOrDefault(idEntidad, 0.0)));
            detalle.add(item);
        }
        detalle.sort((a, b) -> Double.compare(b.getDiferencia(), a.getDiferencia()));

        double diferenciaTotal = 0.0;
        int recibieronDeMas = 0;
        int recibieronDeMenos = 0;
        for (DiferenciaParticipeDistribucionBanda item : detalle) {
            diferenciaTotal += item.getDiferencia();
            if (item.getDiferencia() > 0) {
                recibieronDeMas++;
            } else {
                recibieronDeMenos++;
            }
        }

        ResultadoDiferenciaDistribucionBanda resultado = new ResultadoDiferenciaDistribucionBanda();
        resultado.setOrigen(origen);
        resultado.setIdOrigen(idOrigen);
        resultado.setDiferenciaTotal(redondear(diferenciaTotal));
        resultado.setParticipesConDiferencia(detalle.size());
        resultado.setRecibieronDeMas(recibieronDeMas);
        resultado.setRecibieronDeMenos(recibieronDeMenos);
        resultado.setDetalle(detalle);
        return resultado;
    }

    @Override
    public ResultadoDetalleDistribucionBanda obtenerDetalle(FiltroDetalleDistribucionBanda filtro) throws Throwable {
        System.out.println("DistribucionBandaService.obtenerDetalle - "
            + (filtro != null ? filtro.getOrigen() + "/" + filtro.getIdOrigen() : null));
        if (filtro == null || filtro.getOrigen() == null || filtro.getIdOrigen() == null) {
            throw new IncomeException("origen e idOrigen son obligatorios");
        }
        validarOrigen(filtro.getOrigen());

        ResultadoDetalleDistribucionBanda resultado = new ResultadoDetalleDistribucionBanda();
        int pagina = filtro.getPagina() != null ? Math.max(0, filtro.getPagina()) : 0;
        int tamanio = filtro.getTamanio() != null && filtro.getTamanio() > 0 ? filtro.getTamanio() : 50;
        resultado.setPagina(pagina);
        resultado.setTamanio(tamanio);

        // "Un origen sin filas devuelve 200 con listas vacías, NO 404" (API-AUDITORIA-BANDAS.md
        // Errores) — a diferencia de /cuadre, acá "no hay datos" es un resultado del filtro.
        long totalFilas = distribucionBandaDaoService.contarDetalleFiltrado(filtro);
        resultado.setTotalFilas(totalFilas);
        if (totalFilas == 0L) {
            return resultado;
        }

        List<DistribucionBanda> filas = distribucionBandaDaoService.selectDetalleFiltrado(filtro);

        // Cuenta contable de los conceptos que NO son CAPITAL (2026-09-02, hueco real: el FE
        // destapó que interés/mora/seguros/aportes nunca mostraban cuenta — ver
        // resolverLineaPlantilla). Se resuelve UNA vez por (origen, idOrigen) — no por fila — y
        // se cachea por (concepto, tipoPrestamo, tipoAporte) para esta llamada: un puñado de
        // combinaciones frente a potencialmente miles de filas.
        Long idPlantilla = resolverIdPlantillaParaOrigen(filtro.getOrigen(), filtro.getIdOrigen());
        Map<String, DetallePlantilla> cacheLineas = new LinkedHashMap<>();

        double totalValor = 0.0;
        Map<String, ResumenConceptoDistribucionBanda> resumen = new LinkedHashMap<>();
        List<FilaDistribucionBanda> filasDto = new ArrayList<>();
        for (DistribucionBanda fila : filas) {
            totalValor += nvl(fila.getValor());

            ResumenConceptoDistribucionBanda porConcepto = resumen.get(fila.getConcepto());
            if (porConcepto == null) {
                porConcepto = new ResumenConceptoDistribucionBanda(fila.getConcepto(), 0.0, 0L);
                resumen.put(fila.getConcepto(), porConcepto);
            }
            porConcepto.setValor(redondear(porConcepto.getValor() + nvl(fila.getValor())));
            porConcepto.setFilas(porConcepto.getFilas() + 1);

            filasDto.add(aFilaDto(fila, idPlantilla, cacheLineas));
        }
        resultado.setTotalValorFiltrado(redondear(totalValor));
        resultado.setResumenPorConcepto(new ArrayList<>(resumen.values()));
        resultado.setFilas(filasDto);
        resultado.setResumenJerarquico(construirResumenJerarquico(filtro, idPlantilla, cacheLineas));
        return resultado;
    }

    /**
     * Idem del asiento de aplicación Petro: {@code plantillaService.codigoByAlterno} necesita la
     * empresa contable de la carga, y {@code CargaArchivo} no la tiene como columna propia — hay
     * que derivarla de sus transferencias vigentes (mismo criterio, ÚNICA definición, en
     * {@code TransferenciaCargaPetroDaoService#resolverEmpresaByCarga}).
     *
     * Solo sabe resolver para {@link DsbnOrigen#CARGA_PETRO} — el mapeo concepto→línea de acá es
     * el de {@code PlantillasCredito.APLICACION_PETRO}; para el resto de los orígenes no hay
     * plantilla que resolver todavía y la cuenta queda vacía, que es la ausencia esperada.
     *
     * NUNCA lanza: es una pantalla de solo lectura, y un problema resolviendo la plantilla (o la
     * empresa) no puede tumbarla — la cuenta de los conceptos sin banda simplemente queda null.
     */
    private Long resolverIdPlantillaParaOrigen(String origen, Long idOrigen) {
        if (!DsbnOrigen.CARGA_PETRO.equals(origen) || idOrigen == null) {
            return null;
        }
        try {
            Long idEmpresa = transferenciaCargaPetroDaoService.resolverEmpresaByCarga(idOrigen);
            return plantillaService.codigoByAlterno(PlantillasCredito.APLICACION_PETRO, idEmpresa);
        } catch (Throwable e) {
            System.err.println("DistribucionBandaService: no se pudo resolver la plantilla de la carga "
                + idOrigen + " para mostrar cuentas contables (la pantalla sigue, sin esas cuentas): "
                + e.getMessage());
            return null;
        }
    }

    /**
     * Cuenta contable de un concepto que NO es CAPITAL — espeja el mapeo concepto/tipo → línea
     * que usa {@code CobroPetroContableServiceImpl.contabilizarAplicacion} para armar el asiento
     * de aplicación (2026-09-02): misma línea, para que esta pantalla de auditoría muestre la
     * MISMA cuenta que terminó afectada, nunca una calculada aparte.
     *
     * Cacheada por (concepto, tipoPrestamo, tipoAporte): son un puñado de combinaciones
     * distintas frente a potencialmente miles de filas/grupos — ni una fila entra sin pasar por
     * el cache.
     *
     * @return la línea si existe; {@code null} si el concepto no tiene mapeo conocido
     *         ({@link DsbnConcepto#CAPITAL}, que no pasa por acá, e
     *         {@link DsbnConcepto#INTERES_VENCIDO}, que hoy siempre es cero y por eso nunca tuvo
     *         línea — ver la guarda en {@code contabilizarAplicacion}) o si la plantilla no
     *         tiene esa línea configurada. Nunca lanza: ausencia de configuración en una
     *         pantalla de solo lectura no es un error, es una cuenta vacía.
     */
    private DetallePlantilla resolverLineaPlantilla(String concepto, Long idTipoPrestamo, Long idTipoAporte,
            Long idPlantilla, Map<String, DetallePlantilla> cache) throws Throwable {
        if (idPlantilla == null || concepto == null) {
            return null;
        }
        String clave = concepto + "|" + idTipoPrestamo + "|" + idTipoAporte;
        if (cache.containsKey(clave)) {
            return cache.get(clave);
        }

        DetallePlantilla linea = null;
        if (DsbnConcepto.INTERES_ORDINARIO.equals(concepto)) {
            if (idTipoPrestamo != null) {
                linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                    idPlantilla, CrdLineaAsiento.INTERES_ORDINARIO_POR_COBRAR, idTipoPrestamo.intValue());
            }
        } else if (DsbnConcepto.INTERES_MORA.equals(concepto)) {
            if (idTipoPrestamo != null) {
                linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
                    idPlantilla, CrdLineaAsiento.INTERES_MORA_POR_COBRAR, idTipoPrestamo.intValue());
            }
        } else if (DsbnConcepto.SEGURO_DESGRAVAMEN.equals(concepto)) {
            linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, CrdLineaAsiento.SEGURO_DESGRAVAMEN);
        } else if (DsbnConcepto.SEGURO_INCENDIO.equals(concepto)) {
            if (idTipoPrestamo != null) {
                Integer aux1 = idTipoPrestamo == TIPO_PRESTAMO_HIPOTECARIO ? AUX1_SEGURO_HIPOTECARIO
                    : idTipoPrestamo == TIPO_PRESTAMO_PRENDARIO ? AUX1_SEGURO_PRENDARIO : null;
                if (aux1 != null) {
                    linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, aux1);
                }
            }
        } else if (DsbnConcepto.APORTE.equals(concepto)) {
            if (idTipoAporte != null) {
                if (idTipoAporte == TIPO_APORTE_JUBILACION) {
                    linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, CrdLineaAsiento.APORTES_JUBILACION);
                } else if (idTipoAporte == TIPO_APORTE_CESANTIA) {
                    linea = detallePlantillaDaoService.selectByPlantillaYAuxiliar(idPlantilla, CrdLineaAsiento.APORTES_CESANTIA);
                }
            }
        }
        // CAPITAL no pasa por acá (ya trae su cuenta desde la banda); INTERES_VENCIDO no tiene
        // mapeo (siempre cero hoy) y cualquier otro caso sin línea queda en null — todos
        // ausencias legítimas, no errores.

        cache.put(clave, linea);
        return linea;
    }

    /**
     * Vista RESUMEN — API-AUDITORIA-BANDAS.md "Las DOS vistas" (2026-09-02, decisión del
     * usuario). A diferencia de {@code resumenPorConcepto} de arriba (que suma la PÁGINA que ya
     * trajo {@code selectDetalleFiltrado}), esto corre una consulta GROUP BY aparte sobre el
     * MISMO filtro sin paginar — el conjunto filtrado completo puede ser miles de filas y el
     * resumen tiene que verlas todas, no las 50 de la página. Primer nivel CONCEPTO, segundo
     * cuenta contable + banda (§3 del plan: agrupar por cuenta arriba fusiona mora con interés
     * ordinario, que comparten cuenta). La cuenta de los conceptos sin banda se resuelve sobre
     * los GRUPOS ya agregados por la consulta (un puñado), nunca fila por fila.
     */
    private List<ResumenJerarquicoConcepto> construirResumenJerarquico(FiltroDetalleDistribucionBanda filtro,
            Long idPlantilla, Map<String, DetallePlantilla> cacheLineas) throws Throwable {
        List<Object[]> filas = distribucionBandaDaoService.selectResumenJerarquicoFiltrado(filtro);

        Map<String, ResumenJerarquicoConcepto> porConcepto = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            String concepto = (String) fila[0];
            Long idBanda = (Long) fila[1];
            String etiqueta = (String) fila[2];
            String cuentaContable = (String) fila[3];
            String nombreCuenta = (String) fila[4];
            Long idTipoPrestamo = (Long) fila[5];
            Long idTipoAporte = (Long) fila[6];
            double valor = redondear(((Number) fila[7]).doubleValue());
            long filasCuenta = ((Number) fila[8]).longValue();

            if (cuentaContable == null && idBanda == null) {
                DetallePlantilla linea = resolverLineaPlantilla(concepto, idTipoPrestamo, idTipoAporte,
                    idPlantilla, cacheLineas);
                if (linea != null && linea.getPlanCuenta() != null) {
                    cuentaContable = linea.getPlanCuenta().getCuentaContable();
                    nombreCuenta = linea.getPlanCuenta().getNombre();
                }
            }

            ResumenJerarquicoConcepto nivelConcepto = porConcepto.get(concepto);
            if (nivelConcepto == null) {
                nivelConcepto = new ResumenJerarquicoConcepto();
                nivelConcepto.setConcepto(concepto);
                porConcepto.put(concepto, nivelConcepto);
            }
            nivelConcepto.setValor(redondear(nivelConcepto.getValor() + valor));
            nivelConcepto.setFilas(nivelConcepto.getFilas() + filasCuenta);

            ResumenJerarquicoCuentaBanda item = new ResumenJerarquicoCuentaBanda();
            item.setIdBanda(idBanda);
            item.setBanda(etiqueta);
            item.setCuentaContable(cuentaContable);
            item.setNombreCuenta(nombreCuenta);
            item.setValor(valor);
            item.setFilas(filasCuenta);
            nivelConcepto.getDetalle().add(item);
        }
        return new ArrayList<>(porConcepto.values());
    }

    private FilaDistribucionBanda aFilaDto(DistribucionBanda fila, Long idPlantilla,
            Map<String, DetallePlantilla> cacheLineas) throws Throwable {
        FilaDistribucionBanda dto = new FilaDistribucionBanda();
        dto.setId(fila.getCodigo());
        dto.setConcepto(fila.getConcepto());
        dto.setValor(fila.getValor());

        if (fila.getEntidad() != null) {
            dto.setIdEntidad(fila.getEntidad().getCodigo());
            dto.setParticipe(fila.getEntidad().getRazonSocial());
            dto.setCedula(fila.getEntidad().getNumeroIdentificacion());
            dto.setCodigoAsoprep(fila.getEntidad().getRolPetroComercial());
        }
        if (fila.getPrestamo() != null) {
            dto.setIdPrestamo(fila.getPrestamo().getCodigo());
        }
        if (fila.getDetallePrestamo() != null) {
            Double numeroCuota = fila.getDetallePrestamo().getNumeroCuota();
            dto.setNumeroCuota(numeroCuota != null ? numeroCuota.longValue() : null);
        }
        dto.setFechaVencimiento(fila.getFechaVencimiento());
        dto.setFechaAplicacion(fila.getFechaAplicacion());
        if (fila.getProducto() != null) {
            dto.setIdProducto(fila.getProducto().getCodigo());
            dto.setProducto(fila.getProducto().getNombre());
        }
        dto.setIdTipoPrestamo(fila.getTipoPrestamo());
        dto.setIdTipoAporte(fila.getTipoAporte());
        dto.setTipoCartera(fila.getTipoCartera());
        dto.setDias(fila.getDias());
        if (fila.getBanda() != null) {
            dto.setIdBanda(fila.getBanda().getCodigo());
            dto.setBanda(fila.getEtiqueta());
            if (fila.getBanda().getPlanCuenta() != null) {
                dto.setCuentaContable(fila.getBanda().getPlanCuenta().getCuentaContable());
                dto.setNombreCuenta(fila.getBanda().getPlanCuenta().getNombre());
            }
        } else {
            // 2026-09-02, hueco real reportado por el FE al enganchar el resumen: la cuenta
            // contable SOLO se resolvía desde la banda, y la banda solo existe para CAPITAL —
            // interés ordinario/mora, seguros y aportes nunca mostraban cuenta, con o sin CNT
            // conectado. Se resuelve al leer, por (concepto, tipoPrestamo/tipoAporte) — ver
            // resolverLineaPlantilla. Ausencia de mapeo o de plantilla: cuenta null, no error.
            DetallePlantilla linea = resolverLineaPlantilla(fila.getConcepto(), fila.getTipoPrestamo(),
                fila.getTipoAporte(), idPlantilla, cacheLineas);
            if (linea != null && linea.getPlanCuenta() != null) {
                dto.setCuentaContable(linea.getPlanCuenta().getCuentaContable());
                dto.setNombreCuenta(linea.getPlanCuenta().getNombre());
            }
        }
        dto.setIdAsiento(fila.getIdAsiento());
        return dto;
    }

    @Override
    public List<OrigenDistribucionBandaResumen> listarOrigenes(String origen, LocalDate fechaDesde,
            LocalDate fechaHasta, Integer limite) throws Throwable {
        System.out.println("DistribucionBandaService.listarOrigenes - origen=" + origen);
        if (origen != null) {
            validarOrigen(origen);
        }

        List<Object[]> filas = distribucionBandaDaoService.selectOrigenesDistintos(
            origen, fechaDesde, fechaHasta, limite != null ? limite : 50);

        List<OrigenDistribucionBandaResumen> resultado = new ArrayList<>();
        if (filas == null) {
            return resultado;
        }
        for (Object[] fila : filas) {
            String origenFila = (String) fila[0];
            Long idOrigenFila = (Long) fila[1];
            LocalDate fechaFila = (LocalDate) fila[2];

            OrigenDistribucionBandaResumen dto = new OrigenDistribucionBandaResumen();
            dto.setOrigen(origenFila);
            dto.setIdOrigen(idOrigenFila);
            dto.setFecha(fechaFila);

            List<DistribucionBanda> distribuciones = distribucionBandaDaoService.selectByOrigen(origenFila, idOrigenFila);
            double distribuido = 0.0;
            if (distribuciones != null) {
                for (DistribucionBanda distribucion : distribuciones) {
                    distribuido += nvl(distribucion.getValor());
                }
            }
            dto.setDistribuido(redondear(distribuido));

            if (DsbnOrigen.CARGA_PETRO.equals(origenFila)) {
                CargaArchivo carga = cargaArchivoDaoService.find(new CargaArchivo(), idOrigenFila);
                dto.setDescripcion("Carga Petro"
                    + (carga != null && carga.getMesAfectacion() != null ? " " + carga.getMesAfectacion() : "")
                    + (carga != null && carga.getAnioAfectacion() != null ? "/" + carga.getAnioAfectacion() : ""));

                // Mismo defecto y misma corrección que obtenerCuadre (2026-09-02): "recibido"
                // tiene que salir de las transferencias, no de los pagos que el propio reparto
                // generó — ver el comentario detallado allá.
                double recibido = redondear(transferenciaCargaPetroDaoService.sumaValorVigentesByCarga(idOrigenFila));
                dto.setCuadra(Math.abs(recibido - distribuido) <= TOLERANCIA_CUADRE);
            } else {
                dto.setDescripcion(origenFila + " " + idOrigenFila);
                dto.setCuadra(null);
            }

            resultado.add(dto);
        }
        return resultado;
    }

    private void validarOrigen(String origen) throws Throwable {
        if (origen == null || !ORIGENES_VALIDOS.contains(origen)) {
            throw new IncomeException(ERR_ORIGEN_INVALIDO + ": '" + origen + "' no es un origen válido"
                + " (" + String.join(", ", ORIGENES_VALIDOS) + ")");
        }
    }

    private String nombreEstadoAsiento(Long estado) {
        if (estado == null) {
            return null;
        }
        if (estado == EstadoAsiento.ACTIVO) {
            return "ACTIVO";
        }
        if (estado == EstadoAsiento.ANULADO) {
            return "ANULADO";
        }
        if (estado == EstadoAsiento.REVERSADO) {
            return "REVERSADO";
        }
        if (estado == EstadoAsiento.INCOMPLETO) {
            return "INCOMPLETO";
        }
        return String.valueOf(estado);
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return java.math.BigDecimal.valueOf(valor).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
