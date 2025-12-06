package com.saa.ws.rest.asoprep;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.ejb.asoprep.service.CargaArchivoPetroService;
import com.saa.ejb.credito.service.EstadoCivilService;
import com.saa.model.credito.CargaArchivo;
import com.saa.model.credito.DetalleCargaArchivo;
import com.saa.model.credito.ParticipeXCargaArchivo;
import com.saa.ws.rest.files.FileResponse;

import jakarta.ejb.EJB;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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

        System.out.println("LLEGA A PROCESAR EL ARCHIVO PETRO - formData para archivos extensos");
        System.out.println("=== ENTORNO WILDFLY CHARSET ===");
        System.out.println("Charset por defecto JVM: " + Charset.defaultCharset().name());
        System.out.println("file.encoding: " + System.getProperty("file.encoding"));
        System.out.println("sun.jnu.encoding: " + System.getProperty("sun.jnu.encoding"));
        System.out.println("user.language: " + System.getProperty("user.language"));
        System.out.println("user.country: " + System.getProperty("user.country"));
        System.out.println("java.version: " + System.getProperty("java.version"));
        System.out.println("===============================\n");
        
        //analizarDatosJSON(participesXCargaArchivoJson, "participesXCargaArchivo");
        System.out.println("=============================================");
        System.out.println(participesXCargaArchivoJson);
        System.out.println("=============================================");
        try {
            // Validar archivo
            if (archivoInputStream == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "No se ha enviado el archivo", null))
                        .build();
            }

            String fileName = archivoNombre;
            System.out.println("Archivo encontrado: " + fileName);

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

            // Log para verificación
            System.out.println("Procesando archivo: " + fileName);
            System.out.println("CargaArchivo: " + cargaArchivo.getNombre());
            System.out.println("Filial: " + (cargaArchivo.getFilial() != null ? cargaArchivo.getFilial().getNombre() : "null"));
            System.out.println("Usuario: " + (cargaArchivo.getUsuarioCarga() != null ? cargaArchivo.getUsuarioCarga().getCodigo() : "null"));
            System.out.println("Cantidad de detalles: " + detallesCargaArchivos.size());
            System.out.println("Cantidad de partícipes: " + participesXCargaArchivo.size());

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

        System.out.println("LLEGA A PROCESAR EL ARCHIVO PETRO - formData para archivos extensos");
        System.out.println("java.version: " + System.getProperty("java.version"));
        System.out.println("===============================\n");
        
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
            String rutaArchivo;
            try {
                rutaArchivo = cargaArchivoPetroService.validarArchivoPetro(
                    archivoInputStream, fileName, cargaArchivo);
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
    
    /**
     * Analiza los datos JSON recibidos ANTES del parsing
     */
    private void analizarDatosJSON(String jsonData, String nombre) {
        if (jsonData == null) return;

        System.out.println("=== ANÁLISIS " + nombre.toUpperCase() + " ===");

        // Buscar nombres con caracteres especiales en el JSON crudo
        String[] lineas = jsonData.split("\\n");
        for (int i = 0; i < Math.min(10, lineas.length); i++) {
            String linea = lineas[i];
            if (linea.toLowerCase().contains("nombre") && 
                (linea.matches(".*[áéíóúñüÁÉÍÓÚÑÜ].*") || linea.contains("�"))) {
                System.out.println("Línea " + (i+1) + ": " + linea);
                
                if (linea.contains("�")) {
                    System.out.println("  ⚠️ PROBLEMA: JSON YA TIENE CARACTERES CORRUPTOS");
                } else {
                    System.out.println("  ✅ BIEN: Caracteres especiales correctos en JSON");
                }
            }
        }

        // Análisis general
        boolean tieneCaracteresCorruptos = jsonData.contains("�") || jsonData.contains("??");
        boolean tieneCaracteresEspeciales = jsonData.matches(".*[áéíóúñüÁÉÍÓÚÑÜ].*");

        System.out.println("JSON contiene caracteres especiales: " + tieneCaracteresEspeciales);
        System.out.println("JSON contiene caracteres corruptos: " + tieneCaracteresCorruptos);

        if (tieneCaracteresCorruptos) {
            System.out.println("⚠️ CRÍTICO: Los datos JSON ya llegan corruptos desde el frontend");
            System.out.println("⚠️ El problema NO está en WildFly, está en el cliente/frontend");
        }

        System.out.println("=== FIN " + nombre.toUpperCase() + " ===\n");
    }

    /**
     * Verifica los nombres después del parsing JSON
     */
    private void verificarNombresParseados(List<ParticipeXCargaArchivo> participesXCargaArchivo) {
        System.out.println("=== VERIFICACIÓN NOMBRES PARSEADOS ===");

        if (participesXCargaArchivo != null && !participesXCargaArchivo.isEmpty()) {
            System.out.println("Total partícipes parseados: " + participesXCargaArchivo.size());

            // Verificar primeros 20 nombres para encontrar corruptos
            int limite = Math.min(20, participesXCargaArchivo.size());
            for (int i = 0; i < limite; i++) {
                ParticipeXCargaArchivo participe = participesXCargaArchivo.get(i);
                String nombre = participe.getNombre();

                if (nombre != null) {
                    boolean tieneCorruptos = nombre.contains("�") || nombre.contains("??");
                    boolean tieneEspeciales = nombre.matches(".*[áéíóúñüÁÉÍÓÚÑÜ].*");

                    // Solo mostrar los que tienen problemas o caracteres especiales
                    if (tieneCorruptos || tieneEspeciales) {
                        System.out.println("Partícipe " + (i+1) + ": " + nombre);
                        
                        if (tieneCorruptos) {
                            System.out.println("  ⚠️ CORRUPTO: El JSON ya venía corrupto desde el frontend");
                        } else if (tieneEspeciales) {
                            System.out.println("  ✅ CORRECTO: Caracteres especiales preservados");
                        }
                    }
                }
            }
        }

        System.out.println("=== FIN VERIFICACIÓN NOMBRES ===\n");
    }

    /**
     * Analiza los headers HTTP para detectar problemas de charset
     */
    private void analizarHeadersHTTP(HttpHeaders headers) {
        System.out.println("=== HEADERS HTTP ===");
        
        String contentType = headers.getHeaderString("Content-Type");
        String acceptCharset = headers.getHeaderString("Accept-Charset");
        String userAgent = headers.getHeaderString("User-Agent");
        
        System.out.println("Content-Type: " + contentType);
        System.out.println("Accept-Charset: " + acceptCharset);
        System.out.println("User-Agent: " + userAgent);
        
        // Verificar si el Content-Type especifica charset
        if (contentType != null) {
            if (contentType.toLowerCase().contains("charset=utf-8")) {
                System.out.println("✅ Content-Type especifica UTF-8");
            } else if (contentType.toLowerCase().contains("charset=")) {
                System.out.println("⚠️ Content-Type especifica charset diferente a UTF-8: " + contentType);
            } else {
                System.out.println("⚠️ Content-Type NO especifica charset - usando por defecto");
            }
        }
        
        System.out.println("===================\n");
    }

    

}