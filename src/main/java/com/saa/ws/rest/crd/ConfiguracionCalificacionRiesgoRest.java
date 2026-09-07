package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ConfiguracionCalificacionRiesgoDaoService;
import com.saa.ejb.crd.service.CalificacionRiesgoService;
import com.saa.ejb.crd.service.ConfiguracionCalificacionRiesgoService;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigenciaCalificacion;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionCalificacionRiesgo;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
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

/**
 * Parametrización de la escala de calificación de riesgo (CRD.CFCR) — P22,
 * PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 *
 * Contrato para el frontend en docs/logica-negocio/crd/API-CALIFICACION-RIESGO.md — cualquier
 * cambio de ruta, request o response se registra ahí en el mismo cambio.
 *
 * <b>NO es la misma clasificación que las bandas contables</b> (`/rest/cbpr` + `/rest/bndp`): la
 * banda dice a qué CUENTA CONTABLE va el saldo; esta dice cuánta PROVISIÓN regulatoria se
 * constituye para el G48. Ningún corte coincide entre las dos.
 *
 * Las fechas de query param viajan como {@code yyyy-MM-dd}; en las respuestas Jackson las emite
 * como arreglo (ver CLAUDE.md §Serialización).
 */
@Path("cfcr")
public class ConfiguracionCalificacionRiesgoRest {

    @EJB
    private ConfiguracionCalificacionRiesgoDaoService configuracionCalificacionRiesgoDaoService;

    @EJB
    private ConfiguracionCalificacionRiesgoService configuracionCalificacionRiesgoService;

    /** Endpoint de verificación (`/probar`): mismo camino que consume el G48, sin escribir nada. */
    @EJB
    private CalificacionRiesgoService calificacionRiesgoService;

    @Context
    private UriInfo context;

    public ConfiguracionCalificacionRiesgoRest() {
    }

    // ------------------------------------------------------------------------
    // CRUD estándar del patrón de capas
    // ------------------------------------------------------------------------

