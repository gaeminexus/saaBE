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
 * Representa la tabla APRT (Aporte).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "APRT", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "AporteAll", query = "select e from Aporte e"),
    @NamedQuery(name = "AporteId", query = "select e from Aporte e where e.codigo = :id")
})
public class Aporte implements Serializable {

    /**
     * Código del aporte.
     */
    @Id
    @Basic
    @Column(name = "APRTCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * ID Filial.
     */
    /** FK - Código Tipo Préstamo */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /**
     * ID de la entidad.
     */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /*
     * ID del contrato.
     */
    @ManyToOne
    @JoinColumn(name = "CNTRCDGO", referencedColumnName = "CNTRCDGO")
    private Contrato contrato;
    
    
    /**
     * Código de tipo de aporte.
     */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /**
     * FK - Carga Petro que generó este aporte (CRD.CRAR). NULL si no vino de una carga
     * (ajuste manual, devolución, reverso) o si es anterior al 2026-08-28 — no se hizo
     * backfill de lo histórico. Distinta pregunta de {@link #tipoMovimiento} (APRTTPMV,
     * rubro 235): esta dice DE QUÉ ARCHIVO salió, esa dice la NATURALEZA del movimiento — no
     * mezclar los dos al filtrar. Ver docs/logica-negocio/crd/sql/DDL-TRAZABILIDAD-CARGA-PETRO.sql.
     *
     * <p>⚠️ ES LA COLUMNA GOBERNADA (FK + índice), pero al 2026-08-29 TODAVÍA NO ES LA
     * FUENTE que leen los servicios: {@link #idAsoprep} (APRTIDAS) es la trazabilidad de
     * carga preexistente y sigue siendo la que se consulta en vivo. Esta columna se llena
     * en cargas nuevas desde ahora, en paralelo, para poder migrar el LECTOR más adelante
     * sin tener que reprocesar nada — ver el javadoc de {@link #idAsoprep} para el detalle
     * de la transición y qué la habilita.</p>
     */
    @ManyToOne
    @JoinColumn(name = "CRARCDGO", referencedColumnName = "CRARCDGO")
    private CargaArchivo cargaArchivo;

    /**
     * FK - Devolución de aportes que generó esta fila NEGATIVA (CRD.DVAP). NULL en cualquier
     * fila que no sea de una devolución (tipoMovimiento distinto de DEVOLUCION). Se setea en
     * TODAS las filas que crea {@code DevolucionAporteServiceImpl.crearFilaNegativaDevolucion}
     * — cuando el reparto por período de devengo genera varias filas para el mismo (devolución,
     * tipo), CADA UNA lleva esta FK, a diferencia de {@code CRD.DDVA.DDVAAPRT} que solo
     * referencia la primera. Ver DevolucionAporteService#obtenerIdDevolucionPorAporte.
     */
    @ManyToOne
    @JoinColumn(name = "APRTIDDV", referencedColumnName = "DVAPCDGO")
    private DevolucionAporte devolucion;

    /**
     * Fecha de transacción.
     */
    @Basic
    @Column(name = "APRTFCTR")
    private LocalDateTime fechaTransaccion;

    /**
     * Glosa.
     */
    @Basic
    @Column(name = "APRTGLSA", length = 2000)
    private String glosa;

    /**
     * Valor.
     */
    @Basic
    @Column(name = "APRTVLRR")
    private Double valor;

    /**
     * Valor pagado.
     */
    @Basic
    @Column(name = "APRTVLPG")
    private Double valorPagado;

    /**
     * Saldo.
     */
    @Basic
    @Column(name = "APRTSLDO")
    private Double saldo;

    /**
     * Código de la {@code CargaArchivo} (CRD.CRAR) que generó este aporte. Verificado
     * 2026-08-29: es la trazabilidad de carga que YA EXISTÍA antes de {@link #cargaArchivo}/
     * {@code CRARCDGO}, y hoy SIGUE SIENDO LA FUENTE VIVA — la usan
     * {@code AporteDaoServiceImpl.selectByEntidadTipoYCarga} (busca por
     * {@code idAsoprep = :idAsoprep}) y {@code selectAporteAdelantado}
     * ({@code idAsoprep <> :idAsoprep}), y se pone explícitamente en {@code NULL} para
     * aportes que no vienen de una carga ({@code AporteServiceImpl}, {@code DevolucionAporteServiceImpl},
     * {@code ProcesoPagoPrestamoServiceImpl}) — misma semántica que {@link #cargaArchivo}.
     * <b>NO CONFUNDIR con {@code Prestamo.idAsoprep}</b> (PRSTIDAS): mismo nombre, tabla y
     * significado totalmente distintos — ese es el número de operación del préstamo en
     * ASOPREP (G46-G49, CCPM), sin relación con este campo.
     *
     * @see #cargaArchivo
     */
    @Basic
    @Column(name = "APRTIDAS")
    private Long idAsoprep;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "APRTFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario de registro.
     */
    @Basic
    @Column(name = "APRTUSRG", length = 50)
    private String usuarioRegistro;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "APRTIDST")
    private Long estado;

    /**
     * Mes al que pertenece el aporte (siempre el primer día del mes).
     *
     * NO es la fecha contable: esa sigue siendo {@link #fechaTransaccion} (APRTFCTR), la
     * fecha de CAJA, que no cambia de significado con el devengo y es la que sigue leyendo
     * contabilidad. {@code NULL} = no aplica (movimiento que no es aporte mensual) o dato
     * anterior a la Fase 2 del plan de devengo de aportes (2026-08-27): las consultas de
     * cartera deben leer siempre {@code NVL(APRTPRDV, TRUNC(APRTFCTR,'MM'))}, nunca la
     * columna sola.
     */
    @Basic
    @Column(name = "APRTPRDV")
    private LocalDate periodoDevengo;

    /**
     * Naturaleza del movimiento. Rubro 235 ({@code CRD_TIPO_MOVIMIENTO_APORTE}) — ver
     * {@link com.saa.rubros.CrdTipoMovimientoAporte}.
     */
    @Basic
    @Column(name = "APRTTPMV")
    private Long tipoMovimiento;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public Entidad getEntidad() {
        return entidad;
    }

    public void setEntidad(Entidad entidad) {
        this.entidad = entidad;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public TipoAporte getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(TipoAporte tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public LocalDateTime getFechaTransaccion() {
        return fechaTransaccion;
    }

    public void setFechaTransaccion(LocalDateTime fechaTransaccion) {
        this.fechaTransaccion = fechaTransaccion;
    }

    public String getGlosa() {
        return glosa;
    }

    public void setGlosa(String glosa) {
        this.glosa = glosa;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getValorPagado() {
        return valorPagado;
    }

    public void setValorPagado(Double valorPagado) {
        this.valorPagado = valorPagado;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Long getIdAsoprep() {
        return idAsoprep;
    }

    public void setIdAsoprep(Long idAsoprep) {
        this.idAsoprep = idAsoprep;
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

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public LocalDate getPeriodoDevengo() {
        return periodoDevengo;
    }

    public void setPeriodoDevengo(LocalDate periodoDevengo) {
        this.periodoDevengo = periodoDevengo;
    }

    public Long getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(Long tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public CargaArchivo getCargaArchivo() {
        return cargaArchivo;
    }

    public void setCargaArchivo(CargaArchivo cargaArchivo) {
        this.cargaArchivo = cargaArchivo;
    }

    public DevolucionAporte getDevolucion() {
        return devolucion;
    }

    public void setDevolucion(DevolucionAporte devolucion) {
        this.devolucion = devolucion;
    }
}

