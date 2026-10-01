package com.saa.ws.rest.cxp;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.service.DevolucionAnticipoProveedorService;
import com.saa.model.cxp.DevolucionAnticipoProveedor;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para la devolución del saldo de anticipos de un proveedor (depósito en nuestro banco).
 * Base path: /dvpr
 *
 * Endpoints principales:
 *   POST /dvpr/registrar     → registra el depósito: asiento, cabecera+detalle, saldo de cada
 *                              anticipo y del PRCC, y el movimiento bancario para conciliar
 *   POST /dvpr/anular/{id}   → anula la devolución (si no está conciliada) y repone los saldos
 *   GET  /dvpr/listar        → devoluciones de un proveedor, proyección con su detalle
 *   GET  /dvpr/getAll        → todas las devoluciones
 *   GET  /dvpr/getId/{id}    → devolución por ID
 *   POST /dvpr/selectByCriteria → búsqueda por criterios
 *
 * Ver docs/logica-negocio/cxp/API-DEVOLUCION-ANTICIPO-PROVEEDOR.md.
 */
@Path("dvpr")
public class DevolucionAnticipoProveedorRest {

    @EJB
    private DevolucionAnticipoProveedorService devolucionAnticipoProveedorService;

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DevolucionAnticipoProveedor> lista = devolucionAnticipoProveedorService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener devoluciones de anticipos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DevolucionAnticipoProveedor entidad = devolucionAnticipoProveedorService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Devolución de anticipo con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DevolucionAnticipoProveedor registro) {
        System.out.println("LLEGA AL SERVICIO POST /dvpr");
        try {
            DevolucionAnticipoProveedor resultado = devolucionAnticipoProveedorService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DevolucionAnticipoProveedor registro) {
        System.out.println("LLEGA AL SERVICIO PUT /dvpr");
        try {
            DevolucionAnticipoProveedor resultado = devolucionAnticipoProveedorService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE /dvpr/" + id);
        try {
            Map<String, Object> resultado = devolucionAnticipoProveedorService.anular(
                    id, "Anulación desde la interfaz (DELETE)", null);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<DevolucionAnticipoProveedor> lista = devolucionAnticipoProveedorService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en búsqueda de devoluciones de anticipos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra la devolución del saldo de uno o más anticipos de un proveedor: un depósito en
     * una cuenta bancaria propia.
     * Body: {"idEmpresa":1236,"idTitular":187,"idCuentaBancaria":417,"fecha":"2026-09-30",
     *        "referencia":"DEP 12345","observacion":"…","idUsuario":5,
     *        "anticipos":[{"idAnticipo":7,"valor":8.64},{"idAnticipo":10,"valor":15.30}]}
     * Respuesta 200: {exito, mensaje, devolucion, asiento}.
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dvpr/registrar");
        try {
            Long idEmpresa = toLong(datos != null ? datos.get("idEmpresa") : null);
            Long idTitular = toLong(datos != null ? datos.get("idTitular") : null);
            Long idCuentaBancaria = toLong(datos != null ? datos.get("idCuentaBancaria") : null);
            String fecha = (String) (datos != null ? datos.get("fecha") : null);
            String referencia = (String) (datos != null ? datos.get("referencia") : null);
            String observacion = (String) (datos != null ? datos.get("observacion") : null);
            Long idUsuario = toLong(datos != null ? datos.get("idUsuario") : null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> anticipos =
                    (datos != null) ? (List<Map<String, Object>>) datos.get("anticipos") : null;

            Map<String, Object> resultado = devolucionAnticipoProveedorService.registrar(
                    idEmpresa, idTitular, idCuentaBancaria, fecha, referencia, observacion,
                    idUsuario, anticipos);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula una devolución ACTIVA: repone el saldo de cada anticipo y el PRCC, y anula el
     * movimiento bancario y el asiento. Se rechaza si ya está conciliada.
     * Body: {"motivo":"...","idUsuario":5}
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dvpr/anular/" + id);
        try {
            String motivo = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            Map<String, Object> resultado = devolucionAnticipoProveedorService.anular(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la devolución de anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devoluciones de un proveedor (o de todos, sin idTitular) en una empresa, más reciente
     * primero, como proyección con su detalle por anticipo.
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") Long idEmpresa, @QueryParam("idTitular") Long idTitular) {
        System.out.println("LLEGA AL SERVICIO GET /dvpr/listar");
        try {
            List<Map<String, Object>> resultado = devolucionAnticipoProveedorService.listar(idEmpresa, idTitular);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar las devoluciones de anticipos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : Long.valueOf(texto);
    }
}
