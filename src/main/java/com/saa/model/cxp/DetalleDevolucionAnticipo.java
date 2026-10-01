package com.saa.model.cxp;

import java.io.Serializable;

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
 * Entity DetalleDevolucionAnticipo.
 * Cuánto de un {@link AnticipoProveedor} cubre una {@link DevolucionAnticipoProveedor}. La suma
 * de {@code valor} de las filas de una devolución es su {@code DevolucionAnticipoProveedor.valor}
 * (DDPRVLOR / DVPRVLOR). Tabla: PGS.DDPR (docs/logica-negocio/cxp/sql/e2-78).
 *
 * Un mismo anticipo no se repite dentro de una devolución (UK_DDPR_DVPR_ANTP).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DDPR", schema = "PGS")
@NamedQueries({
    @NamedQuery(name = "DetalleDevolucionAnticipoAll", query = "select e from DetalleDevolucionAnticipo e"),
    @NamedQuery(name = "DetalleDevolucionAnticipoId",  query = "select e from DetalleDevolucionAnticipo e where e.codigo = :id")
})
public class DetalleDevolucionAnticipo implements Serializable {

    @Id
    @Column(name = "DDPRCDGO")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Devolución (cabecera) a la que pertenece esta línea. FK a PGS.DVPR. */
    @ManyToOne
    @JoinColumn(name = "DVPRCDGO", referencedColumnName = "DVPRCDGO")
    private DevolucionAnticipoProveedor devolucion;

    /** Anticipo que se devuelve, total o parcialmente. FK a PGS.ANTP. */
    @ManyToOne
    @JoinColumn(name = "ANTPCDGO", referencedColumnName = "ANTPCDGO")
    private AnticipoProveedor anticipo;

    /** Monto de este anticipo que cubre la devolución. */
    @Basic
    @Column(name = "DDPRVLOR")
    private Double valor;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public DevolucionAnticipoProveedor getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionAnticipoProveedor devolucion) { this.devolucion = devolucion; }

    public AnticipoProveedor getAnticipo() { return anticipo; }
    public void setAnticipo(AnticipoProveedor anticipo) { this.anticipo = anticipo; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
}
