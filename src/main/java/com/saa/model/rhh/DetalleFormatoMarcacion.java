package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Mapeo campo a campo del archivo de marcaciones: posicion, longitud y traduccion de codigos.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DFMR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleFormatoMarcacionId", query = "select e from DetalleFormatoMarcacion e where e.codigo=:id"),
    @NamedQuery(name = "DetalleFormatoMarcacionAll", query = "select e from DetalleFormatoMarcacion e")
})
public class DetalleFormatoMarcacion implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DFMRCDGO")
    private Long codigo;

    /**
     * Formato al que pertenece el campo.
     */
    @ManyToOne
    @JoinColumn(name = "FMRCCDGO", referencedColumnName = "FMRCCDGO")
    private FormatoArchivoMarcacion formato;

    /**
     * Campo logico: detalle del rubro RHH_CAMPO_ARCHIVO_MARCACION.
     */
    @Basic
    @Column(name = "DFMRCMPO")
    private Long campo;

    /**
     * Orden del campo dentro de la linea.
     */
    @Basic
    @Column(name = "DFMRORDN")
    private Integer orden;

    /**
     * Posicion del campo en formatos delimitados, base 1.
     */
    @Basic
    @Column(name = "DFMRPSCN")
    private Integer posicion;

    /**
     * Indice de inicio en formatos de ancho fijo, base 0.
     */
    @Basic
    @Column(name = "DFMRINCO")
    private Integer indiceInicio;

    /**
     * Longitud del campo en formatos de ancho fijo.
     */
    @Basic
    @Column(name = "DFMRLNGT")
    private Integer longitud;

    /**
     * Mapeo de valores origen a codigos del sistema, con formato origen=destino;origen=destino.
     */
    @Basic
    @Column(name = "DFMRMPEO", length = 500)
    private String mapeo;

    /**
     * El campo es obligatorio (S/N).
     */
    @Basic
    @Column(name = "DFMROBLG", length = 1)
    private String obligatorio;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "DFMRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DFMRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "DFMRUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public FormatoArchivoMarcacion getFormato() {
        return formato;
    }

    public void setFormato(FormatoArchivoMarcacion formato) {
        this.formato = formato;
    }

    public Long getCampo() {
        return campo;
    }

    public void setCampo(Long campo) {
        this.campo = campo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getPosicion() {
        return posicion;
    }

    public void setPosicion(Integer posicion) {
        this.posicion = posicion;
    }

    public Integer getIndiceInicio() {
        return indiceInicio;
    }

    public void setIndiceInicio(Integer indiceInicio) {
        this.indiceInicio = indiceInicio;
    }

    public Integer getLongitud() {
        return longitud;
    }

    public void setLongitud(Integer longitud) {
        this.longitud = longitud;
    }

    public String getMapeo() {
        return mapeo;
    }

    public void setMapeo(String mapeo) {
        this.mapeo = mapeo;
    }

    public String getObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(String obligatorio) {
        this.obligatorio = obligatorio;
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
