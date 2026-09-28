package com.saa.ejb.sri.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Detalle del ATS de un período, para la pantalla de comparación (API-DETALLE-ATS.md §4.2).
 * Corre exactamente la misma lógica que {@code GeneradorAtsServiceImpl.generarAts} (el XML se
 * genera y se descarta) -- así {@link #avisos} y los enlaces de retención de esta pantalla son
 * siempre los mismos que los del ZIP que se declara al SRI.
 *
 * <p>El detalle y el ATS no corren en la misma transacción: si alguien registra un documento
 * entre una consulta y la otra, pueden diferir. No es un defecto (§6 del contrato).</p>
 */
public class DetalleAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombreArchivo;
    private int anio;
    private int mes;
    private List<CompraAts> compras;
    private List<VentaAts> ventas;
    private List<AnuladoAts> anulados;
    private List<CompraExcluidaAts> excluidas;
    private List<RetencionNoEnlazada> retencionesNoEnlazadas;
    private TotalesCompras totalesCompras;
    private TotalesVentas totalesVentas;
    private double totalVentasDeclarado;
    private List<String> avisos;

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }

    public List<CompraAts> getCompras() { return compras; }
    public void setCompras(List<CompraAts> compras) { this.compras = compras; }

    public List<VentaAts> getVentas() { return ventas; }
    public void setVentas(List<VentaAts> ventas) { this.ventas = ventas; }

    public List<AnuladoAts> getAnulados() { return anulados; }
    public void setAnulados(List<AnuladoAts> anulados) { this.anulados = anulados; }

    public List<CompraExcluidaAts> getExcluidas() { return excluidas; }
    public void setExcluidas(List<CompraExcluidaAts> excluidas) { this.excluidas = excluidas; }

    public List<RetencionNoEnlazada> getRetencionesNoEnlazadas() { return retencionesNoEnlazadas; }
    public void setRetencionesNoEnlazadas(List<RetencionNoEnlazada> retencionesNoEnlazadas) { this.retencionesNoEnlazadas = retencionesNoEnlazadas; }

    public TotalesCompras getTotalesCompras() { return totalesCompras; }
    public void setTotalesCompras(TotalesCompras totalesCompras) { this.totalesCompras = totalesCompras; }

    public TotalesVentas getTotalesVentas() { return totalesVentas; }
    public void setTotalesVentas(TotalesVentas totalesVentas) { this.totalesVentas = totalesVentas; }

    public double getTotalVentasDeclarado() { return totalVentasDeclarado; }
    public void setTotalVentasDeclarado(double totalVentasDeclarado) { this.totalVentasDeclarado = totalVentasDeclarado; }

    public List<String> getAvisos() { return avisos; }
    public void setAvisos(List<String> avisos) { this.avisos = avisos; }

    /** Suma de las líneas de {@link #compras}, ya redondeadas (API-DETALLE-ATS.md §4.2). */
    public static class TotalesCompras implements Serializable {

        private static final long serialVersionUID = 1L;

        private int cantidad;
        private double baseNoGraIva;
        private double baseImponible;
        private double baseImpGrav;
        private double baseImpExe;
        private double montoIce;
        private double montoIva;
        private double total;
        private double retencionIva;
        private double retencionRenta;

        public int getCantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public double getBaseNoGraIva() { return baseNoGraIva; }
        public void setBaseNoGraIva(double baseNoGraIva) { this.baseNoGraIva = baseNoGraIva; }

        public double getBaseImponible() { return baseImponible; }
        public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }

        public double getBaseImpGrav() { return baseImpGrav; }
        public void setBaseImpGrav(double baseImpGrav) { this.baseImpGrav = baseImpGrav; }

        public double getBaseImpExe() { return baseImpExe; }
        public void setBaseImpExe(double baseImpExe) { this.baseImpExe = baseImpExe; }

        public double getMontoIce() { return montoIce; }
        public void setMontoIce(double montoIce) { this.montoIce = montoIce; }

        public double getMontoIva() { return montoIva; }
        public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

        public double getTotal() { return total; }
        public void setTotal(double total) { this.total = total; }

        public double getRetencionIva() { return retencionIva; }
        public void setRetencionIva(double retencionIva) { this.retencionIva = retencionIva; }

        public double getRetencionRenta() { return retencionRenta; }
        public void setRetencionRenta(double retencionRenta) { this.retencionRenta = retencionRenta; }
    }

    /** Suma de las líneas de {@link #ventas}, ya redondeadas (API-DETALLE-ATS.md §4.2). */
    public static class TotalesVentas implements Serializable {

        private static final long serialVersionUID = 1L;

        private int cantidadLineas;
        private int numeroComprobantes;
        private double baseImponible;
        private double baseImpGrav;
        private double montoIva;
        private double montoIce;
        private double valorRetIva;
        private double valorRetRenta;

        public int getCantidadLineas() { return cantidadLineas; }
        public void setCantidadLineas(int cantidadLineas) { this.cantidadLineas = cantidadLineas; }

        public int getNumeroComprobantes() { return numeroComprobantes; }
        public void setNumeroComprobantes(int numeroComprobantes) { this.numeroComprobantes = numeroComprobantes; }

        public double getBaseImponible() { return baseImponible; }
        public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }

        public double getBaseImpGrav() { return baseImpGrav; }
        public void setBaseImpGrav(double baseImpGrav) { this.baseImpGrav = baseImpGrav; }

        public double getMontoIva() { return montoIva; }
        public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

        public double getMontoIce() { return montoIce; }
        public void setMontoIce(double montoIce) { this.montoIce = montoIce; }

        public double getValorRetIva() { return valorRetIva; }
        public void setValorRetIva(double valorRetIva) { this.valorRetIva = valorRetIva; }

        public double getValorRetRenta() { return valorRetRenta; }
        public void setValorRetRenta(double valorRetRenta) { this.valorRetRenta = valorRetRenta; }
    }

    /**
     * Retención de compra que no quedó enlazada a ninguna compra ni a una factura de
     * intermediario excluida -- mismo recorrido que produce el aviso de texto equivalente en
     * {@code GeneradorAtsServiceImpl.generarXml} (ÍTEM 11c). No tiene archivo propio: es una
     * estructura de tres campos, igual que el ejemplo del contrato (§4.2).
     */
    public static class RetencionNoEnlazada implements Serializable {

        private static final long serialVersionUID = 1L;

        private String numeroRetencion;
        private String documentoSustento;
        private String autorizacion;

        public String getNumeroRetencion() { return numeroRetencion; }
        public void setNumeroRetencion(String numeroRetencion) { this.numeroRetencion = numeroRetencion; }

        public String getDocumentoSustento() { return documentoSustento; }
        public void setDocumentoSustento(String documentoSustento) { this.documentoSustento = documentoSustento; }

        public String getAutorizacion() { return autorizacion; }
        public void setAutorizacion(String autorizacion) { this.autorizacion = autorizacion; }
    }
}
