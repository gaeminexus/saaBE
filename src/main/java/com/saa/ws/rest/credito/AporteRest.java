package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.AporteDaoService;
import com.saa.ejb.credito.service.AporteService;
import com.saa.model.credito.Aporte;
import com.saa.model.credito.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("aprt")
public class AporteRest {

    @EJB
    private AporteDaoService aporteDaoService;

    @EJB
    private AporteService aporteService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AporteRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AporteRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Aporte> getAll() throws Throwable {
        return aporteDaoService.selectAll(NombreEntidadesCredito.APORTE);
    }

    /**
     * Retrieves representation of an instance of AporteRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Aporte getId(@PathParam("id") Long id) throws Throwable {
        return aporteDaoService.selectById(id, NombreEntidadesCredito.APORTE);
    }

    /**
     * PUT method for updating or creating an instance of AporteRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Aporte put(Aporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT - APORTE");
        return aporteService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of AporteRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Aporte post(Aporte registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST - APORTE");
        return aporteService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of AporteRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de APORTE");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK)
    				.entity(aporteService.selectByCriteria(registros))
    				.type(MediaType.APPLICATION_JSON)
    				.build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST)
					.entity(e.getMessage())
					.type(MediaType.APPLICATION_JSON)
					.build();
		}
    	return respuesta;
    }

    /**
     * DELETE method for deleting an instance of AporteRest
     * 
     * @param id identifier for the resource
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE - APORTE");
        Aporte elimina = new Aporte();
        aporteDaoService.remove(elimina, id);
    }

}
