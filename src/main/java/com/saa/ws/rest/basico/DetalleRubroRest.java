package com.saa.ws.rest.basico;

import java.util.List;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.model.scp.DetalleRubro;
import com.saa.model.scp.NombreEntidadesSistema;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("pdtr")
public class DetalleRubroRest {
	
	@EJB
	private DetalleRubroDaoService detalleRubroDaoService;
	
    @Context
    private UriInfo context;

    /**
     * Default constructor. 
     */
    public DetalleRubroRest() {
        // TODO Auto-generated constructor stub clear 
    }
    
    /**
     * Retrieves representation of an instance of AnioMortorRest
     * @return an instance of String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getAll")
    public Response getAll() {
        try {
            List<DetalleRubro> lista = detalleRubroDaoService.selectAll(NombreEntidadesSistema.DETALLE_RUBRO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Retrieves representation of an instance of DetalleRubroRest
     * @return an instance of String
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getRubros/{idRubro}")
    public Response getRubros(@PathParam("idRubro") int idRubro) {
        try {
            List<DetalleRubro> lista = detalleRubroDaoService.selectByCodigoAlternoRubro(idRubro, 1L);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener rubros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
