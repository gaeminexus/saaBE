package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
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
 * Configuracion de nomina por empresa: plantillas y tipos de asiento contable, banderas de funcionalidad y tolerancia de cuadre.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CFNM", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "ConfiguracionNominaId", query = "select e from ConfiguracionNomina e where e.codigo=:id"),
    @NamedQuery(name = "ConfiguracionNominaAll", query = "select e from ConfiguracionNomina e")
})
public class ConfiguracionNomina implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la configuracion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CFNMCDGO")
    private Long codigo;

    /**
     * Empresa propietaria de la configuracion (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Codigo alterno de la plantilla (CNT.PLNS) del asiento de rol de pagos.
     */
    @Basic
    @Column(name = "CFNMPLRL")
    private Long plantillaRol;

    /**
     * Codigo alterno de la plantilla del asiento de provisiones.
     */
    @Basic
    @Column(name = "CFNMPLPR")
    private Long plantillaProvision;

    /**
     * Codigo alterno de la plantilla del asiento de pago.
     */
    @Basic
    @Column(name = "CFNMPLPG")
    private Long plantillaPago;

    /**
     * Codigo alterno de la plantilla del asiento de liquidacion.
     */
    @Basic
    @Column(name = "CFNMPLLQ")
    private Long plantillaLiquidacion;

    /**
     * Codigo alterno del tipo de asiento (CNT.PLNT) del rol de pagos.
     */
    @Basic
    @Column(name = "CFNMTARL")
    private Long tipoAsientoRol;

    /**
     * Codigo alterno del tipo de asiento de provisiones.
     */
    @Basic
    @Column(name = "CFNMTAPR")
    private Long tipoAsientoProvision;

    /**
     * Codigo alterno del tipo de asiento de pago.
     */
    @Basic
    @Column(name = "CFNMTAPG")
    private Long tipoAsientoPago;

    /**
     * Codigo alterno del tipo de asiento de liquidacion.
     */
    @Basic
    @Column(name = "CFNMTALQ")
    private Long tipoAsientoLiquidacion;

    /**
     * Desglosa el asiento por centro de costo (S/N).
     */
    @Basic
    @Column(name = "CFNMDCCS", length = 1)
    private String desglosaCentroCosto;

    /**
     * Aplica reparto de utilidades (S/N). En ASOPREP es 'N'.
     */
    @Basic
    @Column(name = "CFNMAPUT", length = 1)
    private String aplicaUtilidades;

    /**
     * Aplica provision de jubilacion patronal (S/N).
     */
    @Basic
    @Column(name = "CFNMAPJP", length = 1)
    private String aplicaJubilacionPatronal;

    /**
     * Aplica provision de desahucio (S/N).
     */
    @Basic
    @Column(name = "CFNMAPDS", length = 1)
    private String aplicaDesahucio;

    /**
     * Redondea cada renglon a dos decimales (S/N).
     */
    @Basic
    @Column(name = "CFNMRDND", length = 1)
    private String redondeaRenglon;

    /**
     * Tolerancia de cuadre del asiento; diferencias menores se ajustan en la linea de cuadre.
     */
    @Basic
    @Column(name = "CFNMTLCD")
    private Double toleranciaCuadre;

    /**
     * PLNNCDGO de la cuenta contable MARCADORA.
     *
     * <p>Una linea de plantilla que todavia apunte a esta cuenta esta sin configurar:
     * <code>validarCuentasContables</code> la reporta y bloquea la contabilizacion. <b>No es una
     * cuenta real de ningun asiento.</b> Vive aqui y no como literal en Java para respetar la
     * regla 1 del maestro: hoy vale 9678, y cambiarlo es un UPDATE.</p>
     */
    @Basic
    @Column(name = "CFNMCTMR")
    private Long cuentaMarcadora;

    /**
     * Codigo de sucursal del IESS, cuatro digitos. Va en la cabecera de cada registro
     * del archivo de carga batch.
     */
    @Basic
    @Column(name = "CFNMSCIE", length = 4)
    private String sucursalIess;

    /**
     * Codigo del tipo de empleador que el IESS asigno a la empresa. Va en la cabecera
     * del aviso de entrada.
     */
    @Basic
    @Column(name = "CFNMTPEM", length = 10)
    private String tipoEmpleadorIess;

    /**
     * Codigo alterno del detalle del rubro RHH_CODIGO_SEGURO_SOCIAL_IESS que aplica a la
     * empresa. Va en el aviso de entrada.
     *
     * <p>Es de empresa y no de contrato porque en una plantilla normal todos estan bajo el
     * mismo regimen. El dia que una instalacion mezcle regimenes, sube a <code>CNTE</code>
     * como columna propia, igual que la jornada y la relacion de trabajo.</p>
     */
    @Basic
    @Column(name = "CFNMSGSC")
    private Long seguroSocialIess;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "CFNMESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CFNMFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "CFNMUSRR", length = 60)
    private String usuarioRegistro;

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

    public Long getPlantillaRol() {
        return plantillaRol;
    }

    public void setPlantillaRol(Long plantillaRol) {
        this.plantillaRol = plantillaRol;
    }

    public Long getPlantillaProvision() {
        return plantillaProvision;
    }

    public void setPlantillaProvision(Long plantillaProvision) {
        this.plantillaProvision = plantillaProvision;
    }

    public Long getPlantillaPago() {
        return plantillaPago;
    }

    public void setPlantillaPago(Long plantillaPago) {
        this.plantillaPago = plantillaPago;
    }

    public Long getPlantillaLiquidacion() {
        return plantillaLiquidacion;
    }

    public void setPlantillaLiquidacion(Long plantillaLiquidacion) {
        this.plantillaLiquidacion = plantillaLiquidacion;
    }

    public Long getTipoAsientoRol() {
        return tipoAsientoRol;
    }

    public void setTipoAsientoRol(Long tipoAsientoRol) {
        this.tipoAsientoRol = tipoAsientoRol;
    }

    public Long getTipoAsientoProvision() {
        return tipoAsientoProvision;
    }

    public void setTipoAsientoProvision(Long tipoAsientoProvision) {
        this.tipoAsientoProvision = tipoAsientoProvision;
    }

    public Long getTipoAsientoPago() {
        return tipoAsientoPago;
    }

    public void setTipoAsientoPago(Long tipoAsientoPago) {
        this.tipoAsientoPago = tipoAsientoPago;
    }

    public Long getTipoAsientoLiquidacion() {
        return tipoAsientoLiquidacion;
    }

    public void setTipoAsientoLiquidacion(Long tipoAsientoLiquidacion) {
        this.tipoAsientoLiquidacion = tipoAsientoLiquidacion;
    }

    public String getDesglosaCentroCosto() {
        return desglosaCentroCosto;
    }

    public void setDesglosaCentroCosto(String desglosaCentroCosto) {
        this.desglosaCentroCosto = desglosaCentroCosto;
    }

    public String getAplicaUtilidades() {
        return aplicaUtilidades;
    }

    public void setAplicaUtilidades(String aplicaUtilidades) {
        this.aplicaUtilidades = aplicaUtilidades;
    }

    public String getAplicaJubilacionPatronal() {
        return aplicaJubilacionPatronal;
    }

    public void setAplicaJubilacionPatronal(String aplicaJubilacionPatronal) {
        this.aplicaJubilacionPatronal = aplicaJubilacionPatronal;
    }

    public String getAplicaDesahucio() {
        return aplicaDesahucio;
    }

    public void setAplicaDesahucio(String aplicaDesahucio) {
        this.aplicaDesahucio = aplicaDesahucio;
    }

    public String getRedondeaRenglon() {
        return redondeaRenglon;
    }

    public void setRedondeaRenglon(String redondeaRenglon) {
        this.redondeaRenglon = redondeaRenglon;
    }

    public Double getToleranciaCuadre() {
        return toleranciaCuadre;
    }

    public void setToleranciaCuadre(Double toleranciaCuadre) {
        this.toleranciaCuadre = toleranciaCuadre;
    }

    public Long getCuentaMarcadora() {
        return cuentaMarcadora;
    }

    public void setCuentaMarcadora(Long cuentaMarcadora) {
        this.cuentaMarcadora = cuentaMarcadora;
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

    public String getSucursalIess() {
        return sucursalIess;
    }

    public void setSucursalIess(String sucursalIess) {
        this.sucursalIess = sucursalIess;
    }

    public String getTipoEmpleadorIess() {
        return tipoEmpleadorIess;
    }

    public void setTipoEmpleadorIess(String tipoEmpleadorIess) {
        this.tipoEmpleadorIess = tipoEmpleadorIess;
    }

    public Long getSeguroSocialIess() {
        return seguroSocialIess;
    }

    public void setSeguroSocialIess(Long seguroSocialIess) {
        this.seguroSocialIess = seguroSocialIess;
    }
}
