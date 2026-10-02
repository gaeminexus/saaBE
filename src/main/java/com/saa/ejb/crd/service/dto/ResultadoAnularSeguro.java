package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/** Resultado de {@code POST /posg/{id}/anular} (contrato §6). */
public class ResultadoAnularSeguro {

    private DocumentoSeguroDTO documento;
    private List<CuotaNoRevertida> cuotasNoRevertidas = new ArrayList<>();

    public DocumentoSeguroDTO getDocumento() { return documento; }
    public void setDocumento(DocumentoSeguroDTO documento) { this.documento = documento; }

    public List<CuotaNoRevertida> getCuotasNoRevertidas() { return cuotasNoRevertidas; }
    public void setCuotasNoRevertidas(List<CuotaNoRevertida> cuotasNoRevertidas) { this.cuotasNoRevertidas = cuotasNoRevertidas; }
}
