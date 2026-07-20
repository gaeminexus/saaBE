package com.saa.model.cxp;

import java.io.Serializable;

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
 * Entity PathNegociacion.
 * Almacena los paths de los documentos digitalizados asociados a una negociacion
 * con proveedor (contratos, adendums escaneados, anexos, etc.).
 * Tabla: PGS.PTNG
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PTNG", schema = "PGS")
@SequenceGenerator(name = "SQ_PTNGCDGO", sequenceName = "PGS.SQ_PTNGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PathNegociacionAll", query = "select e from PathNegociacion e"),
    @NamedQuery(name = "PathNegociacionId", query = "select e from PathNegociacion e where e.id = :id")
})
public class PathNegociacion implements Serializable {

    /** Id de tabla. */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PTNGCDGO")
    private Long id;

    /** Negociacion a la que pertenece el documento. */
    @ManyToOne
    @JoinColumn(name = "NEGOCIACION", referencedColumnName = "ID")
    private NegociacionProveedor negociacion;

    /** Path o ruta del archivo digitalizado. */
    @Basic
    @Column(name = "PATH", length = 1000)
    private String path;

    /** Nombre descriptivo del documento (ej: "Contrato principal", "Adendum 1"). */
    @Basic
    @Column(name = "NOMBREDOC", length = 500)
    private String nombreDoc;

    /**
     * Tipo de documento almacenado.
     * CONTRATO, ADENDUM, ANEXO, OTRO.
     */
    @Basic
    @Column(name = "TIPODOC", length = 50)
    private String tipoDoc;

    /**
     * Indica si es el documento principal (contrato).
     * 1 = Principal, 0 = Complementario.
     */
    @Basic
    @Column(name = "PRINCIPAL")
    private Long principal;

    /**
     * Adendum al que corresponde el documento (opcional).
     * Se llena si el documento pertenece a un adendum específico.
     */
    @ManyToOne
    @JoinColumn(name = "ADENDUM", referencedColumnName = "ID")
    private AdendumNegociacion adendum;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NegociacionProveedor getNegociacion() { return negociacion; }
    public void setNegociacion(NegociacionProveedor negociacion) { this.negociacion = negociacion; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getNombreDoc() { return nombreDoc; }
    public void setNombreDoc(String nombreDoc) { this.nombreDoc = nombreDoc; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public Long getPrincipal() { return principal; }
    public void setPrincipal(Long principal) { this.principal = principal; }

    public AdendumNegociacion getAdendum() { return adendum; }
    public void setAdendum(AdendumNegociacion adendum) { this.adendum = adendum; }
}
