package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ValorPagoPensionComplementariaDaoService;
import com.saa.ejb.crd.service.ValorPagoPensionComplementariaService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.ValorPagoPensionComplementaria;

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

/**
 * REST API para Valores de Pago de Pensión Complementaria.
 * 
 * @author Sistema SAA
 * @since 2026-04-16
 */
@Path("vppc")
public class ValorPagoPensionComplementariaRest {

    @EJB
    private ValorPagoPensionComplementariaDaoService valorPagoPensionComplementariaDaoService;

    @EJB
    private ValorPagoPensionComplementariaService valorPagoPensionComplementariaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ValorPagoPensionComplementariaRest() {
    }

    /**
     * Obtiene todos los valores de pago de pensión complementaria.
     * 
     * @return Response con lista de ValorPagoPensionComplementaria
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ValorPagoPensionComplementaria> lista = valorPagoPensionComplementariaDaoService.selectAll(NombreEntidadesCredito.VALOR_PAGO_PENSION_COMPLEMENTARIA);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener valores de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un valor de pago de pensión complementaria por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto ValorPagoPensionComplementaria
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ValorPagoPensionComplementaria valorPago = valorPagoPensionComplementariaDaoService.selectById(id, NombreEntidadesCredito.VALOR_PAGO_PENSION_COMPLEMENTARIA);
            if (valorPago == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ValorPagoPensionComplementaria con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(valorPago)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener valor de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Actualiza un valor de pago de pensión complementaria (PUT).
     * 
     * @param registro Objeto ValorPagoPensionComplementaria
     * @return Response con registro actualizado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ValorPagoPensionComplementaria registro) {
        System.out.println("LLEGA AL SERVICIO PUT - VALOR_PAGO_PENSION_COMPLEMENTARIA");
        try {
            ValorPagoPensionComplementaria resultado = valorPagoPensionComplementariaService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar valor de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea un nuevo valor de pago de pensión complementaria (POST).
     * 
     * @param registro Objeto ValorPagoPensionComplementaria
     * @return Response con registro creado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ValorPagoPensionComplementaria registro) {
        System.out.println("LLEGA AL SERVICIO POST - VALOR_PAGO_PENSION_COMPLEMENTARIA");
        try {
            ValorPagoPensionComplementaria resultado = valorPagoPensionComplementariaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear valor de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Busca valores de pago por criterios.
     * 
     * @param registros Lista de criterios de búsqueda
     * @return Response con lista de resultados
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de VALOR_PAGO_PENSION_COMPLEMENTARIA");
        try {
            List<ValorPagoPensionComplementaria> resultado = valorPagoPensionComplementariaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Elimina un valor de pago de pensión complementaria.
     * 
     * @param id Identificador del registro a eliminar
     * @return Response sin contenido si es exitoso
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - VALOR_PAGO_PENSION_COMPLEMENTARIA");
        try {
            ValorPagoPensionComplementaria elimina = new ValorPagoPensionComplementaria();
            valorPagoPensionComplementariaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar valor de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
