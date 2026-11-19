package com.saa.ejb.crd.service;

import java.time.LocalDateTime;

import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.EventoPrestamo;
import com.saa.model.crd.HistDetallePrestamo;

/**
 * C35 (cuadre Superintendencia, 18-nov-2026): regla de {@code DTPRSLOT} ("saldo otros", el capital abonado o
 * precancelado que la cuota registra).
 *
 * <p><b>Regla:</b> el {@code DTPRSLOT} de una cuota es lo que respaldan sus pagos: la suma de los {@code PGPRSLOT} de
 * sus {@code PagoPrestamo} vigentes (no anulados), de cualquier tipo. Un residuo {@code DTPRSLOT - respaldado > 0,01}
 * es capital "abonado" sin ningún pago detrás. El caso real: el script de producción del 12-ago-2026
 * ({@code docs/logica-negocio/crd/ACTUALIZACION-SALDOOTROS-CANCELADOS-ANTICIPADOS.md}) copió {@code DTPRSLCP} a
 * {@code DTPRSLOT} sin PGPR en 611 préstamos cancelados anticipadamente; el re-proceso los reabrió sin limpiar el campo
 * y la precancelación o el abono volvieron a sumar encima (préstamo 4818, operación 68891: 61.498,46 en la cuota 52
 * con un saldo de 30.749,23). Contabilidad, cartera 1.3 y G48 leen los PGPR, no este campo; el defecto está en la
 * tabla, en la pantalla, en la app móvil y en el control del abono sin regenerar (C16b).</p>
 *
 * <p><b>Solo se baja, nunca se sube.</b> Si {@code DTPRSLOT} es MENOR que lo respaldado no se toca: los pagos "DEP" de
 * la migración vieja usan {@code PGPRSLOT} para otra cosa (ver {@code PagoPrestamoDaoServiceImpl}), y llevar el campo
 * a esa suma fabricaría un abono que no existe.</p>
 *
 * <p><b>Rastro.</b> La cuota tal como estaba se copia a {@code CRD.HDTP} con un motivo {@link #PREFIJO_MOTIVO}* y
 * <b>sin evento</b> ({@code EVPRCDGO} nulo, DDL C35-01). Sin evento a propósito: la anulación del abono, su asiento
 * (re-bandeo) y la cartera al corte ({@code ModoCorteCartera#abonosPosterioresSql}) leen {@code HDTP} por evento, y una
 * fila de este ajuste atada al evento del abono se restauraría como cuota o cambiaría la G48.</p>
 *
 * <p>Ver {@code cuadreSuper/03-trabajo/cambios-saa/C35-ABONO-CRUCE-CUOTAS-EN-MORA.md}.</p>
 */
public final class SaldoOtrosRespaldo {

    /** Tolerancia del residuo (un centavo, la misma de sql/06 del diagnóstico C35). */
    public static final double TOLERANCIA = 0.01;

    /** Error fuera de re-proceso: la cuota tiene {@code DTPRSLOT} sin pagos que lo respalden. */
    public static final String ERR_SALDO_OTROS_SIN_RESPALDO = "SALDO_OTROS_SIN_RESPALDO";

    /** Prefijo común de los motivos de {@code HDTPMTVO} de este ajuste (para filtrarlos). */
    public static final String PREFIJO_MOTIVO = "SLOT_SIN_RESPALDO";

    /** Ajuste hecho por la reapertura de un préstamo terminal vigente al corte (D-019). */
    public static final String MOTIVO_REAPERTURA = "SLOT_SIN_RESPALDO_REAPERTURA";

    /** Ajuste hecho por {@code recalcularCuotaDesdePagos} (reversos, reproceso de carga, reapertura). */
    public static final String MOTIVO_RECALCULO = "SLOT_SIN_RESPALDO_RECALCULO";

    /** Ajuste hecho por la precancelación en re-proceso, antes de registrar el capital futuro. */
    public static final String MOTIVO_PRECANCELACION = "SLOT_SIN_RESPALDO_PRECANCEL";

    /** Ajuste hecho por el abono a capital en re-proceso, antes de acumular el abono. */
    public static final String MOTIVO_ABONO = "SLOT_SIN_RESPALDO_ABONO";

    /** Ajuste hecho por el script revisable C35-02 sobre la base de trabajo (mismo motivo en el SQL). */
    public static final String MOTIVO_SCRIPT = "SLOT_SIN_RESPALDO_C35";

