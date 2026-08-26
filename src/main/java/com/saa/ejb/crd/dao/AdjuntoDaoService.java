package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.Adjunto;

import jakarta.ejb.Local;

@Local
public interface AdjuntoDaoService extends EntityDao<Adjunto> {

    /**
     * Adjuntos ACTIVOS de un tipo puntual que referencian un id (ADJNIDRF) dado. Genérico a
     * propósito: {@code ADJNIDRF} no tiene FK — puede apuntar a CNBPCDGO, o a cualquier otro
     * documento que use este mismo catálogo de adjuntos más adelante.
     *
     * @param idReferencia Valor de ADJNIDRF (p.ej. CNBPCDGO de una cuenta bancaria)
     * @param idTipoAdjunto Código de CRD.TPDJ
     * @return Lista de coincidencias (normalmente 0 o 1)
     * @throws Throwable Si ocurre un error
     */
    List<Adjunto> selectByReferenciaYTipo(Long idReferencia, Long idTipoAdjunto) throws Throwable;
}
