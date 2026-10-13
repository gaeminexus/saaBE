package com.saa.ejb.crd.service.dto;

import com.saa.model.crd.Prestamo;

/**
 * Resultado de preparar la aprobación de un préstamo que sustituye a otro (reestructuración o
 * novación): el original, el tipo, el VS recalculado y, en la novación, el desembolso neto.
 */
public class ContextoSustitucion {

    private Prestamo original;
    private boolean novacion;
    private double valorSustitucion;
    private DesgloseConceptosPrestamo desglose;
    private boolean conAtraso;
    /** Novación: monto − VS (lo que se le desembolsa al socio). Reestructuración: 0. */
    private double neto;

    public Prestamo getOriginal() { return original; }
    public void setOriginal(Prestamo original) { this.original = original; }

    public boolean isNovacion() { return novacion; }
    public void setNovacion(boolean novacion) { this.novacion = novacion; }

    public double getValorSustitucion() { return valorSustitucion; }
    public void setValorSustitucion(double valorSustitucion) { this.valorSustitucion = valorSustitucion; }

    public DesgloseConceptosPrestamo getDesglose() { return desglose; }
    public void setDesglose(DesgloseConceptosPrestamo desglose) { this.desglose = desglose; }

    public boolean isConAtraso() { return conAtraso; }
    public void setConAtraso(boolean conAtraso) { this.conAtraso = conAtraso; }

    public double getNeto() { return neto; }
    public void setNeto(double neto) { this.neto = neto; }
}
