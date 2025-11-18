package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ContratoDaoService;
import com.saa.ejb.credito.service.ContratoService;
import com.saa.model.credito.Contrato;
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

@Path("cntr")
public class ContratoRest {

    @EJB
    private ContratoDaoService contratoDaoService;

    @EJB
    private ContratoService contratoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ContratoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ContratoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Contrato> getAll() throws Throwable {
        return contratoDaoService.selectAll(NombreEntidadesCredito.CONTRATO);
    }

    /**
     * Retrieves representation of an instance of ContratoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Contrato getId(@PathParam("id") Long id) throws Throwable {
        return contratoDaoService.selectById(id, NombreEntidadesCredito.CONTRATO);
    }

    /**
     * PUT method for updating or creating an instance of ContratoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Contrato put(Contrato registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - CONTRATO");
        return contratoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ContratoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Contrato post(Contrato registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - CONTRATO");
        return contratoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ContratoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Contrato");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(contratoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * DELETE method for deleting an instance of ContratoRest
     * 
     * @param id identifier for the resource
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - CONTRATO");
        Contrato elimina = new Contrato();
        contratoDaoService.remove(elimina, id);
    }

}
