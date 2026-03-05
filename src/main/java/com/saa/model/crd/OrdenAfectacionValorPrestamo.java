package com.saa.model.crd;

import java.io.Serializable;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entidad que representa el catálogo del orden de afectación de valores en pagos de préstamos.
 * Tabla: OAVP (CRD schema)
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "OAVP", schema = "CRD")
@SequenceGenerator(name = "SQ_OAVPCDGO", sequenceName = "CRD.SQ_OAVPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "OrdenAfectacionValorPrestamoAll", query = "select e from OrdenAfectacionValorPrestamo e"),
    @NamedQuery(name = "OrdenAfectacionValorPrestamoId", query = "select e from OrdenAfectacionValorPrestamo e where e.codigo = :id"),
    @NamedQuery(name = "OrdenAfectacionValorPrestamoByOrden", query = "select e from OrdenAfectacionValorPrestamo e order by e.orden asc")
})
public class OrdenAfectacionValorPrestamo implements Serializable {
    
    /**
     * Código (ID) de la tabla.
     */
    @Id
    @Column(name = "OAVPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_OAVPCDGO")
    private Long codigo;
    
    /**
     * Nombre o descripción de la afectación.
     */
    @Basic
    @Column(name = "OAVPNMBR", length = 200)
    private String nombre;
    
    /**
     * Número de orden en el que se afecta el valor.
     */
    @Basic
    @Column(name = "OAVPORDN")
    private Long orden;
    
    /**
     * Estado del registro (1 = Activo, 0/2 = Inactivo).
     */
    @Basic
    @Column(name = "OAVPESTD")
    private Long estado;
    
    // Constructor vacío
    public OrdenAfectacionValorPrestamo() {
        this.estado = 1L;
    }
    
    // Getters y Setters
    
    public Long getCodigo() {
        return codigo;
    }
    
    public void setCodigo(Long codigo) {
        this.codigo = codigo;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public Long getOrden() {
        return orden;
    }
    
    public void setOrden(Long orden) {
        this.orden = orden;
    }
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return "OrdenAfectacionValorPrestamo{" +
                "codigo=" + codigo +
                ", nombre='" + nombre + '\'' +
                ", orden=" + orden +
                ", estado=" + estado +
                '}';
    }
}
