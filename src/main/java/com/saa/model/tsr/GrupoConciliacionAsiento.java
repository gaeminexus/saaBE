/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;

import com.saa.model.cnt.DetalleAsiento;

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
 * @author GaemiSoft
 * <p>Pojo mapeo de tabla TSR.GCAS.
 * Entity GrupoConciliacionAsiento.
 * Tabla de enlace: qué filas de {@link com.saa.model.cnt.DetalleAsiento}
 * pertenecen a un {@link GrupoConciliacionContable}. Muchas filas de este
 * enlace pueden apuntar al mismo grupo (lado contable de un match N:M).</p>
 * <p>Misma nota que {@link GrupoConciliacionExtracto}: no hay restricción de
 * unicidad a nivel de base de datos sobre DTASCDGO - la regla "a lo sumo un
 * grupo ACTIVO por DetalleAsiento" se valida en la capa de servicio.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GCAS", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "GrupoConciliacionAsientoAll", query = "select e from GrupoConciliacionAsiento e"),
    @NamedQuery(name = "GrupoConciliacionAsientoId",
        query = "select e from GrupoConciliacionAsiento e where e.codigo = :id")
})
public class GrupoConciliacionAsiento implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "GCASCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK al grupo de conciliación al que pertenece esta línea de asiento.
     */
    @ManyToOne
    @JoinColumn(name = "GRCCCDGO", nullable = false)
    private GrupoConciliacionContable grupo;

    /**
     * FK a la fila concreta de DetalleAsiento incluida en el grupo.
     */
    @ManyToOne
    @JoinColumn(name = "DTASCDGO", nullable = false)
    private DetalleAsiento detalleAsiento;

    // -------------------------------------------------------------------------
    // GETTERS Y SETTERS
    // -------------------------------------------------------------------------

    public Long getCodigo() {
        return codigo;
    }

    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }

    public GrupoConciliacionContable getGrupo() {
        return grupo;
    }

    public void setGrupo(GrupoConciliacionContable grupo) {
        this.grupo = grupo;
    }

    public DetalleAsiento getDetalleAsiento() {
        return detalleAsiento;
    }

    public void setDetalleAsiento(DetalleAsiento detalleAsiento) {
        this.detalleAsiento = detalleAsiento;
    }
}
