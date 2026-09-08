package com.saa.ws.rest.rhh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.rhh.dao.ValorNoPagadoDaoService;
import com.saa.ejb.rhh.service.ValorNoPagadoService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ValorNoPagado;

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
 * REST para valores no pagados en nomina (RHH.VNPG). Base path: /vnpg.
 *
 * <p>Contrato: docs/logica-negocio/rhh/PLAN-VALORES-NO-PAGADOS.md §10. Los seis endpoints
 * estandar mas {@code /vnpg/registrar}, {@code /vnpg/anular/{id}} y {@code /vnpg/listar}.
 * {@code DELETE} esta deshabilitado (405): el registro nunca se borra fisicamente, solo se
 * anula con motivo (§8 del plan).</p>
 */
@Path("vnpg")
public class ValorNoPagadoRest {

    @EJB
    private ValorNoPagadoDaoService valorNoPagadoDaoService;

    @EJB
    private ValorNoPagadoService valorNoPagadoService;

    public ValorNoPagadoRest() {
    }

    // =====================================================================
    // Los seis endpoints estandar
    // =====================================================================

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ValorNoPagado> lista = valorNoPagadoDaoService.selectAll(NombreEntidadesRhh.VALOR_NO_PAGADO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener ValorNoPagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ValorNoPagado entidad = valorNoPagadoDaoService.selectById(id, NombreEntidadesRhh.VALOR_NO_PAGADO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ValorNoPagado con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener ValorNoPagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ValorNoPagado registro) {
        try {
            ValorNoPagado resultado = valorNoPagadoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear ValorNoPagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ValorNoPagado registro) {
        try {
            ValorNoPagado resultado = valorNoPagadoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar ValorNoPagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Deshabilitado: ValorNoPagado nunca se borra fisicamente (§8 del plan). Use
     * {@code POST /vnpg/anular/{id}} con un motivo.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        Map<String, Object> cuerpo = new LinkedHashMap<String, Object>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("mensaje", "ValorNoPagado no se elimina fisicamente. Use POST /vnpg/anular/" + id
                + " con un motivo.");
        return Response.status(405).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<ValorNoPagado> result = valorNoPagadoService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(result).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria ValorNoPagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Procesos
    // =====================================================================

    /**
     * Registra un valor no pagado. Body: {idEmpresa, idEmpleado, idPeriodo, valor, motivo,
     * usuario}. Ver contrato §10 / §8.
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /vnpg/registrar");
        try {
            Long idEmpresa = toLong(datos.get("idEmpresa"));
            Long idEmpleado = toLong(datos.get("idEmpleado"));
            Long idPeriodo = toLong(datos.get("idPeriodo"));
            Double valor = toDouble(datos.get("valor"));
            String motivo = (String) datos.get("motivo");
            String usuario = (String) datos.get("usuario");

            Map<String, Object> resultado =
                    valorNoPagadoService.registrar(idEmpresa, idEmpleado, idPeriodo, valor, motivo, usuario);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en registrar valor no pagado: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar el valor no pagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un registro. Body: {motivo, usuario}. Solo desde REGISTRADO — ver contrato §10.
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /vnpg/anular/" + id);
        try {
            String motivo = datos != null ? (String) datos.get("motivo") : null;
            String usuario = datos != null ? (String) datos.get("usuario") : null;

            Map<String, Object> resultado = valorNoPagadoService.anular(id, motivo, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return respuestaConflicto(e.getMessage());
        } catch (Throwable e) {
            System.err.println("ERROR en anular valor no pagado: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el valor no pagado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado con filtros de servidor. Query params: idEmpresa (obligatorio), idPeriodo,
     * idEmpleado, estado (repetible) — ver contrato §10.
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("idPeriodo") Long idPeriodo,
            @QueryParam("idEmpleado") Long idEmpleado,
            @QueryParam("estado") List<Long> estados) {
        System.out.println("LLEGA AL SERVICIO GET /vnpg/listar");
        if (idEmpresa == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Debe indicar idEmpresa.")
                    .type(MediaType.APPLICATION_JSON).build();
        }
        try {
            List<ValorNoPagado> resultado =
                    valorNoPagadoService.listar(idEmpresa, idPeriodo, idEmpleado, estados);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar valores no pagados: " + e.getMessage())
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

    private Double toDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).doubleValue();
        try {
            return Double.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
