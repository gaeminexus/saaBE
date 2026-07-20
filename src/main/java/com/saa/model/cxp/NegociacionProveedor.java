package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
 * Entity NegociacionProveedor.
 * Almacena las negociaciones realizadas con proveedores en el módulo CXP.
 * Puede estar respaldada por un contrato y permite llevar seguimiento completo
 * de pagos, anticipos y adendums.
 * Tabla: PGS.NGCP
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "NGCP", schema = "PGS")
@SequenceGenerator(name = "SQ_NGCPCDGO", sequenceName = "PGS.SQ_NGCPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "NegociacionProveedorAll", query = "select e from NegociacionProveedor e"),
    @NamedQuery(name = "NegociacionProveedorId", query = "select e from NegociacionProveedor e where e.id = :id")
})
public class NegociacionProveedor implements Serializable {

    /** Id de tabla. */
    @Basic
    @Id
    @Column(name = "ID", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_NGCPCDGO")
    private Long id;

    /** Empresa a la que pertenece la negociacion. */
    @ManyToOne
    @JoinColumn(name = "EMPRESA", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Titular proveedor con el que se realiza la negociacion.
     * Debe tener el rol de proveedor asignado (tipoProveedor = 1).
     */
    @ManyToOne
    @JoinColumn(name = "TITULAR", referencedColumnName = "TTLRCDGO")
    private Titular titular;

    /** Fecha en la que se realizó la negociacion con el proveedor. */
    @Basic
    @Column(name = "FECHANEGOCIACION")
    private LocalDate fechaNegociacion;

    /** Fecha de inicio de la vigencia de la negociacion. */
    @Basic
    @Column(name = "FECHAINICIO")
    private LocalDate fechaInicio;

    /** Fecha de fin estimada de la negociacion. */
    @Basic
    @Column(name = "FECHAFIN")
    private LocalDate fechaFin;

    /** Numero de referencia del contrato asociado (opcional). */
    @Basic
    @Column(name = "NUMCONTRATO", length = 200)
    private String numContrato;

    /** Descripcion general de la negociacion o del negocio acordado. */
    @Basic
    @Column(name = "DESCRIPCION", length = 2000)
    private String descripcion;

    /** Valor total original pactado en la negociacion. */
    @Basic
    @Column(name = "VALORTOTAL")
    private Double valorTotal;

    /**
     * Tipo de financiacion/pago acordado.
     * Ej: FIJO (pagos fijos periodicos), HITO (por hitos), PORCENTAJE (por porcentajes), UNICO.
     */
    @Basic
    @Column(name = "TIPOFINANCIACION", length = 50)
    private String tipoFinanciacion;

    /** Numero de pagos o cuotas acordadas. */
    @Basic
    @Column(name = "NUMEROPAGOS")
    private Long numeroPagos;

    /** Observaciones adicionales de la negociacion. */
    @Basic
    @Column(name = "OBSERVACION", length = 2000)
    private String observacion;

    /**
     * Estado de la negociacion.
     * 1 = Activa, 0 = Inactiva/Cerrada, 2 = Suspendida.
     */
    @Basic
    @Column(name = "ESTADO")
    private Long estado;

    /** Usuario que registra la negociacion. */
    @ManyToOne
    @JoinColumn(name = "USUARIO", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /** Fecha y hora en la que se registra la negociacion en el sistema. */
    @Basic
    @Column(name = "FECHAREGISTRO")
    private LocalDateTime fechaRegistro;

    /** Usuario que realizó la última modificacion. */
    @ManyToOne
    @JoinColumn(name = "USUARIOMODIF", referencedColumnName = "PJRQCDGO")
    private Usuario usuarioModif;

    /** Fecha y hora de la última modificacion. */
    @Basic
    @Column(name = "FECHAMODIF")
    private LocalDateTime fechaModif;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Titular getTitular() { return titular; }
    public void setTitular(Titular titular) { this.titular = titular; }

    public LocalDate getFechaNegociacion() { return fechaNegociacion; }
    public void setFechaNegociacion(LocalDate fechaNegociacion) { this.fechaNegociacion = fechaNegociacion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getNumContrato() { return numContrato; }
    public void setNumContrato(String numContrato) { this.numContrato = numContrato; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public String getTipoFinanciacion() { return tipoFinanciacion; }
    public void setTipoFinanciacion(String tipoFinanciacion) { this.tipoFinanciacion = tipoFinanciacion; }

    public Long getNumeroPagos() { return numeroPagos; }
    public void setNumeroPagos(Long numeroPagos) { this.numeroPagos = numeroPagos; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Usuario getUsuarioModif() { return usuarioModif; }
    public void setUsuarioModif(Usuario usuarioModif) { this.usuarioModif = usuarioModif; }

    public LocalDateTime getFechaModif() { return fechaModif; }
    public void setFechaModif(LocalDateTime fechaModif) { this.fechaModif = fechaModif; }
}
