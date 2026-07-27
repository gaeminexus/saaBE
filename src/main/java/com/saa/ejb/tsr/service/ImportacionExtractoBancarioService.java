/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import java.io.InputStream;

import com.saa.model.tsr.ExtractoBancario;
import com.saa.model.tsr.ResumenImportacionExtracto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio de importacion de estados de cuenta bancarios (archivo Excel
 * de un banco -> EXBC + DEXB). Selecciona el parser automaticamente segun
 * el banco de la cuenta indicada (ver {@link com.saa.ejb.tsr.parser.BankStatementParserFactory}).</p>
 * <p>Flujo en dos fases, igual al patron ya usado en el modulo de creditos
 * (validarArchivoPetro/procesarArchivoPetro): validar() previsualiza sin
 * persistir, confirmar() reparsea el mismo archivo y persiste. No se
 * mantiene estado entre ambos llamados.</p>
 */
@Local
public interface ImportacionExtractoBancarioService {

    /**
     * Parsea el archivo y arma un resumen de previsualizacion, sin guardar
     * nada en base de datos.
     * @param archivo         : Contenido del archivo
     * @param nombreArchivo   : Nombre original del archivo
     * @param idCuentaBancaria : Cuenta bancaria a la que pertenece el extracto
     * @param idPeriodo       : Periodo contable elegido por el usuario para este extracto
     * @return                : Resumen de la importacion
     * @throws Throwable      : Excepcion (bloqueo duro si el periodo esta CERRADO)
     */
    ResumenImportacionExtracto validar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria,
            Long idPeriodo) throws Throwable;

    /**
     * Reparsea el archivo y guarda el lote completo (ExtractoBancario +
     * DetalleExtractoBancario) en una sola transaccion. Rechaza el archivo
     * si su hash ya fue cargado previamente (control de duplicados).
     * @param archivo          : Contenido del archivo
     * @param nombreArchivo    : Nombre original del archivo
     * @param idCuentaBancaria : Cuenta bancaria a la que pertenece el extracto
     * @param idPeriodo        : Periodo contable elegido por el usuario para este extracto
     * @param idEmpresa        : Empresa duena de la cuenta
     * @param usuarioCreacion  : Usuario que realiza la carga (auditoria)
     * @return                 : ExtractoBancario guardado
     * @throws Throwable       : Excepcion (bloqueo duro si el periodo esta CERRADO)
     */
    ExtractoBancario confirmar(InputStream archivo, String nombreArchivo, Long idCuentaBancaria, Long idPeriodo,
            Long idEmpresa, String usuarioCreacion) throws Throwable;

}
