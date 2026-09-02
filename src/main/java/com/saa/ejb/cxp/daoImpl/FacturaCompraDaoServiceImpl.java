package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.FacturaCompraDaoService;
import com.saa.model.cxp.FacturaCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
@Stateless
public class FacturaCompraDaoServiceImpl extends EntityDaoImpl<FacturaCompra> implements FacturaCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","observacion","subtotal","subcero","subtotal5","subtotal8","pIVA","vIVA","vIVA5","vIVA8","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","totalSinSub","ahorroSub","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","formaPago","estado","estadoEmision","estadoPago","asiento","esReembolso","codDocReembolso","totalComprobantesReembolso","totalBaseImponibleReembolso","totalImpuestoReembolso","sustentoTributario","fechaRegistroContable","motivoAnulacion","fechaAnulacion","usuarioAnulacion","esIntermediario","idProductoIntermediario"};
	}

	@Override
	public List<FacturaCompra> selectPendientesSustento(Long idEmpresa) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				"select f from FacturaCompra f where f.sustentoTributario is null and f.estado = :estado");
		if (idEmpresa != null) {
			jpql.append(" and f.empresa.codigo = :idEmpresa");
		}
		jpql.append(" order by f.fecha desc");
		TypedQuery<FacturaCompra> query = em.createQuery(jpql.toString(), FacturaCompra.class);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		return query.getResultList();
	}

	@Override
	public List<FacturaCompra> selectActivasByTitular(Long idTitular) throws Throwable {
		TypedQuery<FacturaCompra> query = em.createQuery(
				"select f from FacturaCompra f where f.titular.codigo = :idTitular "
				+ "and f.estado = :estado and (f.estadoEmision is null or f.estadoEmision <> 3) "
				+ "order by f.fecha", FacturaCompra.class);
		query.setParameter("idTitular", idTitular);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		return query.getResultList();
	}
}
