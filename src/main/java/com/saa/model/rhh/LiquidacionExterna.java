package com.saa.model.rhh;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.saa.basico.util.EntidadAuditableFechaHora;
import com.saa.model.cxp.ProductoPago;
import com.saa.model.scp.Empresa;
import com.saa.model.tsr.BancoExterno;

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
 * Liquidacion de un ex-colaborador de la administracion anterior (salida antes de 2026),
 * sin registro en {@code RHH.MPLD}. Diseno:
 * docs/logica-negocio/rhh/API-LIQUIDACION-EXCOLABORADORES.md.
 *
 * <p><b>No es {@link Liquidacion} (RHH.LQDC).</b> Esa exige {@code MPLDCDGO}/{@code CNTECDGO}
 * NOT NULL, y estas personas no se crean como colaboradores (decision D1 del usuario). Se
 * paga por Tesoreria con el origen {@code OrigenPagoExterno.RHH_LIQUIDACION_EXCOLABORADOR}.</p>
 *
 * <p><b>Sin FK de base de datos hacia {@code productoPago} ({@code PGS.PRDP}) ni
 * {@code banco} ({@code TSR.BEXT})</b> (decision D6): una FK entre esquemas exige
 * {@code GRANT REFERENCES}, que ya hizo fallar scripts en silencio. El {@code @ManyToOne}
 * de JPA no depende de que la base tenga la restriccion declarada.</p>
 *
 * <p>Esta clase NO tiene una coleccion de {@link DetalleLiquidacionExterna}: el detalle
 * apunta a la cabecera (patron de {@link Liquidacion}/{@link DetalleLiquidacion}), nunca al
 * reves, para que Jackson no forme un ciclo al serializar.</p>
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "LQEX", schema = "RHH")
@NamedQueries({
	@NamedQuery(name = "LiquidacionExternaId", query = "select e from LiquidacionExterna e where e.codigo=:id"),
	@NamedQuery(name = "LiquidacionExternaAll", query = "select e from LiquidacionExterna e")
})
public class LiquidacionExterna implements Serializable, EntidadAuditableFechaHora {

	/**
	 * Codigo unico de la liquidacion.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Basic
	@Column(name = "LQEXCDGO")
	private Long codigo;

	/**
	 * Empresa a la que pertenece la liquidacion.
	 */
	@ManyToOne
	@JoinColumn(name = "PJRQCDGO", referencedColumnName = "PJRQCDGO", nullable = false)
	private Empresa empresa;

	/**
	 * Tipo de identificacion: C cedula, P pasaporte (CHECK CK_LQEXTPID).
	 */
	@Basic
	@Column(name = "LQEXTPID", length = 1)
	private String tipoIdentificacion;

	/**
	 * Identificacion del ex-colaborador, sin espacios (el servicio hace trim).
	 */
	@Basic
	@Column(name = "LQEXIDNT", length = 20)
	private String identificacion;

	/**
	 * Apellidos, en mayusculas (lo sella el servidor).
	 */
	@Basic
	@Column(name = "LQEXAPLL", length = 100)
	private String apellidos;

	/**
	 * Nombres, en mayusculas (lo sella el servidor).
	 */
	@Basic
	@Column(name = "LQEXNMBR", length = 100)
	private String nombres;

	/**
	 * Cargo que ocupaba. Texto libre.
	 */
	@Basic
	@Column(name = "LQEXCRGO", length = 150)
	private String cargo;

	/**
	 * Fecha de ingreso.
	 */
	@Basic
	@Column(name = "LQEXFCIN")
	private LocalDate fechaIngreso;

	/**
	 * Fecha de salida. Debe ser anterior a 2026-01-01 (CHECK CK_LQEXFCSL, decision D7):
	 * la salida de un colaborador actual se liquida con {@link Liquidacion} (/lqdc).
	 */
	@Basic
	@Column(name = "LQEXFCSL")
	private LocalDate fechaSalida;

	/**
	 * Causal de terminacion.
	 */
	@ManyToOne
	@JoinColumn(name = "LQEXCSTR", referencedColumnName = "CSTRCDGO")
	private CausalTerminacion causalTerminacion;

	/**
	 * Ultima remuneracion. Informativo, para el acta.
	 */
	@Basic
	@Column(name = "LQEXULRM")
	private Double ultimaRemuneracion;

	/**
	 * Total de ingresos. Lo calcula el servidor: suma de los conceptos 1 a 9, redondeada
	 * a 2 decimales. Lo que mande el cliente en este campo se ignora.
	 */
	@Basic
	@Column(name = "LQEXTTIN")
	private Double totalIngresos;

	/**
	 * Total de descuentos. Lo calcula el servidor: suma de los conceptos 20 a 23,
	 * redondeada a 2 decimales. Lo que mande el cliente en este campo se ignora.
	 */
	@Basic
	@Column(name = "LQEXTTDS")
	private Double totalDescuentos;

	/**
	 * Neto a pagar. Lo calcula el servidor: totalIngresos - totalDescuentos, redondeado
	 * a 2 decimales. Lo que mande el cliente en este campo se ignora. CHECK &gt; 0.
	 */
	@Basic
	@Column(name = "LQEXNETO")
	private Double neto;

	/**
	 * Producto de pago (PGS.PRDP) cuyo grupo apunta a la cuenta por pagar que se debita al
	 * pagar (decision D4: una sola cuenta por liquidacion). Sin FK de base de datos (D6).
	 */
	@ManyToOne
	@JoinColumn(name = "LQEXPRDP", referencedColumnName = "ID", nullable = false)
	private ProductoPago productoPago;

