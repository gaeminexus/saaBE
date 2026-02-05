package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CxcKardexParticipeDaoService;
import com.saa.ejb.crd.service.CxcKardexParticipeService;
import com.saa.model.crd.CxcKardexParticipe;
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

@Path("cxckardexparticipe")
public class CxcKardexParticipeRest {

    @EJB
    private CxcKardexParticipeDaoService cxcKardexParticipeDaoService;

    @EJB
    private CxcKardexParticipeService cxcKardexParticipeService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public CxcKardexParticipeRest() {
        // TODO Auto-generated constructor
    }

    /**
     * Retrieves all CxcKardexParticipe records
     * 
     * @return list of CxcKardexParticipe
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CxcKardexParticipe> lista = cxcKardexParticipeDaoService.selectAll(NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener kardex: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CxcKardexParticipe kardex = cxcKardexParticipeDaoService.selectById(id, NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
            if (kardex == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CxcKardexParticipe con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(kardex).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener kardex: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CxcKardexParticipe registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CxcKardexParticipe");
        try {
            CxcKardexParticipe resultado = cxcKardexParticipeService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar kardex: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CxcKardexParticipe registro) {
        System.out.println("LLEGA AL SERVICIO POST - CxcKardexParticipe");
        try {
            CxcKardexParticipe resultado = cxcKardexParticipeService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear kardex: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for selecting CxcKardexParticipe by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CxcKardexParticipe");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(cxcKardexParticipeService.selectByCriteria(registros))
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
     * DELETE method for deleting a CxcKardexParticipe record
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CxcKardexParticipe");
        try {
            CxcKardexParticipe elimina = new CxcKardexParticipe();
            cxcKardexParticipeDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar kardex: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
