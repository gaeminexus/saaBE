package com.saa.ws.rest.contabilidad;


import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.TipoAsientoDaoService;
import com.saa.ejb.contabilidad.service.TipoAsientoService;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.TipoAsiento;

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

@Path("plnt")
public class TipoAsientoRest {

    @EJB
    private TipoAsientoDaoService tipoAsientoDaoService;

    @EJB
    private TipoAsientoService tipoAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoAsientoRest() {
        // Constructor por defecto
    }

    /**
     * Obtiene todos los registros de TipoAsiento
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoAsiento> lista = tipoAsientoDaoService.selectAll(NombreEntidadesContabilidad.TIPO_ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /* Comentamos esta parte por que no usamos orden descendente 

    /**
     * Obtiene todos los registros ordenados descendentemente
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getDesc")
    public Response getDesc() {
        try {
            List<TipoAsiento> lista = tipoAsientoDaoService.selectOrderDesc();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Prueba de consulta con Response
     
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(tipoAsientoDaoService.selectOrderDesc()).build();
    }
    */

    /**
     * Obtiene un TipoAsiento por su ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoAsiento tipoAsiento = tipoAsientoDaoService.selectById(id, NombreEntidadesContabilidad.TIPO_ASIENTO);
            if (tipoAsiento == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Tipo de asiento con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipoAsiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza o crea un registro de TipoAsiento (PUT)
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoAsiento registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TIPO_ASIENTO");
        try {
            TipoAsiento resultado = tipoAsientoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro de TipoAsiento (POST)
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoAsiento registro) {
        System.out.println("LLEGA AL SERVICIO POST - TIPO_ASIENTO");
        try {
            TipoAsiento resultado = tipoAsientoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TipoAsientoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TIPO_ASIENTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tipoAsientoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TipoAsiento
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TIPO_ASIENTO");
        try {
            TipoAsiento elimina = new TipoAsiento();
            tipoAsientoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar tipo de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}

