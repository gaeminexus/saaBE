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
 * resultado: NUEVO | DUPLICADO | NOVEDAD | IGNORADO
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

    @Basic @Id @Column(name = "ID", precision = 0) @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cabecera de la carga a la que pertenece esta línea */
    @ManyToOne @JoinColumn(name = "CARGATXT", referencedColumnName = "ID")
    private CargaArchivoTxt cargaTxt;

    /** Documento único al que apunta esta línea */
    @ManyToOne @JoinColumn(name = "DOCUMENTO", referencedColumnName = "ID")
    private DocumentoCxp documento;

    // --- Valores tal como venían en ESTA carga (pueden diferir del documento maestro) ---
    @Basic @Column(name = "VALORSINIMPUESTOS_CARGA")
    private Double valorSinImpuestosCarga;

    @Basic @Column(name = "IVA_CARGA")
    private Double ivaCarga;

    @Basic @Column(name = "IMPORTETOTAL_CARGA")
    private Double importeTotalCarga;

    @Basic @Column(name = "FECHAAUTORIZACION_CARGA")
    private LocalDateTime fechaAutorizacionCarga;

    @Basic @Column(name = "FECHAEMISION_CARGA")
    private LocalDate fechaEmisionCarga;

    /** NUEVO | DUPLICADO | NOVEDAD | IGNORADO */
    @Basic @Column(name = "RESULTADO", length = 20)
    private String resultado;

    @Basic @Column(name = "OBSERVACION", length = 2000)
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
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}