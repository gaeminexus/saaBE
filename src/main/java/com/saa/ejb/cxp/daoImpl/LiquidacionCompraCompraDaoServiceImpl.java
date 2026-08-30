package com.saa.ejb.cxp.daoImpl;
import java.util.List;
import com.saa.basico.utilImpl.EntityDaoImpl;
import com.saa.ejb.cxp.dao.LiquidacionCompraCompraDaoService;
import com.saa.model.cxp.LiquidacionCompraCompra;
import com.saa.rubros.Estado;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
@Stateless
public class LiquidacionCompraCompraDaoServiceImpl extends EntityDaoImpl<LiquidacionCompraCompra> implements LiquidacionCompraCompraDaoService {
	@PersistenceContext EntityManager em;
	@Override
	public String[] obtieneCampos() {
		return new String[]{"id","tipoComprobante","empresa","titular","tipoDoc","numero","numEstablecimiento","numPtoEmision","secuencial","ambiente","clave","fecha","observacion","subtotal","subcero","pIVA","vIVA","vICE","vIRBPNR","descuento","porDescuento","propina","subsidio","totalSinSub","ahorroSub","total","ptoEmision","usuario","pathGen","autorizacion","fechaAutorizacion","estado","estadoEmision","sustentoTributario","fechaRegistroContable","motivoAnulacion","fechaAnulacion","usuarioAnulacion"};
	}

	@Override
	public List<LiquidacionCompraCompra> selectPendientesSustento(Long idEmpresa) throws Throwable {
		StringBuilder jpql = new StringBuilder(
				"select l from LiquidacionCompraCompra l where l.sustentoTributario is null and l.estado = :estado");
		if (idEmpresa != null) {
			jpql.append(" and l.empresa.codigo = :idEmpresa");
		}
		jpql.append(" order by l.fecha desc");
		TypedQuery<LiquidacionCompraCompra> query = em.createQuery(jpql.toString(), LiquidacionCompraCompra.class);
		query.setParameter("estado", Long.valueOf(Estado.ACTIVO));
		if (idEmpresa != null) {
			query.setParameter("idEmpresa", idEmpresa);
		}
		return query.getResultList();
	}
}
