package com.saa.ejb.cxc.dao;
import com.saa.basico.util.EntityDao;
import com.saa.model.cxc.RetencionV2;
import java.util.List;
import jakarta.ejb.Local;
@Local
public interface RetencionV2DaoService extends EntityDao<RetencionV2> {
	/**
	 * BE-9b: total retenido por retencion e impuesto de las retenciones EMITIDAS (CBR.RTV2/DRV2, no las
	 * recibidas de PGS.RCV2). Una consulta agregada por cada bloque de 500 ids (el limite de Oracle para
	 * un IN es 1000), no una por retencion.
	 *
	 * @param ids : Ids de RetencionV2
	 * @return : Filas {idRetencion (Long), codImpuesto (String: '1' renta, '2' IVA, otro), suma de valorReten (Double)};
	 *         solo detalle activo. Lista vacia si no hay ids.
	 */
	List<Object[]> selectTotalesPorImpuesto(List<Long> ids);
}
