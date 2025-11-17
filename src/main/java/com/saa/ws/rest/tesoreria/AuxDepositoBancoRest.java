package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.AuxDepositoBancoDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoBancoService;
import com.saa.model.tesoreria.AuxDepositoBanco;
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

@Path("adtd")
public class AuxDepositoBancoRest {

    @EJB
    private AuxDepositoBancoDaoService auxDepositoBancoDaoService;

    @EJB
    private AuxDepositoBancoService auxDepositoBancoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public AuxDepositoBancoRest() {
        // Constructor vacío
    }

    /**
     * Obtiene todos los registros de AuxDepositoBanco.
     * 
     * @return Lista de AuxDepositoBanco
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AuxDepositoBanco> getAll() throws Throwable {
        return auxDepositoBancoDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
    }

    /**
     * Obtiene un registro de AuxDepositoBanco por su ID.
     * 
     * @param id identificador
     * @return AuxDepositoBanco
     * @throws Throwable
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public AuxDepositoBanco getId(@PathParam("id") Long id) throws Throwable {
        return auxDepositoBancoDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
    }

    /**
     * Actualiza o crea un registro de AuxDepositoBanco.
     * 
     * @param registro entidad a guardar
     * @return entidad guardada
     * @throws Throwable
     */
    @PUT
    @Consumes("application/json")
    public AuxDepositoBanco put(AuxDepositoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return auxDepositoBancoService.saveSingle(registro);
    }

    /**
     * Crea un nuevo registro de AuxDepositoBanco.
     * 
     * @param registro entidad a crear
     * @return entidad creada
     * @throws Throwable
     */
    @POST
    @Consumes("application/json")
    public AuxDepositoBanco post(AuxDepositoBanco registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST");
        return auxDepositoBancoService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterio (ejemplo).
     * 
     * @param test criterio de búsqueda
     * @return lista de AuxDepositoBanco
     * @throws Throwable
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<AuxDepositoBanco> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return auxDepositoBancoDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
    }

    /**
     * Elimina un registro de AuxDepositoBanco.
     * 
     * @param id identificador
     * @throws Throwable
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        AuxDepositoBanco elimina = new AuxDepositoBanco();
        auxDepositoBancoDaoService.remove(elimina, id);
    }

}

