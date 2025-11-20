package com.saa.model.contabilidad;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "RCNC", schema = "CNT")
@SequenceGenerator(name = "SQ_RCNCCDGO", sequenceName = "CNT.SQ_RCNCCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "ReporteCuentaCCAll", query = "select e from ReporteCuentaCC e"),
	@NamedQuery(name = "ReporteCuentaCCId", query = "select e from ReporteCuentaCC e where e.codigo = :id")
})
public class ReporteCuentaCC implements Serializable {

		/**
		 * Id de la tabla.
		 */
		@Basic
		@Id
		@Column(name = "RCNCCDGO", precision = 0)
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_RCNCCDGO")	
		private Long codigo;
		
		/**
		 * Id de la cuenta contable.
		 */
		@ManyToOne
		@JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
		private PlanCuenta planCuenta;
		
		/**
		 * Nombre de la cuenta.
		 */
		@Basic
		@Column(name = "RCNCNMBR", length = 200)
		private String nombreCuenta;
		
		/**
		 * Numero de la cuenta.
		 */
		@Basic
		@Column(name = "RCNCNMRO", length = 50)
		private String numeroCuenta;
		
		/**
		 * Secuencial del reporte.
		 */
		@Basic
		@Column(name = "RCNCSCNC")
		private Long secuencia;
		
		/**
		 * Saldo anterior.
		 */
		@Basic
		@Column(name = "RCNCSLAN")
		private Long saldoAnterio;
		
		/**
		 * Debe.
		 */
		@Basic
		@Column(name = "RCNCDBEE")
		private Long debe;
		
		/**
		 * Haber. 
		 */
		@Basic
		@Column(name = "RCNCHBRR")
		private Long haber;
		
		/**
		 * Saldo Actual.
		 */
		@Basic
		@Column(name = "RCNCSLAC")
		private Long saldoActual;	
		

		/**
		 * Metodo que recupera los valores de la variable Codigo
		 * @return codigo.
		 */
		public Long getCodigo() {
			return codigo;
		}

		/**
		 * Metodo que asigna los valores de la variable codigo
		 * @param codigo nuevo valor para codigo. 
		 */
		public void setCodigo(Long codigo) {
			this.codigo = codigo;
		}
		
		/**
		 * Metodo que recupera los valores de la variable Plan de Cuentas
		 * @return planCuenta.
		 */
		public PlanCuenta getPlanCuenta() {
			return planCuenta;
		}

		/**
		 * Metodo que asigna los valores de la variable Plan de Cuentas
		 * @param planCuenta nuevo valor para planCuenta. 
		 */
		public void setPlanCuenta(PlanCuenta planCuenta) {
			this.planCuenta = planCuenta;
		}
		
		/**
		 * Metodo que recupera los valores de la variable NombreCuentas
		 * @return nombreCuenta.
		 */
		public String getNombreCuenta() {
			return nombreCuenta;
		}

		/**
		 * Metodo que asigna los valores de la variable NombreCuentas
		 * @param nombreCuenta nuevo valor para nombreCuenta. 
		 */
		public void setNombreCuenta(String nombreCuenta) {
			this.nombreCuenta = nombreCuenta;
		}
		
		/**
		 * Metodo que recupera los valores de la variable Numero Cuenta
		 * @return numeroCuenta.
		 */
		public String getNumeroCuenta() {
			return numeroCuenta;
		}

		/**
		 * Metodo que asigna los valores de la variable Numero Cuenta
		 * @param numeroCuenta nuevo valor para numeroCuenta. 
		 */
		public void setNumeroCuenta(String numeroCuenta) {
			this.numeroCuenta = numeroCuenta;
		}
		
		/**
		 * Metodo que recupera los valores de la variable secuencia
		 * @return secuencia.
		 */
		public Long getSecuencia() {
			return secuencia;
		}

		/**
		 * Metodo que asigna los valores de la variable secuencia
		 * @param secuencia nuevo valor para secuencia. 
		 */
		public void setSecuencia(Long secuencia) {
			this.secuencia = secuencia;
		}
		
		/**
		 * Metodo que recupera los valores de la variable saldoAnterior
		 * @return saldoAnterio.
		 */
		public Long getSaldoAnterio() {
			return saldoAnterio;
		}

		/**
		 * Metodo que asigna los valores de la variable saldoAnterior
		 * @param saldoAnterio nuevo valor para saldoAnterio. 
		 */
		public void setSaldoAnterio(Long saldoAnterio) {
			this.saldoAnterio = saldoAnterio;
		}
		
		/**
		 * Metodo que recupera los valores de la variable Debe
		 * @return debe.
		 */
		public Long getDebe() {
			return debe;
		}

		/**
		 * Metodo que asigna los valores de la variable Debe
		 * @param debe nuevo valor para debe. 
		 */
		public void setDebe(Long debe) {
			this.debe = debe;
		}
		
		/**
		 * Metodo que recupera los valores de la variable haber
		 * @return haber.
		 */
		public Long getHaber() {
			return haber;
		}

		/**
		 * Metodo que asigna los valores de la variable haber
		 * @param haber nuevo valor para haber. 
		 */
		public void setHaber(Long haber) {
			this.haber = haber;
		}
		
		/**
		 * Metodo que recupera los valores de la variable saldo actual
		 * @return saldoActual.
		 */
		public Long getSaldoActual() {
			return saldoActual;
		}

		/**
		 * Metodo que asigna los valores de la variable saldo actual
		 * @param saldoActual nuevo valor para saldoActual. 
		 */
		public void setSaldoActual(Long saldoActual) {
			this.saldoActual = saldoActual;
		}

}
