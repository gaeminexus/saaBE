package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.HistDetalleMayorizacionDaoService;
import com.saa.ejb.contabilidad.service.HistDetalleMayorizacionService;
import com.saa.model.contabilidad.HistDetalleMayorizacion;
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

@Path("dtmh")
public class HistDetalleMayorizacionRest {

    @EJB
    private HistDetalleMayorizacionDaoService histDetalleMayorizacionDaoService;

    @EJB
    private HistDetalleMayorizacionService histDetalleMayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistDetalleMayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistDetalleMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistDetalleMayorizacion> getAll() throws Throwable {
        return histDetalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
    }
    
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public HistDetalleMayorizacion getId(@PathParam("id") Long id) throws Throwable {
        return histDetalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
    }

    /**
     * PUT method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public HistDetalleMayorizacion put(HistDetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return histDetalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public HistDetalleMayorizacion post(HistDetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return histDetalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de HIST_DETALLE_MAYORIZACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(histDetalleMayorizacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        HistDetalleMayorizacion elimina = new HistDetalleMayorizacion();
        histDetalleMayorizacionDaoService.remove(elimina, id);
    }

}
