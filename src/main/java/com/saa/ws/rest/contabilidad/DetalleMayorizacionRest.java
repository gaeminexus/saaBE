package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.DetalleMayorizacionDaoService;
import com.saa.ejb.contabilidad.service.DetalleMayorizacionService;
import com.saa.model.contabilidad.DetalleMayorizacion;
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

@Path("dtmy")
public class DetalleMayorizacionRest {

    @EJB
    private DetalleMayorizacionDaoService detalleMayorizacionDaoService;

    @EJB
    private DetalleMayorizacionService detalleMayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleMayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleMayorizacion> getAll() throws Throwable {
        return detalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION);
    }

    
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleMayorizacion getId(@PathParam("id") Long id) throws Throwable {
        return detalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYORIZACION);
    }

    /**
     * PUT method for updating or creating an instance of DetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleMayorizacion put(DetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleMayorizacion post(DetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_MAYORIZACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleMayorizacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleMayorizacion elimina = new DetalleMayorizacion();
        detalleMayorizacionDaoService.remove(elimina, id);
    }

}
