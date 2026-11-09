package com.saa.ejb.crd.service;

import java.time.LocalDate;

import com.saa.model.crd.Prestamo;

import jakarta.ejb.Local;

/**
 * Primera cuota según el archivo Petro (DISENO-PRIMERA-CUOTA-SEGUN-ARCHIVO-PETRO.md): si el archivo Petro del mes de
 * inicio y de la filial del partícipe aún no se generó, la cuota 1 vence a fin del mes de inicio; si ya se generó, al
 * mes siguiente. Solo LEE (GNAP y CXPG); no escribe nada.
 *
 * <p>«Generado» = existe una cabecera GNAP de ese mes, año y filial en estado 1 GENERADO, 2 ENVIADO o 3 PROCESADO.
 * Estado 0 (anulado) o sin cabecera = no generado.</p>
 */
@Local
public interface PrimeraCuotaPetroService {

    /** ¿Existe la generación del archivo Petro de ese mes y filial en estado 1, 2 o 3? */
    boolean archivoPetroGenerado(LocalDate fechaInicio, Long codigoFilial) throws Throwable;

    /**
     * Valor del flag {@code primeraCuotaEnMesInicio} de la calculadora para el partícipe {@code idEntidad}:
     * {@code TRUE} si el archivo del mes de inicio de su filial no se ha generado; {@code FALSE} (regla de siempre) si
     * ya se generó, si el partícipe o su filial no se conocen o si el inicio es el último día del mes.
     */
    Boolean primeraCuotaEnMesInicio(LocalDate fechaInicio, Long idEntidad) throws Throwable;

    /**
     * Al aprobar: si la PRIMERA cuota de la tabla (la 0 de gracia si existe, si no la 1) vence en el mes de inicio, el archivo de ese mes y filial ya se generó y la
     * cuota no está en CXPG de esa generación, lanza {@code IncomeException} (422) pidiendo regenerar la tabla.
     */
    void validarAlAprobar(Prestamo prestamo) throws Throwable;
}
