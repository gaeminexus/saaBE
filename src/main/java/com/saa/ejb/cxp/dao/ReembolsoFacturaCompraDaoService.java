package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.ReembolsoFacturaCompra;

import jakarta.ejb.Local;

/**
 * DAO para ReembolsoFacturaCompra (PGS.RMBF).
 */
@Local
public interface ReembolsoFacturaCompraDaoService extends EntityDao<ReembolsoFacturaCompra> {

    /**
     * Devuelve los reembolsos ACTIVOS (estado=1) de una factura de compra, ordenados por id.
     * Devuelve lista vacia si no hay registros (no lanza excepcion).
     * @param idFactura id de la factura de compra (PGS.FCTC.ID)
     */
    List<ReembolsoFacturaCompra> selectByFactura(Long idFactura);
}
