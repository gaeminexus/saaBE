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
import jakarta.persistence.Table;

/**
 * Representa la tabla DVAP (DevolucionAporte).
 *
 * Documento de origen de la devolución de dinero de los aportes de un partícipe. Al
 * registrarse genera las filas NEGATIVAS de CRD.APRT (con su CRD.PGAP) y dispara una orden
 * de pago en CXP; cuando el pago queda confirmado, la devolución se marca como PAGADA.
 *
 * Ciclo de vida (DVAPESTD), ver {@link com.saa.rubros.EstadoDevolucionAporte}:
 *
 *   1 REGISTRADA --orden de pago--> 2 EN_PAGO --pago confirmado--> 3 PAGADA
 *        |                              |
 *        +--> 5 ANULADA (motivo)        +--pago rechazado / reversado--> 4 RECHAZADA
 *                                            (contra-movimientos positivos en APRT)
 *
 * <b>{@code idPagoProgramado}, {@code numeroAsiento} e {@code idEmpresa} son números sin
 * FK, a propósito.</b> El sistema se comercializa después SIN el módulo crd, así que no
 * puede quedar integridad referencial entre CRD y PGS/CNT en ninguna de las dos
 * direcciones. La consistencia la garantiza el reconciliador
 * ({@code DevolucionAporteService.sincronizarPagos}), no la base de datos.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DVAP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DevolucionAporteAll", query = "select e from DevolucionAporte e"),
    @NamedQuery(name = "DevolucionAporteId",  query = "select e from DevolucionAporte e where e.codigo = :id")
})
public class DevolucionAporte implements Serializable {

    /** Código de la devolución. */
    @Id
    @Basic
    @Column(name = "DVAPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Partícipe al que se devuelve el dinero. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Filial del partícipe al momento de la devolución. */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** FK - Cuenta bancaria del partícipe a la que se transfiere. */
    @ManyToOne
    @JoinColumn(name = "CNBPCDGO", referencedColumnName = "CNBPCDGO")
    private CuentaBancariaParticipe cuentaParticipe;

    /** Valor total devuelto. Iguala la suma de los detalles. */
    @Basic
    @Column(name = "DVAPVLRR")
    private Double valor;

    /** Fecha de negocio de la devolución. No puede ser futura. */
    @Basic
    @Column(name = "DVAPFCHA")
    private LocalDate fecha;

    /** Motivo u observación que escribe el usuario. */
    @Basic
    @Column(name = "DVAPMTVO", length = 2000)
    private String motivo;

    /**
     * Estado.
     * 1=Registrada, 2=En pago, 3=Pagada, 4=Rechazada, 5=Anulada
     */
    @Basic
    @Column(name = "DVAPESTD")
    private Long estado;

    /**
     * Orden de pago generada en CXP (PGS.PGTR.PGTRCDGO).
     * <b>Sin FK a propósito</b>: CRD no ata el esquema PGS.
     */
    @Basic
    @Column(name = "DVAPIDPG")
    private Long idPagoProgramado;

    /**
     * Código del asiento contable del PAGO (D 2.3.01.05.01/2.3.01.10.01 → H Banco), copiado
     * por el reconciliador al confirmarse. Lo genera CXP, no CRD. <b>Sin FK a propósito.</b>
     *
     * <b>Distinto de {@link #numeroAsientoReclasificacion}</b> — opción C, decisión del
     * usuario 2026-08-31: son las dos mitades de un mismo movimiento contable, generadas por
     * módulos distintos en momentos distintos. Guardar las dos acá perdería la del primero:
     * {@code aplicarPagado} sobreescribe este campo con el asiento de CXP en cuanto el pago
     * se confirma.
     */
    @Basic
    @Column(name = "DVAPNMAS")
    private Long numeroAsiento;

    /**
     * Código del asiento de RECLASIFICACIÓN (D 2.1.01.05.01/2.1.02.05.01 → H
     * 2.3.01.05.01/2.3.01.10.01) que genera CRD al REGISTRAR la devolución — opción C,
     * decisión del usuario 2026-08-31 (script crd/sql/90_DEVOLUCION_APORTES_RECLASIFICACION.sql).
     *
     * Es {@code CNT.ASNT.ASNTCDGO} (la PK), NO {@code ASNTNMRO} (el correlativo por
     * empresa/período) — misma convención que {@code EventoPrestamo.numeroAsiento} y
     * {@link #numeroAsiento}: toda la mecánica de reverso del sistema
     * ({@code AsientoService.anulaAsiento}) recibe el id, no el número.
     *
     * <b>Separada de {@link #numeroAsiento} a propósito</b>: esa guarda el asiento de PAGO
     * que genera CXP, y {@code aplicarPagado} la sobreescribe al confirmarse el pago — si la
     * reclasificación se guardara ahí, se perdería la referencia y no se podría reversar
     * nunca. Son dos asientos de dos módulos distintos: dos columnas.
     *
     * {@code NULL} en toda devolución anterior al 2026-08-31 y en toda devolución registrada
     * con la contabilidad de CRD apagada — no es un dato faltante, es la ausencia esperada.
     */
    @Basic
    @Column(name = "DVAPNMRC")
    private Long numeroAsientoReclasificacion;

    /** Fecha en que el banco confirmó el pago. */
    @Basic
    @Column(name = "DVAPFCPG")
    private LocalDate fechaPago;

    /**
     * Empresa contable con la que se generó la orden de pago.
     * <b>Sin FK a propósito.</b>
     */
    @Basic
    @Column(name = "DVAPIDEM")
    private Long idEmpresa;

    /** Usuario que registró la devolución. */
    @Basic
    @Column(name = "DVAPUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha y hora de registro en el sistema. */
    @Basic
    @Column(name = "DVAPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que anuló la devolución. */
    @Basic
    @Column(name = "DVAPUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha y hora de la anulación. */
    @Basic
    @Column(name = "DVAPFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación. */
    @Basic
    @Column(name = "DVAPMTAN", length = 500)
    private String motivoAnulacion;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }

    public Filial getFilial() { return filial; }
    public void setFilial(Filial filial) { this.filial = filial; }

    public CuentaBancariaParticipe getCuentaParticipe() { return cuentaParticipe; }
    public void setCuentaParticipe(CuentaBancariaParticipe cuentaParticipe) {
        this.cuentaParticipe = cuentaParticipe;
    }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Long getIdPagoProgramado() { return idPagoProgramado; }
    public void setIdPagoProgramado(Long idPagoProgramado) { this.idPagoProgramado = idPagoProgramado; }

    public Long getNumeroAsiento() { return numeroAsiento; }
    public void setNumeroAsiento(Long numeroAsiento) { this.numeroAsiento = numeroAsiento; }

    public Long getNumeroAsientoReclasificacion() { return numeroAsientoReclasificacion; }
    public void setNumeroAsientoReclasificacion(Long numeroAsientoReclasificacion) {
        this.numeroAsientoReclasificacion = numeroAsientoReclasificacion;
    }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getUsuarioAnulacion() { return usuarioAnulacion; }
    public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
}
