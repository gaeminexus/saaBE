package com.saa.model.crd;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.cnt.PlanCuenta;

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
 * Representa la tabla BNDP (BandaProducto).
 *
 * Banda de cartera de una configuracion {@link ConfiguracionBandaProducto}. Numeradas
 * consecutivas desde 1 dentro de la configuracion. Cada banda define cuantos periodos de
 * 30 dias abarca y la cuenta contable donde se registra el CAPITAL de esa banda para ese
 * producto.
 *
 * <b>{@code periodos} NULL = banda abierta</b> ("el resto"): captura todo lo que excede la
 * banda anterior. Solo la ULTIMA banda de la configuracion puede serlo, y debe haber
 * exactamente una — lo valida el servicio.
 *
 * <b>Los rangos en dias NO se almacenan, se derivan</b> acumulando periodos:
 * <pre>
 *   diaInicio(k) = 30 * SUM(periodos 1..k-1) + 1
 *   diaFin(k)    = 30 * SUM(periodos 1..k)      (null en la banda abierta)
 * </pre>
 * El calculo vive en {@code ClasificadorBandaService}; ningun proceso debe repetirlo.
 *
 * Diseno completo en docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md
 * §8; DDL en docs/logica-negocio/crd/sql/DDL-BANDAS-PRODUCTO.sql.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BNDP", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "BandaProductoAll", query = "select e from BandaProducto e"),
    @NamedQuery(name = "BandaProductoId",  query = "select e from BandaProducto e where e.codigo = :id")
})
public class BandaProducto implements Serializable, EntidadAuditableFechaHora {

    /** Codigo de la banda. PK autoincremental (IDENTITY, no secuencia). */
    @Id
    @Basic
    @Column(name = "BNDPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Configuracion a la que pertenece la banda. */
    @ManyToOne
    @JoinColumn(name = "CBPRCDGO", referencedColumnName = "CBPRCDGO")
    private ConfiguracionBandaProducto configuracion;

    /** Numero de banda: 1..N, consecutivo dentro de la configuracion. */
    @Basic
    @Column(name = "BNDPNMRO")
    private Long numero;

    /**
     * Periodos de 30 dias que abarca la banda.
     * NULL = banda abierta ("el resto"); solo puede serlo la ultima banda.
     */
    @Basic
    @Column(name = "BNDPCNTD")
    private Long periodos;

    /**
     * FK - Cuenta contable del capital de esta banda para este producto.
     * Nunca se guarda el codigo de cuenta como texto (decision §9.1 del levantamiento).
     */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "BNDPFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registro. */
    @Basic
    @Column(name = "BNDPUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP desde la que se registro. */
    @Basic
    @Column(name = "BNDPIPRG", length = 50)
    private String ipRegistro;

    /** Fecha y hora de la ultima modificacion. */
    @Basic
    @Column(name = "BNDPFCMD")
    private LocalDateTime fechaModificacion;

    /** Usuario que modifico. */
    @Basic
    @Column(name = "BNDPUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP desde la que se modifico. */
    @Basic
    @Column(name = "BNDPIPMD", length = 50)
    private String ipModificacion;

    /** Estado: 1 = activo, 0 = inactivo. Ver {@link com.saa.rubros.Estado}. */
    @Basic
    @Column(name = "BNDPESTD")
    private Long estado;

    public BandaProducto() {
    }

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public ConfiguracionBandaProducto getConfiguracion() {
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionBandaProducto configuracion) {
        this.configuracion = configuracion;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Long getPeriodos() {
        return periodos;
    }

    public void setPeriodos(Long periodos) {
        this.periodos = periodos;
    }

    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }

    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }

    @Override
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    @Override
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(String usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public String getIpRegistro() {
        return ipRegistro;
    }

    public void setIpRegistro(String ipRegistro) {
        this.ipRegistro = ipRegistro;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    public String getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(String usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public String getIpModificacion() {
        return ipModificacion;
    }

    public void setIpModificacion(String ipModificacion) {
        this.ipModificacion = ipModificacion;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }
}
