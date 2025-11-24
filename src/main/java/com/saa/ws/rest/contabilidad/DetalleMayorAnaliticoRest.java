package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.service.DetalleMayorAnaliticoService;
import com.saa.model.contabilidad.DetalleMayorAnalitico;
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

@Path("dtma")
public class DetalleMayorAnaliticoRest {

    @EJB
    private DetalleMayorAnaliticoDaoService detalleMayorAnaliticoDaoService;

    @EJB
    private DetalleMayorAnaliticoService detalleMayorAnaliticoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleMayorAnaliticoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleMayorAnaliticoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleMayorAnalitico> getAll() throws Throwable {
        return detalleMayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
    }

   

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleMayorAnalitico getId(@PathParam("id") Long id) throws Throwable {
        return detalleMayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
    }

    /**
     * PUT method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleMayorAnalitico put(DetalleMayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleMayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleMayorAnalitico post(DetalleMayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleMayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_MAYOR_ANALITICO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleMayorAnaliticoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleMayorAnalitico elimina = new DetalleMayorAnalitico();
        detalleMayorAnaliticoDaoService.remove(elimina, id);
    }

}
