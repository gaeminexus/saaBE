package com.saa.ws.rest.rhh;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.CargaMarcacionesDaoService;
import com.saa.ejb.rhh.service.CargaMarcacionesService;
import com.saa.ejb.rhh.service.ImportacionMarcacionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.ResultadoImportacionMarcaciones;
import com.saa.model.rhh.CargaMarcaciones;

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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("crmr")
public class CargaMarcacionesRest {

    @EJB
    private CargaMarcacionesDaoService cargaMarcacionesDaoService;

    @EJB
    private CargaMarcacionesService cargaMarcacionesService;

    @EJB
    private ImportacionMarcacionesService importacionMarcacionesService;

    @Context
    private UriInfo context;

    public CargaMarcacionesRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - CARGA_MARCACIONES");
        try {
            List<CargaMarcaciones> lista = cargaMarcacionesDaoService.selectAll(NombreEntidadesRhh.CARGA_MARCACIONES);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - CARGA_MARCACIONES, id: " + id);
        try {
            CargaMarcaciones registro = cargaMarcacionesDaoService.selectById(id, NombreEntidadesRhh.CARGA_MARCACIONES);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CargaMarcaciones registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CARGA_MARCACIONES");
        try {
            CargaMarcaciones actualizado = cargaMarcacionesService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CargaMarcaciones registro) {
        System.out.println("LLEGA AL SERVICIO POST - CARGA_MARCACIONES");
        try {
            CargaMarcaciones creado = cargaMarcacionesService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CARGA_MARCACIONES");
        try {
            List<CargaMarcaciones> lista = cargaMarcacionesService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CARGA_MARCACIONES");
        try {
            CargaMarcaciones elimina = new CargaMarcaciones();
            cargaMarcacionesDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoints de proceso - fase 7
    // =====================================================================

    /**
     * Lee el archivo y devuelve lo que pasaria, sin persistir nada.
     */
    @POST
    @Path("/previsualizar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response previsualizar(@FormParam("archivo") InputStream archivo,
            @FormParam("archivoNombre") String nombreArchivo,
            @FormParam("idFormato") String idFormatoParam,
            @FormParam("idEmpresa") String idEmpresaParam) {
        System.out.println("LLEGA AL SERVICIO previsualizar - CARGA_MARCACIONES, archivo: " + nombreArchivo
                + ", formato: " + idFormatoParam + ", empresa: " + idEmpresaParam);
        try {
            if (archivo == null || nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            // El frontend envia el nombre con encodeURIComponent() porque el
            // proveedor de multipart no declara charset para campos de texto
            // planos y puede no decodificar como UTF-8 - decodificar aqui
            // explicitamente evita que un nombre con tildes o enie llegue corrupto.
            nombreArchivo = URLDecoder.decode(nombreArchivo, StandardCharsets.UTF_8);
            Long idFormato = parseId(idFormatoParam);
            if (idFormato == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El campo idFormato es obligatorio").type(MediaType.APPLICATION_JSON).build();
            }
            Long idEmpresa = parseId(idEmpresaParam);
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El campo idEmpresa es obligatorio").type(MediaType.APPLICATION_JSON).build();
            }
            ResultadoImportacionMarcaciones resultado = importacionMarcacionesService
                    .previsualizar(archivo, nombreArchivo, idFormato, idEmpresa);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al previsualizar el archivo de marcaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Lee el archivo y persiste la carga con sus marcaciones.
     */
    @POST
    @Path("/confirmar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmar(@FormParam("archivo") InputStream archivo,
            @FormParam("archivoNombre") String nombreArchivo,
            @FormParam("idFormato") String idFormatoParam,
            @FormParam("idEmpresa") String idEmpresaParam,
            @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO confirmar - CARGA_MARCACIONES, archivo: " + nombreArchivo
                + ", formato: " + idFormatoParam + ", empresa: " + idEmpresaParam);
        try {
            if (archivo == null || nombreArchivo == null || nombreArchivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            // Ver el comentario equivalente en previsualizar().
            nombreArchivo = URLDecoder.decode(nombreArchivo, StandardCharsets.UTF_8);
            Long idFormato = parseId(idFormatoParam);
            if (idFormato == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El campo idFormato es obligatorio").type(MediaType.APPLICATION_JSON).build();
            }
            Long idEmpresa = parseId(idEmpresaParam);
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El campo idEmpresa es obligatorio").type(MediaType.APPLICATION_JSON).build();
            }
            ResultadoImportacionMarcaciones resultado = importacionMarcacionesService
                    .confirmar(archivo, nombreArchivo, idFormato, idEmpresa, usuarioRegistro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al importar el archivo de marcaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula una carga y retira sus marcaciones. Exige motivo.
     */
    @POST
    @Path("/anular/{idCarga}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("idCarga") Long idCarga, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO anular - CARGA_MARCACIONES, carga: " + idCarga);
        try {
            String motivo = datos != null && datos.get("motivo") != null
                    ? datos.get("motivo").toString() : null;
            String usuario = datos != null && datos.get("usuarioRegistro") != null
                    ? datos.get("usuarioRegistro").toString() : null;
            importacionMarcacionesService.anular(idCarga, motivo, usuario);
            return Response.status(Response.Status.OK).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al anular la carga de marcaciones: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Los identificadores de un multipart se reciben como String y se parsean aqui.
     * Un @FormParam declarado Long que no convierte lo rechaza RESTEasy con un 400
     * sin cuerpo ANTES de despachar el metodo, de modo que el fallo no deja traza
     * ni mensaje. Mismo criterio que ExtractoBancarioRest y SaldoAperturaRest.
     */
    private Long parseId(String valor) {
        try {
            return (valor == null || valor.trim().isEmpty()) ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }
}
