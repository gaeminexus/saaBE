package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.AuxDepositoDesgloseDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoDesgloseService;
import com.saa.model.tsr.AuxDepositoDesglose;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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

@Path("apds")
public class AuxDepositoDesgloseRest {

    @EJB
    private AuxDepositoDesgloseDaoService auxDepositoDesgloseDaoService;

    @EJB
    private AuxDepositoDesgloseService auxDepositoDesgloseService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AuxDepositoDesgloseRest() {
        // Constructor por defecto
    }

    /**
     * Recupera todos los registros de AuxDepositoDesglose.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AuxDepositoDesglose> lista = auxDepositoDesgloseDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desgloses de depósito auxiliares: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro por ID.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            AuxDepositoDesglose auxDepositoDesglose = auxDepositoDesgloseDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
            if (auxDepositoDesglose == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("AuxDepositoDesglose con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(auxDepositoDesglose).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desglose de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AuxDepositoDesglose registro) {
        System.out.println("LLEGA AL SERVICIO PUT - AUX_DEPOSITO_DESGLOSE");
        try {
            AuxDepositoDesglose resultado = auxDepositoDesgloseService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar desglose de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AuxDepositoDesglose registro) {
        System.out.println("LLEGA AL SERVICIO POST - AUX_DEPOSITO_DESGLOSE");
        try {
            AuxDepositoDesglose resultado = auxDepositoDesgloseService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear desglose de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of AuxDepositoDesgloseRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de AUX_DEPOSITO_DESGLOSE");
        try {
            return Response.status(Response.Status.OK)
                    .entity(auxDepositoDesgloseService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro por ID.
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - AUX_DEPOSITO_DESGLOSE");
        try {
            AuxDepositoDesglose elimina = new AuxDepositoDesglose();
            auxDepositoDesgloseDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar desglose de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
