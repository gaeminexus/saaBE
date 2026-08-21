package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;

import com.saa.basico.util.EntidadAuditableFecha;
import com.saa.model.cnt.CentroCosto;

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
import jakarta.persistence.Transient;

/**
 * Contratos del empleado.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CNTE", schema = "RHH")
@NamedQueries({
	@NamedQuery(name = "ContratoEmpleadoId", query = "select e from ContratoEmpleado e where e.codigo=:id"),
    @NamedQuery(name = "ContratoEmpleadoAll", query = "select e from ContratoEmpleado e")
})
public class ContratoEmpleado implements Serializable, EntidadAuditableFecha {

    /**
     * Código único del contrato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "CNTECDGO")
    private Long codigo;

    /**
     * Empleado.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO", nullable = false)
    private Empleado empleado;

    /**
     * Tipo de contrato.
     */
    @ManyToOne
    @JoinColumn(name = "TPCECDGO", referencedColumnName = "TPCECDGO", nullable = false)
    private TipoContratoEmpleado tipoContratoEmpleado;

    /**
     * Número de contrato.
     */
    @Basic
    @Column(name = "CNTENMRO")
    private String numero;

    /**
     * Fecha de inicio.
     */
    @Basic
    @Column(name = "CNTEFCHI", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha de fin.
     */
    @Basic
    @Column(name = "CNTEFCHF")
    private LocalDate fechaFin;

    /**
     * Salario base.
     */
    @Basic
    @Column(name = "CNTESLRB")
    private Double salarioBase;

    /**
     * Estado del contrato.
     */
    @Basic
    @Column(name = "CNTEESTD")
    private String estado;

    /**
     * Fecha de firma.
     */
    @Basic
    @Column(name = "CNTEFRMA")
    private LocalDate fechaFirma;

    /**
     * Observación.
     */
    @Basic
    @Column(name = "CNTEOBSR")
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "CNTEFCHR")
    private LocalDate fechaRegistro;

    /**
     * Usuario que registró.
     */
    @Basic
    @Column(name = "CNTEUSRR")
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================


    /**
     * Tipo de relacion laboral: detalle del rubro RHH_TIPO_RELACION_LABORAL.
     */
    @Basic
    @Column(name = "CNTETPRL")
    private Long tipoRelacionLaboral;

    /**
     * Tipo de jornada: detalle del rubro RHH_TIPO_JORNADA.
     */
    @Basic
    @Column(name = "CNTEJRND")
    private Long jornada;

    /**
     * Horas semanales pactadas en el contrato.
     */
    @Basic
    @Column(name = "CNTEHRSM")
    private Double horasSemanales;

    /**
     * Valor de la hora pactado, aplicable a contratos por horas.
     */
    @Basic
    @Column(name = "CNTEVLHR")
    private Double valorHora;

    /**
     * Modalidad del decimo tercero: detalle del rubro RHH_MODALIDAD_DECIMO_TERCERO.
     */
    @Basic
    @Column(name = "CNTEDCTM")
    private Long modalidadDecimoTercero;

    /**
     * Modalidad del decimo cuarto: detalle del rubro RHH_MODALIDAD_DECIMO_CUARTO.
     */
    @Basic
    @Column(name = "CNTEDCCM")
    private Long modalidadDecimoCuarto;

    /**
     * Modalidad de fondos de reserva: detalle del rubro RHH_MODALIDAD_FONDOS_RESERVA.
     */
    @Basic
    @Column(name = "CNTEFRMD")
    private Long modalidadFondosReserva;

    /**
     * El contrato da derecho a decimo cuarto (S/N).
     */
    @Basic
    @Column(name = "CNTEDCMS", length = 1)
    private String derechoDecimoCuarto;

    /**
     * El contrato genera aporte al IESS (S/N); en servicios profesionales va en N.
     */
    @Basic
    @Column(name = "CNTEAPRT", length = 1)
    private String aportaIess;

    /**
     * Al honorario se le aplica retencion en la fuente por servicios (S/N).
     */
    @Basic
    @Column(name = "CNTERTFN", length = 1)
    private String retieneFuente;

    /**
     * Porcentaje de retencion en la fuente aplicable a los servicios profesionales.
     */
    @Basic
    @Column(name = "CNTEPRRF")
    private Double porcentajeRetencionFuente;

    /**
     * Codigo de ocupacion sectorial del Ministerio de Trabajo.
     */
    @Basic
    @Column(name = "CNTEOCUP", length = 20)
    private String ocupacionMdt;

    /**
     * Codigo de actividad sectorial del IESS, trece digitos. Obligatorio en el aviso
     * de entrada.
     *
     * <p><b>No es el mismo dato que {@link #ocupacionMdt}</b>, aunque los dos hablen de
     * "sectorial": aquel es la ocupacion del Ministerio de Trabajo y este el codigo con
     * el que el IESS identifica el cargo. El catalogo del IESS tiene miles de filas y
     * cambia con las comisiones sectoriales de cada anio, asi que no se carga entero:
     * cada contrato guarda el codigo del suyo.</p>
     */
    @Basic
    @Column(name = "CNTECDSC", length = 13)
    private String codigoSectorialIess;

    /**
     * Este empleador NO retiene impuesto a la renta a este trabajador (S/N).
     *
     * <p><b>No es un apaño para cuadrar un mes: es un caso de la normativa.</b> El art. 43
     * de la LRTI hace obligatoria la retencion, <b>salvo cuando el trabajador tiene varios
     * empleadores</b>: entonces presenta su proyeccion al que mas le paga, ese retiene sobre
     * el total, y a los demas les entrega copia certificada para que se abstengan. Cualquier
     * cliente va a tener este caso; el sistema tiene que saber expresarlo.</p>
     *
     * <p><b>Por que aqui y no en la proyeccion.</b> Falsear <code>PYIR</code> seria mentir
     * sobre lo que al trabajador le toca --que es correcto y que agosto necesita para
     * calcular el alcance-- y ademas seria fragil: cualquiera que invalide la proyeccion la
     * regenera y el cero desaparece sin dejar rastro. La decision de no retener es del
     * <b>empleador</b>, no del calculo, y por eso vive en el contrato.</p>
     *
     * <p><b>Y por que no <code>CNTERTFN</code>:</b> esa bandera se llama "retiene fuente" y
     * hace lo contrario --con <code>'S'</code> el contrato entra en la via de servicios
     * profesionales sin dependencia--. Reutilizarla habria escondido esto detras de un
     * nombre que ya enganya por su cuenta.</p>
     */
    @Basic
    @Column(name = "CNTENRIR", length = 1)
    private String noRetieneImpuestoRenta;

    /**
     * Por que este empleador no retiene. <b>Sin motivo escrito, la excepcion es
     * indistinguible de un error.</b>
     *
     * <p>Es el respaldo de una decision que el SRI puede preguntar: lo normal es "tiene otro
     * empleador que retiene sobre el total, con copia certificada de la proyeccion".</p>
     */
    @Basic
    @Column(name = "CNTENRMT", length = 200)
    private String motivoNoRetencion;

    /**
     * Fecha desde la que rige el cambio que se esta guardando. <b>No se persiste.</b>
     *
     * <p>Es el dato que la pantalla envia cuando toca un campo que dispara una novedad
     * del IESS --relacion laboral, codigo sectorial, jornada o dias declarados-- y sirve
     * para una sola cosa: que el plazo legal se cuente desde el dia en que el cambio
     * empezo a regir y no desde el dia en que alguien lo tecleo.</p>
     *
     * <p>No se ata a <code>NXOO</code> porque no todo cambio tiene adenda formal --un
     * ajuste de sueldo puede no llevar documento-- y esos casos se quedarian sin fecha.
     * Si no viene, la novedad usa la fecha de hoy y <b>lo dice en su observacion</b>: es
     * preferible que se vea que se estimo a que la fecha mienta en silencio.</p>
     */
    @Transient
    private LocalDate fechaVigenciaCambio;

    /**
     * Dias que se declaran al IESS en el mes. Nulo significa el mes completo.
     *
     * <p>En un contrato a tiempo parcial no son los dias trabajados del calendario sino
     * los que resultan de las horas: ocho horas hacen un dia. Con veinte horas semanales
     * salen cuatro horas al dia y quince dias declarados.</p>
     */
    @Basic
    @Column(name = "CNTEDIAD")
    private Long diasDeclaradosIess;

    /**
     * Causal por la que termino el contrato.
     */
    @ManyToOne
    @JoinColumn(name = "CNTECSTR", referencedColumnName = "CSTRCDGO")
    private CausalTerminacion causalTerminacion;

    /**
     * Fecha efectiva de terminacion del contrato.
     */
    @Basic
    @Column(name = "CNTEFCTR")
    private LocalDate fechaTerminacion;

    /**
     * Centro de costo al que se imputa el costo del contrato.
     */
    @ManyToOne
    @JoinColumn(name = "CNTECNCS", referencedColumnName = "CNCSCDGO")
    private CentroCosto centroCosto;

    /**
     * Turno asignado al contrato.
     */
    @ManyToOne
    @JoinColumn(name = "CNTETRNO", referencedColumnName = "TRNOCDGO")
    private Turno turno;

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

    public TipoContratoEmpleado getTipoContratoEmpleado() {
        return tipoContratoEmpleado;
    }

    public void setTipoContrato(TipoContratoEmpleado tipoContratoEmpleado) {
        this.tipoContratoEmpleado = tipoContratoEmpleado;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(Double salarioBase) {
        this.salarioBase = salarioBase;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaFirma() {
        return fechaFirma;
    }

    public void setFechaFirma(LocalDate fechaFirma) {
        this.fechaFirma = fechaFirma;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
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

    public Long getTipoRelacionLaboral() {
        return tipoRelacionLaboral;
    }

    public void setTipoRelacionLaboral(Long tipoRelacionLaboral) {
        this.tipoRelacionLaboral = tipoRelacionLaboral;
    }

    public Long getJornada() {
        return jornada;
    }

    public void setJornada(Long jornada) {
        this.jornada = jornada;
    }

    public Double getHorasSemanales() {
        return horasSemanales;
    }

    public void setHorasSemanales(Double horasSemanales) {
        this.horasSemanales = horasSemanales;
    }

    public Double getValorHora() {
        return valorHora;
    }

    public void setValorHora(Double valorHora) {
        this.valorHora = valorHora;
    }

    public Long getModalidadDecimoTercero() {
        return modalidadDecimoTercero;
    }

    public void setModalidadDecimoTercero(Long modalidadDecimoTercero) {
        this.modalidadDecimoTercero = modalidadDecimoTercero;
    }

    public Long getModalidadDecimoCuarto() {
        return modalidadDecimoCuarto;
    }

    public void setModalidadDecimoCuarto(Long modalidadDecimoCuarto) {
        this.modalidadDecimoCuarto = modalidadDecimoCuarto;
    }

    public Long getModalidadFondosReserva() {
        return modalidadFondosReserva;
    }

    public void setModalidadFondosReserva(Long modalidadFondosReserva) {
        this.modalidadFondosReserva = modalidadFondosReserva;
    }

    public String getDerechoDecimoCuarto() {
        return derechoDecimoCuarto;
    }

    public void setDerechoDecimoCuarto(String derechoDecimoCuarto) {
        this.derechoDecimoCuarto = derechoDecimoCuarto;
    }

    public String getAportaIess() {
        return aportaIess;
    }

    public void setAportaIess(String aportaIess) {
        this.aportaIess = aportaIess;
    }

    public String getRetieneFuente() {
        return retieneFuente;
    }

    public void setRetieneFuente(String retieneFuente) {
        this.retieneFuente = retieneFuente;
    }

    public Double getPorcentajeRetencionFuente() {
        return porcentajeRetencionFuente;
    }

    public void setPorcentajeRetencionFuente(Double porcentajeRetencionFuente) {
        this.porcentajeRetencionFuente = porcentajeRetencionFuente;
    }

    public String getOcupacionMdt() {
        return ocupacionMdt;
    }

    public void setOcupacionMdt(String ocupacionMdt) {
        this.ocupacionMdt = ocupacionMdt;
    }

    public CausalTerminacion getCausalTerminacion() {
        return causalTerminacion;
    }

    public void setCausalTerminacion(CausalTerminacion causalTerminacion) {
        this.causalTerminacion = causalTerminacion;
    }

    public LocalDate getFechaTerminacion() {
        return fechaTerminacion;
    }

    public void setFechaTerminacion(LocalDate fechaTerminacion) {
        this.fechaTerminacion = fechaTerminacion;
    }

    public CentroCosto getCentroCosto() {
        return centroCosto;
    }

    public void setCentroCosto(CentroCosto centroCosto) {
        this.centroCosto = centroCosto;
    }

    public Turno getTurno() {
        return turno;
    }

    public void setTurno(Turno turno) {
        this.turno = turno;
    }

    public String getCodigoSectorialIess() {
        return codigoSectorialIess;
    }

    public void setCodigoSectorialIess(String codigoSectorialIess) {
        this.codigoSectorialIess = codigoSectorialIess;
    }

    public Long getDiasDeclaradosIess() {
        return diasDeclaradosIess;
    }

    public void setDiasDeclaradosIess(Long diasDeclaradosIess) {
        this.diasDeclaradosIess = diasDeclaradosIess;
    }

    public String getNoRetieneImpuestoRenta() {
        return noRetieneImpuestoRenta;
    }

    public void setNoRetieneImpuestoRenta(String noRetieneImpuestoRenta) {
        this.noRetieneImpuestoRenta = noRetieneImpuestoRenta;
    }

    public String getMotivoNoRetencion() {
        return motivoNoRetencion;
    }

    public void setMotivoNoRetencion(String motivoNoRetencion) {
        this.motivoNoRetencion = motivoNoRetencion;
    }

    public LocalDate getFechaVigenciaCambio() {
        return fechaVigenciaCambio;
    }

    public void setFechaVigenciaCambio(LocalDate fechaVigenciaCambio) {
        this.fechaVigenciaCambio = fechaVigenciaCambio;
    }
}
