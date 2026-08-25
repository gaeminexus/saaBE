package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un elemento del listado de devoluciones de un partícipe (GET /rest/dvap/porEntidad).
 *
 * Se arma en la capa REST a partir de la entidad {@code DevolucionAporte} y sus detalles,
 * DESPUÉS de que el reconciliador puso los estados al día. Existe para que la pantalla no
 * tenga que navegar el grafo de entidades JPA ni recibir campos que no le sirven.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResumenDevolucionAporte {

    private Long idDevolucion;

    /** Fecha de negocio. Viaja como {@code yyyy-MM-dd}. */
    private LocalDate fecha;

    private Double valorTotal;

    /** Ver {@link com.saa.rubros.EstadoDevolucionAporte}. */
    private Long estado;

    private String estadoTexto;

    /** Orden de pago generada en CXP. */
    private Long idPagoProgramado;

    /** Código del asiento contable, cuando la devolución ya está pagada. */
    private Long numeroAsiento;

    private LocalDate fechaPago;

    private String motivo;

    /** Cuenta destino ya formateada y enmascarada: "PICHINCHA · AHORROS · 2200****91". */
    private String cuentaDestino;

    private List<DetalleResumenDevolucion> detalle = new ArrayList<>();

    public ResumenDevolucionAporte() {
    }

    public Long getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(Long idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Double getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(Double valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Long getEstado() {
        return estado;
    }

    public void setEstado(Long estado) {
        this.estado = estado;
    }

    public String getEstadoTexto() {
        return estadoTexto;
    }

    public void setEstadoTexto(String estadoTexto) {
        this.estadoTexto = estadoTexto;
    }

    public Long getIdPagoProgramado() {
        return idPagoProgramado;
    }

    public void setIdPagoProgramado(Long idPagoProgramado) {
        this.idPagoProgramado = idPagoProgramado;
    }

    public Long getNumeroAsiento() {
        return numeroAsiento;
    }

    public void setNumeroAsiento(Long numeroAsiento) {
        this.numeroAsiento = numeroAsiento;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public List<DetalleResumenDevolucion> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<DetalleResumenDevolucion> detalle) {
        this.detalle = detalle;
    }
}
