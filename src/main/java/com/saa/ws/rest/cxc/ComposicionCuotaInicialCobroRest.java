package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.ComposicionCuotaInicialCobroDaoService;
import com.saa.ejb.cxc.service.ComposicionCuotaInicialCobroService;
import com.saa.model.cxc.ComposicionCuotaInicialCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

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

@Path("ccic")
public class ComposicionCuotaInicialCobroRest {

    @EJB
    private ComposicionCuotaInicialCobroDaoService composicionCuotaInicialCobroDaoService;

    @EJB
    private ComposicionCuotaInicialCobroService composicionCuotaInicialCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ComposicionCuotaInicialCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Recupera todos los registros de ComposicionCuotaInicialCobro.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ComposicionCuotaInicialCobro> lista = composicionCuotaInicialCobroDaoService.selectAll(NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener composiciones de cuota inicial: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de ComposicionCuotaInicialCobro por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ComposicionCuotaInicialCobro registro = composicionCuotaInicialCobroDaoService.selectById(id, NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("ComposicionCuotaInicialCobro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener composición de cuota inicial: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ComposicionCuotaInicialCobro registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            ComposicionCuotaInicialCobro resultado = composicionCuotaInicialCobroService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar composición de cuota inicial: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ComposicionCuotaInicialCobro registro) {
        System.out.println("LLEGA AL POST");
        try {
            ComposicionCuotaInicialCobro resultado = composicionCuotaInicialCobroService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear composición de cuota inicial: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca registros por criterios.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ComposicionCuotaInicialCobro");
        try {
            return Response.status(Response.Status.OK)
                    .entity(composicionCuotaInicialCobroService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            ComposicionCuotaInicialCobro elimina = new ComposicionCuotaInicialCobro();
            composicionCuotaInicialCobroDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar composición de cuota inicial: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}