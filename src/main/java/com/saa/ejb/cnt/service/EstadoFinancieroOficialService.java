package com.saa.ejb.cnt.service;

import com.saa.ejb.cnt.estadofinanciero.DatosEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.EncabezadoEstadoFinanciero;
import com.saa.ejb.cnt.estadofinanciero.SolicitudEstadoFinanciero;

import jakarta.ejb.Local;

/**
 * Estados financieros oficiales (C19): encabezado institucional (razon social, RUC, nombre del estado, periodo,
 * moneda, PRELIMINAR/DEFINITIVO, firmas), resultado del ejercicio, comparativo y salida PDF/Excel con logo.
 * Reusa el motor de balances (TempReportesService.reporteRangoFecha) y no cambia sus resultados.
 */
@Local
public interface EstadoFinancieroOficialService {

	EncabezadoEstadoFinanciero encabezado(SolicitudEstadoFinanciero solicitud) throws Throwable;

	DatosEstadoFinanciero datos(SolicitudEstadoFinanciero solicitud) throws Throwable;

	/** Archivo PDF o XLSX del estado oficial. */
	byte[] imprimir(SolicitudEstadoFinanciero solicitud) throws Throwable;
}
