package com.saa.ejb.crd.service.dto;

import java.time.LocalDate;

/**
 * Resultado de consultar, BAJO DEMANDA, si el pago de una devolución de aportes ya se
 * confirmó en Cuentas por Pagar (botón "Consultar a contabilidad" del diálogo del aporte
 * negativo).
 *
 * {@code confirmado = false} es una respuesta VÁLIDA, no un error: significa que tesorería
 * todavía no confirmó el pago. El frontend decide si sigue mostrando el botón mirando
 * {@code fecha}: si vino con dato, el pago ya se reconcilió y el botón desaparece;
 * {@code referencia} puede quedar en {@code null} legítimamente (confirmación manual sin
 * referencia escrita) y NO debe usarse para esa decisión.
 */
public class ResultadoConsultaPagoDevolucion {

    private boolean confirmado;
    private LocalDate fecha;
    private String referencia;
    private String mensaje;

    public boolean isConfirmado() {
        return confirmado;
    }

    public void setConfirmado(boolean confirmado) {
        this.confirmado = confirmado;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
