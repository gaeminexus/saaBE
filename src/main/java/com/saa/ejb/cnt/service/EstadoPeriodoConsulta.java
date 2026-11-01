package com.saa.ejb.cnt.service;

import jakarta.ejb.Local;

/**
 * Punto de integracion con el servicio {@code estadoPeriodo(idEmpresa, anio, mes)} que crea la tarea C17
 * (rama cuadre/c17-cierres-bloqueantes). Los estados financieros oficiales (C19) solo necesitan saber si el
 * periodo contable del corte esta CERRADO (leyenda DEFINITIVO) o no (PRELIMINAR).
 *
 * <p>La implementacion {@code EstadoPeriodoConsultaImpl} delega en {@code EstadoCierreService.estadoPeriodo} de
 * C17: cerrado = cierre contable marcado (CNT.PRDO.PRDOCNCR = 1).
 */
@Local
public interface EstadoPeriodoConsulta {

	/** Estado del periodo contable del mes. Nunca null: si no hay periodo, {@code existe = false}. */
	EstadoPeriodo estadoPeriodo(Long idEmpresa, int anio, int mes) throws Throwable;

	/** Resultado minimo que necesita C19. */
	class EstadoPeriodo implements java.io.Serializable {
		private static final long serialVersionUID = 1L;
		private boolean existe;
		private boolean cerrado;
		private Integer estado;
		private String descripcion;

		public EstadoPeriodo() {
		}

		public EstadoPeriodo(boolean existe, boolean cerrado, Integer estado, String descripcion) {
			this.existe = existe;
			this.cerrado = cerrado;
			this.estado = estado;
			this.descripcion = descripcion;
		}

		public boolean isExiste() { return existe; }
		public void setExiste(boolean existe) { this.existe = existe; }
		public boolean isCerrado() { return cerrado; }
		public void setCerrado(boolean cerrado) { this.cerrado = cerrado; }
		public Integer getEstado() { return estado; }
		public void setEstado(Integer estado) { this.estado = estado; }
		public String getDescripcion() { return descripcion; }
		public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	}
}
