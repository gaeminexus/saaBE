package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
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
import jakarta.persistence.Table;

/**
 * Saldo de la migracion de apertura al corte. Se carga desde archivo, se valida, se materializa en las tablas operativas y puede revertirse de forma exacta gracias a SLAPRFTB y SLAPRFID.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "SLAP", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "SaldoAperturaId", query = "select e from SaldoApertura e where e.codigo=:id"),
    @NamedQuery(name = "SaldoAperturaAll", query = "select e from SaldoApertura e")
})
public class SaldoApertura implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo unico del saldo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "SLAPCDGO")
    private Long codigo;

    /**
     * Empresa propietaria del saldo (SCP.PJRQ).
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Empleado enlazado; nulo mientras no se resuelva la identificacion.
     */
    @ManyToOne
    @JoinColumn(name = "MPLDCDGO", referencedColumnName = "MPLDCDGO")
    private Empleado empleado;

    /**
     * Identificacion del empleado tal como vino en el archivo origen.
     */
    @Basic
    @Column(name = "SLAPIDNT", length = 20)
    private String identificacion;

    /**
     * Fecha de corte de la migracion.
     */
    @Basic
    @Column(name = "SLAPFCCR")
    private LocalDate fechaCorte;

    /**
     * Tipo de saldo: detalle del rubro RHH_TIPO_SALDO_APERTURA.
     */
    @Basic
    @Column(name = "SLAPTPSL")
    private Long tipoSaldo;

    /**
     * Valor del saldo.
     */
    @Basic
    @Column(name = "SLAPVLOR")
    private Double valor;

    /**
     * Dias, cuando el saldo es de vacaciones o antiguedad.
     */
    @Basic
    @Column(name = "SLAPDIAS")
    private Double dias;

    /**
     * Fecha, cuando el saldo es de antiguedad.
     */
    @Basic
    @Column(name = "SLAPFCHA")
    private LocalDate fecha;

    /**
     * Anio al que corresponde el saldo.
     */
    @Basic
    @Column(name = "SLAPANOO")
    private Integer anio;

    /**
     * Numero de cuotas pendientes, en prestamos.
     */
    @Basic
    @Column(name = "SLAPNMCT")
    private Integer numeroCuotas;

    /**
     * Observaciones.
     */
    @Basic
    @Column(name = "SLAPOBSR", length = 500)
    private String observacion;

    /**
     * Ya fue materializado en las tablas operativas (S/N). Hace idempotente la aplicacion.
     */
    @Basic
    @Column(name = "SLAPAPLC", length = 1)
    private String aplicado;

    /**
     * Fecha en que se materializo.
     */
    @Basic
    @Column(name = "SLAPFCAP")
    private LocalDate fechaAplicacion;

    /**
     * Tabla destino en la que se materializo; base de la reversion.
     */
    @Basic
    @Column(name = "SLAPRFTB", length = 30)
    private String tablaReferencia;

    /**
     * Id del registro creado en la tabla destino; base de la reversion.
     */
    @Basic
    @Column(name = "SLAPRFID")
    private Long idReferencia;

    /**
     * Fecha de ingreso que tenia el empleado <b>antes</b> de aplicar el saldo de antiguedad.
     * Nulo = no tenia ninguna.
     *
     * <p>Existe porque la antiguedad es <b>el unico</b> de los ocho tipos de saldo que
     * <b>sobreescribe</b> un valor del maestro en vez de crear una fila propia:
     * <code>SLAPRFTB</code>/<code>SLAPRFID</code> bastan para revertir los otros siete
     * --borrando el registro creado-- pero aqui no hay registro que borrar, solo una columna
     * que devolver a su valor anterior.</p>
     *
     * <p>Sin ella, revertir ponia <code>MPLDFCIN</code> en nulo, y un ciclo normal de aplicar,
     * revisar y revertir dejaba a toda la plantilla sin fecha de ingreso, sin un solo error. De
     * ahi salen la antiguedad, los fondos de reserva, el decimo cuarto proporcional y los anios
     * de servicio del finiquito.</p>
     */
    @Basic
    @Column(name = "SLAPFCAN")
    private LocalDate fechaAnterior;

    /**
     * Estado del registro.
     */
    @Basic
    @Column(name = "SLAPESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "SLAPFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "SLAPUSRR", length = 60)
    private String usuarioRegistro;

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

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public LocalDate getFechaCorte() {
        return fechaCorte;
    }

    public void setFechaCorte(LocalDate fechaCorte) {
        this.fechaCorte = fechaCorte;
    }

    public Long getTipoSaldo() {
        return tipoSaldo;
    }

    public void setTipoSaldo(Long tipoSaldo) {
        this.tipoSaldo = tipoSaldo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getDias() {
        return dias;
    }

    public void setDias(Double dias) {
        this.dias = dias;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Integer getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(Integer numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getAplicado() {
        return aplicado;
    }

    public void setAplicado(String aplicado) {
        this.aplicado = aplicado;
    }

    public LocalDate getFechaAplicacion() {
        return fechaAplicacion;
    }

    public void setFechaAplicacion(LocalDate fechaAplicacion) {
        this.fechaAplicacion = fechaAplicacion;
    }

    public String getTablaReferencia() {
        return tablaReferencia;
    }

    public void setTablaReferencia(String tablaReferencia) {
        this.tablaReferencia = tablaReferencia;
    }

    public Long getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(Long idReferencia) {
        this.idReferencia = idReferencia;
    }

    public LocalDate getFechaAnterior() {
        return fechaAnterior;
    }

    public void setFechaAnterior(LocalDate fechaAnterior) {
        this.fechaAnterior = fechaAnterior;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
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
