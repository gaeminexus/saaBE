package com.saa.model.cxc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;
import com.saa.model.tsr.Titular;

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
 * Entity AnticipoCliente.
 * Registra los anticipos recibidos de clientes.
 * Tabla: CBR.ANTC
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ANTC", schema = "CBR")
@SequenceGenerator(name = "SQ_ANTCCDGO", sequenceName = "CBR.SQ_ANTCCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "AnticipoClienteAll", query = "select e from AnticipoCliente e"),
    @NamedQuery(name = "AnticipoClienteId",  query = "select e from AnticipoCliente e where e.id = :id")
})
public class AnticipoCliente implements Serializable {

    /**
     * Identificador único del anticipo.
     */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ANTCCDGO")
    private Long id;

    /**
     * Cliente que entrega el anticipo. FK a TSR.TTLR.
     */
    @ManyToOne
    @JoinColumn(name = "TITULAR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /**
     * Fecha del documento de anticipo.
     */
    @Basic
    @Column(name = "FECHAANTICIPO")
    private LocalDate fechaAnticipo;

    /**
     * Fecha en que se recibió físicamente el anticipo.
     */
    @Basic
    @Column(name = "FECHARECEPCION")
    private LocalDate fechaRecepcion;

    /**
     * Usuario que registra el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Fecha y hora en que se registró en el sistema.
     */
    @Basic
    @Column(name = "FECHAREGISTRO")
    private LocalDateTime fechaRegistro;

    /**
     * Número de documento de referencia del anticipo.
     */
    @Basic
    @Column(name = "NUMERODOC", length = 100)
    private String numeroDoc;

    /**
     * Valor monetario del anticipo.
     */
    @Basic
    @Column(name = "VALOR")
    private Double valor;

    /**
     * Asiento contable generado. FK a CNT.ASNT.
     */
    @ManyToOne
    @JoinColumn(name = "ASIENTO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Estado: 1=Activo, 2=Anulado.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /**
     * Empresa contable a la que pertenece el anticipo. FK a SCP.PJRQ.
     */
    @ManyToOne
    @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Observaciones adicionales.
     */
    @Basic
    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public LocalDate getFechaAnticipo() { return fechaAnticipo; }
    public void setFechaAnticipo(LocalDate fechaAnticipo) { this.fechaAnticipo = fechaAnticipo; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getNumeroDoc() { return numeroDoc; }
    public void setNumeroDoc(String numeroDoc) { this.numeroDoc = numeroDoc; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public Asiento getAsiento() { return asiento; }
    public void setAsiento(Asiento asiento) { this.asiento = asiento; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
