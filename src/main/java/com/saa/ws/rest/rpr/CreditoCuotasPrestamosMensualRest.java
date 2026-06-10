package com.saa.ws.rest.rpr;

import java.util.ArrayList;
import java.util.List;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rpr.dao.CreditoCuotasPrestamosMensualDaoService;
import com.saa.ejb.rpr.service.CreditoCuotasPrestamosMensualService;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("ccpm")
public class CreditoCuotasPrestamosMensualRest {

    @EJB private CreditoCuotasPrestamosMensualDaoService creditoCuotasPrestamosMensualDaoService;
    @EJB private CreditoCuotasPrestamosMensualService creditoCuotasPrestamosMensualService;
    @Context private UriInfo context;

    public CreditoCuotasPrestamosMensualRest() {}

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CreditoCuotasPrestamosMensual> lista = creditoCuotasPrestamosMensualDaoService.selectAll(NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET @Path("/getId/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CreditoCuotasPrestamosMensual entidad = creditoCuotasPrestamosMensualDaoService.selectById(id, NombreEntidadesReporte.CREDITO_CUOTAS_PRESTAMOS_MENSUAL);
            if (entidad == null) return Response.status(Response.Status.NOT_FOUND).entity("CreditoCuotasPrestamosMensual con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response put(CreditoCuotasPrestamosMensual registro) {
        System.out.println("LLEGA AL SERVICIO PUT CreditoCuotasPrestamosMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoCuotasPrestamosMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response post(CreditoCuotasPrestamosMensual registro) {
        System.out.println("LLEGA AL SERVICIO POST CreditoCuotasPrestamosMensual");
        try {
            return Response.status(Response.Status.CREATED).entity(creditoCuotasPrestamosMensualService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CreditoCuotasPrestamosMensual con id: " + id);
        try {
            List<Long> ids = new ArrayList<>(); ids.add(id);
            creditoCuotasPrestamosMensualService.remove(ids);
            return Response.status(Response.Status.OK).entity("CreditoCuotasPrestamosMensual eliminado correctamente").type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST @Path("selectByCriteria") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria CreditoCuotasPrestamosMensual");
        try {
            return Response.status(Response.Status.OK).entity(creditoCuotasPrestamosMensualService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en selectByCriteria CreditoCuotasPrestamosMensual: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
