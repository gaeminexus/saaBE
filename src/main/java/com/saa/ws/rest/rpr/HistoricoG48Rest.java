package com.saa.ws.rest.rpr;

import java.util.List;

import com.saa.ejb.rpr.dao.HistoricoG48DaoService;
import com.saa.model.rpr.HistoricoG48;
import com.saa.model.rpr.NombreEntidadesReporte;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("hm48")
public class HistoricoG48Rest {

    @EJB private HistoricoG48DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG48> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G48);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}