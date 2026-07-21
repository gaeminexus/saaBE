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
import com.saa.rubros.Estado;
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

    // -------------------------------------------------------
    // Estados
    // -------------------------------------------------------
    private static final long ESTADO_LEIDO         = 1L;
    private static final long ESTADO_XML_CARGADO   = 2L;
    private static final long ESTADO_REGISTRADO_BD = 3L;
    private static final long ESTADO_ERROR         = 4L;
    private static final long ESTADO_NOVEDAD       = 5L;
    private static final long ESTADO_REVERTIDO     = 6L;

    private static final long NOVEDAD_PENDIENTE    = 1L;
    private static final long NOVEDAD_REEMPLAZADO  = 2L;
    private static final long NOVEDAD_MANTENIDO    = 3L;

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
                                                 Long idEmpresa, Long idUsuario) throws Throwable {

        System.out.println("=== INICIO cargarArchivoTxt === archivo: " + nombreArchivo);

        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        // Obtener RUC del receptor desde el facturador de la empresa
        String rucReceptor = obtenerRucReceptor(idEmpresa);

        // Crear cabecera de carga
        CargaArchivoTxt cabecera = new CargaArchivoTxt();
        cabecera.setEmpresa(empresa);
        cabecera.setUsuario(usuario);
        cabecera.setFechaCarga(LocalDateTime.now());
        cabecera.setNombreArchivo(nombreArchivo);
        cabecera.setEstado(1L);
        cabecera = cargaArchivoTxtDaoService.save(cabecera, null);

        long totalRegistros = 0, nuevos = 0, duplicados = 0, novedades = 0;
        List<Map<String, Object>> detallesResultado = new ArrayList<>();

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
            String razonSocial               = cols[1].trim();
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
            LocalDate fechaEmision          = parseFecha(fechaEmisionStr);

            Map<String, Object> r = new HashMap<>();
            r.put("linea", i + 1);
            r.put("serie", serie);
            r.put("claveAcceso", claveAcceso);

            // Validar receptor
            if (rucReceptor != null && !identificacionReceptor.equals(rucReceptor)) {
                r.put("resultado", "IGNORADO - receptor no coincide con empresa");
                detallesResultado.add(r);
                totalRegistros--;
                continue;
            }

            // ── Buscar DocumentoCxp único por claveAcceso ──
            DocumentoCxp doc = buscarDocumentoPorClaveAcceso(claveAcceso);

            String resultadoLinea;
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
                doc = documentoCxpDaoService.save(doc, null);
                nuevos++;
                resultadoLinea = "NUEVO";
            } else {
                // Ya existe: comparar valores
                String diferencias = detectarDiferencias(doc, valorSinImpuestos, iva, importeTotal,
                        fechaAutorizacion, fechaEmision);

                if (diferencias.isEmpty()) {
                    // Duplicado sin diferencias: solo registramos la línea, no tocamos el documento
                    duplicados++;
                    resultadoLinea = "DUPLICADO";
                    observacionLinea = "Documento ya existía sin diferencias.";
                } else {
                    // Novedad: el documento existe pero los valores cambiaron
                    // Solo actualizar si el documento no fue ya procesado en BD o si fue revertido
                    if (doc.getEstadoDocumento() == ESTADO_REGISTRADO_BD) {
                        // Marcar novedad sobre documento ya procesado
                        doc.setEstadoDocumento(ESTADO_NOVEDAD);
                        doc.setEstadoNovedad(NOVEDAD_PENDIENTE);
                        doc.setNovedad(diferencias);
                        documentoCxpDaoService.save(doc, doc.getId());
                    } else if (doc.getEstadoDocumento() == ESTADO_LEIDO
                            || doc.getEstadoDocumento() == ESTADO_XML_CARGADO
                            || doc.getEstadoDocumento() == ESTADO_REVERTIDO) {
                        // Actualizar los valores del documento con los nuevos
                        doc.setValorSinImpuestos(valorSinImpuestos);
                        doc.setIva(iva);
                        doc.setImporteTotal(importeTotal);
                        doc.setFechaAutorizacion(fechaAutorizacion);
                        doc.setFechaEmision(fechaEmision);
                        doc.setEstadoDocumento(ESTADO_LEIDO);
                        doc.setNovedad(diferencias);
                        documentoCxpDaoService.save(doc, doc.getId());
                    }
                    novedades++;
                    resultadoLinea = "NOVEDAD";
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

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("idCargaTxt", cabecera.getId());
        resultado.put("nombreArchivo", nombreArchivo);
        resultado.put("totalRegistros", totalRegistros);
        resultado.put("nuevos", nuevos);
        resultado.put("duplicados", duplicados);
        resultado.put("novedades", novedades);
        resultado.put("detalles", detallesResultado);

        System.out.println("=== FIN cargarArchivoTxt === nuevos=" + nuevos
                + " duplicados=" + duplicados + " novedades=" + novedades);
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
        List<String> errores = validarXmlContraDocumento(contenidoXml, doc);

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
    private List<String> validarXmlContraDocumento(String contenidoXml, DocumentoCxp doc) {
        List<String> errores = new ArrayList<>();
        try {
            Document xmlDoc = parsearXmlComprobante(contenidoXml);

            // ── 1. Clave de acceso ──
            String claveXml = getXmlValueOuter(contenidoXml, "claveAccesoConsultada");
            if (claveXml.isEmpty()) claveXml = getXmlValueOuter(contenidoXml, "claveAcceso");
            if (claveXml.isEmpty()) claveXml = getXmlValue(xmlDoc, "claveAcceso");
            if (!claveXml.isEmpty() && !claveXml.equals(doc.getClaveAcceso())) {
                errores.add("claveAcceso: esperada=" + doc.getClaveAcceso()
                        + " | en XML=" + claveXml);
            }

            // ── 2. RUC del emisor ──
            String rucXml = getXmlValue(xmlDoc, "ruc");
            if (rucXml.isEmpty()) rucXml = getXmlValue(xmlDoc, "rucEmisor");
            if (!rucXml.isEmpty() && !rucXml.equals(doc.getRucEmisor())) {
                errores.add("rucEmisor: esperado=" + doc.getRucEmisor()
                        + " | en XML=" + rucXml);
            }

            // ── 3. Razón social del emisor ──
            String razonXml = getXmlValue(xmlDoc, "razonSocial");
            if (!razonXml.isEmpty() && doc.getRazonSocialEmisor() != null
                    && !razonXml.equalsIgnoreCase(doc.getRazonSocialEmisor())) {
                errores.add("razonSocialEmisor: esperada=\"" + doc.getRazonSocialEmisor()
                        + "\" | en XML=\"" + razonXml + "\"");
            }

            // ── 4. Número de serie / comprobante ──
            String estab      = getXmlValue(xmlDoc, "estab");
            String ptoEmi     = getXmlValue(xmlDoc, "ptoEmi");
            String secuencial = getXmlValue(xmlDoc, "secuencial");
            if (!estab.isEmpty() && !ptoEmi.isEmpty() && !secuencial.isEmpty()) {
                String serieXml = estab + "-" + ptoEmi + "-" + secuencial;
                if (doc.getSerieComprobante() != null
                        && !serieXml.equals(doc.getSerieComprobante())) {
                    errores.add("serieComprobante: esperada=" + doc.getSerieComprobante()
                            + " | en XML=" + serieXml);
                }
            }

            // ── 5. Valor sin impuestos ──
            String totalSinImpStr = getXmlValue(xmlDoc, "totalSinImpuestos");
            if (!totalSinImpStr.isEmpty()) {
                double totalSinImpXml = parseDouble(totalSinImpStr);
                if (doc.getValorSinImpuestos() != null
                        && Math.abs(totalSinImpXml - doc.getValorSinImpuestos()) > 0.01) {
                    errores.add("valorSinImpuestos: esperado=" + doc.getValorSinImpuestos()
                            + " | en XML=" + totalSinImpXml);
                }
            }

            // ── 6. Importe total ──
            String importeTotalStr = getXmlValue(xmlDoc, "importeTotal");
            if (!importeTotalStr.isEmpty()) {
                double importeTotalXml = parseDouble(importeTotalStr);
                if (doc.getImporteTotal() != null
                        && Math.abs(importeTotalXml - doc.getImporteTotal()) > 0.01) {
                    errores.add("importeTotal: esperado=" + doc.getImporteTotal()
                            + " | en XML=" + importeTotalXml);
                }
            }

            // ── 7. IVA (primer impuesto encontrado) ──
            NodeList impuestos = xmlDoc.getElementsByTagName("totalImpuesto");
            if (impuestos.getLength() > 0) {
                double ivaXml = 0.0;
                for (int i = 0; i < impuestos.getLength(); i++) {
                    Element imp = (Element) impuestos.item(i);
                    String codigoImp = getElementValue(imp, "codigo");
                    // Código 2 = IVA en el formato SRI
                    if ("2".equals(codigoImp)) {
                        ivaXml += parseDouble(getElementValue(imp, "valor"));
                    }
                }
                if (doc.getIva() != null && Math.abs(ivaXml - doc.getIva()) > 0.01) {
                    errores.add("iva: esperado=" + doc.getIva()
                            + " | en XML=" + ivaXml);
                }
            }

        } catch (Exception e) {
            errores.add("Error al parsear el XML: " + e.getMessage());
        }
        return errores;
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
        List<String> errores = validarXmlContraDocumento(contenidoXml, doc);
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
            } else if (TIPO_RETENCION.equalsIgnoreCase(tipo)) {
                resultadoBD = registrarRetencionCompra(doc, contenidoXml, idEmpresa, idUsuario);
            } else if (TIPO_RETENCION_V2.equalsIgnoreCase(tipo)) {
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
        if (existente != null) return existente;

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
            } else if (TIPO_RETENCION.equalsIgnoreCase(tipo)) {
                resultado = registrarRetencionCompra(doc, xmlContent, idEmpresa, idUsuario);
            } else if (TIPO_RETENCION_V2.equalsIgnoreCase(tipo)) {
                resultado = registrarRetencionCompraV2(doc, xmlContent, idEmpresa, idUsuario);
            } else {
                throw new Exception("Tipo de comprobante no soportado: " + tipo);
            }

            // Si requiere productos, retornar sin actualizar estado
            if (Boolean.TRUE.equals(resultado.get("requiereProductos"))) {
                return resultado;
            }

            doc.setIdDocumentoBD((Long) resultado.get("idDocumentoBD"));
            doc.setTipoTablaDestino((String) resultado.get("tipoTablaDestino"));
            doc.setFechaRegistroBD(LocalDateTime.now());
            doc.setUsuarioRegistroBD(em.find(Usuario.class, idUsuario));
            doc.setEstadoDocumento(ESTADO_REGISTRADO_BD);
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
    public Map<String, Object> resolverNovedad(Long idDocumentoCxp, String accion,
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

        if ("REEMPLAZAR".equalsIgnoreCase(accion)) {
            // Si tenía registros en BD, revertirlos
            if (doc.getIdDocumentoBD() != null && doc.getEstadoDocumento() != null) {
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
            resultado.put("accion", "REEMPLAZADO");

        } else if ("MANTENER".equalsIgnoreCase(accion)) {
            doc.setEstadoNovedad(NOVEDAD_MANTENIDO);
            doc.setObservacion("Usuario decidió mantener el documento previo.");
            documentoCxpDaoService.save(doc, doc.getId());
            resultado.put("accion", "MANTENIDO");
            resultado.put("mensaje", "Se mantiene el documento sin cambios.");
        } else {
            throw new Exception("Acción no válida. Use REEMPLAZAR o MANTENER.");
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

        Titular titular = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

        String numeroAutorizacion = getXmlValueOuter(xmlContent, "numeroAutorizacion");
        if (numeroAutorizacion.isEmpty()) numeroAutorizacion = doc.getClaveAcceso();
        String fechaAutorizacionStr = getXmlValueOuter(xmlContent, "fechaAutorizacion");

        String codDoc      = getXmlValue(xmlDoc, "codDoc");
        String estab       = getXmlValue(xmlDoc, "estab");
        String ptoEmi      = getXmlValue(xmlDoc, "ptoEmi");
        String secuencial  = getXmlValue(xmlDoc, "secuencial");
        String ambienteStr = getXmlValue(xmlDoc, "ambiente");
        String fechaEmisionStr = getXmlValue(xmlDoc, "fechaEmision");
        double totalSinImp = parseDouble(getXmlValue(xmlDoc, "totalSinImpuestos"));
        double totalDescuento = parseDouble(getXmlValue(xmlDoc, "totalDescuento"));
        double importeTotal = parseDouble(getXmlValue(xmlDoc, "importeTotal"));

        // Iterar detalles: auto-crear productos faltantes en grupo PENDIENTE DE CLASIFICAR
        NodeList detallesXml = xmlDoc.getElementsByTagName("detalle");
        List<String> productosPendientes = new ArrayList<>();

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
            String descripcion = getElementValue(el, "descripcion");
            double cantidad    = parseDouble(getElementValue(el, "cantidad"));
            double precioUnit  = parseDouble(getElementValue(el, "precioUnitario"));
            double descuento   = parseDouble(getElementValue(el, "descuento"));
            double precioTotal = parseDouble(getElementValue(el, "precioTotalSinImpuesto"));
            Long porcIVA = null; double valIVA = 0.0;
            NodeList impDet = el.getElementsByTagName("impuesto");
            if (impDet.getLength() > 0) {
                Element impEl = (Element) impDet.item(0);
                porcIVA = parseLong(getElementValue(impEl, "tarifa"));
                valIVA  = parseDouble(getElementValue(impEl, "valor"));
            }
            // Auto-crear en POR CLASIFICAR si no existe
            ProductoPago producto = obtenerOAutoCrearProducto(
                    descripcion,
                    getElementValue(el, "codigoPrincipal"),
                    getElementValue(el, "codigoAuxiliar"),
                    precioUnit, idEmpresa);
            if (producto.getGrupoProducto() != null
                    && producto.getGrupoProducto().getRubroTipoGrupoH() != null
                    && producto.getGrupoProducto().getRubroTipoGrupoH()
                               == TipoGrupoProductos.POR_CLASIFICAR) {
                productosPendientes.add(descripcion);
            }

            DetalleFacturaCompra df = new DetalleFacturaCompra();
            df.setFactura(factura);
            df.setDescripcion(descripcion);
            df.setCantidad(cantidad);
            df.setValor(precioUnit);
            df.setSubTotal(precioTotal);
            df.setDescuento(descuento);
            df.setBaseImponible(precioTotal);
            df.setPorcentajeIVA(porcIVA);
            df.setValorIVA(valIVA);
            df.setTotal(precioTotal + valIVA);
            df.setProducto(producto.getId());
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

        PathFacturaCompra pathFc2 = new PathFacturaCompra();
        pathFc2.setFactura(factura);
        pathFc2.setPath(doc.getPathXml());
        pathFc2.setAlterno(1L);
        pathFacturaCompraDaoService.save(pathFc2, null);

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", factura.getId());
        r.put("tipoTablaDestino", "FACTURA_COMPRA");
        r.put("mensaje", "FacturaCompra registrada con id=" + factura.getId()
                + (productosPendientes.isEmpty() ? "" : ". " + productosPendientes.size()
                + " producto(s) creados en grupo PENDIENTE DE CLASIFICAR."));
        r.put("productosPendientes", productosPendientes);
        return r;
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

        Titular proveedor = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

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

        NodeList impuestos = xmlDoc.getElementsByTagName("impuesto");
        for (int i = 0; i < impuestos.getLength(); i++) {
            Element el = (Element) impuestos.item(i);
            DetalleRetencionCompra d = new DetalleRetencionCompra();
            d.setRetencion(rc);
            d.setCodImpuesto(getElementValue(el, "codigo"));
            d.setCodRetencion(getElementValue(el, "codigoRetencion"));
            d.setBaseImponible(parseDouble(getElementValue(el, "baseImponible")));
            d.setPorcentajeReten(parseDouble(getElementValue(el, "porcentajeRetener")));
            d.setValorReten(parseDouble(getElementValue(el, "valorRetenido")));
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

        Titular proveedor = obtenerOAutoCrearProveedor(doc.getRucEmisor(), doc.getRazonSocialEmisor(), xmlDoc, idUsuario);

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

        PathRetencionCompra path = new PathRetencionCompra();
        path.setPath(doc.getPathXml());
        path.setAlterno(2L);
        pathRetencionCompraDaoService.save(path, null);

        Map<String, Object> r = new HashMap<>();
        r.put("idDocumentoBD", rc.getId());
        r.put("tipoTablaDestino", "RETENCION_COMPRA_V2");
        r.put("mensaje", "RetencionCompraV2 registrada con id=" + rc.getId());
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

    private String detectarDiferencias(DocumentoCxp doc, double valorSinImpuestos,
                                        double iva, double importeTotal,
                                        LocalDateTime fechaAutorizacion, LocalDate fechaEmision) {
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
        String[] formatos = {
            "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy",
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"
        };
        for (String fmt : formatos) {
            try { return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(fmt)); }
            catch (Exception ignored) {}
        }
        try { return LocalDate.parse(v).atStartOfDay(); }
        catch (Exception ignored) {}
        return null;
    }

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
                                    Long idEmpresa) {
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
                    ? doc.getFechaEmision() : java.time.LocalDate.now();
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
            }

        } catch (UnsupportedOperationException uoe) {
            // Los stubs lanzan UnsupportedOperationException hasta que se configuren las plantillas.
            // Se registra como advertencia informativa, no como error crítico.
            resultado.put("advertenciaAsiento",
                    "Documento registrado. El asiento contable aún no está configurado para '"
                    + doc.getTipoTablaDestino() + "': " + uoe.getMessage()
                    + ". Configure la plantilla en Contabilidad → Tipos de Asiento "
                    + "y defina las cuentas auxiliares.");
            System.out.println("ℹ Asiento CXP pendiente de configurar para tipo="
                    + doc.getTipoTablaDestino());

        } catch (Exception e) {
            // Cualquier otro error → advertencia sin revertir el registro
            resultado.put("advertenciaAsiento",
                    "Documento registrado pero ocurrió un error al generar el asiento contable: "
                    + e.getMessage()
                    + ". Genere el asiento manualmente desde Contabilidad.");
            System.err.println("⚠ Error generando asiento CXP tipo=" + doc.getTipoTablaDestino()
                    + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
