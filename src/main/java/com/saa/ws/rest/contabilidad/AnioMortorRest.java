package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.AnioMotorDaoService;
import com.saa.ejb.contabilidad.service.AnioMotorService;
import com.saa.model.contabilidad.AnioMotor;
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

@Path("anio")
public class AnioMortorRest {

    @EJB
    private AnioMotorDaoService anioMotorDaoService;

    @EJB
    private AnioMotorService anioMotorService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AnioMortorRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AnioMotor> getAll() throws Throwable {
        return anioMotorDaoService.selectAll(NombreEntidadesContabilidad.ANIO_MOTOR);
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<AnioMotor> getDesc() throws Throwable {
        return anioMotorDaoService.selectOrderDesc();
    }

    /**
     * Retrieves representation of an instance of AnioMortorRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(anioMotorDaoService.selectOrderDesc()).build();
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public AnioMotor getId(@PathParam("id") Long id) throws Throwable {
        return anioMotorDaoService.selectById(id, NombreEntidadesContabilidad.ANIO_MOTOR);
    }

    /**
     * PUT method for updating or creating an instance of AnioMortorRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public AnioMotor put(AnioMotor registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return anioMotorService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of AnioMortorRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public AnioMotor post(AnioMotor registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return anioMotorService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of AnioMotorRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de ANIO_MOTOR");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(anioMotorService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of AnioMortorRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        AnioMotor elimina = new AnioMotor();
        anioMotorDaoService.remove(elimina, id);
    }

}