    /** Usuario de {@code HDTPUSAR} cuando el llamador no tiene uno (columna NOT NULL, 50). */
    public static final String USUARIO_MOTOR = "SAA_C35";

    private SaldoOtrosRespaldo() {
    }

    /** Residuo sin respaldo, redondeado a centavos; positivo = {@code DTPRSLOT} de más. */
    public static double residuo(Double saldoOtros, double respaldado) {
        return Math.round(((saldoOtros != null ? saldoOtros : 0.0) - respaldado) * 100.0) / 100.0;
    }

    /** {@code true} si el residuo es positivo y supera la tolerancia (lo único que se corrige). */
    public static boolean tieneResiduo(Double saldoOtros, double respaldado) {
        return residuo(saldoOtros, respaldado) > TOLERANCIA;
    }

    /** Texto corto para {@code HDTPUSAR} (NOT NULL, 50). */
    public static String usuarioHistorico(String usuario) {
        String u = usuario == null || usuario.trim().isEmpty() ? USUARIO_MOTOR : usuario.trim();
        return u.length() <= 50 ? u : u.substring(0, 50);
    }

    /**
     * Copia campo a campo una cuota de DTPR a su espejo en HDTP. {@code evento} puede ser nulo solo para los
     * motivos {@link #PREFIJO_MOTIVO}* (requiere el DDL C35-01).
     */
    public static HistDetallePrestamo copiarAHistorico(DetallePrestamo cuota, EventoPrestamo evento, String motivo,
            String usuario) {
        HistDetallePrestamo h = new HistDetallePrestamo();
        h.setCodigoOriginal(cuota.getCodigo());
        h.setEventoPrestamo(evento);
        h.setMotivo(motivo);
        h.setFechaRegistroHist(LocalDateTime.now());
        h.setUsuarioHist(usuario);

        h.setPrestamo(cuota.getPrestamo());
        h.setNumeroCuota(cuota.getNumeroCuota());
        h.setFechaVencimiento(cuota.getFechaVencimiento());
        h.setCapital(cuota.getCapital());
        h.setInteres(cuota.getInteres());
        h.setMora(cuota.getMora());
        h.setInteresVencido(cuota.getInteresVencido());
        h.setSaldoCapital(cuota.getSaldoCapital());
        h.setSaldoInteres(cuota.getSaldoInteres());
        h.setSaldoMora(cuota.getSaldoMora());
        h.setSaldoInteresVencido(cuota.getSaldoInteresVencido());
        h.setFechaPagado(cuota.getFechaPagado());
        h.setAbono(cuota.getAbono());
        h.setCapitalPagado(cuota.getCapitalPagado());
        h.setInteresPagado(cuota.getInteresPagado());
        h.setDesgravamen(cuota.getDesgravamen());
        h.setCuota(cuota.getCuota());
        h.setSaldo(cuota.getSaldo());
        h.setSaldoOtros(cuota.getSaldoOtros());
        h.setDesgravamenFirmado(cuota.getDesgravamenFirmado());
        h.setDesgravamenDiferido(cuota.getDesgravamenDiferido());
        h.setDesgravamenOriginal(cuota.getDesgravamenOriginal());
        h.setValorDiferido(cuota.getValorDiferido());
        h.setTotal(cuota.getTotal());
        h.setMoraPagado(cuota.getMoraPagado());
        h.setDesgravamenPagado(cuota.getDesgravamenPagado());
        h.setInteresVendidoPagado(cuota.getInteresVendidoPagado());
        h.setMoraCalculada(cuota.getMoraCalculada());
        h.setDiasMora(cuota.getDiasMora());
        h.setEstado(cuota.getEstado());
        h.setFechaRegistro(cuota.getFechaRegistro());
        h.setUsuarioRegistro(cuota.getUsuarioRegistro());
        h.setIdEstado(cuota.getIdEstado());
        h.setCodigoExterno(cuota.getCodigoExterno());
        h.setOtrosSeguros(cuota.getOtrosSeguros());
        h.setTotalConSeguro(cuota.getTotalConSeguro());
        h.setValorSeguroIncendio(cuota.getValorSeguroIncendio());
        h.setSaldoInicialCapital(cuota.getSaldoInicialCapital());
        return h;
    }
}
