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
 * Representa la tabla BIPR (Base Inicial Partícipes).
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "BIPR", schema = "CRD")
@SequenceGenerator(name = "SQ_BIPRNMRO", sequenceName = "CRD.ISEQ$$_87710", allocationSize = 1)
@NamedQueries({
    @NamedQuery(name = "BaseInicialParticipesAll", query = "select e from BaseInicialParticipes e"),
    @NamedQuery(name = "BaseInicialParticipesId", query = "select e from BaseInicialParticipes e where e.numero = :id")
})
public class BaseInicialParticipes implements Serializable {

    /**
     * Número de registro (ID).
     */
    @Basic
    @Id
    @Column(name = "BIPRNMRO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_BIPRNMRO")
    private Long numero;

    /**
     * Nombre del partícipe.
     */
    @Basic
    @Column(name = "BIPRNMBR", length = 200, nullable = false)
    private String nombre;

    /**
     * Cesantía Patronal.
     */
    @Basic
    @Column(name = "BIPRCSNP", precision = 18, nullable = false)
    private Double cesantiaPatronal;

    /**
     * Cesantía Personal.
     */
    @Basic
    @Column(name = "BIPRCSPR", precision = 18, nullable = false)
    private Double cesantiaPersonal;

    /**
     * Cesantía Retiro Voluntario.
     */
    @Basic
    @Column(name = "BIPRCSRV", precision = 18, nullable = false)
    private Double cesantiaRetiroVoluntario;

    /**
     * Jubilación Patronal.
     */
    @Basic
    @Column(name = "BIPRJBPT", precision = 18, nullable = false)
    private Double jubilacionPatronal;

    /**
     * Jubilación Personal.
     */
    @Basic
    @Column(name = "BIPRJBPR", precision = 18, nullable = false)
    private Double jubilacionPersonal;

    /**
     * Jubilación Retiro Voluntario.
     */
    @Basic
    @Column(name = "BIPRJBRV", precision = 18, nullable = false)
    private Double jubilacionRetiroVoluntario;

    /**
     * Pensión Complementaria.
     */
    @Basic
    @Column(name = "BIPRPNSN", precision = 18, nullable = false)
    private Double pensionComplementaria;

    /**
     * Rendimiento Cesantía Patronal.
     */
    @Basic
    @Column(name = "BIPRRNCP", precision = 18, nullable = false)
    private Double rendimientoCesantiaPatronal;

    /**
     * Rendimiento Cesantía Personal.
     */
    @Basic
    @Column(name = "BIPRRNPS", precision = 18, nullable = false)
    private Double rendimientoCesantiaPersonal;

    /**
     * Rendimiento Jubilación Patronal.
     */
    @Basic
    @Column(name = "BIPRRNJP", precision = 18, nullable = false)
    private Double rendimientoJubilacionPatronal;

    /**
     * Rendimiento Jubilación Personal.
     */
    @Basic
    @Column(name = "BIPRRNJR", precision = 18, nullable = false)
    private Double rendimientoJubilacionPersonal;

    /**
     * Total General.
     */
    @Basic
    @Column(name = "BIPRTTLG", precision = 18, nullable = false)
    private Double totalGeneral;
    
    /**
     * Id del sistema Saa de la tabla entidad
     */
    @Basic
    @Column(name = "BIPRIDSA")
    private Long idSaa;
    
    /**
     * Id del sistema Saa de la tabla entidad
     */
    @Basic
    @Column(name = "BIPRNMCD")
    private String cedula;

    // ============================================================
    // GETTERS Y SETTERS
    // ============================================================

    /**
     * Devuelve numero
     * @return numero
     */
    public Long getNumero() {
        return numero;
    }

    /**
     * Asigna numero
     * @param numero nuevo valor para numero
     */
    public void setNumero(Long numero) {
        this.numero = numero;
    }

    /**
     * Devuelve nombre
     * @return nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Asigna nombre
     * @param nombre nuevo valor para nombre
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Devuelve cesantiaPatronal
     * @return cesantiaPatronal
     */
    public Double getCesantiaPatronal() {
        return cesantiaPatronal;
    }

    /**
     * Asigna cesantiaPatronal
     * @param cesantiaPatronal nuevo valor para cesantiaPatronal
     */
    public void setCesantiaPatronal(Double cesantiaPatronal) {
        this.cesantiaPatronal = cesantiaPatronal;
    }

    /**
     * Devuelve cesantiaPersonal
     * @return cesantiaPersonal
     */
    public Double getCesantiaPersonal() {
        return cesantiaPersonal;
    }

    /**
     * Asigna cesantiaPersonal
     * @param cesantiaPersonal nuevo valor para cesantiaPersonal
     */
    public void setCesantiaPersonal(Double cesantiaPersonal) {
        this.cesantiaPersonal = cesantiaPersonal;
    }

    /**
     * Devuelve cesantiaRetiroVoluntario
     * @return cesantiaRetiroVoluntario
     */
    public Double getCesantiaRetiroVoluntario() {
        return cesantiaRetiroVoluntario;
    }

