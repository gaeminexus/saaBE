package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Una línea del detalle de {@link SolicitudRegistroCobro}: un préstamo dentro del cobro
 * (o, en REGISTRO_APORTE, la única línea con {@code idPrestamo} nulo).
 */
public class DetalleRegistroCobroDTO {

    /** Préstamo afectado. Obligatorio salvo en REGISTRO_APORTE, donde debe venir nulo. */
    private Long idPrestamo;

    /** Monto de esta línea. Obligatorio, mayor a cero. */
    private Double valor;

    /** Modalidad del abono a capital (1 o 2). Solo para ABONO_CAPITAL. */
    private Long modalidad;

    /** Tipo de aporte. Solo para REGISTRO_APORTE. */
    private Long idTipoAporte;

    /** Período de devengo. Solo para REGISTRO_APORTE. */
    private LocalDate periodoDevengo;

    /** Observación de esta línea. */
    private String observacion;

    /** Acuerdo de pago con condonación. Solo para ACUERDO_CONDONACION (junto con idPrestamo). */
    private Long idAcuerdo;

    /**
     * Aportes que se CONSUMEN (saldo baja) para cubrir parte de la precancelación, junto con
     * {@code valor} (la parte de depósito). Solo para PRECANCELACION — en cualquier otro tipo
     * se rechaza si viene, para no confundirlo con las líneas de aporte de COBRO_MIXTO donde
     * el significado es el opuesto (el socio ENTREGA plata, su saldo SUBE). Mismo formato que
     * {@code SolicitudPrecancelacion.aportes}.
     */
    private List<DesgloseAporte> aportes;

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getModalidad() {
        return modalidad;
    }

    public void setModalidad(Long modalidad) {
        this.modalidad = modalidad;
    }

    public Long getIdTipoAporte() {
        return idTipoAporte;
    }

    public void setIdTipoAporte(Long idTipoAporte) {
        this.idTipoAporte = idTipoAporte;
    }

    public LocalDate getPeriodoDevengo() {
        return periodoDevengo;
    }

    public void setPeriodoDevengo(LocalDate periodoDevengo) {
        this.periodoDevengo = periodoDevengo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getIdAcuerdo() {
        return idAcuerdo;
    }

    public void setIdAcuerdo(Long idAcuerdo) {
        this.idAcuerdo = idAcuerdo;
    }

    public List<DesgloseAporte> getAportes() {
        return aportes;
    }

    public void setAportes(List<DesgloseAporte> aportes) {
        this.aportes = aportes;
    }
}
