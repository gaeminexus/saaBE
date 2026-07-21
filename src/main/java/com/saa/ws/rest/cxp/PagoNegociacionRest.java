package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.PagoNegociacionDaoService;
import com.saa.ejb.cxp.service.PagoNegociacionService;
import com.saa.model.cxp.PagoNegociacion;
import com.saa.model.cxp.NombreEntidadesCompra;

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

@Path("pgng")
public class PagoNegociacionRest {

    @EJB
    private PagoNegociacionDaoService pagoNegociacionDaoService;

    @EJB
    private PagoNegociacionService pagoNegociacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PagoNegociacionRest() {
    }

    /**
     * Obtiene todos los pagos registrados sobre cuotas de negociación.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PagoNegociacion> lista = pagoNegociacionDaoService.selectAll(NombreEntidadesCompra.PAGO_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pagos de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un pago de negociación por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PagoNegociacion entidad = pagoNegociacionDaoService.selectById(id, NombreEntidadesCompra.PAGO_NEGOCIACION);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Pago de negociación con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener pago de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un pago de negociación existente.
     * Usar para: asociar factura a un anticipo previo, o marcar un pago como liquidado.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PagoNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PAGO_NEGOCIACION");
        try {
            PagoNegociacion resultado = pagoNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar pago de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra un nuevo pago (anticipo o con factura) sobre una cuota de negociación.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PagoNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - PAGO_NEGOCIACION");
        try {
            PagoNegociacion resultado = pagoNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear pago de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca pagos de negociación por criterios (endpoint documentado para el frontend).
     * Uso típico: filtrar por formaPago.id para obtener todos los pagos de una cuota.
     */
    @GET
    @Path("/getByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCriteria(List<DatosBusqueda> datos) {
        try {
            List<PagoNegociacion> lista = pagoNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.PAGO_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al buscar pagos de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca pagos de negociación por criterios (estándar interno POST).
     * Uso típico: filtrar por formaPago.id para obtener todos los pagos de una cuota.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de PAGO_NEGOCIACION");
        try {
            List<PagoNegociacion> lista = pagoNegociacionService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un pago de negociación por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PAGO_NEGOCIACION id: " + id);
        try {
            PagoNegociacion elimina = new PagoNegociacion();
            pagoNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar pago de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula/elimina un pago de negociación por su ID (endpoint documentado para el frontend).
     * Alias de DELETE /{id} para mantener compatibilidad con la documentación.
     */
    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteByPath(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE /delete - PAGO_NEGOCIACION id: " + id);
        try {
            PagoNegociacion elimina = new PagoNegociacion();
            pagoNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar pago de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}