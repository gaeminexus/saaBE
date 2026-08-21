package com.saa.ws.rest.rhh;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.CuentaBancariaEmpleadoDaoService;
import com.saa.ejb.rhh.service.CuentaBancariaEmpleadoService;
import com.saa.model.rhh.CuentaBancariaEmpleado;
import com.saa.model.rhh.NombreEntidadesRhh;

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

@Path("cbem")
public class CuentaBancariaEmpleadoRest {

    @EJB
    private CuentaBancariaEmpleadoDaoService cuentaBancariaEmpleadoDaoService;

    @EJB
    private CuentaBancariaEmpleadoService cuentaBancariaEmpleadoService;

    @Context
    private UriInfo context;

    public CuentaBancariaEmpleadoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - CUENTABANCARIAEMPLEADO");
        try {
            List<CuentaBancariaEmpleado> lista = cuentaBancariaEmpleadoDaoService.selectAll(NombreEntidadesRhh.CUENTA_BANCARIA_EMPLEADO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - CUENTABANCARIAEMPLEADO");
        try {
            CuentaBancariaEmpleado registro = cuentaBancariaEmpleadoDaoService.selectById(id, NombreEntidadesRhh.CUENTA_BANCARIA_EMPLEADO);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaBancariaEmpleado registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CUENTABANCARIAEMPLEADO");
        try {
            CuentaBancariaEmpleado actualizado = cuentaBancariaEmpleadoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuentaBancariaEmpleado registro) {
        System.out.println("LLEGA AL SERVICIO POST - CUENTABANCARIAEMPLEADO");
        try {
            CuentaBancariaEmpleado creado = cuentaBancariaEmpleadoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CUENTABANCARIAEMPLEADO");
        try {
            List<CuentaBancariaEmpleado> lista = cuentaBancariaEmpleadoService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CUENTABANCARIAEMPLEADO");
        try {
            cuentaBancariaEmpleadoService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
