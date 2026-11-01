package com.saa.ejb.cnt.service;

import com.saa.model.cnt.ResultadoCargaAsientosManuales;
import com.saa.model.cnt.SolicitudCargaAsientosManuales;

import jakarta.ejb.Local;

/**
 * Cargador de asientos manuales (cambio C5 del proyecto cuadre Superintendencia): registra, con
 * {@code AsientoContableService.generarAsientoManual}, un asiento por cada {@code numero_asiento} de un archivo
 * con el formato de la plantilla {@code PLANTILLA-ASIENTOS-MANUALES.csv}
 * ({@code fecha;numero_asiento;glosa;cuenta;nombre_cuenta;debe;haber;documento_respaldo;centro_costo}; se acepta
 * ademas la variante con {@code cuenta_saa} y {@code cuenta_b17}).
 *
 * <p>Todo o nada: el archivo entero se valida primero y, si una sola validacion falla, no se registra nada y se
 * devuelve la lista completa de errores con numero de linea. Si todas pasan, los asientos se graban en una sola
 * transaccion; cualquier error de grabacion revierte el lote completo.</p>
 *
 * <p>docs/logica-negocio/cnt/CARGADOR-ASIENTOS-MANUALES.md.</p>
 */
@Local
public interface CargaAsientosManualesService {

    /**
     * Valida y registra el archivo.
     *
     * @param solicitud : empresa, usuario, nombre del archivo, tipo de asiento (alterno y/o nombre) y contenido
     * @return : resultado con {@code exito = true} y los asientos creados, o {@code exito = false} y los errores
     *           (no se grabo nada)
     * @throws Throwable : error de grabacion despues de validar (la transaccion ya se revirtio)
     */
    ResultadoCargaAsientosManuales cargar(SolicitudCargaAsientosManuales solicitud) throws Throwable;

}
