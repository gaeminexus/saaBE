package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.ejb.contabilidad.service.DetallePlantillaService;
import com.saa.model.contabilidad.DetallePlantilla;
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

@Path("dtpl")
public class DetallePlantillaRest {

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private DetallePlantillaService detallePlantillaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetallePlantillaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetallePlantillaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetallePlantilla> getAll() throws Throwable {
        return detallePlantillaDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_PLANTILLA);
    }


    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetallePlantilla getId(@PathParam("id") Long id) throws Throwable {
        return detallePlantillaDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_PLANTILLA);
    }
    
    @GET
    @Produces("application/json")
    @Path("/getByParent/{idParent}")
    public List<DetallePlantilla> getByParent(@PathParam("idParent") Long idParent) throws Throwable {
        return detallePlantillaDaoService.selectByPlantilla(idParent);
    }

    /**
     * PUT method for updating or creating an instance of DetallePlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetallePlantilla put(DetallePlantilla registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detallePlantillaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetallePlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetallePlantilla post(DetallePlantilla registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detallePlantillaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetallePlantillaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_PLANTILLA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detallePlantillaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of DetallePlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetallePlantilla elimina = new DetallePlantilla();
        detallePlantillaDaoService.remove(elimina, id);
    }

}
