package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.DetalleOrdenPagoNominaDaoService;
import com.saa.ejb.rhh.service.DetalleOrdenPagoNominaService;
import com.saa.ejb.rhh.service.GeneracionOrdenPagoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.DetalleOrdenPagoNomina;

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

@Path("drpg")
public class DetalleOrdenPagoNominaRest {

    @EJB
    private DetalleOrdenPagoNominaDaoService detalleOrdenPagoNominaDaoService;

    @EJB
    private DetalleOrdenPagoNominaService detalleOrdenPagoNominaService;

    @EJB
    private GeneracionOrdenPagoService generacionOrdenPagoService;

    @Context
    private UriInfo context;

    public DetalleOrdenPagoNominaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - DETALLE_ORDEN_PAGO_NOMINA");
        try {
            List<DetalleOrdenPagoNomina> lista = detalleOrdenPagoNominaDaoService.selectAll(NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - DETALLE_ORDEN_PAGO_NOMINA, id: " + id);
        try {
            DetalleOrdenPagoNomina registro = detalleOrdenPagoNominaDaoService.selectById(id, NombreEntidadesRhh.DETALLE_ORDEN_PAGO_NOMINA);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleOrdenPagoNomina registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_ORDEN_PAGO_NOMINA");
        try {
            DetalleOrdenPagoNomina actualizado = detalleOrdenPagoNominaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleOrdenPagoNomina registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_ORDEN_PAGO_NOMINA");
        try {
            DetalleOrdenPagoNomina creado = detalleOrdenPagoNominaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_ORDEN_PAGO_NOMINA");
        try {
            List<DetalleOrdenPagoNomina> lista = detalleOrdenPagoNominaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_ORDEN_PAGO_NOMINA");
        try {
            DetalleOrdenPagoNomina elimina = new DetalleOrdenPagoNomina();
            detalleOrdenPagoNominaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Detalle de una orden con los transitorios idPago/estadoPago del ultimo pago de cada DRPG.
     * docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md §4.
     */
    @GET
    @Path("/selectByOrden/{idOrden}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByOrden(@PathParam("idOrden") Long idOrden) {
        System.out.println("LLEGA AL SERVICIO selectByOrden - DETALLE_ORDEN_PAGO_NOMINA, orden: " + idOrden);
        try {
            List<DetalleOrdenPagoNomina> lista = generacionOrdenPagoService.detalleConEstadoPago(idOrden);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener el detalle de la orden: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reenvia un DRPG rechazado. docs/logica-negocio/rhh/API-PAGO-NOMINA-POR-EMPLEADO.md §3.4.
     */
    @POST
    @Path("/reenviar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reenviar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO reenviar - DETALLE_ORDEN_PAGO_NOMINA, detalle: " + id);
        try {
            Object valorUsuario = datos != null ? datos.get("idUsuario") : null;
            Long idUsuario = valorUsuario != null ? Long.valueOf(valorUsuario.toString()) : null;
            DetalleOrdenPagoNomina detalle = generacionOrdenPagoService.reenviar(id, idUsuario);
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al reenviar el pago: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
