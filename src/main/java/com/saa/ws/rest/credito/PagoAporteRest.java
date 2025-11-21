package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PagoAporteDaoService;
import com.saa.ejb.credito.service.PagoAporteService;
import com.saa.model.credito.PagoAporte;
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

@Path("pgap")
public class PagoAporteRest {

    @EJB
    private PagoAporteDaoService pagoAporteDaoService;

    @EJB
    private PagoAporteService pagoAporteService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PagoAporteRest() {
        // TODO Auto-generated constructor 
    }

    /**
     * Retrieves representation of an instance of PagoAporteRest
     * 
     * @return an instance of List<PagoAporte>
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PagoAporte> getAll() throws Throwable {
        return pagoAporteDaoService.selectAll(NombreEntidadesCredito.PAGO_APORTE);
    }

    /**
     * Retrieves representation of an instance of PagoAporteRest
     * 
     * @return an instance of PagoAporte
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public PagoAporte getId(@PathParam("id") Long id) throws Throwable {
        return pagoAporteDaoService.selectById(id, NombreEntidadesCredito.PAGO_APORTE);
    }

    /**
     * PUT method for updating or creating an instance of PagoAporte
     * 
     * @param registro representation for the resource
     * @return the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public PagoAporte put(PagoAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - PagoAporte");
        return pagoAporteService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PagoAporte
     * 
     * @param registro representation for the resource
     * @return the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public PagoAporte post(PagoAporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - PagoAporte");
        return pagoAporteService.saveSingle(registro);
    }

    /**
     * POST method for selecting PagoAporte by criteria
     * 
     * @param registros list of search criteria
     * @return filtered list or an error message
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PagoAporte");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(pagoAporteService.selectByCriteria(registros))
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
     * DELETE method for deleting an instance of PagoAporte
     * 
     * @param id identifier for the resource
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - PagoAporte");
        PagoAporte elimina = new PagoAporte();
        pagoAporteDaoService.remove(elimina, id);
    }

}
