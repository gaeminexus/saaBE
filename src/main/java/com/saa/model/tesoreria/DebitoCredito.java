package com.saa.model.tesoreria;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.saa.model.contabilidad.Asiento;
import com.saa.model.scp.Usuario;

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
 * <p>Pojo mapeo de tabla TSR.DBCR.
 * Entity Debito Credito.
 * Almacena los debitos creditos realizados a una cuenta bancaria.
 * Cada movimiento genera un asiento contable y afecta la conciliacion.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DBCR", schema = "TSR")
@SequenceGenerator(name = "SQ_DBCRCDGO", sequenceName = "TSR.SQ_DBCRCDGO", allocationSize = 1)
@NamedQueries({
	@NamedQuery(name = "DebitoCreditoAll", query = "select e from DebitoCredito e"),
	@NamedQuery(name = "DebitoCreditoId", query = "select e from DebitoCredito e where e.codigo = :id")
})
public class DebitoCredito implements Serializable {

    /**
     * Id.
     */
    @Basic
    @Id
    @Column(name = "DBCRCDGO", precision = 0)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SQ_DBCRCDGO")
    private Long codigo;
    
    /**
     * Cuenta bancaria a la que pertenece.
     */
    @ManyToOne
    @JoinColumn(name = "CNBCCDGO", referencedColumnName = "CNBCCDGO")
    private CuentaBancaria cuentaBancaria;    

    /**
     * Descripcion del movimiento.
     */
    @Basic
    @Column(name = "DBCRDSCR", length = 500)
    private String descripcion;
    
    /**
     * Tipo de movimiento. 1 = Debito, 2 = Credito
     */
    @Basic
    @Column(name = "DSCRTPOO")
    private Long tipo;
    
    /**
     * Numero de asiento contable.
     */
    @Basic
    @Column(name = "DBCRNMAS")
    private Long numeroAsiento;
    
    /**
     * Nombre del usuario que realiza la transaccion.
     */
    @Basic
    @Column(name = "DBCRUSRO", length = 200)
    private String nombreUsuario;
    
    /**
     * Fecha en la que se realiza la transaccion.
     */
    @Basic
    @Column(name = "DBCRFCHA")
    private LocalDateTime fecha;
    
    /**
     * Asiento contable generado.
     */
    @ManyToOne
    @JoinColumn(name = "ASNTCDGO", referencedColumnName = "ASNTCDGO")
    private Asiento asiento;
    
    /**
     * Movimiento bancario generado.
     */
    @ManyToOne
    @JoinColumn(name = "MVCBCDGO", referencedColumnName = "MVCBCDGO")
    private MovimientoBanco movimientoBanco;

    /**
     * Usuario que realiza la transaccion.
     */
    @ManyToOne
    @JoinColumn(name = "PJRQCDUS", referencedColumnName = "PJRQCDGO")
    private Usuario usuario;

    /**
     * Estado. 1 = Activo, 0 = Inactivo
     */
    @Basic
    @Column(name = "DBCRESTD")
    private Long estado;
    
	/**
	 * Devuelve codigo
	 * @return codigo
	 */
	public Long getCodigo() {
		return codigo;
	}

	/**
	 * Asigna codigo
	 * @param codigo Nuevo valor para codigo 
	 */
	public void setCodigo(Long codigo) {
		this.codigo = codigo;
	}
	
	/**
	 * Devuelve cuentaBancaria
	 */
	public CuentaBancaria getCuentaBancaria() {
		return this.cuentaBancaria;
	}
	
	/**
	 * Asigna cuentaBancaria
	 */
	public void setCuentaBancaria(CuentaBancaria cuentaBancaria) {
		this.cuentaBancaria = cuentaBancaria;
	}

	/**
	 * Devuelve descripcion
	 * @return descripcion
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna descripcion
	 * @param descripcion Nuevo valor para descripcion 
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}
	
	/**
	 * Devuelve tipo
	 * @return tipo
	 */
	public Long getTipo() {
		return tipo;
	}

	/**
	 * Asigna tipo
	 * @param tipo Nuevo valor para tipo 
	 */
	public void setTipo(Long tipo) {
		this.tipo = tipo;
	}
	
	/**
	 * Devuelve numeroAsiento
	 * @return numeroAsiento
	 */
	public Long getNumeroAsiento() {
		return numeroAsiento;
	}

	/**
	 * Asigna numeroAsiento
	 * @param numeroAsiento Nuevo valor para numeroAsiento 
	 */
	public void setNumeroAsiento(Long numeroAsiento) {
		this.numeroAsiento = numeroAsiento;
	}
	
	/**
	 * Devuelve nombreUsuario
	 * @return nombreUsuario
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna nombreUsuario
	 * @param nombreUsuario Nuevo valor para nombreUsuario 
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	/**
	 * Devuelve fecha
	 * @return fecha
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna fecha
	 * @param fecha Nuevo valor para fecha 
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
	
	/**
	 * Devuelve asiento
	 */
	public Asiento getAsiento() {
		return this.asiento;
	}
	
	/**
	 * Asigna asiento
	 */
	public void setAsiento(Asiento asiento) {
		this.asiento = asiento;
	}	

	/**
	 * Devuelve movimientoBanco
	 */
	public MovimientoBanco getMovimientoBanco() {
		return movimientoBanco;
	}

	/**
	 * Asigna movimientoBanco
	 */
	public void setMovimientoBanco(MovimientoBanco movimientoBanco) {
		this.movimientoBanco = movimientoBanco;
	}

	/**
	 * Devuelve usuario
	 */
	public Usuario getUsuario() {
		return this.usuario;
	}
	
	/**
	 * Asigna usuario
	 */
	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	/**
	 * Devuelve estado
	 * @return estado
	 */
	public Long getEstado() {
		return estado;
	}

	/**
	 * Asigna estado
	 * @param estado Nuevo valor para estado 
	 */
	public void setEstado(Long estado) {
		this.estado = estado;
	}

}
