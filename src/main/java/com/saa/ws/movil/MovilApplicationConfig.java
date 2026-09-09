package com.saa.ws.movil;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Application path {@code /movil}: la lista blanca cerrada que consume el WAR de borde
 * (SaaMovilBE). Ver {@code docs/logica-negocio/crd/CONTRATO-INTRANET-MOVIL.md}.
 *
 * <p><b>⛔ TRAMPA 1 — NO dejar {@link #getClasses()} sin sobreescribir.</b> A diferencia de
 * {@link com.saa.ws.rest.ApplicationConfig} (que SÍ queda vacía a propósito para servir todo
 * {@code /rest}), esta Application NO puede quedar vacía: una {@code Application} vacía en este
 * deployment toma TODOS los {@code @Path} del WAR (~200 recursos, DELETE incluidos), lo que
 * volvería a {@code /movil} un segundo camino a todo en vez de una lista blanca. El conjunto de
 * abajo ES la lista blanca — revisarlo es la revisión de seguridad de este frente.</p>
 *
 * <p>Verificación antes de cada despliegue (§6 del contrato): esta lista no debe estar vacía ni
 * ser {@code null}, y cada clase debe ser del paquete {@code com.saa.ws.movil}.</p>
 */
@ApplicationPath("/movil")
public class MovilApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> clases = new HashSet<>();

        // Filtro de la clave compartida (@ClaveMovilRequerida) - ver ClaveMovilFilter.
        clases.add(ClaveMovilFilter.class);

        // Recursos de la lista blanca (§5 del contrato). Todos anotados @ClaveMovilRequerida.
        clases.add(AuthMovilRest.class);
        clases.add(ParticipeMovilRest.class);
        clases.add(PrestamoMovilRest.class);
        clases.add(AporteMovilRest.class);
        clases.add(CuentaIndividualMovilRest.class);
        clases.add(SimuladorMovilRest.class);
        clases.add(EstadoMovilRest.class);

        return clases;
    }
}
