package com.saa.ws.rest.contabilidad;


import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.TipoAsientoDaoService;
import com.saa.ejb.contabilidad.service.TipoAsientoService;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;
import com.saa.model.contabilidad.TipoAsiento;

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

@Path("plnt")
public class TipoAsientoRest {

    @EJB
    private TipoAsientoDaoService tipoAsientoDaoService;

    @EJB
    private TipoAsientoService tipoAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoAsientoRest() {
        // Constructor por defecto
    }

    /**
     * Obtiene todos los registros de TipoAsiento
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoAsiento> getAll() throws Throwable {
        return tipoAsientoDaoService.selectAll(NombreEntidadesContabilidad.TIPO_ASIENTO);
    }
    
    /* Comentamos esta parte por que no usamos orden descendente 

    /**
     * Obtiene todos los registros ordenados descendentemente
    
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<TipoAsiento> getDesc() throws Throwable {
        return tipoAsientoDaoService.selectOrderDesc();
    }

    /**
     * Prueba de consulta con Response
     
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(tipoAsientoDaoService.selectOrderDesc()).build();
    }
    */

    /**
     * Obtiene un TipoAsiento por su ID
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TipoAsiento getId(@PathParam("id") Long id) throws Throwable {
        return tipoAsientoDaoService.selectById(id, NombreEntidadesContabilidad.TIPO_ASIENTO);
    }

    /**
     * Actualiza o crea un registro de TipoAsiento (PUT)
     */
    @PUT
    @Consumes("application/json")
    public TipoAsiento put(TipoAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT (TipoAsiento)");
        return tipoAsientoService.saveSingle(registro);
    }

    /**
     * Crea un nuevo registro de TipoAsiento (POST)
     */
    @POST
    @Consumes("application/json")
    public TipoAsiento post(TipoAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST (TipoAsiento)");
        return tipoAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TipoAsientoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TIPO_ASIENTO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tipoAsientoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TipoAsiento
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE (TipoAsiento)");
        TipoAsiento elimina = new TipoAsiento();
        tipoAsientoDaoService.remove(elimina, id);
    }

}

