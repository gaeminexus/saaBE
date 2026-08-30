package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.dao.AcuerdoCondonacionDaoService;
import com.saa.ejb.crd.dao.DetalleAcuerdoCondonacionDaoService;
import com.saa.ejb.crd.service.AcuerdoCondonacionService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.DesgloseConceptosPrestamo;
import com.saa.ejb.crd.service.dto.SolicitudRegistroAcuerdo;
import com.saa.model.crd.AcuerdoCondonacion;
import com.saa.model.crd.DetalleAcuerdoCondonacion;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST de acuerdos de pago con condonación (Frente K, CRD.ACCN/DACC). Ver
 * {@code docs/logica-negocio/crd/PLAN-ACUERDOS-PAGO-CONDONACION.md} y
 * {@link AcuerdoCondonacionService}.
 *
 * @author Sistema SAA
 * @since 2026-08-29
 */
@Path("accn")
public class AcuerdoCondonacionRest {

    @EJB
    private AcuerdoCondonacionDaoService acuerdoCondonacionDaoService;

    @EJB
    private DetalleAcuerdoCondonacionDaoService detalleAcuerdoCondonacionDaoService;

    @EJB
    private AcuerdoCondonacionService acuerdoCondonacionService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    public AcuerdoCondonacionRest() {
    }

    /**
     * GET /rest/accn/previsualizar/{idPrestamo}?fecha=yyyy-MM-dd — el desglose por concepto
     * que la pantalla muestra ANTES de confirmar. Desde el 2026-08-30 (K4 derogada) esto ES
     * el control: no hay aprobación de condonación después, así que la pantalla tiene que
     * mostrar exactamente lo que {@code registrarAcuerdo} va a validar. Solo lectura, no
     * registra nada.
     *
     * @param idPrestamo : Préstamo a previsualizar
     * @param fecha      : Fecha de corte; si no viene, hoy
     */
    @GET
    @Path("/previsualizar/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previsualizar(@PathParam("idPrestamo") Long idPrestamo,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO GET previsualizar - ACCN - idPrestamo: " + idPrestamo);
        try {
            LocalDate fechaCorte = fecha != null && !fecha.trim().isEmpty() ? LocalDate.parse(fecha) : null;
            DesgloseConceptosPrestamo desglose =
                    procesoPagoPrestamoService.calcularDesgloseConceptos(idPrestamo, fechaCorte);
            return Response.status(Response.Status.OK).entity(desglose).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al previsualizar el préstamo " + idPrestamo + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AcuerdoCondonacion> lista = acuerdoCondonacionDaoService.selectAll(
                    NombreEntidadesCredito.ACUERDO_CONDONACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener acuerdos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AcuerdoCondonacion acuerdo = acuerdoCondonacionDaoService.selectById(id,
                    NombreEntidadesCredito.ACUERDO_CONDONACION);
            List<DetalleAcuerdoCondonacion> detalles = detalleAcuerdoCondonacionDaoService.selectByAcuerdo(id);
            return Response.status(Response.Status.OK)
                    .entity(new Object() {
                        public final AcuerdoCondonacion cabecera = acuerdo;
                        public final List<DetalleAcuerdoCondonacion> detalle = detalles;
                    })
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el acuerdo " + id + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/accn/bandeja/{estado} — acuerdos por estado (VIGENTE/APLICADO/ANULADO). Ya no
     * es una bandeja de aprobación (K4 derogada) — es la consulta que sostiene que la tabla
     * sea consultable de verdad (consecuencia de K6, §1 del plan): con el préstamo CANCELADO
     * indistinguible de uno pagado normal, esta es la fuente de "cuánto se condonó, a quién".
     * Ver {@link com.saa.rubros.CrdEstadoAcuerdoCondonacion}.
     */
    @GET
    @Path("/bandeja/{estado}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bandeja(@PathParam("estado") Long estado) {
        try {
            List<AcuerdoCondonacion> lista = acuerdoCondonacionDaoService.selectByEstado(estado);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la bandeja: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * GET /rest/accn/porPrestamo/{idPrestamo} — única fuente para saber si un préstamo tuvo
     * un acuerdo (K6: un préstamo condonado queda CANCELADO, indistinguible de uno pagado normal).
     */
    @GET
    @Path("/porPrestamo/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPrestamo(@PathParam("idPrestamo") Long idPrestamo) {
        try {
            List<AcuerdoCondonacion> lista = acuerdoCondonacionDaoService.selectByPrestamo(idPrestamo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los acuerdos del préstamo " + idPrestamo + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        try {
            List<AcuerdoCondonacion> lista = acuerdoCondonacionDaoService.selectByEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los acuerdos de la entidad " + idEntidad + ": " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST /rest/accn/registrar — paso 1. Ver {@link AcuerdoCondonacionService#registrarAcuerdo}.
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(SolicitudRegistroAcuerdo solicitud) {
        System.out.println("LLEGA AL SERVICIO POST registrar - ACCN - idPrestamo: "
                + (solicitud != null ? solicitud.getIdPrestamo() : null));
        try {
            AcuerdoCondonacion acuerdo = acuerdoCondonacionService.registrarAcuerdo(solicitud);
            return Response.status(Response.Status.CREATED).entity(acuerdo)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el acuerdo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ⚠️ NO hay /aprobar ni /rechazar (K4/K10 derogadas el 2026-08-30): el acuerdo nace ya
    // decidido en /registrar. La anulación tampoco tiene REST propio acá — es una cascada de
    // CobroCreditoService#anularCobro cuando el cobro es de tipo ACUERDO_CONDONACION (ver
    // AcuerdoCondonacionService#anularAcuerdoPorCobro).
}
