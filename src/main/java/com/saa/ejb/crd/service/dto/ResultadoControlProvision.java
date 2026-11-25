package com.saa.ejb.crd.service.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lote 22 (niveles 2.3, 2.4 y 3.1) y lote 24: control 1.4.02 = 1.4.99.05 al corte. El saldo contable se compara por CUENTA contable (una cuenta puede
 * servir a varios tipos de préstamo: en ASOPREP la provisión es una sola cuenta para Q, P y H), contra la suma del libro por cuota de los tipos que la usan;
 * la regla del contador se aplica por grupo de cuentas y al total; el detalle por tipo de préstamo es solo del libro (devengo − provisión por componente).
 * Con el ajuste de último recurso que el cierre genera si hay diferencia. Va en la previsualización y en el resultado de la ejecución.
 */
public class ResultadoControlProvision implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Una fila por tipo de préstamo: SOLO el libro por cuota (devengo y provisión por componente). Tolerancia 0,01. */
    public static class Tipo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long idTipoPrestamo;
        private String cuenta1402;
        private String cuenta149905;
        /** Libro por cuota: devengo de interés Σ(11−12), de mora Σ(5−6) y saldo provisionado por componente. */
        private double libro1402Interes;
        private double libro1402Mora;
        private double libroProvInteres;
        private double libroProvMora;
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
        public double getLibro1402Interes() { return libro1402Interes; }
        public void setLibro1402Interes(double libro1402Interes) { this.libro1402Interes = libro1402Interes; }
        public double getLibro1402Mora() { return libro1402Mora; }
        public void setLibro1402Mora(double libro1402Mora) { this.libro1402Mora = libro1402Mora; }
        public double getLibroProvInteres() { return libroProvInteres; }
        public void setLibroProvInteres(double libroProvInteres) { this.libroProvInteres = libroProvInteres; }
        public double getLibroProvMora() { return libroProvMora; }
        public void setLibroProvMora(double libroProvMora) { this.libroProvMora = libroProvMora; }
        public double getDifLibroInteres() { return difLibroInteres; }
        public void setDifLibroInteres(double difLibroInteres) { this.difLibroInteres = difLibroInteres; }
        public double getDifLibroMora() { return difLibroMora; }
        public void setDifLibroMora(double difLibroMora) { this.difLibroMora = difLibroMora; }
        public boolean isCuadra() { return cuadra; }
        public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    }

    /** Una cuenta contable (de la 1.4.02 o de la 1.4.99.05): su saldo contra la suma del libro de los tipos de préstamo que la usan. */
    public static class Cuenta implements Serializable {
        private static final long serialVersionUID = 1L;
        private String cuenta;
        /** {@code 1.4.02} (devengo, saldo deudor) o {@code 1.4.99.05} (provisión, saldo acreedor en positivo): la cuenta contable es la de la plantilla. */
        private String rol;
        private List<Long> tipos = new ArrayList<Long>();
        private double contable;
        private double libro;
        /** contable − libro. */
        private double diferencia;
        private boolean cuadra;

        public String getCuenta() { return cuenta; }
        public void setCuenta(String cuenta) { this.cuenta = cuenta; }
        public String getRol() { return rol; }
        public void setRol(String rol) { this.rol = rol; }
        public List<Long> getTipos() { return tipos; }
        public void setTipos(List<Long> tipos) { this.tipos = tipos; }
        public double getContable() { return contable; }
        public void setContable(double contable) { this.contable = contable; }
        public double getLibro() { return libro; }
        public void setLibro(double libro) { this.libro = libro; }
        public double getDiferencia() { return diferencia; }
        public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
        public boolean isCuadra() { return cuadra; }
        public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    }

    /** Un grupo de cuentas: los tipos de préstamo que comparten alguna cuenta (de la 1.4.02 o de la 1.4.99.05). La regla: Σ 1.4.02 = Σ 1.4.99.05 del grupo. */
    public static class Grupo implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Long> tipos = new ArrayList<Long>();
        private List<String> cuentas1402 = new ArrayList<String>();
        private List<String> cuentas149905 = new ArrayList<String>();
        private double contable1402;
        private double contable149905;
        /** (a) contable 1.4.02 − contable 1.4.99.05 del grupo. */
        private double diferencia;
        private boolean cuadra;

        public List<Long> getTipos() { return tipos; }
        public void setTipos(List<Long> tipos) { this.tipos = tipos; }
        public List<String> getCuentas1402() { return cuentas1402; }
        public void setCuentas1402(List<String> cuentas1402) { this.cuentas1402 = cuentas1402; }
        public List<String> getCuentas149905() { return cuentas149905; }
        public void setCuentas149905(List<String> cuentas149905) { this.cuentas149905 = cuentas149905; }
        public double getContable1402() { return contable1402; }
        public void setContable1402(double contable1402) { this.contable1402 = contable1402; }
        public double getContable149905() { return contable149905; }
        public void setContable149905(double contable149905) { this.contable149905 = contable149905; }
        public double getDiferencia() { return diferencia; }
        public void setDiferencia(double diferencia) { this.diferencia = diferencia; }
        public boolean isCuadra() { return cuadra; }
        public void setCuadra(boolean cuadra) { this.cuadra = cuadra; }
    }

    /** Control 3.1 (informativo): después del ④ del día 1, 1.4.02 − 1.4.99.05 contra el asiento de devengo del día 1, por grupo de cuentas. */
    public static class Dia1 implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Long> tipos = new ArrayList<Long>();
        private double diferenciaContable;
        private double devengoDia1;
        private double diferencia;

        public List<Long> getTipos() { return tipos; }
        public void setTipos(List<Long> tipos) { this.tipos = tipos; }
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
    private List<Cuenta> porCuenta = new ArrayList<Cuenta>();
    private List<Grupo> porGrupo = new ArrayList<Grupo>();
    /** Regla del contador al total: Σ 1.4.02 = Σ 1.4.99.05 de todas las cuentas. */
    private double totalContable1402;
    private double totalContable149905;
    private double difTotal;
    private List<Dia1> dia1 = new ArrayList<Dia1>();
    /** Excepciones registradas: por tipo o por cuenta, los valores y la causa probable. */
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
    public List<Cuenta> getPorCuenta() { return porCuenta; }
    public void setPorCuenta(List<Cuenta> porCuenta) { this.porCuenta = porCuenta; }
    public List<Grupo> getPorGrupo() { return porGrupo; }
    public void setPorGrupo(List<Grupo> porGrupo) { this.porGrupo = porGrupo; }
    public double getTotalContable1402() { return totalContable1402; }
    public void setTotalContable1402(double totalContable1402) { this.totalContable1402 = totalContable1402; }
    public double getTotalContable149905() { return totalContable149905; }
    public void setTotalContable149905(double totalContable149905) { this.totalContable149905 = totalContable149905; }
    public double getDifTotal() { return difTotal; }
    public void setDifTotal(double difTotal) { this.difTotal = difTotal; }
    public List<Dia1> getDia1() { return dia1; }
    public void setDia1(List<Dia1> dia1) { this.dia1 = dia1; }
    public List<String> getExcepciones() { return excepciones; }
    public void setExcepciones(List<String> excepciones) { this.excepciones = excepciones; }
}
