package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.CuentaBancaria;

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
 * Entity LotePago.
 * Cada lote corresponde a un archivo de transferencias enviado a la entidad
 * financiera. Agrupa los pagos programados que el usuario seleccionó para pagar.
 * Tabla: PGS.LTPG
 *
 * Estados (ESTADO), ver {@link com.saa.rubros.EstadoLotePago}:
 *   1 = Generado (archivo creado y enviado al banco)
 *   2 = Respuesta procesada (ya se cargó el archivo de respuesta del banco)
 *   3 = Anulado
 *
 * El usuario que genera el lote es quien aprueba los pagos: seleccionar un pago
 * para el archivo equivale a aprobarlo.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LTPG", schema = "PGS")
@SequenceGenerator(name = "SQ_LTPGCDGO", sequenceName = "PGS.SQ_LTPGCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "LotePagoAll", query = "select e from LotePago e"),
    @NamedQuery(name = "LotePagoId",  query = "select e from LotePago e where e.id = :id"),
    @NamedQuery(name = "LotePagoByEmpresa",
        query = "select e from LotePago e where e.empresa.codigo = :idEmpresa order by e.id desc")
})
public class LotePago implements Serializable {

    /**
     * Identificador único del lote.
     */
    @Basic
    @Id
    @Column(name = "LTPGCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_LTPGCDGO")
    private Long id;

    /**
     * Empresa a la que pertenece el lote. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "LTPGPJRQ", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Cuenta bancaria propia desde la que se ejecutan las transferencias del
     * lote. FK a TSR.CNBC.
     */
    @ManyToOne
    @JoinColumn(name = "LTPGCNBC", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /**
     * Fecha de generación del archivo.
     */
    @Basic
    @Column(name = "LTPGFGNR")
    private LocalDate fechaGeneracion;

    /**
     * Nombre del archivo generado para el banco.
     */
    @Basic
    @Column(name = "LTPGNMAR", length = 200)
    private String nombreArchivo;

    /**
     * Ruta relativa donde se guardó el archivo.
     */
    @Basic
    @Column(name = "LTPGPATH", length = 500)
    private String path;

    /**
     * Valor total de los pagos incluidos en el lote.
     */
    @Basic
    @Column(name = "LTPGVLTT")
    private Double valorTotal;

    /**
     * Cantidad de pagos incluidos en el lote.
     */
    @Basic
    @Column(name = "LTPGNPAG")
    private Long numeroPagos;

    /**
     * Estado del lote.
     * 1 = Generado, 2 = Respuesta procesada, 3 = Anulado
     */
    @Basic
    @Column(name = "LTPGESTD")
    private Long estado;

    /**
     * Observaciones del lote.
     */
    @Basic
    @Column(name = "LTPGOBSR", length = 2000)
    private String observacion;

    /**
     * Usuario que seleccionó los pagos y generó el lote. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "LTPGUSAR", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "LTPGFCRG")
    private LocalDateTime fechaRegistro;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public CuentaBancaria getCuentaBancaria() { return cuentaBancaria; }
    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) { this.cuentaBancaria = cuentaBancaria; }

    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public void setFechaGeneracion(LocalDate fechaGeneracion) { this.fechaGeneracion = fechaGeneracion; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public Long getNumeroPagos() { return numeroPagos; }
    public void setNumeroPagos(Long numeroPagos) { this.numeroPagos = numeroPagos; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
