package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PathLiquidacionCompraCompraDaoService;
import com.saa.model.cxp.PathLiquidacionCompraCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathLiquidacionCompraCompraDaoServiceImpl extends EntityDaoImpl<PathLiquidacionCompraCompra> implements PathLiquidacionCompraCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","liquidacion","path","alterno"};
	}
}
