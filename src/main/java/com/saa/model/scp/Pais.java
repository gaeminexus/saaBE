package com.saa.model.scp;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Representa la tabla CRD.PSSS (Catálogo de países).
 *
 * <h3>⚠️ El paquete Java y el esquema de base NO coinciden, y es a propósito</h3>
 *
 * La clase vive en <b>{@code com.saa.model.scp}</b> pero la tabla sigue siendo
 * <b>{@code CRD.PSSS}</b>. No es un error: no lo "arregles" moviendo la clase de vuelta a
 * {@code com.saa.model.crd} ni cambiando el {@code schema} del {@code @Table}.
 *
 * <ul>
 *   <li><b>El paquete se movió a {@code scp} el 2026-08-24</b> porque
 *       {@code com.saa.model.tsr.Titular} importaba {@code com.saa.model.crd.Pais}. Esa era
 *       la única dependencia {@code tsr → crd} del backend, y dejaba a {@code tsr} sin
 *       compilar si se retiraba el módulo {@code crd}. El país no es un concepto de
 *       créditos: es un catálogo de núcleo, como Empresa, Usuario, Rubro y DetalleRubro.</li>
 *   <li><b>La tabla se quedó en {@code CRD}</b>: la migración de datos a {@code SCP.PSSS} se
 *       intentó en producción el 2026-08-24 y falló, y se decidió no reintentarla por ahora.
 *       Ver {@code docs/general/sql/MIGRACION-PAIS-CRD-A-SCP.md}, marcada NO APLICADA.</li>
 * </ul>
 *
 * El arreglo de compilación <b>no depende</b> del esquema donde viva la tabla, así que las
 * dos decisiones son independientes. Lo que queda pendiente para poder extraer {@code crd}
 * es la FK {@code TSR.TTLR.PSSSCDGO → CRD.PSSS}, que sigue vigente.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PSSS", schema = "CRD")
@NamedQueries({
    @NamedQuery(name = "PaisAll", query = "select e from Pais e"),
    @NamedQuery(name = "PaisId", query = "select e from Pais e where e.codigo = :id")
})
public class Pais implements Serializable {

    /**
     * Código del país.
     */
    @Id
    @Basic
    @Column(name = "PSSSCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * Código alterno (INEC).
     */
    @Basic
    @Column(name = "PSSSCDAL", length = 10)
    private String codigoAlterno;

    /**
     * Nombre del país.
     */
    @Basic
    @Column(name = "PSSSNMBR", length = 2000)
    private String nombre;

    /**
     * Nacionalidad.
     */
    @Basic
    @Column(name = "PSSSNCNL", length = 2000)
    private String nacionalidad;

    /**
     * Código de nacionalidad.
     */
    @Basic
    @Column(name = "PSSSCDNC", length = 10)
    private String codigoNacionalidad;

    /**
     * Código externo.
     */
    @Basic
    @Column(name = "PSSSCDEX", length = 50)
    private String codigoExterno;

    /**
     * Estado.
     */
    @Basic
    @Column(name = "PSSSIDST")
    private Long estado;

    // ============================================================
    // Getters y Setters
    // ============================================================

    /**
     * Devuelve codigo.
     * @return codigo.
     */
    public Long getCodigo() {
        return codigo;
    }

    /**
     * Asigna codigo.
     * @param codigo nuevo valor para codigo.
     */
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    /**
     * Devuelve codigoAlterno.
     * @return codigoAlterno.
     */
    public String getCodigoAlterno() {
        return codigoAlterno;
    }

    /**
     * Asigna codigoAlterno.
     * @param codigoAlterno nuevo valor para codigoAlterno.
     */
    public void setCodigoAlterno(String codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }

    /**
     * Devuelve nombre.
     * @return nombre.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre.
     * @param nombre nuevo valor para nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve nacionalidad.
     * @return nacionalidad.
     */
    public String getNacionalidad() {
        return nacionalidad;
    }

    /**
     * Asigna nacionalidad.
     * @param nacionalidad nuevo valor para nacionalidad.
     */
    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    /**
     * Devuelve codigoNacionalidad.
     * @return codigoNacionalidad.
     */
    public String getCodigoNacionalidad() {
        return codigoNacionalidad;
    }

    /**
     * Asigna codigoNacionalidad.
     * @param codigoNacionalidad nuevo valor para codigoNacionalidad.
     */
    public void setCodigoNacionalidad(String codigoNacionalidad) {
        this.codigoNacionalidad = codigoNacionalidad;
    }

    /**
     * Devuelve codigoExterno.
     * @return codigoExterno.
     */
    public String getCodigoExterno() {
        return codigoExterno;
    }

    /**
     * Asigna codigoExterno.
     * @param codigoExterno nuevo valor para codigoExterno.
     */
    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    /**
     * Devuelve estado.
     * @return estado.
     */
    public Long getEstado() {
        return estado;
    }

    /**
     * Asigna estado.
     * @param estado nuevo valor para estado.
     */
    public void setEstado(Long estado) {
        this.estado = estado;
    }
}

