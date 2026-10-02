package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.PrestamoSeguro;

import jakarta.ejb.Local;

@Local
public interface PrestamoSeguroDaoService extends EntityDao<PrestamoSeguro> {

    /**
     * Los préstamos de un documento de póliza (factura o nota), con el préstamo ya cargado.
     *
     * @param idDocumento código del documento (CRD.POSG)
     * @return préstamos del documento, en el orden en que se grabaron
     * @throws Throwable Si ocurre un error
     */
    List<PrestamoSeguro> selectByDocumento(Long idDocumento) throws Throwable;

    /**
     * Las filas de VARIOS documentos en una sola consulta (familia factura + sus ND/NC), para
     * las novedades (§7) y el choque {@code CUOTA_YA_CUBIERTA} (§5) sin N+1.
     *
     * @param idsDocumento códigos de documento (CRD.POSG)
     * @return filas de todos esos documentos
     * @throws Throwable Si ocurre un error
     */
    List<PrestamoSeguro> selectByDocumentos(List<Long> idsDocumento) throws Throwable;
}
