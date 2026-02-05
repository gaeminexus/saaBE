package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.TipoContratoDaoService;
import com.saa.ejb.crd.service.TipoContratoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoContrato;

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

@Path("tpcn")
public class TipoContratoRest {

    @EJB
    private TipoContratoDaoService tipoContratoDaoService;

    @EJB
    private TipoContratoService tipoContratoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoContratoRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoContrato> lista = tipoContratoDaoService.selectAll(NombreEntidadesCredito.TIPO_CONTRATO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoContrato tipo = tipoContratoDaoService.selectById(id, NombreEntidadesCredito.TIPO_CONTRATO);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TipoContrato con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo de contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoContrato registro) {
        System.out.println("LLEGA AL SERVICIO PUT TipoContrato");
        try {
            TipoContrato resultado = tipoContratoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo de contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoContrato registro) {
        System.out.println("LLEGA AL SERVICIO POST TipoContrato");
        try {
            TipoContrato resultado = tipoContratoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo de contrato: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - SELECT BY CRITERIA
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoContrato");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoContratoService.selectByCriteria(registros))
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
     * DELETE
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TipoContrato");
        TipoContrato elimina = new TipoContrato();
        tipoContratoDaoService.remove(elimina, id);
    }

}
