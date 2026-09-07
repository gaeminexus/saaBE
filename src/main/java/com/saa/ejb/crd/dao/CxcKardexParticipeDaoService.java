package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CxcKardexParticipe;

import jakarta.ejb.Local;

@Local
public interface CxcKardexParticipeDaoService extends EntityDao<CxcKardexParticipe> {

    /**
     * Renglones más recientes del kardex CXC (CRD.CXCK) de una cuenta por cobrar de partícipe,
     * ordenados por fecha DESC. Agregado para {@code /movil} (§5.3 del contrato de
     * intranet-movil): consumido por {@code GET /movil/cuenta-individual/{idEntidad}}.
     *
     * <p>Limitado a propósito (§4.7 del contrato: sin paginación no se sirve una colección que
     * pueda crecer sin techo) — no es un {@code selectAll} del kardex completo.</p>
     *
     * @param codigoCxcParticipe : Código de la cuenta por cobrar (CRD.CXCP) del partícipe
     * @param limite             : Máximo de renglones a devolver
     * @return                   : Renglones del kardex; lista VACÍA si no tiene ninguno
     * @throws Throwable         : Excepcion
     */
    List<CxcKardexParticipe> selectRecientesByCxcParticipe(Long codigoCxcParticipe, int limite) throws Throwable;
}
