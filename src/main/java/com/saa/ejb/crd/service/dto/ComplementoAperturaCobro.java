package com.saa.ejb.crd.service.dto;

import java.util.ArrayList;
import java.util.List;

import com.saa.model.cnt.DetalleAsiento;

/**
 * D-024 (28-oct-2026) — complemento de apertura de un cobro que acredita las cuentas por cobrar
 * del período ({@code 1.4.05.05} aportes, {@code 1.4.05.10} cuotas) por más de lo que la
 * apertura ③ dejó abierto. Lo arma
 * {@code ContabilizacionIndividualCreditoService#complementoApertura}; el llamador decide si lo
 * graba como asiento propio (reparto Petro/Arch, vinculado en {@code CRD.ANCP}) o si agrega las
 * líneas a su propio asiento (reparto de un cobro CBCR).
 *
 * <p>Por producto: {@code cobrado} = lo que el asiento del llamador acredita en la cuenta por
 * cobrar; {@code abierto} = saldo deudor de esa cuenta al fin del mes del cobro, asientos
 * vigentes; {@code complemento} = {@code cobrado − abierto} si es positivo, si no 0.</p>
 */
public class ComplementoAperturaCobro {

    private final List<DetalleAsiento> lineas = new ArrayList<>();
    private double cobradoAportes;
    private double abiertoAportes;
    private double complementoAportes;
    private double cobradoPrestamos;
    private double abiertoPrestamos;
    private double complementoPrestamos;
    private String cuentaAportes;
    private String cuentaPrestamos;

    /** Líneas D por cobrar / H por aplicar (vacía si no hace falta complemento). */
    public List<DetalleAsiento> getLineas() { return lineas; }

    public boolean isVacio() { return lineas.isEmpty(); }

    public double getTotal() {
        return Math.round((complementoAportes + complementoPrestamos) * 100.0) / 100.0;
    }

    /** Texto para la glosa del asiento y la observación de CRD.ANCP. */
    public String detalle() {
        StringBuilder texto = new StringBuilder();
        if (complementoAportes > 0.0) {
            texto.append("aportes (").append(cuentaAportes).append("): cobrado ").append(formato(cobradoAportes))
                    .append(", abierto ").append(formato(abiertoAportes)).append(", complemento ")
                    .append(formato(complementoAportes));
        }
        if (complementoPrestamos > 0.0) {
            if (texto.length() > 0) {
                texto.append("; ");
            }
            texto.append("cuotas (").append(cuentaPrestamos).append("): cobrado ").append(formato(cobradoPrestamos))
                    .append(", abierto ").append(formato(abiertoPrestamos)).append(", complemento ")
                    .append(formato(complementoPrestamos));
        }
        return texto.toString();
    }

    private static String formato(double valor) {
        return String.format("%,.2f", valor);
    }

    public double getCobradoAportes() { return cobradoAportes; }
    public void setCobradoAportes(double cobradoAportes) { this.cobradoAportes = cobradoAportes; }
    public double getAbiertoAportes() { return abiertoAportes; }
    public void setAbiertoAportes(double abiertoAportes) { this.abiertoAportes = abiertoAportes; }
    public double getComplementoAportes() { return complementoAportes; }
    public void setComplementoAportes(double complementoAportes) { this.complementoAportes = complementoAportes; }
    public double getCobradoPrestamos() { return cobradoPrestamos; }
    public void setCobradoPrestamos(double cobradoPrestamos) { this.cobradoPrestamos = cobradoPrestamos; }
    public double getAbiertoPrestamos() { return abiertoPrestamos; }
    public void setAbiertoPrestamos(double abiertoPrestamos) { this.abiertoPrestamos = abiertoPrestamos; }
    public double getComplementoPrestamos() { return complementoPrestamos; }
    public void setComplementoPrestamos(double complementoPrestamos) { this.complementoPrestamos = complementoPrestamos; }
    public String getCuentaAportes() { return cuentaAportes; }
    public void setCuentaAportes(String cuentaAportes) { this.cuentaAportes = cuentaAportes; }
    public String getCuentaPrestamos() { return cuentaPrestamos; }
    public void setCuentaPrestamos(String cuentaPrestamos) { this.cuentaPrestamos = cuentaPrestamos; }
}
