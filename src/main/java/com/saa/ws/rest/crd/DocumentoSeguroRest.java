package com.saa.ws.rest.crd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.DocumentoSeguroService;
import com.saa.ejb.crd.service.SumaAseguradaService;
import com.saa.ejb.crd.service.dto.CandidatoSeguro;
import com.saa.ejb.crd.service.dto.DocumentoSeguroDTO;
import com.saa.ejb.crd.service.dto.ExclusionSeguro;
import com.saa.ejb.crd.service.dto.NovedadesSeguro;
import com.saa.ejb.crd.service.dto.PreviewDistribucionSeguro;
import com.saa.ejb.crd.service.dto.PrestamoSeguroDTO;
import com.saa.ejb.crd.service.dto.ResultadoAnularSeguro;
import com.saa.ejb.crd.service.dto.ResultadoCargaSumaAsegurada;
import com.saa.ejb.crd.service.dto.SolicitudDocumentoSeguro;
import com.saa.ejb.crd.service.dto.SolicitudGenerarListado;
import com.saa.ejb.crd.service.dto.SolicitudNotaSeguro;
import com.saa.ejb.crd.service.dto.SolicitudSumaAsegurada;
import com.saa.ejb.crd.service.dto.SolicitudUsuarioMotivo;

import jakarta.ejb.EJB;
import jakarta.persistence.NoResultException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /rest/posg} — pólizas de seguro de préstamos. Contrato:
 * {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md}.
 */
@Path("posg")
public class DocumentoSeguroRest {

    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    @EJB
    private DocumentoSeguroService documentoSeguroService;

    @EJB
    private SumaAseguradaService sumaAseguradaService;

    // ========================================================================
    // §2 — Suma asegurada del bien
    // ========================================================================

