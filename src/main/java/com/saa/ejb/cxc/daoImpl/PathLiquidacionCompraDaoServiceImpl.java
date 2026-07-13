package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.PathLiquidacionCompraDaoService;
import com.saa.model.cxc.PathLiquidacionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PathLiquidacionCompraDaoServiceImpl extends EntityDaoImpl<PathLiquidacionCompra> implements PathLiquidacionCompraDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "liquidacion", "path", "alterno"};
	}
}
