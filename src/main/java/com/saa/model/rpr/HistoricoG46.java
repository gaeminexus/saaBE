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
@Table(name = "HM46", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "HistoricoG46All", query = "select e from HistoricoG46 e")
})
public class HistoricoG46 implements Serializable {

    @Id @Basic @Column(name = "HM46NMOP", length = 50)  private String numeroOperacion;
    @Basic @Column(name = "HM46TIDS", length = 50)  private String tipoIdentificacion;
    @Basic @Column(name = "HM46IDSJ", length = 50)  private String identificacion;
    @Basic @Column(name = "HM46TDCP", length = 50)  private String tipoCredito;
    @Basic @Column(name = "HM46EDLO", length = 50)  private String estadoOperacion;
    @Basic @Column(name = "HM46SDLO", length = 50)  private String situacionOperacion;
    @Basic @Column(name = "HM46DGPR", length = 100) private String destinoProvincia;
    @Basic @Column(name = "HM46DGCN", length = 100) private String destinoCanton;
    @Basic @Column(name = "HM46DGPS", length = 100) private String destinoParroquia;
    @Basic @Column(name = "HM46FCCN", length = 100) private String fechaConcesion;
    @Basic @Column(name = "HM46FCVN", length = 100) private String fechaVencimiento;
    @Basic @Column(name = "HM46VDLO")               private Double valorOperacion;
    @Basic @Column(name = "HM46TDIN")               private Double tasaInteresNominal;
    @Basic @Column(name = "HM46PDDP", length = 50)  private String periodicidadPago;
    @Basic @Column(name = "HM46FDRV", length = 50)  private String frecuenciaRevision;
    @Basic @Column(name = "HM46GOGR", length = 200) private String garantesGarantias;

    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { this.numeroOperacion = v; }
    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String v) { this.tipoIdentificacion = v; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String v) { this.identificacion = v; }
    public String getTipoCredito() { return tipoCredito; }
    public void setTipoCredito(String v) { this.tipoCredito = v; }
    public String getEstadoOperacion() { return estadoOperacion; }
    public void setEstadoOperacion(String v) { this.estadoOperacion = v; }
    public String getSituacionOperacion() { return situacionOperacion; }
    public void setSituacionOperacion(String v) { this.situacionOperacion = v; }
    public String getDestinoProvincia() { return destinoProvincia; }
    public void setDestinoProvincia(String v) { this.destinoProvincia = v; }
    public String getDestinoCanton() { return destinoCanton; }
    public void setDestinoCanton(String v) { this.destinoCanton = v; }
    public String getDestinoParroquia() { return destinoParroquia; }
    public void setDestinoParroquia(String v) { this.destinoParroquia = v; }
    public String getFechaConcesion() { return fechaConcesion; }
    public void setFechaConcesion(String v) { this.fechaConcesion = v; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String v) { this.fechaVencimiento = v; }
    public Double getValorOperacion() { return valorOperacion; }
    public void setValorOperacion(Double v) { this.valorOperacion = v; }
    public Double getTasaInteresNominal() { return tasaInteresNominal; }
    public void setTasaInteresNominal(Double v) { this.tasaInteresNominal = v; }
    public String getPeriodicidadPago() { return periodicidadPago; }
    public void setPeriodicidadPago(String v) { this.periodicidadPago = v; }
    public String getFrecuenciaRevision() { return frecuenciaRevision; }
    public void setFrecuenciaRevision(String v) { this.frecuenciaRevision = v; }
    public String getGarantesGarantias() { return garantesGarantias; }
    public void setGarantesGarantias(String v) { this.garantesGarantias = v; }
}
