package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.DetalleAsientoDaoService;
import com.saa.ejb.contabilidad.service.DetalleAsientoService;
import com.saa.model.contabilidad.DetalleAsiento;
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

@Path("dtas")
public class DetalleAsientoRest {

    @EJB
    private DetalleAsientoDaoService detalleAsientoDaoService;

    @EJB
    private DetalleAsientoService detalleAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleAsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleAsiento> getAll() throws Throwable {
        return detalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_ASIENTO);
    }

    
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleAsiento getId(@PathParam("id") Long id) throws Throwable {
        return detalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_ASIENTO);
    }

    /**
     * PUT method for updating or creating an instance of DetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleAsiento put(DetalleAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleAsiento post(DetalleAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleAsiento> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return detalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_ASIENTO);
    }

    /**
     * POST method for updating or creating an instance of DetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleAsiento elimina = new DetalleAsiento();
        detalleAsientoDaoService.remove(elimina, id);
    }

}
