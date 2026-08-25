package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ResultadoDevolucionAporte;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.crd.service.dto.SolicitudDevolucionAporte;
import com.saa.model.crd.DevolucionAporte;

import jakarta.ejb.Local;

/**
 * Devolución de dinero de los aportes de un partícipe.
 *
 * El registro genera los aportes NEGATIVOS de CRD.APRT y <b>dispara una orden de pago en
 * CXP</b>, donde se elige la cuenta bancaria de la que sale el dinero, se paga y se
 * contabiliza. Cuando el pago queda confirmado, la devolución se marca como PAGADA.
 *
 * <h3>Por qué CRD consulta en vez de que CXP avise</h3>
 * El sistema se comercializa después SIN el módulo {@code crd}, así que {@code cnt},
 * {@code tsr} y {@code cxp} NO pueden depender de {@code crd}: nada en CXP puede nombrar a
 * CRD, ni por callback, ni por interfaz invertida, ni por lookup. Por eso el aviso de
 * vuelta va al revés: <b>CRD lee el estado del PagoProgramado</b> y actualiza sus propios
 * campos. Tres disparadores, todos del lado de CRD:
 * <ol>
 *   <li>El GET del listado reconcilia antes de responder.</li>
 *   <li>{@code ProcesoDevolucionAporteTimer}, cada 30 minutos.</li>
 *   <li>El endpoint manual {@code POST /rest/dvap/sincronizar}, por si el timer no corrió.</li>
 * </ol>
 * La dirección permitida es {@code crd → cxp/tsr/cnt}, nunca al revés.
 *
 * <h3>Trampas del módulo que este servicio respeta</h3>
 * <ul>
 *   <li>Las filas de CRD.APRT se graban con {@code aporteDaoService.save(aporte, null)}
 *       DIRECTO, nunca con {@code AporteService.saveSingle}: eso forzaría {@code estado = 1}
 *       y la fila volvería a ser visible para el FIFO del proceso Petro
 *       ({@code selectMinAporteConSaldo}), que se la cobraría de nuevo al socio.
 *       Nacen con {@code saldo = 0.0}, {@code valorPagado = 0.0}, {@code estado = 4}.</li>
 *   <li>CRD.APRT es <b>append-only</b> para los reportes (G42, G43, G44, CJBM, CPRM/CCPM,
 *       dashboard, padrón). Un reverso NUNCA borra ni edita la fila negativa: inserta una
 *       positiva.</li>
 *   <li>El saldo de aportes se lee SIEMPRE por {@code SaldoAporteService}, que agrega
 *       {@code SUM(APRTVLRR)} en la base. Nunca se bajan filas a Java: CRD.APRT tiene
 *       ~980.000 registros y hay un OutOfMemoryError documentado por eso.</li>
 * </ul>
 *
 * @author Sistema SAA
 * @since 2026-08-24
 */
@Local
public interface DevolucionAporteService extends EntityService<DevolucionAporte> {

    // ========================================================================
    // Códigos de error de negocio (prefijo del mensaje de IncomeException)
    // ========================================================================

    /** 400 - Falta un parámetro obligatorio o viene malformado */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - La entidad (partícipe) no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 404 - La devolución no existe */
    String ERR_DEVOLUCION_NO_ENCONTRADA = "DEVOLUCION_NO_ENCONTRADA";
    /** 404 - La cuenta bancaria indicada no existe */
    String ERR_CUENTA_NO_ENCONTRADA = "CUENTA_NO_ENCONTRADA";
    /** 409 - El estado actual no permite la operación */
    String ERR_ESTADO_NO_PERMITE = "ESTADO_NO_PERMITE";
    /** 409 - La devolución ya fue pagada: hay que reversar el pago desde CxP */
    String ERR_DEVOLUCION_YA_PAGADA = "DEVOLUCION_YA_PAGADA";
    /** 409 - La devolución ya estaba anulada */
    String ERR_DEVOLUCION_YA_ANULADA = "DEVOLUCION_YA_ANULADA";
    /** 422 - El valor recibido no es válido */
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    /** 422 - La fecha recibida no es válida */
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";
    /** 422 - El saldo del tipo de aporte no alcanza para lo que se pide devolver */
    String ERR_SALDO_INSUFICIENTE = "SALDO_INSUFICIENTE";
    /** 422 - El tipo de aporte no existe o no está vigente */
    String ERR_TIPO_APORTE_NO_VIGENTE = "TIPO_APORTE_NO_VIGENTE";
    /** 422 - El tipo de aporte no tiene producto de pago parametrizado (TPAPPRDP) */
    String ERR_TIPO_APORTE_SIN_PRODUCTO = "TIPO_APORTE_SIN_PRODUCTO";
    /** 422 - El detalle repite un mismo tipo de aporte */
    String ERR_TIPO_DUPLICADO = "TIPO_DUPLICADO";
    /** 422 - El partícipe no tiene una cuenta bancaria utilizable */
    String ERR_SIN_CUENTA_BANCARIA = "SIN_CUENTA_BANCARIA";
    /** 422 - CXP no pudo generar o anular la orden de pago */
    String ERR_ERROR_ORDEN_PAGO = "ERROR_ORDEN_PAGO";

