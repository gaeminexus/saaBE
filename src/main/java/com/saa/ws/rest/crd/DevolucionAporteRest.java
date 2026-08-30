package com.saa.ws.rest.crd;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.basico.ejb.DetalleRubroDaoService;
import com.saa.ejb.crd.dao.DetalleDevolucionAporteDaoService;
import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.DevolucionAporteService;
import com.saa.ejb.crd.service.MotorPagoPrestamoService;
import com.saa.ejb.crd.service.dto.DetalleResumenDevolucion;
import com.saa.ejb.crd.service.dto.DeudaPrestamo;
import com.saa.ejb.crd.service.dto.DeudaVigenteParticipe;
import com.saa.ejb.crd.service.dto.ResultadoConsultaPagoDevolucion;
import com.saa.ejb.crd.service.dto.ResultadoDevolucionAporte;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.crd.service.dto.ResumenDevolucionAporte;
import com.saa.ejb.crd.service.dto.SolicitudAnulacionDevolucion;
import com.saa.ejb.crd.service.dto.SolicitudDevolucionAporte;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.DetalleDevolucionAporte;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.DevolucionAporte;
import com.saa.model.crd.Prestamo;
import com.saa.rubros.EstadoPrestamo;
import com.saa.rubros.Rubros;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Devolución de aportes a partícipes.
 *
 * Sobre de respuesta y mapeo de errores idénticos a los servicios de pago de préstamos
 * (§8 de ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md):
 *
 * <pre>
 * { "exito": true, "etapa": "VALIDACION|APLICACION", "mensaje": "...",
 *   "error": "CODIGO_ESTABLE", "resultado": { } }
 * </pre>
 *
 * Solo expone los cinco endpoints de la §6 del plan: registrar, listar por entidad, anular,
 * sincronizar y el aviso de deuda vigente. No hay CRUD genérico: una devolución no se crea
 * ni se edita a mano.
 */
@Path("dvap")
public class DevolucionAporteRest {

    @EJB
    private DevolucionAporteService devolucionAporteService;

    @EJB
    private DetalleDevolucionAporteDaoService detalleDevolucionAporteDaoService;

    @EJB
    private DetalleRubroDaoService detalleRubroDaoService;

    /** Solo lectura, para el aviso de deuda de la §6.5. No se toca ningún préstamo. */
    @EJB
    private PrestamoDaoService prestamoDaoService;

    /** Solo lectura, para contar las cuotas vencidas del aviso de deuda. */
    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    /**
     * Solo se le invoca {@code calcularTotalPendientePrestamo}, que es de lectura: el saldo
     * se reconstruye desde los PagoPrestamo vigentes. El motor de pagos no se modifica.
     */
    @EJB
    private MotorPagoPrestamoService motorPagoPrestamoService;

    @Context
    private UriInfo context;

    public DevolucionAporteRest() {
    }

    // ------------------------------------------------------------------------
    // §6.1 — Registro
    // ------------------------------------------------------------------------

    /**
     * Registra la devolución de aportes de un partícipe: genera las filas NEGATIVAS de
     * CRD.APRT y dispara la orden de pago en Cuentas por Pagar.
     *
     * El saldo del partícipe baja EN EL MOMENTO DEL REGISTRO, antes de que el dinero salga
     * del banco. Si el pago se rechaza, el reconciliador genera los contra-movimientos.
     *
     * @param solicitud { idEntidad, idCuentaBancariaParticipe, idEmpresa, idUsuario, usuario,
     *                    fecha, motivo, debitoAutomatico, referencia,
     *                    detalle:[{ idTipoAporte, valor }] }
     * @return 201 con el ResultadoDevolucionAporte; 400/404/409/422/500 según el fallo
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(SolicitudDevolucionAporte solicitud) {
        System.out.println("LLEGA AL SERVICIO REGISTRAR DEVOLUCION DE APORTES - Entidad: "
            + (solicitud != null ? solicitud.getIdEntidad() : null)
            + " - Lineas: " + (solicitud != null && solicitud.getDetalle() != null
                ? solicitud.getDetalle().size() : 0));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe enviar el cuerpo de la solicitud", DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getIdEntidad() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el partícipe (idEntidad)", DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getIdEmpresa() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar la empresa contable (idEmpresa)",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el usuario que registra la devolución",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getDetalle() == null || solicitud.getDetalle().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar al menos un tipo de aporte a devolver",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }

        try {
            ResultadoDevolucionAporte resultado =
                devolucionAporteService.registrarDevolucion(solicitud);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("mensaje", "Devolución registrada por $"
                + String.format(Locale.US, "%.2f", nvlDouble(resultado.getValorTotal()))
                + ". Orden de pago " + resultado.getIdPagoProgramado()
                + " generada en Cuentas por Pagar.");
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.CREATED)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError("registrar la devolución de aportes", e);
        }
    }

    // ------------------------------------------------------------------------
    // §6.2 — Listado por partícipe
    // ------------------------------------------------------------------------

    /**
     * Devoluciones de un partícipe. Reconcilia el estado de las órdenes de pago ANTES de
     * responder, así que lo que ve el usuario siempre está al día.
     *
     * Una lista vacía es 200 con [], no un error.
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @return 200 con la lista de devoluciones
     */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO DEVOLUCIONES POR ENTIDAD - Entidad: " + idEntidad);

