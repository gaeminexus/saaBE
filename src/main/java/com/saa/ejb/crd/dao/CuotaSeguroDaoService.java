package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.CuotaSeguro;

import jakarta.ejb.Local;

@Local
public interface CuotaSeguroDaoService extends EntityDao<CuotaSeguro> {

    /**
     * Las filas VIGENTES ({@code fechaReverso IS NULL}) de un documento. Para el reverso de
     * {@code POST /posg/{id}/anular} (contrato §6).
     *
     * @param idDocumento código del documento (CRD.POSG)
     * @return filas vigentes del documento
     * @throws Throwable Si ocurre un error
     */
    List<CuotaSeguro> selectVigentesByDocumento(Long idDocumento) throws Throwable;

    /**
     * Filas VIGENTES ({@code fechaReverso IS NULL}) de ese {@code campo}, sobre cualquiera de
     * las cuotas dadas, que pertenezcan a un documento FUERA de {@code idsDocumentoExcluir} (la
     * familia factura+ND/NC que se está distribuyendo). Para el choque {@code CUOTA_YA_CUBIERTA}
     * de {@code POST /posg/{id}/distribuir} (contrato §5): dos pólizas con vigencias que se
     * pisan sobre la misma cuota y el mismo tipo de seguro.
     *
     * @param idsCuota            cuotas a verificar (CRD.DTPR) — {@code CuotaSeguro.idCuota} es
     *                             un {@code Long} plano, SIN relación (H82: la cuota puede
     *                             haberse borrado por un abono o un reverso)
     * @param campo               1 DESGRAVAMEN o 2 SEGURO — {@code com.saa.rubros.CampoSeguroCuota}
     * @param idsDocumentoExcluir documentos de la propia familia (nunca chocan entre sí)
     * @return filas en conflicto, con el documento ya cargado
     * @throws Throwable Si ocurre un error
     */
    List<CuotaSeguro> selectVigentesByCuotasYCampoExcluyendo(List<Long> idsCuota, Long campo,
            List<Long> idsDocumentoExcluir) throws Throwable;
}
