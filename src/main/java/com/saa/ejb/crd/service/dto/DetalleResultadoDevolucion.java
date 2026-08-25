package com.saa.ejb.crd.service.dto;

/**
 * Una línea del resultado de registrar (o anular) una devolución de aportes.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class DetalleResultadoDevolucion {

    private Long idTipoAporte;

    private String nombreTipoAporte;

    /** Valor devuelto de ese tipo, en POSITIVO. */
    private Double valor;

    /** APRTCDGO de la fila NEGATIVA generada en CRD.APRT. */
    private Long idAporteGenerado;

    /** PGAPCDGO del PagoAporte generado. */
    private Long idPagoAporteGenerado;

    /**
     * APRTCDGO de la fila POSITIVA de contra-movimiento, si la devolución se anuló o el
     * pago se rechazó. Null mientras no ocurra.
     */
    private Long idAporteReverso;

    /** Saldo del tipo de aporte DESPUÉS de la operación, para refrescar la pantalla. */
    private Double saldoTipoAporteDespues;

    public DetalleResultadoDevolucion() {
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public String getNombreTipoAporte() {
        return nombreTipoAporte;
    }

    public void setNombreTipoAporte(String nombreTipoAporte) {
        this.nombreTipoAporte = nombreTipoAporte;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getIdAporteGenerado() {
        return idAporteGenerado;
    }

    public void setIdAporteGenerado(Long idAporteGenerado) {
        this.idAporteGenerado = idAporteGenerado;
    }

    public Long getIdPagoAporteGenerado() {
        return idPagoAporteGenerado;
    }

    public void setIdPagoAporteGenerado(Long idPagoAporteGenerado) {
        this.idPagoAporteGenerado = idPagoAporteGenerado;
    }

    public Long getIdAporteReverso() {
        return idAporteReverso;
    }

    public void setIdAporteReverso(Long idAporteReverso) {
        this.idAporteReverso = idAporteReverso;
    }

    public Double getSaldoTipoAporteDespues() {
        return saldoTipoAporteDespues;
    }

    public void setSaldoTipoAporteDespues(Double saldoTipoAporteDespues) {
        this.saldoTipoAporteDespues = saldoTipoAporteDespues;
    }
}
