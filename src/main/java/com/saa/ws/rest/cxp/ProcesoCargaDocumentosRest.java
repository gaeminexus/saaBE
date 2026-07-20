package com.saa.ws.rest.cxp;

import java.util.List;
import java.util.Map;

import com.saa.ejb.cxp.dao.GrupoProductoPagoDaoService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
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
 */
@Path("carga-documentos")
public class ProcesoCargaDocumentosRest {

    @EJB private ProcesoCargaDocumentosService procesoCargaDocumentosService;
    @EJB private GrupoProductoPagoDaoService   grupoProductoPagoDaoService;
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

            if (contenidoTxt == null || contenidoTxt.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'contenidoTxt' es obligatorio"))
                        .type(MediaType.APPLICATION_JSON).build();

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarArchivoTxt(contenidoTxt, nombreArchivo, idEmpresa, idUsuario);

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

            String pathDestino = params.get("pathDestino") != null
                    ? params.get("pathDestino").toString()
                    : calcularPathXml(doc.getClaveAcceso());

            guardarXmlEnDisco(contenidoXml, pathDestino);

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarXmlYRegistrar(idDocumentoCxp, contenidoXml, pathDestino, idEmpresa, idUsuario);

            boolean valido = Boolean.TRUE.equals(resultado.get("valido"));
            if (!valido) {
                // XML no coincide con el documento del TXT → 422
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

            if (contenidoXml == null || contenidoXml.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'contenidoXml' es obligatorio"))
                        .type(MediaType.APPLICATION_JSON).build();

            DocumentoCxp doc = procesoCargaDocumentosService.obtenerDocumentoPorId(idDocumentoCxp);
            if (doc == null)
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(errorMap("DocumentoCxp con ID " + idDocumentoCxp + " no encontrado"))
                        .type(MediaType.APPLICATION_JSON).build();

            String pathDestino = params.get("pathDestino") != null
                    ? params.get("pathDestino").toString()
                    : calcularPathXml(doc.getClaveAcceso());

            guardarXmlEnDisco(contenidoXml, pathDestino);

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .cargarXmlDocumento(idDocumentoCxp, contenidoXml, pathDestino, idUsuario);

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

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .registrarDocumentoBD(idDocumentoCxp, idEmpresa, idUsuario);

            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMap("Error al registrar en BD: " + e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =========================================================
    // FASE 4: Resolver novedad
    // =========================================================
    /**
     * Body JSON: { "accion": "REEMPLAZAR"|"MANTENER", "contenidoXml": "...", "pathDestino": "...", "idUsuario": 1 }
     */
    @POST
    @Path("/resolverNovedad/{idDocumentoCxp}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resolverNovedad(@PathParam("idDocumentoCxp") Long idDocumentoCxp,
                                     Map<String, Object> params) {
        System.out.println("=== REST resolverNovedad idDocumentoCxp=" + idDocumentoCxp);
        try {
            String accion       = (String) params.get("accion");
            String contenidoXml = (String) params.get("contenidoXml");
            String pathDestino  = (String) params.get("pathDestino");
            Long idUsuario      = Long.valueOf(params.get("idUsuario").toString());

            if (accion == null || accion.isEmpty())
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(errorMap("El campo 'accion' es obligatorio: REEMPLAZAR | MANTENER"))
                        .type(MediaType.APPLICATION_JSON).build();

            if ("REEMPLAZAR".equalsIgnoreCase(accion) && contenidoXml != null && !contenidoXml.isEmpty())
                guardarXmlEnDisco(contenidoXml, pathDestino);

            Map<String, Object> resultado = procesoCargaDocumentosService
                    .resolverNovedad(idDocumentoCxp, accion, contenidoXml, pathDestino, idUsuario);

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
    // Utilitarios privados
    // =========================================================
    private String calcularPathXml(String claveAcceso) {
        return "/docs/xml/cxp/" + claveAcceso + ".xml";
    }

    private void guardarXmlEnDisco(String contenido, String path) throws Exception {
        if (path == null || path.isEmpty()) throw new Exception("Path destino inválido.");
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        if (p.getParent() != null) java.nio.file.Files.createDirectories(p.getParent());
        java.nio.file.Files.write(p, contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Map<String, Object> errorMap(String mensaje) {
        return java.util.Collections.singletonMap("error", mensaje);
    }
}