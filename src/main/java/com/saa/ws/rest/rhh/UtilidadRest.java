package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.UtilidadDaoService;
import com.saa.ejb.rhh.service.CalculoUtilidadesService;
import com.saa.ejb.rhh.service.UtilidadService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.Utilidad;

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

@Path("utld")
public class UtilidadRest {

    @EJB
    private UtilidadDaoService utilidadDaoService;

    @EJB
    private UtilidadService utilidadService;

    @EJB
    private CalculoUtilidadesService calculoUtilidadesService;

    @Context
    private UriInfo context;

    public UtilidadRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - UTILIDAD");
        try {
            List<Utilidad> lista = utilidadDaoService.selectAll(NombreEntidadesRhh.UTILIDAD);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - UTILIDAD, id: " + id);
        try {
            Utilidad registro = utilidadDaoService.selectById(id, NombreEntidadesRhh.UTILIDAD);
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
    public Response put(Utilidad registro) {
        System.out.println("LLEGA AL SERVICIO PUT - UTILIDAD");
        try {
            Utilidad actualizado = utilidadService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Utilidad registro) {
        System.out.println("LLEGA AL SERVICIO POST - UTILIDAD");
        try {
            Utilidad creado = utilidadService.saveSingle(registro);
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
        System.out.println("selectByCriteria de UTILIDAD");
        try {
            List<Utilidad> lista = utilidadService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - UTILIDAD");
        try {
            Utilidad elimina = new Utilidad();
            utilidadDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoint de proceso - fase 9
    // =====================================================================

    /**
     * Calcula el reparto de utilidades del ejercicio.
     */
    @POST
    @Path("/calcular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response calcular(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO calcular - UTILIDAD");
        try {
            Long idEmpresa = datos != null && datos.get("idEmpresa") != null
                    ? Long.valueOf(datos.get("idEmpresa").toString()) : null;
            Integer anio = datos != null && datos.get("anio") != null
                    ? Integer.valueOf(datos.get("anio").toString()) : null;
            Double utilidadContable = datos != null && datos.get("utilidadContable") != null
                    ? Double.valueOf(datos.get("utilidadContable").toString()) : null;
            String usuario = datos != null && datos.get("usuarioRegistro") != null
                    ? datos.get("usuarioRegistro").toString() : null;
            Utilidad utilidad = calculoUtilidadesService.calcular(idEmpresa, anio, utilidadContable, usuario);
            return Response.status(Response.Status.OK).entity(utilidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al calcular las utilidades: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
