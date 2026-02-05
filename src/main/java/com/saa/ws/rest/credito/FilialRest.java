package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.FilialDaoService;
import com.saa.ejb.crd.service.FilialService;
import com.saa.model.crd.Filial;
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

@Path("flll")
public class FilialRest {
    
    @EJB
    private FilialDaoService filialDaoService;
    
    @EJB
    private FilialService filialService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public FilialRest() {
        // Constructor vacío
    }
    
    /**
     * Método de prueba simple para verificar conectividad
     */
    @GET
    @Path("/test")
    @Produces("application/json")
    public String test() {
        return "{\"status\":\"OK\",\"message\":\"FilialRest está funcionando correctamente\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}";
    }
    
    /**
     * Método de diagnóstico para verificar inyección de EJBs
     */
    @GET
    @Path("/diagnostico")
    @Produces("application/json")
    public String diagnostico() {
        StringBuilder resultado = new StringBuilder();
        resultado.append("{\"status\":\"OK\",");
        resultado.append("\"ejbDaoInyectado\":").append(filialDaoService != null).append(",");
        resultado.append("\"ejbServiceInyectado\":").append(filialService != null).append(",");
        resultado.append("\"timestamp\":\"").append(java.time.LocalDateTime.now()).append("\"}");
        return resultado.toString();
    }
    
    /**
     * Obtiene todos los registros de Filial.
     * 
     * @return Response con lista de Filial
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Filial> filiales = filialDaoService.selectAll(NombreEntidadesCredito.FILIAL);
            return Response.status(Response.Status.OK)
                    .entity(filiales)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener filiales: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de Filial por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Filial
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Filial filial = filialDaoService.selectById(id, NombreEntidadesCredito.FILIAL);
            if (filial == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Filial con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(filial)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener filial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Filial (PUT).
     * 
     * @param registro Objeto Filial
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Filial registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Filial");
        try {
            Filial resultado = filialService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar filial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Filial (POST).
     * 
     * @param registro Objeto Filial
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Filial registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Filial");
        try {
            Filial resultado = filialService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear filial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Filial");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(filialService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return respuesta;
    }

    
    /**
     * Elimina un registro de Filial por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Filial");
        try {
            Filial elimina = new Filial();
            filialDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar filial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}