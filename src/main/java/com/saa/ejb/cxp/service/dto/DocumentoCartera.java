package com.saa.ejb.cxp.service.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Un documento pendiente de la cartera por pagar o por cobrar (API-CARTERA-CXP-CXC.md §4). Un
 * solo DTO para los dos lados: {@code cajaChica} viaja null en CxC (no existe ese tipo de
 * aplicación ahí) e {@code intermediario} viaja null en CxC y en las liquidaciones de CxP (esa
 * marca solo existe en {@code FCTC}).
 *
 * <p>⚠️ {@code idDocumento} NO es clave por sí solo: {@code FCTC} y {@code LQCC} usan IDENTITY
 * con numeraciones independientes, así que la clave real es {@code tipoDocumento} +
 * {@code idDocumento} (§6.6).</p>
 *
 * POJO plano: getters y setters escritos a mano, sin anotaciones de Jackson.
 */
public class DocumentoCartera implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tipoDocumento;
    private Long idDocumento;
    private String numeroDocumento;
    private LocalDate fechaEmision;
    private Long plazoDias;
    private LocalDate fechaVencimiento;
    private long diasVencido;
    private String tramo;
    private Long idTitular;
    private String identificacion;
    private String titular;
    private double total;
    private double pagado;
    private double notasCredito;
    private double retenciones;
    private double anticipos;
    private double notasDebito;
    private Double cajaChica;
    private double aplicado;
    private double saldo;
    private boolean sobrepagado;
    private Boolean intermediario;

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public Long getPlazoDias() { return plazoDias; }
    public void setPlazoDias(Long plazoDias) { this.plazoDias = plazoDias; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public long getDiasVencido() { return diasVencido; }
    public void setDiasVencido(long diasVencido) { this.diasVencido = diasVencido; }

    public String getTramo() { return tramo; }
    public void setTramo(String tramo) { this.tramo = tramo; }

    public Long getIdTitular() { return idTitular; }
    public void setIdTitular(Long idTitular) { this.idTitular = idTitular; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public String getTitular() { return titular; }
    public void setTitular(String titular) { this.titular = titular; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getPagado() { return pagado; }
    public void setPagado(double pagado) { this.pagado = pagado; }

    public double getNotasCredito() { return notasCredito; }
    public void setNotasCredito(double notasCredito) { this.notasCredito = notasCredito; }

    public double getRetenciones() { return retenciones; }
    public void setRetenciones(double retenciones) { this.retenciones = retenciones; }

    public double getAnticipos() { return anticipos; }
    public void setAnticipos(double anticipos) { this.anticipos = anticipos; }

    public double getNotasDebito() { return notasDebito; }
    public void setNotasDebito(double notasDebito) { this.notasDebito = notasDebito; }

    public Double getCajaChica() { return cajaChica; }
    public void setCajaChica(Double cajaChica) { this.cajaChica = cajaChica; }

    public double getAplicado() { return aplicado; }
    public void setAplicado(double aplicado) { this.aplicado = aplicado; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public boolean isSobrepagado() { return sobrepagado; }
    public void setSobrepagado(boolean sobrepagado) { this.sobrepagado = sobrepagado; }

    public Boolean getIntermediario() { return intermediario; }
    public void setIntermediario(Boolean intermediario) { this.intermediario = intermediario; }
}
