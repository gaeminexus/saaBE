package com.saa.ws.rest.rhh;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.RolPagoDaoService;
import com.saa.ejb.rhh.service.GeneracionRolPagoService;
import com.saa.ejb.rhh.service.RolPagoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.RolPago;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("rlpg")
public class RolPagoRest {

    @EJB
    private RolPagoDaoService RolPagoDaoService;

    @EJB
    private RolPagoService RolPagoService;

    @EJB
    private GeneracionRolPagoService generacionRolPagoService;

    @Context
    private UriInfo context;

    public RolPagoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<RolPago> lista = RolPagoDaoService.selectAll(NombreEntidadesRhh.ROL_PAGO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            RolPago registro = RolPagoDaoService.selectById(id, NombreEntidadesRhh.ROL_PAGO);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(RolPago registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ROL_PAGO");
        try {
            RolPago actualizado = RolPagoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(RolPago registro) {
        System.out.println("LLEGA AL SERVICIO POST - ROL_PAGO");
        try {
            RolPago creado = RolPagoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ROL_PAGO");
        try {
            List<RolPago> lista = RolPagoService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ROL_PAGO");
        try {
            RolPago elimina = new RolPago();
            RolPagoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoints de proceso - fase 5
    // =====================================================================

    /**
     * Genera o regenera los roles de pago de un periodo aprobado.
     */
    @POST
    @Path("/generar/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generar(@PathParam("idPeriodo") Long idPeriodo,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO generar - ROL_PAGO, periodo: " + idPeriodo);
        try {
            int generados = generacionRolPagoService.generarRoles(idPeriodo, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(Integer.valueOf(generados)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar los roles de pago: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recalcula el hash del rol y lo compara con el grabado.
     */
    @GET
    @Path("/verificar/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificar(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO verificar - ROL_PAGO, rol: " + id);
        try {
            boolean integro = generacionRolPagoService.verificarIntegridad(id);
            return Response.status(Response.Status.OK).entity(Boolean.valueOf(integro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al verificar el rol de pago: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca como entregados los roles indicados.
     *
     * <p>El cuerpo es la lista de ids. El servidor pone recibido en 'S' y sella la fecha
     * de envio con la del dia solo si estaba en nulo.</p>
     */
    @POST
    @Path("/registrarRecepcion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarRecepcion(List<Long> idsRolPago,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO registrarRecepcion - ROL_PAGO");
        try {
            int marcados = generacionRolPagoService.registrarRecepcion(idsRolPago, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(Integer.valueOf(marcados)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al registrar la recepcion de los roles: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
