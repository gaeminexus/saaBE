package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CargaArchivoDaoService;
import com.saa.ejb.crd.dao.DistribucionBandaDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ConfiguracionContabilidadService;
import com.saa.ejb.crd.service.DistribucionBandaService;
import com.saa.ejb.crd.service.dto.AsientoResumenDsbn;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.ejb.crd.service.dto.FiltroDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.FilaDistribucionBanda;
import com.saa.ejb.crd.service.dto.OrigenDistribucionBandaResumen;
import com.saa.ejb.crd.service.dto.ResultadoClasificacionBanda;
import com.saa.ejb.crd.service.dto.ResultadoCuadreDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResultadoDetalleDistribucionBanda;
import com.saa.ejb.crd.service.dto.ResumenConceptoDistribucionBanda;
import com.saa.model.cnt.Asiento;
import com.saa.model.crd.BandaProducto;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.DistribucionBanda;
import com.saa.model.crd.PagoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.Producto;
import com.saa.rubros.DsbnConcepto;
import com.saa.rubros.DsbnOrigen;
import com.saa.rubros.EstadoAsiento;
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
    private com.saa.ejb.cnt.dao.AsientoDaoService asientoDaoService;

    // ========================================================================
    // Escritura — AL APLICAR el pago, no al contabilizar (PLAN-AUDITORIA-BANDAS.md §1)
    // ========================================================================

    @Override
    public Map<Long, ResultadoClasificacionBanda> registrarDistribucionCargaPetro(Long idCarga, Long idEmpresa,
            List<PagoPrestamo> pagos, String usuario) throws Throwable {
        System.out.println("DistribucionBandaService.registrarDistribucionCargaPetro - Carga: " + idCarga
            + " - Pagos: " + (pagos != null ? pagos.size() : 0));

        // Corrección 2026-09-02: se devuelve para que contabilizarAplicacion NO vuelva a llamar
        // a clasificar() por los mismos pagos al armar el asiento — "clasificar una vez por
        // pago, que ambos consuman lo mismo", no duplicar las consultas de clasificación en el
        // mismo proceso de 20+ minutos que ya se estabilizó por otro motivo hoy.
        Map<Long, ResultadoClasificacionBanda> clasificacionPorPago = new LinkedHashMap<>();

        // Idempotente por (origen, idOrigen) — reprocesar REEMPLAZA, no duplica (§5.1 punto 2).
        int borradas = distribucionBandaDaoService.eliminarPorOrigen(DsbnOrigen.CARGA_PETRO, idCarga);
        if (borradas > 0) {
            System.out.println("  " + borradas + " fila(s) previa(s) de la carga " + idCarga + " reemplazadas.");
        }

        if (pagos == null || pagos.isEmpty()) {
            System.out.println("  Carga " + idCarga + " sin pagos de préstamo — nada que distribuir.");
            return clasificacionPorPago;
        }

        LocalDateTime ahora = LocalDateTime.now();
        int filasEscritas = 0;

        // Corrección 2026-09-02 (URGENTE, detectado en una carga real de 1167 pagos: ~2.300
        // consultas, más de dos minutos solo en clasificar): la configuración de bandas NO
        // cambia dentro de una carga, y las combinaciones distintas de (producto, empresa,
        // tipoCartera, fecha) son un puñado frente a miles de pagos. Cache LOCAL a esta
        // llamada — vive lo que dura el proceso, nunca un campo de clase ni entre cargas (la
        // fecha es parte de la clave justo porque la vigencia se resuelve a esa fecha). Mismo
        // patrón que CacheVigenciaParticipe (tormenta de vigencias) y el cache por producto del
        // G48 (hallazgo 3a) de hoy — acá había quedado sin hacer.
        Map<String, List<BandaProductoDetalle>> cacheRangosPorClave = new LinkedHashMap<>();

        for (PagoPrestamo pago : pagos) {
            Prestamo prestamo = pago.getPrestamo();
            if (prestamo == null || prestamo.getEntidad() == null) {
                throw new IncomeException("El pago " + pago.getCodigo() + " de la carga " + idCarga
                    + " no tiene préstamo o partícipe asociado; no se puede auditar su distribución.");
            }
            Producto producto = prestamo.getProducto();
            if (producto == null) {
                throw new IncomeException("El pago " + pago.getCodigo() + " de la carga " + idCarga
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
                    throw new IncomeException("El pago " + pago.getCodigo() + " de la carga " + idCarga
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

                DistribucionBanda fila = filaBase(DsbnOrigen.CARGA_PETRO, idCarga, DsbnConcepto.CAPITAL,
                    capital, prestamo, cuota, producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora);
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
                throw new IncomeException("El pago " + pago.getCodigo() + " de la carga " + idCarga
                    + " tiene interés, mora o seguro de incendio pero su producto no tiene tipo de"
                    + " préstamo asignado; no se puede clasificar su distribución.");
            }

            if (interes > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.INTERES_ORDINARIO, interes, prestamo, pago.getDetallePrestamo(),
                    producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (mora > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.INTERES_MORA, mora, prestamo, pago.getDetallePrestamo(),
                    producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (interesVencido > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.INTERES_VENCIDO, interesVencido, prestamo, pago.getDetallePrestamo(),
                    producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (desgravamen > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.SEGURO_DESGRAVAMEN, desgravamen, prestamo, pago.getDetallePrestamo(),
                    producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
            if (seguroIncendio > TOLERANCIA) {
                distribucionBandaDaoService.save(filaBase(DsbnOrigen.CARGA_PETRO, idCarga,
                    DsbnConcepto.SEGURO_INCENDIO, seguroIncendio, prestamo, pago.getDetallePrestamo(),
                    producto, idTipoPrestamo, null, fechaAplicacion, usuario, ahora), null);
                filasEscritas++;
            }
        }

        System.out.println("  ✅ Distribución en bandas registrada - Carga " + idCarga
            + " - " + filasEscritas + " fila(s)");
        return clasificacionPorPago;
    }

    private DistribucionBanda filaBase(String origen, Long idOrigen, String concepto, double valor,
            Prestamo prestamo, DetallePrestamo cuota, Producto producto, Long idTipoPrestamo,
            Long idTipoAporte, LocalDate fechaAplicacion, String usuario, LocalDateTime ahora) {
        DistribucionBanda fila = new DistribucionBanda();
        fila.setOrigen(origen);
        fila.setIdOrigen(idOrigen);
        fila.setConcepto(concepto);
        fila.setValor(redondear(valor));
        fila.setEntidad(prestamo.getEntidad());
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

            filasDto.add(aFilaDto(fila));
        }
        resultado.setTotalValorFiltrado(redondear(totalValor));
        resultado.setResumenPorConcepto(new ArrayList<>(resumen.values()));
        resultado.setFilas(filasDto);
        return resultado;
    }

    private FilaDistribucionBanda aFilaDto(DistribucionBanda fila) {
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
