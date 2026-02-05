package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DetalleDebitoCreditoService;
import com.saa.model.tsr.DetalleDebitoCredito;
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

@Path("dtdc")
public class DetalleDebitoCreditoRest {

    @EJB
    private DetalleDebitoCreditoDaoService detalleDebitoCreditoDaoService;

    @EJB
    private DetalleDebitoCreditoService detalleDebitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleDebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleDebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleDebitoCredito> lista = detalleDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de débito/crédito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de DetalleDebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleDebitoCredito detalleDebitoCredito = detalleDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
            if (detalleDebitoCredito == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DetalleDebitoCredito con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalleDebitoCredito).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de débito/crédito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleDebitoCredito registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE DEBITO CREDITO");
        try {
            DetalleDebitoCredito resultado = detalleDebitoCreditoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de débito/crédito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleDebitoCredito registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE DEBITO CREDITO");
        try {
            DetalleDebitoCredito resultado = detalleDebitoCreditoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de débito/crédito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of DetalleDebitoCreditoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_DEBITO_CREDITO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(detalleDebitoCreditoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de DetalleDebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE DEBITO CREDITO");
        try {
            DetalleDebitoCredito elimina = new DetalleDebitoCredito();
            detalleDebitoCreditoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de débito/crédito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
