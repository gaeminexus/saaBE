package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class LiquidacionCompraCompraDaoServiceImpl extends EntityDaoImpl<LiquidacionCompraCompra> implements LiquidacionCompraCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","totalSinSub","ahorroSub","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision"};
	}
}
