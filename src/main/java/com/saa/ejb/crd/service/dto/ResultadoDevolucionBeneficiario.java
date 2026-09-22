package com.saa.ejb.crd.service.dto;

/**
 * Una de las devoluciones creadas por {@code registrarParaBeneficiarios}: la de UN
 * beneficiario. Ver docs/logica-negocio/crd/API-DEVOLUCION-APORTES-A-BENEFICIARIOS.md §4.
 *
 * POJO plano: getters y setters escritos a mano, sin Lombok.
 */
public class ResultadoDevolucionBeneficiario {

    /** DVAPCDGO de la devolución de este beneficiario. */
    private Long idDevolucion;

    /** CBBPCDGO del beneficiario. */
    private Long idBeneficiario;

    private String nombre;

    private String identificacion;

    /** Lo que le corresponde a este beneficiario, ya repartido por tipo (§5.1). */
    private Double valor;

    /** Orden de pago generada en CXP para este beneficiario (PGS.PGTR.PGTRCDGO). */
    private Long idPago;

    public ResultadoDevolucionBeneficiario() {
    }

    public Long getIdDevolucion() {
        return idDevolucion;
    }

    public void setIdDevolucion(Long idDevolucion) {
        this.idDevolucion = idDevolucion;
    }

    public Long getIdBeneficiario() {
        return idBeneficiario;
    }

    public void setIdBeneficiario(Long idBeneficiario) {
        this.idBeneficiario = idBeneficiario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIdentificacion() {
        return identificacion;
    }

    public void setIdentificacion(String identificacion) {
        this.identificacion = identificacion;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getIdPago() {
        return idPago;
    }

    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }
}
