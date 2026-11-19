package com.saa.model.cxp;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @author GaemiSoft
 * <p>Resultado detallado de la regla del sustento tributario (Tabla 5 del ATS) de un documento de
 * compra: el código, qué parte de la regla lo decidió y, si hace falta, el aviso para revisar.
 * Lo devuelve {@link com.saa.ejb.cxp.service.SustentoTributarioService#evaluarSustento(String, Long)}
 * (sin guardar nada) y lo usan la verificación de un período, el ATS y el cuadre del 104.</p>
 *
 * <p>Valores de {@link #regla} (C37, D-126 del 19-nov-2026):</p>
 * <ul>
 *   <li>{@value #REGLA_EXCEPCION_GRUPO}: el grupo de producto con mayor base tiene 03/04/06/07/08.</li>
 *   <li>{@value #REGLA_SIN_IVA}: el documento no tiene IVA → 02, sea cual sea el centro.</li>
 *   <li>{@value #REGLA_CENTRO_CON_CREDITO}: con IVA y centro de costo parametrizado con crédito → 01.</li>
 *   <li>{@value #REGLA_CENTRO_SIN_CREDITO}: con IVA y centro de costo sin crédito → 02.</li>
 *   <li>{@value #REGLA_IVA_SIN_CENTRO}: con IVA y sin centro de costo en el asiento → 01 (regla del IVA
 *       de antes de D-126), con aviso.</li>
 *   <li>{@value #REGLA_IVA_SIN_PARAMETRO}: el rubro de los centros con crédito no existe → regla del IVA,
 *       con aviso.</li>
 * </ul>
 */
public class ResultadoSustento implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String REGLA_EXCEPCION_GRUPO = "EXCEPCION_GRUPO";
    public static final String REGLA_SIN_IVA = "SIN_IVA";
    public static final String REGLA_CENTRO_CON_CREDITO = "CENTRO_CON_CREDITO";
    public static final String REGLA_CENTRO_SIN_CREDITO = "CENTRO_SIN_CREDITO";
    public static final String REGLA_IVA_SIN_CENTRO = "IVA_SIN_CENTRO";
    public static final String REGLA_IVA_SIN_PARAMETRO = "IVA_SIN_PARAMETRO";

    /** FACTURA, LIQUIDACION, NOTA_CREDITO o NOTA_DEBITO (constantes de SustentoTributarioService). */
    private String tipoDocumento;
    private Long idDocumento;
    private String numero;
    private LocalDate fecha;
    private Double iva;
    /** Lo que el documento tiene guardado hoy (FCTCCSUS/LQCCCSUS/NTCCCSUS/NTDCCSUS); null = vacío. */
    private String sustentoGuardado;
    /** Lo que da la regla hoy; null solo si el documento no existe. */
    private String sustento;
    private String regla;
    /** Centro de costo que decidió (CNT.CNCS.CNCSCDGO), el de mayor valor en las líneas del asiento. */
    private Long idCentroCosto;
    private String centroCosto;
    /** Más de un centro de costo en el asiento: se tomó el de mayor valor. */
    private boolean centroMixto;
    /** Texto para revisar; null si la regla decidió sin dudas. */
    private String aviso;

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Long getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Long idDocumento) { this.idDocumento = idDocumento; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }

    public String getSustentoGuardado() { return sustentoGuardado; }
    public void setSustentoGuardado(String sustentoGuardado) { this.sustentoGuardado = sustentoGuardado; }

    public String getSustento() { return sustento; }
    public void setSustento(String sustento) { this.sustento = sustento; }

    public String getRegla() { return regla; }
    public void setRegla(String regla) { this.regla = regla; }

    public Long getIdCentroCosto() { return idCentroCosto; }
    public void setIdCentroCosto(Long idCentroCosto) { this.idCentroCosto = idCentroCosto; }

    public String getCentroCosto() { return centroCosto; }
    public void setCentroCosto(String centroCosto) { this.centroCosto = centroCosto; }

    public boolean isCentroMixto() { return centroMixto; }
    public void setCentroMixto(boolean centroMixto) { this.centroMixto = centroMixto; }

    public String getAviso() { return aviso; }
    public void setAviso(String aviso) { this.aviso = aviso; }

    /** El sustento guardado coincide con el de la regla. */
    public boolean isCoincide() {
        return sustento != null && sustento.equals(sustentoGuardado);
    }
}
