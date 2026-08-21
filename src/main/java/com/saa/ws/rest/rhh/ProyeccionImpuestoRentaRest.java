package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.ProyeccionImpuestoRentaDaoService;
import com.saa.ejb.rhh.service.ProyeccionImpuestoRentaService;
import com.saa.ejb.rhh.service.RetencionRentaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ProyeccionImpuestoRenta;
import com.saa.model.rhh.ResultadoProyeccionIr;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("pyir")
public class ProyeccionImpuestoRentaRest {

    @EJB
    private ProyeccionImpuestoRentaDaoService proyeccionImpuestoRentaDaoService;

    @EJB
    private ProyeccionImpuestoRentaService proyeccionImpuestoRentaService;

    @EJB
    private RetencionRentaService retencionRentaService;

    @Context
    private UriInfo context;

    public ProyeccionImpuestoRentaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - PROYECCIONIMPUESTORENTA");
        try {
            List<ProyeccionImpuestoRenta> lista = proyeccionImpuestoRentaDaoService.selectAll(NombreEntidadesRhh.PROYECCION_IMPUESTO_RENTA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - PROYECCIONIMPUESTORENTA");
        try {
            ProyeccionImpuestoRenta registro = proyeccionImpuestoRentaDaoService.selectById(id, NombreEntidadesRhh.PROYECCION_IMPUESTO_RENTA);
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
    public Response put(ProyeccionImpuestoRenta registro) {
        System.out.println("LLEGA AL SERVICIO PUT - PROYECCIONIMPUESTORENTA");
        try {
            ProyeccionImpuestoRenta actualizado = proyeccionImpuestoRentaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ProyeccionImpuestoRenta registro) {
        System.out.println("LLEGA AL SERVICIO POST - PROYECCIONIMPUESTORENTA");
        try {
            ProyeccionImpuestoRenta creado = proyeccionImpuestoRentaService.saveSingle(registro);
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
        System.out.println("selectByCriteria de PROYECCIONIMPUESTORENTA");
        try {
            List<ProyeccionImpuestoRenta> lista = proyeccionImpuestoRentaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - PROYECCIONIMPUESTORENTA");
        try {
            proyeccionImpuestoRentaService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Endpoints de proceso de la proyeccion de impuesto a la renta
    // =====================================================================

    /**
     * Proyecta el impuesto a la renta de un empleado y deja la proyeccion vigente.
     */
    @POST
    @Path("/proyectar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response proyectar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO PROYECTAR - PROYECCIONIMPUESTORENTA");
        try {
            Long idEmpleado = leeLong(datos, "idEmpleado");
            Integer anio = leeEntero(datos, "anio");
            Integer mesDesde = leeEntero(datos, "mesDesde");
            String usuario = leeTexto(datos, "usuarioRegistro");
            ResultadoProyeccionIr resultado = retencionRentaService.proyectar(
                    idEmpleado, anio, mesDesde, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al proyectar el impuesto a la renta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Proyecta a todos los empleados con contrato activo de la empresa.
     */
    @POST
    @Path("/proyectarTodos/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response proyectarTodos(@PathParam("anio") Integer anio,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("usuarioRegistro") String usuario) {
        System.out.println("LLEGA AL SERVICIO PROYECTAR TODOS - PROYECCIONIMPUESTORENTA, anio: " + anio);
        try {
            int proyectados = retencionRentaService.proyectarTodos(idEmpresa, anio, usuario);
            return Response.status(Response.Status.OK).entity(proyectados).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al proyectar todos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long leeLong(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    private Integer leeEntero(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : Integer.valueOf(valor.toString());
    }

    private String leeTexto(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor == null ? null : valor.toString();
    }
}
