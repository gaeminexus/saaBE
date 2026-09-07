package com.saa.model.rhh;

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
import jakarta.persistence.Table;

/**
 * Renglon de una {@link PlanillaIess}: un concepto del comprobante que emite
 * el portal del IESS, con su valor y el de nuestro control (si el tipo de
 * planilla lo tiene). Tabla: RHH.DLIS.
 *
 * <p>Es donde vive el valor real de la conciliacion: el total de la planilla
 * puede cuadrar y estar mal por dentro.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DLIS", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetallePlanillaIessId", query = "select e from DetallePlanillaIess e where e.codigo=:id"),
    @NamedQuery(name = "DetallePlanillaIessAll", query = "select e from DetallePlanillaIess e")
})
public class DetallePlanillaIess implements Serializable {

    /**
     * Codigo unico del renglon.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DLISCDGO")
    private Long codigo;

    /**
     * Planilla a la que pertenece el renglon.
     */
    @ManyToOne
    @JoinColumn(name = "PLISCDGO", referencedColumnName = "PLISCDGO")
    private PlanillaIess planilla;

    /**
     * Texto del renglon tal como lo trae el comprobante del portal.
     */
    @Basic
    @Column(name = "DLISCNCP", length = 200)
    private String concepto;

    /**
     * Concepto normalizado del renglon: detalle del rubro
     * RHH_CONCEPTO_PLANILLA_IESS (331). Ver
     * {@link com.saa.rubros.RhhConceptoPlanillaIess}. Nulo significa "sin
     * clasificar" -- es lo que decide contra que total de la planilla de
     * control se compara este renglon, en vez de adivinarlo por el texto
     * libre de {@link #concepto}.
     */
    @Basic
    @Column(name = "DLISCNCT")
    private Long conceptoTipo;

    /**
     * Valor del renglon segun el IESS.
     */
    @Basic
    @Column(name = "DLISVLIS")
    private Double valorIess;

    /**
     * Valor del renglon segun nuestro control. Nulo si el concepto no tiene
     * contraparte calculada (ver docs/logica-negocio/rhh/API-PLANILLA-IESS.md
     * #5.2): no se inventa.
     */
    @Basic
    @Column(name = "DLISVLCT")
    private Double valorControl;

    /**
     * valorIess - valorControl. Nulo si valorControl es nulo.
     */
    @Basic
    @Column(name = "DLISDIFR")
    private Double diferencia;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public PlanillaIess getPlanilla() {
        return planilla;
    }

    public void setPlanilla(PlanillaIess planilla) {
        this.planilla = planilla;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Long getConceptoTipo() {
        return conceptoTipo;
    }

    public void setConceptoTipo(Long conceptoTipo) {
        this.conceptoTipo = conceptoTipo;
    }

    public Double getValorIess() {
        return valorIess;
    }

    public void setValorIess(Double valorIess) {
        this.valorIess = valorIess;
    }

    public Double getValorControl() {
        return valorControl;
    }

    public void setValorControl(Double valorControl) {
        this.valorControl = valorControl;
    }

    public Double getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(Double diferencia) {
        this.diferencia = diferencia;
    }
}
