package com.saa.ejb.crd.service.dto;

/**
 * Un préstamo vigente del partícipe, dentro del aviso de deuda que se muestra antes de
 * devolverle sus aportes (GET /rest/dvap/deudaVigente/{idEntidad}, §6.5 del plan).
 *
 * <b>Es informativo.</b> Nada de lo que hay acá bloquea el registro de la devolución.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class DeudaPrestamo {

    /** PRSTCDGO del préstamo. */
    private Long idPrestamo;

    /** PRSTIDAS: el número con el que el préstamo viaja en los archivos Petro. */
    private Long idAsoprep;

    /** Nombre del producto de crédito (HIPOTECARIO, QUIROGRAFARIO, ...). */
    private String producto;

    /**
     * Estado operativo del préstamo: PRSTIDST. Ver {@link com.saa.rubros.EstadoPrestamo}.
     * NO es ESPSCDGO, que es la FK al catálogo CRD.ESPS.
     */
    private Long idEstado;

    /** Nombre legible del estado, para la pantalla. */
    private String estadoTexto;

    /**
     * Deuda pendiente del préstamo, calculada por
     * {@code MotorPagoPrestamoService.calcularTotalPendientePrestamo}, que reconstruye los
     * saldos desde los PagoPrestamo vigentes. No es una suma de columnas de DTPR.
     */
    private Double saldoPendiente;

    /**
     * Cuotas vencidas e impagas a la fecha: mismo criterio que el proceso diario de mora y
     * el padrón, para que los números coincidan entre pantallas.
     */
    private Integer cuotasVencidas;

    public DeudaPrestamo() {
    }

    public Long getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Long idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Long getIdAsoprep() {
        return idAsoprep;
    }

    public void setIdAsoprep(Long idAsoprep) {
        this.idAsoprep = idAsoprep;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }

    public Long getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Long idEstado) {
        this.idEstado = idEstado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public Double getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(Double saldoPendiente) {
        this.saldoPendiente = saldoPendiente;
    }

    public Integer getCuotasVencidas() {
        return cuotasVencidas;
    }

    public void setCuotasVencidas(Integer cuotasVencidas) {
        this.cuotasVencidas = cuotasVencidas;
    }
}
