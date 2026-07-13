package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.RetencionV2DaoService;
import com.saa.model.cxc.RetencionV2;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class RetencionV2DaoServiceImpl extends EntityDaoImpl<RetencionV2> implements RetencionV2DaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "tipoComprobante", "facturador", "proveedor", "tipoDoc", "periodoFiscal", "numero", "numEstablecimiento", "numPtoEmision", "secuencial", "ambiente", "clave", "fecha", "observacion", "total", "ptoEmision", "usuario", "pathGen", "autorizacion", "fechaAutorizacion", "estado", "estadoEmision"};
	}
}
