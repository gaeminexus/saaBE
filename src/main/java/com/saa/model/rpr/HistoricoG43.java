package com.saa.model.rpr;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "HM43", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG43All", query = "select e from HistoricoG43 e")
})
public class HistoricoG43 implements Serializable {

    @Id @Basic @Column(name = "HM43IDPR", length = 50)  private String identificacion;
    @Basic @Column(name = "HM43TIDP", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM43FTRL", length = 100) private String fechaTerminoRelacionLaboral;
    @Basic @Column(name = "HM43NIAP")               private Long numeroImposicionesPersonales;
    @Basic @Column(name = "HM43NIAT")               private Long numeroImposicionesPatronales;
    @Basic @Column(name = "HM43FCLQ", length = 100) private String fechaLiquidacion;
    @Basic @Column(name = "HM43SDCI")               private Double saldoCuentaIndividual;
    @Basic @Column(name = "HM43VCAP")               private Double valoresCompensados;
    @Basic @Column(name = "HM43VPAP")               private Double valoresPagados;

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getFechaTerminoRelacionLaboral() { return fechaTerminoRelacionLaboral; }
    public void setFechaTerminoRelacionLaboral(String v) { this.fechaTerminoRelacionLaboral = v; }
    public Long getNumeroImposicionesPersonales() { return numeroImposicionesPersonales; }
    public void setNumeroImposicionesPersonales(Long v) { this.numeroImposicionesPersonales = v; }
    public Long getNumeroImposicionesPatronales() { return numeroImposicionesPatronales; }
    public void setNumeroImposicionesPatronales(Long v) { this.numeroImposicionesPatronales = v; }
    public String getFechaLiquidacion() { return fechaLiquidacion; }
    public void setFechaLiquidacion(String v) { this.fechaLiquidacion = v; }
    public Double getSaldoCuentaIndividual() { return saldoCuentaIndividual; }
    public void setSaldoCuentaIndividual(Double v) { this.saldoCuentaIndividual = v; }
    public Double getValoresCompensados() { return valoresCompensados; }
    public void setValoresCompensados(Double v) { this.valoresCompensados = v; }
    public Double getValoresPagados() { return valoresPagados; }
    public void setValoresPagados(Double v) { this.valoresPagados = v; }
}
