package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code GET /posg/{idFactura}/novedades?desde=&hasta=} (contrato §7). */
public class NovedadesSeguro {

    private List<CandidatoSeguro> inclusiones = new ArrayList<>();
    private List<ExclusionSeguro> exclusiones = new ArrayList<>();

    public List<CandidatoSeguro> getInclusiones() { return inclusiones; }
    public void setInclusiones(List<CandidatoSeguro> inclusiones) { this.inclusiones = inclusiones; }

    public List<ExclusionSeguro> getExclusiones() { return exclusiones; }
    public void setExclusiones(List<ExclusionSeguro> exclusiones) { this.exclusiones = exclusiones; }
}
