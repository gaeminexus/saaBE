package com.saa.ws.rest.crd;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.CuentaBancariaBeneficiarioDaoService;
import com.saa.ejb.crd.service.CuentaBancariaBeneficiarioService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaBeneficiarioConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaBeneficiarioConCertificado;
import com.saa.model.crd.CuentaBancariaBeneficiario;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Beneficiarios del partícipe (CRD.CBBP), sepelio fase 2a. Ver
 * {@code docs/logica-negocio/crd/API-BENEFICIARIOS-PARTICIPE.md}.
 *
 * ⛔ No hay DELETE: un beneficiario se inactiva (PUT con estado = 2), no se borra — es la prueba
 * de a quién se le pagó o se le iba a pagar la plata de un fallecido (§3.5 del contrato).
 */
@Path("cbbp")
public class CuentaBancariaBeneficiarioRest {

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private CuentaBancariaBeneficiarioDaoService cuentaBancariaBeneficiarioDaoService;

    @EJB
    private CuentaBancariaBeneficiarioService cuentaBancariaBeneficiarioService;

    public CuentaBancariaBeneficiarioRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO getAll - CBBP");
        try {
            List<CuentaBancariaBeneficiario> lista =
                cuentaBancariaBeneficiarioDaoService.selectAll(NombreEntidadesCredito.CUENTA_BANCARIA_BENEFICIARIO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener CuentaBancariaBeneficiario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO getId - CBBP id: " + id);
        try {
            CuentaBancariaBeneficiario beneficiario =
                cuentaBancariaBeneficiarioDaoService.selectById(id, NombreEntidadesCredito.CUENTA_BANCARIA_BENEFICIARIO);
            if (beneficiario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("CuentaBancariaBeneficiario con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(beneficiario).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener CuentaBancariaBeneficiario: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Los beneficiarios de un partícipe, activos e inactivos, ordenados por porcentaje
     * descendente y luego por código (§3.1 del contrato). Es la consulta que alimenta la
     * pantalla.
     */
    @GET
    @Path("/porEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO porEntidad - CBBP idEntidad: " + idEntidad);
        try {
            List<CuentaBancariaBeneficiario> lista = cuentaBancariaBeneficiarioDaoService.selectPorEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al consultar beneficiarios: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Actualiza porcentaje, estado, tipoCuenta, numeroCuenta, bancoExterno y nombre de un
     * beneficiario existente. NO cambia entidad ni numeroIdentificacion (§3.4 del contrato).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CuentaBancariaBeneficiario registro) {
        System.out.println("LLEGA AL SERVICIO PUT - CBBP");
        try {
            CuentaBancariaBeneficiario resultado = cuentaBancariaBeneficiarioService.actualizar(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * Crea el beneficiario y su certificado bancario (PDF) en UNA transacción: si algo falla, no
     * queda ni beneficiario huérfano en CRD.CBBP ni archivo colgado en disco. Es el ÚNICO camino
     * soportado para crear un beneficiario (§3.2 del contrato).
     *
     * Contrato multipart/form-data — campos exactos:
     * <pre>
     *   archivo               : InputStream — el PDF del certificado bancario
     *   archivoNombre         : String      — nombre original del archivo, con
     *                                         encodeURIComponent() aplicado por el frontend
     *                                         (acá se decodifica con URLDecoder/UTF-8)
     *   idEntidad              : String (Long) — código del partícipe (CRD.ENTD)
     *   nombre                 : String        — nombre completo del beneficiario
     *   numeroIdentificacion   : String        — cédula
     *   idBancoExterno         : String (Long) — código del banco (TSR.BEXT)
     *   tipoCuenta             : String (Long) — codigoAlterno del DetalleRubro de tipo de cuenta
     *   numeroCuenta           : String
     *   porcentaje             : String (BigDecimal) — con punto decimal
     *   usuarioRegistro        : String — opcional, para bitácora
     * </pre>
     * Los campos numéricos van como String a propósito: un @FormParam declarado Long/BigDecimal
     * que no convierte lo rechaza RESTEasy con un 400 sin cuerpo ANTES de despachar el método
     * (mismo criterio que CuentaBancariaParticipeRest).
     *
     * @return 201 con {@code ResultadoCuentaBancariaBeneficiarioConCertificado}
     *         ({@code beneficiario} + {@code certificado})
     */
    @POST
    @Path("/conCertificado")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postConCertificado(
            @FormParam("archivo") InputStream archivo,
            @FormParam("archivoNombre") String archivoNombre,
            @FormParam("idEntidad") String idEntidadParam,
            @FormParam("nombre") String nombre,
            @FormParam("numeroIdentificacion") String numeroIdentificacion,
            @FormParam("idBancoExterno") String idBancoExternoParam,
            @FormParam("tipoCuenta") String tipoCuentaParam,
            @FormParam("numeroCuenta") String numeroCuenta,
            @FormParam("porcentaje") String porcentajeParam,
            @FormParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO POST conCertificado - CBBP, archivo: " + archivoNombre);
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
            BigDecimal porcentaje = parsePorcentaje(porcentajeParam);
            if (idEntidad == null || idBancoExterno == null || tipoCuenta == null || porcentaje == null
                    || nombre == null || nombre.trim().isEmpty()
                    || numeroIdentificacion == null || numeroIdentificacion.trim().isEmpty()
                    || numeroCuenta == null || numeroCuenta.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("idEntidad, nombre, numeroIdentificacion, idBancoExterno, tipoCuenta,"
                            + " numeroCuenta y porcentaje son obligatorios")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            SolicitudCuentaBancariaBeneficiarioConCertificado solicitud = new SolicitudCuentaBancariaBeneficiarioConCertificado();
            solicitud.setIdEntidad(idEntidad);
            solicitud.setNombre(nombre);
            solicitud.setNumeroIdentificacion(numeroIdentificacion);
            solicitud.setIdBancoExterno(idBancoExterno);
            solicitud.setTipoCuenta(tipoCuenta);
            solicitud.setNumeroCuenta(numeroCuenta);
            solicitud.setPorcentaje(porcentaje);
            solicitud.setArchivo(archivo);
            solicitud.setNombreArchivo(archivoNombre);
            solicitud.setUsuarioRegistro(usuarioRegistro);

            ResultadoCuentaBancariaBeneficiarioConCertificado resultado =
                cuentaBancariaBeneficiarioService.crearConCertificado(solicitud);

            return Response.status(Response.Status.CREATED)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al crear CuentaBancariaBeneficiario con certificado: " + e.getMessage());
            e.printStackTrace();
            return respuestaError(e);
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de CBBP");
        try {
            return Response.status(Response.Status.OK)
                    .entity(cuentaBancariaBeneficiarioService.selectByCriteria(registros))
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
     * de despachar el método. Mismo criterio que CuentaBancariaParticipeRest.
     */
    private Long parseId(String valor) {
        try {
            return (valor == null || valor.trim().isEmpty()) ? null : Long.valueOf(valor.trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private BigDecimal parsePorcentaje(String valor) {
        try {
            return (valor == null || valor.trim().isEmpty()) ? null : new BigDecimal(valor.trim());
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    /**
     * Mapea el CODIGO con el que CuentaBancariaBeneficiarioService prefija sus IncomeException
     * ({@code CODIGO: descripción}) al status HTTP exacto del contrato (§3.2/§3.4): 400/404/409/500,
     * sin 422 — a diferencia de CNBP, este contrato no reserva ese código para CBBP.
     */
    private Response respuestaError(Throwable e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (CuentaBancariaBeneficiarioService.ERR_BENEFICIARIO_DUPLICADO.equals(codigo)) {
            status = Response.Status.CONFLICT.getStatusCode();
        } else if (CuentaBancariaBeneficiarioService.ERR_BENEFICIARIO_NO_ENCONTRADO.equals(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (CuentaBancariaBeneficiarioService.ERR_TIPO_ADJUNTO_NO_CONFIGURADO.equals(codigo)) {
            // No es un error del usuario: falta cargar el catálogo TPDJ en este ambiente.
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        } else if (CuentaBancariaBeneficiarioService.ERR_PARAMETRO_INVALIDO.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_ENTIDAD_NO_ENCONTRADA.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_BANCO_NO_ENCONTRADO.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_EXTENSION_NO_PERMITIDA.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_ARCHIVO_MUY_GRANDE.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_ARCHIVO_VACIO.equals(codigo)
                || CuentaBancariaBeneficiarioService.ERR_PORCENTAJE_INVALIDO.equals(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (e instanceof com.saa.basico.util.IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return Response.status(status).entity(mensaje).type(MediaType.APPLICATION_JSON).build();
    }
}
