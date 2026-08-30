package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CertificadoDaoService;
import com.saa.ejb.crd.dao.ContratoDaoService;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.EstadoParticipeDaoService;
import com.saa.ejb.crd.dao.HistoricoPagoCuentaIndividualDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.CertificadoService;
import com.saa.ejb.crd.service.dto.CampoCertificado;
import com.saa.ejb.crd.service.dto.LiquidacionCertificado;
import com.saa.ejb.crd.service.dto.MotivoBloqueoCertificado;
import com.saa.ejb.crd.service.dto.PrecargaCertificado;
import com.saa.ejb.crd.service.dto.PrestamoCertificado;
import com.saa.ejb.crd.service.dto.ResultadoEmisionCertificado;
import com.saa.ejb.crd.service.dto.SolicitudEmisionCertificado;
import com.saa.ejb.reporte.service.ReporteService;
import com.saa.model.crd.Certificado;
import com.saa.model.crd.Contrato;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EstadoParticipe;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.CrdParametroCertificado;
import com.saa.rubros.CrdTipoCertificado;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoCertificado;
import com.saa.rubros.EstadoParticipeEntidad;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.Rubros;
import com.saa.rubros.TipoCuentasBancarias;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.NoResultException;

/**
 * Emisión, consulta, reimpresión y anulación de certificados de partícipe.
 * Contrato: docs/logica-negocio/crd/API-CERTIFICADOS-PARTICIPE.md (⛔ congelado 2026-08-29).
 *
 * <h3>Cómo se arma un certificado</h3>
 * {@link #resolver} construye el {@link Contexto}: el partícipe, la calidad propuesta, los
 * campos que el sistema puede resolver (con su fuente) y los bloqueos. La precarga devuelve
 * ese contexto tal cual. La emisión lo vuelve a construir desde cero — no confía en lo que
 * mandó el frontend —, mezcla los valores del operador según la tabla de §3.2 del contrato,
 * y recién entonces toma el número y llena el reporte.
 *
 * <h3>Tres decisiones que NO se pueden "simplificar" (cada una evita un fallo silencioso)</h3>
 * <ol>
 *   <li><b>La emisión es UNA transacción</b> ({@link #emitir}, REQUIRED): lock → MAX+1 →
 *       llenar el reporte → INSERT con el BLOB → commit. El número se toma ANTES del PDF
 *       porque va impreso adentro; y si el PDF falla, la excepción (checked, envuelta en
 *       EJBException) revierte todo y el número nunca existió. Separar la numeración del
 *       llenado, o usar una SEQUENCE de Oracle, deja números quemados en una serie que se
 *       firma: huecos que nadie puede explicar.</li>
 *   <li><b>El .jrxml no consulta CRD.CRTF</b>: {@code ReporteServiceImpl} llena el reporte
 *       con una conexión JDBC cruda (JNDI) y en REQUIRES_NEW, así que NO ve la transacción
 *       de arriba — leería la tabla sin la fila que se está insertando. Por eso el número y
 *       todo lo variable viajan como parámetros {@code P_*} ({@link #armarParametros}) y la
 *       query del reporte es solo la entidad. "Mejorar" el reporte para que lea CRTF lo
 *       rompe la primera vez que se ejecute.</li>
 *   <li><b>Firmante, cargo y ciudad (rubro 243) fallan en vez de tener fallback</b>
 *       ({@link #parametro}): se imprimen en un documento firmado. Un valor cableado como
 *       respaldo haría que, cuando cambie el jefe de crédito y la lectura falle por lo que
 *       sea, se siga imprimiendo al anterior sin avisar. El lado seguro es no emitir.</li>
 * </ol>
 *
 * <h3>Trampas del módulo que este servicio respeta</h3>
 * <ul>
 *   <li>Estado del préstamo = {@code PRSTIDST} ({@code idEstado}), nunca {@code ESPSCDGO}.
 *       Cancelado = 3, 4 o 5. El 9 CANCELADO_POR_REVISAR NO cuenta como cancelado.</li>
 *   <li>Número del crédito = {@code PRSTIDAS} ({@code idAsoprep}), o {@code PRSTCDGO} si
 *       es nulo. {@code APRTIDAS} es otra cosa (la carga Petro) y no se usa.</li>
 *   <li>Cuota vencida = por FECHA de vencimiento contra hoy 00:00
 *       ({@code selectCuotasVencidasByPrestamo}), el criterio del proceso diario. NO por
 *       {@code DTPRESTD}: medido en la Fase 1, ningún vigente tiene cuotas en 5/8 y ese
 *       filtro daría siempre cero.</li>
 *   <li>Calidad del partícipe = {@code ENTDIDST} contra el código alterno de CRD.ESPR
 *       ({@code ESPRCDEX}), no contra su PK.</li>
 *   <li>Los aportes se consultan agregados en la base (EXISTS / MIN), nunca se bajan filas:
 *       CRD.APRT tiene ~980.000 registros.</li>
 * </ul>
 *
 * @since 2026-08-29
 */
@Stateless
public class CertificadoServiceImpl implements CertificadoService {

    /** Tipos de aporte (CRD.TPAP) que usan las reglas. Verificados contra el catálogo el 2026-08-29. */
    private static final List<Long> TIPOS_APORTE_PERSONAL = Arrays.asList(9L, 11L);          // jubilación y cesantía personal
    private static final List<Long> TIPOS_JUBILACION_PATRONAL = Arrays.asList(13L, 15L);    // + su rendimiento
    private static final List<Long> TIPOS_CESANTIA_PATRONAL = Arrays.asList(14L, 16L);      // + su rendimiento
    private static final List<Long> TIPOS_PENSION_COMPLEMENTARIA = Arrays.asList(23L);

    /** Estados de préstamo que cuentan como cancelado. El 9 (por revisar) queda fuera a propósito. */
    private static final List<Integer> ESTADOS_CANCELADO = Arrays.asList(
            EstadoPrestamo.CANCELADO, EstadoPrestamo.CANCELADO_ANTICIPADO, EstadoPrestamo.CANCELADO_POR_NOVACION);

    /** Estados de préstamo que bloquean por mora. */
    private static final List<Integer> ESTADOS_MORA = Arrays.asList(
            EstadoPrestamo.DE_PLAZO_VENCIDO, EstadoPrestamo.EN_MORA);

    private static final String[] MESES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    @EJB
    private CertificadoDaoService certificadoDaoService;

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private EstadoParticipeDaoService estadoParticipeDaoService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @EJB
    private HistoricoPagoCuentaIndividualDaoService historicoPagoCuentaIndividualDaoService;

    @EJB
    private DetalleRubroDaoService detalleRubroDaoService;

    /** Llena y exporta el .jasper. Corre en REQUIRES_NEW y con conexión JDBC propia: no ve esta transacción. */
    @EJB
    private ReporteService reporteService;

    // ========================================================================
    // CRUD heredado
    // ========================================================================

    @Override
    public Certificado selectById(Long id) throws Throwable {
        System.out.println("selectById - Certificado: " + id);
        return certificadoDaoService.selectById(id, NombreEntidadesCredito.CERTIFICADO);
    }

