	package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ProductoDaoService;
import com.saa.ejb.credito.service.ProductoService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.Producto;

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

@Path("prdc")
public class ProductoRest {
    
    @EJB
    private ProductoDaoService productoDaoService;
    
    @EJB
    private ProductoService productoService;
    
    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public ProductoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Producto.
     * 
     * @return Response con lista de Producto
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Producto> productos = productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
            return Response.status(Response.Status.OK)
                    .entity(productos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener productos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de Producto por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Producto
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Producto producto = productoDaoService.selectById(id, NombreEntidadesCredito.PRODUCTO);
            if (producto == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Producto con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(producto)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener producto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Producto (PUT).
     * 
     * @param registro Objeto Producto
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Producto registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Producto");
        try {
            Producto resultado = productoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar producto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Producto (POST).
     * 
     * @param registro Objeto Producto
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Producto registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Producto");
        try {
            Producto resultado = productoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear producto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Producto");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(productoService.selectByCriteria(registros))
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
     * Elimina un registro de Producto por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Producto");
        try {
            Producto elimina = new Producto();
            productoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar producto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