    /**
     * GET - Todas las configuraciones, como entidades.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - CONFIGURACION_CALIFICACION_RIESGO");
        try {
            List<ConfiguracionCalificacionRiesgo> lista = configuracionCalificacionRiesgoDaoService
                    .selectAll(NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener configuraciones de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Una configuración por código.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - CONFIGURACION_CALIFICACION_RIESGO - id: " + id);
        try {
            ConfiguracionCalificacionRiesgo entidad = configuracionCalificacionRiesgoDaoService
                    .selectById(id, NombreEntidadesCredito.CONFIGURACION_CALIFICACION_RIESGO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ConfiguracionCalificacionRiesgo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(entidad)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la configuracion de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Crea una configuración (solo la cabecera). El alta real de la pantalla es
     * {@code /guardarConfiguracion}, que graba cabecera y escala juntas.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ConfiguracionCalificacionRiesgo registro) {
        System.out.println("LLEGA AL SERVICIO POST - CONFIGURACION_CALIFICACION_RIESGO");
        try {
            ConfiguracionCalificacionRiesgo resultado = configuracionCalificacionRiesgoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la configuracion de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * PUT - Actualiza una configuración (solo la cabecera).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ConfiguracionCalificacionRiesgo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CONFIGURACION_CALIFICACION_RIESGO");
        try {
            ConfiguracionCalificacionRiesgo resultado = configuracionCalificacionRiesgoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la configuracion de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * DELETE - Elimina una configuración. Falla si todavía tiene calificaciones — la FK lo
     * impide, y así debe ser.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CONFIGURACION_CALIFICACION_RIESGO - id: " + id);
        try {
            ConfiguracionCalificacionRiesgo elimina = new ConfiguracionCalificacionRiesgo();
            configuracionCalificacionRiesgoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar la configuracion de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CONFIGURACION_CALIFICACION_RIESGO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // ------------------------------------------------------------------------
    // Parametrización (lo que consume la pantalla)
    // ------------------------------------------------------------------------

    /**
     * GET - Configuración vigente de un producto, con su escala.
     *
     * @param idProducto Código del producto (CRD.PRDC)
     * @param idEmpresa  Código de la empresa (SCP.PJRQ); ausente = universal
     * @param fecha      Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/vigente")
    @Produces(MediaType.APPLICATION_JSON)
    public Response vigente(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO VIGENTE - CONFIGURACION_CALIFICACION_RIESGO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.selectVigenteConEscala(
                            idProducto, idEmpresa, parseaFecha(fecha)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la configuracion vigente: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Listado completo para la pantalla: todos los productos de crédito (activos e
     * inactivos) con su configuración vigente. Los productos sin configuración salen igual, con
     * la configuración en nulo.
     *
     * @param idEmpresa Código de la empresa (SCP.PJRQ); ausente = solo las universales
     * @param fecha     Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/listado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listado(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO LISTADO - CONFIGURACION_CALIFICACION_RIESGO"
                + " - empresa: " + idEmpresa + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.listarParametrizacion(
                            idEmpresa, parseaFecha(fecha)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el listado de parametrizacion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Historial de configuraciones de un producto, vigentes y cerradas, cada una con su
     * escala. Para auditoría y reprocesos.
     *
     * @param idProducto Código del producto (CRD.PRDC)
     * @param idEmpresa  Código de la empresa (SCP.PJRQ); ausente = universal
     */
    @GET
    @Path("/historial")
    @Produces(MediaType.APPLICATION_JSON)
    public Response historial(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa) {
        System.out.println("LLEGA AL SERVICIO HISTORIAL - CONFIGURACION_CALIFICACION_RIESGO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.selectHistorial(idProducto, idEmpresa))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el historial de configuraciones: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Graba una configuración completa: cabecera más escala, en una transacción. Alta si
     * {@code idConfiguracion} viene nulo; edición en el lugar si viene, y solo si la vigencia
     * todavía no empezó. Valida que la escala no tenga huecos ni solapes — ver el JavaDoc de
     * {@code ConfiguracionCalificacionRiesgoService#guardarConfiguracion}.
     */
    @POST
    @Path("/guardarConfiguracion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response guardarConfiguracion(SolicitudConfiguracionCalificacionRiesgo solicitud) {
        System.out.println("LLEGA AL SERVICIO GUARDAR CONFIGURACION - CONFIGURACION_CALIFICACION_RIESGO"
                + " - configuracion: " + (solicitud != null ? solicitud.getIdConfiguracion() : null)
                + " - escalas: "
                + (solicitud != null && solicitud.getEscalas() != null ? solicitud.getEscalas().size() : 0));
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.guardarConfiguracion(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al guardar la configuracion de calificacion de riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Cambio normativo: cierra la vigencia de la configuración actual y abre la nueva
     * desde la fecha indicada, con la escala que traiga la solicitud.
     */
    @POST
    @Path("/cerrarVigencia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrarVigencia(SolicitudCierreVigenciaCalificacion solicitud) {
        System.out.println("LLEGA AL SERVICIO CERRAR VIGENCIA - CONFIGURACION_CALIFICACION_RIESGO"
                + " - configuracion vigente: "
                + (solicitud != null ? solicitud.getIdConfiguracionVigente() : null));
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionCalificacionRiesgoService.cerrarVigencia(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cerrar la vigencia de la configuracion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // ------------------------------------------------------------------------
    // Verificación
    // ------------------------------------------------------------------------

    /**
     * GET - <b>Endpoint DE VERIFICACIÓN</b>: califica una antigüedad en días contra la
     * parametrización vigente y devuelve la calificación (A1..E) y el porcentaje de provisión
     * que le tocan.
     *
     * Pedido del FE (2026-09-07): la forma más barata de que el usuario detecte un hueco en la
     * escala es meter unos días y ver si no cae en ninguna calificación.
     *
     * <b>Usa {@code CalificacionRiesgoService#calificar} — el MISMO camino que
     * {@code GeneracionG48ServiceImpl}.</b> Si este endpoint calificara distinto que el reporte
     * regulatorio, sería peor que no tenerlo: no reimplementar la resolución acá.
     *
     * @param idProducto Código del producto (CRD.PRDC)
     * @param idEmpresa  Código de la empresa (SCP.PJRQ); ausente = universal
     * @param dias       Días de morosidad
     * @param fecha      Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/probar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response probar(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("dias") Long dias,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO PROBAR - CONFIGURACION_CALIFICACION_RIESGO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " dias: " + dias + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(calificacionRiesgoService.calificar(idProducto, idEmpresa, dias, parseaFecha(fecha)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al calificar el riesgo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Convierte el query param de fecha. Ausente o vacío devuelve nulo, y el servicio lo
     * resuelve como hoy.
     */
    private LocalDate parseaFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(fecha.trim());
    }
}
