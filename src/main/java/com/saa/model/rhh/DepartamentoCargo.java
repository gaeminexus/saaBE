package com.saa.model.rhh;

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
 * Catálogo de departamentos o áreas de la institución.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DPTC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DepartamentoCargoId", query = "select e from DepartamentoCargo e where e.codigo=:id"),
    @NamedQuery(name = "DepartamentoCargoAll", query = "select e from DepartamentoCargo e")
})
public class DepartamentoCargo implements Serializable {

    /**
     * Código único del departamento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DPTCCDGO")
    private Long codigo;
    
    /**
     * Departamento.
     */
    @ManyToOne
    @JoinColumn(name = "DPRTCDGO", referencedColumnName = "DPRTCDGO", nullable = false)
    private Departamento departamento;
    
    
    /**
     * Cargo.
     */
    @ManyToOne
    @JoinColumn(name = "CRGOCDGO", referencedColumnName = "CRGOCDGO", nullable = false)
    private Cargo cargo;
    
    
    /**
     * Estado del registro (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "DPTCESTD")
    private String estado;
    
    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DPTCFCHR")
    private LocalDate fechaRegistro;
   
    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "DPTCUSRR")
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

	public Departamento getDepartamento() {
		return departamento;
	}

	public void setDepartamento(Departamento departamento) {
		this.departamento = departamento;
	}

	public Cargo getCargo() {
		return cargo;
	}

	public void setCargo(Cargo cargo) {
		this.cargo = cargo;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public LocalDate getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(LocalDate fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	public String getUsuarioRegistro() {
		return usuarioRegistro;
	}

	public void setUsuarioRegistro(String usuarioRegistro) {
		this.usuarioRegistro = usuarioRegistro;
	}


}