    /** ⛔ Un certificado emitido no se borra: se anula y conserva su número. */
    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("remove[] - Certificado (bloqueado: se anula, no se borra)");
        throw new IncomeException("Un certificado emitido no se elimina; debe anularse");
    }

    /** ⛔ Un certificado no se crea ni se edita a mano: solo se emite. */
    @Override
    public void save(List<Certificado> lista) throws Throwable {
        System.out.println("save list - Certificado (bloqueado: solo se emite)");
        throw new IncomeException("Un certificado no se crea ni se edita a mano; debe emitirse");
    }

    /** ⛔ Un certificado no se crea ni se edita a mano: solo se emite. */
    @Override
    public Certificado saveSingle(Certificado registro) throws Throwable {
        System.out.println("saveSingle - Certificado (bloqueado: solo se emite)");
        throw new IncomeException("Un certificado no se crea ni se edita a mano; debe emitirse");
    }

    @Override
    public List<Certificado> selectAll() throws Throwable {
        System.out.println("selectAll - Certificado");
        List<Certificado> result = certificadoDaoService.selectAll(NombreEntidadesCredito.CERTIFICADO);
        if (result.isEmpty()) {
            throw new IncomeException("No existen registros Certificado");
        }
        return result;
    }

    @Override
    public List<Certificado> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("selectByCriteria - Certificado");
        List<Certificado> result = certificadoDaoService.selectByCriteria(datos, NombreEntidadesCredito.CERTIFICADO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda por criterio Certificado no devolvio registros");
        }
        return result;
    }

    // ========================================================================
    // Precarga
    // ========================================================================

    @Override
    public PrecargaCertificado precargar(Long idEntidad, Long tipo, Long idPrestamo, Long idLiquidacion)
            throws Throwable {
        System.out.println("precargar - Certificado - entidad: " + idEntidad + " tipo: " + tipo
                + " prestamo: " + idPrestamo + " liquidacion: " + idLiquidacion);
        Contexto ctx = resolver(idEntidad, tipo, idPrestamo, idLiquidacion);

        PrecargaCertificado precarga = new PrecargaCertificado();
        precarga.setIdEntidad(idEntidad);
        precarga.setTipo(tipo);
        precarga.setTipoTexto(ctx.tipoTexto);
        precarga.setNombre(ctx.entidad.getRazonSocial());
        precarga.setCedula(ctx.entidad.getNumeroIdentificacion());
        precarga.setCalidadSistema(ctx.calidadSistema);
        precarga.setCalidadSistemaTexto(ctx.calidadSistemaTexto);
        precarga.setBloqueos(ctx.bloqueos);
        precarga.setCampos(ctx.campos);
        precarga.setPrestamos(ctx.prestamos);
        precarga.setLiquidaciones(ctx.liquidaciones);

        boolean faltaRequerido = false;
        for (CampoCertificado campo : ctx.campos.values()) {
            if (CampoCertificado.ORIGEN_MANUAL_REQUERIDO.equals(campo.getOrigen()) && campo.getValor() == null) {
                faltaRequerido = true;
                break;
            }
        }
        precarga.setPuedeEmitir(ctx.bloqueos.isEmpty() && !faltaRequerido);
        return precarga;
    }

    // ========================================================================
    // Emisión
    // ========================================================================

    /**
     * Todo en UNA transacción: validar → lock + MAX+1 → llenar el reporte → INSERT con el
     * BLOB → commit. Si el reporte revienta, la excepción sale de aquí, la transacción se
     * revierte y el número nunca existió.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ResultadoEmisionCertificado emitir(SolicitudEmisionCertificado solicitud) throws Throwable {
        System.out.println("emitir - Certificado - entidad: " + (solicitud != null ? solicitud.getIdEntidad() : null)
                + " tipo: " + (solicitud != null ? solicitud.getTipo() : null));

        // 1. Validación de forma
        if (solicitud == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": la solicitud es obligatoria");
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el usuario es obligatorio");
        }
        if (solicitud.getTipo() != null && solicitud.getTipo().intValue() == CrdTipoCertificado.NO_ADEUDAR_CREDITO
                && solicitud.getIdPrestamo() == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el certificado por credito requiere idPrestamo");
        }

        // 2. Re-resolver desde cero: los bloqueos y los orígenes NO vienen del frontend
        Contexto ctx = resolver(solicitud.getIdEntidad(), solicitud.getTipo(),
                solicitud.getIdPrestamo(), solicitud.getIdLiquidacion());
        if (!ctx.bloqueos.isEmpty()) {
            StringBuilder motivos = new StringBuilder();
            for (MotivoBloqueoCertificado bloqueo : ctx.bloqueos) {
                if (motivos.length() > 0) {
                    motivos.append(" | ");
                }
                motivos.append(bloqueo.getMensaje());
            }
            throw new IncomeException(ERR_BLOQUEADO + ": " + motivos);
        }

        // 3. Calidad: la propone la base, el operador puede corregirla
        Long calidad = solicitud.getCalidad() != null ? solicitud.getCalidad() : ctx.calidadSistema;
        if (calidad == null || calidad < EstadoParticipeEntidad.ACTIVO || calidad > EstadoParticipeEntidad.NUEVO) {
            throw new IncomeException(ERR_CALIDAD_INVALIDA + ": la calidad " + calidad + " no existe en el catalogo");
        }
        String origenCalidad = calidad.equals(ctx.calidadSistema)
                ? CampoCertificado.ORIGEN_SISTEMA : CampoCertificado.ORIGEN_MANUAL_EDITADO;
        String calidadTexto = textoCalidad(calidad);

        // 4. Mezclar lo que mandó el operador con lo que resolvió el sistema (§3.2 del contrato)
        Map<String, Object> enviados = solicitud.getCampos() != null ? solicitud.getCampos() : new HashMap<>();
        Map<String, CampoCertificado> finales = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();
        for (Map.Entry<String, CampoCertificado> entrada : ctx.campos.entrySet()) {
            String clave = entrada.getKey();
            CampoCertificado sistema = entrada.getValue();
            Object enviado = normalizarValor(clave, enviados.get(clave));
            CampoCertificado finalCampo;
            if (sistema.getValor() != null) {
                if (enviado == null || mismoValor(sistema.getValor(), enviado) || !sistema.isEditable()) {
                    finalCampo = CampoCertificado.sistema(sistema.getValor(), sistema.getValorTexto(),
                            sistema.isEditable(), sistema.getFuente());
                } else {
                    finalCampo = new CampoCertificado(enviado, textoDeCampo(clave, enviado),
                            CampoCertificado.ORIGEN_MANUAL_EDITADO, true, null);
                }
            } else {
                if (enviado == null) {
                    faltantes.add(clave);
                    continue;
                }
                finalCampo = new CampoCertificado(enviado, textoDeCampo(clave, enviado),
                        CampoCertificado.ORIGEN_MANUAL_REQUERIDO, true, null);
            }
            finales.put(clave, finalCampo);
        }
        if (!faltantes.isEmpty()) {
            throw new IncomeException(ERR_CAMPO_REQUERIDO + ": falta capturar: " + String.join(", ", faltantes));
        }

        // 5. Número: lock + MAX+1 en esta transacción.
        //    El LOCK TABLE de CRD.CRTF queda tomado desde aquí hasta el commit, es decir durante
        //    TODA la generación del PDF: las emisiones se serializan a nivel global. Con el
        //    volumen real (~120 certificados al año) es irrelevante y NO es un defecto si algún
        //    día alguien ve una espera. Y no se puede tomar el número DESPUÉS del PDF porque el
        //    número va impreso adentro del documento.
        LocalDate fechaEmision = LocalDate.now();
        Long anio = Long.valueOf(fechaEmision.getYear());
        Long numero = certificadoDaoService.siguienteNumero(anio);
        String numeroAlterno = formatearNumero(anio, numero);

        // 6. Llenar el reporte. Todo por parámetros: el .jrxml no consulta CRD.CRTF.
        Map<String, Object> parametros = armarParametros(ctx, finales, calidadTexto, numeroAlterno,
                fechaEmision, solicitud.getUsuario());
        byte[] pdf = reporteService.generarReporte(MODULO_REPORTE, nombreReporte(ctx.tipo), parametros, "PDF");
        if (pdf == null || pdf.length == 0) {
            throw new IncomeException("El reporte " + nombreReporte(ctx.tipo) + " no produjo ningun PDF");
        }

        // 7. Grabar el certificado con su snapshot y su PDF
        Certificado certificado = new Certificado();
        certificado.setAnio(anio);
        certificado.setNumero(numero);
        certificado.setNumeroAlterno(numeroAlterno);
        certificado.setTipoCertificado(ctx.tipo);
        certificado.setEntidad(ctx.entidad);
        certificado.setPrestamo(ctx.prestamoElegido);
        certificado.setCalidad(calidad);
        certificado.setFechaEmision(fechaEmision);
        certificado.setUsuarioEmision(solicitud.getUsuario().trim());
        certificado.setDatos(armarSnapshot(finales, calidad, calidadTexto, origenCalidad, ctx.calidadSistema,
                solicitud.getIdLiquidacion()));
        certificado.setPdf(pdf);
        certificado.setEstado(Long.valueOf(EstadoCertificado.EMITIDO));
        certificado.setFechaRegistro(LocalDateTime.now());
        certificado = certificadoDaoService.save(certificado, null);

        ResultadoEmisionCertificado resultado = new ResultadoEmisionCertificado();
        resultado.setIdCertificado(certificado.getCodigo());
        resultado.setNumero(numero);
        resultado.setAnio(anio);
        resultado.setNumeroAlterno(numeroAlterno);
        resultado.setFechaEmision(fechaEmision);
        resultado.setTipo(ctx.tipo);
        resultado.setTipoTexto(ctx.tipoTexto);
        resultado.setCalidad(calidad);
        resultado.setCalidadTexto(calidadTexto);
        resultado.setCampos(finales);
        resultado.setUrlPdf("/rest/crtf/pdf/" + certificado.getCodigo());
        return resultado;
    }

    // ========================================================================
    // Consulta, reimpresión, anulación
    // ========================================================================

    @Override
    public List<Certificado> listarPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("listarPorEntidad - Certificado - entidad: " + idEntidad);
        if (idEntidad == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        return certificadoDaoService.selectByEntidad(idEntidad);
    }

    @Override
    public byte[] obtenerPdf(Long idCertificado) throws Throwable {
        System.out.println("obtenerPdf - Certificado: " + idCertificado);
        Certificado certificado = buscar(idCertificado);
        if (certificado.getPdf() == null || certificado.getPdf().length == 0) {
            throw new IncomeException(ERR_CERTIFICADO_NO_ENCONTRADO
                    + ": el certificado " + idCertificado + " no tiene PDF guardado");
        }
        return certificado.getPdf();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Certificado anular(Long idCertificado, String motivo, String usuario) throws Throwable {
        System.out.println("anular - Certificado: " + idCertificado + " por usuario: " + usuario);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el motivo de anulacion es obligatorio");
        }
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el usuario es obligatorio");
        }
        Certificado certificado = buscar(idCertificado);
        if (certificado.getEstado() != null
                && certificado.getEstado().intValue() == EstadoCertificado.ANULADO) {
            throw new IncomeException(ERR_CERTIFICADO_YA_ANULADO
                    + ": el certificado " + certificado.getNumeroAlterno() + " ya estaba anulado");
        }
        certificado.setEstado(Long.valueOf(EstadoCertificado.ANULADO));
        certificado.setMotivoAnulacion(motivo.trim());
        certificado.setUsuarioAnulacion(usuario.trim());
        certificado.setFechaAnulacion(LocalDateTime.now());
        return certificadoDaoService.save(certificado, certificado.getCodigo());
    }

    @Override
    public String formatearNumero(Long anio, Long numero) {
        return String.format("%s-%03d-%d", PREFIJO_NUMERO, numero, anio);
    }

    // ========================================================================
    // Resolución del contexto (compartida por precarga y emisión)
    // ========================================================================

    /** Todo lo que el sistema sabe de un certificado antes de que el operador toque nada. */
    private static class Contexto {
        Long tipo;
        String tipoTexto;
        Entidad entidad;
        Long calidadSistema;
        String calidadSistemaTexto;
        Map<String, CampoCertificado> campos = new LinkedHashMap<>();
        List<MotivoBloqueoCertificado> bloqueos = new ArrayList<>();
        List<PrestamoCertificado> prestamos = new ArrayList<>();
        List<LiquidacionCertificado> liquidaciones = new ArrayList<>();
        Prestamo prestamoElegido;
        HistoricoPagoCuentaIndividual liquidacionElegida;
    }

    private Contexto resolver(Long idEntidad, Long tipo, Long idPrestamo, Long idLiquidacion) throws Throwable {
        if (idEntidad == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idEntidad es obligatorio");
        }
        if (tipo == null || tipo < CrdTipoCertificado.AL_DIA_EN_OBLIGACIONES
                || tipo > CrdTipoCertificado.APORTES_PATRONALES_SIN_JUBILACION) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": tipo de certificado desconocido: " + tipo);
        }

        Contexto ctx = new Contexto();
        ctx.tipo = tipo;
        ctx.tipoTexto = textoTipo(tipo);
        try {
            ctx.entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
        } catch (NoResultException e) {
            throw new IncomeException(ERR_ENTIDAD_NO_ENCONTRADA + ": no existe el participe " + idEntidad);
        }
        ctx.calidadSistema = ctx.entidad.getIdEstado();
        ctx.calidadSistemaTexto = nombreEstadoParticipe(ctx.calidadSistema);

        // Comunes: nunca editables, siempre SISTEMA
        ctx.campos.put(CAMPO_FIRMANTE, CampoCertificado.sistema(
                parametro(CrdParametroCertificado.FIRMANTE), null, false, "SCP.PDTR rubro 243/1"));
        ctx.campos.put(CAMPO_CARGO, CampoCertificado.sistema(
                parametro(CrdParametroCertificado.CARGO_FIRMANTE), null, false, "SCP.PDTR rubro 243/2"));
        ctx.campos.put(CAMPO_CIUDAD, CampoCertificado.sistema(
                parametro(CrdParametroCertificado.CIUDAD_EMISION), null, false, "SCP.PDTR rubro 243/3"));
        ctx.campos.put(CAMPO_FUENTE_DATOS, CampoCertificado.sistema(FUENTE_DATOS, null, false, "fijo"));

        switch (tipo.intValue()) {
            case CrdTipoCertificado.AL_DIA_EN_OBLIGACIONES:
                resolverAlDia(ctx);
                break;
            case CrdTipoCertificado.HABER_RECIBIDO_APORTES:
                resolverHaberRecibidoAportes(ctx, idLiquidacion);
                break;
            case CrdTipoCertificado.NO_ADEUDAR_CREDITO:
                resolverNoAdeudarCredito(ctx, idPrestamo);
                break;
            case CrdTipoCertificado.NO_ADEUDAR_GLOBAL:
                resolverNoAdeudarGlobal(ctx);
                break;
            case CrdTipoCertificado.LICITUD_DE_FONDOS:
                resolverLicitudDeFondos(ctx, idLiquidacion);
                break;
            case CrdTipoCertificado.APORTES_PATRONALES_SIN_JUBILACION:
                resolverAportesPatronales(ctx);
                break;
            default:
                throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": tipo de certificado desconocido: " + tipo);
        }
        // Texto imprimible de los campos que el sistema resolvió
        for (Map.Entry<String, CampoCertificado> e : ctx.campos.entrySet()) {
            CampoCertificado c = e.getValue();
            if (c.getValor() != null && c.getValorTexto() == null) {
                c.setValorTexto(textoDeCampo(e.getKey(), c.getValor()));
            }
        }
        return ctx;
    }

    /** Tipo 1: año desde + regla "al día" (a)+(b)+(c). Bloquea; no hay "con advertencia". */
    private void resolverAlDia(Contexto ctx) throws Throwable {
        Long idEntidad = ctx.entidad.getCodigo();

        // anioDesde: contrato de adhesión, fallback primer aporte personal
        LocalDate desde = null;
        String fuente = null;
        List<Contrato> contratos = contratoDaoService.selectByEntidad(idEntidad);
        if (contratos != null) {
            for (Contrato c : contratos) {
                if (c.getFechaInicio() != null && (desde == null || c.getFechaInicio().toLocalDate().isBefore(desde))) {
                    desde = c.getFechaInicio().toLocalDate();
                    fuente = "CRD.CNTR.CNTRFCIN #" + c.getCodigo();
                }
            }
        }
        if (desde == null) {
            desde = certificadoDaoService.primerPeriodoAporte(idEntidad, TIPOS_APORTE_PERSONAL);
            fuente = "CRD.APRT primer periodo (tipos 9/11)";
        }
        ctx.campos.put(CAMPO_ANIO_DESDE, desde != null
                ? CampoCertificado.sistema(Long.valueOf(desde.getYear()), null, true, fuente)
                : CampoCertificado.requerido());

        // (c) partícipe marcado en mora por falta de aportes
        if (ctx.calidadSistema != null && ctx.calidadSistema.intValue() == EstadoParticipeEntidad.ACTIVO_EN_MORA) {
            ctx.bloqueos.add(new MotivoBloqueoCertificado(MotivoBloqueoCertificado.PARTICIPE_EN_MORA,
                    "El participe esta marcado ACTIVO EN MORA por falta de aportes"));
        }

        // (a) y (b) sobre sus préstamos
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(idEntidad);
        LocalDateTime corte = LocalDate.now().atStartOfDay();   // una cuota que vence hoy todavía no está vencida
        if (prestamos != null) {
            for (Prestamo p : prestamos) {
                int estado = p.getIdEstado() != null ? p.getIdEstado().intValue() : -1;
                if (ESTADOS_MORA.contains(estado)) {
                    ctx.bloqueos.add(bloqueoPrestamo(MotivoBloqueoCertificado.PRESTAMO_EN_MORA, p,
                            "El prestamo " + descripcionPrestamo(p) + " esta " + nombreEstadoPrestamo(p.getIdEstado())));
                } else if (estado == EstadoPrestamo.VIGENTE) {
                    List<DetallePrestamo> vencidas = detallePrestamoDaoService.selectCuotasVencidasByPrestamo(p.getCodigo(), corte);
                    if (vencidas != null && !vencidas.isEmpty()) {
                        ctx.bloqueos.add(bloqueoPrestamo(MotivoBloqueoCertificado.CUOTA_VENCIDA, p,
                                "El prestamo " + descripcionPrestamo(p) + " tiene " + vencidas.size()
                                + (vencidas.size() == 1 ? " cuota vencida" : " cuotas vencidas")));
                    }
                }
            }
        }
    }

    /** Tipo 2: fecha de liquidación de la cuenta patronal desde HPCS (JP/CP). */
    private void resolverHaberRecibidoAportes(Contexto ctx, Long idLiquidacion) throws Throwable {
        List<HistoricoPagoCuentaIndividual> pagos = historicoPagoCuentaIndividualDaoService.selectByCedulaAndTipos(
                ctx.entidad.getNumeroIdentificacion(),
                Arrays.asList(HistoricoPagoCuentaIndividual.TIPO_JUBILACION_PATRONAL,
                              HistoricoPagoCuentaIndividual.TIPO_CESANTIA_PATRONAL));
        HistoricoPagoCuentaIndividual elegida = elegirLiquidacion(ctx, pagos, idLiquidacion);
        ctx.campos.put(CAMPO_FECHA_LIQUIDACION, elegida != null
                ? CampoCertificado.sistema(elegida.getFechaPago().toString(), null, true, "CRD.HPCS #" + elegida.getCodigo())
                : CampoCertificado.requerido());
    }

    /** Tipo 3: un préstamo elegido, cancelado (3/4/5; el 9 se rechaza). */
    private void resolverNoAdeudarCredito(Contexto ctx, Long idPrestamo) throws Throwable {
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(ctx.entidad.getCodigo());
        ctx.prestamos = describirPrestamos(prestamos);
        if (idPrestamo == null) {
            // Sin préstamo elegido: la pantalla muestra la lista; los campos quedan pendientes de la elección
            ctx.campos.put(CAMPO_NUMERO_CREDITO, CampoCertificado.requerido());
            ctx.campos.get(CAMPO_NUMERO_CREDITO).setEditable(false);
            ctx.campos.put(CAMPO_PRODUCTO_TEXTO, CampoCertificado.requerido());
            ctx.campos.get(CAMPO_PRODUCTO_TEXTO).setEditable(false);
            return;
        }
        Prestamo elegido = null;
        if (prestamos != null) {
            for (Prestamo p : prestamos) {
                if (idPrestamo.equals(p.getCodigo())) {
                    elegido = p;
                    break;
                }
            }
        }
        if (elegido == null) {
            ctx.bloqueos.add(new MotivoBloqueoCertificado(MotivoBloqueoCertificado.PRESTAMO_NO_PERTENECE,
                    "El prestamo " + idPrestamo + " no pertenece al participe"));
            ctx.campos.put(CAMPO_NUMERO_CREDITO, CampoCertificado.requerido());
            ctx.campos.put(CAMPO_PRODUCTO_TEXTO, CampoCertificado.requerido());
            return;
        }
        ctx.prestamoElegido = elegido;
        ctx.campos.put(CAMPO_NUMERO_CREDITO, CampoCertificado.sistema(numeroCredito(elegido), null, false,
                elegido.getIdAsoprep() != null ? "CRD.PRST.PRSTIDAS" : "CRD.PRST.PRSTCDGO"));
        ctx.campos.put(CAMPO_PRODUCTO_TEXTO, CampoCertificado.sistema(textoProducto(elegido), null, false,
                "CRD.PRDC.PRDCNMBR"));
        MotivoBloqueoCertificado bloqueo = bloqueoNoCancelado(elegido);
        if (bloqueo != null) {
            ctx.bloqueos.add(bloqueo);
        }
    }

    /** Tipo 4: todos sus préstamos cancelados. Sin préstamos = no adeuda nada. */
    private void resolverNoAdeudarGlobal(Contexto ctx) throws Throwable {
        List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(ctx.entidad.getCodigo());
        ctx.prestamos = describirPrestamos(prestamos);
        if (prestamos != null) {
            for (Prestamo p : prestamos) {
                MotivoBloqueoCertificado bloqueo = bloqueoNoCancelado(p);
                if (bloqueo != null) {
                    ctx.bloqueos.add(bloqueo);
                }
            }
        }
    }

    /** Tipo 5: monto/fecha/concepto desde HPCS (cualquier tipo) y cuenta/banco desde CNBP. */
    private void resolverLicitudDeFondos(Contexto ctx, Long idLiquidacion) throws Throwable {
        List<HistoricoPagoCuentaIndividual> pagos = historicoPagoCuentaIndividualDaoService.selectByCedula(
                ctx.entidad.getNumeroIdentificacion());
        HistoricoPagoCuentaIndividual elegida = elegirLiquidacion(ctx, pagos, idLiquidacion);
        if (elegida != null) {
            String fuente = "CRD.HPCS #" + elegida.getCodigo();
            ctx.campos.put(CAMPO_MONTO, CampoCertificado.sistema(elegida.getValor(), null, true, fuente));
            ctx.campos.put(CAMPO_FECHA_PAGO, CampoCertificado.sistema(elegida.getFechaPago().toString(), null, true, fuente));
            ctx.campos.put(CAMPO_CONCEPTO_DEVOLUCION, CampoCertificado.sistema(
                    conceptoDevolucion(elegida.getTipo()), null, true, fuente + " (" + elegida.getTipo() + ")"));
        } else {
            ctx.campos.put(CAMPO_MONTO, CampoCertificado.requerido());
            ctx.campos.put(CAMPO_FECHA_PAGO, CampoCertificado.requerido());
            ctx.campos.put(CAMPO_CONCEPTO_DEVOLUCION, CampoCertificado.requerido());
        }

        CuentaBancariaParticipe cuenta = null;
        List<CuentaBancariaParticipe> cuentas = cuentaBancariaParticipeDaoService.selectByParent(ctx.entidad.getCodigo());
        if (cuentas != null) {
            for (CuentaBancariaParticipe c : cuentas) {
                boolean activa = c.getEstado() == null || c.getEstado().intValue() == Estado.ACTIVO;
                if (activa && (cuenta == null || c.getCodigo() < cuenta.getCodigo())) {
                    cuenta = c;
                }
            }
        }
        if (cuenta != null) {
            String fuente = "CRD.CNBP #" + cuenta.getCodigo();
            ctx.campos.put(CAMPO_TIPO_CUENTA, CampoCertificado.sistema(cuenta.getTipoCuenta(), null, true, fuente));
            ctx.campos.put(CAMPO_NUMERO_CUENTA, CampoCertificado.sistema(cuenta.getNumeroCuenta(), null, true, fuente));
            ctx.campos.put(CAMPO_BANCO, cuenta.getBancoExterno() != null && cuenta.getBancoExterno().getNombre() != null
                    ? CampoCertificado.sistema(cuenta.getBancoExterno().getNombre(), null, true, "TSR.BEXT.BEXTNMBR")
                    : CampoCertificado.requerido());
        } else {
            ctx.campos.put(CAMPO_TIPO_CUENTA, CampoCertificado.requerido());
            ctx.campos.put(CAMPO_NUMERO_CUENTA, CampoCertificado.requerido());
            ctx.campos.put(CAMPO_BANCO, CampoCertificado.requerido());
        }
    }

    /** Tipo 6: tres banderas desde APRT/HPPJ (editables) + fecha de corte siempre manual. */
    private void resolverAportesPatronales(Contexto ctx) throws Throwable {
        Long idEntidad = ctx.entidad.getCodigo();
        boolean cesantiaPatronal = certificadoDaoService.existeAporteDeTipos(idEntidad, TIPOS_CESANTIA_PATRONAL);
        boolean jubilacionPatronalConMovimientos = certificadoDaoService.existeAporteDeTipos(idEntidad, TIPOS_JUBILACION_PATRONAL);
        boolean pension = certificadoDaoService.existeAporteDeTipos(idEntidad, TIPOS_PENSION_COMPLEMENTARIA)
                || certificadoDaoService.existePagoPension(ctx.entidad.getNumeroIdentificacion());

        ctx.campos.put(CAMPO_RECIBIO_CESANTIA_PATRONAL, CampoCertificado.sistema(
                Boolean.valueOf(cesantiaPatronal), null, true, "CRD.APRT tipos 14/16"));
        ctx.campos.put(CAMPO_JUBILACION_PATRONAL_SIN_MOVIMIENTOS, CampoCertificado.sistema(
                Boolean.valueOf(!jubilacionPatronalConMovimientos), null, true, "CRD.APRT tipos 13/15"));
        ctx.campos.put(CAMPO_RECIBE_PENSION_MENSUAL, CampoCertificado.sistema(
                Boolean.valueOf(pension), null, true, "CRD.APRT tipo 23 / CRD.HPPJ"));
        ctx.campos.put(CAMPO_FECHA_CORTE_PENSION, CampoCertificado.requerido());   // no existe en ninguna tabla

        if (pension) {
            ctx.bloqueos.add(new MotivoBloqueoCertificado(MotivoBloqueoCertificado.RECIBE_PENSION,
                    "El participe registra pagos de pension complementaria: no se puede certificar que no la recibe"));
        }
    }

    // ========================================================================
    // Apoyo: préstamos
    // ========================================================================

    private List<PrestamoCertificado> describirPrestamos(List<Prestamo> prestamos) {
        List<PrestamoCertificado> lista = new ArrayList<>();
        if (prestamos == null) {
            return lista;
        }
        for (Prestamo p : prestamos) {
            PrestamoCertificado dto = new PrestamoCertificado();
            dto.setIdPrestamo(p.getCodigo());
            dto.setNumeroCredito(numeroCredito(p));
            dto.setProducto(p.getProducto() != null ? p.getProducto().getNombre() : null);
            dto.setProductoTexto(textoProducto(p));
            dto.setFecha(p.getFecha() != null ? p.getFecha().toLocalDate() : null);
            dto.setEstado(p.getIdEstado());
            dto.setEstadoTexto(nombreEstadoPrestamo(p.getIdEstado()));
            dto.setCancelado(p.getIdEstado() != null && ESTADOS_CANCELADO.contains(p.getIdEstado().intValue()));
            lista.add(dto);
        }
        return lista;
    }

    /** Un solo motivo por préstamo, el más específico; null si está cancelado. */
    private MotivoBloqueoCertificado bloqueoNoCancelado(Prestamo p) {
        int estado = p.getIdEstado() != null ? p.getIdEstado().intValue() : -1;
        if (ESTADOS_CANCELADO.contains(estado)) {
            return null;
        }
        String descripcion = "El prestamo " + descripcionPrestamo(p);
        if (ESTADOS_MORA.contains(estado)) {
            return bloqueoPrestamo(MotivoBloqueoCertificado.PRESTAMO_EN_MORA, p,
                    descripcion + " esta " + nombreEstadoPrestamo(p.getIdEstado()));
        }
        if (estado == EstadoPrestamo.CANCELADO_POR_REVISAR) {
            return bloqueoPrestamo(MotivoBloqueoCertificado.PRESTAMO_POR_REVISAR, p,
                    descripcion + " esta CANCELADO POR REVISAR: debe revisarse antes de certificar");
        }
        return bloqueoPrestamo(MotivoBloqueoCertificado.PRESTAMO_NO_CANCELADO, p,
                descripcion + " esta " + nombreEstadoPrestamo(p.getIdEstado()) + ", no cancelado");
    }

    private MotivoBloqueoCertificado bloqueoPrestamo(String codigo, Prestamo p, String mensaje) {
        MotivoBloqueoCertificado m = new MotivoBloqueoCertificado(codigo, mensaje);
        m.setIdPrestamo(p.getCodigo());
        m.setNumeroCredito(numeroCredito(p));
        m.setProducto(p.getProducto() != null ? p.getProducto().getNombre() : null);
        m.setEstado(p.getIdEstado());
        m.setEstadoTexto(nombreEstadoPrestamo(p.getIdEstado()));
        return m;
    }

    /** PRSTIDAS (número de operación ASOPREP) o PRSTCDGO si no tiene. NUNCA APRTIDAS. */
    private Long numeroCredito(Prestamo p) {
        return p.getIdAsoprep() != null ? p.getIdAsoprep() : p.getCodigo();
    }

    private String descripcionPrestamo(Prestamo p) {
        return textoProducto(p) + " No. " + numeroCredito(p);
    }

    /** "EMERGENTE" → "Crédito Emergente"; "EMERGENTE RESTRUCTURADO" → "Crédito Emergente Restructurado". */
    private String textoProducto(Prestamo p) {
        String nombre = p.getProducto() != null && p.getProducto().getNombre() != null
                ? p.getProducto().getNombre().trim() : "";
        if (nombre.isEmpty()) {
            return "Crédito";
        }
        StringBuilder sb = new StringBuilder("Crédito");
        for (String palabra : nombre.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!palabra.isEmpty()) {
                sb.append(' ').append(Character.toUpperCase(palabra.charAt(0))).append(palabra.substring(1));
            }
        }
        return sb.toString();
    }

    /** Nombres de PRSTIDST según com.saa.rubros.EstadoPrestamo (el catálogo CRD.ESPS no está alineado con ese código). */
    private String nombreEstadoPrestamo(Long idEstado) {
        if (idEstado == null) {
            return "SIN ESTADO";
        }
        switch (idEstado.intValue()) {
            case EstadoPrestamo.GENERADO: return "GENERADO";
            case EstadoPrestamo.VIGENTE: return "VIGENTE";
            case EstadoPrestamo.CANCELADO: return "CANCELADO";
            case EstadoPrestamo.CANCELADO_ANTICIPADO: return "CANCELADO ANTICIPADO";
            case EstadoPrestamo.CANCELADO_POR_NOVACION: return "CANCELADO POR NOVACION";
            case EstadoPrestamo.PENDIENTE_DE_APROBACION: return "PENDIENTE DE APROBACION";
            case EstadoPrestamo.RECHAZADO: return "RECHAZADO";
            case EstadoPrestamo.DE_PLAZO_VENCIDO: return "DE PLAZO VENCIDO";
            case EstadoPrestamo.CANCELADO_POR_REVISAR: return "CANCELADO POR REVISAR";
            case EstadoPrestamo.VIGENTE_POR_REVISAR: return "VIGENTE POR REVISAR";
            case EstadoPrestamo.EN_MORA: return "EN MORA";
            default: return "ESTADO " + idEstado;
        }
    }

    // ========================================================================
    // Apoyo: liquidaciones (HPCS)
    // ========================================================================

    /** Llena ctx.liquidaciones y devuelve la elegida (por id, o la más reciente). */
    private HistoricoPagoCuentaIndividual elegirLiquidacion(Contexto ctx, List<HistoricoPagoCuentaIndividual> pagos,
            Long idLiquidacion) {
        HistoricoPagoCuentaIndividual elegida = null;
        if (pagos != null) {
            for (HistoricoPagoCuentaIndividual h : pagos) {
                LiquidacionCertificado dto = new LiquidacionCertificado();
                dto.setIdLiquidacion(h.getCodigo());
                dto.setFechaPago(h.getFechaPago());
                dto.setTipo(tipoLiquidacion(h.getTipo()));
                dto.setTipoTexto(textoTipoLiquidacion(h.getTipo()));
                dto.setValor(h.getValor());
                dto.setObservacion(h.getObservacion());
                ctx.liquidaciones.add(dto);
                if (idLiquidacion != null && idLiquidacion.equals(h.getCodigo())) {
                    elegida = h;
                }
            }
            if (elegida == null && idLiquidacion == null && !pagos.isEmpty()) {
                elegida = pagos.get(0);   // el DAO ordena por fecha desc
            }
        }
        ctx.liquidacionElegida = elegida;
        return elegida;
    }

    private String tipoLiquidacion(String tipo) {
        return tipo == null ? null : tipo.trim().toUpperCase(Locale.ROOT);
    }

    private String textoTipoLiquidacion(String tipo) {
        String t = tipoLiquidacion(tipo);
        if (t == null) {
            return null;
        }
        switch (t) {
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION: return "Jubilación";
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA: return "Cesantía";
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION_PATRONAL: return "Jubilación patronal";
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA_PATRONAL: return "Cesantía patronal";
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION_RETIRO_VOLUNTARIO: return "Jubilación retiro voluntario";
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA_RETIRO_VOLUNTARIO: return "Cesantía retiro voluntario";
            default: return t;
        }
    }

    /** Texto del cuerpo del certificado 5 según HPCSTIPC (§4 del contrato). */
    private String conceptoDevolucion(String tipo) {
        String t = tipoLiquidacion(tipo);
        if (t == null) {
            return "fondo";
        }
        switch (t) {
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION_RETIRO_VOLUNTARIO: return "fondo de jubilación retiro voluntario";
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA_RETIRO_VOLUNTARIO: return "fondo de cesantía retiro voluntario";
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION:
            case HistoricoPagoCuentaIndividual.TIPO_JUBILACION_PATRONAL: return "fondo de jubilación";
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA:
            case HistoricoPagoCuentaIndividual.TIPO_CESANTIA_PATRONAL: return "fondo de cesantía";
            default: return "fondo";
        }
    }

    // ========================================================================
    // Apoyo: catálogos y textos
    // ========================================================================

    /**
     * Firmante, cargo y ciudad (rubro 243). <b>SIN fallback, a propósito.</b> Se imprimen en
     * un documento firmado: si el rubro no está cargado o la lectura falla, la emisión falla
     * con 422. Un nombre cableado en el código como respaldo haría que, el día que cambie el
     * jefe de crédito y la lectura falle por lo que sea, el sistema siga imprimiendo al
     * anterior sin avisar — justo lo que la parametrización venía a evitar. El lado seguro
     * aquí es NO emitir. (Para tipoTexto, rubro 244, sí hay fallback: es una etiqueta
     * interna, no va al papel.)
     */
    private String parametro(int alternoDetalle) throws Throwable {
        String valor;
        try {
            valor = detalleRubroDaoService.selectValorStringByRubAltDetAlt(
                    Rubros.CRD_PARAMETROS_CERTIFICADOS, alternoDetalle);
        } catch (IncomeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IncomeException("Falta la parametrizacion de certificados: rubro "
                    + Rubros.CRD_PARAMETROS_CERTIFICADOS + " detalle " + alternoDetalle
                    + " (firmante/cargo/ciudad). Ver sql/DDL-CERTIFICADOS-CREDITO.sql. Causa: " + e.getMessage());
        }
        if (valor == null || valor.trim().isEmpty()) {
            throw new IncomeException("Falta la parametrizacion de certificados: rubro "
                    + Rubros.CRD_PARAMETROS_CERTIFICADOS + " detalle " + alternoDetalle
                    + " no tiene valor en SCP.PDTR.PDTRVLRV");
        }
        return valor.trim();
    }

    private String textoTipo(Long tipo) throws Throwable {
        try {
            String texto = detalleRubroDaoService.selectDescripcionByRubAltDetAlt(
                    Rubros.CRD_TIPO_CERTIFICADO, tipo.intValue());
            if (texto != null && !texto.trim().isEmpty()) {
                return texto.trim();
            }
        } catch (NoResultException e) {
            // el catálogo todavía no está cargado: se cae al nombre de la constante
        }
        switch (tipo.intValue()) {
            case CrdTipoCertificado.AL_DIA_EN_OBLIGACIONES: return "AL DIA EN SUS OBLIGACIONES";
            case CrdTipoCertificado.HABER_RECIBIDO_APORTES: return "HABER RECIBIDO APORTES";
            case CrdTipoCertificado.NO_ADEUDAR_CREDITO: return "NO ADEUDAR - CREDITO";
            case CrdTipoCertificado.NO_ADEUDAR_GLOBAL: return "NO ADEUDAR - GLOBAL";
            case CrdTipoCertificado.LICITUD_DE_FONDOS: return "LICITUD DE FONDOS DEPOSITADOS";
            case CrdTipoCertificado.APORTES_PATRONALES_SIN_JUBILACION: return "APORTES PATRONALES SIN JUBILACION MENSUAL";
            default: return "TIPO " + tipo;
        }
    }

    /** Nombre del estado en CRD.ESPR por código alterno (ESPRCDEX). */
    private String nombreEstadoParticipe(Long codigoAlterno) throws Throwable {
        if (codigoAlterno == null) {
            return null;
        }
        List<EstadoParticipe> estados = estadoParticipeDaoService.selectAll(NombreEntidadesCredito.ESTADO_PARTICIPE);
        if (estados != null) {
            for (EstadoParticipe e : estados) {
                if (codigoAlterno.equals(e.getCodigoExterno())) {
                    return e.getNombre();
                }
            }
        }
        return "ESTADO " + codigoAlterno;
    }

    /** Las tres palabras del certificado: partícipe / partícipe cesante / partícipe jubilado. */
    private String textoCalidad(Long calidad) {
        if (calidad == null) {
            return "partícipe";
        }
        switch (calidad.intValue()) {
            case EstadoParticipeEntidad.CESANTE:
            case EstadoParticipeEntidad.CESANTE_DESAFILIADO:
            case EstadoParticipeEntidad.CESANTE_FALLECIDO:
                return "partícipe cesante";
            case EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO:
            case EstadoParticipeEntidad.JUBILADO_APORTANTE:
            case EstadoParticipeEntidad.JUBILADO_PASIVO:
                return "partícipe jubilado";
            default:
                return "partícipe";
        }
    }

    /** "02 de julio de 2026" */
    private String fechaLarga(LocalDate fecha) {
        return String.format("%02d de %s de %d", fecha.getDayOfMonth(), MESES[fecha.getMonthValue() - 1], fecha.getYear());
    }

    /** "$145.728,15" (formato del certificado original: punto de miles, coma decimal). */
    private String montoTexto(Number monto) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        DecimalFormat formato = new DecimalFormat("#,##0.00", simbolos);
        return "$" + formato.format(new BigDecimal(monto.toString()).setScale(2, RoundingMode.HALF_UP));
    }

    /** Cómo se imprime cada clave. Las fechas llegan como "yyyy-MM-dd". */
    private String textoDeCampo(String clave, Object valor) {
        if (valor == null) {
            return null;
        }
        switch (clave) {
            case CAMPO_FECHA_LIQUIDACION:
            case CAMPO_FECHA_PAGO:
            case CAMPO_FECHA_CORTE_PENSION:
                return fechaLarga(LocalDate.parse(valor.toString()));
            case CAMPO_MONTO:
                return montoTexto((Number) valor);
            case CAMPO_TIPO_CUENTA:
                return ((Number) valor).intValue() == TipoCuentasBancarias.AHORROS ? "cuenta de ahorros" : "cuenta corriente";
            case CAMPO_RECIBIO_CESANTIA_PATRONAL:
            case CAMPO_JUBILACION_PATRONAL_SIN_MOVIMIENTOS:
            case CAMPO_RECIBE_PENSION_MENSUAL:
                return Boolean.TRUE.equals(valor) ? "sí" : "no";
            case CAMPO_ANIO_DESDE:
            case CAMPO_NUMERO_CREDITO:
                return String.valueOf(((Number) valor).longValue());
            default:
                return valor.toString();
        }
    }

    /**
     * Lleva lo que mandó el frontend al tipo con el que se compara y se imprime. Jackson
     * entrega Integer/Double/String/Boolean; aquí se normaliza y se valida el formato.
     */
    private Object normalizarValor(String clave, Object enviado) throws Throwable {
        if (enviado == null || (enviado instanceof String && ((String) enviado).trim().isEmpty())) {
            return null;
        }
        try {
            switch (clave) {
                case CAMPO_FECHA_LIQUIDACION:
                case CAMPO_FECHA_PAGO:
                case CAMPO_FECHA_CORTE_PENSION:
                    return LocalDate.parse(enviado.toString().trim()).toString();
                case CAMPO_MONTO:
                    return Double.valueOf(new BigDecimal(enviado.toString().trim())
                            .setScale(2, RoundingMode.HALF_UP).doubleValue());
                case CAMPO_ANIO_DESDE:
                case CAMPO_NUMERO_CREDITO:
                case CAMPO_TIPO_CUENTA:
                    return Long.valueOf(new BigDecimal(enviado.toString().trim()).longValue());
                case CAMPO_RECIBIO_CESANTIA_PATRONAL:
                case CAMPO_JUBILACION_PATRONAL_SIN_MOVIMIENTOS:
                case CAMPO_RECIBE_PENSION_MENSUAL:
                    return enviado instanceof Boolean ? enviado : Boolean.valueOf(enviado.toString().trim());
                default:
                    return enviado.toString().trim();
            }
        } catch (RuntimeException e) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el campo " + clave
                    + " tiene un valor invalido (" + enviado + "). Las fechas van como yyyy-MM-dd");
        }
    }

    private boolean mismoValor(Object sistema, Object enviado) {
        if (sistema instanceof Number && enviado instanceof Number) {
            return new BigDecimal(sistema.toString()).compareTo(new BigDecimal(enviado.toString())) == 0;
        }
        return sistema.equals(enviado) || sistema.toString().equals(enviado.toString());
    }

    // ========================================================================
    // Reporte y snapshot
    // ========================================================================

    private String nombreReporte(Long tipo) {
        switch (tipo.intValue()) {
            case CrdTipoCertificado.AL_DIA_EN_OBLIGACIONES: return "RPRT_CRTF_ALDI";
            case CrdTipoCertificado.HABER_RECIBIDO_APORTES: return "RPRT_CRTF_APRT";
            case CrdTipoCertificado.NO_ADEUDAR_CREDITO: return "RPRT_CRTF_NOAD";
            case CrdTipoCertificado.NO_ADEUDAR_GLOBAL: return "RPRT_CRTF_NOAG";
            case CrdTipoCertificado.LICITUD_DE_FONDOS: return "RPRT_CRTF_LCTD";
            case CrdTipoCertificado.APORTES_PATRONALES_SIN_JUBILACION: return "RPRT_CRTF_PTRN";
            default: throw new IllegalArgumentException("Tipo de certificado sin reporte: " + tipo);
        }
    }

    /**
     * Parámetros del .jrxml. TODOS los textos van ya formateados como String (nada de
     * coerción de tipos en Jasper), y los 6 reportes comparten los P_* comunes. Las
     * banderas del tipo 6 van como Boolean para la expresión condicional del texto.
     * El único dato que el reporte consulta en la base es la entidad (P_ENTD_CODIGO).
     */
    private Map<String, Object> armarParametros(Contexto ctx, Map<String, CampoCertificado> campos,
            String calidadTexto, String numeroAlterno, LocalDate fechaEmision, String usuario) {
        Map<String, Object> p = new HashMap<>();
        p.put("P_ENTD_CODIGO", ctx.entidad.getCodigo());
        p.put("P_NUMERO", numeroAlterno);
        p.put("P_CIUDAD", texto(campos, CAMPO_CIUDAD));
        p.put("P_FECHA_TEXTO", fechaLarga(fechaEmision));
        p.put("P_NOMBRE", ctx.entidad.getRazonSocial());
        p.put("P_CEDULA", ctx.entidad.getNumeroIdentificacion());
        p.put("P_CALIDAD_TEXTO", calidadTexto);
        p.put("P_FIRMANTE", texto(campos, CAMPO_FIRMANTE));
        p.put("P_CARGO", texto(campos, CAMPO_CARGO));
        p.put("P_FUENTE", texto(campos, CAMPO_FUENTE_DATOS));
        p.put("P_USUARIO", usuario);
        switch (ctx.tipo.intValue()) {
            case CrdTipoCertificado.AL_DIA_EN_OBLIGACIONES:
                p.put("P_ANIO_DESDE", texto(campos, CAMPO_ANIO_DESDE));
                break;
            case CrdTipoCertificado.HABER_RECIBIDO_APORTES:
                p.put("P_FECHA_LIQUIDACION", texto(campos, CAMPO_FECHA_LIQUIDACION));
                break;
            case CrdTipoCertificado.NO_ADEUDAR_CREDITO:
                p.put("P_NUMERO_CREDITO", texto(campos, CAMPO_NUMERO_CREDITO));
                p.put("P_PRODUCTO_TEXTO", texto(campos, CAMPO_PRODUCTO_TEXTO));
                break;
            case CrdTipoCertificado.LICITUD_DE_FONDOS:
                p.put("P_MONTO_TEXTO", texto(campos, CAMPO_MONTO));
                p.put("P_FECHA_PAGO", texto(campos, CAMPO_FECHA_PAGO));
                p.put("P_CONCEPTO", texto(campos, CAMPO_CONCEPTO_DEVOLUCION));
                p.put("P_TIPO_CUENTA_TEXTO", texto(campos, CAMPO_TIPO_CUENTA));
                p.put("P_NUMERO_CUENTA", texto(campos, CAMPO_NUMERO_CUENTA));
                p.put("P_BANCO", texto(campos, CAMPO_BANCO));
                break;
            case CrdTipoCertificado.APORTES_PATRONALES_SIN_JUBILACION:
                p.put("P_RECIBIO_CESANTIA", Boolean.TRUE.equals(campos.get(CAMPO_RECIBIO_CESANTIA_PATRONAL).getValor()));
                p.put("P_JUB_SIN_MOVIMIENTOS", Boolean.TRUE.equals(campos.get(CAMPO_JUBILACION_PATRONAL_SIN_MOVIMIENTOS).getValor()));
                p.put("P_FECHA_CORTE", texto(campos, CAMPO_FECHA_CORTE_PENSION));
                break;
            default:
                break;
        }
        return p;
    }

    private String texto(Map<String, CampoCertificado> campos, String clave) {
        CampoCertificado c = campos.get(clave);
        if (c == null) {
            return null;
        }
        return c.getValorTexto() != null ? c.getValorTexto() : (c.getValor() != null ? c.getValor().toString() : null);
    }

    /** JSON de todo lo impreso con su origen (CRTFDTOS). Solo tipos simples: no hace falta el módulo de fechas. */
    private String armarSnapshot(Map<String, CampoCertificado> campos, Long calidad, String calidadTexto,
            String origenCalidad, Long calidadSistema, Long idLiquidacion) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        Map<String, Object> calidadMap = new LinkedHashMap<>();
        calidadMap.put("valor", calidad);
        calidadMap.put("valorTexto", calidadTexto);
        calidadMap.put("origen", origenCalidad);
        calidadMap.put("calidadSistema", calidadSistema);
        snapshot.put("calidad", calidadMap);
        snapshot.put("idLiquidacion", idLiquidacion);
        snapshot.put("campos", campos);
        try {
            return new ObjectMapper().writeValueAsString(snapshot);
        } catch (Exception e) {
            System.out.println("No se pudo serializar el snapshot del certificado: " + e.getMessage());
            return snapshot.toString();
        }
    }

    /** selectById lanza NoResultException si no existe; aquí se traduce a un error de negocio. */
    private Certificado buscar(Long idCertificado) throws Throwable {
        if (idCertificado == null) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": idCertificado es obligatorio");
        }
        try {
            return certificadoDaoService.selectById(idCertificado, NombreEntidadesCredito.CERTIFICADO);
        } catch (NoResultException e) {
            throw new IncomeException(ERR_CERTIFICADO_NO_ENCONTRADO
                    + ": no existe el certificado " + idCertificado);
        }
    }
}
