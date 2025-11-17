package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.MovimientoBancoDaoService;
import com.saa.ejb.tesoreria.service.MovimientoBancoService;
import com.saa.model.tesoreria.MovimientoBanco;
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
import jakarta.ws.rs.core.UriInfo;

@Path("mvcb")
public class MovimientoBancoRest {

    @EJB
    private MovimientoBancoDaoService movimientoBancoDaoService;

    @EJB
    private MovimientoBancoService movimientoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public MovimientoBancoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de MovimientoBanco.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<MovimientoBanco> getAll() throws Throwable {
        return movimientoBancoDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
    }

    /**
     * Recupera un registro de MovimientoBanco por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public MovimientoBanco getId(@PathParam("id") Long id) throws Throwable {
        return movimientoBancoDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public MovimientoBanco put(MovimientoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT MOVIMIENTO BANCO");
        return movimientoBancoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public MovimientoBanco post(MovimientoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST MOVIMIENTO BANCO");
        return movimientoBancoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de MovimientoBanco por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<MovimientoBanco> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA MOVIMIENTO BANCO: " + test);
        return movimientoBancoDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_BANCO);
    }

    /**
     * Elimina un registro de MovimientoBanco por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE MOVIMIENTO BANCO");
        MovimientoBanco elimina = new MovimientoBanco();
        movimientoBancoDaoService.remove(elimina, id);
    }
}
