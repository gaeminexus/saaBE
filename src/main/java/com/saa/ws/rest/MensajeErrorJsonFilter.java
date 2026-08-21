package com.saa.ws.rest;

import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;

/**
 * Envuelve en JSON el cuerpo de texto de las respuestas de error.
 *
 * <p>
 * <b>El problema que resuelve.</b> Todos los metodos de {@code ws.rest} devuelven
 * sus errores con el mismo patron:
 * </p>
 *
 * <pre>
 * Response.status(INTERNAL_SERVER_ERROR)
 *         .entity("Error al calcular: " + e.getMessage())
 *         .type(MediaType.APPLICATION_JSON).build();
 * </pre>
 *
 * <p>
 * El cuerpo es texto plano <b>sin comillas</b> pero el tipo declarado es JSON, de
 * modo que el cliente intenta parsearlo, falla, y el mensaje real queda enterrado
 * en la propiedad {@code text} del error de HttpClient sin llegar nunca a la
 * pantalla. El usuario ve un mensaje generico y el motivo se pierde. Afectaba a
 * <b>todos los modulos a la vez</b>, porque el patron esta en cada metodo de cada
 * clase REST.
 * </p>
 *
 * <p>
 * <b>Por que un filtro y no corregir los metodos.</b> Son cientos de metodos en
 * decenas de clases. Un filtro los repara todos de una vez, no puede olvidarse
 * ninguno, y el codigo nuevo lo hereda sin que nadie tenga que acordarse. El
 * contrato pretendido siempre fue {@code {"mensaje": "..."}} —los manejadores del
 * frontend leen {@code error.mensaje} primero—; lo que faltaba era honrarlo.
 * </p>
 *
 * <p>
 * <b>Solo actua sobre las tres condiciones a la vez:</b> estado 400 o superior, la
 * entidad es un {@code String}, y el tipo declarado es JSON. Un endpoint que
 * declara {@code TEXT_PLAIN} no se toca —es el caso de {@code validaUsuario},
 * {@code cambiaClave} y {@code verificaPermiso} de {@code UsuarioRest}, que
 * devuelven texto a proposito—, y un cuerpo que ya venia siendo JSON tampoco, para
 * no envolverlo dos veces.
 * </p>
 *
 * <p>
 * El envoltorio se arma como {@code Map} y lo serializa el proveedor de JSON, no
 * la concatenacion de cadenas: los mensajes de este sistema llevan comillas y
 * saltos de linea —{@code aprobarPeriodo} construye uno de varias lineas— y
 * armarlo a mano produciria JSON invalido justo en el mensaje mas largo.
 * </p>
 */
@Provider
public class MensajeErrorJsonFilter implements ContainerResponseFilter {

    /** Clave del envoltorio. Es la que ya leen los manejadores del frontend. */
    private static final String CLAVE_MENSAJE = "mensaje";

    /*
     * (non-Javadoc)
     *
     * @see jakarta.ws.rs.container.ContainerResponseFilter#filter(
     *      jakarta.ws.rs.container.ContainerRequestContext,
     *      jakarta.ws.rs.container.ContainerResponseContext)
     */
    @Override
    public void filter(ContainerRequestContext peticion, ContainerResponseContext respuesta) {
        if (respuesta.getStatus() < 400) {
            return;
        }
        if (!(respuesta.getEntity() instanceof String texto)) {
            return;
        }
        MediaType tipo = respuesta.getMediaType();
        if (tipo == null || !MediaType.APPLICATION_JSON_TYPE.isCompatible(tipo)) {
            return;
        }
        String limpio = texto.trim();
        if (limpio.isEmpty() || limpio.startsWith("{") || limpio.startsWith("[")) {
            // Ya es JSON: envolverlo otra vez esconderia el mensaje un nivel mas abajo.
            return;
        }
        System.out.println("MensajeErrorJsonFilter envuelve un error " + respuesta.getStatus()
                + " de " + peticion.getUriInfo().getPath());
        respuesta.setEntity(Map.of(CLAVE_MENSAJE, texto));
    }
}
