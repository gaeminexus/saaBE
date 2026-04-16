package com.saa.ws.rest.crd;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.GeneracionArchivoPetroDaoService;
import com.saa.ejb.crd.service.GeneracionArchivoPetroService;
import com.saa.model.crd.GeneracionArchivoPetro;
import com.saa.model.crd.NombreEntidadesCredito;

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

/**
 * REST API para Generación de Archivos Petrocomercial.
 * 
 * Este REST SOLO valida parámetros y delega al Service.
 * TODA la lógica de negocio está en GeneracionArchivoPetroService.
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Path("gnap")
public class GeneracionArchivoPetroRest {
	
	@EJB
    private GeneracionArchivoPetroDaoService generacionArchivoPetroDaoService;

    @EJB
    private GeneracionArchivoPetroService generacionArchivoPetroService;

    @Context
    private UriInfo context;

	/**
     * Default constructor.
     */
    public GeneracionArchivoPetroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de GeneracionArchivoPetro.
     * 
     * @return Response con lista de GeneracionArchivoPetro
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<GeneracionArchivoPetro> lista = generacionArchivoPetroDaoService.selectAll(NombreEntidadesCredito.GENERACION_ARCHIVOS_PETRO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener generacionArchivoPetroes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de GeneracionArchivoPetro por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto GeneracionArchivoPetro
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            GeneracionArchivoPetro generacionArchivoPetro = generacionArchivoPetroDaoService.selectById(id, NombreEntidadesCredito.GENERACION_ARCHIVOS_PETRO);
            if (generacionArchivoPetro == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("GeneracionArchivoPetro con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(generacionArchivoPetro)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de GeneracionArchivoPetro (PUT).
     * 
     * @param registro Objeto GeneracionArchivoPetro
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(GeneracionArchivoPetro registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            GeneracionArchivoPetro resultado = generacionArchivoPetroService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * POST - Generar archivo de descuentos Petrocomercial (Paso 2)
     * Recibe el ID de GeneracionArchivoPetro y procesa todo desde el servicio.
     * El usuario se obtiene de la cabecera de generación previamente creada.
     */
    @POST
    @Path("/generarArchivo/{codigoGeneracion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarArchivo(@PathParam("codigoGeneracion") Long codigoGeneracion) {
        try {
            // Validar que se reciba el ID
            if (codigoGeneracion == null || codigoGeneracion <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "ID de generación inválido"))
                    .build();
            }
            
            // Obtener la cabecera para extraer el usuario
            GeneracionArchivoPetro cabecera = generacionArchivoPetroService.buscarPorId(codigoGeneracion);
            if (cabecera == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Generación no encontrada con ID: " + codigoGeneracion))
                    .build();
            }
            
            // Obtener el usuario de la cabecera (quien la creó)
            String usuario = cabecera.getUsuarioGeneracion();
            if (usuario == null || usuario.trim().isEmpty()) {
                usuario = cabecera.getUsuarioIngreso(); // Fallback
            }
            
            // DELEGAR al Service - procesa todo desde el ID de la generación
            Map<String, Object> resultado = generacionArchivoPetroService.procesarGeneracion(
                codigoGeneracion, usuario);
            
            return Response.ok(resultado).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
            }
            
            if (e.getMessage() != null && e.getMessage().contains("ya fue procesada")) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Crea o actualiza un registro de GeneracionArchivoPetro (POST).
     * 
     * @param registro Objeto GeneracionArchivoPetro
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(GeneracionArchivoPetro registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            GeneracionArchivoPetro resultado = generacionArchivoPetroService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST method for updating or creating an instance of GeneracionArchivoPetroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de GeneracionArchivoPetro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(generacionArchivoPetroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de GeneracionArchivoPetro por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            GeneracionArchivoPetro elimina = new GeneracionArchivoPetro();
            generacionArchivoPetroDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}