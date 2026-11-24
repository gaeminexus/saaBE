package com.saa.ejb.crd.service.dto;

import java.io.Serializable;

/** Una fila del reporte de préstamos cancelados (contrato §5, API-REPORTE-PRESTAMOS-CANCELADOS.md). Las 56 columnas en el orden del contrato; todas las fechas viajan como String. Es también el bean que llena RPRT_PRCN_LSTD.jasper (campos = getters). */
public class PrestamoCanceladoFila implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long idPrestamo;
    private Long numeroOperacion;
    private String cedula;
    private String nombreSocio;
    private String filial;
    private String producto;
    private Long estadoCodigo;
    private String estado;
    private String fechaCancelacion;
    private String origenFecha;
    private String forma;
    private String formaDescripcion;
    private String canal;
    private String fechaOtorgamiento;
    private String fechaInicio;
    private String fechaFinPlazo;
    private Long plazo;
    private Double tasa;
    private Double valorCuota;
    private Double montoSolicitado;
    private Double montoLiquido;
    private Long cuotasPagadas;
    private Long cuotasCanceladasAnticipadas;
    private Long ultimaCuotaPagada;
    private Long numeroPagos;
    private String fechaPrimerPago;
    private String fechaUltimoPago;
    private Double totalCapitalPagado;
    private Double totalInteresPagado;
    private Double totalMoraPagada;
    private Double totalInteresVencidoPagado;
    private Double totalDesgravamenPagado;
    private Double totalSeguroIncendioPagado;
    private Double totalSaldoOtrosPagado;
    private Double totalPagado;
    private Double totalAportesUsados;
    private Double totalAportesGarante;
    private String tieneMigracion;
    private String tipoOperacionFinal;
    private String fechaOperacionFinal;
    private Double valorOperacionFinal;
    private Double efectivoOperacionFinal;
    private Double aportesOperacionFinal;
    private Double aportesGaranteOperacionFinal;
    private Double valorCondonado;
    private String usuarioOperacionFinal;
    private Long idEvento;
    private Long idCobro;
    private Long idCargaArchivo;
    private Long idAsiento;
    private Long idPrestamoNuevo;
    private Long numeroOperacionNuevo;
    private Double montoPrestamoNuevo;
    private Double valorSustitucion;
    private String observacionOperacionFinal;
    private String observacionReporte;

    public Long getIdPrestamo() { return idPrestamo; }
    public void setIdPrestamo(Long idPrestamo) { this.idPrestamo = idPrestamo; }
    public Long getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(Long numeroOperacion) { this.numeroOperacion = numeroOperacion; }
    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }
    public String getNombreSocio() { return nombreSocio; }
    public void setNombreSocio(String nombreSocio) { this.nombreSocio = nombreSocio; }
    public String getFilial() { return filial; }
    public void setFilial(String filial) { this.filial = filial; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public Long getEstadoCodigo() { return estadoCodigo; }
    public void setEstadoCodigo(Long estadoCodigo) { this.estadoCodigo = estadoCodigo; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getFechaCancelacion() { return fechaCancelacion; }
    public void setFechaCancelacion(String fechaCancelacion) { this.fechaCancelacion = fechaCancelacion; }
    public String getOrigenFecha() { return origenFecha; }
    public void setOrigenFecha(String origenFecha) { this.origenFecha = origenFecha; }
    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }
    public String getFormaDescripcion() { return formaDescripcion; }
    public void setFormaDescripcion(String formaDescripcion) { this.formaDescripcion = formaDescripcion; }
    public String getCanal() { return canal; }
    public void setCanal(String canal) { this.canal = canal; }
    public String getFechaOtorgamiento() { return fechaOtorgamiento; }
    public void setFechaOtorgamiento(String fechaOtorgamiento) { this.fechaOtorgamiento = fechaOtorgamiento; }
    public String getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(String fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getFechaFinPlazo() { return fechaFinPlazo; }
    public void setFechaFinPlazo(String fechaFinPlazo) { this.fechaFinPlazo = fechaFinPlazo; }
    public Long getPlazo() { return plazo; }
    public void setPlazo(Long plazo) { this.plazo = plazo; }
    public Double getTasa() { return tasa; }
    public void setTasa(Double tasa) { this.tasa = tasa; }
    public Double getValorCuota() { return valorCuota; }
    public void setValorCuota(Double valorCuota) { this.valorCuota = valorCuota; }
    public Double getMontoSolicitado() { return montoSolicitado; }
    public void setMontoSolicitado(Double montoSolicitado) { this.montoSolicitado = montoSolicitado; }
    public Double getMontoLiquido() { return montoLiquido; }
    public void setMontoLiquido(Double montoLiquido) { this.montoLiquido = montoLiquido; }
    public Long getCuotasPagadas() { return cuotasPagadas; }
    public void setCuotasPagadas(Long cuotasPagadas) { this.cuotasPagadas = cuotasPagadas; }
    public Long getCuotasCanceladasAnticipadas() { return cuotasCanceladasAnticipadas; }
    public void setCuotasCanceladasAnticipadas(Long cuotasCanceladasAnticipadas) { this.cuotasCanceladasAnticipadas = cuotasCanceladasAnticipadas; }
    public Long getUltimaCuotaPagada() { return ultimaCuotaPagada; }
    public void setUltimaCuotaPagada(Long ultimaCuotaPagada) { this.ultimaCuotaPagada = ultimaCuotaPagada; }
    public Long getNumeroPagos() { return numeroPagos; }
    public void setNumeroPagos(Long numeroPagos) { this.numeroPagos = numeroPagos; }
    public String getFechaPrimerPago() { return fechaPrimerPago; }
    public void setFechaPrimerPago(String fechaPrimerPago) { this.fechaPrimerPago = fechaPrimerPago; }
    public String getFechaUltimoPago() { return fechaUltimoPago; }
    public void setFechaUltimoPago(String fechaUltimoPago) { this.fechaUltimoPago = fechaUltimoPago; }
    public Double getTotalCapitalPagado() { return totalCapitalPagado; }
    public void setTotalCapitalPagado(Double totalCapitalPagado) { this.totalCapitalPagado = totalCapitalPagado; }
    public Double getTotalInteresPagado() { return totalInteresPagado; }
    public void setTotalInteresPagado(Double totalInteresPagado) { this.totalInteresPagado = totalInteresPagado; }
    public Double getTotalMoraPagada() { return totalMoraPagada; }
    public void setTotalMoraPagada(Double totalMoraPagada) { this.totalMoraPagada = totalMoraPagada; }
    public Double getTotalInteresVencidoPagado() { return totalInteresVencidoPagado; }
    public void setTotalInteresVencidoPagado(Double totalInteresVencidoPagado) { this.totalInteresVencidoPagado = totalInteresVencidoPagado; }
    public Double getTotalDesgravamenPagado() { return totalDesgravamenPagado; }
    public void setTotalDesgravamenPagado(Double totalDesgravamenPagado) { this.totalDesgravamenPagado = totalDesgravamenPagado; }
    public Double getTotalSeguroIncendioPagado() { return totalSeguroIncendioPagado; }
    public void setTotalSeguroIncendioPagado(Double totalSeguroIncendioPagado) { this.totalSeguroIncendioPagado = totalSeguroIncendioPagado; }
    public Double getTotalSaldoOtrosPagado() { return totalSaldoOtrosPagado; }
    public void setTotalSaldoOtrosPagado(Double totalSaldoOtrosPagado) { this.totalSaldoOtrosPagado = totalSaldoOtrosPagado; }
    public Double getTotalPagado() { return totalPagado; }
    public void setTotalPagado(Double totalPagado) { this.totalPagado = totalPagado; }
    public Double getTotalAportesUsados() { return totalAportesUsados; }
    public void setTotalAportesUsados(Double totalAportesUsados) { this.totalAportesUsados = totalAportesUsados; }
    public Double getTotalAportesGarante() { return totalAportesGarante; }
    public void setTotalAportesGarante(Double totalAportesGarante) { this.totalAportesGarante = totalAportesGarante; }
    public String getTieneMigracion() { return tieneMigracion; }
    public void setTieneMigracion(String tieneMigracion) { this.tieneMigracion = tieneMigracion; }
    public String getTipoOperacionFinal() { return tipoOperacionFinal; }
    public void setTipoOperacionFinal(String tipoOperacionFinal) { this.tipoOperacionFinal = tipoOperacionFinal; }
    public String getFechaOperacionFinal() { return fechaOperacionFinal; }
    public void setFechaOperacionFinal(String fechaOperacionFinal) { this.fechaOperacionFinal = fechaOperacionFinal; }
    public Double getValorOperacionFinal() { return valorOperacionFinal; }
    public void setValorOperacionFinal(Double valorOperacionFinal) { this.valorOperacionFinal = valorOperacionFinal; }
    public Double getEfectivoOperacionFinal() { return efectivoOperacionFinal; }
    public void setEfectivoOperacionFinal(Double efectivoOperacionFinal) { this.efectivoOperacionFinal = efectivoOperacionFinal; }
    public Double getAportesOperacionFinal() { return aportesOperacionFinal; }
    public void setAportesOperacionFinal(Double aportesOperacionFinal) { this.aportesOperacionFinal = aportesOperacionFinal; }
    public Double getAportesGaranteOperacionFinal() { return aportesGaranteOperacionFinal; }
    public void setAportesGaranteOperacionFinal(Double aportesGaranteOperacionFinal) { this.aportesGaranteOperacionFinal = aportesGaranteOperacionFinal; }
    public Double getValorCondonado() { return valorCondonado; }
    public void setValorCondonado(Double valorCondonado) { this.valorCondonado = valorCondonado; }
    public String getUsuarioOperacionFinal() { return usuarioOperacionFinal; }
    public void setUsuarioOperacionFinal(String usuarioOperacionFinal) { this.usuarioOperacionFinal = usuarioOperacionFinal; }
    public Long getIdEvento() { return idEvento; }
    public void setIdEvento(Long idEvento) { this.idEvento = idEvento; }
    public Long getIdCobro() { return idCobro; }
    public void setIdCobro(Long idCobro) { this.idCobro = idCobro; }
    public Long getIdCargaArchivo() { return idCargaArchivo; }
    public void setIdCargaArchivo(Long idCargaArchivo) { this.idCargaArchivo = idCargaArchivo; }
    public Long getIdAsiento() { return idAsiento; }
    public void setIdAsiento(Long idAsiento) { this.idAsiento = idAsiento; }
    public Long getIdPrestamoNuevo() { return idPrestamoNuevo; }
    public void setIdPrestamoNuevo(Long idPrestamoNuevo) { this.idPrestamoNuevo = idPrestamoNuevo; }
    public Long getNumeroOperacionNuevo() { return numeroOperacionNuevo; }
    public void setNumeroOperacionNuevo(Long numeroOperacionNuevo) { this.numeroOperacionNuevo = numeroOperacionNuevo; }
    public Double getMontoPrestamoNuevo() { return montoPrestamoNuevo; }
    public void setMontoPrestamoNuevo(Double montoPrestamoNuevo) { this.montoPrestamoNuevo = montoPrestamoNuevo; }
    public Double getValorSustitucion() { return valorSustitucion; }
    public void setValorSustitucion(Double valorSustitucion) { this.valorSustitucion = valorSustitucion; }
    public String getObservacionOperacionFinal() { return observacionOperacionFinal; }
    public void setObservacionOperacionFinal(String observacionOperacionFinal) { this.observacionOperacionFinal = observacionOperacionFinal; }
    public String getObservacionReporte() { return observacionReporte; }
    public void setObservacionReporte(String observacionReporte) { this.observacionReporte = observacionReporte; }
}
