/**
 * Copyright (c) 2010 Compuseg Cía. Ltda. 
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados. 
 */
package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.CuentaBancariaTitularDaoService;
import com.saa.ejb.tsr.service.CuentaBancariaTitularService;
import com.saa.model.tsr.CuentaBancariaTitular;
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

/**
 * @author GaemiSoft
 * <p>REST endpoint para CuentaBancariaTitular.
 * Path: /ctbn</p>
 */
@Path("ctbn")
public class CuentaBancariaTitularRest {

    @EJB
    private CuentaBancariaTitularDaoService cuentaBancariaTitularDaoService;

    @EJB
    private CuentaBancariaTitularService cuentaBancariaTitularService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CuentaBancariaTitularRest() {
    }

    /**
     * Obtiene todos los registros de CuentaBancariaTitular.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CuentaBancariaTitular> lista = cuentaBancariaTitularDaoService
                    .selectAll(NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuentas bancarias de titulares: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene una CuentaBancariaTitular por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CuentaBancariaTitular cuenta = cuentaBancariaTitularDaoService
                    .selectById(id, NombreEntidadesTesoreria.CUENTA_BANCARIA_TITULAR);
            if (cuenta == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("CuentaBancariaTitular con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cuenta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cuenta bancaria del titular: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un registro existente (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaBancariaTitular registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CUENTA_BANCARIA_TITULAR");
        try {
            CuentaBancariaTitular resultado = cuentaBancariaTitularService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar cuenta bancaria del titular: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuentaBancariaTitular registro) {
        System.out.println("LLEGA AL SERVICIO POST - CUENTA_BANCARIA_TITULAR");
        try {
            CuentaBancariaTitular resultado = cuentaBancariaTitularService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear cuenta bancaria del titular: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CUENTA_BANCARIA_TITULAR");
        try {
            List<CuentaBancariaTitular> lista = cuentaBancariaTitularService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria CuentaBancariaTitular: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina registros por lista de IDs.
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(List<Long> ids) {
        System.out.println("LLEGA AL SERVICIO DELETE - CUENTA_BANCARIA_TITULAR");
        try {
            cuentaBancariaTitularService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("Cuentas bancarias eliminadas correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar cuentas bancarias: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
