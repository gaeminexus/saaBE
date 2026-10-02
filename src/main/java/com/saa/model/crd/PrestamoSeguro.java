package com.saa.model.crd;

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
 * Representa la tabla CRD.PSPR (PrestamoSeguro): un préstamo dentro de un documento de póliza —
 * la foto de la base que se envió a la aseguradora y lo que le tocó del valor total. Ver
 * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} §5.2/§5.3.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PSPR", schema = "CRD")
@SequenceGenerator(name = "SQ_PSPRCDGO", sequenceName = "CRD.SQ_PSPRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "PrestamoSeguroAll", query = "select e from PrestamoSeguro e"),
    @NamedQuery(name = "PrestamoSeguroId",  query = "select e from PrestamoSeguro e where e.codigo = :id")
})
public class PrestamoSeguro implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "PSPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PSPRCDGO")
    private Long codigo;

    /** FK - documento de póliza. */
    @ManyToOne
    @JoinColumn(name = "POSGCDGO", referencedColumnName = "POSGCDGO")
    private DocumentoSeguro documento;

    /** FK - préstamo incluido. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** 1 ORIGINAL · 2 INCLUSION · 3 EXCLUSION — {@code com.saa.rubros.NovedadPrestamoSeguro}. */
    @Basic
    @Column(name = "PSPRNVDD")
    private Long novedad;

    /** Base enviada: saldo de capital (desgravamen) o suma asegurada (incendio/prendario). */
    @Basic
    @Column(name = "PSPRBASE")
    private Double base;

    /** Cuotas/meses cubiertos dentro de la vigencia. */
    @Basic
    @Column(name = "PSPRMSCB")
    private Long mesesCubiertos;

    /** Peso = base × mesesCubiertos / mesesVigencia. */
    @Basic
    @Column(name = "PSPRPESO")
    private Double peso;

    /** Valor asignado del documento a este préstamo. */
    @Basic
    @Column(name = "PSPRVLAS")
    private Double valorAsignado;

    /** Cuotas en que se repartió el valor asignado. */
    @Basic
    @Column(name = "PSPRNCTS")
    private Long cuotasRepartidas;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public DocumentoSeguro getDocumento() { return documento; }
    public void setDocumento(DocumentoSeguro documento) { this.documento = documento; }

    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }

    public Long getNovedad() { return novedad; }
    public void setNovedad(Long novedad) { this.novedad = novedad; }

    public Double getBase() { return base; }
    public void setBase(Double base) { this.base = base; }

    public Long getMesesCubiertos() { return mesesCubiertos; }
    public void setMesesCubiertos(Long mesesCubiertos) { this.mesesCubiertos = mesesCubiertos; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getValorAsignado() { return valorAsignado; }
    public void setValorAsignado(Double valorAsignado) { this.valorAsignado = valorAsignado; }

    public Long getCuotasRepartidas() { return cuotasRepartidas; }
    public void setCuotasRepartidas(Long cuotasRepartidas) { this.cuotasRepartidas = cuotasRepartidas; }
}
