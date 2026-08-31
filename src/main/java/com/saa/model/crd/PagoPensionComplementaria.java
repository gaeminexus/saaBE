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
 * Representa la tabla PGPC (PagoPensionComplementaria) — PROPUESTA, DDL sin escribir todavía
 * (2026-08-31, ítem 4 de jubilados: "escribí el proceso completo contra ese modelo, no esperes
 * el DDL" — el árbitro reservó el código de 4 letras en REGISTRO-RESERVAS-EQUIPOS.md y escribe
 * el script).
 *
 * Documento de origen del pago MENSUAL de una pensión complementaria a un jubilado
 * (`EstadoParticipeEntidad.JUBILADO_COMPLEMENTARIO`). Al registrarse genera la fila NEGATIVA de
 * CRD.APRT (tipo 23, pensión complementaria — su saldo baja por el traslado que hizo
 * {@code AporteService#procesarJubilacion}) y dispara una orden de pago en CXP, mismo circuito
 * que {@link DevolucionAporte}.
 *
 * <b>{@code UNIQUE (ENTDCDGO, PGPCANNO, PGPCMESS)}</b> — idempotencia POR DISEÑO: el proceso de
 * generación mensual puede correr más de una vez sobre el mismo mes sin duplicar el pago; la
 * base lo impide, no un chequeo de Java que una rama nueva pueda saltarse.
 *
 * Ciclo de vida (PGPCESTD), ver {@link com.saa.rubros.EstadoPagoPensionComplementaria} — mismo
 * que {@link DevolucionAporte}, mismo motivo: dinero saliendo a un tercero vía CXP.
 *
 * <b>{@code idPagoProgramado}, {@code idAporte} y {@code numeroAsiento} son números sin FK, a
 * propósito</b> — mismo criterio que {@link DevolucionAporte}: el sistema se comercializa
 * después sin CRD, y la consistencia la garantiza el reconciliador, no la base.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PGPC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PagoPensionComplementariaAll", query = "select e from PagoPensionComplementaria e"),
    @NamedQuery(name = "PagoPensionComplementariaId",  query = "select e from PagoPensionComplementaria e where e.codigo = :id")
})
public class PagoPensionComplementaria implements Serializable {

    /** Código del pago. */
    @Id
    @Basic
    @Column(name = "PGPCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Jubilado al que se paga. */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** FK - Filial del jubilado al momento del pago. */
    @ManyToOne
    @JoinColumn(name = "FLLLCDGO", referencedColumnName = "FLLLCDGO")
    private Filial filial;

    /** Año del período pagado. Junto con {@code mes} forma la clave de idempotencia. */
    @Basic
    @Column(name = "PGPCANNO")
    private Long anio;

    /** Mes del período pagado (1-12). */
    @Basic
    @Column(name = "PGPCMESS")
    private Long mes;

    /** Componente de pensión del valor mensual (§3.1 del levantamiento: ej. 280 de 300). */
    @Basic
    @Column(name = "PGPCVLPN")
    private Double valorPension;

    /** Componente de seguro de salud del valor mensual (§3.1: ej. 20 de 300). */
    @Basic
    @Column(name = "PGPCVLSG")
    private Double valorSeguro;

    /** Valor total pagado = valorPension + valorSeguro. */
    @Basic
    @Column(name = "PGPCVLRR")
    private Double valor;

    /** Fecha de negocio del pago. */
    @Basic
    @Column(name = "PGPCFCHA")
    private LocalDate fecha;

    /**
     * Estado.
     * 1=Registrado, 2=En pago, 3=Pagado, 4=Rechazado, 5=Anulado
     */
    @Basic
    @Column(name = "PGPCESTD")
    private Long estado;

    /**
     * Orden de pago generada en CXP (PGS.PGTR.PGTRCDGO).
     * <b>Sin FK a propósito</b>: CRD no ata el esquema PGS.
     */
    @Basic
    @Column(name = "PGPCIDPG")
    private Long idPagoProgramado;

    /**
     * Movimiento NEGATIVO generado en CRD.APRT (tipo 23, pensión complementaria) al registrar
     * este pago. Sin FK a propósito, mismo criterio que el resto de la clase.
     */
    @Basic
    @Column(name = "PGPCIDAP")
    private Long idAporte;

    /**
     * Código del asiento contable del pago, cuando exista (ítem 5 de jubilados, pendiente de
     * confirmar la plantilla alterno 29). {@code null} hasta entonces — ausencia esperada, no
     * un dato faltante.
     */
    @Basic
    @Column(name = "PGPCNMAS")
    private Long numeroAsiento;

    /** Usuario que generó el pago (el proceso mensual, no necesariamente una persona). */
    @Basic
    @Column(name = "PGPCUSRG", length = 50)
    private String usuarioRegistro;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "PGPCFCRG")
    private LocalDateTime fechaRegistro;

    /** Fecha en que el banco confirmó el pago. */
    @Basic
    @Column(name = "PGPCFCPG")
    private LocalDate fechaPago;

    /** Usuario que anuló, si corresponde. */
    @Basic
    @Column(name = "PGPCUSAN", length = 50)
    private String usuarioAnulacion;

    /** Fecha de anulación, si corresponde. */
    @Basic
    @Column(name = "PGPCFCAN")
    private LocalDateTime fechaAnulacion;

    /** Motivo de la anulación, si corresponde. */
    @Basic
    @Column(name = "PGPCMTAN", length = 500)
    private String motivoAnulacion;

    public PagoPensionComplementaria() {
    }

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

    public Filial getFilial() {
        return filial;
    }

    public void setFilial(Filial filial) {
        this.filial = filial;
    }

    public Long getAnio() {
        return anio;
    }

    public void setAnio(Long anio) {
        this.anio = anio;
    }

    public Long getMes() {
        return mes;
    }

    public void setMes(Long mes) {
        this.mes = mes;
    }

    public Double getValorPension() {
        return valorPension;
    }

    public void setValorPension(Double valorPension) {
        this.valorPension = valorPension;
    }

    public Double getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(Double valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public Long getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public void setIdPagoProgramado(Long idPagoProgramado) {
        this.idPagoProgramado = idPagoProgramado;
    }

    public Long getIdAporte() {
        return idAporte;
    }

    public void setIdAporte(Long idAporte) {
        this.idAporte = idAporte;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
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

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getUsuarioAnulacion() {
        return usuarioAnulacion;
    }

    public void setUsuarioAnulacion(String usuarioAnulacion) {
        this.usuarioAnulacion = usuarioAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }
}
