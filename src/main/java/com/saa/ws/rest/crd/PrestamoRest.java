package com.saa.ws.rest.crd;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.AbonoCapitalPrestamoService;
import com.saa.ejb.crd.service.PrestamoService;
import com.saa.ejb.crd.service.ProcesoPagoPrestamoService;
import com.saa.ejb.crd.service.dto.ResultadoAbonoCapital;
import com.saa.ejb.crd.service.dto.ResultadoAnulacion;
import com.saa.ejb.crd.service.dto.ResultadoAplicacionPago;
import com.saa.ejb.crd.service.dto.ResultadoPagoConAportes;
import com.saa.ejb.crd.service.dto.ResultadoPrecancelacion;
import com.saa.ejb.crd.service.dto.SimulacionAbonoCapital;
import com.saa.ejb.crd.service.dto.SimulacionPrecancelacion;
import com.saa.ejb.crd.service.dto.SolicitudAbonoCapital;
import com.saa.ejb.crd.service.dto.SolicitudAnulacion;
import com.saa.ejb.crd.service.dto.SolicitudPagoConAportes;
import com.saa.ejb.crd.service.dto.SolicitudPagoCuota;
import com.saa.ejb.crd.service.dto.SolicitudPrecancelacion;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Prestamo;

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

@Path("prst")
public class PrestamoRest {
    
    @EJB
    private PrestamoDaoService prestamoDaoService;
    
    @EJB
    private PrestamoService prestamoService;

    @EJB
    private ProcesoPagoPrestamoService procesoPagoPrestamoService;

    @EJB
    private AbonoCapitalPrestamoService abonoCapitalPrestamoService;

    @Context
    private UriInfo context;
    
    /**
     * Constructor por defecto.
     */
    public PrestamoRest() {
        // Constructor vacío
    }
    
