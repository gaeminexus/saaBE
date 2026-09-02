package com.saa.ws.rest.cxp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.service.ConflictoNegocioException;
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
 *                                      (con "debitoAutomatico": true el pago ya
 *                                       ejecutado por el banco se abona y se
 *                                       contabiliza en la misma llamada, sin
 *                                       pasar por aprobación ni por lote)
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
     *
     * <b>"idCuentaBancariaOrigen" es opcional desde el 2026-08-27</b> (punto 14, ver
     * docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md): si se omite (o viene
     * null), el pago nace en estado POR_APROBAR (0), sin cuenta ni forma de pago — aparece
     * en {@code GET /pgtr/porAprobar} hasta que tesorería lo apruebe con
     * {@code POST /pgtr/aprobar}. Con cuenta, el comportamiento no cambió: nace REGISTRADO
     * (o CONFIRMADO con cheque/débito automático), como siempre.
     *
     * Para un pago que el banco ya debitó por convenio se agregan:
     * {
     *   "debitoAutomatico": true,
     *   "referencia": "DEB-AUT-0099"     (opcional)
     * }
     * En ese caso "fechaProgramada" es la fecha del débito, "idCuentaDestinoTitular"
     * no hace falta, y el pago queda confirmado con la factura abonada y el
     * asiento contable generado en la misma llamada.
     *
     * "formaPago" (opcional): 1=Efectivo, 2=Transferencia, 3=Cheque, 4=Débito automático.
     * Si se omite se infiere de "debitoAutomatico" (2 o 4). Con formaPago=3 la cuenta de
     * origen debe manejar chequera ("manejaChequera"=1); no hace falta cuentaDestinoTitular,
     * el sistema asigna el siguiente cheque disponible y el pago queda confirmado con la
     * respuesta incluyendo "numeroCheque".
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
            boolean debitoAut   = toBoolean(datos.get("debitoAutomatico"));
            String referencia   = (String) datos.get("referencia");
            Long formaPago      = toLong(datos.get("formaPago"));

            if (idFactura == null || valor == null || idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturaCompra, valor e idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = pagoProgramadoService.registrarPago(idFactura,
                    idCuentaOrigen, idCuentaDest, valor, fecha, idEmpresa, idUsuario, observacion,
                    debitoAut, referencia, formaPago);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Bandeja de pagos POR_APROBAR (punto 14). Proyección, no la entidad — ver
     * docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md.
     * @param idEmpresa : Id de la empresa (obligatorio)
     * @param origen    : OrigenPagoCxp (FACTURA_COMPRA, EGRESO_TESORERIA, ANTICIPO_PROVEEDOR)
     *                    u OrigenPagoExterno (CRD_DEVOLUCION_APORTE, TSR_CAJA_CHICA,
     *                    RHH_ANTICIPO_EMPLEADO); opcional, sin filtro si se omite
     * @param desde     : Fecha solicitada desde, yyyy-MM-dd (opcional)
     * @param hasta     : Fecha solicitada hasta, yyyy-MM-dd (opcional)
     */
    @GET
    @Path("/porAprobar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porAprobar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("origen") String origen,
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        System.out.println("LLEGA AL SERVICIO GET /pgtr/porAprobar");
        try {
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            List<com.saa.model.cxp.PagoPorAprobar> lista =
                    pagoProgramadoService.porAprobar(idEmpresa, origen, desde, hasta);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la bandeja de pagos por aprobar: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Disponibilidad real de una cuenta bancaria a una fecha (punto 14, fase 3): saldo
     * contable, comprometido (pagos REGISTRADO/EN_ARCHIVO de esa cuenta) y disponible = saldo
     * − comprometido. Ver docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md §7.
     *
     * GET /rest/pgtr/disponibilidad/4?fecha=2026-08-28
     *
     * @param idCuenta : Id de la cuenta bancaria
     * @param fecha    : Fecha de corte, yyyy-MM-dd (opcional, vacío = hoy)
     */
    @GET
    @Path("/disponibilidad/{idCuenta}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disponibilidad(@PathParam("idCuenta") Long idCuenta,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO GET /pgtr/disponibilidad/" + idCuenta);
        try {
            Map<String, Object> resultado = pagoProgramadoService.disponibilidad(idCuenta, fecha);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al calcular la disponibilidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Ids de las facturas de compra del proveedor cuyo saldo pendiente ya está íntegramente
     * comprometido por pagos vigentes (POR_APROBAR/REGISTRADO/EN_ARCHIVO/CONFIRMADO, no
     * RECHAZADO ni ANULADO) y por eso no deberían volver a ofrecerse en el combo de registrar
     * pagos. Un pago PARCIAL no saca la factura de la lista.
     * Ver docs/logica-negocio/cxp/DISENO-FACTURAS-COMPROMETIDAS-EN-COMBO-PAGOS.md.
     *
     * GET /rest/pgtr/facturasComprometidas/45 → { "idTitular": 45, "idsFacturas": [12, 87, 103] }
     */
    @GET
    @Path("/facturasComprometidas/{idTitular}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response facturasComprometidas(@PathParam("idTitular") Long idTitular) {
        System.out.println("LLEGA AL SERVICIO GET /pgtr/facturasComprometidas/" + idTitular);
        try {
            Map<String, Object> resultado = pagoProgramadoService.facturasComprometidas(idTitular);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al calcular las facturas comprometidas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Aprueba en bloque los pagos POR_APROBAR indicados: asigna cuenta bancaria y forma de
     * pago, gira el cheque si formaPago=3 y deja cada pago REGISTRADO (transferencia) o
     * CONFIRMADO (cheque o débito automático, contabilizando en el acto). Punto 14, ver
     * docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md.
     *
     * <b>No valida disponibilidad de saldo todavía</b> (fase 3, pendiente de decidir de
     * dónde sale el saldo bancario confiable — ver
     * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §7bis).
     *
     * Body esperado:
     * {
     *   "idsPagos": [21, 22, 23],
     *   "idCuentaBancaria": 4,
     *   "formaPago": 2,
     *   "fechaPago": "2026-08-27",
     *   "idUsuario": 5,
     *   "agruparEnUnCheque": true       (opcional, default false; sólo con formaPago=3)
     * }
     * "formaPago": 2=Transferencia, 3=Cheque, 4=Débito automático (1=Efectivo no soportado).
     * "agruparEnUnCheque" gira UN solo cheque por el total del lote en vez de uno por pago
     * (docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md). Todos los pagos del lote
     * deben compartir el mismo titular o se rechaza con 409.
     */
    @POST
    @Path("/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/aprobar");
        try {
            List<Long> idsPagos = toLongList((datos != null) ? datos.get("idsPagos") : null);
            Long idCuentaBancaria = (datos != null) ? toLong(datos.get("idCuentaBancaria")) : null;
            Long formaPago = (datos != null) ? toLong(datos.get("formaPago")) : null;
            String fechaPago = (datos != null) ? (String) datos.get("fechaPago") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            boolean agruparEnUnCheque = (datos != null) && toBoolean(datos.get("agruparEnUnCheque"));

            if (idsPagos.isEmpty() || idCuentaBancaria == null || formaPago == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idsPagos, idCuentaBancaria y formaPago.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = pagoProgramadoService.aprobar(idsPagos, idCuentaBancaria,
                    formaPago, fechaPago, idUsuario, Boolean.valueOf(agruparEnUnCheque));
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (ConflictoNegocioException e) {
            // La validación de beneficiario único de agruparEnUnCheque (§6 del diseño)
            // es la única razón de este método para responder 409: el resto de las
            // IncomeException de aprobar (cuenta inexistente, pago no POR_APROBAR, saldo
            // insuficiente, ...) siguen siendo 400, como ya eran — catch por TIPO, no por
            // texto del mensaje, para que un cambio de redacción no cambie el status en
            // silencio (ConflictoNegocioException extends IncomeException, así que este
            // catch tiene que ir antes que el catch(Throwable) genérico de abajo).
            return Response.status(Response.Status.CONFLICT)
                    .entity("Error al aprobar los pagos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al aprobar los pagos: " + e.getMessage())
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
     * Confirma manualmente pagos que siguen esperando al banco, como si hubiera
     * llegado el archivo de respuesta: genera aplicación, asiento contable y
     * movimiento bancario.
     * Body esperado:
     *   { "idsPagos": [12, 13], "referencia": "TRX-9981",
     *     "fechaPago": "2026-08-13", "observacion": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/confirmarManual")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarManual(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /pgtr/confirmarManual");
        try {
            List<Long> idsPagos = toLongList((datos != null) ? datos.get("idsPagos") : null);
            if (idsPagos.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe seleccionar al menos un pago para confirmar.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            String referencia  = (String) datos.get("referencia");
            String fechaPago   = (String) datos.get("fechaPago");
            String observacion = (String) datos.get("observacion");
            Long   idUsuario   = toLong(datos.get("idUsuario"));

            Map<String, Object> resultado = pagoProgramadoService.confirmarPagosManual(
                    idsPagos, referencia, fechaPago, observacion, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al confirmar los pagos: " + e.getMessage())
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
        } catch (ConflictoNegocioException e) {
            // D2 (docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md §6): el
            // reverso rechazado por cheque compartido con otros pagos es 409, no el
            // 500 que este catch ya devolvía para el resto de las excepciones. Catch
            // por TIPO (ver PagoProgramadoRest.aprobar), tiene que ir antes que el
            // catch(Throwable) genérico de abajo.
            return Response.status(Response.Status.CONFLICT)
                    .entity("Error al reversar el pago: " + e.getMessage())
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

    private boolean toBoolean(Object valor) {
        if (valor == null) return false;
        if (valor instanceof Boolean) return ((Boolean) valor).booleanValue();
        if (valor instanceof Number) return ((Number) valor).intValue() == 1;
        String texto = valor.toString().trim();
        return "true".equalsIgnoreCase(texto) || "1".equals(texto);
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
