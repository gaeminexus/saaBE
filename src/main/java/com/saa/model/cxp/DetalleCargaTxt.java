package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Entity DetalleCargaTxt.
 * Representa UNA LÍNEA de un archivo TXT cargado (tabla pgs.dctx).
 * Un mismo documento (DocumentoCxp) puede aparecer en múltiples líneas/cargas.
 *
 * resultado: 1=NUEVO | 2=DUPLICADO | 3=NOVEDAD | 4=IGNORADO | 5=DESAPARECIDO
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DCTX", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "DetalleCargaTxtAll",
                query = "select e from DetalleCargaTxt e"),
    @NamedQuery(name = "DetalleCargaTxtId",
                query = "select e from DetalleCargaTxt e where e.id = :id"),
    @NamedQuery(name = "DetalleCargaTxtByCarga",
                query = "select e from DetalleCargaTxt e where e.cargaTxt.id = :idCarga order by e.id"),
    @NamedQuery(name = "DetalleCargaTxtByDocumento",
                query = "select e from DetalleCargaTxt e where e.documento.id = :idDocumento order by e.id")
})
public class DetalleCargaTxt implements Serializable {

    @Basic @Id @Column(name = "DCTXCDGO") @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK única a PGS.CRTX — columna toma el nombre del PK de CRTX: CRTXCDGO */
    @ManyToOne @JoinColumn(name = "CRTXCDGO", referencedColumnName = "CRTXCDGO")
    private CargaArchivoTxt cargaTxt;

    /** FK única a PGS.DCXP — columna toma el nombre del PK de DCXP: DCXPCDGO */
    @ManyToOne @JoinColumn(name = "DCXPCDGO", referencedColumnName = "DCXPCDGO")
    private DocumentoCxp documento;

    // --- Valores tal como venían en ESTA carga (pueden diferir del documento maestro) ---
    @Basic @Column(name = "DCTXVSIM")
    private Double valorSinImpuestosCarga;

    /** IVA = 3 chars → última letra repetida hasta completar 4: IVAA */
    @Basic @Column(name = "DCTXIVAA")
    private Double ivaCarga;

    @Basic @Column(name = "DCTXIMTT")
    private Double importeTotalCarga;

    @Basic @Column(name = "DCTXFAUT")
    private LocalDateTime fechaAutorizacionCarga;

    @Basic @Column(name = "DCTXFEMS")
    private LocalDate fechaEmisionCarga;

    /** Rubro 174 CXP_RESULTADO_CARGA_TXT: 1=NUEVO 2=DUPLICADO 3=NOVEDAD 4=IGNORADO 5=DESAPARECIDO */
    @Basic @Column(name = "DCTXRSLT")
    private Long resultado;

    @Basic @Column(name = "DCTXOBSR", length = 2000)
    private String observacion;

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CargaArchivoTxt getCargaTxt() { return cargaTxt; }
    public void setCargaTxt(CargaArchivoTxt cargaTxt) { this.cargaTxt = cargaTxt; }
    public DocumentoCxp getDocumento() { return documento; }
    public void setDocumento(DocumentoCxp documento) { this.documento = documento; }
    public Double getValorSinImpuestosCarga() { return valorSinImpuestosCarga; }
    public void setValorSinImpuestosCarga(Double valorSinImpuestosCarga) { this.valorSinImpuestosCarga = valorSinImpuestosCarga; }
    public Double getIvaCarga() { return ivaCarga; }
    public void setIvaCarga(Double ivaCarga) { this.ivaCarga = ivaCarga; }
    public Double getImporteTotalCarga() { return importeTotalCarga; }
    public void setImporteTotalCarga(Double importeTotalCarga) { this.importeTotalCarga = importeTotalCarga; }
    public LocalDateTime getFechaAutorizacionCarga() { return fechaAutorizacionCarga; }
    public void setFechaAutorizacionCarga(LocalDateTime fechaAutorizacionCarga) { this.fechaAutorizacionCarga = fechaAutorizacionCarga; }
    public LocalDate getFechaEmisionCarga() { return fechaEmisionCarga; }
    public void setFechaEmisionCarga(LocalDate fechaEmisionCarga) { this.fechaEmisionCarga = fechaEmisionCarga; }
    public Long getResultado() { return resultado; }
    public void setResultado(Long resultado) { this.resultado = resultado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}