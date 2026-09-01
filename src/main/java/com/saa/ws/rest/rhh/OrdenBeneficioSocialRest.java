package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.OrdenBeneficioSocialDaoService;
import com.saa.ejb.rhh.service.OrdenBeneficioSocialService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.OrdenBeneficioSocial;
import com.saa.model.rhh.OrdenBeneficioSocialResumen;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para la orden de pago de beneficio social (decimo acumulado, fondos de reserva). RHH.
 * Base path: /odbs.
 *
 * <p>Contrato: docs/logica-negocio/rhh/API-PAGO-BENEFICIOS-SOCIALES.md. Los seis endpoints
 * estandar (#1.1) mas los cinco procesos (#1.2 a #1.6).</p>
 *
 * <p><b>Códigos de estado, tal como los fija el contrato:</b> {@code generar} devuelve 200
 * tanto si crea la orden como si no hay liquidaciones pendientes ({@code exito:false} en el
 * cuerpo), y 409 solo cuando ya existe una orden viva para la misma combinación
 * (el cuerpo trae {@code idOrdenExistente}). {@code detalle} siempre devuelve 200, con
 * {@code exito:false} si el id no existe. {@code enviarATesoreria}, {@code confirmarPago} y
 * {@code anular} devuelven 409 ante cualquier conflicto de estado — el service los señala
 * lanzando {@code IncomeException}, que este REST traduce a 409 con
 * {@code {exito:false, mensaje}}. Cualquier otro error es 500, texto plano (estilo de la
 * casa).</p>
 */
@Path("odbs")
public class OrdenBeneficioSocialRest {

    @EJB
    private OrdenBeneficioSocialDaoService ordenBeneficioSocialDaoService;

    @EJB
    private OrdenBeneficioSocialService ordenBeneficioSocialService;

    public OrdenBeneficioSocialRest() {
    }

    // =====================================================================
    // Los seis endpoints estandar (#1.1)
    // =====================================================================

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<OrdenBeneficioSocial> lista =
                    ordenBeneficioSocialDaoService.selectAll(NombreEntidadesRhh.ORDEN_BENEFICIO_SOCIAL);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            OrdenBeneficioSocial entidad =
                    ordenBeneficioSocialDaoService.selectById(id, NombreEntidadesRhh.ORDEN_BENEFICIO_SOCIAL);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("OrdenBeneficioSocial con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(OrdenBeneficioSocial registro) {
        try {
            OrdenBeneficioSocial resultado = ordenBeneficioSocialService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(OrdenBeneficioSocial registro) {
        try {
            OrdenBeneficioSocial resultado = ordenBeneficioSocialService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            List<Long> ids = new java.util.ArrayList<>();
            ids.add(id);
            ordenBeneficioSocialService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("OrdenBeneficioSocial eliminada correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<OrdenBeneficioSocial> result = ordenBeneficioSocialService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria OrdenBeneficioSocial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Procesos (#1.2 a #1.6)
    // =====================================================================

    /**
     * Agrupa las liquidaciones sueltas y arma la cabecera. Body: ver contrato #1.2.
     */
    @POST
    @Path("/generar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /odbs/generar");
        try {
            Long idEmpresa = toLong(datos.get("idEmpresa"));
            Long tipoBeneficio = toLong(datos.get("tipoBeneficio"));
            Integer anio = toInteger(datos.get("anio"));
            Long region = toLong(datos.get("region"));
            String usuario = (String) datos.get("usuario");

            Map<String, Object> resultado =
                    ordenBeneficioSocialService.generar(idEmpresa, tipoBeneficio, anio, region, usuario);

            // 409 solo cuando ya existe una orden viva (el cuerpo trae idOrdenExistente);
            // el otro caso de exito:false (sin liquidaciones pendientes) es 200 (contrato #1.2).
            Response.Status status = resultado.containsKey("idOrdenExistente")
                    ? Response.Status.CONFLICT : Response.Status.OK;
            return Response.status(status).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en generar orden de beneficio social: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar la orden: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Detalle de una orden: cabecera mas las liquidaciones agrupadas. Ver contrato #1.3.
     * Siempre 200; {@code exito:false} en el cuerpo si el id no existe.
     */
    @GET
    @Path("/detalle/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response detalle(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET /odbs/detalle/" + id);
        try {
            Map<String, Object> resultado = ordenBeneficioSocialService.detalle(id);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al consultar el detalle de la orden: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Bandeja de ordenes. Ver contrato #1.3bis. Query params: idEmpresa (obligatorio),
     * anio, tipoBeneficio, estado (opcionales).
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("anio") Integer anio,
            @QueryParam("tipoBeneficio") Long tipoBeneficio,
            @QueryParam("estado") Long estado) {
        System.out.println("LLEGA AL SERVICIO GET /odbs/listar");
        if (idEmpresa == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe indicar idEmpresa.")
                    .type(MediaType.APPLICATION_JSON).build();
        }
        try {
            List<OrdenBeneficioSocialResumen> resultado =
                    ordenBeneficioSocialService.listar(idEmpresa, anio, tipoBeneficio, estado);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar ordenes de beneficio social: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra el pago en la bandeja de tesoreria. Body: {idUsuario, observacion} — ver
     * contrato #1.4. {@code idUsuario} es numerico, FK real: nunca se resuelve por nombre.
     */
    @POST
    @Path("/enviarATesoreria/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enviarATesoreria(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /odbs/enviarATesoreria/" + id);
        try {
            Long idUsuario = toLong(datos != null ? datos.get("idUsuario") : null);
            String observacion = datos != null ? (String) datos.get("observacion") : null;

            Map<String, Object> resultado =
                    ordenBeneficioSocialService.enviarATesoreria(id, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en enviarATesoreria: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al enviar la orden a tesoreria: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Cierra el ciclo y contabiliza la baja de provision. Body: {fechaPago, usuario} — ver
     * contrato #1.5. {@code fechaPago} es {@code LocalDate}: {@code yyyy-MM-dd}.
     */
    @POST
    @Path("/confirmarPago/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarPago(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /odbs/confirmarPago/" + id);
        try {
            String fechaPagoTexto = datos != null ? (String) datos.get("fechaPago") : null;
            String usuario = datos != null ? (String) datos.get("usuario") : null;
            LocalDate fechaPago = (fechaPagoTexto != null && !fechaPagoTexto.trim().isEmpty())
                    ? LocalDate.parse(fechaPagoTexto.trim()) : null;

            Map<String, Object> resultado = ordenBeneficioSocialService.confirmarPago(id, fechaPago, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en confirmarPago: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al confirmar el pago: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula la orden y desenlaza las liquidaciones. Body: {motivo, usuario} — ver contrato
     * #1.6. {@code motivo} es obligatorio.
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /odbs/anular/" + id);
        try {
            String motivo = datos != null ? (String) datos.get("motivo") : null;
            String usuario = datos != null ? (String) datos.get("usuario") : null;

            Map<String, Object> resultado = ordenBeneficioSocialService.anular(id, motivo, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en anular orden de beneficio social: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la orden: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Response respuestaConflicto(String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<String, Object>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("mensaje", mensaje);
        return Response.status(Response.Status.CONFLICT).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInteger(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).intValue();
        try {
            return Integer.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
