package com.saa.ws.rest.tsr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.CierreCajaChicaDaoService;
import com.saa.ejb.tsr.service.CierreCajaChicaService;
import com.saa.model.tsr.CierreCajaChica;
import com.saa.model.tsr.NombreEntidadesTesoreria;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST para CierreCajaChica (TSR.CRCH). Base path: /crch
 *
 * Endpoints principales:
 *   POST /crch/preparar          → prepara un cierre en BORRADOR con los totales del periodo
 *   POST /crch/confirmar/{id}    → confirma el cierre con el saldo físico (genera ajuste si hay diferencia)
 *   POST /crch/anular/{id}       → anula el último cierre CERRADO
 *   GET  /crch/listar/{idCaja}   → cierres de una caja
 *   GET  /crch/movimientos/{idCierre} → movimientos incluidos en un cierre
 */
@Path("crch")
public class CierreCajaChicaRest {

    @EJB
    private CierreCajaChicaDaoService cierreCajaChicaDaoService;

    @EJB
    private CierreCajaChicaService cierreCajaChicaService;

    @Context
    private UriInfo context;

    public CierreCajaChicaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CierreCajaChica> lista =
                    cierreCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.CIERRE_CAJA_CHICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cierres de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CierreCajaChica entidad =
                    cierreCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.CIERRE_CAJA_CHICA);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Cierre de caja chica con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el cierre de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CierreCajaChica registro) {
        try {
            CierreCajaChica resultado = cierreCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar el cierre de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CierreCajaChica registro) {
        try {
            CierreCajaChica resultado = cierreCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear el cierre de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Prepara un cierre en BORRADOR. Body: { "idCaja": 1, "fecha": "2026-08-27", "idUsuario": 5 }
     * Respuesta: { "cierre": {...}, "movimientos": [...] }
     */
    @POST
    @Path("/preparar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response preparar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /crch/preparar");
        try {
            Long idCaja = toLong(datos.get("idCaja"));
            LocalDate fecha = toFecha((String) datos.get("fecha"));
            Long idUsuario = toLong(datos.get("idUsuario"));

            Map<String, Object> resultado = cierreCajaChicaService.prepararCierre(idCaja, fecha, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al preparar el cierre: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Confirma el cierre con el saldo físico contado.
     * Body: { "saldoFisico": 145.00, "observacion": "...", "idPlanCuentaDiferencia": 59001, "idUsuario": 5 }
     * `idPlanCuentaDiferencia` sólo es obligatorio si hay diferencia entre el saldo físico y el de libros.
     */
    @POST
    @Path("/confirmar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /crch/confirmar/" + id);
        try {
            Double saldoFisico = toDouble(datos.get("saldoFisico"));
            String observacion = (String) datos.get("observacion");
            Long idPlanCuentaDiferencia = toLong(datos.get("idPlanCuentaDiferencia"));
            Long idUsuario = toLong(datos.get("idUsuario"));

            CierreCajaChica resultado = cierreCajaChicaService.confirmarCierre(id, saldoFisico, observacion,
                    idPlanCuentaDiferencia, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al confirmar el cierre: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula el último cierre CERRADO de la caja. Body: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /crch/anular/" + id);
        try {
            String motivo = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            cierreCajaChicaService.anularCierre(id, motivo, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", "Cierre anulado correctamente."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el cierre: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/listar/{idCaja}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@PathParam("idCaja") Long idCaja) {
        System.out.println("LLEGA AL SERVICIO GET /crch/listar/" + idCaja);
        try {
            List<CierreCajaChica> resultado = cierreCajaChicaService.listar(idCaja);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar los cierres de la caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/movimientos/{idCierre}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response movimientos(@PathParam("idCierre") Long idCierre) {
        System.out.println("LLEGA AL SERVICIO GET /crch/movimientos/" + idCierre);
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCajaChicaService.movimientos(idCierre))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los movimientos del cierre: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(cierreCajaChicaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            CierreCajaChica elimina = new CierreCajaChica();
            cierreCajaChicaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar el cierre de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
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

    private LocalDate toFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
