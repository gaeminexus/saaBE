package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleLiquidacionCompraCompraDaoService;
import com.saa.model.cxp.DetalleLiquidacionCompraCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleLiquidacionCompraCompraDaoServiceImpl extends EntityDaoImpl<DetalleLiquidacionCompraCompra> implements DetalleLiquidacionCompraCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","liquidacion","descripcion","cantidad","valor","subTotal","porcentajeIVA","valorIVA","porcentajeICE","valorICE","subsidio","precioSinSub","descuento","total","producto","estado"};
	}
}
