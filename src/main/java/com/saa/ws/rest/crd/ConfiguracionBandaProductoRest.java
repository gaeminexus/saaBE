package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.ConfiguracionBandaProductoDaoService;
import com.saa.ejb.crd.service.ClasificadorBandaService;
import com.saa.ejb.crd.service.ConfiguracionBandaProductoService;
import com.saa.ejb.crd.service.dto.SolicitudCierreVigencia;
import com.saa.ejb.crd.service.dto.SolicitudConfiguracionBanda;
import com.saa.model.crd.ConfiguracionBandaProducto;
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
 * Parametrización de bandas de cartera por producto (CRD.CBPR).
 *
 * Contrato para el frontend en docs/logica-negocio/crd/API-BANDAS-PRODUCTO.md — cualquier
 * cambio de ruta, request o response se registra ahí en el mismo cambio.
 *
 * Las fechas de query param viajan como {@code yyyy-MM-dd}; en las respuestas Jackson las
 * emite como arreglo (ver CLAUDE.md §Serialización).
 */
@Path("cbpr")
public class ConfiguracionBandaProductoRest {

    @EJB
    private ConfiguracionBandaProductoDaoService configuracionBandaProductoDaoService;

    @EJB
    private ConfiguracionBandaProductoService configuracionBandaProductoService;

    /** Endpoint de verificación de la parametrización (QA / frontend). */
    @EJB
    private ClasificadorBandaService clasificadorBandaService;

    @Context
    private UriInfo context;

