package com.saa.ws.rest.asoprep;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.crd.service.CobroPetroContableService;
import com.saa.ejb.crd.service.EstadoCivilService;
import com.saa.ejb.crd.service.ProcesoCargaPetroService;
import com.saa.ejb.crd.service.dto.EstadoContablePetro;
import com.saa.ejb.crd.service.dto.ResultadoConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.ResultadoReversarRecepcion;
import com.saa.ejb.crd.service.dto.ResumenTransferenciasCarga;
import com.saa.ejb.crd.service.dto.SolicitudConfirmarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudReversarRecepcion;
import com.saa.ejb.crd.service.dto.SolicitudTransferenciaCargaPetro;
import com.saa.ejb.crd.service.dto.TransferenciaCargaPetroDTO;
import com.saa.model.crd.CargaArchivo;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.ParticipeXCargaArchivo;
import com.saa.ws.rest.files.FileResponse;

import jakarta.ejb.EJB;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

@Path("asgn")
public class AsoprepGenerales {

	@EJB
    private FileService fileService;

	@EJB
    private EstadoCivilService estadoCivilService;

	@EJB
    private CargaArchivoPetroService cargaArchivoPetroService;

	@EJB
    private ProcesoCargaPetroService procesoCargaPetroService;

	@EJB
    private CobroPetroContableService cobroPetroContableService;

	@GET
    @Path("/actualizaCodigoPetroEntidad/{codigoPetro}/{idParticipeXCarga}/{idEntidad}") 
    @Produces("application/json")
    public ParticipeXCargaArchivo actualizaCodigoPetroEntidad(@PathParam("codigoPetro") Long codigoPetro, @PathParam("idParticipeXCarga") Long idParticipeXCarga, @PathParam("idEntidad") Long idEntidad) throws Throwable {
        return cargaArchivoPetroService.actualizaCodigoPetroEntidad(codigoPetro, idParticipeXCarga, idEntidad);
    }
	
	/**
     * Upload de archivo con path personalizado
     * 
     * @param inputStream : Stream del archivo
     * @param fileName : Nombre del archivo
     * @param uploadPath : Ruta personalizada para el upload
     * @return Response con el resultado del upload
     */
    @POST
    @Path("upload/custom")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFileCustomPath(InputStream inputStream, 
                                       @QueryParam("fileName") String fileName,
                                       @QueryParam("uploadPath") String uploadPath) {
        try {
            // Validar parámetros
            if (inputStream == null || fileName == null || fileName.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "No se ha enviado archivo o nombre de archivo", null))
                        .build();
            }

            // Validar path personalizado
            if (uploadPath == null || uploadPath.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El path de upload es requerido", null))
                        .build();
            }

