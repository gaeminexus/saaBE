package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.RetencionDaoService;
import com.saa.model.cxc.Retencion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class RetencionDaoServiceImpl extends EntityDaoImpl<Retencion> implements RetencionDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "tipoComprobante", "facturador", "proveedor", "tipoDoc", "periodoFiscal", "numero", "numEstablecimiento", "numPtoEmision", "secuencial", "ambiente", "clave", "fecha", "observacion", "total", "ptoEmision", "usuario", "pathGen", "autorizacion", "fechaAutorizacion", "estado", "estadoEmision"};
	}
}
