package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Proyección de una factura de compra con sustento tributario pendiente, para
 * {@code GET /fctc/sustentoPendiente}. NO es la entidad {@link FacturaCompra} completa -a
 * propósito: serializar la entidad arrastra {@code empresa} con su jerarquía, {@code asiento},
 * mayorización y el resto del grafo (medido: 536 KB para 131 facturas). Esta proyección trae
 * sólo lo que la pantalla necesita para que el usuario revise y corrija antes del primer ATS.</p>
 */
public class FacturaSustentoPendiente implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String numero;
    private LocalDate fecha;
    private String proveedor;
    private String identificacion;
    private Double total;
    private Double iva;

    /**
     * Lo que resolvería {@link com.saa.ejb.cxp.service.SustentoTributarioService#calcularSustento(Long)}
     * hoy: la excepción por grupo de producto si aplica, si no la regla base del IVA de la
     * factura. Es una sugerencia de sólo lectura -no se guarda aquí-, para que el frontend no
     * tenga que reimplementar la regla.
     */
    private String sustentoSugerido;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }

    public String getSustentoSugerido() { return sustentoSugerido; }
    public void setSustentoSugerido(String sustentoSugerido) { this.sustentoSugerido = sustentoSugerido; }
}
