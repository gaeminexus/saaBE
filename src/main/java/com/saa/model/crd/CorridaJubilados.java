package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

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
 * Representa la tabla CRJB (CorridaJubilados) — cabecera de seguimiento de los DOS procesos
 * mensuales de jubilados (seguro médico al inicio de mes, pensiones al final), una fila por
 * (empresa, año, mes). Ver docs/logica-negocio/crd/API-DOS-PROCESOS-MENSUALES-JUBILADOS.md.
 *
 * DDL en {@code docs/logica-negocio/crd/sql/212}, sin correr todavía.
 *
 * <b>Índice único {@code (PJRQCDGO, CRJBANNO, CRJBMESS)}</b> — idempotencia POR DISEÑO: un
 * período, una corrida. Es lo que hace que {@code CobroCreditoServiceImpl#anularCobro} (mismo
 * criterio en todo el proyecto) no tenga que confiar en un chequeo de Java que una rama nueva
 * pueda saltarse.
 *
 * <b>Dos procesos, dos huellas independientes.</b> {@code CRJBESSG}/{@code CRJBESPN} son
 * banderas 0/1 separadas — un período puede tener el seguro generado y las pensiones
 * pendientes (el caso normal, entre el 1 y el fin de mes), nunca al revés (D2: pensiones exige
 * el seguro generado primero).
 *
 * <b>Backfill de agosto 2026 y anteriores</b> ({@code crd/sql/213}): siembra cabeceras con los
 * dos estados en 1 para los períodos que ya se procesaron con el esquema viejo (un solo
 * proceso). Esas filas son identificables porque {@code CRJBIDSG} y {@code CRJBVLCR} quedan
 * {@code NULL} pese a tener {@code CRJBESSG = 1} — el backfill no puede reconstruirlos desde
 * {@code CRD.PGPC} (ver §8 del contrato).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CRJB", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "CorridaJubiladosAll", query = "select e from CorridaJubilados e"),
    @NamedQuery(name = "CorridaJubiladosId",  query = "select e from CorridaJubilados e where e.codigo = :id")
})
public class CorridaJubilados implements Serializable {

