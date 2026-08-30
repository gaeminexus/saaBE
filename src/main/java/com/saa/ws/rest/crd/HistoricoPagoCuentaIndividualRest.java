package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.HistoricoPagoCuentaIndividualDaoService;
import com.saa.ejb.crd.service.HistoricoPagoCuentaIndividualService;
import com.saa.model.crd.HistoricoPagoCuentaIndividual;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Histórico de pagos de cuenta individual (CRD.HPCS). Solo lectura: no hay POST/PUT/DELETE
 * de registros porque la tabla la alimentan los procesos de liquidación.
 */
@Path("hpcs")
public class HistoricoPagoCuentaIndividualRest {

    @EJB
    private HistoricoPagoCuentaIndividualDaoService historicoPagoCuentaIndividualDaoService;

    @EJB
    private HistoricoPagoCuentaIndividualService historicoPagoCuentaIndividualService;

    @Context
    private UriInfo context;

    public HistoricoPagoCuentaIndividualRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoPagoCuentaIndividual> lista = historicoPagoCuentaIndividualDaoService
                    .selectAll(NombreEntidadesCredito.HISTORICO_PAGO_CUENTA_INDIVIDUAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener HistoricoPagoCuentaIndividual: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            HistoricoPagoCuentaIndividual registro = historicoPagoCuentaIndividualDaoService
                    .selectById(id, NombreEntidadesCredito.HISTORICO_PAGO_CUENTA_INDIVIDUAL);
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener HistoricoPagoCuentaIndividual: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Pagos de un partícipe por cédula, del más reciente al más antiguo. Lista vacía si no
     * tiene (no es error).
     */
    @GET
    @Path("/getByCedula/{cedula}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCedula(@PathParam("cedula") String cedula) {
        try {
            List<HistoricoPagoCuentaIndividual> lista = historicoPagoCuentaIndividualService.selectByCedula(cedula);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener HistoricoPagoCuentaIndividual por cedula: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<HistoricoPagoCuentaIndividual> lista = historicoPagoCuentaIndividualService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la busqueda de HistoricoPagoCuentaIndividual: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
