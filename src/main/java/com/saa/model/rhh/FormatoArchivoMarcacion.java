package com.saa.model.rhh;

import java.io.Serializable;
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
 * Definicion del formato del archivo del reloj biometrico. Permite absorber cualquier marca sin tocar codigo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FMRC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "FormatoArchivoMarcacionId", query = "select e from FormatoArchivoMarcacion e where e.codigo=:id"),
    @NamedQuery(name = "FormatoArchivoMarcacionAll", query = "select e from FormatoArchivoMarcacion e")
})
public class FormatoArchivoMarcacion implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del formato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "FMRCCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del formato (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Nombre del formato.
     */
    @Basic
    @Column(name = "FMRCNMBR", length = 100)
    private String nombre;

    /**
     * Marca y modelo del dispositivo.
     */
    @Basic
    @Column(name = "FMRCMRCA", length = 100)
    private String marca;

    /**
     * Tipo de formato: detalle del rubro RHH_FORMATO_ARCHIVO_MARCACION.
     */
    @Basic
    @Column(name = "FMRCTPFR")
    private Long tipoFormato;

    /**
     * Caracter delimitador, en formatos delimitados.
     */
    @Basic
    @Column(name = "FMRCDLMT", length = 5)
    private String delimitador;

    /**
     * Lineas de cabecera a saltar.
     */
    @Basic
    @Column(name = "FMRCLNCB")
    private Integer lineasCabecera;

    /**
     * Lineas de pie a ignorar.
     */
    @Basic
    @Column(name = "FMRCLNPI")
    private Integer lineasPie;

    /**
     * Patron de formato de la fecha.
     */
    @Basic
    @Column(name = "FMRCFRFC", length = 30)
    private String formatoFecha;

    /**
     * Patron de formato de la hora.
     */
    @Basic
    @Column(name = "FMRCFRHR", length = 30)
    private String formatoHora;

    /**
     * Patron de formato de fecha y hora combinadas.
     */
    @Basic
    @Column(name = "FMRCFRFH", length = 50)
    private String formatoFechaHora;

    /**
     * Codificacion del archivo.
     */
    @Basic
    @Column(name = "FMRCCDFC", length = 30)
    private String codificacion;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "FMRCESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "FMRCFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "FMRCUSRR", length = 60)
    private String usuarioRegistro;

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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Long getTipoFormato() {
        return tipoFormato;
    }

    public void setTipoFormato(Long tipoFormato) {
        this.tipoFormato = tipoFormato;
    }

    public String getDelimitador() {
        return delimitador;
    }

    public void setDelimitador(String delimitador) {
        this.delimitador = delimitador;
    }

    public Integer getLineasCabecera() {
        return lineasCabecera;
    }

    public void setLineasCabecera(Integer lineasCabecera) {
        this.lineasCabecera = lineasCabecera;
    }

    public Integer getLineasPie() {
        return lineasPie;
    }

    public void setLineasPie(Integer lineasPie) {
        this.lineasPie = lineasPie;
    }

    public String getFormatoFecha() {
        return formatoFecha;
    }

    public void setFormatoFecha(String formatoFecha) {
        this.formatoFecha = formatoFecha;
    }

    public String getFormatoHora() {
        return formatoHora;
    }

    public void setFormatoHora(String formatoHora) {
        this.formatoHora = formatoHora;
    }

    public String getFormatoFechaHora() {
        return formatoFechaHora;
    }

    public void setFormatoFechaHora(String formatoFechaHora) {
        this.formatoFechaHora = formatoFechaHora;
    }

    public String getCodificacion() {
        return codificacion;
    }

    public void setCodificacion(String codificacion) {
        this.codificacion = codificacion;
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
