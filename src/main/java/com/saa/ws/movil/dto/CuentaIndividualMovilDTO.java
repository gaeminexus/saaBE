package com.saa.ws.movil.dto;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.service.dto.SaldoTipoAporte;

/**
 * Composición de {@code GET /movil/cuenta-individual/{idEntidad}} (§5.2 del contrato): saldos de
 * aportes + préstamos con saldo (no terminales) + kardex CXC del partícipe. Cero lógica de
 * negocio nueva — cada lista sale de un {@code *Service}/{@code *DaoService} existente.
 */
public class CuentaIndividualMovilDTO {

    private Long idEntidad;

    /** {@link SaldoTipoAporte} no tiene fechas ni referencias a entidad: se reutiliza tal cual. */
    private List<SaldoTipoAporte> saldosAportes = new ArrayList<>();

    private List<PrestamoMovilDTO> prestamosConSaldo = new ArrayList<>();
    private List<CxcKardexMovilDTO> kardexCxc = new ArrayList<>();

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public List<SaldoTipoAporte> getSaldosAportes() {
        return saldosAportes;
    }

    public void setSaldosAportes(List<SaldoTipoAporte> saldosAportes) {
        this.saldosAportes = saldosAportes;
    }

    public List<PrestamoMovilDTO> getPrestamosConSaldo() {
        return prestamosConSaldo;
    }

    public void setPrestamosConSaldo(List<PrestamoMovilDTO> prestamosConSaldo) {
        this.prestamosConSaldo = prestamosConSaldo;
    }

    public List<CxcKardexMovilDTO> getKardexCxc() {
        return kardexCxc;
    }

    public void setKardexCxc(List<CxcKardexMovilDTO> kardexCxc) {
        this.kardexCxc = kardexCxc;
    }
}
