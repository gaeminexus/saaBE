package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.GarantePrestamo;

import jakarta.ejb.Local;

/**
 * Garante del préstamo (DISENO-SOLICITUD-V1-AUTORIZACION-Y-REGLAS-OTORGAMIENTO.md, B3). Es OPCIONAL y solo se agrega,
 * cambia o anula mientras el préstamo está pendiente de aprobación (6) o generado (1).
 */
@Local
public interface GarantePrestamoService extends EntityService<GarantePrestamo> {

    /** El garante activo del préstamo, o {@code null}. */
    GarantePrestamo selectActivoByPrestamo(Long idPrestamo) throws Throwable;

    /**
     * Agrega (código nulo) o cambia (con código) el garante. Reglas: el préstamo existe y está en 6 o 1; la cédula no es
     * la del deudor; si viene {@code idEntidad}, nombre y cédula se toman de {@code CRD.ENTD}; un préstamo con garante
     * activo no admite otro alta (se edita o se anula el que hay).
     *
     * @param usuario quien registra; tiene que existir en el sistema
     * @throws com.saa.basico.util.IncomeException con el motivo (la capa REST lo responde 422)
     */
    GarantePrestamo guardarGarante(GarantePrestamo garante, String usuario) throws Throwable;

    /** Anula el garante ({@code GRPRESTD = 0}); no se borra. Con el préstamo en 6, 1 o vivo (2, 8, 10, 11). */
    GarantePrestamo anularGarante(Long idGarante, String usuario) throws Throwable;

    /** Tipo de adjunto (CRD.TPDJ) de la autorización firmada del garante; se busca por nombre exacto y activo. */
    String AUTORIZACION_DEL_GARANTE = "AUTORIZACION DEL GARANTE";

    /**
     * Garante de un préstamo en 6, 1 o vivo (2, 8, 10, 11) CON su autorización firmada (PDF o imagen), en UNA
     * transacción: mismas reglas que {@link #guardarGarante}, y el adjunto en CRD.ADJN (PRSTCDGO = préstamo,
     * ENTDCDGO = el deudor, ADJNIDRF = GRPRCDGO). Si algo falla no queda GRPR, ADJN ni archivo. Es el único alta para un
     * préstamo ya otorgado. Ver DISENO-PAGO-CON-APORTES-DEL-GARANTE.md §G4.
     */
    com.saa.ejb.crd.service.dto.ResultadoGaranteConAutorizacion crearConAutorizacion(
            com.saa.ejb.crd.service.dto.SolicitudGaranteConAutorizacion solicitud) throws Throwable;

    /** El adjunto activo de la autorización del garante, o {@code null} si no tiene (garantes anteriores a G4). */
    com.saa.model.crd.Adjunto obtenerAutorizacion(Long idGarante) throws Throwable;
}