            // Validar extensión
            if (!fileService.validarExtension(fileName)) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "Extensión de archivo no permitida", null))
                        .build();
            }
           
            String filePath = fileService.uploadFileToPath(inputStream, fileName, uploadPath);
            
            System.out.println("Resultado del upload: " + filePath);

            return Response.ok(new FileResponse(true, "Archivo subido exitosamente", filePath)).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al subir archivo: " + e.getMessage(), null))
                    .build();
        }
    }    
    
    /**
     * Procesa archivo Petro - Usando formData para archivos extensos (5000+ líneas)
     * 
     * @param archivoInputStream Stream del archivo
     * @param archivoDetails Detalles del archivo (nombre, tipo, etc.)
     * @param cargaArchivoJson JSON con datos de CargaArchivo
     * @param detallesCargaArchivosJson JSON con lista de DetalleCargaArchivo
     * @param participesXCargaArchivoJson JSON con lista de ParticipeXCargaArchivo
     * @return Response con el resultado del procesamiento
     */
    @POST
    @Path("procesarArchivoPetro")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarArchivoPetro(
    		@Context HttpHeaders headers,
            @Context UriInfo uriInfo,
    		@FormParam("archivo") InputStream archivoInputStream,
            @FormParam("archivoNombre") String archivoNombre,
            @FormParam("cargaArchivo") String cargaArchivoJson,
            @FormParam("detallesCargaArchivos") String detallesCargaArchivosJson,
            @FormParam("participesXCargaArchivo") String participesXCargaArchivoJson) {

        try {
            // Validar archivo
            if (archivoInputStream == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "No se ha enviado el archivo", null))
                        .build();
            }

            String fileName = archivoNombre;

            // Validar que se recibieron todos los datos JSON necesarios
            if (cargaArchivoJson == null || detallesCargaArchivosJson == null || participesXCargaArchivoJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "Faltan datos JSON requeridos", null))
                        .build();
            }

            // Validar extensión del archivo
            try {
                if (!fileService.validarExtension(fileName)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new FileResponse(false, "Extensión de archivo no permitida", null))
                            .build();
                }
            } catch (Throwable e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new FileResponse(false, "Error al validar extensión: " + e.getMessage(), null))
                        .build();
            }

            // Deserializar los JSON usando Jakarta JSON Binding (no necesita URL decode porque viene por formData)
            Jsonb jsonb = crearJsonbUTF8();
            
            CargaArchivo cargaArchivo = jsonb.fromJson(cargaArchivoJson, CargaArchivo.class);
            
            DetalleCargaArchivo[] detallesArray = jsonb.fromJson(detallesCargaArchivosJson, DetalleCargaArchivo[].class);
            List<DetalleCargaArchivo> detallesCargaArchivos = Arrays.asList(detallesArray);
            
            ParticipeXCargaArchivo[] participesArray = jsonb.fromJson(participesXCargaArchivoJson, ParticipeXCargaArchivo[].class);
            List<ParticipeXCargaArchivo> participesXCargaArchivo = Arrays.asList(participesArray);

            // Procesar con el EJB CargaArchivoPetroService
            String rutaArchivo;
            try {
                rutaArchivo = cargaArchivoPetroService.procesarArchivoPetro(
                    archivoInputStream, fileName, cargaArchivo, detallesCargaArchivos, participesXCargaArchivo);
            } catch (Throwable e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new FileResponse(false, "Error al procesar archivo en EJB: " + e.getMessage(), null))
                        .build();
            }

            return Response.ok(new FileResponse(true, "Archivo procesado exitosamente", rutaArchivo)).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al procesar archivo: " + e.getMessage(), null))
                    .build();
        }
    }
    
    /**
     * Procesa archivo Petro - Usando formData para archivos extensos (5000+ líneas)
     * 
     * @param archivoInputStream Stream del archivo
     * @param archivoDetails Detalles del archivo (nombre, tipo, etc.)
     * @param cargaArchivoJson JSON con datos de CargaArchivo
     * @param detallesCargaArchivosJson JSON con lista de DetalleCargaArchivo
     * @param participesXCargaArchivoJson JSON con lista de ParticipeXCargaArchivo
     * @return Response con el resultado del procesamiento
     */
    @POST
    @Path("validarArchivoPetro")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarArchivoPetro(
    		@Context HttpHeaders headers,
            @Context UriInfo uriInfo,
    		@FormParam("archivo") InputStream archivoInputStream,
            @FormParam("archivoNombre") String archivoNombre,
            @FormParam("cargaArchivo") String cargaArchivoJson) {

        try {
            // Validar archivo
            if (archivoInputStream == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "No se ha enviado el archivo", null))
                        .build();
            }

            String fileName = archivoNombre;
            System.out.println("Archivo encontrado: " + fileName);

            // Validar extensión del archivo
            try {
                if (!fileService.validarExtension(fileName)) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new FileResponse(false, "Extensión de archivo no permitida", null))
                            .build();
                }
            } catch (Throwable e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new FileResponse(false, "Error al validar extensión: " + e.getMessage(), null))
                        .build();
            }
            
            // Validar que se recibieron todos los datos JSON necesarios
            if (cargaArchivoJson == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "Faltan datos de carga", null))
                        .build();
            }
            
            // Deserializar los JSON usando Jakarta JSON Binding (no necesita URL decode porque viene por formData)
            Jsonb jsonb = crearJsonbUTF8();
            
            CargaArchivo cargaArchivo = jsonb.fromJson(cargaArchivoJson, CargaArchivo.class);

            // Procesar con el EJB CargaArchivoPetroService
            try {
            	cargaArchivo = cargaArchivoPetroService.validarArchivoPetro(
                    archivoInputStream, fileName, cargaArchivo);
            } catch (Throwable e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new FileResponse(false, "Error al procesar archivo en EJB: " + e.getMessage(), null))
                        .build();
            }

            return Response.ok(cargaArchivo).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al procesar archivo: " + e.getMessage(), null))
                    .build();
        }
    }
    
    /**
     * Obtiene el reporte de novedades del procesamiento FASE 2
     * 
     * @param idCargaArchivo ID del CargaArchivo procesado
     * @return Response con el reporte de novedades
     */
    @GET
    @Path("/reporteProcesamientoPetro/{idCargaArchivo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response obtenerReporteProcesamientoPetro(@PathParam("idCargaArchivo") Long idCargaArchivo) {
        System.out.println("Obteniendo reporte de procesamiento para carga: " + idCargaArchivo);
        
        try {
            if (idCargaArchivo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El ID de carga archivo es requerido", null))
                        .build();
            }
            
            // TODO: Implementar método en el servicio que retorne el reporte estructurado
            // Por ahora retornamos mensaje de éxito
            String mensaje = "Para obtener las novedades, consulta la tabla PRCA con el query SQL correspondiente. " +
                           "Ver documentación en docs/PROCESO-CARGA-ARCHIVO-PETROCOMERCIAL.md";
            
            return Response.ok()
                    .entity(new FileResponse(true, mensaje, null))
                    .build();
            
        } catch (Throwable e) {
            System.err.println("ERROR al obtener reporte:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error: " + e.getMessage(), null))
                    .build();
        }
    }
    
    /**
     * Procesa un archivo de carga Petrocomercial ya validado (FASE 2)
     * Cruza la información con préstamos y aportes del sistema
     * 
     * @param idCargaArchivo ID del CargaArchivo a procesar
     * @return Response con el resultado del procesamiento
     */
    @POST
    @Path("/procesarCargaPetro/{idCargaArchivo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarCargaPetro(@PathParam("idCargaArchivo") Long idCargaArchivo) {
        System.out.println("========================================");
        System.out.println("REST: PROCESAR CARGA PETRO FASE 2");
        System.out.println("ID Carga Archivo: " + idCargaArchivo);
        System.out.println("========================================");
        
        try {
            // Validar parámetro
            if (idCargaArchivo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El ID de carga archivo es requerido", null))
                        .build();
            }
            
            // Procesar con el servicio FASE 2
            CargaArchivo resultado = procesoCargaPetroService.procesarCargaPetro(idCargaArchivo);
            
            System.out.println("Procesamiento completado exitosamente");
            return Response.ok(resultado).build();
            
        } catch (Throwable e) {
            System.err.println("ERROR al procesar carga Petro FASE 2:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al procesar: " + e.getMessage(), null))
                    .build();
        }
    }
    
    /**
     * Aplica los pagos de un archivo Petro ya validado
     * Este endpoint ejecuta el proceso final de carga:
     * - Verifica si existen afectaciones manuales (tabla AVPC)
     * - Si no hay afectaciones manuales, aplica reglas automáticas
     * - Orden de afectación: Desgravamen → Interés → Capital
     * - Maneja casos especiales PH/PP con búsqueda de HS
     * - Actualiza estados de cuotas (PAGADA/PARCIAL)
     * - Registra pagos en la tabla PagoPrestamo
     * 
     * @param idCargaArchivo ID del CargaArchivo a procesar
     * @return Response con el resumen de la aplicación de pagos
     */
    @POST
    @Path("/aplicarPagosArchivoPetro/{idCargaArchivo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarPagosArchivoPetro(@PathParam("idCargaArchivo") Long idCargaArchivo) {
        System.out.println("========================================");
        System.out.println("REST: APLICAR PAGOS ARCHIVO PETRO");
        System.out.println("ID Carga Archivo: " + idCargaArchivo);
        System.out.println("========================================");
        
        try {
            // Validar parámetro
            if (idCargaArchivo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El ID de carga archivo es requerido", null))
                        .build();
            }
            
            // Aplicar pagos con el servicio
            String resumen = cargaArchivoPetroService.aplicarPagosArchivoPetro(idCargaArchivo);

            System.out.println("Aplicación de pagos completada exitosamente");
            System.out.println(resumen);

            return Response.ok()
                    .entity(new FileResponse(true, resumen, null))
                    .build();

        } catch (IncomeException e) {
            // Validaciones de negocio (valores sin destino, carga inexistente):
            // no se procesó nada y el mensaje está listo para mostrarse al usuario.
            System.err.println("Carga no procesada: " + e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new FileResponse(false, e.getMessage(), null))
                    .build();
        } catch (Throwable e) {
            System.err.println("ERROR al aplicar pagos del archivo Petro:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al aplicar pagos: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * Devuelve los registros de la carga cuyo valor descontado todavía no tiene
     * definido a qué préstamo, cuota o aporte aplicarse.
     *
     * Mientras esta lista no venga vacía, /aplicarPagosArchivoPetro responde 409
     * y no procesa nada. Sirve para habilitar o bloquear el botón de procesar y
     * para mostrarle al usuario qué novedades le falta resolver.
     *
     * @param idCargaArchivo ID del CargaArchivo a revisar
     * @return Response con la lista de valores sin destino (array vacío si todo está resuelto)
     */
    @GET
    @Path("/valoresSinDestino/{idCargaArchivo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response valoresSinDestino(@PathParam("idCargaArchivo") Long idCargaArchivo) {
        System.out.println("REST: CONSULTA DE VALORES SIN DESTINO - Carga: " + idCargaArchivo);

        try {
            if (idCargaArchivo == null) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El ID de carga archivo es requerido", null))
                        .build();
            }

            List<Map<String, Object>> pendientes = cargaArchivoPetroService.obtenerValoresSinDestino(idCargaArchivo);

            return Response.ok().entity(pendientes).build();

        } catch (IncomeException e) {
            return Response.status(Status.NOT_FOUND)
                    .entity(new FileResponse(false, e.getMessage(), null))
                    .build();
        } catch (Throwable e) {
            System.err.println("ERROR al consultar valores sin destino:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al consultar valores sin destino: " + e.getMessage(), null))
                    .build();
        }
    }
    
    // =====================================================================================
    // Cobro de Petro en dos pasos (regla 11 de §5 de
    // LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md). CONTRATO CONGELADO con el frontend:
    // docs/logica-negocio/crd/API-COBRO-PETRO-DOS-PASOS.md. Errores: 500 con JSON
    // {"mensaje": "..."} — lo envuelve MensajeErrorJsonFilter sobre el String de e.getMessage().
    // =====================================================================================

    @GET
    @Path("/transferencias/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response transferenciasDeCarga(@PathParam("idCarga") Long idCarga) {
        try {
            ResumenTransferenciasCarga resumen = cobroPetroContableService.resumenTransferencias(idCarga);
            return Response.ok(resumen).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las transferencias de la carga: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/transferencias")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarTransferencia(SolicitudTransferenciaCargaPetro solicitud) {
        try {
            TransferenciaCargaPetroDTO transferencia =
                    cobroPetroContableService.registrarTransferencia(solicitud);
            return Response.status(Status.CREATED).entity(transferencia)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la transferencia: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/transferencias/{idTransferencia}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularTransferencia(@PathParam("idTransferencia") Long idTransferencia,
            @QueryParam("usuario") String usuario) {
        try {
            cobroPetroContableService.anularTransferencia(idTransferencia, usuario);
            return Response.ok(java.util.Collections.singletonMap("anulada", Boolean.TRUE))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la transferencia: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/confirmarRecepcion/{idCarga}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarRecepcion(@PathParam("idCarga") Long idCarga,
            SolicitudConfirmarRecepcion solicitud) {
        try {
            ResultadoConfirmarRecepcion resultado =
                    cobroPetroContableService.confirmarRecepcion(idCarga, solicitud);
            return Response.ok(resultado).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al confirmar la recepción: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/reversarRecepcion/{idCarga}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reversarRecepcion(@PathParam("idCarga") Long idCarga,
            SolicitudReversarRecepcion solicitud) {
        try {
            ResultadoReversarRecepcion resultado =
                    cobroPetroContableService.reversarRecepcion(idCarga, solicitud);
            return Response.ok(resultado).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reversar la recepción: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/estadoContable/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response estadoContable(@PathParam("idCarga") Long idCarga) {
        try {
            EstadoContablePetro estado = cobroPetroContableService.estadoContable(idCarga);
            return Response.ok(estado).build();
        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al consultar el estado contable: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un Jsonb configurado específicamente para UTF-8
     */
    private Jsonb crearJsonbUTF8() {
        JsonbConfig config = new JsonbConfig();
        
        // Configurar encoding UTF-8
        config.withEncoding("UTF-8");
        
        // Configuraciones adicionales recomendadas
        config.withStrictIJSON(false); // Más tolerante con formato JSON
        config.withNullValues(true);   // Incluir valores null
        
        return JsonbBuilder.create(config);
    }

}