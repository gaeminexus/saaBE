package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.ejb.crd.service.dto.ResultadoAnulacion;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoPagoConAportes;
import com.saa.ejb.crd.service.dto.ResultadoPrecancelacion;
import com.saa.ejb.crd.service.dto.SimulacionPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudAnulacion;
import com.saa.ejb.crd.service.dto.SolicitudPagoConAportes;
import com.saa.ejb.crd.service.dto.SolicitudPagoCuota;
import com.saa.ejb.crd.service.dto.SolicitudPrecancelacion;
import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Orquestador de los procesos de pago de préstamos (§7 de
 * ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md).
 *
 * Cada método es UNA transacción REQUIRED: crea su {@code EventoPrestamo} al inicio, delega la
 * aplicación en {@link MotorPagoPrestamoService}, deja huella en la observación del préstamo y
 * llama al hook contable al final. Cualquier {@code IncomeException} revierte todo.
 *
 * Los errores de negocio se lanzan como {@code IncomeException} con el mensaje prefijado por un
 * CÓDIGO ({@code CODIGO: descripción}); la capa REST mapea ese código al status HTTP (§8).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface ProcesoPagoPrestamoService {

    // ========================================================================
    // Tipos de operación (deben coincidir con el CHECK CK_EVPR_TIPO de CRD.EVPR)
    // ========================================================================

    String TIPO_PAGO_MANUAL = "PAGO_MANUAL";
    String TIPO_PAGO_APORTES = "PAGO_APORTES";
    String TIPO_ABONO_CAPITAL = "ABONO_CAPITAL";
    String TIPO_PRECANCELACION = "PRECANCELACION";

    // ========================================================================
    // Códigos de error de negocio (prefijo del mensaje de IncomeException)
    // ========================================================================

    /** 400 - Falta un parámetro obligatorio o viene malformado */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - El préstamo no existe */
    String ERR_PRESTAMO_NO_ENCONTRADO = "PRESTAMO_NO_ENCONTRADO";
    /** 404 - El evento no existe */
    String ERR_EVENTO_NO_ENCONTRADO = "EVENTO_NO_ENCONTRADO";
    /** 409 - El estado del préstamo no permite la operación */
    String ERR_ESTADO_NO_PERMITE = "ESTADO_NO_PERMITE";
    /** 409 - El evento ya fue anulado */
    String ERR_EVENTO_YA_ANULADO = "EVENTO_YA_ANULADO";
    /** 409 - Existen operaciones posteriores vigentes: el reverso es LIFO */
    String ERR_EVENTO_POSTERIOR_VIGENTE = "EVENTO_POSTERIOR_VIGENTE";
    /** 409 - Hay pagos sobre la tabla recalculada por el abono que se quiere reversar */
    String ERR_PAGOS_SOBRE_TABLA_RECALCULADA = "PAGOS_SOBRE_TABLA_RECALCULADA";
    /** 422 - El valor recibido no es válido */
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    /** 422 - La fecha recibida no es válida */
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";
    /** 422 - El valor supera la deuda total del préstamo */
    String ERR_VALOR_EXCEDE_DEUDA = "VALOR_EXCEDE_DEUDA";
    /** 422 - El préstamo no tiene cuotas pendientes con saldo */
    String ERR_SIN_CUOTAS_PENDIENTES = "SIN_CUOTAS_PENDIENTES";
    /** 422 - El desglose de aportes es inválido (vacío, con valores no positivos o con tipos repetidos) */
    String ERR_DESGLOSE_INVALIDO = "DESGLOSE_INVALIDO";
    /** 422 - El tipo de aporte no existe o no está vigente */
    String ERR_TIPO_APORTE_NO_VIGENTE = "TIPO_APORTE_NO_VIGENTE";
    /** 422 - El saldo de aportes del partícipe no alcanza */
    String ERR_SALDO_APORTES_INSUFICIENTE = "SALDO_APORTES_INSUFICIENTE";
    /** 422 - El préstamo no tiene cuotas futuras que precancelar */
    String ERR_SIN_CUOTAS_FUTURAS = "SIN_CUOTAS_FUTURAS";
    /** 422 - El valor enviado no coincide con el valor de precancelación calculado */
    String ERR_MONTO_NO_COINCIDE = "MONTO_NO_COINCIDE";

    // ========================================================================
    // Procesos
    // ========================================================================

    /**
     * Pago manual de cuota(s) con valor: parcial, exacto o con excedente en cascada (§7.1).
     *
     * Si el valor cubre toda la deuda, el préstamo queda CANCELADO(3) en el mismo llamado:
     * pagar la deuda completa NO es precancelación, no condona nada.
     *
     * @param solicitud Préstamo, valor, usuario, observación y fecha de pago
     * @return Resultado con el desglose por cuota y el estado final del préstamo
     * @throws Throwable Si ocurre un error
     */
    ResultadoAplicacionPago pagarCuota(SolicitudPagoCuota solicitud) throws Throwable;

    /**
     * Pago de cuota(s) consumiendo el saldo de aportes del partícipe (§7.4).
     *
     * Cada tipo del desglose genera una fila NEGATIVA en CRD.APRT (invisible para el FIFO del
     * proceso Petro: {@code saldo = 0} y estado 4) y su PagoAporte enlazado al PagoPrestamo.
     *
     * @param solicitud Préstamo, desglose por tipo de aporte, usuario, observación y fecha
     * @return Resultado de la aplicación + los movimientos de aporte generados
     * @throws Throwable Si ocurre un error
     */
    ResultadoPagoConAportes pagarConAportes(SolicitudPagoConAportes solicitud) throws Throwable;

    /**
     * Calcula cuánto cuesta precancelar el préstamo a una fecha, SIN escribir nada (§7.5).
     *
     * Se cobra la deuda EXIGIBLE (cuotas pendientes con vencimiento hasta la fecha, con su mora)
     * más SOLO el capital pendiente de las cuotas futuras; intereses, desgravamen y seguros
     * futuros se condonan.
     *
     * @param idPrestamo Código del préstamo
     * @param fecha      Fecha de corte; si es null se usa hoy
     * @return Detalle del cálculo con el valor total de precancelación
     * @throws Throwable Si ocurre un error
     */
    SimulacionPrecancelacion simularPrecancelacion(Long idPrestamo, LocalDate fecha) throws Throwable;

    /**
     * Ejecuta la precancelación total del préstamo con efectivo, aportes o ambos (§7.5).
     *
     * El backend SIEMPRE re-verifica el monto: {@code |valorEnviado − valorTotal| <= 0.01}.
     *
     * @param solicitud Préstamo, valor en efectivo, desglose de aportes, usuario, obs y fecha
     * @return Resultado con el detalle de lo pagado y el estado final del préstamo (4)
     * @throws Throwable Si ocurre un error
     */
    ResultadoPrecancelacion precancelar(SolicitudPrecancelacion solicitud) throws Throwable;

    /**
     * Anula (reversa) un EventoPrestamo completo, para los 4 tipos de operación (§7.6).
     *
     * El reverso es LIFO: no se puede anular un evento si existen operaciones posteriores
     * vigentes sobre el mismo préstamo.
     *
     * @param solicitud Evento, usuario y motivo
     * @return Resultado con los conteos de lo revertido y el estado final del préstamo
     * @throws Throwable Si ocurre un error
     */
    ResultadoAnulacion anularOperacion(SolicitudAnulacion solicitud) throws Throwable;

    /**
     * Deja huella de una operación en {@code Prestamo.observacion} y sella
     * {@code fechaModificacion}. Lo usan todos los procesos, incluido el abono a capital, que
     * vive en {@link AbonoCapitalPrestamoService}.
     *
     * @param prestamo      Préstamo a marcar (se persiste dentro del método)
     * @param tipoOperacion PAGO_MANUAL | PAGO_APORTES | ABONO_CAPITAL | PRECANCELACION
     * @param valor         Monto de la operación
     * @param observacion   Observación del usuario (puede ser null)
     * @param fecha         Fecha/hora de negocio de la operación
     * @param usuario       Usuario que la ejecutó
     * @throws Throwable Si ocurre un error
     */
    void registrarHuellaPrestamo(Prestamo prestamo, String tipoOperacion, double valor,
            String observacion, LocalDateTime fecha, String usuario) throws Throwable;
}
