package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroChequeDaoService;
import com.saa.ejb.tesoreria.service.TempCobroChequeService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroCheque;

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

@Path("tcch")
public class TempCobroChequeRest {

    @EJB
    private TempCobroChequeDaoService tempCobroChequeDaoService;

    @EJB
    private TempCobroChequeService tempCobroChequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroCheque.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempCobroCheque> lista = tempCobroChequeDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros con cheque temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TempCobroCheque por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempCobroCheque tempCobroCheque = tempCobroChequeDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
            if (tempCobroCheque == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TempCobroCheque con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tempCobroCheque).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro con cheque temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempCobroCheque registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_COBRO_CHEQUE");
        try {
            TempCobroCheque resultado = tempCobroChequeService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro con cheque temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempCobroCheque registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_COBRO_CHEQUE");
        try {
            TempCobroCheque resultado = tempCobroChequeService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro con cheque temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempCobroChequeRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_COBRO_CHEQUE");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempCobroChequeService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TempCobroCheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_COBRO_CHEQUE");
        try {
            TempCobroCheque elimina = new TempCobroCheque();
            tempCobroChequeDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro con cheque temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