    @PUT
    @Path("/sumaAsegurada")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizarSumaAsegurada(SolicitudSumaAsegurada solicitud) {
        System.out.println("DocumentoSeguroRest.actualizarSumaAsegurada");
        try {
            Double anterior = sumaAseguradaService.actualizar(
                solicitud != null ? solicitud.getIdPrestamo() : null,
                solicitud != null ? solicitud.getValor() : null,
                solicitud != null ? solicitud.getUsuario() : null);
            Map<String, Object> cuerpo = Map.of(
                "idPrestamo", solicitud.getIdPrestamo(),
                "valorAnterior", anterior != null ? anterior : Map.of(),
                "valor", solicitud.getValor());
            return Response.ok(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/sumaAsegurada/carga")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarSumaAsegurada(@MultipartForm MultipartFormDataInput input) {
        System.out.println("DocumentoSeguroRest.cargarSumaAsegurada");
        try {
            Map<String, List<InputPart>> form = input.getFormDataMap();
            InputStream archivo = parteArchivo(form, "archivo");
            boolean confirmar = Boolean.parseBoolean(parteTexto(form, "confirmar", "false"));
            String usuario = parteTexto(form, "usuario", null);

            ResultadoCargaSumaAsegurada resultado = sumaAseguradaService.cargarDesdeExcel(archivo, confirmar, usuario);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §3 — Listado (paso 1)
    // ========================================================================

    @GET
    @Path("/listado/preview")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewListado(@QueryParam("tipoSeguro") Long tipoSeguro,
            @QueryParam("fechaCorte") String fechaCorte) {
        try {
            List<CandidatoSeguro> resultado = documentoSeguroService.previewListado(tipoSeguro,
                parseFecha(fechaCorte));
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/listado/preview/excel")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response previewListadoExcel(@QueryParam("tipoSeguro") Long tipoSeguro,
            @QueryParam("fechaCorte") String fechaCorte) {
        try {
            List<CandidatoSeguro> candidatos = documentoSeguroService.previewListado(tipoSeguro,
                parseFecha(fechaCorte));
            byte[] bytes = excelListado(candidatos);
            return respuestaExcel(bytes, "listado_preview.xlsx");
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/listado")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarListado(SolicitudGenerarListado solicitud) {
        try {
            DocumentoSeguroDTO resultado = documentoSeguroService.generarListado(solicitud);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/{id}/listado/excel")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response listadoExcel(@PathParam("id") Long id) {
        try {
            List<PrestamoSeguroDTO> prestamos = documentoSeguroService.getPrestamos(id);
            byte[] bytes = excelListadoDocumento(prestamos);
            return respuestaExcel(bytes, "listado_" + id + ".xlsx");
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §4 — Documento de la aseguradora (paso 2)
    // ========================================================================

    @PUT
    @Path("/{id}/documento")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarDocumento(@PathParam("id") Long id, SolicitudDocumentoSeguro solicitud) {
        try {
            DocumentoSeguroDTO resultado = documentoSeguroService.registrarDocumento(id, solicitud);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §5 — Distribución (paso 3)
    // ========================================================================

    @GET
    @Path("/{id}/distribucion/preview")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewDistribucion(@PathParam("id") Long id) {
        try {
            PreviewDistribucionSeguro resultado = documentoSeguroService.previewDistribucion(id);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/{id}/distribuir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response distribuir(@PathParam("id") Long id, SolicitudUsuarioMotivo solicitud) {
        try {
            DocumentoSeguroDTO resultado = documentoSeguroService.distribuir(id,
                solicitud != null ? solicitud.getUsuario() : null);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §6 — Anular
    // ========================================================================

    @POST
    @Path("/{id}/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, SolicitudUsuarioMotivo solicitud) {
        try {
            ResultadoAnularSeguro resultado = documentoSeguroService.anular(id,
                solicitud != null ? solicitud.getUsuario() : null,
                solicitud != null ? solicitud.getMotivo() : null);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §7 — Novedades y notas
    // ========================================================================

    @GET
    @Path("/{idFactura}/novedades")
    @Produces(MediaType.APPLICATION_JSON)
    public Response novedades(@PathParam("idFactura") Long idFactura, @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        try {
            NovedadesSeguro resultado = documentoSeguroService.novedades(idFactura, parseFecha(desde),
                parseFecha(hasta));
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/{idFactura}/novedades/excel")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response novedadesExcel(@PathParam("idFactura") Long idFactura, @QueryParam("desde") String desde,
            @QueryParam("hasta") String hasta) {
        try {
            NovedadesSeguro novedades = documentoSeguroService.novedades(idFactura, parseFecha(desde),
                parseFecha(hasta));
            byte[] bytes = excelNovedades(novedades);
            return respuestaExcel(bytes, "novedades_" + idFactura + ".xlsx");
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @POST
    @Path("/{idFactura}/nota")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrarNota(@PathParam("idFactura") Long idFactura, SolicitudNotaSeguro solicitud) {
        try {
            DocumentoSeguroDTO resultado = documentoSeguroService.registrarNota(idFactura, solicitud);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §8 — Consultas
    // ========================================================================

    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("tipoSeguro") Long tipoSeguro, @QueryParam("estado") Long estado,
            @QueryParam("clase") Long clase) {
        try {
            List<DocumentoSeguroDTO> resultado = documentoSeguroService.listar(tipoSeguro, estado, clase);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            DocumentoSeguroDTO resultado = documentoSeguroService.getById(id);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/{id}/prestamos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrestamos(@PathParam("id") Long id) {
        try {
            List<PrestamoSeguroDTO> resultado = documentoSeguroService.getPrestamos(id);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ========================================================================
    // §9 — Liberar a pago (ESPERA el contrato de omen-saa-2, ÍTEM 8 — no tocar CXP)
    // ========================================================================

    @POST
    @Path("/{id}/liberar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response liberar(@PathParam("id") Long id) {
        return Response.status(Response.Status.CONFLICT)
            .entity("{\"mensaje\": \"INTEGRACION_CXP_PENDIENTE: la liberación a pago espera el contrato de"
                + " integración con CXP (omen-saa-2)\"}")
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    // ========================================================================
    // Multipart
    // ========================================================================

    private InputStream parteArchivo(Map<String, List<InputPart>> form, String nombre) throws Throwable {
        List<InputPart> partes = form.get(nombre);
        if (partes == null || partes.isEmpty()) {
            throw new IncomeException(SumaAseguradaService.ERR_ARCHIVO_INVALIDO
                + ": no se recibió el archivo ('" + nombre + "'). Campos recibidos: " + form.keySet());
        }
        InputStream archivo = partes.get(0).getBody(InputStream.class, null);
        if (archivo == null) {
            throw new IncomeException(SumaAseguradaService.ERR_ARCHIVO_INVALIDO + ": el archivo llegó vacío");
        }
        return archivo;
    }

    private String parteTexto(Map<String, List<InputPart>> form, String nombre, String porDefecto) throws Throwable {
        List<InputPart> partes = form.get(nombre);
        if (partes == null || partes.isEmpty()) {
            return porDefecto;
        }
        return partes.get(0).getBodyAsString();
    }

    // ========================================================================
    // Excel — exportaciones (ÍTEM 11)
    // ========================================================================

    private byte[] excelListado(List<CandidatoSeguro> candidatos) throws Throwable {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Listado");
            Row encabezado = hoja.createRow(0);
            String[] titulos = {"Número de préstamo", "Cédula", "Partícipe", "Tipo de préstamo", "Estado", "Base"};
            for (int i = 0; i < titulos.length; i++) {
                encabezado.createCell(i).setCellValue(titulos[i]);
            }
            int fila = 1;
            for (CandidatoSeguro c : candidatos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(c.getNumeroPrestamo() != null ? c.getNumeroPrestamo() : "");
                r.createCell(1).setCellValue(c.getCedula() != null ? c.getCedula() : "");
                r.createCell(2).setCellValue(c.getNombreParticipe() != null ? c.getNombreParticipe() : "");
                r.createCell(3).setCellValue(c.getTipoPrestamo() != null ? c.getTipoPrestamo() : "");
                r.createCell(4).setCellValue(c.getEstadoPrestamo() != null ? c.getEstadoPrestamo() : 0);
                r.createCell(5).setCellValue(c.getBase());
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private byte[] excelListadoDocumento(List<PrestamoSeguroDTO> prestamos) throws Throwable {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Listado");
            Row encabezado = hoja.createRow(0);
            String[] titulos = {"Número de préstamo", "Cédula", "Partícipe", "Base"};
            for (int i = 0; i < titulos.length; i++) {
                encabezado.createCell(i).setCellValue(titulos[i]);
            }
            int fila = 1;
            for (PrestamoSeguroDTO p : prestamos) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(p.getNumeroPrestamo() != null ? p.getNumeroPrestamo() : "");
                r.createCell(1).setCellValue(p.getCedula() != null ? p.getCedula() : "");
                r.createCell(2).setCellValue(p.getNombreParticipe() != null ? p.getNombreParticipe() : "");
                r.createCell(3).setCellValue(p.getBase());
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private byte[] excelNovedades(NovedadesSeguro novedades) throws Throwable {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet inclusiones = libro.createSheet("Inclusiones");
            Row encInc = inclusiones.createRow(0);
            String[] titulosInc = {"Número de préstamo", "Cédula", "Partícipe", "Tipo de préstamo", "Base"};
            for (int i = 0; i < titulosInc.length; i++) {
                encInc.createCell(i).setCellValue(titulosInc[i]);
            }
            int fila = 1;
            for (CandidatoSeguro c : novedades.getInclusiones()) {
                Row r = inclusiones.createRow(fila++);
                r.createCell(0).setCellValue(c.getNumeroPrestamo() != null ? c.getNumeroPrestamo() : "");
                r.createCell(1).setCellValue(c.getCedula() != null ? c.getCedula() : "");
                r.createCell(2).setCellValue(c.getNombreParticipe() != null ? c.getNombreParticipe() : "");
                r.createCell(3).setCellValue(c.getTipoPrestamo() != null ? c.getTipoPrestamo() : "");
                r.createCell(4).setCellValue(c.getBase());
            }

            Sheet exclusiones = libro.createSheet("Exclusiones");
            Row encExc = exclusiones.createRow(0);
            String[] titulosExc = {"Número de préstamo", "Cédula", "Partícipe", "Base", "Motivo", "Fecha"};
            for (int i = 0; i < titulosExc.length; i++) {
                encExc.createCell(i).setCellValue(titulosExc[i]);
            }
            fila = 1;
            for (ExclusionSeguro ex : novedades.getExclusiones()) {
                Row r = exclusiones.createRow(fila++);
                r.createCell(0).setCellValue(ex.getNumeroPrestamo() != null ? ex.getNumeroPrestamo() : "");
                r.createCell(1).setCellValue(ex.getCedula() != null ? ex.getCedula() : "");
                r.createCell(2).setCellValue(ex.getNombreParticipe() != null ? ex.getNombreParticipe() : "");
                r.createCell(3).setCellValue(ex.getBase());
                r.createCell(4).setCellValue(ex.getMotivo() != null ? ex.getMotivo() : "");
                r.createCell(5).setCellValue(ex.getFecha() != null ? ex.getFecha().toString() : "");
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private Response respuestaExcel(byte[] bytes, String nombreArchivo) {
        return Response.ok(new ByteArrayInputStream(bytes))
            .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .build();
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private LocalDate parseFecha(String fecha) {
        return fecha != null && !fecha.trim().isEmpty() ? LocalDate.parse(fecha.trim()) : null;
    }

    private Response respuestaError(Throwable e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        int status;
        if (DocumentoSeguroService.ERR_DOCUMENTO_NO_ENCONTRADO.equals(codigo)
                || DocumentoSeguroService.ERR_PRESTAMO_NO_ENCONTRADO.equals(codigo)
                || SumaAseguradaService.ERR_PRESTAMO_NO_ENCONTRADO.equals(codigo)) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (DocumentoSeguroService.ERR_SIN_CUOTAS_EN_VIGENCIA.equals(codigo)
                || DocumentoSeguroService.ERR_DISTRIBUCION_NO_CUADRA.equals(codigo)) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else if (DocumentoSeguroService.ERR_HAY_PRESTAMOS_SIN_SUMA_ASEGURADA.equals(codigo)
                || DocumentoSeguroService.ERR_ESTADO_INVALIDO.equals(codigo)
                || DocumentoSeguroService.ERR_CLAVE_ACCESO_DUPLICADA.equals(codigo)
                || DocumentoSeguroService.ERR_CUOTA_YA_CUBIERTA.equals(codigo)
                || DocumentoSeguroService.ERR_LIBERADO_A_PAGO.equals(codigo)
                || DocumentoSeguroService.ERR_NOTAS_VIVAS.equals(codigo)
                || DocumentoSeguroService.ERR_FACTURA_INVALIDA.equals(codigo)
                || DocumentoSeguroService.ERR_INTEGRACION_CXP_PENDIENTE.equals(codigo)
                || SumaAseguradaService.ERR_NO_ES_HIPOTECARIO_NI_PRENDARIO.equals(codigo)) {
            status = Response.Status.CONFLICT.getStatusCode();
        } else if (DocumentoSeguroService.ERR_PARAMETRO_INVALIDO.equals(codigo)
                || DocumentoSeguroService.ERR_VALOR_INVALIDO.equals(codigo)
                || DocumentoSeguroService.ERR_FECHA_INVALIDA.equals(codigo)
                || DocumentoSeguroService.ERR_CLAVE_ACCESO_OBLIGATORIA.equals(codigo)
                || DocumentoSeguroService.ERR_MOTIVO_OBLIGATORIO.equals(codigo)
                || SumaAseguradaService.ERR_VALOR_INVALIDO.equals(codigo)
                || SumaAseguradaService.ERR_ARCHIVO_INVALIDO.equals(codigo)
                || SumaAseguradaService.ERR_PARAMETRO_INVALIDO.equals(codigo)) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
        } else if (e instanceof NoResultException) {
            status = Response.Status.NOT_FOUND.getStatusCode();
        } else if (e instanceof IncomeException) {
            status = HTTP_REGLA_DE_NEGOCIO;
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        }
        return Response.status(status).entity("{\"mensaje\": \"" + mensaje.replace("\"", "'") + "\"}")
            .type(MediaType.APPLICATION_JSON).build();
    }
}
