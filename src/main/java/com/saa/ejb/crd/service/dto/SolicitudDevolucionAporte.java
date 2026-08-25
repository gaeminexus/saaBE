package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Body de POST /rest/dvap/registrar: devolución de dinero de los aportes de un partícipe.
 *
 * El registro genera los aportes NEGATIVOS de CRD.APRT y dispara una orden de pago en CXP.
 * El saldo del partícipe baja EN EL MOMENTO DEL REGISTRO, antes de que el dinero salga del
 * banco; si el pago se rechaza, el reconciliador genera los contra-movimientos positivos.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudDevolucionAporte {

    /** Código de la entidad (partícipe) — ENTD.ENTDCDGO. Obligatorio. */
    private Long idEntidad;

    /**
     * Cuenta bancaria del partícipe a la que se transfiere — CNBP.CNBPCDGO.
     * Obligatoria salvo débito automático. Debe pertenecer al partícipe y estar activa.
     */
    private Long idCuentaBancariaParticipe;

    /** Cuenta bancaria propia de la que sale el dinero — TSR.CNBC. Obligatoria. */
    private Long idCuentaBancariaOrigen;

    /** Empresa contable con la que se genera la orden de pago. Obligatoria. */
    private Long idEmpresa;

    /** Id del usuario que registra, para la cabecera del pago en CXP. */
    private Long idUsuario;

    /** Nombre del usuario que registra, para la auditoría de CRD. Obligatorio. */
    private String usuario;

    /**
     * Fecha de negocio de la devolución. Si es null se asume hoy. No puede ser futura.
     *
     * Viaja como {@code yyyy-MM-dd}: es un {@code LocalDate}. Nunca un Date de JavaScript
     * ni nada terminado en Z — Jackson descarta el offset en vez de convertirlo.
     */
    private LocalDate fecha;

    /** Motivo u observación de la devolución. */
    private String motivo;

    /** true si el banco ya debitó la cuenta por convenio: el pago nace confirmado. */
    private boolean debitoAutomatico;

    /** Referencia del débito automático (nota de débito, convenio, etc.). */
    private String referencia;

    /** Detalle por tipo de aporte. Al menos una línea, sin tipos repetidos. */
    private List<DetalleSolicitudDevolucion> detalle;

    public SolicitudDevolucionAporte() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Long getIdCuentaBancariaParticipe() {
        return idCuentaBancariaParticipe;
    }

    public void setIdCuentaBancariaParticipe(Long idCuentaBancariaParticipe) {
        this.idCuentaBancariaParticipe = idCuentaBancariaParticipe;
    }

    public Long getIdCuentaBancariaOrigen() {
        return idCuentaBancariaOrigen;
    }

    public void setIdCuentaBancariaOrigen(Long idCuentaBancariaOrigen) {
        this.idCuentaBancariaOrigen = idCuentaBancariaOrigen;
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
