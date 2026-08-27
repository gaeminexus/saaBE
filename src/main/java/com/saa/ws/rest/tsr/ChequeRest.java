package com.saa.ws.rest.tsr;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.ChequeDaoService;
import com.saa.ejb.tsr.service.ChequeService;
import com.saa.model.tsr.Cheque;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("dtch")
public class ChequeRest {

    @EJB
    private ChequeDaoService chequeDaoService;

    @EJB
    private ChequeService chequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Cheque.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Cheque> lista = chequeDaoService.selectAll(NombreEntidadesTesoreria.CHEQUE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cheques: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Cheque cheque = chequeDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUE);
            if (cheque == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Cheque con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cheque).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cheque: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Cheque registro) {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUE");
        try {
            Cheque resultado = chequeService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cheque: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Cheque registro) {
        System.out.println("LLEGA AL SERVICIO POST CHEQUE");
        try {
            Cheque resultado = chequeService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cheque: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of ChequeRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CHEQUE");
        try {
            return Response.status(Response.Status.OK)
                    .entity(chequeService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de Cheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUE");
        try {
            Cheque elimina = new Cheque();
            chequeDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cheque: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Siguiente cheque disponible (ACTIVO, menor número) de una cuenta bancaria.
     * Devuelve 404 con {"mensaje": "..."} si no hay cheques disponibles.
     */
    @GET
    @Path("/siguiente/{idCuenta}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response siguiente(@PathParam("idCuenta") Long idCuenta) {
        System.out.println("LLEGA AL SERVICIO GET /dtch/siguiente/" + idCuenta);
        try {
            Cheque cheque = chequeService.siguienteDisponible(idCuenta);
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("idCheque", cheque.getCodigo());
            body.put("numero", cheque.getNumero());
            return Response.status(Response.Status.OK).entity(body)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            // "La cuenta no tiene cheques disponibles" es el único caso de negocio: 404.
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(java.util.Collections.singletonMap("mensaje", e.getMessage()))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            // Cualquier otra falla (BD caída, etc.) es un error real: no disfrazarla de "sin cheques".
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el siguiente cheque disponible: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de cheques con los datos del pago que los usó, para la pantalla
     * de consulta de cheques. Filtros opcionales: idCuenta, estado, desde, hasta.
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("idCuenta") Long idCuenta,
            @QueryParam("estado") Long estado,
            @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        System.out.println("LLEGA AL SERVICIO GET /dtch/listar");
        try {
            LocalDate fechaDesde = (desde != null && !desde.trim().isEmpty()) ? LocalDate.parse(desde.trim()) : null;
            LocalDate fechaHasta = (hasta != null && !hasta.trim().isEmpty()) ? LocalDate.parse(hasta.trim()) : null;
            List<Map<String, Object>> lista =
                    chequeService.listar(idEmpresa, idCuenta, estado, fechaDesde, fechaHasta);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar cheques: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un cheque suelto (ACTIVO, sin pago asociado).
     * Body esperado: { "motivo": 1, "idUsuario": 5 }  (motivo: rubro MotivoAnulacionCheque)
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dtch/anular/" + id);
        try {
            Long motivo    = (datos != null) ? toLong(datos.get("motivo")) : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            chequeService.anularChequeSuelto(id, motivo, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", "Cheque anulado correctamente."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el cheque: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca como IMPRESOS los cheques indicados (deben estar Generado).
     * Body esperado: { "ids": [12, 13], "idUsuario": 5 }
     */
    @POST
    @Path("/imprimir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response imprimir(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dtch/imprimir");
        try {
            List<Long> ids = toLongList((datos != null) ? datos.get("ids") : null);
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            chequeService.marcarImpresos(ids, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", ids.size() + " cheque(s) marcado(s) como Impreso."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al marcar los cheques como impresos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Marca como ENTREGADOS los cheques indicados (deben estar Impreso).
     * Body esperado: { "ids": [12, 13], "idUsuario": 5 }
     */
    @POST
    @Path("/entregar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response entregar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dtch/entregar");
        try {
            List<Long> ids = toLongList((datos != null) ? datos.get("ids") : null);
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            chequeService.marcarEntregados(ids, idUsuario);
            return Response.status(Response.Status.OK)
                    .entity(java.util.Collections.singletonMap("mensaje", ids.size() + " cheque(s) marcado(s) como Entregado."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al marcar los cheques como entregados: " + e.getMessage())
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

    private List<Long> toLongList(Object valor) {
        List<Long> ids = new ArrayList<>();
        if (valor instanceof List) {
            for (Object item : (List<?>) valor) {
                Long id = toLong(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }
}
