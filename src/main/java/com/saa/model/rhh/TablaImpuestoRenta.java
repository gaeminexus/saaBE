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
 * Tramo de la tabla del impuesto a la renta de personas naturales, por anio fiscal y empresa.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TBIR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "TablaImpuestoRentaId", query = "select e from TablaImpuestoRenta e where e.codigo=:id"),
    @NamedQuery(name = "TablaImpuestoRentaAll", query = "select e from TablaImpuestoRenta e")
})
public class TablaImpuestoRenta implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del tramo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "TBIRCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del registro (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Anio fiscal al que corresponde la tabla.
     */
    @Basic
    @Column(name = "TBIRANOO")
    private Integer anio;

    /**
     * Orden del tramo dentro de la tabla; 1 es el primero.
     */
    @Basic
    @Column(name = "TBIRORDN")
    private Integer orden;

    /**
     * Fraccion basica del tramo.
     */
    @Basic
    @Column(name = "TBIRFRBS")
    private Double fraccionBasica;

    /**
     * Limite superior del tramo; nulo en el ultimo tramo.
     */
    @Basic
    @Column(name = "TBIREXCS")
    private Double excesoHasta;

    /**
     * Impuesto sobre la fraccion basica.
     */
    @Basic
    @Column(name = "TBIRIMFB")
    private Double impuestoFraccionBasica;

    /**
     * Porcentaje aplicable sobre la fraccion excedente.
     */
    @Basic
    @Column(name = "TBIRPRCN")
    private Double porcentaje;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "TBIRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "TBIRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "TBIRUSRR", length = 60)
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

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Double getFraccionBasica() {
        return fraccionBasica;
    }

    public void setFraccionBasica(Double fraccionBasica) {
        this.fraccionBasica = fraccionBasica;
    }

    public Double getExcesoHasta() {
        return excesoHasta;
    }

    public void setExcesoHasta(Double excesoHasta) {
        this.excesoHasta = excesoHasta;
    }

    public Double getImpuestoFraccionBasica() {
        return impuestoFraccionBasica;
    }

    public void setImpuestoFraccionBasica(Double impuestoFraccionBasica) {
        this.impuestoFraccionBasica = impuestoFraccionBasica;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
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
