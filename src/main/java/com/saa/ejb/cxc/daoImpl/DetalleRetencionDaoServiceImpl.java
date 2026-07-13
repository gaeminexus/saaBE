package com.saa.ejb.cxc.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxc.dao.DetalleRetencionDaoService;
import com.saa.model.cxc.DetalleRetencion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleRetencionDaoServiceImpl extends EntityDaoImpl<DetalleRetencion> implements DetalleRetencionDaoService {
	@PersistenceContext
	EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id", "retencion", "tipoDocReten", "numDocReten", "fechaEmiDoc", "codImpuesto", "codRetencion", "baseImponible", "porcentajeReten", "valorReten", "estado"};
	}
}
