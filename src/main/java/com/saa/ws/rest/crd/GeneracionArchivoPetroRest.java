package com.saa.ws.rest.crd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.GeneracionArchivoPetroDaoService;
import com.saa.ejb.crd.service.GeneracionArchivoPetroService;
import com.saa.model.crd.GeneracionArchivoPetro;
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
 * REST API para Generación de Archivos Petrocomercial.
 * 
 * Este REST SOLO valida parámetros y delega al Service.
 * TODA la lógica de negocio está en GeneracionArchivoPetroService.
 * 
 * @author Sistema SAA
 * @since 2026-04-15
 */
@Path("gnap")
public class GeneracionArchivoPetroRest {
	
	@EJB
    private GeneracionArchivoPetroDaoService generacionArchivoPetroDaoService;

    @EJB
    private GeneracionArchivoPetroService generacionArchivoPetroService;

    @Context
    private UriInfo context;

	/**
     * Default constructor.
     */
    public GeneracionArchivoPetroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Obtiene todos los registros de GeneracionArchivoPetro.
     * 
     * @return Response con lista de GeneracionArchivoPetro
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<GeneracionArchivoPetro> lista = generacionArchivoPetroDaoService.selectAll(NombreEntidadesCredito.GENERACION_ARCHIVOS_PETRO);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener generacionArchivoPetroes: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Obtiene un registro de GeneracionArchivoPetro por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto GeneracionArchivoPetro
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            GeneracionArchivoPetro generacionArchivoPetro = generacionArchivoPetroDaoService.selectById(id, NombreEntidadesCredito.GENERACION_ARCHIVOS_PETRO);
            if (generacionArchivoPetro == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("GeneracionArchivoPetro con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(generacionArchivoPetro)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Crea o actualiza un registro de GeneracionArchivoPetro (PUT).
     * 
     * @param registro Objeto GeneracionArchivoPetro
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(GeneracionArchivoPetro registro) {
        System.out.println("LLEGA AL SERVICIO PUT");
        try {
            GeneracionArchivoPetro resultado = generacionArchivoPetroService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * POST - Crear la cabecera de una generación (Paso 1).
     *
     * La filial define qué partícipes entran y con qué formato sale el archivo:
     * 1 = Petrocomercial (archivo posicional), 2 = ARCH (plano por columnas).
     *
     * A diferencia del POST genérico, este endpoint valida que no exista ya una
     * generación para el mismo periodo y filial.
     *
     * @param mes Mes del periodo (1-12)
     * @param anio Año del periodo
     * @param codigoFilial Código de la filial (CRD.FLLL)
     * @param usuario Usuario que crea la generación
     * @return Response con la cabecera creada
     */
    @POST
    @Path("/crearCabecera")
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearCabecera(@QueryParam("mes") Long mes,
                                  @QueryParam("anio") Long anio,
                                  @QueryParam("codigoFilial") Long codigoFilial,
                                  @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO CREAR CABECERA GENERACION: " + mes + "/" + anio
            + " - Filial: " + codigoFilial);
        try {
            if (mes == null || mes < 1 || mes > 12) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Mes inválido: debe estar entre 1 y 12"))
                        .build();
            }
            if (anio == null || anio < 2000) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Año inválido"))
                        .build();
            }
            if (codigoFilial == null || codigoFilial <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Debe indicar la filial de la generación"))
                        .build();
            }

            GeneracionArchivoPetro cabecera = generacionArchivoPetroService.crearCabeceraGeneracion(
                mes, anio, codigoFilial, usuario);

            return Response.status(Response.Status.CREATED)
                    .entity(cabecera)
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            String mensaje = e.getMessage() != null ? e.getMessage() : "Error al crear la cabecera";

            if (mensaje.contains("Ya existe")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", mensaje))
                        .build();
            }

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", mensaje))
                    .build();
        }
    }

    /**
     * GET - Lista las generaciones de una filial, de la más reciente a la más antigua.
     *
     * @param codigoFilial Código de la filial (CRD.FLLL)
     * @return Response con la lista de generaciones
     */
    @GET
    @Path("/porFilial/{codigoFilial}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPorFilial(@PathParam("codigoFilial") Long codigoFilial) {
        System.out.println("LLEGA AL SERVICIO LISTAR GENERACIONES POR FILIAL: " + codigoFilial);
        try {
            List<GeneracionArchivoPetro> lista = generacionArchivoPetroService.listarPorFilial(codigoFilial);
            return Response.status(Response.Status.OK)
                    .entity(lista)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar generaciones de la filial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST - Generar archivo de descuentos Petrocomercial (Paso 2)
     * Recibe el ID de GeneracionArchivoPetro y procesa todo desde el servicio.
     * El usuario se obtiene de la cabecera de generación previamente creada.
     */
    @POST
    @Path("/generarArchivo/{codigoGeneracion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarArchivo(@PathParam("codigoGeneracion") Long codigoGeneracion) {
        try {
            // Validar que se reciba el ID
            if (codigoGeneracion == null || codigoGeneracion <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "ID de generación inválido"))
                    .build();
            }
            
            // Obtener la cabecera para extraer el usuario
            GeneracionArchivoPetro cabecera = generacionArchivoPetroService.buscarPorId(codigoGeneracion);
            if (cabecera == null) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Generación no encontrada con ID: " + codigoGeneracion))
                    .build();
            }
            
            // Obtener el usuario de la cabecera (quien la creó)
            String usuario = cabecera.getUsuarioGeneracion();
            if (usuario == null || usuario.trim().isEmpty()) {
                usuario = cabecera.getUsuarioIngreso(); // Fallback
            }
            
            // DELEGAR al Service - procesa todo desde el ID de la generación
            Map<String, Object> resultado = generacionArchivoPetroService.procesarGeneracion(
                codigoGeneracion, usuario);
            
            return Response.ok(resultado).build();
            
        } catch (Exception e) {
            e.printStackTrace();
            
            if (e.getMessage() != null && e.getMessage().contains("no encontrada")) {
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
            }
            
            if (e.getMessage() != null && e.getMessage().contains("ya fue procesada")) {
                return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
            }
            
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Crea o actualiza un registro de GeneracionArchivoPetro (POST).
     * 
     * @param registro Objeto GeneracionArchivoPetro
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(GeneracionArchivoPetro registro) {
        System.out.println("LLEGA AL SERVICIO");
        try {
            GeneracionArchivoPetro resultado = generacionArchivoPetroService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear cantón: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * POST method for updating or creating an instance of GeneracionArchivoPetroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de GeneracionArchivoPetro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(generacionArchivoPetroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * GET - Descarga el archivo TXT de una generación y la marca como descargada.
     *
     * Una vez descargado el archivo la generación ya NO se puede eliminar.
     *
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que descarga (opcional; por defecto el de la generación)
     * @return Response con el archivo TXT como adjunto
     */
    @GET
    @Path("/descargarArchivo/{codigoGeneracion}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarArchivo(@PathParam("codigoGeneracion") Long codigoGeneracion,
                                     @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO DESCARGAR ARCHIVO PETRO: " + codigoGeneracion);
        try {
            if (codigoGeneracion == null || codigoGeneracion <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("ID de generación inválido")
                        .build();
            }

            GeneracionArchivoPetro generacion = generacionArchivoPetroService.buscarPorId(codigoGeneracion);
            if (generacion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Generación no encontrada con ID: " + codigoGeneracion)
                        .build();
            }

            String rutaArchivo = generacion.getRutaArchivo();
            if (rutaArchivo == null || rutaArchivo.trim().isEmpty()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("La generación aún no tiene archivo generado")
                        .build();
            }

            File archivo = new File(rutaArchivo);
            if (!archivo.exists()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("El archivo no existe en el servidor: " + rutaArchivo)
                        .build();
            }

            // Se marca ANTES de entregar el archivo: a partir de aquí la
            // generación queda bloqueada para eliminación.
            String usuarioDescarga = (usuario != null && !usuario.trim().isEmpty())
                    ? usuario
                    : generacion.getUsuarioGeneracion();
            generacionArchivoPetroService.marcarDescargado(codigoGeneracion, usuarioDescarga);

            String nombreArchivo = generacion.getNombreArchivo() != null
                    ? generacion.getNombreArchivo()
                    : archivo.getName();

            InputStream flujo = new FileInputStream(archivo);

            return Response.ok(flujo)
                    .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                    .build();

        } catch (Throwable e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al descargar el archivo de la generación: " + e.getMessage())
                    .build();
        }
    }

    /**
     * DELETE - Elimina una generación completa (cabecera, detalles, partícipes,
     * cuotas y archivo TXT) para poder volver a generar el periodo.
     *
     * Rechaza la eliminación con 409 CONFLICT si el archivo ya fue descargado
     * o si la generación ya fue marcada como ENVIADA o PROCESADA.
     *
     * @param codigoGeneracion ID de la generación
     * @param usuario Usuario que elimina
     * @return Response con el resumen de lo eliminado
     */
    @DELETE
    @Path("/eliminar/{codigoGeneracion}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarGeneracion(@PathParam("codigoGeneracion") Long codigoGeneracion,
                                       @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO ELIMINAR GENERACION PETRO: " + codigoGeneracion);
        try {
            if (codigoGeneracion == null || codigoGeneracion <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "ID de generación inválido"))
                        .build();
            }

            Map<String, Object> resultado = generacionArchivoPetroService.eliminarGeneracion(codigoGeneracion, usuario);
            return Response.ok(resultado).build();

        } catch (Exception e) {
            e.printStackTrace();

            String mensaje = e.getMessage() != null ? e.getMessage() : "Error al eliminar la generación";

            if (mensaje.contains("no encontrada")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", mensaje))
                        .build();
            }

            if (mensaje.contains("No se puede eliminar")) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("error", mensaje))
                        .build();
            }

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", mensaje))
                    .build();
        }
    }

    /**
     * Elimina un registro de GeneracionArchivoPetro por ID.
     *
     * Delega en el mismo proceso que /eliminar/{codigoGeneracion} para que el
     * borrado siempre pase por las validaciones y arrastre el detalle.
     *
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO DELETE");
        return eliminarGeneracion(id, usuario);
    }
}