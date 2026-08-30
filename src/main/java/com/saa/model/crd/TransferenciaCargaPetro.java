package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.tsr.Banco;
import com.saa.model.tsr.BancoExterno;
import com.saa.model.tsr.CuentaBancaria;

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
 * Representa la tabla CRD.TRCR (TransferenciaCargaPetro).
 *
 * Una fila por transferencia con la que Petro/ARCH pagó una carga: "Petro puede pagar con
 * más de 1 transferencia" (pizarra), así que una carga tiene N filas. La suma de
 * {@link #valor} de una carga alimenta el Debe del asiento TRANSITORIO del paso 1
 * (§3.3 y §5.11 de LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md) — el servicio la valida
 * contra el total cobrado del archivo antes de contabilizar; la base no puede garantizarlo
 * con un CHECK porque involucra otra tabla.
 *
 * <b>{@code cuentaBancaria}/{@code banco}/{@code bancoExterno} son FK a TSR</b>: dirección
 * {@code crd -> tsr}, la única permitida (el sistema se comercializa sin crd).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "TRCR", schema = "CRD")
@SequenceGenerator(name = "SQ_TRCRCDGO", sequenceName = "CRD.SQ_TRCRCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "TransferenciaCargaPetroAll",
                query = "select e from TransferenciaCargaPetro e"),
    @NamedQuery(name = "TransferenciaCargaPetroId",
                query = "select e from TransferenciaCargaPetro e where e.codigo = :id")
})
public class TransferenciaCargaPetro implements Serializable, EntidadAuditableFechaHora {

    /** Código de la fila. PK. */
    @Id
    @Basic
    @Column(name = "TRCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_TRCRCDGO")
    private Long codigo;

    /** FK - Carga Petro a la que pertenece esta transferencia. */
    @ManyToOne
    @JoinColumn(name = "CRARCDGO", referencedColumnName = "CRARCDGO")
    private CargaArchivo cargaArchivo;

    /** FK TSR.CNBC - Cuenta bancaria DESTINO (la de la empresa) donde entró el dinero. */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /** FK TSR.BNCO - Banco de la cuenta destino. */
    @ManyToOne
    @JoinColumn(name = "BNCOCDGO", referencedColumnName = "BNCOCDGO")
    private Banco banco;

    /** FK TSR.BEXT - Banco externo de origen (de Petro/ARCH). */
    @ManyToOne
    @JoinColumn(name = "BEXTCDGO", referencedColumnName = "BEXTCDGO")
    private BancoExterno bancoExterno;

    /** Cuenta origen de la transferencia (texto libre, igual que TSR.CTRN). */
    @Basic
    @Column(name = "TRCRCTOR", length = 50)
    private String cuentaOrigen;

    /** Número o referencia de la transferencia. */
    @Basic
    @Column(name = "TRCRNMRO", length = 50)
    private String numero;

    /** Valor recibido en ESTA transferencia. */
    @Basic
    @Column(name = "TRCRVLRR")
    private Double valor;

    /** Fecha en que el dinero entró al banco. Es la fecha del asiento del paso 1. */
    @Basic
    @Column(name = "TRCRFCHA")
    private LocalDate fecha;

    /** Observación libre. */
    @Basic
    @Column(name = "TRCROBSR", length = 2000)
    private String observacion;

    /** Estado OPERATIVO: 1 = activa, 0 = anulada. */
    @Basic
    @Column(name = "TRCRIDST")
    private Long idEstado;

    /** Usuario que registró. */
    @Basic
    @Column(name = "TRCRUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "TRCRFCRG")
    private LocalDateTime fechaRegistro;

    /** IP desde la que se registró. */
    @Basic
    @Column(name = "TRCRIPRG", length = 50)
    private String ipRegistro;

    public TransferenciaCargaPetro() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public CargaArchivo getCargaArchivo() {
        return cargaArchivo;
    }

    public void setCargaArchivo(CargaArchivo cargaArchivo) {
        this.cargaArchivo = cargaArchivo;
    }

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public Banco getBanco() {
        return banco;
    }

    public void setBanco(Banco banco) {
        this.banco = banco;
    }

    public BancoExterno getBancoExterno() {
        return bancoExterno;
    }

    public void setBancoExterno(BancoExterno bancoExterno) {
        this.bancoExterno = bancoExterno;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    @Override
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getIpRegistro() {
        return ipRegistro;
    }

    public void setIpRegistro(String ipRegistro) {
        this.ipRegistro = ipRegistro;
    }
}
