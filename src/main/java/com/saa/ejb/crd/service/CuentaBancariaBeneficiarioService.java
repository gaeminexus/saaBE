package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.ResultadoCuentaBancariaBeneficiarioConCertificado;
import com.saa.ejb.crd.service.dto.SolicitudCuentaBancariaBeneficiarioConCertificado;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaBeneficiario;

import jakarta.ejb.Local;

@Local
public interface CuentaBancariaBeneficiarioService extends EntityService<CuentaBancariaBeneficiario> {

    /**
     * Nombre exacto del {@code TipoAdjunto} (CRD.TPDJ) que identifica el certificado bancario de
     * un BENEFICIARIO. Tipo propio, NO el {@code CERTIFICADO_BANCARIO} de
     * {@code CuentaBancariaParticipeService}: los adjuntos se resuelven sólo por
     * {@code (ADJNIDRF, TPDJCDGO)} y {@code CRD.ADJN} no guarda de qué tabla viene la referencia,
     * así que compartir el tipo con CNBP hacía que un beneficiario y una cuenta bancaria de
     * partícipe con el mismo código (p. ej. los dos "1") fueran indistinguibles para el catálogo
     * de adjuntos. Ver {@code docs/logica-negocio/crd/sql/240_TIPO_ADJUNTO_CERTIFICADO_BENEFICIARIO.sql}.
     */
    String CERTIFICADO_BANCARIO_BENEFICIARIO = "CERTIFICADO BANCARIO BENEFICIARIO";

    /** 400 - Falta un campo obligatorio (incluido el archivo), o un numérico no parsea */
    String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    /** 400 - El partícipe no existe */
    String ERR_ENTIDAD_NO_ENCONTRADA = "ENTIDAD_NO_ENCONTRADA";
    /** 400 - El banco externo no existe */
    String ERR_BANCO_NO_ENCONTRADO = "BANCO_NO_ENCONTRADO";
    /** 400 - El archivo no es un PDF */
    String ERR_EXTENSION_NO_PERMITIDA = "EXTENSION_NO_PERMITIDA";
    /** 400 - El archivo supera el tope de FileService.TAMAÑO_MAXIMO */
    String ERR_ARCHIVO_MUY_GRANDE = "ARCHIVO_MUY_GRANDE";
    /** 400 - El archivo llegó vacío */
    String ERR_ARCHIVO_VACIO = "ARCHIVO_VACIO";
    /** 400 - El porcentaje está fuera de (0, 100] */
    String ERR_PORCENTAJE_INVALIDO = "PORCENTAJE_INVALIDO";
    /** 404 - El beneficiario (por código) no existe, en la actualización */
    String ERR_BENEFICIARIO_NO_ENCONTRADO = "BENEFICIARIO_NO_ENCONTRADO";
    /**
     * 409 - Ya existe un beneficiario con esa identificación PARA ESE PARTÍCIPE
     * (índice CRD.UX_CBBP_PARTICIPE_IDENT).
     */
    String ERR_BENEFICIARIO_DUPLICADO = "BENEFICIARIO_DUPLICADO";
    /**
     * 500 (de configuración, no del usuario) - Falta cargar CRD.TPDJ con
     * {@link #CERTIFICADO_BANCARIO_BENEFICIARIO}.
     */
    String ERR_TIPO_ADJUNTO_NO_CONFIGURADO = "TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO";

    /**
     * Los beneficiarios de un partícipe, activos e inactivos, ordenados por porcentaje
     * descendente y luego por código (§3.1 del contrato).
     *
     * @param idEntidad código del partícipe
     * @throws Throwable Si ocurre un error
     */
    List<CuentaBancariaBeneficiario> selectPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Registra un beneficiario JUNTO con su certificado bancario, en una sola transacción: si
     * algo falla, no queda ni beneficiario huérfano en CRD.CBBP ni archivo colgado en disco. Es
     * el ÚNICO camino soportado para crear un beneficiario.
     *
     * Antes de insertar, chequea que no exista ya un beneficiario con la misma identificación
     * para el mismo partícipe (índice CRD.UX_CBBP_PARTICIPE_IDENT) e informa
     * {@link #ERR_BENEFICIARIO_DUPLICADO} en vez de dejar salir el ORA-00001 como 500.
     *
     * NO valida que los porcentajes de los beneficiarios de un mismo partícipe sumen 100: esa
     * guarda vive en el pago (fase 2b). Sólo valida que el porcentaje de esta fila esté en
     * (0, 100].
     *
     * @param solicitud Datos del beneficiario + el PDF del certificado
     * @return El beneficiario y el adjunto creados
     * @throws Throwable Si algún dato es inválido, el archivo no es un PDF válido, ya existe el
     *                    beneficiario, o falla el guardado (en cuyo caso no queda rastro de
     *                    ninguna de las dos partes)
     */
    ResultadoCuentaBancariaBeneficiarioConCertificado crearConCertificado(
            SolicitudCuentaBancariaBeneficiarioConCertificado solicitud) throws Throwable;

    /**
     * Actualiza porcentaje, estado, tipoCuenta, numeroCuenta, bancoExterno, nombre y
     * numeroIdentificacion de un beneficiario existente. NO cambia entidad: mover un beneficiario
     * de un partícipe a otro es otra operación (§3.4 del contrato, revisado 2026-09-22).
     *
     * Si la identificación cambia, corre el mismo chequeo de duplicado que
     * {@link #crearConCertificado}: si la nueva identificación ya existe para el mismo partícipe
     * en OTRO beneficiario, {@link #ERR_BENEFICIARIO_DUPLICADO} en vez del ORA-00001 crudo contra
     * {@code UX_CBBP_PARTICIPE_IDENT}.
     *
     * @param cambio Beneficiario con el código del registro a actualizar y los campos permitidos
     * @return El beneficiario actualizado
     * @throws Throwable Si el código no existe, el porcentaje está fuera de rango, o la nueva
     *                    identificación ya la tiene otro beneficiario del mismo partícipe
     */
    CuentaBancariaBeneficiario actualizar(CuentaBancariaBeneficiario cambio) throws Throwable;

    /**
     * El certificado bancario ACTIVO de un beneficiario, o null si no tiene. Mismo criterio que
     * {@code CuentaBancariaParticipeService.obtenerCertificado}.
     *
     * @param idBeneficiario Código de CuentaBancariaBeneficiario (CBBPCDGO)
     * @throws Throwable Si ocurre un error
     */
    Adjunto obtenerCertificado(Long idBeneficiario) throws Throwable;

    /**
     * Elimina REALMENTE un beneficiario: su fila de CRD.CBBP, su adjunto de CRD.ADJN y el archivo
     * del disco, en una transacción (decisión del usuario, 2026-09-22 — el contrato original sólo
     * preveía inactivar). El archivo se borra al final; si falla, se loguea y NO se revierte la
     * eliminación de las filas (inverso del patrón de {@link #crearConCertificado}: ahí un fallo
     * de BD borra el archivo recién subido, acá un fallo al borrar el archivo no revive las filas).
     *
     * @param idBeneficiario Código del beneficiario a eliminar
     * @throws Throwable Si el código no existe
     */
    void eliminar(Long idBeneficiario) throws Throwable;
}
