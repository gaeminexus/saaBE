package com.saa.ws.rest.tsr;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.ChequeraDaoService;
import com.saa.ejb.tsr.service.ChequeraService;
import com.saa.model.tsr.Chequera;
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

@Path("chqr")
public class ChequeraRest {

    @EJB
    private ChequeraDaoService chequeraDaoService;

    @EJB
    private ChequeraService chequeraService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeraRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Chequera.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Chequera> lista = chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener chequeras: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Chequera chequera = chequeraDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUERA);
            if (chequera == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Chequera con ID " + id + " no encontrada").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(chequera).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener chequera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Chequera registro) {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUERA");
        try {
            Chequera resultado = chequeraService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar chequera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Chequera registro) {
        System.out.println("LLEGA AL SERVICIO POST CHEQUERA");
        try {
            Chequera resultado = chequeraService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear chequera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * POST method for updating or creating an instance of ChequeraRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CHEQUERA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(chequeraService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de Chequera por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUERA");
        try {
            Chequera elimina = new Chequera();
            chequeraDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar chequera: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Sugiere el número inicial de una nueva chequera para la cuenta: max(finaliza)+1
     * de las chequeras no anuladas, o 1 si la cuenta no tiene ninguna.
     */
    @GET
    @Path("/sugerirInicio/{idCuenta}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sugerirInicio(@PathParam("idCuenta") Long idCuenta) {
        System.out.println("LLEGA AL SERVICIO GET /chqr/sugerirInicio/" + idCuenta);
        try {
            Long siguiente = chequeraService.sugerirNumeroInicial(idCuenta);
            return Response.status(Response.Status.OK).entity(java.util.Collections.singletonMap("siguiente", siguiente))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al sugerir el número inicial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra la recepción de una chequera: valida el rango, graba la
     * chequera ACTIVA y genera un Cheque ACTIVO por cada número del rango.
     * Body esperado:
     * {
     *   "idCuentaBancaria": 4,
     *   "comienza": 1001,
     *   "finaliza": 1050,
     *   "fechaEntrega": "2026-08-26T09:00:00",
     *   "idUsuario": 5
     * }
     */
    @POST
    @Path("/registrarRecepcion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarRecepcion(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /chqr/registrarRecepcion");
        try {
            Long idCuentaBancaria = toLong(datos.get("idCuentaBancaria"));
            Long comienza         = toLong(datos.get("comienza"));
            Long finaliza         = toLong(datos.get("finaliza"));
            String fechaEntrega   = (String) datos.get("fechaEntrega");
            Long idUsuario        = toLong(datos.get("idUsuario"));

            LocalDateTime fecha = (fechaEntrega != null && !fechaEntrega.trim().isEmpty())
                    ? LocalDateTime.parse(fechaEntrega.trim()) : null;

            Chequera chequera = chequeraService.registrarRecepcion(idCuentaBancaria, comienza, finaliza,
                    fecha, idUsuario);
            return Response.status(Response.Status.CREATED).entity(chequera)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la recepción de la chequera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Resumen de una chequera: rango, total y distribución de cheques por
     * estado, más el siguiente número disponible.
     */
    @GET
    @Path("/resumen/{idChequera}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumen(@PathParam("idChequera") Long idChequera) {
        System.out.println("LLEGA AL SERVICIO GET /chqr/resumen/" + idChequera);
        try {
            Map<String, Object> resumen = chequeraService.resumen(idChequera);
            return Response.status(Response.Status.OK).entity(resumen)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el resumen de la chequera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula una chequera completa (rechaza si tiene cheques Generado, Impreso
     * o Entregado). Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /chqr/anular/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            chequeraService.anularChequera(id, motivo, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", "Chequera anulada correctamente."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la chequera: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Chequeras de una cuenta bancaria.
     */
    @GET
    @Path("/porCuenta/{idCuenta}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porCuenta(@PathParam("idCuenta") Long idCuenta) {
        System.out.println("LLEGA AL SERVICIO GET /chqr/porCuenta/" + idCuenta);
        try {
            List<Chequera> lista = chequeraService.selectByCuentaBancaria(idCuenta);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las chequeras de la cuenta: " + e.getMessage())
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
}
