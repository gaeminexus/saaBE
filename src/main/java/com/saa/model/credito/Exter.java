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
 * Representa la tabla EXTR (Exter).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "EXTR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ExterAll", query = "select e from Exter e"),
    @NamedQuery(name = "ExterByCedula", query = "select e from Exter e where e.cedula = :cedula")
})
public class Exter implements Serializable {
    
    /**
     * Cédula.
     */
    @Id
    @Basic
    @Column(name = "EXTRCDLA", length = 2000)
    private String cedula;
    
    /**
     * Nombre.
     */
    @Basic
    @Column(name = "EXTRNMBR", length = 2000)
    private String nombre;
    
    /**
     * Estado.
     */
    @Basic
    @Column(name = "EXTRESTD", length = 2000)
    private String estado;
    
    /**
     * Fecha Nacimiento.
     */
    @Basic
    @Column(name = "EXTRFCNC")
    private LocalDateTime fechaNacimiento;
    
    /**
     * Estado Civil.
     */
    @Basic
    @Column(name = "EXTRESCV", length = 2000)
    private String estadoCivil;
    
    /**
     * Nivel Estudios.
     */
    @Basic
    @Column(name = "EXTRNVES", length = 2000)
    private String nivelEstudios;
    
    /**
     * Edad.
     */
    @Basic
    @Column(name = "EXTREDDD", length = 2000)
    private String edad;
    
    /**
     * Profesión.
     */
    @Basic
    @Column(name = "EXTRPRFS", length = 2000)
    private String profesion;
    
    /**
     * Género.
     */
    @Basic
    @Column(name = "EXTRGNRO", length = 2000)
    private String genero;
    
    /**
     * Fecha Desde.
     */
    @Basic
    @Column(name = "EXTRFCDF")
    private LocalDateTime fechaDefuncion;
    
    /**
     * Nacionalidad.
     */
    @Basic
    @Column(name = "EXTRNCNL", length = 2000)
    private String nacionalidad;
    
    /**
     * Provincia.
     */
    @Basic
    @Column(name = "EXTRPRVN", length = 2000)
    private String provincia;
    
    /**
     * Cantón.
     */
    @Basic
    @Column(name = "EXTRCNTN", length = 2000)
    private String canton;
    
    /**
     * Móvil.
     */
    @Basic
    @Column(name = "EXTRMVLL", length = 2000)
    private String movil;
    
    /**
     * Teléfono.
     */
    @Basic
    @Column(name = "EXTRTLFN", length = 2000)
    private String telefono;
    
    /**
     * Correo Principal.
     */
    @Basic
    @Column(name = "EXTRCRPR", length = 2000)
    private String correoPrincipal;
    
    /**
     * Correo Institucional.
     */
    @Basic
    @Column(name = "EXTRCRIN", length = 2000)
    private String correoInstitucional;
    
    /**
     * Celular 1.
     */
    @Basic
    @Column(name = "EXTRCLL1", length = 2000)
    private String celular1;
    
    /**
     * Celular 2.
     */
    @Basic
    @Column(name = "EXTRCLL2", length = 2000)
    private String celular2;
    
    /**
     * Correo Extra.
     */
    @Basic
    @Column(name = "EXTRCREX", length = 2000)
    private String correoExtra;
    
    /**
     * Teléfono Laboral Institucion Empleadora.
     */
    @Basic
    @Column(name = "EXTRTLIE", length = 2000)
    private String telefonoLaboralIE;
    
    /**
     * Correo Institucion Empleadora.
     */
    @Basic
    @Column(name = "EXTRCRIE", length = 2000)
    private String correoIE;
    
    /**
     * Salario Fijo.
     */
    @Basic
    @Column(name = "EXTRSLFJ", precision = 10, scale = 2)
    private Double salarioFijo;
    
    /**
     * Salario Variable.
     */
    @Basic
    @Column(name = "EXTRSLVR", precision = 10, scale = 2)
    private Double salarioVariable;
    
    /**
     * Salario Total.
     */
    @Basic
    @Column(name = "EXTRSLTT", precision = 10, scale = 2)
    private Double salarioTotal;
    
    /**
     * Sumados Ingresos.
     */
    @Basic
    @Column(name = "EXTRSMCI", precision = 10, scale = 2)
    private Double sumadosIngresos;
    
    /**
     * Sumados Egresos.
     */
    @Basic
    @Column(name = "EXTRSMSC", precision = 10, scale = 2)
    private Double sumadosEgresos;
    
    /**
     * Disponible.
     */
    @Basic
    @Column(name = "EXTRDSPN", precision = 10, scale = 6)
    private Double disponible;
    
    // ============================================================
    // Getters y Setters
    // ============================================================
    
