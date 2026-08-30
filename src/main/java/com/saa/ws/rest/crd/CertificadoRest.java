package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.CertificadoDaoService;
import com.saa.ejb.crd.service.CertificadoService;
import com.saa.ejb.crd.service.dto.PrecargaCertificado;
import com.saa.ejb.crd.service.dto.ResultadoEmisionCertificado;
import com.saa.ejb.crd.service.dto.SolicitudEmisionCertificado;
import com.saa.model.crd.Certificado;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Certificados de partícipe (CRD.CRTF).
 *
 * Solo consulta, reimpresión y anulación. No hay POST/PUT/DELETE genéricos: un certificado
 * no se crea ni se edita a mano. El endpoint de EMISIÓN se agrega cuando quede fijado el
 * contrato de API con el frontend (precarga + captura del operador).
 */
@Path("crtf")
public class CertificadoRest {

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private CertificadoDaoService certificadoDaoService;

    @EJB
    private CertificadoService certificadoService;

    @Context
    private UriInfo context;

    public CertificadoRest() {}

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Certificado> lista = certificadoDaoService.selectAll(NombreEntidadesCredito.CERTIFICADO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Certificado certificado = certificadoDaoService.selectById(id, NombreEntidadesCredito.CERTIFICADO);
            return Response.status(Response.Status.OK).entity(certificado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Certificados de un partícipe (sin el PDF), del más reciente al más antiguo. */
    @GET
    @Path("/getByEntidad/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByEntidad(@PathParam("idEntidad") Long idEntidad) {
        try {
            List<Certificado> lista = certificadoService.listarPorEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener Certificado por entidad: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Certificados de un año, por número (para revisar la serie y sus anulados). */
    @GET
    @Path("/getByAnio/{anio}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByAnio(@PathParam("anio") Long anio) {
        try {
            List<Certificado> lista = certificadoDaoService.selectByAnio(anio);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener Certificado por anio: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Reimpresión: el PDF guardado al emitir, tal cual. Responde application/pdf con el
     * número del certificado como nombre de archivo.
     */
    @GET
    @Path("/pdf/{id}")
    @Produces("application/pdf")
    public Response pdf(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET PDF - CRTF: " + id);
        try {
            // obtenerPdf primero: traduce "no existe" a IncomeException (404); selectById lanzaría NoResultException (500)
            byte[] pdf = certificadoService.obtenerPdf(id);
            Certificado certificado = certificadoService.selectById(id);
            String nombre = (certificado.getNumeroAlterno() != null ? certificado.getNumeroAlterno() : "certificado-" + id)
                    + ".pdf";
            return Response.ok(pdf)
                    .header("Content-Disposition", "inline; filename=\"" + nombre + "\"")
                    .header("Content-Type", "application/pdf")
                    .build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el PDF del Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Precarga (§3.1 del contrato): lo que el sistema resuelve, el origen de cada campo y
     * los bloqueos. No escribe nada.
     */
    @GET
    @Path("/precarga/{idEntidad}/{tipo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response precarga(@PathParam("idEntidad") Long idEntidad,
                             @PathParam("tipo") Long tipo,
                             @QueryParam("idPrestamo") Long idPrestamo,
                             @QueryParam("idLiquidacion") Long idLiquidacion) {
        System.out.println("LLEGA AL SERVICIO GET PRECARGA - CRTF: entidad " + idEntidad + " tipo " + tipo);
        try {
            PrecargaCertificado precarga = certificadoService.precargar(idEntidad, tipo, idPrestamo, idLiquidacion);
            return Response.status(Response.Status.OK).entity(precarga).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(HTTP_REGLA_DE_NEGOCIO)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la precarga del Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Emisión (§3.2 del contrato). 422 ante un bloqueo o un campo faltante; 500 si falló
     * el reporte o la base — en ambos casos nada quedó grabado y ningún número se consumió.
     */
    @POST
    @Path("/emitir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response emitir(SolicitudEmisionCertificado solicitud) {
        System.out.println("LLEGA AL SERVICIO POST EMITIR - CRTF: entidad "
                + (solicitud != null ? solicitud.getIdEntidad() : null)
                + " tipo " + (solicitud != null ? solicitud.getTipo() : null));
        try {
            ResultadoEmisionCertificado resultado = certificadoService.emitir(solicitud);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(HTTP_REGLA_DE_NEGOCIO)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            // Si el EJB envolvió la IncomeException (EJBException), rescatar el mensaje de negocio
            Throwable causa = e;
            while (causa.getCause() != null && causa.getCause() != causa) {
                causa = causa.getCause();
                if (causa instanceof IncomeException) {
                    return Response.status(HTTP_REGLA_DE_NEGOCIO)
                            .entity(causa.getMessage())
                            .type(MediaType.APPLICATION_JSON).build();
                }
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al emitir el Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Anula un certificado. El número queda ocupado y documentado con el motivo. */
    @POST
    @Path("/anular/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id,
                           @QueryParam("motivo") String motivo,
                           @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST ANULAR - CRTF: " + id);
        try {
            Certificado certificado = certificadoService.anular(id, motivo, usuario);
            return Response.status(Response.Status.OK).entity(certificado).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(HTTP_REGLA_DE_NEGOCIO)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<Certificado> lista = certificadoService.selectByCriteria(datos);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la busqueda de Certificado: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
