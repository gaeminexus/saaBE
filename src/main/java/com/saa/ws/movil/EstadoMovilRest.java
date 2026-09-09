package com.saa.ws.movil;

import com.saa.ws.movil.dto.MensajeMovilDTO;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code GET /movil/estado} — semáforo de la clave compartida {@code saa.movil.key}
 * (§3.2 de {@code ORDEN-CIERRE-CADENA-2026-09-07.md}). Sin este endpoint, en un
 * despliegue nuevo no hay forma de distinguir "la clave está mal puesta" de "el
 * endpoint falló" sin pegarle a un recurso de datos con un {@code idEntidad} real.
 *
 * No consulta la base ni inyecta ningún DAO: es puro semáforo de
 * {@link ClaveMovilFilter}. Si el filtro deja pasar el request, este método corre y
 * devuelve un {@link MensajeMovilDTO} fijo — nunca versión, build, nombre/IP del
 * servidor, ni si Oracle responde. Quien tiene la clave ya sabe que el path existe;
 * quien no la tiene recibe el mismo 401 genérico que el resto de {@code /movil}.
 */
@ClaveMovilRequerida
@Path("estado")
public class EstadoMovilRest {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response estado() {
        System.out.println("LLEGA AL SERVICIO GET estado - MOVIL");
        return Response.status(Response.Status.OK)
                .entity(new MensajeMovilDTO("Clave movil valida"))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
