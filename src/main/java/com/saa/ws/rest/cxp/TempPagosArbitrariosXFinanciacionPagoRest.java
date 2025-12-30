package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.TempPagosArbitrariosXFinanciacionPagoDaoService;
import com.saa.ejb.cxp.service.TempPagosArbitrariosXFinanciacionPagoService;
import com.saa.model.cxp.NombreEntidadesPago;
import com.saa.model.cxp.TempPagosArbitrariosXFinanciacionPago;

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

@Path("tpfp")
public class TempPagosArbitrariosXFinanciacionPagoRest {

    @EJB
    private TempPagosArbitrariosXFinanciacionPagoDaoService TempPagosArbitrariosXFinanciacionPagoDaoService;

    @EJB
    private TempPagosArbitrariosXFinanciacionPagoService TempPagosArbitrariosXFinanciacionPagoService;

    @Context
    private UriInfo context;

    public TempPagosArbitrariosXFinanciacionPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempPagosArbitrariosXFinanciacionPago> lista = TempPagosArbitrariosXFinanciacionPagoDaoService.selectAll(NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempPagosArbitrariosXFinanciacionPago registro = TempPagosArbitrariosXFinanciacionPagoDaoService.selectById(id, NombreEntidadesPago.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO);
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
    public Response put(TempPagosArbitrariosXFinanciacionPago registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        try {
            TempPagosArbitrariosXFinanciacionPago actualizado = TempPagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempPagosArbitrariosXFinanciacionPago registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        try {
            TempPagosArbitrariosXFinanciacionPago creado = TempPagosArbitrariosXFinanciacionPagoService.saveSingle(registro);
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
        System.out.println("selectByCriteria de TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        try {
            List<TempPagosArbitrariosXFinanciacionPago> lista = TempPagosArbitrariosXFinanciacionPagoService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_PAGO");
        try {
            TempPagosArbitrariosXFinanciacionPago elimina = new TempPagosArbitrariosXFinanciacionPago();
            TempPagosArbitrariosXFinanciacionPagoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
 