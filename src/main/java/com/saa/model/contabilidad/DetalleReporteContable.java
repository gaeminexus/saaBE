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
@Table(name = "DTRP", schema = "CNT")
@SequenceGenerator(name = "SQ_DTRPCDGO", sequenceName = "CNT.SQ_DTRPCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DetalleReporteContableAll", query = "select e from DetalleReporteContable e"),
	@NamedQuery(name = "DetalleReporteContableId", query = "select e from DetalleReporteContable e where e.codigo = :id")
})
public class DetalleReporteContable implements Serializable {

	/**
	 * Id de la tabla. codigo.
	 */
	@Basic
	@Id
	@Column(name = "DTRPCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTRPCDGO")
	private Long codigo;
	
	/**
	 * Clave foranea a RPRT.
	 */
	@ManyToOne
	@JoinColumn(name = "RPRTCDGO", referencedColumnName = "RPRTCDGO")
	private ReporteContable reporteContable;	

	/**
	 * Id de plan contable desde. 
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDDS", referencedColumnName = "PLNNCDGO")
	private PlanCuenta cuentaDesde;	

	/**
	 * Número de la Cuenta Contable Desde.
	 */
	@Basic
	@Column(name = "NUMCTADS", length = 50)
	private String numeroDesde;
	
	/**
	 * Nombre de la Cuenta Contable Desde.
	 */
	@Basic
	@Column(name = "NMBCTADS", length = 100)
	private String nombreDesde;
	
	/**
	 * Id de plan contable hasta. cuentaHasta
	 */
	@ManyToOne
	@JoinColumn(name = "PLNNCDHS", referencedColumnName = "PLNNCDGO")
	private PlanCuenta cuentaHasta;	

	/**
	 * Número de la Cuenta Contable Hasta.
	 */
	@Basic
	@Column(name = "NUMCTAHS", length = 50)
	private String numeroHasta;
	
	/**
	 * Nombre de la Cuenta Contable Hasta. nombreHasta.
	 */
	@Basic
	@Column(name = "NMBCTAHS", length = 100)
	private String nombreHasta;
	
	/**
	 * Signo, 1 = Suma, 2 = Resta. signo.
	 */
	@Basic
	@Column(name = "DTRPSGNO")
	private Long signo;	
	
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve reporteContable
	 */
	public ReporteContable getReporteContable() {
		return this.reporteContable;
	}
	
	/**
	 * Asigna reporteContable
	 */
	public void setReporteContable(ReporteContable reporteContable) {
		this.reporteContable = reporteContable;
	}

	/**
	 * Devuelve CuentaDesde
	 */	
	public PlanCuenta getCuentaDesde() {
		return this.cuentaDesde;
	}
	
	/**
	 * Asigna CuentaDesde
	 */
	public void setCuentaDesde(PlanCuenta cuentaDesde) {
		this.cuentaDesde = cuentaDesde;
	}

	/**
	 * Devuelve numeroDesde
	 * @return numeroDesde
	 */
	public String getNumeroDesde() {
		return numeroDesde;
	}

	/**
	 * Asigna numeroDesde
	 * @param numeroDesde nuevo valor para numeroDesde 
	 */
	public void setNumeroDesde(String numeroDesde) {
		this.numeroDesde = numeroDesde;
	}
	
	/**
	 * Devuelve nombreDesde
	 * @return nombreDesde
	 */
	public String getNombreDesde() {
		return nombreDesde;
	}

	/**
	 * Asigna nombreDesde
	 * @param nombreDesde nuevo valor para nombreDesde 
	 */
	public void setNombreDesde(String nombreDesde) {
		this.nombreDesde = nombreDesde;
	}
	
	/**
	 * Devuelve CuentaHasta
	 */
	public PlanCuenta getCuentaHasta() {
		return this.cuentaHasta;
	}
	
	/**
	 * Asigna CuentaHasta
	 */
	public void setCuentaHasta(PlanCuenta cuentaHasta) {
		this.cuentaHasta = cuentaHasta;
	}

	/**
	 * Devuelve numeroHasta
	 * @return numeroHasta
	 */
	public String getNumeroHasta() {
		return numeroHasta;
	}

	/**
	 * Asigna numeroHasta
	 * @param numeroHasta nuevo valor para numeroHasta 
	 */
	public void setNumeroHasta(String numeroHasta) {
		this.numeroHasta = numeroHasta;
	}
	
	/**
	 * Devuelve nombreHasta
	 * @return nombreHasta
	 */
	public String getNombreHasta() {
		return nombreHasta;
	}

	/**
	 * Asigna nombreHasta
	 * @param nombreHasta nuevo valor para nombreHasta 
	 */
	public void setNombreHasta(String nombreHasta) {
		this.nombreHasta = nombreHasta;
	}
	
	/**
	 * Devuelve signo
	 * @return signo
	 */
	public Long getSigno() {
		return signo;
	}

	/**
	 * Asigna signo
	 * @param signo nuevo valor para signo 
	 */
	public void setSigno(Long signo) {
		this.signo = signo;
	}
   
}
