package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;

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
 * Campo de la linea de detalle del archivo bancario.
 *
 * <p>Uno por columna del archivo, en el orden en que se escriben. <code>campo</code> dice que
 * dato va —detalle del rubro 224—, y el resto describe como se escribe: posicion y longitud en
 * ancho fijo, relleno, decimales y formato de fecha.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DFMB", schema = "RHH")
@NamedQueries({
    @NamedQuery(name = "DetalleFormatoBancarioId",  query = "select e from DetalleFormatoBancario e where e.codigo=:id"),
    @NamedQuery(name = "DetalleFormatoBancarioAll", query = "select e from DetalleFormatoBancario e")
})
public class DetalleFormatoBancario implements Serializable, EntidadAuditableFechaHora {

    /**
     * Codigo del campo.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic
    @Column(name = "DFMBCDGO")
    private Long codigo;

    /**
     * Formato al que pertenece el campo.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "FMBNCDGO", referencedColumnName = "FMBNCDGO", nullable = false)
    private FormatoArchivoBancario formato;

    /**
     * Dato que va en el campo: detalle del rubro RHH_CAMPO_ARCHIVO_BANCARIO.
     */
    @Basic
    @Column(name = "DFMBCMPO")
    private Long campo;

    /**
     * Orden del campo dentro de la linea de detalle.
     */
    @Basic
    @Column(name = "DFMBORDN")
    private Integer orden;

    /**
     * Posicion de inicio en formato de ancho fijo, base 1.
     */
    @Basic
    @Column(name = "DFMBINCO")
    private Integer indiceInicio;

    /**
     * Longitud del campo en formato de ancho fijo.
     */
    @Basic
    @Column(name = "DFMBLNGT")
    private Integer longitud;

    /**
     * Lado del relleno hasta la longitud: I izquierda, D derecha.
     */
    @Basic
    @Column(name = "DFMBRLLN", length = 1)
    private String ladoRelleno;

    /**
     * Caracter con el que se rellena.
     */
    @Basic
    @Column(name = "DFMBCRLL", length = 1)
    private String caracterRelleno;

    /**
     * Decimales del importe cuando el campo es un valor.
     */
    @Basic
    @Column(name = "DFMBDCML")
    private Integer decimales;

    /**
     * Indica si el importe lleva separador decimal (S) o va en centavos corridos (N).
     */
    @Basic
    @Column(name = "DFMBSPDC", length = 1)
    private String incluyeSeparadorDecimal;

    /**
     * Formato de fecha propio del campo. Nulo usa el del formato.
     */
    @Basic
    @Column(name = "DFMBFRFC", length = 20)
    private String formatoFecha;

    /**
     * Valor cuando el campo es LITERAL FIJO.
     */
    @Basic
    @Column(name = "DFMBVLFJ", length = 100)
    private String valorFijo;

    /**
     * Estado del campo.
     */
    @Basic
    @Column(name = "DFMBESTD")
    private Long estado;

    /**
     * Fecha de registro.
     */
    @Basic
    @Column(name = "DFMBFCHR")
    private LocalDateTime fechaRegistro;

    /**
     * Usuario que registro.
     */
    @Basic
    @Column(name = "DFMBUSRR", length = 60)
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

    public FormatoArchivoBancario getFormato() {
        return formato;
    }

    public void setFormato(FormatoArchivoBancario formato) {
        this.formato = formato;
    }

    public Long getCampo() {
        return campo;
    }

    public void setCampo(Long campo) {
        this.campo = campo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Integer getIndiceInicio() {
        return indiceInicio;
    }

    public void setIndiceInicio(Integer indiceInicio) {
        this.indiceInicio = indiceInicio;
    }

    public Integer getLongitud() {
        return longitud;
    }

    public void setLongitud(Integer longitud) {
        this.longitud = longitud;
    }

    public String getLadoRelleno() {
        return ladoRelleno;
    }

    public void setLadoRelleno(String ladoRelleno) {
        this.ladoRelleno = ladoRelleno;
    }

    public String getCaracterRelleno() {
        return caracterRelleno;
    }

    public void setCaracterRelleno(String caracterRelleno) {
        this.caracterRelleno = caracterRelleno;
    }

    public Integer getDecimales() {
        return decimales;
    }

    public void setDecimales(Integer decimales) {
        this.decimales = decimales;
    }

    public String getIncluyeSeparadorDecimal() {
        return incluyeSeparadorDecimal;
    }

    public void setIncluyeSeparadorDecimal(String incluyeSeparadorDecimal) {
        this.incluyeSeparadorDecimal = incluyeSeparadorDecimal;
    }

    public String getFormatoFecha() {
        return formatoFecha;
    }

    public void setFormatoFecha(String formatoFecha) {
        this.formatoFecha = formatoFecha;
    }

    public String getValorFijo() {
        return valorFijo;
    }

    public void setValorFijo(String valorFijo) {
        this.valorFijo = valorFijo;
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
