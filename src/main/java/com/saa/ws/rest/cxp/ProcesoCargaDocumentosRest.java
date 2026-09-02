package com.saa.ws.rest.cxp;

import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FileService;
import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.service.ClasificacionProductosLoteService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
import com.saa.ejb.cxp.service.ProcesoLoteCxpService;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.GrupoProductoPago;
import com.saa.model.cxp.NombreEntidadesPago;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

/**
 * REST para el proceso de carga de documentos recibidos del SRI.
 *
 * FLUJO:
 *  POST /carga-documentos/cargarTxt                        → Fase 1: Leer TXT
 *  POST /carga-documentos/cargarXml/{idDocumentoCxp}       → Fase 2: Subir XML del documento
 *  POST /carga-documentos/registrarBD/{idDocumentoCxp}     → Fase 3: Crear registros en tablas CXP
 *  POST /carga-documentos/resolverNovedad/{idDocumentoCxp} → Fase 4: Resolver novedad
 *  POST /carga-documentos/revertir/{idDocumentoCxp}        → Fase 5: Revertir documento
 *  GET  /carga-documentos/resumen/{idCargaTxt}             → Consultar resumen de una carga
 *  GET  /carga-documentos/documento/{id}                   → Consultar un DocumentoCxp
 *  GET  /carga-documentos/novedades/{idEmpresa}            → Novedades pendientes
 *
 * LOTE POR CARGA TXT (plan de carga automática desde el SRI, §6):
 *  POST /carga-documentos/descargarXmlLote/{idCargaTxt}    → §6.1 Bajar del SRI el XML de toda la carga
 *  POST /carga-documentos/registrarLote/{idCargaTxt}       → §6.2 Registrar y contabilizar toda la carga
 *  GET  /carga-documentos/progresoLote/{idCargaTxt}        → §6.3 Progreso del lote en curso
 *  POST /carga-documentos/clasificarProductosLote          → §6.4 Asignar grupo a varios productos
 *  GET  /carga-documentos/productosSinClasificarLote/{idCargaTxt} → §6.5 Qué falta clasificar
 */
@Path("carga-documentos")
public class ProcesoCargaDocumentosRest {

    @EJB private ProcesoCargaDocumentosService procesoCargaDocumentosService;
    @EJB private ProcesoLoteCxpService         procesoLoteCxpService;
    @EJB private ClasificacionProductosLoteService clasificacionProductosLoteService;
    @EJB private GrupoProductoPagoDaoService   grupoProductoPagoDaoService;
    @EJB private FileService                   fileService;
    @Context private UriInfo context;

    public ProcesoCargaDocumentosRest() {}

