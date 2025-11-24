package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DesgloseDetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.DesgloseDetalleDepositoService;
import com.saa.model.tesoreria.DesgloseDetalleDeposito;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("dsdt")
public class DesgloseDetalleDepositoRest {

    @EJB
    private DesgloseDetalleDepositoDaoService desgloseDetalleDepositoDaoService;

    @EJB
    private DesgloseDetalleDepositoService desgloseDetalleDepositoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DesgloseDetalleDepositoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DesgloseDetalleDeposito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DesgloseDetalleDeposito> getAll() throws Throwable {
        return desgloseDetalleDepositoDaoService.selectAll(NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO);
    }

    /**
     * Recupera un registro de DesgloseDetalleDeposito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DesgloseDetalleDeposito getId(@PathParam("id") Long id) throws Throwable {
        return desgloseDetalleDepositoDaoService.selectById(id, NombreEntidadesTesoreria.DESGLOSE_DETALLE_DEPOSITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DesgloseDetalleDeposito put(DesgloseDetalleDeposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DESGLOSE DETALLE DEPOSITO");
        return desgloseDetalleDepositoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DesgloseDetalleDeposito post(DesgloseDetalleDeposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DESGLOSE DETALLE DEPOSITO");
        return desgloseDetalleDepositoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DesgloseDetalleDepositoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DESGLOSE_DETALLE_DEPOSITO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(desgloseDetalleDepositoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DesgloseDetalleDeposito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DESGLOSE DETALLE DEPOSITO");
        DesgloseDetalleDeposito elimina = new DesgloseDetalleDeposito();
        desgloseDetalleDepositoDaoService.remove(elimina, id);
    }
}
