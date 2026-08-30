package com.saa.model.rhh;

import java.io.Serializable;
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
 * Detalle de qué año de {@link SaldoVacaciones} consumió cada aprobación de vacaciones.
 * Tabla RHH.DVAC. Ver docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md y
 * docs/logica-negocio/rhh/sql/03-detalle-consumo-vacaciones.sql.
 *
 * <p>Una solicitud que consume de varios años produce una fila por año. Permite que
 * {@code anularAprobacion} devuelva los días exactamente a los años de los que salieron,
 * sin depender del orden en que se consumió después — mismo patrón de trazabilidad que
 * {@code RhhOrigenRenglon} + {@code idReferencia}.</p>
 *
 * <p><b>No hay backfill de las solicitudes aprobadas antes de esta tabla</b> (2026-08-27):
 * esas siguen revirtiéndose con {@code AcreditacionVacacionesService.revertirConsumo}, que
 * es una heurística sobre el estado actual del saldo, no un registro exacto. No hay de
 * dónde reconstruir el histórico sin inventarlo.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DVAC", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleConsumoVacacionesAll", query = "select e from DetalleConsumoVacaciones e"),
    @NamedQuery(name = "DetalleConsumoVacacionesId", query = "select e from DetalleConsumoVacaciones e where e.codigo = :id")
})
public class DetalleConsumoVacaciones implements Serializable {

    /** Código único. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DVACCDGO")
    private Long codigo;

    /** Solicitud de vacaciones que consumió este año (RHH.SLCT). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "SLCTCDGO", nullable = false)
    private SolicitudVacaciones solicitud;

    /** Saldo anual del que se tomaron los días (RHH.SLDV). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "SLDVCDGO", nullable = false)
    private SaldoVacaciones saldo;

    /** Días tomados de ese año por esta solicitud. */
    @Basic
    @Column(name = "DVACDIAS", nullable = false)
    private Double dias;

    /** 1 vigente, 0 revertido al anular la aprobación. */
    @Basic
    @Column(name = "DVACESTD", nullable = false)
    private Long estado;

    /** Fecha de registro. */
    @Basic
    @Column(name = "DVACFCHR")
    private LocalDateTime fechaRegistro;

    /** Usuario que aprobó. */
    @Basic
    @Column(name = "DVACUSRR", length = 60)
    private String usuarioRegistro;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public SolicitudVacaciones getSolicitud() { return solicitud; }
    public void setSolicitud(SolicitudVacaciones solicitud) { this.solicitud = solicitud; }

    public SaldoVacaciones getSaldo() { return saldo; }
    public void setSaldo(SaldoVacaciones saldo) { this.saldo = saldo; }

    public Double getDias() { return dias; }
    public void setDias(Double dias) { this.dias = dias; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
