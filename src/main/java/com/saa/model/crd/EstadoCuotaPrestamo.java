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
 * Entidad que representa el catálogo de estados de cuotas de préstamos.
 * Tabla: ESCP (CRD schema)
 * @author GaemiSoft
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "ESCP", schema = "CRD")
@SequenceGenerator(name = "SQ_ESCPCDGO", sequenceName = "CRD.SQ_ESCPCDGO", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "EstadoCuotaPrestamoAll", query = "select e from EstadoCuotaPrestamo e"),
    @NamedQuery(name = "EstadoCuotaPrestamoId", query = "select e from EstadoCuotaPrestamo e where e.codigo = :id")
})
public class EstadoCuotaPrestamo implements Serializable {
    
    /**
     * Código (ID) de la tabla.
     */
    @Id
    @Column(name = "ESCPCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_ESCPCDGO")
    private Long codigo;
    
    /**
     * Nombre del estado de cuota de préstamo.
     */
    @Basic
    @Column(name = "ESCPNMBR", length = 200)
    private String nombre;
    
    /**
     * Código alterno numérico del estado.
     */
    @Basic
    @Column(name = "ESCPCDAL")
    private Long codigoAlterno;
    
    /**
     * Estado del registro (1 = Activo, 0/2 = Inactivo).
     */
    @Basic
    @Column(name = "ESCPESTD")
    private Long estado;
    
    // Constructor vacío
    public EstadoCuotaPrestamo() {
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
    
    public Long getCodigoAlterno() {
        return codigoAlterno;
    }
    
    public void setCodigoAlterno(Long codigoAlterno) {
        this.codigoAlterno = codigoAlterno;
    }
    
    public Long getEstado() {
        return estado;
    }
    
    public void setEstado(Long estado) {
        this.estado = estado;
    }
    
    @Override
    public String toString() {
        return "EstadoCuotaPrestamo{" +
                "codigo=" + codigo +
                ", nombre='" + nombre + '\'' +
                ", codigoAlterno=" + codigoAlterno +
                ", estado=" + estado +
                '}';
    }
}
