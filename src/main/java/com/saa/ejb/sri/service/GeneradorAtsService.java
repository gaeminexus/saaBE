package com.saa.ejb.sri.service;

import com.saa.ejb.sri.service.dto.ResultadoGeneracionAts;

import jakarta.ejb.Local;

/**
 * Generador del Anexo Transaccional Simplificado (ATS) del SRI — Fase 4 de
 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md.
 *
 * <p><b>Cobertura verificada, no exhaustiva.</b> Genera {@code &lt;compras&gt;} (FCTC, LQCC,
 * NTCC, NTDC), {@code &lt;ventas&gt;} (agrupada por cliente y tipo, FCTR/NTCR/NTDB) y
 * {@code &lt;anulados&gt;}, con los campos confirmados contra datos reales del §3.3-§3.5. Los
 * campos que dependen de catálogos del SRI todavía sin verificar (Tablas 13, 20, 21 — ver §3.6)
 * u otros sin fuente de datos hoy (retenciones de IVA/renta por documento, exterior, banano,
 * dividendos, reembolsos detallados) se dejan vacíos y se reportan en
 * {@link ResultadoGeneracionAts#getAvisos()} — nunca se inventa un código. No modela
 * {@code &lt;exportaciones&gt;}, {@code &lt;RECAPS&gt;}, fideicomisos ni rendimientos
 * financieros: el propio levantamiento confirma que no aplican a esta empresa.</p>
 */
@Local
public interface GeneradorAtsService {

    /**
     * Genera el ZIP del ATS de un período para un facturador (identidad fiscal — RUC/razón
     * social viven en {@code Facturador}, no en {@code Empresa}; ver §10).
     *
     * @param idFacturador	: Id del facturador (CBR.FCDR.ID)
     * @param anio			: Año del período (el de la fecha de REGISTRO CONTABLE, no de emisión)
     * @param mes			: Mes del período, 1-12
     * @return				: ZIP generado, con avisos de lo que no se pudo resolver
     * @throws Throwable	: IncomeException si el facturador no existe o el período es inválido
     */
    ResultadoGeneracionAts generarAts(Long idFacturador, int anio, int mes) throws Throwable;

}
