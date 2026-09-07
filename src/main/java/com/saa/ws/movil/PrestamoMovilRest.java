package com.saa.ws.movil;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.model.crd.DetallePrestamo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;
import com.saa.ws.movil.dto.CuotaPrestamoMovilDTO;
import com.saa.ws.movil.dto.MensajeMovilDTO;
import com.saa.ws.movil.dto.PrestamoMovilDTO;

import jakarta.ejb.EJB;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/prestamos/...} — §5.2 del contrato. Todo {@code GET}, todo recortado a
 * {@link PrestamoMovilDTO}/{@link CuotaPrestamoMovilDTO}: nunca se serializa {@code Prestamo}
 * directo (arrastraría {@code Entidad} completa con datos personales, ver §4.2 del contrato).
 */
@ClaveMovilRequerida
@Path("prestamos")
public class PrestamoMovilRest {

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    /** GET /movil/prestamos/{idEntidad} — todos los préstamos del partícipe, en cualquier estado. */
    @GET
    @Path("/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET prestamos/{idEntidad} - MOVIL - idEntidad: " + idEntidad);
        try {
            List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(idEntidad);
            List<PrestamoMovilDTO> dtos = new ArrayList<>();
            for (Prestamo prestamo : prestamos) {
                dtos.add(MovilMappers.aDTO(prestamo));
            }
            return Response.status(Response.Status.OK).entity(dtos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorInterno("Error al obtener los préstamos del partícipe");
        }
    }

    /** GET /movil/prestamos/{idEntidad}/{idPrestamo} — un préstamo puntual, validando pertenencia. */
    @GET
    @Path("/{idEntidad}/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porId(@PathParam("idEntidad") Long idEntidad, @PathParam("idPrestamo") Long idPrestamo) {
        System.out.println("LLEGA AL SERVICIO GET prestamos/{idEntidad}/{idPrestamo} - MOVIL - idEntidad: "
                + idEntidad + " - idPrestamo: " + idPrestamo);
        try {
            Prestamo prestamo = buscarDeEntidad(idEntidad, idPrestamo);
            if (prestamo == null) {
                return noEncontrado();
            }
            return Response.status(Response.Status.OK).entity(MovilMappers.aDTO(prestamo)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorInterno("Error al obtener el préstamo");
        }
    }

    /** GET /movil/prestamos/{idEntidad}/{idPrestamo}/cuotas — tabla de amortización, validando pertenencia. */
    @GET
    @Path("/{idEntidad}/{idPrestamo}/cuotas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cuotas(@PathParam("idEntidad") Long idEntidad, @PathParam("idPrestamo") Long idPrestamo) {
        System.out.println("LLEGA AL SERVICIO GET prestamos/{idEntidad}/{idPrestamo}/cuotas - MOVIL - idEntidad: "
                + idEntidad + " - idPrestamo: " + idPrestamo);
        try {
            Prestamo prestamo = buscarDeEntidad(idEntidad, idPrestamo);
            if (prestamo == null) {
                return noEncontrado();
            }
            List<DetallePrestamo> cuotas = detallePrestamoDaoService.selectByPrestamo(idPrestamo);
            List<CuotaPrestamoMovilDTO> dtos = new ArrayList<>();
            for (DetallePrestamo cuota : cuotas) {
                dtos.add(MovilMappers.aDTO(cuota));
            }
            return Response.status(Response.Status.OK).entity(dtos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorInterno("Error al obtener las cuotas del préstamo");
        }
    }

    /**
     * Busca el préstamo por id y valida pertenencia al {@code idEntidad} del path — 404, no 403,
     * si no es de esa entidad (§4.4 del contrato: no confirmar que el id existe).
     */
    private Prestamo buscarDeEntidad(Long idEntidad, Long idPrestamo) throws Throwable {
        Prestamo prestamo;
        try {
            prestamo = prestamoDaoService.selectById(idPrestamo, NombreEntidadesCredito.PRESTAMO);
        } catch (NoResultException e) {
            return null;
        }
        if (prestamo.getEntidad() == null || !prestamo.getEntidad().getCodigo().equals(idEntidad)) {
            return null;
        }
        return prestamo;
    }

    private Response noEncontrado() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new MensajeMovilDTO("No existe el préstamo indicado"))
                .type(MediaType.APPLICATION_JSON).build();
    }

    private Response errorInterno(String texto) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new MensajeMovilDTO(texto))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
