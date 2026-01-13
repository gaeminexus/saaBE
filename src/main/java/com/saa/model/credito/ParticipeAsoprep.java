package com.saa.model.credito;

	import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

	/**
	 * Representa la tabla PRAS (Datos personales del participe).
	 */
	@SuppressWarnings("serial")
	@Entity
	@Table(name = "PRAS", schema = "CRD")
	@NamedQueries({
	    @NamedQuery(name = "ParticipeAsoprepAll", query = "select e from ParticipeAsoprep e"),
	    @NamedQuery(name = "ParticipeAsoprepId", query = "select e from ParticipeAsoprep e where e.id = :id")
	})
	
	public class ParticipeAsoprep implements Serializable {

	    // ============================================================
	    // PK
	    // ============================================================

	    @Id
	    @Basic
	    @Column(name = "PRASIDDD")
	    private Long id;

	    // ============================================================
	    // DATOS PERSONALES
	    // ============================================================

	    @Basic
	    @Column(name = "PRASCDLA", length = 20)
	    private String cedula;

	    @Basic
	    @Column(name = "PRASCDGF", length = 10)
	    private String codigoCdgf;

	    @Basic
	    @Column(name = "PRASCDGA", length = 10)
	    private String codigoCdga;

	    @Basic
	    @Column(name = "PRASSTDP", length = 30)
	    private String estadoParticipante;

	    @Basic
	    @Column(name = "PRASAPLL", length = 100)
	    private String apellidos;

	    @Basic
	    @Column(name = "PRASNMBR", length = 100)
	    private String nombres;

	    @Basic
	    @Column(name = "PRASFCHN")
	    private LocalDateTime fechaNacimiento;

	    @Basic
	    @Column(name = "PRASGNRO", length = 1)
	    private String genero;

	    @Basic
	    @Column(name = "PRASSTDC", length = 20)
	    private String estadoCivil;

	    // ============================================================
	    // UBICACIÓN
	    // ============================================================

	    @Basic
	    @Column(name = "PRASPRVD", length = 60)
	    private String provincia;

	    @Basic
	    @Column(name = "PRASCNTD", length = 60)
	    private String canton;

	    @Basic
	    @Column(name = "PRASCDDD", length = 80)
	    private String ciudad;

	    @Basic
	    @Column(name = "PRASPRQD", length = 100)
	    private String parroquia;

	    @Basic
	    @Column(name = "PRASDRCD", length = 350)
	    private String direccion;

	    // ============================================================
	    // CONTACTO
	    // ============================================================

	    @Basic
	    @Column(name = "PRASTLFD", length = 60)
	    private String telefonoFijo;

	    @Basic
	    @Column(name = "PRASTLFC", length = 30)
	    private String telefonoCelular;

	    @Basic
	    @Column(name = "PRASCRRE", length = 90)
	    private String correoElectronico;

	    // ============================================================
	    // DATOS BANCARIOS
	    // ============================================================

	    @Basic
	    @Column(name = "PRASBNCO", length = 80)
	    private String banco;

	    @Basic
	    @Column(name = "PRASTPCB", length = 30)
	    private String tipoCuentaBancaria;

	    @Basic
	    @Column(name = "PRASCTAB", length = 40)
	    private String cuentaBancaria;

	    // ============================================================
	    // FECHAS IMPORTANTES
	    // ============================================================

	    @Basic
	    @Column(name = "PRASFCCS")
	    private LocalDateTime fechaCcs;

	    @Basic
	    @Column(name = "PRASFCJB")
	    private LocalDateTime fechaJubilacion;

	    @Basic
	    @Column(name = "PRASFCIN")
	    private LocalDateTime fechaIngreso;

	    @Basic
	    @Column(name = "PRASFCSN")
	    private LocalDateTime fechaCsn;

	    @Basic
	    @Column(name = "PRASFCJC")
	    private LocalDateTime fechaCjc;

	    // ============================================================
	    // INFORMACIÓN LABORAL
	    // ============================================================

	    @Basic
	    @Column(name = "PRASNSTT", length = 40)
	    private String institucion;

	    @Basic
	    @Column(name = "PRASLCCN", length = 50)
	    private String localidad;

	    @Basic
	    @Column(name = "PRASRGNN", length = 50)
	    private String region;

	    @Basic
	    @Column(name = "PRASNDAD", length = 200)
	    private String edad;

	    @Basic
	    @Column(name = "PRASRGMN", length = 30)
	    private String regimen;

	    @Basic
	    @Column(name = "PRASTLFT", length = 20)
	    private String telefonoTrabajo;

	    @Basic
	    @Column(name = "PRASCRGO", length = 200)
	    private String cargo;

	    @Basic
	    @Column(name = "PRASCDLG", length = 15)
	    private String codigoCdlg;

	    // ============================================================
	    // GETTERS Y SETTERS
	    // ============================================================

	    public Long getId() {
	        return id;
	    }

	    public void setId(Long id) {
	        this.id = id;
	    }

	    public String getCedula() {
	        return cedula;
	    }

	    public void setCedula(String cedula) {
	        this.cedula = cedula;
	    }

	    public String getCodigoCdgf() {
	        return codigoCdgf;
	    }

	    public void setCodigoCdgf(String codigoCdgf) {
	        this.codigoCdgf = codigoCdgf;
	    }

	    public String getCodigoCdga() {
	        return codigoCdga;
	    }

	    public void setCodigoCdga(String codigoCdga) {
	        this.codigoCdga = codigoCdga;
	    }

	    public String getEstadoParticipante() {
	        return estadoParticipante;
	    }

	    public void setEstadoParticipante(String estadoParticipante) {
	        this.estadoParticipante = estadoParticipante;
	    }

	    public String getApellidos() {
	        return apellidos;
	    }

	    public void setApellidos(String apellidos) {
	        this.apellidos = apellidos;
	    }

	    public String getNombres() {
	        return nombres;
	    }

	    public void setNombres(String nombres) {
	        this.nombres = nombres;
	    }

	    public LocalDateTime getFechaNacimiento() {
	        return fechaNacimiento;
	    }

	    public void setFechaNacimiento(LocalDateTime fechaNacimiento) {
	        this.fechaNacimiento = fechaNacimiento;
	    }

	    public String getGenero() {
	        return genero;
	    }

	    public void setGenero(String genero) {
	        this.genero = genero;
	    }

	    public String getEstadoCivil() {
	        return estadoCivil;
	    }

	    public void setEstadoCivil(String estadoCivil) {
	        this.estadoCivil = estadoCivil;
	    }

	    public String getProvincia() {
	        return provincia;
	    }

	    public void setProvincia(String provincia) {
	        this.provincia = provincia;
	    }

	    public String getCanton() {
	        return canton;
	    }

	    public void setCanton(String canton) {
	        this.canton = canton;
	    }

	    public String getCiudad() {
	        return ciudad;
	    }

	    public void setCiudad(String ciudad) {
	        this.ciudad = ciudad;
	    }

	    public String getParroquia() {
	        return parroquia;
	    }

	    public void setParroquia(String parroquia) {
	        this.parroquia = parroquia;
	    }

	    public String getDireccion() {
	        return direccion;
	    }

	    public void setDireccion(String direccion) {
	        this.direccion = direccion;
	    }

	    public String getTelefonoFijo() {
	        return telefonoFijo;
	    }

	    public void setTelefonoFijo(String telefonoFijo) {
	        this.telefonoFijo = telefonoFijo;
	    }

	    public String getTelefonoCelular() {
	        return telefonoCelular;
	    }

	    public void setTelefonoCelular(String telefonoCelular) {
	        this.telefonoCelular = telefonoCelular;
	    }

	    public String getCorreoElectronico() {
	        return correoElectronico;
	    }

	    public void setCorreoElectronico(String correoElectronico) {
	        this.correoElectronico = correoElectronico;
	    }

	    public String getBanco() {
	        return banco;
	    }

	    public void setBanco(String banco) {
	        this.banco = banco;
	    }

	    public String getTipoCuentaBancaria() {
	        return tipoCuentaBancaria;
	    }

	    public void setTipoCuentaBancaria(String tipoCuentaBancaria) {
	        this.tipoCuentaBancaria = tipoCuentaBancaria;
	    }

	    public String getCuentaBancaria() {
	        return cuentaBancaria;
	    }

	    public void setCuentaBancaria(String cuentaBancaria) {
	        this.cuentaBancaria = cuentaBancaria;
	    }

	    public LocalDateTime getFechaIngreso() {
	        return fechaIngreso;
	    }

	    public void setFechaIngreso(LocalDateTime fechaIngreso) {
	        this.fechaIngreso = fechaIngreso;
	    }

	

}
