package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.USAP (UsuarioApp): credencial de acceso a la app móvil ASOPREP,
 * una fila por partícipe (CRD.ENTD) habilitado para usar la app. El login es la
 * identificación (cédula), nunca el código de Entidad.
 *
 * @see com.saa.rubros.EstadoUsuarioApp
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "USAP", schema = "CRD")
@SequenceGenerator(name = "SQ_USAPCDGO", sequenceName = "CRD.SQ_USAPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "UsuarioAppAll", query = "select e from UsuarioApp e"),
    @NamedQuery(name = "UsuarioAppId", query = "select e from UsuarioApp e where e.codigo = :id")
})
public class UsuarioApp implements Serializable {

    /** Código. PK. */
    @Id
    @Basic
    @Column(name = "USAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_USAPCDGO")
    private Long codigo;

    /** FK - Entidad (partícipe) dueña de la credencial. Relación 1:1 (UNIQUE en BD). */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Identificación (cédula/RUC/pasaporte) - usuario de login de la app. */
    @Basic
    @Column(name = "USAPIDNT", length = 20)
    private String identificacion;

    /** Hash de clave, formato iteraciones$saltBase64$hashBase64 (PBKDF2WithHmacSHA256). */
    @Basic
    @Column(name = "USAPCLVE", length = 256)
    private String claveHash;

    /** Estado: {@link com.saa.rubros.EstadoUsuarioApp}. */
    @Basic
    @Column(name = "USAPESTD")
    private Long estado;

    /** Intentos fallidos de login consecutivos. */
    @Basic
    @Column(name = "USAPINTF")
    private Long intentosFallidos;

    /** Fecha/hora hasta la cual el usuario está bloqueado por intentos fallidos. */
    @Basic
    @Column(name = "USAPBLHS")
    private LocalDateTime bloqueadoHasta;

    /** Flag: 1 = debe cambiar la clave en el próximo ingreso, 0 = no. */
    @Basic
    @Column(name = "USAPDCCL")
    private Long debeCambiarClave;

    /** Fecha de creación del registro. */
    @Basic
    @Column(name = "USAPFCRG")
    private LocalDateTime fechaCreacion;

    /** Fecha/hora del último acceso exitoso. */
    @Basic
    @Column(name = "USAPFCUA")
    private LocalDateTime fechaUltimoAcceso;

    /**
     * Usuario de oficina que registró el último cambio de credencial: quien enroló
     * ({@code crear}) o, si hubo un reseteo posterior, quien reseteó ({@code
     * resetearClave} SIEMPRE sobreescribe este campo — el rastro que importa es el del
     * último cambio, no el del enrolamiento original).
     */
    @Basic
    @Column(name = "USAPUSAR", length = 50)
    private String usuarioRegistro;

    /** Hash PBKDF2 del código de recuperación vigente (mismo formato que claveHash). Nulo = sin código. NUNCA se expone. */
    @Basic
    @Column(name = "USAPCRHS", length = 256)
    private String codigoRecuperacionHash;

    /** Vencimiento del código de recuperación vigente. */
    @Basic
    @Column(name = "USAPCRVN")
    private LocalDateTime codigoRecuperacionVence;

    /** Intentos fallidos sobre el código de recuperación vigente. */
    @Basic
    @Column(name = "USAPCRIN")
    private Long codigoRecuperacionIntentos;

    /** Hora del último envío de código (freno de 60 s). */
    @Basic
    @Column(name = "USAPCRUE")
    private LocalDateTime recuperacionUltimoEnvio;

    /** Inicio de la ventana de 24 h para contar envíos. */
    @Basic
    @Column(name = "USAPCRVE")
    private LocalDateTime recuperacionVentanaInicio;

    /** Envíos de código dentro de la ventana de 24 h. */
    @Basic
    @Column(name = "USAPCRNE")
    private Long recuperacionEnvios;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getClaveHash() {
        return claveHash;
    }

    public void setClaveHash(String claveHash) {
        this.claveHash = claveHash;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(Long intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Long getDebeCambiarClave() {
        return debeCambiarClave;
    }

    public void setDebeCambiarClave(Long debeCambiarClave) {
        this.debeCambiarClave = debeCambiarClave;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimoAcceso() {
        return fechaUltimoAcceso;
    }

    public void setFechaUltimoAcceso(LocalDateTime fechaUltimoAcceso) {
        this.fechaUltimoAcceso = fechaUltimoAcceso;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getCodigoRecuperacionHash() {
        return codigoRecuperacionHash;
    }

    public void setCodigoRecuperacionHash(String codigoRecuperacionHash) {
        this.codigoRecuperacionHash = codigoRecuperacionHash;
    }

    public LocalDateTime getCodigoRecuperacionVence() {
        return codigoRecuperacionVence;
    }

    public void setCodigoRecuperacionVence(LocalDateTime codigoRecuperacionVence) {
        this.codigoRecuperacionVence = codigoRecuperacionVence;
    }

    public Long getCodigoRecuperacionIntentos() {
        return codigoRecuperacionIntentos;
    }

    public void setCodigoRecuperacionIntentos(Long codigoRecuperacionIntentos) {
        this.codigoRecuperacionIntentos = codigoRecuperacionIntentos;
    }

    public LocalDateTime getRecuperacionUltimoEnvio() {
        return recuperacionUltimoEnvio;
    }

    public void setRecuperacionUltimoEnvio(LocalDateTime recuperacionUltimoEnvio) {
        this.recuperacionUltimoEnvio = recuperacionUltimoEnvio;
    }

    public LocalDateTime getRecuperacionVentanaInicio() {
        return recuperacionVentanaInicio;
    }

    public void setRecuperacionVentanaInicio(LocalDateTime recuperacionVentanaInicio) {
        this.recuperacionVentanaInicio = recuperacionVentanaInicio;
    }

    public Long getRecuperacionEnvios() {
        return recuperacionEnvios;
    }

    public void setRecuperacionEnvios(Long recuperacionEnvios) {
        this.recuperacionEnvios = recuperacionEnvios;
    }
}
