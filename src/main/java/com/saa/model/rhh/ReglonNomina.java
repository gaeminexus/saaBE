package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Detalle de rubros aplicados en la nómina.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "RNGL", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ReglonNominaId", query = "select e from ReglonNomina e where e.codigo=:id"),
    @NamedQuery(name = "ReglonNominaAll", query = "select e from ReglonNomina e")
})
public class ReglonNomina implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "RNGLCDGO")
    private Long codigo;

    /**
     * Nómina asociada.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "NMNACDGO", nullable = false)
    private Nomina nomina;


    /**
     * Cantidad o factor del rubro.
     */
    @Basic
    @Column(name = "RNGLCANT")
    private Double cantidad;

    /**
     * Valor del rubro.
     */
    @Basic
    @Column(name = "RNGLVLRO")
    private Double valor;

    /**
     * Indica si es imponible (S/N).
     */
    @Basic
    @Column(name = "RNGLIMPN")
    private String imponible;

    /**
     * Orden de cálculo o visualización.
     */
    @Basic
    @Column(name = "RNGLORDN")
    private Integer orden;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "RNGLFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "RNGLUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Concepto de nomina que origino el renglon. Sin esto la tabla es un numero anonimo.
     */
    @ManyToOne
    @JoinColumn(name = "CPNMCDGO", referencedColumnName = "CPNMCDGO")
    private ConceptoNomina conceptoNomina;

    /**
     * Descripcion del renglon.
     */
    @Basic
    @Column(name = "RNGLDSCR", length = 200)
    private String descripcion;

    /**
     * Tipo de concepto, copiado como snapshot del catalogo al momento del calculo.
     */
    @Basic
    @Column(name = "RNGLTPCN")
    private Long tipoConcepto;

    /**
     * Base sobre la que se calculo el renglon.
     */
    @Basic
    @Column(name = "RNGLBSCL")
    private Double baseCalculo;

    /**
     * Porcentaje aplicado.
     */
    @Basic
    @Column(name = "RNGLPRCN")
    private Double porcentaje;

    /**
     * Origen del renglon: detalle del rubro RHH_ORIGEN_RENGLON.
     */
    @Basic
    @Column(name = "RNGLORGN")
    private Long origen;

    /**
     * Fue editado a mano (S/N). Los renglones manuales sobreviven al recalculo.
     */
    @Basic
    @Column(name = "RNGLMNAL", length = 1)
    private String manual;

    /**
     * Fue imponible IESS (snapshot del concepto).
     */
    @Basic
    @Column(name = "RNGLIMIE", length = 1)
    private String imponibleIess;

    /**
     * Fue gravado de impuesto a la renta (snapshot del concepto).
     */
    @Basic
    @Column(name = "RNGLIMIR", length = 1)
    private String gravadoIr;

    /**
     * Fue costo patronal (snapshot del concepto).
     */
    @Basic
    @Column(name = "RNGLPTRN", length = 1)
    private String patronal;

    /**
     * Tabla de origen del renglon, para trazabilidad.
     */
    @Basic
    @Column(name = "RNGLRFTB", length = 30)
    private String tablaReferencia;

    /**
     * Id del registro de origen, para trazabilidad.
     */
    @Basic
    @Column(name = "RNGLRFID")
    private Long idReferencia;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Nomina getNomina() {
        return nomina;
    }

    public void setNomina(Nomina nomina) {
        this.nomina = nomina;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getImponible() {
        return imponible;
    }

    public void setImponible(String imponible) {
        this.imponible = imponible;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
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

    public ConceptoNomina getConceptoNomina() {
        return conceptoNomina;
    }

    public void setConceptoNomina(ConceptoNomina conceptoNomina) {
        this.conceptoNomina = conceptoNomina;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getTipoConcepto() {
        return tipoConcepto;
    }

    public void setTipoConcepto(Long tipoConcepto) {
        this.tipoConcepto = tipoConcepto;
    }

    public Double getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(Double baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public Long getOrigen() {
        return origen;
    }

    public void setOrigen(Long origen) {
        this.origen = origen;
    }

    public String getManual() {
        return manual;
    }

    public void setManual(String manual) {
        this.manual = manual;
    }

    public String getImponibleIess() {
        return imponibleIess;
    }

    public void setImponibleIess(String imponibleIess) {
        this.imponibleIess = imponibleIess;
    }

    public String getGravadoIr() {
        return gravadoIr;
    }

    public void setGravadoIr(String gravadoIr) {
        this.gravadoIr = gravadoIr;
    }

    public String getPatronal() {
        return patronal;
    }

    public void setPatronal(String patronal) {
        this.patronal = patronal;
    }

    public String getTablaReferencia() {
        return tablaReferencia;
    }

    public void setTablaReferencia(String tablaReferencia) {
        this.tablaReferencia = tablaReferencia;
    }

    public Long getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(Long idReferencia) {
        this.idReferencia = idReferencia;
    }
}
