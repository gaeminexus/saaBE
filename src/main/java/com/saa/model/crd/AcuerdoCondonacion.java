package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

import com.saa.model.scp.Empresa;

/**
 * Representa la tabla CRD.ACCN (AcuerdoCondonacion).
 *
 * Ciclo de registro y aprobación de un acuerdo de pago con condonación (Frente K): crédito
 * registra qué se paga y qué se condona por concepto, un SEGUNDO usuario aprueba o rechaza la
 * condonación. Es previo a la aplicación real — recién cuando el cobro por la parte pagada
 * se PROCESA en {@link CobroCredito} se genera el {@link EventoPrestamo} (K8) y se afecta el
 * préstamo.
 *
 * Con el préstamo quedando CANCELADO (K6) al aplicarse, esta tabla es la ÚNICA fuente
 * consultable de cuánto se condonó, a quién y quién lo autorizó.
 *
 * Ver {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md}.
 *
 * @see com.saa.rubros.CrdEstadoAcuerdoCondonacion
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ACCN", schema = "CRD")
@SequenceGenerator(name = "SQ_ACCNCDGO", sequenceName = "CRD.SQ_ACCNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AcuerdoCondonacionAll", query = "select e from AcuerdoCondonacion e"),
    @NamedQuery(name = "AcuerdoCondonacionId",  query = "select e from AcuerdoCondonacion e where e.codigo = :id")
})
public class AcuerdoCondonacion implements Serializable {

    /** Código del acuerdo. PK. */
    @Id
    @Basic
    @Column(name = "ACCNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ACCNCDGO")
    private Long codigo;

    /** FK - Entidad (partícipe) titular del préstamo. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Préstamo del acuerdo. Debe estar EN_MORA(11) o DE_PLAZO_VENCIDO(8) — K7, PRSTIDST nunca ESPSCDGO. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Estado: ver {@link com.saa.rubros.CrdEstadoAcuerdoCondonacion} (rubro 247). */
    @Basic
    @Column(name = "ACCNESTD")
    private Long estado;

    /**
     * Valor total a pagar — la parte NO condonada (capital pagado + seguros al 100%, K3). El
     * "piso". INVARIANTE: SIEMPRE es la suma de {@code DetalleAcuerdoCondonacion.valorPagado}
     * de las 5 líneas — nunca un dato de entrada independiente. Ver
     * {@code AcuerdoCondonacionService#registrarAcuerdo}.
     */
    @Basic
    @Column(name = "ACCNVLPG")
    private Double valorPagar;

    /**
     * Valor total a condonar. Mismo invariante que {@link #valorPagar}: SIEMPRE es la suma de
     * {@code DetalleAcuerdoCondonacion.valorCondonado}.
     */
    @Basic
    @Column(name = "ACCNVLCN")
    private Double valorCondonar;

    /**
     * Parte de {@link #valorPagar} cubierta cruzando saldos de aportes del propio socio.
     * Su composición por tipo de aporte está en {@code CRD.DAAP}
     * ({@link DetalleAporteAcuerdoCondonacion}). NO genera cobro en {@code CRD.CBCR}: ahí no
     * entra dinero al banco, y por lo tanto no hay nada que contabilidad pueda verificar.
     *
     * INVARIANTE: {@code valorPagarAportes + valorPagarDeposito = valorPagar} (tolerancia
     * $0.01, validada en {@code AcuerdoCondonacionService#registrarAcuerdo}).
     */
    @Basic
    @Column(name = "ACCNVLAP")
    private Double valorPagarAportes;

    /**
     * Parte de {@link #valorPagar} cubierta con depósito o transferencia. ES LA ÚNICA que
     * genera cobro en {@code CRD.CBCR} y aprobación de contabilidad — la única donde entra
     * dinero al banco. Si vale 0, el acuerdo se aplica en el mismo acto del registro, sin
     * pasar por la bandeja (ver {@code AcuerdoCondonacionService#registrarAcuerdo}: K11 hace
     * esperar la aprobación para protegerse de que el depósito nunca llegue — cancelar antes
     * de verificarlo dejaría un préstamo condonado contra dinero inexistente. Un saldo de
     * aportes que ya está en el sistema no tiene ese riesgo: no hay nada que esperar).
     */
    @Basic
    @Column(name = "ACCNVLDP")
    private Double valorPagarDeposito;

    /** Fecha de negocio del acuerdo. */
    @Basic
    @Column(name = "ACCNFCHA")
    private LocalDate fecha;

    /** Observación del usuario. */
    @Basic
    @Column(name = "ACCNOBSR", length = 2000)
    private String observacion;

    /** Usuario que registró el acuerdo. */
    @Basic
    @Column(name = "ACCNUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha de registro. */
    @Basic
    @Column(name = "ACCNFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * ⚠️ SIN USO desde el 2026-08-30 (K4 derogada): nació para el segundo usuario que
     * aprobaba la condonación, pero ya no hay esa aprobación — la previsualización en
     * pantalla la reemplaza. La columna queda en la tabla (no se dropea) pero ningún código
     * la escribe. No confundir con la aprobación del cobro en CBCR, que sigue existiendo.
     */
    @Basic
    @Column(name = "ACCNUSAP", length = 50)
    private String usuarioAprobacion;

    /** Sin uso — ver {@link #usuarioAprobacion}. */
    @Basic
    @Column(name = "ACCNFCAP")
    private LocalDateTime fechaAprobacion;

    /**
     * Usuario que anuló el acuerdo (copiado de {@code CobroCredito.usuarioAnulacion} por
     * {@code AcuerdoCondonacionService#anularAcuerdoPorCobro}).
     *
     * ⚠️ RECICLADO el 2026-08-30 al derogarse K4/K10: esta columna (ACCNUSRC) nació como
     * "usuario que RECHAZÓ la condonación". Ya no hay rechazo de condonación — el nombre del
     * campo Java quedó igual para no arrastrar un rename de bajo valor, pero el significado es
     * el de la anulación. {@code CK_ACCN_MTRC} (que exigía motivo para RECHAZADO=3) sigue
     * activo con el mismo código para ANULADO=3 — si algún día se toca este campo sin pasar
     * por {@code anularAcuerdoPorCobro}, ese CHECK es lo que evita dejarlo sin motivo.
     */
    @Basic
    @Column(name = "ACCNUSRC", length = 50)
    private String usuarioRechazo;

    /** Fecha de anulación (mismo reciclado que {@link #usuarioRechazo}). */
    @Basic
    @Column(name = "ACCNFCRC")
    private LocalDateTime fechaRechazo;

    /**
     * Motivo de la anulación (mismo reciclado que {@link #usuarioRechazo}). El acuerdo
     * anulado CONSERVA su registro: sigue siendo cierto que alguien negoció perdonar dinero,
     * aunque no se haya cobrado.
     */
    @Basic
    @Column(name = "ACCNMTRC", length = 2000)
    private String motivoRechazo;

    /** FK - EventoPrestamo de la aplicación real (pago + condonación + CANCELADO). NULL hasta procesarse (K8). */
    @ManyToOne
    @JoinColumn(name = "EVPRCDGO", referencedColumnName = "EVPRCDGO")
    private EventoPrestamo eventoPrestamo;

    /** FK - Cobro en CRD.CBCR por la parte pagada. Se llena al registrar el cobro (paso 3, §5 del plan). */
    @ManyToOne
    @JoinColumn(name = "CBCRCDGO", referencedColumnName = "CBCRCDGO")
    private CobroCredito cobroCredito;

    /**
     * FK - Empresa (nodo de jerarquía SCP.PJRQ) del acuerdo. Agregada el 2026-08-30
     * ({@code sql/86_ACUERDO_EMPRESA.sql}): la ÚNICA fuente de empresa para contabilizar el
     * acuerdo, en los DOS caminos (con depósito y 100% aportes) — NUNCA derivarla de
     * {@code cobroCredito}, porque un acuerdo 100% aportes no tiene cobro. Obligatoria en el
     * servicio ({@code AcuerdoCondonacionService#registrarAcuerdo}), aunque la columna es
     * nullable en la base a propósito (no se inventa un valor para una fila histórica).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Double getValorPagar() {
        return valorPagar;
    }

    public void setValorPagar(Double valorPagar) {
        this.valorPagar = valorPagar;
    }

    public Double getValorCondonar() {
        return valorCondonar;
    }

    public void setValorCondonar(Double valorCondonar) {
        this.valorCondonar = valorCondonar;
    }

    public Double getValorPagarAportes() {
        return valorPagarAportes;
    }

    public void setValorPagarAportes(Double valorPagarAportes) {
        this.valorPagarAportes = valorPagarAportes;
    }

    public Double getValorPagarDeposito() {
        return valorPagarDeposito;
    }

    public void setValorPagarDeposito(Double valorPagarDeposito) {
        this.valorPagarDeposito = valorPagarDeposito;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioAprobacion() {
        return usuarioAprobacion;
    }

    public void setUsuarioAprobacion(String usuarioAprobacion) {
        this.usuarioAprobacion = usuarioAprobacion;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getUsuarioRechazo() {
        return usuarioRechazo;
    }

    public void setUsuarioRechazo(String usuarioRechazo) {
        this.usuarioRechazo = usuarioRechazo;
    }

    public LocalDateTime getFechaRechazo() {
        return fechaRechazo;
    }

    public void setFechaRechazo(LocalDateTime fechaRechazo) {
        this.fechaRechazo = fechaRechazo;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public EventoPrestamo getEventoPrestamo() {
        return eventoPrestamo;
    }

    public void setEventoPrestamo(EventoPrestamo eventoPrestamo) {
        this.eventoPrestamo = eventoPrestamo;
    }

    public CobroCredito getCobroCredito() {
        return cobroCredito;
    }

    public void setCobroCredito(CobroCredito cobroCredito) {
        this.cobroCredito = cobroCredito;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
}
