package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body de {@code POST /rest/c35ensayo}. Ver {@link com.saa.ejb.crd.service.EnsayoSaldoOtrosService}.
 */
public class SolicitudEnsayoSaldoOtros {

    private Long idPrestamo;
    private Long idEmpresa;
    private String usuario;
    private List<Paso> pasos = new ArrayList<>();

    /**
     * Un paso del ensayo. {@code tipo}:
     * <ul>
     * <li>{@code REAPERTURA}: {@code prepararPrestamoAlCorte(prestamo, fecha)} (D-019, la misma del reproceso);</li>
     * <li>{@code ANULAR}: {@code anularOperacion(idEvento, motivo)};</li>
     * <li>{@code PRECANCELAR}: simula a {@code fecha} y precancela por el total simulado, en efectivo, con
     *     {@code reproceso};</li>
     * <li>{@code ABONO}: abono a capital de {@code valor} ({@code modalidad}, {@code regenerarTabla},
     *     {@code numeroCuota}, {@code reproceso}) a {@code fecha};</li>
     * <li>{@code RECALCULAR}: {@code recalcularCuotaDesdePagos} sobre todas las cuotas del préstamo.</li>
     * </ul>
     */
    public static class Paso {
        private String tipo;
        private LocalDate fecha;
        private Long idEvento;
        private String motivo;
        private Boolean reproceso;
        private Double valor;
        private Integer modalidad;
        private Boolean regenerarTabla;
        private Integer numeroCuota;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public LocalDate getFecha() { return fecha; }
        public void setFecha(LocalDate fecha) { this.fecha = fecha; }
        public Long getIdEvento() { return idEvento; }
        public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo; }
        public Boolean getReproceso() { return reproceso; }
        public void setReproceso(Boolean reproceso) { this.reproceso = reproceso; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
        public Integer getModalidad() { return modalidad; }
        public void setModalidad(Integer modalidad) { this.modalidad = modalidad; }
        public Boolean getRegenerarTabla() { return regenerarTabla; }
        public void setRegenerarTabla(Boolean regenerarTabla) { this.regenerarTabla = regenerarTabla; }
        public Integer getNumeroCuota() { return numeroCuota; }
        public void setNumeroCuota(Integer numeroCuota) { this.numeroCuota = numeroCuota; }
    }

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getIdEmpresa() { return idEmpresa; }
    public void setIdEmpresa(Long idEmpresa) { this.idEmpresa = idEmpresa; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public List<Paso> getPasos() { return pasos; }
    public void setPasos(List<Paso> pasos) { this.pasos = pasos; }
}
