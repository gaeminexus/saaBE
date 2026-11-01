package com.saa.model.crd;

import java.io.Serializable;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.DSSB (DestinoSobrante): el destino que el operador elige para el SOBRANTE de un
 * partícipe en una carga Petro/ARCH — una fila activa por (carga, partícipe). B8, DISENO-DESTINO-SOBRANTE-DSSB.md;
 * DDL en {@code crd/sql/357_DDL_DSSB_DESTINO_SOBRANTE_Y_LINEA_61.sql}.
 *
 * <p>{@code idPagoProgramado} (DSSBPGTR) e {@code idAsiento} (DSSBASNT) van SIN FK a propósito: son de otros
 * esquemas (PGS, CNT), mismo patrón que {@code Prestamo.idPagoProgramado}.</p>
 *
 * @see com.saa.rubros.DestinoSobrante
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DSSB", schema = "CRD")
@SequenceGenerator(name = "SQ_DSSBCDGO", sequenceName = "CRD.SQ_DSSBCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DestinoSobranteAll", query = "select e from DestinoSobrante e"),
    @NamedQuery(name = "DestinoSobranteId", query = "select e from DestinoSobrante e where e.codigo = :id")
})
public class DestinoSobrante implements Serializable, EntidadAuditableFechaHora {

    /** PK. */
    @Id
    @Basic
    @Column(name = "DSSBCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DSSBCDGO")
    private Long codigo;

    /** FK - Carga Petro/ARCH. */
    @ManyToOne
    @JoinColumn(name = "CRARCDGO", referencedColumnName = "CRARCDGO")
    private CargaArchivo cargaArchivo;

    /** FK - Partícipe (entidad). */
    @ManyToOne
    @JoinColumn(name = "ENTDCDGO", referencedColumnName = "ENTDCDGO")
    private Entidad entidad;

    /** Destino: 1 siguiente cuota, 2 aporte, 3 devolución ({@link com.saa.rubros.DestinoSobrante}). */
    @Basic
    @Column(name = "DSSBTPDS")
    private Long tipoDestino;

    /** FK - Préstamo elegido (solo destino 1; nulo = la cuota pendiente más antigua). */
    @ManyToOne
    @JoinColumn(name = "PRSTCDGO", referencedColumnName = "PRSTCDGO")
    private Prestamo prestamo;

    /** FK - Tipo de aporte (solo destino 2: 9 jubilación, 11 cesantía). */
    @ManyToOne
    @JoinColumn(name = "TPAPCDGO", referencedColumnName = "TPAPCDGO")
    private TipoAporte tipoAporte;

    /** Lo aplicado o devuelto cuyo dinero entró por una fila de APORTES (puente aux1 3); lo escribe el proceso. */
    @Basic
    @Column(name = "DSSBVLAH")
    private Double valorAplicadoAportes;

    /** Lo aplicado o devuelto cuyo dinero entró por una fila de PRÉSTAMOS (puente aux1 4); lo escribe el proceso. */
    @Basic
    @Column(name = "DSSBVLPR")
    private Double valorAplicadoPrestamos;

    /** Pago programado de Tesorería (PGS.PGTR.PGTRCDGO), sin FK; solo destino 3. */
    @Basic
    @Column(name = "DSSBPGTR")
    private Long idPagoProgramado;

    /** Asiento que llevó el valor a socios por pagar (CNT.ASNT.ASNTCDGO), sin FK; solo destino 3. */
    @Basic
    @Column(name = "DSSBASNT")
    private Long idAsiento;

    /** 1 activo, 0 anulado. */
    @Basic
    @Column(name = "DSSBESTD")
    private Long estado;

    @Basic
    @Column(name = "DSSBOBSR", length = 1000)
    private String observacion;

    @Basic
    @Column(name = "DSSBUSRG", length = 50)
    private String usuarioRegistro;

    @Basic
    @Column(name = "DSSBFCRG")
    private LocalDateTime fechaRegistro;

    @Basic
    @Column(name = "DSSBUSMD", length = 50)
    private String usuarioModificacion;

    @Basic
    @Column(name = "DSSBFCMD")
    private LocalDateTime fechaModificacion;

    public DestinoSobrante() {
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public CargaArchivo getCargaArchivo() { return cargaArchivo; }
    public void setCargaArchivo(CargaArchivo cargaArchivo) { this.cargaArchivo = cargaArchivo; }
    public Entidad getEntidad() { return entidad; }
    public void setEntidad(Entidad entidad) { this.entidad = entidad; }
    public Long getTipoDestino() { return tipoDestino; }
    public void setTipoDestino(Long tipoDestino) { this.tipoDestino = tipoDestino; }
    public Prestamo getPrestamo() { return prestamo; }
    public void setPrestamo(Prestamo prestamo) { this.prestamo = prestamo; }
    public TipoAporte getTipoAporte() { return tipoAporte; }
    public void setTipoAporte(TipoAporte tipoAporte) { this.tipoAporte = tipoAporte; }
    public Double getValorAplicadoAportes() { return valorAplicadoAportes; }
    public void setValorAplicadoAportes(Double valorAplicadoAportes) { this.valorAplicadoAportes = valorAplicadoAportes; }
    public Double getValorAplicadoPrestamos() { return valorAplicadoPrestamos; }
    public void setValorAplicadoPrestamos(Double valorAplicadoPrestamos) { this.valorAplicadoPrestamos = valorAplicadoPrestamos; }
    public Long getIdPagoProgramado() { return idPagoProgramado; }
    public void setIdPagoProgramado(Long idPagoProgramado) { this.idPagoProgramado = idPagoProgramado; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    @Override
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    @Override
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    @Override
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public String getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
