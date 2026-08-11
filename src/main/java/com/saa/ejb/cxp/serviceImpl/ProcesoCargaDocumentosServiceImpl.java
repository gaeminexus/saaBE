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
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.dao.ProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
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
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;
import com.saa.rubros.AccionNovedad;
import com.saa.rubros.Estado;
import com.saa.rubros.EstadoDocumentoCxp;
import com.saa.rubros.EstadoNovedad;
import com.saa.rubros.ResultadoCargaTxt;
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

    @EJB private ProductoPagoDaoService                   productoPagoDaoService;
    @EJB private GrupoProductoPagoDaoService              grupoProductoPagoDaoService;

    @EJB private com.saa.ejb.tsr.dao.TitularDaoService    titularDaoService;
    @EJB private com.saa.ejb.cnt.service.AsientoContableService asientoContableService;

    @EJB private com.saa.ejb.cxp.service.AplicacionPagoCxpService aplicacionPagoCxpService;

    @EJB private com.saa.ejb.cxc.service.AplicacionPagoCxcService aplicacionPagoCxcService;
    @EJB private com.saa.ejb.cnt.service.TipoAsientoService     tipoAsientoService;

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
    @Override
    public Map<String, Object> cargarXmlDocumento(Long idDocumentoCxp, String contenidoXml,
                                                   String pathDestino, Long idUsuario) throws Throwable {

        System.out.println("=== cargarXmlDocumento idDocumentoCxp=" + idDocumentoCxp);

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
        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
        DocumentoCxp docActualizado = documentoCxpDaoService.save(doc, doc.getId());

        resultado.put("valido", true);
        resultado.put("documento", docActualizado);
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
            documentoCxpDaoService.save(doc, doc.getId());

            // ── Generar asiento contable (CXP) ────────────────────────────────
            // Solo si el registro fue exitoso (ESTADO_REGISTRADO_BD).
            // Si el asiento falla NO revertimos el registro — se advierte en el resultado.
            generarAsientoCxp(doc, resultadoBD, idEmpresa);

        } catch (Exception e) {
            doc.setEstadoDocumento(ESTADO_ERROR);
            doc.setObservacion("Error al registrar en BD: " + e.getMessage());
            documentoCxpDaoService.save(doc, doc.getId());
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

        String xmlContent = leerArchivoXml(doc.getPathXml());
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
            documentoCxpDaoService.save(doc, doc.getId());

            // ── Generar asiento contable (CXP) ────────────────────────────────
            // Solo si el registro fue exitoso (ESTADO_REGISTRADO_BD).
            // Si el asiento falla NO revertimos el registro — se advierte en el resultado.
            generarAsientoCxp(doc, resultado, idEmpresa);

        } catch (Exception e) {
            doc.setEstadoDocumento(ESTADO_ERROR);
            doc.setObservacion("Error al registrar en BD: " + e.getMessage());
            documentoCxpDaoService.save(doc, doc.getId());
            throw e;
        }

        return resultado;
    }

    // =========================================================
    // FASE 4: Resolver novedad  →  opera sobre DocumentoCxp
    // =========================================================
    @Override
    public Map<String, Object> resolverNovedad(Long idDocumentoCxp, Integer accion,
                                                String contenidoXml, String pathDestino,
                                                Long idUsuario) throws Throwable {

        System.out.println("=== resolverNovedad idDocumentoCxp=" + idDocumentoCxp + " accion=" + accion);

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

        // ══════════════════════════════════════════════════════════════════
        // PASO 1 — Acciones automáticas (siempre se ejecutan)
        //   · Crear titular si no existe y asignarle rol de proveedor
        //   · Si existe pero no tiene rol proveedor → asignarlo
        //   · Crear grupo POR CLASIFICAR si no existe
        //   · Crear producto dentro de POR CLASIFICAR si no existe
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
        if (generaConta) {
            try {
                Long idTipoAsiento = tipoAsientoService.codigoByAlterno(
                        com.saa.rubros.TipoAsientos.FACTURAS_COMPRA, idEmpresa);
                if (idTipoAsiento == null) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
                    b.put("detalle", "No existe el Tipo de Asiento con código alterno "
                            + com.saa.rubros.TipoAsientos.FACTURAS_COMPRA
                            + " para Facturas de Compra. Configúrelo en Contabilidad → Tipos de Asiento.");
                    bloqueantes.add(b);
                }
            } catch (Throwable e) {
                Map<String, Object> b = new HashMap<>();
                b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
                b.put("detalle", "No existe el Tipo de Asiento para Facturas de Compra (codigoAlterno="
                        + com.saa.rubros.TipoAsientos.FACTURAS_COMPRA
                        + "). Configúrelo en Contabilidad → Tipos de Asiento.");
                bloqueantes.add(b);
            }
        }

        // 2b. Productos en POR_CLASIFICAR o sin cuenta contable en su grupo
        List<String> productosSinClasificar = new ArrayList<>();
        List<String> gruposSinCuenta = new ArrayList<>();

        for (ProductoPago producto : productosDetalle) {
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

        NodeList totalImpuestos = xmlDoc.getElementsByTagName("totalImpuesto");
        if (totalImpuestos.getLength() > 0) {
            Element impEl = (Element) totalImpuestos.item(0);
            factura.setvIVA(parseDouble(getElementValue(impEl, "valor")));
            factura.setpIVA(parseDouble(getElementValue(impEl, "tarifa")));
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

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", factura.getId());
        r.put("tipoTablaDestino", "FACTURA_COMPRA");
        r.put("mensaje", "FacturaCompra registrada correctamente con id=" + factura.getId() + ".");
        r.put("productosPendientes", new ArrayList<>());
        r.put("pendienteClasificacion", false);
        return r;
    }

    /**
     * Verifica si el titular tiene cuenta contable CxP (tipoCuenta=1) asignada para la empresa.
     */
    private boolean verificarCuentaContableProveedor(Long codigoTitular, Long idEmpresa) {
        try {
            List<?> result = em.createQuery(
                    "SELECT pcc FROM PersonaCuentaContable pcc "
                    + "JOIN pcc.personaRol pr "
                    + "WHERE pr.titular.codigo = :titular "
                    + "AND pcc.tipoCuenta = 1 "
                    + "AND pcc.empresa.codigo = :empresa")
                    .setParameter("titular", codigoTitular)
                    .setParameter("empresa", idEmpresa)
                    .setMaxResults(1).getResultList();
            return !result.isEmpty();
        } catch (Exception e) {
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


    private Map<String, Object> registrarNotaCreditoCompra(DocumentoCxp doc, String xmlContent,
                                                            Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

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
        nc.setNumDocModificado(getXmlValue(xmlDoc, "numDocModificado"));
        nc.setFechaEmisionDM(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionDocSustento")));
        nc.setSubtotal(parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos")));
        nc.setTotal(parseDouble(getXmlValue(xmlDoc, "importeTotal")));
        nc.setTitular(titular);
        nc.setUsuario(usuario);
        nc.setEstado(Long.valueOf(Estado.ACTIVO));
        nc.setEstadoEmision(2L);

        // BLOQUEANTE: la factura de compra afectada debe existir en el sistema.
        // Sin ella no se puede registrar el abono ni generar la contabilidad, así
        // que se aborta antes de grabar nada.
        aplicacionPagoCxpService.resolverFacturaCompraPorNumero(
                nc.getNumDocModificado(),
                (titular != null ? titular.getCodigo() : null), idEmpresa);

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

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", nc.getId());
        r.put("tipoTablaDestino", "NOTA_CREDITO_COMPRA");
        r.put("mensaje", "NotaCreditoCompra registrada con id=" + nc.getId());
        return r;
    }

    private Map<String, Object> registrarNotaDebitoCompra(DocumentoCxp doc, String xmlContent,
                                                           Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

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
        nd.setNumDocModificado(getXmlValue(xmlDoc, "numDocModificado"));
        nd.setFechaEmisionDM(parseFechaHora(getXmlValue(xmlDoc, "fechaEmisionDocSustento")));
        nd.setSubtotal(parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos")));
        nd.setTotal(parseDouble(getXmlValue(xmlDoc, "importeTotal")));
        nd.setTitular(titular);
        nd.setUsuario(usuario);
        nd.setEstado(Long.valueOf(Estado.ACTIVO));
        nd.setEstadoEmision(2L);

        // BLOQUEANTE: la factura de compra afectada debe existir en el sistema.
        aplicacionPagoCxpService.resolverFacturaCompraPorNumero(
                nd.getNumDocModificado(),
                (titular != null ? titular.getCodigo() : null), idEmpresa);

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

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", nd.getId());
        r.put("tipoTablaDestino", "NOTA_DEBITO_COMPRA");
        r.put("mensaje", "NotaDebitoCompra registrada con id=" + nd.getId());
        return r;
    }

    private Map<String, Object> registrarLiquidacionCompraCompra(DocumentoCxp doc, String xmlContent,
                                                                  Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

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

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", lq.getId());
        r.put("tipoTablaDestino", "LIQUIDACION_COMPRA_COMPRA");
        r.put("mensaje", "LiquidacionCompraCompra registrada con id=" + lq.getId());
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

        // 2b. Tipo de asiento para Retenciones de Compra configurado en BD
        try {
            Long idTipoAsiento = tipoAsientoService.codigoByAlterno(
                    com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS, idEmpresa);
            if (idTipoAsiento == null) {
                Map<String, Object> b = new HashMap<>();
                b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
                b.put("detalle", "No existe el Tipo de Asiento con código alterno "
                        + com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS
                        + " para Retenciones de Compra. Configúrelo en Contabilidad → Tipos de Asiento.");
                bloqueantes.add(b);
            }
        } catch (Throwable e) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
            b.put("detalle", "No existe el Tipo de Asiento para Retenciones de Compra (codigoAlterno="
                    + com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS
                    + "). Configúrelo en Contabilidad → Tipos de Asiento.");
            bloqueantes.add(b);
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

        // 2d. Verificar documento sustento en CXC — SOLO ADVERTENCIA, no bloquea el proceso
        String advertenciaDocSustento = null;
        if (!numAutDocSustento.isEmpty() || !numDocSustento.isEmpty()) {
            boolean docSustentoEncontrado = false;
            try {
                if (!numAutDocSustento.isEmpty()) {
                    Long cnt = (Long) em.createQuery(
                            "SELECT COUNT(f) FROM Factura f "
                            + "WHERE f.autorizacion = :val AND f.estado = 1")
                            .setParameter("val", numAutDocSustento)
                            .getSingleResult();
                    docSustentoEncontrado = cnt != null && cnt > 0;
                }
                if (!docSustentoEncontrado && !numDocSustento.isEmpty()) {
                    Long cnt = (Long) em.createQuery(
                            "SELECT COUNT(f) FROM Factura f "
                            + "WHERE f.numero = :val AND f.estado = 1")
                            .setParameter("val", numDocSustento)
                            .getSingleResult();
                    docSustentoEncontrado = cnt != null && cnt > 0;
                }
            } catch (Exception ex) {
                // No se silencia: si no se pudo verificar, se trata como no encontrado
                // para no registrar una retención cuyo cobro no se puede aplicar.
                System.err.println("⚠ Error al verificar documento sustento en CXC: " + ex.getMessage());
                docSustentoEncontrado = false;
            }
            if (!docSustentoEncontrado) {
                // BLOQUEANTE: la retención abona una factura de venta. Sin la factura
                // no se puede registrar el cobro ni generar la contabilidad.
                advertenciaDocSustento = "El documento sustento no fue encontrado en CXC. "
                        + "Autorización: '" + numAutDocSustento + "' | Número: '" + numDocSustento + "'.";
                Map<String, Object> b = new HashMap<>();
                b.put("tipo", "FACTURA_VENTA_NO_ENCONTRADA");
                b.put("mensaje", "No existe en el sistema la factura de venta a la que afecta esta "
                        + "retención. Número: '" + numDocSustento + "' | Autorización: '"
                        + numAutDocSustento + "'. Emita o cargue primero la factura.");
                b.put("solucion", "Verifique que la factura de venta exista y esté activa en CXC.");
                bloqueantes.add(b);
                System.out.println("⚠ BLOQUEANTE doc sustento: " + advertenciaDocSustento);
            }
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
        rc.setTotal(doc.getImporteTotal());
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
        if (advertenciaDocSustento != null) r.put("advertenciaDocSustento", advertenciaDocSustento);
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

        // 2b. Tipo de asiento para Retenciones de Compra V2 configurado en BD
        try {
            Long idTipoAsiento = tipoAsientoService.codigoByAlterno(
                    com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2, idEmpresa);
            if (idTipoAsiento == null) {
                Map<String, Object> b = new HashMap<>();
                b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
                b.put("detalle", "No existe el Tipo de Asiento con código alterno "
                        + com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2
                        + " para Retenciones de Compra V2. Configúrelo en Contabilidad → Tipos de Asiento.");
                bloqueantes.add(b);
            }
        } catch (Throwable e) {
            Map<String, Object> b = new HashMap<>();
            b.put("tipo", "TIPO_ASIENTO_NO_CONFIGURADO");
            b.put("detalle", "No existe el Tipo de Asiento para Retenciones de Compra V2 (codigoAlterno="
                    + com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS_V2
                    + "). Configúrelo en Contabilidad → Tipos de Asiento.");
            bloqueantes.add(b);
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

        // 2d. Verificar documento sustento en CXC — SOLO ADVERTENCIA, no bloquea el proceso
        String advertenciaDocSustento = null;
        if (!numAutDocSustento.isEmpty() || !numDocSustento.isEmpty()) {
            boolean docSustentoEncontrado = false;
            try {
                if (!numAutDocSustento.isEmpty()) {
                    Long cnt = (Long) em.createQuery(
                            "SELECT COUNT(f) FROM Factura f "
                            + "WHERE f.autorizacion = :val AND f.estado = 1")
                            .setParameter("val", numAutDocSustento)
                            .getSingleResult();
                    docSustentoEncontrado = cnt != null && cnt > 0;
                }
                if (!docSustentoEncontrado && !numDocSustento.isEmpty()) {
                    Long cnt = (Long) em.createQuery(
                            "SELECT COUNT(f) FROM Factura f "
                            + "WHERE f.numero = :val AND f.estado = 1")
                            .setParameter("val", numDocSustento)
                            .getSingleResult();
                    docSustentoEncontrado = cnt != null && cnt > 0;
                }
            } catch (Exception ex) {
                System.err.println("⚠ Error al verificar documento sustento en CXC: " + ex.getMessage());
                docSustentoEncontrado = true;
            }
            if (!docSustentoEncontrado) {
                advertenciaDocSustento = "El documento sustento no fue encontrado en CXC. "
                        + "Autorización: '" + numAutDocSustento + "' | Número: '" + numDocSustento + "'.";
                System.out.println("⚠ ADVERTENCIA doc sustento V2: " + advertenciaDocSustento);
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
        rc.setTotal(doc.getImporteTotal());
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
        if (advertenciaDocSustento != null) r.put("advertenciaDocSustento", advertenciaDocSustento);
        return r;
    }

    // =========================================================
    // Reversión de registros en tablas destino
    // =========================================================
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

    private String leerArchivoXml(String path) throws Exception {
        if (path == null || path.isEmpty()) throw new Exception("El path del XML es nulo o vacío.");
        return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(path)));
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
     * Normaliza una cadena para comparación: convierte a mayúsculas y elimina tildes/diacríticos.
     * Ej: "CORPORACIÓN" → "CORPORACION", "Eléctrica" → "ELECTRICA"
     */
    @SuppressWarnings("unused")
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
            java.time.LocalDate fechaDoc = doc.getFechaEmision() != null
                    ? doc.getFechaEmision().toLocalDate() : java.time.LocalDate.now();
            String serie   = doc.getSerieComprobante() != null ? doc.getSerieComprobante() : doc.getClaveAcceso();
            String emisor  = doc.getRazonSocialEmisor() != null ? doc.getRazonSocialEmisor() : doc.getRucEmisor();

            System.out.println("Generando asiento CXP | tipo=" + tipo
                    + " | idDocBD=" + idDocBD + " | empresa=" + idEmpresa);

            com.saa.model.cnt.Asiento asiento = null;
            String obsBase = serie + " | Proveedor: " + emisor;

            if ("FACTURA_COMPRA".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.FACTURAS_COMPRA con el codigoAlterno
                //       correcto una vez que se defina la plantilla en BD.
                // TODO: AuxiliarUno DEBE:  cuenta de gasto/costo del grupo de producto (GrupoProductoPago.planCuenta)
                //                          + cuenta de IVA en compras
                // TODO: AuxiliarUno HABER: cuenta CxP del proveedor
                try { asiento = asientoContableService.generarAsientoFacturaCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.FACTURAS_COMPRA,
                        fechaDoc, "Factura compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("NOTA_CREDITO_COMPRA".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.NOTAS_CREDITO_COMPRA con el codigoAlterno correcto.
                // TODO: AuxiliarUno DEBE:  cuenta CxP del proveedor
                // TODO: AuxiliarUno HABER: cuenta de gasto/costo del grupo + cuenta IVA
                try { asiento = asientoContableService.generarAsientoNotaCreditoCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.NOTAS_CREDITO_COMPRA,
                        fechaDoc, "NC compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("NOTA_DEBITO_COMPRA".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.NOTAS_DEBITO_COMPRA con el codigoAlterno correcto.
                // TODO: AuxiliarUno DEBE:  cuenta de gasto/motivo del débito
                // TODO: AuxiliarUno HABER: cuenta CxP del proveedor
                try { asiento = asientoContableService.generarAsientoNotaDebitoCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.NOTAS_DEBITO_COMPRA,
                        fechaDoc, "ND compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("LIQUIDACION_COMPRA_COMPRA".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS con el codigoAlterno correcto.
                // TODO: AuxiliarUno DEBE:  cuenta de gasto/costo del grupo + IVA compras
                // TODO: AuxiliarUno HABER: cuenta CxP del prestador de servicio
                try { asiento = asientoContableService.generarAsientoLiquidacionCompraCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.LIQUIDACIONES_COMPRA_RECIBIDAS,
                        fechaDoc, "Liquidación compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("RETENCION_COMPRA".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.RETENCIONES_RECIBIDAS con el codigoAlterno correcto.
                // TODO: AuxiliarUno DEBE:  cuenta CxP del proveedor (monto retenido)
                // TODO: AuxiliarUno HABER: cuenta de retención recibida por código SRI
                try { asiento = asientoContableService.generarAsientoRetencionCompra(
                        idDocBD, idEmpresa,
                        com.saa.rubros.TipoAsientos.RETENCIONES_RECIBIDAS,
                        fechaDoc, "Retención compra: " + obsBase, "SISTEMA"); }
                catch (UnsupportedOperationException uoe) { throw uoe; }
                catch (Throwable t) { throw new Exception(t.getMessage(), t); }

            } else if ("RETENCION_COMPRA_V2".equals(tipo)) {
                // TODO: Reemplazar TipoAsientos.RETENCIONES_RECIBIDAS_V2 con el codigoAlterno correcto.
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
