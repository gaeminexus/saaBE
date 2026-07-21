package com.saa.ws.rest.cxp;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.dao.NegociacionProveedorDaoService;
import com.saa.ejb.cxp.service.NegociacionProveedorService;
import com.saa.model.cxp.NegociacionProveedor;
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

@Path("ngcp")
public class NegociacionProveedorRest {

    @EJB
    private NegociacionProveedorDaoService negociacionProveedorDaoService;

    @EJB
    private NegociacionProveedorService negociacionProveedorService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public NegociacionProveedorRest() {
    }

    /**
     * Obtiene todas las negociaciones con proveedores.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<NegociacionProveedor> lista = negociacionProveedorDaoService.selectAll(NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener negociaciones con proveedores: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene una negociación con proveedor por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            NegociacionProveedor entidad = negociacionProveedorDaoService.selectById(id, NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Negociación con proveedor con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener negociación con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza una negociación con proveedor existente.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(NegociacionProveedor registro) {
        System.out.println("LLEGA AL SERVICIO PUT - NEGOCIACION_PROVEEDOR");
        try {
            NegociacionProveedor resultado = negociacionProveedorService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar negociación con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea una nueva negociación con proveedor.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(NegociacionProveedor registro) {
        System.out.println("LLEGA AL SERVICIO POST - NEGOCIACION_PROVEEDOR");
        try {
            NegociacionProveedor resultado = negociacionProveedorService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear negociación con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca negociaciones con proveedor por criterios (endpoint documentado para el frontend).
     * Uso típico: filtrar por empresa.pjrqcdgo, titular.ttlrcdgo, estado, etc.
     */
    @GET
    @Path("/getByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCriteria(List<DatosBusqueda> datos) {
        try {
            List<NegociacionProveedor> lista = negociacionProveedorDaoService.selectByCriteria(datos, NombreEntidadesCompra.NEGOCIACION_PROVEEDOR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al buscar negociaciones con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca negociaciones con proveedor por criterios (estándar interno POST).
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de NEGOCIACION_PROVEEDOR");
        try {
            List<NegociacionProveedor> lista = negociacionProveedorService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina una negociación con proveedor por su ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - NEGOCIACION_PROVEEDOR id: " + id);
        try {
            NegociacionProveedor elimina = new NegociacionProveedor();
            negociacionProveedorDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar negociación con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina una negociación con proveedor por su ID (endpoint documentado para el frontend).
     * Alias de DELETE /{id} para mantener compatibilidad con la documentación.
     */
    @DELETE
    @Path("/delete/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteByPath(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE /delete - NEGOCIACION_PROVEEDOR id: " + id);
        try {
            NegociacionProveedor elimina = new NegociacionProveedor();
            negociacionProveedorDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar negociación con proveedor: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}