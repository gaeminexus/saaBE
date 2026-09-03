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

    /** "GENERADO" | "YA_EXISTIA" | "ERROR" */
    private String estado;

    private String mensaje;

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
}
