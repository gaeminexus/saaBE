package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.NivelEstudioDaoService;
import com.saa.ejb.credito.service.NivelEstudioService;
import com.saa.model.credito.NivelEstudio;
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

@Path("nvls")
public class NivelEstudioRest {

    @EJB
    private NivelEstudioDaoService nivelEstudioDaoService;

    @EJB
    private NivelEstudioService nivelEstudioService;

    @Context
    private UriInfo context;

    public NivelEstudioRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NivelEstudio> lista = nivelEstudioDaoService.selectAll(NombreEntidadesCredito.NIVEL_ESTUDIO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener niveles de estudio: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NivelEstudio nivel = nivelEstudioDaoService.selectById(id, NombreEntidadesCredito.NIVEL_ESTUDIO);
            if (nivel == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("NivelEstudio con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(nivel).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener nivel de estudio: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NivelEstudio registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            NivelEstudio resultado = nivelEstudioService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar nivel de estudio: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NivelEstudio registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            NivelEstudio resultado = nivelEstudioService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear nivel de estudio: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Nivel Estudio");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(nivelEstudioService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            NivelEstudio elimina = new NivelEstudio();
            nivelEstudioDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar nivel de estudio: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
