package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.PagoAporteDaoService;
import com.saa.ejb.crd.service.PagoAporteService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PagoAporte;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PagoAporte> lista = pagoAporteDaoService.selectAll(NombreEntidadesCredito.PAGO_APORTE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pagos de aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PagoAporte pago = pagoAporteDaoService.selectById(id, NombreEntidadesCredito.PAGO_APORTE);
            if (pago == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("PagoAporte con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(pago).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pago de aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PagoAporte registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PagoAporte");
        try {
            PagoAporte resultado = pagoAporteService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar pago de aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PagoAporte registro) {
        System.out.println("LLEGA AL SERVICIO POST - PagoAporte");
        try {
            PagoAporte resultado = pagoAporteService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear pago de aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PagoAporte");
        try {
            PagoAporte elimina = new PagoAporte();
            pagoAporteDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar pago de aporte: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