        if (idEntidad == null || idEntidad <= 0) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar una entidad válida", DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }

        try {
            List<DevolucionAporte> devoluciones =
                devolucionAporteService.listarPorEntidad(idEntidad);

            List<ResumenDevolucionAporte> resultado = new ArrayList<>();
            for (DevolucionAporte devolucion : devoluciones) {
                resultado.add(armaResumen(devolucion));
            }

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("resultado", resultado);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError("obtener las devoluciones del partícipe", e);
        }
    }

    // ------------------------------------------------------------------------
    // §6.3 — Anulación
    // ------------------------------------------------------------------------

    /**
     * Anula una devolución que todavía no se pagó: genera los contra-movimientos positivos
     * (el saldo del partícipe vuelve a su valor previo) y anula la orden de pago.
     *
     * @param idDevolucion Código de la devolución
     * @param solicitud    { motivo, usuario, idUsuario }
     * @return 200 con el ResultadoDevolucionAporte; 409 si ya está pagada
     */
    @POST
    @Path("/anular/{idDevolucion}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("idDevolucion") Long idDevolucion,
            SolicitudAnulacionDevolucion solicitud) {
        System.out.println("LLEGA AL SERVICIO ANULAR DEVOLUCION DE APORTES - Devolucion: "
            + idDevolucion);

        if (idDevolucion == null || idDevolucion <= 0) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar una devolución válida",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe enviar el cuerpo de la solicitud",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el motivo de la anulación",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar el usuario que anula la devolución",
                DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }

        try {
            ResultadoDevolucionAporte resultado = devolucionAporteService.anularDevolucion(
                idDevolucion, solicitud.getMotivo(), solicitud.getUsuario());

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("mensaje", "Devolución " + idDevolucion + " anulada. Los aportes "
                + "devueltos volvieron al saldo del partícipe.");
            cuerpo.put("resultado", resultado);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError("anular la devolución de aportes", e);
        }
    }

    // ------------------------------------------------------------------------
    // §6.4 — Sincronización manual
    // ------------------------------------------------------------------------

    /**
     * Reconcilia todas las devoluciones pendientes contra el estado real de su orden de
     * pago. Es la recuperación manual: el timer hace exactamente lo mismo cada 30 minutos.
     *
     * Idempotente: correrlo dos veces seguidas no cambia nada.
     *
     * @return 200 con { evaluadas, marcadasPagadas, marcadasRechazadas, huerfanas,
     *         conError, errores }
     */
    @POST
    @Path("/sincronizar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sincronizar() {
        System.out.println("LLEGA AL SERVICIO SINCRONIZAR DEVOLUCIONES DE APORTES");

        try {
            ResultadoSincronizacion resultado = devolucionAporteService.sincronizarPagos();

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("mensaje", "Sincronización terminada: " + resultado.getEvaluadas()
                + " evaluada(s), " + resultado.getMarcadasPagadas() + " pagada(s), "
                + resultado.getMarcadasRechazadas() + " rechazada(s).");
            cuerpo.put("resultado", resultado);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError("sincronizar las devoluciones de aportes", e);
        }
    }

    /**
     * Consulta BAJO DEMANDA si el pago de una devolución ya se confirmó en Cuentas por
     * Pagar — botón "Consultar a contabilidad" del diálogo de un aporte negativo que todavía
     * no tiene fecha/referencia. Si se confirmó, copia esos dos datos a los PagoAporte de la
     * devolución (uno por tipo de aporte) y los devuelve.
     *
     * {@code confirmado: false} es una respuesta 200 normal, no un error: significa que
     * tesorería todavía no confirmó el pago — el frontend deja el botón visible. Con
     * {@code confirmado: true}, el frontend oculta el botón mirando {@code fecha} (siempre
     * viene con dato); {@code referencia} puede venir en {@code null} legítimamente, cuando
     * tesorería confirmó manualmente sin escribir una referencia — mostrar algo como
     * "no registrada" en ese caso, nunca un espacio en blanco.
     *
     * @param idDevolucion Código de la devolución (CRD.DVAP)
     * @return 200 con { confirmado, fecha, referencia, mensaje }
     */
    @POST
    @Path("/{idDevolucion}/consultarPago")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultarPago(@PathParam("idDevolucion") Long idDevolucion) {
        System.out.println("LLEGA AL SERVICIO CONSULTAR PAGO DEVOLUCION - idDevolucion: " + idDevolucion);
        try {
            ResultadoConsultaPagoDevolucion resultado =
                devolucionAporteService.consultarPagoDevolucion(idDevolucion);
            return Response.status(Response.Status.OK)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError("consultar el pago de la devolución " + idDevolucion, e);
        }
    }

    /**
     * A qué devolución pertenece un aporte negativo — para que el diálogo del "ojo" pueda
     * armar el botón "Consultar a contabilidad" (que llama a
     * {@code POST /dvap/{idDevolucion}/consultarPago}) sin tener que resolver la relación en
     * el cliente.
     *
     * @param idAporte Código del aporte (CRD.APRT)
     * @return 200 con {@code { idDevolucion }} ({@code null} si el aporte no es de una
     *         devolución o no se pudo enlazar), o 404 si el aporte no existe
     */
    @GET
    @Path("/porAporte/{idAporte}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porAporte(@PathParam("idAporte") Long idAporte) {
        System.out.println("LLEGA AL SERVICIO GET porAporte DEVOLUCION - idAporte: " + idAporte);
        try {
            Long idDevolucion = devolucionAporteService.obtenerIdDevolucionPorAporte(idAporte);
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("idAporte", idAporte);
            cuerpo.put("idDevolucion", idDevolucion);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError("obtener la devolución del aporte " + idAporte, e);
        }
    }

    // ------------------------------------------------------------------------
    // §6.5 — Aviso de deuda vigente
    // ------------------------------------------------------------------------

    /**
     * Deuda vigente del partícipe: los préstamos que todavía debe, con su saldo y sus cuotas
     * vencidas.
     *
     * <h3>ES UN AVISO, NO UNA VALIDACIÓN</h3>
     * La pantalla lo llama al seleccionar al partícipe y lo muestra en el diálogo de
     * confirmación; el operador decide. <b>{@code POST /dvap/registrar} NO valida esto</b>,
     * no tiene ningún código de error asociado y registra la devolución igual aunque haya
     * deuda. Decisión del usuario del 2026-08-24 (§10.2 del plan), que descartó
     * explícitamente bloquear con un 422 y netear los préstamos contra los aportes.
     *
     * <p>Este endpoint <b>no toca ningún préstamo, cuota ni pago</b>: todo es lectura.</p>
     *
     * <p>Un partícipe sin préstamos vigentes devuelve 200 con {@code totalDeuda: 0},
     * {@code cantidadPrestamos: 0} y {@code prestamos: []}. <b>Nunca un error</b>, y nunca
     * una IncomeException por lista vacía.</p>
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @return 200 con { idEntidad, totalDeuda, cantidadPrestamos, tieneMora, prestamos }
     */
    @GET
    @Path("/deudaVigente/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deudaVigente(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO DEUDA VIGENTE DEL PARTICIPE - Entidad: " + idEntidad);

        if (idEntidad == null || idEntidad <= 0) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(),
                "Debe indicar una entidad válida", DevolucionAporteService.ERR_PARAMETRO_INVALIDO);
        }

        try {
            DeudaVigenteParticipe deuda = new DeudaVigenteParticipe();
            deuda.setIdEntidad(idEntidad);
            deuda.setTotalDeuda(0.0);
            deuda.setCantidadPrestamos(0);
            deuda.setTieneMora(Boolean.FALSE);

            // Corte del día: equivale al TRUNC(SYSDATE) del criterio de cuotas vencidas.
            LocalDateTime corte = LocalDate.now().atStartOfDay();

            List<Prestamo> prestamos = prestamoDaoService.selectVigentesByEntidad(idEntidad);
            if (prestamos == null) {
                prestamos = new ArrayList<>();
            }

            BigDecimal total = BigDecimal.ZERO;
            boolean tieneMora = false;

            for (Prestamo prestamo : prestamos) {

                DeudaPrestamo linea = new DeudaPrestamo();
                linea.setIdPrestamo(prestamo.getCodigo());
                linea.setIdAsoprep(prestamo.getIdAsoprep());
                linea.setProducto((prestamo.getProducto() != null)
                    ? prestamo.getProducto().getNombre() : null);
                // PRSTIDST, el estado operativo. Nunca ESPSCDGO.
                linea.setIdEstado(prestamo.getIdEstado());
                linea.setEstadoTexto(nombreEstadoPrestamo(prestamo.getIdEstado()));
                linea.setSaldoPendiente(0.0);
                linea.setCuotasVencidas(0);

                // Un préstamo con datos malos no puede romper el aviso completo: se lo
                // reporta igual, con lo que se pudo calcular, y se deja el rastro en el log.
                try {
                    linea.setSaldoPendiente(
                        motorPagoPrestamoService.calcularTotalPendientePrestamo(
                            prestamo.getCodigo()));
                } catch (Throwable e) {
                    System.err.println("No se pudo calcular el saldo pendiente del préstamo "
                        + prestamo.getCodigo() + ": " + e.getMessage());
                }

                try {
                    List<DetallePrestamo> vencidas =
                        detallePrestamoDaoService.selectCuotasVencidasByPrestamo(
                            prestamo.getCodigo(), corte);
                    linea.setCuotasVencidas((vencidas != null) ? vencidas.size() : 0);
                } catch (Throwable e) {
                    System.err.println("No se pudieron contar las cuotas vencidas del préstamo "
                        + prestamo.getCodigo() + ": " + e.getMessage());
                }

                if (linea.getSaldoPendiente() != null) {
                    total = total.add(BigDecimal.valueOf(linea.getSaldoPendiente()));
                }

                // 8 = DE_PLAZO_VENCIDO, 11 = EN_MORA
                int estado = (prestamo.getIdEstado() != null)
                    ? prestamo.getIdEstado().intValue() : 0;
                if (estado == EstadoPrestamo.DE_PLAZO_VENCIDO
                        || estado == EstadoPrestamo.EN_MORA
                        || linea.getCuotasVencidas() > 0) {
                    tieneMora = true;
                }

                deuda.getPrestamos().add(linea);
            }

            deuda.setTotalDeuda(total.setScale(2, RoundingMode.HALF_UP).doubleValue());
            deuda.setCantidadPrestamos(deuda.getPrestamos().size());
            deuda.setTieneMora(Boolean.valueOf(tieneMora));

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", "APLICACION");
            cuerpo.put("resultado", deuda);
            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return respuestaError("obtener la deuda vigente del partícipe", e);
        }
    }

    // ------------------------------------------------------------------------
    // Sobre de respuesta y mapeo de errores (mismo convenio que AporteRest, §8)
    // ------------------------------------------------------------------------

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    /** 409 CONFLICT */
    private static final int HTTP_CONFLICTO = Response.Status.CONFLICT.getStatusCode();

    private static final List<String> CODIGOS_400 = Arrays.asList(
        DevolucionAporteService.ERR_PARAMETRO_INVALIDO);

    private static final List<String> CODIGOS_404 = Arrays.asList(
        DevolucionAporteService.ERR_ENTIDAD_NO_ENCONTRADA,
        DevolucionAporteService.ERR_DEVOLUCION_NO_ENCONTRADA,
        DevolucionAporteService.ERR_CUENTA_NO_ENCONTRADA);

    private static final List<String> CODIGOS_409 = Arrays.asList(
        DevolucionAporteService.ERR_ESTADO_NO_PERMITE,
        DevolucionAporteService.ERR_DEVOLUCION_YA_PAGADA,
        DevolucionAporteService.ERR_DEVOLUCION_YA_ANULADA);

    /**
     * Traduce una excepción del servicio a la respuesta HTTP que le corresponde, leyendo el
     * código estable del prefijo del mensaje.
     * @param accion : Qué se estaba haciendo, para el mensaje genérico
     * @param e      : Excepción capturada
     * @return       : Respuesta con el sobre de error
     */
    private Response respuestaError(String accion, Throwable e) {
        System.err.println("ERROR al " + accion + ": " + e.getMessage());
        e.printStackTrace();

        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":")
            ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (CODIGOS_400.contains(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (CODIGOS_404.contains(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (CODIGOS_409.contains(codigo)) {
            status = HTTP_CONFLICTO;
        } else if (e instanceof com.saa.basico.util.IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return respuestaFallo(status, mensaje, codigo);
    }

    private Response respuestaFallo(int status, String mensaje, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("etapa", "VALIDACION");
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null && !codigo.isEmpty() ? codigo : mensaje);
        return Response.status(status)
                .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    // ------------------------------------------------------------------------
    // Armado del listado
    // ------------------------------------------------------------------------

    /**
     * Proyecta una devolución y sus detalles al resumen que consume la pantalla.
     * @param devolucion : Devolución ya reconciliada
     * @return           : Resumen listo para serializar
     * @throws Throwable : Excepcion
     */
    private ResumenDevolucionAporte armaResumen(DevolucionAporte devolucion) throws Throwable {

        ResumenDevolucionAporte resumen = new ResumenDevolucionAporte();
        resumen.setIdDevolucion(devolucion.getCodigo());
        resumen.setFecha(devolucion.getFecha());
        resumen.setValorTotal(devolucion.getValor());
        resumen.setEstado(devolucion.getEstado());
        resumen.setEstadoTexto(nombreEstado(devolucion.getEstado()));
        resumen.setIdPagoProgramado(devolucion.getIdPagoProgramado());
        resumen.setNumeroAsiento(devolucion.getNumeroAsiento());
        resumen.setFechaPago(devolucion.getFechaPago());
        resumen.setMotivo(devolucion.getMotivo());
        resumen.setCuentaDestino(describeCuenta(devolucion.getCuentaParticipe()));

        List<DetalleDevolucionAporte> detalles =
            detalleDevolucionAporteDaoService.selectByDevolucion(devolucion.getCodigo());
        if (detalles != null) {
            for (DetalleDevolucionAporte detalle : detalles) {
                DetalleResumenDevolucion linea = new DetalleResumenDevolucion();
                linea.setIdTipoAporte((detalle.getTipoAporte() != null)
                    ? detalle.getTipoAporte().getCodigo() : null);
                linea.setNombreTipoAporte((detalle.getTipoAporte() != null)
                    ? detalle.getTipoAporte().getNombre() : null);
                linea.setValor(detalle.getValor());
                resumen.getDetalle().add(linea);
            }
        }
        return resumen;
    }

    /**
     * Describe la cuenta destino para la pantalla, con el número enmascarado:
     * {@code "PICHINCHA · AHORROS · 2200****91"}.
     *
     * El nombre del tipo de cuenta sale del catálogo de rubros; si no se puede resolver se
     * usa el código crudo, porque una etiqueta faltante no debe romper el listado.
     * @param cuenta : Cuenta bancaria del partícipe; puede ser null
     * @return       : Descripción legible, o null si no hay cuenta
     */
    private String describeCuenta(CuentaBancariaParticipe cuenta) {
        if (cuenta == null) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        if (cuenta.getBancoExterno() != null && cuenta.getBancoExterno().getNombre() != null) {
            texto.append(cuenta.getBancoExterno().getNombre().trim());
        }
        String tipo = nombreTipoCuenta(cuenta.getTipoCuenta());
        if (tipo != null && !tipo.isEmpty()) {
            if (texto.length() > 0) {
                texto.append(" · ");
            }
            texto.append(tipo);
        }
        String enmascarado = enmascara(cuenta.getNumeroCuenta());
        if (enmascarado != null && !enmascarado.isEmpty()) {
            if (texto.length() > 0) {
                texto.append(" · ");
            }
            texto.append(enmascarado);
        }
        return texto.toString();
    }

    /**
     * Nombre del tipo de cuenta bancaria según el catálogo de rubros.
     * @param tipoCuenta : Codigo alterno del DetalleRubro
     * @return           : Descripción, o el código crudo si no se puede resolver
     */
    private String nombreTipoCuenta(Long tipoCuenta) {
        if (tipoCuenta == null) {
            return null;
        }
        try {
            String descripcion = detalleRubroDaoService.selectDescripcionByRubAltDetAlt(
                Rubros.TIPO_CUENTAS_BANCARIAS, tipoCuenta.intValue());
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                return descripcion.trim();
            }
        } catch (Throwable e) {
            System.err.println("No se pudo resolver el nombre del tipo de cuenta "
                + tipoCuenta + ": " + e.getMessage());
        }
        return String.valueOf(tipoCuenta);
    }

    /**
     * Enmascara el número de cuenta dejando visibles los primeros cuatro y los últimos dos
     * caracteres.
     * @param numero : Número de cuenta
     * @return       : Número enmascarado
     */
    private String enmascara(String numero) {
        if (numero == null) {
            return null;
        }
        String limpio = numero.trim();
        if (limpio.length() <= 6) {
            return limpio;
        }
        return limpio.substring(0, 4) + "****" + limpio.substring(limpio.length() - 2);
    }

    private String nombreEstado(Long estado) {
        if (estado == null) {
            return "SIN ESTADO";
        }
        switch (estado.intValue()) {
            case com.saa.rubros.EstadoDevolucionAporte.REGISTRADA: return "REGISTRADA";
            case com.saa.rubros.EstadoDevolucionAporte.EN_PAGO:    return "EN PAGO";
            case com.saa.rubros.EstadoDevolucionAporte.PAGADA:     return "PAGADA";
            case com.saa.rubros.EstadoDevolucionAporte.RECHAZADA:  return "RECHAZADA";
            case com.saa.rubros.EstadoDevolucionAporte.ANULADA:    return "ANULADA";
            default: return String.valueOf(estado);
        }
    }

    /**
     * Nombre legible del estado operativo de un préstamo (PRSTIDST), para el aviso de deuda.
     *
     * <p>Ojo con las etiquetas: <b>8 = DE_PLAZO_VENCIDO</b> y <b>11 = EN_MORA</b>. El JavaDoc
     * de {@code PrestamoDaoService.countVigentesMoraVencidosByEntidad} las tiene cruzadas —el
     * conjunto {2, 8, 11} que usa sí es correcto, las etiquetas no—. Acá se usan los nombres
     * del rubro {@link EstadoPrestamo}, que es la fuente buena.</p>
     *
     * @param idEstado : Valor de PRSTIDST
     * @return         : Nombre del estado
     */
    private String nombreEstadoPrestamo(Long idEstado) {
        if (idEstado == null) {
            return "SIN ESTADO";
        }
        switch (idEstado.intValue()) {
            case EstadoPrestamo.GENERADO:                return "GENERADO";
            case EstadoPrestamo.VIGENTE:                 return "VIGENTE";
            case EstadoPrestamo.CANCELADO:               return "CANCELADO";
            case EstadoPrestamo.CANCELADO_ANTICIPADO:    return "CANCELADO ANTICIPADO";
            case EstadoPrestamo.CANCELADO_POR_NOVACION:  return "CANCELADO POR NOVACION";
            case EstadoPrestamo.PENDIENTE_DE_APROBACION: return "PENDIENTE DE APROBACION";
            case EstadoPrestamo.RECHAZADO:               return "RECHAZADO";
            case EstadoPrestamo.DE_PLAZO_VENCIDO:        return "DE PLAZO VENCIDO";
            case EstadoPrestamo.CANCELADO_POR_REVISAR:   return "CANCELADO POR REVISAR";
            case EstadoPrestamo.VIGENTE_POR_REVISAR:     return "VIGENTE POR REVISAR";
            case EstadoPrestamo.EN_MORA:                 return "EN MORA";
            default: return String.valueOf(idEstado);
        }
    }

    private double nvlDouble(Double valor) {
        return (valor != null) ? valor : 0.0;
    }
}
