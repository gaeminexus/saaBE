package com.saa.model.rpr;

import java.io.Serializable;
import java.time.LocalDate;

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
 * Entidad que representa la tabla RPR.CJBM - CJBM (Crédito Jubilados Mensual).
 * Similar al G44 pero para uso interno del departamento de cartera.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CJBM", schema = "RPR")
@NamedQueries({
    @NamedQuery(name = "CreditoJubiladosMensualAll", query = "select e from CreditoJubiladosMensual e"),
    @NamedQuery(name = "CreditoJubiladosMensualId",  query = "select e from CreditoJubiladosMensual e where e.codigo = :id")
})
public class CreditoJubiladosMensual implements Serializable {

    /** Código único del registro (Identity). */
    @Id
    @Basic
    @Column(name = "CJBMCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Tipo de identificación del jubilado. */
    @Basic
    @Column(name = "CJBMTIDJ", length = 50)
    private String tipoIdentificacion;

    /** Identificación del jubilado. */
    @Basic
    @Column(name = "CJBMIDJB", length = 50)
    private String identificacion;

    /** Tipo de jubilación. */
    @Basic
    @Column(name = "CJBMTPJB", length = 50)
    private String tipoJubilacion;

    /** Fecha de jubilación. */
    @Basic
    @Column(name = "CJBMFCJB")
    private LocalDate fechaJubilacion;

    /** Imposiciones acumuladas por jubilación. */
    @Basic
    @Column(name = "CJBMIAJB")
    private Long imposicionesAcumuladas;

    /** Valor de la pensión. */
    @Basic
    @Column(name = "CJBMVLPN")
    private Double valorPension;

    /** Valor neto a recibir. */
    @Basic
    @Column(name = "CJBMVNAR")
    private Double valorNetoRecibir;

    /** Saldo de cuenta del jubilado. */
    @Basic
    @Column(name = "CJBMSCJB")
    private Double saldoCuenta;

    /** Valores compensados al partícipe. */
    @Basic
    @Column(name = "CJBMVCAP")
    private Double valoresCompensados;

    /** Jubilación en el IESS. */
    @Basic
    @Column(name = "CJBMJEIS", length = 50)
    private String jubilacionIess;

    /** Valor de jubilación (valorPagar de VPPC). */
    @Basic
    @Column(name = "CJBMVLJB")
    private Double valorJubilacion;

    /** Valor del seguro (valorSeguro de VPPC). */
    @Basic
    @Column(name = "CJBMVLSG")
    private Double valorSeguro;

    /** FK al control de ejecución (RPR.EJCC) al que pertenece este registro. */
    @ManyToOne
    @JoinColumn(name = "CJBMEJCC", referencedColumnName = "EJCCCDGO")
    private EjecucionReporteCartera ejecucionReporte;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getTipoJubilacion() { return tipoJubilacion; }
    public void setTipoJubilacion(String tipoJubilacion) { this.tipoJubilacion = tipoJubilacion; }

    public LocalDate getFechaJubilacion() { return fechaJubilacion; }
    public void setFechaJubilacion(LocalDate fechaJubilacion) { this.fechaJubilacion = fechaJubilacion; }

    public Long getImposicionesAcumuladas() { return imposicionesAcumuladas; }
    public void setImposicionesAcumuladas(Long imposicionesAcumuladas) { this.imposicionesAcumuladas = imposicionesAcumuladas; }

    public Double getValorPension() { return valorPension; }
    public void setValorPension(Double valorPension) { this.valorPension = valorPension; }

    public Double getValorNetoRecibir() { return valorNetoRecibir; }
    public void setValorNetoRecibir(Double valorNetoRecibir) { this.valorNetoRecibir = valorNetoRecibir; }

    public Double getSaldoCuenta() { return saldoCuenta; }
    public void setSaldoCuenta(Double saldoCuenta) { this.saldoCuenta = saldoCuenta; }

    public Double getValoresCompensados() { return valoresCompensados; }
    public void setValoresCompensados(Double valoresCompensados) { this.valoresCompensados = valoresCompensados; }

    public String getJubilacionIess() { return jubilacionIess; }
    public void setJubilacionIess(String jubilacionIess) { this.jubilacionIess = jubilacionIess; }

    public Double getValorJubilacion() { return valorJubilacion; }
    public void setValorJubilacion(Double valorJubilacion) { this.valorJubilacion = valorJubilacion; }

    public Double getValorSeguro() { return valorSeguro; }
    public void setValorSeguro(Double valorSeguro) { this.valorSeguro = valorSeguro; }

    public EjecucionReporteCartera getEjecucionReporte() { return ejecucionReporte; }
    public void setEjecucionReporte(EjecucionReporteCartera ejecucionReporte) { this.ejecucionReporte = ejecucionReporte; }
}
