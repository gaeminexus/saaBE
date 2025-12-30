package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DesgloseDetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.DesgloseDetalleDepositoService;
import com.saa.model.tesoreria.DesgloseDetalleDeposito;
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

@Path("dsdt")
public class DesgloseDetalleDepositoRest {

    @EJB
    private DesgloseDetalleDepositoDaoService desgloseDetalleDepositoDaoService;

    @EJB
    private DesgloseDetalleDepositoService desgloseDetalleDepositoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DesgloseDetalleDepositoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DesgloseDetalleDeposito.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DesgloseDetalleDeposito> lista = desgloseDetalleDepositoDaoService.selectAll(NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desgloses de detalle de depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de DesgloseDetalleDeposito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DesgloseDetalleDeposito desgloseDetalleDeposito = desgloseDetalleDepositoDaoService.selectById(id, NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO);
            if (desgloseDetalleDeposito == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DesgloseDetalleDeposito con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(desgloseDetalleDeposito).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener desglose de detalle de depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DesgloseDetalleDeposito registro) {
        System.out.println("LLEGA AL SERVICIO PUT DESGLOSE DETALLE DEPOSITO");
        try {
            DesgloseDetalleDeposito resultado = desgloseDetalleDepositoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar desglose de detalle de depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DesgloseDetalleDeposito registro) {
        System.out.println("LLEGA AL SERVICIO POST DESGLOSE DETALLE DEPOSITO");
        try {
            DesgloseDetalleDeposito resultado = desgloseDetalleDepositoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear desglose de detalle de depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of DesgloseDetalleDepositoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DESGLOSE_DETALLE_DEPOSITO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(desgloseDetalleDepositoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de DesgloseDetalleDeposito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DESGLOSE DETALLE DEPOSITO");
        try {
            DesgloseDetalleDeposito elimina = new DesgloseDetalleDeposito();
            desgloseDetalleDepositoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar desglose de detalle de depósito: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
