package com.saa.ws.rest.cxc;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.AplicacionPagoCxcService;
import com.saa.model.cxc.AplicacionPagoCxc;

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
 * REST para Aplicaciones de Cobro sobre facturas de venta (CXC).
 * Base path: /aplc
 *
 * Endpoints principales:
 *   GET  /aplc/factura/{id}          → historial de cobros/abonos de una factura
 *   GET  /aplc/saldo/{id}            → total, cobrado y saldo pendiente de una factura
 *   POST /aplc/cobroTransferencia    → registra un cobro recibido por transferencia
 *   POST /aplc/anticipo              → cruza anticipos por monto total (FIFO sobre los disponibles)
 *   POST /aplc/anticipos             → cruza anticipos ESPECÍFICOS elegidos por el usuario
 *   POST /aplc/revertir/{id}         → reversa una aplicación (requiere motivo)
 *
 * Las aplicaciones por retención recibida y por notas de crédito/débito NO se
 * registran desde aquí: se generan automáticamente junto con el asiento contable
 * del documento correspondiente.
 */
@Path("aplc")
public class AplicacionPagoCxcRest {

    @EJB
    private AplicacionPagoCxcService aplicacionPagoCxcService;

    @Context
    private UriInfo context;

    public AplicacionPagoCxcRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AplicacionPagoCxc> lista = aplicacionPagoCxcService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las aplicaciones de cobro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AplicacionPagoCxc aplicacion = aplicacionPagoCxcService.selectById(id);
            if (aplicacion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Aplicación de cobro con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(aplicacion)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la aplicación de cobro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Historial de aplicaciones de una factura de venta.
     * @param idFactura   : Id de la factura de venta
     * @param soloActivas : true (por defecto) devuelve solo las aplicaciones vigentes
     */
    @GET
    @Path("/factura/{idFactura}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porFactura(@PathParam("idFactura") Long idFactura,
            @QueryParam("soloActivas") @DefaultValue("true") boolean soloActivas) {
        System.out.println("LLEGA AL SERVICIO GET /aplc/factura/" + idFactura);
        try {
            List<AplicacionPagoCxc> lista =
                    aplicacionPagoCxcService.consultarPorFactura(idFactura, soloActivas);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las aplicaciones de la factura: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Total, cobrado y saldo pendiente de una factura de venta.
     * @param idFactura : Id de la factura de venta
     */
    @GET
    @Path("/saldo/{idFactura}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saldo(@PathParam("idFactura") Long idFactura) {
        System.out.println("LLEGA AL SERVICIO GET /aplc/saldo/" + idFactura);
        try {
            Map<String, Object> saldos = aplicacionPagoCxcService.saldoFactura(idFactura);
            return Response.status(Response.Status.OK).entity(saldos)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el saldo de la factura: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra un cobro recibido del cliente por transferencia bancaria.
     * Body esperado:
     * {
     *   "idFactura": 123,
     *   "valor": 500.00,
     *   "fechaCobro": "2026-08-07",
     *   "numeroTransferencia": "TRF-889977",
     *   "idCuentaBancaria": 4,
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Abono parcial"
     * }
     */
    @POST
    @Path("/cobroTransferencia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cobroTransferencia(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplc/cobroTransferencia");
        try {
            Long idFactura        = toLong(datos.get("idFactura"));
            Double valor          = toDouble(datos.get("valor"));
            String fechaCobro     = (String) datos.get("fechaCobro");
            String numTransfer    = (String) datos.get("numeroTransferencia");
            Long idCuentaBancaria = toLong(datos.get("idCuentaBancaria"));
            Long idEmpresa        = toLong(datos.get("idEmpresa"));
            Long idUsuario        = toLong(datos.get("idUsuario"));
            String observacion    = (String) datos.get("observacion");

            if (idFactura == null || valor == null || idCuentaBancaria == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFactura, valor, idCuentaBancaria e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (numTransfer == null || numTransfer.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el número de la transferencia recibida.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxcService.aplicarCobroTransferencia(
                    idFactura, valor, fechaCobro, numTransfer, idCuentaBancaria, idEmpresa,
                    idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el cobro: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cruza el saldo de anticipos del cliente contra una factura de venta.
     * Body esperado:
     * {
     *   "idFactura": 123,
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
        System.out.println("LLEGA AL SERVICIO POST /aplc/anticipo");
        try {
            Long idFactura     = toLong(datos.get("idFactura"));
            Double valor       = toDouble(datos.get("valor"));
            String fecha       = (String) datos.get("fechaAplicacion");
            Long idEmpresa     = toLong(datos.get("idEmpresa"));
            Long idUsuario     = toLong(datos.get("idUsuario"));
            String observacion = (String) datos.get("observacion");

            if (idFactura == null || valor == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFactura, valor e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxcService.aplicarAnticipo(
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
     * Cruza anticipos ESPECÍFICOS del cliente contra una factura de venta.
     * Cada línea dice de qué anticipo sale el dinero y cuánto, y genera su
     * propia aplicación con su propio asiento.
     * <p>
     * Los anticipos elegibles se consultan con
     * {@code GET /antp/disponibles/{idTitular}/{idEmpresa}}.
     * <p>
     * Body esperado:
     * <pre>
     * {
     *   "idFactura": 123,
     *   "anticipos": [ { "idAnticipo": 7, "valor": 300.00 },
     *                  { "idAnticipo": 9, "valor": 200.00 } ],
     *   "fechaAplicacion": "2026-08-20",
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Cruce parcial"
     * }
     * </pre>
     */
    @POST
    @Path("/anticipos")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aplicarAnticipos(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplc/anticipos");
        try {
            Long idFactura     = toLong(datos.get("idFactura"));
            String fecha       = (String) datos.get("fechaAplicacion");
            Long idEmpresa     = toLong(datos.get("idEmpresa"));
            Long idUsuario     = toLong(datos.get("idUsuario"));
            String observacion = (String) datos.get("observacion");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> anticipos =
                    (List<Map<String, Object>>) datos.get("anticipos");

            if (idFactura == null || idEmpresa == null || anticipos == null || anticipos.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFactura, idEmpresa y al menos un anticipo.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = aplicacionPagoCxcService.aplicarAnticipos(
                    idFactura, anticipos, fecha, idEmpresa, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cruzar los anticipos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reversa una aplicación de cobro.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/revertir/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertir(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /aplc/revertir/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la reversión.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado =
                    aplicacionPagoCxcService.revertirAplicacion(id, motivo, idUsuario);
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
        System.out.println("selectByCriteria AplicacionPagoCxc");
        try {
            return Response.status(Response.Status.OK)
                    .entity(aplicacionPagoCxcService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria AplicacionPagoCxc: " + e.getMessage())
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
