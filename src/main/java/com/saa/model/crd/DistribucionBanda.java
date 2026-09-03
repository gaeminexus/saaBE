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
 * Representa CRD.DSBN (DistribucionBanda) — DDL en {@code sql/174_TABLA_DISTRIBUCION_BANDAS.sql}.
 * Fuente de la pantalla de auditoría de bandas (PLAN-AUDITORIA-BANDAS.md).
 *
 * <b>Se escribe al APLICAR el pago, no al contabilizar</b> — la banda es un dato de cartera,
 * no contable: "esta cuota vence en 45 días" es verdad exista o no un asiento detrás. Por eso
 * {@link #idAsiento} es la ÚNICA columna que engancha con contabilidad, y es anulable a
 * propósito: con contabilidad desconectada queda en {@code null} y la pantalla sigue
 * funcionando completa, solo sin esa columna.
 *
 * <b>{@link #etiqueta} se guarda y NO se recalcula</b>: la deriva CRD desde su propia
 * parametrización de bandas al momento de aplicar, y una reconfiguración posterior no debe
 * reescribir la historia de lo ya distribuido.
 *
 * <b>{@link #origen}/{@link #idOrigen}</b>, no "idCarga": la carga Petro es UN origen
 * ({@link com.saa.rubros.DsbnOrigen}), no la única fuente — también clasifican el cobro
 * individual, el abono a capital/precancelación y (a futuro) el pago de pensión.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DSBN", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "DistribucionBandaAll", query = "select e from DistribucionBanda e"),
    @NamedQuery(name = "DistribucionBandaId",  query = "select e from DistribucionBanda e where e.codigo = :id")
})
public class DistribucionBanda implements Serializable {

    @Id
    @Basic
    @Column(name = "DSBNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Hecho que originó esta distribución — {@link com.saa.rubros.DsbnOrigen}. */
    @Basic
    @Column(name = "DSBNORGN", length = 20)
    private String origen;

    /** Código del hecho en su propia tabla (CRAR / CBCR / EVPR / PGPC según {@link #origen}). */
    @Basic
    @Column(name = "DSBNIDOR")
    private Long idOrigen;

    /** Qué se distribuyó — {@link com.saa.rubros.DsbnConcepto}. Agrupador PRIMARIO de la pantalla. */
    @Basic
    @Column(name = "DSBNCNCP", length = 25)
    private String concepto;

    /** Valor distribuido de este concepto. */
    @Basic
    @Column(name = "DSBNVALR")
    private Double valor;

    /** Partícipe. Único FK obligatorio: todo hecho distribuido es de alguien. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Préstamo. Null en filas de {@code APORTE}. */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** Cuota. Null en filas de {@code APORTE}. */
    @ManyToOne
    @JoinColumn(name = "DTPRCDGO", referencedColumnName = "DTPRCDGO")
    private DetallePrestamo detallePrestamo;

    /** Producto del préstamo. Null en filas de {@code APORTE}. */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    /** Código del tipo de préstamo. Sin FK a propósito (mismo criterio que el resto del
     * módulo con catálogos de solo lectura). Null en filas de {@code APORTE}. */
    @Basic
    @Column(name = "TPPRCDGO")
    private Long tipoPrestamo;

    /** Código del tipo de aporte. Sin FK. Null en filas de préstamo. */
    @Basic
    @Column(name = "TPAPCDGO")
    private Long tipoAporte;

    /** Tipo de cartera ({@link com.saa.rubros.TipoCarteraBanda}) — solo aplica a {@code CAPITAL}. */
    @Basic
    @Column(name = "DSBNTPCR")
    private Long tipoCartera;

    /** Días al vencimiento (o de vencido) al momento de aplicar — solo {@code CAPITAL}. */
    @Basic
    @Column(name = "DSBNDIAS")
    private Long dias;

    /** Banda resuelta por {@code ClasificadorBandaService} — solo {@code CAPITAL}. */
    @ManyToOne
    @JoinColumn(name = "BNDPCDGO", referencedColumnName = "BNDPCDGO")
    private BandaProducto banda;

    /** Etiqueta de la banda AL MOMENTO de aplicar. No se recalcula — ver el javadoc de la clase. */
    @Basic
    @Column(name = "DSBNETQT", length = 100)
    private String etiqueta;

    /** Vencimiento de la cuota — solo {@code CAPITAL}. */
    @Basic
    @Column(name = "DSBNFCVN")
    private LocalDate fechaVencimiento;

    /** Fecha en que se aplicó el pago — la que decide la banda. */
    @Basic
    @Column(name = "DSBNFCAP")
    private LocalDate fechaAplicacion;

    /** Único enganche con contabilidad. Sin FK, anulable a propósito — ver el javadoc de la clase. */
    @Basic
    @Column(name = "ASNTCDGO")
    private Long idAsiento;

    @Basic
    @Column(name = "DSBNFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "DSBNUSAR", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "DSBNESTD")
    private Long estado;

    public DistribucionBanda() {
    }

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Long getIdOrigen() {
        return idOrigen;
    }

    public void setIdOrigen(Long idOrigen) {
        this.idOrigen = idOrigen;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
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

    public DetallePrestamo getDetallePrestamo() {
        return detallePrestamo;
    }

    public void setDetallePrestamo(DetallePrestamo detallePrestamo) {
        this.detallePrestamo = detallePrestamo;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Long getTipoPrestamo() {
        return tipoPrestamo;
    }

    public void setTipoPrestamo(Long tipoPrestamo) {
        this.tipoPrestamo = tipoPrestamo;
    }

    public Long getTipoAporte() {
        return tipoAporte;
    }

    public void setTipoAporte(Long tipoAporte) {
        this.tipoAporte = tipoAporte;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public Long getDias() {
        return dias;
    }

    public void setDias(Long dias) {
        this.dias = dias;
    }

    public BandaProducto getBanda() {
        return banda;
    }

    public void setBanda(BandaProducto banda) {
        this.banda = banda;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public void setEtiqueta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public Long getIdAsiento() {
        return idAsiento;
    }

    public void setIdAsiento(Long idAsiento) {
        this.idAsiento = idAsiento;
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
}
