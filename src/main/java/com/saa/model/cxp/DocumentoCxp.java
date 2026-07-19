package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
                query = "select e from DocumentoCxp e where e.empresa.codigo = :idEmpresa and e.estadoDocumento = 5 and e.estadoNovedad = 1 order by e.id desc")
})
public class DocumentoCxp implements Serializable {

    @Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    // --- Datos del documento (del TXT SRI) ---
    @Basic @Column(name = "RUCEMISOR", length = 20)
    private String rucEmisor;

    @Basic @Column(name = "RAZONSOCIALEMISOR", length = 500)
    private String razonSocialEmisor;

    @Basic @Column(name = "TIPOCOMPROBANTE", length = 100)
    private String tipoComprobante;

    @Basic @Column(name = "SERIECOMPROBANTE", length = 50)
    private String serieComprobante;

    @Basic @Column(name = "CLAVEACCESO", length = 100, unique = true)
    private String claveAcceso;

    @Basic @Column(name = "FECHAAUTORIZACION")
    private LocalDateTime fechaAutorizacion;

    @Basic @Column(name = "FECHAEMISION")
    private LocalDate fechaEmision;

    @Basic @Column(name = "IDENTIFICACIONRECEPTOR", length = 20)
    private String identificacionReceptor;

    @Basic @Column(name = "VALORSINIMPUESTOS")
    private Double valorSinImpuestos;

    @Basic @Column(name = "IVA")
    private Double iva;

    @Basic @Column(name = "IMPORTETOTAL")
    private Double importeTotal;

    @Basic @Column(name = "NUMERODOCUMENTOMODIFICADO", length = 50)
    private String numeroDocumentoModificado;

    // --- Estado del proceso ---
    @Basic @Column(name = "ESTADODOCUMENTO")
    private Long estadoDocumento;

    // --- XML ---
    @Basic @Column(name = "PATHXML", length = 2000)
    private String pathXml;

    @Basic @Column(name = "FECHACARGAXML")
    private LocalDateTime fechaCargaXml;

    @ManyToOne @JoinColumn(name = "USUARIOCARGAXML", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioCargaXml;

    // --- Registro en BD ---
    @Basic @Column(name = "IDDOCUMENTOBD")
    private Long idDocumentoBD;

    @Basic @Column(name = "TIPOTABLADESTINO", length = 50)
    private String tipoTablaDestino;

    @Basic @Column(name = "FECHAREGISTROBD")
    private LocalDateTime fechaRegistroBD;

    @ManyToOne @JoinColumn(name = "USUARIOREGISTROBD", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioRegistroBD;

    // --- Reversión ---
    @Basic @Column(name = "FECHAREVERSION")
    private LocalDateTime fechaReversion;

    @ManyToOne @JoinColumn(name = "USUARIOREVERSION", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioReversion;

    // --- Novedad ---
    @Basic @Column(name = "NOVEDAD", length = 2000)
    private String novedad;

    @Basic @Column(name = "ESTADONOVEDAD")
    private Long estadoNovedad;

    // --- General ---
    @Basic @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

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
    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }
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
}
