package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ArchivoFirma;
import com.saa.ejb.crd.service.dto.ResultadoOperacionFirma;
import com.saa.ejb.crd.service.dto.TramiteFirma;
import com.saa.model.crd.FirmaContrato;

import jakarta.ejb.Local;

/**
 * Firma electrónica del contrato de adhesión (CRD.FRCN / CRD.FRFR). Diseño:
 * docs/logica-negocio/crd/DISENO-FIRMA-ELECTRONICA-CONTRATO-ADHESION.md. Contrato para el FE:
 * docs/logica-negocio/crd/API-FIRMA-ELECTRONICA-CONTRATO.md.
 *
 * <p>Las operaciones que graban validan el usuario primero (U-2) y corren en una transacción propia. Errores de validación:
 * {@code IncomeException} con un código al inicio del mensaje, antes de tocar nada. Errores del PROVEEDOR: el trámite queda
 * guardado con el error y se devuelve {@link ResultadoOperacionFirma} con {@code exito = false}.</p>
 */
@Local
public interface FirmaContratoService extends EntityService<FirmaContrato> {

    /**
     * Crea el trámite y lo manda a firmar. Valida contrato vigente, sin trámite vivo, partícipe con cédula y contacto; genera
     * el PDF (FORMATO_CONTRATO_PENDIENTE mientras no exista el reporte), lo guarda como adjunto con su SHA-256, copia el
     * firmante a CRD.FRFR y llama al proveedor. Si el proveedor falla el trámite queda en PENDIENTE_ENVIO con el error.
     */
    ResultadoOperacionFirma enviarAFirmar(Long idContrato, String usuario, String codigoDactilar) throws Throwable;

    /** Reenvía al proveedor un trámite en PENDIENTE_ENVIO con el mismo PDF (verifica su SHA-256). */
    ResultadoOperacionFirma reintentarEnvio(Long idFirma, String usuario, String codigoDactilar) throws Throwable;

    /**
     * Consulta el estado al proveedor. Si quedó firmado descarga el PDF firmado, lo guarda como adjunto y cierra el trámite.
     * Idempotente: un trámite ya firmado no se vuelve a consultar.
     */
    ResultadoOperacionFirma actualizarEstado(Long idFirma, String usuario) throws Throwable;

    /** Anula un trámite vivo (cancela en el proveedor si ya se envió). Si el proveedor falla, el trámite sigue vivo. */
    ResultadoOperacionFirma anular(Long idFirma, String motivo, String usuario) throws Throwable;

    /** El PDF original o el firmado de un trámite. {@code tipo}: ORIGINAL o FIRMADO. */
    ArchivoFirma descargar(Long idFirma, String tipo) throws Throwable;

    /** Historial de trámites de un contrato (el más reciente primero) con sus firmantes. */
    List<TramiteFirma> consultarPorContrato(Long idContrato) throws Throwable;
}
