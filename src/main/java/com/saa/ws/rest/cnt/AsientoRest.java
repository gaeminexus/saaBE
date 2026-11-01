package com.saa.ws.rest.cnt;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.AsientoDaoService;
import com.saa.ejb.cnt.service.AsientoService;
import com.saa.ejb.cnt.service.CargaAsientosManualesService;
import com.saa.model.cnt.Asiento;
import com.saa.model.cnt.AsientoResumen;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ResultadoCargaAsientosManuales;
import com.saa.model.cnt.SolicitudCargaAsientosManuales;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
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

@Path("asnt")
public class AsientoRest {

    /** U-2: todo proceso que graba valida el usuario PRIMERO. */
    @jakarta.ejb.EJB
    private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

    @EJB
    private AsientoDaoService asientoDaoService;

    @EJB
    private AsientoService asientoService;

    @EJB
    private CargaAsientosManualesService cargaAsientosManualesService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Asiento> lista = asientoDaoService.selectAll(NombreEntidadesContabilidad.ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asientos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de asientos de una empresa y período, en proyección (código, número, fecha,
     * glosa, estado, total debe, total haber) — reemplaza el fallback de getAll que usaba
     * listado-asientos.component.ts cuando el filtro por criterios fallaba (bajaba los 1.784
     * asientos de todas las empresas, ~4 MB, para filtrar en el cliente). Ver
     * docs/estandar/ESTANDAR-PROYECCIONES-EN-LISTADOS.md.
     */
    @GET
    @Path("/resumen/{idEmpresa}/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumenPorEmpresaPeriodo(@PathParam("idEmpresa") Long idEmpresa,
            @PathParam("idPeriodo") Long idPeriodo) {
        try {
            List<AsientoResumen> lista = asientoDaoService.selectResumenPorEmpresaPeriodo(idEmpresa, idPeriodo);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el resumen de asientos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Asiento asiento = asientoDaoService.selectById(id, NombreEntidadesContabilidad.ASIENTO);
            if (asiento == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Asiento con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(asiento).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generaReversion/{idAsiento}")
    public Response generaReversion(@PathParam("idAsiento") Long idAsiento) {
        try {
            return Response.status(Response.Status.OK).entity(asientoService.reversionAsiento(idAsiento)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /**
     * C17 — Anula un asiento con la regla de cierre (docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md §4).
     * Período abierto y sin fecha: anula. Período CERRADO: genera el reverso en {@code fechaReverso}
     * (yyyy-MM-dd, tiene que caer en un período abierto) o, si no viene, en el primer período abierto
     * posterior; el original queda REVERSADO y enlazado. Devuelve el asiento original.
     *
     * POST /asnt/anular/{idAsiento}?usuario=...&motivo=...&fechaReverso=2025-07-01
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/anular/{idAsiento}")
    public Response anular(@PathParam("idAsiento") Long idAsiento,
            @jakarta.ws.rs.QueryParam("usuario") String usuario,
            @jakarta.ws.rs.QueryParam("motivo") String motivo,
            @jakarta.ws.rs.QueryParam("fechaReverso") String fechaReverso) {
        try {
            // U-2: lo primero, el usuario que anula existe (antes, nulo o vacío se grababa como «SISTEMA»).
            usuario = usuarioValidacionService.validar(usuario);
            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("El motivo de la anulación es obligatorio.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            java.time.LocalDate fecha = fechaReverso != null && !fechaReverso.trim().isEmpty()
                    ? java.time.LocalDate.parse(fechaReverso.trim()) : null;
            return Response.status(Response.Status.OK)
                    .entity(asientoService.anulaAsiento(idAsiento, usuario, motivo, fecha))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.UsuarioInvalidoException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("No se puede anular el asiento sin un usuario válido: " + e.getMotivo())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            String m = e.getMessage() != null ? e.getMessage() : "Error inesperado";
            Response.Status st = m.startsWith("PERIODO_CERRADO") ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR;
            return Response.status(st).entity("Error al anular el asiento: " + m).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/generaCopia/{idAsiento}")
    public Response generaCopia(@PathParam("idAsiento") Long idAsiento) {
        try {
            return Response.status(Response.Status.OK).entity(asientoService.copiaAsiento(idAsiento)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Asiento registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ASIENTO");
        try {
            Asiento resultado = asientoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Asiento registro) {
        System.out.println("LLEGA AL SERVICIO POST - ASIENTO");
        try {
            Asiento resultado = asientoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de ASIENTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(asientoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // =====================================================================
    // Cargador de asientos manuales (C5) - docs/logica-negocio/cnt/CARGADOR-ASIENTOS-MANUALES.md
    // =====================================================================

    /**
     * Carga un archivo de asientos manuales enviado como texto dentro del JSON:
     * {@code {idEmpresa, usuario, nombreArchivo, codigoAltTipoAsiento, nombreTipoAsiento, contenido}}.
     * 200 con los asientos creados; 400 con la lista de errores (nada grabado); 500 si fallo la grabacion
     * (transaccion revertida).
     */
    @POST
    @Path("/cargarManuales")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarManuales(SolicitudCargaAsientosManuales solicitud) {
        System.out.println("LLEGA AL SERVICIO cargarManuales - ASIENTO, archivo: "
                + (solicitud != null ? solicitud.getNombreArchivo() : null));
        try {
            ResultadoCargaAsientosManuales resultado = cargaAsientosManualesService.cargar(solicitud);
            return Response.status(resultado.isExito() ? Response.Status.OK : Response.Status.BAD_REQUEST)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cargar asientos manuales: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Misma carga con el archivo como multipart (campo {@code archivo}), para el navegador. Los demas campos
     * viajan como texto: {@code archivoNombre} (con encodeURIComponent, igual que CargaMarcacionesRest),
     * {@code idEmpresa}, {@code usuario}, {@code codigoAltTipoAsiento} y {@code nombreTipoAsiento} (opcionales).
     */
    @POST
    @Path("/cargarManualesArchivo")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarManualesArchivo(@FormParam("archivo") InputStream archivo,
            @FormParam("archivoNombre") String nombreArchivo,
            @FormParam("idEmpresa") String idEmpresaParam,
            @FormParam("usuario") String usuario,
            @FormParam("codigoAltTipoAsiento") String codigoAltTipoAsientoParam,
            @FormParam("nombreTipoAsiento") String nombreTipoAsiento) {
        System.out.println("LLEGA AL SERVICIO cargarManualesArchivo - ASIENTO, archivo: " + nombreArchivo
                + ", empresa: " + idEmpresaParam);
        try {
            if (archivo == null || nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            SolicitudCargaAsientosManuales solicitud = new SolicitudCargaAsientosManuales();
            solicitud.setNombreArchivo(URLDecoder.decode(nombreArchivo, StandardCharsets.UTF_8));
            solicitud.setUsuario(usuario);
            solicitud.setNombreTipoAsiento(nombreTipoAsiento);
            if (idEmpresaParam != null && !idEmpresaParam.trim().isEmpty()) {
                solicitud.setIdEmpresa(Long.valueOf(idEmpresaParam.trim()));
            }
            if (codigoAltTipoAsientoParam != null && !codigoAltTipoAsientoParam.trim().isEmpty()) {
                solicitud.setCodigoAltTipoAsiento(Integer.valueOf(codigoAltTipoAsientoParam.trim()));
            }
            solicitud.setContenido(new String(archivo.readAllBytes(), StandardCharsets.UTF_8));
            ResultadoCargaAsientosManuales resultado = cargaAsientosManualesService.cargar(solicitud);
            return Response.status(resultado.isExito() ? Response.Status.OK : Response.Status.BAD_REQUEST)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cargar asientos manuales: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ASIENTO");
        try {
            Asiento elimina = new Asiento();
            asientoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
