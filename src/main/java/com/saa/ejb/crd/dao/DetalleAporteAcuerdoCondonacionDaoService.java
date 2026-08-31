package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleAporteAcuerdoCondonacion;

import jakarta.ejb.Local;

/**
 * Interface DAO para la entidad DetalleAporteAcuerdoCondonacion (CRD.DAAP): el desglose por
 * tipo de aporte del cruce que cubre la parte {@code valorPagarAportes} de un
 * {@code AcuerdoCondonacion}.
 */
@Local
public interface DetalleAporteAcuerdoCondonacionDaoService extends EntityDao<DetalleAporteAcuerdoCondonacion> {

    /**
     * Las líneas de aporte de un acuerdo.
     *
     * @param idAcuerdo  : Código del acuerdo (CRD.ACCN)
     * @return           : Listado; vacío si el acuerdo no tiene parte de aportes
     * @throws Throwable : Excepcion
     */
    List<DetalleAporteAcuerdoCondonacion> selectByAcuerdo(Long idAcuerdo) throws Throwable;
}
