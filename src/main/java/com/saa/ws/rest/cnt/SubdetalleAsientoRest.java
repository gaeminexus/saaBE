package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.SubdetalleAsientoDaoService;
import com.saa.ejb.cnt.service.SubdetalleAsientoService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.SubdetalleAsiento;

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

@Path("sdas")
public class SubdetalleAsientoRest {

    @EJB
    private SubdetalleAsientoDaoService subdetalleAsientoDaoService;

    @EJB
    private SubdetalleAsientoService subdetalleAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public SubdetalleAsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of SubdetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SubdetalleAsiento> lista = subdetalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.SUBDETALLE_ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalles de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            SubdetalleAsiento subdetalle = subdetalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.SUBDETALLE_ASIENTO);
            if (subdetalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("SubdetalleAsiento con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(subdetalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalle de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByDetalleAsiento/{idDetalleAsiento}")
    public Response getByDetalleAsiento(@PathParam("idDetalleAsiento") Long idDetalleAsiento) {
        try {
            List<SubdetalleAsiento> lista = subdetalleAsientoService.selectByIdDetalleAsiento(idDetalleAsiento);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalles por detalle asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByCodigoActivo/{codigoActivo}")
    public Response getByCodigoActivo(@PathParam("codigoActivo") String codigoActivo) {
        try {
            List<SubdetalleAsiento> lista = subdetalleAsientoService.selectByCodigoActivo(codigoActivo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalles por código activo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByCategoria/{categoria}")
    public Response getByCategoria(@PathParam("categoria") String categoria) {
        try {
            List<SubdetalleAsiento> lista = subdetalleAsientoService.selectByCategoria(categoria);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalles por categoría: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getByResponsable/{responsable}")
    public Response getByResponsable(@PathParam("responsable") String responsable) {
        try {
            List<SubdetalleAsiento> lista = subdetalleAsientoService.selectByResponsable(responsable);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener subdetalles por responsable: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(SubdetalleAsiento registro) {
        System.out.println("LLEGA AL SERVICIO PUT - SUBDETALLE_ASIENTO");
        try {
            SubdetalleAsiento resultado = subdetalleAsientoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar subdetalle de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SubdetalleAsiento registro) {
        System.out.println("LLEGA AL SERVICIO POST - SUBDETALLE_ASIENTO");
        try {
            SubdetalleAsiento resultado = subdetalleAsientoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear subdetalle de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de SUBDETALLE_ASIENTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(subdetalleAsientoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - SUBDETALLE_ASIENTO");
        try {
            SubdetalleAsiento elimina = new SubdetalleAsiento();
            subdetalleAsientoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar subdetalle de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
