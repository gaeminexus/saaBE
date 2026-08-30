package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.ejb.crd.dao.VigenciaContratoDaoService;
import com.saa.ejb.crd.service.VigenciaContratoService;
import com.saa.ejb.crd.service.dto.SolicitudVigenciaContrato;
import com.saa.ejb.crd.service.dto.VigenciaDTO;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.VigenciaContrato;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST de vigencias de contrato (CRD.VGCN). Contrato de API congelado en
 * docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.1.
 *
 * El POST no es un saveSingle plano: cierra la vigencia abierta del mismo (contrato, tipo)
 * y abre la nueva (VigenciaContratoService.crear) — por eso NO expone un PUT/POST genérico
 * de entidad cruda, sólo este endpoint con el body de {@link SolicitudVigenciaContrato}.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Path("vgcn")
public class VigenciaContratoRest {

    @EJB
    private VigenciaContratoDaoService vigenciaContratoDaoService;

    @EJB
    private VigenciaContratoService vigenciaContratoService;

    public VigenciaContratoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<VigenciaContrato> lista = vigenciaContratoDaoService.selectAll(NombreEntidadesCredito.VIGENCIA_CONTRATO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener vigencias: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            VigenciaContrato vigencia = vigenciaContratoDaoService.selectById(id, NombreEntidadesCredito.VIGENCIA_CONTRATO);
            if (vigencia == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Vigencia con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(vigencia).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener vigencia: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/vgcn/porContrato/{idContrato} — historial completo, más reciente primero.
     */
    @GET
    @Path("/porContrato/{idContrato}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porContrato(@PathParam("idContrato") Long idContrato) {
        System.out.println("LLEGA AL SERVICIO GET porContrato - VGCN - idContrato: " + idContrato);
        try {
            List<VigenciaDTO> lista = vigenciaContratoService.selectByContrato(idContrato);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener vigencias del contrato: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/vgcn — crea una vigencia nueva, cerrando la abierta del mismo
     * (contrato, tipo) con fechaFin = fechaInicio - 1 día.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SolicitudVigenciaContrato solicitud) {
        System.out.println("LLEGA AL SERVICIO POST - VGCN");
        try {
            VigenciaDTO resultado = vigenciaContratoService.crear(solicitud);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al crear vigencia: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * DELETE /rest/vgcn/{id} — anula (VGCNIDST = 0). Sólo la vigencia abierta.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id, @jakarta.ws.rs.QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO DELETE - VGCN id: " + id);
        try {
            vigenciaContratoService.anular(id, usuario);
            return Response.status(Response.Status.OK)
                    .entity("Vigencia anulada correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular vigencia: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
