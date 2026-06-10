package com.saa.model.rpr;

import java.io.Serializable;
import jakarta.persistence.*;
import com.saa.model.crd.TipoAporte;

/**
 * Entidad histórica HMPR - Histórico de CPRM (Crédito Partícipes Mensual).
 * Un registro por cada combinación identificación + tipo de aporte.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HMPR", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoCPRMAll", query = "select e from HistoricoCPRM e")
})
public class HistoricoCPRM implements Serializable {

    @Id @Basic @Column(name = "HMPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Basic @Column(name = "HMPRIDPR", length = 50) private String identificacion;
    @Basic @Column(name = "HMPRTIDP", length = 50) private String tipoIdentificacion;

    /** FK al tipo de aporte (CRD.TPAP). */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Total acumulado de aportes para este tipo. */
    @Basic @Column(name = "HMPRTTL") private Double total;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long v) { this.codigo = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public TipoAporte getTipoAporte() { return tipoAporte; }
    public void setTipoAporte(TipoAporte v) { this.tipoAporte = v; }
    public Double getTotal() { return total; }
    public void setTotal(Double v) { this.total = v; }
}