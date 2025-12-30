package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PerfilEconomicoDaoService;
import com.saa.ejb.credito.service.PerfilEconomicoService;
import com.saa.model.credito.PerfilEconomico;
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

@Path("prec")
public class PerfilEconomicoRest {

    @EJB
    private PerfilEconomicoDaoService perfilEconomicoDaoService;

    @EJB
    private PerfilEconomicoService perfilEconomicoService;

    @Context
    private UriInfo context;

    public PerfilEconomicoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PerfilEconomico> lista = perfilEconomicoDaoService.selectAll(NombreEntidadesCredito.PERFIL_ECONOMICO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener perfiles económicos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PerfilEconomico perfil = perfilEconomicoDaoService.selectById(id, NombreEntidadesCredito.PERFIL_ECONOMICO);
            if (perfil == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("PerfilEconomico con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(perfil).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener perfil económico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PerfilEconomico registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PERFIL ECONOMICO");
        try {
            PerfilEconomico resultado = perfilEconomicoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar perfil económico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PerfilEconomico registro) {
        System.out.println("LLEGA AL SERVICIO POST - PERFIL ECONOMICO");
        try {
            PerfilEconomico resultado = perfilEconomicoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear perfil económico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PERFIL ECONOMICO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(perfilEconomicoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PERFIL ECONOMICO");
        try {
            PerfilEconomico elimina = new PerfilEconomico();
            perfilEconomicoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar perfil económico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
