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
    public DocumentoCxp cargarXmlDocumento(Long idDocumentoCxp, String contenidoXml,
                                            String pathDestino, Long idUsuario) throws Throwable {

        System.out.println("=== cargarXmlDocumento idDocumentoCxp=" + idDocumentoCxp);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new Exception("DocumentoCxp no encontrado: " + idDocumentoCxp);

        doc.setPathXml(pathDestino);
        doc.setFechaCargaXml(LocalDateTime.now());
        doc.setUsuarioCargaXml(em.find(Usuario.class, idUsuario));
        doc.setEstadoDocumento(ESTADO_XML_CARGADO);
        return documentoCxpDaoService.save(doc, doc.getId());
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

        Titular titular = buscarTitularPorRuc(doc.getRucEmisor());
        if (titular == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "TITULAR_NO_ENCONTRADO");
            error.put("mensaje", "El emisor con RUC " + doc.getRucEmisor() + " ("
                    + doc.getRazonSocialEmisor() + ") no existe en TSR. Créelo como Proveedor.");
            error.put("rucEmisor", doc.getRucEmisor());
            error.put("razonSocial", doc.getRazonSocialEmisor());
            return error;
        }
        if (titular.getTipoProveedor() == null || titular.getTipoProveedor() != 1L) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "TITULAR_NO_ES_PROVEEDOR");
            error.put("mensaje", "El titular con RUC " + doc.getRucEmisor() + " no tiene rol de Proveedor.");
            error.put("rucEmisor", doc.getRucEmisor());
            error.put("idTitular", titular.getCodigo());
            return error;
        }

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

        // Verificar productos faltantes
        NodeList detallesXml = xmlDoc.getElementsByTagName("detalle");
        List<Map<String, Object>> productosFaltantes = new ArrayList<>();
        for (int i = 0; i < detallesXml.getLength(); i++) {
            Element el = (Element) detallesXml.item(i);
            String descripcion = getElementValue(el, "descripcion");
            if (buscarProductoPorNombre(descripcion, idEmpresa) == null) {
                Map<String, Object> faltante = new HashMap<>();
                faltante.put("nombre", descripcion);
                faltante.put("codigo", getElementValue(el, "codigoPrincipal"));
                faltante.put("codigoAux", getElementValue(el, "codigoAuxiliar"));
                faltante.put("precioUnitario", parseDouble(getElementValue(el, "precioUnitario")));
                productosFaltantes.add(faltante);
            }
        }
        if (!productosFaltantes.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("requiereProductos", true);
            resp.put("mensaje", "Productos del XML no existen. Asigne un grupo y llame /crearProductosYRegistrar");
            resp.put("productosNuevos", productosFaltantes);
            resp.put("idDocumentoCxp", doc.getId());
            return resp;
        }

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
            ProductoPago producto = buscarProductoPorNombre(descripcion, idEmpresa);
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
            df.setProducto(producto != null ? producto.getId() : null);
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
        r.put("mensaje", "FacturaCompra registrada con id=" + factura.getId());
        r.put("requiereProductos", false);
        return r;
    }

    private Map<String, Object> registrarNotaCreditoCompra(DocumentoCxp doc, String xmlContent,
                                                            Long idEmpresa, Long idUsuario) throws Throwable {
        Document xmlDoc = parsearXmlComprobante(xmlContent);
        Empresa empresa = em.find(Empresa.class, idEmpresa);
        Usuario usuario = em.find(Usuario.class, idUsuario);

        Titular titular = buscarTitularPorRuc(doc.getRucEmisor());
        if (titular == null) return errorTitular("TITULAR_NO_ENCONTRADO", doc.getRucEmisor(), null);
        if (titular.getTipoProveedor() == null || titular.getTipoProveedor() != 1L)
            return errorTitular("TITULAR_NO_ES_PROVEEDOR", doc.getRucEmisor(), titular.getCodigo());

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

        Titular titular = buscarTitularPorRuc(doc.getRucEmisor());
        if (titular == null) return errorTitular("TITULAR_NO_ENCONTRADO", doc.getRucEmisor(), null);
        if (titular.getTipoProveedor() == null || titular.getTipoProveedor() != 1L)
            return errorTitular("TITULAR_NO_ES_PROVEEDOR", doc.getRucEmisor(), titular.getCodigo());

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

        Titular titular = buscarTitularPorRuc(doc.getRucEmisor());
        if (titular == null) return errorTitular("TITULAR_NO_ENCONTRADO", doc.getRucEmisor(), null);
        if (titular.getTipoProveedor() == null || titular.getTipoProveedor() != 1L)
            return errorTitular("TITULAR_NO_ES_PROVEEDOR", doc.getRucEmisor(), titular.getCodigo());

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

        Titular proveedor = buscarTitularPorRuc(doc.getRucEmisor());
        if (proveedor == null) return errorTitular("TITULAR_NO_ENCONTRADO", doc.getRucEmisor(), null);
        if (proveedor.getTipoProveedor() == null || proveedor.getTipoProveedor() != 1L)
            return errorTitular("TITULAR_NO_ES_PROVEEDOR", doc.getRucEmisor(), proveedor.getCodigo());

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

        Titular proveedor = buscarTitularPorRuc(doc.getRucEmisor());
        if (proveedor == null) return errorTitular("TITULAR_NO_ENCONTRADO", doc.getRucEmisor(), null);
        if (proveedor.getTipoProveedor() == null || proveedor.getTipoProveedor() != 1L)
            return errorTitular("TITULAR_NO_ES_PROVEEDOR", doc.getRucEmisor(), proveedor.getCodigo());

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
}
