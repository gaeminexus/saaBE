package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.RetencionCompraDaoService;
import com.saa.model.cxp.RetencionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class RetencionCompraDaoServiceImpl extends EntityDaoImpl<RetencionCompra> implements RetencionCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","proveedor","tipoDoc","periodoFiscal","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","observacion","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision"};
	}
}
