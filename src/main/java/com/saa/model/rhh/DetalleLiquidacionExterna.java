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
 * Concepto de una {@link LiquidacionExterna}: tipo (ver
 * {@link com.saa.rubros.RhhConceptoLiquidacionExterna}) y valor, siempre positivo (el signo
 * lo da el tipo). Diseno: docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md.
 *
 * <p>Apunta a la cabecera y no al reves (mismo patron que {@link DetalleLiquidacion} →
 * {@link Liquidacion}): {@link LiquidacionExterna} no tiene una coleccion de detalles, asi
 * que Jackson no forma un ciclo al serializar.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "DLEX", schema = "RHH")
@NamedQueries({
	@NamedQuery(name = "DetalleLiquidacionExternaId", query = "select e from DetalleLiquidacionExterna e where e.codigo=:id"),
	@NamedQuery(name = "DetalleLiquidacionExternaAll", query = "select e from DetalleLiquidacionExterna e")
})
public class DetalleLiquidacionExterna implements Serializable, EntidadAuditableFechaHora {

	/**
	 * Codigo unico del detalle.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Basic
	@Column(name = "DLEXCDGO")
	private Long codigo;

	/**
	 * Liquidacion a la que pertenece.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "LQEXCDGO", referencedColumnName = "LQEXCDGO", nullable = false)
	private LiquidacionExterna liquidacion;

	/**
	 * Tipo de concepto. Ver {@link com.saa.rubros.RhhConceptoLiquidacionExterna} (CHECK
	 * CK_DLEXTPCN: 1 a 9 o 20 a 23).
	 */
	@Basic
	@Column(name = "DLEXTPCN")
	private Long tipoConcepto;

	/**
	 * Descripcion del concepto. Opcional: si viene vacia, el acta usa el nombre del tipo.
	 */
	@Basic
	@Column(name = "DLEXDSCR", length = 200)
	private String descripcion;

	/**
	 * Valor del concepto. Siempre positivo (CHECK CK_DLEXVLOR &gt; 0); el signo (ingreso o
	 * descuento) lo da {@code tipoConcepto}.
	 */
	@Basic
	@Column(name = "DLEXVLOR")
	private Double valor;

	/**
	 * Orden de presentacion en el acta.
	 */
	@Basic
	@Column(name = "DLEXORDN")
	private Long orden;

	/**
	 * Fecha de registro. La sella el servidor.
	 */
	@Basic
	@Column(name = "DLEXFCHR")
	private LocalDateTime fechaRegistro;

	/**
	 * Usuario que registro.
	 */
	@Basic
	@Column(name = "DLEXUSRR", length = 60)
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

	public LiquidacionExterna getLiquidacion() {
		return liquidacion;
	}

	public void setLiquidacion(LiquidacionExterna liquidacion) {
		this.liquidacion = liquidacion;
	}

	public Long getTipoConcepto() {
		return tipoConcepto;
	}

	public void setTipoConcepto(Long tipoConcepto) {
		this.tipoConcepto = tipoConcepto;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public Long getOrden() {
		return orden;
	}

	public void setOrden(Long orden) {
		this.orden = orden;
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
