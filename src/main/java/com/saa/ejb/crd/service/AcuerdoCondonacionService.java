package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.ResultadoAplicacionAcuerdo;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAcuerdo;
import com.saa.model.crd.AcuerdoCondonacion;

import jakarta.ejb.Local;

/**
 * Acuerdos de pago con condonación (Frente K). Ver
 * {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md}.
 *
 * ⚠️ REDISEÑADO el 2026-08-30 (K4/K10 derogadas): ya NO hay aprobación de condonación
 * separada. La previsualización en pantalla ({@link com.saa.ejb.crd.service.ProcesoPagoPrestamoService#calcularDesgloseConceptos})
 * cumple ese papel — el operador ve el desglose y confirma ahí mismo. Queda UNA sola
 * aprobación, la de contabilidad sobre el cobro en {@code CRD.CBCR}.
 *
 * Flujo: {@link #registrarAcuerdo} confirma el acuerdo Y registra su cobro en CBCR en el
 * MISMO acto → contabilidad aprueba el cobro → al procesarlo, {@code CobroCreditoService}
 * invoca {@link #aplicarAcuerdo} (K11: recién ahí se cancela el préstamo).
 *
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Local
public interface AcuerdoCondonacionService {

    /**
     * Confirma un acuerdo y registra su cobro en CBCR en el MISMO acto (paso 1 de §5 del
     * plan). Valida que el préstamo esté EN_MORA(11) o DE_PLAZO_VENCIDO(8) (K7), que vengan
     * exactamente los 5 conceptos sin repetir, que Desgravamen y Seguro de incendio nunca se
     * condonen (K3, se pagan al 100%), y que en cada concepto pagado + condonado cubra
     * exactamente lo adeudado (tolerancia $0.01) — el acuerdo liquida el préstamo en el acto
     * (K1), no deja remanente.
     *
     * NO afecta el préstamo ni genera ningún movimiento sobre él: eso ocurre recién al
     * procesar el cobro (K11, ver {@link #aplicarAcuerdo}). Sí crea el {@code CobroCredito}
     * (tipo {@code ACUERDO_CONDONACION}, valor = la parte no condonada) con el respaldo,
     * cuenta bancaria y referencia que trae la solicitud, y enlaza
     * {@code AcuerdoCondonacion.cobroCredito} desde el nacimiento — nunca queda una ventana
     * donde el acuerdo exista sin su cobro.
     *
     * ⚠️ INVARIANTE: {@code ACCNVLPG}/{@code ACCNVLCN} de la cabecera SIEMPRE son la suma del
     * detalle — nunca un dato de entrada independiente. No hay ningún camino donde la
     * cabecera diga una cosa y el detalle sume otra, porque la cabecera no se recibe por
     * separado. Cualquier código futuro que actualice un acuerdo tiene que sostener esto de
     * la misma forma: producir el detalle y derivar la cabecera de él, nunca dos
     * actualizaciones paralelas.
     *
     * @param solicitud  : Préstamo, fecha, observación, usuario, el detalle de los 5
     *                     conceptos, y los datos del cobro (cuenta bancaria, referencia,
     *                     respaldo obligatorio)
     * @return           : El acuerdo creado, en estado VIGENTE, con su {@code cobroCredito} ya enlazado
     * @throws Throwable : Si alguna validación falla (la del acuerdo o la del cobro en CBCR)
     */
    AcuerdoCondonacion registrarAcuerdo(SolicitudRegistroAcuerdo solicitud) throws Throwable;

    /**
     * Staleness al PROCESAR (§3 del plan, reubicado el 2026-08-30 tras derogarse K4). Solo
     * lectura — NO persiste nada, compara el desglose por concepto recalculado FRESCO contra
     * lo registrado (tolerancia $0.01). Lo llama {@code CobroCreditoService#procesarCobro}
     * ANTES de invocar {@link #aplicarAcuerdo}: si se llamara a {@code aplicarAcuerdo}
     * directamente y este lanzara por estar obsoleto, el contenedor ya habría marcado la
     * transacción rollback-only (misma razón que la precancelación en CBCR) y no se podría
     * grabar el rechazo del cobro después.
     *
     * @param idAcuerdo  : Código del acuerdo
     * @return           : {@code null} si el desglose sigue vigente; el motivo del rechazo
     *                     (para {@code ResultadoProcesoCobro.mensaje}) si no
     * @throws Throwable : Si el acuerdo no existe
     */
    String verificarVigencia(Long idAcuerdo) throws Throwable;

    /**
     * Aplica el acuerdo — pago + condonación + préstamo a CANCELADO (K11) — en una sola
     * operación. Invocado por {@code CobroCreditoService#procesarCobro} cuando
     * {@code CBCRTPOO = ACUERDO_CONDONACION} y el staleness (§3 del plan, reubicado al
     * PROCESO tras derogarse K4) ya confirmó que el desglose sigue vigente. Solo una vez: un
     * acuerdo con {@code eventoPrestamo} ya asignado no se vuelve a aplicar.
     *
     * Este método NO valida el estado de {@code ACCN} ni de su {@code CBCR} — esa
     * comprobación (que el cobro esté APROBADO) ya la hace {@code procesarCobro} antes de
     * llamar acá. Sí valida, defensivamente, que el acuerdo esté VIGENTE (no ANULADO) y sin
     * aplicar todavía.
     *
     * ⚠️ REGLA INNEGOCIABLE (§3 del plan): aplica con {@code acuerdo.getFecha()}, NUNCA
     * {@code LocalDate.now()} — es la MISMA fecha contra la que el staleness del PROCESO ya
     * comparó. Aplicar con otra fecha significaría cerrar el préstamo con números distintos
     * de los que se verificaron, sin que nada lo detecte.
     *
     * K9: lo condonado NUNCA se registra como {@code PagoPrestamo} — el único
     * {@code PagoPrestamo} que genera lleva exclusivamente los montos PAGADOS de los 5
     * conceptos, nunca los condonados. Es lo que hace que {@code anularOperacion} (ya
     * existente) revierta el acuerdo completo sin lógica nueva: al reversar un PagoPrestamo
     * que nunca tuvo el monto condonado, la deuda condonada simplemente no estaba ahí para
     * restaurar aparte — vuelve sola porque el préstamo deja de estar CANCELADO.
     *
     * Genera el asiento contable (plantilla alterno 25 + cuentas reales, D=H incluyendo la
     * línea de gasto) SOLO si {@code contabilidadActiva()} — hoy el flag está en 0, así que
     * este método no llega a esa parte. Si algún día se enciende el flag ANTES de que esa
     * parte esté terminada, este método debe fallar fuerte (no generar un asiento a medias),
     * no completarlo silenciosamente con una cuenta adivinada.
     *
     * @param idAcuerdo  : Código del acuerdo
     * @param usuario    : Usuario que procesa (el mismo que procesa el CBCR)
     * @return           : Resultado con el evento, el préstamo y su estado final
     * @throws Throwable : Si el acuerdo no existe, no está VIGENTE, o ya fue aplicado
     */
    ResultadoAplicacionAcuerdo aplicarAcuerdo(Long idAcuerdo, String usuario) throws Throwable;

    /**
     * Anula el acuerdo — se llama SOLO en cascada desde
     * {@code CobroCreditoService#anularCobro} cuando el cobro que se anula es de tipo
     * {@code ACUERDO_CONDONACION} (el depósito nunca llegó, o se corrigió por otra vía). NO
     * es una acción independiente que el usuario dispare sobre el acuerdo directamente — no
     * tiene REST propio.
     *
     * Solo válido desde VIGENTE (nunca desde APLICADO: ahí el reverso es
     * {@code anularOperacion}, un camino distinto que ya existe). El acuerdo CONSERVA su
     * registro (antes K10, ahora consecuencia de este mismo diseño): sigue siendo cierto que
     * alguien negoció perdonar dinero, aunque no se haya cobrado.
     *
     * ⚠️ {@code usuario}/{@code fecha}/{@code motivo} son OBLIGATORIOS — {@code CK_ACCN_MTRC}
     * (que antes exigía motivo para RECHAZADO=3 y ahora exige lo mismo para ANULADO=3, mismo
     * código, distinto significado) revienta con ORA-02290 si llegan nulos. El llamador
     * ({@code CobroCreditoService#anularCobro}) los copia del {@code CBCR} que se está
     * anulando (que ya los tiene obligatorios) — se DUPLICAN a propósito, no se resuelven por
     * join, porque K6 hace de {@code ACCN} la única fuente consultable de esta información y
     * una auditoría sobre acuerdos anulados no debería depender de acordarse de unir con CBCR.
     *
     * @param idAcuerdo  : Código del acuerdo
     * @param usuario    : Usuario que anuló el cobro (copiado de {@code CBCR.usuarioAnulacion})
     * @param fecha      : Fecha de la anulación (copiada de {@code CBCR.fechaAnulacion})
     * @param motivo     : Motivo de la anulación (copiado de {@code CBCR.motivoAnulacion})
     * @throws Throwable : Si el acuerdo no existe o no está VIGENTE
     */
    void anularAcuerdoPorCobro(Long idAcuerdo, String usuario, java.time.LocalDateTime fecha, String motivo)
            throws Throwable;
}
