package com.saa.ejb.crd.service.dto;

/**
 * Una línea de asiento del cierre de cartera, tal como la va a ver contabilidad en la
 * previsualización y tal como se graba después.
 *
 * Es el equivalente de {@code com.saa.model.rhh.LineaAsientoNomina} para CRD: cuenta,
 * descripción, debe y haber, más el origen de la línea para poder auditarla sin abrir el
 * asiento.
 */
public class LineaAsientoCierre {

    /** Cuenta contable con puntos, p.ej. "1.3.01.05". */
    private String cuenta;

    /** Nombre de la cuenta. */
    private String nombreCuenta;

    /** Código de la cuenta (CNT.PLNN.PLNNCDGO). */
    private Long idPlanCuenta;

    /**
     * Descripción de la línea. En el devengo de intereses distingue explícitamente si es
     * mora o interés ordinario: las dos comparten cuenta (decisión D3 de §9.1) y la
     * descripción es lo único que las separa en el mayor.
     */
    private String descripcion;

    /** Valor al DEBE. Cero si la línea es de HABER. */
    private Double debe;

    /** Valor al HABER. Cero si la línea es de DEBE. */
    private Double haber;

    /** Código del producto que originó la línea. Nulo en las líneas agregadas. */
    private Long idProducto;

    /** Nombre del producto que originó la línea. Nulo en las líneas agregadas. */
    private String nombreProducto;

    /** Tipo de cartera de la línea: 1 = por vencer, 2 = vencido. Nulo si no aplica. */
    private Long tipoCartera;

    /** Número de banda que originó la línea. Nulo si la línea no es de banda. */
    private Long numeroBanda;

    /** Código del papel de la línea ({@link com.saa.rubros.CrdLineaAsiento}). Nulo en las de banda. */
    private Long codigoLinea;

    public LineaAsientoCierre() {
    }

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public Long getIdPlanCuenta() {
        return idPlanCuenta;
    }

    public void setIdPlanCuenta(Long idPlanCuenta) {
        this.idPlanCuenta = idPlanCuenta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Double getDebe() {
        return debe;
    }

    public void setDebe(Double debe) {
        this.debe = debe;
    }

    public Double getHaber() {
        return haber;
    }

    public void setHaber(Double haber) {
        this.haber = haber;
    }

    public Long getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Long idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Long getTipoCartera() {
        return tipoCartera;
    }

    public void setTipoCartera(Long tipoCartera) {
        this.tipoCartera = tipoCartera;
    }

    public Long getNumeroBanda() {
        return numeroBanda;
    }

    public void setNumeroBanda(Long numeroBanda) {
        this.numeroBanda = numeroBanda;
    }

    public Long getCodigoLinea() {
        return codigoLinea;
    }

    public void setCodigoLinea(Long codigoLinea) {
        this.codigoLinea = codigoLinea;
    }
}
