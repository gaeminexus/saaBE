package com.saa.model.contabilidad;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SuppressWarnings("serial")
@Entity
@Table(name = "DTMT", schema = "CNT")
@SequenceGenerator(name = "SQ_DTMTCDGO", sequenceName = "CNT.SQ_DTMTCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "TempReportesAll", query = "select e from TempReportes e"),
	@NamedQuery(name = "TempReportesId", query = "select e from TempReportes e where e.codigo = :id")
})
public class TempReportes implements Serializable {
	
	
	/**
	 * Secuencia de cada ejecución de Reporte.
	 */
	@Basic
	@Column(name = "DTMTSLAC")
	private Double valorActual;

	/**
	 * Id de la tabla.
	 */
	@Basic
	@Id
	@Column(name = "DTMTCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTMTCDGO")
	private Long codigo;
	
	
	
	/**
	 * Secuencia de cada ejecución de Reporte.
	 */
	@Basic
	@Column(name = "DTMTSECN")
	private Long secuencia;
	
	/**
	 * Id de tabla PLNN.
	 */
	@Basic
	@Column(name = "DTMTPLNN")
	private PlanCuenta planCuenta;	

	/**
	 * Saldo del periodo anterior de la cuenta contable.
	 */
	@Basic
	@Column(name = "DTMTSLDO")
	private Double saldoCuenta;
	
	/**
	 * Valor del Debe del periodo actual. 
	 */
	@Basic
	@Column(name = "DTMTVLDB")
	private Double valorDebe;
	
	/**
	 * Valor del Haber del periodo actual.
	 */
	@Basic
	@Column(name = "DTMTVLHB")
	private Double valorHaber;
	
	/**
	 * Saldo al final del período actual.
	 */
	@Basic
	@Column(name = "DTMTSLFN")
	private Double saldoFinal;

	/**
	 * Cuenta contable.
	 */
	private String cuentaContable;
	
	/**
	 * Código de padre de la cuenta contable. 
	 */
	private Long codigoCuentaPadre;
	
	/**
	 * Nombre de la cuenta contable. nombreCuenta.
	 */
	private String nombreCuenta;
	
	/**
	 * Tipo de la cuenta contable. 1 = Acumulación, 2 = Movimiento.
	 */
	private Long tipo;
	
	/**
	 * Nivel de la cuenta contable. 
	 */
	private Long nivel;
	
	/**
	 * Id de la mayorizacion MYRZCDGO.
	 */
	private Mayorizacion mayorizacion;
	
	/**
	 * ID Centro de Costo asociado a la cuenta contable.
	 */
	private CentroCosto centroCosto;
	
	/**
	 * Nombre del centro de costo.
	 */
	private String nombreCentroCosto;
	
	/**
	 * Numero del centro de costo. 
	 */
	private String numeroCentroCosto;	
	
	
	
	public Double getValorActual() {
		return valorActual;
	}

	/**
	 * Metodo que asigna los valores de la variable valor actual
	 * @param valorActual nuevo valor para valorActual. 
	 */
	public void setValorActual(Double valorActual) {
		this.valorActual = valorActual;
	}
	
	/**
	 * Metodo que recupera los valores de la variable Codigo
	 * @return codigo.
	 */	
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Metodo que asignar los valores de la variable Codigo
	 * @param codigo nuevo valor para codigo. 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
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
	 * get planCuenta.
	 */
	public PlanCuenta getPlanCuenta() {
		return this.planCuenta;
	}
	
	/**
	 * set planCuenta.
	 */
	public void setPlanCuenta(PlanCuenta planCuenta) {
		this.planCuenta = planCuenta;
	}

	/**
	 * Metodo que recupera los valores de la variable saldo cuenta
	 * @return saldoCuenta.
	 */
	public Double getSaldoCuenta() {
		return saldoCuenta;
	}

