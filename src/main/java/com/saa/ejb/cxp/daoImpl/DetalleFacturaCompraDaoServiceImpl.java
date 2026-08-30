package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.DetalleFacturaCompraDaoService;
import com.saa.model.cxp.DetalleFacturaCompra;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
@Stateless
public class DetalleFacturaCompraDaoServiceImpl extends EntityDaoImpl<DetalleFacturaCompra> implements DetalleFacturaCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","factura","descripcion","cantidad","valor","subTotal","descuento","baseImponible","porcentajeIVA","valorIVA","porcentajeICE","valorICE","subsidio","precioSinSub","total","producto","codigoIVASRI","estado"};
	}

	@Override
	public List<DetalleFacturaCompra> selectByFactura(Long idFactura) throws Throwable {
		return em.createQuery(
				"select d from DetalleFacturaCompra d where d.factura.id = :idFactura",
				DetalleFacturaCompra.class)
				.setParameter("idFactura", idFactura)
				.getResultList();
	}
}
