package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.ejb.credito.dao.EstadoCivilDaoService;
import com.saa.ejb.credito.service.EstadoCivilService;
import com.saa.model.credito.EstadoCivil;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("escv")
public class EstadoCivilRest {
    
    @EJB
    private EstadoCivilDaoService estadoCivilDaoService;
    
    @EJB
    private EstadoCivilService estadoCivilService;
    
    @Context
    private UriInfo context;
    
    /**
     * Default constructor.
     */
    public EstadoCivilRest() {
        // TODO Auto-generated constructor stub
    }
    
    /**
     * Retrieves representation of an instance of EstadoCivilRest
     * 
     * @return an instance of List<EstadoCivil>
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<EstadoCivil> getAll() throws Throwable {
        return estadoCivilDaoService.selectAll(NombreEntidadesCredito.ESTADO_CIVIL);
    }
    
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public EstadoCivil getId(@PathParam("id") Long id) throws Throwable {
        return estadoCivilDaoService.selectById(id, NombreEntidadesCredito.ESTADO_CIVIL);
    }
    
    /**
     * PUT method for updating or creating an instance of EstadoCivilRest
     * 
     * @param registro representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public EstadoCivil put(EstadoCivil registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE EstadoCivil");
        return estadoCivilService.saveSingle(registro);
    }
    
    /**
     * POST method for updating or creating an instance of EstadoCivilRest
     * 
     * @param registro representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public EstadoCivil post(EstadoCivil registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE EstadoCivil");
        return estadoCivilService.saveSingle(registro);
    }
    
    /**
     * POST method for querying by criteria
     * 
     * @param test Dummy parameter for testing
     * @return List of EstadoCivil
     * @throws Throwable
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<EstadoCivil> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return estadoCivilDaoService.selectAll(NombreEntidadesCredito.ESTADO_CIVIL);
    }
    
    /**
     * DELETE method for removing an instance of EstadoCivilRest
     * 
     * @param id Identifier of the resource
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE EstadoCivil");
        EstadoCivil elimina = new EstadoCivil();
        estadoCivilDaoService.remove(elimina, id);
    }
}