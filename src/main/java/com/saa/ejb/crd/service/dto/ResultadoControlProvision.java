package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote 22 (niveles 2.3, 2.4 y 3.1): control 1.4.02 = 1.4.99.05 al corte, por tipo de préstamo, contra la contabilidad y contra el libro por cuota, con
 * el ajuste de último recurso que el cierre genera si hay diferencia. Va en la previsualización y en el resultado de la ejecución.
 */
public class ResultadoControlProvision implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Una fila por tipo de préstamo. Tolerancia 0,01 en cada diferencia. */
    public static class Tipo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private String cuenta1402;
        private String cuenta149905;
        /** Saldo contable de la 1.4.02 (deudor) y de la 1.4.99.05 (acreedor, en positivo) al corte. */
        private double contable1402;
        private double contable149905;
        /** Libro por cuota: devengo de interés Σ(11−12), de mora Σ(5−6) y saldo provisionado por componente. */
        private double libro1402Interes;
        private double libro1402Mora;
        private double libroProvInteres;
        private double libroProvMora;
        /** (a) contable 1.4.02 − contable 1.4.99.05 (la regla). */
        private double difContable;
        /** (b) contable 1.4.02 − libro del devengo, y contable 1.4.99.05 − libro de la provisión. */
        private double dif1402Libro;
        private double difProvLibro;
        /** Por componente, solo en el libro: libro 1.4.02 − libro provisión (interés y mora). */
        private double difLibroInteres;
        private double difLibroMora;
        private boolean cuadra;

        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public String getCuenta1402() { return cuenta1402; }
        public void setCuenta1402(String cuenta1402) { this.cuenta1402 = cuenta1402; }
        public String getCuenta149905() { return cuenta149905; }
        public void setCuenta149905(String cuenta149905) { this.cuenta149905 = cuenta149905; }
        public double getContable1402() { return contable1402; }
        public void setContable1402(double contable1402) { this.contable1402 = contable1402; }
        public double getContable149905() { return contable149905; }
        public void setContable149905(double contable149905) { this.contable149905 = contable149905; }
        public double getLibro1402Interes() { return libro1402Interes; }
        public void setLibro1402Interes(double libro1402Interes) { this.libro1402Interes = libro1402Interes; }
        public double getLibro1402Mora() { return libro1402Mora; }
        public void setLibro1402Mora(double libro1402Mora) { this.libro1402Mora = libro1402Mora; }
        public double getLibroProvInteres() { return libroProvInteres; }
        public void setLibroProvInteres(double libroProvInteres) { this.libroProvInteres = libroProvInteres; }
        public double getLibroProvMora() { return libroProvMora; }
        public void setLibroProvMora(double libroProvMora) { this.libroProvMora = libroProvMora; }
        public double getDifContable() { return difContable; }
        public void setDifContable(double difContable) { this.difContable = difContable; }
        public double getDif1402Libro() { return dif1402Libro; }
        public void setDif1402Libro(double dif1402Libro) { this.dif1402Libro = dif1402Libro; }
        public double getDifProvLibro() { return difProvLibro; }
        public void setDifProvLibro(double difProvLibro) { this.difProvLibro = difProvLibro; }
        public double getDifLibroInteres() { return difLibroInteres; }
        public void setDifLibroInteres(double difLibroInteres) { this.difLibroInteres = difLibroInteres; }
        public double getDifLibroMora() { return difLibroMora; }
        public void setDifLibroMora(double difLibroMora) { this.difLibroMora = difLibroMora; }
        public boolean isCuadra() { return cuadra; }
        public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    }

    /** Control 3.1 (informativo): después del ④ del día 1, 1.4.02 − 1.4.99.05 contra el asiento de devengo del día 1, por tipo. */
    public static class Dia1 implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private double diferenciaContable;
        private double devengoDia1;
        private double diferencia;

        public Long getIdTipoPrestamo() { return idTipoPrestamo; }
        public void setIdTipoPrestamo(Long idTipoPrestamo) { this.idTipoPrestamo = idTipoPrestamo; }
        public double getDiferenciaContable() { return diferenciaContable; }
        public void setDiferenciaContable(double diferenciaContable) { this.diferenciaContable = diferenciaContable; }
        public double getDevengoDia1() { return devengoDia1; }
        public void setDevengoDia1(double devengoDia1) { this.devengoDia1 = devengoDia1; }
        public double getDiferencia() { return diferencia; }
        public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
    }

    private String corte;
    /** {@code true} si el devengo completo está activo al corte (si no, el control no aplica y no se calcula). */
    private boolean aplica;
    /** {@code true} si es una proyección (previsualización): suma lo que el cierre va a asentar y todavía no está asentado. */
    private boolean proyeccion;
    private boolean cuadra = true;
    /** Hay un ajuste de último recurso vigente al corte (registrado). */
    private boolean ajusteRegistrado;
    private boolean ajusteAplicado;
    private Long idAsientoAjuste;
    private List<Tipo> porTipo = new ArrayList<Tipo>();
    private List<Dia1> dia1 = new ArrayList<Dia1>();
    /** Excepciones registradas: por tipo, las cuotas, los valores y la causa probable. */
    private List<String> excepciones = new ArrayList<String>();

    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public boolean isAplica() { return aplica; }
    public void setAplica(boolean aplica) { this.aplica = aplica; }
    public boolean isProyeccion() { return proyeccion; }
    public void setProyeccion(boolean proyeccion) { this.proyeccion = proyeccion; }
    public boolean isCuadra() { return cuadra; }
    public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    public boolean isAjusteRegistrado() { return ajusteRegistrado; }
    public void setAjusteRegistrado(boolean ajusteRegistrado) { this.ajusteRegistrado = ajusteRegistrado; }
    public boolean isAjusteAplicado() { return ajusteAplicado; }
    public void setAjusteAplicado(boolean ajusteAplicado) { this.ajusteAplicado = ajusteAplicado; }
    public Long getIdAsientoAjuste() { return idAsientoAjuste; }
    public void setIdAsientoAjuste(Long idAsientoAjuste) { this.idAsientoAjuste = idAsientoAjuste; }
    public List<Tipo> getPorTipo() { return porTipo; }
    public void setPorTipo(List<Tipo> porTipo) { this.porTipo = porTipo; }
    public List<Dia1> getDia1() { return dia1; }
    public void setDia1(List<Dia1> dia1) { this.dia1 = dia1; }
    public List<String> getExcepciones() { return excepciones; }
    public void setExcepciones(List<String> excepciones) { this.excepciones = excepciones; }
}
