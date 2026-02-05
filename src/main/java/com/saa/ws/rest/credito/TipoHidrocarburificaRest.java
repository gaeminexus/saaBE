package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.TipoHidrocarburificaDaoService;
import com.saa.ejb.crd.service.TipoHidrocarburificaService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoHidrocarburifica;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoHidrocarburifica> lista = tipoHidrocarburificaDaoService.selectAll(NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos hidrocarburíficos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoHidrocarburifica tipo = tipoHidrocarburificaDaoService.selectById(id, NombreEntidadesCredito.TIPO_HIDROCARBURIFICA);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TipoHidrocarburifica con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo hidrocarburífero: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoHidrocarburifica registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE TipoHidrocarburifica");
        try {
            TipoHidrocarburifica resultado = tipoHidrocarburificaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo hidrocarburífero: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoHidrocarburifica registro) {
        System.out.println("LLEGA AL SERVICIO POST DE TipoHidrocarburifica");
        try {
            TipoHidrocarburifica resultado = tipoHidrocarburificaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo hidrocarburífero: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE TipoHidrocarburifica");
        try {
            TipoHidrocarburifica elimina = new TipoHidrocarburifica();
            tipoHidrocarburificaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar tipo hidrocarburífero: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
