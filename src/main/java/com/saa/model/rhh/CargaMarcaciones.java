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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Cabecera del lote de marcaciones importado del reloj biometrico.
 *
 * <p>El control antiduplicado es el <b>hash SHA-256 del archivo</b>, con el unique
 * <code>UQ_CRMR_HASH (PJRQCDGO, CRMRHASH)</code>: el mismo archivo no se puede cargar dos veces
 * en la misma empresa. Una carga anulada libera el hash a efectos del proceso —la comprobacion
 * ignora las anuladas—, aunque la fila se conserva para dejar rastro de que se intento.</p>
 *
 * <p><code>log</code> acumula el detalle de las lineas que no se pudieron procesar. Es un
 * <code>CLOB</code> porque con un archivo mensual de veinticinco empleados el log de errores
 * puede pasar holgadamente de los cuatro mil caracteres.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRMR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "CargaMarcacionesId",  query = "select e from CargaMarcaciones e where e.codigo=:id"),
    @NamedQuery(name = "CargaMarcacionesAll", query = "select e from CargaMarcaciones e")
})
public class CargaMarcaciones implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo de la carga.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CRMRCDGO")
    private Long codigo;

    /**
     * Empresa a la que pertenece la carga.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Formato con el que se leyo el archivo.
     */
    @ManyToOne
    @JoinColumn(name = "FMRCCDGO", referencedColumnName = "FMRCCDGO")
    private FormatoArchivoMarcacion formato;

    /**
     * Nombre del archivo cargado.
     */
    @Basic
    @Column(name = "CRMRNMAR", length = 300)
    private String nombreArchivo;

    /**
     * Hash SHA-256 del contenido del archivo.
     */
    @Basic
    @Column(name = "CRMRHASH", length = 64)
    private String hash;

    /**
     * Fecha de la carga.
     */
    @Basic
    @Column(name = "CRMRFCCR")
    private LocalDate fechaCarga;

    /**
     * Fecha de la marcacion mas antigua del archivo.
     */
    @Basic
    @Column(name = "CRMRFCDS")
    private LocalDate fechaDesde;

    /**
     * Fecha de la marcacion mas reciente del archivo.
     */
    @Basic
    @Column(name = "CRMRFCHS")
    private LocalDate fechaHasta;

    /**
     * Lineas totales del archivo, sin contar cabecera ni pie.
     */
    @Basic
    @Column(name = "CRMRLNTT")
    private Integer lineasTotales;

    /**
     * Lineas procesadas correctamente.
     */
    @Basic
    @Column(name = "CRMRLNOK")
    private Integer lineasOk;

    /**
     * Lineas con error.
     */
    @Basic
    @Column(name = "CRMRLNER")
    private Integer lineasError;

    /**
     * Lineas duplicadas descartadas.
     */
    @Basic
    @Column(name = "CRMRLNDP")
    private Integer lineasDuplicadas;

    /**
     * Log del procesamiento, con una entrada por linea que no se pudo procesar.
     */
    @Lob
    @Basic
    @Column(name = "CRMRLGGO")
    private String log;

    /**
     * Estado de la carga: detalle del rubro RHH_ESTADO_CARGA_MARCACIONES.
     */
    @Basic
    @Column(name = "CRMRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CRMRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CRMRUSRR", length = 60)
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

    public FormatoArchivoMarcacion getFormato() {
        return formato;
    }

    public void setFormato(FormatoArchivoMarcacion formato) {
        this.formato = formato;
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

    public LocalDate getFechaCarga() {
        return fechaCarga;
    }

    public void setFechaCarga(LocalDate fechaCarga) {
        this.fechaCarga = fechaCarga;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public Integer getLineasTotales() {
        return lineasTotales;
    }

    public void setLineasTotales(Integer lineasTotales) {
        this.lineasTotales = lineasTotales;
    }

    public Integer getLineasOk() {
        return lineasOk;
    }

    public void setLineasOk(Integer lineasOk) {
        this.lineasOk = lineasOk;
    }

    public Integer getLineasError() {
        return lineasError;
    }

    public void setLineasError(Integer lineasError) {
        this.lineasError = lineasError;
    }

    public Integer getLineasDuplicadas() {
        return lineasDuplicadas;
    }

    public void setLineasDuplicadas(Integer lineasDuplicadas) {
        this.lineasDuplicadas = lineasDuplicadas;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
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