    /**
     * Obtiene todos los registros de Prestamo.
     * 
     * @return Response con lista de Prestamo
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Prestamo> prestamos = prestamoDaoService.selectAll(NombreEntidadesCredito.PRESTAMO);
            return Response.status(Response.Status.OK)
                    .entity(prestamos)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Obtiene un registro de Prestamo por su ID.
     * 
     * @param id Identificador del registro
     * @return Response con objeto Prestamo
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getId/{id}")
    public Response getId(@PathParam("id") Long id) {
        try {
            Prestamo prestamo = prestamoDaoService.selectById(id, NombreEntidadesCredito.PRESTAMO);
            if (prestamo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Prestamo con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (PUT).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro actualizado o creado
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO PUT DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.OK)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Crea o actualiza un registro de Prestamo (POST).
     * 
     * @param registro Objeto Prestamo
     * @return Response con registro creado o actualizado
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Prestamo registro) {
        System.out.println("LLEGA AL SERVICIO POST DE Prestamo");
        try {
            Prestamo resultado = prestamoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Consulta registros de Prestamo por criterios (dummy method para pruebas).
     * 
     * @param test Parámetro de prueba
     * @return Lista de Prestamo
     * @throws Throwable
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Prestamo Cuenta");
        Response respuesta = null;
    	try {
    		respuesta = Response.status(Response.Status.OK).entity(prestamoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
		} catch (Throwable e) {
			respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
		}
    	return respuesta;
    }
    
    /**
     * Elimina un registro de Prestamo por ID.
     * 
     * @param id Identificador del registro
     * @return Response con resultado de la eliminación
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DE Prestamo");
        try {
            Prestamo elimina = new Prestamo();
            prestamoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar préstamo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Genera la tabla de amortización para un préstamo.
     * 
     * @param id Identificador del préstamo
     * @param tieneCuotaCero Indica si tiene período de gracia (1=sí, 0=no). Por defecto 0.
     * @return Response con el préstamo actualizado
     */
    @POST
    @Path("/generarTablaAmortizacion/{id}/{tieneCuotaCero}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response generarTablaAmortizacion(
            @PathParam("id") Long id,
            @PathParam("tieneCuotaCero") Long tieneCuotaCero) {
        System.out.println("GENERAR TABLA DE AMORTIZACIÓN - Préstamo ID: " + id + ", Cuota 0: " + tieneCuotaCero);
        try {
            Prestamo prestamo = prestamoService.generarTablaAmortizacion(id, tieneCuotaCero);
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar tabla de amortización: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
    
    /**
     * Carga la tabla de amortización desde un archivo Excel.
     * 
     * @param id Identificador del préstamo
     * @param uploadedInputStream InputStream del archivo
     * @param fileDetail Detalles del archivo
     * @return Response con el préstamo actualizado
     */
    @POST
    @Path("/cargarTablaExcel/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarTablaAmortizacionDesdeExcel(
            @PathParam("id") Long id,
            @org.jboss.resteasy.annotations.providers.multipart.MultipartForm MultipartFormDataInput input) {
        System.out.println("CARGAR TABLA DE AMORTIZACIÓN DESDE EXCEL - Préstamo ID: " + id);
        
        InputStream inputStream = null;
        try {
            // Obtener el archivo del multipart form
            java.util.Map<String, java.util.List<org.jboss.resteasy.plugins.providers.multipart.InputPart>> uploadForm = 
                input.getFormDataMap();
            
            System.out.println("Partes del formulario recibidas: " + uploadForm.keySet());
            
            // Intentar obtener el archivo con diferentes nombres de campo
            java.util.List<org.jboss.resteasy.plugins.providers.multipart.InputPart> inputParts = uploadForm.get("file");
            
            if (inputParts == null || inputParts.isEmpty()) {
                // Intentar con otros nombres comunes
                inputParts = uploadForm.get("archivo");
                if (inputParts == null || inputParts.isEmpty()) {
                    inputParts = uploadForm.get("fileData");
                }
            }
            
            if (inputParts == null || inputParts.isEmpty()) {
                System.out.println("ERROR: No se encontró el archivo en ningún campo conocido");
                System.out.println("Campos disponibles: " + uploadForm.keySet());
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("No se recibió ningún archivo. Campos disponibles: " + uploadForm.keySet())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            
            org.jboss.resteasy.plugins.providers.multipart.InputPart inputPart = inputParts.get(0);
            
            // Obtener el InputStream del archivo
            inputStream = inputPart.getBody(InputStream.class, null);
            
            if (inputStream == null) {
                System.out.println("ERROR: El InputStream del archivo es null");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El archivo recibido está vacío")
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }
            
            System.out.println("Archivo recibido correctamente, procesando...");
            Prestamo prestamo = prestamoService.cargarTablaAmortizacionDesdeExcel(id, inputStream);
            
            return Response.status(Response.Status.OK)
                    .entity(prestamo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
                    
        } catch (Throwable e) {
            System.err.println("ERROR al cargar tabla desde Excel:");
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al cargar tabla desde Excel: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // NOTA: el endpoint POST /prst/aplicarAbonoCapital/{id}/{valorAbono}/{opcionRecalculo}
    // se eliminó en la Fase 0 de los servicios de pago de préstamos. Lo reemplazan
    // GET /prst/simularAbonoCapital/{idPrestamo} y POST /prst/abonarCapital (montos en el
    // body, nunca en el path). Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §7.3 y §8.

    // ========================================================================
    // SERVICIOS DE PAGO DE PRÉSTAMOS
    // El REST solo valida parámetros y delega; los montos SIEMPRE viajan en el body.
    // Ver ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md §8.
    // ========================================================================

    /**
     * Pago manual de cuota(s) con valor: parcial, exacto o con excedente en cascada.
     *
     * @param solicitud { idPrestamo, valor, usuario, observacion, fechaPago }
     * @return 200 con el ResultadoAplicacionPago; 404/409/422/500 según el fallo
     */
    @POST
    @Path("/pagarCuota")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagarCuota(SolicitudPagoCuota solicitud) {
        System.out.println("LLEGA AL SERVICIO PAGAR CUOTA - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null)
            + " - Valor: " + (solicitud != null ? solicitud.getValor() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdPrestamo() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el préstamo (idPrestamo)", null);
        }
        if (solicitud.getValor() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el valor del pago", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el usuario que ejecuta el pago", null);
        }

        try {
            ResultadoAplicacionPago resultado = procesoPagoPrestamoService.pagarCuota(solicitud);

            String mensaje = "Pago aplicado por $" + resultado.getValorAplicado()
                + " en " + resultado.getCuotasAfectadas().size() + " cuota(s)"
                + (resultado.isPrestamoCancelado() ? ". El préstamo quedó CANCELADO." : "");

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_APLICACION);
            cuerpo.put("mensaje", mensaje);
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo)
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        } catch (Throwable e) {
            System.err.println("ERROR al pagar cuota: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Pago de cuota(s) consumiendo el saldo de aportes del partícipe.
     *
     * @param solicitud { idPrestamo, aportes[{idTipoAporte, valor}], usuario, observacion, fechaPago }
     * @return 200 con el resultado y los movimientos de aporte generados
     */
    @POST
    @Path("/pagarConAportes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pagarConAportes(SolicitudPagoConAportes solicitud) {
        System.out.println("LLEGA AL SERVICIO PAGAR CON APORTES - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdPrestamo() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el préstamo (idPrestamo)", null);
        }
        if (solicitud.getAportes() == null || solicitud.getAportes().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el desglose de aportes", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el usuario que ejecuta el pago", null);
        }

        try {
            ResultadoPagoConAportes respuesta = procesoPagoPrestamoService.pagarConAportes(solicitud);
            ResultadoAplicacionPago resultado = respuesta.getResultado();

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_APLICACION);
            cuerpo.put("mensaje", "Pago con aportes aplicado por $" + resultado.getValorAplicado()
                + " en " + resultado.getCuotasAfectadas().size() + " cuota(s)"
                + (resultado.isPrestamoCancelado() ? ". El préstamo quedó CANCELADO." : ""));
            cuerpo.put("resultado", resultado);
            cuerpo.put("movimientosAporte", respuesta.getMovimientosAporte());

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al pagar con aportes: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Simula un abono a capital: devuelve la tabla proyectada SIN escribir nada.
     *
     * @param idPrestamo Préstamo
     * @param valor      Monto del abono
     * @param modalidad  1 = mantiene la cuota y reduce el plazo; 2 = mantiene el plazo y reduce la cuota
     * @return 200 con la SimulacionAbonoCapital
     */
    @GET
    @Path("/simularAbonoCapital/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response simularAbonoCapital(@PathParam("idPrestamo") Long idPrestamo,
                                        @QueryParam("valor") Double valor,
                                        @QueryParam("modalidad") Integer modalidad) {
        System.out.println("LLEGA AL SERVICIO SIMULAR ABONO CAPITAL - Préstamo: " + idPrestamo
            + " - Valor: " + valor + " - Modalidad: " + modalidad);

        if (idPrestamo == null || idPrestamo <= 0) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar un préstamo válido", null);
        }
        if (valor == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el valor del abono", null);
        }
        if (modalidad == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar la modalidad (1 o 2)", null);
        }

        try {
            SimulacionAbonoCapital simulacion =
                abonoCapitalPrestamoService.simular(idPrestamo, valor, modalidad);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_SIMULACION);
            cuerpo.put("mensaje", "Simulación calculada");
            cuerpo.put("resultado", simulacion);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al simular abono a capital: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Aplica un abono a capital con re-amortización.
     *
     * @param solicitud { idPrestamo, valor, modalidad, usuario, observacion, fecha }
     * @return 201 con el ResultadoAbonoCapital
     */
    @POST
    @Path("/abonarCapital")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response abonarCapital(SolicitudAbonoCapital solicitud) {
        System.out.println("LLEGA AL SERVICIO ABONAR CAPITAL - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdPrestamo() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el préstamo (idPrestamo)", null);
        }
        if (solicitud.getValor() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el valor del abono", null);
        }
        if (solicitud.getModalidad() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar la modalidad (1 o 2)", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el usuario que ejecuta el abono", null);
        }

        try {
            ResultadoAbonoCapital resultado = abonoCapitalPrestamoService.aplicar(solicitud);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_APLICACION);
            cuerpo.put("mensaje", "Abono a capital aplicado por $" + resultado.getValorAbono()
                + ". Plazo: " + resultado.getPlazoAnterior() + " → " + resultado.getPlazoNuevo()
                + ". Cuota: " + resultado.getCuotaAnterior() + " → " + resultado.getCuotaNueva());
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.CREATED)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al abonar a capital: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Simula una precancelación total a una fecha de corte, SIN escribir nada.
     *
     * @param idPrestamo Préstamo
     * @param fecha      Fecha de corte en formato yyyy-MM-dd; si falta se usa hoy
     * @return 200 con la SimulacionPrecancelacion
     */
    @GET
    @Path("/simularPrecancelacion/{idPrestamo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response simularPrecancelacion(@PathParam("idPrestamo") Long idPrestamo,
                                          @QueryParam("fecha") String fecha) {
        System.out.println("LLEGA AL SERVICIO SIMULAR PRECANCELACION - Préstamo: " + idPrestamo
            + " - Fecha: " + fecha);

        if (idPrestamo == null || idPrestamo <= 0) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar un préstamo válido", null);
        }

        java.time.LocalDate fechaCorte;
        try {
            fechaCorte = (fecha != null && !fecha.trim().isEmpty())
                ? java.time.LocalDate.parse(fecha.trim())
                : java.time.LocalDate.now();
        } catch (Exception e) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "La fecha debe tener el formato yyyy-MM-dd", null);
        }

        try {
            SimulacionPrecancelacion simulacion =
                procesoPagoPrestamoService.simularPrecancelacion(idPrestamo, fechaCorte);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_SIMULACION);
            cuerpo.put("mensaje", "Simulación calculada");
            cuerpo.put("resultado", simulacion);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al simular precancelación: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Ejecuta la precancelación total del préstamo (efectivo, aportes o mixto).
     *
     * @param solicitud { idPrestamo, valorEfectivo, aportes[], usuario, observacion, fecha }
     * @return 200 con el ResultadoPrecancelacion; 422 con valorTotalPrecancelacion si el monto no coincide
     */
    @POST
    @Path("/precancelar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response precancelar(SolicitudPrecancelacion solicitud) {
        System.out.println("LLEGA AL SERVICIO PRECANCELAR - Préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdPrestamo() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el préstamo (idPrestamo)", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el usuario que ejecuta la precancelación", null);
        }

        try {
            ResultadoPrecancelacion resultado = procesoPagoPrestamoService.precancelar(solicitud);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_APLICACION);
            cuerpo.put("mensaje", "Préstamo precancelado por $" + resultado.getValorTotalPrecancelacion()
                + ". Cuotas canceladas anticipadamente: " + resultado.getCuotasCanceladasAnticipadas());
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al precancelar: " + e.getMessage());
            e.printStackTrace();

            // Si el monto no coincide, se devuelve el valor correcto para que el frontend reintente
            String mensaje = e.getMessage() != null ? e.getMessage() : "";
            if (mensaje.startsWith("MONTO_NO_COINCIDE")) {
                Map<String, Object> cuerpo = new LinkedHashMap<>();
                cuerpo.put("exito", Boolean.FALSE);
                cuerpo.put("etapa", ETAPA_VALIDACION);
                cuerpo.put("mensaje", mensaje);
                cuerpo.put("error", "MONTO_NO_COINCIDE");
                try {
                    SimulacionPrecancelacion simulacion = procesoPagoPrestamoService
                        .simularPrecancelacion(solicitud.getIdPrestamo(), solicitud.getFecha());
                    cuerpo.put("valorTotalPrecancelacion", simulacion.getValorTotalPrecancelacion());
                } catch (Throwable ignorada) {
                    System.err.println("No se pudo recalcular el valor de precancelación: "
                        + ignorada.getMessage());
                }
                return Response.status(HTTP_REGLA_DE_NEGOCIO)
                        .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
            }

            return respuestaErrorNegocio(e);
        }
    }

    /**
     * Anula (reversa) una operación de pago completa a partir de su evento.
     *
     * @param solicitud { idEvento, usuario, motivo }
     * @return 200 con el ResultadoAnulacion
     */
    @POST
    @Path("/anularOperacion")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularOperacion(SolicitudAnulacion solicitud) {
        System.out.println("LLEGA AL SERVICIO ANULAR OPERACION - Evento: "
            + (solicitud != null ? solicitud.getIdEvento() : null));

        if (solicitud == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe enviar el cuerpo de la solicitud", null);
        }
        if (solicitud.getIdEvento() == null) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el evento (idEvento)", null);
        }
        if (solicitud.getUsuario() == null || solicitud.getUsuario().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el usuario que ejecuta la anulación", null);
        }
        if (solicitud.getMotivo() == null || solicitud.getMotivo().trim().isEmpty()) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION,
                "Debe indicar el motivo de la anulación", null);
        }

        try {
            ResultadoAnulacion resultado = procesoPagoPrestamoService.anularOperacion(solicitud);

            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.TRUE);
            cuerpo.put("etapa", ETAPA_APLICACION);
            cuerpo.put("mensaje", "Operación " + resultado.getTipoOperacion() + " anulada. Pagos anulados: "
                + resultado.getPagosAnulados() + ", cuotas recalculadas: " + resultado.getCuotasRecalculadas());
            cuerpo.put("resultado", resultado);

            return Response.status(Response.Status.OK)
                    .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            System.err.println("ERROR al anular la operación: " + e.getMessage());
            e.printStackTrace();
            return respuestaErrorNegocio(e);
        }
    }

    // ------------------------------------------------------------------------
    // Sobre de respuesta y mapeo de errores de los procesos de pago (§8)
    // ------------------------------------------------------------------------

    private static final String ETAPA_VALIDACION = "VALIDACION";
    private static final String ETAPA_APLICACION = "APLICACION";
    private static final String ETAPA_SIMULACION = "SIMULACION";

    /** 422 UNPROCESSABLE ENTITY - no existe en el enum Response.Status de Jakarta REST */
    private static final int HTTP_REGLA_DE_NEGOCIO = 422;

    /** Códigos de negocio que se responden como 404 NOT FOUND */
    private static final List<String> CODIGOS_404 = Arrays.asList(
        "PRESTAMO_NO_ENCONTRADO", "EVENTO_NO_ENCONTRADO", "CUOTA_NO_ENCONTRADA");

    /** Códigos de negocio que se responden como 409 CONFLICT */
    private static final List<String> CODIGOS_409 = Arrays.asList(
        "ESTADO_NO_PERMITE", "EVENTO_YA_ANULADO", "EVENTO_POSTERIOR_VIGENTE", "PAGOS_SOBRE_TABLA_RECALCULADA");

    /** Códigos de negocio que se responden como 400 BAD REQUEST */
    private static final List<String> CODIGOS_400 = Arrays.asList(
        "PARAMETRO_INVALIDO");

    /**
     * Mapea la excepción de un proceso de pago al status HTTP de §8, usando el CÓDIGO con el
     * que el servicio prefija el mensaje ({@code CODIGO: descripción}). Las reglas de negocio
     * sin código explícito caen en 422; lo inesperado, en 500.
     */
    private Response respuestaErrorNegocio(Throwable e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : "Error inesperado";
        String codigo = mensaje.contains(":") ? mensaje.substring(0, mensaje.indexOf(':')).trim() : "";

        if (CODIGOS_400.contains(codigo)) {
            return respuestaFallo(Response.Status.BAD_REQUEST.getStatusCode(), ETAPA_VALIDACION, mensaje, codigo);
        }
        if (CODIGOS_404.contains(codigo)) {
            return respuestaFallo(Response.Status.NOT_FOUND.getStatusCode(), ETAPA_VALIDACION, mensaje, codigo);
        }
        if (CODIGOS_409.contains(codigo)) {
            return respuestaFallo(Response.Status.CONFLICT.getStatusCode(), ETAPA_VALIDACION, mensaje, codigo);
        }
        if (e instanceof com.saa.basico.util.IncomeException) {
            return respuestaFallo(HTTP_REGLA_DE_NEGOCIO, ETAPA_VALIDACION, mensaje, codigo);
        }
        return respuestaFallo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ETAPA_APLICACION, mensaje, codigo);
    }

    private Response respuestaFallo(int status, String etapa, String mensaje, String codigo) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("etapa", etapa);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null && !codigo.isEmpty() ? codigo : mensaje);
        return Response.status(status)
                .entity(cuerpo)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Clase interna para manejar la carga de archivos multipart.
     */
    public static class FileUploadForm {
        
        private InputStream fileData;
        private String fileName;
        
        @jakarta.ws.rs.FormParam("file")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("application/octet-stream")
        public InputStream getFileData() {
            return fileData;
        }
        
        public void setFileData(InputStream fileData) {
            this.fileData = fileData;
        }
        
        @jakarta.ws.rs.FormParam("fileName")
        @org.jboss.resteasy.annotations.providers.multipart.PartType("text/plain")
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}