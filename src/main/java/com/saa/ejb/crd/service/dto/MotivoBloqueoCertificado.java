package com.saa.ejb.crd.service.dto;

/**
 * Un impedimento para emitir un certificado, con su dato concreto (qué préstamo, en qué
 * estado). Ver §5 de API-CERTIFICADOS-PARTICIPE.md. Los códigos son estables.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class MotivoBloqueoCertificado {

    public static final String PRESTAMO_EN_MORA = "PRESTAMO_EN_MORA";
    public static final String CUOTA_VENCIDA = "CUOTA_VENCIDA";
    public static final String PARTICIPE_EN_MORA = "PARTICIPE_EN_MORA";
    public static final String PRESTAMO_NO_CANCELADO = "PRESTAMO_NO_CANCELADO";
    public static final String PRESTAMO_POR_REVISAR = "PRESTAMO_POR_REVISAR";
    public static final String PRESTAMO_NO_PERTENECE = "PRESTAMO_NO_PERTENECE";
    public static final String RECIBE_PENSION = "RECIBE_PENSION";

    private String codigo;
    /** Accionable, listo para mostrar. */
    private String mensaje;
    /** Null cuando el motivo no aplica a un préstamo. */
    private Long idPrestamo;
    private Long numeroCredito;
    private String producto;
    private Long estado;
    private String estadoTexto;

    public MotivoBloqueoCertificado() {
    }

    public MotivoBloqueoCertificado(String codigo, String mensaje) {
        this.codigo = codigo;
        this.mensaje = mensaje;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }

    public Long getNumeroCredito() { return numeroCredito; }
    public void setNumeroCredito(Long numeroCredito) { this.numeroCredito = numeroCredito; }

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public String getEstadoTexto() { return estadoTexto; }
    public void setEstadoTexto(String estadoTexto) { this.estadoTexto = estadoTexto; }
}