    // =========================================================
    // FASE 1: Carga del TXT
    // =========================================================
    @POST
    @Path("/cargarTxt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarTxt(Map<String, Object> params) {
        System.out.println("=== REST cargarTxt ===");
        try {
            String contenidoTxt  = (String) params.get("contenidoTxt");
            String nombreArchivo = (String) params.get("nombreArchivo");
            Long idEmpresa       = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario       = Long.valueOf(params.get("idUsuario").toString());
            Long idPeriodo       = params.get("idPeriodo") != null
                    ? Long.valueOf(params.get("idPeriodo").toString()) : null;

            if (contenidoTxt == null || contenidoTxt.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'contenidoTxt' es obligatorio"))
                        .type(MediaType.APPLICATION_JSON).build();

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarArchivoTxt(contenidoTxt, nombreArchivo, idEmpresa, idUsuario, idPeriodo);

            return Response.status(Response.Status.CREATED)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al cargar TXT: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // PROCESO UNIFICADO: Subir XML + Registrar en BD en un solo paso
    // =========================================================
    /**
     * Endpoint principal recomendado. Valida el XML, lo guarda en disco y
     * registra en las tablas CXP en un único llamado.
     *
     * Los productos que no existan se crean automáticamente en el grupo
     * "PENDIENTE DE CLASIFICAR". El documento queda en estado REGISTRADO_BD (3).
     *
     * Body JSON: { "contenidoXml": "...", "idEmpresa": 1, "idUsuario": 5 }
     *
     * Response 422 → XML no coincide con el documento esperado:
     *   { "valido": false, "errores": [...], "documento": {...} }
     *
     * Response 200 → Registrado correctamente:
     *   { "valido": true, "idDocumentoBD": 234, "tipoTablaDestino": "FACTURA_COMPRA",
     *     "mensaje": "...", "productosPendientes": ["Producto A"] }
     *
     * Si productosPendientes no está vacío, el usuario debe ir a la pantalla
     * de clasificación de productos antes de poder contabilizar la factura.
     */
    @POST
    @Path("/procesarXml/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarXml(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                 Map<String, Object> params) {
        System.out.println("=== REST procesarXml idDocumentoCxp=" + idDocumentoCxp);
        try {
            String contenidoXml = (String) params.get("contenidoXml");
            Long idEmpresa      = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario      = Long.valueOf(params.get("idUsuario").toString());

            if (contenidoXml == null || contenidoXml.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'contenidoXml' es obligatorio"))
                        .type(MediaType.APPLICATION_JSON).build();

            DocumentoCxp doc = procesoCargaDocumentosService.obtenerDocumentoPorId(idDocumentoCxp);
            if (doc == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errorMap("DocumentoCxp con ID " + idDocumentoCxp + " no encontrado"))
                        .type(MediaType.APPLICATION_JSON).build();

            // Propagar flag esReembolso del body (el usuario puede marcarlo antes de subir el XML)
            if (params.containsKey("esReembolso")) {
                Object v = params.get("esReembolso");
                boolean flagReembolso = "1".equals(v != null ? v.toString() : "")
                        || Boolean.TRUE.equals(v);
                if (flagReembolso) {
                    doc.setEsReembolso(1L);
                    procesoCargaDocumentosService.marcarReembolso(idDocumentoCxp, true, idUsuario);
                }
            }

            String subDir = "docs/xml/cxp";
            String nombreArchivo = doc.getClaveAcceso() + ".xml";
            String pathDestino = params.get("pathDestino") != null
                    ? params.get("pathDestino").toString()
                    : fileService.uploadFileToPath(
                            new java.io.ByteArrayInputStream(contenidoXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            nombreArchivo, subDir);

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarXmlYRegistrar(idDocumentoCxp, contenidoXml, pathDestino, idEmpresa, idUsuario);

            boolean valido = Boolean.TRUE.equals(resultado.get("valido"));
            if (!valido) {
                // XML no coincide con el documento del TXT → 422
                return Response.status(422)
                        .entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }

            // Hay bloqueantes (proveedor sin cuenta, productos sin clasificar, tipo asiento no configurado) → 422
            if (Boolean.TRUE.equals(resultado.get("pendienteClasificacion"))) {
                return Response.status(422)
                        .entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al procesar XML: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Consulta si una FacturaCompra tiene productos en el grupo PENDIENTE DE CLASIFICAR.
     * Debe usarse antes de intentar contabilizar una factura de compra.
     *
     * Response 200: { "pendientes": ["Producto A", "Producto B"] }
     * Si pendientes está vacío, la factura puede contabilizarse.
     */
    @GET
    @Path("/productosPendientes/{idFacturaCompra}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response productosPendientes(@PathParam("idFacturaCompra") Long idFacturaCompra) {
        try {
            java.util.List<String> pendientes = procesoCargaDocumentosService
                    .obtenerProductosPendientesDeClasificar(idFacturaCompra);
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("idFacturaCompra", idFacturaCompra);
            resp.put("pendientes", pendientes);
            resp.put("puedeContabilizar", pendientes.isEmpty());
            return Response.ok(resp).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // FASE 2: Subir XML de un DocumentoCxp (separado — se mantiene por compatibilidad)
    // =========================================================
    /**
     * Body JSON: { "contenidoXml": "...", "idUsuario": 1, "pathDestino": "..." (opcional) }
     */
    @POST
    @Path("/cargarXml/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarXml(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                               Map<String, Object> params) {
        System.out.println("=== REST cargarXml idDocumentoCxp=" + idDocumentoCxp);
        try {
            String contenidoXml = (String) params.get("contenidoXml");
            Long idUsuario      = Long.valueOf(params.get("idUsuario").toString());

            // §6.1 — leer esReembolso del body y pasarlo al service
            Boolean esReembolsoBody = null;
            if (params.containsKey("esReembolso")) {
                Object v = params.get("esReembolso");
                esReembolsoBody = (v instanceof Boolean)
                        ? (Boolean) v
                        : ("1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()));
            }

            if (contenidoXml == null || contenidoXml.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'contenidoXml' es obligatorio"))
                        .type(MediaType.APPLICATION_JSON).build();

            DocumentoCxp doc = procesoCargaDocumentosService.obtenerDocumentoPorId(idDocumentoCxp);
            if (doc == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errorMap("DocumentoCxp con ID " + idDocumentoCxp + " no encontrado"))
                        .type(MediaType.APPLICATION_JSON).build();

            String subDir2 = "docs/xml/cxp";
            String nombreArchivo2 = doc.getClaveAcceso() + ".xml";
            String pathDestino = params.get("pathDestino") != null
                    ? params.get("pathDestino").toString()
                    : fileService.uploadFileToPath(
                            new java.io.ByteArrayInputStream(contenidoXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            nombreArchivo2, subDir2);

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarXmlDocumento(idDocumentoCxp, contenidoXml, pathDestino, idUsuario, esReembolsoBody);

            boolean valido = Boolean.TRUE.equals(resultado.get("valido"));
            if (!valido) {
                // HTTP 422 Unprocessable Entity: el XML no coincide con el documento esperado
                return Response.status(422)
                        .entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al cargar XML: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // FASE 3: Registrar en BD
    // =========================================================
    /**
     * Body JSON: { "idEmpresa": 1, "idUsuario": 1 }
     *
     * "esIntermediario" (boolean, opcional, default false) e "idProductoIntermediario"
     * (Long, obligatorio si esIntermediario=true) marcan la factura como de intermediario:
     * el asiento ignora detalles e impuestos y manda el total completo a la cuenta del
     * grupo de ese producto (docs/logica-negocio/cxp/DISENO-FACTURA-INTERMEDIARIO.md).
     * A diferencia de "esReembolso" (que se marca al subir el XML), esta marca se decide
     * al registrar, así que viaja en este body y no en el documento ya persistido.
     */
    @POST
    @Path("/registrarBD/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarBD(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                 Map<String, Object> params) {
        System.out.println("=== REST registrarBD idDocumentoCxp=" + idDocumentoCxp);
        try {
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());

            boolean esIntermediario = false;
            if (params.containsKey("esIntermediario")) {
                Object v = params.get("esIntermediario");
                esIntermediario = (v instanceof Boolean)
                        ? (Boolean) v
                        : ("1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()));
            }
            Long idProductoIntermediario = (params.get("idProductoIntermediario") != null)
                    ? Long.valueOf(params.get("idProductoIntermediario").toString())
                    : null;

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .registrarDocumentoBD(idDocumentoCxp, idEmpresa, idUsuario,
                            esIntermediario, idProductoIntermediario);

            // Bloqueantes (productos sin clasificar, tipo asiento no configurado, etc.)
            if (Boolean.TRUE.equals(resultado.get("pendienteClasificacion"))) {
                return Response.status(422).entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            java.util.Map<String, Object> error = new java.util.HashMap<>();
            error.put("error", e.getMessage());
            error.put("tipo", "ERROR_REGISTRO");
            return Response.status(422)
                    .entity(error).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // FASE 4: Resolver novedad
    // =========================================================
    /**
     * Body JSON: { "accion": 1|2, "contenidoXml": "...", "pathDestino": "...", "idUsuario": 1 }
     * Rubro 177 CXP_ACCION_NOVEDAD: 1=MANTENER, 2=REEMPLAZAR
     */
    @POST
    @Path("/resolverNovedad/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolverNovedad(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                     Map<String, Object> params) {
        System.out.println("=== REST resolverNovedad idDocumentoCxp=" + idDocumentoCxp);
        try {
            Integer accion      = params.get("accion") != null
                    ? Integer.valueOf(params.get("accion").toString()) : null;
            String contenidoXml = (String) params.get("contenidoXml");
            String pathDestino  = (String) params.get("pathDestino");
            Long idUsuario      = Long.valueOf(params.get("idUsuario").toString());

            // §Novedad 2 — leer esReembolso del body para pasarlo al service en REEMPLAZAR
            Boolean esReembolsoBody = null;
            if (params.containsKey("esReembolso")) {
                Object v = params.get("esReembolso");
                esReembolsoBody = (v instanceof Boolean)
                        ? (Boolean) v
                        : ("1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString()));
            }

            if (accion == null)
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'accion' es obligatorio: 1=MANTENER, 2=REEMPLAZAR (Rubro 177)"))
                        .type(MediaType.APPLICATION_JSON).build();

            if (accion == com.saa.rubros.AccionNovedad.REEMPLAZAR && contenidoXml != null && !contenidoXml.isEmpty()) {
                DocumentoCxp docNovedad = procesoCargaDocumentosService.obtenerDocumentoPorId(idDocumentoCxp);
                String nombreArchivoNov = docNovedad.getClaveAcceso() + ".xml";
                pathDestino = fileService.uploadFileToPath(
                        new java.io.ByteArrayInputStream(contenidoXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        nombreArchivoNov, "docs/xml/cxp");
            }

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .resolverNovedad(idDocumentoCxp, accion, contenidoXml, pathDestino, idUsuario, esReembolsoBody);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al resolver novedad: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // FASE 5: Revertir
    // =========================================================
    /**
     * Body JSON: { "idUsuario": 1 }
     */
    @POST
    @Path("/revertir/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertir(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                              Map<String, Object> params) {
        System.out.println("=== REST revertir idDocumentoCxp=" + idDocumentoCxp);
        try {
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .revertirDocumento(idDocumentoCxp, idUsuario);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al revertir: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // Anular / recontabilizar
    //
    // Tres operaciones que NO borran nada, a diferencia de /revertir. Ver
    // docs/logica-negocio/cxp/DISENO-ANULAR-VS-RECONTABILIZAR-FACTURA-COMPRA.md
    //
    // El pago se anula APARTE, desde la bandeja de pagos: estos endpoints sólo verifican que no
    // quede ninguno vigente y devuelven 409 si lo hay.
    // =========================================================

    /**
     * CASO B, PASO 1. Anula el asiento y deja el documento pendiente de contabilizar.
     * Body JSON: { "motivo": "cuenta del grupo mal configurada", "idUsuario": 1 }
     */
    @POST
    @Path("/anularContabilidad/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularContabilidad(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                        Map<String, Object> params) {
        System.out.println("=== REST anularContabilidad idDocumentoCxp=" + idDocumentoCxp);
        try {
            String motivo = params.get("motivo") != null ? params.get("motivo").toString() : null;
            Long idUsuario = params.get("idUsuario") != null
                    ? Long.valueOf(params.get("idUsuario").toString()) : null;

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .anularContabilidadDocumento(idDocumentoCxp, motivo, idUsuario);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            // 409 y no 500: no es un fallo del servidor, es que el documento no está en
            // condiciones de esta operación (estado equivocado, o hay pagos vigentes).
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al anular la contabilidad: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * CASO B, PASO 2. Genera el asiento nuevo con las cuentas ya corregidas.
     * Body JSON: { "idUsuario": 1 }
     */
    @POST
    @Path("/recontabilizar/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recontabilizar(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                    Map<String, Object> params) {
        System.out.println("=== REST recontabilizar idDocumentoCxp=" + idDocumentoCxp);
        try {
            Long idUsuario = params != null && params.get("idUsuario") != null
                    ? Long.valueOf(params.get("idUsuario").toString()) : null;

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .recontabilizarDocumento(idDocumentoCxp, idUsuario);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al recontabilizar: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * CASO A, paso final. Marca el documento como ANULADO(7). <b>Terminal: no se reprocesa.</b>
     * Se llama después de anular la factura con {@code POST /fctc/anular}.
     * Body JSON: { "motivo": "anulada ante el SRI", "idUsuario": 1 }
     */
    @POST
    @Path("/anularDocumento/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularDocumento(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                     Map<String, Object> params) {
        System.out.println("=== REST anularDocumento idDocumentoCxp=" + idDocumentoCxp);
        try {
            String motivo = params.get("motivo") != null ? params.get("motivo").toString() : null;
            Long idUsuario = params.get("idUsuario") != null
                    ? Long.valueOf(params.get("idUsuario").toString()) : null;

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .marcarDocumentoAnulado(idDocumentoCxp, motivo, idUsuario);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al anular el documento: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // Consultas
    // =========================================================
    @GET
    @Path("/resumen/{idCargaTxt}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumen(@PathParam("idCargaTxt") Long idCargaTxt) {
        try {
            Map<String, Object> resultado = procesoCargaDocumentosService.obtenerResumenCarga(idCargaTxt);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al obtener resumen: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/documento/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumento(@PathParam("id") Long id) {
        try {
            DocumentoCxp doc = procesoCargaDocumentosService.obtenerDocumentoPorId(id);
            if (doc == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errorMap("DocumentoCxp con ID " + id + " no encontrado"))
                        .type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK)
                    .entity(doc).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/novedades/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response novedadesPendientes(@PathParam("idEmpresa") Long idEmpresa) {
        try {
            List<DocumentoCxp> lista = procesoCargaDocumentosService.obtenerNovedadesPendientes(idEmpresa);
            return Response.status(Response.Status.OK)
                    .entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al obtener novedades: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * FASE 3b: Crea productos faltantes y registra.
     * Body JSON: { "idEmpresa":1, "idUsuario":5, "productosConGrupo":[...] }
     */
    @POST
    @Path("/crearProductosYRegistrar/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearProductosYRegistrar(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                              Map<String, Object> params) {
        System.out.println("=== REST crearProductosYRegistrar idDocumentoCxp=" + idDocumentoCxp);
        try {
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> productosConGrupo =
                    (List<Map<String, Object>>) params.get("productosConGrupo");

            if (productosConGrupo == null || productosConGrupo.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'productosConGrupo' es obligatorio y no puede estar vacío"))
                        .type(MediaType.APPLICATION_JSON).build();

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .crearProductosYRegistrar(idDocumentoCxp, idEmpresa, idUsuario, productosConGrupo);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al crear productos y registrar: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // Reembolso de gastos (§7)
    // =========================================================

    /** POST /carga-documentos/marcarReembolso/{idDocumentoCxp}
     *  body {esReembolso: 0|1, idUsuario} */
    @POST @Path("/marcarReembolso/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response marcarReembolso(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                     Map<String, Object> params) {
        System.out.println("=== REST marcarReembolso idDocumentoCxp=" + idDocumentoCxp);
        try {
            boolean esReembolso = params.get("esReembolso") != null
                    && ("1".equals(params.get("esReembolso").toString())
                        || Boolean.TRUE.equals(params.get("esReembolso")));
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());
            Map<String, Object> resultado = procesoCargaDocumentosService
                    .marcarReembolso(idDocumentoCxp, esReembolso, idUsuario);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException ie) {
            return Response.status(422).entity(errorMap(ie.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error marcarReembolso: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /carga-documentos/contabilizarReembolso/{idFacturaCompra}
     *  body {idEmpresa, idUsuario} */
    @POST @Path("/contabilizarReembolso/{idFacturaCompra}")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response contabilizarReembolso(@PathParam("idFacturaCompra") Long idFacturaCompra,
                                           Map<String, Object> params) {
        System.out.println("=== REST contabilizarReembolso idFacturaCompra=" + idFacturaCompra);
        try {
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());
            Map<String, Object> resultado = procesoCargaDocumentosService
                    .contabilizarReembolso(idFacturaCompra, idEmpresa, idUsuario);
            // §Novedad 3 — bloqueantes con la misma estructura del PASO 2
            if (Boolean.TRUE.equals(resultado.get("pendienteClasificacion"))) {
                return Response.status(422).entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException ie) {
            // IncomeException queda como fallback para errores no bloqueantes (ej: factura no encontrada)
            Map<String, Object> err = new java.util.HashMap<>();
            err.put("error", ie.getMessage());
            return Response.status(422).entity(err).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error contabilizarReembolso: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /carga-documentos/recalcularTotalesReembolso/{idFacturaCompra} */
    @POST @Path("/recalcularTotalesReembolso/{idFacturaCompra}")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response recalcularTotalesReembolso(@PathParam("idFacturaCompra") Long idFacturaCompra,
                                                Map<String, Object> params) {
        System.out.println("=== REST recalcularTotalesReembolso idFacturaCompra=" + idFacturaCompra);
        try {
            Map<String, Object> resultado = procesoCargaDocumentosService
                    .recalcularTotalesReembolso(idFacturaCompra);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error recalcularTotalesReembolso: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** POST /carga-documentos/crearProductoPorClasificar
     *  body {nombre, codigo?, idEmpresa} */
    @POST @Path("/crearProductoPorClasificar")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response crearProductoPorClasificar(Map<String, Object> params) {
        System.out.println("=== REST crearProductoPorClasificar");
        try {
            String nombre  = (String) params.get("nombre");
            String codigo  = params.get("codigo") != null ? params.get("codigo").toString() : null;
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            com.saa.model.cxp.ProductoPago prod = procesoCargaDocumentosService
                    .crearProductoPorClasificar(nombre, codigo, idEmpresa);
            return Response.ok(prod).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error crearProductoPorClasificar: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Lista los grupos de productos disponibles para asignar a nuevos productos.
     */
    @GET
    @Path("/gruposProducto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGruposProducto() {
        try {
            List<GrupoProductoPago> lista = grupoProductoPagoDaoService
                    .selectAll(NombreEntidadesPago.GRUPO_PRODUCTO_PAGO);
            return Response.status(Response.Status.OK)
                    .entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al obtener grupos: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // LOTE POR CARGA TXT — §6.1 y §6.3 del plan de carga automática
    // =========================================================

    /**
     * §6.1 — Descarga masiva del XML desde el SRI para una carga TXT.
     *
     * <p>
     * Body JSON: <code>{ "idEmpresa": 1236, "idUsuario": 5 }</code>
     * </p>
     *
     * <p>
     * Responde <b>202</b> en cuanto el lote arranca, con los contadores de lo que
     * va a procesar; el avance se sigue por {@code /progresoLote/{idCargaTxt}}.
     * Responde <b>409</b> si ya hay un lote corriendo para esa carga.
     * </p>
     */
    @POST
    @Path("/descargarXmlLote/{idCargaTxt}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response descargarXmlLote(@PathParam("idCargaTxt") Long idCargaTxt,
                                      Map<String, Object> params) {
        System.out.println("=== REST descargarXmlLote idCargaTxt=" + idCargaTxt);
        boolean reservado = false;
        try {
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());

            Map<String, Object> arranque = procesoLoteCxpService.reservarDescargaLote(idCargaTxt);
            if (Boolean.TRUE.equals(arranque.get("conflicto")))
                return Response.status(Response.Status.CONFLICT)
                        .entity(errorMap("Ya hay una descarga en curso para esta carga."))
                        .type(MediaType.APPLICATION_JSON).build();
            reservado = true;

            // Va por el proxy del EJB, que es lo que hace que el método corra de
            // verdad en segundo plano: un this.descargarXmlLote(...) dentro del
            // propio bean se ejecutaría sincrónicamente y la petición se quedaría
            // colgada los minutos que tarde el lote.
            procesoLoteCxpService.descargarXmlLote(idCargaTxt, idEmpresa, idUsuario);

            return Response.status(Response.Status.ACCEPTED)
                    .entity(arranque).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            // Si ya se había reservado, hay que soltar la carga o queda trabada
            // para siempre: el lote que la liberaría no llegó a arrancar.
            if (reservado) procesoLoteCxpService.liberarLote(idCargaTxt);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al iniciar la descarga del lote: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * §6.2 — Registro y contabilización de toda una carga.
     *
     * <p>
     * Body JSON: <code>{ "idEmpresa": 1236, "idUsuario": 5 }</code>
     * </p>
     *
     * <p>
     * Procesa los documentos que tengan el XML cargado, en el orden obligatorio
     * (facturas y liquidaciones → notas → retenciones) y con una transacción por
     * documento. Responde <b>202</b> al arrancar y <b>409</b> si ya hay un lote
     * corriendo sobre esa carga, sea de descarga o de registro.
     * </p>
     */
    @POST
    @Path("/registrarLote/{idCargaTxt}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarLote(@PathParam("idCargaTxt") Long idCargaTxt,
                                   Map<String, Object> params) {
        System.out.println("=== REST registrarLote idCargaTxt=" + idCargaTxt);
        boolean reservado = false;
        try {
            Long idEmpresa = Long.valueOf(params.get("idEmpresa").toString());
            Long idUsuario = Long.valueOf(params.get("idUsuario").toString());

            Map<String, Object> arranque = procesoLoteCxpService.reservarRegistroLote(idCargaTxt);
            if (Boolean.TRUE.equals(arranque.get("conflicto")))
                return Response.status(Response.Status.CONFLICT)
                        .entity(errorMap("Ya hay un lote en curso para esta carga."))
                        .type(MediaType.APPLICATION_JSON).build();
            reservado = true;

            // Por el proxy del EJB, que es lo que hace que corra de verdad en
            // segundo plano. Un this.registrarLote(...) sería síncrono y la
            // petición se quedaría colgada los minutos que tarde el lote.
            procesoLoteCxpService.registrarLote(idCargaTxt, idEmpresa, idUsuario);

            return Response.status(Response.Status.ACCEPTED)
                    .entity(arranque).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            // Si ya se había reservado hay que soltar la carga, o queda trabada:
            // el lote que la liberaría no llegó a arrancar.
            if (reservado) procesoLoteCxpService.liberarLote(idCargaTxt);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al iniciar el registro del lote: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * §6.3 — Progreso del lote de una carga. Sirve a los dos lotes (descarga y
     * registro); {@code tipoLote} dice cuál, y es null cuando no hay ninguno.
     * El frontend lo consulta cada 2 s mientras {@code enCurso} sea true.
     */
    @GET
    @Path("/progresoLote/{idCargaTxt}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response progresoLote(@PathParam("idCargaTxt") Long idCargaTxt) {
        System.out.println("=== REST progresoLote idCargaTxt=" + idCargaTxt);
        try {
            Map<String, Object> progreso = procesoLoteCxpService.progresoLote(idCargaTxt);
            return Response.status(Response.Status.OK)
                    .entity(progreso).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al consultar el progreso del lote: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // CLASIFICACIÓN MASIVA DE PRODUCTOS — §6.4 y §6.5
    // =========================================================

    /**
     * §6.5 — Productos en POR CLASIFICAR que aparecen en los documentos de una
     * carga, con los comprobantes que los usan.
     *
     * <p>
     * Devuelve <b>200 con la lista vacía</b> cuando no hay nada pendiente o
     * cuando ningún documento de la carga llegó a registrarse todavía. No
     * devuelve 404: "no hay nada que clasificar" es una respuesta, no un error.
     * </p>
     */
    @GET
    @Path("/productosSinClasificarLote/{idCargaTxt}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response productosSinClasificarLote(@PathParam("idCargaTxt") Long idCargaTxt) {
        System.out.println("=== REST productosSinClasificarLote idCargaTxt=" + idCargaTxt);
        try {
            Map<String, Object> resultado = clasificacionProductosLoteService
                    .productosSinClasificarLote(idCargaTxt);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al listar los productos sin clasificar: "
                            + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * §6.4 — Asigna grupo a productos que ya existen, todos en una transacción.
     *
     * <p>
     * Body JSON:
     * <code>{ "idEmpresa": 1236,
     *         "asignaciones": [ {"idProducto": 88, "idGrupo": 4} ] }</code>
     * </p>
     *
     * <p>
     * <b>422</b> cuando el envío no se puede aplicar entero: grupo inexistente o
     * de otra empresa, líneas sin idProducto/idGrupo, lista vacía. Es el mismo
     * código que ya usa {@code registrarBD} para los errores de negocio.
     * </p>
     */
    @POST
    @Path("/clasificarProductosLote")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clasificarProductosLote(Map<String, Object> params) {
        System.out.println("=== REST clasificarProductosLote");
        try {
            Long idEmpresa = params.get("idEmpresa") != null
                    ? Long.valueOf(params.get("idEmpresa").toString()) : null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> asignaciones =
                    (List<Map<String, Object>>) params.get("asignaciones");

            Map<String, Object> resultado = clasificacionProductosLoteService
                    .clasificarProductosLote(idEmpresa, asignaciones);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (com.saa.basico.util.IncomeException e) {
            // Error de negocio: el usuario puede corregirlo y reenviar.
            return Response.status(422)
                    .entity(errorMap(e.getMessage())).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al clasificar los productos: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // Utilitarios privados
    // =========================================================
    private Map<String, Object> errorMap(String mensaje) {
        return java.util.Collections.singletonMap("error", mensaje);
    }
}