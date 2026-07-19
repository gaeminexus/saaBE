package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.FormaPagoLiquidacionCompraCompraDaoService;
import com.saa.model.cxp.FormaPagoLiquidacionCompraCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FormaPagoLiquidacionCompraCompraDaoServiceImpl extends EntityDaoImpl<FormaPagoLiquidacionCompraCompra> implements FormaPagoLiquidacionCompraCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","liquidacion","formaPago","valor","plazo","unidadTiempo"};
	}
}
