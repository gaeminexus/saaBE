package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.model.cnt.Asiento;
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
import jakarta.persistence.Table;

/**
 * Devolución (total o parcial) de un anticipo a empleado: el colaborador
 * deposita la plata en una cuenta de la empresa en vez de que se le siga
 * descontando del rol. Tabla: RHH.DVAN.
 *
 * <p>Es una tabla y no columnas en {@link AnticipoEmpleado} porque puede haber
 * varias devoluciones, totales o parciales, sobre un mismo anticipo (decisión
 * del usuario, ver docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md #1).</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DVAN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DevolucionAnticipoId", query = "select e from DevolucionAnticipo e where e.codigo=:id"),
    @NamedQuery(name = "DevolucionAnticipoAll", query = "select e from DevolucionAnticipo e")
})
public class DevolucionAnticipo implements Serializable {

    /**
     * Codigo unico de la devolucion.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DVANCDGO")
    private Long codigo;

    /**
     * Anticipo que se devuelve.
     */
    @ManyToOne
    @JoinColumn(name = "ANTECDGO", referencedColumnName = "ANTECDGO")
    private AnticipoEmpleado anticipo;

    /**
     * Fecha real del depósito, no la de captura.
     */
    @Basic
    @Column(name = "DVANFCHA")
    private LocalDate fecha;

    /**
     * Valor devuelto.
     */
    @Basic
    @Column(name = "DVANVLOR")
    private Double valor;

    /**
     * Cuenta bancaria de la empresa donde depositó el empleado.
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;

    /**
     * Número de papeleta o de transferencia.
     */
    @Basic
    @Column(name = "DVANRFRN", length = 200)
    private String referencia;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "DVANOBSR", length = 2000)
    private String observacion;

    /**
     * Id del TSR.INGR generado por {@code IngresoService.procesarIngreso}.
     * <p>
     * ⚠️ A PROPÓSITO no es {@code @ManyToOne}: {@code Ingreso} tiene varios
     * {@code @ManyToOne} EAGER, así que una relación acá arrastraría ese
     * grafo en cada fila de un listado — el mismo patrón que causó
     * {@code ORA-04036} con {@code PagoProgramado} (ver el javadoc de
     * {@code MovimientoCajaChica.idPago}). Id crudo: para anular solo hace
     * falta el id.
     */
    @Column(name = "INGRCDGO")
    private Long idIngreso;

    /**
     * Ids de las {@code CuotaDescuento} que esta devolución canceló (estado
     * ANULADA), separados por coma. Sin esto, anular la devolución no podría
     * distinguir qué cuotas devolver a PENDIENTE cuando el anticipo tiene más
     * de una devolución (decisión del usuario, #1 del contrato): dos
     * devoluciones nunca tocan la misma cuota (una cuota ya ANULADA sale de
     * {@code selectPendientesPorDescuento}), así que la lista identifica sin
     * ambigüedad las de ESTA devolución. Columna propuesta, no está en la
     * versión de {@code lap1-11} verificada contra el contrato original —
     * ver el reporte del ítem 9.
     */
    @Basic
    @Column(name = "DVANCUAN", length = 2000)
    private String idsCuotasCanceladas;

    /**
     * Id de la {@code CuotaDescuento} que esta devolución dejó con un valor
     * menor (siguió PENDIENTE), o null si no ajustó ninguna. Columna
     * propuesta, mismo motivo que {@link #idsCuotasCanceladas}.
     */
    @Basic
    @Column(name = "DVANCUAJ")
    private Long idCuotaAjustada;

    /**
     * Valor de la cuota ajustada ANTES de esta devolución, para poder
     * restaurarlo si se anula. Columna propuesta, mismo motivo.
     */
    @Basic
    @Column(name = "DVANCVOR")
    private Double valorOriginalCuotaAjustada;

    /**
     * Asiento contable del ingreso.
     */
    @ManyToOne
    @JoinColumn(name = "DVANASNT", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;

    /**
     * Estado: 1 Vigente, 2 Anulada. Ver
     * {@link com.saa.rubros.EstadoDevolucionAnticipo}.
     */
    @Basic
    @Column(name = "DVANESTD")
    private Long estado;

    /**
     * Motivo de anulación.
     */
    @Basic
    @Column(name = "DVANMTAN", length = 500)
    private String motivoAnulacion;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DVANFCRG")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registra.
     */
    @Basic
    @Column(name = "DVANUSAR")
    private Long usuario;

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public AnticipoEmpleado getAnticipo() {
        return anticipo;
    }

    public void setAnticipo(AnticipoEmpleado anticipo) {
        this.anticipo = anticipo;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public CuentaBancaria getCuentaBancaria() {
        return cuentaBancaria;
    }

    public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
        this.cuentaBancaria = cuentaBancaria;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Long getIdIngreso() {
        return idIngreso;
    }

    public void setIdIngreso(Long idIngreso) {
        this.idIngreso = idIngreso;
    }

    public String getIdsCuotasCanceladas() {
        return idsCuotasCanceladas;
    }

    public void setIdsCuotasCanceladas(String idsCuotasCanceladas) {
        this.idsCuotasCanceladas = idsCuotasCanceladas;
    }

    public Long getIdCuotaAjustada() {
        return idCuotaAjustada;
    }

    public void setIdCuotaAjustada(Long idCuotaAjustada) {
        this.idCuotaAjustada = idCuotaAjustada;
    }

    public Double getValorOriginalCuotaAjustada() {
        return valorOriginalCuotaAjustada;
    }

    public void setValorOriginalCuotaAjustada(Double valorOriginalCuotaAjustada) {
        this.valorOriginalCuotaAjustada = valorOriginalCuotaAjustada;
    }

    public Asiento getAsiento() {
        return asiento;
    }

    public void setAsiento(Asiento asiento) {
        this.asiento = asiento;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Long getUsuario() {
        return usuario;
    }

    public void setUsuario(Long usuario) {
        this.usuario = usuario;
    }
}
