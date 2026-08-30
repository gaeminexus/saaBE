package com.saa.model.cnt;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de un asiento contable para listados — código, número, fecha, glosa, estado y
 * los totales de debe/haber. NO es la entidad {@link Asiento} completa: evita arrastrar
 * {@code empresa} (con su jerarquía), {@code tipoAsiento}, {@code mayorizacion} y
 * {@code periodo} (con su propia {@code empresa} anidada otra vez).</p>
 *
 * <p>Reemplaza el fallback de <code>GET /asnt/getAll</code> que usaba
 * <code>listado-asientos.component.ts</code> cuando el filtro por criterios fallaba: bajar los
 * 1.784 asientos completos de <b>todas las empresas</b> (~4 MB) para filtrar en el cliente. Ver
 * {@code AsientoDaoService.selectResumenPorEmpresaPeriodo} y
 * docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md.</p>
 */
public class AsientoResumen implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codigo;
    private String numero;
    private LocalDate fecha;
    private String glosa;
    private Long estado;
    private Double totalDebe;
    private Double totalHaber;

    public AsientoResumen() {
    }

    /**
     * Constructor usado por la proyección JPQL ({@code SELECT NEW ...}) — el orden y los tipos
     * deben coincidir exactamente con la consulta en {@code AsientoDaoServiceImpl}.
     */
    public AsientoResumen(Long codigo, String numero, LocalDate fecha, String glosa, Long estado,
            Double totalDebe, Double totalHaber) {
        this.codigo = codigo;
        this.numero = numero;
        this.fecha = fecha;
        this.glosa = glosa;
        this.estado = estado;
        this.totalDebe = totalDebe;
        this.totalHaber = totalHaber;
    }

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getGlosa() { return glosa; }
    public void setGlosa(String glosa) { this.glosa = glosa; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public Double getTotalDebe() { return totalDebe; }
    public void setTotalDebe(Double totalDebe) { this.totalDebe = totalDebe; }

    public Double getTotalHaber() { return totalHaber; }
    public void setTotalHaber(Double totalHaber) { this.totalHaber = totalHaber; }
}
