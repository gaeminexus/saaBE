package com.saa.ws.movil.dto;

/** Envoltorio {@code {"mensaje": "..."}} para las respuestas de puro texto de {@code /movil}. */
public class MensajeMovilDTO {

    private String mensaje;

    public MensajeMovilDTO() {
    }

    public MensajeMovilDTO(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
