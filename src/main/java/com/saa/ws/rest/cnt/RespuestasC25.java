package com.saa.ws.rest.cnt;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * C25 — Respuestas HTTP comunes del seguimiento de novedades: usuario inválido (U-2) y errores de negocio
 * ({@code IncomeException}) son 400 con el mensaje tal cual; lo inesperado, 500.
 */
final class RespuestasC25 {

	private RespuestasC25() {
	}

	static Response ok(Object cuerpo) {
		return Response.ok(cuerpo, MediaType.APPLICATION_JSON).build();
	}

	static Response error(Throwable e, String prefijo) {
		Throwable t = e;
		if (t instanceof jakarta.ejb.EJBException && t.getCause() != null) {
			t = t.getCause();
		}
		if (t instanceof com.saa.basico.util.UsuarioInvalidoException) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("No se puede procesar sin un usuario válido: " + ((com.saa.basico.util.UsuarioInvalidoException) t).getMotivo())
					.type(MediaType.APPLICATION_JSON).build();
		}
		boolean negocio = t instanceof com.saa.basico.util.IncomeException;
		return Response.status(negocio ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR)
				.entity(prefijo + t.getMessage()).type(MediaType.APPLICATION_JSON).build();
	}
}
