package com.saa.ws.rest.cxp;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.service.AplicacionPagoCxpService;
import com.saa.model.cxp.AplicacionPagoCxp;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST para Aplicaciones de Pago sobre facturas de compra (CXP).
 * Base path: /aplp
 *
 * Endpoints principales:
 *   GET  /aplp/factura/{id}              → historial de aplicaciones de una factura
 *   GET  /aplp/saldo/{id}                → total, aplicado y saldo pendiente de una factura
 *   POST /aplp/anticipo                  → cruza saldo de anticipos del proveedor con una factura
 *   POST /aplp/revertir/{id}             → reversa una aplicación (requiere motivo)
 *   GET  /aplp/getAll                    → todas las aplicaciones
 *   GET  /aplp/getId/{id}                → aplicación por ID
 *   POST /aplp/selectByCriteria          → búsqueda por criterios
 *
 * Las aplicaciones por retención y por notas de crédito/débito NO se registran
 * desde aquí: se generan automáticamente junto con el asiento contable del
 * documento correspondiente.
 */
@Path("aplp")
public class AplicacionPagoCxpRest {

    @EJB
    private AplicacionPagoCxpService aplicacionPagoCxpService;

    @Context
    private UriInfo context;

    public AplicacionPagoCxpRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AplicacionPagoCxp> lista = aplicacionPagoCxpService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener aplicaciones de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AplicacionPagoCxp aplicacion = aplicacionPagoCxpService.selectById(id);
            if (aplicacion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Aplicación de pago con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(aplicacion)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la aplicación de pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Historial de aplicaciones de una factura de compra.
     * @param idFactura   : Id de la factura de compra
     * @param soloActivas : true (por defecto) devuelve solo las aplicaciones vigentes
     */
    @GET
    @Path("/factura/{idFactura}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porFactura(@PathParam("idFactura") Long idFactura,
            @QueryParam("soloActivas") @DefaultValue("true") boolean soloActivas) {
        System.out.println("LLEGA AL SERVICIO GET /aplp/factura/" + idFactura);
        try {
            List<AplicacionPagoCxp> lista =
                    aplicacionPagoCxpService.consultarPorFactura(idFactura, soloActivas);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las aplicaciones de la factura: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Total, aplicado y saldo pendiente de una factura de compra.
     * @param idFactura : Id de la factura de compra
     */
    @GET
    @Path("/saldo/{idFactura}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldo(@PathParam("idFactura") Long idFactura) {
        System.out.println("LLEGA AL SERVICIO GET /aplp/saldo/" + idFactura);
        try {
            Map<String, Object> saldos = aplicacionPagoCxpService.saldoFactura(idFactura);
            return Response.status(Response.Status.OK).entity(saldos)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el saldo de la factura: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cruza el saldo de anticipos del proveedor contra una factura de compra.
     * Body esperado:
     * {
     *   "idFacturaCompra": 123,
     *   "valor": 225.00,
     *   "fechaAplicacion": "2026-08-07",
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Cruce parcial"
     * }
     */
    @POST
    @Path("/anticipo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarAnticipo(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplp/anticipo");
        try {
            Long idFactura   = toLong(datos.get("idFacturaCompra"));
            Double valor     = toDouble(datos.get("valor"));
            String fecha     = (String) datos.get("fechaAplicacion");
            Long idEmpresa   = toLong(datos.get("idEmpresa"));
            Long idUsuario   = toLong(datos.get("idUsuario"));
            String observacion = (String) datos.get("observacion");

            if (idFactura == null || valor == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturaCompra, valor e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxpService.aplicarAnticipo(
                    idFactura, valor, fecha, idEmpresa, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cruzar el anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reversa una aplicación de pago.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/revertir/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertir(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplp/revertir/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la reversión.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado =
                    aplicacionPagoCxpService.revertirAplicacion(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reversar la aplicación: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria AplicacionPagoCxp");
        try {
            return Response.status(Response.Status.OK)
                    .entity(aplicacionPagoCxpService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria AplicacionPagoCxp: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── Helpers de conversión del JSON ───────────────────────────────────────

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).doubleValue();
        try {
            return Double.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
