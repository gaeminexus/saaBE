/**
 * Copyright (c) 2010 Compuseg Cía. Ltda.
 * Av. Amazonas 3517 y Juan Pablo Sanz, Edif Xerox 6to. piso
 * Quito - Ecuador
 * Todos los derechos reservados.
 */
package com.saa.ejb.tsr.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.ConciliacionContable;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio de la cabecera de conciliación contable (extracto bancario
 * contra contabilidad), una fila por cuenta bancaria + período.</p>
 */
@Local
public interface ConciliacionContableService extends EntityService<ConciliacionContable> {

    /**
     * Recupera la cabecera para una cuenta bancaria y período, creándola si
     * todavía no existe (idempotente - primera vez que alguien abre la
     * pantalla de conciliación para ese mes).
     * @param idCuentaBancaria : Id de la cuenta bancaria
     * @param idPeriodo        : Id del periodo contable
     * @return                 : ConciliacionContable existente o recien creada
     * @throws Throwable       : Excepcion
     */
    ConciliacionContable obtenerOCrear(Long idCuentaBancaria, Long idPeriodo) throws Throwable;

    /**
     * Recalcula los contadores (totalGrupos, totalPendientesExtracto,
     * totalPendientesAsiento) contando en vivo los registros reales - se
     * llama despues de conciliar o deshacer un grupo.
     * @param idConciliacionContable : Id de la cabecera a recalcular
     * @throws Throwable            : Excepcion
     */
    void recalcularContadores(Long idConciliacionContable) throws Throwable;

    /**
     * Marca la cuenta/período como verificado - solo permitido cuando no
     * quedan pendientes en ninguno de los dos lados.
     * @param idConciliacionContable : Id de la cabecera
     * @param usuario                : Usuario que verifica
     * @throws Throwable             : Excepcion (si aun hay pendientes)
     */
    void verificar(Long idConciliacionContable, String usuario) throws Throwable;

}
