package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.DetalleAsiento;

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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.DTCN. Entity DetalleTransito.</p>
 * <p>Detalle de una partida en tránsito declarada en un cierre de conciliación bancaria
 * (TSR.CNCL). Ver docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §5.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DTCN", schema = "TSR")
@SequenceGenerator(name = "SQ_DTCNCDGO", sequenceName = "TSR.SQ_DTCNCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "DetalleTransitoAll", query = "select e from DetalleTransito e"),
    @NamedQuery(name = "DetalleTransitoId", query = "select e from DetalleTransito e where e.codigo = :id")
})
public class DetalleTransito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DTCNCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DTCNCDGO")
    private Long codigo;

    /**
     * Cierre (TSR.CNCL) que DECLARÓ la partida en tránsito.
     */
    @ManyToOne
    @JoinColumn(name = "CNCLCDGO", referencedColumnName = "CNCLCDGO")
    private Conciliacion cierre;

    /**
     * Línea de asiento (CNT.DTAS) sobre la cuenta bancaria, origen real de la partida para
     * tipo 1 y 2 (Depósito en tránsito / Cheque girado no cobrado). Nula para tipo 3 y 4.
     *
     * <p><b>Corrección del 2026-08-27 (§7bis del diseño):</b> el ancla de tipo 1/2 era
     * {@code MVCBCDGO}, dando por sentado que todo detalle de asiento sobre una cuenta
     * bancaria tiene su fila en TSR.MVCB. Medido: solo el 8% la tiene — pagos, cheques y
     * caja chica la crean, un asiento contable directo no. El 92% restante quedaba sin
     * forma de declararse en tránsito y el cierre nunca lograba cerrar. `DetalleAsiento` es
     * lo que siempre existe.</p>
     */
    @ManyToOne
    @JoinColumn(name = "DTCNDTAS", referencedColumnName = "DTASCDGO")
    private DetalleAsiento detalleAsiento;

    /**
     * Movimiento de libros (TSR.MVCB), cuando existe. Información adicional opcional desde
     * el 2026-08-27 (§7bis): ya no es el ancla de la partida ni lo exige ningún CHECK, se
     * llena si {@code movimientoDeLaCuenta} encuentra uno para el mismo asiento y cuenta.
     */
    @ManyToOne
    @JoinColumn(name = "MVCBCDGO", referencedColumnName = "MVCBCDGO")
    private MovimientoBanco movimientoBanco;

    /**
     * Línea del extracto bancario (TSR.DEXB) no registrada en libros. Solo para tipo 3 y 4
     * (NC/ND del banco no registradas). Nula para tipo 1 y 2.
     */
    @ManyToOne
    @JoinColumn(name = "DTCNIDEX", referencedColumnName = "DEXBCDGO")
    private DetalleExtractoBancario detalleExtracto;

    /**
     * Tipo de partida en tránsito - ver {@link com.saa.rubros.TipoPartidaTransito}.
     * 1 Depósito en tránsito, 2 Cheque girado no cobrado, 3 NC del banco no registrada,
     * 4 ND del banco no registrada.
     */
    @Basic
    @Column(name = "DTCNTPOO")
    private Long tipo;

    /**
     * Valor de la partida, siempre positivo; el tipo indica si suma o resta en la
     * ecuación de cierre.
     */
    @Basic
    @Column(name = "DTCNVLOR")
    private Double valor;

    /**
     * Estado de la partida - ver {@link com.saa.rubros.EstadoPartidaTransito}.
     * 1 Pendiente, 2 Saldada.
     */
    @Basic
    @Column(name = "DTCNESTD")
    private Long estado;

    /**
     * Cierre (TSR.CNCL) en el que la partida quedó SALDADA (se conciliadó de verdad, vía el
     * N:M existente). Nulo mientras siga Pendiente.
     */
    @ManyToOne
    @JoinColumn(name = "DTCNCNSL", referencedColumnName = "CNCLCDGO")
    private Conciliacion cierreSaldo;

    /**
     * Motivo por el que la partida quedó en tránsito.
     */
    @Basic
    @Column(name = "DTCNOBSR", length = 1000)
    private String observacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DTCNFCRG")
    private LocalDateTime fechaRegistro;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Conciliacion getCierre() { return cierre; }
    public void setCierre(Conciliacion cierre) { this.cierre = cierre; }

    public DetalleAsiento getDetalleAsiento() { return detalleAsiento; }
    public void setDetalleAsiento(DetalleAsiento detalleAsiento) { this.detalleAsiento = detalleAsiento; }

    public MovimientoBanco getMovimientoBanco() { return movimientoBanco; }
    public void setMovimientoBanco(MovimientoBanco movimientoBanco) { this.movimientoBanco = movimientoBanco; }

    public DetalleExtractoBancario getDetalleExtracto() { return detalleExtracto; }
    public void setDetalleExtracto(DetalleExtractoBancario detalleExtracto) { this.detalleExtracto = detalleExtracto; }

    public Long getTipo() { return tipo; }
    public void setTipo(Long tipo) { this.tipo = tipo; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Conciliacion getCierreSaldo() { return cierreSaldo; }
    public void setCierreSaldo(Conciliacion cierreSaldo) { this.cierreSaldo = cierreSaldo; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
