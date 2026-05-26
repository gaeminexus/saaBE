package com.saa.ws.rest.rpr;

import com.saa.ejb.rpr.dao.HistoricoG47DaoService;
import com.saa.model.rpr.HistoricoG47;
import com.saa.model.rpr.NombreEntidadesReporte;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("hm47")
public class HistoricoG47Rest {

    @EJB private HistoricoG47DaoService dao;

    @GET @Path("/getAll") @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoG47> lista = dao.selectAll(NombreEntidadesReporte.HISTORICO_G47);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error: " + e.getMessage()).build();
        }
    }
}