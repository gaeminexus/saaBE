package com.saa.ws.rest.crd;

import java.io.InputStream;
import java.util.List;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("prst")
public class PrestamoRest {
    
    @EJB
    private PrestamoDaoService prestamoDaoService;
    
    @EJB
    private PrestamoService prestamoService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public PrestamoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Prestamo.
     * 
     * @return Response con lista de Prestamo
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Prestamo> prestamos = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
            return Response.status(Response.Status.OK)
                    .entity(prestamos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de Prestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Prestamo
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Prestamo prestamo = prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
            if (prestamo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Prestamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (PUT).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (POST).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Consulta registros de Prestamo por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de Prestamo
     * @throws Throwable
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Prestamo Cuenta");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK).entity(prestamoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    	return respuesta;
    }
    
    /**
     * Elimina un registro de Prestamo por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Prestamo");
        try {
            Prestamo elimina = new Prestamo();
            prestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Genera la tabla de amortización para un préstamo.
     * 
     * @param id Identificador del préstamo
     * @param tieneCuotaCero Indica si tiene período de gracia (1=sí, 0=no). Por defecto 0.
     * @return Response con el préstamo actualizado
     */
    @POST
    @Path("/generarTablaAmortizacion/{id}/{tieneCuotaCero}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarTablaAmortizacion(
            @PathParam("id") Long id,
            @PathParam("tieneCuotaCero") Long tieneCuotaCero) {
        System.out.println("GENERAR TABLA DE AMORTIZACIÓN - Préstamo ID: " + id + ", Cuota 0: " + tieneCuotaCero);
        try {
            Prestamo prestamo = prestamoService.generarTablaAmortizacion(id, tieneCuotaCero);
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar tabla de amortización: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Carga la tabla de amortización desde un archivo Excel.
     * 
     * @param id Identificador del préstamo
     * @param uploadedInputStream InputStream del archivo
     * @param fileDetail Detalles del archivo
     * @return Response con el préstamo actualizado
     */
    @POST
    @Path("/cargarTablaExcel/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarTablaAmortizacionDesdeExcel(
            @PathParam("id") Long id,
            @org.jboss.resteasy.annotations.providers.multipart.MultipartForm MultipartFormDataInput input) {
        System.out.println("CARGAR TABLA DE AMORTIZACIÓN DESDE EXCEL - Préstamo ID: " + id);
        
        InputStream inputStream = null;
        try {
            // Obtener el archivo del multipart form
            java.util.Map<String, java.util.List<org.jboss.resteasy.plugins.providers.multipart.InputPart>> uploadForm = 
                input.getFormDataMap();
            
            System.out.println("Partes del formulario recibidas: " + uploadForm.keySet());
            
            // Intentar obtener el archivo con diferentes nombres de campo
            java.util.List<org.jboss.resteasy.plugins.providers.multipart.InputPart> inputParts = uploadForm.get("file");
            
            if (inputParts == null || inputParts.isEmpty()) {
                // Intentar con otros nombres comunes
                inputParts = uploadForm.get("archivo");
                if (inputParts == null || inputParts.isEmpty()) {
                    inputParts = uploadForm.get("fileData");
                }
            }
            
            if (inputParts == null || inputParts.isEmpty()) {
                System.out.println("ERROR: No se encontró el archivo en ningún campo conocido");
                System.out.println("Campos disponibles: " + uploadForm.keySet());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se recibió ningún archivo. Campos disponibles: " + uploadForm.keySet())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            
            org.jboss.resteasy.plugins.providers.multipart.InputPart inputPart = inputParts.get(0);
            
            // Obtener el InputStream del archivo
            inputStream = inputPart.getBody(InputStream.class, null);
            
            if (inputStream == null) {
                System.out.println("ERROR: El InputStream del archivo es null");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El archivo recibido está vacío")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            
            System.out.println("Archivo recibido correctamente, procesando...");
            Prestamo prestamo = prestamoService.cargarTablaAmortizacionDesdeExcel(id, inputStream);
            
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
                    
        } catch (Throwable e) {
            System.err.println("ERROR al cargar tabla desde Excel:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cargar tabla desde Excel: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Aplica un abono a capital al préstamo y recalcula la tabla de amortización.
     * 
     * @param id Identificador del préstamo
     * @param valorAbono Valor del abono a capital
     * @param opcionRecalculo 1=Mantener plazo/reducir cuota, 2=Reducir plazo/mantener cuota
     * @return Response con el préstamo actualizado
     */
    @POST
    @Path("/aplicarAbonoCapital/{id}/{valorAbono}/{opcionRecalculo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarAbonoCapital(
            @PathParam("id") Long id,
            @PathParam("valorAbono") Double valorAbono,
            @PathParam("opcionRecalculo") Integer opcionRecalculo) {
        System.out.println("APLICAR ABONO A CAPITAL - Préstamo ID: " + id + 
                         ", Abono: " + valorAbono + ", Opción: " + opcionRecalculo);
        try {
            Prestamo prestamo = prestamoService.aplicarAbonoCapital(id, valorAbono, opcionRecalculo);
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            System.err.println("ERROR al aplicar abono a capital:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al aplicar abono a capital: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Clase interna para manejar la carga de archivos multipart.
     */
    public static class FileUploadForm {
        
        private InputStream fileData;
        private String fileName;
        
        @jakarta.ws.rs.FormParam("file")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("application/octet-stream")
        public InputStream getFileData() {
            return fileData;
        }
        
        public void setFileData(InputStream fileData) {
            this.fileData = fileData;
        }
        
        @jakarta.ws.rs.FormParam("fileName")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("text/plain")
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}