package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.FormaPagoNegociacionDaoService;
import com.saa.model.cxp.FormaPagoNegociacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class FormaPagoNegociacionDaoServiceImpl extends EntityDaoImpl<FormaPagoNegociacion> implements FormaPagoNegociacionDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","negociacion","numeroCuota","descripcion","fechaPago","porcentaje","valorCuota","estado","orden"};
	}
}