    public ConfiguracionBandaProductoRest() {
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
        System.out.println("LLEGA AL SERVICIO GET ALL - CONFIGURACION_BANDA_PRODUCTO");
        try {
            List<ConfiguracionBandaProducto> lista = configuracionBandaProductoDaoService
                    .selectAll(NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener configuraciones de bandas: " + e.getMessage())
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
        System.out.println("LLEGA AL SERVICIO GET ID - CONFIGURACION_BANDA_PRODUCTO - id: " + id);
        try {
            ConfiguracionBandaProducto entidad = configuracionBandaProductoDaoService
                    .selectById(id, NombreEntidadesCredito.CONFIGURACION_BANDA_PRODUCTO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ConfiguracionBandaProducto con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(entidad)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la configuracion de bandas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Crea una configuración (solo la cabecera). El alta real de la pantalla es
     * {@code /guardarConfiguracion}, que graba cabecera y bandas juntas.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ConfiguracionBandaProducto registro) {
        System.out.println("LLEGA AL SERVICIO POST - CONFIGURACION_BANDA_PRODUCTO");
        try {
            ConfiguracionBandaProducto resultado = configuracionBandaProductoService
                    .saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear la configuracion de bandas: " + e.getMessage())
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
    public Response put(ConfiguracionBandaProducto registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CONFIGURACION_BANDA_PRODUCTO");
        try {
            ConfiguracionBandaProducto resultado = configuracionBandaProductoService
                    .saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la configuracion de bandas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * DELETE - Elimina una configuración. Falla si todavía tiene bandas: la FK
     * {@code FK_BNDP_CBPR} lo impide, y así debe ser.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CONFIGURACION_BANDA_PRODUCTO - id: " + id);
        try {
            ConfiguracionBandaProducto elimina = new ConfiguracionBandaProducto();
            configuracionBandaProductoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar la configuracion de bandas: " + e.getMessage())
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
        System.out.println("selectByCriteria de CONFIGURACION_BANDA_PRODUCTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.selectByCriteria(registros))
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
     * GET - Configuración vigente de un producto para un tipo de cartera, con sus bandas y
     * los rangos en días derivados.
     *
     * @param idProducto  Código del producto (CRD.PRDC)
     * @param idEmpresa   Código de la empresa (SCP.PJRQ)
     * @param tipoCartera 1 = por vencer, 2 = vencido
     * @param fecha       Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/vigente")
    @Produces(MediaType.APPLICATION_JSON)
    public Response vigente(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("tipoCartera") Long tipoCartera,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO VIGENTE - CONFIGURACION_BANDA_PRODUCTO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.selectVigenteConBandas(
                            idProducto, idEmpresa, tipoCartera, parseaFecha(fecha)))
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
     * GET - Listado completo para la pantalla: todos los productos de la empresa (activos
     * e inactivos) con sus configuraciones vigentes de los dos tipos de cartera. Los
     * productos sin configuración salen igual, con la configuración en nulo.
     *
     * @param idEmpresa Código de la empresa (SCP.PJRQ)
     * @param fecha     Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/listado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listado(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO LISTADO - CONFIGURACION_BANDA_PRODUCTO"
                + " - empresa: " + idEmpresa + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.listarParametrizacion(
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
     * GET - Historial de configuraciones de una terna, vigentes y cerradas, cada una con
     * sus bandas. Para auditoría y reprocesos.
     */
    @GET
    @Path("/historial")
    @Produces(MediaType.APPLICATION_JSON)
    public Response historial(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("tipoCartera") Long tipoCartera) {
        System.out.println("LLEGA AL SERVICIO HISTORIAL - CONFIGURACION_BANDA_PRODUCTO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.selectHistorial(
                            idProducto, idEmpresa, tipoCartera))
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
     * POST - Graba una configuración completa: cabecera más bandas, en una transacción.
     * Alta si {@code idConfiguracion} viene nulo; edición en el lugar si viene, y solo si
     * la vigencia todavía no empezó.
     */
    @POST
    @Path("/guardarConfiguracion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response guardarConfiguracion(SolicitudConfiguracionBanda solicitud) {
        System.out.println("LLEGA AL SERVICIO GUARDAR CONFIGURACION - CONFIGURACION_BANDA_PRODUCTO"
                + " - configuracion: "
                + (solicitud != null ? solicitud.getIdConfiguracion() : null)
                + " - bandas: "
                + (solicitud != null && solicitud.getBandas() != null
                        ? solicitud.getBandas().size() : 0));
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.guardarConfiguracion(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al guardar la configuracion de bandas: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Cambio normativo: cierra la vigencia de la configuración actual y abre la
     * nueva desde la fecha indicada, con las bandas que traiga la solicitud.
     */
    @POST
    @Path("/cerrarVigencia")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cerrarVigencia(SolicitudCierreVigencia solicitud) {
        System.out.println("LLEGA AL SERVICIO CERRAR VIGENCIA - CONFIGURACION_BANDA_PRODUCTO"
                + " - configuracion vigente: "
                + (solicitud != null ? solicitud.getIdConfiguracionVigente() : null));
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.cerrarVigencia(solicitud))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cerrar la vigencia de la configuracion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * GET - Catálogo de cuentas para el buscador de la pantalla: cuentas activas y de
     * MOVIMIENTO de la empresa cuyo número o nombre contiene el filtro.
     *
     * @param idEmpresa Código de la empresa (SCP.PJRQ)
     * @param filtro    Texto a buscar en el número de cuenta o en el nombre; ausente = todas
     */
    @GET
    @Path("/cuentas")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cuentas(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("filtro") String filtro) {
        System.out.println("LLEGA AL SERVICIO CUENTAS - CONFIGURACION_BANDA_PRODUCTO"
                + " - empresa: " + idEmpresa + " filtro: " + filtro);
        try {
            return Response.status(Response.Status.OK)
                    .entity(configuracionBandaProductoService.buscarCuentas(idEmpresa, filtro))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al buscar cuentas contables: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    // ------------------------------------------------------------------------
    // Verificación
    // ------------------------------------------------------------------------

    /**
     * GET - <b>Endpoint DE VERIFICACIÓN</b>: clasifica una antigüedad en días contra la
     * parametrización vigente y devuelve la banda y la cuenta contable que le tocan.
     *
     * No lo consume ningún proceso contable —ellos llaman al
     * {@code ClasificadorBandaService} directamente—; está para que QA y el frontend puedan
     * comprobar que la parametrización cargada hace lo que el usuario espera.
     *
     * @param idProducto  Código del producto (CRD.PRDC)
     * @param idEmpresa   Código de la empresa (SCP.PJRQ)
     * @param tipoCartera 1 = por vencer, 2 = vencido
     * @param dias        Días de antigüedad, >= 1
     * @param fecha       Fecha de vigencia en {@code yyyy-MM-dd}; ausente = hoy
     */
    @GET
    @Path("/clasificar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clasificar(@QueryParam("idProducto") Long idProducto,
            @QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("tipoCartera") Long tipoCartera,
            @QueryParam("dias") Long dias,
            @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO CLASIFICAR - CONFIGURACION_BANDA_PRODUCTO"
                + " - producto: " + idProducto + " empresa: " + idEmpresa
                + " tipoCartera: " + tipoCartera + " dias: " + dias + " fecha: " + fecha);
        try {
            return Response.status(Response.Status.OK)
                    .entity(clasificadorBandaService.clasificar(
                            idProducto, idEmpresa, tipoCartera, dias, parseaFecha(fecha)))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al clasificar la banda: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Convierte el query param de fecha. Ausente o vacío devuelve nulo, y el servicio lo
     * resuelve como hoy.
     *
     * @param fecha : Fecha en {@code yyyy-MM-dd}
     * @return      : Fecha, o {@code null}
     */
    private LocalDate parseaFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(fecha.trim());
    }
}
