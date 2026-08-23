package com.saa.ejb.cxp.service;

import java.time.LocalDate;

import com.saa.ejb.cxp.service.dto.ResultadoAutorizacionSri;

import jakarta.ejb.Local;

/**
 * Cliente del servicio de autorización del SRI (WS2,
 * {@code AutorizacionComprobantesOffline}), compartido.
 *
 * <p>
 * <b>No es un cliente nuevo.</b> Es el {@code llamarAutorizacionSRI} que ya
 * corre en producción en {@code FacturaServiceImpl:1705} —duplicado en
 * {@code LiquidacionCompraServiceImpl}, {@code NotaCreditoServiceImpl},
 * {@code NotaDebitoServiceImpl}, {@code RetencionServiceImpl} y
 * {@code RetencionV2ServiceImpl}— extraído a un bean, sobre el mismo
 * {@code com.saa.ejb.cxc.util.SriHttpUtil} que resuelve el TLS del SRI.
 * Los seis {@code serviceImpl} de CXC <b>no se tocaron</b> en esta tanda; migrarlos
 * a este servicio es un refactor aparte.
 * </p>
 *
 * <p>
 * Diferencia con el original, y es la razón de existir de este bean: aquí lo que
 * interesa no es el {@code <comprobante>} interno sino el sobre
 * {@code <autorizacion>} <b>completo</b>. Ver §8 regla 3 del plan.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Local
public interface SriAutorizacionService {

    /** WS2 de certificación/pruebas — ambiente 1. */
    String URL_AUTORIZACION_PRUEBAS =
            "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

    /** WS2 de producción — ambiente 2. */
    String URL_AUTORIZACION_PRODUCCION =
            "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";

    /** Largo de una clave de acceso del SRI. */
    int LARGO_CLAVE_ACCESO = 49;

    /**
     * Consulta el comprobante por su clave de acceso.
     *
     * <p>
     * El ambiente al que se llama sale del <b>dígito 24 de la clave</b>
     * (1=pruebas, 2=producción), no de una constante ni de la configuración del
     * facturador: la clave la emitió el proveedor, y es ella la que dice en qué
     * ambiente vive el comprobante.
     * </p>
     *
     * @param claveAcceso : Clave de acceso de 49 dígitos
     * @return            : Resultado con estado, autorización y el sobre XML completo
     * @throws Throwable  : Si no se pudo hablar con el SRI o la clave es inválida
     */
    ResultadoAutorizacionSri consultarAutorizacion(String claveAcceso) throws Throwable;

    /**
     * Revisa la clave <b>sin lanzar</b>: largo, que sean todos dígitos y que el
     * dígito 24 sea un ambiente válido.
     *
     * <p>
     * Existe para que el llamador pueda descartar una clave mala y dejar
     * constancia en el documento, en vez de comerse una {@code IncomeException}.
     * Eso importa aquí: {@code IncomeException} está anotada
     * {@code @ApplicationException(rollback = true)}, y atraparla para después
     * seguir escribiendo es pedir problemas.
     * </p>
     *
     * @param claveAcceso : Clave de acceso a revisar
     * @return            : Motivo por el que no sirve, o null si está bien
     */
    String motivoClaveInvalida(String claveAcceso);

    /**
     * Deduce el ambiente del dígito 24 de la clave de acceso.
     *
     * @param claveAcceso : Clave de acceso de 49 dígitos
     * @return            : 1=pruebas 2=producción
     * @throws Throwable  : IncomeException si la clave no tiene el largo esperado
     */
    Long ambienteDesdeClaveAcceso(String claveAcceso) throws Throwable;

    /**
     * Primer día que el SRI todavía sirve, contado desde hoy.
     *
     * <p>
     * Medido contra producción el 2026-08-22: la ventana es <b>exactamente un mes
     * hacia atrás, por día del mes</b>. Con fecha de emisión del 22/07/2026 el
     * servicio responde; con la del 21/07/2026 contesta que la fecha está fuera
     * del rango permitido. Ver §2 del plan.
     * </p>
     *
     * @return : Fecha de emisión más antigua que vale la pena consultar
     */
    LocalDate limiteVentanaConsulta();

    /**
     * ¿Vale la pena gastar la llamada de red por este documento?
     *
     * <p>
     * Una fecha de emisión <b>nula</b> devuelve {@code true}: no hay base para
     * descartar el documento, así que se consulta y que conteste el SRI.
     * </p>
     *
     * @param fechaEmision : Fecha de emisión del documento
     * @return             : true si la fecha está dentro de la ventana
     */
    boolean dentroDeVentana(LocalDate fechaEmision);
}
