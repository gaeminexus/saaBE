package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.OrdenPagoNominaDaoService;
import com.saa.ejb.rhh.service.GeneracionOrdenPagoService;
import com.saa.ejb.rhh.service.OrdenPagoNominaService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.OrdenPagoNomina;

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

@Path("rdpg")
public class OrdenPagoNominaRest {

    @EJB
    private OrdenPagoNominaDaoService ordenPagoNominaDaoService;

    @EJB
    private OrdenPagoNominaService ordenPagoNominaService;

    @EJB
    private GeneracionOrdenPagoService generacionOrdenPagoService;

    @Context
    private UriInfo context;

    public OrdenPagoNominaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - ORDEN_PAGO_NOMINA");
        try {
            List<OrdenPagoNomina> lista = ordenPagoNominaDaoService.selectAll(NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - ORDEN_PAGO_NOMINA, id: " + id);
        try {
            OrdenPagoNomina registro = ordenPagoNominaDaoService.selectById(id, NombreEntidadesRhh.ORDEN_PAGO_NOMINA);
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
    public Response put(OrdenPagoNomina registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ORDEN_PAGO_NOMINA");
        try {
            OrdenPagoNomina actualizado = ordenPagoNominaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(OrdenPagoNomina registro) {
        System.out.println("LLEGA AL SERVICIO POST - ORDEN_PAGO_NOMINA");
        try {
            OrdenPagoNomina creado = ordenPagoNominaService.saveSingle(registro);
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
        System.out.println("selectByCriteria de ORDEN_PAGO_NOMINA");
        try {
            List<OrdenPagoNomina> lista = ordenPagoNominaService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ORDEN_PAGO_NOMINA");
        try {
            OrdenPagoNomina elimina = new OrdenPagoNomina();
            ordenPagoNominaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoints de proceso - fase 6
    // =====================================================================

    /**
     * Genera la orden de pago del periodo con su detalle por empleado.
     */
    @POST
    @Path("/generar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO generar - ORDEN_PAGO_NOMINA");
        try {
            Long idPeriodo = leeLong(datos, "idPeriodo");
            Long idCuentaBancaria = leeLong(datos, "idCuentaBancaria");
            String usuario = leeTexto(datos, "usuarioRegistro");
            OrdenPagoNomina orden = generacionOrdenPagoService.generar(idPeriodo, idCuentaBancaria, usuario);
            return Response.status(Response.Status.OK).entity(orden).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar la orden de pago: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devuelve el archivo bancario de la orden.
     */
    @GET
    @Path("/archivoBancario/{id}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response archivoBancario(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO archivoBancario - ORDEN_PAGO_NOMINA, orden: " + id);
        try {
            byte[] contenido = generacionOrdenPagoService.generarArchivoBancario(id);
            return Response.status(Response.Status.OK).entity(contenido)
                    .header("Content-Disposition", "attachment; filename=\"orden_pago_" + id + ".txt\"")
                    .type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al generar el archivo bancario: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Confirma la acreditacion de la orden y dispara el asiento de pago.
     */
    @POST
    @Path("/confirmar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO confirmar - ORDEN_PAGO_NOMINA, orden: " + id);
        try {
            LocalDate fechaAcreditacion = leeFecha(datos, "fechaAcreditacion");
            String usuario = leeTexto(datos, "usuarioRegistro");
            OrdenPagoNomina orden = generacionOrdenPagoService.confirmar(id, fechaAcreditacion, usuario);
            return Response.status(Response.Status.OK).entity(orden).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al confirmar la orden de pago: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Lectura del cuerpo
    // =====================================================================

    private Long leeLong(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? Long.valueOf(valor.toString()) : null;
    }

    private String leeTexto(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? valor.toString() : null;
    }

    private LocalDate leeFecha(Map<String, Object> datos, String clave) {
        Object valor = datos != null ? datos.get(clave) : null;
        return valor != null ? LocalDate.parse(valor.toString()) : null;
    }
}
