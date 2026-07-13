package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleNotaCreditoDaoService;
import com.saa.model.cxc.DetalleNotaCredito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleNotaCreditoDaoServiceImpl extends EntityDaoImpl<DetalleNotaCredito> implements DetalleNotaCreditoDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "notaCredito", "descripcion", "cantidad", "valor", "subTotal", "descuento", "baseImponible", "porcentajeIVA", "valorIVA", "porcentajeICE", "valorICE", "subsidio", "total", "producto", "estado"};
	}
}
