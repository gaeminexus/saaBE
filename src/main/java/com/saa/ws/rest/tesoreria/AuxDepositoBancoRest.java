package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
     * POST method for updating or creating an instance of AuxDepositoBancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de AUX_DEPOSITO_BANCO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(auxDepositoBancoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
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

