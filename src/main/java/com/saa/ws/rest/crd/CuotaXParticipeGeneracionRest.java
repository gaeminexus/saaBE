package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CuotaXParticipeGeneracionDaoService;
import com.saa.ejb.crd.service.CuotaXParticipeGeneracionService;
import com.saa.model.crd.CuotaXParticipeGeneracion;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("cxpg")
public class CuotaXParticipeGeneracionRest {

    @EJB
    private CuotaXParticipeGeneracionDaoService cuotaXParticipeGeneracionDaoService;

    @EJB
    private CuotaXParticipeGeneracionService cuotaXParticipeGeneracionService;

    @Context
    private UriInfo context;

    public CuotaXParticipeGeneracionRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CuotaXParticipeGeneracion> lista = cuotaXParticipeGeneracionDaoService.selectAll(NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuotas de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CuotaXParticipeGeneracion cuota = cuotaXParticipeGeneracionDaoService.selectById(id, NombreEntidadesCredito.CUOTA_X_PARTICIPE_GENERACION);
            if (cuota == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Cuota de generación con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cuota).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuota de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuotaXParticipeGeneracion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CUOTA_X_PARTICIPE_GENERACION");
        try {
            CuotaXParticipeGeneracion resultado = cuotaXParticipeGeneracionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cuota de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuotaXParticipeGeneracion registro) {
        System.out.println("LLEGA AL SERVICIO POST - CUOTA_X_PARTICIPE_GENERACION");
        try {
            CuotaXParticipeGeneracion resultado = cuotaXParticipeGeneracionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cuota de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CUOTA_X_PARTICIPE_GENERACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(cuotaXParticipeGeneracionService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - CUOTA_X_PARTICIPE_GENERACION");
        try {
            CuotaXParticipeGeneracion elimina = new CuotaXParticipeGeneracion();
            cuotaXParticipeGeneracionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cuota de generación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
