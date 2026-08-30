package com.saa.ejb.cxp.serviceImpl;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.saa.ejb.cxc.dao.FacturadorDaoService;
import com.saa.ejb.cxp.dao.CargaArchivoTxtDaoService;
import com.saa.ejb.cxp.dao.DetalleCargaTxtDaoService;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.ejb.cxp.dao.DetalleFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.DetalleLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.dao.DetalleNotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.DetalleNotaDebitoCompraDaoService;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraDaoService;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.ejb.cxp.dao.FormaPagoFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.NotaDebitoCompraDaoService;
import com.saa.ejb.cxp.dao.PathFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.PathLiquidacionCompraCompraDaoService;
import com.saa.ejb.cxp.dao.PathNotaCreditoCompraDaoService;
import com.saa.ejb.cxp.dao.PathNotaDebitoCompraDaoService;
import com.saa.ejb.cxp.dao.PathRetencionCompraDaoService;
import com.saa.ejb.cxp.dao.RetencionCompraDaoService;
import com.saa.ejb.cxp.dao.RetencionCompraV2DaoService;
import com.saa.ejb.cxp.dao.ReembolsoFacturaCompraDaoService;
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
import com.saa.basico.util.IncomeException;
import com.saa.model.cxc.Facturador;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.cnt.Periodo;
import com.saa.model.cxp.CargaArchivoTxt;
import com.saa.model.cxp.DetalleCargaTxt;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.DetalleFacturaCompra;
import com.saa.model.cxp.DetalleLiquidacionCompraCompra;
import com.saa.model.cxp.DetalleNotaCreditoCompra;
import com.saa.model.cxp.DetalleNotaDebitoCompra;
import com.saa.model.cxp.DetalleRetencionCompra;
import com.saa.model.cxp.FacturaCompra;
import com.saa.model.cxp.FormaPagoFacturaCompra;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.model.cxp.NotaCreditoCompra;
import com.saa.model.cxp.NotaDebitoCompra;
import com.saa.model.cxp.PathFacturaCompra;
import com.saa.model.cxp.PathLiquidacionCompraCompra;
import com.saa.model.cxp.PathNotaCreditoCompra;
import com.saa.model.cxp.PathNotaDebitoCompra;
import com.saa.model.cxp.PathRetencionCompra;
import com.saa.model.cxp.RetencionCompra;
import com.saa.model.cxp.RetencionCompraV2;
import com.saa.model.cxp.ReembolsoFacturaCompra;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;
import com.saa.rubros.AccionNovedad;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoDocumentoCxp;
import com.saa.rubros.EstadoNovedad;
import com.saa.rubros.OrigenReembolso;
import com.saa.rubros.ResultadoCargaTxt;
import com.saa.rubros.RolPersona;
import com.saa.rubros.TipoGrupoProductos;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class ProcesoCargaDocumentosServiceImpl implements ProcesoCargaDocumentosService {

    @PersistenceContext
    private EntityManager em;

    @EJB private CargaArchivoTxtDaoService   cargaArchivoTxtDaoService;
    @EJB private DetalleCargaTxtDaoService   detalleCargaTxtDaoService;
    @EJB private DocumentoCxpDaoService      documentoCxpDaoService;
    @EJB private FacturadorDaoService        facturadorDaoService;
    @EJB private com.saa.ejb.tsr.dao.PersonaCuentaContableDaoService personaCuentaContableDaoService;

    // Destinos DAO
    @EJB private FacturaCompraDaoService                  facturaCompraDaoService;
    @EJB private DetalleFacturaCompraDaoService           detalleFacturaCompraDaoService;
    @EJB private FormaPagoFacturaCompraDaoService         formaPagoFacturaCompraDaoService;
    @EJB private PathFacturaCompraDaoService              pathFacturaCompraDaoService;

    @EJB private NotaCreditoCompraDaoService              notaCreditoCompraDaoService;
    @EJB private DetalleNotaCreditoCompraDaoService       detalleNotaCreditoCompraDaoService;
    @EJB private PathNotaCreditoCompraDaoService          pathNotaCreditoCompraDaoService;

    @EJB private NotaDebitoCompraDaoService               notaDebitoCompraDaoService;
    @EJB private DetalleNotaDebitoCompraDaoService        detalleNotaDebitoCompraDaoService;
    @EJB private PathNotaDebitoCompraDaoService           pathNotaDebitoCompraDaoService;

    @EJB private LiquidacionCompraCompraDaoService        liquidacionCompraCompraDaoService;
    @EJB private DetalleLiquidacionCompraCompraDaoService detalleLiquidacionCompraCompraDaoService;
    @EJB private PathLiquidacionCompraCompraDaoService    pathLiquidacionCompraCompraDaoService;

    @EJB private RetencionCompraDaoService                retencionCompraDaoService;
    @EJB private DetalleRetencionCompraDaoService         detalleRetencionCompraDaoService;
    @EJB private PathRetencionCompraDaoService            pathRetencionCompraDaoService;

    @EJB private RetencionCompraV2DaoService              retencionCompraV2DaoService;

    @EJB private ReembolsoFacturaCompraDaoService         reembolsoFacturaCompraDaoService;

    @EJB private ProductoPagoDaoService                   productoPagoDaoService;
    @EJB private GrupoProductoPagoDaoService              grupoProductoPagoDaoService;

    @EJB private com.saa.ejb.tsr.dao.TitularDaoService    titularDaoService;
    @EJB private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

    @EJB private com.saa.ejb.cxp.service.AplicacionPagoCxpService aplicacionPagoCxpService;

    // T3 (ATS): resuelve FCTCCSUS al registrar una factura de compra. No debe poder abortar el
    // registro del documento ni bloquear el lote -ver el uso en registrarFacturaCompra y el
    // marcado de "sustentoTributarioPendiente" en los dos call-sites de PASO 3-.
    @EJB private com.saa.ejb.cxp.service.SustentoTributarioService sustentoTributarioService;

    // El MISMO resolutor que usa la aplicación de pago, a nivel de DAO. Se llama
    // al DAO y no a AplicacionPagoCxpService.resolverFacturaCompraPorNumero
    // porque ese comunica el fallo con IncomeException, anotada
    // @ApplicationException(rollback = true): corre en esta transacción, así que
    // atraparla para devolver un bloqueante la dejaría marcada para rollback.
    // La consulta es la misma, de modo que los dos no pueden discrepar.
    @EJB private com.saa.ejb.cxp.dao.AplicacionPagoCxpDaoService aplicacionPagoCxpDaoService;

    @EJB private com.saa.ejb.cxc.service.AplicacionPagoCxcService aplicacionPagoCxcService;

    // El resolutor de la factura de VENTA, a nivel de DAO, por la misma razón que
    // el de compra: AplicacionPagoCxcService.resolverFacturaPorNumero comunica el
    // fallo con IncomeException y no se puede atrapar para continuar. La consulta
    // es la misma, así que el bloqueante y la aplicación de pago del Paso 4 no
    // pueden discrepar. Ver §11 decisión 18.
    @EJB private com.saa.ejb.cxc.dao.AplicacionPagoCxcDaoService aplicacionPagoCxcDaoService;
    // El DAO de tipos de asiento, no el servicio: TipoAsientoService.codigoByAlterno
    // comunica el "no existe" lanzando IncomeException, y esa no se puede atrapar
    // para seguir. Ver existeTipoAsiento y §11 decisión 18.
    @EJB private com.saa.ejb.cnt.dao.TipoAsientoDaoService      tipoAsientoDaoService;

    // Marcado del estado ERROR en transacción propia (REQUIRES_NEW).
    // Tiene que ser un bean DISTINTO inyectado con @EJB: si el método viviera en
    // esta misma clase, la llamada interna no pasaría por el proxy del contenedor,
    // no abriría transacción nueva y el marcado se perdería en el rollback igual
    // que antes. Ver MarcadoErrorDocumentoService.
    @EJB private com.saa.ejb.cxp.service.MarcadoErrorDocumentoService marcadoErrorDocumentoService;

    // -------------------------------------------------------
    // Estados — delegados a las interfaces de rubros (174-177)
    // -------------------------------------------------------
    private static final long ESTADO_LEIDO         = EstadoDocumentoCxp.LEIDO;
    private static final long ESTADO_XML_CARGADO   = EstadoDocumentoCxp.XML_CARGADO;
    private static final long ESTADO_REGISTRADO_BD = EstadoDocumentoCxp.REGISTRADO_BD;
    private static final long ESTADO_ERROR         = EstadoDocumentoCxp.ERROR;
    private static final long ESTADO_NOVEDAD       = EstadoDocumentoCxp.NOVEDAD;
    private static final long ESTADO_REVERTIDO     = EstadoDocumentoCxp.REVERTIDO;

    private static final long NOVEDAD_PENDIENTE    = EstadoNovedad.PENDIENTE;
    private static final long NOVEDAD_REEMPLAZADO  = EstadoNovedad.REEMPLAZADO;
    private static final long NOVEDAD_MANTENIDO    = EstadoNovedad.MANTENIDO;

    // Tipos de comprobante
    private static final String TIPO_FACTURA       = "Factura";
    private static final String TIPO_NOTA_CREDITO  = "Nota de Crédito";
    private static final String TIPO_NOTA_DEBITO   = "Nota de Débito";
    private static final String TIPO_LIQUIDACION   = "Liquidación de compra";
    private static final String TIPO_RETENCION     = "Comprobante de Retención";
    private static final String TIPO_RETENCION_V2  = "Comprobante de Retención electrónica versión 2.0";

    // =========================================================
    // FASE 1: Carga del archivo TXT
    // =========================================================
    @Override
    public Map<String, Object> cargarArchivoTxt(String contenidoTxt, String nombreArchivo,
                                                 Long idEmpresa, Long idUsuario, Long idPeriodo) throws Throwable {

        System.out.println("=== INICIO cargarArchivoTxt === archivo: " + nombreArchivo + " periodo=" + idPeriodo);

        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        // Resolver período contable
        Periodo periodo = null;
        if (idPeriodo != null) {
            periodo = em.find(Periodo.class, idPeriodo);
            if (periodo == null)
                throw new Exception("Período contable no encontrado: " + idPeriodo);
        }

        // Obtener RUC del receptor desde el facturador de la empresa
        String rucReceptor = obtenerRucReceptor(idEmpresa);

        // Crear cabecera de carga
        CargaArchivoTxt cabecera = new CargaArchivoTxt();
        cabecera.setEmpresa(empresa);
        cabecera.setUsuario(usuario);
        cabecera.setFechaCarga(LocalDateTime.now());
        cabecera.setNombreArchivo(nombreArchivo);
        cabecera.setEstado(1L);
        cabecera.setPeriodoContable(periodo);
        cabecera = cargaArchivoTxtDaoService.save(cabecera, null);

        long totalRegistros = 0, nuevos = 0, duplicados = 0, novedades = 0, registradosConDiferencias = 0;
        List<Map<String, Object>> detallesResultado = new ArrayList<>();
        // Claves de acceso procesadas en ESTA carga (para detectar desaparecidos)
        java.util.Set<String> clavesEnEstaCarga = new java.util.HashSet<>();
        // Tipos de comprobante presentes en ESTA carga (para filtrar desaparecidos por tipo)
        java.util.Set<String> tiposEnEstaCarga = new java.util.HashSet<>();

        String[] lineas = contenidoTxt.split("\n");

        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty()) continue;
            // Saltar encabezado
            if (linea.startsWith("RUC_EMISOR")) continue;

            String[] cols = linea.split("\t", -1);
            if (cols.length < 11) continue;

            totalRegistros++;

            String rucEmisor                 = cols[0].trim();
            String razonSocial               = cols[1].trim().toUpperCase();
            String tipoComprobante           = cols[2].trim();
            String serie                     = cols[3].trim();
            String claveAcceso               = cols[4].trim();
            String fechaAutorizacionStr      = cols[5].trim();
            String fechaEmisionStr           = cols[6].trim();
            String identificacionReceptor    = cols[7].trim();
            double valorSinImpuestos         = parseDouble(cols[8].trim());
            double iva                       = parseDouble(cols[9].trim());
            double importeTotal              = parseDouble(cols[10].trim());
            String numDocModificado          = cols.length > 11 ? cols[11].trim() : "";

            LocalDateTime fechaAutorizacion = parseFechaHora(fechaAutorizacionStr);
            LocalDateTime fechaEmision      = parseFechaHora(fechaEmisionStr);

            Map<String, Object> r = new HashMap<>();
            r.put("linea", i + 1);
            r.put("serie", serie);
            r.put("claveAcceso", claveAcceso);

            // Validar receptor
            if (rucReceptor != null && !identificacionReceptor.equals(rucReceptor)) {
                r.put("resultado", (long) ResultadoCargaTxt.IGNORADO);
                detallesResultado.add(r);                totalRegistros--;
                continue;
            }

            // Registrar clave como procesada en esta carga
            clavesEnEstaCarga.add(claveAcceso);
            tiposEnEstaCarga.add(tipoComprobante);

            // ── Buscar DocumentoCxp único por claveAcceso ──
            DocumentoCxp doc = buscarDocumentoPorClaveAcceso(claveAcceso);

            long resultadoLinea;
            String observacionLinea = null;

            if (doc == null) {
                // NUEVO: crear el documento maestro
                doc = new DocumentoCxp();
                doc.setEmpresa(empresa);
                doc.setRucEmisor(rucEmisor);
                doc.setRazonSocialEmisor(razonSocial);
                doc.setTipoComprobante(tipoComprobante);
                doc.setSerieComprobante(serie);
                doc.setClaveAcceso(claveAcceso);
                doc.setFechaAutorizacion(fechaAutorizacion);
                doc.setFechaEmision(fechaEmision);
                doc.setIdentificacionReceptor(identificacionReceptor);
                doc.setValorSinImpuestos(valorSinImpuestos);
                doc.setIva(iva);
                doc.setImporteTotal(importeTotal);
                doc.setNumeroDocumentoModificado(numDocModificado);
                doc.setEstadoDocumento(ESTADO_LEIDO);
                // Asignar período contable
                doc.setPeriodoContable(periodo != null ? periodo
                        : resolverPeriodoPorFecha(fechaEmision, idEmpresa));
                doc = documentoCxpDaoService.save(doc, null);
                nuevos++;
                resultadoLinea = (long) ResultadoCargaTxt.NUEVO;
            } else {
                // Ya existe: comparar valores
                // Para retenciones NO se comparan valores porque el TXT del SRI no reporta
                // totales confiables para este tipo de comprobante.
                boolean esRetencion = TIPO_RETENCION.equalsIgnoreCase(tipoComprobante)
                        || TIPO_RETENCION_V2.equalsIgnoreCase(tipoComprobante);

                String diferencias = esRetencion ? ""
                        : detectarDiferencias(doc, valorSinImpuestos, iva, importeTotal,
                                fechaAutorizacion, fechaEmision);

                if (diferencias.isEmpty()) {
                    duplicados++;
                    resultadoLinea = (long) ResultadoCargaTxt.DUPLICADO;
                    observacionLinea = "Documento ya existía sin diferencias.";
                } else if (doc.getEstadoDocumento() == ESTADO_REGISTRADO_BD) {
                    // Documento ya registrado con asiento contable → NO modificar, solo reportar diferencia
                    registradosConDiferencias++;
                    resultadoLinea = (long) ResultadoCargaTxt.REGISTRADO_CON_DIFERENCIAS;
                    observacionLinea = "Documento ya registrado en BD con asiento contable, pero el SRI reporta valores diferentes: " + diferencias;
                    r.put("diferencias", diferencias);
                } else {
                    if (doc.getEstadoDocumento() == ESTADO_LEIDO
                            || doc.getEstadoDocumento() == ESTADO_XML_CARGADO
                            || doc.getEstadoDocumento() == ESTADO_REVERTIDO) {
                        doc.setValorSinImpuestos(valorSinImpuestos);
                        doc.setIva(iva);
                        doc.setImporteTotal(importeTotal);
                        doc.setFechaAutorizacion(fechaAutorizacion);
                        doc.setFechaEmision(fechaEmision);
                        doc.setEstadoDocumento(ESTADO_LEIDO);
                        doc.setNovedad(diferencias);
                        documentoCxpDaoService.save(doc, doc.getId());
                    } else if (doc.getEstadoDocumento() == ESTADO_NOVEDAD) {
                        doc.setNovedad(diferencias);
                        documentoCxpDaoService.save(doc, doc.getId());
                    }
                    novedades++;
                    resultadoLinea = (long) ResultadoCargaTxt.NOVEDAD;
                    r.put("diferencias", diferencias);
                }
            }

            // ── Registrar siempre la línea de esta carga ──
            DetalleCargaTxt linea2 = new DetalleCargaTxt();
            linea2.setCargaTxt(cabecera);
            linea2.setDocumento(doc);
            linea2.setValorSinImpuestosCarga(valorSinImpuestos);
            linea2.setIvaCarga(iva);
            linea2.setImporteTotalCarga(importeTotal);
            linea2.setFechaAutorizacionCarga(fechaAutorizacion);
            linea2.setFechaEmisionCarga(fechaEmision);
            linea2.setResultado(resultadoLinea);
            linea2.setObservacion(observacionLinea);
            detalleCargaTxtDaoService.save(linea2, null);

            r.put("resultado", resultadoLinea);
            r.put("idDocumentoCxp", doc.getId());
            detallesResultado.add(r);
        }

        // Actualizar cabecera con totales
        cabecera.setTotalRegistros(totalRegistros);
        cabecera.setRegistrosNuevos(nuevos);
        cabecera.setRegistrosDuplicados(duplicados);
        cabecera.setRegistrosNovedad(novedades);
        cargaArchivoTxtDaoService.save(cabecera, cabecera.getId());

        // ── Detectar documentos desaparecidos ──────────────────────────────────────
        // Si hay un periodo asociado: buscar documentos activos del mismo periodo que
        // NO aparecen en esta carga y marcarlos como NOVEDAD con motivo "DESAPARECIDO".
        long desaparecidos = 0;
        List<Map<String, Object>> desaparecidosDetalle = new ArrayList<>();
        if (periodo != null) {
            desaparecidos = detectarDocumentosDesaparecidos(
                    idEmpresa, periodo, clavesEnEstaCarga, tiposEnEstaCarga, cabecera, desaparecidosDetalle);
            // Actualizar cabecera con conteo real de novedades (incluye desaparecidos)
            cabecera.setRegistrosNovedad(novedades + desaparecidos);
            cargaArchivoTxtDaoService.save(cabecera, cabecera.getId());
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("idCargaTxt", cabecera.getId());
        resultado.put("nombreArchivo", nombreArchivo);
        resultado.put("totalRegistros", totalRegistros);
        resultado.put("nuevos", nuevos);
        resultado.put("duplicados", duplicados);
        resultado.put("novedades", novedades);
        resultado.put("registradosConDiferencias", registradosConDiferencias);
        resultado.put("desaparecidos", desaparecidos);
        resultado.put("detalles", detallesResultado);
        resultado.put("desaparecidosDetalle", desaparecidosDetalle);

        System.out.println("=== FIN cargarArchivoTxt === nuevos=" + nuevos
                + " duplicados=" + duplicados + " novedades=" + novedades
                + " registradosConDiferencias=" + registradosConDiferencias
                + " desaparecidos=" + desaparecidos);
        return resultado;
    }

    // =========================================================
    // FASE 2: Carga del XML  →  opera sobre DocumentoCxp
    // =========================================================
    // Método puente de compatibilidad — no está en la interfaz (usarlo internamente si es necesario)
    public Map<String, Object> cargarXmlDocumento(Long idDocumentoCxp, String contenidoXml,
                                                   String pathDestino, Long idUsuario) throws Throwable {
        return cargarXmlDocumento(idDocumentoCxp, contenidoXml, pathDestino, idUsuario, null);
    }

    @Override
    public Map<String, Object> cargarXmlDocumento(Long idDocumentoCxp, String contenidoXml,
                                                   String pathDestino, Long idUsuario,
                                                   Boolean esReembolsoBody) throws Throwable {

        System.out.println("=== cargarXmlDocumento idDocumentoCxp=" + idDocumentoCxp
                + " esReembolsoBody=" + esReembolsoBody);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        // ── Validar que el XML corresponde al documento esperado ──
        List<Map<String, Object>> errores = validarXmlContraDocumento(contenidoXml, doc);

        Map<String, Object> resultado = new HashMap<>();
        if (!errores.isEmpty()) {
            resultado.put("valido", false);
            resultado.put("errores", errores);
            resultado.put("documento", doc);
            System.out.println("=== cargarXmlDocumento VALIDACIÓN FALLIDA: " + errores);
            return resultado;
        }

        // ── XML válido: guardar y cambiar estado ──
        doc.setPathXml(pathDestino);
        doc.setFechaCargaXml(LocalDateTime.now());
        doc.setUsuarioCargaXml(em.find(Usuario.class, idUsuario));

        // §6.1 — Leer y persistir esReembolso en DCXP también en cargarXml.
        // El frontend usa el flujo de dos pasos: cargarXml (paso 1) + registrarBD (paso 2).
        // registrarBD lee esReembolso del documento ya persistido, no del request.
        // Por eso es CRÍTICO persistirlo aquí para que registrarBD lo encuentre.
        boolean marcadoBody = Boolean.TRUE.equals(esReembolsoBody);
        boolean yaMarcado   = doc.getEsReembolso() != null && doc.getEsReembolso() == 1L;
        boolean xmlTieneReembolsos = contenidoXml != null && contenidoXml.contains("<reembolsoDetalle>");
        if (marcadoBody || yaMarcado || xmlTieneReembolsos) {
            doc.setEsReembolso(1L);
        } else if (doc.getEsReembolso() == null) {
            doc.setEsReembolso(0L);
        }
        // OJO: si doc.getEsReembolso() ya era 1 (marcado antes desde la bandeja), NO pisarlo a 0.

        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
        DocumentoCxp docActualizado = documentoCxpDaoService.save(doc, doc.getId());

        resultado.put("valido", true);
        resultado.put("documento", docActualizado);
        if (Boolean.valueOf(1L == (docActualizado.getEsReembolso() != null ? docActualizado.getEsReembolso() : 0L))) {
            resultado.put("esReembolso", true);
        }
        return resultado;
    }

    /**
     * Valida que el contenido del XML coincida con los datos esperados del DocumentoCxp.
     * Compara: claveAcceso, rucEmisor, razonSocialEmisor, importeTotal, valorSinImpuestos,
     *          iva, serieComprobante, fechaEmision.
     *
     * @return Lista de mensajes de error (vacía si todo es correcto)
     */
    private List<Map<String, Object>> validarXmlContraDocumento(String contenidoXml, DocumentoCxp doc) {
        List<Map<String, Object>> errores = new ArrayList<>();
        try {
            Document xmlDoc = parsearXmlComprobante(contenidoXml);

            // ── 1. Clave de acceso ──
            String claveXml = getXmlValueOuter(contenidoXml, "claveAccesoConsultada");
            if (claveXml.isEmpty()) claveXml = getXmlValueOuter(contenidoXml, "claveAcceso");
            if (claveXml.isEmpty()) claveXml = getXmlValue(xmlDoc, "claveAcceso");
            if (!claveXml.isEmpty() && !claveXml.equals(doc.getClaveAcceso())) {
                errores.add(errorDiff("claveAcceso", doc.getClaveAcceso(), claveXml));
            }

            // ── 2. RUC del emisor ──
            String rucXml = getXmlValue(xmlDoc, "ruc");
            if (rucXml.isEmpty()) rucXml = getXmlValue(xmlDoc, "rucEmisor");
            if (!rucXml.isEmpty() && !rucXml.equals(doc.getRucEmisor())) {
                errores.add(errorDiff("rucEmisor", doc.getRucEmisor(), rucXml));
            }

            // ── 3. Razón social del emisor ──
            // Solo se registra en log como advertencia: NO bloquea el proceso.
            // El orden de apellidos/nombres puede diferir entre el TXT del SRI y el XML (ej: persona natural).
            // La validación de identidad se garantiza con el RUC/identificación (campo 2 arriba).
            String razonXml = getXmlValue(xmlDoc, "razonSocial").toUpperCase();
            if (!razonXml.isEmpty() && doc.getRazonSocialEmisor() != null) {
                try {
                    int similitud = titularDaoService.calcularSimilitudNombre(razonXml, doc.getRazonSocialEmisor());
                    System.out.println("INFO razonSocial (similitud=" + similitud + "%): XML=[" + razonXml + "] DOC=[" + doc.getRazonSocialEmisor() + "]"
                        + (similitud < 92 ? " -- ADVERTENCIA: nombres difieren pero se continúa (identidad validada por RUC)" : ""));
                } catch (Throwable ex) {
                    System.out.println("WARN: No se pudo calcular similitud razonSocial: " + ex.getMessage());
                }
            }

            // ── 4. Número de serie / comprobante ──
            String estab      = getXmlValue(xmlDoc, "estab");
            String ptoEmi     = getXmlValue(xmlDoc, "ptoEmi");
            String secuencial = getXmlValue(xmlDoc, "secuencial");
            if (!estab.isEmpty() && !ptoEmi.isEmpty() && !secuencial.isEmpty()) {
                String serieXml = estab + "-" + ptoEmi + "-" + secuencial;
                if (doc.getSerieComprobante() != null
                        && !serieXml.equals(doc.getSerieComprobante())) {
                    errores.add(errorDiff("serieComprobante", doc.getSerieComprobante(), serieXml));
                }
            }

            // ── 5. Valor sin impuestos ──
            // Para retenciones el TXT del SRI reporta 0.0 en este campo → no comparar
            boolean esRetencionXml = TIPO_RETENCION.equalsIgnoreCase(doc.getTipoComprobante())
                    || TIPO_RETENCION_V2.equalsIgnoreCase(doc.getTipoComprobante());

            if (!esRetencionXml) {
                // ── Solo se valida importeTotal ──
                String importeTotalStr = getXmlValue(xmlDoc, "importeTotal");
                if (!importeTotalStr.isEmpty()) {
                    double importeTotalXml = parseDouble(importeTotalStr);
                    if (doc.getImporteTotal() != null
                            && Math.abs(importeTotalXml - doc.getImporteTotal()) > 0.01) {
                        errores.add(errorDiff("importeTotal",
                                String.valueOf(doc.getImporteTotal()),
                                String.valueOf(importeTotalXml)));
                    }
                }
            } // fin if (!esRetencionXml)

        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("campo", "xml");
            err.put("esperado", "XML válido");
            err.put("enXml", "Error al parsear: " + e.getMessage());
            errores.add(err);
        }
        return errores;
    }

    private Map<String, Object> errorDiff(String campo, String esperado, String enXml) {
        Map<String, Object> err = new HashMap<>();
        err.put("campo", campo);
        err.put("esperado", esperado);
        err.put("enXml", enXml);
        return err;
    }

    // =========================================================
    // FASE 2+3 UNIFICADA: Validar XML + Registrar en BD en un paso
    // =========================================================
    @Override
    public Map<String, Object> cargarXmlYRegistrar(Long idDocumentoCxp, String contenidoXml,
                                                    String pathDestino, Long idEmpresa,
                                                    Long idUsuario) throws Throwable {

        System.out.println("=== cargarXmlYRegistrar idDocumentoCxp=" + idDocumentoCxp);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        // ── 1. Validar XML contra el documento esperado ──
        List<Map<String, Object>> errores = validarXmlContraDocumento(contenidoXml, doc);
        if (!errores.isEmpty()) {
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("valido", false);
            resultado.put("errores", errores);
            resultado.put("documento", doc);
            System.out.println("=== cargarXmlYRegistrar VALIDACIÓN FALLIDA: " + errores);
            return resultado;
        }

        // ── 2. Guardar path del XML y cambiar estado a XML_CARGADO ──
        doc.setPathXml(pathDestino);
        doc.setFechaCargaXml(LocalDateTime.now());
        doc.setUsuarioCargaXml(em.find(Usuario.class, idUsuario));
        // Flag de reembolso: se respeta si ya fue marcado por el usuario (DCXPESRM=1),
        // y se autodetecta desde el contenido del XML.
        boolean marcadoReembolso = doc.getEsReembolso() != null && doc.getEsReembolso() == 1L;
        boolean xmlTieneReembolsos = contenidoXml != null && contenidoXml.contains("<reembolsoDetalle>");
        if (marcadoReembolso || xmlTieneReembolsos) {
            doc.setEsReembolso(1L);
        } else if (doc.getEsReembolso() == null) {
            doc.setEsReembolso(0L);
        }
        // OJO: si doc.getEsReembolso() ya era 1 (marcado antes desde la bandeja), NO pisarlo a 0.
        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
        doc = documentoCxpDaoService.save(doc, doc.getId());

        // ── 3. Registrar en tablas CXP ──
        String tipo = doc.getTipoComprobante();
        Map<String, Object> resultadoBD;

        try {
            if (TIPO_FACTURA.equalsIgnoreCase(tipo)) {
                resultadoBD = registrarFacturaCompra(doc, contenidoXml, idEmpresa, idUsuario);
            } else if (TIPO_NOTA_CREDITO.equalsIgnoreCase(tipo)) {
                resultadoBD = registrarNotaCreditoCompra(doc, contenidoXml, idEmpresa, idUsuario);
            } else if (TIPO_NOTA_DEBITO.equalsIgnoreCase(tipo)) {
                resultadoBD = registrarNotaDebitoCompra(doc, contenidoXml, idEmpresa, idUsuario);
            } else if (TIPO_LIQUIDACION.equalsIgnoreCase(tipo)) {
                resultadoBD = registrarLiquidacionCompraCompra(doc, contenidoXml, idEmpresa, idUsuario);
            } else if (TIPO_RETENCION.equalsIgnoreCase(tipo) || TIPO_RETENCION_V2.equalsIgnoreCase(tipo)) {
                // Todas las retenciones recibidas se registran en RetencionCompraV2,
                // sin importar la version del comprobante: el parser tolera los dos
                // esquemas del SRI (ver obtenerDetallesRetencion / getValorDocSustento).
                // PGS.RTCM queda solo para consultar lo cargado antes de este cambio.
                resultadoBD = registrarRetencionCompraV2(doc, contenidoXml, idEmpresa, idUsuario);
            } else {
                throw new Exception("Tipo de comprobante no soportado: " + tipo);
            }

            // Si el proveedor no fue encontrado → marcar ERROR y retornar sin registrar
            if (resultadoBD.containsKey("error")) {
                doc.setEstadoDocumento(ESTADO_ERROR);
                doc.setObservacion(resultadoBD.get("mensaje").toString());
                documentoCxpDaoService.save(doc, doc.getId());
                resultadoBD.put("valido", true);
                return resultadoBD;
            }

            // Si hay productos pendientes de clasificación → NO grabar en BD, esperar al usuario
            if (Boolean.TRUE.equals(resultadoBD.get("pendienteClasificacion"))) {
                doc.setEstadoDocumento(ESTADO_XML_CARGADO); // se mantiene en XML_CARGADO, no avanza
                doc.setObservacion("Productos pendientes de clasificación: " + resultadoBD.get("productosPendientes"));
                documentoCxpDaoService.save(doc, doc.getId());
                resultadoBD.put("valido", false);
                return resultadoBD;
            }

            doc.setIdDocumentoBD((Long) resultadoBD.get("idDocumentoBD"));
            doc.setTipoTablaDestino((String) resultadoBD.get("tipoTablaDestino"));
            doc.setFechaRegistroBD(LocalDateTime.now());
            doc.setUsuarioRegistroBD(em.find(Usuario.class, idUsuario));
            doc.setEstadoDocumento(ESTADO_REGISTRADO_BD);
            // T3 (ATS): no bloquea el registro, solo lo anota en la observación del documento
            // -mismo mecanismo de "pendiente por documento" que ya usa este proceso, pero sin
            // frenar el registro como sí hace pendienteClasificacion arriba-.
            if (Boolean.TRUE.equals(resultadoBD.get("sustentoTributarioPendiente"))) {
                doc.setObservacion("Sustento tributario (codSustento) sin resolver: "
                        + resultadoBD.get("advertenciaSustentoTributario"));
            }
            documentoCxpDaoService.save(doc, doc.getId());

            // ── Generar asiento contable (CXP) ────────────────────────────────
            // Solo si el registro fue exitoso (ESTADO_REGISTRADO_BD).
            // Si el asiento falla NO revertimos el registro — se advierte en el resultado.
            generarAsientoCxp(doc, resultadoBD, idEmpresa);

        } catch (Exception e) {
            // El marcado NO puede hacerse sobre `doc` en esta transacción: al
            // re-lanzar, el rollback se lleva también el estado 4 y el documento
            // queda como si nunca hubiera fallado. Va en un bean aparte con
            // REQUIRES_NEW, que confirma el marcado por su cuenta.
            // Ver docs/logica-negocio/cxp/PLAN-CARGA-AUTOMATICA-SRI.md §7, fase 0.1.
            //
            // El try de aquí no es redundante con el que ya tiene marcarError: lo
            // que ese bean no puede atrapar es el cierre de su propia transacción,
            // que el contenedor hace después de que el método retorna. Lo que el
            // usuario tiene que ver es `e`, no un fallo del marcado.
            try {
                marcadoErrorDocumentoService.marcarError(idDocumentoCxp,
                        "Error al registrar en BD: " + e.getMessage());
            } catch (Throwable t) {
                System.err.println("⚠ Falló el marcado en ERROR del DocumentoCxp "
                        + idDocumentoCxp + ": " + t.getMessage());
            }
            throw e;
        }

        resultadoBD.put("valido", true);
        return resultadoBD;
    }

    /**
     * Obtiene el grupo "PENDIENTE DE CLASIFICAR" de la empresa.
     * Si no existe, lo crea automáticamente.
     */
    private GrupoProductoPago obtenerOCrearGrupoPendienteClasificar(Long idEmpresa) {
        try {
            @SuppressWarnings("unchecked")
            List<GrupoProductoPago> lista = em.createQuery(
                    "select g from GrupoProductoPago g " +
                    "where g.rubroTipoGrupoH = :tipo and g.empresa.codigo = :idEmpresa")
                    .setParameter("tipo", (long) TipoGrupoProductos.POR_CLASIFICAR)
                    .setParameter("idEmpresa", idEmpresa)
                    .setMaxResults(1).getResultList();
            if (!lista.isEmpty()) return lista.get(0);

            // No existe → crear automáticamente
            GrupoProductoPago grupo = new GrupoProductoPago();
            grupo.setNombre("POR CLASIFICAR");
            grupo.setRubroTipoGrupoH((long) TipoGrupoProductos.POR_CLASIFICAR);
            grupo.setEmpresa(em.find(Empresa.class, idEmpresa));
            grupo.setEstado(1L);
            return grupoProductoPagoDaoService.save(grupo, null);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "No se pudo obtener/crear el grupo POR CLASIFICAR: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un producto por nombre en la empresa. Si no existe lo crea en el grupo
     * "PENDIENTE DE CLASIFICAR" para no interrumpir el flujo de registro.
     */
    private ProductoPago obtenerOAutoCrearProducto(String nombre, String codigo, String codigoAux,
                                                    double precioUnitario, Long idEmpresa) {
        ProductoPago existente = buscarProductoPorNombre(nombre, idEmpresa);
        if (existente != null) {
            // Si el producto existe pero no tiene grupo asignado, asignarlo automáticamente
            if (existente.getGrupoProducto() == null) {
                GrupoProductoPago grupoPendiente = obtenerOCrearGrupoPendienteClasificar(idEmpresa);
                existente.setGrupoProducto(grupoPendiente);
                try {
                    existente = productoPagoDaoService.save(existente, existente.getId());
                    System.out.println("⚠ Producto '" + nombre + "' (ID=" + existente.getId()
                            + ") no tenía grupo asignado → asignado a POR CLASIFICAR automáticamente.");
                } catch (Throwable e) {
                    throw new RuntimeException(
                            "Error al asignar grupo al producto '" + nombre + "': " + e.getMessage(), e);
                }
            }
            return existente;
        }

        GrupoProductoPago grupoPendiente = obtenerOCrearGrupoPendienteClasificar(idEmpresa);
        ProductoPago nuevo = new ProductoPago();
        nuevo.setEmpresa(em.find(Empresa.class, idEmpresa));
        nuevo.setGrupoProducto(grupoPendiente);
        nuevo.setNombre(nombre);
        nuevo.setCodigo(codigo == null || codigo.isEmpty() ? null : codigo);
        nuevo.setCodigoAux(codigoAux == null || codigoAux.isEmpty() ? null : codigoAux);
        nuevo.setPrecioUnitario(precioUnitario);
        try {
            return productoPagoDaoService.save(nuevo, null);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "Error al auto-crear producto '" + nombre + "': " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> obtenerProductosPendientesDeClasificar(Long idFacturaCompra) throws Throwable {
        // Si la factura es reembolso, los pendientes se calculan desde RMBF, no desde DFCC
        @SuppressWarnings("unchecked")
        List<Long> esReembolsoList = em.createQuery(
                "select f.esReembolso from FacturaCompra f where f.id = :id")
                .setParameter("id", idFacturaCompra).setMaxResults(1).getResultList();
        boolean esReembolso = !esReembolsoList.isEmpty()
                && esReembolsoList.get(0) != null && esReembolsoList.get(0) == 1L;

        if (esReembolso) {
            @SuppressWarnings("unchecked")
            List<String> pendientes = em.createQuery(
                    "select p.nombre from ReembolsoFacturaCompra r, ProductoPago p, GrupoProductoPago g " +
                    "where p.id = r.producto and g.codigo = p.grupoProducto.codigo " +
                    "and r.factura.id = :idFactura and r.estado = 1 " +
                    "and g.rubroTipoGrupoH = :tipo")
                    .setParameter("idFactura", idFacturaCompra)
                    .setParameter("tipo", (long) TipoGrupoProductos.POR_CLASIFICAR)
                    .getResultList();
            return pendientes;
        }
        @SuppressWarnings("unchecked")
        List<String> pendientes = em.createQuery(
                "select df.descripcion from DetalleFacturaCompra df, ProductoPago p, GrupoProductoPago g " +
                "where p.id = df.producto and g.codigo = p.grupoProducto.codigo " +
                "and df.factura.id = :idFactura " +
                "and g.rubroTipoGrupoH = :tipo")
                .setParameter("idFactura", idFacturaCompra)
                .setParameter("tipo", (long) TipoGrupoProductos.POR_CLASIFICAR)
                .getResultList();
        return pendientes;
    }

    // =========================================================
    // FASE 3: Registro en BD desde XML  →  opera sobre DocumentoCxp
    // =========================================================
    @Override
    public Map<String, Object> registrarDocumentoBD(Long idDocumentoCxp,
                                                     Long idEmpresa, Long idUsuario) throws Throwable {

        System.out.println("=== registrarDocumentoBD idDocumentoCxp=" + idDocumentoCxp);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        if (doc.getEstadoDocumento() == null || doc.getEstadoDocumento() != ESTADO_XML_CARGADO)
            throw new Exception("El documento debe tener estado XML_CARGADO (2). Estado actual: "
                    + doc.getEstadoDocumento());

        // Una factura de reembolso sin sustentos vuelve a estado 2 con la FCTC ya
        // creada (ver generarAsientoCxp). Sin esta guarda, volver a pulsar
        // "registrar" grabaria una segunda factura con su detalle. El documento se
        // completa ingresando los sustentos y contabilizando, no re-registrando.
        if (registroBDVigente(doc))
            throw new com.saa.basico.util.IncomeException("El documento ya esta registrado en "
                    + doc.getTipoTablaDestino() + " con id=" + doc.getIdDocumentoBD()
                    + ". Si es una factura de reembolso pendiente, ingrese los documentos sustento "
                    + "y contabilicela; si desea volver a registrarla, reviertala primero.");

        String xmlContent = leerArchivoXml(doc);
        String tipo = doc.getTipoComprobante();
        Map<String, Object> resultado;

        try {
            if (TIPO_FACTURA.equalsIgnoreCase(tipo)) {
                resultado = registrarFacturaCompra(doc, xmlContent, idEmpresa, idUsuario);
            } else if (TIPO_NOTA_CREDITO.equalsIgnoreCase(tipo)) {
                resultado = registrarNotaCreditoCompra(doc, xmlContent, idEmpresa, idUsuario);
            } else if (TIPO_NOTA_DEBITO.equalsIgnoreCase(tipo)) {
                resultado = registrarNotaDebitoCompra(doc, xmlContent, idEmpresa, idUsuario);
            } else if (TIPO_LIQUIDACION.equalsIgnoreCase(tipo)) {
                resultado = registrarLiquidacionCompraCompra(doc, xmlContent, idEmpresa, idUsuario);
            } else if (TIPO_RETENCION.equalsIgnoreCase(tipo) || TIPO_RETENCION_V2.equalsIgnoreCase(tipo)) {
                // Ver nota en el despacho de procesarDocumento: todas las
                // retenciones recibidas van a RetencionCompraV2.
                resultado = registrarRetencionCompraV2(doc, xmlContent, idEmpresa, idUsuario);
            } else {
                throw new Exception("Tipo de comprobante no soportado: " + tipo);
            }

            // Si hay productos pendientes de clasificación → NO grabar en BD, esperar al usuario
            if (Boolean.TRUE.equals(resultado.get("pendienteClasificacion"))) {
                doc.setEstadoDocumento(ESTADO_XML_CARGADO); // se mantiene en XML_CARGADO
                doc.setObservacion("Productos pendientes de clasificación: " + resultado.get("productosPendientes"));
                documentoCxpDaoService.save(doc, doc.getId());
                return resultado;
            }

            doc.setIdDocumentoBD((Long) resultado.get("idDocumentoBD"));
            doc.setTipoTablaDestino((String) resultado.get("tipoTablaDestino"));
            doc.setFechaRegistroBD(LocalDateTime.now());
            doc.setUsuarioRegistroBD(em.find(Usuario.class, idUsuario));
            doc.setEstadoDocumento(ESTADO_REGISTRADO_BD);
            doc.setObservacion(null); // limpiar cualquier observación previa (ej: "Productos pendientes de clasificación")
            // T3 (ATS): igual que en procesarDocumento, no bloquea, solo anota.
            if (Boolean.TRUE.equals(resultado.get("sustentoTributarioPendiente"))) {
                doc.setObservacion("Sustento tributario (codSustento) sin resolver: "
                        + resultado.get("advertenciaSustentoTributario"));
            }
            documentoCxpDaoService.save(doc, doc.getId());

            // ── Generar asiento contable (CXP) ────────────────────────────────
            // Solo si el registro fue exitoso (ESTADO_REGISTRADO_BD).
            // Si el asiento falla NO revertimos el registro — se advierte en el resultado.
            generarAsientoCxp(doc, resultado, idEmpresa);

        } catch (Exception e) {
            // El marcado NO puede hacerse sobre `doc` en esta transacción: al
            // re-lanzar, el rollback se lleva también el estado 4 y el documento
            // queda como si nunca hubiera fallado. Va en un bean aparte con
            // REQUIRES_NEW, que confirma el marcado por su cuenta.
            // Ver docs/logica-negocio/cxp/PLAN-CARGA-AUTOMATICA-SRI.md §7, fase 0.1.
            //
            // El try de aquí no es redundante con el que ya tiene marcarError: lo
            // que ese bean no puede atrapar es el cierre de su propia transacción,
            // que el contenedor hace después de que el método retorna. Lo que el
            // usuario tiene que ver es `e`, no un fallo del marcado.
            try {
                marcadoErrorDocumentoService.marcarError(idDocumentoCxp,
                        "Error al registrar en BD: " + e.getMessage());
            } catch (Throwable t) {
                System.err.println("⚠ Falló el marcado en ERROR del DocumentoCxp "
                        + idDocumentoCxp + ": " + t.getMessage());
            }
            throw e;
        }

        return resultado;
    }

    // =========================================================
    // FASE 4: Resolver novedad  →  opera sobre DocumentoCxp
    // =========================================================
    // Método puente de compatibilidad — no está en la interfaz
    public Map<String, Object> resolverNovedad(Long idDocumentoCxp, Integer accion,
                                                String contenidoXml, String pathDestino,
                                                Long idUsuario) throws Throwable {
        return resolverNovedad(idDocumentoCxp, accion, contenidoXml, pathDestino, idUsuario, null);
    }

    @Override
    public Map<String, Object> resolverNovedad(Long idDocumentoCxp, Integer accion,
                                                String contenidoXml, String pathDestino,
                                                Long idUsuario, Boolean esReembolsoBody) throws Throwable {

        System.out.println("=== resolverNovedad idDocumentoCxp=" + idDocumentoCxp
                + " accion=" + accion + " esReembolsoBody=" + esReembolsoBody);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        if (doc.getEstadoDocumento() == null || doc.getEstadoDocumento() != ESTADO_NOVEDAD)
            throw new Exception("El documento debe tener estado NOVEDAD (5). Estado actual: "
                    + doc.getEstadoDocumento());

        Map<String, Object> resultado = new HashMap<>();

        if (AccionNovedad.REEMPLAZAR == accion) {
            if (doc.getIdDocumentoBD() != null) {
                revertirRegistrosBD(doc);
            }
            doc.setPathXml(pathDestino);
            doc.setFechaCargaXml(LocalDateTime.now());
            doc.setUsuarioCargaXml(em.find(Usuario.class, idUsuario));
            doc.setEstadoDocumento(ESTADO_XML_CARGADO);
            doc.setEstadoNovedad(NOVEDAD_REEMPLAZADO);

            // §Novedad 2 — si el frontend envía esReembolso marcado, setearlo ANTES de re-registrar.
            // Usar el mismo helper leerFlagReembolso; nunca pisar a 0 un flag ya en 1.
            if (Boolean.TRUE.equals(esReembolsoBody)) {
                doc.setEsReembolso(1L);
            } else if (doc.getEsReembolso() == null) {
                doc.setEsReembolso(0L);
            }
            // Si esReembolsoBody es null o false y el doc ya tenía 1 → no pisar

            documentoCxpDaoService.save(doc, doc.getId());

            Long idEmpresa = obtenerEmpresaPorReceptor(doc.getIdentificacionReceptor());
            resultado = registrarDocumentoBD(idDocumentoCxp, idEmpresa, idUsuario);
            resultado.put("accion", AccionNovedad.REEMPLAZAR);

        } else if (AccionNovedad.MANTENER == accion) {
            doc.setEstadoNovedad(NOVEDAD_MANTENIDO);
            doc.setObservacion("Usuario decidió mantener el documento previo.");
            documentoCxpDaoService.save(doc, doc.getId());
            resultado.put("accion", AccionNovedad.MANTENER);
            resultado.put("mensaje", "Se mantiene el documento sin cambios.");
        } else {
            throw new Exception("Acción no válida. Use "
                    + AccionNovedad.REEMPLAZAR + " (REEMPLAZAR) o "
                    + AccionNovedad.MANTENER + " (MANTENER).");
        }

        return resultado;
    }

    // =========================================================
    // FASE 5: Revertir documento  →  opera sobre DocumentoCxp
    // =========================================================
    @Override
    public Map<String, Object> revertirDocumento(Long idDocumentoCxp, Long idUsuario) throws Throwable {

        System.out.println("=== revertirDocumento idDocumentoCxp=" + idDocumentoCxp);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        if (doc.getEstadoDocumento() == null || doc.getEstadoDocumento() != ESTADO_REGISTRADO_BD)
            throw new Exception("Solo se pueden revertir documentos con estado REGISTRADO_BD (3). Estado actual: "
                    + doc.getEstadoDocumento());

        revertirRegistrosBD(doc);

        doc.setEstadoDocumento(ESTADO_REVERTIDO);
        doc.setFechaReversion(LocalDateTime.now());
        doc.setUsuarioReversion(em.find(Usuario.class, idUsuario));
        documentoCxpDaoService.save(doc, doc.getId());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Documento revertido correctamente.");
        resultado.put("idDocumentoCxp", idDocumentoCxp);
        resultado.put("idDocumentoBD", doc.getIdDocumentoBD());
        resultado.put("tipoTablaDestino", doc.getTipoTablaDestino());
        return resultado;
    }

    // =========================================================
    // Consultas
    // =========================================================
    @Override
    public Map<String, Object> obtenerResumenCarga(Long idCargaTxt) throws Throwable {
        CargaArchivoTxt cabecera = cargaArchivoTxtDaoService.selectById(idCargaTxt,
                NombreEntidadesCompra.CARGA_ARCHIVO_TXT);
        if (cabecera == null) throw new Exception("CargaArchivoTxt no encontrada: " + idCargaTxt);

        @SuppressWarnings("unchecked")
        List<DetalleCargaTxt> lineas = em.createQuery(
                "select d from DetalleCargaTxt d where d.cargaTxt.id = :id order by d.id")
                .setParameter("id", idCargaTxt)
                .getResultList();

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("cabecera", cabecera);
        resultado.put("lineas", lineas);
        return resultado;
    }

    @Override
    public DocumentoCxp obtenerDocumentoPorId(Long id) throws Throwable {
        return documentoCxpDaoService.selectById(id, NombreEntidadesCompra.DOCUMENTO_CXP);
    }

    @Override
    public boolean tieneRegistroVigente(Long idDocumentoCxp) throws Throwable {
        System.out.println("=== tieneRegistroVigente idDocumentoCxp=" + idDocumentoCxp);
        if (idDocumentoCxp == null) return false;
        // em.find y no selectById: aquel resuelve con getSingleResult y lanza
        // NoResultException cuando la fila no está, y aquí un documento que no
        // existe es simplemente un "no" — este método no lanza por eso.
        DocumentoCxp doc = em.find(DocumentoCxp.class, idDocumentoCxp);
        if (doc == null) return false;
        return registroBDVigente(doc);
    }

    @Override
    public DetalleCargaTxt obtenerDetallePorId(Long id) throws Throwable {
        return detalleCargaTxtDaoService.selectById(id, NombreEntidadesCompra.DETALLE_CARGA_TXT);
    }

    @Override
    public List<DocumentoCxp> obtenerNovedadesPendientes(Long idEmpresa) throws Throwable {
        @SuppressWarnings("unchecked")
        List<DocumentoCxp> lista = em.createNamedQuery("DocumentoCxpNovedadesPendientes")
                .setParameter("idEmpresa", idEmpresa)
                .getResultList();
        return lista;
    }

    // =========================================================
    // FASE 3b: Crear productos faltantes y registrar
    // =========================================================
    @Override
    public Map<String, Object> crearProductosYRegistrar(Long idDocumentoCxp, Long idEmpresa,
            Long idUsuario, List<Map<String, Object>> productosConGrupo) throws Throwable {

        System.out.println("=== crearProductosYRegistrar idDocumentoCxp=" + idDocumentoCxp);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        Empresa empresa = em.find(Empresa.class, idEmpresa);

        for (Map<String, Object> prod : productosConGrupo) {
            String nombre     = (String) prod.get("nombre");
            String codigo     = prod.get("codigo") != null ? prod.get("codigo").toString() : "";
            String codigoAux  = prod.get("codigoAux") != null ? prod.get("codigoAux").toString() : "";
            double precioUnit = prod.get("precioUnitario") != null
                    ? Double.parseDouble(prod.get("precioUnitario").toString()) : 0.0;
            Long idGrupo      = prod.get("idGrupo") != null
                    ? Long.valueOf(prod.get("idGrupo").toString()) : null;

            if (idGrupo == null)
                throw new Exception("El producto '" + nombre + "' no tiene grupo asignado.");

            GrupoProductoPago grupo = em.find(GrupoProductoPago.class, idGrupo);
            if (grupo == null)
                throw new Exception("GrupoProductoPago con id=" + idGrupo + " no existe.");

            if (buscarProductoPorNombre(nombre, idEmpresa) == null) {
                ProductoPago nuevo = new ProductoPago();
                nuevo.setEmpresa(empresa);
                nuevo.setGrupoProducto(grupo);
                nuevo.setNombre(nombre);
                nuevo.setCodigo(codigo.isEmpty() ? null : codigo);
                nuevo.setCodigoAux(codigoAux.isEmpty() ? null : codigoAux);
                nuevo.setPrecioUnitario(precioUnit);
                productoPagoDaoService.save(nuevo, null);
            }
        }

        return registrarDocumentoBD(idDocumentoCxp, idEmpresa, idUsuario);
    }

    // =========================================================
    // Métodos privados de registro por tipo de comprobante
    // (igual que antes pero reciben DocumentoCxp en lugar de DetalleCargaTxt)
    // =========================================================

    private Map<String, Object> registrarFacturaCompra(DocumentoCxp doc, String xmlContent,
                                                        Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        // ── Detección de reembolso de gastos (SRI ANEXO 5) ───────────────────
        // OJO: <codDocReembolso> existe también DENTRO de cada <reembolsoDetalle>;
        // leerlo desde <infoFactura> específicamente, NO con getXmlValue(xmlDoc, ...)
        // que toma la primera ocurrencia del documento.
        String codDocReembolsoCab = "";
        NodeList infoFacturaList = xmlDoc.getElementsByTagName("infoFactura");
        Element infoFactura = infoFacturaList.getLength() > 0 ? (Element) infoFacturaList.item(0) : null;
        if (infoFactura != null) {
            codDocReembolsoCab = getElementValue(infoFactura, "codDocReembolso");
        }
        NodeList reembolsosXml = xmlDoc.getElementsByTagName("reembolsoDetalle");
        boolean marcadoReembolso = doc.getEsReembolso() != null && doc.getEsReembolso() == 1L;
        boolean esReembolso = marcadoReembolso
                || (codDocReembolsoCab != null && !codDocReembolsoCab.isEmpty())
                || reembolsosXml.getLength() > 0;

        // Traza de la deteccion: sin esto, "la factura quedo sin registros en RMBF"
        // no se puede distinguir de "el XML no traia sustentos" mirando el log.
        System.out.println("registrarFacturaCompra reembolso -> DCXPESRM=" + doc.getEsReembolso()
                + " codDocReembolso(cab)=[" + codDocReembolsoCab + "]"
                + " nodos <reembolsoDetalle>=" + reembolsosXml.getLength()
                + " => esReembolso=" + esReembolso);

        // El XML trae el bloque pero el DOM no lo ve: sintoma de que se esta
        // parseando un contenido distinto al que se cree (archivo viejo en disco,
        // <comprobante> que no corresponde). Es un error, no un caso de negocio.
        if (reembolsosXml.getLength() == 0 && xmlContent != null
                && xmlContent.contains("reembolsoDetalle")) {
            System.out.println("✖ El texto del XML contiene 'reembolsoDetalle' pero el documento "
                    + "parseado no tiene ningun nodo. Revise el archivo apuntado por DCXPPTXM: "
                    + doc.getPathXml());
        }

        // ══════════════════════════════════════════════════════════════════
        // PASO 1 — Acciones automáticas (siempre se ejecutan)
        // ══════════════════════════════════════════════════════════════════
        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        NodeList detallesXml = xmlDoc.getElementsByTagName("detalle");
        List<ProductoPago> productosDetalle = new ArrayList<>();

        for (int i = 0; i < detallesXml.getLength(); i++) {
            Element el = (Element) detallesXml.item(i);
            ProductoPago producto = obtenerOAutoCrearProducto(
                    getElementValue(el, "descripcion"),
                    getElementValue(el, "codigoPrincipal"),
                    getElementValue(el, "codigoAuxiliar"),
                    parseDouble(getElementValue(el, "precioUnitario")),
                    idEmpresa);
            productosDetalle.add(producto);
        }

        // Resolución de productos de los sustentos de reembolso (PASO 1 reembolso)
        List<ProductoPago> productosReembolso = new ArrayList<>();
        if (esReembolso) {
            for (int i = 0; i < reembolsosXml.getLength(); i++) {
                Element el = (Element) reembolsosXml.item(i);
                String idProv = getElementValue(el, "identificacionProveedorReembolso");
                String nombreProd = "REEMBOLSO " + idProv;
                // OJO: el XSD del SRI no define <totalDocReembolso>; el total del
                // sustento solo existe como suma de sus <detalleImpuesto>.
                ProductoPago prod = obtenerOAutoCrearProducto(nombreProd, idProv, null,
                        totalDocumentoReembolso(el), idEmpresa);
                productosReembolso.add(prod);
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones previas al registro en BD
        //   · Titular debe tener cuenta contable de facturación (CxP)
        //   · Cada producto debe estar en un grupo ≠ POR_CLASIFICAR
        //   · Ese grupo debe tener cuenta contable asignada
        // Si alguna validación falla → retornar bloqueantes sin grabar nada.
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();

        // 2a. Cuenta contable del proveedor
        boolean titularTieneCuenta = verificarCuentaContableProveedor(titular.getCodigo(), idEmpresa);
        if (!titularTieneCuenta) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "PROVEEDOR_SIN_CUENTA");
            b.put("detalle", "El proveedor '" + titular.getNombre() + "' (RUC: " + titular.getIdentificacion()
                    + ") no tiene cuenta contable CxP asignada. Configúrela en Contabilidad → Cuentas por Titular.");
            bloqueantes.add(b);
        }

        // 2b. Verificar que existe el TipoAsiento para FACTURA_COMPRA (generación contable)
        boolean generaConta = verificarGeneraConta(idEmpresa);
        if (generaConta
                && !existeTipoAsiento(com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, idEmpresa)) {
            bloqueantes.add(bloqueanteTipoAsiento(
                    com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, "Facturas de Compra"));
        }

        // 2b. Productos en POR_CLASIFICAR o sin cuenta contable en su grupo
        List<String> productosSinClasificar = new ArrayList<>();
        List<String> gruposSinCuenta = new ArrayList<>();

        // Cuando es reembolso: solo los productos de sustentos bloquean;
        // los del detalle normal NO bloquean (no participan del asiento).
        List<ProductoPago> productosParaValidar = esReembolso ? productosReembolso : productosDetalle;

        for (ProductoPago producto : productosParaValidar) {
            GrupoProductoPago grupo = producto.getGrupoProducto();
            if (grupo == null
                    || (grupo.getRubroTipoGrupoH() != null
                        && grupo.getRubroTipoGrupoH() == TipoGrupoProductos.POR_CLASIFICAR)) {
                productosSinClasificar.add(producto.getNombre());
            } else if (grupo.getPlanCuenta() == null) {
                gruposSinCuenta.add("Grupo '" + grupo.getNombre() + "' (producto: '" + producto.getNombre() + "')");
            }
        }

        if (!productosSinClasificar.isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "PRODUCTOS_SIN_CLASIFICAR");
            b.put("detalle", "Los siguientes productos están en el grupo POR CLASIFICAR y deben ser reclasificados: "
                    + productosSinClasificar);
            b.put("productos", productosSinClasificar);
            bloqueantes.add(b);
        }

        if (!gruposSinCuenta.isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "GRUPOS_SIN_CUENTA_CONTABLE");
            b.put("detalle", "Los siguientes grupos de producto no tienen cuenta contable asignada: "
                    + gruposSinCuenta);
            b.put("grupos", gruposSinCuenta);
            bloqueantes.add(b);
        }

        // Si hay bloqueantes → cortar sin grabar nada
        if (!bloqueantes.isEmpty()) {
            System.out.println("⚠ Registro de FacturaCompra detenido. Bloqueantes: " + bloqueantes);
            Map<String, Object> r = new HashMap<>();
            r.put("pendienteClasificacion", true);
            r.put("bloqueantes", bloqueantes);
            // Mantener retrocompatibilidad con el campo productosPendientes
            r.put("productosPendientes", productosSinClasificar);
            r.put("mensaje", "No se puede registrar la factura. Hay " + bloqueantes.size()
                    + " condición(es) bloqueante(s) que deben resolverse primero.");
            return r;
        }

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        String codDoc      = getXmlValue(xmlDoc, "codDoc");
        String estab       = getXmlValue(xmlDoc, "estab");
        String ptoEmi      = getXmlValue(xmlDoc, "ptoEmi");
        String secuencial  = getXmlValue(xmlDoc, "secuencial");
        String ambienteStr = getXmlValue(xmlDoc, "ambiente");
        String fechaEmisionStr = getXmlValue(xmlDoc, "fechaEmision");
        double totalSinImp    = parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos"));
        double totalDescuento = parseDouble(getXmlValue(xmlDoc, "totalDescuento"));
        double importeTotal   = parseDouble(getXmlValue(xmlDoc, "importeTotal"));

        FacturaCompra factura = new FacturaCompra();
        factura.setEmpresa(empresa);
        factura.setTipoComprobante(codDoc.isEmpty() ? "01" : codDoc);
        factura.setClave(doc.getClaveAcceso());
        factura.setNumero(doc.getSerieComprobante());
        factura.setNumEstablecimiento(estab);
        factura.setNumPtoEmision(ptoEmi);
        factura.setSecuencial(secuencial);
        factura.setAmbiente(parseLong(ambienteStr));
        factura.setFecha(parseFechaHora(fechaEmisionStr));
        factura.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        factura.setAutorizacion(numeroAutorizacion);
        factura.setSubtotal(totalSinImp);
        factura.setTotal(importeTotal);
        factura.setDescuento(totalDescuento);
        factura.setTitular(titular);
        factura.setUsuario(usuario);
        factura.setEstado(Long.valueOf(Estado.ACTIVO));
        factura.setEstadoEmision(2L);

        // ── Reembolso de gastos ──────────────────────────────────────────────
        if (esReembolso) {
            factura.setEsReembolso(1L);
            factura.setCodDocReembolso(
                    (codDocReembolsoCab == null || codDocReembolsoCab.isEmpty()) ? "41" : codDocReembolsoCab);
            if (infoFactura != null) {
                factura.setTotalComprobantesReembolso(
                        parseDouble(getElementValue(infoFactura, "totalComprobantesReembolso")));
                factura.setTotalBaseImponibleReembolso(
                        parseDouble(getElementValue(infoFactura, "totalBaseImponibleReembolso")));
                factura.setTotalImpuestoReembolso(
                        parseDouble(getElementValue(infoFactura, "totalImpuestoReembolso")));
            }
        } else {
            factura.setEsReembolso(0L);
        }

        // ── IVA de la cabecera (<totalConImpuestos>) ──────────────────────────
        Double[] ivaCab = leerIvaCabecera(xmlDoc, "totalImpuesto");
        if (ivaCab != null) {
            factura.setvIVA(ivaCab[0]);
            factura.setpIVA(ivaCab[1]);
        }
        factura = facturaCompraDaoService.save(factura, null);

        for (int i = 0; i < detallesXml.getLength(); i++) {
            Element el = (Element) detallesXml.item(i);
            double cantidad    = parseDouble(getElementValue(el, "cantidad"));
            double precioUnit  = parseDouble(getElementValue(el, "precioUnitario"));
            double descuento   = parseDouble(getElementValue(el, "descuento"));
            double precioTotal = parseDouble(getElementValue(el, "precioTotalSinImpuesto"));
            Long porcIVA = null; double valIVA = 0.0; Long codigoIVASRI = null;
            NodeList impDet = el.getElementsByTagName("impuesto");
            if (impDet.getLength() > 0) {
                Element impEl = (Element) impDet.item(0);
                String codigoTipoImp = getElementValue(impEl, "codigo");
                // codigo=2 significa IVA → usar codigoPorcentaje para buscar en TSRI lsri=17
                if ("2".equals(codigoTipoImp)) {
                    codigoIVASRI = parseLong(getElementValue(impEl, "codigoPorcentaje"));
                }
                porcIVA = parseLong(getElementValue(impEl, "tarifa"));
                valIVA  = parseDouble(getElementValue(impEl, "valor"));
            }
            DetalleFacturaCompra df = new DetalleFacturaCompra();
            df.setFactura(factura);
            df.setDescripcion(getElementValue(el, "descripcion"));
            df.setCantidad(cantidad);
            df.setValor(precioUnit);
            df.setSubTotal(precioTotal);
            df.setDescuento(descuento);
            df.setBaseImponible(precioTotal);
            df.setCodigoIVASRI(codigoIVASRI);
            df.setPorcentajeIVA(porcIVA);
            df.setValorIVA(valIVA);
            df.setTotal(precioTotal + valIVA);
            df.setProducto(productosDetalle.get(i).getId());
            df.setEstado(Long.valueOf(Estado.ACTIVO));
            detalleFacturaCompraDaoService.save(df, null);
        }

        // ══════════════════════════════════════════════════════════════════
        // Valores recaudados por cuenta de terceros (bomberos / basura)
        // ══════════════════════════════════════════════════════════════════
        // Las planillas electricas cobran estos rubros por cuenta de un
        // tercero y NO los incluyen en <importeTotal>. Si no se registran,
        // la factura queda por menos de lo que realmente se debe pagar y el
        // saldo del modulo no coincide con el contable.
        //
        // Se agregan como detalles adicionales apuntando al MISMO producto
        // del primer detalle: asi heredan su grupo y, por lo tanto, su
        // cuenta contable (generarAsientoFacturaCompra agrupa el DEBE por
        // GrupoProductoPago y calcula el HABER como la suma de los DEBE,
        // de modo que el asiento sigue cuadrando solo).
        String observacionTerceros = null;
        List<Object[]> valoresTerceros = leerValoresTerceros(xmlDoc);
        if (!valoresTerceros.isEmpty() && !productosDetalle.isEmpty()) {

            Long idProductoBase = productosDetalle.get(0).getId();
            double totalTerceros = 0.0;
            StringBuilder detalleTerceros = new StringBuilder();

            for (Object[] concepto : valoresTerceros) {
                String nombreConcepto = (String) concepto[0];
                double valorConcepto  = (Double) concepto[1];

                DetalleFacturaCompra dt = new DetalleFacturaCompra();
                dt.setFactura(factura);
                dt.setDescripcion(nombreConcepto);
                dt.setCantidad(1.0);
                dt.setValor(valorConcepto);
                dt.setSubTotal(valorConcepto);
                dt.setDescuento(0.0);
                dt.setBaseImponible(valorConcepto);
                // No grava IVA: va al 0% (codigoPorcentaje 0 del SRI)
                dt.setCodigoIVASRI(0L);
                dt.setPorcentajeIVA(0L);
                dt.setValorIVA(0.0);
                dt.setTotal(valorConcepto);
                dt.setProducto(idProductoBase);
                dt.setEstado(Long.valueOf(Estado.ACTIVO));
                detalleFacturaCompraDaoService.save(dt, null);

                totalTerceros += valorConcepto;
                if (detalleTerceros.length() > 0) detalleTerceros.append(", ");
                detalleTerceros.append(nombreConcepto).append(": ")
                               .append(String.format(java.util.Locale.US, "%.2f", valorConcepto));
            }

            // El valor no grava IVA -> suma al subtotal 0% y al total de la
            // factura. El total DEBE crecer: es lo que realmente se le debe
            // al proveedor y lo que el asiento va a registrar en la CxP.
            factura.setSubtotal(nvlDouble(factura.getSubtotal()) + totalTerceros);
            factura.setSubcero(nvlDouble(factura.getSubcero()) + totalTerceros);
            factura.setTotal(nvlDouble(factura.getTotal()) + totalTerceros);
            factura = facturaCompraDaoService.save(factura, factura.getId());

            observacionTerceros = "Se agregaron valores recaudados por terceros no incluidos en el "
                    + "importe total del XML (" + detalleTerceros + "). Total factura: "
                    + String.format(java.util.Locale.US, "%.2f", importeTotal) + " -> "
                    + String.format(java.util.Locale.US, "%.2f", factura.getTotal()) + ".";
            System.out.println("✓ " + observacionTerceros);
        }

        NodeList pagos = xmlDoc.getElementsByTagName("pago");
        for (int i = 0; i < pagos.getLength(); i++) {
            Element el = (Element) pagos.item(i);
            FormaPagoFacturaCompra fp = new FormaPagoFacturaCompra();
            fp.setFactura(factura);
            fp.setFormaPago(getElementValue(el, "formaPago"));
            fp.setValor(parseDouble(getElementValue(el, "total")));
            fp.setPlazo(parseLong(getElementValue(el, "plazo")));
            fp.setUnidadTiempo(getElementValue(el, "unidadTiempo"));
            formaPagoFacturaCompraDaoService.save(fp, null);
        }

        PathFacturaCompra pathFc = new PathFacturaCompra();
        pathFc.setFactura(factura);
        pathFc.setPath(doc.getPathXml());
        pathFc.setAlterno(1L);
        pathFacturaCompraDaoService.save(pathFc, null);

        // ── Grabar sustentos de reembolso desde XML ──────────────────────────
        int reembolsosLeidos = 0;
        if (esReembolso) {
            reembolsosLeidos = grabarReembolsosDesdeXml(xmlDoc, factura, productosReembolso);
        }

        // ── T3 (ATS): resolver codSustento (FCTCCSUS) ────────────────────────
        // Todas las lineas ya estan grabadas (incluidas las de valores de terceros), asi que
        // este es el primer momento en que "el grupo con mayor base imponible" tiene sentido.
        // NO bloquea el lote si no se puede resolver -decision explicita, distinta del
        // bloqueante PROVEEDOR_SIN_CUENTA del PASO 2-: se deja nulo y se marca como pendiente,
        // siguiendo el mismo mecanismo no bloqueante que ya usa "advertenciaReembolso" arriba.
        boolean sustentoPendiente = false;
        try {
            sustentoPendiente = sustentoTributarioService.resolverSiFalta(factura) == null;
        } catch (Throwable e) {
            System.out.println("ATENCION: fallo la resolucion de codSustento de la factura "
                    + factura.getId() + ": " + e.getMessage());
            sustentoPendiente = true;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", factura.getId());
        r.put("tipoTablaDestino", "FACTURA_COMPRA");
        r.put("mensaje", "FacturaCompra registrada correctamente con id=" + factura.getId() + ".");
        r.put("productosPendientes", new ArrayList<>());
        r.put("pendienteClasificacion", false);
        if (observacionTerceros != null) r.put("valoresTerceros", observacionTerceros);
        if (sustentoPendiente) {
            r.put("sustentoTributarioPendiente", true);
            r.put("advertenciaSustentoTributario", "La factura " + factura.getId() + " quedo sin"
                    + " codSustento (Tabla 5 del ATS) resuelto: ninguna de sus lineas pertenece a"
                    + " un grupo de producto con sustento por defecto configurado, o hubo un error"
                    + " al resolverlo. Corrijalo con PUT /rest/fctc/sustento/" + factura.getId()
                    + " antes de generar el ATS.");
        }

        if (esReembolso) {
            r.put("esReembolso", true);
            r.put("reembolsosLeidos", reembolsosLeidos);
            if (reembolsosLeidos == 0) {
                System.out.println("⚠ Factura " + factura.getId() + " marcada como reembolso pero el XML "
                        + "no trae sustentos: PGS.RMBF queda vacia y la factura NO se contabiliza. "
                        + "Los sustentos deben ingresarse a mano (POST /rest/rmbf) y luego "
                        + "recalcularTotalesReembolso + contabilizarReembolso.");
                r.put("reembolsoManualPendiente", true);
                r.put("advertenciaReembolso",
                    "La factura fue marcada como reembolso de gastos pero el XML no contiene el bloque <reembolsos>. "
                    + "Ingrese los documentos sustento desde Gestión de Documentos y luego contabilice.");
            }
        }
        return r;
    }

    /**
     * Devuelve 0.0 si el valor es nulo. Para acumular sobre campos de la
     * factura que pueden venir sin inicializar.
     * @param valor : Valor a evaluar
     * @return      : El valor, o 0.0 si es nulo
     */
    private double nvlDouble(Double valor) {
        return (valor != null) ? valor : 0.0;
    }

    // =========================================================
    // Reembolso de gastos — métodos privados
    // =========================================================

    /**
     * Lee los <reembolsoDetalle> del XML y los graba en PGS.RMBF (uno por documento sustento).
     * Los <detalleImpuesto> se aplanan: codigo=2 (IVA) con tarifa>0 suma a base gravada, con
     * tarifa=0 a base cero; codigo=3 (ICE) suma a valor ICE; otros codigos suman su base a base
     * cero. Si hay varias tarifas gravadas se conserva la del bloque de mayor impuesto (mismo
     * criterio que leerIvaCabecera).
     * @param productosReembolso productos resueltos en el PASO 1, indexados igual que los nodos
     * @return numero de registros grabados
     */
    private int grabarReembolsosDesdeXml(Document xmlDoc, FacturaCompra factura,
            List<ProductoPago> productosReembolso) throws Throwable {
        NodeList reembolsos = xmlDoc.getElementsByTagName("reembolsoDetalle");
        System.out.println("grabarReembolsosDesdeXml factura=" + factura.getId()
                + " sustentos en el XML=" + reembolsos.getLength());
        int grabados = 0;
        for (int i = 0; i < reembolsos.getLength(); i++) {
            Element el = (Element) reembolsos.item(i);
            ReembolsoFacturaCompra r = new ReembolsoFacturaCompra();
            r.setFactura(factura);
            r.setTipoIdentificacionProveedor(getElementValue(el, "tipoIdentificacionProveedorReembolso"));
            r.setIdentificacionProveedor(getElementValue(el, "identificacionProveedorReembolso"));
            r.setCodPaisPago(getElementValue(el, "codPaisPagoProveedorReembolso"));
            r.setTipoProveedor(getElementValue(el, "tipoProveedorReembolso"));
            r.setCodDoc(getElementValue(el, "codDocReembolso"));
            r.setEstablecimiento(getElementValue(el, "estabDocReembolso"));
            r.setPuntoEmision(getElementValue(el, "ptoEmiDocReembolso"));
            r.setSecuencial(getElementValue(el, "secuencialDocReembolso"));
            // El tag oficial es "numeroautorizacionDocReemb" (minuscula, ver XSD);
            // tolerar la variante "numeroAutorizacionDocReemb" de algunos emisores.
            String numAut = getElementValue(el, "numeroautorizacionDocReemb");
            if (numAut == null || numAut.isEmpty()) numAut = getElementValue(el, "numeroAutorizacionDocReemb");
            r.setNumeroAutorizacion(numAut);
            LocalDateTime fe = parseFechaHora(getElementValue(el, "fechaEmisionDocReembolso"));
            r.setFechaEmision(fe != null ? fe.toLocalDate() : null);

            double baseCero = 0.0, baseGravada = 0.0, valorIva = 0.0, valorIce = 0.0;
            double tarifaIva = 0.0, mayorImpuesto = -1.0;
            NodeList imps = el.getElementsByTagName("detalleImpuesto");
            for (int j = 0; j < imps.getLength(); j++) {
                Element impEl = (Element) imps.item(j);
                String codigo = getElementValue(impEl, "codigo");
                double base   = parseDouble(getElementValue(impEl, "baseImponibleReembolso"));
                double imp    = parseDouble(getElementValue(impEl, "impuestoReembolso"));
                double tarifa = parseDouble(getElementValue(impEl, "tarifa"));
                if ("2".equals(codigo)) {           // IVA
                    if (tarifa > 0) {
                        baseGravada += base;
                        valorIva    += imp;
                        if (imp > mayorImpuesto) { mayorImpuesto = imp; tarifaIva = tarifa; }
                    } else {
                        baseCero += base;
                    }
                } else if ("3".equals(codigo)) {    // ICE
                    valorIce += imp;
                } else {
                    baseCero += base;
                }
            }
            r.setBaseImponibleCero(baseCero);
            r.setBaseImponibleGravada(baseGravada);
            r.setTarifaIva(baseGravada > 0 ? tarifaIva : null);
            r.setValorIva(valorIva);
            r.setValorIce(valorIce);
            r.setTotal(baseCero + baseGravada + valorIva + valorIce);
            if (i < productosReembolso.size()) {
                r.setProducto(productosReembolso.get(i).getId());
            }
            r.setOrigen(Long.valueOf(OrigenReembolso.XML));
            r.setEstado(Long.valueOf(Estado.ACTIVO));
            reembolsoFacturaCompraDaoService.save(r, null);
            grabados++;
            System.out.println("  RMBF <- " + r.getIdentificacionProveedor()
                    + " " + r.getEstablecimiento() + "-" + r.getPuntoEmision() + "-" + r.getSecuencial()
                    + " total=" + r.getTotal() + " producto=" + r.getProducto());
        }
        System.out.println("grabarReembolsosDesdeXml grabados=" + grabados);
        return grabados;
    }

    /**
     * Total de un documento sustento de reembolso: la suma de las bases y los
     * impuestos de sus &lt;detalleImpuesto&gt;. El XSD del SRI (ANEXO 5) no
     * declara ningun elemento con el total del sustento, asi que hay que
     * calcularlo.
     * @param el : Elemento &lt;reembolsoDetalle&gt;
     * @return   : Total del documento sustento
     */
    private double totalDocumentoReembolso(Element el) {
        double total = 0.0;
        NodeList imps = el.getElementsByTagName("detalleImpuesto");
        for (int j = 0; j < imps.getLength(); j++) {
            Element impEl = (Element) imps.item(j);
            total += parseDouble(getElementValue(impEl, "baseImponibleReembolso"));
            total += parseDouble(getElementValue(impEl, "impuestoReembolso"));
        }
        return total;
    }

    // =========================================================
    // Reembolso de gastos — métodos de negocio (§6.5)
    // =========================================================

    @Override
    public Map<String, Object> marcarReembolso(Long idDocumentoCxp, boolean esReembolso, Long idUsuario) throws Throwable {
        System.out.println("marcarReembolso idDocumentoCxp=" + idDocumentoCxp + " esReembolso=" + esReembolso);
        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp, NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null) throw new com.saa.basico.util.IncomeException("DocumentoCxp no encontrado: " + idDocumentoCxp);

        Map<String, Object> resultado = new java.util.HashMap<>();

        if (esReembolso) {
            doc.setEsReembolso(1L);
            documentoCxpDaoService.save(doc, doc.getId());
            // Si ya está registrado como FACTURA_COMPRA, cascadear
            if ("FACTURA_COMPRA".equals(doc.getTipoTablaDestino()) && doc.getIdDocumentoBD() != null) {
                FacturaCompra fc = em.find(FacturaCompra.class, doc.getIdDocumentoBD());
                if (fc != null) {
                    // Verificar que no tenga pagos aplicados
                    java.util.List<com.saa.model.cxp.AplicacionPagoCxp> aplic =
                            aplicacionPagoCxpService.consultarPorFactura(doc.getIdDocumentoBD(), true);
                    if (aplic != null && !aplic.isEmpty())
                        throw new com.saa.basico.util.IncomeException(
                                "La factura de compra tiene " + aplic.size()
                                + " pago(s) aplicados. Reverse primero esos pagos antes de marcarla como reembolso.");
                    // Si tiene asiento activo → anularlo
                    anularAsientoDeDocumento("FACTURA_COMPRA", doc.getIdDocumentoBD());
                    fc.setEsReembolso(1L);
                    if (fc.getCodDocReembolso() == null) fc.setCodDocReembolso("41");
                    facturaCompraDaoService.save(fc, fc.getId());
                    // Verificar si debe generar conta
                    boolean generaConta = verificarGeneraConta(doc.getEmpresa().getCodigo());
                    if (generaConta) {
                        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
                        doc.setObservacion("REEMBOLSO: pendiente ingreso de documentos sustento y contabilizacion");
                        documentoCxpDaoService.save(doc, doc.getId());
                    }
                    resultado.put("idFacturaCompra", fc.getId());
                }
            }
            resultado.put("esReembolso", true);
        } else {
            // Desmarcar: verificar que no haya RMBF activos
            if ("FACTURA_COMPRA".equals(doc.getTipoTablaDestino()) && doc.getIdDocumentoBD() != null) {
                long countRmbf = ((Number) em.createQuery(
                        "select count(r) from ReembolsoFacturaCompra r where r.factura.id = :id and r.estado = 1")
                        .setParameter("id", doc.getIdDocumentoBD()).getSingleResult()).longValue();
                if (countRmbf > 0)
                    throw new com.saa.basico.util.IncomeException(
                            "Elimine primero los documentos de reembolso (" + countRmbf + ") antes de desmarcar la factura.");
                FacturaCompra fc = em.find(FacturaCompra.class, doc.getIdDocumentoBD());
                if (fc != null) {
                    fc.setEsReembolso(0L);
                    fc.setCodDocReembolso(null);
                    fc.setTotalComprobantesReembolso(null);
                    fc.setTotalBaseImponibleReembolso(null);
                    fc.setTotalImpuestoReembolso(null);
                    facturaCompraDaoService.save(fc, fc.getId());
                }
            }
            doc.setEsReembolso(0L);
            documentoCxpDaoService.save(doc, doc.getId());
            resultado.put("esReembolso", false);
        }
        resultado.put("idDocumentoCxp", idDocumentoCxp);
        resultado.put("estadoDocumento", doc.getEstadoDocumento());
        return resultado;
    }

    @Override
    public Map<String, Object> contabilizarReembolso(Long idFacturaCompra, Long idEmpresa, Long idUsuario) throws Throwable {
        System.out.println("contabilizarReembolso idFacturaCompra=" + idFacturaCompra + " empresa=" + idEmpresa);
        FacturaCompra fc = em.find(FacturaCompra.class, idFacturaCompra);
        if (fc == null) throw new com.saa.basico.util.IncomeException("FacturaCompra no encontrada: " + idFacturaCompra);

        @SuppressWarnings("unchecked")
        java.util.List<ReembolsoFacturaCompra> reembolsos = em.createNamedQuery(
                "ReembolsoFacturaCompraByFactura", ReembolsoFacturaCompra.class)
                .setParameter("idFactura", idFacturaCompra).getResultList();

        // Precondición 1: al menos un RMBF activo
        if (reembolsos == null || reembolsos.isEmpty()) {
            // §Novedad 3 — usar la misma estructura de bloqueantes del PASO 2
            java.util.List<java.util.Map<String, Object>> bloqueantes = new java.util.ArrayList<>();
            java.util.Map<String, Object> b = new java.util.HashMap<>();
            b.put("tipo", "SIN_DOCUMENTOS_SUSTENTO");
            b.put("detalle", "La factura no tiene documentos sustento registrados en RMBF. "
                    + "Ingrese los documentos sustento antes de contabilizar.");
            b.put("productos", java.util.Collections.emptyList());
            bloqueantes.add(b);
            java.util.Map<String, Object> r = new java.util.HashMap<>();
            r.put("pendienteClasificacion", true);
            r.put("bloqueantes", bloqueantes);
            r.put("productosPendientes", java.util.Collections.emptyList());
            r.put("mensaje", "No se puede contabilizar. Hay 1 condición bloqueante.");
            return r;
        }

        // Precondición 2: todos los productos clasificados y con cuenta contable
        // §Novedad 3 — armar bloqueantes con EXACTAMENTE la misma estructura que el PASO 2
        java.util.List<String> pendientes = new java.util.ArrayList<>();
        java.util.List<String> sinCuenta = new java.util.ArrayList<>();
        for (ReembolsoFacturaCompra r : reembolsos) {
            if (r.getProducto() == null) { pendientes.add("(sin producto)"); continue; }
            ProductoPago p = em.find(ProductoPago.class, r.getProducto());
            if (p == null || p.getGrupoProducto() == null
                    || (p.getGrupoProducto().getRubroTipoGrupoH() != null
                        && p.getGrupoProducto().getRubroTipoGrupoH() == TipoGrupoProductos.POR_CLASIFICAR)) {
                pendientes.add(p != null ? p.getNombre() : "(id=" + r.getProducto() + ")");
            } else if (p.getGrupoProducto().getPlanCuenta() == null) {
                sinCuenta.add("Grupo '" + p.getGrupoProducto().getNombre() + "' (producto: '" + (p.getNombre() != null ? p.getNombre() : "") + "')");
            }
        }

        // Precondición 3: cuadratura
        double sumRmbf = reembolsos.stream().mapToDouble(r -> r.getTotal() != null ? r.getTotal() : 0.0).sum();
        double totalFc = fc.getTotal() != null ? fc.getTotal() : 0.0;
        double diferencia = Math.abs(sumRmbf - totalFc);

        // Armar bloqueantes (misma estructura del PASO 2)
        java.util.List<java.util.Map<String, Object>> bloqueantes = new java.util.ArrayList<>();
        if (!pendientes.isEmpty()) {
            java.util.Map<String, Object> b = new java.util.HashMap<>();
            b.put("tipo", "PRODUCTOS_SIN_CLASIFICAR");
            b.put("detalle", "Los siguientes productos están en el grupo POR CLASIFICAR y deben ser reclasificados: " + pendientes);
            b.put("productos", pendientes);
            bloqueantes.add(b);
        }
        if (!sinCuenta.isEmpty()) {
            java.util.Map<String, Object> b = new java.util.HashMap<>();
            b.put("tipo", "GRUPOS_SIN_CUENTA_CONTABLE");
            b.put("detalle", "Los siguientes grupos de producto no tienen cuenta contable asignada: " + sinCuenta);
            b.put("grupos", sinCuenta);
            bloqueantes.add(b);
        }
        if (diferencia > 0.01) {
            java.util.Map<String, Object> b = new java.util.HashMap<>();
            b.put("tipo", "DESCUADRE_REEMBOLSO");
            b.put("detalle", String.format(
                    "Descuadre de %.2f (sum RMBF=%.2f vs factura.total=%.2f). "
                    + "Ajuste los documentos sustento antes de contabilizar.", diferencia, sumRmbf, totalFc));
            b.put("diferencia", diferencia);
            b.put("sumRmbf", sumRmbf);
            b.put("totalFactura", totalFc);
            bloqueantes.add(b);
        }

        if (!bloqueantes.isEmpty()) {
            System.out.println("⚠ contabilizarReembolso detenido. Bloqueantes: " + bloqueantes);
            java.util.Map<String, Object> r = new java.util.HashMap<>();
            r.put("pendienteClasificacion", true);
            r.put("bloqueantes", bloqueantes);
            r.put("productosPendientes", pendientes);
            r.put("mensaje", "No se puede contabilizar la factura de reembolso. Hay "
                    + bloqueantes.size() + " condición(es) bloqueante(s).");
            return r;
        }
        if (diferencia > 0.01)
            throw new com.saa.basico.util.IncomeException(String.format(
                    "REEMBOLSO: descuadre de %.2f (sum RMBF=%.2f vs factura.total=%.2f). "
                    + "Ajuste los documentos sustento antes de contabilizar.", diferencia, sumRmbf, totalFc));

        // Generar asiento
        java.time.LocalDate fechaDoc = fc.getFecha() != null ? fc.getFecha().toLocalDate() : java.time.LocalDate.now();
        String obs = "Factura reembolso: " + (fc.getNumero() != null ? fc.getNumero() : fc.getClave())
                + " | Proveedor: " + (fc.getTitular() != null ? fc.getTitular().getNombre() : "");
        com.saa.model.cnt.Asiento asiento = asientoContableService.generarAsientoFacturaCompra(
                idFacturaCompra, idEmpresa, com.saa.rubros.TipoAsientos.FACTURAS_COMPRA,
                fechaDoc, obs, "SISTEMA");

        // Actualizar FCTC con referencia al asiento y pasar DCXP a estado 3
        fc.setAsiento(asiento);
        facturaCompraDaoService.save(fc, fc.getId());

        // Buscar DocumentoCxp y actualizarlo
        @SuppressWarnings("unchecked")
        java.util.List<Long> dcxpIds = em.createQuery(
                "select d.id from DocumentoCxp d where d.idDocumentoBD = :id and d.tipoTablaDestino = 'FACTURA_COMPRA'")
                .setParameter("id", idFacturaCompra).getResultList();
        if (!dcxpIds.isEmpty()) {
            DocumentoCxp doc = documentoCxpDaoService.selectById(dcxpIds.get(0), NombreEntidadesCompra.DOCUMENTO_CXP);
            if (doc != null) {
                doc.setEstadoDocumento(ESTADO_REGISTRADO_BD);
                doc.setObservacion(null);
                if (doc.getFechaRegistroBD() == null) doc.setFechaRegistroBD(java.time.LocalDateTime.now());
                documentoCxpDaoService.save(doc, doc.getId());
            }
        }

        Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("idFacturaCompra", idFacturaCompra);
        resultado.put("asiento", asiento != null ? asiento.getCodigo() : null);
        resultado.put("cantidadReembolsos", reembolsos.size());
        resultado.put("diferencia", diferencia);
        resultado.put("cuadra", diferencia <= 0.01);
        return resultado;
    }

    @Override
    public Map<String, Object> recalcularTotalesReembolso(Long idFacturaCompra) throws Throwable {
        System.out.println("recalcularTotalesReembolso idFacturaCompra=" + idFacturaCompra);
        FacturaCompra fc = em.find(FacturaCompra.class, idFacturaCompra);
        if (fc == null) throw new com.saa.basico.util.IncomeException("FacturaCompra no encontrada: " + idFacturaCompra);

        @SuppressWarnings("unchecked")
        java.util.List<ReembolsoFacturaCompra> reembolsos = em.createNamedQuery(
                "ReembolsoFacturaCompraByFactura", ReembolsoFacturaCompra.class)
                .setParameter("idFactura", idFacturaCompra).getResultList();

        double totalComp = 0.0, totalBase = 0.0, totalImp = 0.0;
        for (ReembolsoFacturaCompra r : reembolsos) {
            totalComp += r.getTotal() != null ? r.getTotal() : 0.0;
            totalBase += (r.getBaseImponibleCero() != null ? r.getBaseImponibleCero() : 0.0)
                       + (r.getBaseImponibleGravada() != null ? r.getBaseImponibleGravada() : 0.0);
            totalImp  += (r.getValorIva() != null ? r.getValorIva() : 0.0)
                       + (r.getValorIce() != null ? r.getValorIce() : 0.0);
        }
        fc.setTotalComprobantesReembolso(Math.round(totalComp * 100.0) / 100.0);
        fc.setTotalBaseImponibleReembolso(Math.round(totalBase * 100.0) / 100.0);
        fc.setTotalImpuestoReembolso(Math.round(totalImp * 100.0) / 100.0);
        facturaCompraDaoService.save(fc, fc.getId());

        double diferencia = Math.abs(totalComp - (fc.getTotal() != null ? fc.getTotal() : 0.0));
        Map<String, Object> resultado = new java.util.HashMap<>();
        resultado.put("idFacturaCompra", idFacturaCompra);
        resultado.put("cantidadReembolsos", reembolsos.size());
        resultado.put("totalComprobantesReembolso", fc.getTotalComprobantesReembolso());
        resultado.put("totalBaseImponibleReembolso", fc.getTotalBaseImponibleReembolso());
        resultado.put("totalImpuestoReembolso", fc.getTotalImpuestoReembolso());
        resultado.put("importeTotalFactura", fc.getTotal());
        resultado.put("diferencia", Math.round(diferencia * 100.0) / 100.0);
        resultado.put("cuadra", diferencia <= 0.01);
        return resultado;
    }

    @Override
    public ProductoPago crearProductoPorClasificar(String nombre, String codigo, Long idEmpresa) throws Throwable {
        System.out.println("crearProductoPorClasificar nombre=" + nombre + " codigo=" + codigo + " empresa=" + idEmpresa);
        // Si ya existe un producto con ese código, devolverlo sin crear
        if (codigo != null && !codigo.isEmpty()) {
            @SuppressWarnings("unchecked")
            java.util.List<ProductoPago> existentes = em.createQuery(
                    "select p from ProductoPago p where p.codigo = :codigo and p.empresa.codigo = :emp")
                    .setParameter("codigo", codigo).setParameter("emp", idEmpresa)
                    .setMaxResults(1).getResultList();
            if (!existentes.isEmpty()) return existentes.get(0);
        }
        return obtenerOAutoCrearProducto(nombre, codigo, null, 0.0, idEmpresa);
    }

    /**
     * Lee el IVA declarado en la CABECERA del comprobante.
     *
     * Es el valor que se contabiliza en el asiento (ver
     * AsientoContableServiceImpl.distribuirIvaCabecera), asi que debe quedar
     * completo: el XML trae un bloque de impuesto por cada tarifa y hay que
     * sumarlos todos, no quedarse con el primero. Solo cuentan los de IVA
     * (codigo=2); ICE (3) e IRBPNR (5) van a otras cuentas.
     *
     * El nombre del bloque cambia segun el comprobante: la factura, la nota de
     * credito y la liquidacion usan <totalConImpuestos><totalImpuesto>, la nota
     * de debito usa <impuestos><impuesto>. En la ND no hay <detalle>, asi que
     * buscar "impuesto" en todo el documento no arrastra impuestos de linea.
     *
     * La tarifa que se devuelve es la del bloque de mayor valor. Si el bloque no
     * la trae (esquemas del SRI anteriores a la 1.1.0 solo llevan
     * <codigoPorcentaje>) se deduce del codigo de porcentaje, porque de esa
     * tarifa depende que el asiento encuentre la cuenta de IVA cuando los
     * detalles no desglosan el impuesto.
     *
     * @param xmlDoc       : XML del comprobante ya parseado
     * @param tagImpuesto  : Nombre del bloque de impuesto de la cabecera
     * @return             : Arreglo {valorIVA, tarifa}, o null si la cabecera no
     *                       declara IVA
     */
    private Double[] leerIvaCabecera(Document xmlDoc, String tagImpuesto) {
        NodeList impuestos = xmlDoc.getElementsByTagName(tagImpuesto);
        double valorTotal = 0.0;
        double mayorValor = -1.0;
        Double tarifa     = null;
        boolean hayIva    = false;

        for (int i = 0; i < impuestos.getLength(); i++) {
            Element impEl = (Element) impuestos.item(i);
            String codigoImp = getElementValue(impEl, "codigo");
            // Si el XML no trae <codigo> (esquemas antiguos) se asume IVA
            if (!codigoImp.isEmpty() && !"2".equals(codigoImp)) continue;

            double valorImp = parseDouble(getElementValue(impEl, "valor"));
            hayIva = true;
            valorTotal += valorImp;

            if (valorImp > mayorValor) {
                mayorValor = valorImp;
                String tarifaStr = getElementValue(impEl, "tarifa");
                tarifa = !tarifaStr.isEmpty()
                        ? parseDouble(tarifaStr)
                        : tarifaDesdeCodigoPorcentaje(getElementValue(impEl, "codigoPorcentaje"));
            }
        }

        if (!hayIva) return null;
        return new Double[] { Math.round(valorTotal * 100.0) / 100.0, tarifa };
    }

    /**
     * Deduce la tarifa de IVA a partir del <codigoPorcentaje> del SRI, para los
     * XML cuya cabecera no incluye <tarifa>.
     * @param codigoPorcentaje : Codigo de porcentaje del SRI (tabla 17)
     * @return                 : Tarifa en porcentaje, o null si no se reconoce
     */
    private Double tarifaDesdeCodigoPorcentaje(String codigoPorcentaje) {
        if (codigoPorcentaje == null || codigoPorcentaje.isEmpty()) return null;
        switch (codigoPorcentaje.trim()) {
            case "0": return 0.0;    // IVA 0%
            case "2": return 12.0;   // IVA 12% (historico)
            case "3": return 14.0;   // IVA 14% (historico)
            case "4": return 15.0;   // IVA 15%
            case "5": return 5.0;    // IVA 5%
            case "6": return 0.0;    // No objeto de impuesto
            case "7": return 0.0;    // Exento de IVA
            case "8": return 8.0;    // IVA tarifa especial 8%
            default:  return null;
        }
    }

    /**
     * Devuelve la fecha de emisión del documento ya registrado en su tabla
     * específica. Esa fecha se tomó del XML (&lt;fechaEmision&gt;) al registrarlo,
     * asi que es la fuente correcta para fechar el asiento contable — a
     * diferencia de DocumentoCxp.fechaEmision, que viene de la columna 6 del
     * TXT del SRI y puede faltar o no parsearse.
     *
     * @param tipo     : Tipo de tabla destino (DocumentoCxp.tipoTablaDestino)
     * @param idDocBD  : Id del documento en su tabla especifica
     * @return         : Fecha de emision, o null si no se pudo obtener
     */
    private java.time.LocalDate obtenerFechaDocumento(String tipo, Long idDocBD) {
        if (tipo == null || idDocBD == null) return null;

        String entidad;
        switch (tipo) {
            case "FACTURA_COMPRA":            entidad = "FacturaCompra";           break;
            case "NOTA_CREDITO_COMPRA":       entidad = "NotaCreditoCompra";       break;
            case "NOTA_DEBITO_COMPRA":        entidad = "NotaDebitoCompra";        break;
            case "LIQUIDACION_COMPRA_COMPRA": entidad = "LiquidacionCompraCompra"; break;
            case "RETENCION_COMPRA":          entidad = "RetencionCompra";         break;
            case "RETENCION_COMPRA_V2":       entidad = "RetencionCompraV2";       break;
            default: return null;
        }

        try {
            List<?> resultado = em.createQuery(
                    "select d.fecha from " + entidad + " d where d.id = :id")
                    .setParameter("id", idDocBD)
                    .setMaxResults(1)
                    .getResultList();
            if (!resultado.isEmpty() && resultado.get(0) != null) {
                return ((LocalDateTime) resultado.get(0)).toLocalDate();
            }
        } catch (Exception e) {
            System.err.println("⚠ obtenerFechaDocumento (" + tipo + ", id=" + idDocBD + "): "
                    + e.getMessage());
        }
        return null;
    }

    /**
     * Verifica si el titular tiene cuenta contable CxP (tipoCuenta=1) asignada
     * para la empresa EN SU ROL DE PROVEEDOR.
     * <p>
     * Usa {@code AsientoContableService.existeCuentaConRolEstricto} en vez de
     * llamar directo a {@code PersonaCuentaContableDaoService
     * .selectByTitularRolTipoCuenta}: ese DAO tiene un fallback "sin filtro
     * de rol" (compatibilidad con datos antiguos) que devolvía la cuenta de
     * CLIENTE del titular cuando no encontraba la de PROVEEDOR — el
     * comentario original de este método ya advertía el síntoma
     * ("un titular que sólo es cliente pasaba la validación con la cuenta
     * de cliente"), pero la implementación de entonces seguía llamando al
     * DAO tolerante, así que el fallback igual se disparaba por debajo.
     * Medido contra la base: 61 de 87 titulares con cuenta sólo la tienen
     * bajo rol Proveedor y 24 sólo bajo Cliente — el hueco era real y
     * frecuente, no un caso de borde.
     * <p>
     * No aborta la carga completa si falla: es un bloqueante MÁS del
     * documento (PROVEEDOR_SIN_CUENTA, ver el llamador), que deja ese
     * documento en XML_CARGADO con el motivo y sigue con el resto del lote
     * — mismo patrón que docs/general/CORRECCION_MANEJO_EXCEPCIONES_DAO.md.
     * {@code existeCuentaConRolEstricto} ya captura sus propios errores de
     * BD y devuelve {@code false} sin propagar, así que un timeout aquí
     * tampoco tumba el lote: sólo bloquea este documento con el mismo
     * mensaje que "no tiene cuenta".
     */
    private boolean verificarCuentaContableProveedor(Long codigoTitular, Long idEmpresa) {
        try {
            return asientoContableService.existeCuentaConRolEstricto(
                    codigoTitular, idEmpresa, 1L, RolPersona.PROVEEDOR);
        } catch (Throwable e) {
            System.err.println("⚠ verificarCuentaContableProveedor: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si la empresa tiene generación contable habilitada (Facturador.generaConta=1).
     */
    private boolean verificarGeneraConta(Long idEmpresa) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> lista = em.createQuery(
                    "SELECT f.generaConta FROM Facturador f "
                    + "WHERE f.empresa.codigo = :idEmpresa AND f.estado = 1")
                    .setParameter("idEmpresa", idEmpresa)
                    .setMaxResults(1).getResultList();
            return !lista.isEmpty() && Long.valueOf(1L).equals(lista.get(0));
        } catch (Exception e) {
            System.err.println("⚠ verificarGeneraConta: " + e.getMessage());
            return false;
        }
    }

    /**
     * ¿Existe la plantilla de asiento con ese código alterno para la empresa?
     *
     * <p>
     * <b>Consulta el DAO, no {@code TipoAsientoService.codigoByAlterno}</b>, y el
     * motivo es el de la §11 decisión 18: aquel método <b>lanza</b>
     * {@code IncomeException} cuando no encuentra la plantilla
     * ({@code TipoAsientoServiceImpl:120}) — nunca devuelve {@code null}—, y esa
     * excepción está anotada {@code @ApplicationException(rollback = true)}. Al
     * cruzar la frontera del EJB el contenedor marca <b>esta</b> transacción para
     * rollback antes de entregárnosla, y atraparla no la desmarca: el bloqueante
     * se armaba bien pero el commit fallaba después con
     * {@code EJBTransactionRolledbackException} y el REST devolvía un 500 opaco.
     * </p>
     *
     * <p>
     * El DAO ejecuta exactamente la misma consulta y devuelve lista vacía, así que
     * el veredicto es el mismo sin condenar la transacción. Un fallo real de base
     * se registra y se responde {@code false}: con la base caída da igual, y si
     * fuera un problema puntual el bloqueante avisa en vez de dejar pasar un
     * documento sin plantilla.
     * </p>
     *
     * @param codigoAlterno : Código alterno de la plantilla
     * @param idEmpresa     : Id de la empresa contable
     * @return              : true si la plantilla existe
     */
    private boolean existeTipoAsiento(int codigoAlterno, Long idEmpresa) {
        try {
            List<com.saa.model.cnt.TipoAsiento> tipos =
                    tipoAsientoDaoService.selectByAlterno(codigoAlterno, idEmpresa);
            return tipos != null && !tipos.isEmpty();
        } catch (Throwable e) {
            System.err.println("⚠ existeTipoAsiento (alterno=" + codigoAlterno
                    + ", empresa=" + idEmpresa + "): " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // Bloqueantes comunes a NC, ND y Liquidación
    // =========================================================
    /**
     * Agrega los dos bloqueantes que comparten todos los documentos de compra:
     * el proveedor tiene que tener cuenta contable CxP, y —si la empresa lleva
     * contabilidad— tiene que existir la plantilla de asiento del tipo.
     *
     * <p>
     * Son los mismos que ya aplicaba {@code registrarFacturaCompra}; aquí se
     * extraen para que NC, ND y Liquidación no los escriban tres veces y no
     * puedan divergir entre sí.
     * </p>
     *
     * @param bloqueantes   : Lista donde se acumulan
     * @param titular       : Proveedor emisor del documento
     * @param idEmpresa     : Id de la empresa contable
     * @param codigoAlterno : Código alterno del TipoAsiento que usará este documento
     * @param etiquetaTipo  : Nombre del tipo para el mensaje ("Notas de Crédito de Compra")
     */
    private void agregarBloqueantesComunesCompra(List<Map<String, Object>> bloqueantes,
            Titular titular, Long idEmpresa, int codigoAlterno, String etiquetaTipo) {

        // Cuenta contable CxP del proveedor
        if (titular == null || !verificarCuentaContableProveedor(titular.getCodigo(), idEmpresa)) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "PROVEEDOR_SIN_CUENTA");
            b.put("detalle", "El proveedor '" + (titular != null ? titular.getNombre() : "?")
                    + "' (RUC: " + (titular != null ? titular.getIdentificacion() : "?")
                    + ") no tiene cuenta contable CxP asignada."
                    + " Configúrela en Contabilidad → Cuentas por Titular.");
            bloqueantes.add(b);
        }

        // Plantilla de asiento del tipo. Solo cuando la empresa genera
        // contabilidad: si generaConta no es 1 no se va a emitir ningún asiento,
        // así que exigir la plantilla bloquearía documentos que hoy se registran
        // sin problema.
        if (!verificarGeneraConta(idEmpresa)) return;

        if (!existeTipoAsiento(codigoAlterno, idEmpresa))
            bloqueantes.add(bloqueanteTipoAsiento(codigoAlterno, etiquetaTipo));
    }

    private Map<String, Object> bloqueanteTipoAsiento(int codigoAlterno, String etiquetaTipo) {
        Map<String, Object> b = new HashMap<>();
        b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
        b.put("detalle", "No existe el Tipo de Asiento con código alterno " + codigoAlterno
                + " para " + etiquetaTipo + ". Configúrelo en Contabilidad → Tipos de Asiento.");
        return b;
    }

    /**
     * Bloqueante de la factura de compra que una NC o una ND afecta: tiene que
     * existir, y tiene que ser una sola.
     *
     * <p>
     * <b>Por qué se resuelve por el DAO y no por
     * {@code AplicacionPagoCxpService.resolverFacturaCompraPorNumero}</b>, que es
     * el método que después usa la aplicación de pago: los dos leen exactamente
     * la misma consulta —{@code selectFacturaByNumero}, que compara el número sin
     * guiones, de modo que '001-001-000000123' y '001001000000123' son el mismo
     * documento—, así que no pueden discrepar. Lo que cambia es que el servicio
     * comunica el fallo con una {@code IncomeException}, y esa está anotada
     * {@code @ApplicationException(rollback = true)}: como corre en ESTA
     * transacción, atraparla la dejaría marcada para rollback y el retorno
     * estructurado se perdería junto con el proveedor recién autocreado. El
     * criterio de las tres condiciones es el mismo que aplica aquel método.
     * </p>
     *
     * @param bloqueantes      : Lista donde se acumulan
     * @param numDocModificado : Número de la factura afectada, tal como viene en el XML
     * @param titular          : Proveedor emisor
     * @param idEmpresa        : Id de la empresa contable
     * @param etiquetaDoc      : "nota de crédito" o "nota de débito", para el mensaje
     */
    private void agregarBloqueanteFacturaAfectada(List<Map<String, Object>> bloqueantes,
            String numDocModificado, Titular titular, Long idEmpresa, String etiquetaDoc) {

        String solucion = "Cargue primero la factura de compra afectada. El número se compara"
                + " sin guiones, así que '001-001-000000123' y '001001000000123' son"
                + " equivalentes.";

        if (numDocModificado == null || numDocModificado.trim().isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "FACTURA_COMPRA_NO_ENCONTRADA");
            b.put("detalle", "El XML de la " + etiquetaDoc + " no indica el número de la factura"
                    + " de compra que modifica (<numDocModificado> vacío), así que no hay"
                    + " forma de saber a qué documento afecta.");
            b.put("solucion", "Revise el XML con el proveedor: sin ese dato no se puede"
                    + " registrar el abono ni generar la contabilidad.");
            bloqueantes.add(b);
            System.out.println("⚠ BLOQUEANTE factura afectada: numDocModificado vacío");
            return;
        }

        Long idTitular = titular != null ? titular.getCodigo() : null;
        List<FacturaCompra> candidatas;
        try {
            candidatas = aplicacionPagoCxpDaoService
                    .selectFacturaByNumero(numDocModificado, idTitular, idEmpresa);
        } catch (Throwable e) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "FACTURA_COMPRA_NO_ENCONTRADA");
            b.put("mensaje", e.getMessage());
            b.put("detalle", "No se pudo buscar la factura de compra N° " + numDocModificado
                    + " que afecta esta " + etiquetaDoc + ": " + e.getMessage());
            b.put("solucion", solucion);
            bloqueantes.add(b);
            System.out.println("⚠ BLOQUEANTE factura afectada (error de consulta): " + e.getMessage());
            return;
        }

        if (candidatas == null || candidatas.isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "FACTURA_COMPRA_NO_ENCONTRADA");
            b.put("detalle", "No existe en el sistema la factura de compra N° " + numDocModificado
                    + " del proveedor '" + (titular != null ? titular.getNombre() : "?")
                    + "', que es la que esta " + etiquetaDoc + " modifica.");
            b.put("solucion", solucion);
            bloqueantes.add(b);
            System.out.println("⚠ BLOQUEANTE factura afectada no encontrada: N° "
                    + numDocModificado + " titular=" + idTitular);
            return;
        }

        if (candidatas.size() > 1) {
            List<String> ids = new ArrayList<>();
            for (FacturaCompra fc : candidatas) ids.add(String.valueOf(fc.getId()));
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "FACTURA_COMPRA_NO_ENCONTRADA");
            b.put("detalle", "Existe más de una factura de compra con el número "
                    + numDocModificado + " para el mismo proveedor (ids " + ids + "),"
                    + " así que no se puede saber cuál modifica esta " + etiquetaDoc + ".");
            b.put("solucion", "Revise los documentos duplicados en Cuentas por Pagar y anule"
                    + " o corrija el que sobre.");
            b.put("facturas", ids);
            bloqueantes.add(b);
            System.out.println("⚠ BLOQUEANTE factura afectada duplicada: N° "
                    + numDocModificado + " ids=" + ids);
            return;
        }

        System.out.println("✓ Factura de compra afectada resuelta: id="
                + candidatas.get(0).getId() + " | buscado='" + numDocModificado + "'");
    }

    /**
     * Bloqueante de la factura de <b>venta</b> que una retención recibida abona:
     * tiene que existir, y tiene que ser una sola.
     *
     * <p>
     * Se resuelve con {@code AplicacionPagoCxcDaoService.selectFacturaByNumero},
     * que es la consulta que usa después la aplicación de pago del Paso 4 —
     * compara el número <b>sin guiones</b>, así que el SRI puede mandar
     * '001001000000784' y encontrar el '001-001-000000784' de {@code CBR.FCTR}—,
     * de modo que el bloqueante y el cobro no pueden discrepar.
     * </p>
     *
     * <p>
     * <b>No se llama a {@code AplicacionPagoCxcService.resolverFacturaPorNumero}</b>,
     * que sería el equivalente a nivel de servicio, y este es el motivo: comunica
     * el fallo con {@code IncomeException}, anotada
     * {@code @ApplicationException(rollback = true)}. Al cruzar la frontera del
     * EJB el contenedor marca <b>esta</b> transacción para rollback antes de
     * entregarnos la excepción, y atraparla no la desmarca: el método retornaría
     * su mapa de bloqueantes, el contenedor encontraría la marca al hacer commit
     * y el REST devolvería un 500 opaco en vez del 422 con {@code bloqueantes}.
     * Así estuvo desde el 2026-08-13 hasta el 2026-08-23. Ver §11 decisión 18.
     * </p>
     *
     * @param bloqueantes       : Lista donde se acumulan
     * @param numDocSustento    : Número de la factura de venta, tal como viene en el XML
     * @param numAutDocSustento : Autorización del sustento, solo para el mensaje
     * @param idEmpresa         : Id de la empresa contable
     * @param etiquetaLog       : "doc sustento" o "doc sustento V2", para la traza
     */
    private void agregarBloqueanteFacturaVenta(List<Map<String, Object>> bloqueantes,
            String numDocSustento, String numAutDocSustento, Long idEmpresa, String etiquetaLog) {

        String numero = numDocSustento != null ? numDocSustento.trim() : "";
        String motivo;

        if (numero.isEmpty()) {
            motivo = "El documento no indica el número de la factura a la que afecta. "
                    + "No es posible registrar el cobro ni generar la contabilidad.";
        } else {
            List<com.saa.model.cxc.Factura> facturas;
            try {
                facturas = aplicacionPagoCxcDaoService.selectFacturaByNumero(numero, null, idEmpresa);
            } catch (Throwable ex) {
                facturas = null;
                System.err.println("⚠ Error consultando la factura de venta N° " + numero
                        + ": " + ex.getMessage());
            }

            if (facturas == null || facturas.isEmpty()) {
                motivo = "No existe en el sistema la factura de venta N° " + numero
                        + ". No se puede registrar el documento que la afecta.";
            } else if (facturas.size() > 1) {
                motivo = "Existe más de una factura de venta con el número " + numero
                        + ". Revise los documentos duplicados.";
            } else {
                System.out.println("✓ Documento sustento resuelto en CXC (" + etiquetaLog
                        + "): factura id=" + facturas.get(0).getId()
                        + " | numero=" + facturas.get(0).getNumero()
                        + " | buscado='" + numero + "'");
                return;
            }
        }

        Map<String, Object> b = new HashMap<>();
        b.put("tipo", "FACTURA_VENTA_NO_ENCONTRADA");
        b.put("mensaje", motivo);
        b.put("detalle", "No se pudo resolver la factura de venta a la que afecta esta "
                + "retención. Número: '" + numero + "' | Autorización: '"
                + numAutDocSustento + "'. Emita o cargue primero la factura.");
        b.put("solucion", "Verifique que la factura de venta exista en CXC. El número se "
                + "compara sin guiones, así que '001001000000784' y '001-001-000000784' "
                + "son equivalentes.");
        bloqueantes.add(b);
        System.out.println("⚠ BLOQUEANTE " + etiquetaLog + ": " + motivo
                + " | Número: '" + numero + "' | Autorización: '" + numAutDocSustento + "'");
    }

    /**
     * Arma el retorno estructurado que corta el registro sin grabar nada. Misma
     * forma que devuelve {@code registrarFacturaCompra}, que es la que el 422 de
     * {@code registrarBD} y el lote de la fase 3 ya saben leer.
     *
     * @param bloqueantes : Condiciones que impiden registrar
     * @param etiquetaDoc : "la nota de crédito", "la liquidación de compra", …
     * @return            : Mapa con pendienteClasificacion, bloqueantes y mensaje
     */
    private Map<String, Object> cortarPorBloqueantes(List<Map<String, Object>> bloqueantes,
            String etiquetaDoc) {
        System.out.println("⚠ Registro de " + etiquetaDoc + " detenido. Bloqueantes: " + bloqueantes);
        Map<String, Object> r = new HashMap<>();
        r.put("pendienteClasificacion", true);
        r.put("bloqueantes", bloqueantes);
        r.put("mensaje", "No se puede registrar " + etiquetaDoc + ". Hay " + bloqueantes.size()
                + " condición(es) bloqueante(s) que deben resolverse primero.");
        return r;
    }


    private Map<String, Object> registrarNotaCreditoCompra(DocumentoCxp doc, String xmlContent,
                                                            Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        String numDocModificado = getXmlValue(xmlDoc, "numDocModificado");

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones bloqueantes (sin grabar nada si alguna falla)
        // Antes esto reventaba con una excepción y el frontend recibía un 500
        // con texto plano; dentro de un lote de 50 eso es ruido inservible.
        // Ver §9 defecto 3.
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();
        agregarBloqueantesComunesCompra(bloqueantes, titular, idEmpresa,
                com.saa.rubros.TipoAsientos.NOTAS_CREDITO_COMPRA, "Notas de Crédito de Compra");
        agregarBloqueanteFacturaAfectada(bloqueantes, numDocModificado, titular, idEmpresa,
                "nota de crédito");

        if (!bloqueantes.isEmpty())
            return cortarPorBloqueantes(bloqueantes, "la nota de crédito");

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        NotaCreditoCompra nc = new NotaCreditoCompra();
        nc.setEmpresa(empresa);
        nc.setTipoComprobante(getXmlValue(xmlDoc, "codDoc"));
        nc.setClave(doc.getClaveAcceso());
        nc.setNumero(doc.getSerieComprobante());
        nc.setNumEstablecimiento(getXmlValue(xmlDoc, "estab"));
        nc.setNumPtoEmision(getXmlValue(xmlDoc, "ptoEmi"));
        nc.setSecuencial(getXmlValue(xmlDoc, "secuencial"));
        nc.setAmbiente(parseLong(getXmlValue(xmlDoc, "ambiente")));
        nc.setFecha(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionNotaCredito")));
        nc.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        nc.setAutorizacion(numeroAutorizacion);
        nc.setObservacion(getXmlValue(xmlDoc, "motivo"));
        nc.setTipoDocModificado(getXmlValue(xmlDoc, "codDocModificado"));
        nc.setNumDocModificado(numDocModificado);
        nc.setFechaEmisionDM(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionDocSustento")));
        nc.setSubtotal(parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos")));
        // El total de la NC es <valorModificacion>; <importeTotal> es de la
        // factura y no existe en el esquema de nota de credito del SRI.
        String totalNcStr = getXmlValue(xmlDoc, "valorModificacion");
        if (totalNcStr.isEmpty()) totalNcStr = getXmlValue(xmlDoc, "importeTotal");
        nc.setTotal(parseDouble(totalNcStr));
        // IVA de la cabecera: es el que se contabiliza (ver leerIvaCabecera)
        Double[] ivaCabNc = leerIvaCabecera(xmlDoc, "totalImpuesto");
        if (ivaCabNc != null) {
            nc.setvIVA(ivaCabNc[0]);
            nc.setpIVA(ivaCabNc[1]);
        }
        nc.setTitular(titular);
        nc.setUsuario(usuario);
        nc.setEstado(Long.valueOf(Estado.ACTIVO));
        nc.setEstadoEmision(2L);

        // La factura de compra afectada ya se verificó en el PASO 2 como
        // bloqueante FACTURA_COMPRA_NO_ENCONTRADA, con la misma consulta que usa
        // después la aplicación de pago. Si llegamos aquí, existe y es una sola.

        nc = notaCreditoCompraDaoService.save(nc, null);

        NodeList detallesXml = xmlDoc.getElementsByTagName("detalle");
        for (int i = 0; i < detallesXml.getLength(); i++) {
            Element el = (Element) detallesXml.item(i);
            Long porcIVA = null; double valIVA = 0.0;
            NodeList impDet = el.getElementsByTagName("impuesto");
            if (impDet.getLength() > 0) {
                Element impEl = (Element) impDet.item(0);
                porcIVA = parseLong(getElementValue(impEl, "tarifa"));
                valIVA  = parseDouble(getElementValue(impEl, "valor"));
            }
            DetalleNotaCreditoCompra d = new DetalleNotaCreditoCompra();
            d.setNotaCredito(nc);
            d.setDescripcion(getElementValue(el, "descripcion"));
            d.setCantidad(parseDouble(getElementValue(el, "cantidad")));
            d.setValor(parseDouble(getElementValue(el, "precioUnitario")));
            d.setSubTotal(parseDouble(getElementValue(el, "precioTotalSinImpuesto")));
            d.setDescuento(parseDouble(getElementValue(el, "descuento")));
            d.setBaseImponible(parseDouble(getElementValue(el, "precioTotalSinImpuesto")));
            d.setPorcentajeIVA(porcIVA);
            d.setValorIVA(valIVA);
            d.setTotal(parseDouble(getElementValue(el, "precioTotalSinImpuesto")) + valIVA);
            d.setEstado(Long.valueOf(Estado.ACTIVO));
            detalleNotaCreditoCompraDaoService.save(d, null);
        }

        PathNotaCreditoCompra path = new PathNotaCreditoCompra();
        path.setNotaCredito(nc);
        path.setPath(doc.getPathXml());
        path.setAlterno(1L);
        pathNotaCreditoCompraDaoService.save(path, null);

        // ── codSustento (ATS, Tabla 5) extendido a NTCC (2026-08-28) ──────────
        // No bloquea el registro si falla -mismo criterio que FacturaCompra-.
        boolean sustentoPendienteNc = false;
        try {
            sustentoPendienteNc = sustentoTributarioService.resolverSiFaltaNotaCredito(nc) == null;
        } catch (Throwable e) {
            System.out.println("ATENCION: fallo la resolucion de codSustento de la nota de credito "
                    + nc.getId() + ": " + e.getMessage());
            sustentoPendienteNc = true;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", nc.getId());
        r.put("tipoTablaDestino", "NOTA_CREDITO_COMPRA");
        r.put("mensaje", "NotaCreditoCompra registrada con id=" + nc.getId());
        if (sustentoPendienteNc) {
            r.put("sustentoTributarioPendiente", true);
            r.put("advertenciaSustentoTributario", "La nota de credito " + nc.getId() + " quedo sin"
                    + " codSustento (Tabla 5 del ATS) resuelto. Corrijalo con PUT"
                    + " /rest/ntcc/sustento/" + nc.getId() + " antes de generar el ATS.");
        }
        return r;
    }

    private Map<String, Object> registrarNotaDebitoCompra(DocumentoCxp doc, String xmlContent,
                                                           Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        String numDocModificado = getXmlValue(xmlDoc, "numDocModificado");

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones bloqueantes (sin grabar nada si alguna falla)
        // Ver §9 defecto 3: antes esto reventaba con un 500 de texto plano.
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();
        agregarBloqueantesComunesCompra(bloqueantes, titular, idEmpresa,
                com.saa.rubros.TipoAsientos.NOTAS_DEBITO_COMPRA, "Notas de Débito de Compra");
        agregarBloqueanteFacturaAfectada(bloqueantes, numDocModificado, titular, idEmpresa,
                "nota de débito");

        if (!bloqueantes.isEmpty())
            return cortarPorBloqueantes(bloqueantes, "la nota de débito");

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        NotaDebitoCompra nd = new NotaDebitoCompra();
        nd.setEmpresa(empresa);
        nd.setTipoComprobante(getXmlValue(xmlDoc, "codDoc"));
        nd.setClave(doc.getClaveAcceso());
        nd.setNumero(doc.getSerieComprobante());
        nd.setNumEstablecimiento(getXmlValue(xmlDoc, "estab"));
        nd.setNumPtoEmision(getXmlValue(xmlDoc, "ptoEmi"));
        nd.setSecuencial(getXmlValue(xmlDoc, "secuencial"));
        nd.setAmbiente(parseLong(getXmlValue(xmlDoc, "ambiente")));
        nd.setFecha(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionNotaDebito")));
        nd.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        nd.setAutorizacion(numeroAutorizacion);
        nd.setObservacion(getXmlValue(xmlDoc, "motivo"));
        nd.setTipoDocModificado(getXmlValue(xmlDoc, "codDocModificado"));
        nd.setNumDocModificado(numDocModificado);
        nd.setFechaEmisionDM(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionDocSustento")));
        nd.setSubtotal(parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos")));
        // El total de la ND es <valorTotal>; <importeTotal> es de la factura y
        // no existe en el esquema de nota de debito del SRI.
        String totalNdStr = getXmlValue(xmlDoc, "valorTotal");
        if (totalNdStr.isEmpty()) totalNdStr = getXmlValue(xmlDoc, "importeTotal");
        nd.setTotal(parseDouble(totalNdStr));
        // IVA de la cabecera: la ND lo declara en <impuestos><impuesto> y sus
        // <motivo> vienen sin impuesto, asi que es la unica fuente del IVA.
        Double[] ivaCabNd = leerIvaCabecera(xmlDoc, "impuesto");
        if (ivaCabNd != null) {
            nd.setvIVA(ivaCabNd[0]);
            nd.setpIVA(ivaCabNd[1]);
        }
        nd.setTitular(titular);
        nd.setUsuario(usuario);
        nd.setEstado(Long.valueOf(Estado.ACTIVO));
        nd.setEstadoEmision(2L);

        // La factura de compra afectada ya se verificó en el PASO 2 como
        // bloqueante FACTURA_COMPRA_NO_ENCONTRADA.

        nd = notaDebitoCompraDaoService.save(nd, null);

        NodeList motivos = xmlDoc.getElementsByTagName("motivo");
        for (int i = 0; i < motivos.getLength(); i++) {
            Element el = (Element) motivos.item(i);
            DetalleNotaDebitoCompra d = new DetalleNotaDebitoCompra();
            d.setNotaDebito(nd);
            d.setDescripcion(getElementValue(el, "razon"));
            double val = parseDouble(getElementValue(el, "valor"));
            d.setValor(val);
            d.setSubTotal(val);
            d.setTotal(val);
            d.setEstado(Long.valueOf(Estado.ACTIVO));
            detalleNotaDebitoCompraDaoService.save(d, null);
        }

        PathNotaDebitoCompra path = new PathNotaDebitoCompra();
        path.setNotaDebito(nd);
        path.setPath(doc.getPathXml());
        path.setAlterno(1L);
        pathNotaDebitoCompraDaoService.save(path, null);

        // ── codSustento (ATS, Tabla 5) extendido a NTDC (2026-08-28) ──────────
        // Sin excepcion por grupo -DetalleNotaDebitoCompra no tiene columna de
        // producto-, siempre regla base del IVA. No bloquea el registro si falla.
        boolean sustentoPendienteNd = false;
        try {
            sustentoPendienteNd = sustentoTributarioService.resolverSiFaltaNotaDebito(nd) == null;
        } catch (Throwable e) {
            System.out.println("ATENCION: fallo la resolucion de codSustento de la nota de debito "
                    + nd.getId() + ": " + e.getMessage());
            sustentoPendienteNd = true;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", nd.getId());
        r.put("tipoTablaDestino", "NOTA_DEBITO_COMPRA");
        r.put("mensaje", "NotaDebitoCompra registrada con id=" + nd.getId());
        if (sustentoPendienteNd) {
            r.put("sustentoTributarioPendiente", true);
            r.put("advertenciaSustentoTributario", "La nota de debito " + nd.getId() + " quedo sin"
                    + " codSustento (Tabla 5 del ATS) resuelto. Corrijalo con PUT"
                    + " /rest/ntdc/sustento/" + nd.getId() + " antes de generar el ATS.");
        }
        return r;
    }

    private Map<String, Object> registrarLiquidacionCompraCompra(DocumentoCxp doc, String xmlContent,
                                                                  Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones bloqueantes (sin grabar nada si alguna falla)
        // Ver §9 defecto 3. La liquidación no modifica ningún documento previo,
        // así que solo lleva los dos bloqueantes comunes.
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();
        agregarBloqueantesComunesCompra(bloqueantes, titular, idEmpresa,
                com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS,
                "Liquidaciones de Compra recibidas");

        if (!bloqueantes.isEmpty())
            return cortarPorBloqueantes(bloqueantes, "la liquidación de compra");

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        LiquidacionCompraCompra lq = new LiquidacionCompraCompra();
        lq.setEmpresa(empresa);
        lq.setTipoComprobante(getXmlValue(xmlDoc, "codDoc"));
        lq.setClave(doc.getClaveAcceso());
        lq.setNumero(doc.getSerieComprobante());
        lq.setNumEstablecimiento(getXmlValue(xmlDoc, "estab"));
        lq.setNumPtoEmision(getXmlValue(xmlDoc, "ptoEmi"));
        lq.setSecuencial(getXmlValue(xmlDoc, "secuencial"));
        lq.setAmbiente(parseLong(getXmlValue(xmlDoc, "ambiente")));
        lq.setFecha(parseFechaHora(getXmlValue(xmlDoc, "fechaEmision")));
        lq.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        lq.setAutorizacion(numeroAutorizacion);
        lq.setSubtotal(parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos")));
        lq.setTotal(parseDouble(getXmlValue(xmlDoc, "importeTotal")));
        // IVA de la cabecera: es el que se contabiliza (ver leerIvaCabecera)
        Double[] ivaCabLq = leerIvaCabecera(xmlDoc, "totalImpuesto");
        if (ivaCabLq != null) {
            lq.setvIVA(ivaCabLq[0]);
            lq.setpIVA(ivaCabLq[1]);
        }
        lq.setTitular(titular);
        lq.setUsuario(usuario);
        lq.setEstado(Long.valueOf(Estado.ACTIVO));
        lq.setEstadoEmision(2L);
        lq = liquidacionCompraCompraDaoService.save(lq, null);

        NodeList detallesXml = xmlDoc.getElementsByTagName("detalle");
        for (int i = 0; i < detallesXml.getLength(); i++) {
            Element el = (Element) detallesXml.item(i);
            Long porcIVA = null; double valIVA = 0.0;
            NodeList impDet = el.getElementsByTagName("impuesto");
            if (impDet.getLength() > 0) {
                Element impEl = (Element) impDet.item(0);
                porcIVA = parseLong(getElementValue(impEl, "tarifa"));
                valIVA  = parseDouble(getElementValue(impEl, "valor"));
            }
            DetalleLiquidacionCompraCompra d = new DetalleLiquidacionCompraCompra();
            d.setLiquidacion(lq);
            d.setDescripcion(getElementValue(el, "descripcion"));
            d.setCantidad(parseDouble(getElementValue(el, "cantidad")));
            d.setValor(parseDouble(getElementValue(el, "precioUnitario")));
            d.setSubTotal(parseDouble(getElementValue(el, "precioTotalSinImpuesto")));
            d.setPorcentajeIVA(porcIVA);
            d.setValorIVA(valIVA);
            d.setTotal(parseDouble(getElementValue(el, "precioTotalSinImpuesto")) + valIVA);
            d.setEstado(Long.valueOf(Estado.ACTIVO));
            detalleLiquidacionCompraCompraDaoService.save(d, null);
        }

        PathLiquidacionCompraCompra path = new PathLiquidacionCompraCompra();
        path.setLiquidacion(lq);
        path.setPath(doc.getPathXml());
        path.setAlterno(1L);
        pathLiquidacionCompraCompraDaoService.save(path, null);

        // ── codSustento (ATS, Tabla 5) extendido a LQCC (2026-08-28) ──────────
        // Las lineas de esta carga automatica no traen producto (el XML del SRI no lo
        // declara), asi que la excepcion por grupo no aplica aqui y siempre resuelve por
        // la regla base del IVA -consistente, no es un error-. No bloquea el registro.
        boolean sustentoPendienteLq = false;
        try {
            sustentoPendienteLq = sustentoTributarioService.resolverSiFaltaLiquidacion(lq) == null;
        } catch (Throwable e) {
            System.out.println("ATENCION: fallo la resolucion de codSustento de la liquidacion "
                    + lq.getId() + ": " + e.getMessage());
            sustentoPendienteLq = true;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", lq.getId());
        r.put("tipoTablaDestino", "LIQUIDACION_COMPRA_COMPRA");
        r.put("mensaje", "LiquidacionCompraCompra registrada con id=" + lq.getId());
        if (sustentoPendienteLq) {
            r.put("sustentoTributarioPendiente", true);
            r.put("advertenciaSustentoTributario", "La liquidacion " + lq.getId() + " quedo sin"
                    + " codSustento (Tabla 5 del ATS) resuelto. Corrijalo con PUT"
                    + " /rest/lqcc/sustento/" + lq.getId() + " antes de generar el ATS.");
        }
        return r;
    }

    private Map<String, Object> registrarRetencionCompra(DocumentoCxp doc, String xmlContent,
                                                         Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        // ══════════════════════════════════════════════════════════════════
        // PASO 1 — Obtener proveedor y leer nodos del XML para validación
        // ══════════════════════════════════════════════════════════════════
        Titular proveedor = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        NodeList retenciones = obtenerDetallesRetencion(xmlDoc);

        // Datos del documento sustento (para validación cruzada con CXC)
        String numAutDocSustento = getXmlValue(xmlDoc, "numAutDocSustento");
        String numDocSustento    = getXmlValue(xmlDoc, "numDocSustento");

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones bloqueantes (sin grabar nada si alguna falla)
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();

        // 2a. Cuenta contable CxP del proveedor
        boolean titularTieneCuenta = verificarCuentaContableProveedor(proveedor.getCodigo(), idEmpresa);
        if (!titularTieneCuenta) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "PROVEEDOR_SIN_CUENTA");
            b.put("detalle", "El proveedor '" + proveedor.getNombre() + "' (RUC: " + proveedor.getIdentificacion()
                    + ") no tiene cuenta contable CxP asignada. Configúrela en Contabilidad → Cuentas por Titular.");
            bloqueantes.add(b);
        }

        // 2b. Tipo de asiento para Retenciones de Compra configurado en BD.
        //     Solo cuando la empresa genera contabilidad: con generaConta = 0 no se
        //     va a emitir ningún asiento, así que exigir la plantilla impediría
        //     registrar el documento por una razón que no aplica. Misma guarda que
        //     usan la factura de compra y agregarBloqueantesComunesCompra.
        if (verificarGeneraConta(idEmpresa)
                && !existeTipoAsiento(com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS, idEmpresa)) {
            bloqueantes.add(bloqueanteTipoAsiento(
                    com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS, "Retenciones de Compra"));
        }

        // 2c. Cada código de retención del XML debe tener cuenta en TSRI (PGS)
        //     codigo=1 (Renta) → lsri.tabla='608' | codigo=2 (IVA) → lsri.tabla='20'
        List<String> codigosSinCuenta = new ArrayList<>();
        for (int i = 0; i < retenciones.getLength(); i++) {
            Element el = (Element) retenciones.item(i);
            String codImpuesto  = getElementValue(el, "codigo");
            String codRetencion = getElementValue(el, "codigoRetencion");
            String lsriTabla = "1".equals(codImpuesto) ? "608" : ("2".equals(codImpuesto) ? "20" : null);
            if (lsriTabla == null) {
                codigosSinCuenta.add("Tipo de impuesto desconocido: codigo=" + codImpuesto
                        + " (codigoRetencion=" + codRetencion + ")");
                continue;
            }
            try {
                List<?> cuentas = em.createQuery(
                        "SELECT t.planCuenta FROM TsriCompra t "
                        + "WHERE t.lsri.tabla = :lsri AND t.codigo = :cod "
                        + "AND t.estado = 1 AND t.planCuenta IS NOT NULL")
                        .setParameter("lsri", lsriTabla)
                        .setParameter("cod", codRetencion)
                        .setMaxResults(1).getResultList();
                if (cuentas.isEmpty() || cuentas.get(0) == null) {
                    codigosSinCuenta.add("codigoRetencion=" + codRetencion
                            + " (tipo=" + codImpuesto + ", lsri=" + lsriTabla + ")");
                }
            } catch (Exception ex) {
                codigosSinCuenta.add("codigoRetencion=" + codRetencion
                        + " (error al consultar TSRI: " + ex.getMessage() + ")");
            }
        }
        if (!codigosSinCuenta.isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "CODIGOS_RETENCION_SIN_CUENTA");
            b.put("detalle", "Los siguientes códigos de retención no tienen cuenta contable en TSRI: "
                    + codigosSinCuenta
                    + ". Configúrelos en Compras → Tipos SRI.");
            b.put("codigos", codigosSinCuenta);
            bloqueantes.add(b);
        }

        // 2d. BLOQUEANTE: la retención abona una factura de VENTA (CXC), que debe
        //     existir. Se resuelve con la misma consulta que usa después la
        //     aplicación de pago, pero por el DAO: ver agregarBloqueanteFacturaVenta
        //     y §11 decisión 18 — el resolutor de servicio lanza IncomeException y
        //     atraparla dejaba la transacción condenada.
        if (!numDocSustento.isEmpty() || !numAutDocSustento.isEmpty()) {
            agregarBloqueanteFacturaVenta(bloqueantes, numDocSustento, numAutDocSustento,
                    idEmpresa, "doc sustento");
        }

        // Si hay bloqueantes → cortar sin grabar nada
        if (!bloqueantes.isEmpty()) {
            System.out.println("⚠ Registro de RetencionCompra detenido. Bloqueantes: " + bloqueantes);
            Map<String, Object> r = new HashMap<>();
            r.put("pendienteClasificacion", true);
            r.put("bloqueantes", bloqueantes);
            r.put("mensaje", "No se puede registrar la retención. Hay " + bloqueantes.size()
                    + " condición(es) bloqueante(s) que deben resolverse primero.");
            return r;
        }

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        double totalRetenido = calculaTotalRetenido(retenciones, doc);

        RetencionCompra rc = new RetencionCompra();
        rc.setEmpresa(empresa);
        rc.setTipoComprobante(getXmlValue(xmlDoc, "codDoc"));
        rc.setClave(doc.getClaveAcceso());
        rc.setNumero(doc.getSerieComprobante());
        rc.setNumEstablecimiento(getXmlValue(xmlDoc, "estab"));
        rc.setNumPtoEmision(getXmlValue(xmlDoc, "ptoEmi"));
        rc.setSecuencial(getXmlValue(xmlDoc, "secuencial"));
        rc.setAmbiente(parseLong(getXmlValue(xmlDoc, "ambiente")));
        rc.setFecha(parseFechaHora(getXmlValue(xmlDoc, "fechaEmision")));
        rc.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        rc.setAutorizacion(numeroAutorizacion);
        rc.setPeriodoFiscal(getXmlValue(xmlDoc, "periodoFiscal"));
        rc.setTotal(totalRetenido);
        rc.setProveedor(proveedor);
        rc.setUsuario(usuario);
        rc.setEstado(Long.valueOf(Estado.ACTIVO));
        rc.setEstadoEmision(2L);
        rc = retencionCompraDaoService.save(rc, null);

        for (int i = 0; i < retenciones.getLength(); i++) {
            Element el = (Element) retenciones.item(i);
            DetalleRetencionCompra d = new DetalleRetencionCompra();
            d.setRetencion(rc);
            d.setCodImpuesto(getElementValue(el, "codigo"));
            d.setCodRetencion(getElementValue(el, "codigoRetencion"));
            d.setBaseImponible(parseDouble(getElementValue(el, "baseImponible")));
            d.setPorcentajeReten(parseDouble(getElementValue(el, "porcentajeRetener")));
            d.setValorReten(parseDouble(getElementValue(el, "valorRetenido")));
            // Documento sustento: es lo que permite localizar despues la factura
            // de venta a la que esta retencion abona. Sin esto el detalle queda
            // huerfano y no se puede registrar el cobro.
            d.setTipoDocReten(getValorDocSustento(el, xmlDoc, "codDocSustento"));
            d.setNumDocReten(getValorDocSustento(el, xmlDoc, "numDocSustento"));
            d.setFechaEmiDoc(parseFecha(getValorDocSustento(el, xmlDoc, "fechaEmisionDocSustento")));
            d.setEstado(Long.valueOf(Estado.ACTIVO));
            detalleRetencionCompraDaoService.save(d, null);
        }

        PathRetencionCompra path = new PathRetencionCompra();
        path.setRetencion(rc);
        path.setPath(doc.getPathXml());
        path.setAlterno(1L);
        pathRetencionCompraDaoService.save(path, null);

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", rc.getId());
        r.put("tipoTablaDestino", "RETENCION_COMPRA");
        r.put("mensaje", "RetencionCompra registrada con id=" + rc.getId());
        return r;
    }

    private Map<String, Object> registrarRetencionCompraV2(DocumentoCxp doc, String xmlContent,
                                                           Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        // ══════════════════════════════════════════════════════════════════
        // PASO 1 — Obtener proveedor y leer nodos del XML para validación
        // ══════════════════════════════════════════════════════════════════
        Titular proveedor = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        NodeList retenciones = obtenerDetallesRetencion(xmlDoc);

        // Datos del documento sustento (para validación cruzada con CXC)
        String numAutDocSustento = getXmlValue(xmlDoc, "numAutDocSustento");
        String numDocSustento    = getXmlValue(xmlDoc, "numDocSustento");

        // ══════════════════════════════════════════════════════════════════
        // PASO 2 — Validaciones bloqueantes (sin grabar nada si alguna falla)
        // ══════════════════════════════════════════════════════════════════
        List<Map<String, Object>> bloqueantes = new ArrayList<>();

        // 2a. Cuenta contable CxP del proveedor
        boolean titularTieneCuenta = verificarCuentaContableProveedor(proveedor.getCodigo(), idEmpresa);
        if (!titularTieneCuenta) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "PROVEEDOR_SIN_CUENTA");
            b.put("detalle", "El proveedor '" + proveedor.getNombre() + "' (RUC: " + proveedor.getIdentificacion()
                    + ") no tiene cuenta contable CxP asignada. Configúrela en Contabilidad → Cuentas por Titular.");
            bloqueantes.add(b);
        }

        // 2b. Tipo de asiento para Retenciones de Compra V2 configurado en BD.
        //     Solo si la empresa genera contabilidad — ver la nota equivalente en
        //     registrarRetencionCompra.
        if (verificarGeneraConta(idEmpresa)
                && !existeTipoAsiento(com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2, idEmpresa)) {
            bloqueantes.add(bloqueanteTipoAsiento(
                    com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2,
                    "Retenciones de Compra V2"));
        }

        // 2c. Cada código de retención del XML debe tener cuenta en TSRI
        List<String> codigosSinCuenta = new ArrayList<>();
        for (int i = 0; i < retenciones.getLength(); i++) {
            Element el = (Element) retenciones.item(i);
            String codImpuesto  = getElementValue(el, "codigo");
            String codRetencion = getElementValue(el, "codigoRetencion");
            String lsriTabla = "1".equals(codImpuesto) ? "608" : ("2".equals(codImpuesto) ? "20" : null);
            if (lsriTabla == null) {
                codigosSinCuenta.add("Tipo de impuesto desconocido: codigo=" + codImpuesto
                        + " (codigoRetencion=" + codRetencion + ")");
                continue;
            }
            try {
                List<?> cuentas = em.createQuery(
                        "SELECT t.planCuenta FROM TsriCompra t "
                        + "WHERE t.lsri.tabla = :lsri AND t.codigo = :cod "
                        + "AND t.estado = 1 AND t.planCuenta IS NOT NULL")
                        .setParameter("lsri", lsriTabla)
                        .setParameter("cod", codRetencion)
                        .setMaxResults(1).getResultList();
                if (cuentas.isEmpty() || cuentas.get(0) == null) {
                    codigosSinCuenta.add("codigoRetencion=" + codRetencion
                            + " (tipo=" + codImpuesto + ", lsri=" + lsriTabla + ")");
                }
            } catch (Exception ex) {
                codigosSinCuenta.add("codigoRetencion=" + codRetencion
                        + " (error al consultar TSRI: " + ex.getMessage() + ")");
            }
        }
        if (!codigosSinCuenta.isEmpty()) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "CODIGOS_RETENCION_SIN_CUENTA");
            b.put("detalle", "Los siguientes códigos de retención no tienen cuenta contable en TSRI: "
                    + codigosSinCuenta
                    + ". Configúrelos en Compras → Tipos SRI.");
            b.put("codigos", codigosSinCuenta);
            bloqueantes.add(b);
        }

        // 2d. BLOQUEANTE: la retención abona una factura de VENTA (CXC), así que
        //     esa factura debe existir antes de registrar la retención.
        //     Se resuelve con el MISMO método que usará la aplicación de pago del
        //     Paso 4 (AplicacionPagoCxcService.resolverFacturaPorNumero →
        //     selectFacturaByNumero), que compara el número SIN GUIONES: el SRI
        //     manda '001001000000784' y en CBR.FCTR está como
        //     '001-001-000000784'. Antes se comparaba el número tal cual con un
        //     COUNT propio, así que la validación fallaba aunque la factura
        //     existiera. Reutilizar el resolutor garantiza que el bloqueante y la
        //     aplicación de pago nunca discrepen.
        //     Solo se valida si la empresa genera contabilidad: con generaConta=0
        //     no hay asiento ni aplicación de pago, y la factura no hace falta.
        if (verificarGeneraConta(idEmpresa)) {
            // Números de documento sustento presentes en el XML. Se comparan tal
            // cual (sin normalizar) porque así los distingue el Paso 4, que lee
            // 'select distinct d.numDocReten' de los detalles ya grabados.
            java.util.LinkedHashSet<String> numerosSustento = new java.util.LinkedHashSet<>();
            for (int i = 0; i < retenciones.getLength(); i++) {
                String num = getValorDocSustento((Element) retenciones.item(i), xmlDoc, "numDocSustento");
                if (num != null && !num.trim().isEmpty()) numerosSustento.add(num.trim());
            }
            if (numerosSustento.isEmpty() && !numDocSustento.isEmpty()) {
                numerosSustento.add(numDocSustento.trim());
            }

            if (numerosSustento.size() > 1) {
                Map<String, Object> b = new HashMap<>();
                b.put("tipo", "RETENCION_MULTIDOCUMENTO");
                b.put("mensaje", "La retención afecta a " + numerosSustento.size()
                        + " documentos sustento distintos " + numerosSustento
                        + ". El registro automático del cobro solo está soportado para "
                        + "retenciones de un solo documento sustento.");
                b.put("solucion", "Registre la retención manualmente o divídala por documento.");
                b.put("numeros", new ArrayList<>(numerosSustento));
                bloqueantes.add(b);
                System.out.println("⚠ BLOQUEANTE doc sustento V2 (multidocumento): " + numerosSustento);
            } else {
                String numSustento = numerosSustento.isEmpty() ? "" : numerosSustento.iterator().next();
                agregarBloqueanteFacturaVenta(bloqueantes, numSustento, numAutDocSustento,
                        idEmpresa, "doc sustento V2");
            }
        }

        // Si hay bloqueantes → cortar sin grabar nada
        if (!bloqueantes.isEmpty()) {
            System.out.println("⚠ Registro de RetencionCompraV2 detenido. Bloqueantes: " + bloqueantes);
            Map<String, Object> r = new HashMap<>();
            r.put("pendienteClasificacion", true);
            r.put("bloqueantes", bloqueantes);
            r.put("mensaje", "No se puede registrar la retención V2. Hay " + bloqueantes.size()
                    + " condición(es) bloqueante(s) que deben resolverse primero.");
            return r;
        }

        // ══════════════════════════════════════════════════════════════════
        // PASO 3 — Todas las condiciones OK → grabar en BD
        // ══════════════════════════════════════════════════════════════════
        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        double totalRetenido = calculaTotalRetenido(retenciones, doc);

        RetencionCompraV2 rc = new RetencionCompraV2();
        rc.setEmpresa(empresa);
        rc.setTipoComprobante(getXmlValue(xmlDoc, "codDoc"));
        rc.setClave(doc.getClaveAcceso());
        rc.setNumero(doc.getSerieComprobante());
        rc.setNumEstablecimiento(getXmlValue(xmlDoc, "estab"));
        rc.setNumPtoEmision(getXmlValue(xmlDoc, "ptoEmi"));
        rc.setSecuencial(getXmlValue(xmlDoc, "secuencial"));
        rc.setAmbiente(parseLong(getXmlValue(xmlDoc, "ambiente")));
        rc.setFecha(parseFechaHora(getXmlValue(xmlDoc, "fechaEmision")));
        rc.setFechaAutorizacion(parseFechaHora(fechaAutorizacionStr));
        rc.setAutorizacion(numeroAutorizacion);
        rc.setPeriodoFiscal(getXmlValue(xmlDoc, "periodoFiscal"));
        rc.setTotal(totalRetenido);
        rc.setProveedor(proveedor);
        rc.setUsuario(usuario);
        rc.setEstado(Long.valueOf(Estado.ACTIVO));
        rc.setEstadoEmision(2L);
        rc = retencionCompraV2DaoService.save(rc, null);

        // Grabar detalles de retención V2
        for (int i = 0; i < retenciones.getLength(); i++) {
            Element el = (Element) retenciones.item(i);
            com.saa.model.cxp.DetalleRetencionCompraV2 d = new com.saa.model.cxp.DetalleRetencionCompraV2();
            d.setRetencionCompraV2(rc);
            d.setCodImpuesto(getElementValue(el, "codigo"));
            d.setCodRetencion(getElementValue(el, "codigoRetencion"));
            d.setBaseImponible(parseDouble(getElementValue(el, "baseImponible")));
            d.setPorcentajeReten(parseDouble(getElementValue(el, "porcentajeRetener")));
            d.setValorReten(parseDouble(getElementValue(el, "valorRetenido")));
            // Documento sustento: en el esquema v2 vive en el <docSustento> que
            // envuelve a cada <retencion>, no dentro del propio detalle.
            d.setTipoDocReten(getValorDocSustento(el, xmlDoc, "codDocSustento"));
            d.setNumDocReten(getValorDocSustento(el, xmlDoc, "numDocSustento"));
            d.setFechaEmiDoc(parseFecha(getValorDocSustento(el, xmlDoc, "fechaEmisionDocSustento")));
            d.setEstado(Long.valueOf(Estado.ACTIVO));
            em.persist(d);
        }

        // TODO: Pendiente crear entidad PathRetencionCompraV2 para guardar el path.
        // Por ahora no se persiste path para evitar FK nula.

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", rc.getId());
        r.put("tipoTablaDestino", "RETENCION_COMPRA_V2");
        r.put("mensaje", "RetencionCompraV2 registrada con id=" + rc.getId());
        return r;
    }

    // =========================================================
    // Reversión de registros en tablas destino
    // =========================================================
    /**
     * Indica si el documento ya tiene una fila viva en su tabla destino.
     *
     * <p>
     * No basta con mirar {@code idDocumentoBD}: al revertir se borra la fila
     * pero el campo se conserva apuntando a un id muerto (ver
     * {@link #revertirRegistrosBD}), asi que hay que preguntar por la fila.
     * </p>
     *
     * @param doc : Documento de la bandeja
     * @return    : true si la fila destino todavia existe
     */
    private boolean registroBDVigente(DocumentoCxp doc) {
        String tipo  = doc.getTipoTablaDestino();
        Long idDocBD = doc.getIdDocumentoBD();
        if (tipo == null || idDocBD == null) return false;

        String entidad;
        switch (tipo) {
            case "FACTURA_COMPRA":            entidad = "FacturaCompra";           break;
            case "NOTA_CREDITO_COMPRA":       entidad = "NotaCreditoCompra";       break;
            case "NOTA_DEBITO_COMPRA":        entidad = "NotaDebitoCompra";        break;
            case "LIQUIDACION_COMPRA_COMPRA": entidad = "LiquidacionCompraCompra"; break;
            case "RETENCION_COMPRA":          entidad = "RetencionCompra";         break;
            case "RETENCION_COMPRA_V2":       entidad = "RetencionCompraV2";       break;
            default: return false;
        }
        try {
            long existe = ((Number) em.createQuery(
                    "select count(e) from " + entidad + " e where e.id = :id")
                    .setParameter("id", idDocBD).getSingleResult()).longValue();
            return existe > 0;
        } catch (Throwable e) {
            System.out.println("Error verificando registro vigente " + entidad + " id=" + idDocBD
                    + ": " + e.getMessage());
            return false;
        }
    }

    private void revertirRegistrosBD(DocumentoCxp doc) throws Throwable {
        String tipo = doc.getTipoTablaDestino();
        Long idDocBD = doc.getIdDocumentoBD();
        if (tipo == null || idDocBD == null) return;

        System.out.println("Revirtiendo tipo=" + tipo + " idDoc=" + idDocBD);

        // ── Eliminar las aplicaciones de pago que generó este documento ────────
        // Se hace ANTES de borrar el documento para no chocar con las FK. El
        // servicio marca la aplicación como reversada (para que el trigger
        // recalcule el estado de pago de la factura) y luego la borra.
        if ("NOTA_CREDITO_COMPRA".equals(tipo)) {
            aplicacionPagoCxpService.eliminarAplicacionesDeDocumento("NOTA_CREDITO", idDocBD);
        } else if ("NOTA_DEBITO_COMPRA".equals(tipo)) {
            aplicacionPagoCxpService.eliminarAplicacionesDeDocumento("NOTA_DEBITO", idDocBD);
        } else if ("RETENCION_COMPRA".equals(tipo)) {
            aplicacionPagoCxcService.eliminarAplicacionesDeDocumento("RETENCION", idDocBD);
        } else if ("RETENCION_COMPRA_V2".equals(tipo)) {
            aplicacionPagoCxcService.eliminarAplicacionesDeDocumento("RETENCION_V2", idDocBD);
        } else if ("FACTURA_COMPRA".equals(tipo)) {
            // No se puede borrar una factura que ya tiene pagos o abonos aplicados.
            java.util.List<com.saa.model.cxp.AplicacionPagoCxp> aplicaciones =
                    aplicacionPagoCxpService.consultarPorFactura(idDocBD, true);
            if (aplicaciones != null && !aplicaciones.isEmpty()) {
                throw new com.saa.basico.util.IncomeException("La factura de compra tiene " + aplicaciones.size()
                        + " pago(s) o abono(s) aplicados. Reverse primero esos pagos "
                        + "(retenciones, notas, anticipos o transferencias) antes de revertir "
                        + "el documento.");
            }
        }

        // ── Anular asiento contable vinculado ──────────────────────────────────
        anularAsientoDeDocumento(tipo, idDocBD);

        switch (tipo) {
            case "FACTURA_COMPRA":
                // Borrar reembolsos ANTES del detalle y la cabecera (FK FK_RMBF_FACTURA)
                em.createQuery("delete from ReembolsoFacturaCompra r where r.factura.id = :id")
                        .setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from DetalleFacturaCompra d where d.factura.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from FormaPagoFacturaCompra f where f.factura.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from PathFacturaCompra p where p.factura.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from FacturaCompra f where f.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            case "NOTA_CREDITO_COMPRA":
                em.createQuery("delete from DetalleNotaCreditoCompra d where d.notaCredito.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from PathNotaCreditoCompra p where p.notaCredito.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from NotaCreditoCompra n where n.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            case "NOTA_DEBITO_COMPRA":
                em.createQuery("delete from DetalleNotaDebitoCompra d where d.notaDebito.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from PathNotaDebitoCompra p where p.notaDebito.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from NotaDebitoCompra n where n.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            case "LIQUIDACION_COMPRA_COMPRA":
                em.createQuery("delete from DetalleLiquidacionCompraCompra d where d.liquidacion.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from PathLiquidacionCompraCompra p where p.liquidacion.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from LiquidacionCompraCompra l where l.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            case "RETENCION_COMPRA":
                em.createQuery("delete from DetalleRetencionCompra d where d.retencion.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from PathRetencionCompra p where p.retencion.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from RetencionCompra r where r.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            case "RETENCION_COMPRA_V2":
                // El detalle se borra primero por la FK DRC2.RETENCIONV2 -> RCV2.ID.
                // (No hay PathRetencionCompraV2: la ruta del XML queda en DocumentoCxp.)
                em.createQuery("delete from DetalleRetencionCompraV2 d where d.retencionCompraV2.id = :id").setParameter("id", idDocBD).executeUpdate();
                em.createQuery("delete from RetencionCompraV2 r where r.id = :id").setParameter("id", idDocBD).executeUpdate();
                break;
            default:
                throw new Exception("Tipo de tabla destino no reconocido para reversión: " + tipo);
        }
    }

    /**
     * Anula el asiento contable vinculado al documento CXP cambiando su estado a ANULADO (2).
     * Si el documento no tiene asiento vinculado, no hace nada.
     */
    private void anularAsientoDeDocumento(String tipo, Long idDocBD) {
        try {
            com.saa.model.cnt.Asiento asiento = null;
            switch (tipo) {
                case "FACTURA_COMPRA": {
                    FacturaCompra fc = em.find(FacturaCompra.class, idDocBD);
                    if (fc != null) asiento = fc.getAsiento();
                    break;
                }
                case "NOTA_CREDITO_COMPRA": {
                    com.saa.model.cxp.NotaCreditoCompra nc = em.find(com.saa.model.cxp.NotaCreditoCompra.class, idDocBD);
                    if (nc != null) asiento = nc.getAsiento();
                    break;
                }
                case "NOTA_DEBITO_COMPRA": {
                    com.saa.model.cxp.NotaDebitoCompra nd = em.find(com.saa.model.cxp.NotaDebitoCompra.class, idDocBD);
                    if (nd != null) asiento = nd.getAsiento();
                    break;
                }
                case "LIQUIDACION_COMPRA_COMPRA": {
                    com.saa.model.cxp.LiquidacionCompraCompra lq = em.find(com.saa.model.cxp.LiquidacionCompraCompra.class, idDocBD);
                    if (lq != null) asiento = lq.getAsiento();
                    break;
                }
                case "RETENCION_COMPRA": {
                    com.saa.model.cxp.RetencionCompra rc = em.find(com.saa.model.cxp.RetencionCompra.class, idDocBD);
                    if (rc != null) asiento = rc.getAsiento();
                    break;
                }
                case "RETENCION_COMPRA_V2": {
                    com.saa.model.cxp.RetencionCompraV2 rc = em.find(com.saa.model.cxp.RetencionCompraV2.class, idDocBD);
                    if (rc != null) asiento = rc.getAsiento();
                    break;
                }
            }
            if (asiento != null) {
                asiento.setEstado((long) com.saa.rubros.EstadoAsiento.ANULADO);
                em.merge(asiento);
                System.out.println("✓ Asiento " + asiento.getNumeroAlterno() + " anulado por reversión del documento " + tipo + " id=" + idDocBD);
            } else {
                System.out.println("ℹ Documento " + tipo + " id=" + idDocBD + " no tiene asiento vinculado, nada que anular.");
            }
        } catch (Exception e) {
            System.err.println("⚠ No se pudo anular el asiento del documento " + tipo + " id=" + idDocBD + ": " + e.getMessage());
        }
    }

    // =========================================================
    // Métodos utilitarios privados
    // =========================================================

    private String obtenerRucReceptor(Long idEmpresa) {
        try {
            @SuppressWarnings("unchecked")
            List<Facturador> lista = em.createQuery(
                    "select f from Facturador f where f.empresa.codigo = :idEmpresa and f.estado = 1")
                    .setParameter("idEmpresa", idEmpresa).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0).getNumDoc();
        } catch (Exception e) { return null; }
    }

    private Long obtenerEmpresaPorReceptor(String rucReceptor) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> lista = em.createQuery(
                    "select f.empresa.codigo from Facturador f where f.numDoc = :ruc and f.estado = 1")
                    .setParameter("ruc", rucReceptor).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) { return null; }
    }

    private DocumentoCxp buscarDocumentoPorClaveAcceso(String claveAcceso) {
        try {
            @SuppressWarnings("unchecked")
            List<DocumentoCxp> lista = em.createQuery(
                    "select d from DocumentoCxp d where d.claveAcceso = :clave")
                    .setParameter("clave", claveAcceso).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) { return null; }
    }

    /**
     * Resuelve el período contable para una fecha de emisión dada.
     * Busca en CNT.PRDO el período cuyo mes y año coinciden con la fecha de emisión
     * y que pertenezca a la empresa indicada.
     * Retorna null si no encuentra un período.
     */
    private Periodo resolverPeriodoPorFecha(LocalDateTime fechaEmision, Long idEmpresa) {
        if (fechaEmision == null || idEmpresa == null) return null;
        try {
            @SuppressWarnings("unchecked")
            List<Periodo> lista = em.createNamedQuery("PeriodoByEmpresaMesAnio")
                    .setParameter("idEmpresa", idEmpresa)
                    .setParameter("mes", (long) fechaEmision.getMonthValue())
                    .setParameter("anio", (long) fechaEmision.getYear())
                    .setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) {
            System.out.println("WARN resolverPeriodoPorFecha: " + e.getMessage());
            return null;
        }
    }

    /**
     * Detecta documentos del mismo período que estaban activos (no REVERTIDOS) en
     * cargas anteriores y NO aparecen en la carga actual (clavesEnEstaCarga).
     * Los marca como NOVEDAD con motivo "DESAPARECIDO_EN_CARGA".
     * Registra una DetalleCargaTxt con resultado "DESAPARECIDO" para cada uno.
     *
     * @return cantidad de documentos desaparecidos detectados
     */
    @SuppressWarnings("unchecked")
    private long detectarDocumentosDesaparecidos(Long idEmpresa, Periodo periodo,
            java.util.Set<String> clavesEnEstaCarga, java.util.Set<String> tiposEnEstaCarga,
            CargaArchivoTxt cabecera, List<Map<String, Object>> desaparecidosDetalle) {
        long count = 0;
        try {
            // Buscar TODOS los documentos activos del período del mismo tipo
            // (incluye estado 3 para reportarlos como REGISTRADO_DESAPARECIDO)
            List<DocumentoCxp> activosDelPeriodo = em.createQuery(
                    "SELECT e FROM DocumentoCxp e "
                    + "WHERE e.empresa.codigo = :idEmpresa "
                    + "AND e.periodoContable.codigo = :idPeriodo "
                    + "AND e.tipoComprobante IN :tipos "
                    + "AND e.estadoDocumento <> 6 "  // excluir solo REVERTIDO
                    + "ORDER BY e.id DESC")
                    .setParameter("idEmpresa", idEmpresa)
                    .setParameter("idPeriodo", periodo.getCodigo())
                    .setParameter("tipos", tiposEnEstaCarga)
                    .getResultList();

            for (DocumentoCxp doc : activosDelPeriodo) {
                if (clavesEnEstaCarga.contains(doc.getClaveAcceso())) continue;

                boolean yaRegistrado = doc.getEstadoDocumento() == ESTADO_REGISTRADO_BD;

                if (yaRegistrado) {
                    // Documento ya procesado con asiento → solo reportar, NO cambiar estado
                    String motivo = "REGISTRADO_DESAPARECIDO: Documento ya registrado en BD con asiento contable "
                            + "pero no apareció en la carga " + cabecera.getId()
                            + " del período " + periodo.getCodigo()
                            + ". Verificar con el proveedor o el SRI.";

                    DetalleCargaTxt linea = new DetalleCargaTxt();
                    linea.setCargaTxt(cabecera);
                    linea.setDocumento(doc);
                    linea.setResultado((long) ResultadoCargaTxt.REGISTRADO_DESAPARECIDO);
                    linea.setObservacion(motivo);
                    try {
                        detalleCargaTxtDaoService.save(linea, null);
                    } catch (Throwable e) {
                        System.out.println("WARN detectarDesaparecidos (registrado) detalle: " + e.getMessage());
                    }

                    Map<String, Object> d = new HashMap<>();
                    d.put("idDocumentoCxp", doc.getId());
                    d.put("claveAcceso", doc.getClaveAcceso());
                    d.put("serie", doc.getSerieComprobante());
                    d.put("resultado", "REGISTRADO_DESAPARECIDO");
                    d.put("novedad", motivo);
                    desaparecidosDetalle.add(d);
                    // No se incrementa count porque no es una novedad pendiente de acción

                } else {
                    // Documento pendiente de procesar → marcar como NOVEDAD DESAPARECIDO
                    String motivoAnterior = doc.getNovedad() != null ? doc.getNovedad() : "";
                    String motivo = "DESAPARECIDO_EN_CARGA: No apareció en la carga " + cabecera.getId()
                            + " del período " + periodo.getCodigo()
                            + (motivoAnterior.isEmpty() ? "" : " | Novedad previa: " + motivoAnterior);

                    if (doc.getEstadoDocumento() == ESTADO_LEIDO
                            || doc.getEstadoDocumento() == ESTADO_XML_CARGADO
                            || doc.getEstadoDocumento() == ESTADO_NOVEDAD) {
                        doc.setEstadoDocumento(ESTADO_NOVEDAD);
                        doc.setEstadoNovedad(NOVEDAD_PENDIENTE);
                    }
                    doc.setNovedad(motivo);
                    try {
                        documentoCxpDaoService.save(doc, doc.getId());
                    } catch (Throwable e) {
                        System.out.println("WARN detectarDocumentosDesaparecidos save: " + e.getMessage());
                    }

                    DetalleCargaTxt linea = new DetalleCargaTxt();
                    linea.setCargaTxt(cabecera);
                    linea.setDocumento(doc);
                    linea.setResultado((long) ResultadoCargaTxt.DESAPARECIDO);
                    linea.setObservacion(motivo);
                    try {
                        detalleCargaTxtDaoService.save(linea, null);
                    } catch (Throwable e) {
                        System.out.println("WARN detectarDocumentosDesaparecidos detalle: " + e.getMessage());
                    }

                    Map<String, Object> d = new HashMap<>();
                    d.put("idDocumentoCxp", doc.getId());
                    d.put("claveAcceso", doc.getClaveAcceso());
                    d.put("serie", doc.getSerieComprobante());
                    d.put("resultado", "DESAPARECIDO");
                    d.put("novedad", motivo);
                    desaparecidosDetalle.add(d);
                    count++;
                }
            }
        } catch (Exception e) {
            System.out.println("WARN detectarDocumentosDesaparecidos: " + e.getMessage());
        }
        return count;
    }

    private String detectarDiferencias(DocumentoCxp doc, double valorSinImpuestos,
                                        double iva, double importeTotal,
                                        LocalDateTime fechaAutorizacion, LocalDateTime fechaEmision) {
        List<String> diffs = new ArrayList<>();
        if (doc.getValorSinImpuestos() != null && Math.abs(doc.getValorSinImpuestos() - valorSinImpuestos) > 0.001)
            diffs.add("valorSinImpuestos: previo=" + doc.getValorSinImpuestos() + " nuevo=" + valorSinImpuestos);
        if (doc.getIva() != null && Math.abs(doc.getIva() - iva) > 0.001)
            diffs.add("iva: previo=" + doc.getIva() + " nuevo=" + iva);
        if (doc.getImporteTotal() != null && Math.abs(doc.getImporteTotal() - importeTotal) > 0.001)
            diffs.add("importeTotal: previo=" + doc.getImporteTotal() + " nuevo=" + importeTotal);
        if (fechaEmision != null && doc.getFechaEmision() != null && !doc.getFechaEmision().equals(fechaEmision))
            diffs.add("fechaEmision: previo=" + doc.getFechaEmision() + " nuevo=" + fechaEmision);
        return String.join(" | ", diffs);
    }

    @SuppressWarnings("unused")
	private Map<String, Object> errorTitular(String codigoError, String ruc, Long idTitular) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", codigoError);
        if ("TITULAR_NO_ENCONTRADO".equals(codigoError))
            error.put("mensaje", "El emisor con RUC " + ruc + " no existe en TSR. Créelo como Proveedor.");
        else
            error.put("mensaje", "El titular con RUC " + ruc + " existe en TSR pero NO tiene rol de Proveedor.");
        error.put("rucEmisor", ruc);
        if (idTitular != null) error.put("idTitular", idTitular);
        return error;
    }

    private Titular buscarTitularPorRuc(String ruc) {
        try {
            @SuppressWarnings("unchecked")
            List<Titular> lista = em.createQuery(
                    "select t from Titular t where t.identificacion = :ruc")
                    .setParameter("ruc", ruc).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) { return null; }
    }

    /**
     * Busca un Titular con el RUC dado. Si no existe lo crea con rol de Proveedor.
     * Si existe pero no tiene rol de Proveedor, se lo asigna y lo actualiza en BD.
     * <p>
     * Datos que se toman del XML para crear el Titular (en orden de preferencia):
     * <ul>
     *   <li>{@code razonSocial} — campo razonSocial del infoTributaria (personas jurídicas)</li>
     *   <li>{@code nombreComercial} — si razonSocial está vacío</li>
     *   <li>{@code razonSocialEmisor} — del DocumentoCxp (venido del TXT), como último recurso</li>
     * </ul>
     * El tipo de identificación se detecta por la longitud del RUC:
     * <ul>
     *   <li>13 dígitos → RUC (rubroTipoIdentificacionH = 2)</li>
     *   <li>10 dígitos → Cédula (rubroTipoIdentificacionH = 1)</li>
     *   <li>otro → Pasaporte/Exterior (rubroTipoIdentificacionH = 3)</li>
     * </ul>
     *
     * @param ruc              Número de identificación del emisor
     * @param razonSocialTxt   Razón social venida del TXT (fallback si el XML no la tiene)
     * @param xmlDoc           Document XML ya parseado del comprobante
     * @param idUsuario        ID del usuario que está procesando
     * @return Titular existente o recién creado, siempre con tipoProveedor = 1
     */
    private Titular obtenerOAutoCrearProveedor(String ruc, String razonSocialTxt,
                                                Document xmlDoc, Long idUsuario) {
        // 1. Buscar titular existente por RUC/identificación
        Titular titular = buscarTitularPorRuc(ruc);

        if (titular != null) {
            // Existe: verificar si ya tiene rol de Proveedor
            if (!Long.valueOf(1L).equals(titular.getTipoProveedor())) {
                // Asignar rol de Proveedor y actualizar
                titular.setTipoProveedor(1L);
                try {
                    titularDaoService.save(titular, titular.getCodigo());
                    System.out.println("✓ Rol de Proveedor asignado a Titular existente: "
                            + ruc + " | id=" + titular.getCodigo());
                } catch (Throwable e) {
                    System.err.println("⚠ No se pudo asignar rol Proveedor al Titular id="
                            + titular.getCodigo() + ": " + e.getMessage());
                }
            } else {
                System.out.println("ℹ Titular ya tiene rol de Proveedor: " + ruc + " | id=" + titular.getCodigo()
                        + " | nombre=" + titular.getNombre());
            }
            return titular;
        }

        // 2. No existe → crear automáticamente con los datos del XML/TXT
        System.out.println("Auto-creando Titular-Proveedor para RUC: " + ruc);

        // Obtener la razón social del XML (infoTributaria)
        String razonSocial = getXmlValue(xmlDoc, "razonSocial");
        if (razonSocial.isEmpty()) razonSocial = getXmlValue(xmlDoc, "nombreComercial");
        if (razonSocial.isEmpty() && razonSocialTxt != null && !razonSocialTxt.isEmpty())
            razonSocial = razonSocialTxt;
        if (razonSocial.isEmpty()) razonSocial = ruc; // último recurso
        razonSocial = razonSocial.toUpperCase();

        // Datos adicionales del XML si están disponibles
        String telefono  = getXmlValue(xmlDoc, "telefono");
        String email     = getXmlValue(xmlDoc, "correoElectronico");
        String direccion = getXmlValue(xmlDoc, "dirEstablecimiento");
        if (direccion.isEmpty()) direccion = getXmlValue(xmlDoc, "dirMatriz");

        // Determinar tipo de identificación por longitud
        // rubroTipoIdentificacionH: según Rubro 36 del sistema
        //   1 = Cédula (10 dígitos), 2 = RUC (13 dígitos), 3 = Pasaporte/Exterior
        Long tipoIdentif;
        if (ruc != null && ruc.length() == 13) {
            tipoIdentif = 2L; // RUC
        } else if (ruc != null && ruc.length() == 10) {
            tipoIdentif = 1L; // Cédula
        } else {
            tipoIdentif = 3L; // Pasaporte / Exterior
        }

        Titular nuevo = new Titular();
        nuevo.setIdentificacion(ruc);
        nuevo.setNombre(razonSocial);
        nuevo.setRazonSocial(razonSocial);
        nuevo.setTipoProveedor(1L);      // rol Proveedor
        nuevo.setTipoCliente(0L);        // no es cliente por defecto
        nuevo.setEstado(1L);             // activo
        nuevo.setRubroTipoIdentificacionH(tipoIdentif);
        if (!telefono.isEmpty())  nuevo.setTelefono(telefono);
        if (!email.isEmpty())     nuevo.setEmail(email);
        if (!direccion.isEmpty()) nuevo.setDireccion(direccion);

        try {
            nuevo = titularDaoService.save(nuevo, null);
            System.out.println("✓ Titular-Proveedor creado automáticamente: "
                    + ruc + " | " + razonSocial + " | id=" + nuevo.getCodigo());
        } catch (Throwable e) {
            // Si falla el guardado, lanzar excepción para que el documento quede en ERROR
            throw new RuntimeException("Error al auto-crear Titular-Proveedor para RUC "
                    + ruc + ": " + e.getMessage(), e);
        }

        return nuevo;
    }

    /**
     * Lee del disco el XML al que apunta el documento.
     *
     * <p>
     * Comprueba que el archivo esté <b>antes</b> de leerlo, y no por prolijidad.
     * Si se deja que falle {@code Files.readAllBytes}, la
     * {@code NoSuchFileException} llega con la ruta a secas como {@code getMessage()}
     * —sin verbo y sin sujeto—, y eso es exactamente lo que termina copiado en la
     * {@code observacion} del documento: <i>"Error al registrar en BD:
     * C:\Users\...\xxx.xml"</i>. Uno por uno se tolera porque el usuario tiene el
     * contexto delante; en un lote de cincuenta, cincuenta observaciones que solo
     * traen una ruta no le dicen nada a nadie.
     * </p>
     *
     * <p>
     * El caso que lo destapó: documentos cargados a mano en otra máquina, con
     * {@code pathXml} <b>absoluto</b> apuntando a una raíz de subidas que en este
     * servidor no existe. Se reconocen por {@code origenXml} nulo.
     * </p>
     *
     * @param doc        : Documento cuyo XML se quiere leer
     * @return           : Contenido del archivo
     * @throws Exception : Con un mensaje que se explica solo y dice qué hacer
     */
    private String leerArchivoXml(DocumentoCxp doc) throws Exception {

        String referencia = referenciaDocumento(doc);
        String path = doc != null ? doc.getPathXml() : null;

        if (path == null || path.trim().isEmpty())
            throw new Exception("El documento " + referencia + " no tiene registrada la ruta de "
                    + "su XML. Vuelva a subir el archivo desde la pantalla de carga.");

        java.nio.file.Path archivo = java.nio.file.Paths.get(path.trim());

        if (!java.nio.file.Files.exists(archivo))
            throw new Exception("No se encuentra el archivo XML del documento " + referencia
                    + ". La ruta registrada es [" + path + "] y ahí no hay nada: el archivo se "
                    + "guardó en otra ubicación o en otro servidor. Vuelva a subir el XML desde "
                    + "la pantalla de carga para que quede en la raíz de subidas de este servidor.");

        if (!java.nio.file.Files.isReadable(archivo))
            throw new Exception("El archivo XML del documento " + referencia + " existe pero no "
                    + "se puede leer: [" + path + "]. Revise los permisos del archivo, o vuelva "
                    + "a subirlo desde la pantalla de carga.");

        try {
            return new String(java.nio.file.Files.readAllBytes(archivo));
        } catch (java.io.IOException e) {
            throw new Exception("No se pudo leer el archivo XML del documento " + referencia
                    + " [" + path + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Cómo se nombra un documento en un mensaje de error para que el usuario lo
     * reconozca en la grilla: la serie es lo que ve en pantalla, y el id es lo
     * que sirve para buscarlo en la base.
     *
     * @param doc : Documento, puede ser nulo
     * @return    : Texto como {@code "001-002-000012345 (id=901)"}
     */
    private String referenciaDocumento(DocumentoCxp doc) {
        if (doc == null) return "(desconocido)";

        String nombre = doc.getSerieComprobante() != null && !doc.getSerieComprobante().isEmpty()
                ? doc.getSerieComprobante()
                : doc.getClaveAcceso();

        return (nombre != null ? nombre : "sin serie") + " (id=" + doc.getId() + ")";
    }

    private Document parsearXmlComprobante(String xmlCompleto) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document docOuter = builder.parse(new InputSource(new StringReader(xmlCompleto)));
        NodeList comprobantes = docOuter.getElementsByTagName("comprobante");
        if (comprobantes.getLength() > 0) {
            String cdataContent = comprobantes.item(0).getTextContent();
            if (cdataContent != null && !cdataContent.trim().isEmpty())
                return builder.parse(new InputSource(new StringReader(cdataContent.trim())));
        }
        return docOuter;
    }

    /**
     * Lee de &lt;infoAdicional&gt; los valores que la empresa emisora cobra por
     * cuenta de terceros y que NO estan incluidos en el importeTotal del
     * comprobante. Caso tipico: las planillas electricas (CNEL), que
     * recaudan la contribucion al cuerpo de bomberos y la tasa de
     * recoleccion de basura.
     *
     * Ejemplo de lo que trae el XML:
     *   &lt;campoAdicional nombre="CONTRIBUCION BOMBEROS"&gt;2.41&lt;/campoAdicional&gt;
     *   &lt;campoAdicional nombre="TASA RECOLECCION BASURA"&gt;0.00&lt;/campoAdicional&gt;
     *   &lt;campoAdicional nombre="FORMA DE PAGO TERCEROS BASURA Y BOMBEROS"&gt;SIN UTILIZACION...&lt;/campoAdicional&gt;
     *   &lt;campoAdicional nombre="TOTAL FORMA DE PAGO TERCEROS BASURA Y BOMBEROS"&gt;2.41&lt;/campoAdicional&gt;
     *
     * Se toman SOLO los conceptos individuales (bomberos, basura). Los
     * campos "FORMA DE PAGO..." se excluyen a proposito: uno es texto y el
     * otro es el TOTAL de los anteriores — sumarlo duplicaria el valor.
     *
     * @param xmlDoc : Documento XML del comprobante
     * @return       : Lista de [nombre, valor] con valor &gt; 0; vacia si no aplica
     */
    private List<Object[]> leerValoresTerceros(Document xmlDoc) {
        List<Object[]> valores = new ArrayList<>();
        try {
            NodeList campos = xmlDoc.getElementsByTagName("campoAdicional");
            for (int i = 0; i < campos.getLength(); i++) {
                Element campo = (Element) campos.item(i);
                String nombre = campo.getAttribute("nombre");
                String nombreNorm = normalizarParaComparacion(nombre);

                boolean esConceptoTercero = nombreNorm.contains("BOMBERO") || nombreNorm.contains("BASURA");
                // "FORMA DE PAGO TERCEROS..." y "TOTAL FORMA DE PAGO TERCEROS..."
                // no son conceptos, son la forma de pago y el total agregado.
                boolean esFormaPagoOTotal = nombreNorm.contains("FORMA DE PAGO");

                if (esConceptoTercero && !esFormaPagoOTotal) {
                    String texto = (campo.getFirstChild() != null)
                            ? campo.getFirstChild().getNodeValue() : null;
                    double valor = parseDouble(texto != null ? texto.trim() : null);
                    if (valor > 0) {
                        valores.add(new Object[]{ nombre.trim(), valor });
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠ leerValoresTerceros: " + e.getMessage());
        }
        return valores;
    }

    /**
     * Normaliza una cadena para comparación: convierte a mayúsculas y elimina tildes/diacríticos.
     * Ej: "CORPORACIÓN" → "CORPORACION", "Eléctrica" → "ELECTRICA"
     */
	private String normalizarParaComparacion(String s) {
        if (s == null) return "";
        String normalizado = java.text.Normalizer.normalize(s.toUpperCase(),
                java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}", "").trim();
    }

    private String getXmlValueOuter(String xmlCompleto, String tag) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xmlCompleto)));
            NodeList nl = doc.getElementsByTagName(tag);
            if (nl.getLength() > 0 && nl.item(0).getFirstChild() != null)
                return nl.item(0).getFirstChild().getNodeValue().trim();
        } catch (Exception e) { /* ignorar */ }
        return "";
    }

    private String getXmlValue(Document doc, String tag) {
        try {
            NodeList nl = doc.getElementsByTagName(tag);
            if (nl.getLength() > 0 && nl.item(0).getFirstChild() != null)
                return nl.item(0).getFirstChild().getNodeValue().trim();
        } catch (Exception e) { /* ignorar */ }
        return "";
    }

    private String getElementValue(Element el, String tag) {
        try {
            NodeList nl = el.getElementsByTagName(tag);
            if (nl.getLength() > 0 && nl.item(0).getFirstChild() != null)
                return nl.item(0).getFirstChild().getNodeValue().trim();
        } catch (Exception e) { /* ignorar */ }
        return "";
    }

    /**
     * Obtiene un dato del documento sustento correspondiente a UN detalle de
     * retención. El SRI lo ubica en distinto lugar según la versión del
     * comprobante de retención:
     *
     *   v1.x : dentro del propio &lt;impuesto&gt;
     *          &lt;impuesto&gt;...&lt;numDocSustento&gt;001001000000123&lt;/numDocSustento&gt;&lt;/impuesto&gt;
     *
     *   v2.0 : en el &lt;docSustento&gt; que envuelve al &lt;retencion&gt;
     *          &lt;docSustento&gt;&lt;numDocSustento&gt;...&lt;/numDocSustento&gt;
     *              &lt;retenciones&gt;&lt;retencion&gt;...&lt;/retencion&gt;&lt;/retenciones&gt;&lt;/docSustento&gt;
     *
     * Se busca primero dentro del detalle, luego subiendo hasta el
     * &lt;docSustento&gt; que lo contiene, y como último recurso se toma el primer
     * valor del documento (sirve para retenciones de un solo sustento).
     *
     * @param detalle : Elemento del detalle (&lt;impuesto&gt; o &lt;retencion&gt;)
     * @param xmlDoc  : Documento XML completo, para el fallback
     * @param tag     : Nombre del tag a buscar (numDocSustento, codDocSustento, ...)
     * @return        : Valor encontrado, o cadena vacía
     */
    private String getValorDocSustento(Element detalle, Document xmlDoc, String tag) {
        // 1. Dentro del propio detalle (esquema v1)
        String valor = getElementValue(detalle, tag);
        if (valor != null && !valor.isEmpty()) return valor;

        // 2. En el <docSustento> que lo envuelve (esquema v2)
        org.w3c.dom.Node padre = detalle.getParentNode();
        while (padre != null) {
            if (padre instanceof Element && "docSustento".equals(padre.getNodeName())) {
                valor = getElementValue((Element) padre, tag);
                if (valor != null && !valor.isEmpty()) return valor;
                break;
            }
            padre = padre.getParentNode();
        }

        // 3. Primer valor del documento (retención con un solo sustento)
        return getXmlValue(xmlDoc, tag);
    }

    /**
     * Devuelve los elementos de detalle de una retención recibida, tolerando
     * las dos versiones del esquema del SRI: &lt;retencion&gt; (v2) y, si no hay
     * ninguno, &lt;impuesto&gt; (v1). Antes solo se leía &lt;retencion&gt;, por lo que
     * un comprobante v1 no generaba ningún detalle.
     * @param xmlDoc : Documento XML del comprobante
     * @return       : Lista de elementos de detalle
     */
    private NodeList obtenerDetallesRetencion(Document xmlDoc) {
        NodeList nl = xmlDoc.getElementsByTagName("retencion");
        if (nl != null && nl.getLength() > 0) return nl;
        return xmlDoc.getElementsByTagName("impuesto");
    }

    private double parseDouble(String val) {
        try { return val == null || val.isEmpty() ? 0.0 : Double.parseDouble(val.replace(",", ".")); }
        catch (Exception e) { return 0.0; }
    }

    private Long parseLong(String val) {
        try { return val == null || val.isEmpty() ? null : Long.parseLong(val.trim()); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime parseFechaHora(String val) {
        if (val == null || val.isEmpty()) return null;
        String v = val.trim().replaceAll("[+-]\\d{2}:\\d{2}$", "").trim();
        // Intentar primero formatos con fecha Y hora
        String[] formatosConHora = {
            "dd/MM/yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"
        };
        for (String fmt : formatosConHora) {
            try { return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(fmt)); }
            catch (Exception ignored) {}
        }
        // Intentar formatos solo-fecha (sin hora) → convertir a medianoche
        String[] formatosSoloFecha = {
            "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"
        };
        for (String fmt : formatosSoloFecha) {
            try { return LocalDate.parse(v, DateTimeFormatter.ofPattern(fmt)).atStartOfDay(); }
            catch (Exception ignored) {}
        }
        // Último recurso: ISO básico
        try { return LocalDate.parse(v).atStartOfDay(); }
        catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unused")
	private LocalDate parseFecha(String val) {
        if (val == null || val.isEmpty()) return null;
        for (String fmt : new String[]{"dd/MM/yyyy", "yyyy-MM-dd"}) {
            try { return LocalDate.parse(val.trim(), DateTimeFormatter.ofPattern(fmt)); }
            catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Calcula el total de una retención recibida sumando los {@code <valorRetenido>}
     * de sus detalles.
     * <p>
     * No se usa {@code DocumentoCxp.importeTotal} porque el comprobante de
     * retención del SRI no trae {@code <importeTotal>} y la columna
     * IMPORTE_TOTAL del TXT llega en {@code 0.00}: la cabecera quedaba con total
     * cero y la aplicación de pago fallaba con "El monto a aplicar no puede ser
     * cero". La suma de los valores retenidos es además el mismo valor que
     * {@code generarAsientoRetencionCompra(V2)} usa para el lado HABER del
     * asiento, así que cabecera, asiento y aplicación de pago quedan alineados.
     *
     * @param retenciones : Nodos {@code <retencion>} / {@code <impuesto>} del XML
     * @param doc         : Documento CXP, solo para el respaldo de importeTotal
     * @return            : Total retenido, redondeado a 2 decimales
     */
    private double calculaTotalRetenido(NodeList retenciones, DocumentoCxp doc) {
        double total = 0.0;
        for (int i = 0; i < retenciones.getLength(); i++) {
            total += parseDouble(getElementValue((Element) retenciones.item(i), "valorRetenido"));
        }
        // La columna es NUMBER(18,2): se redondea aquí para que el valor en memoria
        // (el que valida la aplicación de pago contra el saldo) sea el mismo que
        // el que queda grabado.
        total = Math.round(total * 100.0) / 100.0;

        if (total == 0.0 && doc.getImporteTotal() != null && doc.getImporteTotal() != 0.0) {
            System.out.println("⚠ La retención " + doc.getSerieComprobante()
                    + " no tiene valores retenidos en el XML; se usa el importe total del TXT: "
                    + doc.getImporteTotal());
            return doc.getImporteTotal();
        }
        System.out.println("Total retenido calculado para " + doc.getSerieComprobante()
                + ": " + total + " (" + retenciones.getLength() + " detalle(s))");
        return total;
    }

    /**
     * Número de la factura de VENTA a la que afecta una retención recibida, para
     * ponerlo en la observación del asiento.
     * <p>
     * Devuelve el número tal como está en CBR.FCTR (con guiones,
     * '001-001-000000784') porque es el que se reconoce en el sistema; el
     * documento sustento del XML llega sin guiones. Si la factura no se puede
     * resolver devuelve el número crudo del sustento, que es mejor que nada: la
     * observación es informativa y no debe hacer fallar el asiento.
     * <p>
     * Ojo con el nombre del campo padre del detalle: es {@code retencion} en V1 y
     * {@code retencionCompraV2} en V2 (columna DRC2.RETENCIONV2).
     *
     * @param tipo      : RETENCION_COMPRA o RETENCION_COMPRA_V2
     * @param idDocBD   : Id de la retención en su tabla
     * @param idEmpresa : Id de la empresa
     * @return          : Número de la factura afectada, null si no se pudo determinar
     */
    private String obtenerFacturaAfectadaRetencion(String tipo, Long idDocBD, Long idEmpresa) {
        String numDocSustento = null;
        try {
            boolean esV2 = "RETENCION_COMPRA_V2".equals(tipo);
            String entidadDetalle = esV2 ? "DetalleRetencionCompraV2" : "DetalleRetencionCompra";
            String campoCabecera  = esV2 ? "retencionCompraV2"        : "retencion";

            @SuppressWarnings("unchecked")
            List<String> numeros = em.createQuery(
                    " select distinct d.numDocReten from " + entidadDetalle + " d " +
                    " where  d." + campoCabecera + ".id = :idRetencion " +
                    " and    d.numDocReten is not null ")
                    .setParameter("idRetencion", idDocBD)
                    .getResultList();

            if (numeros.isEmpty()) {
                System.out.println("ℹ La retención " + tipo + " id=" + idDocBD
                        + " no tiene documento sustento en sus detalles; "
                        + "la observación del asiento va sin número de factura.");
                return null;
            }
            // Con más de un sustento no hay una sola factura que nombrar (el
            // bloqueante RETENCION_MULTIDOCUMENTO lo impide en V2, pero puede
            // haber datos históricos): se listan los números crudos.
            if (numeros.size() > 1) {
                return String.join(", ", numeros);
            }

            numDocSustento = numeros.get(0);
            return aplicacionPagoCxcService
                    .resolverFacturaPorNumero(numDocSustento, null, idEmpresa).getNumero();

        } catch (Throwable e) {
            System.err.println("⚠ No se pudo resolver la factura afectada por la retención "
                    + tipo + " id=" + idDocBD + ": " + e.getMessage()
                    + ". Se usa el número del documento sustento tal como vino en el XML.");
            return numDocSustento;
        }
    }

    private ProductoPago buscarProductoPorNombre(String nombre, Long idEmpresa) {
        if (nombre == null || nombre.trim().isEmpty()) return null;
        try {
            @SuppressWarnings("unchecked")
            List<ProductoPago> lista = em.createQuery(
                    "select p from ProductoPago p where lower(p.nombre) = lower(:nombre) " +
                    "and p.empresa.codigo = :idEmpresa")
                    .setParameter("nombre", nombre.trim())
                    .setParameter("idEmpresa", idEmpresa).setMaxResults(1).getResultList();
            return lista.isEmpty() ? null : lista.get(0);
        } catch (Exception e) { return null; }
    }

    // =========================================================
    // Generación de asiento contable para documentos CXP
    // =========================================================

    /**
     * Intenta generar el asiento contable para un documento CXP recién registrado.
     * <p>
     * Se invoca automáticamente después de que el {@code DocumentoCxp} pasa a estado
     * {@code ESTADO_REGISTRADO_BD (3)}. Si el asiento falla, el error se registra como
     * advertencia en el mapa de resultado pero <b>NO revierte</b> el registro del documento.
     * <p>
     * Condición para generar asiento: la empresa receptora ({@code idEmpresa}) debe tener
     * un {@code Facturador} con {@code generaConta = 1}.
     * <p>
     * TODO — Para activar cada tipo de asiento, implementar el método correspondiente en
     *        {@code AsientoContableService} con la plantilla y auxiliares definidos en BD.
     *
     * @param doc       DocumentoCxp ya en estado REGISTRADO_BD
     * @param resultado Mapa de respuesta donde se añadirán "asiento" o "advertenciaAsiento"
     * @param idEmpresa ID de la empresa receptora (empresa contable)
     */
    private void generarAsientoCxp(DocumentoCxp doc,
                                    Map<String, Object> resultado,
                                    Long idEmpresa) throws Exception {
        try {
            // Verificar si la empresa tiene facturador con generaConta=1
            @SuppressWarnings("unchecked")
            List<Long> lista = em.createQuery(
                    "select f.generaConta from Facturador f " +
                    "where f.empresa.codigo = :idEmpresa and f.estado = 1")
                    .setParameter("idEmpresa", idEmpresa)
                    .setMaxResults(1)
                    .getResultList();

            if (lista.isEmpty() || !Long.valueOf(1L).equals(lista.get(0))) {
                // La empresa no tiene generación contable habilitada → omitir asiento
                return;
            }

            Long idDocBD   = doc.getIdDocumentoBD();
            String tipo    = doc.getTipoTablaDestino();
            String serie   = doc.getSerieComprobante() != null ? doc.getSerieComprobante() : doc.getClaveAcceso();

            // ── Fecha contable: SIEMPRE la fecha de emisión del documento ──────
            // Se toma del documento ya registrado, cuya fecha viene del XML
            // (<fechaEmision>), que es la fuente autoritativa. Como respaldo se
            // usa la del TXT (DocumentoCxp.fechaEmision), que puede venir vacía
            // o con un formato no reconocido.
            // Si no hay ninguna de las dos NO se genera el asiento: antes se
            // caía a la fecha de hoy en silencio, lo que contabilizaba el
            // documento en un período equivocado sin que nadie se enterara.
            java.time.LocalDate fechaDoc = obtenerFechaDocumento(tipo, idDocBD);
            if (fechaDoc == null && doc.getFechaEmision() != null) {
                fechaDoc = doc.getFechaEmision().toLocalDate();
                System.out.println("⚠ El documento " + serie + " no tiene fecha propia; "
                        + "se usa la fecha de emisión del TXT: " + fechaDoc);
            }
            if (fechaDoc == null) {
                throw new Exception("No se pudo determinar la fecha de emisión del documento "
                        + serie + " (tipo " + tipo + "). No se genera el asiento contable para "
                        + "no registrarlo en un período equivocado. Verifique la fecha del "
                        + "documento y vuelva a procesarlo.");
            }
            String emisor  = doc.getRazonSocialEmisor() != null ? doc.getRazonSocialEmisor() : doc.getRucEmisor();

            System.out.println("Generando asiento CXP | tipo=" + tipo
                    + " | idDocBD=" + idDocBD + " | empresa=" + idEmpresa);

            com.saa.model.cnt.Asiento asiento = null;

            // ── Observación del asiento ───────────────────────────────────────
            // En una retención recibida la contraparte NO es un proveedor: es el
            // CLIENTE que nos retuvo sobre una factura de VENTA. El documento
            // entra por la carga de CXP, pero contablemente afecta a CXC, así que
            // la observación debe decir "Cliente" e indicar la factura afectada
            // para que se pueda rastrear el asiento hasta ella.
            boolean esRetencion = "RETENCION_COMPRA".equals(tipo)
                    || "RETENCION_COMPRA_V2".equals(tipo);
            String obsBase = serie + (esRetencion ? " | Cliente: " : " | Proveedor: ") + emisor;
            if (esRetencion) {
                String facturaAfectada = obtenerFacturaAfectadaRetencion(tipo, idDocBD, idEmpresa);
                if (facturaAfectada != null && !facturaAfectada.isEmpty()) {
                    obsBase += " | Factura: " + facturaAfectada;
                }
            }

            if ("FACTURA_COMPRA".equals(tipo)) {
                // Si la factura es reembolso con contabilización pendiente (sin RMBF o descuadre),
                // NO generar el asiento; dejar doc en estado 2 con observación descriptiva.
                FacturaCompra fcCheck = em.find(FacturaCompra.class, idDocBD);
                if (fcCheck != null && fcCheck.getEsReembolso() != null && fcCheck.getEsReembolso() == 1L) {
                    @SuppressWarnings("unchecked")
                    long countRmbf = ((Number) em.createQuery(
                            "select count(r) from ReembolsoFacturaCompra r where r.factura.id = :id and r.estado = 1")
                            .setParameter("id", idDocBD).getSingleResult()).longValue();
                    if (countRmbf == 0) {
                        // Sin sustentos → pendiente de ingreso manual
                        resultado.put("contabilizacionPendiente", true);
                        resultado.put("motivoContabilizacionPendiente",
                                "REEMBOLSO: pendiente ingreso de documentos sustento y contabilizacion");
                        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
                        doc.setObservacion("REEMBOLSO: pendiente ingreso de documentos sustento y contabilizacion");
                        try { documentoCxpDaoService.save(doc, doc.getId()); }
                        catch (Throwable t) { throw new Exception("Error guardando DCXP pendiente: " + t.getMessage(), t); }
                        return;
                    }
                    // Hay sustentos: verificar cuadratura
                    double sumRmbf = ((Number) em.createQuery(
                            "select coalesce(sum(r.total),0) from ReembolsoFacturaCompra r where r.factura.id = :id and r.estado = 1")
                            .setParameter("id", idDocBD).getSingleResult()).doubleValue();
                    double totalFc = fcCheck.getTotal() != null ? fcCheck.getTotal() : 0.0;
                    double dif = Math.abs(sumRmbf - totalFc);
                    if (dif > 0.01) {
                        String motivo = String.format("REEMBOLSO: descuadre de %.2f (sum RMBF=%.2f vs total=%.2f)", dif, sumRmbf, totalFc);
                        resultado.put("contabilizacionPendiente", true);
                        resultado.put("motivoContabilizacionPendiente", motivo);
                        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
                        doc.setObservacion(motivo);
                        try { documentoCxpDaoService.save(doc, doc.getId()); }
                        catch (Throwable t) { throw new Exception("Error guardando DCXP descuadre: " + t.getMessage(), t); }
                        return;
                    }
                }
                try { asiento = asientoContableService.generarAsientoFacturaCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.FACTURAS_COMPRA,
                        fechaDoc, "Factura compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("NOTA_CREDITO_COMPRA".equals(tipo)) {
                // TODO: AuxiliarUno DEBE:  cuenta CxP del proveedor
                // TODO: AuxiliarUno HABER: cuenta de gasto/costo del grupo + cuenta IVA
                try { asiento = asientoContableService.generarAsientoNotaCreditoCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.NOTAS_CREDITO_COMPRA,
                        fechaDoc, "NC compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("NOTA_DEBITO_COMPRA".equals(tipo)) {
                // TODO: AuxiliarUno DEBE:  cuenta de gasto/motivo del débito
                // TODO: AuxiliarUno HABER: cuenta CxP del proveedor
                try { asiento = asientoContableService.generarAsientoNotaDebitoCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.NOTAS_DEBITO_COMPRA,
                        fechaDoc, "ND compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("LIQUIDACION_COMPRA_COMPRA".equals(tipo)) {
                // TODO: AuxiliarUno DEBE:  cuenta de gasto/costo del grupo + IVA compras
                // TODO: AuxiliarUno HABER: cuenta CxP del prestador de servicio
                try { asiento = asientoContableService.generarAsientoLiquidacionCompraCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS,
                        fechaDoc, "Liquidación compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("RETENCION_COMPRA".equals(tipo)) {
                // TODO: AuxiliarUno DEBE:  cuenta CxP del proveedor (monto retenido)
                // TODO: AuxiliarUno HABER: cuenta de retención recibida por código SRI
                try { asiento = asientoContableService.generarAsientoRetencionCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS,
                        fechaDoc, "Retención compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("RETENCION_COMPRA_V2".equals(tipo)) {
                // TODO: AuxiliarUno DEBE:  cuenta CxP del proveedor (monto retenido)
                // TODO: AuxiliarUno HABER: cuenta de retención recibida por código SRI
                try { asiento = asientoContableService.generarAsientoRetencionCompraV2(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2,
                        fechaDoc, "Retención compra V2: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }
            }

            if (asiento != null) {
                resultado.put("asiento", asiento.getNumeroAlterno());
                System.out.println("✓ Asiento CXP generado: " + asiento.getNumeroAlterno()
                        + " | tipo=" + tipo);

                // ── Grabar FK ASIENTO de vuelta en la tabla específica del documento ──
                // Esto completa la trazabilidad: documento → asiento contable
                try {
                    grabarAsientoEnDocumento(tipo, idDocBD, asiento);
                } catch (Exception ex) {
                    System.err.println("⚠ Asiento generado pero no se pudo grabar FK en documento "
                            + tipo + " id=" + idDocBD + ": " + ex.getMessage());
                    resultado.put("advertenciaAsientoFK",
                            "Asiento generado (" + asiento.getNumeroAlterno()
                            + ") pero no se pudo vincular al documento: " + ex.getMessage());
                }

                // ── Registrar el abono/cargo sobre la factura de compra ───────
                // Va en la misma transacción que el asiento: si el pago no se
                // puede registrar, se revierte todo el registro del documento.
                registrarAplicacionPagoCxp(tipo, idDocBD, asiento, idEmpresa, resultado);
            }

        } catch (UnsupportedOperationException uoe) {
            // Stubs aún no implementados → advertencia informativa, no bloquea
            resultado.put("advertenciaAsiento",
                    "El asiento contable aún no está configurado para '"
                    + doc.getTipoTablaDestino() + "': " + uoe.getMessage()
                    + ". Configure la plantilla en Contabilidad → Tipos de Asiento.");
            System.out.println("ℹ Asiento CXP pendiente de configurar para tipo="
                    + doc.getTipoTablaDestino());

        } catch (Exception e) {
            // Error de negocio (asiento descuadrado, cuenta no configurada, etc.)
            // → propagar para revertir toda la transacción (factura + asiento)
            System.err.println("⚠ Error generando asiento CXP tipo=" + doc.getTipoTablaDestino()
                    + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Graba el FK del asiento contable generado de vuelta en la tabla cabecera
     * del documento CXP correspondiente (campo ASIENTO agregado en tarea 1.1).
     * Esto completa la trazabilidad bidireccional: documento ↔ asiento contable.
     */
    /**
     * Registra el abono (nota de crédito) o el cargo (nota de débito) sobre la
     * factura de compra afectada, en la misma transacción del asiento contable.
     * Si el documento no tiene factura afectada o el monto no cuadra, propaga la
     * excepción para que se revierta todo el registro del documento.
     *
     * @param tipo       : Tipo de tabla destino del documento
     * @param idDocBD    : Id del documento en su tabla específica
     * @param asiento    : Asiento contable recién generado
     * @param idEmpresa  : Id de la empresa
     * @param resultado  : Mapa de resultado del proceso, para informar al frontend
     * @throws Exception : Si no se puede registrar la aplicación de pago
     */
    private void registrarAplicacionPagoCxp(String tipo, Long idDocBD,
            com.saa.model.cnt.Asiento asiento, Long idEmpresa,
            Map<String, Object> resultado) throws Exception {
        try {
            if ("NOTA_CREDITO_COMPRA".equals(tipo)) {
                com.saa.model.cxp.NotaCreditoCompra nc =
                        em.find(com.saa.model.cxp.NotaCreditoCompra.class, idDocBD);
                if (nc != null) {
                    aplicacionPagoCxpService.aplicarNotaCredito(nc, asiento, idEmpresa, "SISTEMA");
                    resultado.put("aplicacionPago", "Nota de crédito aplicada a la factura afectada.");
                }
            } else if ("NOTA_DEBITO_COMPRA".equals(tipo)) {
                com.saa.model.cxp.NotaDebitoCompra nd =
                        em.find(com.saa.model.cxp.NotaDebitoCompra.class, idDocBD);
                if (nd != null) {
                    aplicacionPagoCxpService.aplicarNotaDebito(nd, asiento, idEmpresa, "SISTEMA");
                    resultado.put("aplicacionPago", "Nota de débito aplicada a la factura afectada.");
                }
            } else if ("RETENCION_COMPRA".equals(tipo)) {
                // La retención que nos emite el cliente abona una factura de VENTA.
                com.saa.model.cxp.RetencionCompra rc =
                        em.find(com.saa.model.cxp.RetencionCompra.class, idDocBD);
                if (rc != null) {
                    aplicacionPagoCxcService.aplicarRetencionRecibida(rc, asiento, idEmpresa, "SISTEMA");
                    resultado.put("aplicacionPago", "Retención aplicada a la factura de venta afectada.");
                }
            } else if ("RETENCION_COMPRA_V2".equals(tipo)) {
                com.saa.model.cxp.RetencionCompraV2 rc2 =
                        em.find(com.saa.model.cxp.RetencionCompraV2.class, idDocBD);
                if (rc2 != null) {
                    aplicacionPagoCxcService.aplicarRetencionRecibidaV2(rc2, asiento, idEmpresa, "SISTEMA");
                    resultado.put("aplicacionPago", "Retención V2 aplicada a la factura de venta afectada.");
                }
            }
        } catch (Throwable t) {
            System.err.println("⚠ Error registrando la aplicación de pago para " + tipo
                    + " id=" + idDocBD + ": " + t.getMessage());
            throw new Exception(t.getMessage(), t);
        }
    }

    private void grabarAsientoEnDocumento(String tipo, Long idDocBD,
                                           com.saa.model.cnt.Asiento asiento) {
        switch (tipo) {
            case "FACTURA_COMPRA": {
                FacturaCompra fc = em.find(FacturaCompra.class, idDocBD);
                if (fc != null) { fc.setAsiento(asiento); em.merge(fc); }
                break;
            }
            case "NOTA_CREDITO_COMPRA": {
                com.saa.model.cxp.NotaCreditoCompra nc =
                        em.find(com.saa.model.cxp.NotaCreditoCompra.class, idDocBD);
                if (nc != null) { nc.setAsiento(asiento); em.merge(nc); }
                break;
            }
            case "NOTA_DEBITO_COMPRA": {
                com.saa.model.cxp.NotaDebitoCompra nd =
                        em.find(com.saa.model.cxp.NotaDebitoCompra.class, idDocBD);
                if (nd != null) { nd.setAsiento(asiento); em.merge(nd); }
                break;
            }
            case "LIQUIDACION_COMPRA_COMPRA": {
                LiquidacionCompraCompra lq =
                        em.find(LiquidacionCompraCompra.class, idDocBD);
                if (lq != null) { lq.setAsiento(asiento); em.merge(lq); }
                break;
            }
            case "RETENCION_COMPRA": {
                RetencionCompra rc = em.find(RetencionCompra.class, idDocBD);
                if (rc != null) { rc.setAsiento(asiento); em.merge(rc); }
                break;
            }
            case "RETENCION_COMPRA_V2": {
                RetencionCompraV2 rc = em.find(RetencionCompraV2.class, idDocBD);
                if (rc != null) { rc.setAsiento(asiento); em.merge(rc); }
                break;
            }
            default:
                System.out.println("WARN grabarAsientoEnDocumento: tipo desconocido=" + tipo);
        }
        System.out.println("✓ FK ASIENTO grabado en " + tipo + " id=" + idDocBD
                + " → asiento id=" + asiento.getCodigo());
    }
}
