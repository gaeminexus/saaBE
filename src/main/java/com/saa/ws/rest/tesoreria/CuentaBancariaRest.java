package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CuentaBancariaDaoService;
import com.saa.ejb.tesoreria.service.CuentaBancariaService;
import com.saa.model.tsr.CuentaBancaria;
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

@Path("cnbc")
public class CuentaBancariaRest {

    @EJB
    private CuentaBancariaDaoService cuentaBancariaDaoService;

    @EJB
    private CuentaBancariaService cuentaBancariaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CuentaBancariaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CuentaBancaria.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CuentaBancaria> lista = cuentaBancariaDaoService.selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuentas bancarias: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CuentaBancaria cuentaBancaria = cuentaBancariaDaoService.selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA);
            if (cuentaBancaria == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CuentaBancaria con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cuentaBancaria).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuenta bancaria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaBancaria registro) {
        System.out.println("LLEGA AL SERVICIO PUT CUENTA BANCARIA");
        try {
            CuentaBancaria resultado = cuentaBancariaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cuenta bancaria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuentaBancaria registro) {
        System.out.println("LLEGA AL SERVICIO POST CUENTA BANCARIA");
        try {
            CuentaBancaria resultado = cuentaBancariaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cuenta bancaria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of CuentaBancariaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CUENTA_BANCARIA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(cuentaBancariaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de CuentaBancaria por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CUENTA BANCARIA");
        try {
            CuentaBancaria elimina = new CuentaBancaria();
            cuentaBancariaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cuenta bancaria: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
