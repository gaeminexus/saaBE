package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.PrecargaCertificado;
import com.saa.ejb.crd.service.dto.ResultadoEmisionCertificado;
import com.saa.ejb.crd.service.dto.SolicitudEmisionCertificado;
import com.saa.model.crd.Certificado;

import jakarta.ejb.Local;

/**
 * Certificados de partícipe (CRD.CRTF). Contrato: docs/logica-negocio/crd/API-CERTIFICADOS-PARTICIPE.md
 * (⛔ congelado el 2026-08-29).
 *
 * <h3>Lo que no se negocia</h3>
 * <ul>
 *   <li><b>Una sola serie por año, compartida por los 6 tipos.</b> El número se asigna al
 *       emitir como MAX+1 del año bajo lock, en la MISMA transacción que llena el reporte
 *       y graba el PDF: si algo falla, la transacción revierte y el número nunca existió.
 *       No hay secuencia de Oracle a propósito (no reinicia por año, no participa del
 *       rollback).</li>
 *   <li><b>El .jrxml no consulta CRD.CRTF.</b> El reporte se llena con una conexión JDBC
 *       cruda que no ve esta transacción; el número y todo lo variable van como
 *       parámetros ({@code P_*}).</li>
 *   <li><b>Cada valor impreso lleva su origen</b> (SISTEMA / MANUAL_REQUERIDO /
 *       MANUAL_EDITADO), calculado aquí comparando lo resuelto contra lo que mandó el
 *       operador. El frontend nunca manda el origen.</li>
 *   <li><b>Los bloqueos se re-evalúan al emitir.</b> Un POST directo no se salta la regla.</li>
 *   <li><b>El PDF se guarda tal cual.</b> Reimprimir es devolver ese binario.</li>
 * </ul>
 *
 * Los métodos de escritura heredados de {@link EntityService} están bloqueados: un
 * certificado no se crea ni se edita a mano, solo se emite y se anula.
 */
@Local
public interface CertificadoService extends EntityService<Certificado> {

    /** Prefijo fijo del número impreso: ASOPREP-FCPC-PARTICIPE-NNN-AAAA */
    String PREFIJO_NUMERO = "ASOPREP-FCPC-PARTICIPE";

    /** Fuente citada en los 6 certificados (decisión del usuario 2026-08-29: nunca DELTA21). */
    String FUENTE_DATOS = "sistema S.A.A.";

    /** Módulo de reportes donde viven los .jrxml/.jasper (rep/crd). */
    String MODULO_REPORTE = "crd";

    // ---- Claves de campos (§4 del contrato) ----
    String CAMPO_FIRMANTE = "firmante";
    String CAMPO_CARGO = "cargo";
    String CAMPO_CIUDAD = "ciudad";
    String CAMPO_FUENTE_DATOS = "fuenteDatos";
    String CAMPO_ANIO_DESDE = "anioDesde";
    String CAMPO_FECHA_LIQUIDACION = "fechaLiquidacion";
    String CAMPO_NUMERO_CREDITO = "numeroCredito";
    String CAMPO_PRODUCTO_TEXTO = "productoTexto";
    String CAMPO_MONTO = "monto";
    String CAMPO_FECHA_PAGO = "fechaPago";
    String CAMPO_CONCEPTO_DEVOLUCION = "conceptoDevolucion";
    String CAMPO_TIPO_CUENTA = "tipoCuenta";
    String CAMPO_NUMERO_CUENTA = "numeroCuenta";
    String CAMPO_BANCO = "banco";
    String CAMPO_RECIBIO_CESANTIA_PATRONAL = "recibioCesantiaPatronal";
    String CAMPO_JUBILACION_PATRONAL_SIN_MOVIMIENTOS = "jubilacionPatronalSinMovimientos";
    String CAMPO_RECIBE_PENSION_MENSUAL = "recibePensionMensual";
    String CAMPO_FECHA_CORTE_PENSION = "fechaCortePension";

