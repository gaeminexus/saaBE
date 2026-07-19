package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleNotaCreditoCompraDaoService;
import com.saa.model.cxp.DetalleNotaCreditoCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleNotaCreditoCompraDaoServiceImpl extends EntityDaoImpl<DetalleNotaCreditoCompra> implements DetalleNotaCreditoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","notaCredito","descripcion","cantidad","valor","subTotal","descuento","baseImponible","porcentajeIVA","valorIVA","porcentajeICE","valorICE","subsidio","total","producto","estado"};
	}
}