    /** Código de la corrida. PK autoincremental (IDENTITY, no secuencia — como CRD.APRT/PGAP). */
    @Id
    @Basic
    @Column(name = "CRJBCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Empresa (nodo SCP.PJRQ de nivel empresa). La misma que usan ASNT y PLNN. */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /** Año del período. */
    @Basic
    @Column(name = "CRJBANNO")
    private Long anio;

    /** Mes del período (1-12). */
    @Basic
    @Column(name = "CRJBMESS")
    private Long mes;

    /** Estado del proceso de SEGURO: 0 pendiente, 1 generado. */
    @Basic
    @Column(name = "CRJBESSG")
    private Long estadoSeguro;

    /** Cuándo se generó el seguro. */
    @Basic
    @Column(name = "CRJBFCSG")
    private LocalDateTime fechaSeguro;

    /** Quién generó el seguro. */
    @Basic
    @Column(name = "CRJBUSSG", length = 50)
    private String usuarioSeguro;

    /** Total pagado al proveedor del seguro médico en este período. */
    @Basic
    @Column(name = "CRJBVLSG")
    private Double totalSeguro;

    /**
     * Id de la orden de pago (PGS.PGTR) al proveedor del seguro. Sin FK a propósito, mismo
     * criterio que {@link PagoPensionComplementaria#idPagoProgramado}. {@code NULL} en las
     * filas sembradas por el backfill de agosto 2026 (no se puede reconstruir desde PGPC).
     */
    @Basic
    @Column(name = "CRJBIDSG")
    private Long idOrdenPagoSeguro;

    /** Cuántos jubilados entraron en el proceso de seguro de este período. */
    @Basic
    @Column(name = "CRJBCTSG")
    private Long cantidadJubiladosSeguro;

    /** Estado del proceso de PENSIONES: 0 pendiente, 1 generado. */
    @Basic
    @Column(name = "CRJBESPN")
    private Long estadoPensiones;

    /** Cuándo se generaron las pensiones. */
    @Basic
    @Column(name = "CRJBFCPN")
    private LocalDateTime fechaPensiones;

    /** Quién generó las pensiones. */
    @Basic
    @Column(name = "CRJBUSPN", length = 50)
    private String usuarioPensiones;

    /** Total de las órdenes de pensión individuales generadas en este período. */
    @Basic
    @Column(name = "CRJBVLPN")
    private Double totalPensiones;

    /**
     * Total cruzado a préstamos en este período. {@code NULL} en las filas sembradas por el
     * backfill (no se puede derivar de PGPC sin recorrer los pagos — no vale la pena para un
     * histórico).
     */
    @Basic
    @Column(name = "CRJBVLCR")
    private Double totalCruzadoPrestamos;

    /** Cuántos jubilados entraron en el proceso de pensiones de este período. */
    @Basic
    @Column(name = "CRJBCTPN")
    private Long cantidadJubiladosPensiones;

    public CorridaJubilados() {
    }

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

    public Long getEstadoSeguro() {
        return estadoSeguro;
    }

    public void setEstadoSeguro(Long estadoSeguro) {
        this.estadoSeguro = estadoSeguro;
    }

    public LocalDateTime getFechaSeguro() {
        return fechaSeguro;
    }

    public void setFechaSeguro(LocalDateTime fechaSeguro) {
        this.fechaSeguro = fechaSeguro;
    }

    public String getUsuarioSeguro() {
        return usuarioSeguro;
    }

    public void setUsuarioSeguro(String usuarioSeguro) {
        this.usuarioSeguro = usuarioSeguro;
    }

    public Double getTotalSeguro() {
        return totalSeguro;
    }

    public void setTotalSeguro(Double totalSeguro) {
        this.totalSeguro = totalSeguro;
    }

    public Long getIdOrdenPagoSeguro() {
        return idOrdenPagoSeguro;
    }

    public void setIdOrdenPagoSeguro(Long idOrdenPagoSeguro) {
        this.idOrdenPagoSeguro = idOrdenPagoSeguro;
    }

    public Long getCantidadJubiladosSeguro() {
        return cantidadJubiladosSeguro;
    }

    public void setCantidadJubiladosSeguro(Long cantidadJubiladosSeguro) {
        this.cantidadJubiladosSeguro = cantidadJubiladosSeguro;
    }

    public Long getEstadoPensiones() {
        return estadoPensiones;
    }

    public void setEstadoPensiones(Long estadoPensiones) {
        this.estadoPensiones = estadoPensiones;
    }

    public LocalDateTime getFechaPensiones() {
        return fechaPensiones;
    }

    public void setFechaPensiones(LocalDateTime fechaPensiones) {
        this.fechaPensiones = fechaPensiones;
    }

    public String getUsuarioPensiones() {
        return usuarioPensiones;
    }

    public void setUsuarioPensiones(String usuarioPensiones) {
        this.usuarioPensiones = usuarioPensiones;
    }

    public Double getTotalPensiones() {
        return totalPensiones;
    }

    public void setTotalPensiones(Double totalPensiones) {
        this.totalPensiones = totalPensiones;
    }

    public Double getTotalCruzadoPrestamos() {
        return totalCruzadoPrestamos;
    }

    public void setTotalCruzadoPrestamos(Double totalCruzadoPrestamos) {
        this.totalCruzadoPrestamos = totalCruzadoPrestamos;
    }

    public Long getCantidadJubiladosPensiones() {
        return cantidadJubiladosPensiones;
    }

    public void setCantidadJubiladosPensiones(Long cantidadJubiladosPensiones) {
        this.cantidadJubiladosPensiones = cantidadJubiladosPensiones;
    }
}
