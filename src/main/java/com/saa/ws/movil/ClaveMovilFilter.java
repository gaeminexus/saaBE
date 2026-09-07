package com.saa.ws.movil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.saa.ws.movil.dto.MensajeMovilDTO;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Exige el header {@code X-Movil-Key} en cada request a {@code /movil}, comparado contra la
 * propiedad de sistema {@code saa.movil.key} (nunca commiteada, sin valor por defecto en el
 * código). Ver §3 de {@code CONTRATO-INTRANET-MOVIL.md}.
 *
 * <p><b>Falla cerrado, a propósito:</b> si la propiedad no está puesta, llega vacía, o no
 * coincide con el header, responde 401 y NO ejecuta el recurso. Un despliegue sin configurar la
 * propiedad deja el móvil caído — es la alternativa correcta frente a arrancar abierto.</p>
 *
 * <p>El 401 sale sin cuerpo descriptivo: no dice si la clave falta, es corta o no coincide. La
 * comparación es en tiempo constante ({@link MessageDigest#isEqual}), igual que la verificación
 * de clave de {@code UsuarioAppServiceImpl}.</p>
 *
 * <p>Esto es defensa en profundidad, no la defensa principal: la principal es el firewall (el
 * WildFly de SaaBE solo acepta 8080 desde el WAR de borde y la intranet).</p>
 *
 * <p>⚠️ Anotado {@link ClaveMovilRequerida}, NO solo {@link Provider} — ver el JavaDoc de esa
 * anotación para el porqué (Trampa 2 del contrato).</p>
 */
@ClaveMovilRequerida
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ClaveMovilFilter implements ContainerRequestFilter {

    private static final String HEADER_CLAVE = "X-Movil-Key";
    private static final String PROPIEDAD_CLAVE = "saa.movil.key";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String claveConfigurada = System.getProperty(PROPIEDAD_CLAVE);
        String claveRecibida = requestContext.getHeaderString(HEADER_CLAVE);

        if (!clavesCoinciden(claveConfigurada, claveRecibida)) {
            MensajeMovilDTO cuerpo = new MensajeMovilDTO("No autorizado");
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity(cuerpo)
                            .type(MediaType.APPLICATION_JSON)
                            .build());
        }
    }

    /**
     * Falla cerrado: {@code null}/vacía en cualquiera de los dos lados nunca "coincide", ni
     * siquiera si las dos están vacías. Comparación en tiempo constante sobre los bytes UTF-8.
     */
    private boolean clavesCoinciden(String claveConfigurada, String claveRecibida) {
        if (claveConfigurada == null || claveConfigurada.isEmpty()
                || claveRecibida == null || claveRecibida.isEmpty()) {
            return false;
        }
        byte[] esperado = claveConfigurada.getBytes(StandardCharsets.UTF_8);
        byte[] recibido = claveRecibida.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, recibido);
    }
}
