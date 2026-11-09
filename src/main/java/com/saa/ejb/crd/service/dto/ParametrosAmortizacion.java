package com.saa.ejb.crd.service.dto;

import java.time.LocalDateTime;

/**
 * Entrada de {@code CalculadoraAmortizacionService.calcular}. Escalares puros: a propósito NO
 * recibe una entidad {@code Prestamo}, para poder simular una tabla de amortización sin que el
 * préstamo exista todavía (PLAN-SIMULADORES-PRESTAMOS.md §6).
 */
public class ParametrosAmortizacion {

    private Double monto;
    private Double tasaAnual;
    private Integer plazo;

    /** 1 = Francesa, 2 = Alemana (com.saa.rubros.TipoAmortizacion, aún literal en el resto del código). */
    private Long tipoAmortizacion;

    private LocalDateTime fechaInicio;
    private Boolean tieneCuotaCero;

    /**
     * Valor FIJO de desgravamen por cuota. Se usa tal cual (sin recalcular) SOLO si
     * {@link #calcularDesgravamenSobreSaldo} es {@code false}/{@code null} — no participa en
     * el cálculo de capital/interés.
     */
    private Double desgravamenPorCuota;

    /**
     * Valor FIJO de seguro de incendio por cuota. Se aplica tal cual a toda cuota que NO tenga
     * una entrada en {@link #seguroPorNumeroCuota} para su propio número (o si ese mapa viene
     * vacío/null) — no participa en el cálculo de capital/interés.
     */
    private Double seguroIncendioPorCuota;

    /**
     * Segunda ola, pedido 2 (2026-08-27); ampliado 2026-08-29: si es {@code true}, el desgravamen
     * de CADA cuota se calcula como {@code saldoDeCapitalDeLaCuota * 0.9 / 1000} y
     * {@link #desgravamenPorCuota} se ignora — incluida la cuota 0 de gracia, si la hay
     * ({@code CalculadoraAmortizacionServiceImpl.agregarCuotaCero} respeta el mismo flag).
     * {@code false}/{@code null} (el valor por defecto) conserva el comportamiento viejo:
     * {@code desgravamenPorCuota} se aplica tal cual a todas las cuotas.
     *
     * <p>Lo encienden {@code SimulacionPrestamoServiceImpl.simularCreditoNuevo} (desde
     * 2026-08-27) Y {@code simularReestructuracion} (desde 2026-08-29, corrección: antes seguía
     * con el valor fijo, sin cambios). <b>Sigue sin tocarlo</b> el generador real de préstamos
     * ({@code PrestamoServiceImpl}), que continúa con el valor fijo que trae el request —
     * decisión de negocio pendiente, no un olvido.</p>
     */
    private Boolean calcularDesgravamenSobreSaldo;

    /**
     * Seguro de incendio de cada cuota HISTORIZADA que se está re-amortizando, indexado por el
     * NÚMERO DE CUOTA NUEVO que le corresponde (no el número viejo/histórico — este motor
     * siempre numera sus filas regulares 1..plazo desde cero, sin conocer la numeración de
     * origen; quien arma este mapa es responsable de traducir la correspondencia). Opcional:
     * {@code null} o sin entrada para una cuota puntual usa {@link #seguroIncendioPorCuota}
     * como respaldo fijo para esa fila. Usado por {@code simularReestructuracion} (2026-08-29)
     * para preservar el seguro de cada cuota vieja en su cuota nueva correspondiente por
     * POSICIÓN, en vez de aplicar un solo valor (el de la última pendiente) a toda la tabla —
     * mismo criterio que {@code AbonoCapitalPrestamoServiceImpl}, que resuelve la equivalencia
     * en su propio motor de re-amortización porque ahí sí coincide con el número viejo. La
     * cuota 0 de gracia NUNCA se busca en este mapa: no tiene número regular ni correspondencia
     * histórica, siempre usa {@link #seguroIncendioPorCuota}.
     */
    private java.util.Map<Long, Double> seguroPorNumeroCuota;

    /**
     * DISENO-PRIMERA-CUOTA-SEGUN-ARCHIVO-PETRO.md: si es {@code true}, la cuota 1 vence a fin del MES DE INICIO con el
     * interés solo de los días reales de ese mes (el archivo Petro de ese mes aún no se generó), y el resto de cuotas se
     * corre un mes antes. Con cuota 0, la cuota 0 vence a fin del mes de inicio y la 1 al mes siguiente. Si el inicio es
     * el último día del mes (0 días) se ignora y rige la regla de siempre. {@code null}/{@code false} = comportamiento de
     * siempre (cuota 1 a fin del mes siguiente). Quien llama lo decide con {@code PrimeraCuotaPetroService}.
     */
    private Boolean primeraCuotaEnMesInicio;

    /**
     * Opcional, solo para los simuladores: partícipe (ENTD) para el que se simula. Si viene,
     * {@code simularCreditoNuevo} decide {@link #primeraCuotaEnMesInicio} según el archivo Petro de su filial; si no,
     * rige la regla de siempre.
     */
    private Long idEntidad;

    public ParametrosAmortizacion() {
    }

    public Boolean getPrimeraCuotaEnMesInicio() {
        return primeraCuotaEnMesInicio;
    }

    public void setPrimeraCuotaEnMesInicio(Boolean primeraCuotaEnMesInicio) {
        this.primeraCuotaEnMesInicio = primeraCuotaEnMesInicio;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Double getTasaAnual() {
        return tasaAnual;
    }

    public void setTasaAnual(Double tasaAnual) {
        this.tasaAnual = tasaAnual;
    }

    public Integer getPlazo() {
        return plazo;
    }

    public void setPlazo(Integer plazo) {
        this.plazo = plazo;
    }

    public Long getTipoAmortizacion() {
        return tipoAmortizacion;
    }

    public void setTipoAmortizacion(Long tipoAmortizacion) {
        this.tipoAmortizacion = tipoAmortizacion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Boolean getTieneCuotaCero() {
        return tieneCuotaCero;
    }

    public void setTieneCuotaCero(Boolean tieneCuotaCero) {
        this.tieneCuotaCero = tieneCuotaCero;
    }

    public Double getDesgravamenPorCuota() {
        return desgravamenPorCuota;
    }

    public void setDesgravamenPorCuota(Double desgravamenPorCuota) {
        this.desgravamenPorCuota = desgravamenPorCuota;
    }

    public Double getSeguroIncendioPorCuota() {
        return seguroIncendioPorCuota;
    }

    public void setSeguroIncendioPorCuota(Double seguroIncendioPorCuota) {
        this.seguroIncendioPorCuota = seguroIncendioPorCuota;
    }

    public Boolean getCalcularDesgravamenSobreSaldo() {
        return calcularDesgravamenSobreSaldo;
    }

    public void setCalcularDesgravamenSobreSaldo(Boolean calcularDesgravamenSobreSaldo) {
        this.calcularDesgravamenSobreSaldo = calcularDesgravamenSobreSaldo;
    }

    public java.util.Map<Long, Double> getSeguroPorNumeroCuota() {
        return seguroPorNumeroCuota;
    }

    public void setSeguroPorNumeroCuota(java.util.Map<Long, Double> seguroPorNumeroCuota) {
        this.seguroPorNumeroCuota = seguroPorNumeroCuota;
    }
}
