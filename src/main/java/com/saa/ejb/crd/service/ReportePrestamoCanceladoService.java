package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.FiltroPrestamosCancelados;
import com.saa.ejb.crd.service.dto.ReportePrestamosCancelados;

import jakarta.ejb.Local;

/**
 * Reporte de préstamos cancelados en un rango de fechas (pantalla y PDF; el CSV lo arma el FE con las filas).
 * Solo lectura. Contrato: docs/logica-negocio/crd/API-REPORTE-PRESTAMOS-CANCELADOS.md.
 */
@Local
public interface ReportePrestamoCanceladoService {

    // ---- Formas de cancelación (§4) ----
    String FORMA_PAGO_ULTIMA_CUOTA = "PAGO_ULTIMA_CUOTA";
    String FORMA_ABONO_CAPITAL = "ABONO_CAPITAL";
    String FORMA_PRECANCELACION = "PRECANCELACION";
    String FORMA_PRECANCELACION_CON_PRESTAMO = "PRECANCELACION_CON_PRESTAMO";
    String FORMA_ACUERDO_CONDONACION = "ACUERDO_CONDONACION";
    String FORMA_NOVACION = "NOVACION";
    String FORMA_REESTRUCTURACION = "REESTRUCTURACION";
    String FORMA_SIN_DETERMINAR = "SIN_DETERMINAR";

    /** Las formas en el orden de la tabla de §4 (orden del resumen y de los grupos del PDF). */
    List<String> FORMAS_EN_ORDEN = List.of(FORMA_PAGO_ULTIMA_CUOTA, FORMA_ABONO_CAPITAL, FORMA_PRECANCELACION,
        FORMA_PRECANCELACION_CON_PRESTAMO, FORMA_ACUERDO_CONDONACION, FORMA_NOVACION, FORMA_REESTRUCTURACION,
        FORMA_SIN_DETERMINAR);

    // ---- Canales: origen del dinero de la operación final (§4.2) ----
    String CANAL_PRESTAMO_NUEVO = "PRESTAMO_NUEVO";
    String CANAL_CONDONACION = "CONDONACION";
    String CANAL_DESCUENTO_ROL_PETRO = "DESCUENTO_ROL_PETRO";
    String CANAL_ARCH = "ARCH";
    String CANAL_MIGRACION = "MIGRACION";
    String CANAL_APORTES_GARANTE = "APORTES_GARANTE";
    String CANAL_APORTES = "APORTES";
    String CANAL_MIXTO = "MIXTO";
    String CANAL_CAJA = "CAJA";
    String CANAL_SIN_DATO = "SIN_DATO";

    // ---- Origen de la fecha de cancelación (§3) ----
    String ORIGEN_SUSTITUCION = "SUSTITUCION";
    String ORIGEN_EVENTO = "EVENTO";
    String ORIGEN_PAGO_CARGA = "PAGO_CARGA";
    String ORIGEN_MIGRACION = "MIGRACION";
    String ORIGEN_CUOTA = "CUOTA";

    /**
     * Arma el reporte: valida el filtro (§2.2), deriva la fecha de cancelación (§3), clasifica forma y canal
     * (§4), y calcula las 56 columnas, el resumen por forma y los totales.
     *
     * @param filtro rango de fechas obligatorio y filtros opcionales (§2.1)
     * @return el reporte, con {@code filas} ordenadas por fecha de cancelación y número de operación
     * @throws com.saa.basico.util.IncomeException si el filtro es inválido (el mensaje nombra el campo)
     * @throws Throwable cualquier otra falla
     */
    ReportePrestamosCancelados consultar(FiltroPrestamosCancelados filtro) throws Throwable;

    /**
     * El PDF del mismo reporte (§7): llena {@code rep/crd/RPRT_PRCN_LSTD.jasper} con las filas de
     * {@link #consultar}, ordenadas por forma y fecha. Con cero filas igual sale el PDF.
     *
     * @param filtro igual que {@link #consultar}
     * @return los bytes del PDF
     * @throws com.saa.basico.util.IncomeException si el filtro es inválido
     * @throws Throwable cualquier otra falla
     */
    byte[] pdf(FiltroPrestamosCancelados filtro) throws Throwable;
}
