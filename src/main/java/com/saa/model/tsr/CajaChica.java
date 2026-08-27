package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.cnt.PlanCuenta;
import com.saa.model.scp.Empresa;
import com.saa.model.scp.Usuario;

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
 * Entity CajaChica.
 * Fondo fijo con límite y cuenta contable propia. Tabla: TSR.CJCH.
 *
 * El saldo NUNCA se guarda aquí: se calcula sumando/restando los
 * {@link MovimientoCajaChica} activos (ver CajaChicaServiceImpl.saldo).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CJCH", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "CajaChicaAll", query = "select e from CajaChica e"),
    @NamedQuery(name = "CajaChicaId", query = "select e from CajaChica e where e.codigo = :id")
})
public class CajaChica implements Serializable {

    @Id
    @Basic
    @Column(name = "CJCHCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** Empresa contable a la que pertenece la caja. FK a SCP.PJRQ. */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /** Nombre de la caja chica. */
    @Basic
    @Column(name = "CJCHNMBR", length = 200)
    private String nombre;

    /** Cuenta contable de la caja. FK a CNT.PLNN. */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    /** Monto del fondo fijo (límite de la caja). */
    @Basic
    @Column(name = "CJCHMNTO")
    private Double montoFondo;

    /** Monto máximo permitido por gasto individual; null = sin tope. */
    @Basic
    @Column(name = "CJCHMXGS")
    private Double montoMaximoGasto;

    /** Porcentaje del fondo bajo el cual se alerta que hay que reponer (default 20). */
    @Basic
    @Column(name = "CJCHPRAL")
    private Double porcentajeAlerta;

    /** Nombre del responsable o custodio. */
    @Basic
    @Column(name = "CJCHRSPN", length = 200)
    private String responsable;

    /** Usuario custodio (SCP.PJRQ), opcional. */
    @ManyToOne
    @JoinColumn(name = "CJCHUSCS", referencedColumnName = "PJRQCDGO")
    private Usuario custodio;

    /** Observaciones. */
    @Basic
    @Column(name = "CJCHOBSR", length = 1000)
    private String observacion;

    /** Estado: 1=Activa, 2=Inactiva. Ver {@link com.saa.rubros.EstadoCajaChica}. */
    @Basic
    @Column(name = "CJCHESTD")
    private Long estado;

    /** Fecha de registro. */
    @Basic
    @Column(name = "CJCHFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registra. */
    @Basic
    @Column(name = "CJCHUSAR")
    private Long usuario;

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public PlanCuenta getPlanCuenta() { return planCuenta; }
    public void setPlanCuenta(PlanCuenta planCuenta) { this.planCuenta = planCuenta; }

    public Double getMontoFondo() { return montoFondo; }
    public void setMontoFondo(Double montoFondo) { this.montoFondo = montoFondo; }

    public Double getMontoMaximoGasto() { return montoMaximoGasto; }
    public void setMontoMaximoGasto(Double montoMaximoGasto) { this.montoMaximoGasto = montoMaximoGasto; }

    public Double getPorcentajeAlerta() { return porcentajeAlerta; }
    public void setPorcentajeAlerta(Double porcentajeAlerta) { this.porcentajeAlerta = porcentajeAlerta; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public Usuario getCustodio() { return custodio; }
    public void setCustodio(Usuario custodio) { this.custodio = custodio; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public Long getUsuario() { return usuario; }
    public void setUsuario(Long usuario) { this.usuario = usuario; }
}
