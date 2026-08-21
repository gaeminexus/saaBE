package com.saa.model.rhh;

import java.io.Serializable;
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
 * Formato del archivo bancario de la orden de pago de nomina.
 *
 * <p>Espejo de salida de <code>FormatoArchivoMarcacion</code>: el formato del banco es dato, no
 * codigo. La cabecera y el pie son plantillas con los marcadores <code>{FECHA}</code>,
 * <code>{CONTADOR}</code>, <code>{TOTAL}</code>, <code>{EMPRESA}</code> y
 * <code>{SECUENCIAL}</code>; una plantilla nula significa que ese banco no pide esa linea.</p>
 *
 * <p><code>tipoFormato</code> usa el rubro <b>209</b>, el mismo de las marcaciones: describe
 * cualquier archivo plano, y crear un rubro gemelo con los mismos valores habria sido
 * duplicar el catalogo.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "FMBN", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "FormatoArchivoBancarioId",  query = "select e from FormatoArchivoBancario e where e.codigo=:id"),
    @NamedQuery(name = "FormatoArchivoBancarioAll", query = "select e from FormatoArchivoBancario e")
})
public class FormatoArchivoBancario implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo del formato.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "FMBNCDGO")
    private Long codigo;

    /**
     * Empresa a la que pertenece el formato.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO")
    private Empresa empresa;

    /**
     * Nombre del formato.
     */
    @Basic
    @Column(name = "FMBNNMBR", length = 100)
    private String nombre;

    /**
     * Banco al que corresponde el formato.
     */
    @Basic
    @Column(name = "FMBNBNCO", length = 100)
    private String banco;

    /**
     * Tipo de formato: detalle del rubro 209. 1 ancho fijo, 2 delimitado.
     */
    @Basic
    @Column(name = "FMBNTPFR")
    private Long tipoFormato;

    /**
     * Caracter delimitador cuando el tipo es delimitado.
     */
    @Basic
    @Column(name = "FMBNDLMT", length = 5)
    private String delimitador;

    /**
     * Extension del archivo generado.
     */
    @Basic
    @Column(name = "FMBNEXTN", length = 10)
    private String extension;

    /**
     * Codificacion de caracteres del archivo generado.
     */
    @Basic
    @Column(name = "FMBNCDFC", length = 20)
    private String codificacion;

    /**
     * Formato de las fechas dentro del archivo, patron de java.time.
     */
    @Basic
    @Column(name = "FMBNFRFC", length = 20)
    private String formatoFecha;

    /**
     * Plantilla de la linea de cabecera, con marcadores. Nula si el banco no pide cabecera.
     */
    @Basic
    @Column(name = "FMBNCBCR", length = 1000)
    private String plantillaCabecera;

    /**
     * Plantilla de la linea de pie, con marcadores. Nula si el banco no pide pie.
     */
    @Basic
    @Column(name = "FMBNPIEE", length = 1000)
    private String plantillaPie;

    /**
     * Mapa del tipo de cuenta al codigo del banco: <code>alternoRubro199=codigo</code>
     * separado por punto y coma, por ejemplo <code>1=AH;2=CC</code>.
     */
    @Basic
    @Column(name = "FMBNMPTC", length = 200)
    private String mapaTipoCuenta;

    /**
     * Estado del formato: 1 activo, 0 inactivo.
     */
    @Basic
    @Column(name = "FMBNESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "FMBNFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "FMBNUSRR", length = 60)
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

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public Long getTipoFormato() {
        return tipoFormato;
    }

    public void setTipoFormato(Long tipoFormato) {
        this.tipoFormato = tipoFormato;
    }

    public String getDelimitador() {
        return delimitador;
    }

    public void setDelimitador(String delimitador) {
        this.delimitador = delimitador;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getCodificacion() {
        return codificacion;
    }

    public void setCodificacion(String codificacion) {
        this.codificacion = codificacion;
    }

    public String getFormatoFecha() {
        return formatoFecha;
    }

    public void setFormatoFecha(String formatoFecha) {
        this.formatoFecha = formatoFecha;
    }

    public String getPlantillaCabecera() {
        return plantillaCabecera;
    }

    public void setPlantillaCabecera(String plantillaCabecera) {
        this.plantillaCabecera = plantillaCabecera;
    }

    public String getPlantillaPie() {
        return plantillaPie;
    }

    public void setPlantillaPie(String plantillaPie) {
        this.plantillaPie = plantillaPie;
    }

    public String getMapaTipoCuenta() {
        return mapaTipoCuenta;
    }

    public void setMapaTipoCuenta(String mapaTipoCuenta) {
        this.mapaTipoCuenta = mapaTipoCuenta;
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
