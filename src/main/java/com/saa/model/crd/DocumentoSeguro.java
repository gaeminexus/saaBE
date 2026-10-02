package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
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
 * Representa la tabla CRD.POSG (DocumentoSeguro): una factura anual de la aseguradora, o una de
 * sus notas de débito (inclusión) / crédito (exclusión). Ver
 * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} §5 y
 * {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md}, y
 * {@code docs/logica-negocio/crd/sql/306_DDL_POLIZAS_SEGURO.sql} (29 columnas, mapeadas
 * exactamente).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "POSG", schema = "CRD")
@SequenceGenerator(name = "SQ_POSGCDGO", sequenceName = "CRD.SQ_POSGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DocumentoSeguroAll", query = "select e from DocumentoSeguro e"),
    @NamedQuery(name = "DocumentoSeguroId",  query = "select e from DocumentoSeguro e where e.codigo = :id")
})
public class DocumentoSeguro implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "POSGCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_POSGCDGO")
    private Long codigo;

    /** 1 DESGRAVAMEN · 2 INCENDIO · 3 PRENDARIO — {@code com.saa.rubros.TipoSeguro}. */
    @Basic
    @Column(name = "POSGTPSG")
    private Long tipoSeguro;

    /** 1 FACTURA · 2 NOTA_DEBITO · 3 NOTA_CREDITO — {@code com.saa.rubros.ClaseDocumentoSeguro}. */
    @Basic
    @Column(name = "POSGCLSE")
    private Long clase;

    /** La FACTURA madre. Obligatoria para ND/NC, nula para una factura. */
    @ManyToOne
    @JoinColumn(name = "POSGPADR", referencedColumnName = "POSGCDGO")
    private DocumentoSeguro padre;

    /** Ciclo del documento — {@code com.saa.rubros.EstadoDocumentoSeguro}. */
    @Basic
    @Column(name = "POSGESTD")
    private Long estado;

    /** Fecha de corte del listado: la base de cada préstamo se tomó a esta fecha. */
    @Basic
    @Column(name = "POSGFCCT")
    private LocalDate fechaCorte;

    // ============================================================
    // Documento de la aseguradora (paso 2)
    // ============================================================

    @Basic
    @Column(name = "POSGASGR", length = 300)
    private String aseguradora;

    @Basic
    @Column(name = "POSGRUCA", length = 20)
    private String ruc;

    @Basic
    @Column(name = "POSGNMPL", length = 60)
    private String numeroPoliza;

    @Basic
    @Column(name = "POSGNMDC", length = 60)
    private String numeroDocumento;

    /** Clave de acceso del SRI. Única. Llave del enlace con CXP (omen-saa-2, S7). */
    @Basic
    @Column(name = "POSGCLAC", length = 60)
    private String claveAcceso;

    @Basic
    @Column(name = "POSGFCEM")
    private LocalDate fechaEmision;

    @Basic
    @Column(name = "POSGFCIN")
    private LocalDate fechaInicio;

    @Basic
    @Column(name = "POSGFCFN")
    private LocalDate fechaFin;

    /** Tasa aplicada por la aseguradora. Informativa: el reparto usa el valor total. */
    @Basic
    @Column(name = "POSGTASA")
    private Double tasa;

    /** VALOR TOTAL con impuestos — lo que se prorratea en las cuotas (S1-S12). */
    @Basic
    @Column(name = "POSGVLTT")
    private Double valorTotal;

    /** Id del documento en CXP, una vez enlazado (§9, pendiente de omen-saa-2). */
    @Basic
    @Column(name = "POSGDCXP")
    private Long idDocumentoCxp;

    @Basic
    @Column(name = "POSGOBSR", length = 1000)
    private String observacion;

    // ============================================================
    // Auditoría por paso
    // ============================================================

    @Basic
    @Column(name = "POSGUSRG", length = 50)
    private String usuarioListado;

    @Basic
    @Column(name = "POSGFCRG")
    private LocalDateTime fechaListado;

    @Basic
    @Column(name = "POSGUSDC", length = 50)
    private String usuarioDocumento;

    @Basic
    @Column(name = "POSGFCDC")
    private LocalDateTime fechaDocumento;

    @Basic
    @Column(name = "POSGUSDS", length = 50)
    private String usuarioDistribucion;

    @Basic
    @Column(name = "POSGFCDS")
    private LocalDateTime fechaDistribucion;

    @Basic
    @Column(name = "POSGUSLB", length = 50)
    private String usuarioLiberacion;

    @Basic
    @Column(name = "POSGFCLB")
    private LocalDateTime fechaLiberacion;

    @Basic
    @Column(name = "POSGUSAN", length = 50)
    private String usuarioAnulacion;

    @Basic
    @Column(name = "POSGFCAN")
    private LocalDateTime fechaAnulacion;

    @Basic
    @Column(name = "POSGMTAN", length = 500)
    private String motivoAnulacion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Long getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(Long tipoSeguro) { this.tipoSeguro = tipoSeguro; }

    public Long getClase() { return clase; }
    public void setClase(Long clase) { this.clase = clase; }

    public DocumentoSeguro getPadre() { return padre; }
    public void setPadre(DocumentoSeguro padre) { this.padre = padre; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getAseguradora() { return aseguradora; }
    public void setAseguradora(String aseguradora) { this.aseguradora = aseguradora; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getNumeroPoliza() { return numeroPoliza; }
    public void setNumeroPoliza(String numeroPoliza) { this.numeroPoliza = numeroPoliza; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Double getTasa() { return tasa; }
    public void setTasa(Double tasa) { this.tasa = tasa; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public Long getIdDocumentoCxp() { return idDocumentoCxp; }
    public void setIdDocumentoCxp(Long idDocumentoCxp) { this.idDocumentoCxp = idDocumentoCxp; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getUsuarioListado() { return usuarioListado; }
    public void setUsuarioListado(String usuarioListado) { this.usuarioListado = usuarioListado; }

    public LocalDateTime getFechaListado() { return fechaListado; }
    public void setFechaListado(LocalDateTime fechaListado) { this.fechaListado = fechaListado; }

    public String getUsuarioDocumento() { return usuarioDocumento; }
    public void setUsuarioDocumento(String usuarioDocumento) { this.usuarioDocumento = usuarioDocumento; }

    public LocalDateTime getFechaDocumento() { return fechaDocumento; }
    public void setFechaDocumento(LocalDateTime fechaDocumento) { this.fechaDocumento = fechaDocumento; }

    public String getUsuarioDistribucion() { return usuarioDistribucion; }
    public void setUsuarioDistribucion(String usuarioDistribucion) { this.usuarioDistribucion = usuarioDistribucion; }

    public LocalDateTime getFechaDistribucion() { return fechaDistribucion; }
    public void setFechaDistribucion(LocalDateTime fechaDistribucion) { this.fechaDistribucion = fechaDistribucion; }

    public String getUsuarioLiberacion() { return usuarioLiberacion; }
    public void setUsuarioLiberacion(String usuarioLiberacion) { this.usuarioLiberacion = usuarioLiberacion; }

    public LocalDateTime getFechaLiberacion() { return fechaLiberacion; }
    public void setFechaLiberacion(LocalDateTime fechaLiberacion) { this.fechaLiberacion = fechaLiberacion; }

    public String getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
