package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.cnt.DetallePlantilla;
import com.saa.model.cnt.PlanCuenta;
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
 * Catalogo configurable de conceptos de nomina. Es la pieza central del motor de calculo: cada ingreso, descuento, aporte patronal y provision es una fila de esta tabla.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CPNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ConceptoNominaId", query = "select e from ConceptoNomina e where e.codigo=:id"),
    @NamedQuery(name = "ConceptoNominaAll", query = "select e from ConceptoNomina e")
})
public class ConceptoNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del concepto.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CPNMCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del concepto (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Nombre del concepto.
     */
    @Basic
    @Column(name = "CPNMNMBR", length = 100)
    private String nombre;

    /**
     * Abreviatura para el rol de pago.
     */
    @Basic
    @Column(name = "CPNMABRV", length = 20)
    private String abreviatura;

    /**
     * Codigo alterno estable del concepto, unico por empresa.
     */
    @Basic
    @Column(name = "CPNMALTR")
    private Long codigoAlterno;

    /**
     * Tipo de concepto: detalle del rubro RHH_TIPO_CONCEPTO_NOMINA.
     */
    @Basic
    @Column(name = "CPNMTPCN")
    private Long tipoConcepto;

    /**
     * Tipo de calculo: detalle del rubro RHH_TIPO_CALCULO_CONCEPTO.
     */
    @Basic
    @Column(name = "CPNMTPCL")
    private Long tipoCalculo;

    /**
     * Base sobre la que se calcula: detalle del rubro RHH_BASE_CALCULO.
     */
    @Basic
    @Column(name = "CPNMBSCL")
    private Long baseCalculo;

    /**
     * Relacion laboral a la que aplica (rubro RHH_TIPO_RELACION_LABORAL); nulo aplica a todas.
     */
    @Basic
    @Column(name = "CPNMTPRL")
    private Long tipoRelacionLaboral;

    /**
     * Valor fijo, cuando el tipo de calculo es VALOR_FIJO.
     */
    @Basic
    @Column(name = "CPNMVLRR")
    private Double valor;

    /**
     * Porcentaje, cuando el tipo de calculo es PORCENTAJE_SOBRE_BASE.
     */
    @Basic
    @Column(name = "CPNMPRCN")
    private Double porcentaje;

    /**
     * Expresion de la formula, cuando el tipo de calculo es FORMULA.
     */
    @Basic
    @Column(name = "CPNMFRML", length = 500)
    private String formula;

    /**
     * Forma parte de la base imponible del IESS (S/N).
     */
    @Basic
    @Column(name = "CPNMIMIE", length = 1)
    private String imponibleIess;

    /**
     * Es gravado para el impuesto a la renta (S/N).
     */
    @Basic
    @Column(name = "CPNMIMIR", length = 1)
    private String imponibleIr;

    /**
     * Forma parte de la base de fondos de reserva (S/N).
     */
    @Basic
    @Column(name = "CPNMAPFR", length = 1)
    private String aportaFondosReserva;

    /**
     * Forma parte de la base del decimo tercero (S/N).
     */
    @Basic
    @Column(name = "CPNMBSDT", length = 1)
    private String baseDecimoTercero;

    /**
     * Forma parte de la base del decimo cuarto (S/N).
     */
    @Basic
    @Column(name = "CPNMBSDC", length = 1)
    private String baseDecimoCuarto;

    /**
     * Forma parte de la base de vacaciones (S/N).
     */
    @Basic
    @Column(name = "CPNMBSVC", length = 1)
    private String baseVacaciones;

    /**
     * Forma parte de la base de utilidades (S/N).
     */
    @Basic
    @Column(name = "CPNMBSUT", length = 1)
    private String baseUtilidades;

    /**
     * Es costo patronal y no afecta el neto (S/N).
     */
    @Basic
    @Column(name = "CPNMPTRN", length = 1)
    private String patronal;

    /**
     * Genera provision contable (S/N).
     */
    @Basic
    @Column(name = "CPNMPRVS", length = 1)
    private String provision;

    /**
     * Se aplica automaticamente a todo contrato vigente (S/N).
     */
    @Basic
    @Column(name = "CPNMOBLG", length = 1)
    private String obligatorio;

    /**
     * Puede recortarse ante neto negativo (S/N). Los descuentos de ley van en 'N'.
     */
    @Basic
    @Column(name = "CPNMRCRT", length = 1)
    private String recortable;

    /**
     * Casillero del anexo RDEP del SRI al que se acumula.
     */
    @Basic
    @Column(name = "CPNMRDEP", length = 10)
    private String casilleroRdep;

    /**
     * Codigo del concepto en la planilla de aportes del IESS.
     */
    @Basic
    @Column(name = "CPNMIESS", length = 10)
    private String codigoIess;

    /**
     * Casillero del formulario 107 al que se acumula.
     */
    @Basic
    @Column(name = "CPNMF107", length = 10)
    private String casilleroF107;

    /**
     * Cuenta contable propia del concepto (CNT.PLNN).
     */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    /**
     * Linea de plantilla contable asociada (CNT.DTPL), patron alternativo al planCuenta propio.
     */
    @ManyToOne
    @JoinColumn(name = "DTPLCDGO", referencedColumnName = "DTPLCDGO")
    private DetallePlantilla detallePlantilla;

    /**
     * Orden de presentacion en el rol y prelacion ante neto negativo.
     */
    @Basic
    @Column(name = "CPNMORDN")
    private Integer orden;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CPNMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CPNMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CPNMUSRR", length = 60)
    private String usuarioRegistro;


    /**
     * Rol del concepto dentro del motor: detalle del rubro RHH_ROL_CONCEPTO_MOTOR. Es como el motor lo localiza, sin depender del codigo alterno ni de la terna tipo/base. Nulo en los conceptos ordinarios.
     */
    @Basic
    @Column(name = "CPNMROLM")
    private Long rolMotor;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAbreviatura() {
        return abreviatura;
    }

    public void setAbreviatura(String abreviatura) {
        this.abreviatura = abreviatura;
    }

    public Long getCodigoAlterno() {
        return codigoAlterno;
    }

    public void setCodigoAlterno(Long codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    public Long getTipoConcepto() {
        return tipoConcepto;
    }

    public void setTipoConcepto(Long tipoConcepto) {
        this.tipoConcepto = tipoConcepto;
    }

    public Long getTipoCalculo() {
        return tipoCalculo;
    }

    public void setTipoCalculo(Long tipoCalculo) {
        this.tipoCalculo = tipoCalculo;
    }

    public Long getBaseCalculo() {
        return baseCalculo;
    }

    public void setBaseCalculo(Long baseCalculo) {
        this.baseCalculo = baseCalculo;
    }

    public Long getTipoRelacionLaboral() {
        return tipoRelacionLaboral;
    }

    public void setTipoRelacionLaboral(Long tipoRelacionLaboral) {
        this.tipoRelacionLaboral = tipoRelacionLaboral;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(Double porcentaje) {
        this.porcentaje = porcentaje;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getImponibleIess() {
        return imponibleIess;
    }

    public void setImponibleIess(String imponibleIess) {
        this.imponibleIess = imponibleIess;
    }

    public String getImponibleIr() {
        return imponibleIr;
    }

    public void setImponibleIr(String imponibleIr) {
        this.imponibleIr = imponibleIr;
    }

    public String getAportaFondosReserva() {
        return aportaFondosReserva;
    }

    public void setAportaFondosReserva(String aportaFondosReserva) {
        this.aportaFondosReserva = aportaFondosReserva;
    }

    public String getBaseDecimoTercero() {
        return baseDecimoTercero;
    }

    public void setBaseDecimoTercero(String baseDecimoTercero) {
        this.baseDecimoTercero = baseDecimoTercero;
    }

    public String getBaseDecimoCuarto() {
        return baseDecimoCuarto;
    }

    public void setBaseDecimoCuarto(String baseDecimoCuarto) {
        this.baseDecimoCuarto = baseDecimoCuarto;
    }

    public String getBaseVacaciones() {
        return baseVacaciones;
    }

    public void setBaseVacaciones(String baseVacaciones) {
        this.baseVacaciones = baseVacaciones;
    }

    public String getBaseUtilidades() {
        return baseUtilidades;
    }

    public void setBaseUtilidades(String baseUtilidades) {
        this.baseUtilidades = baseUtilidades;
    }

    public String getPatronal() {
        return patronal;
    }

    public void setPatronal(String patronal) {
        this.patronal = patronal;
    }

    public String getProvision() {
        return provision;
    }

    public void setProvision(String provision) {
        this.provision = provision;
    }

    public String getObligatorio() {
        return obligatorio;
    }

    public void setObligatorio(String obligatorio) {
        this.obligatorio = obligatorio;
    }

    public String getRecortable() {
        return recortable;
    }

    public void setRecortable(String recortable) {
        this.recortable = recortable;
    }

    public String getCasilleroRdep() {
        return casilleroRdep;
    }

    public void setCasilleroRdep(String casilleroRdep) {
        this.casilleroRdep = casilleroRdep;
    }

    public String getCodigoIess() {
        return codigoIess;
    }

    public void setCodigoIess(String codigoIess) {
        this.codigoIess = codigoIess;
    }

    public String getCasilleroF107() {
        return casilleroF107;
    }

    public void setCasilleroF107(String casilleroF107) {
        this.casilleroF107 = casilleroF107;
    }

    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }

    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }

    public DetallePlantilla getDetallePlantilla() {
        return detallePlantilla;
    }

    public void setDetallePlantilla(DetallePlantilla detallePlantilla) {
        this.detallePlantilla = detallePlantilla;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
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

    public Long getRolMotor() {
        return rolMotor;
    }

    public void setRolMotor(Long rolMotor) {
        this.rolMotor = rolMotor;
    }
}
