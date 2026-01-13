package com.saa.model.credito;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

	/**
	 * Representa la tabla TRAS (Transacciones).
	 */
	@SuppressWarnings("serial")
	@Entity
	@Table(name = "TRAS", schema = "CRD")
	@NamedQueries({
	    @NamedQuery(name = "TransaccionesAll", query = "select t from Transacciones t"),
	    @NamedQuery(name = "TransaccionesById", query = "select t from Transacciones t where t.codigo = :id")
	})
	public class Transacciones implements Serializable {

	    // ============================================================
	    // PK
	    // ============================================================

	    @Id
	    @Basic
	    @Column(name = "TRASTRRR")
	    private Long codigo;

	    // ============================================================
	    // CAMPOS
	    // ============================================================

	    /** Id de la cuenta de la transacción */
	    @Basic
	    @Column(name = "TRASCNTA")
	    private Long cuentaId;

	    /** Concepto de la transacción */
	    @Basic
	    @Column(name = "TRASCNCP", length = 150)
	    private String concepto;

	    /** Saldo anterior */
	    @Basic
	    @Column(name = "TRASSLDN", precision = 12, scale = 2)
	    private BigDecimal saldoAnterior;

	    /** Total débito */
	    @Basic
	    @Column(name = "TRASTTLD", precision = 12, scale = 2)
	    private BigDecimal totalDebito;

	    /** Total crédito */
	    @Basic
	    @Column(name = "TRASTTLC", precision = 12, scale = 2)
	    private BigDecimal totalCredito;

	    /** Saldo actual */
	    @Basic
	    @Column(name = "TRASSLDA", precision = 12, scale = 2)
	    private BigDecimal saldoActual;

	    /** Saldo calculado */
	    @Basic
	    @Column(name = "TRASSLDC", precision = 12, scale = 2)
	    private BigDecimal saldoCalculado;

	    /** Diferencia */
	    @Basic
	    @Column(name = "TRASDFRN", precision = 12, scale = 2)
	    private BigDecimal diferencia;

	    /** Fecha del depósito */
	    @Basic
	    @Column(name = "TRASFCHD")
	    private LocalDateTime fechaDeposito;

	    /** Fecha del aporte */
	    @Basic
	    @Column(name = "TRASFCHP")
	    private LocalDateTime fechaAporte;

	    /** Débito */
	    @Basic
	    @Column(name = "TRASDBBB", precision = 12, scale = 2)
	    private BigDecimal debito;

	    /** Crédito */
	    @Basic
	    @Column(name = "TRASCRRR", precision = 12, scale = 2)
	    private BigDecimal credito;

	    // ============================================================
	    // GETTERS Y SETTERS
	    // ============================================================

	    public Long getCodigo() {
	        return codigo;
	    }

	    public void setCodigo(Long codigo) {
	        this.codigo = codigo;
	    }

	    public Long getCuentaId() {
	        return cuentaId;
	    }

	    public void setCuentaId(Long cuentaId) {
	        this.cuentaId = cuentaId;
	    }

	    public String getConcepto() {
	        return concepto;
	    }

	    public void setConcepto(String concepto) {
	        this.concepto = concepto;
	    }

	    public BigDecimal getSaldoAnterior() {
	        return saldoAnterior;
	    }

	    public void setSaldoAnterior(BigDecimal saldoAnterior) {
	        this.saldoAnterior = saldoAnterior;
	    }

	    public BigDecimal getTotalDebito() {
	        return totalDebito;
	    }

	    public void setTotalDebito(BigDecimal totalDebito) {
	        this.totalDebito = totalDebito;
	    }

	    public BigDecimal getTotalCredito() {
	        return totalCredito;
	    }

	    public void setTotalCredito(BigDecimal totalCredito) {
	        this.totalCredito = totalCredito;
	    }

	    public BigDecimal getSaldoActual() {
	        return saldoActual;
	    }

	    public void setSaldoActual(BigDecimal saldoActual) {
	        this.saldoActual = saldoActual;
	    }

	    public BigDecimal getSaldoCalculado() {
	        return saldoCalculado;
	    }

	    public void setSaldoCalculado(BigDecimal saldoCalculado) {
	        this.saldoCalculado = saldoCalculado;
	    }

	    public BigDecimal getDiferencia() {
	        return diferencia;
	    }

	    public void setDiferencia(BigDecimal diferencia) {
	        this.diferencia = diferencia;
	    }

	    public LocalDateTime getFechaDeposito() {
	        return fechaDeposito;
	    }

	    public void setFechaDeposito(LocalDateTime fechaDeposito) {
	        this.fechaDeposito = fechaDeposito;
	    }

	    public LocalDateTime getFechaAporte() {
	        return fechaAporte;
	    }

	    public void setFechaAporte(LocalDateTime fechaAporte) {
	        this.fechaAporte = fechaAporte;
	    }

	    public BigDecimal getDebito() {
	        return debito;
	    }

	    public void setDebito(BigDecimal debito) {
	        this.debito = debito;
	    }

	    public BigDecimal getCredito() {
	        return credito;
	    }

	    public void setCredito(BigDecimal credito) {
	        this.credito = credito;
	    }
	

}