	/**
	 * Metodo que asigna los valores de la variable saldo cuenta
	 * @param saldoCuenta nuevo valor para saldoCuenta. 
	 */
	public void setSaldoCuenta(Double saldoCuenta) {
		this.saldoCuenta = saldoCuenta;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor debe
	 * @return valorDebe.
	 */
	public Double getValorDebe() {
		return valorDebe;
	}

	/**
	 * Metodo que asigna los valores de la variable valor debe
	 * @param valorDebe nuevo valor para valorDebe. 
	 */
	public void setValorDebe(Double valorDebe) {
		this.valorDebe = valorDebe;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor haber
	 * @return valorHaber.
	 */
	public Double getValorHaber() {
		return valorHaber;
	}

	/**
	 * Metodo que asigna los valores de la variable valor haber
	 * @param valorHaber nuevo valor para valorHaber. 
	 */
	public void setValorHaber(Double valorHaber) {
		this.valorHaber = valorHaber;
	}
	
	/**
	 * Metodo que recupera los valores de la variable saldo final
	 * @return saldoFinal.
	 */
	public Double getSaldoFinal() {
		return saldoFinal;
	}

	/**
	 * Metodo que asigna los valores de la variable saldo final
	 * @param saldoFinal nuevo valor para saldoFinal. 
	 */
	public void setSaldoFinal(Double saldoFinal) {
		this.saldoFinal = saldoFinal;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor cuenta contable
	 * @return cuentaContable.
	 */
	public String getCuentaContable() {
		return cuentaContable;
	}

	/**
	 * Metodo que asigna los valores de la variable valor cuenta contable
	 * @param cuentaContable nuevo valor para cuentaContable. 
	 */
	public void setCuentaContable(String cuentaContable) {
		this.cuentaContable = cuentaContable;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor CodigoCuenta Padre
	 * @return codigoCuentaPadre.
	 */
	public Long getCodigoCuentaPadre() {
		return codigoCuentaPadre;
	}

	/**
	 * Metodo que asigna los valores de la variable valor CodigoCuenta Padre
	 * @param codigoCuentaPadre nuevo valor para codigoCuentaPadre. 
	 */
	public void setCodigoCuentaPadre(Long codigoCuentaPadre) {
		this.codigoCuentaPadre = codigoCuentaPadre;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor nombreCuenta
	 * @return nombreCuenta.
	 */
	public String getNombreCuenta() {
		return nombreCuenta;
	}

	/**
	 * Metodo que asigna los valores de la variable valor nombreCuenta
	 * @param nombreCuenta nuevo valor para nombreCuenta. 
	 */
	public void setNombreCuenta(String nombreCuenta) {
		this.nombreCuenta = nombreCuenta;
	}
	
	/**
	 * Metodo que recupera los valores de la variable valor tipo
	 * @return tipo.
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Metodo que asigna los valores de la variable valor tipo
	 * @param tipo nuevo valor para tipo. 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
	
	/**
	 * Metodo que recupera los valores de la variable nivel
	 * @return nivel.
	 */
	public Long getNivel() {
		return nivel;
	}

	/**
	 * Metodo que asigna los valores de la variable nivel
	 * @param nivel nuevo valor para nivel. 
	 */
	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}
	
	/**
	 * Metodo que recupera los valores de la variable mayorizacion
	 * @return mayorizacion.
	 */
	public Mayorizacion getMayorizacion() {
		return mayorizacion;
	}

	/**
	 * Metodo que asigna los valores de la variable mayorizacion
	 * @param mayorizacion nuevo valor para mayorizacion. 
	 */
	public void setMayorizacion(Mayorizacion mayorizacion) {
		this.mayorizacion = mayorizacion;
	}
	
	/**
	 * Metodo que recupera los valores de la variable idcentroCosto
	 * @return idCentroCosto.
	 */
	public CentroCosto getCentroCosto() {
		return centroCosto;
	}

	/**
	 * Metodo que asigna los valores de la variable idcentroCosto
	 * @param idCentroCosto nuevo valor para idCentroCosto. 
	 */
	public void setCentroCosto(CentroCosto centroCosto) {
		this.centroCosto = centroCosto;
	}
	
	/**
	 * Metodo que recupera los valores de la variable nombrecentroCosto
	 * @return nombreCentroCosto.
	 */
	public String getNombreCentroCosto() {
		return nombreCentroCosto;
	}

	/**
	 * Metodo que asigna los valores de la variable nombrecentroCosto
	 * @param nombreCentroCosto nuevo valor para nombreCentroCosto. 
	 */
	public void setNombreCentroCosto(String nombreCentroCosto) {
		this.nombreCentroCosto = nombreCentroCosto;
	}
	
	/**
	 * Metodo que recupera los valores de la variable numerocentroCosto
	 * @return numeroCentroCosto.
	 */
	public String getNumeroCentroCosto() {
		return numeroCentroCosto;
	}

	/**
	 * Metodo que asigna los valores de la variable numerocentroCosto
	 * @param numeroCentroCosto nuevo valor para numeroCentroCosto. 
	 */
	public void setNumeroCentroCosto(String numeroCentroCosto) {
		this.numeroCentroCosto = numeroCentroCosto;
	}
	
	
   
}
