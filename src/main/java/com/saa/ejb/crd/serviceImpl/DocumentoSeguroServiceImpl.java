package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.DeclaracionPlazoVencidoDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.DocumentoSeguroDaoService;
import com.saa.ejb.crd.dao.CuotaSeguroDaoService;
import com.saa.ejb.crd.dao.EventoPrestamoDaoService;
import com.saa.ejb.crd.dao.PagoPrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoSeguroDaoService;
import com.saa.ejb.crd.service.DetallePrestamoService;
import com.saa.ejb.crd.service.DocumentoSeguroService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.CandidatoSeguro;
import com.saa.ejb.crd.service.dto.CuotaDistribucionPreview;
import com.saa.ejb.crd.service.dto.CuotaNoRevertida;
import com.saa.ejb.crd.service.dto.DocumentoSeguroDTO;
import com.saa.ejb.crd.service.dto.ExclusionSeguro;
import com.saa.ejb.crd.service.dto.NovedadesSeguro;
import com.saa.ejb.crd.service.dto.PreviewDistribucionSeguro;
import com.saa.ejb.crd.service.dto.PrestamoDistribucionPreview;
import com.saa.ejb.crd.service.dto.PrestamoSeguroDTO;
import com.saa.ejb.crd.service.dto.PrestamoSinCuotasEnVigencia;
import com.saa.ejb.crd.service.dto.ResultadoAnularSeguro;
import com.saa.ejb.crd.service.dto.SolicitudDocumentoSeguro;
import com.saa.ejb.crd.service.dto.SolicitudGenerarListado;
import com.saa.ejb.crd.service.dto.SolicitudNotaSeguro;
import com.saa.model.crd.CuotaSeguro;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.DocumentoSeguro;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.Prestamo;
import com.saa.model.crd.PrestamoSeguro;
import com.saa.model.crd.DeclaracionPlazoVencido;
import com.saa.rubros.CampoSeguroCuota;
import com.saa.rubros.ClaseDocumentoSeguro;
import com.saa.rubros.EstadoCuotaPrestamo;
import com.saa.rubros.EstadoDocumentoSeguro;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.NovedadPrestamoSeguro;
import com.saa.rubros.TipoSeguro;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * @see DocumentoSeguroService
 */
@Stateless
public class DocumentoSeguroServiceImpl implements DocumentoSeguroService {

    private static final double TOLERANCIA = 0.01;

    @EJB
    private DocumentoSeguroDaoService documentoSeguroDaoService;

    @EJB
    private PrestamoSeguroDaoService prestamoSeguroDaoService;

    @EJB
    private CuotaSeguroDaoService cuotaSeguroDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private DetallePrestamoService detallePrestamoService;

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private EventoPrestamoDaoService eventoPrestamoDaoService;

    @EJB
    private DeclaracionPlazoVencidoDaoService declaracionPlazoVencidoDaoService;

    // ========================================================================
    // ÍTEM 2/3 — Listado (paso 1)
    // ========================================================================

