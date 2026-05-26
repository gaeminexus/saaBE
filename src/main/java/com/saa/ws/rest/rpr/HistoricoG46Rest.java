package com.saa.ws.rest.rpr;

import com.saa.ejb.rpr.dao.HistoricoG46DaoService;
import com.saa.model.rpr.HistoricoG46;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("hm46")
public class HistoricoG46Rest {

    @EJB private HistoricoG46DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG46> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G46);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}