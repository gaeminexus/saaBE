package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoHidrocarburificaDaoService;
import com.saa.ejb.credito.service.TipoHidrocarburificaService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.TipoHidrocarburifica;

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

@Path("tphd")
public class TipoHidrocarburificaRest {
    
    @EJB
    private TipoHidrocarburificaDaoService tipoHidrocarburificaDaoService;
    
    @EJB
    private TipoHidrocarburificaService tipoHidrocarburificaService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public TipoHidrocarburificaRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de TipoHidrocarburifica.
     * 
     * @return Lista de TipoHidrocarburifica
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TipoHidrocarburifica> getAll() throws Throwable {
        return tipoHidrocarburificaDaoService.selectAll(NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
    }
    
    /**
     * Obtiene un registro de TipoHidrocarburifica por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto TipoHidrocarburifica
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TipoHidrocarburifica getId(@PathParam("id") Long id) throws Throwable {
        return tipoHidrocarburificaDaoService.selectById(id, NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
    }
    
    /**
     * Crea o actualiza un registro de TipoHidrocarburifica (PUT).
     * 
     * @param registro Objeto TipoHidrocarburifica
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public TipoHidrocarburifica put(TipoHidrocarburifica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoHidrocarburifica");
        return tipoHidrocarburificaService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de TipoHidrocarburifica (POST).
     * 
     * @param registro Objeto TipoHidrocarburifica
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public TipoHidrocarburifica post(TipoHidrocarburifica registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE TipoHidrocarburifica");
        return tipoHidrocarburificaService.saveSingle(registro);
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoHidrocarburifica");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoHidrocarburificaService.selectByCriteria(registros))
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
     * Elimina un registro de TipoHidrocarburifica por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoHidrocarburifica");
        TipoHidrocarburifica elimina = new TipoHidrocarburifica();
        tipoHidrocarburificaDaoService.remove(elimina, id);
    }
}
