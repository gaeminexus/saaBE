package com.saa.ws.rest.rpr;

import com.saa.ejb.rpr.dao.HistoricoG43DaoService;
import com.saa.model.rpr.HistoricoG43;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("hm43")
public class HistoricoG43Rest {

    @EJB private HistoricoG43DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG43> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G43);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}