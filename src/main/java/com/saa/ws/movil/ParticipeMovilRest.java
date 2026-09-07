package com.saa.ws.movil;

import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.ejb.crd.dao.PersonaNaturalDaoService;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.PersonaNatural;
import com.saa.ws.movil.dto.MensajeMovilDTO;
import com.saa.ws.movil.dto.ParticipeMovilDTO;

import jakarta.ejb.EJB;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/participe/{idEntidad}} — §5.2 del contrato. Perfil consolidado del partícipe,
 * recortado a identificación/nombres/apellidos/contacto. Se apoya en {@code entd/getId} +
 * {@link ParticipeDaoService#selectByEntidad(Long)}.
 */
@ClaveMovilRequerida
@Path("participe")
public class ParticipeMovilRest {

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private ParticipeDaoService participeDaoService;

    @EJB
    private PersonaNaturalDaoService personaNaturalDaoService;

    @GET
    @Path("/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET participe/{idEntidad} - MOVIL - idEntidad: " + idEntidad);
        try {
            Entidad entidad;
            try {
                entidad = entidadDaoService.selectById(idEntidad, NombreEntidadesCredito.ENTIDAD);
            } catch (NoResultException e) {
                return noEncontrado();
            }

            // Confirma que la entidad tiene rol de partícipe (CRD.PRTC); si no, no hay perfil que mostrar.
            if (participeDaoService.selectByEntidad(idEntidad).isEmpty()) {
                return noEncontrado();
            }

            ParticipeMovilDTO dto = new ParticipeMovilDTO();
            dto.setIdEntidad(entidad.getCodigo());
            dto.setIdentificacion(entidad.getNumeroIdentificacion());
            dto.setCorreoPersonal(entidad.getCorreoPersonal());
            dto.setCorreoInstitucional(entidad.getCorreoInstitucional());
            dto.setTelefono(entidad.getTelefono());
            dto.setMovil(entidad.getMovil());

            // Nombres/apellidos viven en CRD.PRSN, que comparte PK con CRD.ENTD — no toda Entidad
            // tiene fila PersonaNatural (persona jurídica); ausencia no es error, viajan null.
            try {
                PersonaNatural persona = personaNaturalDaoService.selectById(idEntidad,
                        NombreEntidadesCredito.PERSONA_NATURAL);
                dto.setNombres(persona.getNombres());
                dto.setApellidos(persona.getApellidos());
            } catch (NoResultException e) {
                System.out.println("ParticipeMovilRest.porEntidad - entidad " + idEntidad
                        + " sin PersonaNatural asociada");
            }

            return Response.status(Response.Status.OK).entity(dto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MensajeMovilDTO("Error al obtener el perfil del partícipe"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Response noEncontrado() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new MensajeMovilDTO("No existe el partícipe indicado"))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
