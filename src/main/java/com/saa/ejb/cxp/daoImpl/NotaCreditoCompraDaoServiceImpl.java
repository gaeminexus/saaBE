package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.NotaCreditoCompraDaoService;
import com.saa.model.cxp.NotaCreditoCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class NotaCreditoCompraDaoServiceImpl extends EntityDaoImpl<NotaCreditoCompra> implements NotaCreditoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","tipoDocModificado","numDocModificado","fechaEmisionDM","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision"};
	}
}
