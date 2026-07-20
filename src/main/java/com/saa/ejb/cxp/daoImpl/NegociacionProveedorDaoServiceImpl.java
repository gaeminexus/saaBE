package com.saa.ejb.cxp.daoImpl;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.NegociacionProveedorDaoService;
import com.saa.model.cxp.NegociacionProveedor;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class NegociacionProveedorDaoServiceImpl extends EntityDaoImpl<NegociacionProveedor> implements NegociacionProveedorDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","empresa","titular","fechaNegociacion","fechaInicio","fechaFin","numContrato","descripcion","valorTotal","tipoFinanciacion","numeroPagos","observacion","estado","usuario","fechaRegistro","usuarioModif","fechaModif"};
	}
}