    /** Usuario con el que el temporizador registra la corrida automática */
    String USUARIO_PROCESO = "SAA_DEVOLUCION";

    /**
     * Registra la devolución, genera los aportes negativos y dispara la orden de pago en CXP.
     *
     * Todo ocurre en UNA transacción {@code REQUIRED}: si CXP no puede generar la orden de
     * pago, se revierten también los aportes negativos y no quedan huérfanos.
     *
     * Secuencia:
     * <ol>
     *   <li>Validar. Resolver el {@code TPAPPRDP} de cada tipo.</li>
     *   <li>Crear el DVAP en estado REGISTRADA(1) y una DDVA por tipo.</li>
     *   <li>Por cada tipo, revalidar el saldo y crear la fila NEGATIVA de CRD.APRT con su
     *       PagoAporte.</li>
     *   <li>Llamar a {@code PagoProgramadoService.registrarPagoDeOrigenExterno}.</li>
     *   <li>Guardar el id del pago y pasar el DVAP a EN_PAGO(2). Si el pago nació
     *       CONFIRMADO (débito automático), aplicar de una el paso a PAGADA(3).</li>
     * </ol>
     *
     * @param solicitud Partícipe, cuentas, empresa, fecha, motivo y detalle por tipo
     * @return Datos de la devolución creada, la orden de pago y el saldo por tipo resultante
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Ante cualquier fallo de validación (revierte todo)
     */
    ResultadoDevolucionAporte registrarDevolucion(SolicitudDevolucionAporte solicitud)
            throws Throwable;

    /**
     * Devoluciones de un partícipe, reconciliadas contra el estado real del pago antes de
     * responder: lo que ve el usuario siempre está al día.
     *
     * Una lista vacía NO es error: el partícipe simplemente no tiene devoluciones.
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @return Listado de devoluciones, de la más reciente a la más antigua
     * @throws Throwable Si ocurre un error
     */
    List<DevolucionAporte> listarPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Reconcilia contra PGS.PGTR todas las devoluciones en estado REGISTRADA(1) o
     * EN_PAGO(2) que ya tienen orden de pago.
     *
     * <b>Idempotente</b>: correrlo N veces da el mismo resultado. Una devolución ya en
     * PAGADA(3) o RECHAZADA(4) sale del universo y no se vuelve a tocar, y los
     * contra-movimientos no se repiten porque {@code DDVAAPRV} ya tiene valor.
     *
     * El orquestador corre en {@code NOT_SUPPORTED} y cada devolución en su propia
     * transacción {@code REQUIRES_NEW}: una devolución con datos malos no aborta el lote.
     * Es el patrón exacto de {@code ProcesoMoraPrestamoServiceImpl}.
     *
     * @return Resumen de la corrida
     * @throws Throwable Si ocurre un error
     */
    ResultadoSincronizacion sincronizarPagos() throws Throwable;

    /**
     * Reconcilia UNA devolución contra el estado de su orden de pago.
     *
     * Corre en {@code REQUIRES_NEW}: es el método que el lote invoca a través del proxy EJB
     * para que cada devolución commitee por separado. También lo usa
     * {@code listarPorEntidad} antes de responder.
     *
     * @param idDevolucion Código de la devolución (CRD.DVAP)
     * @return Resumen parcial con los contadores de esa devolución
     * @throws Throwable Si ocurre un error
     */
    ResultadoSincronizacion sincronizarDevolucion(Long idDevolucion) throws Throwable;

    /**
     * Anula una devolución que todavía no se pagó: genera los contra-movimientos positivos
     * de CRD.APRT (el saldo del partícipe vuelve a su valor previo) y anula la orden de
     * pago en CXP.
     *
     * Solo en estado REGISTRADA(1) o EN_PAGO(2) y con el pago NO confirmado. Si el pago ya
     * está confirmado hay que reversarlo primero desde Cuentas por Pagar.
     *
     * @param idDevolucion Código de la devolución
     * @param motivo       Motivo de la anulación, obligatorio
     * @param usuario      Usuario que anula
     * @return Datos de la devolución anulada y el saldo por tipo resultante
     * @throws Throwable Si ocurre un error
     */
    ResultadoDevolucionAporte anularDevolucion(Long idDevolucion, String motivo, String usuario)
            throws Throwable;
}
