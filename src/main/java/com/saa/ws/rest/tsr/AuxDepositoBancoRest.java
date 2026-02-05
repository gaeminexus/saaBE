package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.AuxDepositoBancoDaoService;
import com.saa.ejb.tsr.service.AuxDepositoBancoService;
import com.saa.model.tsr.AuxDepositoBanco;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AuxDepositoBanco> lista = auxDepositoBancoDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener depósitos bancarios auxiliares: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un registro de AuxDepositoBanco por su ID.
     * 
     * @param id identificador
     * @return AuxDepositoBanco
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            AuxDepositoBanco auxDepositoBanco = auxDepositoBancoDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_BANCO);
            if (auxDepositoBanco == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("AuxDepositoBanco con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(auxDepositoBanco).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener depósito bancario auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza o crea un registro de AuxDepositoBanco.
     * 
     * @param registro entidad a guardar
     * @return entidad guardada
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AuxDepositoBanco registro) {
        System.out.println("LLEGA AL SERVICIO PUT - AUX_DEPOSITO_BANCO");
        try {
            AuxDepositoBanco resultado = auxDepositoBancoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar depósito bancario auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro de AuxDepositoBanco.
     * 
     * @param registro entidad a crear
     * @return entidad creada
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AuxDepositoBanco registro) {
        System.out.println("LLEGA AL SERVICIO POST - AUX_DEPOSITO_BANCO");
        try {
            AuxDepositoBanco resultado = auxDepositoBancoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear depósito bancario auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of AuxDepositoBancoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de AUX_DEPOSITO_BANCO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(auxDepositoBancoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de AuxDepositoBanco.
     * 
     * @param id identificador
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        try {
            AuxDepositoBanco elimina = new AuxDepositoBanco();
            auxDepositoBancoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar depósito bancario auxiliar: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}

