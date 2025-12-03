package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.NaturalezaCuentaDaoService;
import com.saa.ejb.contabilidad.service.NaturalezaCuentaService;
import com.saa.model.contabilidad.NaturalezaCuenta;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

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

@Path("ntrl")
public class NaturalezaCuentaRest {

    @EJB
    private NaturalezaCuentaDaoService naturalezaCuentaDaoService;

    @EJB
    private NaturalezaCuentaService naturalezaCuentaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public NaturalezaCuentaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of NaturalezaCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<NaturalezaCuenta> getAll() throws Throwable {
        return naturalezaCuentaDaoService.selectAll(NombreEntidadesContabilidad.NATURALEZA_CUENTA);
    }

	/**
	 * Retrieves representation of an instance of NaturalezaCuentaRest
	 * 
	 * @return an instance of String
	 * @throws Throwable
	 */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public NaturalezaCuenta getId(@PathParam("id") Long id) throws Throwable {
        return naturalezaCuentaDaoService.selectById(id, NombreEntidadesContabilidad.NATURALEZA_CUENTA);
    }
    
    /**
	 * Retrieves representation of an instance of NaturalezaCuentaRest
	 * 
	 * @return an instance of String
	 * @throws Throwable
	 */
    @GET
    @Path("/validaTieneCuentas/{idNaturaleza}") /* hizo mely*/
    @Produces("application/json")
    public Long validaTieneCuentas(@PathParam("idNaturaleza") Long idNaturaleza) throws Throwable {
        return naturalezaCuentaService.validaTieneCuentas(idNaturaleza);
    }

    /**
     * PUT method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public NaturalezaCuenta put(NaturalezaCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return naturalezaCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public NaturalezaCuenta post(NaturalezaCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return naturalezaCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Naturaleza Cuenta");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK).entity(naturalezaCuentaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    	return respuesta;
    }

    /**
     * POST method for updating or creating an instance of NaturalezaCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE 11");
        NaturalezaCuenta elimina = new NaturalezaCuenta();
        naturalezaCuentaDaoService.remove(elimina, id);
    }

}
