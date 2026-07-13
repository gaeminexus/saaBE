package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleLiquidacionCompraDaoService;
import com.saa.model.cxc.DetalleLiquidacionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleLiquidacionCompraDaoServiceImpl extends EntityDaoImpl<DetalleLiquidacionCompra> implements DetalleLiquidacionCompraDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "liquidacion", "descripcion", "cantidad", "valor", "subTotal", "porcentajeIVA", "valorIVA", "porcentajeICE", "valorICE", "subsidio", "precioSinSub", "descuento", "total", "producto", "estado"};
	}
}