	/**
	 * Banco del ex-colaborador. Sin FK de base de datos (D6).
	 */
	@ManyToOne
	@JoinColumn(name = "LQEXBEXT", referencedColumnName = "BEXTCDGO")
	private BancoExterno banco;

	/**
	 * Tipo de cuenta: alterno del rubro 23. 1 ahorro, 2 corriente.
	 */
	@Basic
	@Column(name = "LQEXTPCT")
	private Long tipoCuenta;

	/**
	 * Numero de cuenta del ex-colaborador.
	 */
	@Basic
	@Column(name = "LQEXNMCT", length = 30)
	private String numeroCuenta;

	/**
	 * Estado de la liquidacion. Ver {@link com.saa.rubros.RhhEstadoLiquidacionExterna}.
	 */
	@Basic
	@Column(name = "LQEXESTD")
	private Long estado;

	/**
	 * Id del pago en Tesoreria (PGS.PGTR). Sin FK (otro esquema, D6).
	 */
	@Basic
	@Column(name = "LQEXPGTR")
	private Long idPago;

	/**
	 * Id del asiento contable del pago, copiado del pago confirmado al sincronizar.
	 */
	@Basic
	@Column(name = "LQEXASNT")
	private Long idAsiento;

	/**
	 * Fecha real del pago, copiada de {@code PagoProgramado.fechaRespuesta} al sincronizar.
	 * Decide el anio en el que esta liquidacion entra al RDEP.
	 */
	@Basic
	@Column(name = "LQEXFCPG")
	private LocalDate fechaPago;

	/**
	 * Observacion libre.
	 */
	@Basic
	@Column(name = "LQEXOBSR", length = 500)
	private String observacion;

	/**
	 * Motivo de la anulacion.
	 */
	@Basic
	@Column(name = "LQEXMTAN", length = 300)
	private String motivoAnulacion;

	/**
	 * Fecha de registro. La sella el servidor: JPA manda null y el DEFAULT de la columna
	 * no se aplica.
	 */
	@Basic
	@Column(name = "LQEXFCHR")
	private LocalDateTime fechaRegistro;

	/**
	 * Usuario que registro.
	 */
	@Basic
	@Column(name = "LQEXUSRR", length = 60)
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

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getTipoIdentificacion() {
		return tipoIdentificacion;
	}

	public void setTipoIdentificacion(String tipoIdentificacion) {
		this.tipoIdentificacion = tipoIdentificacion;
	}

	public String getIdentificacion() {
		return identificacion;
	}

	public void setIdentificacion(String identificacion) {
		this.identificacion = identificacion;
	}

	public String getApellidos() {
		return apellidos;
	}

	public void setApellidos(String apellidos) {
		this.apellidos = apellidos;
	}

	public String getNombres() {
		return nombres;
	}

	public void setNombres(String nombres) {
		this.nombres = nombres;
	}

	public String getCargo() {
		return cargo;
	}

	public void setCargo(String cargo) {
		this.cargo = cargo;
	}

	public LocalDate getFechaIngreso() {
		return fechaIngreso;
	}

	public void setFechaIngreso(LocalDate fechaIngreso) {
		this.fechaIngreso = fechaIngreso;
	}

	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}

	public CausalTerminacion getCausalTerminacion() {
		return causalTerminacion;
	}

	public void setCausalTerminacion(CausalTerminacion causalTerminacion) {
		this.causalTerminacion = causalTerminacion;
	}

	public Double getUltimaRemuneracion() {
		return ultimaRemuneracion;
	}

	public void setUltimaRemuneracion(Double ultimaRemuneracion) {
		this.ultimaRemuneracion = ultimaRemuneracion;
	}

	public Double getTotalIngresos() {
		return totalIngresos;
	}

	public void setTotalIngresos(Double totalIngresos) {
		this.totalIngresos = totalIngresos;
	}

	public Double getTotalDescuentos() {
		return totalDescuentos;
	}

	public void setTotalDescuentos(Double totalDescuentos) {
		this.totalDescuentos = totalDescuentos;
	}

	public Double getNeto() {
		return neto;
	}

	public void setNeto(Double neto) {
		this.neto = neto;
	}

	public ProductoPago getProductoPago() {
		return productoPago;
	}

	public void setProductoPago(ProductoPago productoPago) {
		this.productoPago = productoPago;
	}

	public BancoExterno getBanco() {
		return banco;
	}

	public void setBanco(BancoExterno banco) {
		this.banco = banco;
	}

	public Long getTipoCuenta() {
		return tipoCuenta;
	}

	public void setTipoCuenta(Long tipoCuenta) {
		this.tipoCuenta = tipoCuenta;
	}

	public String getNumeroCuenta() {
		return numeroCuenta;
	}

	public void setNumeroCuenta(String numeroCuenta) {
		this.numeroCuenta = numeroCuenta;
	}

	public Long getEstado() {
		return estado;
	}

	public void setEstado(Long estado) {
		this.estado = estado;
	}

	public Long getIdPago() {
		return idPago;
	}

	public void setIdPago(Long idPago) {
		this.idPago = idPago;
	}

	public Long getIdAsiento() {
		return idAsiento;
	}

	public void setIdAsiento(Long idAsiento) {
		this.idAsiento = idAsiento;
	}

	public LocalDate getFechaPago() {
		return fechaPago;
	}

	public void setFechaPago(LocalDate fechaPago) {
		this.fechaPago = fechaPago;
	}

	public String getObservacion() {
		return observacion;
	}

	public void setObservacion(String observacion) {
		this.observacion = observacion;
	}

	public String getMotivoAnulacion() {
		return motivoAnulacion;
	}

	public void setMotivoAnulacion(String motivoAnulacion) {
		this.motivoAnulacion = motivoAnulacion;
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
