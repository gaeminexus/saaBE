package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleNotaDebitoDaoService;
import com.saa.model.cxc.DetalleNotaDebito;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleNotaDebitoDaoServiceImpl extends EntityDaoImpl<DetalleNotaDebito> implements DetalleNotaDebitoDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "notaDebito", "descripcion", "cantidad", "valor", "subTotal", "descuento", "baseImponible", "porcentajeIVA", "valorIVA", "porcentajeICE", "valorICE", "subsidio", "total", "estado"};
	}
}
