package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.AsientoDaoService;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.model.contabilidad.Asiento;
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

@Path("asnt")
public class AsientoRest {

    @EJB
    private AsientoDaoService asientoDaoService;

    @EJB
    private AsientoService asientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Asiento> lista = asientoDaoService.selectAll(NombreEntidadesContabilidad.ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asientos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Asiento asiento = asientoDaoService.selectById(id, NombreEntidadesContabilidad.ASIENTO);
            if (asiento == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Asiento con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(asiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Asiento registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ASIENTO");
        try {
            Asiento resultado = asientoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Asiento registro) {
        System.out.println("LLEGA AL SERVICIO POST - ASIENTO");
        try {
            Asiento resultado = asientoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ASIENTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(asientoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ASIENTO");
        try {
            Asiento elimina = new Asiento();
            asientoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
