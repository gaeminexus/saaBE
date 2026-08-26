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
 * Representa la tabla BDCC (BandaCierreCartera).
 *
 * <b>El snapshot.</b> Una fila por (corrida, producto, tipo de cartera, banda) con el
 * CAPITAL que esa corrida contabilizó en esa banda. Es la "distribución anterior" contra la
 * que la corrida siguiente calcula las diferencias del asiento de reclasificación.
 *
 * <b>Por qué existe y no se lee la mayorización.</b> El asiento de cambio de bandas
 * registra diferencias. Si esa diferencia se calculara contra el saldo de la cuenta en
 * {@code CNT.DTMY}, arrastraría todo lo que otros procesos —pagos, entregas, novaciones—
 * escribieron sobre las mismas cuentas durante el mes, y la reclasificación movería plata
 * que no le corresponde. El snapshot guarda lo que ESTE proceso contabilizó, así que la
 * diferencia es exactamente lo que este proceso debe corregir.
 *
 * <b>La cuenta va congelada.</b> Además de la FK a {@link BandaProducto} se guarda la FK a
 * {@link PlanCuenta}: si la parametrización cierra su vigencia y la banda pasa a otra
 * cuenta, el snapshot tiene que seguir sabiendo en qué cuenta quedó el saldo que
 * contabilizó, o el reverso descargaría la cuenta equivocada.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BDCC", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "BandaCierreCarteraAll",
                query = "select e from BandaCierreCartera e"),
    @NamedQuery(name = "BandaCierreCarteraId",
                query = "select e from BandaCierreCartera e where e.codigo = :id")
})
public class BandaCierreCartera implements Serializable, EntidadAuditableFechaHora {

    /** Código de la fila del snapshot. PK autoincremental. */
    @Id
    @Basic
    @Column(name = "BDCCCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Corrida a la que pertenece el snapshot. */
    @ManyToOne
    @JoinColumn(name = "CRCTCDGO", referencedColumnName = "CRCTCDGO")
    private CorridaCierreCartera corrida;

    /** FK - Producto de crédito. */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    /** Tipo de cartera: 1 = por vencer, 2 = vencido. Ver {@link com.saa.rubros.TipoCarteraBanda}. */
    @Basic
    @Column(name = "BDCCTPCR")
    private Long tipoCartera;

    /** FK - Banda de la parametrización vigente a la fecha de proceso. */
    @ManyToOne
    @JoinColumn(name = "BNDPCDGO", referencedColumnName = "BNDPCDGO")
    private BandaProducto banda;

    /** Número de banda, copiado para ordenar y leer sin unir con BNDP. */
    @Basic
    @Column(name = "BDCCNMRO")
    private Long numero;

    /** FK - Cuenta contable en la que quedó el capital. Congelada, ver el javadoc de la clase. */
    @ManyToOne
    @JoinColumn(name = "PLNNCDGO", referencedColumnName = "PLNNCDGO")
    private PlanCuenta planCuenta;

    /** Capital contabilizado en la banda. */
    @Basic
    @Column(name = "BDCCCPTL")
    private Double capital;

    /** Cantidad de cuotas que aportaron a ese capital. Informativo y de control. */
    @Basic
    @Column(name = "BDCCCNTD")
    private Long cantidad;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "BDCCFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registró. */
    @Basic
    @Column(name = "BDCCUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP desde la que se registró. */
    @Basic
    @Column(name = "BDCCIPRG", length = 50)
    private String ipRegistro;

    /** Fecha y hora de la última modificación. */
    @Basic
    @Column(name = "BDCCFCMD")
    private LocalDateTime fechaModificacion;

    /** Usuario que modificó. */
    @Basic
    @Column(name = "BDCCUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP desde la que se modificó. */
    @Basic
    @Column(name = "BDCCIPMD", length = 50)
    private String ipModificacion;

    /** Estado de la fila: 1 = activo, 0 = inactivo. */
    @Basic
    @Column(name = "BDCCESTD")
    private Long estado;

    public BandaCierreCartera() {
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

    public CorridaCierreCartera getCorrida() {
        return corrida;
    }

    public void setCorrida(CorridaCierreCartera corrida) {
        this.corrida = corrida;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public BandaProducto getBanda() {
        return banda;
    }

    public void setBanda(BandaProducto banda) {
        this.banda = banda;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public PlanCuenta getPlanCuenta() {
        return planCuenta;
    }

    public void setPlanCuenta(PlanCuenta planCuenta) {
        this.planCuenta = planCuenta;
    }

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
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