    /**
     * Devuelve cedula.
     * @return cedula.
     */
    public String getCedula() {
        return cedula;
    }
    
    /**
     * Asigna cedula.
     * @param cedula nuevo valor para cedula.
     */
    public void setCedula(String cedula) {
        this.cedula = cedula;
    }
    
    /**
     * Devuelve nombre.
     * @return nombre.
     */
    public String getNombre() {
        return nombre;
    }
    
    /**
     * Asigna nombre.
     * @param nombre nuevo valor para nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    /**
     * Devuelve estado.
     * @return estado.
     */
    public String getEstado() {
        return estado;
    }
    
    /**
     * Asigna estado.
     * @param estado nuevo valor para estado.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    /**
     * Devuelve fechaNacimiento.
     * @return fechaNacimiento.
     */
    public LocalDateTime getFechaNacimiento() {
        return fechaNacimiento;
    }
    
    /**
     * Asigna fechaNacimiento.
     * @param fechaNacimiento nuevo valor para fechaNacimiento.
     */
    public void setFechaNacimiento(LocalDateTime fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }
    
    /**
     * Devuelve estadoCivil.
     * @return estadoCivil.
     */
    public String getEstadoCivil() {
        return estadoCivil;
    }
    
    /**
     * Asigna estadoCivil.
     * @param estadoCivil nuevo valor para estadoCivil.
     */
    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }
    
    /**
     * Devuelve nivelEstudios.
     * @return nivelEstudios.
     */
    public String getNivelEstudios() {
        return nivelEstudios;
    }
    
    /**
     * Asigna nivelEstudios.
     * @param nivelEstudios nuevo valor para nivelEstudios.
     */
    public void setNivelEstudios(String nivelEstudios) {
        this.nivelEstudios = nivelEstudios;
    }
    
    /**
     * Devuelve edad.
     * @return edad.
     */
    public String getEdad() {
        return edad;
    }
    
    /**
     * Asigna edad.
     * @param edad nuevo valor para edad.
     */
    public void setEdad(String edad) {
        this.edad = edad;
    }
    
    /**
     * Devuelve profesion.
     * @return profesion.
     */
    public String getProfesion() {
        return profesion;
    }
    
    /**
     * Asigna profesion.
     * @param profesion nuevo valor para profesion.
     */
    public void setProfesion(String profesion) {
        this.profesion = profesion;
    }
    
    /**
     * Devuelve genero.
     * @return genero.
     */
    public String getGenero() {
        return genero;
    }
    
    /**
     * Asigna genero.
     * @param genero nuevo valor para genero.
     */
    public void setGenero(String genero) {
        this.genero = genero;
    }
    
    /**
     * Devuelve fechaDefuncion.
     * @return fechaDefuncion.
     */
    public LocalDateTime getFechaDefuncion() {
        return fechaDefuncion;
    }
    
    /**
     * Asigna fechaDefuncion.
     * @param fechaDefuncion nuevo valor para fechaDesde.
     */
    public void setFechaDefuncion(LocalDateTime fechaDefuncion) {
        this.fechaDefuncion = fechaDefuncion;
    }
    
    /**
     * Devuelve nacionalidad.
     * @return nacionalidad.
     */
    public String getNacionalidad() {
        return nacionalidad;
    }
    
    /**
     * Asigna nacionalidad.
     * @param nacionalidad nuevo valor para nacionalidad.
     */
    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }
    
    /**
     * Devuelve provincia.
     * @return provincia.
     */
    public String getProvincia() {
        return provincia;
    }
    
    /**
     * Asigna provincia.
     * @param provincia nuevo valor para provincia.
     */
    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }
    
    /**
     * Devuelve canton.
     * @return canton.
     */
    public String getCanton() {
        return canton;
    }
    
    /**
     * Asigna canton.
     * @param canton nuevo valor para canton.
     */
    public void setCanton(String canton) {
        this.canton = canton;
    }
    
    /**
     * Devuelve movil.
     * @return movil.
     */
    public String getMovil() {
        return movil;
    }
    
    /**
     * Asigna movil.
     * @param movil nuevo valor para movil.
     */
    public void setMovil(String movil) {
        this.movil = movil;
    }
    
    /**
     * Devuelve telefono.
     * @return telefono.
     */
    public String getTelefono() {
        return telefono;
    }
    
    /**
     * Asigna telefono.
     * @param telefono nuevo valor para telefono.
     */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
    
    /**
     * Devuelve correoPrincipal.
     * @return correoPrincipal.
     */
    public String getCorreoPrincipal() {
        return correoPrincipal;
    }
    
    /**
     * Asigna correoPrincipal.
     * @param correoPrincipal nuevo valor para correoPrincipal.
     */
    public void setCorreoPrincipal(String correoPrincipal) {
        this.correoPrincipal = correoPrincipal;
    }
    
    /**
     * Devuelve correoInstitucional.
     * @return correoInstitucional.
     */
    public String getCorreoInstitucional() {
        return correoInstitucional;
    }
    
    /**
     * Asigna correoInstitucional.
     * @param correoInstitucional nuevo valor para correoInstitucional.
     */
    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }
    
    /**
     * Devuelve celular1.
     * @return celular1.
     */
    public String getCelular1() {
        return celular1;
    }
    
    /**
     * Asigna celular1.
     * @param celular1 nuevo valor para celular1.
     */
    public void setCelular1(String celular1) {
        this.celular1 = celular1;
    }
    
    /**
     * Devuelve celular2.
     * @return celular2.
     */
    public String getCelular2() {
        return celular2;
    }
    
    /**
     * Asigna celular2.
     * @param celular2 nuevo valor para celular2.
     */
    public void setCelular2(String celular2) {
        this.celular2 = celular2;
    }
    
    /**
     * Devuelve correoExtra.
     * @return correoExtra.
     */
    public String getCorreoExtra() {
        return correoExtra;
    }
    
    /**
     * Asigna correoExtra.
     * @param correoExtra nuevo valor para correoExtra.
     */
    public void setCorreoExtra(String correoExtra) {
        this.correoExtra = correoExtra;
    }
    
    /**
     * Devuelve telefonoLaboralIE.
     * @return telefonoLaboralIE.
     */
    public String getTelefonoLaboralIE() {
        return telefonoLaboralIE;
    }
    
    /**
     * Asigna telefonoLaboralIE.
     * @param telefonoLaboralIE nuevo valor para telefonoLaboralIE.
     */
    public void setTelefonoLaboralIE(String telefonoLaboralIE) {
        this.telefonoLaboralIE = telefonoLaboralIE;
    }
    
    /**
     * Devuelve correoIE.
     * @return correoIE.
     */
    public String getCorreoIE() {
        return correoIE;
    }
    
    /**
     * Asigna correoIE.
     * @param correoIE nuevo valor para correoIE.
     */
    public void setCorreoIE(String correoIE) {
        this.correoIE = correoIE;
    }
    
    /**
     * Devuelve salarioFijo.
     * @return salarioFijo.
     */
    public Double getSalarioFijo() {
        return salarioFijo;
    }
    
    /**
     * Asigna salarioFijo.
     * @param salarioFijo nuevo valor para salarioFijo.
     */
    public void setSalarioFijo(Double salarioFijo) {
        this.salarioFijo = salarioFijo;
    }
    
    /**
     * Devuelve salarioVariable.
     * @return salarioVariable.
     */
    public Double getSalarioVariable() {
        return salarioVariable;
    }
    
    /**
     * Asigna salarioVariable.
     * @param salarioVariable nuevo valor para salarioVariable.
     */
    public void setSalarioVariable(Double salarioVariable) {
        this.salarioVariable = salarioVariable;
    }
    
    /**
     * Devuelve salarioTotal.
     * @return salarioTotal.
     */
    public Double getSalarioTotal() {
        return salarioTotal;
    }
    
    /**
     * Asigna salarioTotal.
     * @param salarioTotal nuevo valor para salarioTotal.
     */
    public void setSalarioTotal(Double salarioTotal) {
        this.salarioTotal = salarioTotal;
    }
    
    /**
     * Devuelve sumadosIngresos.
     * @return sumadosIngresos.
     */
    public Double getSumadosIngresos() {
        return sumadosIngresos;
    }
    
    /**
     * Asigna sumadosIngresos.
     * @param sumadosIngresos nuevo valor para sumadosIngresos.
     */
    public void setSumadosIngresos(Double sumadosIngresos) {
        this.sumadosIngresos = sumadosIngresos;
    }
    
    /**
     * Devuelve sumadosEgresos.
     * @return sumadosEgresos.
     */
    public Double getSumadosEgresos() {
        return sumadosEgresos;
    }
    
    /**
     * Asigna sumadosEgresos.
     * @param sumadosEgresos nuevo valor para sumadosEgresos.
     */
    public void setSumadosEgresos(Double sumadosEgresos) {
        this.sumadosEgresos = sumadosEgresos;
    }
    
    /**
     * Devuelve disponible.
     * @return disponible.
     */
    public Double getDisponible() {
        return disponible;
    }
    
    /**
     * Asigna disponible.
     * @param disponible nuevo valor para disponible.
     */
    public void setDisponible(Double disponible) {
        this.disponible = disponible;
    }
}

