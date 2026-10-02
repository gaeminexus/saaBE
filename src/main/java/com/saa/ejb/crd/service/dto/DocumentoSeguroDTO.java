package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** El documento con sus totales — {@code GET /posg/listar} y {@code GET /posg/{id}} (contrato §8). */
public class DocumentoSeguroDTO {

    private Long id;
    private Long tipoSeguro;
    private Long clase;
    private Long idPadre;
    private Long estado;
    private LocalDate fechaCorte;
    private String aseguradora;
    private String ruc;
    private String numeroPoliza;
    private String numeroDocumento;
    private String claveAcceso;
    private LocalDate fechaEmision;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Double tasa;
    private Double valorTotal;
    private Long idDocumentoCxp;
    private int cantidadPrestamos;
    private double sumaBase;
    private double sumaDistribuida;
    private List<DocumentoSeguroDTO> notas = new ArrayList<>();
    private AuditoriaDocumentoSeguro auditoria;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTipoSeguro() { return tipoSeguro; }
    public void setTipoSeguro(Long tipoSeguro) { this.tipoSeguro = tipoSeguro; }

    public Long getClase() { return clase; }
    public void setClase(Long clase) { this.clase = clase; }

    public Long getIdPadre() { return idPadre; }
    public void setIdPadre(Long idPadre) { this.idPadre = idPadre; }

    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }

    public LocalDate getFechaCorte() { return fechaCorte; }
    public void setFechaCorte(LocalDate fechaCorte) { this.fechaCorte = fechaCorte; }

    public String getAseguradora() { return aseguradora; }
    public void setAseguradora(String aseguradora) { this.aseguradora = aseguradora; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getNumeroPoliza() { return numeroPoliza; }
    public void setNumeroPoliza(String numeroPoliza) { this.numeroPoliza = numeroPoliza; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getClaveAcceso() { return claveAcceso; }
    public void setClaveAcceso(String claveAcceso) { this.claveAcceso = claveAcceso; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Double getTasa() { return tasa; }
    public void setTasa(Double tasa) { this.tasa = tasa; }

    public Double getValorTotal() { return valorTotal; }
    public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }

    public Long getIdDocumentoCxp() { return idDocumentoCxp; }
    public void setIdDocumentoCxp(Long idDocumentoCxp) { this.idDocumentoCxp = idDocumentoCxp; }

    public int getCantidadPrestamos() { return cantidadPrestamos; }
    public void setCantidadPrestamos(int cantidadPrestamos) { this.cantidadPrestamos = cantidadPrestamos; }

    public double getSumaBase() { return sumaBase; }
    public void setSumaBase(double sumaBase) { this.sumaBase = sumaBase; }

    public double getSumaDistribuida() { return sumaDistribuida; }
    public void setSumaDistribuida(double sumaDistribuida) { this.sumaDistribuida = sumaDistribuida; }

    public List<DocumentoSeguroDTO> getNotas() { return notas; }
    public void setNotas(List<DocumentoSeguroDTO> notas) { this.notas = notas; }

    public AuditoriaDocumentoSeguro getAuditoria() { return auditoria; }
    public void setAuditoria(AuditoriaDocumentoSeguro auditoria) { this.auditoria = auditoria; }

    /** Auditoría por paso — {@code com.saa.model.crd.DocumentoSeguro}, campos POSGUSxx/POSGFCxx. */
    public static class AuditoriaDocumentoSeguro {
        private String usuarioListado;
        private LocalDateTime fechaListado;
        private String usuarioDocumento;
        private LocalDateTime fechaDocumento;
        private String usuarioDistribucion;
        private LocalDateTime fechaDistribucion;
        private String usuarioLiberacion;
        private LocalDateTime fechaLiberacion;
        private String usuarioAnulacion;
        private LocalDateTime fechaAnulacion;
        private String motivoAnulacion;

        public String getUsuarioListado() { return usuarioListado; }
        public void setUsuarioListado(String usuarioListado) { this.usuarioListado = usuarioListado; }

        public LocalDateTime getFechaListado() { return fechaListado; }
        public void setFechaListado(LocalDateTime fechaListado) { this.fechaListado = fechaListado; }

        public String getUsuarioDocumento() { return usuarioDocumento; }
        public void setUsuarioDocumento(String usuarioDocumento) { this.usuarioDocumento = usuarioDocumento; }

        public LocalDateTime getFechaDocumento() { return fechaDocumento; }
        public void setFechaDocumento(LocalDateTime fechaDocumento) { this.fechaDocumento = fechaDocumento; }

        public String getUsuarioDistribucion() { return usuarioDistribucion; }
        public void setUsuarioDistribucion(String usuarioDistribucion) { this.usuarioDistribucion = usuarioDistribucion; }

        public LocalDateTime getFechaDistribucion() { return fechaDistribucion; }
        public void setFechaDistribucion(LocalDateTime fechaDistribucion) { this.fechaDistribucion = fechaDistribucion; }

        public String getUsuarioLiberacion() { return usuarioLiberacion; }
        public void setUsuarioLiberacion(String usuarioLiberacion) { this.usuarioLiberacion = usuarioLiberacion; }

        public LocalDateTime getFechaLiberacion() { return fechaLiberacion; }
        public void setFechaLiberacion(LocalDateTime fechaLiberacion) { this.fechaLiberacion = fechaLiberacion; }

        public String getUsuarioAnulacion() { return usuarioAnulacion; }
        public void setUsuarioAnulacion(String usuarioAnulacion) { this.usuarioAnulacion = usuarioAnulacion; }

        public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
        public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

        public String getMotivoAnulacion() { return motivoAnulacion; }
        public void setMotivoAnulacion(String motivoAnulacion) { this.motivoAnulacion = motivoAnulacion; }
    }
}
