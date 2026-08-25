package com.saa.model.crd;

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
 * Representa la tabla CBPR (ConfiguracionBandaProducto).
 *
 * Cabecera de la parametrizacion de bandas de cartera: una fila por
 * (producto, empresa, tipo de cartera, vigencia). A una fecha dada solo puede haber UNA
 * configuracion vigente y activa por esa terna — la unicidad la valida el servicio, no la
 * base (ver {@code ConfiguracionBandaProductoService.guardarConfiguracion}).
 *
 * <b>Vigencia historica.</b> Un cambio normativo NO muta las bandas en caliente: cierra
 * {@code fechaHasta} de la configuracion vigente y crea una nueva desde la fecha del
 * cambio. La anterior queda para reprocesos y auditoria. Ver
 * {@code ConfiguracionBandaProductoService.cerrarVigencia}.
 *
 * <b>Solo el CAPITAL se distribuye por bandas.</b> Intereses, mora y seguros van a cuentas
 * propias del producto resueltas por plantilla contable (CNT.PLNS/CNT.DTPL), no por aqui.
 *
 * Diseno completo en docs/logica-negocio/crd/LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md
 * §8; DDL en docs/logica-negocio/crd/sql/DDL-BANDAS-PRODUCTO.sql.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "CBPR", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "ConfiguracionBandaProductoAll",
                query = "select e from ConfiguracionBandaProducto e"),
    @NamedQuery(name = "ConfiguracionBandaProductoId",
                query = "select e from ConfiguracionBandaProducto e where e.codigo = :id")
})
public class ConfiguracionBandaProducto implements Serializable, EntidadAuditableFechaHora {

    /** Codigo de la configuracion. PK autoincremental (IDENTITY, no secuencia). */
    @Id
    @Basic
    @Column(name = "CBPRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /** FK - Producto de credito al que aplica la parametrizacion. */
    @ManyToOne
    @JoinColumn(name = "PRDCCDGO", referencedColumnName = "PRDCCDGO")
    private Producto producto;

    /** FK - Empresa (nodo SCP.PJRQ de nivel empresa). La parametrizacion es por producto + empresa. */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Tipo de cartera: 1 = por vencer, 2 = vencido.
     * Se almacena el valor, no la FK al catalogo. Ver {@link com.saa.rubros.TipoCarteraBanda}.
     */
    @Basic
    @Column(name = "CBPRTPCR")
    private Long tipoCartera;

    /** Inicio de vigencia de esta configuracion. Obligatorio. */
    @Basic
    @Column(name = "CBPRFCIN")
    private LocalDate fechaDesde;

    /** Fin de vigencia. NULL = configuracion vigente. */
    @Basic
    @Column(name = "CBPRFCFN")
    private LocalDate fechaHasta;

    /** Fecha y hora de registro. */
    @Basic
    @Column(name = "CBPRFCRG")
    private LocalDateTime fechaRegistro;

    /** Usuario que registro. */
    @Basic
    @Column(name = "CBPRUSRG", length = 2000)
    private String usuarioRegistro;

    /** IP desde la que se registro. */
    @Basic
    @Column(name = "CBPRIPRG", length = 50)
    private String ipRegistro;

    /** Fecha y hora de la ultima modificacion. */
    @Basic
    @Column(name = "CBPRFCMD")
    private LocalDateTime fechaModificacion;

    /** Usuario que modifico. */
    @Basic
    @Column(name = "CBPRUSMD", length = 2000)
    private String usuarioModificacion;

    /** IP desde la que se modifico. */
    @Basic
    @Column(name = "CBPRIPMD", length = 50)
    private String ipModificacion;

    /** Estado: 1 = activo, 2 = inactivo. Ver {@link com.saa.rubros.Estado}. */
    @Basic
    @Column(name = "CBPRESTD")
    private Long estado;

    public ConfiguracionBandaProducto() {
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

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
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
