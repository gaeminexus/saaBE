package com.saa.ejb.asoprep.serviceImpl;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.saa.model.crd.ParticipeXCargaArchivo;

/**
 * C46: lo que devuelve {@link LectorRetornoPetro}: las filas del retorno de Petro y el cuadre de lo cargado contra los TOTAL que el propio reporte imprime
 * (por sección de producto y el TOTAL ==> final). Clase pura, sin EJB ni BD.
 */
public class ResultadoLecturaRetornoPetro implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Diferencia máxima, en valor absoluto, que se acepta entre lo que imprime Petro y lo cargado. */
    public static final double TOLERANCIA = 0.005;

    /** Tope de la columna CRD.CRAR.CRARALCD. */
    public static final int MAXIMO_ALERTA = 2000;

    /** Cuadre de una sección de producto del reporte (AH, HS, PE, PH, PQ, ...). */
    public static class SeccionCuadre implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String codigoProducto;
        private final String descripcion;
        /** Subtotal de la línea «TOTAL &lt;producto&gt;» del reporte (columna total descontado); {@code null} si el reporte no la trae. */
        private Double subtotalPetro;
        private double sumaCargada;
        private int lineas;

        SeccionCuadre(String codigoProducto, String descripcion) {
            this.codigoProducto = codigoProducto;
            this.descripcion = descripcion;
        }

        public String getCodigoProducto() { return codigoProducto; }
        public String getDescripcion() { return descripcion; }
        public Double getSubtotalPetro() { return subtotalPetro; }
        public double getSumaCargada() { return sumaCargada; }
        public int getLineas() { return lineas; }
        /** Subtotal de Petro menos lo cargado; {@code null} si Petro no imprimió el subtotal. */
        public Double getDiferencia() { return subtotalPetro == null ? null : Double.valueOf(subtotalPetro.doubleValue() - sumaCargada); }
        public boolean cuadra() { return subtotalPetro != null && Math.abs(subtotalPetro.doubleValue() - sumaCargada) <= TOLERANCIA; }

        void setSubtotalPetro(Double subtotalPetro) { this.subtotalPetro = subtotalPetro; }
        void suma(double valor) { this.sumaCargada += valor; this.lineas++; }
    }

    private List<ParticipeXCargaArchivo> registros = new ArrayList<ParticipeXCargaArchivo>();
    private final Map<String, SeccionCuadre> secciones = new LinkedHashMap<String, SeccionCuadre>();
    private Double totalGeneralPetro;
    private double sumaTotalCargada;
    private int lineasResumen;
    private int continuaciones;
    /** Alertas que no son una diferencia de sección ni del total: TOTAL EMPLEADO que no cuadra, TOTAL sin sección, falta de TOTAL, etc. */
    private final List<String> otrasAlertas = new ArrayList<String>();

    public List<ParticipeXCargaArchivo> getRegistros() { return registros; }
    public List<SeccionCuadre> getSecciones() { return new ArrayList<SeccionCuadre>(secciones.values()); }
    public Double getTotalGeneralPetro() { return totalGeneralPetro; }
    public double getSumaTotalCargada() { return sumaTotalCargada; }
    public int getLineasResumen() { return lineasResumen; }
    public int getContinuaciones() { return continuaciones; }
    public List<String> getOtrasAlertas() { return otrasAlertas; }
    public Double getDiferenciaTotal() { return totalGeneralPetro == null ? null : Double.valueOf(totalGeneralPetro.doubleValue() - sumaTotalCargada); }

    void setRegistros(List<ParticipeXCargaArchivo> registros) { this.registros = registros; }
    void setTotalGeneralPetro(Double totalGeneralPetro) { this.totalGeneralPetro = totalGeneralPetro; }
    void sumaTotal(double valor) { this.sumaTotalCargada += valor; }
    void cuentaResumen() { this.lineasResumen++; }
    void cuentaContinuacion() { this.continuaciones++; }

    SeccionCuadre seccion(String codigoProducto, String descripcion) {
        SeccionCuadre s = secciones.get(codigoProducto);
        if (s == null) {
            s = new SeccionCuadre(codigoProducto, descripcion);
            secciones.put(codigoProducto, s);
        }
        return s;
    }

    /** {@code true} si no hay ninguna diferencia ni alerta. */
    public boolean cuadra() {
        return construyeAlertas().isEmpty();
    }

    /**
     * Texto persistente de la alerta de cuadre (CRD.CRAR.CRARALCD) o {@code null} si todo cuadra. Máximo {@value #MAXIMO_ALERTA} caracteres.
     */
    public String getAlertaCuadre() {
        List<String> alertas = construyeAlertas();
        if (alertas.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Descuadre con el reporte de Petro: ");
        for (int i = 0; i < alertas.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(alertas.get(i));
        }
        String texto = sb.toString();
        return texto.length() <= MAXIMO_ALERTA ? texto : texto.substring(0, MAXIMO_ALERTA - 3) + "...";
    }

    private List<String> construyeAlertas() {
        List<String> alertas = new ArrayList<String>();
        for (SeccionCuadre s : secciones.values()) {
            String etiqueta = s.getCodigoProducto() + (s.getDescripcion() == null || s.getDescripcion().isEmpty() ? "" : " (" + s.getDescripcion() + ")");
            if (s.getSubtotalPetro() == null) {
                alertas.add(etiqueta + " sin línea TOTAL en el reporte, cargado " + formato(s.getSumaCargada()));
            } else if (!s.cuadra()) {
                alertas.add(etiqueta + " subtotal " + formato(s.getSubtotalPetro().doubleValue()) + ", cargado " + formato(s.getSumaCargada())
                    + ", diferencia " + formato(s.getDiferencia().doubleValue()));
            }
        }
        if (totalGeneralPetro == null) {
            alertas.add("el archivo no trae la línea TOTAL ==>, cargado " + formato(sumaTotalCargada));
        } else if (Math.abs(totalGeneralPetro.doubleValue() - sumaTotalCargada) > TOLERANCIA) {
            alertas.add("TOTAL ==> " + formato(totalGeneralPetro.doubleValue()) + ", cargado " + formato(sumaTotalCargada)
                + ", diferencia " + formato(totalGeneralPetro.doubleValue() - sumaTotalCargada));
        }
        alertas.addAll(otrasAlertas);
        return alertas;
    }

    /** Número como lo imprime Petro: punto de miles, coma decimal y dos decimales. */
    public static String formato(double valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.ROOT);
        simbolos.setGroupingSeparator('.');
        simbolos.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", simbolos).format(valor);
    }
}