    @Override
    public List<CandidatoSeguro> previewListado(Long tipoSeguro, LocalDate fechaCorte) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.previewListado - tipoSeguro: " + tipoSeguro
            + " - fechaCorte: " + fechaCorte);
        List<ItemCandidato> candidatos = calcularCandidatos(tipoSeguro, fechaCorte);
        List<CandidatoSeguro> resultado = new ArrayList<>();
        for (ItemCandidato item : candidatos) {
            resultado.add(toCandidatoDTO(item));
        }
        return resultado;
    }

    @Override
    public DocumentoSeguroDTO generarListado(SolicitudGenerarListado solicitud) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.generarListado - solicitud: "
            + (solicitud != null ? solicitud.getTipoSeguro() : null));
        if (solicitud == null || solicitud.getTipoSeguro() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": tipoSeguro es obligatorio");
        }
        if (solicitud.getFechaCorte() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": fechaCorte es obligatoria");
        }
        if (vacio(solicitud.getUsuario())) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }

        List<ItemCandidato> candidatos = calcularCandidatos(solicitud.getTipoSeguro(), solicitud.getFechaCorte());

        if (solicitud.getTipoSeguro() != TipoSeguro.DESGRAVAMEN) {
            List<String> sinSuma = new ArrayList<>();
            for (ItemCandidato item : candidatos) {
                if (item.sinSumaAsegurada) {
                    sinSuma.add(numeroPrestamoDe(item.prestamo));
                }
            }
            if (!sinSuma.isEmpty()) {
                throw new IncomeException(ERR_HAY_PRESTAMOS_SIN_SUMA_ASEGURADA
                    + ": los siguientes préstamos no tienen suma asegurada (PRSTVLAS): "
                    + String.join(", ", sinSuma));
            }
        }

        DocumentoSeguro doc = new DocumentoSeguro();
        doc.setTipoSeguro(solicitud.getTipoSeguro());
        doc.setClase(ClaseDocumentoSeguro.FACTURA);
        doc.setEstado(EstadoDocumentoSeguro.LISTADO_ENVIADO);
        doc.setFechaCorte(solicitud.getFechaCorte());
        doc.setObservacion(solicitud.getObservacion());
        doc.setUsuarioListado(solicitud.getUsuario());
        doc.setFechaListado(LocalDateTime.now());
        doc = documentoSeguroDaoService.save(doc, null);

        for (ItemCandidato item : candidatos) {
            PrestamoSeguro ps = new PrestamoSeguro();
            ps.setDocumento(doc);
            ps.setPrestamo(item.prestamo);
            ps.setNovedad(NovedadPrestamoSeguro.ORIGINAL);
            ps.setBase(redondear(item.base));
            prestamoSeguroDaoService.save(ps, null);
        }

        System.out.println("  Listado generado - documento: " + doc.getCodigo()
            + " - préstamos: " + candidatos.size());
        return toDTO(doc);
    }

    /** Universo elegible con su base — compartido por {@link #previewListado} y {@link #generarListado}. */
    private List<ItemCandidato> calcularCandidatos(Long tipoSeguro, LocalDate fechaCorte) throws Throwable {
        if (tipoSeguro == null || (tipoSeguro != TipoSeguro.DESGRAVAMEN && tipoSeguro != TipoSeguro.INCENDIO
                && tipoSeguro != TipoSeguro.PRENDARIO)) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": tipoSeguro debe ser 1, 2 o 3");
        }
        if (fechaCorte == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": fechaCorte es obligatoria");
        }

        // Elegibles: PRSTIDST IN (2 VIGENTE, 11 EN_MORA). El 8 (DE PLAZO VENCIDO) queda fuera
        // porque ni siquiera se incluye en la lista de estados (D13 del diseño de plazo vencido).
        List<Long> estados = Arrays.asList((long) EstadoPrestamo.VIGENTE, (long) EstadoPrestamo.EN_MORA);
        List<Prestamo> prestamos = prestamoDaoService.selectByEstados(estados);

        List<Prestamo> filtrados = new ArrayList<>();
        for (Prestamo prestamo : prestamos) {
            if (tipoSeguro == TipoSeguro.DESGRAVAMEN) {
                filtrados.add(prestamo);
                continue;
            }
            Long tipoPrestamo = tipoPrestamoDe(prestamo);
            long esperado = tipoSeguro == TipoSeguro.INCENDIO ? TipoSeguro.INCENDIO : TipoSeguro.PRENDARIO;
            if (tipoPrestamo != null && tipoPrestamo == esperado) {
                filtrados.add(prestamo);
            }
        }

        // Base en LOTE (ÍTEM 0c/ÍTEM 3): nunca una consulta por préstamo. Se carga SIEMPRE,
        // para los tres tipos — S15 (plazo terminado) la necesita incendio/prendario también,
        // no solo desgravamen.
        List<Long> idsPrestamo = new ArrayList<>();
        for (Prestamo p : filtrados) {
            idsPrestamo.add(p.getCodigo());
        }
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(idsPrestamo);
        Map<Long, double[]> pagosPorCuota = tipoSeguro == TipoSeguro.DESGRAVAMEN
            ? cargarPagosPorCuota(cuotasPorPrestamo) : null;

        List<ItemCandidato> resultado = new ArrayList<>();
        for (Prestamo p : filtrados) {
            List<DetallePrestamo> cuotas = cuotasPorPrestamo.get(p.getCodigo());

            // S15 (decisión del usuario, sql/312): el plazo terminado queda fuera del listado —
            // hace falta al menos una cuota no PAGADA(4)/no CANCELADA_ANTICIPADA(7) con
            // vencimiento POSTERIOR a fechaCorte. Una cuota que vence el mismo día del corte no
            // alcanza (mismo criterio "> corte", no ">="). Sin eso, el préstamo no es candidato
            // en NINGUNO de los tres tipos de seguro, aunque siga VIGENTE/EN_MORA.
            boolean tienePlazoVigente = false;
            if (cuotas != null) {
                for (DetallePrestamo cuota : cuotas) {
                    if (esPagadaOCancelada(cuota) || cuota.getFechaVencimiento() == null) {
                        continue;
                    }
                    if (cuota.getFechaVencimiento().toLocalDate().isAfter(fechaCorte)) {
                        tienePlazoVigente = true;
                        break;
                    }
                }
            }
            if (!tienePlazoVigente) {
                continue;
            }

            ItemCandidato item = new ItemCandidato();
            item.prestamo = p;
            if (tipoSeguro == TipoSeguro.DESGRAVAMEN) {
                double saldoCapital = 0.0;
                if (cuotas != null) {
                    for (DetallePrestamo cuota : cuotas) {
                        if (esPagadaOCancelada(cuota)) {
                            continue;
                        }
                        double[] pagos = pagosPorCuota.get(cuota.getCodigo());
                        double capitalPagado = pagos != null ? pagos[4] : 0.0;
                        saldoCapital += Math.max(0.0, redondear(nvl(cuota.getCapital()) - capitalPagado));
                    }
                }
                item.base = redondear(saldoCapital);
                item.sinSumaAsegurada = false;
            } else {
                double suma = nvl(p.getValorAsegurado());
                item.base = redondear(suma);
                item.sinSumaAsegurada = suma <= 0.0;
            }
            resultado.add(item);
        }
        return resultado;
    }

    private CandidatoSeguro toCandidatoDTO(ItemCandidato item) {
        Prestamo p = item.prestamo;
        CandidatoSeguro dto = new CandidatoSeguro();
        dto.setIdPrestamo(p.getCodigo());
        dto.setNumeroPrestamo(numeroPrestamoDe(p));
        if (p.getEntidad() != null) {
            dto.setNombreParticipe(p.getEntidad().getRazonSocial());
            dto.setCedula(p.getEntidad().getNumeroIdentificacion());
        }
        dto.setTipoPrestamo(nombreTipoPrestamoDe(p));
        dto.setEstadoPrestamo(p.getIdEstado());
        dto.setBase(item.base);
        dto.setSinSumaAsegurada(item.sinSumaAsegurada);
        return dto;
    }

    // ========================================================================
    // ÍTEM 4 — Registrar el documento de la aseguradora (paso 2)
    // ========================================================================

    @Override
    public DocumentoSeguroDTO registrarDocumento(Long id, SolicitudDocumentoSeguro solicitud) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.registrarDocumento - id: " + id);
        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        DocumentoSeguro doc = buscarDocumento(id);
        if (doc.getEstado() == null || (doc.getEstado() != EstadoDocumentoSeguro.LISTADO_ENVIADO
                && doc.getEstado() != EstadoDocumentoSeguro.DOCUMENTO_REGISTRADO)) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": el documento " + id
                + " está en estado " + doc.getEstado() + "; tiene que estar en 1 o 2 para registrar/corregir"
                + " el documento de la aseguradora");
        }
        if (solicitud.getValorTotal() == null || solicitud.getValorTotal() <= 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": valorTotal debe ser mayor a 0");
        }
        if (solicitud.getFechaInicio() == null || solicitud.getFechaFin() == null) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": fechaInicio y fechaFin son obligatorias");
        }
        if (solicitud.getFechaFin().isBefore(solicitud.getFechaInicio())) {
            throw new IncomeException(ERR_FECHA_INVALIDA + ": fechaFin no puede ser anterior a fechaInicio");
        }
        if (vacio(solicitud.getClaveAcceso())) {
            throw new IncomeException(ERR_CLAVE_ACCESO_OBLIGATORIA + ": claveAcceso es obligatoria");
        }
        DocumentoSeguro existente = documentoSeguroDaoService.selectByClaveAccesoNormalizada(solicitud.getClaveAcceso());
        if (existente != null && !existente.getCodigo().equals(id)) {
            throw new IncomeException(ERR_CLAVE_ACCESO_DUPLICADA + ": la clave de acceso " + solicitud.getClaveAcceso()
                + " ya está registrada en el documento " + existente.getCodigo());
        }

        doc.setAseguradora(solicitud.getAseguradora());
        doc.setRuc(solicitud.getRuc());
        doc.setNumeroPoliza(solicitud.getNumeroPoliza());
        doc.setNumeroDocumento(solicitud.getNumeroDocumento());
        doc.setClaveAcceso(solicitud.getClaveAcceso());
        doc.setFechaEmision(solicitud.getFechaEmision());
        doc.setFechaInicio(solicitud.getFechaInicio());
        doc.setFechaFin(solicitud.getFechaFin());
        doc.setTasa(solicitud.getTasa());
        doc.setValorTotal(redondear(solicitud.getValorTotal()));
        doc.setEstado(EstadoDocumentoSeguro.DOCUMENTO_REGISTRADO);
        doc.setUsuarioDocumento(solicitud.getUsuario());
        doc.setFechaDocumento(LocalDateTime.now());
        doc = documentoSeguroDaoService.save(doc, doc.getCodigo());
        return toDTO(doc);
    }

    // ========================================================================
    // ÍTEM 5 — Distribución (paso 3)
    // ========================================================================

    @Override
    public PreviewDistribucionSeguro previewDistribucion(Long id) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.previewDistribucion - id: " + id);
        DocumentoSeguro doc = buscarDocumento(id);
        if (doc.getClase() != null && doc.getClase() == ClaseDocumentoSeguro.NOTA_CREDITO) {
            return previewReduccionNC(doc);
        }
        ResultadoCalculoDistribucion calculo = calcularDistribucionAditiva(doc);
        return toPreviewDTO(calculo);
    }

    @Override
    public DocumentoSeguroDTO distribuir(Long id, String usuario) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.distribuir - id: " + id + " - usuario: " + usuario);
        DocumentoSeguro doc = buscarDocumento(id);
        if (doc.getEstado() == null || doc.getEstado() != EstadoDocumentoSeguro.DOCUMENTO_REGISTRADO) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": el documento " + id + " está en estado "
                + doc.getEstado() + "; tiene que estar en 2 (DOCUMENTO_REGISTRADO) para distribuir");
        }

        long campo = campoDe(doc.getTipoSeguro());
        List<Long> idsFamilia = idsFamilia(doc);

        if (doc.getClase() != null && doc.getClase() == ClaseDocumentoSeguro.NOTA_CREDITO) {
            ResultadoReduccionNC calculo = calcularReduccionNC(doc);
            // La NC solo RESTA: nunca puede chocar con otra póliza (no abre PSCT nuevo sobre una
            // cuota ajena), así que no corresponde el choque CUOTA_YA_CUBIERTA acá.
            for (ItemReduccionCuota item : calculo.cuotas) {
                if (item.aplicado <= 0.0) {
                    continue;
                }
                double anterior = seguroActualDe(item.cuota, campo);
                escribirSeguro(item.cuota, campo, item.nuevo);
                detallePrestamoService.saveSingle(item.cuota);

                CuotaSeguro psct = new CuotaSeguro();
                psct.setDocumento(doc);
                psct.setPrestamoSeguro(item.prestamoSeguro);
                psct.setIdCuota(item.cuota.getCodigo());
                psct.setCampo(campo);
                psct.setSaldoInicialCapital(item.saldoInicialCapital);
                psct.setValorAnterior(anterior);
                psct.setValorNuevo(item.nuevo);
                cuotaSeguroDaoService.save(psct, null);
            }
            for (ItemReduccionPrestamo itemPrestamo : calculo.prestamos) {
                PrestamoSeguro ps = itemPrestamo.prestamoSeguro;
                ps.setMesesCubiertos((long) itemPrestamo.cuotas.size());
                ps.setCuotasRepartidas((long) itemPrestamo.cuotas.size());
                prestamoSeguroDaoService.save(ps, ps.getCodigo());
            }
            doc.setEstado(EstadoDocumentoSeguro.DISTRIBUIDO);
            doc.setUsuarioDistribucion(usuario);
            doc.setFechaDistribucion(LocalDateTime.now());
            doc = documentoSeguroDaoService.save(doc, doc.getCodigo());
            return toDTO(doc);
        }

        ResultadoCalculoDistribucion calculo = calcularDistribucionAditiva(doc);
        if (!calculo.cuadra) {
            throw new IncomeException(ERR_DISTRIBUCION_NO_CUADRA + ": el documento " + id + " no cuadra —"
                + " valorTotal $" + redondear(doc.getValorTotal()) + ", préstamos $" + redondear(calculo.sumaPrestamos)
                + ", cuotas $" + redondear(calculo.sumaCuotas) + "; no se graba nada");
        }

        List<Long> idsCuotasConPeso = new ArrayList<>();
        for (ItemPrestamoCalculo item : calculo.items) {
            for (ItemCuotaCalculo c : item.cuotasConPeso) {
                idsCuotasConPeso.add(c.cuota.getCodigo());
            }
        }
        List<CuotaSeguro> conflictos = cuotaSeguroDaoService.selectVigentesByCuotasYCampoExcluyendo(
            idsCuotasConPeso, campo, idsFamilia);
        if (!conflictos.isEmpty()) {
            List<String> cuotasConflicto = new ArrayList<>();
            for (CuotaSeguro c : conflictos) {
                cuotasConflicto.add("cuota " + c.getIdCuota() + " (documento " + c.getDocumento().getCodigo() + ")");
            }
            throw new IncomeException(ERR_CUOTA_YA_CUBIERTA + ": las siguientes cuotas ya tienen un seguro vigente"
                + " de otra póliza del mismo tipo: " + String.join("; ", cuotasConflicto));
        }

        for (ItemPrestamoCalculo item : calculo.items) {
            PrestamoSeguro ps = new PrestamoSeguro();
            ps.setDocumento(doc);
            ps.setPrestamo(item.prestamo);
            ps.setNovedad(novedadDe(doc));
            ps.setBase(redondear(item.base));
            ps.setMesesCubiertos(item.mesesCubiertos);
            ps.setPeso(item.peso);
            ps.setValorAsignado(redondear(item.valorAsignado));
            ps.setCuotasRepartidas((long) item.cuotasConPeso.size());
            ps = prestamoSeguroDaoService.save(ps, null);

            for (ItemCuotaCalculo c : item.cuotasConPeso) {
                double anterior = seguroActualDe(c.cuota, campo);
                escribirSeguro(c.cuota, campo, c.seguroNuevo);
                detallePrestamoService.saveSingle(c.cuota);

                CuotaSeguro psct = new CuotaSeguro();
                psct.setDocumento(doc);
                psct.setPrestamoSeguro(ps);
                psct.setIdCuota(c.cuota.getCodigo());
                psct.setCampo(campo);
                psct.setSaldoInicialCapital(c.saldoInicialCapital);
                psct.setValorAnterior(anterior);
                psct.setValorNuevo(c.seguroNuevo);
                cuotaSeguroDaoService.save(psct, null);
            }
        }

        doc.setEstado(EstadoDocumentoSeguro.DISTRIBUIDO);
        doc.setUsuarioDistribucion(usuario);
        doc.setFechaDistribucion(LocalDateTime.now());
        doc = documentoSeguroDaoService.save(doc, doc.getCodigo());
        System.out.println("  Distribución OK - documento: " + doc.getCodigo()
            + " - préstamos: " + calculo.items.size() + " - total: " + calculo.sumaCuotas);
        return toDTO(doc);
    }

    private Long novedadDe(DocumentoSeguro doc) {
        if (doc.getClase() == null || doc.getClase() == ClaseDocumentoSeguro.FACTURA) {
            return NovedadPrestamoSeguro.ORIGINAL;
        }
        return doc.getClase() == ClaseDocumentoSeguro.NOTA_DEBITO
            ? NovedadPrestamoSeguro.INCLUSION : NovedadPrestamoSeguro.EXCLUSION;
    }

    /**
     * El cálculo aditivo (factura y ND comparten exactamente la misma fórmula — S1/S2/S3/S9/
     * S11/S12 del diseño, §5.3 del contrato): peso del préstamo por meses cubiertos, valor del
     * préstamo por peso relativo, valor de cada cuota por su DTPRSICP relativo. Usado tanto por
     * la vista previa como por la distribución real (que SIEMPRE recalcula, nunca confía en la
     * vista previa — contrato §5).
     */
    private ResultadoCalculoDistribucion calcularDistribucionAditiva(DocumentoSeguro doc) throws Throwable {
        List<PrestamoSeguro> pspr = prestamoSeguroDaoService.selectByDocumento(doc.getCodigo());
        if (pspr.isEmpty()) {
            throw new IncomeException(ERR_SIN_CUOTAS_EN_VIGENCIA + ": el documento " + doc.getCodigo()
                + " no tiene ningún préstamo");
        }

        boolean esNotaDebito = doc.getClase() != null && doc.getClase() == ClaseDocumentoSeguro.NOTA_DEBITO;
        // La ND cubre la vigencia RESTANTE: desde su propia emisión hasta el fin de la vigencia
        // de la factura (contrato §7); la factura cubre su vigencia completa.
        LocalDate inicio = esNotaDebito ? doc.getFechaEmision() : doc.getFechaInicio();
        LocalDate fin = doc.getFechaFin();
        long mesesVigencia = Math.max(1, mesesEntre(inicio, fin));
        long campo = campoDe(doc.getTipoSeguro());

        List<Long> idsPrestamo = new ArrayList<>();
        for (PrestamoSeguro p : pspr) {
            idsPrestamo.add(p.getPrestamo().getCodigo());
        }
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(idsPrestamo);
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(cuotasPorPrestamo);

        List<ItemPrestamoCalculo> items = new ArrayList<>();
        List<PrestamoSinCuotasEnVigencia> sinCuotas = new ArrayList<>();
        double sumaPesos = 0.0;

        for (PrestamoSeguro p : pspr) {
            List<DetallePrestamo> todas = cuotasPorPrestamo.getOrDefault(p.getPrestamo().getCodigo(),
                new ArrayList<>());
            List<ItemCuotaCalculo> conPeso = new ArrayList<>();
            List<ItemCuotaCalculo> sinPeso = new ArrayList<>();
            for (DetallePrestamo cuota : todas) {
                if (esPagadaOCancelada(cuota) || !vencimientoEnRango(cuota, inicio, fin)) {
                    continue;
                }
                ItemCuotaCalculo c = new ItemCuotaCalculo();
                c.cuota = cuota;
                c.seguroAnterior = seguroActualDe(cuota, campo);
                double sicp = nvl(cuota.getSaldoInicialCapital());
                if (sicp > TOLERANCIA) {
                    c.saldoInicialCapital = sicp;
                    conPeso.add(c);
                } else {
                    // ÍTEM 0a, fallback aprobado por el árbitro: DTPRSICP nulo/≤0 — fuera del
                    // reparto interno de su préstamo, se informa como sinPeso, no se toca.
                    c.saldoInicialCapital = 0.0;
                    c.seguroNuevo = c.seguroAnterior;
                    sinPeso.add(c);
                }
            }

            ItemPrestamoCalculo item = new ItemPrestamoCalculo();
            item.prestamo = p.getPrestamo();
            item.base = nvl(p.getBase());
            item.cuotasConPeso = conPeso;
            item.cuotasSinPeso = sinPeso;
            item.mesesCubiertos = conPeso.size();

            if (item.mesesCubiertos == 0) {
                item.peso = 0.0;
                sinCuotas.add(new PrestamoSinCuotasEnVigencia(p.getPrestamo().getCodigo(),
                    numeroPrestamoDe(p.getPrestamo())));
            } else {
                item.peso = item.base * item.mesesCubiertos / (double) mesesVigencia;
                sumaPesos += item.peso;
            }
            items.add(item);
        }

        if (sumaPesos <= 0.0) {
            throw new IncomeException(ERR_SIN_CUOTAS_EN_VIGENCIA + ": ningún préstamo del documento "
                + doc.getCodigo() + " tiene cuotas dentro de la vigencia [" + inicio + ", " + fin + "]");
        }

        List<ItemPrestamoCalculo> conPesoItems = new ArrayList<>();
        for (ItemPrestamoCalculo item : items) {
            if (item.mesesCubiertos > 0) {
                conPesoItems.add(item);
            }
        }

        double valorTotal = nvl(doc.getValorTotal());
        double[] rawPrestamos = new double[conPesoItems.size()];
        for (int i = 0; i < conPesoItems.size(); i++) {
            rawPrestamos[i] = valorTotal * conPesoItems.get(i).peso / sumaPesos;
        }
        double[] valoresPrestamos = redondearConSobranteAlMayor(rawPrestamos, valorTotal);
        double sumaPrestamos = 0.0;
        for (int i = 0; i < conPesoItems.size(); i++) {
            conPesoItems.get(i).valorAsignado = valoresPrestamos[i];
            sumaPrestamos = redondear(sumaPrestamos + valoresPrestamos[i]);
        }

        double sumaCuotasTotal = 0.0;
        for (ItemPrestamoCalculo item : conPesoItems) {
            double sumaSICP = 0.0;
            for (ItemCuotaCalculo c : item.cuotasConPeso) {
                sumaSICP += c.saldoInicialCapital;
            }
            double[] raw = new double[item.cuotasConPeso.size()];
            for (int i = 0; i < item.cuotasConPeso.size(); i++) {
                raw[i] = sumaSICP > 0.0 ? item.valorAsignado * item.cuotasConPeso.get(i).saldoInicialCapital / sumaSICP
                    : 0.0;
            }
            raw = aplicarPisoParcial(item.cuotasConPeso, raw, pagosPorCuota, campo);
            double[] redondeadas = redondearConSobranteAlMayor(raw, item.valorAsignado);
            for (int i = 0; i < item.cuotasConPeso.size(); i++) {
                item.cuotasConPeso.get(i).seguroNuevo = redondeadas[i];
                sumaCuotasTotal = redondear(sumaCuotasTotal + redondeadas[i]);
            }
        }

        ResultadoCalculoDistribucion resultado = new ResultadoCalculoDistribucion();
        resultado.valorTotal = valorTotal;
        resultado.sumaPrestamos = sumaPrestamos;
        resultado.sumaCuotas = sumaCuotasTotal;
        resultado.cuadra = Math.abs(redondear(valorTotal - sumaPrestamos)) <= TOLERANCIA
            && Math.abs(redondear(valorTotal - sumaCuotasTotal)) <= TOLERANCIA;
        resultado.prestamosSinCuotasEnVigencia = sinCuotas;
        resultado.items = conPesoItems;
        return resultado;
    }

    private PreviewDistribucionSeguro toPreviewDTO(ResultadoCalculoDistribucion calculo) {
        PreviewDistribucionSeguro dto = new PreviewDistribucionSeguro();
        dto.setValorTotal(calculo.valorTotal);
        dto.setSumaPrestamos(calculo.sumaPrestamos);
        dto.setSumaCuotas(calculo.sumaCuotas);
        dto.setCuadra(calculo.cuadra);
        dto.setPrestamosSinCuotasEnVigencia(calculo.prestamosSinCuotasEnVigencia);
        List<PrestamoDistribucionPreview> prestamos = new ArrayList<>();
        for (ItemPrestamoCalculo item : calculo.items) {
            PrestamoDistribucionPreview p = new PrestamoDistribucionPreview();
            p.setIdPrestamo(item.prestamo.getCodigo());
            p.setNumeroPrestamo(numeroPrestamoDe(item.prestamo));
            p.setBase(item.base);
            p.setMesesCubiertos(item.mesesCubiertos);
            p.setPeso(item.peso);
            p.setValorAsignado(item.valorAsignado);
            List<CuotaDistribucionPreview> cuotas = new ArrayList<>();
            for (ItemCuotaCalculo c : item.cuotasConPeso) {
                cuotas.add(toCuotaPreview(c, false));
            }
            for (ItemCuotaCalculo c : item.cuotasSinPeso) {
                cuotas.add(toCuotaPreview(c, true));
            }
            p.setCuotas(cuotas);
            prestamos.add(p);
        }
        dto.setPrestamos(prestamos);
        return dto;
    }

    private CuotaDistribucionPreview toCuotaPreview(ItemCuotaCalculo c, boolean sinPeso) {
        CuotaDistribucionPreview dto = new CuotaDistribucionPreview();
        dto.setIdCuota(c.cuota.getCodigo());
        dto.setNumeroCuota(c.cuota.getNumeroCuota());
        dto.setFechaVencimiento(c.cuota.getFechaVencimiento() != null
            ? c.cuota.getFechaVencimiento().toLocalDate() : null);
        dto.setSaldoInicialCapital(c.saldoInicialCapital);
        dto.setSeguroAnterior(c.seguroAnterior);
        dto.setSeguroNuevo(c.seguroNuevo);
        dto.setSinPeso(sinPeso);
        return dto;
    }

    /**
     * El piso de una cuota PARCIAL (o cualquiera con algo ya pagado de este campo): no baja de
     * lo ya cobrado. El déficit que eso genera se resta, proporcional, de las cuotas del MISMO
     * préstamo que no estén en el piso — si TODAS están en el piso, el déficit queda sin
     * resolver y lo atrapa la invariante dura más arriba (nunca se fuerza un negativo).
     */
    private double[] aplicarPisoParcial(List<ItemCuotaCalculo> cuotas, double[] raw,
            Map<Long, double[]> pagosPorCuota, long campo) {
        double[] ajustado = Arrays.copyOf(raw, raw.length);
        boolean[] floreado = new boolean[raw.length];
        double deficit = 0.0;
        for (int i = 0; i < cuotas.size(); i++) {
            double piso = pisoPagado(cuotas.get(i).cuota, pagosPorCuota, campo);
            if (piso > ajustado[i] + TOLERANCIA) {
                deficit += (piso - ajustado[i]);
                ajustado[i] = piso;
                floreado[i] = true;
            }
        }
        if (deficit > 0.0) {
            double sumaNoFloreado = 0.0;
            for (int i = 0; i < ajustado.length; i++) {
                if (!floreado[i]) {
                    sumaNoFloreado += ajustado[i];
                }
            }
            if (sumaNoFloreado > 0.0) {
                for (int i = 0; i < ajustado.length; i++) {
                    if (!floreado[i]) {
                        ajustado[i] = ajustado[i] - deficit * ajustado[i] / sumaNoFloreado;
                    }
                }
            }
        }
        return ajustado;
    }

    private double pisoPagado(DetallePrestamo cuota, Map<Long, double[]> pagosPorCuota, long campo) {
        double[] pagos = pagosPorCuota.get(cuota.getCodigo());
        if (pagos == null) {
            return 0.0;
        }
        return campo == CampoSeguroCuota.DESGRAVAMEN ? pagos[0] : pagos[5];
    }

    // ========================================================================
    // Nota de crédito — reducción (contrato §7)
    // ========================================================================

    private PreviewDistribucionSeguro previewReduccionNC(DocumentoSeguro doc) throws Throwable {
        ResultadoReduccionNC calculo = calcularReduccionNC(doc);
        PreviewDistribucionSeguro dto = new PreviewDistribucionSeguro();
        dto.setValorTotal(nvl(doc.getValorTotal()));
        dto.setSumaPrestamos(calculo.totalAplicado);
        dto.setSumaCuotas(calculo.totalAplicado);
        // La NC NO tiene que cuadrar al centavo contra el total (puede aplicar MENOS si el
        // préstamo ya no tiene de dónde reducir) — la invariante es Σ aplicado ≤ total.
        dto.setCuadra(calculo.totalAplicado <= nvl(doc.getValorTotal()) + TOLERANCIA);
        for (ItemReduccionPrestamo ip : calculo.prestamos) {
            PrestamoDistribucionPreview p = new PrestamoDistribucionPreview();
            p.setIdPrestamo(ip.prestamoSeguro.getPrestamo().getCodigo());
            p.setNumeroPrestamo(numeroPrestamoDe(ip.prestamoSeguro.getPrestamo()));
            p.setBase(nvl(ip.prestamoSeguro.getBase()));
            p.setMesesCubiertos(ip.cuotas.size());
            p.setValorAsignado(ip.aplicado);
            List<CuotaDistribucionPreview> cuotas = new ArrayList<>();
            for (ItemReduccionCuota c : ip.cuotas) {
                CuotaDistribucionPreview cp = new CuotaDistribucionPreview();
                cp.setIdCuota(c.cuota.getCodigo());
                cp.setNumeroCuota(c.cuota.getNumeroCuota());
                cp.setFechaVencimiento(c.cuota.getFechaVencimiento() != null
                    ? c.cuota.getFechaVencimiento().toLocalDate() : null);
                cp.setSaldoInicialCapital(c.saldoInicialCapital);
                cp.setSeguroAnterior(c.anterior);
                cp.setSeguroNuevo(c.nuevo);
                cuotas.add(cp);
            }
            p.setCuotas(cuotas);
            dto.getPrestamos().add(p);
        }
        return dto;
    }

    /**
     * NC (exclusión): resta {@code doc.valorTotal} entre TODAS las cuotas que todavía existan de
     * los préstamos de la nota (en vigencia, con peso), en proporción a su propio
     * {@code DTPRSICP} — un solo nivel de reparto, directo a cuotas, sin el paso intermedio de
     * peso-por-préstamo que sí usa la factura/ND (el contrato no define ese paso para la NC).
     * Piso en 0 y en lo ya pagado; lo que no se puede aplicar se informa, nunca se fuerza
     * (invariante: Σ aplicado ≤ total, nunca "=").
     */
    private ResultadoReduccionNC calcularReduccionNC(DocumentoSeguro doc) throws Throwable {
        List<PrestamoSeguro> pspr = prestamoSeguroDaoService.selectByDocumento(doc.getCodigo());
        LocalDate inicio = doc.getFechaInicio();
        LocalDate fin = doc.getFechaFin();
        long campo = campoDe(doc.getTipoSeguro());

        List<Long> idsPrestamo = new ArrayList<>();
        for (PrestamoSeguro p : pspr) {
            idsPrestamo.add(p.getPrestamo().getCodigo());
        }
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(idsPrestamo);
        Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(cuotasPorPrestamo);

        List<ItemReduccionPrestamo> itemsPrestamo = new ArrayList<>();
        double sumaSICPTotal = 0.0;
        for (PrestamoSeguro p : pspr) {
            ItemReduccionPrestamo ip = new ItemReduccionPrestamo();
            ip.prestamoSeguro = p;
            List<DetallePrestamo> todas = cuotasPorPrestamo.getOrDefault(p.getPrestamo().getCodigo(),
                new ArrayList<>());
            for (DetallePrestamo cuota : todas) {
                if (esPagadaOCancelada(cuota) || !vencimientoEnRango(cuota, inicio, fin)) {
                    continue;
                }
                double sicp = nvl(cuota.getSaldoInicialCapital());
                if (sicp <= TOLERANCIA) {
                    continue;
                }
                ItemReduccionCuota ic = new ItemReduccionCuota();
                ic.prestamoSeguro = p;
                ic.cuota = cuota;
                ic.saldoInicialCapital = sicp;
                ic.anterior = seguroActualDe(cuota, campo);
                ic.piso = pisoPagado(cuota, pagosPorCuota, campo);
                ip.cuotas.add(ic);
                sumaSICPTotal += sicp;
            }
            itemsPrestamo.add(ip);
        }

        double totalAAplicar = nvl(doc.getValorTotal());
        double totalAplicado = 0.0;
        for (ItemReduccionPrestamo ip : itemsPrestamo) {
            double aplicadoPrestamo = 0.0;
            for (ItemReduccionCuota ic : ip.cuotas) {
                double rawReduccion = sumaSICPTotal > 0.0
                    ? totalAAplicar * ic.saldoInicialCapital / sumaSICPTotal : 0.0;
                double maxReducible = Math.max(0.0, redondear(ic.anterior - ic.piso));
                ic.aplicado = redondear(Math.min(rawReduccion, maxReducible));
                ic.nuevo = redondear(ic.anterior - ic.aplicado);
                aplicadoPrestamo += ic.aplicado;
            }
            ip.aplicado = redondear(aplicadoPrestamo);
            totalAplicado = redondear(totalAplicado + ip.aplicado);
        }

        ResultadoReduccionNC resultado = new ResultadoReduccionNC();
        resultado.prestamos = itemsPrestamo;
        resultado.cuotas = new ArrayList<>();
        for (ItemReduccionPrestamo ip : itemsPrestamo) {
            resultado.cuotas.addAll(ip.cuotas);
        }
        resultado.totalAAplicar = totalAAplicar;
        resultado.totalAplicado = totalAplicado;
        resultado.noAplicado = redondear(totalAAplicar - totalAplicado);
        return resultado;
    }

    // ========================================================================
    // ÍTEM 6 — Anular
    // ========================================================================

    @Override
    public ResultadoAnularSeguro anular(Long id, String usuario, String motivo) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.anular - id: " + id + " - usuario: " + usuario);
        if (vacio(motivo)) {
            throw new IncomeException(ERR_MOTIVO_OBLIGATORIO + ": motivo es obligatorio");
        }
        DocumentoSeguro doc = buscarDocumento(id);
        if (doc.getEstado() != null && doc.getEstado() == EstadoDocumentoSeguro.LIBERADO_A_PAGO) {
            throw new IncomeException(ERR_LIBERADO_A_PAGO + ": el documento " + id
                + " ya está liberado a pago; no se puede anular en esta fase");
        }
        if (doc.getEstado() != null && doc.getEstado() == EstadoDocumentoSeguro.ANULADO) {
            throw new IncomeException(ERR_ESTADO_INVALIDO + ": el documento " + id + " ya está anulado");
        }
        if (doc.getClase() != null && doc.getClase() == ClaseDocumentoSeguro.FACTURA) {
            List<DocumentoSeguro> notas = documentoSeguroDaoService.selectNotasByPadre(id);
            List<String> vivas = new ArrayList<>();
            for (DocumentoSeguro nota : notas) {
                if (nota.getEstado() == null || nota.getEstado() != EstadoDocumentoSeguro.ANULADO) {
                    vivas.add(String.valueOf(nota.getCodigo()));
                }
            }
            if (!vivas.isEmpty()) {
                throw new IncomeException(ERR_NOTAS_VIVAS + ": la factura " + id + " tiene notas vivas sin anular: "
                    + String.join(", ", vivas));
            }
        }

        List<CuotaNoRevertida> noRevertidas = new ArrayList<>();
        if (doc.getEstado() != null && doc.getEstado() == EstadoDocumentoSeguro.DISTRIBUIDO) {
            long campo = campoDe(doc.getTipoSeguro());
            List<CuotaSeguro> vigentes = cuotaSeguroDaoService.selectVigentesByDocumento(id);

            // H82: PSCT.idCuota es un Long plano, sin relación — la cuota puede haberse borrado
            // después (un abono a capital o un reverso de operación). Se cargan en lote y se
            // TOLERA que falten: las que no vuelven de selectByCodigos simplemente no están en
            // el mapa, y se cuentan como no revertidas en vez de reventar con un NPE.
            List<Long> idsCuota = new ArrayList<>();
            for (CuotaSeguro psct : vigentes) {
                idsCuota.add(psct.getIdCuota());
            }
            Map<Long, DetallePrestamo> cuotasPorId = new HashMap<>();
            for (DetallePrestamo cuota : detallePrestamoDaoService.selectByCodigos(idsCuota)) {
                cuotasPorId.put(cuota.getCodigo(), cuota);
            }

            for (CuotaSeguro psct : vigentes) {
                DetallePrestamo cuota = cuotasPorId.get(psct.getIdCuota());
                if (cuota == null) {
                    CuotaNoRevertida nr = new CuotaNoRevertida();
                    nr.setIdCuota(psct.getIdCuota());
                    noRevertidas.add(nr);
                    continue;
                }
                if (esPagadaOCancelada(cuota)) {
                    CuotaNoRevertida nr = new CuotaNoRevertida();
                    nr.setIdCuota(cuota.getCodigo());
                    nr.setIdPrestamo(cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null);
                    nr.setNumeroPrestamo(cuota.getPrestamo() != null ? numeroPrestamoDe(cuota.getPrestamo()) : null);
                    nr.setNumeroCuota(cuota.getNumeroCuota());
                    noRevertidas.add(nr);
                    continue;
                }
                escribirSeguro(cuota, campo, nvl(psct.getValorAnterior()));
                detallePrestamoService.saveSingle(cuota);
                psct.setFechaReverso(LocalDateTime.now());
                cuotaSeguroDaoService.save(psct, psct.getCodigo());
            }
        }

        doc.setEstado(EstadoDocumentoSeguro.ANULADO);
        doc.setUsuarioAnulacion(usuario);
        doc.setFechaAnulacion(LocalDateTime.now());
        doc.setMotivoAnulacion(motivo);
        doc = documentoSeguroDaoService.save(doc, doc.getCodigo());

        ResultadoAnularSeguro resultado = new ResultadoAnularSeguro();
        resultado.setDocumento(toDTO(doc));
        resultado.setCuotasNoRevertidas(noRevertidas);
        return resultado;
    }

    // ========================================================================
    // ÍTEM 7 — Novedades y notas
    // ========================================================================

    @Override
    public NovedadesSeguro novedades(Long idFactura, LocalDate desde, LocalDate hasta) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.novedades - idFactura: " + idFactura
            + " - desde: " + desde + " - hasta: " + hasta);
        if (desde == null || hasta == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": desde y hasta son obligatorias");
        }
        DocumentoSeguro factura = buscarDocumento(idFactura);
        if (factura.getClase() == null || factura.getClase() != ClaseDocumentoSeguro.FACTURA) {
            throw new IncomeException(ERR_FACTURA_INVALIDA + ": el documento " + idFactura + " no es una factura");
        }
        if (factura.getEstado() == null || (factura.getEstado() != EstadoDocumentoSeguro.DISTRIBUIDO
                && factura.getEstado() != EstadoDocumentoSeguro.LIBERADO_A_PAGO)) {
            throw new IncomeException(ERR_FACTURA_INVALIDA + ": la factura " + idFactura + " está en estado "
                + factura.getEstado() + "; tiene que estar DISTRIBUIDA (3) o LIBERADA (4)");
        }

        List<DocumentoSeguro> notas = documentoSeguroDaoService.selectNotasByPadre(idFactura);
        List<Long> idsFamilia = new ArrayList<>();
        idsFamilia.add(idFactura);
        List<Long> idsDocumentosND = new ArrayList<>();
        idsDocumentosND.add(idFactura);
        for (DocumentoSeguro nota : notas) {
            idsFamilia.add(nota.getCodigo());
            boolean esNDViva = nota.getClase() != null && nota.getClase() == ClaseDocumentoSeguro.NOTA_DEBITO
                && (nota.getEstado() == null || nota.getEstado() != EstadoDocumentoSeguro.ANULADO);
            if (esNDViva) {
                idsDocumentosND.add(nota.getCodigo());
            }
        }

        List<PrestamoSeguro> yaIncluidos = prestamoSeguroDaoService.selectByDocumentos(idsDocumentosND);
        java.util.Set<Long> idsYaIncluidos = new java.util.HashSet<>();
        for (PrestamoSeguro p : yaIncluidos) {
            idsYaIncluidos.add(p.getPrestamo().getCodigo());
        }

        List<ItemCandidato> elegiblesHoy = calcularCandidatos(factura.getTipoSeguro(), LocalDate.now());
        NovedadesSeguro resultado = new NovedadesSeguro();
        for (ItemCandidato item : elegiblesHoy) {
            if (!idsYaIncluidos.contains(item.prestamo.getCodigo())) {
                resultado.getInclusiones().add(toCandidatoDTO(item));
            }
        }

        List<PrestamoSeguro> familiaCompleta = prestamoSeguroDaoService.selectByDocumentos(idsFamilia);
        java.util.Set<Long> idsFamiliaPrestamos = new java.util.HashSet<>();
        Map<Long, PrestamoSeguro> pspPorPrestamo = new HashMap<>();
        for (PrestamoSeguro p : familiaCompleta) {
            idsFamiliaPrestamos.add(p.getPrestamo().getCodigo());
            pspPorPrestamo.put(p.getPrestamo().getCodigo(), p);
        }
        List<Long> idsFamiliaPrestamosList = new ArrayList<>(idsFamiliaPrestamos);

        LocalDateTime desdeHora = desde.atStartOfDay();
        LocalDateTime hastaHora = hasta.atTime(23, 59, 59);
        List<EventoPrestamo> eventos = eventoPrestamoDaoService.selectByPrestamosYTiposYRango(idsFamiliaPrestamosList,
            Arrays.asList(ProcesoPagoPrestamoService.TIPO_ABONO_CAPITAL, ProcesoPagoPrestamoService.TIPO_PRECANCELACION),
            desdeHora, hastaHora);
        for (EventoPrestamo evento : eventos) {
            ExclusionSeguro ex = new ExclusionSeguro();
            Prestamo prestamo = evento.getPrestamo();
            ex.setIdPrestamo(prestamo.getCodigo());
            ex.setNumeroPrestamo(numeroPrestamoDe(prestamo));
            if (prestamo.getEntidad() != null) {
                ex.setNombreParticipe(prestamo.getEntidad().getRazonSocial());
                ex.setCedula(prestamo.getEntidad().getNumeroIdentificacion());
            }
            PrestamoSeguro ps = pspPorPrestamo.get(prestamo.getCodigo());
            ex.setBase(ps != null ? nvl(ps.getBase()) : 0.0);
            ex.setMotivo(ProcesoPagoPrestamoService.TIPO_PRECANCELACION.equals(evento.getTipoOperacion())
                ? "PRECANCELACION" : "ABONO_CAPITAL");
            ex.setFecha(evento.getFecha() != null ? evento.getFecha().toLocalDate() : null);
            resultado.getExclusiones().add(ex);
        }

        List<DeclaracionPlazoVencido> declaraciones = declaracionPlazoVencidoDaoService
            .selectVivasByPrestamosYRango(idsFamiliaPrestamosList, desdeHora, hastaHora);
        for (DeclaracionPlazoVencido declaracion : declaraciones) {
            ExclusionSeguro ex = new ExclusionSeguro();
            Prestamo prestamo = declaracion.getPrestamo();
            ex.setIdPrestamo(prestamo.getCodigo());
            ex.setNumeroPrestamo(numeroPrestamoDe(prestamo));
            if (prestamo.getEntidad() != null) {
                ex.setNombreParticipe(prestamo.getEntidad().getRazonSocial());
                ex.setCedula(prestamo.getEntidad().getNumeroIdentificacion());
            }
            PrestamoSeguro ps = pspPorPrestamo.get(prestamo.getCodigo());
            ex.setBase(ps != null ? nvl(ps.getBase()) : 0.0);
            ex.setMotivo("PLAZO_VENCIDO");
            ex.setFecha(declaracion.getFechaDeclaracion() != null
                ? declaracion.getFechaDeclaracion().toLocalDate() : null);
            resultado.getExclusiones().add(ex);
        }

        return resultado;
    }

    @Override
    public DocumentoSeguroDTO registrarNota(Long idFactura, SolicitudNotaSeguro solicitud) throws Throwable {
        System.out.println("DocumentoSeguroServiceImpl.registrarNota - idFactura: " + idFactura);
        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": no se recibió el cuerpo de la solicitud");
        }
        DocumentoSeguro factura = buscarDocumento(idFactura);
        if (factura.getClase() == null || factura.getClase() != ClaseDocumentoSeguro.FACTURA) {
            throw new IncomeException(ERR_FACTURA_INVALIDA + ": el documento " + idFactura + " no es una factura");
        }
        if (factura.getEstado() == null || (factura.getEstado() != EstadoDocumentoSeguro.DISTRIBUIDO
                && factura.getEstado() != EstadoDocumentoSeguro.LIBERADO_A_PAGO)) {
            throw new IncomeException(ERR_FACTURA_INVALIDA + ": la factura " + idFactura + " está en estado "
                + factura.getEstado() + "; tiene que estar DISTRIBUIDA (3) o LIBERADA (4)");
        }
        if (solicitud.getClase() == null || (solicitud.getClase() != ClaseDocumentoSeguro.NOTA_DEBITO
                && solicitud.getClase() != ClaseDocumentoSeguro.NOTA_CREDITO)) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": clase debe ser 2 (NOTA_DEBITO) o 3 (NOTA_CREDITO)");
        }
        if (solicitud.getPrestamos() == null || solicitud.getPrestamos().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": prestamos es obligatorio y no puede estar vacío");
        }
        if (vacio(solicitud.getUsuario())) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": usuario es obligatorio");
        }
        if (solicitud.getValorTotal() == null || solicitud.getValorTotal() <= 0.0) {
            throw new IncomeException(ERR_VALOR_INVALIDO + ": valorTotal debe ser mayor a 0");
        }
        if (vacio(solicitud.getClaveAcceso())) {
            throw new IncomeException(ERR_CLAVE_ACCESO_OBLIGATORIA + ": claveAcceso es obligatoria");
        }
        DocumentoSeguro existente = documentoSeguroDaoService.selectByClaveAccesoNormalizada(solicitud.getClaveAcceso());
        if (existente != null) {
            throw new IncomeException(ERR_CLAVE_ACCESO_DUPLICADA + ": la clave de acceso " + solicitud.getClaveAcceso()
                + " ya está registrada en el documento " + existente.getCodigo());
        }

        DocumentoSeguro nota = new DocumentoSeguro();
        nota.setTipoSeguro(factura.getTipoSeguro());
        nota.setClase(solicitud.getClase());
        nota.setPadre(factura);
        nota.setEstado(EstadoDocumentoSeguro.DOCUMENTO_REGISTRADO);
        nota.setFechaCorte(factura.getFechaCorte());
        nota.setAseguradora(solicitud.getAseguradora());
        nota.setRuc(solicitud.getRuc());
        nota.setNumeroPoliza(solicitud.getNumeroPoliza());
        nota.setNumeroDocumento(solicitud.getNumeroDocumento());
        nota.setClaveAcceso(solicitud.getClaveAcceso());
        nota.setFechaEmision(solicitud.getFechaEmision());
        nota.setFechaInicio(factura.getFechaInicio());
        nota.setFechaFin(factura.getFechaFin());
        nota.setValorTotal(redondear(solicitud.getValorTotal()));
        nota.setUsuarioDocumento(solicitud.getUsuario());
        nota.setFechaDocumento(LocalDateTime.now());
        nota = documentoSeguroDaoService.save(nota, null);

        long novedad = solicitud.getClase() == ClaseDocumentoSeguro.NOTA_DEBITO
            ? NovedadPrestamoSeguro.INCLUSION : NovedadPrestamoSeguro.EXCLUSION;
        for (Long idPrestamo : solicitud.getPrestamos()) {
            Prestamo prestamo = prestamoDaoService.find(new Prestamo(), idPrestamo);
            if (prestamo == null) {
                throw new IncomeException(ERR_PRESTAMO_NO_ENCONTRADO + ": no existe el préstamo " + idPrestamo);
            }
            double base = baseActualDe(prestamo, factura.getTipoSeguro());
            PrestamoSeguro ps = new PrestamoSeguro();
            ps.setDocumento(nota);
            ps.setPrestamo(prestamo);
            ps.setNovedad(novedad);
            ps.setBase(redondear(base));
            prestamoSeguroDaoService.save(ps, null);
        }

        return toDTO(nota);
    }

    private double baseActualDe(Prestamo prestamo, Long tipoSeguro) throws Throwable {
        if (tipoSeguro != null && tipoSeguro == TipoSeguro.DESGRAVAMEN) {
            List<Long> ids = java.util.Collections.singletonList(prestamo.getCodigo());
            Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = cargarCuotasPorPrestamo(ids);
            Map<Long, double[]> pagosPorCuota = cargarPagosPorCuota(cuotasPorPrestamo);
            double saldo = 0.0;
            for (DetallePrestamo cuota : cuotasPorPrestamo.getOrDefault(prestamo.getCodigo(), new ArrayList<>())) {
                if (esPagadaOCancelada(cuota)) {
                    continue;
                }
                double[] pagos = pagosPorCuota.get(cuota.getCodigo());
                double capitalPagado = pagos != null ? pagos[4] : 0.0;
                saldo += Math.max(0.0, redondear(nvl(cuota.getCapital()) - capitalPagado));
            }
            return redondear(saldo);
        }
        return nvl(prestamo.getValorAsegurado());
    }

    // ========================================================================
    // ÍTEM 8 — consultas
    // ========================================================================

    @Override
    public List<DocumentoSeguroDTO> listar(Long tipoSeguro, Long estado, Long clase) throws Throwable {
        List<DocumentoSeguro> documentos = documentoSeguroDaoService.selectByFiltros(tipoSeguro, estado, clase);
        List<DocumentoSeguroDTO> resultado = new ArrayList<>();
        for (DocumentoSeguro doc : documentos) {
            resultado.add(toDTO(doc));
        }
        return resultado;
    }

    @Override
    public DocumentoSeguroDTO getById(Long id) throws Throwable {
        return toDTO(buscarDocumento(id));
    }

    @Override
    public List<PrestamoSeguroDTO> getPrestamos(Long id) throws Throwable {
        buscarDocumento(id);
        List<PrestamoSeguro> filas = prestamoSeguroDaoService.selectByDocumento(id);
        List<PrestamoSeguroDTO> resultado = new ArrayList<>();
        for (PrestamoSeguro p : filas) {
            PrestamoSeguroDTO dto = new PrestamoSeguroDTO();
            dto.setIdPrestamo(p.getPrestamo().getCodigo());
            dto.setNumeroPrestamo(numeroPrestamoDe(p.getPrestamo()));
            if (p.getPrestamo().getEntidad() != null) {
                dto.setNombreParticipe(p.getPrestamo().getEntidad().getRazonSocial());
                dto.setCedula(p.getPrestamo().getEntidad().getNumeroIdentificacion());
            }
            dto.setNovedad(p.getNovedad());
            dto.setBase(nvl(p.getBase()));
            dto.setMesesCubiertos(p.getMesesCubiertos());
            dto.setPeso(p.getPeso());
            dto.setValorAsignado(p.getValorAsignado());
            dto.setCuotasRepartidas(p.getCuotasRepartidas());
            resultado.add(dto);
        }
        return resultado;
    }

    private DocumentoSeguroDTO toDTO(DocumentoSeguro doc) throws Throwable {
        DocumentoSeguroDTO dto = new DocumentoSeguroDTO();
        dto.setId(doc.getCodigo());
        dto.setTipoSeguro(doc.getTipoSeguro());
        dto.setClase(doc.getClase());
        dto.setIdPadre(doc.getPadre() != null ? doc.getPadre().getCodigo() : null);
        dto.setEstado(doc.getEstado());
        dto.setFechaCorte(doc.getFechaCorte());
        dto.setAseguradora(doc.getAseguradora());
        dto.setRuc(doc.getRuc());
        dto.setNumeroPoliza(doc.getNumeroPoliza());
        dto.setNumeroDocumento(doc.getNumeroDocumento());
        dto.setClaveAcceso(doc.getClaveAcceso());
        dto.setFechaEmision(doc.getFechaEmision());
        dto.setFechaInicio(doc.getFechaInicio());
        dto.setFechaFin(doc.getFechaFin());
        dto.setTasa(doc.getTasa());
        dto.setValorTotal(doc.getValorTotal());
        dto.setIdDocumentoCxp(doc.getIdDocumentoCxp());

        List<PrestamoSeguro> filas = prestamoSeguroDaoService.selectByDocumento(doc.getCodigo());
        dto.setCantidadPrestamos(filas.size());
        double sumaBase = 0.0;
        double sumaDistribuida = 0.0;
        for (PrestamoSeguro p : filas) {
            sumaBase += nvl(p.getBase());
            sumaDistribuida += nvl(p.getValorAsignado());
        }
        dto.setSumaBase(redondear(sumaBase));
        dto.setSumaDistribuida(redondear(sumaDistribuida));

        if (doc.getClase() == null || doc.getClase() == ClaseDocumentoSeguro.FACTURA) {
            for (DocumentoSeguro nota : documentoSeguroDaoService.selectNotasByPadre(doc.getCodigo())) {
                dto.getNotas().add(toDTO(nota));
            }
        }

        DocumentoSeguroDTO.AuditoriaDocumentoSeguro auditoria = new DocumentoSeguroDTO.AuditoriaDocumentoSeguro();
        auditoria.setUsuarioListado(doc.getUsuarioListado());
        auditoria.setFechaListado(doc.getFechaListado());
        auditoria.setUsuarioDocumento(doc.getUsuarioDocumento());
        auditoria.setFechaDocumento(doc.getFechaDocumento());
        auditoria.setUsuarioDistribucion(doc.getUsuarioDistribucion());
        auditoria.setFechaDistribucion(doc.getFechaDistribucion());
        auditoria.setUsuarioLiberacion(doc.getUsuarioLiberacion());
        auditoria.setFechaLiberacion(doc.getFechaLiberacion());
        auditoria.setUsuarioAnulacion(doc.getUsuarioAnulacion());
        auditoria.setFechaAnulacion(doc.getFechaAnulacion());
        auditoria.setMotivoAnulacion(doc.getMotivoAnulacion());
        dto.setAuditoria(auditoria);

        return dto;
    }

    // ========================================================================
    // Helpers — escritura en la cuota
    // ========================================================================

    private void escribirSeguro(DetallePrestamo cuota, long campo, double nuevo) {
        double anterior = seguroActualDe(cuota, campo);
        double diferencia = redondear(nuevo - anterior);
        if (campo == CampoSeguroCuota.DESGRAVAMEN) {
            cuota.setDesgravamen(nuevo);
            cuota.setDesgravamenOriginal(nuevo);
            cuota.setDesgravamenFirmado(nuevo);
        } else {
            cuota.setValorSeguroIncendio(nuevo);
        }
        cuota.setTotal(redondear(nvl(cuota.getTotal()) + diferencia));
        cuota.setTotalConSeguro(redondear(nvl(cuota.getTotalConSeguro()) + diferencia));
    }

    private double seguroActualDe(DetallePrestamo cuota, long campo) {
        return campo == CampoSeguroCuota.DESGRAVAMEN ? nvl(cuota.getDesgravamen()) : nvl(cuota.getValorSeguroIncendio());
    }

    // ========================================================================
    // Helpers — carga en lote (mismo patrón que plazo vencido, ítem 0.c de ese frente)
    // ========================================================================

    private Map<Long, List<DetallePrestamo>> cargarCuotasPorPrestamo(List<Long> idsPrestamo) throws Throwable {
        Map<Long, List<DetallePrestamo>> cuotasPorPrestamo = new HashMap<>();
        if (idsPrestamo == null || idsPrestamo.isEmpty()) {
            return cuotasPorPrestamo;
        }
        List<DetallePrestamo> todas = detallePrestamoDaoService.selectByPrestamos(idsPrestamo);
        for (DetallePrestamo cuota : todas) {
            Long idPrestamo = cuota.getPrestamo() != null ? cuota.getPrestamo().getCodigo() : null;
            cuotasPorPrestamo.computeIfAbsent(idPrestamo, k -> new ArrayList<>()).add(cuota);
        }
        return cuotasPorPrestamo;
    }

    private Map<Long, double[]> cargarPagosPorCuota(Map<Long, List<DetallePrestamo>> cuotasPorPrestamo)
            throws Throwable {
        List<Long> idsCuotas = new ArrayList<>();
        for (List<DetallePrestamo> cuotas : cuotasPorPrestamo.values()) {
            for (DetallePrestamo cuota : cuotas) {
                idsCuotas.add(cuota.getCodigo());
            }
        }
        return agruparPagosPorCuota(pagoPrestamoDaoService.selectDatosPagosVigentes(idsCuotas));
    }

    /**
     * Índices del arreglo resultado: 0 desgravamen pagado, 1 moraPagada, 2 interesVencidoPagado,
     * 3 interesPagado, 4 capitalPagado, 5 valorSeguroIncendio pagado, 6 saldoOtros ya filtrado
     * por tipo de pago — mismo orden que {@code PagoPrestamoDaoService#selectDatosPagosVigentes}
     * desplazado -1 (acá no se guarda el id de la cuota, es la llave del mapa).
     */
    private Map<Long, double[]> agruparPagosPorCuota(List<Object[]> filas) {
        Map<Long, double[]> mapa = new HashMap<>();
        if (filas == null) {
            return mapa;
        }
        for (Object[] fila : filas) {
            Long idCuota = (Long) fila[0];
            double[] acumulado = mapa.computeIfAbsent(idCuota, k -> new double[7]);
            acumulado[0] += nvl((Double) fila[1]);
            acumulado[1] += nvl((Double) fila[2]);
            acumulado[2] += nvl((Double) fila[3]);
            acumulado[3] += nvl((Double) fila[4]);
            acumulado[4] += nvl((Double) fila[5]);
            acumulado[5] += nvl((Double) fila[6]);
            acumulado[6] += nvl((Double) fila[7]);
        }
        return mapa;
    }

    // ========================================================================
    // Helpers varios
    // ========================================================================

    private DocumentoSeguro buscarDocumento(Long id) throws Throwable {
        if (id == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": id es obligatorio");
        }
        DocumentoSeguro doc = documentoSeguroDaoService.find(new DocumentoSeguro(), id);
        if (doc == null) {
            throw new IncomeException(ERR_DOCUMENTO_NO_ENCONTRADO + ": no existe el documento " + id);
        }
        return doc;
    }

    private List<Long> idsFamilia(DocumentoSeguro doc) throws Throwable {
        Long idFactura = doc.getClase() == null || doc.getClase() == ClaseDocumentoSeguro.FACTURA
            ? doc.getCodigo() : doc.getPadre().getCodigo();
        List<Long> ids = new ArrayList<>();
        ids.add(idFactura);
        for (DocumentoSeguro nota : documentoSeguroDaoService.selectNotasByPadre(idFactura)) {
            ids.add(nota.getCodigo());
        }
        return ids;
    }

    private long campoDe(Long tipoSeguro) {
        return tipoSeguro != null && tipoSeguro == TipoSeguro.DESGRAVAMEN
            ? CampoSeguroCuota.DESGRAVAMEN : CampoSeguroCuota.SEGURO;
    }

    private boolean esPagadaOCancelada(DetallePrestamo cuota) {
        Long estado = cuota.getEstado();
        return estado != null && (estado == EstadoCuotaPrestamo.PAGADA || estado == EstadoCuotaPrestamo.CANCELADA_ANTICIPADA);
    }

    private boolean vencimientoEnRango(DetallePrestamo cuota, LocalDate inicio, LocalDate fin) {
        if (cuota.getFechaVencimiento() == null || inicio == null || fin == null) {
            return false;
        }
        LocalDate vencimiento = cuota.getFechaVencimiento().toLocalDate();
        return !vencimiento.isBefore(inicio) && !vencimiento.isAfter(fin);
    }

    private long mesesEntre(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            return 1;
        }
        long meses = (fin.getYear() * 12L + fin.getMonthValue()) - (inicio.getYear() * 12L + inicio.getMonthValue()) + 1;
        return Math.max(1, meses);
    }

    private Long tipoPrestamoDe(Prestamo prestamo) {
        return prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getCodigo() : null;
    }

    private String nombreTipoPrestamoDe(Prestamo prestamo) {
        return prestamo.getProducto() != null && prestamo.getProducto().getTipoPrestamo() != null
            ? prestamo.getProducto().getTipoPrestamo().getNombre() : null;
    }

    private String numeroPrestamoDe(Prestamo prestamo) {
        return prestamo.getIdAsoprep() != null ? String.valueOf(prestamo.getIdAsoprep())
            : String.valueOf(prestamo.getCodigo());
    }

    @Override
    public double[] repartirPorPeso(List<Double> pesos, double total) {
        if (pesos == null || pesos.isEmpty()) {
            return new double[0];
        }
        double sumaPesos = 0.0;
        for (Double peso : pesos) {
            sumaPesos += nvl(peso);
        }
        double[] raw = new double[pesos.size()];
        if (sumaPesos > 0.0) {
            for (int i = 0; i < pesos.size(); i++) {
                raw[i] = total * nvl(pesos.get(i)) / sumaPesos;
            }
        }
        return redondearConSobranteAlMayor(raw, total);
    }

    /** Redondea cada valor a 2 decimales y pone el sobrante/faltante en el de mayor valor. */
    private double[] redondearConSobranteAlMayor(double[] valoresRaw, double objetivo) {
        double[] redondeados = new double[valoresRaw.length];
        double suma = 0.0;
        int indiceMayor = -1;
        double mayor = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < valoresRaw.length; i++) {
            redondeados[i] = redondear(valoresRaw[i]);
            suma = redondear(suma + redondeados[i]);
            if (redondeados[i] > mayor) {
                mayor = redondeados[i];
                indiceMayor = i;
            }
        }
        double diferencia = redondear(objetivo - suma);
        if (indiceMayor >= 0 && Math.abs(diferencia) > 0.0) {
            redondeados[indiceMayor] = redondear(redondeados[indiceMayor] + diferencia);
        }
        return redondeados;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private double nvl(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // ========================================================================
    // Estructuras internas de cálculo
    // ========================================================================

    private static class ItemCandidato {
        Prestamo prestamo;
        double base;
        boolean sinSumaAsegurada;
    }

    private static class ItemCuotaCalculo {
        DetallePrestamo cuota;
        double saldoInicialCapital;
        double seguroAnterior;
        double seguroNuevo;
    }

    private static class ItemPrestamoCalculo {
        Prestamo prestamo;
        double base;
        long mesesCubiertos;
        double peso;
        double valorAsignado;
        List<ItemCuotaCalculo> cuotasConPeso;
        List<ItemCuotaCalculo> cuotasSinPeso;
    }

    private static class ResultadoCalculoDistribucion {
        double valorTotal;
        double sumaPrestamos;
        double sumaCuotas;
        boolean cuadra;
        List<PrestamoSinCuotasEnVigencia> prestamosSinCuotasEnVigencia;
        List<ItemPrestamoCalculo> items;
    }

    private static class ItemReduccionCuota {
        PrestamoSeguro prestamoSeguro;
        DetallePrestamo cuota;
        double saldoInicialCapital;
        double anterior;
        double piso;
        double aplicado;
        double nuevo;
    }

    private static class ItemReduccionPrestamo {
        PrestamoSeguro prestamoSeguro;
        List<ItemReduccionCuota> cuotas = new ArrayList<>();
        double aplicado;
    }

    private static class ResultadoReduccionNC {
        List<ItemReduccionPrestamo> prestamos;
        List<ItemReduccionCuota> cuotas;
        double totalAAplicar;
        double totalAplicado;
        double noAplicado;
    }
}
