package com.saa.ejb.crd.service.dto;

/**
 * Cuerpo de {@code POST /rest/prst/simulacion/reporte} (PLAN-SIMULADORES-PRESTAMOS.md §7). El
 * backend RECALCULA con los parámetros de acá — nunca recibe las filas de la tabla — así el PDF
 * no puede diferir de lo que se mostró en pantalla ni ser manipulado desde el cliente.
 *
 * Trae anidado el cuerpo exacto del simulador correspondiente a {@code tipo}; los otros dos
 * quedan en null y se ignoran.
 */
public class SolicitudReporteSimulacion {

    /** "CREDITO_NUEVO" | "ABONO_CAPITAL" | "REESTRUCTURACION" */
    private String tipo;

    /** Cabecera del PDF. La calculadora no conoce entidades: el nombre lo manda el frontend. */
    private String nombreSocio;
    private String identificacionSocio;

    /** tipo = CREDITO_NUEVO */
    private ParametrosAmortizacion creditoNuevo;

    /** tipo = ABONO_CAPITAL — mismos parámetros que ya usa GET /simularAbonoCapital */
    private Long idPrestamo;
    private Double valorAbono;
    private Integer modalidadAbono;

    /** tipo = REESTRUCTURACION */
    private SolicitudReestructuracion reestructuracion;

    public SolicitudReporteSimulacion() {
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombreSocio() {
        return nombreSocio;
    }

    public void setNombreSocio(String nombreSocio) {
        this.nombreSocio = nombreSocio;
    }

    public String getIdentificacionSocio() {
        return identificacionSocio;
    }

    public void setIdentificacionSocio(String identificacionSocio) {
        this.identificacionSocio = identificacionSocio;
    }

    public ParametrosAmortizacion getCreditoNuevo() {
        return creditoNuevo;
    }

    public void setCreditoNuevo(ParametrosAmortizacion creditoNuevo) {
        this.creditoNuevo = creditoNuevo;
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValorAbono() {
        return valorAbono;
    }

    public void setValorAbono(Double valorAbono) {
        this.valorAbono = valorAbono;
    }

    public Integer getModalidadAbono() {
        return modalidadAbono;
    }

    public void setModalidadAbono(Integer modalidadAbono) {
        this.modalidadAbono = modalidadAbono;
    }

    public SolicitudReestructuracion getReestructuracion() {
        return reestructuracion;
    }

    public void setReestructuracion(SolicitudReestructuracion reestructuracion) {
        this.reestructuracion = reestructuracion;
    }
}
