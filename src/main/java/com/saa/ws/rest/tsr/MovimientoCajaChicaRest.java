package com.saa.ws.rest.tsr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.MovimientoCajaChicaDaoService;
import com.saa.ejb.tsr.service.MovimientoCajaChicaService;
import com.saa.model.tsr.MovimientoCajaChica;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST para MovimientoCajaChica (TSR.MVCH). Base path: /mvch
 *
 * Endpoints principales:
 *   POST /mvch/gasto       → registra un gasto y lo contabiliza en el acto
 *   POST /mvch/reposicion  → reposición del fondo desde una cuenta bancaria (circuito de pagos)
 *   POST /mvch/apertura    → apertura de una caja nueva pagada desde una cuenta bancaria
 *   POST /mvch/anular/{id} → anula un gasto (no aperturas/reposiciones: reverse su pago)
 *   GET  /mvch/listar      → movimientos de una caja, con filtros
 */
@Path("mvch")
public class MovimientoCajaChicaRest {

    @EJB
    private MovimientoCajaChicaDaoService movimientoCajaChicaDaoService;

    @EJB
    private MovimientoCajaChicaService movimientoCajaChicaService;

    @Context
    private UriInfo context;

    public MovimientoCajaChicaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<MovimientoCajaChica> lista =
                    movimientoCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.MOVIMIENTO_CAJA_CHICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener movimientos de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            MovimientoCajaChica entidad =
                    movimientoCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.MOVIMIENTO_CAJA_CHICA);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Movimiento de caja chica con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el movimiento de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(MovimientoCajaChica registro) {
        try {
            MovimientoCajaChica resultado = movimientoCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar el movimiento de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(MovimientoCajaChica registro) {
        try {
            MovimientoCajaChica resultado = movimientoCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear el movimiento de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra un gasto de caja chica y lo contabiliza en el acto.
     * Body esperado:
     * {
     *   "idCaja": 1, "fecha": "2026-08-27", "valor": 12.50,
     *   "descripcion": "Compra de suministros", "observacion": "Factura sin RUC del proveedor",
     *   "idProducto": 7, "idTitular": 25, "numeroDocumento": "001-001-000123", "idUsuario": 5
     * }
     */
    @POST
    @Path("/gasto")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response gasto(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /mvch/gasto");
        try {
            Long idCaja = toLong(datos.get("idCaja"));
            LocalDate fecha = toFecha((String) datos.get("fecha"));
            Double valor = toDouble(datos.get("valor"));
            String descripcion = (String) datos.get("descripcion");
            String observacion = (String) datos.get("observacion");
            Long idProducto = toLong(datos.get("idProducto"));
            Long idTitular = toLong(datos.get("idTitular"));
            String numeroDocumento = (String) datos.get("numeroDocumento");
            Long idUsuario = toLong(datos.get("idUsuario"));

            MovimientoCajaChica resultado = movimientoCajaChicaService.registrarGasto(idCaja, fecha, valor,
                    descripcion, observacion, idProducto, idTitular, numeroDocumento, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el gasto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reposición del fondo desde una cuenta bancaria. Body igual al de
     * "apertura" (ver abajo).
     */
    @POST
    @Path("/reposicion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reposicion(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /mvch/reposicion");
        try {
            Map<String, Object> resultado = movimientoCajaChicaService.registrarReposicion(
                    toLong(datos.get("idCaja")), toDouble(datos.get("valor")),
                    toLong(datos.get("idCuentaBancariaOrigen")), toLong(datos.get("formaPago")),
                    toBoolean(datos.get("debitoAutomatico")), (String) datos.get("referencia"),
                    toFecha((String) datos.get("fecha")), (String) datos.get("descripcion"),
                    toLong(datos.get("idUsuario")));
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la reposición: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Apertura del fondo de una caja nueva, pagada desde una cuenta bancaria.
     * Body esperado:
     * {
     *   "idCaja": 1, "valor": 500.00, "idCuentaBancariaOrigen": 4,
     *   "formaPago": 3, "debitoAutomatico": false, "referencia": "",
     *   "fecha": "2026-08-27", "descripcion": "Apertura fondo caja Matriz",
     *   "idUsuario": 5
     * }
     * `formaPago`: 1=Efectivo (no soportado), 2=Transferencia, 3=Cheque, 4=Débito automático.
     */
    @POST
    @Path("/apertura")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response apertura(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /mvch/apertura");
        try {
            Map<String, Object> resultado = movimientoCajaChicaService.registrarApertura(
                    toLong(datos.get("idCaja")), toDouble(datos.get("valor")),
                    toLong(datos.get("idCuentaBancariaOrigen")), toLong(datos.get("formaPago")),
                    toBoolean(datos.get("debitoAutomatico")), (String) datos.get("referencia"),
                    toFecha((String) datos.get("fecha")), (String) datos.get("descripcion"),
                    toLong(datos.get("idUsuario")));
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la apertura: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un gasto (no aplica a aperturas/reposiciones: reverse su pago en /pgtr).
     * Body: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /mvch/anular/" + id);
        try {
            String motivo = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            movimientoCajaChicaService.anularGasto(id, motivo, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", "Gasto anulado correctamente."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el gasto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de movimientos de una caja, con filtros opcionales.
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idCaja") Long idCaja,
            @QueryParam("desde") String desde, @QueryParam("hasta") String hasta,
            @QueryParam("tipo") Long tipo, @QueryParam("estado") Long estado) {
        System.out.println("LLEGA AL SERVICIO GET /mvch/listar");
        try {
            if (idCaja == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idCaja.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            List<MovimientoCajaChica> resultado = movimientoCajaChicaService.listar(idCaja,
                    toFecha(desde), toFecha(hasta), tipo, estado);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar movimientos de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(movimientoCajaChicaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            MovimientoCajaChica elimina = new MovimientoCajaChica();
            movimientoCajaChicaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar el movimiento de caja chica: " + e.getMessage())
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

    private boolean toBoolean(Object valor) {
        if (valor == null) return false;
        if (valor instanceof Boolean) return ((Boolean) valor).booleanValue();
        if (valor instanceof Number) return ((Number) valor).intValue() == 1;
        return "true".equalsIgnoreCase(valor.toString()) || "1".equals(valor.toString());
    }

    private LocalDate toFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
