package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Proyeccion anual del impuesto a la renta de un empleado, con la retencion mensual que el motor de nomina descuenta. Solo una version por empleado y anio esta vigente.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PYIR", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ProyeccionImpuestoRentaId", query = "select e from ProyeccionImpuestoRenta e where e.codigo=:id"),
    @NamedQuery(name = "ProyeccionImpuestoRentaAll", query = "select e from ProyeccionImpuestoRenta e")
})
public class ProyeccionImpuestoRenta implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la proyeccion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "PYIRCDGO")
    private Long codigo;

    /**
     * Empleado proyectado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Anio fiscal de la proyeccion.
     */
    @Basic
    @Column(name = "PYIRANOO")
    private Integer anio;

    /**
     * Mes desde el que rige la proyeccion.
     */
    @Basic
    @Column(name = "PYIRMSDS")
    private Integer mesDesde;

    /**
     * Ingresos gravados ya percibidos en el anio.
     */
    @Basic
    @Column(name = "PYIRINRL")
    private Double ingresosRealizados;

    /**
     * Ingresos gravados futuros proyectados.
     */
    @Basic
    @Column(name = "PYIRINFT")
    private Double ingresosFuturos;

    /**
     * Total de ingresos gravados proyectados del anio.
     */
    @Basic
    @Column(name = "PYIRINPR")
    private Double ingresosProyectados;

    /**
     * Aporte personal al IESS proyectado del anio.
     */
    @Basic
    @Column(name = "PYIRAPPR")
    private Double aportePersonalProyectado;

    /**
     * Base imponible: ingresos proyectados menos aporte personal.
     */
    @Basic
    @Column(name = "PYIRBSIM")
    private Double baseImponible;

    /**
     * Impuesto causado segun la tabla progresiva.
     */
    @Basic
    @Column(name = "PYIRIMCS")
    private Double impuestoCausado;

    /**
     * Gastos personales declarados por el empleado.
     */
    @Basic
    @Column(name = "PYIRGSDC")
    private Double gastosDeclarados;

    /**
     * Tope de gastos personales segun el numero de cargas.
     */
    @Basic
    @Column(name = "PYIRTPGS")
    private Double topeGastos;

    /**
     * Rebaja aplicada por gastos personales.
     */
    @Basic
    @Column(name = "PYIRRBJA")
    private Double rebaja;

    /**
     * Impuesto a pagar del anio, con piso en cero.
     */
    @Basic
    @Column(name = "PYIRIMPG")
    private Double impuestoAPagar;

    /**
     * Retenciones ya efectuadas en el anio.
     */
    @Basic
    @Column(name = "PYIRRTEF")
    private Double retencionesEfectuadas;

    /**
     * Meses restantes del anio sobre los que se prorratea.
     */
    @Basic
    @Column(name = "PYIRMSRS")
    private Integer mesesRestantes;

    /**
     * Retencion mensual resultante.
     */
    @Basic
    @Column(name = "PYIRRTEM")
    private Double retencionMensual;

    /**
     * Numero de cargas familiares consideradas.
     */
    @Basic
    @Column(name = "PYIRNCRG")
    private Integer numeroCargas;

    /**
     * Aplico el tope ampliado por enfermedad catastrofica (S/N).
     */
    @Basic
    @Column(name = "PYIRCTSF", length = 1)
    private String enfermedadCatastrofica;

    /**
     * Es la proyeccion vigente (S/N). Solo una por empleado y anio.
     */
    @Basic
    @Column(name = "PYIRVGNT", length = 1)
    private String vigente;

    /**
     * Motivo de la reproyeccion.
     */
    @Basic
    @Column(name = "PYIRMTVO", length = 300)
    private String motivo;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "PYIRESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "PYIRFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "PYIRUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getMesDesde() {
        return mesDesde;
    }

    public void setMesDesde(Integer mesDesde) {
        this.mesDesde = mesDesde;
    }

    public Double getIngresosRealizados() {
        return ingresosRealizados;
    }

    public void setIngresosRealizados(Double ingresosRealizados) {
        this.ingresosRealizados = ingresosRealizados;
    }

    public Double getIngresosFuturos() {
        return ingresosFuturos;
    }

    public void setIngresosFuturos(Double ingresosFuturos) {
        this.ingresosFuturos = ingresosFuturos;
    }

    public Double getIngresosProyectados() {
        return ingresosProyectados;
    }

    public void setIngresosProyectados(Double ingresosProyectados) {
        this.ingresosProyectados = ingresosProyectados;
    }

    public Double getAportePersonalProyectado() {
        return aportePersonalProyectado;
    }

    public void setAportePersonalProyectado(Double aportePersonalProyectado) {
        this.aportePersonalProyectado = aportePersonalProyectado;
    }

    public Double getBaseImponible() {
        return baseImponible;
    }

    public void setBaseImponible(Double baseImponible) {
        this.baseImponible = baseImponible;
    }

    public Double getImpuestoCausado() {
        return impuestoCausado;
    }

    public void setImpuestoCausado(Double impuestoCausado) {
        this.impuestoCausado = impuestoCausado;
    }

    public Double getGastosDeclarados() {
        return gastosDeclarados;
    }

    public void setGastosDeclarados(Double gastosDeclarados) {
        this.gastosDeclarados = gastosDeclarados;
    }

    public Double getTopeGastos() {
        return topeGastos;
    }

    public void setTopeGastos(Double topeGastos) {
        this.topeGastos = topeGastos;
    }

    public Double getRebaja() {
        return rebaja;
    }

    public void setRebaja(Double rebaja) {
        this.rebaja = rebaja;
    }

    public Double getImpuestoAPagar() {
        return impuestoAPagar;
    }

    public void setImpuestoAPagar(Double impuestoAPagar) {
        this.impuestoAPagar = impuestoAPagar;
    }

    public Double getRetencionesEfectuadas() {
        return retencionesEfectuadas;
    }

    public void setRetencionesEfectuadas(Double retencionesEfectuadas) {
        this.retencionesEfectuadas = retencionesEfectuadas;
    }

    public Integer getMesesRestantes() {
        return mesesRestantes;
    }

    public void setMesesRestantes(Integer mesesRestantes) {
        this.mesesRestantes = mesesRestantes;
    }

    public Double getRetencionMensual() {
        return retencionMensual;
    }

    public void setRetencionMensual(Double retencionMensual) {
        this.retencionMensual = retencionMensual;
    }

    public Integer getNumeroCargas() {
        return numeroCargas;
    }

    public void setNumeroCargas(Integer numeroCargas) {
        this.numeroCargas = numeroCargas;
    }

    public String getEnfermedadCatastrofica() {
        return enfermedadCatastrofica;
    }

    public void setEnfermedadCatastrofica(String enfermedadCatastrofica) {
        this.enfermedadCatastrofica = enfermedadCatastrofica;
    }

    public String getVigente() {
        return vigente;
    }

    public void setVigente(String vigente) {
        this.vigente = vigente;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