    // ---- Códigos de error de negocio (prefijo del mensaje de IncomeException) ----
    /** 400 - Falta un parámetro obligatorio o viene malformado */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 404 - El partícipe no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 404 - El certificado no existe */
    String ERR_CERTIFICADO_NO_ENCONTRADO = "CERTIFICADO_NO_ENCONTRADO";
    /** 409 - El certificado ya estaba anulado */
    String ERR_CERTIFICADO_YA_ANULADO = "CERTIFICADO_YA_ANULADO";
    /** 422 - Falta capturar un campo MANUAL_REQUERIDO */
    String ERR_CAMPO_REQUERIDO = "CAMPO_REQUERIDO";
    /** 422 - La calidad no está en 1..9 */
    String ERR_CALIDAD_INVALIDA = "CALIDAD_INVALIDA";
    /** 422 - Hay un bloqueo vigente (el mensaje trae el motivo concreto) */
    String ERR_BLOQUEADO = "BLOQUEADO";

    /**
     * Precarga: resuelve todo lo que el sistema puede, marca el origen de cada campo y lista
     * los bloqueos. No escribe nada.
     *
     * @param idEntidad     Código de la entidad (partícipe)
     * @param tipo          Tipo de certificado ({@link com.saa.rubros.CrdTipoCertificado})
     * @param idPrestamo    Solo tipo 3: préstamo elegido; null → se devuelve la lista para elegir
     * @param idLiquidacion Solo tipos 2 y 5: fila de HPCS elegida; null → la más reciente
     * @return Precarga con campos, bloqueos y listas auxiliares
     * @throws Throwable Si ocurre un error
     */
    PrecargaCertificado precargar(Long idEntidad, Long tipo, Long idPrestamo, Long idLiquidacion)
            throws Throwable;

    /**
     * Emisión. En UNA transacción: re-valida los bloqueos, recalcula los orígenes, toma el
     * número (lock + MAX+1), llena el reporte, graba el certificado con el PDF y responde.
     *
     * @param solicitud Partícipe, tipo, calidad, valores capturados y usuario
     * @return Certificado emitido con el origen final de cada campo y la URL del PDF
     * @throws Throwable Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Ante un bloqueo o un campo faltante (nada queda grabado)
     */
    ResultadoEmisionCertificado emitir(SolicitudEmisionCertificado solicitud) throws Throwable;

    /**
     * Certificados emitidos a un partícipe, del más reciente al más antiguo. Una lista
     * vacía NO es error.
     * @param idEntidad Código de la entidad (partícipe)
     * @return Listado de certificados (sin el PDF)
     * @throws Throwable Si ocurre un error
     */
    List<Certificado> listarPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Reimpresión: devuelve el PDF guardado al emitir, bit a bit. No se regenera nada.
     * Un certificado anulado también se puede reimprimir (queda como constancia).
     * @param idCertificado Código del certificado
     * @return Bytes del PDF
     * @throws Throwable Si ocurre un error
     * @throws com.saa.basico.util.IncomeException Si el certificado no existe
     */
    byte[] obtenerPdf(Long idCertificado) throws Throwable;

    /**
     * Anula un certificado emitido. El número NO se libera ni se reutiliza: el hueco en
     * la serie queda documentado con su motivo.
     * @param idCertificado Código del certificado
     * @param motivo        Motivo de la anulación, obligatorio
     * @param usuario       Usuario que anula
     * @return Certificado anulado
     * @throws Throwable Si ocurre un error
     */
    Certificado anular(Long idCertificado, String motivo, String usuario) throws Throwable;

    /**
     * Arma el número impreso a partir del año y el secuencial:
     * {@code ASOPREP-FCPC-PARTICIPE-099-2026} (secuencial con al menos tres dígitos).
     * @param anio   Año de la serie
     * @param numero Secuencial dentro del año
     * @return Número alterno
     */
    String formatearNumero(Long anio, Long numero);
}
