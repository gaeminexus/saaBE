package com.saa.ws.rest.tsr;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.service.IngresoService;
import com.saa.model.tsr.Ingreso;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST para Ingresos de tesorería sin documento físico (TSR.INGR).
 * Base path: /ingr
 *
 * Endpoints principales:
 *   POST /ingr/procesar        → registra un ingreso ya recibido: graba,
 *                                genera asiento y movimiento bancario en un paso
 *   POST /ingr/anular/{id}     → anula (reversa asiento y movimiento; requiere motivo)
 *   GET  /ingr/listar          → ingresos por empresa y estado
 *   GET  /ingr/getAll          → todos los ingresos
 *   GET  /ingr/getId/{id}      → ingreso por ID
 *   POST /ingr/selectByCriteria → búsqueda por criterios
 */
@Path("ingr")
public class IngresoRest {

    @EJB
    private IngresoService ingresoService;

    @Context
    private UriInfo context;

    public IngresoRest() {
    }

    /**
     * Registra un ingreso ya recibido en un solo paso.
     * Body esperado:
     * {
     *   "idEmpresa": 1,
     *   "idTitular": 25,               (opcional)
     *   "idProductoCobro": 12,         (producto CXC; su grupo da la cuenta contable)
     *   "descripcion": "Intereses ganados agosto",
     *   "valor": 45.10,
     *   "fecha": "2026-08-12",
     *   "idCuentaBancaria": 4,
     *   "referencia": "NC-BANCO-123",  (opcional)
     *   "observacion": "...",          (opcional)
     *   "idUsuario": 5
     * }
     */
    @POST
    @Path("/procesar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /ingr/procesar");
        try {
            Long idEmpresa     = toLong(datos.get("idEmpresa"));
            Long idTitular     = toLong(datos.get("idTitular"));
            Long idProducto    = toLong(datos.get("idProductoCobro"));
            String descripcion = (String) datos.get("descripcion");
            Double valor       = toDouble(datos.get("valor"));
            String fecha       = (String) datos.get("fecha");
            Long idCuenta      = toLong(datos.get("idCuentaBancaria"));
            String referencia  = (String) datos.get("referencia");
            String observacion = (String) datos.get("observacion");
            Long idUsuario     = toLong(datos.get("idUsuario"));

            if (idEmpresa == null || idProducto == null || valor == null || idCuenta == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa, idProductoCobro, valor e idCuentaBancaria.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = ingresoService.procesarIngreso(idEmpresa, idTitular,
                    idProducto, descripcion, valor, fecha, idCuenta, referencia, observacion,
                    idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al procesar el ingreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un ingreso: reversa el asiento y el movimiento bancario.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /ingr/anular/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la anulación.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = ingresoService.anularIngreso(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el ingreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de ingresos por empresa, opcionalmente por estado.
     * @param idEmpresa : Id de la empresa (obligatorio)
     * @param estado    : 1=Activo 2=Anulado (opcional)
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("estado") Long estado) {
        System.out.println("LLEGA AL SERVICIO GET /ingr/listar");
        try {
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            List<Ingreso> lista = ingresoService.listar(idEmpresa, estado);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar los ingresos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Ingreso> lista = ingresoService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los ingresos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Ingreso ingreso = ingresoService.selectById(id);
            if (ingreso == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Ingreso con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(ingreso)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el ingreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria Ingreso");
        try {
            return Response.status(Response.Status.OK)
                    .entity(ingresoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria Ingreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── Helpers de conversión del JSON ───────────────────────────────────────

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
