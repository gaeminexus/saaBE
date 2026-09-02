package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.cxp.PagoProgramado;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Orden de pago consolidada de beneficios sociales (decimo tercero, decimo cuarto y fondos de
 * reserva acumulados). Tabla: RHH.ODBS.
 *
 * <p>Agrupa las liquidaciones sueltas ({@link LiquidacionBeneficioSocial}) de un tipo de
 * beneficio y un anio (y region, solo para decimo cuarto) y las paga con un solo
 * {@link PagoProgramado} en tesoreria. Ver
 * docs/logica-negocio/rhh/PLAN-PAGO-BENEFICIOS-Y-SALIDA-POR-TESORERIA.md #3.1 y el contrato
 * docs/logica-negocio/rhh/API-PAGO-BENEFICIOS-SOCIALES.md.</p>
 *
 * <p><b>Ojo con dos columnas:</b> igual que <code>OrdenPagoNomina.asientoPago</code>,
 * <code>ASNTCDGO</code> se mapea como <code>Long</code> y no como relacion JPA, porque cruza a
 * CNT y la relacion acoplaria los esquemas sin necesidad. <code>PGTRCDGO</code>, en cambio, SI
 * se mapea como relacion a {@link PagoProgramado} -igual que
 * <code>AnticipoEmpleado.pagoProgramado</code>-, porque PGS vive en el mismo mapeo JPA del
 * proyecto y es el precedente de la casa para este ciclo (guardar el PagoProgramado y leer su
 * estado).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ODBS", schema = "RHH")
@SequenceGenerator(name = "SQ_ODBSCDGO", sequenceName = "RHH.SQ_ODBSCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "OrdenBeneficioSocialId",  query = "select e from OrdenBeneficioSocial e where e.codigo=:id"),
    @NamedQuery(name = "OrdenBeneficioSocialAll", query = "select e from OrdenBeneficioSocial e")
})
public class OrdenBeneficioSocial implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico de la orden.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ODBSCDGO")
    @Basic
    @Column(name = "ODBSCDGO")
    private Long codigo;

    /**
     * Empresa.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de beneficio: detalle del rubro RHH_TIPO_BENEFICIO_SOCIAL. Esta orden solo admite
     * 1 (decimo tercero), 2 (decimo cuarto) y 3 (fondos de reserva).
     */
    @Basic
    @Column(name = "ODBSTPBN")
    private Long tipoBeneficio;

    /**
     * Anio del beneficio.
     */
    @Basic
    @Column(name = "ODBSANOO")
    private Integer anio;

    /**
     * Region del decimo cuarto (RhhRegionDecimoCuarto). NULL para los demas tipos.
     */
    @Basic
    @Column(name = "ODBSRGON")
    private Long region;

    /**
     * Numero de la orden.
     */
    @Basic
    @Column(name = "ODBSNMRO", length = 50)
    private String numero;

    /**
     * Fecha de emision.
     */
    @Basic
    @Column(name = "ODBSFCEM")
    private LocalDate fechaEmision;

    /**
     * Fecha de acreditacion del pago. Nula hasta confirmarPago.
     */
    @Basic
    @Column(name = "ODBSFCPG")
    private LocalDate fechaPago;

    /**
     * Total consolidado de la orden. Suma de las liquidaciones agrupadas.
     */
    @Basic
    @Column(name = "ODBSTTAL")
    private Double total;

    /**
     * Cantidad de empleados incluidos.
     */
    @Basic
    @Column(name = "ODBSNMEM")
    private Integer numeroEmpleados;

    /**
     * Pago programado en tesoreria. Se escribe al enviar a tesoreria.
     */
    @ManyToOne
    @JoinColumn(name = "PGTRCDGO", referencedColumnName = "PGTRCDGO")
    private PagoProgramado pagoProgramado;

    /**
     * Codigo del asiento de baja de provision. Sin relacion JPA: cruza a CNT. Lo escribe RRHH
     * al confirmar el pago, no tesoreria.
     */
    @Basic
    @Column(name = "ASNTCDGO")
    private Long asiento;

    /**
     * Estado de la orden: detalle del rubro RHH_ESTADO_ORDEN_BENEFICIO.
     */
    @Basic
    @Column(name = "ODBSESTD")
    private Long estado;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "ODBSOBSR", length = 500)
    private String observaciones;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "ODBSFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "ODBSUSRR", length = 60)
    private String usuarioRegistro;

    // =============================
    // Getters y Setters
    // =============================

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

    public Long getTipoBeneficio() {
        return tipoBeneficio;
    }

    public void setTipoBeneficio(Long tipoBeneficio) {
        this.tipoBeneficio = tipoBeneficio;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Long getRegion() {
        return region;
    }

    public void setRegion(Long region) {
        this.region = region;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Integer getNumeroEmpleados() {
        return numeroEmpleados;
    }

    public void setNumeroEmpleados(Integer numeroEmpleados) {
        this.numeroEmpleados = numeroEmpleados;
    }

    public PagoProgramado getPagoProgramado() {
        return pagoProgramado;
    }

    public void setPagoProgramado(PagoProgramado pagoProgramado) {
        this.pagoProgramado = pagoProgramado;
    }

    public Long getAsiento() {
        return asiento;
    }

    public void setAsiento(Long asiento) {
        this.asiento = asiento;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }
}
