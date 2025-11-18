package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.TipoRequisitoPrestamoDaoService;
import com.saa.ejb.credito.service.TipoRequisitoPrestamoService;
import com.saa.model.credito.TipoRequisitoPrestamo;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("tprq")
public class TipoRequisitoPrestamoRest {

    @EJB
    private TipoRequisitoPrestamoDaoService tipoRequisitoPrestamoDaoService;

    @EJB
    private TipoRequisitoPrestamoService tipoRequisitoPrestamoService;

    @Context
    private UriInfo context;

    public TipoRequisitoPrestamoRest() {
        // Constructor vacío
    }

    /**
     * GET - Obtener todos los registros
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TipoRequisitoPrestamo> getAll() throws Throwable {
        return tipoRequisitoPrestamoDaoService.selectAll(NombreEntidadesCredito.TIPO_REQUISITO_PRESTAMO);
    }

    /**
     * GET - Obtener por ID
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public TipoRequisitoPrestamo getId(@PathParam("id") Long id) throws Throwable {
        return tipoRequisitoPrestamoDaoService.selectById(id, NombreEntidadesCredito.TIPO_REQUISITO_PRESTAMO);
    }

    /**
     * PUT - Actualizar o crear registro
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoRequisitoPrestamo put(TipoRequisitoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tipoRequisitoPrestamoService.saveSingle(registro);
    }

    /**
     * POST - Crear registro
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public TipoRequisitoPrestamo post(TipoRequisitoPrestamo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tipoRequisitoPrestamoService.saveSingle(registro);
    }

    /**
     * POST - Select by criteria
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Tipo Requisito Prestamo");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoRequisitoPrestamoService.selectByCriteria(registros))
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
     * DELETE - Eliminar registro por ID
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TipoRequisitoPrestamo elimina = new TipoRequisitoPrestamo();
        tipoRequisitoPrestamoDaoService.remove(elimina, id);
    }
}
