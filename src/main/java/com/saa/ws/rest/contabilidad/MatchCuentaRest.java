package com.saa.ws.rest.contabilidad;

import java.util.List;

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
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<MatchCuenta> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return matchCuentaDaoService.selectAll(NombreEntidadesContabilidad.MATCH_CUENTA);
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
