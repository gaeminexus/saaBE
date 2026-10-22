package com.saa.ws.movil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.dao.EntidadDaoService;
import com.saa.ejb.crd.dao.EstadoParticipeDaoService;
import com.saa.ejb.crd.dao.ParticipeDaoService;
import com.saa.model.crd.Entidad;
import com.saa.model.crd.EstadoParticipe;
import com.saa.model.crd.NombreEntidadesCredito;
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
    private EstadoParticipeDaoService estadoParticipeDaoService;

    @EJB
    private AporteDaoService aporteDaoService;

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

            // El nombre del partícipe vive en CRD.ENTD.ENTDRZNS (razonSocial), no en CRD.PRSN:
            // PRSN no es la fuente de datos del partícipe (ver CONTRATO-INTRANET-MOVIL.md).
            // Antes se consultaba PersonaNatural acá — se sacó porque, además de ser la tabla
            // equivocada, PRSN.PRSNESCV (estado civil) está mapeado como Long pero la columna
            // real es VARCHAR2(2000) (texto: "CASADO(A)"), y esa consulta reventaba con
            // "Could not extract column [4]" / T4CVarcharAccessor.getLong en cualquier
            // partícipe con estado civil cargado. Ese desajuste es de otro equipo (crd) y no
            // se toca; acá alcanza con no depender de esa tabla. razonSocial es un campo único
            // (nombre completo), por eso apellidos viaja en null — partirlo por espacios
            // inventaría dónde terminan los apellidos.
            dto.setNombres(entidad.getRazonSocial());
            dto.setApellidos(null);

            // ENTDIDST guarda el código ALTERNO (ESPRCDEX) del catálogo CRD.ESPR, NO su PK
            // (MIGRACION-ESTADO-PARTICIPE.md; mismo criterio que EntidadDaoServiceImpl y
            // CertificadoServiceImpl). Un código fuera del catálogo sale null, no un texto inventado.
            dto.setIdEstadoParticipe(entidad.getIdEstado());
            dto.setEstadoParticipe(nombreEstadoParticipe(entidad.getIdEstado()));
            dto.setFechaPrimerAporte(fechaPrimerAporte(idEntidad));

            return Response.status(Response.Status.OK).entity(dto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MensajeMovilDTO("Error al obtener el perfil del partícipe"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Catálogo CRD.ESPR (acotado, ~9 filas): se busca por código alterno ESPRCDEX. */
    private String nombreEstadoParticipe(Long codigoAlterno) throws Throwable {
        if (codigoAlterno == null) {
            return null;
        }
        List<EstadoParticipe> estados = estadoParticipeDaoService.selectAll(NombreEntidadesCredito.ESTADO_PARTICIPE);
        if (estados != null) {
            for (EstadoParticipe estado : estados) {
                if (codigoAlterno.equals(estado.getCodigoExterno())) {
                    return estado.getNombre();
                }
            }
        }
        return null;
    }

    /**
     * Fecha (APRTFCTR) del aporte más antiguo de la entidad. Reusa la MISMA consulta que alimenta
     * {@code /movil/aportes/.../movimientos} ({@code selectMovimientosPorEntidadYRangoDevengo}), con
     * un rango que lo abarca todo: esa consulta filtra solo por entidad (más el rango de devengo,
     * incluyendo periodo nulo), sin excluir anulados ni reversados. Si falla, devuelve null: el
     * perfil no debe caerse por un dato decorativo.
     */
    private LocalDateTime fechaPrimerAporte(Long idEntidad) {
        try {
            List<Object[]> filas = aporteDaoService.selectMovimientosPorEntidadYRangoDevengo(
                    idEntidad, LocalDate.of(1900, 1, 1), LocalDate.of(2999, 12, 1));
            LocalDateTime primera = null;
            if (filas != null) {
                for (Object[] fila : filas) {
                    LocalDateTime fecha = (LocalDateTime) fila[3];
                    if (fecha != null && (primera == null || fecha.isBefore(primera))) {
                        primera = fecha;
                    }
                }
            }
            return primera;
        } catch (Throwable e) {
            System.err.println("ParticipeMovilRest.fechaPrimerAporte - entidad " + idEntidad + ": "
                    + e.getClass().getName() + ": " + e.getMessage());
            return null;
        }
    }

    private Response noEncontrado() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new MensajeMovilDTO("No existe el partícipe indicado"))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
