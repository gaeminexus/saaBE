package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de un {@code PagoProgramado} en estado POR_APROBAR para la bandeja de
 * aprobación de pagos ({@code GET /pgtr/porAprobar}) — NO es la entidad completa. Ver
 * docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md y
 * docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md.</p>
 *
 * <p>Se arma en el <b>service</b>, no con {@code select new} en el DAO: qué campo de
 * {@code PagoProgramado} usar para {@code origen}/{@code beneficiario}/{@code concepto}
 * depende de cuál de {@code facturaCompra}/{@code egreso}/{@code anticipo}/
 * {@code origenExterno} está no-nulo — la misma excepción que documenta
 * {@code FacturaSustentoPendiente} para {@code sustentoSugerido}.</p>
 *
 * <p>{@code origen} toma uno de los valores de {@link com.saa.rubros.OrigenPagoCxp} (pago
 * propio de CXP) o de {@link com.saa.rubros.OrigenPagoExterno} (documento de otro módulo).</p>
 */
public class PagoPorAprobar implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String origen;
    private String beneficiario;
    private String concepto;
    private Double valor;
    private LocalDate fechaSolicitada;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public String getBeneficiario() { return beneficiario; }
    public void setBeneficiario(String beneficiario) { this.beneficiario = beneficiario; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public LocalDate getFechaSolicitada() { return fechaSolicitada; }
    public void setFechaSolicitada(LocalDate fechaSolicitada) { this.fechaSolicitada = fechaSolicitada; }
}
