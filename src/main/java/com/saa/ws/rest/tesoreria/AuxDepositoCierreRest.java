package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.AuxDepositoCierreDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoCierreService;
import com.saa.model.tsr.AuxDepositoCierre;
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

@Path("acpd")
public class AuxDepositoCierreRest {

    @EJB
    private AuxDepositoCierreDaoService auxDepositoCierreDaoService;

    @EJB
    private AuxDepositoCierreService auxDepositoCierreService;

    @Context
    private UriInfo context;

    public AuxDepositoCierreRest() {
    }

    /**
     * Obtiene todos los registros de AuxDepositoCierre.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AuxDepositoCierre> lista = auxDepositoCierreDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cierres de depósito auxiliares: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un registro por ID.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            AuxDepositoCierre auxDepositoCierre = auxDepositoCierreDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
            if (auxDepositoCierre == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("AuxDepositoCierre con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(auxDepositoCierre).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cierre de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza o crea un registro.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AuxDepositoCierre registro) {
        System.out.println("LLEGA AL SERVICIO PUT - AUX_DEPOSITO_CIERRE");
        try {
            AuxDepositoCierre resultado = auxDepositoCierreService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cierre de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AuxDepositoCierre registro) {
        System.out.println("LLEGA AL SERVICIO POST - AUX_DEPOSITO_CIERRE");
        try {
            AuxDepositoCierre resultado = auxDepositoCierreService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cierre de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of AuxDepositoCierreRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de AUX_DEPOSITO_CIERRE");
        try {
            return Response.status(Response.Status.OK)
                    .entity(auxDepositoCierreService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro.
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - AUX_DEPOSITO_CIERRE");
        try {
            AuxDepositoCierre elimina = new AuxDepositoCierre();
            auxDepositoCierreDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cierre de depósito auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
