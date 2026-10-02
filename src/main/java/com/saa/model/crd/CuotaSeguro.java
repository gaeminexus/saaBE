package com.saa.model.crd;

import java.io.Serializable;
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
 * Representa la tabla CRD.PSCT (CuotaSeguro): una cuota cuyo seguro escribió un documento de
 * póliza. Guarda el valor anterior para poder reversar una distribución o una póliza anulada.
 * Nunca se borra — un reverso solo graba {@code fechaReverso}. Ver
 * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} §5.2/§5.3.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PSCT", schema = "CRD")
@SequenceGenerator(name = "SQ_PSCTCDGO", sequenceName = "CRD.SQ_PSCTCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "CuotaSeguroAll", query = "select e from CuotaSeguro e"),
    @NamedQuery(name = "CuotaSeguroId",  query = "select e from CuotaSeguro e where e.codigo = :id")
})
public class CuotaSeguro implements Serializable {

    /** Código PK. */
    @Id
    @Basic
    @Column(name = "PSCTCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PSCTCDGO")
    private Long codigo;

    /** FK - documento de póliza. */
    @ManyToOne
    @JoinColumn(name = "POSGCDGO", referencedColumnName = "POSGCDGO")
    private DocumentoSeguro documento;

    /** FK - préstamo dentro del documento. */
    @ManyToOne
    @JoinColumn(name = "PSPRCDGO", referencedColumnName = "PSPRCDGO")
    private PrestamoSeguro prestamoSeguro;

    /**
     * DTPRCDGO de la cuota afectada, SIN relación ni FK a propósito (corregido 2026-10-02,
     * d51db5f7, antes de correr el sql/306: el abono a capital y el reverso de operaciones
     * BORRAN cuotas de DTPR — {@code AbonoCapitalPrestamoServiceImpl:194-203},
     * {@code ProcesoPagoPrestamoServiceImpl:1336}). Con un {@code @ManyToOne} EAGER, cargar un
     * PSCT cuya cuota ya no existe revienta con {@code EntityNotFoundException} — mismo defecto
     * (H82) que tenía {@code DetallePlazoVencido.cuota} antes de corregirse. PSCT es historia:
     * conserva el id aunque la cuota ya no exista. Buscar la cuota aparte, por este id, y
     * tolerar que no exista.
     */
    @Basic
    @Column(name = "DTPRCDGO")
    private Long idCuota;

    /** 1 DESGRAVAMEN (DTPRDSGR) · 2 SEGURO (DTPRVLSI) — {@code com.saa.rubros.CampoSeguroCuota}. */
    @Basic
    @Column(name = "PSCTCMPO")
    private Long campo;

    /** Saldo inicial de capital de la cuota usado como peso (DTPRSICP), al momento de distribuir. */
    @Basic
    @Column(name = "PSCTSICP")
    private Double saldoInicialCapital;

    /** Valor del seguro ANTES de este documento. */
    @Basic
    @Column(name = "PSCTVLAN")
    private Double valorAnterior;

    /** Valor del seguro DESPUÉS de este documento. */
    @Basic
    @Column(name = "PSCTVLNV")
    private Double valorNuevo;

    /** Cuándo se reversó (anulación). Nulo = vigente. */
    @Basic
    @Column(name = "PSCTFCRV")
    private LocalDateTime fechaReverso;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public DocumentoSeguro getDocumento() { return documento; }
    public void setDocumento(DocumentoSeguro documento) { this.documento = documento; }

    public PrestamoSeguro getPrestamoSeguro() { return prestamoSeguro; }
    public void setPrestamoSeguro(PrestamoSeguro prestamoSeguro) { this.prestamoSeguro = prestamoSeguro; }

    public Long getIdCuota() { return idCuota; }
    public void setIdCuota(Long idCuota) { this.idCuota = idCuota; }

    public Long getCampo() { return campo; }
    public void setCampo(Long campo) { this.campo = campo; }

    public Double getSaldoInicialCapital() { return saldoInicialCapital; }
    public void setSaldoInicialCapital(Double saldoInicialCapital) { this.saldoInicialCapital = saldoInicialCapital; }

    public Double getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(Double valorAnterior) { this.valorAnterior = valorAnterior; }

    public Double getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(Double valorNuevo) { this.valorNuevo = valorNuevo; }

    public LocalDateTime getFechaReverso() { return fechaReverso; }
    public void setFechaReverso(LocalDateTime fechaReverso) { this.fechaReverso = fechaReverso; }
}
