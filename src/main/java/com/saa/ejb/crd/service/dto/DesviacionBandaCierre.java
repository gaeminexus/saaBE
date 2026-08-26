package com.saa.ejb.crd.service.dto;

/**
 * Diferencia, en una banda, entre el snapshot que dejó la corrida anterior y la
 * distribución que esa misma banda tendría hoy medida a la fecha de corte anterior.
 *
 * <b>Para qué sirve.</b> El asiento de reclasificación se calcula a cartera constante —el
 * mismo juego de cuotas medido en dos fechas— para que cuadre por construcción. El snapshot
 * anterior, en cambio, es lo que se contabilizó hace un mes. La diferencia entre los dos es
 * exactamente lo que movieron los OTROS procesos durante el mes: pagos, entregas de
 * préstamos, novaciones, cancelaciones. No es un error: es información.
 *
 * <b>Cómo leerla.</b> Una desviación negativa significa que la banda tiene hoy menos
 * capital del que se contabilizó: lo normal, porque durante el mes se cobró. Una desviación
 * positiva significa que entró cartera nueva. Cuando la Fase 3 contabilice pagos y
 * entregas, estas desviaciones deben quedar explicadas por esos asientos; mientras tanto,
 * son el aviso de que las cuentas de banda del mayor y el snapshot no coinciden.
 */
public class DesviacionBandaCierre {

    /** Código del producto. */
    private Long idProducto;

    /** Nombre del producto. */
    private String nombreProducto;

    /** Tipo de cartera: 1 = por vencer, 2 = vencido. */
    private Long tipoCartera;

    /** Número de banda. */
    private Long numeroBanda;

    /** Cuenta contable de la banda. */
    private String cuenta;

    /** Capital que la corrida anterior dejó contabilizado en la banda. */
    private Double capitalSnapshot;

    /** Capital que esa banda tendría hoy, medido a la fecha de corte anterior. */
    private Double capitalRecalculado;

    /** {@code capitalRecalculado - capitalSnapshot}. */
    private Double desviacion;

    public DesviacionBandaCierre() {
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

    public String getCuenta() {
        return cuenta;
    }

    public void setCuenta(String cuenta) {
        this.cuenta = cuenta;
    }

    public Double getCapitalSnapshot() {
        return capitalSnapshot;
    }

    public void setCapitalSnapshot(Double capitalSnapshot) {
        this.capitalSnapshot = capitalSnapshot;
    }

    public Double getCapitalRecalculado() {
        return capitalRecalculado;
    }

    public void setCapitalRecalculado(Double capitalRecalculado) {
        this.capitalRecalculado = capitalRecalculado;
    }

    public Double getDesviacion() {
        return desviacion;
    }

    public void setDesviacion(Double desviacion) {
        this.desviacion = desviacion;
    }
}
