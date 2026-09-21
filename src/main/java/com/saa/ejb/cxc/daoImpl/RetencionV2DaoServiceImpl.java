package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.RetencionV2DaoService;
import com.saa.model.cxc.RetencionV2;
import java.util.ArrayList;
import java.util.List;
import com.saa.rubros.Estado;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class RetencionV2DaoServiceImpl extends EntityDaoImpl<RetencionV2> implements RetencionV2DaoService {
	@PersistenceContext
	EntityManager em;
	private static final int TAMANO_BLOQUE_IDS = 500;

	@Override
	public List<Object[]> selectTotalesPorImpuesto(List<Long> ids) {
		System.out.println("selectTotalesPorImpuesto RetencionV2: " + (ids != null ? ids.size() : 0) + " retencion(es)");
		List<Object[]> resultado = new ArrayList<Object[]>();
		if (ids == null || ids.isEmpty()) {
			return resultado;
		}
		for (int desde = 0; desde < ids.size(); desde += TAMANO_BLOQUE_IDS) {
			List<Long> bloque = ids.subList(desde, Math.min(desde + TAMANO_BLOQUE_IDS, ids.size()));
			@SuppressWarnings("unchecked")
			List<Object[]> filas = em.createQuery(
					"select d.retencionV2.id, d.codImpuesto, sum(d.valorReten) from DetalleRetencionV2 d "
							+ "where d.retencionV2.id in :ids and d.estado = :activo "
							+ "group by d.retencionV2.id, d.codImpuesto")
					.setParameter("ids", bloque)
					.setParameter("activo", Long.valueOf(Estado.ACTIVO))
					.getResultList();
			resultado.addAll(filas);
		}
		return resultado;
	}

	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "tipoComprobante", "facturador", "proveedor", "tipoDoc", "periodoFiscal", "numero", "numEstablecimiento", "numPtoEmision", "secuencial", "ambiente", "clave", "fecha", "observacion", "total", "ptoEmision", "usuario", "pathGen", "autorizacion", "fechaAutorizacion", "estado", "estadoEmision"};
	}
}
