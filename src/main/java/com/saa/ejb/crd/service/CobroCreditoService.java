package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.FilaBandejaAprobacion;
import com.saa.ejb.crd.service.dto.ResultadoProcesoCobro;
import com.saa.ejb.crd.service.dto.ResultadoRegistroCobro;
import com.saa.ejb.crd.service.dto.SolicitudEdicionCobro;
import com.saa.ejb.crd.service.dto.SolicitudRegistroCobro;
import com.saa.model.crd.CobroCredito;

import jakarta.ejb.Local;

/**
 * Autorización de contabilidad para cobros individuales (CRD.CBCR/DCBC). Tres pasos,
 * mismo comportamiento que la carga Petro:
 *
 * <pre>
 *   1. REGISTRO (crédito)     — este servicio. Guarda la INTENCIÓN con su respaldo
 *                                digitalizado y arma el asiento TRANSITORIO. No toca
 *                                ni un préstamo ni un aporte.
 *   2. APROBACIÓN (contabilidad) — fase 3.
 *   3. PROCESO (crédito)      — fase 4. Reconstruye la Solicitud correspondiente y
 *                                llama al método del motor de pago que ya existe.
 * </pre>
 *
 * Ver {@code docs/logica-negocio/crd/sql/DDL-COBROS-APROBACION-CONTABILIDAD.sql}.
 *
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Local
public interface CobroCreditoService {

    /**
     * Registra un cobro pendiente de aprobación de contabilidad (paso 1).
     *
     * Valida respaldo obligatorio, cuenta bancaria obligatoria, montos mayores a cero
     * (total y cada línea, y que cuadren entre sí), que la entidad exista, que el tipo
     * de operación sea uno de {@link com.saa.rubros.CrdTipoOperacionCobro}, y que todos
     * los préstamos del detalle sean de la entidad que paga.
     *
     * Con la contabilidad de CRD activa (rubro 237) arma el asiento transitorio:
     * D cuenta contable de la cuenta bancaria elegida → H 2.3.01.15.01, plantilla
     * alterno 19 (la misma de Petro). Con la contabilidad apagada el cobro se registra
     * igual, sin asiento — mismo criterio que el resto de CRD.
     *
     * NO se toca ningún préstamo ni aporte: eso ocurre recién en el PROCESO (fase 4).
     *
     * @param solicitud  : Datos del cobro y su detalle por préstamo
     * @return           : El cobro creado, en estado REGISTRADO
     * @throws Throwable : Si alguna validación falla, o hay un error al generar el asiento
     */
    ResultadoRegistroCobro registrarCobro(SolicitudRegistroCobro solicitud) throws Throwable;

    /**
     * Aprueba un cobro registrado (paso 2, lado contabilidad). Solo válido desde
     * {@link com.saa.rubros.CrdEstadoCobro#REGISTRADO}. No genera asiento ni toca préstamos
     * ni aportes: eso ocurre en el PROCESO (fase 4).
     *
     * @param idCobro    : Código del cobro
     * @param usuario    : Usuario de contabilidad que aprueba
     * @return           : El cobro actualizado, en estado APROBADO
     * @throws Throwable : Si el cobro no existe o no está en estado REGISTRADO
     */
    CobroCredito aprobarCobro(Long idCobro, String usuario) throws Throwable;

    /**
     * Rechaza un cobro con motivo obligatorio. Válido desde REGISTRADO o desde APROBADO
     * (mientras no esté PROCESADO) — la comprobación del estado ocurre dentro de la misma
     * transacción que el cambio, para que no haya carrera con un PROCESO simultáneo.
     *
     * @param idCobro    : Código del cobro
     * @param usuario    : Usuario de contabilidad que rechaza
     * @param motivo     : Motivo del rechazo. Obligatorio; sobreescribe el motivo de un
     *                     rechazo anterior si lo hubiera (no hay historial de rechazos
     *                     sucesivos, es deliberado — ver el DDL).
     * @return           : El cobro actualizado, en estado RECHAZADO
     * @throws Throwable : Si el cobro no existe, el motivo viene vacío, o el estado no
     *                     admite el rechazo (ya PROCESADO, o ya RECHAZADO)
     */
    CobroCredito rechazarCobro(Long idCobro, String usuario, String motivo) throws Throwable;

    /**
     * Reenvía un cobro RECHAZADO de vuelta a REGISTRADO. Es el MISMO registro (no crea uno
     * nuevo): la huella del último rechazo (usuario/fecha/motivo) queda visible hasta el
     * próximo rechazo, que la sobreescribe.
     *
     * ⚠️ RETIRADO 2026-08-29 en favor de {@link #editarYReenviarCobro} — se fusionaron en un
     * solo método porque los cuatro motivos reales de rechazo (respaldo que no sirve, monto
     * que no coincide, cuenta equivocada, depósito que nunca llegó) O requieren corregir algo,
     * O requieren anular ({@link #anularCobro}); un reenvío que solo cambia el estado sin
     * corregir nada no tiene caso de uso real y el cobro solo rebotaría de nuevo.
     */

    /**
     * Corrige un cobro RECHAZADO y lo reenvía a REGISTRADO en el mismo acto — DECISIÓN: se
     * unificó editar + reenviar en un solo método (no dos), porque un cobro corregido pero
     * todavía RECHAZADO no tiene ningún sentido de negocio como estado intermedio visible: la
     * corrección solo importa si va a volver a la bandeja.
     *
     * Solo desde RECHAZADO — nunca sobre REGISTRADO (esperando aprobación), APROBADO ni
     * PROCESADO. Se puede corregir TODO: cuenta bancaria, referencia, respaldo, valor y el
     * detalle completo (se reemplaza entero: las líneas viejas nunca tuvieron
     * EventoPrestamo/PagoAporte porque el cobro nunca se procesó, así que no hay nada que
     * preservar). La entidad y el tipo de operación NO cambian.
     *
     * Si el valor o la cuenta bancaria cambian, el asiento transitorio se REHACE: se anula el
     * anterior con motivo (queda su rastro en CNT.ASNT) y se genera uno nuevo, que reemplaza a
     * {@code CBCRASN1}. Si solo cambian referencia, observación o respaldo, el asiento
     * existente NO se toca. Todo en una sola transacción — si falla la regeneración del
     * asiento, no queda un cobro editado con el asiento viejo.
     *
     * @param idCobro    : Código del cobro
     * @param usuario    : Usuario de crédito que corrige y reenvía
     * @param correccion : Datos corregidos (todos los campos que trae SolicitudRegistroCobro
     *                     salvo entidad y tipo de operación)
     * @return           : El cobro actualizado, en estado REGISTRADO
     * @throws Throwable : Si el cobro no existe, no está en estado RECHAZADO, o la corrección
     *                     no pasa las mismas validaciones que un registro nuevo
     */
    CobroCredito editarYReenviarCobro(Long idCobro, String usuario, SolicitudEdicionCobro correccion)
            throws Throwable;

    /**
     * Anula un cobro porque el depósito NUNCA llegó al banco — no es un caso de "corregir y
     * reenviar": no hubo cobro. Reversa el asiento transitorio si existía (con motivo, vía
     * {@code AsientoService.anulaAsiento}), porque el DEBE al banco nunca debió registrarse.
     *
     * Válido desde REGISTRADO, APROBADO o RECHAZADO. Nunca desde PROCESADO: ahí ya se
     * afectaron préstamos y aportes, y deshacerlo es {@code anularOperacion}, un camino
     * distinto que ya existe.
     *
     * Lo ejecuta CRÉDITO (decisión del usuario 2026-08-29): contabilidad detecta el problema
     * y lo dice por escrito rechazando con el motivo correspondiente; crédito, al ver que no
     * hay nada que corregir, anula. El control cruzado no se pierde porque crédito no anula
     * por su cuenta — anula porque contabilidad se lo señaló, y ese motivo queda registrado.
     *
     * @param idCobro    : Código del cobro
     * @param usuario    : Usuario de crédito que anula
     * @param motivo     : Motivo de la anulación. Obligatorio.
     * @return           : El cobro actualizado, en estado ANULADO
     * @throws Throwable : Si el cobro no existe, el motivo viene vacío, o el estado es
     *                     PROCESADO (o ya ANULADO)
     */
    CobroCredito anularCobro(Long idCobro, String usuario, String motivo) throws Throwable;

    /**
     * Bandeja combinada de aprobación de contabilidad: cobros individuales REGISTRADOS +
     * cargas Petro pendientes del paso 1 (confirmación de recepción), sin modelo común.
     * Es de solo lectura — cada fila trae su tipo para que la pantalla despache al endpoint
     * correcto de cada uno (este mismo REST para cobros, {@code CobroPetroContableService}
     * para cargas Petro).
     *
     * @return           : Filas ordenadas por fecha de registro ascendente (FIFO); VACÍA
     *                     si no hay nada pendiente
     * @throws Throwable : Excepcion
     */
    List<FilaBandejaAprobacion> bandejaAprobacion() throws Throwable;

    /**
     * PROCESO (paso 3): reconstruye la Solicitud correspondiente desde CRD.CBCR/DCBC según
     * {@link com.saa.rubros.CrdTipoOperacionCobro} y llama al método del motor de pago que ya
     * existe — {@code pagarCuota}, {@code pagarMultiplesCuotas}, el abono a capital, la
     * precancelación o el registro manual de aportes — exactamente como se llama hoy. Recién
     * acá se afectan préstamos y aportes.
     *
     * Solo válido desde {@link com.saa.rubros.CrdEstadoCobro#APROBADO}, con la comprobación de
     * estado dentro de la misma transacción que el cambio.
     *
     * PRECANCELACION es la única excepción con manejo especial: si el monto registrado ya no
     * coincide con el valor de precancelación recalculado al momento de procesar (el préstamo
     * cambió entre el registro y el proceso), el cobro se RECHAZA automáticamente con un
     * motivo generado por el sistema, en vez de fallar — no se llama al motor. Las otras 4
     * operaciones se reproducen tal cual y el motor valida solo: si el motor lanza, el proceso
     * entero se revierte (todo o nada) y el cobro queda como estaba (APROBADO), nunca a medias.
     *
     * NOTA: este método aún NO genera el asiento definitivo (CBCRASN2) — ver la pregunta
     * pendiente al árbitro sobre la clasificación contable antes de construirlo.
     *
     * @param idCobro    : Código del cobro
     * @param usuario    : Usuario de crédito que procesa
     * @return           : Resultado con el estado final (PROCESADO, o RECHAZADO si hubo
     *                     rechazo automático por staleness de precancelación)
     * @throws Throwable : Si el cobro no existe, no está en estado APROBADO, o el motor de
     *                     pago rechaza la operación (todo o nada: nada queda aplicado)
     */
    ResultadoProcesoCobro procesarCobro(Long idCobro, String usuario) throws Throwable;
}
