package com.saa.ws.movil;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Marca un recurso (o método) de {@code /movil} como protegido por {@link ClaveMovilFilter}.
 *
 * <p><b>⛔ TRAMPA 2 — por qué esto NO es un {@code @Provider} suelto.</b>
 * {@link com.saa.ws.rest.ApplicationConfig} (path {@code /rest}) queda vacía a propósito, y una
 * {@code Application} vacía en este deployment auto-descubre TODOS los {@code @Provider} del WAR,
 * no solo los de su propio path. Si {@link ClaveMovilFilter} fuera un
 * {@code ContainerRequestFilter} anotado solo {@code @Provider}, {@code /rest} lo recogería
 * también y le pediría la clave compartida a todo el SaaFE — rompiendo la intranet entera.</p>
 *
 * <p>Con {@code @NameBinding}, el filtro solo se ejecuta sobre los recursos que llevan ESTA
 * anotación — la protección viaja con el recurso, no con el {@code Application} que lo sirve, así
 * que da igual que {@code /rest} también termine registrando la clase del filtro. Se pone en
 * {@link ClaveMovilFilter} y en cada clase de recurso de {@code /movil}.</p>
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ClaveMovilRequerida {
}
