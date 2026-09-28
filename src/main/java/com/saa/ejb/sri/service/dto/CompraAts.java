package com.saa.ejb.sri.service.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * Una línea de {@code <detalleCompras>} del detalle del ATS (API-DETALLE-ATS.md §4.3). Espejo de
 * lo que escribe {@code GeneradorAtsServiceImpl.writeDetalleCompra} en el XML, con los montos
 * declarados (Math.abs + 2 decimales, ver {@code comoDeclarado}) en vez del signo de la tabla.
 *
 * <p><b>{@link #baseImpGrav} no es "base 15%"</b>: incluye también el 5% y el 8%, porque el ATS
 * no tiene elemento propio para esas tarifas (ver el javadoc de
 * {@code GeneradorAtsServiceImpl.baseGravadaCompra}).</p>
 */
public class CompraAts implements Serializable {

    private static final long serialVersionUID = 1L;

    private String origen;
    private Long idDocumento;
    private String tipoComprobante;
    private String codSustento;
    private String tpIdProv;
    private String idProv;
    private String proveedor;
    private String numeroDocumento;
    private String establecimiento;
    private String puntoEmision;
    private String secuencial;
    private String autorizacion;
    private LocalDate fechaEmision;
    private LocalDate fechaRegistro;
    private boolean fechaRegistroCapturada;
    private double baseNoGraIva;
    private double baseImponible;
    private double baseImpGrav;
    private double baseImpExe;
    private double montoIce;
    private double montoIva;
    private double total;
    private double valRetBien10;
    private double valRetServ20;
    private double valorRetBienes;
    private double valRetServ50;
    private double valorRetServicios;
    private double valRetServ100;
    private double retencionIva;
    private List<AirAts> air;
    private double retencionRenta;
    private String numeroRetencion;
    private String autorizacionRetencion;
    private LocalDate fechaRetencion;
    private List<String> formasPago;
    private boolean formasPagoDeclaradas;

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getCodSustento() { return codSustento; }
    public void setCodSustento(String codSustento) { this.codSustento = codSustento; }

    public String getTpIdProv() { return tpIdProv; }
    public void setTpIdProv(String tpIdProv) { this.tpIdProv = tpIdProv; }

    public String getIdProv() { return idProv; }
    public void setIdProv(String idProv) { this.idProv = idProv; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public String getEstablecimiento() { return establecimiento; }
    public void setEstablecimiento(String establecimiento) { this.establecimiento = establecimiento; }

    public String getPuntoEmision() { return puntoEmision; }
    public void setPuntoEmision(String puntoEmision) { this.puntoEmision = puntoEmision; }

    public String getSecuencial() { return secuencial; }
    public void setSecuencial(String secuencial) { this.secuencial = secuencial; }

    public String getAutorizacion() { return autorizacion; }
    public void setAutorizacion(String autorizacion) { this.autorizacion = autorizacion; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isFechaRegistroCapturada() { return fechaRegistroCapturada; }
    public void setFechaRegistroCapturada(boolean fechaRegistroCapturada) { this.fechaRegistroCapturada = fechaRegistroCapturada; }

    public double getBaseNoGraIva() { return baseNoGraIva; }
    public void setBaseNoGraIva(double baseNoGraIva) { this.baseNoGraIva = baseNoGraIva; }

    public double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(double baseImponible) { this.baseImponible = baseImponible; }

    public double getBaseImpGrav() { return baseImpGrav; }
    public void setBaseImpGrav(double baseImpGrav) { this.baseImpGrav = baseImpGrav; }

    public double getBaseImpExe() { return baseImpExe; }
    public void setBaseImpExe(double baseImpExe) { this.baseImpExe = baseImpExe; }

    public double getMontoIce() { return montoIce; }
    public void setMontoIce(double montoIce) { this.montoIce = montoIce; }

    public double getMontoIva() { return montoIva; }
    public void setMontoIva(double montoIva) { this.montoIva = montoIva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getValRetBien10() { return valRetBien10; }
    public void setValRetBien10(double valRetBien10) { this.valRetBien10 = valRetBien10; }

    public double getValRetServ20() { return valRetServ20; }
    public void setValRetServ20(double valRetServ20) { this.valRetServ20 = valRetServ20; }

    public double getValorRetBienes() { return valorRetBienes; }
    public void setValorRetBienes(double valorRetBienes) { this.valorRetBienes = valorRetBienes; }

    public double getValRetServ50() { return valRetServ50; }
    public void setValRetServ50(double valRetServ50) { this.valRetServ50 = valRetServ50; }

    public double getValorRetServicios() { return valorRetServicios; }
    public void setValorRetServicios(double valorRetServicios) { this.valorRetServicios = valorRetServicios; }

    public double getValRetServ100() { return valRetServ100; }
    public void setValRetServ100(double valRetServ100) { this.valRetServ100 = valRetServ100; }

    public double getRetencionIva() { return retencionIva; }
    public void setRetencionIva(double retencionIva) { this.retencionIva = retencionIva; }

    public List<AirAts> getAir() { return air; }
    public void setAir(List<AirAts> air) { this.air = air; }

    public double getRetencionRenta() { return retencionRenta; }
    public void setRetencionRenta(double retencionRenta) { this.retencionRenta = retencionRenta; }

    public String getNumeroRetencion() { return numeroRetencion; }
    public void setNumeroRetencion(String numeroRetencion) { this.numeroRetencion = numeroRetencion; }

    public String getAutorizacionRetencion() { return autorizacionRetencion; }
    public void setAutorizacionRetencion(String autorizacionRetencion) { this.autorizacionRetencion = autorizacionRetencion; }

    public LocalDate getFechaRetencion() { return fechaRetencion; }
    public void setFechaRetencion(LocalDate fechaRetencion) { this.fechaRetencion = fechaRetencion; }

    public List<String> getFormasPago() { return formasPago; }
    public void setFormasPago(List<String> formasPago) { this.formasPago = formasPago; }

    public boolean isFormasPagoDeclaradas() { return formasPagoDeclaradas; }
    public void setFormasPagoDeclaradas(boolean formasPagoDeclaradas) { this.formasPagoDeclaradas = formasPagoDeclaradas; }
}
