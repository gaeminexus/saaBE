package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.MayorizacionCCDaoService;
import com.saa.ejb.contabilidad.service.MayorizacionCCService;
import com.saa.model.contabilidad.MayorizacionCC;
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

@Path("myrc")
public class MayorizacionCCRest {

    @EJB
    private MayorizacionCCDaoService mayorizacionCCDaoService;

    @EJB
    private MayorizacionCCService mayorizacionCCService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public MayorizacionCCRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of MayorizacionCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MayorizacionCC> getAll() throws Throwable {
        return mayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.MAYORIZACION_CC);
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public MayorizacionCC getId(@PathParam("id") Long id) throws Throwable {
        return mayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.MAYORIZACION_CC);
    }

    /**
     * PUT method for updating or creating an instance of MayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public MayorizacionCC put(MayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return mayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public MayorizacionCC post(MayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return mayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorizacionCCRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MAYORIZACION_CC");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(mayorizacionCCService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of MayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        MayorizacionCC elimina = new MayorizacionCC();
        mayorizacionCCDaoService.remove(elimina, id);
    }

}
