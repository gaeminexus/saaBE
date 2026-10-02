package com.saa.ejb.crd.service;

import java.time.LocalDate;
import java.util.List;

import com.saa.ejb.crd.service.dto.CandidatoSeguro;
import com.saa.ejb.crd.service.dto.DocumentoSeguroDTO;
import com.saa.ejb.crd.service.dto.NovedadesSeguro;
import com.saa.ejb.crd.service.dto.PreviewDistribucionSeguro;
import com.saa.ejb.crd.service.dto.PrestamoSeguroDTO;
import com.saa.ejb.crd.service.dto.ResultadoAnularSeguro;
import com.saa.ejb.crd.service.dto.SolicitudDocumentoSeguro;
import com.saa.ejb.crd.service.dto.SolicitudGenerarListado;
import com.saa.ejb.crd.service.dto.SolicitudNotaSeguro;

import jakarta.ejb.Local;

/**
 * Pólizas de seguro de préstamos: factura anual (desgravamen / incendio / prendario), sus notas
 * de débito/crédito, el prorrateo a cuotas y la liberación a pago por CXP. Contrato completo:
 * {@code docs/logica-negocio/crd/API-POLIZAS-SEGURO.md}. Diseño:
 * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} (S1-S12, §5).
 */
@Local
public interface DocumentoSeguroService {

    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    String ERR_DOCUMENTO_NO_ENCONTRADO = "DOCUMENTO_NO_ENCONTRADO";
    String ERR_PRESTAMO_NO_ENCONTRADO = "PRESTAMO_NO_ENCONTRADO";
    String ERR_HAY_PRESTAMOS_SIN_SUMA_ASEGURADA = "HAY_PRESTAMOS_SIN_SUMA_ASEGURADA";
    String ERR_ESTADO_INVALIDO = "ESTADO_INVALIDO";
    String ERR_VALOR_INVALIDO = "VALOR_INVALIDO";
    String ERR_FECHA_INVALIDA = "FECHA_INVALIDA";
    String ERR_CLAVE_ACCESO_OBLIGATORIA = "CLAVE_ACCESO_OBLIGATORIA";
    String ERR_CLAVE_ACCESO_DUPLICADA = "CLAVE_ACCESO_DUPLICADA";
    String ERR_SIN_CUOTAS_EN_VIGENCIA = "SIN_CUOTAS_EN_VIGENCIA";
    String ERR_DISTRIBUCION_NO_CUADRA = "DISTRIBUCION_NO_CUADRA";
    String ERR_CUOTA_YA_CUBIERTA = "CUOTA_YA_CUBIERTA";
    String ERR_LIBERADO_A_PAGO = "LIBERADO_A_PAGO";
    String ERR_NOTAS_VIVAS = "NOTAS_VIVAS";
    String ERR_FACTURA_INVALIDA = "FACTURA_INVALIDA";
    String ERR_MOTIVO_OBLIGATORIO = "MOTIVO_OBLIGATORIO";
    String ERR_INTEGRACION_CXP_PENDIENTE = "INTEGRACION_CXP_PENDIENTE";

    /** Paso 1 (vista previa) — {@code GET /posg/listado/preview} (contrato §3). */
    List<CandidatoSeguro> previewListado(Long tipoSeguro, LocalDate fechaCorte) throws Throwable;

    /** Paso 1 — {@code POST /posg/listado} (contrato §3). */
    DocumentoSeguroDTO generarListado(SolicitudGenerarListado solicitud) throws Throwable;

    /** Paso 2 — {@code PUT /posg/{id}/documento} (contrato §4). */
    DocumentoSeguroDTO registrarDocumento(Long id, SolicitudDocumentoSeguro solicitud) throws Throwable;

    /** Paso 3 (vista previa) — {@code GET /posg/{id}/distribucion/preview} (contrato §5). */
    PreviewDistribucionSeguro previewDistribucion(Long id) throws Throwable;

    /** Paso 3 — {@code POST /posg/{id}/distribuir} (contrato §5). */
    DocumentoSeguroDTO distribuir(Long id, String usuario) throws Throwable;

    /** {@code POST /posg/{id}/anular} (contrato §6). */
    ResultadoAnularSeguro anular(Long id, String usuario, String motivo) throws Throwable;

    /** {@code GET /posg/{idFactura}/novedades} (contrato §7). */
    NovedadesSeguro novedades(Long idFactura, LocalDate desde, LocalDate hasta) throws Throwable;

    /** {@code POST /posg/{idFactura}/nota} (contrato §7). */
    DocumentoSeguroDTO registrarNota(Long idFactura, SolicitudNotaSeguro solicitud) throws Throwable;

    /** {@code GET /posg/listar} (contrato §8). */
    List<DocumentoSeguroDTO> listar(Long tipoSeguro, Long estado, Long clase) throws Throwable;

    /** {@code GET /posg/{id}} (contrato §8). */
    DocumentoSeguroDTO getById(Long id) throws Throwable;

    /** {@code GET /posg/{id}/prestamos} (contrato §8). */
    List<PrestamoSeguroDTO> getPrestamos(Long id) throws Throwable;
}
