package com.saa.ejb.cnt.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** C52: resultado de {@code POST /prdo/regularizarRedondeos}: lo que se llevaria (simulacion) o lo que se asento. */
public class ResultadoRegularizacionRedondeos implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    private Long anio;
    private Long mes;
    private boolean simulado;
    /** Ya habia un asiento vigente de esta regularizacion para el mes: no se repite. */
    private boolean yaRegularizado;
    private Long idAsiento;
    private String fecha;
    private double total;
    /** Una linea de texto por movimiento: «D cuenta valor» / «H cuenta valor». */
    private List<String> lineas = new ArrayList<String>();
    private String mensaje;

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public Long getAnio() { return anio; }
    public void setAnio(Long anio) { this.anio = anio; }
    public Long getMes() { return mes; }
    public void setMes(Long mes) { this.mes = mes; }
    public boolean isSimulado() { return simulado; }
    public void setSimulado(boolean simulado) { this.simulado = simulado; }
    public boolean isYaRegularizado() { return yaRegularizado; }
    public void setYaRegularizado(boolean yaRegularizado) { this.yaRegularizado = yaRegularizado; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public List<String> getLineas() { return lineas; }
    public void setLineas(List<String> lineas) { this.lineas = lineas; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
