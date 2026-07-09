package com.saa.ws.rest.rpr;

import java.util.List;

import com.saa.ejb.rpr.dao.HistoricoG45DaoService;
import com.saa.model.rpr.HistoricoG45;
import com.saa.model.rpr.NombreEntidadesReporte;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("hm45")
public class HistoricoG45Rest {

    @EJB private HistoricoG45DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG45> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G45);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}