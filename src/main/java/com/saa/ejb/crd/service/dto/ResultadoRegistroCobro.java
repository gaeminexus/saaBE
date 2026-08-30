package com.saa.ejb.crd.service.dto;

/**
 * Resultado de {@link com.saa.ejb.crd.service.CobroCreditoService#registrarCobro}.
 */
public class ResultadoRegistroCobro {

    private Long idCobro;
    private Long estado;
    private Double valor;
    private Boolean contabilidadActiva;
    private Long idAsientoTransitorio;
    private String numeroAsientoTransitorio;
    private String mensaje;

    public Long getIdCobro() {
        return idCobro;
    }

    public void setIdCobro(Long idCobro) {
        this.idCobro = idCobro;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Boolean getContabilidadActiva() {
        return contabilidadActiva;
    }

    public void setContabilidadActiva(Boolean contabilidadActiva) {
        this.contabilidadActiva = contabilidadActiva;
    }

    public Long getIdAsientoTransitorio() {
        return idAsientoTransitorio;
    }

    public void setIdAsientoTransitorio(Long idAsientoTransitorio) {
        this.idAsientoTransitorio = idAsientoTransitorio;
    }

    public String getNumeroAsientoTransitorio() {
        return numeroAsientoTransitorio;
    }

    public void setNumeroAsientoTransitorio(String numeroAsientoTransitorio) {
        this.numeroAsientoTransitorio = numeroAsientoTransitorio;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
