package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.PagoNegociacionDaoService;
import com.saa.model.cxp.PagoNegociacion;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class PagoNegociacionDaoServiceImpl extends EntityDaoImpl<PagoNegociacion> implements PagoNegociacionDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","formaPago","fechaPago","valorPago","descripcion","tipoPago","facturaCompra","facturado","pagado","refComprobante","estado","usuario","fechaRegistro"};
	}
}
