/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ws.rest.tsr;

import java.io.InputStream;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.ExtractoBancarioDaoService;
import com.saa.ejb.tsr.service.ExtractoBancarioService;
import com.saa.ejb.tsr.service.ImportacionExtractoBancarioService;
import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.ResumenImportacionExtracto;

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

/**
 * @author GaemiSoft
 * <p>REST endpoint para ExtractoBancario.
 * Path: /exbc</p>
 */
@Path("exbc")
public class ExtractoBancarioRest {

    @EJB
    private ExtractoBancarioDaoService extractoBancarioDaoService;

    @EJB
    private ExtractoBancarioService extractoBancarioService;

    @EJB
    private ImportacionExtractoBancarioService importacionExtractoBancarioService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ExtractoBancarioRest() {
    }

    /**
     * Obtiene todos los registros de ExtractoBancario.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ExtractoBancario> lista = extractoBancarioDaoService
                    .selectAll(NombreEntidadesTesoreria.EXTRACTO_BANCARIO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Obtiene un ExtractoBancario por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ExtractoBancario extracto = extractoBancarioDaoService
                    .selectById(id, NombreEntidadesTesoreria.EXTRACTO_BANCARIO);
            if (extracto == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("ExtractoBancario con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(extracto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Busca un extracto por el hash de su archivo origen (control de duplicados).
     */
    @GET
    @Path("/getByHash/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByHash(@PathParam("hash") String hash) {
        try {
            ExtractoBancario extracto = extractoBancarioDaoService.selectByHash(hash);
            if (extracto == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No existe extracto con hash " + hash)
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(extracto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al buscar extracto por hash: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza un registro existente (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ExtractoBancario registro) {
        System.out.println("LLEGA AL SERVICIO PUT - EXTRACTO_BANCARIO");
        try {
            ExtractoBancario resultado = extractoBancarioService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Crea un nuevo registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ExtractoBancario registro) {
        System.out.println("LLEGA AL SERVICIO POST - EXTRACTO_BANCARIO");
        try {
            ExtractoBancario resultado = extractoBancarioService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Búsqueda por criterios dinámicos.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de EXTRACTO_BANCARIO");
        try {
            List<ExtractoBancario> lista = extractoBancarioService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria ExtractoBancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Previsualiza la importacion de un archivo de estado de cuenta bancario
     * sin persistir nada - parsea el archivo con el parser que corresponda
     * al banco de la cuenta indicada y devuelve un resumen (periodo, saldos,
     * totales, advertencias de saldo, aviso si el archivo ya fue cargado).
     */
    @POST
    @Path("/importar/validar/{idCuentaBancaria}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarImportacion(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @FormParam("archivo") InputStream archivoInputStream,
            @FormParam("archivoNombre") String archivoNombre) {
        System.out.println("LLEGA AL SERVICIO VALIDAR IMPORTACION - EXTRACTO_BANCARIO, cuenta: " + idCuentaBancaria);
        try {
            if (archivoInputStream == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            ResumenImportacionExtracto resumen = importacionExtractoBancarioService
                    .validar(archivoInputStream, archivoNombre, idCuentaBancaria);
            return Response.status(Response.Status.OK).entity(resumen).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al validar importacion de extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reparsea el archivo y guarda el lote completo (ExtractoBancario +
     * DetalleExtractoBancario). Rechaza el archivo si su hash ya fue
     * cargado previamente.
     */
    @POST
    @Path("/importar/confirmar/{idCuentaBancaria}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarImportacion(@PathParam("idCuentaBancaria") Long idCuentaBancaria,
            @FormParam("archivo") InputStream archivoInputStream,
            @FormParam("archivoNombre") String archivoNombre,
            @FormParam("idEmpresa") String idEmpresaParam,
            @FormParam("usuarioCreacion") String usuarioCreacion) {
        System.out.println("LLEGA AL SERVICIO CONFIRMAR IMPORTACION - EXTRACTO_BANCARIO, cuenta: " + idCuentaBancaria
                + ", empresa: " + idEmpresaParam);
        try {
            if (archivoInputStream == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el archivo").type(MediaType.APPLICATION_JSON).build();
            }
            Long idEmpresa;
            try {
                idEmpresa = (idEmpresaParam == null || idEmpresaParam.trim().isEmpty())
                        ? null : Long.valueOf(idEmpresaParam.trim());
            } catch (NumberFormatException nfe) {
                idEmpresa = null;
            }
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El campo idEmpresa es obligatorio").type(MediaType.APPLICATION_JSON).build();
            }
            ExtractoBancario resultado = importacionExtractoBancarioService
                    .confirmar(archivoInputStream, archivoNombre, idCuentaBancaria, idEmpresa, usuarioCreacion);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al confirmar importacion de extracto bancario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina registros por lista de IDs.
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(List<Long> ids) {
        System.out.println("LLEGA AL SERVICIO DELETE - EXTRACTO_BANCARIO");
        try {
            extractoBancarioService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("Extractos bancarios eliminados correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar extractos bancarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
