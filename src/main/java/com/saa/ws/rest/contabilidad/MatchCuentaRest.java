package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.MatchCuentaDaoService;
import com.saa.ejb.contabilidad.service.MatchCuentaService;
import com.saa.model.contabilidad.MatchCuenta;
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

@Path("mtch")
public class MatchCuentaRest {

    @EJB
    private MatchCuentaDaoService matchCuentaDaoService;

    @EJB
    private MatchCuentaService matchCuentaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public MatchCuentaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of MatchCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MatchCuenta> getAll() throws Throwable {
        return matchCuentaDaoService.selectAll(NombreEntidadesContabilidad.MATCH_CUENTA);
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public MatchCuenta getId(@PathParam("id") Long id) throws Throwable {
        return matchCuentaDaoService.selectById(id, NombreEntidadesContabilidad.MATCH_CUENTA);
    }

    /**
     * PUT method for updating or creating an instance of MatchCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public MatchCuenta put(MatchCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return matchCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MatchCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public MatchCuenta post(MatchCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return matchCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MatchCuentaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MATCH_CUENTA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(matchCuentaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * POST method for updating or creating an instance of MatchCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        MatchCuenta elimina = new MatchCuenta();
        matchCuentaDaoService.remove(elimina, id);
    }

}
