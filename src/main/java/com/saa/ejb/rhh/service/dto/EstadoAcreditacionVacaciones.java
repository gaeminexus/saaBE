package com.saa.ejb.rhh.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de {@code GET /rest/sldv/estadoAcreditacion/{idEmpresa}}: hasta donde esta
 * acreditado el anio, quien se quedo sin saldo y si la acreditacion esta desactualizada.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class EstadoAcreditacionVacaciones {

	/** Ultima corrida registrada, de cualquier estado; nula si nunca se registro ninguna. */
	private UltimaCorrida ultimaCorrida;

	/** Ultimo periodo mensual cerrado de la empresa; nulo si no ha cerrado ninguno. */
	private UltimoPeriodoCerrado ultimoPeriodoCerrado;

	/** Fin del ultimo periodo cerrado; si no hay ninguno, hoy. */
	private LocalDate cortePropuesto;

	/** Activos del anio en curso que no tienen fila en RHH.SLDV. */
	private int activosSinSaldo;

	/** Los mismos empleados de {@link #activosSinSaldo}, uno por uno. */
	private List<EmpleadoSinSaldo> empleadosSinSaldo = new ArrayList<EmpleadoSinSaldo>();

	/**
	 * true si la ultima corrida OK es anterior al ultimo periodo cerrado, si nunca hubo una
	 * corrida OK habiendo periodo cerrado, o si hay activos sin saldo.
	 */
	private boolean desactualizada;

	public EstadoAcreditacionVacaciones() {
	}

	public UltimaCorrida getUltimaCorrida() {
		return ultimaCorrida;
	}

	public void setUltimaCorrida(UltimaCorrida ultimaCorrida) {
		this.ultimaCorrida = ultimaCorrida;
	}

	public UltimoPeriodoCerrado getUltimoPeriodoCerrado() {
		return ultimoPeriodoCerrado;
	}

	public void setUltimoPeriodoCerrado(UltimoPeriodoCerrado ultimoPeriodoCerrado) {
		this.ultimoPeriodoCerrado = ultimoPeriodoCerrado;
	}

	public LocalDate getCortePropuesto() {
		return cortePropuesto;
	}

	public void setCortePropuesto(LocalDate cortePropuesto) {
		this.cortePropuesto = cortePropuesto;
	}

	public int getActivosSinSaldo() {
		return activosSinSaldo;
	}

	public void setActivosSinSaldo(int activosSinSaldo) {
		this.activosSinSaldo = activosSinSaldo;
	}

	public List<EmpleadoSinSaldo> getEmpleadosSinSaldo() {
		return empleadosSinSaldo;
	}

	public void setEmpleadosSinSaldo(List<EmpleadoSinSaldo> empleadosSinSaldo) {
		this.empleadosSinSaldo = empleadosSinSaldo;
	}

	public boolean isDesactualizada() {
		return desactualizada;
	}

	public void setDesactualizada(boolean desactualizada) {
		this.desactualizada = desactualizada;
	}

	/** Ultima corrida de la bitacora RHH.CRVC. */
	public static class UltimaCorrida {

		private LocalDate fechaCorte;
		private LocalDateTime fechaEjecucion;
		private String usuario;
		private String origen;
		private String estado;
		private String mensaje;
		private Integer empleadosAcreditados;

		public UltimaCorrida() {
		}

		public LocalDate getFechaCorte() {
			return fechaCorte;
		}

		public void setFechaCorte(LocalDate fechaCorte) {
			this.fechaCorte = fechaCorte;
		}

		public LocalDateTime getFechaEjecucion() {
			return fechaEjecucion;
		}

		public void setFechaEjecucion(LocalDateTime fechaEjecucion) {
			this.fechaEjecucion = fechaEjecucion;
		}

		public String getUsuario() {
			return usuario;
		}

		public void setUsuario(String usuario) {
			this.usuario = usuario;
		}

		public String getOrigen() {
			return origen;
		}

		public void setOrigen(String origen) {
			this.origen = origen;
		}

		public String getEstado() {
			return estado;
		}

		public void setEstado(String estado) {
			this.estado = estado;
		}

		public String getMensaje() {
			return mensaje;
		}

		public void setMensaje(String mensaje) {
			this.mensaje = mensaje;
		}

		public Integer getEmpleadosAcreditados() {
			return empleadosAcreditados;
		}

		public void setEmpleadosAcreditados(Integer empleadosAcreditados) {
			this.empleadosAcreditados = empleadosAcreditados;
		}
	}

	/** Ultimo periodo mensual cerrado. */
	public static class UltimoPeriodoCerrado {

		private Integer anio;
		private Integer mes;
		private LocalDate fechaFin;

		public UltimoPeriodoCerrado() {
		}

		public Integer getAnio() {
			return anio;
		}

		public void setAnio(Integer anio) {
			this.anio = anio;
		}

		public Integer getMes() {
			return mes;
		}

		public void setMes(Integer mes) {
			this.mes = mes;
		}

		public LocalDate getFechaFin() {
			return fechaFin;
		}

		public void setFechaFin(LocalDate fechaFin) {
			this.fechaFin = fechaFin;
		}
	}

	/** Empleado activo sin saldo del anio. */
	public static class EmpleadoSinSaldo {

		private Long codigo;
		private String identificacion;
		private String nombre;

		public EmpleadoSinSaldo() {
		}

		public Long getCodigo() {
			return codigo;
		}

		public void setCodigo(Long codigo) {
			this.codigo = codigo;
		}

		public String getIdentificacion() {
			return identificacion;
		}

		public void setIdentificacion(String identificacion) {
			this.identificacion = identificacion;
		}

		public String getNombre() {
			return nombre;
		}

		public void setNombre(String nombre) {
			this.nombre = nombre;
		}
	}
}
