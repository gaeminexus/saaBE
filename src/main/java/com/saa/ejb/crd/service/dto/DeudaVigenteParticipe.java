package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Aviso de deuda vigente de un partícipe (GET /rest/dvap/deudaVigente/{idEntidad}, §6.5).
 *
 * <h3>Es un AVISO, no una validación</h3>
 * La pantalla lo llama al seleccionar al partícipe y lo muestra en el diálogo de
 * confirmación. <b>{@code POST /dvap/registrar} NO valida esto</b> y no tiene ningún código
 * de error asociado: si el operador confirma con deuda a la vista, la devolución se registra
 * igual. Decisión del usuario del 2026-08-24 (§10.2 del plan), que descartó explícitamente
 * tanto bloquear con un 422 como netear los préstamos contra los aportes.
 *
 * Un partícipe sin préstamos vigentes devuelve {@code totalDeuda = 0},
 * {@code cantidadPrestamos = 0} y {@code prestamos = []}. <b>Nunca un error.</b>
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class DeudaVigenteParticipe {

    /** ENTDCDGO del partícipe consultado. */
    private Long idEntidad;

    /** Suma de {@code saldoPendiente} de todos los préstamos vigentes. 0.0 si no hay. */
    private Double totalDeuda;

    /** Cantidad de préstamos vigentes. 0 si no hay. */
    private Integer cantidadPrestamos;

    /**
     * true si algún préstamo está DE_PLAZO_VENCIDO(8) o EN_MORA(11), o si alguno tiene al
     * menos una cuota vencida.
     */
    private Boolean tieneMora;

    /** Detalle por préstamo. Lista vacía si el partícipe no tiene préstamos vigentes. */
    private List<DeudaPrestamo> prestamos = new ArrayList<>();

    public DeudaVigenteParticipe() {
    }

    public Long getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public Double getTotalDeuda() {
        return totalDeuda;
    }

    public void setTotalDeuda(Double totalDeuda) {
        this.totalDeuda = totalDeuda;
    }

    public Integer getCantidadPrestamos() {
        return cantidadPrestamos;
    }

    public void setCantidadPrestamos(Integer cantidadPrestamos) {
        this.cantidadPrestamos = cantidadPrestamos;
    }

    public Boolean getTieneMora() {
        return tieneMora;
    }

    public void setTieneMora(Boolean tieneMora) {
        this.tieneMora = tieneMora;
    }

    public List<DeudaPrestamo> getPrestamos() {
        return prestamos;
    }

    public void setPrestamos(List<DeudaPrestamo> prestamos) {
        this.prestamos = prestamos;
    }
}
