package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa la tabla HPCS (Histórico de Pagos de Cuenta Individual).
 *
 * Una fila por cada pago de liquidación de la cuenta individual de un partícipe
 * (cesantía, jubilación, retiro voluntario, cuenta patronal). La tabla ya existía en la
 * base y la alimentan los procesos de liquidación; el backend la mapeó el 2026-08-29
 * SOLO PARA LECTURA, como fuente de los certificados de partícipe:
 * <ul>
 *   <li>Certificado de haber recibido aportes: fecha en que se liquidó la cuenta
 *       patronal (tipos JP / CP).</li>
 *   <li>Certificado de licitud de fondos: monto y fecha del pago (tipo JRV / CRV / J / C).</li>
 * </ul>
 *
 * <b>Trampas verificadas contra la base (2026-08-29):</b>
 * <ul>
 *   <li>No tiene FK a CRD.ENTD: se relaciona por la cédula ({@code HPCSCEDU} =
 *       {@code ENTDNMID}). Las 304 cédulas existentes cruzan todas con ENTD.</li>
 *   <li>Solo hay filas desde 2024. Las liquidaciones anteriores quedaron en el sistema
 *       viejo (DELTA21) y NO están aquí: para esos partícipes el certificado no puede
 *       precargar el dato.</li>
 *   <li>{@code HPCSTIPC} viene con inconsistencias de mayúsculas ("crv"): comparar
 *       siempre con {@code UPPER}.</li>
 * </ul>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "HPCS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "HistoricoPagoCuentaIndividualAll", query = "select e from HistoricoPagoCuentaIndividual e"),
    @NamedQuery(name = "HistoricoPagoCuentaIndividualId",  query = "select e from HistoricoPagoCuentaIndividual e where e.codigo = :id")
})
public class HistoricoPagoCuentaIndividual implements Serializable {

    /** Tipo de pago: jubilación (personal). */
    public static final String TIPO_JUBILACION = "J";
    /** Tipo de pago: cesantía (personal). */
    public static final String TIPO_CESANTIA = "C";
    /** Tipo de pago: jubilación patronal. */
    public static final String TIPO_JUBILACION_PATRONAL = "JP";
    /** Tipo de pago: cesantía patronal. */
    public static final String TIPO_CESANTIA_PATRONAL = "CP";
    /** Tipo de pago: jubilación retiro voluntario. */
    public static final String TIPO_JUBILACION_RETIRO_VOLUNTARIO = "JRV";
    /** Tipo de pago: cesantía retiro voluntario. */
    public static final String TIPO_CESANTIA_RETIRO_VOLUNTARIO = "CRV";

    /** Código del pago. La tabla no es identity: el proceso que la alimenta asigna el código. */
    @Id
    @Basic
    @Column(name = "HPCSCDGO")
    private Long codigo;

    /** Cédula del partícipe (= CRD.ENTD.ENTDNMID). Sin FK. */
    @Basic
    @Column(name = "HPCSCEDU", length = 200)
    private String cedula;

    /** Fecha del pago. */
    @Basic
    @Column(name = "HPCSFCHP")
    private LocalDate fechaPago;

    /** Observación / concepto del pago (ej. "PAGO DE CUENTA INDIVIDUAL"). */
    @Basic
    @Column(name = "HPCSOBSV", length = 4000)
    private String observacion;

    /** Tipo de pago: J, C, JP, CP, JRV, CRV. Ver las constantes {@code TIPO_*}. */
    @Basic
    @Column(name = "HPCSTIPC", length = 50)
    private String tipo;

    /** Valor pagado. */
    @Basic
    @Column(name = "HPCSVLRR")
    private Double valor;

    /** Fecha de registro de la fila. */
    @Basic
    @Column(name = "HPCSFCRG")
    private LocalDate fechaRegistro;

    /** Usuario que registró la fila. */
    @Basic
    @Column(name = "HPCSUSRG", length = 30)
    private String usuarioRegistro;

    // ============================================================
    // Getters y Setters
    // ============================================================

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
}
