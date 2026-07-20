package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.AdendumNegociacionDaoService;
import com.saa.ejb.cxp.service.AdendumNegociacionService;
import com.saa.model.cxp.AdendumNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;

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

@Path("adng")
public class AdendumNegociacionRest {

    @EJB
    private AdendumNegociacionDaoService adendumNegociacionDaoService;

    @EJB
    private AdendumNegociacionService adendumNegociacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AdendumNegociacionRest() {
    }

    /**
     * Obtiene todos los adendums de negociaciones.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AdendumNegociacion> lista = adendumNegociacionDaoService.selectAll(NombreEntidadesCompra.ADENDUM_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener adendums de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un adendum de negociación por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AdendumNegociacion entidad = adendumNegociacionDaoService.selectById(id, NombreEntidadesCompra.ADENDUM_NEGOCIACION);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Adendum de negociación con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener adendum de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un adendum de negociación existente.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AdendumNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ADENDUM_NEGOCIACION");
        try {
            AdendumNegociacion resultado = adendumNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar adendum de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo adendum de negociación.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AdendumNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - ADENDUM_NEGOCIACION");
        try {
            AdendumNegociacion resultado = adendumNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear adendum de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca adendums de negociación por criterios.
     * Uso típico: filtrar por negociacion.id para obtener todos los adendums de una negociación.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ADENDUM_NEGOCIACION");
        try {
            List<AdendumNegociacion> lista = adendumNegociacionService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un adendum de negociación por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ADENDUM_NEGOCIACION id: " + id);
        try {
            AdendumNegociacion elimina = new AdendumNegociacion();
            adendumNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar adendum de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}