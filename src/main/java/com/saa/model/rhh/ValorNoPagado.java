package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.scp.Empresa;

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
 * Valor no pagado a un empleado en un período de nómina: se retiene en el período en que se
 * registra y se devuelve completo en el período siguiente. Tabla: RHH.VNPG.
 *
 * <p>Es un "anticipo al revés": el anticipo es plata que sale antes y el rol la recupera; esto
 * es plata que se retiene en un mes y el pago la devuelve en el siguiente. La contabilidad del
 * rol no se entera (devenga el neto completo); sólo cambia cuánto se paga, vía dos renglones
 * INFORMATIVOS (rol de motor 34/35, {@link com.saa.rubros.RhhRolConceptoMotor}) que no entran al
 * neto ni a la contabilidad. Ver docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md.</p>
 *
 * <p><b>Ojo con las dos FK a órdenes de pago:</b> {@link #ordenRetencion} es la orden del período
 * en que se retuvo (P), {@link #ordenPago} es la orden del período en que se devolvió (P+1) — son
 * dos órdenes distintas de {@link OrdenPagoNomina}, nunca la misma fila.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "VNPG", schema = "RHH")
@SequenceGenerator(name = "SQ_VNPGCDGO", sequenceName = "RHH.SQ_VNPGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "ValorNoPagadoId",  query = "select e from ValorNoPagado e where e.codigo=:id"),
    @NamedQuery(name = "ValorNoPagadoAll", query = "select e from ValorNoPagado e")
})
public class ValorNoPagado implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del registro.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_VNPGCDGO")
    @Basic
    @Column(name = "VNPGCDGO")
    private Long codigo;

    /**
     * Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Empleado al que no se le pagó el valor.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Período en que NO se paga (P): el que lo retiene. Distinto de {@link #periodoRecuperacion}.
     */
    @ManyToOne
    @JoinColumn(name = "VNPGPRNM", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoNomina;

    /**
     * Valor X, siempre positivo. El signo (−X en P, +X en P+1) lo pone el renglón del rol, no
     * esta columna.
     */
    @Basic
    @Column(name = "VNPGVLOR")
    private Double valor;

    /**
     * Motivo del registro. Obligatorio (lo exige el service, no una constraint de negocio en la
     * lectura de este campo).
     */
    @Basic
    @Column(name = "VNPGMTVO", length = 500)
    private String motivo;

    /**
     * Estado: detalle del rubro RHH_ESTADO_VALOR_NO_PAGADO (ver
     * {@link com.saa.rubros.RhhEstadoValorNoPagado}).
     */
    @Basic
    @Column(name = "VNPGESTD")
    private Long estado;

    /**
     * Período en que se recuperó (P+1). Null hasta que se confirma el pago que lo devuelve.
     */
    @ManyToOne
    @JoinColumn(name = "VNPGPRRC", referencedColumnName = "PRDNCDGO")
    private PeriodoNomina periodoRecuperacion;

    /**
     * Orden de pago de nómina que lo RETUVO (la del período P). Null hasta que se genera esa
     * orden.
     */
    @ManyToOne
    @JoinColumn(name = "VNPGORRT", referencedColumnName = "RDPGCDGO")
    private OrdenPagoNomina ordenRetencion;

    /**
     * Orden de pago de nómina que lo DEVOLVIÓ (la del período P+1). Null hasta que se confirma
     * el pago de esa orden.
     */
    @ManyToOne
    @JoinColumn(name = "VNPGORPG", referencedColumnName = "RDPGCDGO")
    private OrdenPagoNomina ordenPago;

    /**
     * Liquidación de haberes (finiquito) que absorbió este valor, si el empleado salió antes de
     * cobrarlo. Null salvo que el estado sea FINIQUITADO.
     */
    @ManyToOne
    @JoinColumn(name = "VNPGLQDC", referencedColumnName = "LQDCCDGO")
    private Liquidacion liquidacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "VNPGFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "VNPGUSRR", length = 60)
    private String usuarioRegistro;

    /**
     * Motivo de la anulación. Null salvo que el estado sea ANULADO.
     */
    @Basic
    @Column(name = "VNPGMTAN", length = 500)
    private String motivoAnulacion;

    /**
     * Fecha de la anulación.
     */
    @Basic
    @Column(name = "VNPGFCAN")
    private LocalDateTime fechaAnulacion;

    /**
     * Usuario que anuló.
     */
    @Basic
    @Column(name = "VNPGUSAN", length = 60)
    private String usuarioAnulacion;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public PeriodoNomina getPeriodoNomina() {
        return periodoNomina;
    }

    public void setPeriodoNomina(PeriodoNomina periodoNomina) {
        this.periodoNomina = periodoNomina;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public PeriodoNomina getPeriodoRecuperacion() {
        return periodoRecuperacion;
    }

    public void setPeriodoRecuperacion(PeriodoNomina periodoRecuperacion) {
        this.periodoRecuperacion = periodoRecuperacion;
    }

    public OrdenPagoNomina getOrdenRetencion() {
        return ordenRetencion;
    }

    public void setOrdenRetencion(OrdenPagoNomina ordenRetencion) {
        this.ordenRetencion = ordenRetencion;
    }

    public OrdenPagoNomina getOrdenPago() {
        return ordenPago;
    }

    public void setOrdenPago(OrdenPagoNomina ordenPago) {
        this.ordenPago = ordenPago;
    }

    public Liquidacion getLiquidacion() {
        return liquidacion;
    }

    public void setLiquidacion(Liquidacion liquidacion) {
        this.liquidacion = liquidacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(String usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }
}
