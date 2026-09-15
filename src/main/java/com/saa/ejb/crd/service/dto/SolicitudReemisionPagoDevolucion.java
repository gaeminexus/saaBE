package com.saa.ejb.crd.service.dto;

/**
 * Body de POST /rest/dvap/{idDevolucion}/reemitirPago.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class SolicitudReemisionPagoDevolucion {

    /**
     * Cuenta bancaria NUEVA del partícipe — CNBP.CNBPCDGO. Obligatoria. Misma validación
     * que el paso 9 de {@code registrarDevolucion}: existe, es del partícipe de la
     * devolución y está ACTIVA.
     */
    private Long idCuentaBancariaParticipe;

    /** Motivo de la reemisión (por qué rebotó, o por qué se corrige la cuenta). Obligatorio. */
    private String motivo;

    /**
     * Obligatorio en {@code true} SOLO cuando la orden vigente está EN_ARCHIVO(2): el
     * archivo ya está en poder del banco, así que sin esta confirmación explícita anular y
     * reemitir podría pagar dos veces.
     */
    private Boolean confirmaRechazoBanco;

    /** Empresa contable con la que se genera la orden de pago nueva. Obligatoria. */
    private Long idEmpresa;

    /** Id del usuario que reemite, para la traza de la anulación de la orden vieja y el
     * registro de la nueva en CXP. */
    private Long idUsuario;

    /** Nombre del usuario que reemite, para la auditoría de CRD. Obligatorio. */
    private String usuario;

    public SolicitudReemisionPagoDevolucion() {
    }

    public Long getIdCuentaBancariaParticipe() {
        return idCuentaBancariaParticipe;
    }

    public void setIdCuentaBancariaParticipe(Long idCuentaBancariaParticipe) {
        this.idCuentaBancariaParticipe = idCuentaBancariaParticipe;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Boolean getConfirmaRechazoBanco() {
        return confirmaRechazoBanco;
    }

    public void setConfirmaRechazoBanco(Boolean confirmaRechazoBanco) {
        this.confirmaRechazoBanco = confirmaRechazoBanco;
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
}
