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
     * @throws Throwable 
     */
    @GET
    @Produces("application/json")
    @Path("/getAll")
    public List<DetalleRubro> getAll() throws Throwable {
        return detalleRubroDaoService.selectAll(NombreEntidadesSistema.DETALLE_RUBRO);
    }

    /**
     * Retrieves representation of an instance of DetalleRubroRest
     * @return an instance of String
     */
    @GET
    @Produces("application/json")
    @Path("/getRubros/{idRubro}")
    public List<DetalleRubro> getRubros(@PathParam("idRubro") int idRubro) throws Throwable {
        return detalleRubroDaoService.selectByCodigoAlternoRubro(idRubro, 1L);
    }

}
