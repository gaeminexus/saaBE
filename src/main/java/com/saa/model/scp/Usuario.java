package com.saa.model.scp;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * @author GaemiSoft
 *         <p>
 *         Pojo mapeo de tabla SCP.PJRQ.
 *         Entity Usuario.
 *         Contiene los usuarios registrados en el sistema.
 *         </p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "PJRQ", schema = "SCP")
@SequenceGenerator(name = "SQ_PJRQCDGO", sequenceName = "SCP.SQ_PJRQCDGO")
@NamedQueries({
		@NamedQuery(name = "UsuarioAll", query = "select e from Usuario e"),
		@NamedQuery(name = "UsuarioId", query = "select e from Usuario e where e.codigo = :id")
})
public class Usuario implements Serializable {

	@Id
	@Column(name = "PJRQCDGO", precision = 0)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_PJRQCDGO")
	private Long codigo;

	@ManyToOne
	@JoinColumn(name = "PGSPCDGO", referencedColumnName = "PGSPCDGO")
	private Jerarquia jerarquia;

	@Basic
	@Column(name = "PJRQNMBR")
	private String nombre;

	@Basic
	@Column(name = "PJRQNVLL")
	private Long nivel;

	/**
	 * Id del padre
	 */
	@Basic
	@Column(name = "PJRQCDPD")
	private Long codigoPadre;

	/**
	 * Indica si la parametrizacion fue ingresada 1 = Ingresada, 0 = No ingresada
	 */
	@Basic
	@Column(name = "PJRQINGR")
	private Long ingresado;

	/**
	 * Devuelve codigo
	 * 
	 * @return codigo
	 */

	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * 
	 * @param codigo nuevo valor para codigo
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}

	/**
	 * Devuelve Id de la jerarqu�a por parametrizac�n, clave for�nea a PGSP.PGSPCDGO
	 * .
	 * 
	 * @return Id de la jerarqu�a por parametrizac�n, clave for�nea a PGSP.PGSPCDGO
	 *         .
	 */

	public Jerarquia getJerarquia() {
		return jerarquia;
	}

	/**
	 * Asigna Id de la jerarqu�a por parametrizac�n, clave for�nea a PGSP.PGSPCDGO.
	 * 
	 * @param Id de la jerarqu�a por parametrizac�n, clave for�nea a PGSP.PGSPCDGO.
	 */
	public void setJerarquia(Jerarquia jerarquia) {
		this.jerarquia = jerarquia;
	}

	/**
	 * Devuelve nombre
	 * 
	 * @return nombre
	 */

	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna nombre
	 * 
	 * @param nombre nuevo valor para nombre
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve nivel
	 * 
	 * @return nivel
	 */

	public Long getNivel() {
		return nivel;
	}

	/**
	 * Asigna nivel
	 * 
	 * @param nivel nuevo valor para nivel
	 */
	public void setNivel(Long nivel) {
		this.nivel = nivel;
	}

	/**
	 * Devuelve codigoPadre
	 * 
	 * @return codigoPadre
	 */

	public Long getCodigoPadre() {
		return codigoPadre;
	}

	/**
	 * Asigna codigoPadre
	 * 
	 * @param codigoPadre nuevo valor para codigoPadre
	 */
	public void setCodigoPadre(Long codigoPadre) {
		this.codigoPadre = codigoPadre;
	}

	/**
	 * Devuelve ingresado
	 * 
	 * @return ingresado
	 */

	public Long getIngresado() {
		return ingresado;
	}

	/**
	 * Asigna ingresado
	 * 
	 * @param ingresado Nuevo valor para ingresado
	 */
	public void setIngresado(Long ingresado) {
		this.ingresado = ingresado;
	}

}
