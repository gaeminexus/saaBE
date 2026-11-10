package com.saa.ejb.crd.service;

import java.util.Map;

import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionesFirma;
import com.saa.ejb.crd.firma.SecurityDataPruebas.Archivo;

import jakarta.ejb.Local;

/**
 * Apoyo local de la pantalla de pruebas de Security Data (crd/DISENO-PANTALLA-PRUEBAS-SECURITY-DATA.md): lo que necesita
 * del SAA y no es una llamada al proveedor. Solo LEE: no graba nada.
 */
@Local
public interface SecurityDataPruebasService {

    /** Paso 2, «llenar desde socio»: busca la entidad por cédula y devuelve nombre, correo y celular ({@code encontrado=false} si no existe). */
    Map<String, Object> socioPorCedula(String cedula) throws Throwable;

    /** Paso 5, «usar el contrato N»: genera el PDF del contrato de adhesión (RPRT_CNTR_ADHS) sin guardarlo. */
    Archivo generarPdfContrato(Long idContrato) throws Throwable;

    /** Paso 5, «calcular para el contrato»: dónde firman el partícipe y el representante en el PDF de ese contrato. */
    PosicionesFirma calcularPosiciones(Long idContrato) throws Throwable;
}
