package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.MayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.service.MayorAnaliticoService;
import com.saa.model.contabilidad.MayorAnalitico;
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

@Path("myan")
public class MayorAnaliticoRest {

    @EJB
    private MayorAnaliticoDaoService mayorAnaliticoDaoService;

    @EJB
    private MayorAnaliticoService mayorAnaliticoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public MayorAnaliticoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of MayorAnaliticoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MayorAnalitico> getAll() throws Throwable {
        return mayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.MAYOR_ANALITICO);
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public MayorAnalitico getId(@PathParam("id") Long id) throws Throwable {
        return mayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.MAYOR_ANALITICO);
    }

    /**
     * PUT method for updating or creating an instance of MayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public MayorAnalitico put(MayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return mayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public MayorAnalitico post(MayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return mayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorAnaliticoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MAYOR_ANALITICO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(mayorAnaliticoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of MayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        MayorAnalitico elimina = new MayorAnalitico();
        mayorAnaliticoDaoService.remove(elimina, id);
    }

}
