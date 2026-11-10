package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.ejb.crd.dao.FirmanteContratoDaoService;
import com.saa.ejb.crd.service.FirmanteContratoService;
import com.saa.model.crd.FirmanteContrato;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST de lectura de los firmantes de un trámite de firma del contrato (CRD.FRFR). Los escribe el servicio de firma. */
@Path("frfr")
public class FirmanteContratoRest {

    @EJB
    private FirmanteContratoDaoService firmanteContratoDaoService;

    @EJB
    private FirmanteContratoService firmanteContratoService;

    public FirmanteContratoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<FirmanteContrato> lista = firmanteContratoDaoService.selectAll(NombreEntidadesCredito.FIRMANTE_CONTRATO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener firmantes: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            FirmanteContrato f = firmanteContratoDaoService.selectById(id, NombreEntidadesCredito.FIRMANTE_CONTRATO);
            return Response.status(Response.Status.OK).entity(f).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener firmante: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** GET /rest/frfr/firma/{idFirma} — firmantes de un trámite, por orden de firma. */
    @GET
    @Path("/firma/{idFirma}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porFirma(@PathParam("idFirma") Long idFirma) {
        try {
            List<FirmanteContrato> lista = firmanteContratoService.selectByFirma(idFirma);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener firmantes: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
