package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.model.cxp.FacturaCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FacturaCompraDaoServiceImpl extends EntityDaoImpl<FacturaCompra> implements FacturaCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","observacion","subtotal","subcero","subtotal5","subtotal8","pIVA","vIVA","vIVA5","vIVA8","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","totalSinSub","ahorroSub","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","formaPago","estado","estadoEmision"};
	}
}
