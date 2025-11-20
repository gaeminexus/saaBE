package com.saa.ws.rest.files;

import java.io.InputStream;
import java.util.List;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.saa.basico.ejb.FileService;

/**
 * @author GaemiSoft.
 *         <p>
 *         Servicios REST para manejo de archivos (upload/download).
 *         </p>
 */
@Path("file")
public class FileRest {

    @EJB
    private FileService fileService;
    
    /**
     * Upload de archivo básico
     * 
     * @param inputStream : Stream del archivo
     * @param fileName : Nombre del archivo via query param
     * @return Response con el resultado del upload
     */
    @POST
    @Path("upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(InputStream inputStream, @QueryParam("fileName") String fileName) {
        try {
            // Validar que se haya enviado un archivo
            if (inputStream == null || fileName == null || fileName.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "No se ha enviado archivo o nombre de archivo", null))
                        .build();
            }

            // Validar extensión
            if (!fileService.validarExtension(fileName)) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "Extensión de archivo no permitida", null))
                        .build();
            }

            // Subir archivo usando InputStream directamente (EJB @Local)
            String filePath = fileService.uploadFile(inputStream, fileName);

            return Response.ok(new FileResponse(true, "Archivo subido exitosamente", filePath)).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al subir archivo: " + e.getMessage(), null))
                    .build();
        }
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
     * Download de archivo
     * 
     * @param filePath : Ruta del archivo a descargar
     * @return Response con el archivo
     */
    @GET
    @Path("download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadFile(@QueryParam("filePath") String filePath) {
        try {
            // Validar parámetros
            if (filePath == null || filePath.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity("La ruta del archivo es requerida")
                        .build();
            }

            // Verificar que el archivo existe
            if (!fileService.fileExists(filePath)) {
                return Response.status(Status.NOT_FOUND)
                        .entity("El archivo no existe")
                        .build();
            }

            // Obtener el archivo
            InputStream fileStream = fileService.downloadFile(filePath);
            
            // Extraer nombre del archivo de la ruta
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            return Response.ok(fileStream)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al descargar archivo: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Eliminar archivo
     * 
     * @param filePath : Ruta del archivo a eliminar
     * @return Response con el resultado
     */
    @DELETE
    @Path("delete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFile(@QueryParam("filePath") String filePath) {
        try {
            // Validar parámetros
            if (filePath == null || filePath.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "La ruta del archivo es requerida", null))
                        .build();
            }

            // Eliminar archivo
            boolean deleted = fileService.deleteFile(filePath);

            if (deleted) {
                return Response.ok(new FileResponse(true, "Archivo eliminado exitosamente", null)).build();
            } else {
                return Response.status(Status.NOT_FOUND)
                        .entity(new FileResponse(false, "El archivo no existe o no se pudo eliminar", null))
                        .build();
            }

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al eliminar archivo: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * Listar archivos en un directorio
     * 
     * @param directoryPath : Ruta del directorio
     * @return Response con la lista de archivos
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFiles(@QueryParam("directoryPath") String directoryPath) {
        try {
            // Usar directorio por defecto si no se especifica
            if (directoryPath == null || directoryPath.trim().isEmpty()) {
                directoryPath = "uploads/"; // Directorio por defecto
            }

            List<String> files = fileService.listFiles(directoryPath);

            return Response.ok(new FileListResponse(true, "Archivos listados exitosamente", files)).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileListResponse(false, "Error al listar archivos: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * Obtener información de un archivo
     * 
     * @param filePath : Ruta del archivo
     * @return Response con la información del archivo
     */
    @GET
    @Path("info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileInfo(@QueryParam("filePath") String filePath) {
        try {
            // Validar parámetros
            if (filePath == null || filePath.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileInfoResponse(false, "La ruta del archivo es requerida", null))
                        .build();
            }

            // Verificar que el archivo existe
            if (!fileService.fileExists(filePath)) {
                return Response.status(Status.NOT_FOUND)
                        .entity(new FileInfoResponse(false, "El archivo no existe", null))
                        .build();
            }

            // Obtener información del archivo
            long fileSize = fileService.getFileSize(filePath);
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

            FileInfo fileInfo = new FileInfo(fileName, filePath, fileSize);

            return Response.ok(new FileInfoResponse(true, "Información obtenida exitosamente", fileInfo)).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileInfoResponse(false, "Error al obtener información: " + e.getMessage(), null))
                    .build();
        }
    }

    /**
     * Validar archivo (extensión y tamaño)
     * 
     * @param fileName : Nombre del archivo
     * @param fileSize : Tamaño del archivo en bytes
     * @return Response con el resultado de la validación
     */
    @GET
    @Path("/validate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateFile(@QueryParam("fileName") String fileName,
                               @QueryParam("fileSize") Long fileSize) {
        try {
            // Validar parámetros
            if (fileName == null || fileName.trim().isEmpty()) {
                return Response.status(Status.BAD_REQUEST)
                        .entity(new FileResponse(false, "El nombre del archivo es requerido", null))
                        .build();
            }

            boolean extensionValida = fileService.validarExtension(fileName);
            boolean tamañoValido = fileSize != null ? fileService.validarTamaño(fileSize) : true;

            String mensaje;
            if (extensionValida && tamañoValido) {
                mensaje = "Archivo válido";
            } else if (!extensionValida) {
                mensaje = "Extensión de archivo no permitida";
            } else {
                mensaje = "Tamaño de archivo excede el límite permitido";
            }

            boolean esValido = extensionValida && tamañoValido;

            return Response.ok(new FileValidationResponse(esValido, mensaje, extensionValida, tamañoValido)).build();

        } catch (Throwable e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(new FileResponse(false, "Error al validar archivo: " + e.getMessage(), null))
                    .build();
        }
    }
    
}
