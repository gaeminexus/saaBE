package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.PlantillaDaoService;
import com.saa.ejb.contabilidad.service.PlantillaService;
import com.saa.model.contabilidad.Plantilla;
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

@Path("plns")
public class PlantillaRest {

    @EJB
    private PlantillaDaoService plantillaDaoService;

    @EJB
    private PlantillaService plantillaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PlantillaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PlantillaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Plantilla> getAll() throws Throwable {
        return plantillaDaoService.selectAll(NombreEntidadesContabilidad.PLANTILLA);
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of PlantillaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<Plantilla> getDesc() throws Throwable {
//        return plantillaDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of PlantillaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(plantillaDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Plantilla getId(@PathParam("id") Long id) throws Throwable {
        return plantillaDaoService.selectById(id, NombreEntidadesContabilidad.PLANTILLA);
    }

    /**
     * PUT method for updating or creating an instance of PlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Plantilla put(Plantilla registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return plantillaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Plantilla post(Plantilla registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return plantillaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Plantilla> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return plantillaDaoService.selectAll(NombreEntidadesContabilidad.PLANTILLA);
    }

    /**
     * POST method for updating or creating an instance of PlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Plantilla elimina = new Plantilla();
        plantillaDaoService.remove(elimina, id);
    }

}