package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.MovimientoBancoDaoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.tesoreria.MovimientoBanco;
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

@Path("mvcb")
public class MovimientoBancoRest {

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public MovimientoBancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de MovimientoBanco.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<MovimientoBanco> lista = movimientoBancoDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener movimientos de banco: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de MovimientoBanco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            MovimientoBanco movimientoBanco = movimientoBancoDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
            if (movimientoBanco == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("MovimientoBanco con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(movimientoBanco).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener movimiento de banco: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(MovimientoBanco registro) {
        System.out.println("LLEGA AL SERVICIO PUT MOVIMIENTO BANCO");
        try {
            MovimientoBanco resultado = movimientoBancoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar movimiento de banco: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(MovimientoBanco registro) {
        System.out.println("LLEGA AL SERVICIO POST MOVIMIENTO BANCO");
        try {
            MovimientoBanco resultado = movimientoBancoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear movimiento de banco: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of MovimientoBancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de MOVIMIENTO_BANCO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(movimientoBancoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de MovimientoBanco por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE MOVIMIENTO BANCO");
        try {
            MovimientoBanco elimina = new MovimientoBanco();
            movimientoBancoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar movimiento de banco: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
