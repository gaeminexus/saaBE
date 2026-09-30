package com.saa.ejb.crd.service.dto;

import java.util.List;

/** Cuerpo de {@code POST /rest/plvn/documentos} — descarga masiva en un ZIP (§8bis del contrato). */
public class SolicitudDocumentosPlazoVencido {

    private List<Long> ids;
    private String formato;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }

    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
}
