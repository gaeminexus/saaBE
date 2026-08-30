package com.saa.ws.rest.crd;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.service.CuentaBancariaParticipeService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaConCertificado;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.NombreEntidadesCredito;

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

@Path("cnbp")
public class CuentaBancariaParticipeRest {

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @EJB
    private CuentaBancariaParticipeService cuentaBancariaParticipeService;

    @EJB
    private FileService fileService;

    @Context
    private UriInfo context;

    public CuentaBancariaParticipeRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CuentaBancariaParticipe> lista = cuentaBancariaParticipeDaoService.selectAll(NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener CuentaBancariaParticipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CuentaBancariaParticipe cuenta = cuentaBancariaParticipeDaoService.selectById(id, NombreEntidadesCredito.CUENTA_BANCARIA_PARTICIPE);
            if (cuenta == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("CuentaBancariaParticipe con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cuenta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener CuentaBancariaParticipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getByParent/{idParent}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByParent(@PathParam("idParent") Long idParent) {
        try {
            List<CuentaBancariaParticipe> lista = cuentaBancariaParticipeDaoService.selectByParent(idParent);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener CuentaBancariaParticipe por entidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaBancariaParticipe registro, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO PUT - CNBP");
        try {
            CuentaBancariaParticipe resultado = cuentaBancariaParticipeService.saveSingle(registro, usuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar CuentaBancariaParticipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * ⛔ BLOQUEADO A PROPÓSITO. No se puede registrar una cuenta bancaria de un partícipe sin
     * adjuntar el PDF de su certificado bancario (regla de negocio de la pantalla de
     * entidad-participe-info). Verificado el 2026-08-25: ningún proceso del backend crea
     * CuentaBancariaParticipe por este camino (el único escritor era este mismo endpoint desde
     * el frontend) — cerrarlo no rompe nada más. Crear cuentas SOLO por
     * {@link #postConCertificado}.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CuentaBancariaParticipe registro) {
        System.out.println("LLEGA AL SERVICIO POST - CNBP (bloqueado: falta el certificado bancario)");
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("No se puede crear una cuenta bancaria de partícipe sin adjuntar su certificado"
                    + " bancario en PDF. Use POST /rest/cnbp/conCertificado (multipart, campo 'archivo').")
                .type(MediaType.APPLICATION_JSON).build();
    }

    /**
     * Crea la cuenta bancaria y su certificado bancario (PDF) en UNA transacción: si algo falla,
     * no queda ni cuenta huérfana en CRD.CNBP ni archivo colgado en disco. Es el ÚNICO camino
     * soportado para crear una cuenta bancaria de partícipe.
     *
     * Contrato multipart/form-data — campos exactos:
     * <pre>
     *   archivo          : InputStream — el PDF del certificado bancario
     *   archivoNombre    : String      — nombre original del archivo, con encodeURIComponent()
     *                                    aplicado por el frontend (acá se decodifica con
     *                                    URLDecoder/UTF-8; sin esto un nombre con tilde o eñe
     *                                    llega corrupto — mismo caso que CargaMarcacionesRest)
     *   idEntidad        : String (Long) — código del partícipe (CRD.ENTD)
     *   idBancoExterno   : String (Long) — código del banco (TSR.BEXT)
     *   tipoCuenta       : String (Long) — codigoAlterno del DetalleRubro de tipo de cuenta
     *   numeroCuenta     : String
     *   usuarioRegistro  : String — opcional, para bitácora del adjunto
     * </pre>
     * Los campos numéricos van como String a propósito: un @FormParam declarado Long que no
     * convierte lo rechaza RESTEasy con un 400 sin cuerpo ANTES de despachar el método (mismo
     * criterio que CargaMarcacionesRest, ExtractoBancarioRest y SaldoAperturaRest).
     *
     * @return 201 con {@code ResultadoCuentaBancariaConCertificado} ({@code cuenta} +
     *         {@code certificado})
     */
    @POST
    @Path("/conCertificado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postConCertificado(
            @FormParam("archivo") InputStream archivo,
            @FormParam("archivoNombre") String archivoNombre,
            @FormParam("idEntidad") String idEntidadParam,
            @FormParam("idBancoExterno") String idBancoExternoParam,
            @FormParam("tipoCuenta") String tipoCuentaParam,
            @FormParam("numeroCuenta") String numeroCuenta,
            @FormParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO POST conCertificado - CNBP, archivo: " + archivoNombre);
        try {
            if (archivo == null || archivoNombre == null || archivoNombre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se ha enviado el certificado bancario (campo 'archivo')")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            // El frontend manda el nombre con encodeURIComponent() porque el proveedor de
            // multipart no declara charset para campos de texto planos y puede no decodificar
            // como UTF-8 — decodificar acá evita que un nombre con tilde o eñe llegue corrupto.
            archivoNombre = URLDecoder.decode(archivoNombre, StandardCharsets.UTF_8);

            Long idEntidad = parseId(idEntidadParam);
            Long idBancoExterno = parseId(idBancoExternoParam);
            Long tipoCuenta = parseId(tipoCuentaParam);
            if (idEntidad == null || idBancoExterno == null || tipoCuenta == null
                    || numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("idEntidad, idBancoExterno, tipoCuenta y numeroCuenta son obligatorios")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            SolicitudCuentaBancariaConCertificado solicitud = new SolicitudCuentaBancariaConCertificado();
            solicitud.setIdEntidad(idEntidad);
            solicitud.setIdBancoExterno(idBancoExterno);
            solicitud.setTipoCuenta(tipoCuenta);
            solicitud.setNumeroCuenta(numeroCuenta);
            solicitud.setArchivo(archivo);
            solicitud.setNombreArchivo(archivoNombre);
            solicitud.setUsuarioRegistro(usuarioRegistro);

            ResultadoCuentaBancariaConCertificado resultado =
                cuentaBancariaParticipeService.crearConCertificado(solicitud);

            return Response.status(Response.Status.CREATED)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al crear CuentaBancariaParticipe con certificado: " + e.getMessage());
            e.printStackTrace();
            return respuestaError(e);
        }
    }

    /**
     * Metadatos del certificado bancario de una cuenta (para mostrar nombre/fecha en pantalla
     * antes de descargar). 404 si la cuenta no tiene certificado registrado.
     */
    @GET
    @Path("/{id}/certificado")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCertificado(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getCertificado - CNBP id: " + id);
        try {
            Adjunto certificado = cuentaBancariaParticipeService.obtenerCertificado(id);
            if (certificado == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("La cuenta " + id + " no tiene certificado bancario registrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(certificado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * Descarga el PDF del certificado bancario de una cuenta.
     */
    @GET
    @Path("/{id}/certificado/descargar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarCertificado(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO descargarCertificado - CNBP id: " + id);
        try {
            Adjunto certificado = cuentaBancariaParticipeService.obtenerCertificado(id);
            if (certificado == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("La cuenta " + id + " no tiene certificado bancario registrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (certificado.getUrlArchivo() == null || !fileService.fileExists(certificado.getUrlArchivo())) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("El archivo del certificado ya no existe en el servidor")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            InputStream contenido = fileService.downloadFile(certificado.getUrlArchivo());
            String nombreDescarga = certificado.getNombreArchivo() != null
                ? certificado.getNombreArchivo() : ("certificado_" + id + ".pdf");

            return Response.ok(contenido)
                    .header("Content-Disposition", "attachment; filename=\"" + nombreDescarga + "\"")
                    .header("Content-Type", "application/pdf")
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al descargar el certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - CNBP id: " + id);
        try {
            List<Long> ids = new java.util.ArrayList<>();
            ids.add(id);
            cuentaBancariaParticipeService.remove(ids);
            return Response.status(Response.Status.OK)
                    .entity("CuentaBancariaParticipe eliminada correctamente")
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar CuentaBancariaParticipe: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CNBP");
        try {
            return Response.status(Response.Status.OK)
                    .entity(cuentaBancariaParticipeService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Los identificadores de un multipart se reciben como String y se parsean acá. Un
     * @FormParam declarado Long que no convierte lo rechaza RESTEasy con un 400 sin cuerpo ANTES
     * de despachar el método. Mismo criterio que CargaMarcacionesRest.
     */
    private Long parseId(String valor) {
        try {
            return (valor == null || valor.trim().isEmpty()) ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Mapea el CODIGO con el que CuentaBancariaParticipeService prefija sus IncomeException
     * ({@code CODIGO: descripción}) al status HTTP. Lo que no tiene código reconocido cae en 422
     * si es una regla de negocio, o 500 si es inesperado.
     */
    private Response respuestaError(Throwable e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (CuentaBancariaParticipeService.ERR_PARAMETRO_INVALIDO.equals(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (CuentaBancariaParticipeService.ERR_ENTIDAD_NO_ENCONTRADA.equals(codigo)
                || CuentaBancariaParticipeService.ERR_BANCO_NO_ENCONTRADO.equals(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (CuentaBancariaParticipeService.ERR_EXTENSION_NO_PERMITIDA.equals(codigo)
                || CuentaBancariaParticipeService.ERR_ARCHIVO_MUY_GRANDE.equals(codigo)
                || CuentaBancariaParticipeService.ERR_ARCHIVO_VACIO.equals(codigo)) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else if (CuentaBancariaParticipeService.ERR_TIPO_ADJUNTO_NO_CONFIGURADO.equals(codigo)) {
            // No es un error del usuario: falta cargar el catálogo TPDJ en este ambiente.
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        } else if (e instanceof com.saa.basico.util.IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return Response.status(status).entity(mensaje).type(MediaType.APPLICATION_JSON).build();
    }
}
