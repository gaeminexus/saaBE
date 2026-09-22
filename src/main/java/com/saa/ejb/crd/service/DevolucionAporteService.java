package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ResultadoConsultaPagoDevolucion;
import com.saa.ejb.crd.service.dto.ResultadoDevolucionAporte;
import com.saa.ejb.crd.service.dto.ResultadoDevolucionBeneficiario;
import com.saa.ejb.crd.service.dto.ResultadoReemisionPagoDevolucion;
import com.saa.ejb.crd.service.dto.ResultadoSincronizacion;
import com.saa.ejb.crd.service.dto.SolicitudDevolucionAporte;
import com.saa.ejb.crd.service.dto.SolicitudDevolucionAporteBeneficiarios;
import com.saa.ejb.crd.service.dto.SolicitudReemisionPagoDevolucion;
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
 *       DIRECTO, nunca con {@code AporteServiceImpl.saveSingle}: eso forzaría
 *       {@code estado = 1} (Estado.ACTIVO) en todo INSERT, pisando el PAGADA(4) recién
 *       asignado. Nacen con {@code saldo = 0.0}, {@code valorPagado = 0.0}, {@code estado = 4}:
 *       en el modelo vigente (Fase 1 del plan de devengo de aportes, D1) toda fila nace
 *       pagada por construcción.</li>
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
    /** 409 - La orden vigente ya fue confirmada por tesorería; hay que reversarla primero */
    String ERR_PAGO_CONFIRMADO = "PAGO_CONFIRMADO";
    /** 409 - La orden vigente está en un archivo enviado al banco: falta confirmar el rechazo */
    String ERR_CONFIRMAR_RECHAZO_BANCO = "CONFIRMAR_RECHAZO_BANCO";
    /** 400 - El partícipe no está fallecido: use el endpoint normal (registrarDevolucion) */
    String ERR_PARTICIPE_NO_FALLECIDO = "PARTICIPE_NO_FALLECIDO";
    /** 400 - El partícipe fallecido no tiene beneficiarios activos cargados en CRD.CBBP */
    String ERR_SIN_BENEFICIARIOS = "SIN_BENEFICIARIOS";
    /** 400 - Los porcentajes de los beneficiarios activos no suman exactamente 100 */
    String ERR_PORCENTAJES_NO_SUMAN_100 = "PORCENTAJES_NO_SUMAN_100";
    /**
     * 400 - El partícipe está fallecido y tiene beneficiarios activos: registrarDevolucion lo
     * rechaza y remite a registrarParaBeneficiarios (guarda agregada 2026-09-22).
     */
    String ERR_USAR_DEVOLUCION_BENEFICIARIOS = "USAR_DEVOLUCION_BENEFICIARIOS";

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
     * <b>Idempotente</b>: correrlo N veces da el mismo resultado. Una devolución que pasa a
     * PAGADA(3) sale del universo y no se vuelve a tocar. Desde el contrato de reemisión de
     * pago (2026-09-15, §4), una cuya orden queda RECHAZADA o ANULADA en tesorería <b>ya NO</b>
     * se marca RECHAZADA ni genera contra-movimientos: sigue EN_PAGO, se cuenta en
     * {@code pendientesReemision} en cada corrida, y queda a la espera de que el operador
     * reemita el pago ({@link #reemitirPagoDevolucion}) o anule la devolución
     * ({@link #anularDevolucion}) explícitamente.
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
     * Anula una devolución: genera los contra-movimientos positivos de CRD.APRT (el saldo
     * del partícipe vuelve a su valor previo) y, si la orden de pago sigue viva
     * (POR_APROBAR(0) o REGISTRADO(1)), la anula también en CXP.
     *
     * Estados que lo permiten: REGISTRADA(1) o EN_PAGO(2), siempre; y, desde el contrato de
     * reemisión de pago (2026-09-15, §5.2), PAGADA(3) cuando su orden de pago ya quedó
     * RECHAZADA(4) o ANULADA(5) en tesorería — es la reversión completa explícita. Una
     * PAGADA con la orden todavía CONFIRMADA(3), o sin ninguna orden en ese estado, sigue
     * rechazándose igual que siempre: hay que reversar el pago desde Cuentas por Pagar
     * primero. Una orden EN_ARCHIVO(2) tampoco se anula: hay que procesar la respuesta del
     * banco antes.
     *
     * @param idDevolucion Código de la devolución
     * @param motivo       Motivo de la anulación, obligatorio
     * @param usuario      Usuario que anula
     * @return Datos de la devolución anulada y el saldo por tipo resultante
     * @throws Throwable Si ocurre un error
     */
    ResultadoDevolucionAporte anularDevolucion(Long idDevolucion, String motivo, String usuario)
            throws Throwable;

    /**
     * Reemite el pago de una devolución cuya transferencia rebotó: anula (o reconoce ya
     * anulada/rechazada) la orden de pago vigente y genera una orden nueva con la cuenta
     * bancaria correcta, SIN tocar el aporte negativo ni el asiento de reclasificación.
     *
     * Todo ocurre en UNA transacción {@code REQUIRED}. Ver
     * {@code docs/logica-negocio/crd/API-REEMITIR-PAGO-DEVOLUCION.md} §3.2 para el orden
     * exacto de las doce reglas de negocio, y §3.3 para cómo se arma la orden nueva (copia
     * del valor y el desglose de la anterior, cuenta y beneficiario nuevos).
     *
     * @param idDevolucion Código de la devolución
     * @param solicitud    Cuenta bancaria nueva, motivo, confirmación de rechazo del banco
     *                     (obligatoria solo si la orden actual está EN_ARCHIVO), empresa y
     *                     usuario
     * @return Ids de la orden anterior y de la nueva, y el resultado de la devolución
     * @throws Throwable Si ocurre un error
     */
    ResultadoReemisionPagoDevolucion reemitirPagoDevolucion(Long idDevolucion,
            SolicitudReemisionPagoDevolucion solicitud) throws Throwable;

    /**
     * Consulta BAJO DEMANDA si el pago de una devolución ya se confirmó en Cuentas por Pagar,
     * y si es así copia {@code fechaRespuesta}/{@code referenciaBanco} a los {@code PagoAporte}
     * de esa devolución (uno por tipo de aporte; todos llevan la misma fecha y referencia).
     *
     * ⚠️ Es DISTINTO de {@link #sincronizarDevolucion}: no toca {@code DevolucionAporte.estado}
     * ni genera contra-movimientos — eso ya lo hacen los otros disparadores (el GET del
     * listado, y el timer si algún día se reactiva). Este método SOLO alimenta las dos
     * columnas de {@code PagoAporte} para la pantalla, y por eso funciona igual sin importar
     * en qué estado esté la devolución: sirve tanto para el backlog de devoluciones que ya
     * están PAGADA pero nunca recibieron el backfill, como para una consulta anticipada de
     * una que todavía no se confirmó.
     *
     * Idempotente: se puede llamar tantas veces como haga falta, re-copia los mismos valores.
     *
     * @param idDevolucion Código de la devolución
     * @return {@code confirmado = false} con un mensaje NO es un error: significa que
     *         tesorería todavía no confirmó el pago
     * @throws Throwable Si la devolución no existe
     */
    ResultadoConsultaPagoDevolucion consultarPagoDevolucion(Long idDevolucion) throws Throwable;

    /**
     * A qué devolución pertenece un aporte negativo — para que el diálogo del "ojo" pueda
     * llamar a {@link #consultarPagoDevolucion} sin que el frontend arme la relación por su
     * cuenta.
     *
     * ⚠️ NO es un simple {@code DDVA where DDVAAPRT = idAporte}: esa columna solo referencia
     * la PRIMERA fila cuando el reparto por período de devengo generó varias (ver el
     * comentario de {@code crearFilaNegativaDevolucion}). Este método prueba esa vía directa
     * primero y, si no encuentra nada, cae a buscar entre las demás filas que generó el mismo
     * registro (mismo entidad+tipo+instante+tipoMovimiento=DEVOLUCION).
     *
     * @param idAporte : Código del aporte (CRD.APRT)
     * @return         : Código de la devolución, o {@code null} si el aporte no es de tipo
     *                   DEVOLUCION o no se pudo enlazar (dato inconsistente)
     * @throws Throwable : Si el aporte no existe
     */
    Long obtenerIdDevolucionPorAporte(Long idAporte) throws Throwable;

    /**
     * Nombre legible de un estado de {@code PagoProgramado} (PGTRESTD) — "POR APROBAR",
     * "REGISTRADO", "EN ARCHIVO", "CONFIRMADO", "RECHAZADO", "ANULADO", o "SIN ESTADO" si
     * {@code estado} es {@code null}.
     * <p>
     * Expuesto (§6 del contrato de reemisión de pago) para que
     * {@code DevolucionAporteRest.armaResumen} arme {@code ResumenDevolucionAporte
     * .estadoPagoTexto} con el MISMO catálogo que usa este servicio internamente, sin
     * duplicar el switch. Es un catálogo DISTINTO del de {@code DevolucionAporte.estado}
     * ({@link com.saa.rubros.EstadoDevolucionAporte}) — nunca confundir los dos.
     * @param estado : PGTRESTD, o {@code null}
     * @return       : Nombre del estado
     */
    String nombreEstadoPago(Long estado);

    /**
     * Registra la devolución de aportes de un partícipe FALLECIDO, repartida entre sus
     * beneficiarios activos (CRD.CBBP): UNA devolución completa por beneficiario, cada una con
     * su propio DVAP, orden y pago. Ver
     * {@code docs/logica-negocio/crd/API-DEVOLUCION-APORTES-A-BENEFICIARIOS.md} §3: CXP rechaza
     * un segundo pago vivo para el mismo {@code idOrigen}, así que no puede ser N órdenes de una
     * sola devolución.
     *
     * <p>Todo ocurre en UNA transacción {@code REQUIRED}: si la devolución de cualquier
     * beneficiario falla, se revierten TODAS — repartir la plata de un fallecido a medias entre
     * su familia es peor que no repartir nada (§6 del contrato, H78).
     *
     * <p>El reparto es <b>POR TIPO DE APORTE, no por el total</b> (§5.1): cada línea de tipo se
     * reparte entre los beneficiarios por su porcentaje, con el residuo de ESE tipo al de mayor
     * porcentaje (empate → menor código). El total de cada beneficiario sale de sumar sus
     * líneas ya repartidas, nunca al revés — repartir primero el total y escalar las líneas
     * después descuadra el tipo (§5.2).
     *
     * @param solicitud Partícipe, empresa, fecha, motivo y detalle por tipo — igual que
     *                  {@link #registrarDevolucion}, sin cuenta bancaria (el destino sale de
     *                  cada beneficiario)
     * @return Una entrada por beneficiario activo, con su devolución, su valor repartido y su
     *         orden de pago
     * @throws Throwable                          Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Si el partícipe no está fallecido
     *                  ({@link #ERR_PARTICIPE_NO_FALLECIDO}), no tiene beneficiarios activos
     *                  ({@link #ERR_SIN_BENEFICIARIOS}), sus porcentajes no suman 100
     *                  ({@link #ERR_PORCENTAJES_NO_SUMAN_100}), o cualquier fallo de la
     *                  validación normal (revierte todas las devoluciones ya generadas en esta
     *                  transacción)
     */
    List<ResultadoDevolucionBeneficiario> registrarParaBeneficiarios(
            SolicitudDevolucionAporteBeneficiarios solicitud) throws Throwable;
}
