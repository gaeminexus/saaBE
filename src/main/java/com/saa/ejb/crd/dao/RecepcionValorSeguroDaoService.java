/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.RecepcionValorSeguro;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad RecepcionValorSeguro (CRD.RVSG): recepción del valor
 *         de seguro (sepelio) pendiente de aprobación de contabilidad.
 */
@Local
public interface RecepcionValorSeguroDaoService extends EntityDao<RecepcionValorSeguro> {

    /**
     * Las recepciones de un estado puntual, para la bandeja de contabilidad.
     *
     * @param estado     : {@link com.saa.rubros.CrdEstadoRecepcionSeguro}
     * @return           : Listado por fecha de registro ascendente (FIFO); VACÍO si no hay ninguna
     * @throws Throwable : Excepcion
     */
    List<RecepcionValorSeguro> selectByEstado(Long estado) throws Throwable;

    /**
     * Las recepciones de un partícipe, para su ficha.
     *
     * @param idEntidad  : Código de la entidad (CRD.ENTD)
     * @return           : Listado, la más reciente primero; VACÍO si no tiene ninguna
     * @throws Throwable : Excepcion
     */
    List<RecepcionValorSeguro> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Recepciones VIGENTES cuya referencia (recortada) coincide con la buscada, para el chequeo
     * de unicidad al registrar. MISMO criterio que el índice único {@code CRD.UX_RVSG_REFERENCIA}:
     * {@code TRIM(RVSGRFRN)} y excluye las recepciones RECHAZADAS y ANULADAS (una recepción que
     * no vale libera su referencia). Los valores exentos '9' y '09' los decide el llamador, igual
     * que en cobros.
     *
     * @param referenciaTrim : Ya recortada por el llamador; este método nunca la recorta
     * @return               : Recepciones en conflicto (normalmente 0 o 1)
     * @throws Throwable     : Excepcion
     */
    List<RecepcionValorSeguro> selectByReferencia(String referenciaTrim) throws Throwable;

    /**
     * Lee la recepción con bloqueo de escritura (FOR UPDATE) dentro de la transacción en curso:
     * dos aprobaciones o anulaciones simultáneas de la misma recepción se serializan y la
     * segunda ve el estado ya cambiado, en vez de generar el asiento dos veces.
     *
     * @param idRecepcion : Código de la recepción (CRD.RVSG)
     * @return            : La recepción bloqueada, o {@code null} si no existe
     * @throws Throwable  : Excepcion
     */
    RecepcionValorSeguro selectParaActualizar(Long idRecepcion) throws Throwable;
}
