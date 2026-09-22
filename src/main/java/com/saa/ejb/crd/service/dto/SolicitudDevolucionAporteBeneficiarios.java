package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Body de POST /rest/dvap/registrarParaBeneficiarios: devolución de los aportes de un
 * partícipe FALLECIDO, repartida entre sus beneficiarios activos (CRD.CBBP).
 *
 * Mismos campos que {@link SolicitudDevolucionAporte}, MENOS {@code idCuentaBancariaParticipe}
 * (no aplica: el destino sale de cada beneficiario, no de una cuenta del partícipe) y sin el
 * campo legado {@code idCuentaBancariaOrigen} (ya sin uso en el endpoint original).
 *
 * Ver docs/logica-negocio/crd/API-DEVOLUCION-APORTES-A-BENEFICIARIOS.md §4.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudDevolucionAporteBeneficiarios {

    /** Código de la entidad (partícipe fallecido) — ENTD.ENTDCDGO. Obligatorio. */
    private Long idEntidad;

    /** Empresa contable con la que se genera cada orden de pago. Obligatoria. */
    private Long idEmpresa;

    /** Id del usuario que registra, para la cabecera de cada pago en CXP. */
    private Long idUsuario;

    /** Nombre del usuario que registra, para la auditoría de CRD. Obligatorio. */
    private String usuario;

    /**
     * Fecha de negocio de la devolución (la misma para todas las que se generen). Si es null
     * se asume hoy. No puede ser futura.
     */
    private LocalDate fecha;

    /** Motivo u observación de la devolución (el mismo para todas las que se generen). */
    private String motivo;

    /** true si el banco ya debitó la cuenta por convenio: cada pago nace confirmado. */
    private boolean debitoAutomatico;

    /** Referencia del débito automático (nota de débito, convenio, etc.). */
    private String referencia;

    /**
     * Detalle por tipo de aporte, del TOTAL a devolver (antes de repartir entre
     * beneficiarios). Al menos una línea, sin tipos repetidos.
     */
    private List<DetalleSolicitudDevolucion> detalle;

    public SolicitudDevolucionAporteBeneficiarios() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public void setIdEmpresa(Long idEmpresa) {
        this.idEmpresa = idEmpresa;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isDebitoAutomatico() {
        return debitoAutomatico;
    }

    public void setDebitoAutomatico(boolean debitoAutomatico) {
        this.debitoAutomatico = debitoAutomatico;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public List<DetalleSolicitudDevolucion> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetalleSolicitudDevolucion> detalle) {
        this.detalle = detalle;
    }
}
