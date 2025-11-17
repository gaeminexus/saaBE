package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoViviendaDaoService;
import com.saa.ejb.credito.service.TipoViviendaService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoVivienda;

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

@Path("tpvv")
public class TipoViviendaRest {
    
    @EJB
    private TipoViviendaDaoService tipoViviendaDaoService;
    
    @EJB
    private TipoViviendaService tipoViviendaService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoViviendaRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoVivienda
     * 
     * @return Lista de TipoVivienda
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoVivienda> getAll() throws Throwable {
        return tipoViviendaDaoService.selectAll(NombreEntidadesCredito.TIPO_VIVIENDA);
    }
    
    /**
     * Obtiene un registro de TipoVivienda por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto TipoVivienda
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TipoVivienda getId(@PathParam("id") Long id) throws Throwable {
        return tipoViviendaDaoService.selectById(id, NombreEntidadesCredito.TIPO_VIVIENDA);
    }
    
    /**
     * Crea o actualiza un registro de TipoVivienda (PUT).
     * 
     * @param registro Objeto TipoVivienda
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public TipoVivienda put(TipoVivienda registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoVivienda");
        return tipoViviendaService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de TipoVivienda (POST).
     * 
     * @param registro Objeto TipoVivienda
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public TipoVivienda post(TipoVivienda registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE TipoVivienda");
        return tipoViviendaService.saveSingle(registro);
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoVivienda");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoViviendaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return respuesta;
    }
    
    /**
     * Elimina un registro de TipoVivienda por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoVivienda");
        TipoVivienda elimina = new TipoVivienda();
        tipoViviendaDaoService.remove(elimina, id);
    }
}
