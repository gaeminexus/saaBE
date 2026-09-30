package com.saa.ejb.cxp.service.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta de {@code GET /aplp/cartera} y {@code GET /aplc/cartera}: todos los documentos
 * pendientes de pago/cobro a una fecha de corte, con saldo y antigüedad
 * (API-CARTERA-CXP-CXC.md §4). Un solo DTO para los dos lados -- {@link #tipo} distingue
 * {@code POR_PAGAR} de {@code POR_COBRAR}, y los campos que solo aplican a un lado viajan null
 * en el otro (ver {@link DocumentoCartera}).
 *
 * <p>Solo resta lo que ya está APLICADO (§6.1): una retención o nota de crédito registrada pero
 * no aplicada no reduce el saldo acá, igual que en {@code /aplp/saldo}/{@code /aplc/saldo}.</p>
 *
 * POJO plano: getters y setters escritos a mano, sin anotaciones de Jackson. {@link #fechaCorte}
 * viaja como arreglo {@code [aaaa,mm,dd]} (Jackson serializa {@code LocalDate} así).
 */
public class ReporteCartera implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipo;
    private LocalDate fechaCorte;
    private TotalesCartera totales;
    private List<ResumenTitularCartera> resumen;
    private List<DocumentoCartera> documentos;
    private List<String> avisos;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public TotalesCartera getTotales() { return totales; }
    public void setTotales(TotalesCartera totales) { this.totales = totales; }

    public List<ResumenTitularCartera> getResumen() { return resumen; }
    public void setResumen(List<ResumenTitularCartera> resumen) { this.resumen = resumen; }

    public List<DocumentoCartera> getDocumentos() { return documentos; }
    public void setDocumentos(List<DocumentoCartera> documentos) { this.documentos = documentos; }

    public List<String> getAvisos() { return avisos; }
    public void setAvisos(List<String> avisos) { this.avisos = avisos; }
}
