package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.FormaPagoNegociacionDaoService;
import com.saa.ejb.cxp.service.FormaPagoNegociacionService;
import com.saa.model.cxp.FormaPagoNegociacion;
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

@Path("fpng")
public class FormaPagoNegociacionRest {

    @EJB
    private FormaPagoNegociacionDaoService formaPagoNegociacionDaoService;

    @EJB
    private FormaPagoNegociacionService formaPagoNegociacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public FormaPagoNegociacionRest() {
    }

    /**
     * Obtiene todas las cuotas/formas de pago de negociaciones.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<FormaPagoNegociacion> lista = formaPagoNegociacionDaoService.selectAll(NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuotas de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene una cuota/forma de pago de negociación por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            FormaPagoNegociacion entidad = formaPagoNegociacionDaoService.selectById(id, NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Cuota de negociación con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cuota de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza una cuota/forma de pago de negociación existente.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(FormaPagoNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT - FORMA_PAGO_NEGOCIACION");
        try {
            FormaPagoNegociacion resultado = formaPagoNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cuota de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea una nueva cuota/forma de pago de negociación.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(FormaPagoNegociacion registro) {
        System.out.println("LLEGA AL SERVICIO POST - FORMA_PAGO_NEGOCIACION");
        try {
            FormaPagoNegociacion resultado = formaPagoNegociacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cuota de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca cuotas/formas de pago de negociación por criterios (endpoint documentado para el frontend).
     * Uso típico: filtrar por negociacion.id para obtener todas las cuotas de una negociación.
     */
    @GET
    @Path("/getByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCriteria(List<DatosBusqueda> datos) {
        try {
            List<FormaPagoNegociacion> lista = formaPagoNegociacionDaoService.selectByCriteria(datos, NombreEntidadesCompra.FORMA_PAGO_NEGOCIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al buscar cuotas de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca cuotas/formas de pago de negociación por criterios (estándar interno POST).
     * Uso típico: filtrar por negociacion.id para obtener todas las cuotas de una negociación.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de FORMA_PAGO_NEGOCIACION");
        try {
            List<FormaPagoNegociacion> lista = formaPagoNegociacionService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina una cuota/forma de pago de negociación por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - FORMA_PAGO_NEGOCIACION id: " + id);
        try {
            FormaPagoNegociacion elimina = new FormaPagoNegociacion();
            formaPagoNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cuota de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina una cuota/forma de pago de negociación por su ID (endpoint documentado para el frontend).
     * Alias de DELETE /{id} para mantener compatibilidad con la documentación.
     */
    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteByPath(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE /delete - FORMA_PAGO_NEGOCIACION id: " + id);
        try {
            FormaPagoNegociacion elimina = new FormaPagoNegociacion();
            formaPagoNegociacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cuota de negociación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}