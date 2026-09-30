package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetallePlazoVencido;

import jakarta.ejb.Local;

@Local
public interface DetallePlazoVencidoDaoService extends EntityDao<DetallePlazoVencido> {

    /**
     * Las filas de seguro anulado de una declaración, ordenadas por código. Las usa el reverso
     * (§7 del contrato) para restituir el seguro a cada cuota que siga sin pagar.
     *
     * @param idDeclaracion código de la declaración (CRD.PLVN)
     * @return filas de CRD.DPLV de esa declaración (vacía si no anuló ningún seguro)
     * @throws Throwable Si ocurre un error
     */
    List<DetallePlazoVencido> selectByDeclaracion(Long idDeclaracion) throws Throwable;
}
