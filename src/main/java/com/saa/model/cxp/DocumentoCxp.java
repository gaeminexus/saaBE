package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Periodo;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

import jakarta.persistence.*;

/**
 * Entity DocumentoCxp.
 * Representa un documento ÚNICO emitido por un proveedor (tabla pgs.dcxp).
 * La clave de unicidad es claveAcceso.
 *
 * Un mismo documento puede aparecer en múltiples archivos TXT (DetalleCargaTxt),
 * pero siempre existe un único DocumentoCxp al que se le da seguimiento.
 *
 * estadoDocumento: 1=LEIDO 2=XML_CARGADO 3=REGISTRADO_BD 4=ERROR 5=NOVEDAD 6=REVERTIDO
 * estadoNovedad:   1=PENDIENTE 2=REEMPLAZADO 3=MANTENIDO
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCXP", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "DocumentoCxpAll",
                query = "select e from DocumentoCxp e"),
    @NamedQuery(name = "DocumentoCxpId",
                query = "select e from DocumentoCxp e where e.id = :id"),
    @NamedQuery(name = "DocumentoCxpByClave",
                query = "select e from DocumentoCxp e where e.claveAcceso = :claveAcceso"),
    @NamedQuery(name = "DocumentoCxpByEmpresa",
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa order by e.id desc"),
    @NamedQuery(name = "DocumentoCxpByEmpresaEstado",
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa and e.estadoDocumento = :estado order by e.id desc"),
    @NamedQuery(name = "DocumentoCxpNovedadesPendientes",
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa and e.estadoDocumento = 5 and e.estadoNovedad = 1 order by e.id desc"),
    @NamedQuery(name = "DocumentoCxpByEmpresaPeriodo",
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa and e.periodoContable.codigo = :idPeriodo order by e.id desc"),
    @NamedQuery(name = "DocumentoCxpActivosByEmpresaPeriodo",
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa and e.periodoContable.codigo = :idPeriodo and e.estadoDocumento <> 6 order by e.id desc")
})
public class DocumentoCxp implements Serializable {

    @Basic @Id @Column(name = "DCXPCDGO") @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "DCXPPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    // --- Datos del documento (del TXT SRI) ---
    @Basic @Column(name = "DCXPRCEM", length = 20)
    private String rucEmisor;

    @Basic @Column(name = "DCXPRSEM", length = 500)
    private String razonSocialEmisor;

    @Basic @Column(name = "DCXPTPCM", length = 100)
    private String tipoComprobante;

    @Basic @Column(name = "DCXPSRCM", length = 50)
    private String serieComprobante;

    @Basic @Column(name = "DCXPCLAC", length = 100, unique = true)
    private String claveAcceso;

    @Basic @Column(name = "DCXPFAUT")
    private LocalDateTime fechaAutorizacion;

    @Basic @Column(name = "DCXPFEMS")
    private LocalDateTime fechaEmision;

    @Basic @Column(name = "DCXPIDRC", length = 20)
    private String identificacionReceptor;

    @Basic @Column(name = "DCXPVSIM")
    private Double valorSinImpuestos;

    /** IVA = 3 chars → última letra repetida hasta completar 4: IVAA */
    @Basic @Column(name = "DCXPIVAA")
    private Double iva;

    @Basic @Column(name = "DCXPIMTT")
    private Double importeTotal;

    @Basic @Column(name = "DCXPNDMD", length = 50)
    private String numeroDocumentoModificado;

    // --- Estado del proceso ---
    @Basic @Column(name = "DCXPESTD")
    private Long estadoDocumento;

    // --- XML ---
    @Basic @Column(name = "DCXPPXML", length = 2000)
    private String pathXml;

    @Basic @Column(name = "DCXPFCXM")
    private LocalDateTime fechaCargaXml;

    @ManyToOne @JoinColumn(name = "DCXPUCXM", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioCargaXml;

    // --- Registro en BD ---
    @Basic @Column(name = "DCXPIDBD")
    private Long idDocumentoBD;

    @Basic @Column(name = "DCXPTBTD", length = 50)
    private String tipoTablaDestino;

    @Basic @Column(name = "DCXPFRBD")
    private LocalDateTime fechaRegistroBD;

    @ManyToOne @JoinColumn(name = "DCXPURBD", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioRegistroBD;

    // --- Reversión ---
    @Basic @Column(name = "DCXPFRVS")
    private LocalDateTime fechaReversion;

    @ManyToOne @JoinColumn(name = "DCXPURVS", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioReversion;

    // --- Novedad ---
    @Basic @Column(name = "DCXPNVDD", length = 2000)
    private String novedad;

    @Basic @Column(name = "DCXPENOV")
    private Long estadoNovedad;

    // --- General ---
    @Basic @Column(name = "DCXPOBSR", length = 2000)
    private String observacion;

    /** Período contable — FK única a CNT.PRDO, columna toma el nombre del PK: PRDOCDGO */
    @ManyToOne @JoinColumn(name = "PRDOCDGO", referencedColumnName = "PRDOCDGO")
    private Periodo periodoContable;

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public String getRucEmisor() { return rucEmisor; }
    public void setRucEmisor(String rucEmisor) { this.rucEmisor = rucEmisor; }
    public String getRazonSocialEmisor() { return razonSocialEmisor; }
    public void setRazonSocialEmisor(String razonSocialEmisor) { this.razonSocialEmisor = razonSocialEmisor; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getSerieComprobante() { return serieComprobante; }
    public void setSerieComprobante(String serieComprobante) { this.serieComprobante = serieComprobante; }
    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }
    public LocalDateTime getFechaAutorizacion() { return fechaAutorizacion; }
    public void setFechaAutorizacion(LocalDateTime fechaAutorizacion) { this.fechaAutorizacion = fechaAutorizacion; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
    public String getIdentificacionReceptor() { return identificacionReceptor; }
    public void setIdentificacionReceptor(String identificacionReceptor) { this.identificacionReceptor = identificacionReceptor; }
    public Double getValorSinImpuestos() { return valorSinImpuestos; }
    public void setValorSinImpuestos(Double valorSinImpuestos) { this.valorSinImpuestos = valorSinImpuestos; }
    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }
    public Double getImporteTotal() { return importeTotal; }
    public void setImporteTotal(Double importeTotal) { this.importeTotal = importeTotal; }
    public String getNumeroDocumentoModificado() { return numeroDocumentoModificado; }
    public void setNumeroDocumentoModificado(String numeroDocumentoModificado) { this.numeroDocumentoModificado = numeroDocumentoModificado; }
    public Long getEstadoDocumento() { return estadoDocumento; }
    public void setEstadoDocumento(Long estadoDocumento) { this.estadoDocumento = estadoDocumento; }
    public String getPathXml() { return pathXml; }
    public void setPathXml(String pathXml) { this.pathXml = pathXml; }
    public LocalDateTime getFechaCargaXml() { return fechaCargaXml; }
    public void setFechaCargaXml(LocalDateTime fechaCargaXml) { this.fechaCargaXml = fechaCargaXml; }
    public Usuario getUsuarioCargaXml() { return usuarioCargaXml; }
    public void setUsuarioCargaXml(Usuario usuarioCargaXml) { this.usuarioCargaXml = usuarioCargaXml; }
    public Long getIdDocumentoBD() { return idDocumentoBD; }
    public void setIdDocumentoBD(Long idDocumentoBD) { this.idDocumentoBD = idDocumentoBD; }
    public String getTipoTablaDestino() { return tipoTablaDestino; }
    public void setTipoTablaDestino(String tipoTablaDestino) { this.tipoTablaDestino = tipoTablaDestino; }
    public LocalDateTime getFechaRegistroBD() { return fechaRegistroBD; }
    public void setFechaRegistroBD(LocalDateTime fechaRegistroBD) { this.fechaRegistroBD = fechaRegistroBD; }
    public Usuario getUsuarioRegistroBD() { return usuarioRegistroBD; }
    public void setUsuarioRegistroBD(Usuario usuarioRegistroBD) { this.usuarioRegistroBD = usuarioRegistroBD; }
    public LocalDateTime getFechaReversion() { return fechaReversion; }
    public void setFechaReversion(LocalDateTime fechaReversion) { this.fechaReversion = fechaReversion; }
    public Usuario getUsuarioReversion() { return usuarioReversion; }
    public void setUsuarioReversion(Usuario usuarioReversion) { this.usuarioReversion = usuarioReversion; }
    public String getNovedad() { return novedad; }
    public void setNovedad(String novedad) { this.novedad = novedad; }
    public Long getEstadoNovedad() { return estadoNovedad; }
    public void setEstadoNovedad(Long estadoNovedad) { this.estadoNovedad = estadoNovedad; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public Periodo getPeriodoContable() { return periodoContable; }
    public void setPeriodoContable(Periodo periodoContable) { this.periodoContable = periodoContable; }
}