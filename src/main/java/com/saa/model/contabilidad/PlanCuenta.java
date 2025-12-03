package com.saa.model.contabilidad;

import java.io.Serializable;
import java.time.LocalDate;

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

@SuppressWarnings("serial")
@Entity
@Table(name = "PLNN", schema = "CNT")
@SequenceGenerator(name = "SQ_PLNNCDGO", sequenceName = "CNT.SQ_PLNNCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "PlanCuentaAll", query = "select e from PlanCuenta e"),
	@NamedQuery(name = "PlanCuentaId", query = "select e from PlanCuenta e where e.codigo = :id")
})
public class PlanCuenta implements Serializable {

    /**
     * Id de Tabla.
     */
    @Basic
    @Id
    @Column(name = "PLNNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PLNNCDGO")
    private Long codigo;
    
    
    
    /**
     * NaturalezaCuenta a la que pertenece el plan 
     */
    @ManyToOne
    @JoinColumn(name = "NTRLCDGO", referencedColumnName = "NTRLCDGO")
    private NaturalezaCuenta naturalezaCuenta;    

    /**
     * Cuenta Contable.
     */
    @Basic
    @Column(name = "PLNNCNTA", length = 50)
    private String cuentaContable;
    
    /**
     * Nombre.
     */
    @Basic
    @Column(name = "PLNNNMBR", length = 100)
    private String nombre;
    
    /**
     *  Tipo de la Cuenta Contable. 1 = Acumulación, 2 = Movimiento.
     */
    @Basic
    @Column(name = "PLNNTPOO")
    private Long tipo;
    
    /**
     *  Nivel de la Cuenta Contable.
     */
    @Basic
    @Column(name = "PLNNNVLL")
    private Long nivel;
    
    
    /**
     * Código de Padre de la Cuenta Contable.
     */
    @Basic
    @Column(name = "PLNNCDPD")
    private Long idPadre;
   
    
    /**
     * Estado de la Cuenta Contable. 1 = Activo, 2 = Inactivo.
     */
    @Basic
    @Column(name = "PLNNESTD")
    private Long estado;
    
    /**
     *  fecha desactivación de cuenta contable.
     */
    @Basic
    @Column(name = "PLNNFCDS")
    private LocalDate fechaInactivo;
    
    /**
     * Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;
    
    /**
     * Fecha de Creación o Ultima Actualización de Cuenta Contable.
     */
    @Basic
    @Column(name = "PLNNFCHA")
    private LocalDate fechaUpdate;
    
    /**
	 * Devuelve codigo
	 * @return codigo.
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo nuevo valor para codigo.
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve naturalezaCuenta.
	 */
	public NaturalezaCuenta getNaturalezaCuenta() {
		return this.naturalezaCuenta;
	}
	
	/**
	 * Asigna naturalezaCuenta.
	 */
	public void setNaturalezaCuenta(NaturalezaCuenta naturalezaCuenta) {
		this.naturalezaCuenta = naturalezaCuenta;
	}

	/**
	 * Devuelve cuentaContable
	 * @return cuentaContable.
	 */
	public String getCuentaContable() {
		return cuentaContable;
	}

	/**
	 * Asigna cuentaContable
	 * @param cuentaContable nuevo valor para cuentaContable. 
	 */
	public void setCuentaContable(String cuentaContable) {
		this.cuentaContable = cuentaContable;
	}
	
	/**
	 * Devuelve nombre
	 * @return nombre.
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * @param nombre nuevo valor para nombre.
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	/**
	 * Devuelve tipo
	 * @return tipo.
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna tipo
	 * @param tipo nuevo valor para tipo. 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
	
	/**
	 * Devuelve nivel
	 * @return nivel.
	 */
	public Long getNivel() {
		return nivel;
	}

	/**
	 * Asigna nivel
	 * @param nivel nuevo valor para nivel.
	 */
	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}
	
	/**
	 * Devuelve idPadre
	 * @return idPadre.
	 */
	public Long getIdPadre() {
		return idPadre;
	}

	/**
	 * Asigna idPadre
	 * @param idPadre nuevo valor para idPadre. 
	 */
	public void setIdPadre(Long idPadre) {
		this.idPadre = idPadre;
	}
	
	/**
	 * Devuelve estado
	 * @return estado.
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado nuevo valor para estado. 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}
	
	/**
	 * Devuelve fechaInactivo
	 * @return fechaInactivo.
	 */
	public LocalDate getFechaInactivo() {
		return fechaInactivo;
	}

	/**
	 * Asigna fechaInactivo
	 * @param fechaInactivo nuevo valor para fechaInactivo. 
	 */
	public void setFechaInactivo(LocalDate fechaInactivo) {
		this.fechaInactivo = fechaInactivo;
	}
	
	/**
	 * Devuelve empresa
	 * @return empresa.
	 */
	public Empresa getEmpresa() {
		return empresa;
	}

	/**
	 * Asigna empresa
	 * @param empresa nuevo valor para empresa. 
	 */
	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}
	
	/**
	 * Devuelve fechaUpdate
	 * @return fechaUpdate.
	 */
	public LocalDate getFechaUpdate() {
		return fechaUpdate;
	}

	/**
	 * Asigna fechaUpdate
	 * @param fechaUpdate nuevo valor para fechaUpdate. 
	 */
	public void setFechaUpdate(LocalDate fechaUpdate) {
		this.fechaUpdate = fechaUpdate;
	}
	

}
