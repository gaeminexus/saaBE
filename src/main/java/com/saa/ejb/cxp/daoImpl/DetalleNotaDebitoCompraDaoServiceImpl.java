package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleNotaDebitoCompraDaoService;
import com.saa.model.cxp.DetalleNotaDebitoCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleNotaDebitoCompraDaoServiceImpl extends EntityDaoImpl<DetalleNotaDebitoCompra> implements DetalleNotaDebitoCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","notaDebito","descripcion","cantidad","valor","subTotal","descuento","baseImponible","porcentajeIVA","valorIVA","porcentajeICE","valorICE","subsidio","total","estado"};
	}
}
