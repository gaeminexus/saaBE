package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoIdentificacionDaoService;
import com.saa.ejb.credito.service.TipoIdentificacionService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoIdentificacion;

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

@Path("tpdn")
public class TipoIdentificacionRest {
    
    @EJB
    private TipoIdentificacionDaoService tipoIdentificacionDaoService;
    
    @EJB
    private TipoIdentificacionService tipoIdentificacionService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoIdentificacionRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoIdentificacion.
     * 
     * @return Lista de TipoIdentificacion
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoIdentificacion> lista = tipoIdentificacionDaoService.selectAll(NombreEntidadesCredito.TIPO_IDENTIFICACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de identificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoIdentificacion tipo = tipoIdentificacionDaoService.selectById(id, NombreEntidadesCredito.TIPO_IDENTIFICACION);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TipoIdentificacion con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo de identificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoIdentificacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoIdentificacion");
        try {
            TipoIdentificacion resultado = tipoIdentificacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo de identificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoIdentificacion registro) {
        System.out.println("LLEGA AL SERVICIO POST DE TipoIdentificacion");
        try {
            TipoIdentificacion resultado = tipoIdentificacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo de identificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoIdentificacion");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoIdentificacionService.selectByCriteria(registros))
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
     * Elimina un registro de TipoIdentificacion por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoIdentificacion");
        TipoIdentificacion elimina = new TipoIdentificacion();
        tipoIdentificacionDaoService.remove(elimina, id);
    }
}
