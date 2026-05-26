package com.saa.ws.rest.rpr;

import com.saa.ejb.rpr.dao.HistoricoG42DaoService;
import com.saa.model.rpr.HistoricoG42;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("hm42")
public class HistoricoG42Rest {

    @EJB private HistoricoG42DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG42> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G42);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}