package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DepositoDaoService;
import com.saa.ejb.tesoreria.service.DepositoService;
import com.saa.model.tesoreria.Deposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("dpst")
public class DepositoRest {

    @EJB
    private DepositoDaoService depositoDaoService;

    @EJB
    private DepositoService depositoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DepositoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Deposito.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Deposito> lista = depositoDaoService.selectAll(NombreEntidadesTesoreria.DEPOSITO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener depósitos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de Deposito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Deposito deposito = depositoDaoService.selectById(id, NombreEntidadesTesoreria.DEPOSITO);
            if (deposito == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Deposito con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(deposito).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Deposito registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DEPOSITO");
        try {
            Deposito resultado = depositoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Deposito registro) {
        System.out.println("LLEGA AL SERVICIO POST - DEPOSITO");
        try {
            Deposito resultado = depositoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of DepositoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DEPOSITO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(depositoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de Deposito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - DEPOSITO");
        try {
            Deposito elimina = new Deposito();
            depositoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
