package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CxcKardexParticipeDaoService;
import com.saa.ejb.credito.service.CxcKardexParticipeService;
import com.saa.model.credito.CxcKardexParticipe;
import com.saa.model.credito.NombreEntidadesCredito;

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
    @Produces("application/json")
    public List<CxcKardexParticipe> getAll() throws Throwable {
        return cxcKardexParticipeDaoService.selectAll(NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
    }

    /**
     * Retrieves CxcKardexParticipe by ID
     * 
     * @return CxcKardexParticipe
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CxcKardexParticipe getId(@PathParam("id") Long id) throws Throwable {
        return cxcKardexParticipeDaoService.selectById(id, NombreEntidadesCredito.CXC_KARDEX_PARTICIPE);
    }

    /**
     * PUT method for updating or creating CxcKardexParticipe
     */
    @PUT
    @Consumes("application/json")
    public CxcKardexParticipe put(CxcKardexParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CxcKardexParticipe");
        return cxcKardexParticipeService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating CxcKardexParticipe
     */
    @POST
    @Consumes("application/json")
    public CxcKardexParticipe post(CxcKardexParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CxcKardexParticipe");
        return cxcKardexParticipeService.saveSingle(registro);
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
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - CxcKardexParticipe");
        CxcKardexParticipe elimina = new CxcKardexParticipe();
        cxcKardexParticipeDaoService.remove(elimina, id);
    }

}
