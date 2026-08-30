package com.saa.model.tsr;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de una fila pendiente de TSR.DEXB para la preparación del cierre de partidas en
 * tránsito - NO es la entidad completa (evita arrastrar cuentaBancaria→banco→empresa). Ver
 * ConciliacionCierreService.prepararCierre.</p>
 */
public class PendienteExtractoTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idDetalleExtracto;
    private LocalDate fecha;
    private String descripcion;
    private Double valor;
    private boolean esArrastrada;
    private Integer tipoSugerido;

    public Long getIdDetalleExtracto() { return idDetalleExtracto; }
    public void setIdDetalleExtracto(Long idDetalleExtracto) { this.idDetalleExtracto = idDetalleExtracto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }

    public boolean isEsArrastrada() { return esArrastrada; }
    public void setEsArrastrada(boolean esArrastrada) { this.esArrastrada = esArrastrada; }

    public Integer getTipoSugerido() { return tipoSugerido; }
    public void setTipoSugerido(Integer tipoSugerido) { this.tipoSugerido = tipoSugerido; }
}
