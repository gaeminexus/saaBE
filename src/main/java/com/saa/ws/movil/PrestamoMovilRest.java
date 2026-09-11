package com.saa.ws.movil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.dao.DetallePrestamoDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.dto.SaldoPrestamoResumen;
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

    /** Máximo de códigos por llamada a {@code calcularSaldosEnLote} (contrato del motor). */
    private static final int MAX_LOTE_SALDOS = 500;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private DetallePrestamoDaoService detallePrestamoDaoService;

    @EJB
    private PrestamoService prestamoService;

    /** GET /movil/prestamos/{idEntidad} — todos los préstamos del partícipe, en cualquier estado. */
    @GET
    @Path("/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET prestamos/{idEntidad} - MOVIL - idEntidad: " + idEntidad);
        try {
            List<Prestamo> prestamos = prestamoDaoService.selectByEntidad(idEntidad);
            List<Long> codigos = new ArrayList<>();
            for (Prestamo prestamo : prestamos) {
                codigos.add(prestamo.getCodigo());
            }
            Map<Long, SaldoPrestamoResumen> saldos = calcularSaldos(codigos);
            List<PrestamoMovilDTO> dtos = new ArrayList<>();
            for (Prestamo prestamo : prestamos) {
                dtos.add(MovilMappers.aDTO(prestamo, saldos.get(prestamo.getCodigo())));
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
            Map<Long, SaldoPrestamoResumen> saldos = calcularSaldos(Collections.singletonList(prestamo.getCodigo()));
            return Response.status(Response.Status.OK)
                    .entity(MovilMappers.aDTO(prestamo, saldos.get(prestamo.getCodigo())))
                    .type(MediaType.APPLICATION_JSON).build();
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
     * Saldos reales del motor para el lote de códigos, indexados por {@code idPrestamo}
     * (CONTRATO-INTRANET-MOVIL.md §8). {@code calcularSaldosEnLote} lanza {@code IncomeException}
     * con lista vacía o con más de {@link #MAX_LOTE_SALDOS} códigos — las dos guardas de acá evitan
     * pisar esas dos condiciones en vez de confiar en que nunca ocurran.
     */
    private Map<Long, SaldoPrestamoResumen> calcularSaldos(List<Long> codigos) throws Throwable {
        Map<Long, SaldoPrestamoResumen> porId = new HashMap<>();
        if (codigos.isEmpty()) {
            return porId;
        }
        for (int inicio = 0; inicio < codigos.size(); inicio += MAX_LOTE_SALDOS) {
            List<Long> lote = codigos.subList(inicio, Math.min(inicio + MAX_LOTE_SALDOS, codigos.size()));
            for (SaldoPrestamoResumen resumen : prestamoService.calcularSaldosEnLote(lote)) {
                porId.put(resumen.getIdPrestamo(), resumen);
            }
        }
        return porId;
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
