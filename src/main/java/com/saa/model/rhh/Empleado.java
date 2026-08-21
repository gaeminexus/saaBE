package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;
import com.saa.model.cnt.CentroCosto;
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
import jakarta.persistence.Table;

/**
 * Registro maestro de empleados.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "MPLD", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "EmpleadoId", query = "select e from Empleado e where e.codigo=:id"),
    @NamedQuery(name = "EmpleadoAll", query = "select e from Empleado e")
})
public class Empleado implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del empleado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "MPLDCDGO")
    private Long codigo;

    /**
     * Identificación (cédula/RUC/pasaporte).
     */
    @Basic
    @Column(name = "MPLDIDNT")
    private String identificacion;

    /**
     * Apellidos.
     */
    @Basic
    @Column(name = "MPLDAPLL")
    private String apellidos;

    /**
     * Nombres.
     */
    @Basic
    @Column(name = "MPLDNMBR")
    private String nombres;

    /**
     * Fecha de nacimiento.
     */
    @Basic
    @Column(name = "MPLDFCHN")
    private LocalDate fechaNacimiento;

    /**
     * Correo electrónico.
     */
    @Basic
    @Column(name = "MPLDEMAI")
    private String email;

    /**
     * Teléfono.
     */
    @Basic
    @Column(name = "MPLDTLFN")
    private String telefono;

    /**
     * Dirección.
     */
    @Basic
    @Column(name = "MPLDDRCC")
    private String direccion;

    /**
     * Estado del empleado (A=Activo, I=Inactivo).
     */
    @Basic
    @Column(name = "MPLDESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "MPLDFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "MPLDUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Empresa a la que pertenece el empleado (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de identificacion: detalle del rubro TipoIdentificacion.
     */
    @Basic
    @Column(name = "MPLDTPID")
    private Long tipoIdentificacion;

    /**
     * Estado civil: detalle del rubro RHH_ESTADO_CIVIL.
     */
    @Basic
    @Column(name = "MPLDESTC")
    private Long estadoCivil;

    /**
     * Genero: detalle del rubro RHH_GENERO.
     */
    @Basic
    @Column(name = "MPLDGNRO")
    private Long genero;

    /**
     * Nacionalidad del empleado.
     */
    @Basic
    @Column(name = "MPLDNCNL", length = 60)
    private String nacionalidad;

    /**
     * Nivel de instruccion: detalle del rubro RHH_NIVEL_INSTRUCCION.
     */
    @Basic
    @Column(name = "MPLDNVIN")
    private Long nivelInstruccion;

    /**
     * Profesion o titulo del empleado.
     */
    @Basic
    @Column(name = "MPLDPRFS", length = 150)
    private String profesion;

    /**
     * Tipo de sangre del empleado.
     */
    @Basic
    @Column(name = "MPLDTPSN", length = 5)
    private String tipoSangre;

    /**
     * Tiene discapacidad reconocida (S/N).
     */
    @Basic
    @Column(name = "MPLDDSCP", length = 1)
    private String discapacidad;

    /**
     * Porcentaje de discapacidad; incide en la exoneracion de impuesto a la renta.
     */
    @Basic
    @Column(name = "MPLDPRDS")
    private Double porcentajeDiscapacidad;

    /**
     * Numero de carne del CONADIS.
     */
    @Basic
    @Column(name = "MPLDCNDS", length = 30)
    private String carneConadis;

    /**
     * Padece enfermedad catastrofica, rara u huerfana (S/N); amplia el tope de gastos personales.
     */
    @Basic
    @Column(name = "MPLDCTSF", length = 1)
    private String enfermedadCatastrofica;

    /**
     * Codigo de afiliacion del empleado en el IESS.
     */
    @Basic
    @Column(name = "MPLDCDAF", length = 30)
    private String codigoAfiliacion;

    /**
     * Fecha de ingreso a la empresa; base del calculo de antiguedad.
     */
    @Basic
    @Column(name = "MPLDFCIN")
    private LocalDate fechaIngreso;

    /**
     * Region para el decimo cuarto: detalle del rubro RHH_REGION_DECIMO_CUARTO.
     */
    @Basic
    @Column(name = "MPLDRGNN")
    private Long region;

    /**
     * Codigo con el que el empleado se identifica en el reloj biometrico.
     */
    @Basic
    @Column(name = "MPLDCDBM", length = 30)
    private String codigoBiometrico;

    /**
     * Nombre del contacto de emergencia.
     */
    @Basic
    @Column(name = "MPLDCTEM", length = 150)
    private String contactoEmergencia;

    /**
     * Telefono del contacto de emergencia.
     */
    @Basic
    @Column(name = "MPLDTLEM", length = 30)
    private String telefonoEmergencia;

    /**
     * Centro de costo (CNT.CNCS) al que se imputa el costo del empleado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCNCS", referencedColumnName = "CNCSCDGO")
    private CentroCosto centroCosto;

    /**
     * Ruta de la fotografia del empleado.
     */
    @Basic
    @Column(name = "MPLDFOTO", length = 300)
    private String foto;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
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

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
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

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getTipoIdentificacion() {
        return tipoIdentificacion;
    }

    public void setTipoIdentificacion(Long tipoIdentificacion) {
        this.tipoIdentificacion = tipoIdentificacion;
    }

    public Long getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(Long estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public Long getGenero() {
        return genero;
    }

    public void setGenero(Long genero) {
        this.genero = genero;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public Long getNivelInstruccion() {
        return nivelInstruccion;
    }

    public void setNivelInstruccion(Long nivelInstruccion) {
        this.nivelInstruccion = nivelInstruccion;
    }

    public String getProfesion() {
        return profesion;
    }

    public void setProfesion(String profesion) {
        this.profesion = profesion;
    }

    public String getTipoSangre() {
        return tipoSangre;
    }

    public void setTipoSangre(String tipoSangre) {
        this.tipoSangre = tipoSangre;
    }

    public String getDiscapacidad() {
        return discapacidad;
    }

    public void setDiscapacidad(String discapacidad) {
        this.discapacidad = discapacidad;
    }

    public Double getPorcentajeDiscapacidad() {
        return porcentajeDiscapacidad;
    }

    public void setPorcentajeDiscapacidad(Double porcentajeDiscapacidad) {
        this.porcentajeDiscapacidad = porcentajeDiscapacidad;
    }

    public String getCarneConadis() {
        return carneConadis;
    }

    public void setCarneConadis(String carneConadis) {
        this.carneConadis = carneConadis;
    }

    public String getEnfermedadCatastrofica() {
        return enfermedadCatastrofica;
    }

    public void setEnfermedadCatastrofica(String enfermedadCatastrofica) {
        this.enfermedadCatastrofica = enfermedadCatastrofica;
    }

    public String getCodigoAfiliacion() {
        return codigoAfiliacion;
    }

    public void setCodigoAfiliacion(String codigoAfiliacion) {
        this.codigoAfiliacion = codigoAfiliacion;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public Long getRegion() {
        return region;
    }

    public void setRegion(Long region) {
        this.region = region;
    }

    public String getCodigoBiometrico() {
        return codigoBiometrico;
    }

    public void setCodigoBiometrico(String codigoBiometrico) {
        this.codigoBiometrico = codigoBiometrico;
    }

    public String getContactoEmergencia() {
        return contactoEmergencia;
    }

    public void setContactoEmergencia(String contactoEmergencia) {
        this.contactoEmergencia = contactoEmergencia;
    }

    public String getTelefonoEmergencia() {
        return telefonoEmergencia;
    }

    public void setTelefonoEmergencia(String telefonoEmergencia) {
        this.telefonoEmergencia = telefonoEmergencia;
    }

    public CentroCosto getCentroCosto() {
        return centroCosto;
    }

    public void setCentroCosto(CentroCosto centroCosto) {
        this.centroCosto = centroCosto;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}
