package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.HistoricoDesgloseAporteParticipeDaoService;
import com.saa.ejb.crd.service.HistoricoDesgloseAporteParticipeService;
import com.saa.model.crd.HistoricoDesgloseAporteParticipe;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("hdap")
public class HistoricoDesgloseAporteParticipeRest {

    @EJB
    private HistoricoDesgloseAporteParticipeDaoService historicoDesgloseAporteParticipeDaoService;

    @EJB
    private HistoricoDesgloseAporteParticipeService historicoDesgloseAporteParticipeService;

    @Context
    private UriInfo context;

    public HistoricoDesgloseAporteParticipeRest() {}

    /**
     * Obtiene todos los registros de desglose de aportes
     * GET /hdap/getAll
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistoricoDesgloseAporteParticipe> lista = historicoDesgloseAporteParticipeDaoService.selectAll(
                NombreEntidadesCredito.HISTORICO_DESGLOSE_APORTE_PARTICIPE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener histórico de desglose: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un registro por ID
     * GET /hdap/getId/{id}
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            HistoricoDesgloseAporteParticipe registro = historicoDesgloseAporteParticipeDaoService.selectById(
                id, NombreEntidadesCredito.HISTORICO_DESGLOSE_APORTE_PARTICIPE);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("HistoricoDesgloseAporteParticipe con ID " + id + " no encontrado")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene registros por ID de carga
     * GET /hdap/getByCarga/{idCarga}
     */
    @GET
    @Path("/getByCarga/{idCarga}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCarga(@PathParam("idCarga") Long idCarga) {
        try {
            List<HistoricoDesgloseAporteParticipe> lista = historicoDesgloseAporteParticipeService.obtenerPorCarga(idCarga);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registros por carga: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene registros por cédula
     * GET /hdap/getByCedula?cedula=0123456789
     */
    @GET
    @Path("/getByCedula")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCedula(@QueryParam("cedula") String cedula) {
        try {
            if (cedula == null || cedula.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El parámetro 'cedula' es requerido")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            List<HistoricoDesgloseAporteParticipe> lista = historicoDesgloseAporteParticipeService.obtenerPorCedula(cedula);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registros por cédula: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un registro por código interno y carga
     * GET /hdap/getByCodigoYCarga?codigoInterno=ABC123&idCarga=1
     */
    @GET
    @Path("/getByCodigoYCarga")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByCodigoYCarga(@QueryParam("codigoInterno") String codigoInterno, 
                                      @QueryParam("idCarga") Long idCarga) {
        try {
            if (codigoInterno == null || codigoInterno.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El parámetro 'codigoInterno' es requerido")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            if (idCarga == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("El parámetro 'idCarga' es requerido")
                    .type(MediaType.APPLICATION_JSON).build();
            }
            HistoricoDesgloseAporteParticipe registro = historicoDesgloseAporteParticipeService
                .obtenerPorCodigoInternoYCarga(codigoInterno, idCarga);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity("No se encontró registro con código interno " + codigoInterno + " y carga " + idCarga)
                    .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al obtener registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un registro existente
     * PUT /hdap
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(HistoricoDesgloseAporteParticipe registro) {
        System.out.println("LLEGA AL SERVICIO PUT - HISTORICODESGLOSEAPORTEPARTICIPE");
        try {
            HistoricoDesgloseAporteParticipe resultado = historicoDesgloseAporteParticipeService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al actualizar registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro
     * POST /hdap
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(HistoricoDesgloseAporteParticipe registro) {
        System.out.println("LLEGA AL SERVICIO POST - HISTORICODESGLOSEAPORTEPARTICIPE");
        try {
            HistoricoDesgloseAporteParticipe resultado = historicoDesgloseAporteParticipeService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al crear registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea múltiples registros
     * POST /hdap/saveBatch
     */
    @POST
    @Path("/saveBatch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveBatch(List<HistoricoDesgloseAporteParticipe> registros) {
        System.out.println("LLEGA AL SERVICIO POST BATCH - HISTORICODESGLOSEAPORTEPARTICIPE");
        try {
            historicoDesgloseAporteParticipeService.save(registros);
            return Response.status(Response.Status.CREATED)
                .entity("Se guardaron " + registros.size() + " registros correctamente")
                .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al guardar registros: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca por criterios
     * POST /hdap/selectByCriteria
     */
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de HISTORICODESGLOSEAPORTEPARTICIPE");
        try {
            List<HistoricoDesgloseAporteParticipe> lista = historicoDesgloseAporteParticipeService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Error en búsqueda: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro por ID
     * DELETE /hdap/{id}
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - HISTORICODESGLOSEAPORTEPARTICIPE");
        try {
            HistoricoDesgloseAporteParticipe elimina = new HistoricoDesgloseAporteParticipe();
            historicoDesgloseAporteParticipeDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Error al eliminar registro: " + e.getMessage())
                .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
