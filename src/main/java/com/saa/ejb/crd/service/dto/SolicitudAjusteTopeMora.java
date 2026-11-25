package com.saa.ejb.crd.service.dto;

import java.io.Serializable;

/**
 * Lote 7 (D-186): cuerpo de {@code POST /rest/prov/ajusteTopeMora}. Contrato:
 * docs/logica-negocio/crd/API-AJUSTE-TOPE-PROVISION-MORA.md.
 */
public class SolicitudAjusteTopeMora implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idEmpresa;
    /** Fecha de corte, {@code yyyy-MM-dd} (fin de mes). Tiene que ser la fecha del asiento. */
    private String corte;
    /** Asiento que el cuadre contabilizó por el exceso (D 1.4.99.05 / H 4.7.05.10). */
    private Long idAsiento;
    private String usuario;
    /** {@code true} graba el ajuste; {@code false} (o nulo) solo previsualiza. */
    private Boolean grabar;
    /** {@code true} devuelve el detalle por cuota (máximo 500 filas, por ajuste descendente). */
    private Boolean incluirDetalle;

    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getCorte() { return corte; }
    public void setCorte(String corte) { this.corte = corte; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public Boolean getGrabar() { return grabar; }
    public void setGrabar(Boolean grabar) { this.grabar = grabar; }
    public Boolean getIncluirDetalle() { return incluirDetalle; }
    public void setIncluirDetalle(Boolean incluirDetalle) { this.incluirDetalle = incluirDetalle; }
}