    /**
     * Asigna cesantiaRetiroVoluntario
     * @param cesantiaRetiroVoluntario nuevo valor para cesantiaRetiroVoluntario
     */
    public void setCesantiaRetiroVoluntario(Double cesantiaRetiroVoluntario) {
        this.cesantiaRetiroVoluntario = cesantiaRetiroVoluntario;
    }

    /**
     * Devuelve jubilacionPatronal
     * @return jubilacionPatronal
     */
    public Double getJubilacionPatronal() {
        return jubilacionPatronal;
    }

    /**
     * Asigna jubilacionPatronal
     * @param jubilacionPatronal nuevo valor para jubilacionPatronal
     */
    public void setJubilacionPatronal(Double jubilacionPatronal) {
        this.jubilacionPatronal = jubilacionPatronal;
    }

    /**
     * Devuelve jubilacionPersonal
     * @return jubilacionPersonal
     */
    public Double getJubilacionPersonal() {
        return jubilacionPersonal;
    }

    /**
     * Asigna jubilacionPersonal
     * @param jubilacionPersonal nuevo valor para jubilacionPersonal
     */
    public void setJubilacionPersonal(Double jubilacionPersonal) {
        this.jubilacionPersonal = jubilacionPersonal;
    }

    /**
     * Devuelve jubilacionRetiroVoluntario
     * @return jubilacionRetiroVoluntario
     */
    public Double getJubilacionRetiroVoluntario() {
        return jubilacionRetiroVoluntario;
    }

    /**
     * Asigna jubilacionRetiroVoluntario
     * @param jubilacionRetiroVoluntario nuevo valor para jubilacionRetiroVoluntario
     */
    public void setJubilacionRetiroVoluntario(Double jubilacionRetiroVoluntario) {
        this.jubilacionRetiroVoluntario = jubilacionRetiroVoluntario;
    }

    /**
     * Devuelve pensionComplementaria
     * @return pensionComplementaria
     */
    public Double getPensionComplementaria() {
        return pensionComplementaria;
    }

    /**
     * Asigna pensionComplementaria
     * @param pensionComplementaria nuevo valor para pensionComplementaria
     */
    public void setPensionComplementaria(Double pensionComplementaria) {
        this.pensionComplementaria = pensionComplementaria;
    }

    /**
     * Devuelve rendimientoCesantiaPatronal
     * @return rendimientoCesantiaPatronal
     */
    public Double getRendimientoCesantiaPatronal() {
        return rendimientoCesantiaPatronal;
    }

    /**
     * Asigna rendimientoCesantiaPatronal
     * @param rendimientoCesantiaPatronal nuevo valor para rendimientoCesantiaPatronal
     */
    public void setRendimientoCesantiaPatronal(Double rendimientoCesantiaPatronal) {
        this.rendimientoCesantiaPatronal = rendimientoCesantiaPatronal;
    }

    /**
     * Devuelve rendimientoCesantiaPersonal
     * @return rendimientoCesantiaPersonal
     */
    public Double getRendimientoCesantiaPersonal() {
        return rendimientoCesantiaPersonal;
    }

    /**
     * Asigna rendimientoCesantiaPersonal
     * @param rendimientoCesantiaPersonal nuevo valor para rendimientoCesantiaPersonal
     */
    public void setRendimientoCesantiaPersonal(Double rendimientoCesantiaPersonal) {
        this.rendimientoCesantiaPersonal = rendimientoCesantiaPersonal;
    }

    /**
     * Devuelve rendimientoJubilacionPatronal
     * @return rendimientoJubilacionPatronal
     */
    public Double getRendimientoJubilacionPatronal() {
        return rendimientoJubilacionPatronal;
    }

    /**
     * Asigna rendimientoJubilacionPatronal
     * @param rendimientoJubilacionPatronal nuevo valor para rendimientoJubilacionPatronal
     */
    public void setRendimientoJubilacionPatronal(Double rendimientoJubilacionPatronal) {
        this.rendimientoJubilacionPatronal = rendimientoJubilacionPatronal;
    }

    /**
     * Devuelve rendimientoJubilacionPersonal
     * @return rendimientoJubilacionPersonal
     */
    public Double getRendimientoJubilacionPersonal() {
        return rendimientoJubilacionPersonal;
    }

    /**
     * Asigna rendimientoJubilacionPersonal
     * @param rendimientoJubilacionPersonal nuevo valor para rendimientoJubilacionPersonal
     */
    public void setRendimientoJubilacionPersonal(Double rendimientoJubilacionPersonal) {
        this.rendimientoJubilacionPersonal = rendimientoJubilacionPersonal;
    }

    /**
     * Devuelve totalGeneral
     * @return totalGeneral
     */
    public Double getTotalGeneral() {
        return totalGeneral;
    }

    /**
     * Asigna totalGeneral
     * @param totalGeneral nuevo valor para totalGeneral
     */
    public void setTotalGeneral(Double totalGeneral) {
        this.totalGeneral = totalGeneral;
    }

	public Long getIdSaa() {
		return idSaa;
	}

	public void setIdSaa(Long idSaa) {
		this.idSaa = idSaa;
	}

	public String getCedula() {
		return cedula;
	}

	public void setCedula(String cedula) {
		this.cedula = cedula;
	}
}
