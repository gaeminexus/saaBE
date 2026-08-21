package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.SalidaOficialDaoService;
import com.saa.ejb.rhh.service.GeneracionSalidasOficialesService;
import com.saa.ejb.rhh.service.SalidaOficialService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SalidaOficial;

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

@Path("slof")
public class SalidaOficialRest {

    @EJB
    private SalidaOficialDaoService salidaOficialDaoService;

    @EJB
    private SalidaOficialService salidaOficialService;

    @EJB
    private GeneracionSalidasOficialesService generacionSalidasOficialesService;

    @Context
    private UriInfo context;

    public SalidaOficialRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - SALIDA_OFICIAL");
        try {
            List<SalidaOficial> lista = salidaOficialDaoService.selectAll(NombreEntidadesRhh.SALIDA_OFICIAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - SALIDA_OFICIAL, id: " + id);
        try {
            SalidaOficial registro = salidaOficialDaoService.selectById(id, NombreEntidadesRhh.SALIDA_OFICIAL);
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
    public Response put(SalidaOficial registro) {
        System.out.println("LLEGA AL SERVICIO PUT - SALIDA_OFICIAL");
        try {
            SalidaOficial actualizado = salidaOficialService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SalidaOficial registro) {
        System.out.println("LLEGA AL SERVICIO POST - SALIDA_OFICIAL");
        try {
            SalidaOficial creado = salidaOficialService.saveSingle(registro);
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
        System.out.println("selectByCriteria de SALIDA_OFICIAL");
        try {
            List<SalidaOficial> lista = salidaOficialService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - SALIDA_OFICIAL");
        try {
            SalidaOficial elimina = new SalidaOficial();
            salidaOficialDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoints de proceso - fase 9
    // =====================================================================

    /**
     * Genera el XML del RDEP del ejercicio para el DIMM.
     */
    @POST
    @Path("/generarRdep/{anio}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response generarRdep(@PathParam("anio") Integer anio,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO generarRdep - SALIDA_OFICIAL, anio: " + anio);
        try {
            byte[] contenido = generacionSalidasOficialesService.generarRdep(idEmpresa, anio, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(contenido)
                    .header("Content-Disposition", "attachment; filename=\"rdep_" + anio + ".xml\"")
                    .type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar el RDEP: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra que una salida se genero. Lo usan los reportes, que se piden por
     * /rest/rprt/generar pero deben dejar constancia igual.
     */
    @POST
    @Path("/registrarGeneracion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarGeneracion(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO registrarGeneracion - SALIDA_OFICIAL");
        try {
            SalidaOficial salida = generacionSalidasOficialesService.registrarGeneracion(
                    leeLong(datos, "idEmpresa"),
                    leeLong(datos, "tipoSalida").intValue(),
                    leeEntero(datos, "anio"),
                    leeEntero(datos, "mes"),
                    leeLong(datos, "idEmpleado"),
                    leeTexto(datos, "nombreArchivo"),
                    null,
                    leeTexto(datos, "usuarioRegistro"));
            return Response.status(Response.Status.OK).entity(salida).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al registrar la generacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra que una salida se presento al organismo, con su numero de comprobante.
     */
    @POST
    @Path("/registrarPresentacion/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarPresentacion(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO registrarPresentacion - SALIDA_OFICIAL, salida: " + id);
        try {
            LocalDate fecha = datos != null && datos.get("fechaPresentacion") != null
                    ? LocalDate.parse(datos.get("fechaPresentacion").toString()) : null;
            SalidaOficial salida = generacionSalidasOficialesService.registrarPresentacion(id, fecha,
                    leeTexto(datos, "numeroComprobante"), leeTexto(datos, "usuarioRegistro"));
            return Response.status(Response.Status.OK).entity(salida).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al registrar la presentacion: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Lectura del cuerpo
    // =====================================================================

    private Long leeLong(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? Long.valueOf(valor.toString()) : null;
    }

    private Integer leeEntero(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? Integer.valueOf(valor.toString()) : null;
    }

    private String leeTexto(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? valor.toString() : null;
    }
}
