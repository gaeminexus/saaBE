package com.saa.ejb.sri.service.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Resultado de generar el ATS de un período (Fase 4, 2026-08-28). El ZIP viaja en
 * {@code contenidoBase64} porque el resto de descargas de archivo del sistema
 * (ej. {@code PagoProgramadoService.obtenerArchivoLote}) ya responden como JSON con el
 * contenido adentro, no como octet-stream — mismo criterio aquí para no introducir un
 * segundo patrón de descarga.
 *
 * <p><b>{@link #avisos} no es opcional de leer.</b> Varios campos del ATS dependen de datos
 * que hoy pueden venir nulos (parteRel/tipoProv del titular, fechaRegistro del documento) o de
 * catálogos del SRI que este sistema todavía no tiene verificados (Tablas 13/20/21 — ver
 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §3.6 y §10). El generador nunca inventa
 * un código: cuando no puede resolver un campo, lo deja vacío/en su valor más conservador y
 * agrega una línea aquí. Un ZIP con avisos igual se genera —para que el usuario pueda revisar
 * la estructura— pero no debería enviarse al SRI sin resolver esos avisos primero.
 */
public class ResultadoGeneracionAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombreArchivo;
    private String contenidoBase64;
    private long tamanoBytes;
    private int totalCompras;
    private int totalVentas;
    private int totalAnulados;
    private double totalVentasDeclarado;
    private List<String> avisos;

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getContenidoBase64() { return contenidoBase64; }
    public void setContenidoBase64(String contenidoBase64) { this.contenidoBase64 = contenidoBase64; }

    public long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public int getTotalCompras() { return totalCompras; }
    public void setTotalCompras(int totalCompras) { this.totalCompras = totalCompras; }

    public int getTotalVentas() { return totalVentas; }
    public void setTotalVentas(int totalVentas) { this.totalVentas = totalVentas; }

    public int getTotalAnulados() { return totalAnulados; }
    public void setTotalAnulados(int totalAnulados) { this.totalAnulados = totalAnulados; }

    public double getTotalVentasDeclarado() { return totalVentasDeclarado; }
    public void setTotalVentasDeclarado(double totalVentasDeclarado) { this.totalVentasDeclarado = totalVentasDeclarado; }

    public List<String> getAvisos() { return avisos; }
    public void setAvisos(List<String> avisos) { this.avisos = avisos; }
}
