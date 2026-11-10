package com.saa.model.crd;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Representa la tabla FRCN (FirmaContrato): trámite de firma electrónica del contrato de adhesión (sql/376). Solo uno vivo (estado 1 o 2) por contrato.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FRCN", schema = "CRD")
@SequenceGenerator(name = "SQ_FRCNCDGO", sequenceName = "CRD.SQ_FRCNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "FirmaContratoAll", query = "select e from FirmaContrato e"),
    @NamedQuery(name = "FirmaContratoId", query = "select e from FirmaContrato e where e.codigo = :id")
})
public class FirmaContrato implements Serializable {

    /** Código. */
    @Id
    @Basic
    @Column(name = "FRCNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_FRCNCDGO")
    private Long codigo;

    /** Contrato (CRD.CNTR). */
    @Basic
    @Column(name = "CNTRCDGO")
    private Long idContrato;

    /** Proveedor de firma (SECURITY_DATA). */
    @Basic
    @Column(name = "FRCNPRVD", length = 30)
    private String proveedor;

    /** Identificador del trámite en el proveedor. */
    @Basic
    @Column(name = "FRCNIDEX", length = 200)
    private String idExterno;

    /** Estado del trámite: ver {@link com.saa.rubros.EstadoFirmaContrato}. */
    @Basic
    @Column(name = "FRCNESTD")
    private Long estado;

    /** Adjunto (CRD.ADJN) con el PDF original enviado. */
    @Basic
    @Column(name = "FRCNADOR")
    private Long idAdjuntoOriginal;

    /** Adjunto (CRD.ADJN) con el PDF firmado. */
    @Basic
    @Column(name = "FRCNADFR")
    private Long idAdjuntoFirmado;

    /** SHA-256 (hex) del PDF enviado. */
    @Basic
    @Column(name = "FRCNHASH", length = 64)
    private String hash;

    /** Fecha de envío al proveedor. */
    @Basic
    @Column(name = "FRCNFCEN")
    private LocalDateTime fechaEnvio;

    /** Fecha en que quedó firmado por todos. */
    @Basic
    @Column(name = "FRCNFCFR")
    private LocalDateTime fechaFirma;

    /** Última consulta del estado al proveedor. */
    @Basic
    @Column(name = "FRCNFCUC")
    private LocalDateTime fechaUltimaConsulta;

    /** Último error del proveedor o del proceso. */
    @Basic
    @Column(name = "FRCNERRR", length = 2000)
    private String error;

    /** Fecha de registro. */
    @Basic
    @Column(name = "FRCNFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que envió a firmar. */
    @Basic
    @Column(name = "FRCNUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de anulación. */
    @Basic
    @Column(name = "FRCNFCAN")
    private LocalDateTime fechaAnulacion;

    /** Usuario que anuló. */
    @Basic
    @Column(name = "FRCNUSAN", length = 50)
    private String usuarioAnulacion;

    /** Motivo de la anulación. */
    @Basic
    @Column(name = "FRCNMTAN", length = 500)
    private String motivoAnulacion;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public Long getIdContrato() { return idContrato; }
    public void setIdContrato(Long idContrato) { this.idContrato = idContrato; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getIdExterno() { return idExterno; }
    public void setIdExterno(String idExterno) { this.idExterno = idExterno; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public Long getIdAdjuntoOriginal() { return idAdjuntoOriginal; }
    public void setIdAdjuntoOriginal(Long idAdjuntoOriginal) { this.idAdjuntoOriginal = idAdjuntoOriginal; }
    public Long getIdAdjuntoFirmado() { return idAdjuntoFirmado; }
    public void setIdAdjuntoFirmado(Long idAdjuntoFirmado) { this.idAdjuntoFirmado = idAdjuntoFirmado; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public LocalDateTime getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(LocalDateTime fechaFirma) { this.fechaFirma = fechaFirma; }
    public LocalDateTime getFechaUltimaConsulta() { return fechaUltimaConsulta; }
    public void setFechaUltimaConsulta(LocalDateTime fechaUltimaConsulta) { this.fechaUltimaConsulta = fechaUltimaConsulta; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }
    public String getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
