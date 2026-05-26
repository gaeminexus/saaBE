package com.saa.ws.rest.rpr;

import com.saa.ejb.rpr.dao.HistoricoG50DaoService;
import com.saa.model.rpr.HistoricoG50;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("hm50")
public class HistoricoG50Rest {

    @EJB private HistoricoG50DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG50> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G50);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}