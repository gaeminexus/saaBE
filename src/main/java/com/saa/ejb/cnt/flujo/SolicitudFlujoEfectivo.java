package com.saa.ejb.cnt.flujo;

import java.io.Serializable;

/**
 * Cuerpo de POST /flef/generar, /flef/excel, /flef/comparativo y /flef/comparativoExcel (C38).
 * <ul>
 * <li>{@code periodo}: {@link #PERIODO_MES} (variacion contra el cierre del mes anterior, como el formato de enero
 * 2026) o {@link #PERIODO_ACUMULADO} (contra el 31-dic del anio anterior, como el formato de diciembre 2025; si la
 * contabilidad del SAA empieza despues, contra el arranque).</li>
 * <li>{@code preliminar}: true = generar aunque el mes de corte no este cerrado (sale PRELIMINAR); false/null = exige
 * el mes cerrado para cartera y contabilidad (REPORTE_MES_NO_CERRADO), igual que el B17.</li>
 * <li>{@code reporte} (solo /flef/excel): {@link #REPORTE_INDIRECTO} (por defecto) o {@link #REPORTE_DIRECTO}.</li>
 * </ul>
 */
public class SolicitudFlujoEfectivo implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String PERIODO_MES = "MES";
	public static final String PERIODO_ACUMULADO = "ACUMULADO";
	public static final String REPORTE_INDIRECTO = "INDIRECTO";
	public static final String REPORTE_DIRECTO = "DIRECTO";

	private Long empresa;
	private Integer anio;
	private Integer mes;
	private String periodo;
	private Boolean preliminar;
	/** true = calcula ademas el flujo de caja directo (formato BIESS), un bloque por mes del periodo. */
	private Boolean incluirDirecto;
	private String reporte;
	private String usuario;

	public boolean esAcumulado() {
		return periodo != null && PERIODO_ACUMULADO.equalsIgnoreCase(periodo.trim());
	}

	public boolean esDirecto() {
		return reporte != null && REPORTE_DIRECTO.equalsIgnoreCase(reporte.trim());
	}

	public Long getEmpresa() { return empresa; }
	public void setEmpresa(Long empresa) { this.empresa = empresa; }
	public Integer getAnio() { return anio; }
	public void setAnio(Integer anio) { this.anio = anio; }
	public Integer getMes() { return mes; }
	public void setMes(Integer mes) { this.mes = mes; }
	public String getPeriodo() { return periodo; }
	public void setPeriodo(String periodo) { this.periodo = periodo; }
	public Boolean getPreliminar() { return preliminar; }
	public void setPreliminar(Boolean preliminar) { this.preliminar = preliminar; }
	public Boolean getIncluirDirecto() { return incluirDirecto; }
	public void setIncluirDirecto(Boolean incluirDirecto) { this.incluirDirecto = incluirDirecto; }
	public String getReporte() { return reporte; }
	public void setReporte(String reporte) { this.reporte = reporte; }
	public String getUsuario() { return usuario; }
	public void setUsuario(String usuario) { this.usuario = usuario; }
}
