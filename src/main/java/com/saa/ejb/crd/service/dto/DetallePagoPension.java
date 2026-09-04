package com.saa.ejb.crd.service.dto;

/**
 * Un renglón del detalle de {@link ResultadoGeneracionPagosPension} — un jubilado evaluado en
 * la corrida de {@code PagoPensionComplementariaService#generarPagosDelMes}.
 *
 * Ver API-PAGO-PENSION-COMPLEMENTARIA.md §1 y PLAN-PAGO-JUBILADOS.md §3/§4.
 */
public class DetallePagoPension {

    private Long idEntidad;
    private String nombre;
    private Long idPago;
    private Double valorPension;
    private Double valorSeguroSalud;

    /** Cuánto de la pensión del mes se cruzó contra deuda de préstamo vigente (0 si no aplica). */
    private Double valorCruzadoAPrestamo;

    /** Cuánto salió como orden de pago hacia tesorería (puede ser 0 si el cruce se llevó todo). */
    private Double valorOrdenPago;

    /**
     * {@code false} con {@code valorCruzadoAPrestamo > 0} NO es un error: es el caso en que la
     * deuda se llevó toda la pensión del mes. El pago existe, se contabilizó, y no hubo salida
     * de dinero (PLAN-PAGO-JUBILADOS.md §3, punto ⛔).
     */
    private boolean generoOrdenPago;

    /** Asiento de devengo generado en CRD (D cuenta del jubilado, H por pagar) — null si la
     * contabilidad de CRD está inactiva. */
    private Long idAsientoDevengo;

    /**
     * "GENERADO" | "YA_EXISTIA" | "ERROR" | "SIN_ANCLA" | "AL_DIA" (PLAN-PAGO-RETROACTIVO-
     * JUBILADOS.md): las tres últimas son finales NORMALES de un jubilado con préstamo, nunca
     * "ERROR" — el operador necesita distinguir "terminó bien" de "se rompió".
     */
    private String estado;

    private String mensaje;

    /**
     * Cuántos períodos (PGPC) NUEVOS generó esta llamada — 1 en el circuito normal sin
     * préstamo, 0..N en el retroactivo. Campo nuevo y opcional: el frontend actual sigue
     * funcionando sin leerlo.
     */
    private int mesesAplicados;

    /**
     * Cuál de las tres condiciones de corte terminó el bucle retroactivo:
     * "MES_CORRIDA_ALCANZADO" | "PRESTAMO_AL_DIA" | "SALDO_AGOTADO". {@code null} fuera del
     * circuito retroactivo. Campo nuevo y opcional.
     */
    private String motivoCorte;

    /**
     * "COMPLETA" | "SOLO_CRUCE" | "BLOQUEADO" — API-PAGO-PENSION-COMPLEMENTARIA.md §6.
     * {@code COMPLETA}: hubo cruce (si tenía préstamo) y no quedó remanente retenido —
     * incluye tanto "salió dinero al banco" como "el cruce absorbió el 100%, no había nada
     * que sacar". {@code SOLO_CRUCE}: tiene préstamo, canceló deuda, pero quedó un remanente
     * que no se pudo entregar (sin cuenta o sin certificado). {@code BLOQUEADO}: no participó
     * en absoluto — sin préstamo y sin certificado (no hay cruce posible y no puede salir
     * dinero), o cualquier otro motivo que impidió generar el pago ({@code mensaje} dice cuál).
     * {@code null} en YA_EXISTIA/AL_DIA: no es un evento de participación nuevo de esta corrida.
     */
    private String participacion;

    public DetallePagoPension() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public Double getValorPension() {
        return valorPension;
    }

    public void setValorPension(Double valorPension) {
        this.valorPension = valorPension;
    }

    public Double getValorSeguroSalud() {
        return valorSeguroSalud;
    }

    public void setValorSeguroSalud(Double valorSeguroSalud) {
        this.valorSeguroSalud = valorSeguroSalud;
    }

    public Double getValorCruzadoAPrestamo() {
        return valorCruzadoAPrestamo;
    }

    public void setValorCruzadoAPrestamo(Double valorCruzadoAPrestamo) {
        this.valorCruzadoAPrestamo = valorCruzadoAPrestamo;
    }

    public Double getValorOrdenPago() {
        return valorOrdenPago;
    }

    public void setValorOrdenPago(Double valorOrdenPago) {
        this.valorOrdenPago = valorOrdenPago;
    }

    public boolean isGeneroOrdenPago() {
        return generoOrdenPago;
    }

    public void setGeneroOrdenPago(boolean generoOrdenPago) {
        this.generoOrdenPago = generoOrdenPago;
    }

    public Long getIdAsientoDevengo() {
        return idAsientoDevengo;
    }

    public void setIdAsientoDevengo(Long idAsientoDevengo) {
        this.idAsientoDevengo = idAsientoDevengo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getMesesAplicados() {
        return mesesAplicados;
    }

    public void setMesesAplicados(int mesesAplicados) {
        this.mesesAplicados = mesesAplicados;
    }

    public String getMotivoCorte() {
        return motivoCorte;
    }

    public void setMotivoCorte(String motivoCorte) {
        this.motivoCorte = motivoCorte;
    }

    public String getParticipacion() {
        return participacion;
    }

    public void setParticipacion(String participacion) {
        this.participacion = participacion;
    }
}
