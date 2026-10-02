package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code POST /posg/sumaAsegurada/carga} (contrato §2). */
public class ResultadoCargaSumaAsegurada {

    private int total;
    private int ok;
    private int conError;
    private List<FilaResultadoSumaAsegurada> filas = new ArrayList<>();

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getOk() { return ok; }
    public void setOk(int ok) { this.ok = ok; }

    public int getConError() { return conError; }
    public void setConError(int conError) { this.conError = conError; }

    public List<FilaResultadoSumaAsegurada> getFilas() { return filas; }
    public void setFilas(List<FilaResultadoSumaAsegurada> filas) { this.filas = filas; }
}
