package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
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
 * Aviso de entrada, aviso de salida o novedad de sueldo a reportar al IESS, con su fecha limite legal y su estado de envio.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NVIS", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "NovedadIessId", query = "select e from NovedadIess e where e.codigo=:id"),
    @NamedQuery(name = "NovedadIessAll", query = "select e from NovedadIess e")
})
public class NovedadIess implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la novedad.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "NVISCDGO")
    private Long codigo;

    /**
     * Empleado al que se refiere la novedad.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Contrato que origina la novedad.
     */
    @ManyToOne
    @JoinColumn(name = "CNTECDGO", referencedColumnName = "CNTECDGO")
    private ContratoEmpleado contrato;

    /**
     * Tipo de novedad: detalle del rubro RHH_TIPO_NOVEDAD_IESS.
     */
    @Basic
    @Column(name = "NVISTPNV")
    private Long tipoNovedad;

    /**
     * Fecha del hecho que se reporta.
     */
    @Basic
    @Column(name = "NVISFCHC")
    private LocalDate fechaHecho;

    /**
     * Fecha limite legal para reportarlo; sale del PDTRVLRN del rubro 204.
     */
    @Basic
    @Column(name = "NVISFCLM")
    private LocalDate fechaLimite;

    /**
     * Fecha en que efectivamente se reporto al IESS.
     */
    @Basic
    @Column(name = "NVISFCRP")
    private LocalDate fechaReporte;

    /**
     * Sueldo anterior, en novedades de modificacion.
     */
    @Basic
    @Column(name = "NVISSLAN")
    private Double sueldoAnterior;

    /**
     * Sueldo nuevo, en novedades de modificacion.
     */
    @Basic
    @Column(name = "NVISSLNW")
    private Double sueldoNuevo;

    /**
     * Modalidad de fondos de reserva: detalle del rubro RHH_MODALIDAD_FONDOS_RESERVA.
     */
    @Basic
    @Column(name = "NVISMDFR")
    private Long modalidadFondosReserva;

    /**
     * Causal de terminacion, en los avisos de salida.
     */
    @ManyToOne
    @JoinColumn(name = "NVISCSTR", referencedColumnName = "CSTRCDGO")
    private CausalTerminacion causalTerminacion;

    /**
     * Estado de envio: detalle del rubro RHH_ESTADO_NOVEDAD_IESS.
     */
    @Basic
    @Column(name = "NVISESTD")
    private Long estado;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "NVISOBSR", length = 500)
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "NVISFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "NVISUSRR", length = 60)
    private String usuarioRegistro;

    // =====================================================================
    // Campos que exige el archivo de carga batch del IESS -- script 41
    //
    // Los anade NORMATIVA-IESS-NOVEDADES.md 5.2. Ninguno lleva un numero de
    // la normativa: los codigos de un digito viven en los rubros 225 a 229 y
    // se leen de PDTRVLRV, no de aqui.
    // =====================================================================

    /**
     * Dias declarados al IESS. Para el aviso de entrada y el cambio de jornada de
     * un contrato a tiempo parcial. Nulo significa mes completo.
     */
    @Basic
    @Column(name = "NVISDIAS")
    private Long diasDeclarados;

    /**
     * Sueldo referencial de treinta dias, el que corresponderia a jornada completa.
     * Nunca menor al SBU. Lo pide el aviso de entrada de un tiempo parcial.
     */
    @Basic
    @Column(name = "NVISSLRF")
    private Double sueldoReferencial;

    /**
     * Valor de la variacion del mes, para la novedad de variacion por extras.
     */
    @Basic
    @Column(name = "NVISVLVR")
    private Double valorVariacion;

    /**
     * Codigo IESS de la causa. Sale de <code>PDTRVLRV</code>: del rubro
     * {@link com.saa.rubros.Rubros#RHH_CAUSA_SALIDA_IESS} en el aviso de salida y del
     * {@link com.saa.rubros.Rubros#RHH_CAUSA_VARIACION_IESS} en la variacion por extras.
     *
     * <p>Se guarda el codigo y no la referencia al detalle porque es lo que viaja en
     * el archivo, y el archivo tiene que poder reconstruirse tal cual se envio aunque
     * el catalogo cambie despues.</p>
     */
    @Basic
    @Column(name = "NVISCAIS", length = 2)
    private String causaIess;

    /**
     * Fecha de fallecimiento, solo cuando la causa de la salida es esa.
     */
    @Basic
    @Column(name = "NVISFCFL")
    private LocalDate fechaFallecimiento;

    /**
     * Fecha de fin, para las licencias sin remuneracion.
     */
    @Basic
    @Column(name = "NVISFCFN")
    private LocalDate fechaFin;

    /**
     * Periodo de fondos de reserva, desde. Formato <code>YYYY-MM</code>, que es el que
     * imprime el archivo.
     */
    @Basic
    @Column(name = "NVISPRDS", length = 7)
    private String periodoDesde;

    /**
     * Periodo de fondos de reserva, hasta. Formato <code>YYYY-MM</code>.
     */
    @Basic
    @Column(name = "NVISPRHS", length = 7)
    private String periodoHasta;

    /**
     * Meses laborados del periodo de fondos de reserva.
     */
    @Basic
    @Column(name = "NVISMSLB")
    private Long mesesLaborados;

    /**
     * Respuesta del IESS. Lleva el motivo cuando la novedad vuelve rechazada.
     */
    @Basic
    @Column(name = "NVISRSPT", length = 500)
    private String respuestaIess;

    /**
     * Lote o comprobante del envio batch, para poder rastrear una novedad hasta el
     * archivo en que se mando.
     */
    @Basic
    @Column(name = "NVISLOTE", length = 60)
    private String lote;

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

    public ContratoEmpleado getContrato() {
        return contrato;
    }

    public void setContrato(ContratoEmpleado contrato) {
        this.contrato = contrato;
    }

    public Long getTipoNovedad() {
        return tipoNovedad;
    }

    public void setTipoNovedad(Long tipoNovedad) {
        this.tipoNovedad = tipoNovedad;
    }

    public LocalDate getFechaHecho() {
        return fechaHecho;
    }

    public void setFechaHecho(LocalDate fechaHecho) {
        this.fechaHecho = fechaHecho;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public LocalDate getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(LocalDate fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    public Double getSueldoAnterior() {
        return sueldoAnterior;
    }

    public void setSueldoAnterior(Double sueldoAnterior) {
        this.sueldoAnterior = sueldoAnterior;
    }

    public Double getSueldoNuevo() {
        return sueldoNuevo;
    }

    public void setSueldoNuevo(Double sueldoNuevo) {
        this.sueldoNuevo = sueldoNuevo;
    }

    public Long getModalidadFondosReserva() {
        return modalidadFondosReserva;
    }

    public void setModalidadFondosReserva(Long modalidadFondosReserva) {
        this.modalidadFondosReserva = modalidadFondosReserva;
    }

    public CausalTerminacion getCausalTerminacion() {
        return causalTerminacion;
    }

    public void setCausalTerminacion(CausalTerminacion causalTerminacion) {
        this.causalTerminacion = causalTerminacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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

    public Long getDiasDeclarados() {
        return diasDeclarados;
    }

    public void setDiasDeclarados(Long diasDeclarados) {
        this.diasDeclarados = diasDeclarados;
    }

    public Double getSueldoReferencial() {
        return sueldoReferencial;
    }

    public void setSueldoReferencial(Double sueldoReferencial) {
        this.sueldoReferencial = sueldoReferencial;
    }

    public Double getValorVariacion() {
        return valorVariacion;
    }

    public void setValorVariacion(Double valorVariacion) {
        this.valorVariacion = valorVariacion;
    }

    public String getCausaIess() {
        return causaIess;
    }

    public void setCausaIess(String causaIess) {
        this.causaIess = causaIess;
    }

    public LocalDate getFechaFallecimiento() {
        return fechaFallecimiento;
    }

    public void setFechaFallecimiento(LocalDate fechaFallecimiento) {
        this.fechaFallecimiento = fechaFallecimiento;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getPeriodoDesde() {
        return periodoDesde;
    }

    public void setPeriodoDesde(String periodoDesde) {
        this.periodoDesde = periodoDesde;
    }

    public String getPeriodoHasta() {
        return periodoHasta;
    }

    public void setPeriodoHasta(String periodoHasta) {
        this.periodoHasta = periodoHasta;
    }

    public Long getMesesLaborados() {
        return mesesLaborados;
    }

    public void setMesesLaborados(Long mesesLaborados) {
        this.mesesLaborados = mesesLaborados;
    }

    public String getRespuestaIess() {
        return respuestaIess;
    }

    public void setRespuestaIess(String respuestaIess) {
        this.respuestaIess = respuestaIess;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }
}
