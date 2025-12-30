package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CierreCajaDaoService;
import com.saa.ejb.tesoreria.service.CierreCajaService;
import com.saa.model.tesoreria.CierreCaja;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("crcj")
public class CierreCajaRest {

    @EJB
    private CierreCajaDaoService cierreCajaDaoService;

    @EJB
    private CierreCajaService cierreCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CierreCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CierreCaja.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CierreCaja> lista = cierreCajaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cierres de caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CierreCaja cierreCaja = cierreCajaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA);
            if (cierreCaja == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CierreCaja con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cierreCaja).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cierre de caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CierreCaja registro) {
        System.out.println("LLEGA AL SERVICIO PUT CIERRE CAJA");
        try {
            CierreCaja resultado = cierreCajaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cierre de caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CierreCaja registro) {
        System.out.println("LLEGA AL SERVICIO POST CIERRE CAJA");
        try {
            CierreCaja resultado = cierreCajaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cierre de caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of CierreCajaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CIERRE_CAJA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cierreCajaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de CierreCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CIERRE CAJA");
        try {
            CierreCaja elimina = new CierreCaja();
            cierreCajaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cierre de caja: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
