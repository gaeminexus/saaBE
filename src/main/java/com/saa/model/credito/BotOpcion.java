package com.saa.model.credito;


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
	 * Representa la tabla BTPC (Opciones del sistema).
	 */
	@SuppressWarnings("serial")
	@Entity
	@Table(name = "BTPC", schema = "CRD")
	@NamedQueries({
	    @NamedQuery(name = "BotOpcionAll", query = "select e from BotOpcion e"),
	    @NamedQuery(name = "BotOpcionId", query = "select e from BotOpcion e where e.codigo = :id")
	})
	public class BotOpcion implements Serializable {

	    /**
	     * Código de la opción.
	     */
	    @Id
	    @Basic
	    @Column(name = "BTPCCDGO", precision = 0)
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long codigo;

	    /**
	     * Estado.
	     */
	    @Basic
	    @Column(name = "BTPCIDST")
	    private Long estado;

	    /**
	     * Código de la opción padre.
	     */
	    @Basic
	    @Column(name = "BTPCIDPC")
	    private Long codigoPadre;

	    /**
	     * Nombre de la opción.
	     */
	    @Basic
	    @Column(name = "BTPCNMBR", length = 50)
	    private String nombre;

	    /**
	     * Número de la opción.
	     */
	    @Basic
	    @Column(name = "BTPCNMRP")
	    private Long numero;

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

	    /**
	     * Devuelve codigoPadre.
	     * @return codigoPadre.
	     */
	    public Long getCodigoPadre() {
	        return codigoPadre;
	    }

	    /**
	     * Asigna codigoPadre.
	     * @param codigoPadre nuevo valor para codigoPadre.
	     */
	    public void setCodigoPadre(Long codigoPadre) {
	        this.codigoPadre = codigoPadre;
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
	     * Devuelve numero.
	     * @return numero.
	     */
	    public Long getNumero() {
	        return numero;
	    }

	    /**
	     * Asigna numero.
	     * @param numero nuevo valor para numero.
	     */
	    public void setNumero(Long numero) {
	        this.numero = numero;
	    }
	}


