package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.CreditoJubiladosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoJubiladosMensualService;
import com.saa.model.rpr.CreditoJubiladosMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("cjbm")
public class CreditoJubiladosMensualRest {

    @EJB private CreditoJubiladosMensualDaoService creditoJubiladosMensualDaoService;
    @EJB private CreditoJubiladosMensualService creditoJubiladosMensualService;
    @Context private UriInfo context;

    public CreditoJubiladosMensualRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CreditoJubiladosMensual> lista = creditoJubiladosMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_JUBILADOS_MENSUAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CreditoJubiladosMensual entidad = creditoJubiladosMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_JUBILADOS_MENSUAL);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("CreditoJubiladosMensual con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(CreditoJubiladosMensual registro) {
        System.out.println("LLEGA AL SERVICIO PUT CreditoJubiladosMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoJubiladosMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(CreditoJubiladosMensual registro) {
        System.out.println("LLEGA AL SERVICIO POST CreditoJubiladosMensual");
        try {
            return Response.status(Response.Status.CREATED).entity(creditoJubiladosMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CreditoJubiladosMensual con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            creditoJubiladosMensualService.remove(ids);
            return Response.status(Response.Status.OK).entity("CreditoJubiladosMensual eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria CreditoJubiladosMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoJubiladosMensualService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria CreditoJubiladosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
