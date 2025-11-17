package com.saa.ws.rest.credito;

import java.util.List;

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
     * @return Lista de Producto
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Producto> getAll() throws Throwable {
        return productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
    }
    
    /**
     * Obtiene un registro de Producto por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto Producto
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Producto getId(@PathParam("id") Long id) throws Throwable {
        return productoDaoService.selectById(id, NombreEntidadesCredito.PRODUCTO);
    }
    
    /**
     * Crea o actualiza un registro de Producto (PUT).
     * 
     * @param registro Objeto Producto
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public Producto put(Producto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE Producto");
        return productoService.saveSingle(registro);
    }
    
    /**
     * Crea o actualiza un registro de Producto (POST).
     * 
     * @param registro Objeto Producto
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public Producto post(Producto registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE Producto");
        return productoService.saveSingle(registro);
    }
    
    /**
     * Consulta registros de Producto por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de Producto
     * @throws Throwable
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Producto> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DE Producto: " + test);
        return productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
    }
    
    /**
     * Elimina un registro de Producto por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE Producto");
        Producto elimina = new Producto();
        productoDaoService.remove(elimina, id);
    }
}
