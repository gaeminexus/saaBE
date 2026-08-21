package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Detalle por empleado de la orden de pago de nomina.
 *
 * <p><b>Los cinco campos de snapshot</b> —numero de cuenta, tipo de cuenta, banco,
 * identificacion y nombre del beneficiario— se copian del <code>CBEM</code> vigente al generar
 * la orden y <b>no se releen nunca</b>. Son la constancia de a que cuenta se ordeno pagar,
 * aunque el empleado cambie de banco despues. Es el mismo criterio de los snapshot de
 * <code>RNGL</code>.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DRPG", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleOrdenPagoNominaId",  query = "select e from DetalleOrdenPagoNomina e where e.codigo=:id"),
    @NamedQuery(name = "DetalleOrdenPagoNominaAll", query = "select e from DetalleOrdenPagoNomina e")
})
public class DetalleOrdenPagoNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del detalle.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DRPGCDGO")
    private Long codigo;

    /**
     * Orden de pago a la que pertenece.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "RDPGCDGO", referencedColumnName = "RDPGCDGO", nullable = false)
    private OrdenPagoNomina ordenPagoNomina;

    /**
     * Empleado beneficiario.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Nomina de la que sale el neto.
     */
    @ManyToOne
    @JoinColumn(name = "NMNACDGO", referencedColumnName = "NMNACDGO")
    private Nomina nomina;

    /**
     * Cuenta bancaria del empleado a la que se acredita.
     */
    @ManyToOne
    @JoinColumn(name = "CBEMCDGO", referencedColumnName = "CBEMCDGO")
    private CuentaBancariaEmpleado cuentaBancariaEmpleado;

    /**
     * Valor a acreditar. Obligatorio.
     */
    @Basic
    @Column(name = "DRPGVLOR", nullable = false)
    private Double valor;

    /**
     * Numero de cuenta destino. Snapshot al momento de emitir.
     */
    @Basic
    @Column(name = "DRPGNMCT", length = 30)
    private String numeroCuenta;

    /**
     * Tipo de cuenta destino: detalle del rubro RHH_TIPO_CUENTA_BANCARIA. Snapshot.
     */
    @Basic
    @Column(name = "DRPGTPCT")
    private Long tipoCuenta;

    /**
     * Banco destino. Snapshot.
     */
    @Basic
    @Column(name = "DRPGBNCO", length = 100)
    private String banco;

    /**
     * Identificacion del beneficiario. Snapshot.
     */
    @Basic
    @Column(name = "DRPGIDNT", length = 20)
    private String identificacion;

    /**
     * Nombre del beneficiario. Snapshot.
     */
    @Basic
    @Column(name = "DRPGNMBN", length = 200)
    private String nombreBeneficiario;

    /**
     * Fue rechazado por el banco (S/N).
     */
    @Basic
    @Column(name = "DRPGRCHZ", length = 1)
    private String rechazado;

    /**
     * Motivo del rechazo.
     */
    @Basic
    @Column(name = "DRPGMTRC", length = 300)
    private String motivoRechazo;

    /**
     * Estado del detalle.
     */
    @Basic
    @Column(name = "DRPGESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DRPGFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "DRPGUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public OrdenPagoNomina getOrdenPagoNomina() {
        return ordenPagoNomina;
    }

    public void setOrdenPagoNomina(OrdenPagoNomina ordenPagoNomina) {
        this.ordenPagoNomina = ordenPagoNomina;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Nomina getNomina() {
        return nomina;
    }

    public void setNomina(Nomina nomina) {
        this.nomina = nomina;
    }

    public CuentaBancariaEmpleado getCuentaBancariaEmpleado() {
        return cuentaBancariaEmpleado;
    }

    public void setCuentaBancariaEmpleado(CuentaBancariaEmpleado cuentaBancariaEmpleado) {
        this.cuentaBancariaEmpleado = cuentaBancariaEmpleado;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public Long getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(Long tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public String getNombreBeneficiario() {
        return nombreBeneficiario;
    }

    public void setNombreBeneficiario(String nombreBeneficiario) {
        this.nombreBeneficiario = nombreBeneficiario;
    }

    public String getRechazado() {
        return rechazado;
    }

    public void setRechazado(String rechazado) {
        this.rechazado = rechazado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
}
