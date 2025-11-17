package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.PagoPrestamoDaoService;
import com.saa.ejb.credito.service.PagoPrestamoService;
import com.saa.model.credito.NombreEntidadesCredito;
import com.saa.model.credito.PagoPrestamo;

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

@Path("pgpr")
public class PagoPrestamoRest {

    @EJB
    private PagoPrestamoDaoService pagoPrestamoDaoService;

    @EJB
    private PagoPrestamoService pagoPrestamoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PagoPrestamoRest() {
        // Constructor vacío
    }

    /**
     * Obtiene todos los registros de PagoPrestamo.
     * 
     * @return Lista de PagoPrestamo
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PagoPrestamo> getAll() throws Throwable {
        return pagoPrestamoDaoService.selectAll(NombreEntidadesCredito.PAGO_PRESTAMO);
    }

    /**
     * Obtiene un registro de PagoPrestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Objeto PagoPrestamo
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public PagoPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return pagoPrestamoDaoService.selectById(id, NombreEntidadesCredito.PAGO_PRESTAMO);
    }

    /**
     * Crea o actualiza un registro de PagoPrestamo (PUT).
     * 
     * @param registro Objeto PagoPrestamo
     * @return Registro actualizado o creado
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public PagoPrestamo put(PagoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DE PagoPrestamo");
        return pagoPrestamoService.saveSingle(registro);
    }

    /**
     * Crea o actualiza un registro de PagoPrestamo (POST).
     * 
     * @param registro Objeto PagoPrestamo
     * @return Registro creado o actualizado
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public PagoPrestamo post(PagoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DE PagoPrestamo");
        return pagoPrestamoService.saveSingle(registro);
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de PagoPrestamo");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(pagoPrestamoService.selectByCriteria(registros))
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
     * Elimina un registro de PagoPrestamo por ID.
     * 
     * @param id Identificador del registro
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DE PagoPrestamo");
        PagoPrestamo elimina = new PagoPrestamo();
        pagoPrestamoDaoService.remove(elimina, id);
    }
}
