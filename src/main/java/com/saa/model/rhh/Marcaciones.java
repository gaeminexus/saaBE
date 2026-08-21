package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFecha;

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
 * Marcaciones de asistencia (entrada/salida).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MRCC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "MarcacionesId", query = "select e from Marcaciones e where e.codigo=:id"),
    @NamedQuery(name = "MarcacionesAll", query = "select e from Marcaciones e")
})
public class Marcaciones implements Serializable, EntidadAuditableFecha {

    /**
     * Código único de la marcación.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "MRCCCDGO")
    private Long codigo;

    /**
     * Empleado que marcó.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Fecha y hora de la marcación.
     */
    @Basic
    @Column(name = "MRCCFCHH", nullable = false)
    private LocalDateTime fechaHora;

    /**
     * Tipo de marcación (ENTRADA / SALIDA).
     */
    @Basic
    @Column(name = "MRCCTPOO")
    private Long tipo;

    /**
     * Origen (RELOJ / WEB / MOVIL / etc.).
     */
    @Basic
    @Column(name = "MRCCORGN")
    private Long origen;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "MRCCOBSR")
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "MRCCFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "MRCCUSRR")
    private String usuarioRegistro;


    /**
     * Lote de importacion del que vino esta marcacion. Nula en las registradas a mano.
     */
    @ManyToOne
    @JoinColumn(name = "CRMRCDGO", referencedColumnName = "CRMRCDGO")
    private CargaMarcaciones cargaMarcaciones;

    /**
     * Identificador del dispositivo que registro la marcacion.
     */
    @Basic
    @Column(name = "MRCCDSPS", length = 50)
    private String dispositivo;

    /**
     * Numero de linea dentro del archivo origen. Sirve para rastrear una marcacion hasta su
     * linea del archivo cuando hay que explicar una diferencia.
     */
    @Basic
    @Column(name = "MRCCLNAR")
    private Integer lineaArchivo;

    /**
     * Ya fue consolidada en un resumen diario (S/N). Evita que una reconsolidacion la cuente
     * dos veces.
     */
    @Basic
    @Column(name = "MRCCPRCS", length = 1)
    private String procesado;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Long getTipo() {
        return tipo;
    }

    public void setTipo(Long tipo) {
        this.tipo = tipo;
    }

    public Long getOrigen() {
        return origen;
    }

    public void setOrigen(Long origen) {
        this.origen = origen;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDate fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public CargaMarcaciones getCargaMarcaciones() {
        return cargaMarcaciones;
    }

    public void setCargaMarcaciones(CargaMarcaciones cargaMarcaciones) {
        this.cargaMarcaciones = cargaMarcaciones;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public Integer getLineaArchivo() {
        return lineaArchivo;
    }

    public void setLineaArchivo(Integer lineaArchivo) {
        this.lineaArchivo = lineaArchivo;
    }

    public String getProcesado() {
        return procesado;
    }

    public void setProcesado(String procesado) {
        this.procesado = procesado;
    }
}
