package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.scp.Empresa;

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
import jakarta.persistence.Table;

/**
 * Registro de una salida oficial generada: RDEP, formulario 107, planilla del IESS,
 * formularios del MDT y acta de finiquito.
 *
 * <p><b>No duplica datos.</b> Las filas del RDEP y del 107 ya viven en <code>RNGL</code>,
 * <code>ACMN</code> y <code>LQBS</code>, y los casilleros salen de <code>CPNMRDEP</code>,
 * <code>CPNMF107</code> y <code>CPNMIESS</code>: regenerar un archivo es determinista. Lo que
 * no existia en ninguna parte es <b>el hecho de la presentacion</b>, y eso es lo unico que
 * persiste esta tabla.</p>
 *
 * <p><b><code>fechaGeneracion</code> y <code>fechaPresentacion</code> son cosas distintas y
 * ninguna sustituye a la otra.</b> La primera la pone el sistema al generar; la segunda la
 * escribe una persona cuando el organismo recibe, junto con <code>numeroComprobante</code>. Una
 * salida generada y no presentada es el estado normal durante dias.</p>
 *
 * <p><b>La idempotencia es del servicio, no de un unique.</b> El indice
 * <code>IX_SLOF_BUSQ</code> no es unico a proposito: <code>SLOFMESS</code> y
 * <code>MPLDCDGO</code> son nulos en las salidas anuales y consolidadas, y Oracle no considera
 * duplicadas dos filas donde alguna columna de la clave es nula, de modo que un UNIQUE no
 * impediria nada justo en los casos que importan.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SLOF", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "SalidaOficialId",  query = "select e from SalidaOficial e where e.codigo=:id"),
    @NamedQuery(name = "SalidaOficialAll", query = "select e from SalidaOficial e")
})
public class SalidaOficial implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo de la salida.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "SLOFCDGO")
    private Long codigo;

    /**
     * Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de salida: detalle del rubro RHH_TIPO_SALIDA_OFICIAL.
     */
    @Basic
    @Column(name = "SLOFTPSL")
    private Long tipoSalida;

    /**
     * Ejercicio fiscal.
     */
    @Basic
    @Column(name = "SLOFANOO")
    private Integer anio;

    /**
     * Mes. Nulo en las salidas anuales.
     */
    @Basic
    @Column(name = "SLOFMESS")
    private Integer mes;

    /**
     * Empleado. Nulo en las salidas consolidadas.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Ruta del archivo generado.
     */
    @Basic
    @Column(name = "SLOFRUTA", length = 500)
    private String rutaArchivo;

    /**
     * Nombre del archivo generado.
     */
    @Basic
    @Column(name = "SLOFNMAR", length = 200)
    private String nombreArchivo;

    /**
     * SHA-256 del contenido generado.
     */
    @Basic
    @Column(name = "SLOFHASH", length = 64)
    private String hash;

    /**
     * Fecha en que el sistema genero la salida.
     */
    @Basic
    @Column(name = "SLOFFCGN")
    private LocalDate fechaGeneracion;

    /**
     * Fecha en que la salida se presento al organismo. Nula mientras no se presente.
     */
    @Basic
    @Column(name = "SLOFFCPR")
    private LocalDate fechaPresentacion;

    /**
     * Numero de comprobante devuelto por el organismo.
     */
    @Basic
    @Column(name = "SLOFNRCM", length = 60)
    private String numeroComprobante;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "SLOFOBSR", length = 500)
    private String observaciones;

    /**
     * Estado de la salida.
     */
    @Basic
    @Column(name = "SLOFESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "SLOFFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "SLOFUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getTipoSalida() {
        return tipoSalida;
    }

    public void setTipoSalida(Long tipoSalida) {
        this.tipoSalida = tipoSalida;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public void setRutaArchivo(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public LocalDate getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDate fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public LocalDate getFechaPresentacion() {
        return fechaPresentacion;
    }

    public void setFechaPresentacion(LocalDate fechaPresentacion) {
        this.fechaPresentacion = fechaPresentacion;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
