package com.saa.ws.rest.cxp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.service.PagoProgramadoService;
import com.saa.model.cxp.PagoProgramado;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
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
 * REST para Pagos a Proveedores por transferencia (CXP).
 * Base path: /pgtr
 *
 * Flujo de la pantalla:
 *   POST /pgtr                       → registra un pago sobre una factura
 *   GET  /pgtr/listar                → listado para seleccionar qué se paga
 *   POST /pgtr/lote                  → genera el archivo para el banco con los seleccionados
 *   GET  /pgtr/lote/{id}/archivo     → vuelve a descargar el archivo de un lote
 *   POST /pgtr/lote/{id}/respuesta   → carga el archivo de respuesta del banco
 *   POST /pgtr/anular/{id}           → anula un pago no confirmado (requiere motivo)
 *   POST /pgtr/revertirConfirmado/{id} → reversa un pago ya confirmado (requiere motivo)
 */
@Path("pgtr")
public class PagoProgramadoRest {

    @EJB
    private PagoProgramadoService pagoProgramadoService;

    @Context
    private UriInfo context;

    public PagoProgramadoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PagoProgramado> lista = pagoProgramadoService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los pagos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PagoProgramado pago = pagoProgramadoService.selectById(id);
            if (pago == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Pago con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(pago)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de pagos para la pantalla de selección.
     * @param idEmpresa : Id de la empresa (obligatorio)
     * @param estado    : 1=Registrado 2=En archivo 3=Confirmado 4=Rechazado 5=Anulado (opcional)
     * @param idTitular : Id del proveedor (opcional)
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("estado") Long estado,
            @QueryParam("idTitular") Long idTitular) {
        System.out.println("LLEGA AL SERVICIO GET /pgtr/listar");
        try {
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            List<PagoProgramado> lista = pagoProgramadoService.listar(idEmpresa, estado, idTitular);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar los pagos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra un pago por transferencia sobre una factura de compra.
     * Body esperado:
     * {
     *   "idFacturaCompra": 123,
     *   "idCuentaBancariaOrigen": 4,
     *   "idCuentaDestinoTitular": 9,
     *   "valor": 1500.00,
     *   "fechaProgramada": "2026-08-15",
     *   "idEmpresa": 1,
     *   "idUsuario": 5,
     *   "observacion": "Pago factura agosto"
     * }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr");
        try {
            Long idFactura      = toLong(datos.get("idFacturaCompra"));
            Long idCuentaOrigen = toLong(datos.get("idCuentaBancariaOrigen"));
            Long idCuentaDest   = toLong(datos.get("idCuentaDestinoTitular"));
            Double valor        = toDouble(datos.get("valor"));
            String fecha        = (String) datos.get("fechaProgramada");
            Long idEmpresa      = toLong(datos.get("idEmpresa"));
            Long idUsuario      = toLong(datos.get("idUsuario"));
            String observacion  = (String) datos.get("observacion");

            if (idFactura == null || idCuentaOrigen == null || valor == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturaCompra, idCuentaBancariaOrigen, valor e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = pagoProgramadoService.registrarPago(idFactura,
                    idCuentaOrigen, idCuentaDest, valor, fecha, idEmpresa, idUsuario, observacion);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Genera el archivo de pagos para el banco con los pagos seleccionados.
     * Seleccionar un pago aquí equivale a aprobarlo.
     * Body esperado:
     * {
     *   "idsPagos": [12, 13, 14],
     *   "idCuentaOrigen": 4,
     *   "idEmpresa": 1,
     *   "idUsuario": 5
     * }
     */
    @POST
    @Path("/lote")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarLote(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/lote");
        try {
            List<Long> idsPagos  = toLongList(datos.get("idsPagos"));
            Long idCuentaOrigen  = toLong(datos.get("idCuentaOrigen"));
            Long idEmpresa       = toLong(datos.get("idEmpresa"));
            Long idUsuario       = toLong(datos.get("idUsuario"));

            if (idsPagos.isEmpty() || idCuentaOrigen == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idsPagos, idCuentaOrigen e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado =
                    pagoProgramadoService.generarLote(idsPagos, idCuentaOrigen, idEmpresa, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar el archivo de pagos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devuelve el contenido del archivo de un lote ya generado.
     * @param idLote : Id del lote
     */
    @GET
    @Path("/lote/{idLote}/archivo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response archivoLote(@PathParam("idLote") Long idLote) {
        System.out.println("LLEGA AL SERVICIO GET /pgtr/lote/" + idLote + "/archivo");
        try {
            Map<String, Object> resultado = pagoProgramadoService.obtenerArchivoLote(idLote);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el archivo del lote: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Carga el archivo de respuesta del banco (Excel) para el lote indicado.
     * Se envía el contenido binario del archivo en el cuerpo de la petición.
     * @param idLote : Id del lote
     * @param archivo : Contenido del archivo de respuesta
     */
    @POST
    @Path("/lote/{idLote}/respuesta")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesarRespuesta(@PathParam("idLote") Long idLote,
            @QueryParam("idUsuario") Long idUsuario, byte[] archivo) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/lote/" + idLote + "/respuesta");
        try {
            if (archivo == null || archivo.length == 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se recibió el archivo de respuesta del banco.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            Map<String, Object> resultado =
                    pagoProgramadoService.procesarRespuestaBanco(idLote, archivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al procesar la respuesta del banco: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un pago que aún no fue confirmado por el banco.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/anular/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la anulación.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = pagoProgramadoService.anularPago(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reversa un pago ya confirmado por el banco.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/revertirConfirmado/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertirConfirmado(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/revertirConfirmado/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la reversión.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado =
                    pagoProgramadoService.revertirPagoConfirmado(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al reversar el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria PagoProgramado");
        try {
            return Response.status(Response.Status.OK)
                    .entity(pagoProgramadoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria PagoProgramado: " + e.getMessage())
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

    private List<Long> toLongList(Object valor) {
        List<Long> ids = new ArrayList<>();
        if (valor instanceof List) {
            for (Object item : (List<?>) valor) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
