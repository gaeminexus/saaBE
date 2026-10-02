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

    /**
     * TODAS las filas (vigentes o ya reversadas) de cualquiera de {@code idsCuota}, en una sola
     * consulta — «PSCT por id de cuota», el único punto de esta búsqueda (S13,
     * {@code docs/logica-negocio/crd/DISENO-POLIZAS-SEGURO-PRESTAMOS.md} §5.6bis). Dos usos:
     * (a) el abono a capital, ANTES de historizar sus cuotas pendientes, para saber qué pólizas
     * vivas tocan y cuánto re-repartir; (b) el reverso del abono, para re-enlazar por
     * {@code HistDetallePrestamo.codigoOriginal} solo si el PSCT original seguía vigente al
     * momento del abono.
     *
     * @param idsCuota códigos de cuota (CRD.DTPR) a buscar, vivan o no
     * @return filas encontradas, con el documento y el préstamo-dentro-del-documento ya cargados;
     *         vacía si {@code idsCuota} es nulo o vacío
     * @throws Throwable Si ocurre un error
     */
    List<CuotaSeguro> selectByCuotas(List<Long> idsCuota) throws Throwable;
}
