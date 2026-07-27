/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.model.tsr;

import java.io.Serializable;

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
 * <p>Pojo mapeo de tabla TSR.GCEX.
 * Entity GrupoConciliacionExtracto.
 * Tabla de enlace: qué filas de {@link DetalleExtractoBancario} pertenecen a
 * un {@link GrupoConciliacionContable}. Muchas filas de este enlace pueden
 * apuntar al mismo grupo (lado bancario de un match N:M).</p>
 * <p>Un mismo DetalleExtractoBancario puede aparecer en más de una fila de
 * esta tabla a lo largo del tiempo (ej. se concilió, se deshizo el grupo -
 * grupo.estado pasa a 0 - y luego se concilió de nuevo en un grupo distinto),
 * por eso NO hay una restricción de unicidad a nivel de base de datos sobre
 * DEXBCDGO: la regla real es "a lo sumo un grupo ACTIVO por
 * DetalleExtractoBancario", que se valida en la capa de servicio antes de
 * crear un grupo nuevo (join contra GrupoConciliacionContable.estado),
 * conservando así el historial completo de grupos deshechos en vez de
 * borrar filas.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "GCEX", schema = "TSR")
@NamedQueries({
    @NamedQuery(name = "GrupoConciliacionExtractoAll", query = "select e from GrupoConciliacionExtracto e"),
    @NamedQuery(name = "GrupoConciliacionExtractoId",
        query = "select e from GrupoConciliacionExtracto e where e.codigo = :id")
})
public class GrupoConciliacionExtracto implements Serializable {

    /**
     * Código / PK autoincrementable.
     */
    @Basic
    @Id
    @Column(name = "GCEXCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    /**
     * FK al grupo de conciliación al que pertenece esta fila del extracto.
     */
    @ManyToOne
    @JoinColumn(name = "GRCCCDGO", nullable = false)
    private GrupoConciliacionContable grupo;

    /**
     * FK a la fila concreta de DetalleExtractoBancario incluida en el grupo.
     */
    @ManyToOne
    @JoinColumn(name = "DEXBCDGO", nullable = false)
    private DetalleExtractoBancario detalleExtractoBancario;

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

    public DetalleExtractoBancario getDetalleExtractoBancario() {
        return detalleExtractoBancario;
    }

    public void setDetalleExtractoBancario(DetalleExtractoBancario detalleExtractoBancario) {
        this.detalleExtractoBancario = detalleExtractoBancario;
    }
}
