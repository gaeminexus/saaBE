package com.saa.ejb.cnt.flujo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta de POST /flef/generar: estado de flujos de efectivo (metodo indirecto, formato de ASOPREP), sus controles
 * y, si se pidio, el flujo de caja directo (formato BIESS) mes a mes. Tambien lo usa /flef/excel.
 */
public class ResultadoFlujoEfectivo implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String DEFINITIVO = "DEFINITIVO";
	public static final String PRELIMINAR = "PRELIMINAR";

	private Long empresa;
	private int anio;
	private int mes;
	/** MES o ACUMULADO. */
	private String periodo;
	/** Corte contra el que se mide la variacion (cierre del mes anterior, 31-dic anterior o arranque del SAA). */
	private LocalDate fechaBase;
	/** Primer dia del periodo (el dia siguiente a la fecha base). */
	private LocalDate fechaInicio;
	private LocalDate fechaCorte;
	/** true si el periodo cruza el cierre del ejercicio (el resultado del anio anterior pasa a 34 el 1-ene). */
	private boolean cruzaCierreEjercicio;
	private boolean preliminar;
	/** DEFINITIVO o PRELIMINAR. */
	private String leyenda;
	private String estadoPeriodo;
	private String razonSocial;
	private String ruc;
	private String titulo;
	/** "AL 31 DE DICIEMBRE DE 2025". */
	private String subtitulo;
	/** "Del 01/06/2025 al 31/12/2025". */
	private String textoPeriodo;
	private String codigoEntidad;
	/** Cuenta 1 del B17 al corte (cabecera del formato BIESS). */
	private BigDecimal activosTotales;
	private List<LineaFlujo> lineas = new ArrayList<>();
	private List<ControlFlujo> controles = new ArrayList<>();
	private List<String> advertencias = new ArrayList<>();
	/** Notas del formato (observaciones del mapeo, arranque, cierre del ejercicio). */
	private List<String> notas = new ArrayList<>();
	/** Flujo de caja directo, un bloque por mes del periodo (vacio si no se pidio). */
	private List<FlujoCajaMes> flujoCaja = new ArrayList<>();
	private String firma1Nombre;
	private String firma1Cargo;
	private String firma2Nombre;
	private String firma2Cargo;
	private String nombreArchivo;

	public int contarErrores() {
		int n = 0;
		for (ControlFlujo c : controles) {
			if (ControlFlujo.ERROR.equals(c.getEstado())) {
				n++;
			}
		}
		for (FlujoCajaMes m : flujoCaja) {
			for (ControlFlujo c : m.getControles()) {
				if (ControlFlujo.ERROR.equals(c.getEstado())) {
					n++;
				}
			}
		}
		return n;
	}

	public LineaFlujo linea(String codigo) {
		for (LineaFlujo l : lineas) {
			if (l.getCodigo().equals(codigo)) {
				return l;
			}
		}
		return null;
	}

	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public int getAnio() { return anio; }
	public void setAnio(int anio) { this.anio = anio; }
	public int getMes() { return mes; }
	public void setMes(int mes) { this.mes = mes; }
	public String getPeriodo() { return periodo; }
	public void setPeriodo(String periodo) { this.periodo = periodo; }
	public LocalDate getFechaBase() { return fechaBase; }
	public void setFechaBase(LocalDate fechaBase) { this.fechaBase = fechaBase; }
	public LocalDate getFechaInicio() { return fechaInicio; }
	public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
	public LocalDate getFechaCorte() { return fechaCorte; }
	public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }
	public boolean isCruzaCierreEjercicio() { return cruzaCierreEjercicio; }
	public void setCruzaCierreEjercicio(boolean cruzaCierreEjercicio) { this.cruzaCierreEjercicio = cruzaCierreEjercicio; }
	public boolean isPreliminar() { return preliminar; }
	public void setPreliminar(boolean preliminar) { this.preliminar = preliminar; }
	public String getLeyenda() { return leyenda; }
	public void setLeyenda(String leyenda) { this.leyenda = leyenda; }
	public String getEstadoPeriodo() { return estadoPeriodo; }
	public void setEstadoPeriodo(String estadoPeriodo) { this.estadoPeriodo = estadoPeriodo; }
	public String getRazonSocial() { return razonSocial; }
	public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
	public String getRuc() { return ruc; }
	public void setRuc(String ruc) { this.ruc = ruc; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getSubtitulo() { return subtitulo; }
	public void setSubtitulo(String subtitulo) { this.subtitulo = subtitulo; }
	public String getTextoPeriodo() { return textoPeriodo; }
	public void setTextoPeriodo(String textoPeriodo) { this.textoPeriodo = textoPeriodo; }
	public String getCodigoEntidad() { return codigoEntidad; }
	public void setCodigoEntidad(String codigoEntidad) { this.codigoEntidad = codigoEntidad; }
	public BigDecimal getActivosTotales() { return activosTotales; }
	public void setActivosTotales(BigDecimal activosTotales) { this.activosTotales = activosTotales; }
	public List<LineaFlujo> getLineas() { return lineas; }
	public void setLineas(List<LineaFlujo> lineas) { this.lineas = lineas; }
	public List<ControlFlujo> getControles() { return controles; }
	public void setControles(List<ControlFlujo> controles) { this.controles = controles; }
	public List<String> getAdvertencias() { return advertencias; }
	public void setAdvertencias(List<String> advertencias) { this.advertencias = advertencias; }
	public List<String> getNotas() { return notas; }
	public void setNotas(List<String> notas) { this.notas = notas; }
	public List<FlujoCajaMes> getFlujoCaja() { return flujoCaja; }
	public void setFlujoCaja(List<FlujoCajaMes> flujoCaja) { this.flujoCaja = flujoCaja; }
	public String getFirma1Nombre() { return firma1Nombre; }
	public void setFirma1Nombre(String firma1Nombre) { this.firma1Nombre = firma1Nombre; }
	public String getFirma1Cargo() { return firma1Cargo; }
	public void setFirma1Cargo(String firma1Cargo) { this.firma1Cargo = firma1Cargo; }
	public String getFirma2Nombre() { return firma2Nombre; }
	public void setFirma2Nombre(String firma2Nombre) { this.firma2Nombre = firma2Nombre; }
	public String getFirma2Cargo() { return firma2Cargo; }
	public void setFirma2Cargo(String firma2Cargo) { this.firma2Cargo = firma2Cargo; }
	public String getNombreArchivo() { return nombreArchivo; }
	public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
}
