package com.saa.model.tsr;

import java.io.Serializable;
import java.util.List;

/**
 * @author GaemiSoft
 * <p>Respuesta de GET /cnct/transito/preparar/{idCuentaBancaria}/{idPeriodo}. Ver
 * ConciliacionCierreService.prepararCierre y
 * docs/logica-negocio/tsr/DISENO-CONCILIACION-PARTIDAS-EN-TRANSITO.md §6.</p>
 */
public class PreparacionCierreTransito implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idCuentaBancaria;
    private Long idPeriodo;
    private List<GrupoConciliadoResumen> conciliadosDelMes;
    private List<PendienteExtractoTransito> pendientesExtracto;
    private List<PendienteAsientoTransito> pendientesAsiento;

    /**
     * Saldo según libros: contabilidad, no TSR.MVCB (corregido 2026-08-27, §7bis del diseño).
     * Sale de PlanCuentaService.saldoCuentaFechaEmpresa al último día del período.
     */
    private Double saldoLibros;

    /** Sugerido a partir del último renglón de TSR.DEXB del período; el usuario lo confirma o lo corrige en cerrar(). */
    private Double saldoExtractoSugerido;

    /** saldoLibros vs. saldoExtractoSugerido, aplicando la regla de signos a TODOS los pendientes con su tipoSugerido -no es la ecuación final, solo una vista previa. */
    private Double diferenciaSugerida;

    public Long getIdCuentaBancaria() { return idCuentaBancaria; }
    public void setIdCuentaBancaria(Long idCuentaBancaria) { this.idCuentaBancaria = idCuentaBancaria; }

    public Long getIdPeriodo() { return idPeriodo; }
    public void setIdPeriodo(Long idPeriodo) { this.idPeriodo = idPeriodo; }

    public List<GrupoConciliadoResumen> getConciliadosDelMes() { return conciliadosDelMes; }
    public void setConciliadosDelMes(List<GrupoConciliadoResumen> conciliadosDelMes) { this.conciliadosDelMes = conciliadosDelMes; }

    public List<PendienteExtractoTransito> getPendientesExtracto() { return pendientesExtracto; }
    public void setPendientesExtracto(List<PendienteExtractoTransito> pendientesExtracto) { this.pendientesExtracto = pendientesExtracto; }

    public List<PendienteAsientoTransito> getPendientesAsiento() { return pendientesAsiento; }
    public void setPendientesAsiento(List<PendienteAsientoTransito> pendientesAsiento) { this.pendientesAsiento = pendientesAsiento; }

    public Double getSaldoLibros() { return saldoLibros; }
    public void setSaldoLibros(Double saldoLibros) { this.saldoLibros = saldoLibros; }

    public Double getSaldoExtractoSugerido() { return saldoExtractoSugerido; }
    public void setSaldoExtractoSugerido(Double saldoExtractoSugerido) { this.saldoExtractoSugerido = saldoExtractoSugerido; }

    public Double getDiferenciaSugerida() { return diferenciaSugerida; }
    public void setDiferenciaSugerida(Double diferenciaSugerida) { this.diferenciaSugerida = diferenciaSugerida; }
}
