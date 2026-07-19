package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleRetencionCompraDaoService;
import com.saa.model.cxp.DetalleRetencionCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleRetencionCompraDaoServiceImpl extends EntityDaoImpl<DetalleRetencionCompra> implements DetalleRetencionCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","retencion","tipoDocReten","numDocReten","fechaEmiDoc","codImpuesto","codRetencion","baseImponible","porcentajeReten","valorReten","estado"};
	}
}
