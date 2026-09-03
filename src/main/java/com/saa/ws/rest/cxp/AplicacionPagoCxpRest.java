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
 *   GET  /aplp/factura/{id}              → historial de aplicaciones de una FACTURA
 *   GET  /aplp/liquidacion/{id}           → historial de aplicaciones de una LIQUIDACIÓN
 *   GET  /aplp/saldo/{id}                → total, aplicado y saldo pendiente de una FACTURA
 *   GET  /aplp/saldoLiquidacion/{id}      → total, aplicado y saldo pendiente de una LIQUIDACIÓN
 *   POST /aplp/anticipo                  → cruza anticipos por monto total (FIFO sobre los disponibles)
 *   POST /aplp/anticipos                 → cruza anticipos ESPECÍFICOS elegidos por el usuario
 *   POST /aplp/revertir/{id}             → reversa una aplicación (requiere motivo)
 *   GET  /aplp/getAll                    → todas las aplicaciones
 *   GET  /aplp/getId/{id}                → aplicación por ID
 *   POST /aplp/selectByCriteria          → búsqueda por criterios
 *
 * Las aplicaciones por retención y por notas de crédito/débito NO se registran
 * desde aquí: se generan automáticamente junto con el asiento contable del
 * documento correspondiente.
 *
 * ⛔ Los pares /aplp/saldo{,Liquidacion}/{id} y /aplp/factura{,liquidacion}/{id} NO son
 * intercambiables entre sí: FCTC y LQCC usan IDENTITY con numeraciones independientes, así
 * que pasarle un id de liquidación al endpoint de factura (o viceversa) devolvería los datos
 * de un documento ajeno que coincida en número, sin ningún error
 * (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md §4.2).
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
     * Historial de aplicaciones de una liquidación de compra.
     * ⛔ NO es intercambiable con /factura/{id} — ver el aviso en la cabecera de esta clase.
     * @param idLiquidacion : Id de la liquidación de compra
     * @param soloActivas   : true (por defecto) devuelve solo las aplicaciones vigentes
     */
    @GET
    @Path("/liquidacion/{idLiquidacion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porLiquidacion(@PathParam("idLiquidacion") Long idLiquidacion,
            @QueryParam("soloActivas") @DefaultValue("true") boolean soloActivas) {
        System.out.println("LLEGA AL SERVICIO GET /aplp/liquidacion/" + idLiquidacion);
        try {
            List<AplicacionPagoCxp> lista =
                    aplicacionPagoCxpService.consultarPorLiquidacion(idLiquidacion, soloActivas);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las aplicaciones de la liquidación: " + e.getMessage())
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
     * Total, aplicado y saldo pendiente de una liquidación de compra.
     * ⛔ NO es intercambiable con /saldo/{id} — ver el aviso en la cabecera de esta clase.
     * @param idLiquidacion : Id de la liquidación de compra
     */
    @GET
    @Path("/saldoLiquidacion/{idLiquidacion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldoLiquidacion(@PathParam("idLiquidacion") Long idLiquidacion) {
        System.out.println("LLEGA AL SERVICIO GET /aplp/saldoLiquidacion/" + idLiquidacion);
        try {
            Map<String, Object> saldos = aplicacionPagoCxpService.saldoLiquidacion(idLiquidacion);
            return Response.status(Response.Status.OK).entity(saldos)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el saldo de la liquidación de compra: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cruza anticipos del proveedor contra una factura de compra indicando solo
     * el monto total: el backend lo reparte entre los anticipos con saldo, del
     * más antiguo al más nuevo. Para elegir a mano de qué anticipos sale el
     * dinero, usar POST /aplp/anticipos.
     * Body esperado (contra factura):
     * {
     *   "idFacturaCompra": 123,
     *   "valor": 225.00,
     *   "fechaAplicacion": "2026-08-07",
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Cruce parcial"
     * }
     * O contra liquidación de compra, con "idLiquidacionCompra" en vez de
     * "idFacturaCompra" — exactamente uno de los dos, nunca los dos ni ninguno
     * (docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md §4.1).
     */
    @POST
    @Path("/anticipo")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarAnticipo(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplp/anticipo");
        try {
            Long idFactura     = toLong(datos.get("idFacturaCompra"));
            Long idLiquidacion = toLong(datos.get("idLiquidacionCompra"));
            Double valor     = toDouble(datos.get("valor"));
            String fecha     = (String) datos.get("fechaAplicacion");
            Long idEmpresa   = toLong(datos.get("idEmpresa"));
            Long idUsuario   = toLong(datos.get("idUsuario"));
            String observacion = (String) datos.get("observacion");

            if (valor == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar valor e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            // Exactamente uno de los dos: ninguno o los dos son 400, no "gana el
            // primero" — un cliente que manda los dos está confundido.
            if (idFactura == null && idLiquidacion == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturaCompra o idLiquidacionCompra.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (idFactura != null && idLiquidacion != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar sólo uno de los dos: idFacturaCompra o "
                                + "idLiquidacionCompra, no ambos.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxpService.aplicarAnticipo(
                    idFactura, idLiquidacion, valor, fecha, idEmpresa, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cruzar el anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cruza anticipos ESPECÍFICOS del proveedor contra una factura de compra.
     * Cada línea dice de qué anticipo sale el dinero y cuánto, y genera su
     * propia aplicación con su propio asiento.
     * <p>
     * Los anticipos elegibles se consultan con
     * {@code GET /antp/disponibles/{idTitular}/{idEmpresa}}.
     * <p>
     * Body esperado (contra factura):
     * <pre>
     * {
     *   "idFacturaCompra": 123,
     *   "anticipos": [ { "idAnticipo": 7, "valor": 300.00 },
     *                  { "idAnticipo": 9, "valor": 200.00 } ],
     *   "fechaAplicacion": "2026-08-20",
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Cruce parcial"
     * }
     * </pre>
     * O contra liquidación de compra, con "idLiquidacionCompra" en vez de
     * "idFacturaCompra" — mismo criterio de exclusividad que POST /aplp/anticipo.
     */
    @POST
    @Path("/anticipos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarAnticipos(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplp/anticipos");
        try {
            Long idFactura     = toLong(datos.get("idFacturaCompra"));
            Long idLiquidacion = toLong(datos.get("idLiquidacionCompra"));
            String fecha       = (String) datos.get("fechaAplicacion");
            Long idEmpresa     = toLong(datos.get("idEmpresa"));
            Long idUsuario     = toLong(datos.get("idUsuario"));
            String observacion = (String) datos.get("observacion");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> anticipos =
                    (List<Map<String, Object>>) datos.get("anticipos");

            if (idEmpresa == null || anticipos == null || anticipos.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa y al menos un anticipo.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (idFactura == null && idLiquidacion == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturaCompra o idLiquidacionCompra.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (idFactura != null && idLiquidacion != null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar sólo uno de los dos: idFacturaCompra o "
                                + "idLiquidacionCompra, no ambos.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxpService.aplicarAnticipos(
                    idFactura, idLiquidacion, anticipos, fecha, idEmpresa, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cruzar los anticipos: " + e.getMessage())
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
